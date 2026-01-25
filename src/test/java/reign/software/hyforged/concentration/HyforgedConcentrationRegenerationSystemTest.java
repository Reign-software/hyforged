package reign.software.hyforged.concentration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HyforgedConcentrationRegenerationSystem")
class HyforgedConcentrationRegenerationSystemTest {

    @Test
    @DisplayName("Wisdom scaling produces expected regen per second")
    void wisdomScalingCalculation() {
        float regen = HyforgedConcentrationRegenerationSystem.calculateRegenPerSecond(10, 0.5f, 0);
        assertEquals(5.0f, regen, 0.001f);
    }

    @Test
    @DisplayName("Regen rate modifier applies to regen per second")
    void regenRateModifierApplies() {
        float regen = HyforgedConcentrationRegenerationSystem.calculateRegenPerSecond(10, 0.5f, 2000);
        assertEquals(6.0f, regen, 0.001f);
    }
}
