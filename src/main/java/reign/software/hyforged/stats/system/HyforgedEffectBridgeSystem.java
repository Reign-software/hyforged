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
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.effect.HyforgedEffectDefinition;
import reign.software.hyforged.effect.HyforgedEffectRegistry;
import reign.software.hyforged.effect.HyforgedEffectModifierSpec;
import reign.software.hyforged.stats.component.EffectBridgeComponent;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

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
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapType;

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
        this.entityStatMapType = EntityStatMap.getComponentType();
        this.effectControllerType = EffectControllerComponent.getComponentType();
        this.effectBridgeType = plugin.getEffectBridgeComponentType();

        // Query for entities with all required components
        // EntityStatMap is needed for putModifier/removeModifier operations
        this.query = Query.and(
            entityStatMapType,
            Query.and(statComponentType, Query.and(effectControllerType, effectBridgeType))
        );

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
        EntityStatMap entityStatMap = chunk.getComponent(index, entityStatMapType);
        EffectControllerComponent effectController = chunk.getComponent(index, effectControllerType);
        EffectBridgeComponent effectBridge = chunk.getComponent(index, effectBridgeType);

        if (statComponent == null || entityStatMap == null || effectController == null || effectBridge == null) {
            return;
        }

        processEffectChanges(statComponent, entityStatMap, effectController, effectBridge);
    }

    /**
     * Process effect changes for an entity.
     * <p>
     * Detects added and removed effects by comparing current active effects
     * to the previously bridged set.
     *
     * @param statComponent The entity's stat component (for dirty flags)
     * @param entityStatMap The entity's stat map (for modifier operations)
     * @param effectController The entity's effect controller
     * @param effectBridge The entity's effect bridge tracking component
     */
    private void processEffectChanges(
            @Nonnull HyforgedStatComponent statComponent,
            @Nonnull EntityStatMap entityStatMap,
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
                removeEffectModifiers(statComponent, entityStatMap, bridgedIndex);
                effectBridge.unmarkBridged(bridgedIndex);
            }
        }

        // Detect added effects (in current but not in bridged)
        for (int effectIndex : currentEffectIndices) {
            if (!bridgedSet.contains(effectIndex)) {
                applyEffectModifiers(statComponent, entityStatMap, effectIndex);
                effectBridge.markBridged(effectIndex);
            }
        }
    }

    /**
     * Apply stat modifiers from an EntityEffect to the EntityStatMap.
     * <p>
     * Uses EntityStatMap.putModifier() with a unique key format to allow
     * removal when the effect ends.
     *
     * @param statComponent The entity's stat component (for dirty flags)
     * @param entityStatMap The entity's stat map (for modifier operations)
     * @param effectIndex The index of the effect to apply modifiers from
     */
    private void applyEffectModifiers(
            @Nonnull HyforgedStatComponent statComponent,
            @Nonnull EntityStatMap entityStatMap,
            int effectIndex
    ) {
        EntityEffect effect = getEntityEffect(effectIndex);
        if (effect == null) {
            return;
        }
        boolean applied = false;

        applied |= applyEntityStatModifiers(entityStatMap, effect);
        applied |= applyStaticEffectModifiers(entityStatMap, effect);
        applied |= applyHyforgedEffectModifiers(entityStatMap, effect.getId());

        if (applied) {
            statComponent.markAllDirty();
        }
    }

    /**
     * Remove all modifiers from an effect.
     * <p>
     * Uses EntityStatMap.removeModifier() with the same key format used when applying.
     *
     * @param statComponent The entity's stat component (for dirty flags)
     * @param entityStatMap The entity's stat map (for modifier operations)
     * @param effectIndex The index of the effect whose modifiers to remove
     */
    private void removeEffectModifiers(
            @Nonnull HyforgedStatComponent statComponent,
            @Nonnull EntityStatMap entityStatMap,
            int effectIndex
    ) {
        EntityEffect effect = getEntityEffect(effectIndex);
        if (effect == null) {
            // Effect asset not found, cannot determine which stats to clean up
            LOGGER.warning("Cannot remove modifiers for unknown effect index: " + effectIndex);
            statComponent.markAllDirty();
            return;
        }
        boolean removed = false;

        removed |= removeEntityStatModifiers(entityStatMap, effect);
        removed |= removeStaticEffectModifiers(entityStatMap, effect);
        removed |= removeHyforgedEffectModifiers(entityStatMap, effect.getId());

        if (removed) {
            statComponent.markAllDirty();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Removed modifiers from effect: " + effect.getId());
            }
        }
    }

    private boolean applyEntityStatModifiers(
            @Nonnull EntityStatMap entityStatMap,
            @Nonnull EntityEffect effect
    ) {
        Int2FloatMap entityStats = effect.getEntityStats();
        if (entityStats == null || entityStats.isEmpty()) {
            return false;
        }

        ValueType valueType = effect.getValueType();
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        boolean applied = false;

        for (Int2FloatMap.Entry entry : entityStats.int2FloatEntrySet()) {
            int hytaleStatIndex = entry.getIntKey();
            float value = entry.getFloatValue();

            StatId statId = mapHytaleStatToHyforged(hytaleStatIndex);
            if (statId == null) {
                continue;
            }

            int hyforgedStatIndex = registry.getIndex(statId);
            if (hyforgedStatIndex < 0) {
                continue;
            }

            String modifierKey = buildEntityModifierKey(effect.getId(), hyforgedStatIndex);
            HyforgedModifier modifier = buildEffectModifier(valueType, value, hyforgedStatIndex, effect.getId());

            entityStatMap.putModifier(hyforgedStatIndex, modifierKey, modifier);
            applied = true;

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Applied effect modifier: " + modifierKey + " -> " + statId.fullId() + " = " + value);
            }
        }

        return applied;
    }

    private boolean applyStaticEffectModifiers(
            @Nonnull EntityStatMap entityStatMap,
            @Nonnull EntityEffect effect
    ) {
        Int2ObjectMap<StaticModifier[]> statModifiers = effect.getStatModifiers();
        if (statModifiers == null || statModifiers.isEmpty()) {
            return false;
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        boolean applied = false;

        for (Int2ObjectMap.Entry<StaticModifier[]> entry : statModifiers.int2ObjectEntrySet()) {
            int hytaleStatIndex = entry.getIntKey();
            StaticModifier[] modifiers = entry.getValue();
            if (modifiers == null || modifiers.length == 0) {
                continue;
            }

            StatId statId = mapHytaleStatToHyforged(hytaleStatIndex);
            if (statId == null) {
                continue;
            }

            int hyforgedStatIndex = registry.getIndex(statId);
            if (hyforgedStatIndex < 0) {
                continue;
            }

            for (StaticModifier staticModifier : modifiers) {
                if (staticModifier == null) {
                    continue;
                }

                HyforgedModifier modifier = buildStaticEffectModifier(staticModifier, hyforgedStatIndex, effect.getId());
                if (modifier == null) {
                    continue;
                }

                String modifierKey = buildStaticModifierKey(effect.getId(), hyforgedStatIndex, staticModifier);
                entityStatMap.putModifier(hyforgedStatIndex, modifierKey, modifier);
                applied = true;

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Applied static effect modifier: " + modifierKey + " -> " + statId.fullId());
                }
            }
        }

        return applied;
    }

    private boolean removeEntityStatModifiers(
            @Nonnull EntityStatMap entityStatMap,
            @Nonnull EntityEffect effect
    ) {
        Int2FloatMap entityStats = effect.getEntityStats();
        if (entityStats == null || entityStats.isEmpty()) {
            return false;
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        boolean removed = false;

        for (Int2FloatMap.Entry entry : entityStats.int2FloatEntrySet()) {
            int hytaleStatIndex = entry.getIntKey();

            StatId statId = mapHytaleStatToHyforged(hytaleStatIndex);
            if (statId == null) {
                continue;
            }

            int hyforgedStatIndex = registry.getIndex(statId);
            if (hyforgedStatIndex < 0) {
                continue;
            }

            String modifierKey = buildEntityModifierKey(effect.getId(), hyforgedStatIndex);
            Modifier removedModifier = entityStatMap.removeModifier(hyforgedStatIndex, modifierKey);
            if (removedModifier != null) {
                removed = true;
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Removed effect modifier: " + modifierKey);
                }
            }
        }

        return removed;
    }

    private boolean removeStaticEffectModifiers(
            @Nonnull EntityStatMap entityStatMap,
            @Nonnull EntityEffect effect
    ) {
        Int2ObjectMap<StaticModifier[]> statModifiers = effect.getStatModifiers();
        if (statModifiers == null || statModifiers.isEmpty()) {
            return false;
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        boolean removed = false;

        for (Int2ObjectMap.Entry<StaticModifier[]> entry : statModifiers.int2ObjectEntrySet()) {
            int hytaleStatIndex = entry.getIntKey();
            StaticModifier[] modifiers = entry.getValue();
            if (modifiers == null || modifiers.length == 0) {
                continue;
            }

            StatId statId = mapHytaleStatToHyforged(hytaleStatIndex);
            if (statId == null) {
                continue;
            }

            int hyforgedStatIndex = registry.getIndex(statId);
            if (hyforgedStatIndex < 0) {
                continue;
            }

            for (StaticModifier staticModifier : modifiers) {
                if (staticModifier == null) {
                    continue;
                }

                String modifierKey = buildStaticModifierKey(effect.getId(), hyforgedStatIndex, staticModifier);
                Modifier removedModifier = entityStatMap.removeModifier(hyforgedStatIndex, modifierKey);
                if (removedModifier != null) {
                    removed = true;
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Removed static effect modifier: " + modifierKey);
                    }
                }
            }
        }

        return removed;
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

    private HyforgedModifier buildEffectModifier(
            @Nullable ValueType valueType,
            float value,
            int statIndex,
            @Nonnull String effectId
    ) {
        HyforgedModifier.Builder builder = HyforgedModifier.builder()
            .target(Modifier.ModifierTarget.MAX)
            .sourceType(HyforgedModifier.SourceType.EFFECT)
            .sourceId(effectId)
            .targetStat(statIndex);

        if (valueType == ValueType.Percent) {
            int bps = convertPercentToBps(value);
            return builder.increased(bps).build();
        }

        return builder.flat(Math.round(value)).build();
    }

    @Nullable
    private HyforgedModifier buildStaticEffectModifier(
            @Nonnull StaticModifier modifier,
            int statIndex,
            @Nonnull String effectId
    ) {
        HyforgedModifier.Builder builder = HyforgedModifier.builder()
                .target(modifier.getTarget())
                .sourceType(HyforgedModifier.SourceType.EFFECT)
                .sourceId(effectId)
                .targetStat(statIndex);

        float amount = modifier.getAmount();
        if (modifier.getCalculationType() == StaticModifier.CalculationType.ADDITIVE) {
            int flat = Math.round(amount);
            if (flat == 0) {
                return null;
            }
            return builder.flat(flat).build();
        }

        if (modifier.getCalculationType() == StaticModifier.CalculationType.MULTIPLICATIVE) {
            int bps = Math.round((amount - 1.0f) * HyforgedModifier.BPS_100_PERCENT);
            if (bps == 0) {
                return null;
            }
            return builder.more(bps).build();
        }

        return null;
    }

    private boolean applyHyforgedEffectModifiers(
            @Nonnull EntityStatMap entityStatMap,
            @Nonnull String effectId
    ) {
        HyforgedEffectDefinition definition = HyforgedEffectRegistry.get().get(effectId);
        if (definition == null || definition.getModifiers().isEmpty()) {
            return false;
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        boolean applied = false;

        int ordinal = 0;
        for (HyforgedEffectModifierSpec spec : definition.getModifiers()) {
            if (spec == null) {
                ordinal++;
                continue;
            }

            String specStatId = spec.getStatId();
            if (specStatId == null || specStatId.isEmpty()) {
                ordinal++;
                continue;
            }

            StatId statId;
            if (specStatId.contains(":")) {
                try {
                    statId = StatId.parse(specStatId);
                } catch (IllegalArgumentException ignored) {
                    ordinal++;
                    continue;
                }
            } else {
                statId = StatId.hyforged(specStatId);
            }

            int statIndex = registry.getIndex(statId);
            if (statIndex < 0) {
                ordinal++;
                continue;
            }

            HyforgedModifier modifier = HyforgedModifier.builder()
                    .target(spec.getTarget())
                    .stackType(spec.getStackType())
                    .amount(spec.getAmount())
                    .sourceType(HyforgedModifier.SourceType.EFFECT)
                    .sourceId(effectId)
                    .priority(spec.getPriority())
                    .targetStat(statIndex)
                    .build();

            String modifierKey = buildHyforgedModifierKey(effectId, statIndex, ordinal);
            entityStatMap.putModifier(statIndex, modifierKey, modifier);
            applied = true;
            ordinal++;
        }

        return applied;
    }

    private boolean removeHyforgedEffectModifiers(
            @Nonnull EntityStatMap entityStatMap,
            @Nonnull String effectId
    ) {
        HyforgedEffectDefinition definition = HyforgedEffectRegistry.get().get(effectId);
        if (definition == null || definition.getModifiers().isEmpty()) {
            return false;
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        boolean removed = false;

        int ordinal = 0;
        for (HyforgedEffectModifierSpec spec : definition.getModifiers()) {
            if (spec == null) {
                ordinal++;
                continue;
            }

            String specStatId = spec.getStatId();
            if (specStatId == null || specStatId.isEmpty()) {
                ordinal++;
                continue;
            }

            StatId statId;
            if (specStatId.contains(":")) {
                try {
                    statId = StatId.parse(specStatId);
                } catch (IllegalArgumentException ignored) {
                    ordinal++;
                    continue;
                }
            } else {
                statId = StatId.hyforged(specStatId);
            }

            int statIndex = registry.getIndex(statId);
            if (statIndex < 0) {
                ordinal++;
                continue;
            }

            String modifierKey = buildHyforgedModifierKey(effectId, statIndex, ordinal);
            Modifier removedModifier = entityStatMap.removeModifier(statIndex, modifierKey);
            if (removedModifier != null) {
                removed = true;
            }
            ordinal++;
        }

        return removed;
    }

    private String buildHyforgedModifierKey(
            @Nonnull String effectId,
            int statIndex,
            int ordinal
    ) {
        return EFFECT_SOURCE_PREFIX + effectId + ":hy:" + statIndex + ":" + ordinal;
    }

    private String buildEntityModifierKey(@Nonnull String effectId, int statIndex) {
        return EFFECT_SOURCE_PREFIX + effectId + ":" + statIndex;
    }

    private String buildStaticModifierKey(
            @Nonnull String effectId,
            int statIndex,
            @Nonnull StaticModifier modifier
    ) {
        return EFFECT_SOURCE_PREFIX + effectId + ":static:" + statIndex + ":" + modifier.getTarget() + ":" + modifier.getCalculationType();
    }

    private int convertPercentToBps(float value) {
        float abs = Math.abs(value);
        if (abs <= 1.0f) {
            return Math.round(value * HyforgedModifier.BPS_100_PERCENT);
        }
        return Math.round(value * 100f);
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

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();

        // If Hytale stat ID is namespaced, try it directly
        if (hytaleStatId.contains(":")) {
            try {
                StatId direct = StatId.parse(hytaleStatId);
                if (registry.getIndex(direct) >= 0) {
                    return direct;
                }
            } catch (IllegalArgumentException ignored) {
                // Fall through to hyforged namespace mapping
            }
        }

        // Fallback to naming convention: Hytale stat "MyStat" -> Hyforged stat "hyforged:MyStat"
        StatId fallback = StatId.hyforged(hytaleStatId);
        if (registry.getIndex(fallback) >= 0) {
            return fallback;
        }

        return null;
    }
}
