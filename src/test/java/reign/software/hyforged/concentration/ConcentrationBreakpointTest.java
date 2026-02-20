package reign.software.hyforged.concentration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConcentrationBreakpoint computation")
class ConcentrationBreakpointTest {

    @Test
    @DisplayName("empty component produces no breakpoints")
    void emptyAbilitiesNoBreakpoints() {
        ConcentrationPriorityComponent component = new ConcentrationPriorityComponent();

        List<ConcentrationBreakpoint> breakpoints = computeBreakpoints(component);
        assertTrue(breakpoints.isEmpty());
    }

    @Test
    @DisplayName("single ability produces one breakpoint with matching cumulative cost")
    void singleAbilityOneBreakpoint() {
        ConcentrationPriorityComponent component = new ConcentrationPriorityComponent();
        ConcentrationService.registerAbility(component, "test:buff", 25, 10, null, null);

        List<ConcentrationBreakpoint> breakpoints = computeBreakpoints(component);
        assertEquals(1, breakpoints.size());

        ConcentrationBreakpoint bp = breakpoints.get(0);
        assertEquals("test:buff", bp.abilityId());
        assertEquals(25, bp.cost());
        assertEquals(25, bp.cumulativeCost());
        assertTrue(bp.enabled());
    }

    @Test
    @DisplayName("multiple abilities produce cumulative breakpoints in priority order")
    void multipleCumulativeBreakpoints() {
        ConcentrationPriorityComponent component = new ConcentrationPriorityComponent();
        component.setCurrentConcentration(100);

        ConcentrationService.registerAbility(component, "test:high", 30, 30, null, null);
        ConcentrationService.registerAbility(component, "test:mid", 20, 20, null, null);
        ConcentrationService.registerAbility(component, "test:low", 10, 10, null, null);

        List<ConcentrationBreakpoint> breakpoints = computeBreakpoints(component);
        assertEquals(3, breakpoints.size());

        // Highest priority first
        assertEquals("test:high", breakpoints.get(0).abilityId());
        assertEquals(30, breakpoints.get(0).cost());
        assertEquals(30, breakpoints.get(0).cumulativeCost());

        assertEquals("test:mid", breakpoints.get(1).abilityId());
        assertEquals(20, breakpoints.get(1).cost());
        assertEquals(50, breakpoints.get(1).cumulativeCost()); // 30 + 20

        assertEquals("test:low", breakpoints.get(2).abilityId());
        assertEquals(10, breakpoints.get(2).cost());
        assertEquals(60, breakpoints.get(2).cumulativeCost()); // 30 + 20 + 10
    }

    @Test
    @DisplayName("disabled abilities are marked in breakpoints")
    void disabledAbilitiesMarked() {
        ConcentrationPriorityComponent component = new ConcentrationPriorityComponent();
        component.setCurrentConcentration(100);

        ConcentrationService.registerAbility(component, "test:high", 40, 30, null, null);
        ConcentrationService.registerAbility(component, "test:mid", 40, 20, null, null);
        ConcentrationService.registerAbility(component, "test:low", 40, 10, null, null);

        // Only 100 concentration → first two enabled (cost 80), third disabled (would need 120)
        ConcentrationService.applyLossToComponent(component, 100, 0);

        List<ConcentrationBreakpoint> breakpoints = computeBreakpoints(component);
        assertEquals(3, breakpoints.size());

        assertTrue(breakpoints.get(0).enabled());
        assertTrue(breakpoints.get(1).enabled());
        assertFalse(breakpoints.get(2).enabled());
    }

    @Test
    @DisplayName("zero cost abilities contribute zero to cumulative")
    void zeroCostAbilities() {
        ConcentrationPriorityComponent component = new ConcentrationPriorityComponent();
        component.setCurrentConcentration(100);

        ConcentrationService.registerAbility(component, "test:free", 0, 20, null, null);
        ConcentrationService.registerAbility(component, "test:paid", 25, 10, null, null);

        List<ConcentrationBreakpoint> breakpoints = computeBreakpoints(component);
        assertEquals(2, breakpoints.size());

        assertEquals(0, breakpoints.get(0).cost());
        assertEquals(0, breakpoints.get(0).cumulativeCost());
        assertEquals(25, breakpoints.get(1).cost());
        assertEquals(25, breakpoints.get(1).cumulativeCost());
    }

    /**
     * Helper that computes breakpoints from a component directly
     * (mirrors ConcentrationService.getAbilityCostBreakpoints logic
     * without requiring ECS store access).
     */
    private static List<ConcentrationBreakpoint> computeBreakpoints(ConcentrationPriorityComponent component) {
        List<ConcentratedAbility> abilities = component.getAbilities();
        if (abilities.isEmpty()) {
            return List.of();
        }

        java.util.List<ConcentrationBreakpoint> breakpoints = new java.util.ArrayList<>(abilities.size());
        int cumulative = 0;
        for (ConcentratedAbility ability : abilities) {
            cumulative += Math.max(0, ability.cost());
            breakpoints.add(new ConcentrationBreakpoint(
                    ability.abilityId(),
                    ability.cost(),
                    cumulative,
                    ability.enabled()
            ));
        }
        return breakpoints;
    }
}
