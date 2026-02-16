package reign.software.hyforged.stats.engine;

import reign.software.hyforged.stats.DisplayFormat;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;

/**
 * Utility class for converting stat ratings to effectiveness percentages.
 * <p>
 * Uses PoE-style diminishing returns formula:
 * {@code den = abs(rating) + k * targetLevel}
 * {@code effectivenessBps = sign(rating) * (abs(rating) * 10000 / den)}
 * <p>
 * This provides diminishing returns as rating increases, and
 * the effectiveness decreases against higher level targets.
 * <p>
 * All results are in basis points (10000 = 100%) with integer math.
 */
public final class RatingConverter {
    
    private RatingConverter() {} // Static utility class

    /** Basis points representing 100% (10000 bps = 100%) */
    public static final int BPS_100_PERCENT = 10000;
    
    /**
     * Default k constant for armor rating calculations.
     * Higher values mean more diminishing returns.
     */
    public static final int K_ARMOR = 10;
    
    /**
     * Default k constant for evasion rating calculations.
     */
    public static final int K_EVASION = 10;
    
    /**
     * Default k constant for resistance rating calculations.
     */
    public static final int K_RESISTANCE = 10;
    
    /**
     * Default k constant for accuracy rating calculations.
     */
    public static final int K_ACCURACY = 10;
    
    /**
     * Minimum effectiveness value in basis points (can go negative for debuffs).
    * -7500 = -75%
     */
    public static final int MIN_EFFECTIVENESS_BPS = -7500;
    
    /**
     * Maximum effectiveness value in basis points.
    * 7500 = 75% (prevents full immunity)
     */
    public static final int MAX_EFFECTIVENESS_BPS = 7500;
    
    /**
     * Maximum hit chance from accuracy in basis points.
    * 9500 = 95% (always 5% chance to miss)
     */
    public static final int MAX_HIT_CHANCE_BPS = 9500;
    
    /**
     * Minimum hit chance from accuracy in basis points.
    * 500 = 5% (always 5% chance to hit)
     */
    public static final int MIN_HIT_CHANCE_BPS = 500;
    
    /**
     * Convert a rating value to effectiveness in basis points.
     * <p>
    * Formula: effectivenessBps = sign(rating) * (abs(rating) * 10000 / den)
     * where: den = abs(rating) + k * targetLevel
     * <p>
     * Positive ratings give damage reduction (for defense) or increased chance (for offense).
     * Negative ratings have the opposite effect.
     *
     * @param rating The raw rating value (can be positive or negative)
     * @param targetLevel The level of the target (attacker for defense, defender for offense)
     * @param k The diminishing returns constant
    * @return Effectiveness in basis points (10000 = 100%)
     */
    public static int toEffectiveness(int rating, int targetLevel, int k) {
        if (rating == 0) {
            return 0;
        }
        
        // Ensure positive target level
        int level = Math.max(1, targetLevel);
        
        // Calculate absolute values for the formula
        long absRating = Math.abs((long) rating);
        long denominator = absRating + (long) k * level;
        
        // Prevent division by zero (shouldn't happen with k > 0)
        if (denominator == 0) {
            return 0;
        }
        
        // Calculate effectiveness: abs(rating) * 10000 / denominator
        long effectivenessAbs = (absRating * BPS_100_PERCENT) / denominator;
        
        // Apply sign
        int effectiveness = (int) (rating > 0 ? effectivenessAbs : -effectivenessAbs);
        
        return effectiveness;
    }
    
    /**
     * Convert armor rating to physical damage reduction in basis points.
     * 
     * @param armorRating The armor rating value
     * @param attackerLevel The level of the attacker
     * @return Physical damage reduction in basis points, clamped to bounds
     */
    public static int armorToReduction(int armorRating, int attackerLevel) {
        int effectiveness = toEffectiveness(armorRating, attackerLevel, K_ARMOR);
        return clampEffectiveness(effectiveness);
    }
    
