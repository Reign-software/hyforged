package reign.software.hyforged.stats.scaling;

import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Threshold scaling rule: contribution = floor(sourceValue / perPoints) * bonusBps
 * <p>
 * Example: Every 5 Luck = 100 bps (1%) crit chance → perPoints = 5, bonusBps = 100
 * <p>
 * This creates step-based scaling where you need a minimum amount of the source
 * stat before gaining any benefit, and benefits increase in discrete steps.
 * 
 * @param source The source stat to read from
 * @param perPoints Number of source stat points required for each bonus step
 * @param bonusBps Bonus in basis points (1/100th of a percent) per step
 */
public record ThresholdScaling(
    @Nonnull StatId source,
    int perPoints,
    int bonusBps
) implements ScalingRule {
    
    public static final String TYPE = "threshold";
    
    public ThresholdScaling {
        Objects.requireNonNull(source, "source cannot be null");
        if (perPoints <= 0) {
            throw new IllegalArgumentException("perPoints must be positive");
        }
        // bonusBps can be negative (for penalties) or zero (no-op, but valid)
    }
    
    @Override
    @Nonnull
    public String type() {
        return TYPE;
    }
    
    /**
     * Compute the contribution from this scaling rule.
     * 
     * @param sourceValue The final value of the source stat
     * @return The contribution to add to the target stat's base (in basis points)
     */
    public int computeContribution(int sourceValue) {
        if (sourceValue < 0) {
            // Negative source values floor towards negative infinity
            return ((sourceValue - perPoints + 1) / perPoints) * bonusBps;
        }
        return (sourceValue / perPoints) * bonusBps;
    }
    
    @Override
    public String toString() {
        return "ThresholdScaling[source=" + source.fullId() + 
               ", perPoints=" + perPoints + 
               ", bonusBps=" + bonusBps + "]";
    }
}
