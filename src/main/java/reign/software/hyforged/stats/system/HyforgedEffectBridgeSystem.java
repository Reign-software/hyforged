package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.ValueType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.EffectBridgeComponent;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.component.ModifierSource;
import reign.software.hyforged.stats.component.ModifierType;
import reign.software.hyforged.stats.component.StatModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ECS System for bridging Hytale EntityEffect stat modifiers into Hyforged stats.
 * <p>
 * This system observes the EffectControllerComponent for effect changes and mirrors
 * any stat modifiers from EntityEffect assets into the HyforgedStatComponent. This
 * enables Hyforged's tag-based modifier system to work with Hytale's native effects.
 * <p>
 * <b>Effect Tracking Strategy:</b>
 * <ul>
 *   <li>Tracks previously bridged effects in EffectBridgeComponent</li>
 *   <li>Compares current active effects to bridged set each tick</li>
 *   <li>Detects additions (in current but not bridged) → apply modifiers</li>
 *   <li>Detects removals (in bridged but not current) → remove modifiers</li>
 * </ul>
 * <p>
 * <b>Modifier Source Pattern:</b>
 * <p>
 * Modifiers are created with sourceId format: {@code "effect:{effectId}"}
 * This allows removal by matching the prefix when an effect ends.
 * <p>
 * Following ECS principles, this system contains only processing logic.
 */
