package reign.software.hyforged.stats.condition;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Condition that checks health thresholds.
 * <p>
 * Examples:
 * - "when health below 50%"
 * - "when health at or above 90%"
 * - "on low life" (typically below 35%)
 */
public class HealthThresholdCondition implements ModifierCondition {

    /** Low life threshold (35%) in basis points */
    public static final int LOW_LIFE_THRESHOLD_BPS = 3500;
    
    /** Full life threshold (90%) in basis points */
    public static final int FULL_LIFE_THRESHOLD_BPS = 9000;

    private final int thresholdBps;
    private final boolean checkBelow;

    /**
     * Create a health threshold condition.
     *
     * @param thresholdBps The threshold in basis points (10000 = 100%)
     * @param checkBelow If true, condition passes when health is below threshold;
     *                   if false, condition passes when health is at or above threshold
     */
    public HealthThresholdCondition(int thresholdBps, boolean checkBelow) {
        this.thresholdBps = Math.max(0, Math.min(10000, thresholdBps));
        this.checkBelow = checkBelow;
    }

    /**
     * Create a condition for "when health below X%".
     *
     * @param percentBps The percentage threshold in basis points
     * @return A new HealthThresholdCondition
     */
    public static HealthThresholdCondition below(int percentBps) {
        return new HealthThresholdCondition(percentBps, true);
    }

    /**
     * Create a condition for "when health at or above X%".
     *
     * @param percentBps The percentage threshold in basis points
     * @return A new HealthThresholdCondition
     */
    public static HealthThresholdCondition atOrAbove(int percentBps) {
        return new HealthThresholdCondition(percentBps, false);
    }

    /**
     * Create a "low life" condition (below 35% health).
     *
     * @return A new HealthThresholdCondition for low life
     */
    public static HealthThresholdCondition lowLife() {
        return below(LOW_LIFE_THRESHOLD_BPS);
    }

    /**
     * Create a "full life" condition (at or above 90% health).
     *
     * @return A new HealthThresholdCondition for full life
     */
    public static HealthThresholdCondition fullLife() {
        return atOrAbove(FULL_LIFE_THRESHOLD_BPS);
    }

    @Override
    public boolean evaluate(@Nonnull Ref<EntityStore> entityRef, @Nonnull QueryContext context) {
        if (checkBelow) {
            return context.isHealthBelow(thresholdBps);
        } else {
            return context.isHealthAtOrAbove(thresholdBps);
        }
    }

    public int getThresholdBps() {
        return thresholdBps;
    }

    public boolean isCheckBelow() {
        return checkBelow;
    }

    @Override
    public String toString() {
        double percent = thresholdBps / 100.0;
        return String.format("HealthThresholdCondition[health %s %.1f%%]",
            checkBelow ? "below" : "at or above",
            percent);
    }
}
