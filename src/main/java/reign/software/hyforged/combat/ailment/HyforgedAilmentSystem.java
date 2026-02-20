package reign.software.hyforged.combat.ailment;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.combat.CombatMath;
import reign.software.hyforged.combat.CombatMeta;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.damage.DamageTypeExtensionRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import com.hypixel.hytale.logger.HytaleLogger;

/**
 * System for triggering ailments based on accumulated elemental damage.
 * <p>
 * Runs in the {@code inspectDamageGroup} after damage has been applied.
 * Accumulates damage by element and triggers ailments when thresholds are exceeded.
 * <p>
 * Ailment duration can be scaled by the attacker's {@code effect-duration-bps} stat.
 *
 * @see AilmentAccumulatorComponent
 * @see AilmentRegistry
 */
public class HyforgedAilmentSystem extends DamageEventSystem {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    /** Stat ID for effect duration scaling */
    private static final StatId EFFECT_DURATION_STAT = StatId.hyforged("effect-duration-bps");

    /** Stat ID for ailment threshold scaling */
    private static final StatId AILMENT_THRESHOLD_STAT = StatId.hyforged("ailment-threshold-bps");
    
    /** Cached stat index for effect duration */
    private int effectDurationStatIndex = -1;

    /** Cached stat index for ailment threshold */
    private int ailmentThresholdStatIndex = -1;

    /**
     * Per-damage-cause cached ailment chance stat index (attacker-side direct trigger chance).
     * Populated lazily on first encounter via {@link DamageTypeExtensionRegistry#getAilmentChanceStatForDamage}.
     */
    private final Map<String, Integer> ailmentChanceIndices = new HashMap<>();

    /**
     * Per-damage-cause cached ailment duration stat index (attacker-side duration scaling).
     */
    private final Map<String, Integer> ailmentDurationIndices = new HashMap<>();

    /**
     * Per-damage-cause cached ailment damage stat index (attacker-side damage scaling; stored as meta).
     */
    private final Map<String, Integer> ailmentDamageIndices = new HashMap<>();
    
    @Nonnull
    private final ComponentType<EntityStore, AilmentAccumulatorComponent> accumulatorComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, EffectControllerComponent> effectControllerComponentType;
    
    @Nonnull
    private final Query<EntityStore> query;
    
    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;
    
