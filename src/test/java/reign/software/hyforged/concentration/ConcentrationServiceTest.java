package reign.software.hyforged.concentration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConcentrationService")
class ConcentrationServiceTest {

    @Test
    @DisplayName("registers abilities in priority order")
    void registersAbilitiesInPriorityOrder() {
        ConcentrationPriorityComponent component = new ConcentrationPriorityComponent();

        ConcentrationService.registerAbility(component, "hyforged:ability-low", 10, 5, null, null);
        ConcentrationService.registerAbility(component, "hyforged:ability-high", 10, 20, null, null);
        ConcentrationService.registerAbility(component, "hyforged:ability-mid", 10, 12, null, null);

        assertEquals("hyforged:ability-high", component.getAbilities().get(0).abilityId());
        assertEquals("hyforged:ability-mid", component.getAbilities().get(1).abilityId());
        assertEquals("hyforged:ability-low", component.getAbilities().get(2).abilityId());
    }

    @Test
    @DisplayName("disables lowest priority abilities when concentration is insufficient")
    void disablesLowestPriorityAbilitiesFirst() {
        ConcentrationPriorityComponent component = new ConcentrationPriorityComponent();
        AtomicInteger disabledCount = new AtomicInteger();

        ConcentrationService.registerAbility(component, "hyforged:ability-high", 30, 30, disabledCount::incrementAndGet, null);
        ConcentrationService.registerAbility(component, "hyforged:ability-mid", 30, 20, disabledCount::incrementAndGet, null);
        ConcentrationService.registerAbility(component, "hyforged:ability-low", 30, 10, disabledCount::incrementAndGet, null);

        component.setCurrentConcentration(50);
        ConcentrationService.applyLossToComponent(component, 100, 0);

        assertTrue(component.getAbilities().get(0).enabled());
        assertFalse(component.getAbilities().get(1).enabled());
        assertFalse(component.getAbilities().get(2).enabled());
        assertEquals(2, disabledCount.get());
    }

    @Test
    @DisplayName("re-enables highest priority abilities when concentration recovers")
    void reEnablesInPriorityOrder() {
        ConcentrationPriorityComponent component = new ConcentrationPriorityComponent();
        AtomicInteger enabledCount = new AtomicInteger();

        ConcentrationService.registerAbility(component, "hyforged:ability-high", 30, 30, null, enabledCount::incrementAndGet);
        ConcentrationService.registerAbility(component, "hyforged:ability-mid", 30, 20, null, enabledCount::incrementAndGet);
        ConcentrationService.registerAbility(component, "hyforged:ability-low", 30, 10, null, enabledCount::incrementAndGet);

        component.setCurrentConcentration(50);
        ConcentrationService.applyLossToComponent(component, 100, 0);

        ConcentrationService.applyRegenToComponent(component, 100, 10f);

        assertTrue(component.getAbilities().get(0).enabled());
        assertTrue(component.getAbilities().get(1).enabled());
        assertFalse(component.getAbilities().get(2).enabled());
        assertEquals(1, enabledCount.get());
    }

    @Test
    @DisplayName("reorders abilities and reconciles enabled state")
    void reordersAbilitiesAndReconcilesEnabledState() {
        ConcentrationPriorityComponent component = new ConcentrationPriorityComponent();

        ConcentrationService.registerAbility(component, "hyforged:ability-high", 30, 30, null, null);
        ConcentrationService.registerAbility(component, "hyforged:ability-mid", 30, 20, null, null);
        ConcentrationService.registerAbility(component, "hyforged:ability-low", 30, 10, null, null);

        component.setCurrentConcentration(50);
        ConcentrationService.applyPriorityOrder(
                component,
                List.of("hyforged:ability-low", "hyforged:ability-high", "hyforged:ability-mid"),
                50
        );

        assertEquals("hyforged:ability-low", component.getAbilities().get(0).abilityId());
        assertTrue(component.getAbilities().get(0).enabled());
        assertFalse(component.getAbilities().get(1).enabled());
        assertFalse(component.getAbilities().get(2).enabled());
        assertTrue(component.getAbilities().get(0).priority() > component.getAbilities().get(1).priority());
        assertTrue(component.getAbilities().get(1).priority() > component.getAbilities().get(2).priority());
    }
}
