package reign.software.hyforged.stats.scaling;

import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Diminishing returns scaling rule using a rating-to-effectiveness formula.
 * <p>
 * This uses the {@link reign.software.hyforged.stats.engine.RatingConverter} to
 * convert a rating value to an effectiveness percentage (in basis points),
 * with a configurable cap.
 * <p>
 * Example: Crit rating → crit chance with 75% cap (7500 bps)
 * 
 * @param source The source stat (typically a "rating" stat) to read from
 * @param curve The curve identifier for the RatingConverter
 * @param scale A multiplier applied to the rating before conversion
 * @param capBps Maximum contribution in basis points (e.g., 7500 = 75%)
 */
public record DiminishingScaling(
    @Nonnull StatId source,
    @Nonnull String curve,
    double scale,
    int capBps
) implements ScalingRule {
    
    public static final String TYPE = "diminishing";
    
    /**
     * Default curve identifier for standard diminishing returns.
     */
    public static final String DEFAULT_CURVE = "rating";
    
    public DiminishingScaling {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(curve, "curve cannot be null");
        if (curve.isEmpty()) {
            throw new IllegalArgumentException("curve cannot be empty");
        }
        if (!Double.isFinite(scale)) {
            throw new IllegalArgumentException("scale must be a finite number");
        }
        if (scale <= 0) {
            throw new IllegalArgumentException("scale must be positive");
        }
        if (capBps < 0) {
            throw new IllegalArgumentException("capBps cannot be negative");
        }
    }
    
    /**
     * Create a DiminishingScaling with the default curve.
     */
    public static DiminishingScaling withDefaultCurve(StatId source, double scale, int capBps) {
        return new DiminishingScaling(source, DEFAULT_CURVE, scale, capBps);
    }
    
    @Override
    @Nonnull
    public String type() {
        return TYPE;
    }
    
    /**
     * Compute the contribution from this scaling rule.
     * <p>
     * Note: This method requires the RatingConverter for the actual computation.
     * The contribution is capped at {@link #capBps()}.
     * 
     * @param sourceValue The final value of the source stat (rating)
     * @param convertedBps The value after passing through RatingConverter
     * @return The contribution to add to the target stat's base (in basis points), capped
     */
    public int computeContribution(int convertedBps) {
        return Math.min(convertedBps, capBps);
    }
    
    /**
     * Get the scaled rating value to pass to the RatingConverter.
     * 
     * @param sourceValue The raw source stat value
     * @return The scaled value for conversion
     */
    public double getScaledRating(int sourceValue) {
        return sourceValue * scale;
    }
    
    @Override
    public String toString() {
        return "DiminishingScaling[source=" + source.fullId() + 
               ", curve=" + curve + 
               ", scale=" + scale + 
               ", capBps=" + capBps + "]";
    }
}