    public HyforgedAilmentSystem(
            @Nonnull ComponentType<EntityStore, AilmentAccumulatorComponent> accumulatorComponentType
    ) {
        this.accumulatorComponentType = accumulatorComponentType;
        this.effectControllerComponentType = EffectControllerComponent.getComponentType();
        
        // Query for entities with the accumulator component
        this.query = Query.and(accumulatorComponentType, effectControllerComponentType);
        
        // Run after damage has been applied
        this.dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, DamageSystems.ApplyDamage.class)
        );
    }
    
    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
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
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damage
    ) {
        // Skip cancelled or zero damage
        if (damage.isCancelled() || damage.getAmount() <= 0) {
            return;
        }
        
        // Get damage cause to determine element
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause == null) {
            return;
        }
        
        // Get element tag from damage type extension
        String elementTag = getElementTagForDamage(damageCause);
        if (elementTag == null) {
            return;
        }
        
        // Check if there's an ailment for this element
        AilmentDefinition ailment = AilmentRegistry.get().getByElement(elementTag);
        if (ailment == null) {
            return;
        }
        
        // Get the defender's accumulator component
        AilmentAccumulatorComponent accumulator = archetypeChunk.getComponent(index, accumulatorComponentType);
        if (accumulator == null) {
            return;
        }
        
        // Get the defender's effect controller
        EffectControllerComponent effectController = archetypeChunk.getComponent(index, effectControllerComponentType);
        if (effectController == null) {
            return;
        }

        // Store ailment-damage-bps meta for DoT systems (attacker side, data-driven)
        if (damage.getSource() instanceof Damage.EntitySource entitySourceForMeta) {
            Ref<EntityStore> attackerRefForMeta = entitySourceForMeta.getRef();
            if (attackerRefForMeta.isValid()) {
                int ailmentDmgIdx = getOrCacheAilmentDamageIdx(store, damageCause);
                if (ailmentDmgIdx >= 0) {
                    int ailmentDmgBps = StatAccessor.getStatValueInt(store, attackerRefForMeta, ailmentDmgIdx);
                    if (ailmentDmgBps != 0) {
                        damage.putMetaObject(CombatMeta.AILMENT_DAMAGE_BPS, ailmentDmgBps);
                    }
                }
            }
        }

        // Direct chance-based ailment trigger (attacker side, per-element, data-driven)
        // If the attacker has an ailment chance stat for this damage type and the roll succeeds,
        // apply the ailment immediately and reset the accumulator.
        if (damage.getSource() instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef.isValid()) {
                int chanceIdx = getOrCacheAilmentChanceIdx(store, damageCause);
                if (chanceIdx >= 0) {
                    int chanceBps = StatAccessor.getStatValueInt(store, attackerRef, chanceIdx);
                    if (chanceBps > 0 && CombatMath.rollChance(chanceBps)) {
                        applyAilment(ailment, damage, damageCause, effectController, store, commandBuffer, archetypeChunk, index);
                        accumulator.resetAccumulation(elementTag);
                        return;
                    }
                }
            }
        }

        // Accumulate damage for this element
        long currentTime = System.currentTimeMillis();
        float damageAmount = damage.getAmount();
        
        // Configure accumulator with ailment thresholds (scaled by defender stats)
        int threshold = getScaledAilmentThreshold(ailment, store, archetypeChunk.getReferenceTo(index));
        accumulator.setThreshold(elementTag, threshold);
        accumulator.setWindow(elementTag, ailment.accumulationWindowMs());
        
        // Check if threshold is reached
        boolean thresholdReached = accumulator.accumulateDamage(elementTag, damageAmount, currentTime);
        
        if (thresholdReached) {
            // Trigger the ailment
            applyAilment(ailment, damage, damageCause, effectController, store, commandBuffer, archetypeChunk, index);
            
            // Reset the accumulator for this element
            accumulator.resetAccumulation(elementTag);
        }
    }
    
    /**
     * Apply an ailment effect to the target.
     */
    private void applyAilment(
            @Nonnull AilmentDefinition ailment,
            @Nonnull Damage damage,
            @Nonnull DamageCause damageCause,
            @Nonnull EffectControllerComponent effectController,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            int index
    ) {
        // Get the entity effect
        EntityEffect entityEffect = EntityEffect.getAssetMap().getAsset(ailment.entityEffectId());
        if (entityEffect == null) {
            LOGGER.atWarning().log("EntityEffect not found for ailment: %s (effectId: %s)",
                    ailment.id(), ailment.entityEffectId());
            return;
        }
        
        // Calculate duration with attacker's effect duration scaling
        float duration = calculateScaledDuration(ailment, damage, damageCause, store);
        
        // Get the defender ref
        Ref<EntityStore> defenderRef = archetypeChunk.getReferenceTo(index);
        
        // Apply the effect
        boolean applied = effectController.addEffect(
                defenderRef,
                entityEffect,
                duration,
                OverlapBehavior.OVERWRITE,
                store
        );
        
        if (applied) {
            LOGGER.at(Level.FINE).log("Applied ailment %s (duration: %ss)",
                    ailment.displayName(), duration);
        }
    }
    
    /**
     * Calculate the scaled duration for an ailment.
     * <p>
     * Duration is scaled by:<br>
     * 1. Attacker's global {@code effect-duration-bps}<br>
     * 2. Attacker's per-element {@code ailmentDurationStat} (data-driven via registry)<br>
     * Both are applied as independent MORE multipliers.
     */
    private float calculateScaledDuration(
            @Nonnull AilmentDefinition ailment,
            @Nonnull Damage damage,
            @Nonnull DamageCause damageCause,
            @Nonnull Store<EntityStore> store
    ) {
        float baseDuration = ailment.baseDurationSeconds();
        
        // Get attacker's effect duration stat if available
        if (damage.getSource() instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef.isValid()) {
                // 1. Global effect-duration-bps
                int effectDurationBps = getEffectDurationBps(store, attackerRef);
                if (effectDurationBps != 0) {
                    baseDuration *= (1.0f + effectDurationBps / 10000.0f);
                }
                // 2. Per-element ailment-duration stat (data-driven)
                int durationIdx = getOrCacheAilmentDurationIdx(store, damageCause);
                if (durationIdx >= 0) {
                    int elementDurationBps = StatAccessor.getStatValueInt(store, attackerRef, durationIdx);
                    if (elementDurationBps != 0) {
                        baseDuration *= (1.0f + elementDurationBps / 10000.0f);
                    }
                }
            }
        }
        
        return baseDuration;
    }
    
    /**
     * Get the effect duration stat value from a stat component.
     */
    private int getEffectDurationBps(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> attackerRef
    ) {
        if (effectDurationStatIndex < 0) {
            effectDurationStatIndex = StatDefinitionRegistry.get().getIndex(EFFECT_DURATION_STAT);
        }
        
        if (effectDurationStatIndex < 0) {
            return 0;
        }

        return StatAccessor.getStatValueInt(store, attackerRef, effectDurationStatIndex);
    }

    /**
     * Get the scaled ailment threshold for the defender.
     * <p>
     * Threshold scaling uses {@code ailment-threshold-bps}:
     * {@code scaled = base * (1 + bps / 10000)}.
     */
    private int getScaledAilmentThreshold(
            @Nonnull AilmentDefinition ailment,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> defenderRef
    ) {
        int baseThreshold = ailment.baseThreshold();
        int thresholdBps = getAilmentThresholdBps(store, defenderRef);
        if (thresholdBps == 0) {
            return baseThreshold;
        }

        float multiplier = 1.0f + (thresholdBps / 10000.0f);
        int scaled = Math.round(baseThreshold * multiplier);
        return Math.max(1, scaled);
    }

    /**
     * Get the ailment threshold stat value from a stat component.
     */
    private int getAilmentThresholdBps(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> defenderRef
    ) {
        if (ailmentThresholdStatIndex < 0) {
            ailmentThresholdStatIndex = StatDefinitionRegistry.get().getIndex(AILMENT_THRESHOLD_STAT);
        }

        if (ailmentThresholdStatIndex < 0) {
            return 0;
        }

        return StatAccessor.getStatValueInt(store, defenderRef, ailmentThresholdStatIndex);
    }

    /**
     * Look up and cache the ailment chance stat index for a given damage cause.
     * Returns -1 if no ailment chance stat is defined for this damage type.
     */
    private int getOrCacheAilmentChanceIdx(@Nonnull Store<EntityStore> store, @Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        Integer cached = ailmentChanceIndices.get(id);
        if (cached != null) {
            return cached;
        }
        StatId statId = DamageTypeExtensionRegistry.get().getAilmentChanceStatForDamage(damageCause);
        int idx = statId != null ? StatDefinitionRegistry.get().getIndex(statId) : -1;
        ailmentChanceIndices.put(id, idx);
        return idx;
    }

    /**
     * Look up and cache the ailment duration stat index for a given damage cause.
     * Returns -1 if no ailment duration stat is defined for this damage type.
     */
    private int getOrCacheAilmentDurationIdx(@Nonnull Store<EntityStore> store, @Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        Integer cached = ailmentDurationIndices.get(id);
        if (cached != null) {
            return cached;
        }
        StatId statId = DamageTypeExtensionRegistry.get().getAilmentDurationStatForDamage(damageCause);
        int idx = statId != null ? StatDefinitionRegistry.get().getIndex(statId) : -1;
        ailmentDurationIndices.put(id, idx);
        return idx;
    }

    /**
     * Look up and cache the ailment damage stat index for a given damage cause.
     * Returns -1 if no ailment damage stat is defined for this damage type.
     */
    private int getOrCacheAilmentDamageIdx(@Nonnull Store<EntityStore> store, @Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        Integer cached = ailmentDamageIndices.get(id);
        if (cached != null) {
            return cached;
        }
        StatId statId = DamageTypeExtensionRegistry.get().getAilmentDamageStatForDamage(damageCause);
        int idx = statId != null ? StatDefinitionRegistry.get().getIndex(statId) : -1;
        ailmentDamageIndices.put(id, idx);
        return idx;
    }
    
    /**
     * Get the element tag for a damage cause.
     * <p>
     * This looks up the element tag from the damage type extension registry.
     */
    @Nullable
    private String getElementTagForDamage(@Nonnull DamageCause damageCause) {
        DamageTypeExtensionRegistry registry = DamageTypeExtensionRegistry.get();
        return registry.getElementTagForDamage(damageCause);
    }
}
