package reign.software.hyforged.concentration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Concentration System Integration Tests")
class ConcentrationSystemIntegrationTest {

    @Test
    @DisplayName("Concentration loss triggers only when HP below threshold")
    void lossTriggersBelowThreshold() {
        assertFalse(HyforgedConcentrationDisruptionSystem.isBelowThreshold(800f, 1000f, 7500));
        assertTrue(HyforgedConcentrationDisruptionSystem.isBelowThreshold(700f, 1000f, 7500));
    }

    @Test
    @DisplayName("Concentration loss scales with damage and max concentration")
    void lossScalesWithDamage() {
        int loss = HyforgedConcentrationDisruptionSystem.calculateBaseLoss(250f, 1000f, 200);
        assertEquals(50, loss);
    }

    @Test
    @DisplayName("Loss reduction reduces effective loss")
    void lossReductionApplies() {
        int effective = HyforgedConcentrationDisruptionSystem.applyLossReduction(40, 5000);
        assertEquals(20, effective);
    }

    @Test
    @DisplayName("Blocked or missed hits do not trigger loss")
    void blockedAndMissedNoLoss() {
        assertFalse(HyforgedConcentrationDisruptionSystem.shouldApplyLoss(false, 100f, true, false));
        assertFalse(HyforgedConcentrationDisruptionSystem.shouldApplyLoss(false, 100f, false, true));
    }

    @Test
    @DisplayName("Regeneration re-enables abilities in priority order")
    void regenReenablesByPriority() {
        ConcentrationPriorityComponent component = new ConcentrationPriorityComponent();
        ConcentrationService.registerAbility(component, "hyforged:ability-high", 30, 30, null, null);
        ConcentrationService.registerAbility(component, "hyforged:ability-mid", 30, 20, null, null);
        ConcentrationService.registerAbility(component, "hyforged:ability-low", 30, 10, null, null);

        component.setCurrentConcentration(50);
        ConcentrationService.applyLossToComponent(component, 100, 0);

        ConcentrationService.applyRegenToComponent(component, 100, 10f);

        assertTrue(component.getAbilities().get(0).enabled());
        assertTrue(component.getAbilities().get(1).enabled());
        assertFalse(component.getAbilities().get(2).enabled());
    }

    @Test
    @DisplayName("Regen formula is always active")
    void regenAlwaysActiveFormula() {
        float regen = HyforgedConcentrationRegenerationSystem.calculateRegenPerSecond(12, 0.5f, 1000);
        assertTrue(regen > 0f);
    }
}
