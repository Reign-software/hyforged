package reign.software.hyforged.combat;

import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;

/**
 * Meta keys for combat pipeline data tracking.
 * <p>
 * These keys are used to pass data between combat systems and to the combat log,
 * enabling full per-attack breakdowns including base damage, resistance, and penetration.
 */
public final class CombatMeta {

    private CombatMeta() {
        // Utility class
    }

    // ==================== Pipeline Control ====================

    /**
     * Indicates that this damage event has already been processed by CombatService.
     * <p>
     * When true, ECS damage systems (hit resolution, auto-block, crit, resistance)
     * should skip their processing to avoid re-applying the combat pipeline.
     * This is set by CombatServiceImpl when dispatching damage events.
     */
    public static final MetaKey<Boolean> PIPELINE_PROCESSED = 
            Damage.META_REGISTRY.registerMetaObject(data -> Boolean.FALSE);

    // ==================== Damage Breakdown ====================

    /**
     * Base damage before any modifiers (resistance, block, crit).
     * <p>
     * Set by the first system in the pipeline to record original damage.
     */
    public static final MetaKey<Float> BASE_DAMAGE = 
            Damage.META_REGISTRY.registerMetaObject(data -> 0f);

    /**
     * Resistance applied in basis points.
     * <p>
     * Set by HyforgedDamageReductionSystem.
     */
    public static final MetaKey<Integer> RESISTANCE_BPS = 
            Damage.META_REGISTRY.registerMetaObject(data -> 0);

    /**
     * Penetration applied in basis points.
     * <p>
     * Set by HyforgedDamageReductionSystem.
     */
    public static final MetaKey<Integer> PENETRATION_BPS = 
            Damage.META_REGISTRY.registerMetaObject(data -> 0);

    /**
     * Effective resistance after penetration (resistance - penetration).
     * <p>
     * Set by HyforgedDamageReductionSystem.
     */
    public static final MetaKey<Integer> EFFECTIVE_RESISTANCE_BPS = 
            Damage.META_REGISTRY.registerMetaObject(data -> 0);

    /**
     * Block mitigation applied in basis points.
     * <p>
     * Set by HyforgedAutoBlockSystem when auto-block triggers.
     */
    public static final MetaKey<Integer> BLOCK_MITIGATION_BPS = 
            Damage.META_REGISTRY.registerMetaObject(data -> 0);

    // ==================== RNG Tracking for Determinism ====================

    /**
     * Seed used for combat RNG rolls in this damage event.
     * <p>
     * When set, allows replay of combat calculations for debugging.
     */
    public static final MetaKey<Long> RNG_SEED = 
            Damage.META_REGISTRY.registerMetaObject(data -> 0L);

    /**
     * Hit roll value (0-9999) used in hit resolution.
     */
    public static final MetaKey<Integer> HIT_ROLL = 
            Damage.META_REGISTRY.registerMetaObject(data -> -1);

    /**
     * Block roll value (0-9999) used in block resolution.
     */
    public static final MetaKey<Integer> BLOCK_ROLL = 
            Damage.META_REGISTRY.registerMetaObject(data -> -1);

    /**
     * Crit roll value (0-9999) used in crit resolution.
     */
    public static final MetaKey<Integer> CRIT_ROLL = 
            Damage.META_REGISTRY.registerMetaObject(data -> -1);

    // ==================== Special Mechanics ====================

    /**
     * Set when a double-damage roll triggers in {@code HyforgedDamageBonusSystem}.
     * Downstream systems (e.g., combat log) can use this flag to display the result.
     */
    public static final MetaKey<Boolean> DOUBLE_DAMAGE =
            Damage.META_REGISTRY.registerMetaObject(data -> Boolean.FALSE);

    /**
     * Per-element ailment damage scaling (BPS) stored by {@code HyforgedAilmentSystem}.
     * DoT systems can read this to scale ongoing ailment damage based on attacker stats.
     */
    public static final MetaKey<Integer> AILMENT_DAMAGE_BPS =
            Damage.META_REGISTRY.registerMetaObject(data -> 0);

    /**
     * Set when a dodge roll triggers in {@code HyforgedDodgeSystem}.
     * <p>
     * Distinct from {@link HyforgedHitResolutionSystem#MISS} (evasion-based miss).
     * Downstream systems (e.g., combat log) can use this flag to display "Dodge"
     * instead of "Miss" in the combat feed.
     */
    public static final MetaKey<Boolean> DODGE_ROLLED =
            Damage.META_REGISTRY.registerMetaObject(data -> Boolean.FALSE);

    /**
     * Amount of damage absorbed by Ward in this hit.
     * <p>
     * Set by {@code WardAbsorptionSystem}. Downstream systems (e.g., combat log)
     * can use this to show how much Ward mitigated.
     */
    public static final MetaKey<Float> WARD_ABSORBED =
            Damage.META_REGISTRY.registerMetaObject(data -> 0f);
}
