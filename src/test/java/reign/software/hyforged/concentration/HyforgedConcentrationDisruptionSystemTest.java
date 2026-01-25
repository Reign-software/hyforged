package reign.software.hyforged.concentration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HyforgedConcentrationDisruptionSystem")
class HyforgedConcentrationDisruptionSystemTest {

    @Test
    @DisplayName("HP threshold check blocks loss above threshold")
    void thresholdCheck() {
        assertFalse(HyforgedConcentrationDisruptionSystem.isBelowThreshold(800f, 1000f, 7500));
        assertTrue(HyforgedConcentrationDisruptionSystem.isBelowThreshold(700f, 1000f, 7500));
    }

    @Test
    @DisplayName("base loss calculation scales with damage and max concentration")
    void baseLossCalculation() {
        int loss = HyforgedConcentrationDisruptionSystem.calculateBaseLoss(100f, 1000f, 200);
        assertEquals(20, loss);
    }

    @Test
    @DisplayName("loss reduction reduces concentration loss")
    void lossReductionCalculation() {
        int reduced = HyforgedConcentrationDisruptionSystem.applyLossReduction(20, 2500);
        assertEquals(15, reduced);
    }

    @Test
    @DisplayName("blocked or missed damage prevents loss")
    void blockedOrMissedPreventsLoss() {
        assertFalse(HyforgedConcentrationDisruptionSystem.shouldApplyLoss(false, 100f, true, false));
        assertFalse(HyforgedConcentrationDisruptionSystem.shouldApplyLoss(false, 100f, false, true));
        assertTrue(HyforgedConcentrationDisruptionSystem.shouldApplyLoss(false, 100f, false, false));
    }
}
