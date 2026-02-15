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
import reign.software.hyforged.hud.HyforgedHud;
import reign.software.hyforged.hud.HyforgedHudManager;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Updates the currency section of the composite Hyforged HUD.
 * Displays Tradebar balance in inventory and vault.
 */
public class CurrencyHudSystem extends DelayedEntitySystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(CurrencyHudSystem.class.getName());

    /** Update interval in seconds - currency doesn't change rapidly */
    private static final float UPDATE_INTERVAL_SEC = 0.5f;

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
                
                if (!playerUuid.equals(vault.getOwnerUUID())) {
                    invalidRefs.add(vaultRef);
                    continue;
                }
                
                vaultBalance += vault.getStoredAmount();
                hasVault = true;
            }
            
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

        // Only show HUD if player has any tradebars
        boolean shouldShowHud = inventoryBalance > 0 || hasVault;

        HyforgedHud hud = HyforgedHudManager.getOrCreate(playerUuid, player, playerRef);
        if (hud == null) {
            return; // Client not ready yet
        }

        if (!shouldShowHud) {
            if (cache != null) {
                hud.hideCurrency();
                playerCache.remove(playerUuid);
            }
            return;
        }

        if (needsUpdate) {
            hud.updateCurrency(inventoryBalance, vaultBalance, hasVault);
            playerCache.put(playerUuid, new CurrencyHudCache(inventoryBalance, vaultBalance, hasVault));
        }
    }

    /**
     * Register a vault reference for HUD tracking.
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
     *
     * @param playerUuid The player's UUID
     */
    public static void clearPlayerVaults(@Nonnull UUID playerUuid) {
        playerVaultRefs.remove(playerUuid);
        playerCache.remove(playerUuid);
    }

    private record CurrencyHudCache(int inventoryBalance, int vaultBalance, boolean hasVault) {}
}
