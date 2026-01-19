package reign.software.hyforged.stats.scaling;

import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Linear scaling rule: contribution = sourceValue * ratio
 * <p>
 * Example: 1 Strength = 2 Attack Power → ratio = 2.0
 * <p>
 * The ratio can be fractional. The final contribution is truncated to an integer.
 * 
 * @param source The source stat to read from
 * @param ratio The multiplier applied to the source stat value
 */
public record LinearScaling(
    @Nonnull StatId source,
    double ratio
) implements ScalingRule {
    
    public static final String TYPE = "linear";
    
    public LinearScaling {
        Objects.requireNonNull(source, "source cannot be null");
        if (!Double.isFinite(ratio)) {
            throw new IllegalArgumentException("ratio must be a finite number");
        }
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
     * @return The contribution to add to the target stat's base
     */
    public int computeContribution(int sourceValue) {
        return (int) (sourceValue * ratio);
    }
    
    @Override
    public String toString() {
        return "LinearScaling[source=" + source.fullId() + ", ratio=" + ratio + "]";
    }
}
