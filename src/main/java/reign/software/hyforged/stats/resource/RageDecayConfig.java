package reign.software.hyforged.stats.resource;

import javax.annotation.Nonnull;

public class RageDecayConfig {

    private static final RageDecayConfig INSTANCE = new RageDecayConfig();

    private float outOfCombatDelaySeconds = 4.0f;
    private float decayPerSecond = 7.0f;

    private RageDecayConfig() {
    }

    @Nonnull
    public static RageDecayConfig get() {
        return INSTANCE;
    }

    public void applyFromAsset(@Nonnull RageDecayConfigAsset asset) {
        outOfCombatDelaySeconds = asset.getOutOfCombatDelaySeconds();
        decayPerSecond = asset.getDecayPerSecond();
    }

    public float getOutOfCombatDelaySeconds() {
        return outOfCombatDelaySeconds;
    }

    public float getDecayPerSecond() {
        return decayPerSecond;
    }
}
