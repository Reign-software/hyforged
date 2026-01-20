package reign.software.hyforged.stats.npc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NPCStatScaling record.
 */
class NPCStatScalingTest {

    @Test
    void resolveAt_level0_returnsBase() {
        NPCStatScaling scaling = new NPCStatScaling(100, 20);
        assertEquals(100, scaling.resolveAt(0));
    }

    @Test
    void resolveAt_level5_returnsCorrectValue() {
        NPCStatScaling scaling = new NPCStatScaling(100, 20);
        // 100 + (20 * 5) = 200
        assertEquals(200, scaling.resolveAt(5));
    }

    @Test
    void resolveAt_level10_returnsCorrectValue() {
        NPCStatScaling scaling = new NPCStatScaling(50, 10);
        // 50 + (10 * 10) = 150
        assertEquals(150, scaling.resolveAt(10));
    }

    @Test
    void resolveAt_negativeLevel_treatAsZero() {
        NPCStatScaling scaling = new NPCStatScaling(100, 20);
        // Negative levels should be treated as 0
        assertEquals(100, scaling.resolveAt(-5));
    }

    @Test
    void flat_createsScalingWithNoPerLevel() {
        NPCStatScaling scaling = NPCStatScaling.flat(50);
        assertEquals(50, scaling.base());
        assertEquals(0, scaling.perLevel());
        assertEquals(50, scaling.resolveAt(10));
    }

    @Test
    void zeroPerLevel_flatScaling() {
        NPCStatScaling scaling = new NPCStatScaling(100, 0);
        assertEquals(100, scaling.resolveAt(0));
        assertEquals(100, scaling.resolveAt(100));
    }
}
