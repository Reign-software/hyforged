package reign.software.hyforged.stats;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Display format for stat values in the UI.
 * <p>
 * This is pure data - no behavior, following ECS principles.
 * Loaded from the {@code "DisplayFormat"} field in stat JSON definitions.
 */
public enum DisplayFormat {
    /** Plain integer (e.g., "150") */
    INTEGER,
    
    /** Basis points displayed as percent (e.g., 1500 bps → "15.0%") */
    PERCENT_BPS,
    
    /** Flat percentage — raw value IS the percent (e.g., 100 → "100%") */
    PERCENT,
    
    /** Rating value with effectiveness preview (e.g., "500 rating") */
    RATING,
    
    /** Flat bonus with sign (e.g., "+50", "-10") */
    FLAT_BONUS,
    
    /** Multiplier in basis points (e.g., 15000 → "1.50x") */
    MULTIPLIER;
    
    /**
     * Parse a DisplayFormat from a JSON string value.
     * Case-insensitive. Returns null if not recognized.
     *
     * @param value The string value from JSON (e.g., "PERCENT_BPS", "RATING")
     * @return The matching DisplayFormat, or null if unrecognized
     */
    @Nullable
    public static DisplayFormat fromString(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
