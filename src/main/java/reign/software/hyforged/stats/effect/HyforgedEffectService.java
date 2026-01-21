package reign.software.hyforged.stats.effect;

import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Service for effect duration scaling based on Hyforged stats.
 * <p>
 * This service provides utility methods to calculate scaled effect durations
 * based on Hyforged stats like `effect-duration-bps`.
 * <p>
 * <b>Usage:</b>
 * Before applying an effect with a custom duration, use this service to 
 * calculate the scaled duration based on the entity's stats.
 * <p>
 * <b>Duration Scaling Formula:</b>
 * <pre>
 * scaledDuration = baseDuration * (10000 + effectDurationBps) / 10000
 * </pre>
 * <p>
 * Example: If base duration is 100 ticks and effect-duration-bps is 2500 (25%),
 * then scaled duration = 100 * (10000 + 2500) / 10000 = 125 ticks.
 * <p>
 * <b>Negative Duration Bonus:</b>
 * The effect-duration-bps stat can be negative to reduce effect durations.
 * Minimum duration is 1 tick (effects cannot be completely negated).
 */
public class HyforgedEffectService {

    /**
     * Basis points for 100% (no scaling).
     */
    public static final int BASIS_100_PERCENT = 10000;

    /**
     * Minimum effect duration in ticks.
     */
    public static final int MIN_DURATION_TICKS = 1;

    /**
     * The stat ID for effect duration bonus in basis points.
     * 10000 = 100% (no change), 12500 = 125% (+25% duration), etc.
     */
    public static final StatId EFFECT_DURATION_STAT = StatId.hyforged("effect-duration-bps");

    // Private constructor - utility class
    private HyforgedEffectService() {}

    /**
     * Calculate the scaled duration for an effect.
     * <p>
     * Uses the entity's `effect-duration-bps` stat to scale the duration.
     * If the entity has no HyforgedStatComponent or the stat is not defined,
     * returns the base duration unchanged.
     *
     * @param statComponent The entity's stat component (may be null)
     * @param baseDuration The base duration in ticks
     * @return The scaled duration (minimum 1 tick)
     */
    public static int calculateScaledDuration(
            @Nullable HyforgedStatComponent statComponent,
            int baseDuration
    ) {
        if (statComponent == null || baseDuration <= 0) {
            return Math.max(baseDuration, MIN_DURATION_TICKS);
        }
        
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int statIndex = registry.getIndex(EFFECT_DURATION_STAT);
        
        if (statIndex < 0) {
            // Stat not defined, return base duration
            return baseDuration;
        }
        
        int effectDurationBps = statComponent.getCachedValue(statIndex);
        
        // Apply scaling: scaledDuration = baseDuration * (10000 + bonusBps) / 10000
        // This handles both positive and negative bonuses
        long scaled = (long) baseDuration * (BASIS_100_PERCENT + effectDurationBps) / BASIS_100_PERCENT;
        
        // Clamp to valid range
        return (int) Math.max(MIN_DURATION_TICKS, Math.min(scaled, Integer.MAX_VALUE));
    }

    /**
     * Calculate effective duration multiplier from basis points.
     * <p>
     * Useful for displaying the duration bonus in UI.
     *
     * @param effectDurationBps The effect duration bonus in basis points
     * @return The multiplier as a decimal (e.g., 1.25 for +25%)
     */
    public static double calculateDurationMultiplier(int effectDurationBps) {
        return (BASIS_100_PERCENT + effectDurationBps) / (double) BASIS_100_PERCENT;
    }

    /**
     * Format duration bonus for display.
     *
     * @param effectDurationBps The effect duration bonus in basis points
     * @return Formatted string (e.g., "+25%" or "-10%")
     */
    @Nonnull
    public static String formatDurationBonus(int effectDurationBps) {
        double percent = effectDurationBps / 100.0;
        if (effectDurationBps >= 0) {
            return String.format("+%.1f%%", percent);
        } else {
            return String.format("%.1f%%", percent);
        }
    }
}
