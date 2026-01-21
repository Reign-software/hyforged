package reign.software.hyforged.combat;

/**
 * Shared combat formulas and utilities.
 * <p>
 * All chance values are in basis points (bps), where 10000 = 100%.
 */
public class CombatMath {
    
    /** Basis points representing 100% */
    public static int BPS_100 = 10000;
    
    /** Base hit chance in basis points (100% base - reduced by evasion) */
    public static int BASE_HIT_CHANCE_BPS = 10000;
    
    /** Minimum hit chance in basis points (0%) */
    public static int MIN_HIT_CHANCE_BPS = 0;
    
    /** Maximum hit chance in basis points (100%) */
    public static int MAX_HIT_CHANCE_BPS = 10000;
    
    /** Level difference penalty per level in basis points (5% per level) */
    public static int LEVEL_PENALTY_PER_LEVEL_BPS = 500;
    
    private CombatMath() {}
    
    /**
     * Calculate the effective hit chance after accuracy vs evasion.
     * <p>
     * Formula: hitChance = BASE_HIT_CHANCE - effectiveEvasion + accuracyBonus
     * <p>
     * Level difference affects both sides:
     * <ul>
     *   <li>When defender is higher level, attacker's hit chance is penalized</li>
     *   <li>When attacker is higher level, defender's evasion is reduced</li>
     * </ul>
     * 
     * @param attackerAccuracyBps Attacker's accuracy stat in basis points
     * @param defenderEvasionBps Defender's evasion chance in basis points
     * @param attackerLevel Attacker's combat level
     * @param defenderLevel Defender's combat level
     * @return Hit chance in basis points, clamped to [MIN_HIT_CHANCE_BPS, MAX_HIT_CHANCE_BPS]
     */
    public static int calculateHitChance(
            int attackerAccuracyBps,
            int defenderEvasionBps,
            int attackerLevel,
            int defenderLevel
    ) {
        // Base hit chance is 100%
        int hitChance = BASE_HIT_CHANCE_BPS;
        
        // Calculate effective evasion (reduced when attacker is higher level)
        int effectiveEvasion = calculateEvasionChance(defenderEvasionBps, defenderLevel, attackerLevel);
        
        // Subtract defender's effective evasion chance
        hitChance -= effectiveEvasion;
        
        // Add attacker's accuracy (accuracy bonus reduces evasion effectiveness)
        hitChance += attackerAccuracyBps;
        
        // Apply level difference penalty when defender is higher level
        int levelDiff = defenderLevel - attackerLevel;
        if (levelDiff > 0) {
            // Higher level defenders are harder to hit
            // Each level difference reduces hit chance by LEVEL_PENALTY_PER_LEVEL_BPS
            int levelPenalty = levelDiff * LEVEL_PENALTY_PER_LEVEL_BPS;
            hitChance -= levelPenalty;
        }
        
        // Clamp to valid range
        return clamp(hitChance, MIN_HIT_CHANCE_BPS, MAX_HIT_CHANCE_BPS);
    }
    
    /**
     * Roll a chance-based check.
     * <p>
     * Uses CombatRandom for deterministic replay support.
     * 
     * @param chanceBps The chance in basis points (10000 = 100%)
     * @return true if the roll succeeded
     */
    public static boolean rollChance(int chanceBps) {
        return CombatRandom.rollChance(chanceBps);
    }
    
    /**
     * Roll a chance-based check with a provided random value.
     * <p>
     * Useful for deterministic testing.
     * 
     * @param chanceBps The chance in basis points (10000 = 100%)
     * @param randomRoll A random value in [0, 10000)
     * @return true if the roll succeeded
     */
    public static boolean rollChance(int chanceBps, int randomRoll) {
        if (chanceBps <= 0) return false;
        if (chanceBps >= BPS_100) return true;
        
        return randomRoll < chanceBps;
    }
    