    /**
     * Convert evasion rating to dodge/evade chance in basis points.
     * 
     * @param evasionRating The evasion rating value
     * @param attackerLevel The level of the attacker
     * @return Evasion chance in basis points, clamped to bounds
     */
    public static int evasionToChance(int evasionRating, int attackerLevel) {
        int effectiveness = toEffectiveness(evasionRating, attackerLevel, K_EVASION);
        return clampEffectiveness(effectiveness);
    }
    
    /**
     * Convert resistance rating to elemental damage reduction in basis points.
     * 
     * @param resistRating The resistance rating value
     * @param attackerLevel The level of the attacker
     * @return Elemental damage reduction in basis points, clamped to bounds
     */
    public static int resistanceToReduction(int resistRating, int attackerLevel) {
        int effectiveness = toEffectiveness(resistRating, attackerLevel, K_RESISTANCE);
        return clampEffectiveness(effectiveness);
    }
    
    /**
     * Convert accuracy rating to hit chance in basis points.
     * <p>
     * Uses different clamping bounds to ensure minimum/maximum hit chance.
     * 
     * @param accuracyRating The accuracy rating value
     * @param defenderLevel The level of the defender
     * @return Hit chance in basis points, clamped to hit chance bounds
     */
    public static int accuracyToHitChance(int accuracyRating, int defenderLevel) {
        int effectiveness = toEffectiveness(accuracyRating, defenderLevel, K_ACCURACY);
        // Accuracy translates to hit chance: 500 base + effectiveness
        // 5000 bps = 50% base hit chance
        int hitChance = 5000 + effectiveness;
        return clamp(hitChance, MIN_HIT_CHANCE_BPS, MAX_HIT_CHANCE_BPS);
    }
    
    /**
     * Get the effectiveness for a stat rating, looking up the appropriate k constant.
     * 
     * @param statId The stat ID
     * @param rating The rating value
     * @param targetLevel The target's level
     * @return Effectiveness in basis points
     */
    public static int getEffectivenessForStat(@Nonnull StatId statId, int rating, int targetLevel) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        StatDefinition statDef = registry.getStat(statId);
        
        if (statDef == null || statDef.displayFormat() != DisplayFormat.RATING) {
            // Not a rating stat, return raw value (or 0)
            return rating;
        }
        
        // Use the data-driven k constant from the stat definition
        int k = statDef.ratingK();
        
        return toEffectiveness(rating, targetLevel, k);
    }
    
    /**
     * Clamp effectiveness to standard bounds.
     */
    public static int clampEffectiveness(int effectiveness) {
        return clamp(effectiveness, MIN_EFFECTIVENESS_BPS, MAX_EFFECTIVENESS_BPS);
    }
    
    /**
     * Clamp value to bounds.
     */
    public static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
    
    /**
     * Apply damage reduction from effectiveness to damage value.
     * <p>
    * Formula: finalDamage = damage * (10000 - reductionBps) / 10000
     * 
     * @param damage The raw damage value
    * @param reductionBps The damage reduction in basis points (0-10000)
     * @return The reduced damage value
     */
    public static int applyReduction(int damage, int reductionBps) {
        if (damage <= 0) return 0;
        if (reductionBps <= 0) return damage;
        if (reductionBps >= BPS_100_PERCENT) return 0;
        
        return (int) (((long) damage * (BPS_100_PERCENT - reductionBps)) / BPS_100_PERCENT);
    }
    
    /**
     * Check if a random roll hits based on hit chance.
     * <p>
     * Note: This is a pure calculation, the caller provides the random value.
     * 
    * @param hitChanceBps The hit chance in basis points
    * @param roll A random value 0-9999 (inclusive)
     * @return true if the roll is a hit
     */
    public static boolean isHit(int hitChanceBps, int roll) {
        return roll < hitChanceBps;
    }
}
