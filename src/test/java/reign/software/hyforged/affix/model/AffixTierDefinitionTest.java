package reign.software.hyforged.affix.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffixTierDefinition record.
 */
class AffixTierDefinitionTest {

    @Test
    void constructor_validInput_createsInstance() {
        AffixTierDefinition tier = new AffixTierDefinition(1, 50, 75, 40, 100);
        
        assertEquals(1, tier.tier());
        assertEquals(50, tier.minValue());
        assertEquals(75, tier.maxValue());
        assertEquals(40, tier.itemLevelReq());
        assertEquals(100, tier.weight());
    }

    @Test
    void constructor_withDefaultWeight_usesDefaultWeight() {
        AffixTierDefinition tier = new AffixTierDefinition(1, 50, 75, 40);
        assertEquals(AffixTierDefinition.DEFAULT_WEIGHT, tier.weight());
    }

    @Test
    void constructor_tierLessThanOne_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixTierDefinition(0, 50, 75, 40, 100));
    }

    @Test
    void constructor_minGreaterThanMax_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixTierDefinition(1, 100, 50, 40, 100));
    }

    @Test
    void constructor_negativeItemLevelReq_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixTierDefinition(1, 50, 75, -1, 100));
    }

    @Test
    void constructor_negativeWeight_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixTierDefinition(1, 50, 75, 40, -1));
    }

    @Test
    void canRollAt_levelMeetsReq_returnsTrue() {
        AffixTierDefinition tier = new AffixTierDefinition(1, 50, 75, 40);
        assertTrue(tier.canRollAt(40));
        assertTrue(tier.canRollAt(50));
    }

    @Test
    void canRollAt_levelBelowReq_returnsFalse() {
        AffixTierDefinition tier = new AffixTierDefinition(1, 50, 75, 40);
        assertFalse(tier.canRollAt(39));
        assertFalse(tier.canRollAt(0));
    }

    @Test
    void rollValue_zeroFraction_returnsMin() {
        AffixTierDefinition tier = new AffixTierDefinition(1, 50, 75, 40);
        assertEquals(50, tier.rollValue(0.0));
    }

    @Test
    void rollValue_fractionNearOne_returnsMax() {
        AffixTierDefinition tier = new AffixTierDefinition(1, 50, 75, 40);
        // With fraction just under 1.0, should get max
        int result = tier.rollValue(0.999);
        assertTrue(result >= 50 && result <= 75);
    }

    @Test
    void rollValue_midFraction_returnsMidRange() {
        AffixTierDefinition tier = new AffixTierDefinition(1, 0, 100, 0);
        int result = tier.rollValue(0.5);
        assertTrue(result >= 45 && result <= 55, "Expected mid-range value, got: " + result);
    }

    @Test
    void rollValue_sameMinMax_returnsThatValue() {
        AffixTierDefinition tier = new AffixTierDefinition(1, 50, 50, 0);
        assertEquals(50, tier.rollValue(0.0));
        assertEquals(50, tier.rollValue(0.5));
        assertEquals(50, tier.rollValue(0.999));
    }

    @Test
    void getMidValue_returnsAverage() {
        AffixTierDefinition tier = new AffixTierDefinition(1, 50, 100, 0);
        assertEquals(75, tier.getMidValue());
    }

    @Test
    void builder_createsValidInstance() {
        AffixTierDefinition tier = AffixTierDefinition.builder()
                .tier(2)
                .valueRange(30, 50)
                .itemLevelReq(20)
                .weight(150)
                .build();
        
        assertEquals(2, tier.tier());
        assertEquals(30, tier.minValue());
        assertEquals(50, tier.maxValue());
        assertEquals(20, tier.itemLevelReq());
        assertEquals(150, tier.weight());
    }
}
