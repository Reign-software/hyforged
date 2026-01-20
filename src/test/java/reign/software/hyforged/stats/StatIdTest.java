package reign.software.hyforged.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StatId record.
 */
class StatIdTest {

    @Test
    void hyforged_createsWithCorrectNamespace() {
        StatId id = StatId.hyforged("strength");
        assertEquals("hyforged", id.namespace());
        assertEquals("strength", id.name());
    }

    @Test
    void fullId_returnsCorrectFormat() {
        StatId id = new StatId("mymod", "custom-stat");
        assertEquals("mymod:custom-stat", id.fullId());
    }

    @Test
    void parse_validId_returnsStatId() {
        StatId id = StatId.parse("hyforged:max-health");
        assertEquals("hyforged", id.namespace());
        assertEquals("max-health", id.name());
    }

    @Test
    void parse_invalidId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            StatId.parse("invalid-no-colon");
        });
    }

    @Test
    void constructor_emptyNamespace_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new StatId("", "stat");
        });
    }

    @Test
    void constructor_emptyName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new StatId("namespace", "");
        });
    }

    @Test
    void constructor_colonInNamespace_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new StatId("name:space", "stat");
        });
    }

    @Test
    void constructor_colonInName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new StatId("namespace", "stat:name");
        });
    }

    @Test
    void toString_returnsFullId() {
        StatId id = new StatId("hyforged", "strength");
        assertEquals("hyforged:strength", id.toString());
    }

    @Test
    void equals_sameValues_returnsTrue() {
        StatId id1 = new StatId("hyforged", "strength");
        StatId id2 = new StatId("hyforged", "strength");
        assertEquals(id1, id2);
    }

    @Test
    void equals_differentNamespace_returnsFalse() {
        StatId id1 = new StatId("hyforged", "strength");
        StatId id2 = new StatId("othermod", "strength");
        assertNotEquals(id1, id2);
    }
}
