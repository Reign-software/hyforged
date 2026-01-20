package reign.software.hyforged.stats.scaling;

import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;

/**
 * Sealed interface for scaling rules that compute stat base values from other stats.
 * <p>
 * A stat can have multiple scaling rules; their contributions are summed.
 * Scaling uses the final (post-modifier) values of source stats.
 * <p>
 * Implementations:
 * <ul>
 *   <li>{@link LinearScaling} - Simple ratio-based scaling (e.g., 1 STR = 2 Attack Power)</li>
 *   <li>{@link ThresholdScaling} - Step-based scaling (e.g., every 5 LCK = 1% crit)</li>
 *   <li>{@link DiminishingScaling} - Rating-to-effectiveness with cap (e.g., crit rating)</li>
 * </ul>
 */
public sealed interface ScalingRule permits LinearScaling, ThresholdScaling, DiminishingScaling {
    
    /**
     * Get the source stat that this scaling rule reads from.
     * 
     * @return The source stat ID
     */
    @Nonnull
    StatId source();
    
    /**
     * Get the type name of this scaling rule for serialization.
     * 
     * @return The type name (e.g., "linear", "threshold", "diminishing")
     */
    @Nonnull
    String type();
}
