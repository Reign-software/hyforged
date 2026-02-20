package reign.software.hyforged.quality.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.logger.HytaleLogger;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.logging.Level;

/**
 * Stub system for item-quantity and item-rarity multiplier stats.
 * <p>
 * Caches the two loot-multiplier stat indices and, on each item-entity spawn, attempts
 * to resolve the nearest player's stats for debug logging. Actual item-quantity roll
 * expansion and rarity-tier upgrading are deferred until a centralized loot-generation
 * API is available.
 * <p>
 * <b>Stat IDs cached:</b>
 * <ul>
 *   <li>{@code hyforged:item-quantity-increased-bps} — player's item quantity multiplier</li>
 *   <li>{@code hyforged:item-rarity-increased-bps} — player's item rarity multiplier</li>
 * </ul>
 * <p>
 * <b>TODO (Phase 6+):</b>
 * <ul>
 *   <li><b>Item quantity:</b> When a centralized drop generation API exists, use
 *       {@code item-quantity-increased-bps} to roll extra item copies. Formula:
 *       {@code extraCopies = floor(bps / 10000.0)} with a fractional chance for the
 *       remainder, using {@code ThreadLocalRandom}.</li>
 *   <li><b>Item rarity:</b> Pass {@code item-rarity-increased-bps} as a quality-tier
 *       bonus into {@link QualityRollerService#rollQuality} by extending
 *       {@link reign.software.hyforged.quality.model.QualityRollContext} with a
 *       player-rarity-bonus field. This avoids modifying core quality rules.</li>
 * </ul>
 */
public class HyforgedLootMultiplierSystem extends RefChangeSystem<EntityStore, ItemComponent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final StatId ITEM_QUANTITY_BPS = StatId.hyforged("item-quantity-increased-bps");
    private static final StatId ITEM_RARITY_BPS   = StatId.hyforged("item-rarity-increased-bps");

    private final Set<Dependency<EntityStore>> dependencies;

    // Lazily cached stat indices
    private int itemQuantityIndex = -1;
    private int itemRarityIndex   = -1;
    private boolean indicesCached = false;

    public HyforgedLootMultiplierSystem() {
        this.dependencies = Set.of(
                new SystemDependency<>(Order.AFTER, LootQualitySystem.class)
        );
    }

    @Nonnull
    @Override
    public ComponentType<EntityStore, ItemComponent> componentType() {
        return ItemComponent.getComponentType();
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return ItemComponent.getComponentType();
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ItemComponent itemComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        ensureIndicesCached();

        if (itemQuantityIndex < 0 && itemRarityIndex < 0) {
            return; // Stats not registered — nothing to do
        }

        // Attempt to find a nearby player to read their loot multiplier stats
        Ref<EntityStore> playerRef = resolveLootSourcePlayer(ref, store);
        if (playerRef == null) {
            return;
        }

        int quantityBps = itemQuantityIndex >= 0
                ? StatAccessor.getStatValueInt(store, playerRef, itemQuantityIndex)
                : 0;
        int rarityBps = itemRarityIndex >= 0
                ? StatAccessor.getStatValueInt(store, playerRef, itemRarityIndex)
                : 0;

        if (quantityBps == 0 && rarityBps == 0) {
            return; // No bonuses — nothing to apply
        }

        LOGGER.at(Level.FINE).log(
                "HyforgedLootMultiplierSystem: item-quantity-bps=%d item-rarity-bps=%d "
                        + "(TODO: apply extra rolls / rarity tier upgrade)",
                quantityBps, rarityBps);

        // TODO (item-quantity): Roll extra copies of this item drop (not exactly "copies" if we want to apply quality rerolls to the extras) using quantityBps:
        //   int extra = quantityBps / 10000;
        //   double fractional = (quantityBps % 10000) / 10000.0;
        //   if (ThreadLocalRandom.current().nextDouble() < fractional) extra++;
        //   for (int i = 0; i < extra; i++) spawnItemCopy(ref, store, commandBuffer);

        // TODO (item-rarity): Upgrade the rolled quality tier using rarityBps:
        //   Pass rarityBps into QualityRollerService as a contextual bonus once
        //   QualityRollContext is extended to accept a player-rarity-bonus field.
    }

    @Override
    public void onComponentSet(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ItemComponent oldComponent,
            @Nonnull ItemComponent newComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No action needed when item component is replaced
    }

    @Override
    public void onComponentRemoved(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ItemComponent itemComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No action needed when item is picked up or despawned
    }

    /**
     * Attempt to resolve which player caused this item to drop.
     * Returns {@code null} if no player can be identified.
     * TODO: implement proximity or metadata-based player lookup in full implementation.
     */
    @Nullable
    private Ref<EntityStore> resolveLootSourcePlayer(
            @Nonnull Ref<EntityStore> itemRef,
            @Nonnull Store<EntityStore> store
    ) {
        return null;
    }

    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        itemQuantityIndex = registry.getIndex(ITEM_QUANTITY_BPS);
        itemRarityIndex   = registry.getIndex(ITEM_RARITY_BPS);
        indicesCached     = true;

        LOGGER.at(Level.FINE).log(
                "HyforgedLootMultiplierSystem: cached indices — quantity=%d, rarity=%d",
                itemQuantityIndex, itemRarityIndex);
    }
}
