package reign.software.hyforged.stats.engine;

import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.scaling.DiminishingScaling;
import reign.software.hyforged.stats.scaling.LinearScaling;
import reign.software.hyforged.stats.scaling.ScalingRule;
import reign.software.hyforged.stats.scaling.ThresholdScaling;

import javax.annotation.Nonnull;
import java.util.function.IntUnaryOperator;

/**
 * Utility class for computing stat base values from scaling rules.
 * <p>
 * This engine computes the base value for stats that derive their value from
 * other stats (e.g., Attack Power scales from Strength). It handles three
 * scaling types:
 * <ul>
 *   <li><b>Linear</b>: Simple ratio-based scaling (e.g., 1 STR = 2 Attack Power)</li>
 *   <li><b>Threshold</b>: Step-based scaling (e.g., every 5 LCK = 100 bps crit)</li>
 *   <li><b>Diminishing</b>: Rating-to-effectiveness with cap (e.g., crit rating)</li>
 * </ul>
 * <p>
 * Scaling contributions are summed when multiple rules are defined.
 * The computed base value then has modifiers applied via {@link StackingEngine}.
 */
public final class ScalingEngine {
    
    private ScalingEngine() {} // Static utility class
    
    /**
     * Default k constant for diminishing returns curves when not specified.
     */
    public static final int DEFAULT_DIMINISHING_K = 10;
    
    /**
     * Default target level for diminishing returns calculations.
     */
    public static final int DEFAULT_TARGET_LEVEL = 1;
    
    /**
     * Compute the scaled base value for a stat definition.
     * <p>
     * This sums the contributions from all scaling rules defined on the stat.
     * If the stat has no scaling rules, returns 0 (the caller should use
     * defaultValue or a stored base value instead).
     *
     * @param statDef The stat definition containing scaling rules
     * @param sourceValueProvider Function that returns the final value of a source stat by index
     * @param registry The stat definition registry for looking up source stat indices
     * @return The computed base value (sum of all scaling contributions)
     */
    public static int computeScaledBase(
            @Nonnull StatDefinition statDef,
            @Nonnull IntUnaryOperator sourceValueProvider,
            @Nonnull StatDefinitionRegistry registry
    ) {
        if (!statDef.hasScaling()) {
            return 0;
        }
        
        int total = 0;
        for (ScalingRule rule : statDef.scaling()) {
            int sourceIndex = registry.getIndex(rule.source());
            if (sourceIndex < 0) {
                // Source stat not found - skip this rule (should have been caught at registration)
                continue;
            }
            int sourceValue = sourceValueProvider.applyAsInt(sourceIndex);
            total += computeContribution(rule, sourceValue);
        }
        return total;
    }
    
    /**
     * Compute the scaled base value for a stat definition using source stat IDs directly.
     * <p>
     * This variant looks up source stats by their full ID string.
     *
     * @param statDef The stat definition containing scaling rules
     * @param sourceValueByIdProvider Function that returns the final value of a source stat by full ID
     * @return The computed base value (sum of all scaling contributions)
     */
    public static int computeScaledBaseById(
            @Nonnull StatDefinition statDef,
            @Nonnull java.util.function.ToIntFunction<String> sourceValueByIdProvider
    ) {
        if (!statDef.hasScaling()) {
            return 0;
        }
        
        int total = 0;
        for (ScalingRule rule : statDef.scaling()) {
            String sourceFullId = rule.source().fullId();
            int sourceValue = sourceValueByIdProvider.applyAsInt(sourceFullId);
            total += computeContribution(rule, sourceValue);
        }
        return total;
    }
    
    /**
     * Compute the contribution from a single scaling rule.
     *
     * @param rule The scaling rule
     * @param sourceValue The final value of the source stat
     * @return The contribution to add to the target stat's base
     */
    public static int computeContribution(@Nonnull ScalingRule rule, int sourceValue) {
        return switch (rule) {
            case LinearScaling linear -> computeLinearContribution(linear, sourceValue);
            case ThresholdScaling threshold -> computeThresholdContribution(threshold, sourceValue);
            case DiminishingScaling diminishing -> computeDiminishingContribution(diminishing, sourceValue);
        };
    }
    
    /**
     * Compute contribution for linear scaling.
     * <p>
     * Formula: contribution = (int)(sourceValue * ratio)
     *
     * @param rule The linear scaling rule
     * @param sourceValue The source stat value
     * @return The contribution
     */
    public static int computeLinearContribution(@Nonnull LinearScaling rule, int sourceValue) {
        return rule.computeContribution(sourceValue);
    }
    
    /**
     * Compute contribution for threshold scaling.
     * <p>
     * Formula: contribution = floor(sourceValue / perPoints) * bonusBps
     *
     * @param rule The threshold scaling rule
     * @param sourceValue The source stat value
     * @return The contribution (typically in basis points)
     */
    public static int computeThresholdContribution(@Nonnull ThresholdScaling rule, int sourceValue) {
        return rule.computeContribution(sourceValue);
    }
    
    /**
     * Compute contribution for diminishing returns scaling.
     * <p>
     * Uses the {@link RatingConverter} to convert the scaled rating to effectiveness,
     * then caps the result.
     *
     * @param rule The diminishing scaling rule
     * @param sourceValue The source stat value (rating)
     * @return The contribution (in basis points), capped at capBps
     */
    public static int computeDiminishingContribution(@Nonnull DiminishingScaling rule, int sourceValue) {
        return computeDiminishingContribution(rule, sourceValue, DEFAULT_TARGET_LEVEL);
    }
    
    /**
     * Compute contribution for diminishing returns scaling with a specific target level.
     *
     * @param rule The diminishing scaling rule
     * @param sourceValue The source stat value (rating)
     * @param targetLevel The target level for diminishing returns calculation
     * @return The contribution (in basis points), capped at capBps
     */
    public static int computeDiminishingContribution(
            @Nonnull DiminishingScaling rule, 
            int sourceValue, 
            int targetLevel
    ) {
        if (sourceValue == 0) {
            return 0;
        }
        
        // Scale the rating
        double scaledRating = rule.getScaledRating(sourceValue);
        int rating = (int) scaledRating;
        
        // Get k constant based on curve name
        int k = getKConstantForCurve(rule.curve());
        
        // Convert rating to effectiveness using RatingConverter
        int effectivenessBps = RatingConverter.toEffectiveness(rating, targetLevel, k);
        
        // Apply the cap
        return rule.computeContribution(effectivenessBps);
    }
    
    /**
     * Get the k constant for a named diminishing returns curve.
     *
     * @param curve The curve name
     * @return The k constant to use
     */
    public static int getKConstantForCurve(@Nonnull String curve) {
        return switch (curve.toLowerCase()) {
            case "armor" -> RatingConverter.K_ARMOR;
            case "evasion" -> RatingConverter.K_EVASION;
            case "resistance" -> RatingConverter.K_RESISTANCE;
            case "accuracy" -> RatingConverter.K_ACCURACY;
            case "rating" -> DEFAULT_DIMINISHING_K;
            default -> DEFAULT_DIMINISHING_K;
        };
    }
}
