package reign.software.hyforged.stats;

/**
 * Display format for stat values in the UI.
 * <p>
 * This is pure data - no behavior, following ECS principles.
 */
public enum DisplayFormat {
    /** Plain integer (e.g., "150") */
    INTEGER,
    
    /** Basis points displayed as percent (e.g., 150 bps → "15.0%") */
    PERCENT_BPS,
    
    /** Rating value with effectiveness preview (e.g., "500 (25% vs L50)") */
    RATING,
    
    /** Flat bonus with sign (e.g., "+50", "-10") */
    FLAT_BONUS,
    
    /** Multiplier displayed as percent (e.g., 1500 → "150%") */
    MULTIPLIER
}
