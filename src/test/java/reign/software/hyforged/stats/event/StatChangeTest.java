package reign.software.hyforged.stats.event;

import org.junit.jupiter.api.Test;
import reign.software.hyforged.stats.StatId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StatChange record.
 */
class StatChangeTest {

    @Test
    void delta_increase_returnsPositive() {
        StatChange change = new StatChange(StatId.hyforged("strength"), 0, 10, 25, "test");
        assertEquals(15, change.delta());
    }

    @Test
    void delta_decrease_returnsNegative() {
        StatChange change = new StatChange(StatId.hyforged("strength"), 0, 50, 30, "test");
        assertEquals(-20, change.delta());
    }

    @Test
    void delta_noChange_returnsZero() {
        StatChange change = new StatChange(StatId.hyforged("strength"), 0, 100, 100, "test");
        assertEquals(0, change.delta());
    }

    @Test
    void isIncrease_positiveChange_returnsTrue() {
        StatChange change = new StatChange(StatId.hyforged("strength"), 0, 10, 20, "test");
        assertTrue(change.isIncrease());
        assertFalse(change.isDecrease());
    }

    @Test
    void isDecrease_negativeChange_returnsTrue() {
        StatChange change = new StatChange(StatId.hyforged("strength"), 0, 50, 25, "test");
        assertTrue(change.isDecrease());
        assertFalse(change.isIncrease());
    }

    @Test
    void noChange_bothReturnFalse() {
        StatChange change = new StatChange(StatId.hyforged("strength"), 0, 100, 100, "test");
        assertFalse(change.isIncrease());
        assertFalse(change.isDecrease());
    }

    @Test
    void recordFieldsAccessible() {
        StatId statId = StatId.hyforged("max-health");
        StatChange change = new StatChange(statId, 5, 100, 150, "equipment");
        
        assertEquals(statId, change.statId());
        assertEquals(5, change.statIndex());
        assertEquals(100, change.oldValue());
        assertEquals(150, change.newValue());
        assertEquals("equipment", change.sourceId());
    }
}
