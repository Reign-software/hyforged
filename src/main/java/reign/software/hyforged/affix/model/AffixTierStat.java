package reign.software.hyforged.affix.model;

import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Defines a single stat modifier for a specific affix tier.
 * <p>
 * Each tier can grant multiple stats, and each stat has its own value range
 * and stack type. For example, a T1 "of the Titan" suffix might grant:
 * <ul>
 *   <li>+45-55 Strength (FLAT)</li>
 *   <li>+100-150 Max Health (FLAT)</li>
 * </ul>
 * <p>
 * This is pure immutable data following ECS principles.
 *
 * @param statId The stat this modifier affects
 * @param stackType How this modifier stacks (FLAT, INCREASED, MORE)
 * @param minValue Minimum rolled value (inclusive)
 * @param maxValue Maximum rolled value (inclusive)
 */
public record AffixTierStat(
    @Nonnull StatId statId,
    @Nonnull HyforgedModifier.StackType stackType,
    int minValue,
    int maxValue
) {
    
    public AffixTierStat {
        Objects.requireNonNull(statId, "statId cannot be null");
        Objects.requireNonNull(stackType, "stackType cannot be null");
        
        if (minValue > maxValue) {
            throw new IllegalArgumentException(
                "minValue (" + minValue + ") cannot be greater than maxValue (" + maxValue + ")");
        }
    }
    
    /**
     * Create a tier stat with a fixed value (min = max).
     */
    public AffixTierStat(@Nonnull StatId statId, @Nonnull HyforgedModifier.StackType stackType, int value) {
        this(statId, stackType, value, value);
    }
    
    /**
     * Roll a random value within this stat's range.
     *
     * @param randomFraction A random value in [0.0, 1.0)
     * @return A value between minValue and maxValue (inclusive)
     */
    public int rollValue(double randomFraction) {
        if (minValue == maxValue) {
            return minValue;
        }
        int range = maxValue - minValue + 1;
        return minValue + (int) (randomFraction * range);
    }
    
    /**
     * Get the midpoint value of this stat's range.
     * Useful for display or comparison purposes.
     */
    public int getMidValue() {
        return (minValue + maxValue) / 2;
    }
}
