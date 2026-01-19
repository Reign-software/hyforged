package reign.software.hyforged.stats.breakdown;

import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * A scaling contribution entry in a stat breakdown.
 * <p>
 * Represents the contribution from a source stat via a scaling rule.
 * For example, "+40 from Strength" for Attack Power.
 * <p>
 * This is pure data for UI display - no behavior.
 *
 * @param sourceStatId The source stat that this scaling reads from
 * @param sourceDisplayName Human-readable name of the source stat
 * @param contribution The computed contribution value
 * @param ruleType Type of scaling rule ("linear", "threshold", "diminishing")
 */
public record ScalingContribution(
    @Nonnull StatId sourceStatId,
    @Nonnull String sourceDisplayName,
    int contribution,
    @Nonnull String ruleType
) {
    
    public ScalingContribution {
        Objects.requireNonNull(sourceStatId, "sourceStatId cannot be null");
        Objects.requireNonNull(sourceDisplayName, "sourceDisplayName cannot be null");
        Objects.requireNonNull(ruleType, "ruleType cannot be null");
    }
    
    /**
     * Get the formatted value string for UI display.
     * <p>
     * Examples:
     * - "+40 from Strength"
     * - "+200 from Luck"
     */
    @Nonnull
    public String getFormattedValue() {
        String sign = contribution >= 0 ? "+" : "";
        return sign + contribution + " from " + sourceDisplayName;
    }
    
    /**
     * Get just the contribution amount formatted with sign.
     */
    @Nonnull
    public String getFormattedContribution() {
        if (contribution >= 0) {
            return "+" + contribution;
        }
        return String.valueOf(contribution);
    }
    
    @Override
    public String toString() {
        return "ScalingContribution[" + getFormattedValue() + " (" + ruleType + ")]";
    }
}