    /**
     * Calculate crit chance with level difference penalty.
     * <p>
     * When attacking higher-level targets, crit chance is reduced.
     * 
     * @param baseCritChanceBps Base crit chance in basis points
     * @param attackerLevel Attacker's level
     * @param defenderLevel Defender's level
     * @return Effective crit chance in basis points
     */
    public static int calculateCritChance(
            int baseCritChanceBps,
            int attackerLevel,
            int defenderLevel
    ) {
        int levelDiff = defenderLevel - attackerLevel;
        if (levelDiff > 0) {
            // Reduce crit chance against higher level targets
            int penalty = levelDiff * LEVEL_PENALTY_PER_LEVEL_BPS;
            return Math.max(0, baseCritChanceBps - penalty);
        }
        return baseCritChanceBps;
    }
    
    /**
     * Calculate block chance with level difference consideration.
     * <p>
     * When defender is lower level than attacker, block chance is reduced.
     * This makes higher-level attackers harder to block.
     * 
     * @param baseBlockChanceBps Base block chance in basis points
     * @param defenderLevel Defender's level
     * @param attackerLevel Attacker's level
     * @return Effective block chance in basis points
     */
    public static int calculateBlockChance(int baseBlockChanceBps, int defenderLevel, int attackerLevel) {
        int levelDiff = attackerLevel - defenderLevel;
        if (levelDiff > 0) {
            // Attacker is higher level: reduce defender's block chance
            int penalty = levelDiff * LEVEL_PENALTY_PER_LEVEL_BPS;
            return Math.max(0, baseBlockChanceBps - penalty);
        }
        return Math.max(0, baseBlockChanceBps);
    }
    
    /**
     * Calculate block chance without level consideration.
     * <p>
     * @deprecated Use {@link #calculateBlockChance(int, int, int)} for level-aware calculation.
     * 
     * @param baseBlockChanceBps Base block chance in basis points
     * @return Effective block chance in basis points
     */
    @Deprecated
    public static int calculateBlockChance(int baseBlockChanceBps) {
        return Math.max(0, baseBlockChanceBps);
    }
    
    /**
     * Calculate evasion chance with level difference consideration.
     * <p>
     * When defender is lower level than attacker, evasion becomes less effective.
     * This is in addition to the accuracy penalty applied in hit chance calculation.
     * 
     * @param baseEvasionChanceBps Base evasion chance in basis points
     * @param defenderLevel Defender's level
     * @param attackerLevel Attacker's level
     * @return Effective evasion chance in basis points
     */
    public static int calculateEvasionChance(int baseEvasionChanceBps, int defenderLevel, int attackerLevel) {
        int levelDiff = attackerLevel - defenderLevel;
        if (levelDiff > 0) {
            // Attacker is higher level: reduce defender's evasion chance
            int penalty = levelDiff * LEVEL_PENALTY_PER_LEVEL_BPS;
            return Math.max(0, baseEvasionChanceBps - penalty);
        }
        return Math.max(0, baseEvasionChanceBps);
    }
    
    /**
     * Calculate effective resistance after penetration is applied.
     * <p>
     * Formula: effectiveResistance = max(0, resistance - penetration)
     * <p>
     * Penetration directly reduces resistance, but cannot make resistance go below 0.
     * 
     * @param resistanceBps The defender's resistance in basis points
     * @param penetrationBps The attacker's penetration in basis points
     * @return The effective resistance in basis points (never negative)
     */
    public static int calculateEffectiveResistance(int resistanceBps, int penetrationBps) {
        return Math.max(0, resistanceBps - penetrationBps);
    }
    
    /**
     * Apply a multiplier to damage.
     * 
     * @param baseDamage The base damage value
     * @param multiplierBps The multiplier in basis points (10000 = 1.0x, 15000 = 1.5x)
     * @return The multiplied damage
     */
    public static float applyMultiplier(float baseDamage, int multiplierBps) {
        return baseDamage * multiplierBps / BPS_100;
    }
    
    /**
     * Apply a percentage reduction to damage.
     * 
     * @param baseDamage The base damage value
     * @param reductionBps The reduction in basis points (5000 = 50% reduction)
     * @return The reduced damage
     */
    public static float applyReduction(float baseDamage, int reductionBps) {
        int effectiveMultiplier = BPS_100 - reductionBps;
        return baseDamage * effectiveMultiplier / BPS_100;
    }
    
    /**
     * Clamp a value between min and max.
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