public class HyforgedEffectBridgeSystem extends EntityTickingSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(HyforgedEffectBridgeSystem.class.getName());

    /**
     * Source ID prefix for effect-based modifiers.
     */
    public static final String EFFECT_SOURCE_PREFIX = "effect:";

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final ComponentType<EntityStore, EffectControllerComponent> effectControllerType;

    @Nonnull
    private final ComponentType<EntityStore, EffectBridgeComponent> effectBridgeType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    public HyforgedEffectBridgeSystem() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.effectControllerType = EffectControllerComponent.getComponentType();
        this.effectBridgeType = plugin.getEffectBridgeComponentType();

        // Query for entities with all three components
        this.query = Query.and(statComponentType, Query.and(effectControllerType, effectBridgeType));

        // Run BEFORE stat computation so modifiers are included
        this.dependencies = Set.of(
            new SystemDependency<>(Order.BEFORE, HyforgedStatComputeSystem.class)
        );
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        HyforgedStatComponent statComponent = chunk.getComponent(index, statComponentType);
        EffectControllerComponent effectController = chunk.getComponent(index, effectControllerType);
        EffectBridgeComponent effectBridge = chunk.getComponent(index, effectBridgeType);

        if (statComponent == null || effectController == null || effectBridge == null) {
            return;
        }

        processEffectChanges(statComponent, effectController, effectBridge);
    }

    /**
     * Process effect changes for an entity.
     * <p>
     * Detects added and removed effects by comparing current active effects
     * to the previously bridged set.
     *
     * @param statComponent The entity's stat component
     * @param effectController The entity's effect controller
     * @param effectBridge The entity's effect bridge tracking component
     */
    private void processEffectChanges(
            @Nonnull HyforgedStatComponent statComponent,
            @Nonnull EffectControllerComponent effectController,
            @Nonnull EffectBridgeComponent effectBridge
    ) {
        // Get current active effect indices
        int[] currentEffectIndices = effectController.getActiveEffectIndexes();
        IntSet currentSet = new IntOpenHashSet(currentEffectIndices);
        IntSet bridgedSet = effectBridge.getBridgedEffectIndices();

        // Detect removed effects (in bridged but not in current)
        for (int bridgedIndex : bridgedSet.toIntArray()) {
            if (!currentSet.contains(bridgedIndex)) {
                removeEffectModifiers(statComponent, bridgedIndex);
                effectBridge.unmarkBridged(bridgedIndex);
            }
        }

        // Detect added effects (in current but not in bridged)
        for (int effectIndex : currentEffectIndices) {
            if (!bridgedSet.contains(effectIndex)) {
                applyEffectModifiers(statComponent, effectIndex);
                effectBridge.markBridged(effectIndex);
            }
        }
    }

    /**
     * Apply stat modifiers from an EntityEffect to the Hyforged stat component.
     *
     * @param statComponent The entity's stat component
     * @param effectIndex The index of the effect to apply modifiers from
     */
    private void applyEffectModifiers(
            @Nonnull HyforgedStatComponent statComponent,
            int effectIndex
    ) {
        EntityEffect effect = getEntityEffect(effectIndex);
        if (effect == null) {
            return;
        }

        Int2FloatMap entityStats = effect.getEntityStats();
        if (entityStats == null || entityStats.isEmpty()) {
            return;
        }

        String sourceId = EFFECT_SOURCE_PREFIX + effect.getId();
        ValueType valueType = effect.getValueType();
        ModifierType modifierType = mapValueTypeToModifierType(valueType);

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();

        // Apply each stat modifier from the effect
        for (Int2FloatMap.Entry entry : entityStats.int2FloatEntrySet()) {
            int hytaleStatIndex = entry.getIntKey();
            float value = entry.getFloatValue();

            // Try to find a matching Hyforged stat by mapping Hytale stat index
            // For now, skip if we can't map it (Hytale stat, not Hyforged)
            // Future: Add Hytale→Hyforged stat mapping
            StatId statId = mapHytaleStatToHyforged(hytaleStatIndex);
            if (statId == null) {
                continue;
            }

            int hyforgedStatIndex = registry.getIndex(statId);
            if (hyforgedStatIndex < 0) {
                continue;
            }

            // Convert value based on modifier type
            int intValue = convertValue(value, modifierType);

            StatModifier modifier = new StatModifier.Builder(sourceId)
                    .sourceType(ModifierSource.EFFECT)
                    .modifierType(modifierType)
                    .targetStat(hyforgedStatIndex)
                    .value(intValue)
                    .permanent() // Removed when effect ends
                    .build();

            statComponent.addModifier(modifier);

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Applied effect modifier: " + sourceId + " -> " + statId.fullId() + " = " + intValue);
            }
        }

        // Mark stats as needing recomputation
        statComponent.markAllDirty();
    }

    /**
     * Remove all modifiers from an effect.
     *
     * @param statComponent The entity's stat component
     * @param effectIndex The index of the effect whose modifiers to remove
     */
    private void removeEffectModifiers(
            @Nonnull HyforgedStatComponent statComponent,
            int effectIndex
    ) {
        EntityEffect effect = getEntityEffect(effectIndex);
        if (effect == null) {
            // Effect asset not found, fallback handled by dirty flag
            // Next stat computation will clean up orphaned modifiers
            LOGGER.warning("Cannot remove modifiers for unknown effect index: " + effectIndex);
            statComponent.markAllDirty();
            return;
        }

        String sourceId = EFFECT_SOURCE_PREFIX + effect.getId();
        boolean removed = statComponent.removeModifiersBySource(sourceId);

        if (removed) {
            statComponent.markAllDirty();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Removed modifiers from effect: " + effect.getId());
            }
        }
    }

    /**
     * Get an EntityEffect by its index.
     *
     * @param effectIndex The effect index
     * @return The EntityEffect, or null if not found
     */
    @Nullable
    private EntityEffect getEntityEffect(int effectIndex) {
        try {
            return EntityEffect.getAssetMap().getAsset(effectIndex);
        } catch (Exception e) {
            LOGGER.warning("Failed to get EntityEffect at index " + effectIndex + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Map Hytale's ValueType to Hyforged's ModifierType.
     * <p>
     * Hytale uses: Percent, Absolute
     * Hyforged uses: FLAT, INCREASED, MORE
     *
     * @param valueType The Hytale value type
     * @return The corresponding Hyforged modifier type
     */
    @Nonnull
    private ModifierType mapValueTypeToModifierType(@Nullable ValueType valueType) {
        if (valueType == null) {
            return ModifierType.FLAT;
        }
        return switch (valueType) {
            case Absolute -> ModifierType.FLAT;
            case Percent -> ModifierType.INCREASED;
        };
    }

    /**
     * Convert a float value to an integer based on modifier type.
     * <p>
     * For percentage/multiplier types, converts to basis points (10000 = 100%).
     *
     * @param value The float value from the effect
     * @param modifierType The modifier type
     * @return The integer value for the modifier
     */
    private int convertValue(float value, @Nonnull ModifierType modifierType) {
        return switch (modifierType) {
            case FLAT -> Math.round(value);
            case INCREASED, MORE -> Math.round(value * 100); // Convert percent to basis points
            case CAP -> Math.round(value);
        };
    }

    /**
     * Map a Hytale entity stat index to a Hyforged StatId.
     * <p>
     * This method looks up the Hytale EntityStatType by index, gets its ID,
     * and attempts to find a matching Hyforged stat. Hyforged stats that need
     * to receive effect modifiers should be registered as Hytale EntityStatType
     * via JSON in the Server/Hyforged/Entity/Stats directory.
     * <p>
     * Naming convention: If the Hytale stat ID matches a Hyforged stat name
     * (with hyforged: namespace), they are considered equivalent.
     *
     * @param hytaleStatIndex The Hytale stat index
     * @return The Hyforged StatId, or null if no mapping exists
     */
    @Nullable
    private StatId mapHytaleStatToHyforged(int hytaleStatIndex) {
        // Get the Hytale stat's string ID
        EntityStatType hytaleStatType = EntityStatType.getAssetMap().getAsset(hytaleStatIndex);
        if (hytaleStatType == null || hytaleStatType.isUnknown()) {
            return null;
        }
        
        String hytaleStatId = hytaleStatType.getId();
        
        // Try to find a Hyforged stat with matching name
        // Convention: Hytale stat "MyCustomStat" -> Hyforged stat "hyforged:MyCustomStat"
        StatId hyforgedStat = StatId.hyforged(hytaleStatId);
        
        // Check if this stat is registered in Hyforged's registry
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        if (registry.getIndex(hyforgedStat) >= 0) {
            return hyforgedStat;
        }
        
        // No matching Hyforged stat found
        return null;
    }
}
