package reign.software.hyforged.affix.model;

import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Template for generating affix tier definitions from a stat definition.
 * <p>
 * Allows stats to define default tier progressions that can be reused
 * across multiple affixes. Affix definitions may reference the stat's
 * template; explicit affix tiers override template values.
 * <p>
 * Loaded from stat definition JSON:
 * <pre>
 * {
 *   "id": "hyforged:strength",
 *   "affixTierTemplate": {
 *     "tierCount": 5,
 *     "t1Range": [8, 10],
 *     "t5Range": [1, 2],
 *     "scalingCurve": "linear"
 *   }
 * }
 * </pre>
 */
public record AffixTierTemplate(
        int tierCount,
        int t1Min,
        int t1Max,
        int t5Min,
        int t5Max,
        @Nonnull ScalingCurve scalingCurve
) {
    
    /**
     * Scaling curves for tier value interpolation.
     */
    public enum ScalingCurve {
        /** Linear interpolation between T1 and T5 */
        LINEAR,
        /** Exponential curve (faster falloff) */
        EXPONENTIAL,
        /** Logarithmic curve (slower falloff) */
        LOGARITHMIC
    }
    
    /**
     * Default template with 5 tiers and linear scaling.
     */
    public static final AffixTierTemplate DEFAULT = new AffixTierTemplate(
            5, 10, 15, 1, 3, ScalingCurve.LINEAR
    );
    
    public AffixTierTemplate {
        if (tierCount < 1 || tierCount > 10) {
            throw new IllegalArgumentException("tierCount must be between 1 and 10");
        }
        if (t1Min > t1Max) {
            throw new IllegalArgumentException("t1Min cannot be greater than t1Max");
        }
        if (t5Min > t5Max) {
            throw new IllegalArgumentException("t5Min cannot be greater than t5Max");
        }
        Objects.requireNonNull(scalingCurve, "scalingCurve cannot be null");
    }
    
    /**
     * Create a template from T1 and T5 ranges with linear scaling.
     */
    public static AffixTierTemplate linear(int tierCount, int t1Min, int t1Max, int t5Min, int t5Max) {
        return new AffixTierTemplate(tierCount, t1Min, t1Max, t5Min, t5Max, ScalingCurve.LINEAR);
    }
    
    /**
     * Generate tier definitions from this template.
     *
     * @param baseItemLevel The minimum item level for tier 1
     * @param itemLevelStep The item level increase per tier
     * @param statId The stat ID to use for the generated tiers
     * @param stackType The stack type for the stat modifier
     * @return Array of tier definitions
     */
    public AffixTierDefinition[] generateTiers(int baseItemLevel, int itemLevelStep, 
                                                @Nonnull String statId, 
                                                @Nonnull HyforgedModifier.StackType stackType) {
        Objects.requireNonNull(statId, "statId cannot be null");
        Objects.requireNonNull(stackType, "stackType cannot be null");
        
        AffixTierDefinition[] tiers = new AffixTierDefinition[tierCount];
        
        for (int i = 0; i < tierCount; i++) {
            int tier = i + 1;
            int itemLevelReq = baseItemLevel + (i * itemLevelStep);
            
            // Calculate min/max values based on interpolation
            double progress = tierCount > 1 ? (double) i / (tierCount - 1) : 0;
            int[] range = interpolateRange(progress);
            
            // Create stats map with single stat
            Map<String, AffixTierStat> stats = new HashMap<>();
            stats.put(statId, new AffixTierStat(StatId.parse(statId), stackType, range[0], range[1]));
            
            tiers[i] = new AffixTierDefinition(tier, itemLevelReq, AffixTierDefinition.DEFAULT_WEIGHT, stats);
        }
        
        return tiers;
    }
    
    /**
     * Interpolate the min/max range for a given progress (0.0 = T1, 1.0 = T5).
     */
    private int[] interpolateRange(double progress) {
        double curvedProgress = applyCurve(progress);
        
        // Interpolate from T1 (progress=0) to T5 (progress=1)
        int min = (int) Math.round(t1Min + (t5Min - t1Min) * curvedProgress);
        int max = (int) Math.round(t1Max + (t5Max - t1Max) * curvedProgress);
        
        // Ensure min <= max
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }
        
        return new int[]{min, max};
    }
    
    /**
     * Apply the scaling curve to the linear progress.
     */
    private double applyCurve(double progress) {
        return switch (scalingCurve) {
            case LINEAR -> progress;
            case EXPONENTIAL -> progress * progress;
            case LOGARITHMIC -> Math.sqrt(progress);
        };
    }
    
    /**
     * Get the min value for a specific tier.
     *
     * @param tier The tier number (1-based)
     * @return The minimum value for that tier
     */
    public int getMinForTier(int tier) {
        if (tier < 1 || tier > tierCount) {
            throw new IllegalArgumentException("tier must be between 1 and " + tierCount);
        }
        double progress = tierCount > 1 ? (double) (tier - 1) / (tierCount - 1) : 0;
        return interpolateRange(progress)[0];
    }
    
    /**
     * Get the max value for a specific tier.
     *
     * @param tier The tier number (1-based)
     * @return The maximum value for that tier
     */
    public int getMaxForTier(int tier) {
        if (tier < 1 || tier > tierCount) {
            throw new IllegalArgumentException("tier must be between 1 and " + tierCount);
        }
        double progress = tierCount > 1 ? (double) (tier - 1) / (tierCount - 1) : 0;
        return interpolateRange(progress)[1];
    }
}
