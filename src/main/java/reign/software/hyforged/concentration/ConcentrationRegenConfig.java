package reign.software.hyforged.concentration;

import javax.annotation.Nonnull;

/**
 * Configuration for concentration regeneration.
 */
public final class ConcentrationRegenConfig {

    private static final ConcentrationRegenConfig INSTANCE = new ConcentrationRegenConfig();

    private float wisdomScalingFactor = 0.5f;
    private float updateIntervalSeconds = 0.2f;

    private ConcentrationRegenConfig() {
    }

    @Nonnull
    public static ConcentrationRegenConfig get() {
        return INSTANCE;
    }

    public float getWisdomScalingFactor() {
        return wisdomScalingFactor;
    }

    public float getUpdateIntervalSeconds() {
        return updateIntervalSeconds;
    }

    public void setWisdomScalingFactor(float wisdomScalingFactor) {
        this.wisdomScalingFactor = wisdomScalingFactor;
    }

    public void setUpdateIntervalSeconds(float updateIntervalSeconds) {
        this.updateIntervalSeconds = updateIntervalSeconds;
    }
}
