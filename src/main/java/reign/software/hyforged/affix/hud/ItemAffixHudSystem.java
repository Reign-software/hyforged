package reign.software.hyforged.affix.hud;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.service.AffixTooltipProvider;
import reign.software.hyforged.affix.service.HyforgedItemDataService;
import reign.software.hyforged.hud.HyforgedHud;
import reign.software.hyforged.hud.HyforgedHudManager;
import reign.software.hyforged.quality.service.HyforgedQualityService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HUD system that displays affix information for the player's currently held item.
 * <p>
 * Polls the active hotbar slot at a fixed interval and compares the current item's
 * affix data hash to the last displayed one. Only sends a HUD update when the item
 * or its affixes change (dirty-check pattern).
 * <p>
 * Shows a compact affix tooltip above the XP bar when the held item changes to one
 * with affixes. The tooltip auto-hides after a short duration.
 */
public class ItemAffixHudSystem extends DelayedEntitySystem<EntityStore> {

    /** Poll every 0.25 seconds - fast enough for responsiveness, avoids per-tick cost */
    private static final float UPDATE_INTERVAL_SEC = 0.25f;

    /** How long the tooltip stays visible after an item change (milliseconds) */
    private static final long DISPLAY_DURATION_MS = 4000;

    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType;

    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType;

    @Nonnull
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    /**
     * Per-player tracking of the last displayed item (for dirty-checking).
     * Key = player UUID, Value = cached state of what was last displayed.
     */
    private static final Map<UUID, CachedAffixDisplay> playerCache = new ConcurrentHashMap<>();

    public ItemAffixHudSystem() {
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

        HyforgedHud hud = HyforgedHudManager.getOrCreate(playerUuid, player, playerRef);
        if (hud == null) {
            return; // Client not ready yet
        }

        // Get the item in the active hotbar slot
        ItemStack heldItem = getHeldItem(player);

        // Read affix data from the held item
        String currentItemId = null;
        HyforgedItemData currentData = null;
        String currentQuality = null;
        if (heldItem != null && !ItemStack.isEmpty(heldItem)) {
            currentData = HyforgedItemDataService.read(heldItem);
            if (currentData.hasAffixes()) {
                currentItemId = heldItem.getItemId();
                currentQuality = HyforgedQualityService.getEffectiveQuality(heldItem);
            } else {
                currentData = null; // No affixes, treat as empty
            }
        }

        // Dirty check: has anything changed since last update?
        CachedAffixDisplay cached = playerCache.get(playerUuid);
        int currentHash = computeHash(currentItemId, currentData);

        // Auto-hide: if tooltip is visible and display duration has elapsed, hide it
        if (cached != null && cached.visible() && cached.shownAtMs() > 0) {
            long elapsed = System.currentTimeMillis() - cached.shownAtMs();
            if (elapsed >= DISPLAY_DURATION_MS) {
                hud.hideItemAffixes();
                // Keep the hash so we don't re-show for the same item
                playerCache.put(playerUuid, new CachedAffixDisplay(cached.hash(), false, 0));
                return;
            }
        }

        if (cached != null && cached.hash() == currentHash) {
            return; // No change, skip update
        }

        // Update the HUD
        if (currentItemId == null || currentData == null) {
            // No item with affixes - hide the panel immediately
            hud.hideItemAffixes();
            playerCache.put(playerUuid, new CachedAffixDisplay(0, false));
        } else {
            // Generate tooltip content and send to HUD
            AffixTooltipProvider.TooltipContent content = AffixTooltipProvider.generateTooltip(currentData);
            if (content.hasContent()) {
                String displayName = formatDisplayName(currentItemId, currentQuality);
                hud.updateItemAffixes(displayName, content);
                playerCache.put(playerUuid, new CachedAffixDisplay(currentHash, true));
            } else {
                hud.hideItemAffixes();
                playerCache.put(playerUuid, new CachedAffixDisplay(0, false));
            }
        }
    }

    /**
     * Get the item in the player's active hotbar slot.
     */
    @Nullable
    private static ItemStack getHeldItem(@Nonnull Player player) {
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return null;
        }

        byte activeSlot = inventory.getActiveHotbarSlot();
        if (activeSlot < 0) {
            return null;
        }

        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar == null) {
            return null;
        }

        return hotbar.getItemStack(activeSlot);
    }

    /**
     * Format an item ID into a display name with quality prefix.
     * <p>
     * e.g., "Hyforged:Crude_Sword" with quality "Rare" -> "Rare Crude Sword"
     */
    @Nonnull
    private static String formatDisplayName(@Nonnull String itemId, @Nullable String quality) {
        String baseName = formatItemName(itemId);
        if (quality != null && !quality.isBlank()) {
            return quality + " " + baseName;
        }
        return baseName;
    }

    /**
     * Format an item ID into a display name.
     * <p>
     * Strips namespace prefix and replaces underscores with spaces.
     * e.g., "Hyforged:Crude_Sword" → "Crude Sword"
     */
    @Nonnull
    private static String formatItemName(@Nonnull String itemId) {
        // Strip namespace (e.g., "Hyforged:" or "Hytale:")
        int colonIndex = itemId.indexOf(':');
        String name = colonIndex >= 0 ? itemId.substring(colonIndex + 1) : itemId;
        // Replace underscores with spaces
        return name.replace('_', ' ');
    }

    /**
     * Compute a hash for dirty-checking. If the hash changes, we need to update the HUD.
     */
    private static int computeHash(@Nullable String itemId, @Nullable HyforgedItemData data) {
        if (itemId == null || data == null || !data.hasAffixes()) {
            return 0;
        }
        // Combine item ID hash with affix data hash
        int hash = itemId.hashCode();
        hash = 31 * hash + data.affixes().hashCode();
        return hash;
    }

    /**
     * Clear cached display state for a disconnecting player.
     */
    public static void clearCache(@Nonnull UUID playerUuid) {
        playerCache.remove(playerUuid);
    }

    /**
     * Simple record to track what we last displayed for a player.
     * Includes the timestamp when the tooltip was shown for auto-hide.
     */
    private record CachedAffixDisplay(int hash, boolean visible, long shownAtMs) {
        CachedAffixDisplay(int hash, boolean visible) {
            this(hash, visible, visible ? System.currentTimeMillis() : 0);
        }
    }
}
