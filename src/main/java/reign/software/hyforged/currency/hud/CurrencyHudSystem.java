package reign.software.hyforged.currency.hud;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.currency.component.TradebarVaultComponent;
import reign.software.hyforged.currency.service.CurrencyService;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * ECS system that updates the Currency HUD for each player.
 * Displays Tradebar balance in inventory and vault.
 */
public class CurrencyHudSystem extends DelayedEntitySystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(CurrencyHudSystem.class.getName());

    /** Whether MultipleHUD is available at runtime */
    private static final boolean MULTIPLE_HUD_AVAILABLE;

    static {
        boolean available = false;
        try {
            Class.forName("com.buuz135.mhud.MultipleHUD");
            available = true;
        } catch (ClassNotFoundException e) {
            LOGGER.warning("MultipleHUD not available - currency HUD disabled");
        }
        MULTIPLE_HUD_AVAILABLE = available;
    }

    /** Unique identifier for this HUD in MultipleHUD */
    public static final String HUD_ID = "hyforged:currency";

    /** Update interval in seconds - currency doesn't change rapidly */
    private static final float UPDATE_INTERVAL_SEC = 0.5f;

    /** Per-player HUD instances for updates */
    private static final Map<UUID, CurrencyHud> playerHuds = new ConcurrentHashMap<>();

    /** Cache of last values to avoid redundant updates */
    private static final Map<UUID, CurrencyHudCache> playerCache = new ConcurrentHashMap<>();

    /** 
     * Tracks vault block references for each player.
     * Key: player UUID, Value: set of vault block references
     */
    private static final Map<UUID, Set<Ref<ChunkStore>>> playerVaultRefs = new ConcurrentHashMap<>();

    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType;

    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType;

    @Nonnull
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    public CurrencyHudSystem() {
        super(UPDATE_INTERVAL_SEC);
        this.playerComponentType = Player.getComponentType();
        this.playerRefComponentType = PlayerRef.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
        this.query = Query.and(playerComponentType, playerRefComponentType, uuidComponentType);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (!MULTIPLE_HUD_AVAILABLE) {
            return;
        }

        Player player = archetypeChunk.getComponent(index, playerComponentType);
        PlayerRef playerRef = archetypeChunk.getComponent(index, playerRefComponentType);
        UUIDComponent uuidComponent = archetypeChunk.getComponent(index, uuidComponentType);

        if (player == null || playerRef == null || uuidComponent == null) {
            return;
        }

        UUID playerUuid = uuidComponent.getUuid();
        
        // Get inventory balance
        Inventory inventory = player.getInventory();
        int inventoryBalance = 0;
        if (inventory != null) {
            CombinedItemContainer container = inventory.getCombinedHotbarFirst();
            if (container != null) {
                inventoryBalance = container.countItemStacks(
                    stack -> CurrencyService.TRADEBAR_ITEM_ID.equals(stack.getItemId())
                );
            }
        }

        // Get vault balance by aggregating all tracked vaults for this player
        int vaultBalance = 0;
        boolean hasVault = false;
        
        Set<Ref<ChunkStore>> vaultRefs = playerVaultRefs.get(playerUuid);
        if (vaultRefs != null && !vaultRefs.isEmpty()) {
            // Iterate over tracked vaults and sum balances
            Set<Ref<ChunkStore>> invalidRefs = new HashSet<>();
            ComponentType<ChunkStore, TradebarVaultComponent> vaultComponentType = 
                    HyforgedPlugin.getInstance().getTradebarVaultComponentType();
            
            for (Ref<ChunkStore> vaultRef : vaultRefs) {
                if (!vaultRef.isValid()) {
                    invalidRefs.add(vaultRef);
                    continue;
                }
                
                TradebarVaultComponent vault = vaultRef.getStore().getComponent(vaultRef, vaultComponentType);
                if (vault == null) {
                    invalidRefs.add(vaultRef);
                    continue;
                }
                
                // Verify ownership still matches
                if (!playerUuid.equals(vault.getOwnerUUID())) {
                    invalidRefs.add(vaultRef);
                    continue;
                }
                
                vaultBalance += vault.getStoredAmount();
                hasVault = true;
            }
            
            // Clean up invalid references
            if (!invalidRefs.isEmpty()) {
                vaultRefs.removeAll(invalidRefs);
            }
        }

        // Check if values changed since last update
        CurrencyHudCache cache = playerCache.get(playerUuid);
        boolean needsUpdate = cache == null
                || cache.inventoryBalance != inventoryBalance
                || cache.vaultBalance != vaultBalance
                || cache.hasVault != hasVault;

        // Only show HUD if player has any tradebars (DEBUG: Always true)
        boolean shouldShowHud = true; // inventoryBalance > 0 || hasVault;

        com.buuz135.mhud.MultipleHUD multipleHUD = com.buuz135.mhud.MultipleHUD.getInstance();
        CurrencyHud existingHud = playerHuds.get(playerUuid);

        if (!shouldShowHud) {
            // Hide HUD if visible
            if (existingHud != null) {
                multipleHUD.hideCustomHud(player, playerRef, HUD_ID);
                playerHuds.remove(playerUuid);
                playerCache.remove(playerUuid);
            }
            return;
        }

        // Create HUD if not exists
        CurrencyHud currencyHud;
        if (existingHud == null) {
            currencyHud = new CurrencyHud(playerRef);
            multipleHUD.setCustomHud(player, playerRef, HUD_ID, currencyHud);
            playerHuds.put(playerUuid, currencyHud);
        } else {
            currencyHud = existingHud;
        }

        // Only update if values changed
        if (needsUpdate) {
            currencyHud.updateValues(inventoryBalance, vaultBalance, hasVault);
            playerCache.put(playerUuid, new CurrencyHudCache(inventoryBalance, vaultBalance, hasVault));
        }
    }

    /**
     * Register a vault reference for HUD tracking.
     * Called when a player interacts with their vault.
     *
     * @param playerUuid The player's UUID
     * @param vaultRef   The vault block reference
     */
    public static void registerVaultAccess(@Nonnull UUID playerUuid, @Nonnull Ref<ChunkStore> vaultRef) {
        playerVaultRefs.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet())
                .add(vaultRef);
        LOGGER.fine(() -> "Registered vault access for player " + playerUuid);
    }

    /**
     * Notify the system that a player has a vault, so it can be shown on the HUD.
     * Called when a player interacts with their vault.
     *
     * @param playerUuid The player's UUID
     * @param vaultBalance The current vault balance
     * @deprecated Use {@link #registerVaultAccess(UUID, Ref)} instead
     */
    @Deprecated
    public static void notifyVaultAccess(UUID playerUuid, int vaultBalance) {
        CurrencyHudCache cache = playerCache.get(playerUuid);
        if (cache != null) {
            playerCache.put(playerUuid, new CurrencyHudCache(cache.inventoryBalance, vaultBalance, true));
        } else {
            playerCache.put(playerUuid, new CurrencyHudCache(0, vaultBalance, true));
        }
    }

    /**
     * Clear all tracked vaults for a player.
     * Called when player logs out or when vaults need to be re-discovered.
     *
     * @param playerUuid The player's UUID
     */
    public static void clearPlayerVaults(@Nonnull UUID playerUuid) {
        playerVaultRefs.remove(playerUuid);
        playerCache.remove(playerUuid);
    }

    /**
     * Cache for last HUD values to avoid redundant updates.
     */
    private record CurrencyHudCache(int inventoryBalance, int vaultBalance, boolean hasVault) {}
}
