package reign.software.hyforged.stats.breakdown;

import reign.software.hyforged.stats.component.ModifierSource;
import reign.software.hyforged.stats.component.ModifierType;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * A single entry in a stat breakdown, representing one modifier's contribution.
 * <p>
 * This is pure data for UI display - no behavior.
 *
 * @param sourceId Unique identifier for the source (e.g., "equipment:iron_sword")
 * @param sourceType Category of the source (e.g., EQUIPMENT, BUFF, PASSIVE)
 * @param modifierType Type of modifier (FLAT, INCREASED, MORE, CAP)
 * @param value The modifier's value
 * @param displayName Human-readable name for the source
 */
public record BreakdownEntry(
    @Nonnull String sourceId,
    @Nonnull ModifierSource sourceType,
    @Nonnull ModifierType modifierType,
    int value,
    @Nonnull String displayName
) {
    
    public BreakdownEntry {
        Objects.requireNonNull(sourceId, "sourceId cannot be null");
        Objects.requireNonNull(sourceType, "sourceType cannot be null");
        Objects.requireNonNull(modifierType, "modifierType cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
    }
    
    /**
     * Get the formatted value string for UI display.
     * <p>
     * Examples:
     * - FLAT: "+15" or "-5"
     * - INCREASED: "+10.5%" or "-5.0%"
     * - MORE: "×1.10" (10% more)
     * - CAP: "≤500" (max cap) or "≥10" (min cap)
     */
    @Nonnull
    public String getFormattedValue() {
        return switch (modifierType) {
            case FLAT -> formatFlat(value);
            case INCREASED -> formatPercentBps(value);
            case MORE -> formatMultiplierBps(value);
            case CAP -> formatCap(value);
        };
    }
    
    private static String formatFlat(int value) {
        if (value >= 0) {
            return "+" + value;
        }
        return String.valueOf(value);
    }
    
    private static String formatPercentBps(int valueBps) {
        // Convert basis points to percentage (1000 bps = 100%)
        double percent = valueBps / 10.0;
        String sign = percent >= 0 ? "+" : "";
        return sign + String.format("%.1f%%", percent);
    }
    
    private static String formatMultiplierBps(int valueBps) {
        // Convert basis points to multiplier (1000 bps = 100% = ×1.0 more)
        double multiplier = 1.0 + (valueBps / 1000.0);
        return String.format("×%.2f", multiplier);
    }
    
    private static String formatCap(int value) {
        if (value >= 0) {
            return "≤" + value; // Max cap
        }
        return "≥" + (-value); // Min cap
    }
}
