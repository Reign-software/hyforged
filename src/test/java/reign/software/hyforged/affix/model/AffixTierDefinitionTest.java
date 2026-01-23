package reign.software.hyforged.affix.model;

import org.junit.jupiter.api.Test;
import reign.software.hyforged.affix.AffixTestFixtures;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffixTierDefinition record.
 */
class AffixTierDefinitionTest {
    
    private static final String HEALTH = "hyforged:health";
    
    private Map<String, AffixTierStat> singleStat(String statId, HyforgedModifier.StackType stackType, int min, int max) {
        Map<String, AffixTierStat> stats = new HashMap<>();
        stats.put(statId, new AffixTierStat(
                reign.software.hyforged.stats.StatId.parse(statId), 
                stackType, 
                min, 
                max));
        return stats;
    }

    @Test
    void constructor_validInput_createsInstance() {
        Map<String, AffixTierStat> stats = singleStat(HEALTH, HyforgedModifier.StackType.FLAT, 50, 75);
        AffixTierDefinition tier = new AffixTierDefinition(1, 40, 100, stats);
        
        assertEquals(1, tier.tier());
        assertEquals(40, tier.itemLevelReq());
        assertEquals(100, tier.weight());
        assertEquals(1, tier.stats().size());
        assertTrue(tier.stats().containsKey(HEALTH));
    }

    @Test
    void constructor_withDefaultWeight_usesDefaultWeight() {
        Map<String, AffixTierStat> stats = singleStat(HEALTH, HyforgedModifier.StackType.FLAT, 50, 75);
        AffixTierDefinition tier = new AffixTierDefinition(1, 40, stats);
        assertEquals(AffixTierDefinition.DEFAULT_WEIGHT, tier.weight());
    }

    @Test
    void constructor_tierLessThanOne_throwsException() {
        Map<String, AffixTierStat> stats = singleStat(HEALTH, HyforgedModifier.StackType.FLAT, 50, 75);
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixTierDefinition(0, 40, 100, stats));
    }

    @Test
    void constructor_negativeItemLevelReq_throwsException() {
        Map<String, AffixTierStat> stats = singleStat(HEALTH, HyforgedModifier.StackType.FLAT, 50, 75);
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixTierDefinition(1, -1, 100, stats));
    }

    @Test
    void constructor_negativeWeight_throwsException() {
        Map<String, AffixTierStat> stats = singleStat(HEALTH, HyforgedModifier.StackType.FLAT, 50, 75);
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixTierDefinition(1, 40, -1, stats));
    }
    
    @Test
    void constructor_emptyStats_allowsEmpty() {
        AffixTierDefinition tier = new AffixTierDefinition(1, 40, 100, new HashMap<>());
        assertTrue(tier.stats().isEmpty());
    }
    
    @Test
    void constructor_nullStats_throwsException() {
        assertThrows(NullPointerException.class, () -> 
                new AffixTierDefinition(1, 40, 100, null));
    }

    @Test
    void canRollAt_levelMeetsReq_returnsTrue() {
        Map<String, AffixTierStat> stats = singleStat(HEALTH, HyforgedModifier.StackType.FLAT, 50, 75);
        AffixTierDefinition tier = new AffixTierDefinition(1, 40, stats);
        assertTrue(tier.canRollAt(40));
        assertTrue(tier.canRollAt(50));
    }

    @Test
    void canRollAt_levelBelowReq_returnsFalse() {
        Map<String, AffixTierStat> stats = singleStat(HEALTH, HyforgedModifier.StackType.FLAT, 50, 75);
        AffixTierDefinition tier = new AffixTierDefinition(1, 40, stats);
        assertFalse(tier.canRollAt(39));
        assertFalse(tier.canRollAt(0));
    }
    
    @Test
    void stats_areImmutable() {
        Map<String, AffixTierStat> stats = singleStat(HEALTH, HyforgedModifier.StackType.FLAT, 50, 75);
        AffixTierDefinition tier = new AffixTierDefinition(1, 40, stats);
        
        assertThrows(UnsupportedOperationException.class, () ->
                tier.stats().put("another", new AffixTierStat(
                        reign.software.hyforged.stats.StatId.hyforged("armor"),
                        HyforgedModifier.StackType.FLAT, 10, 20)));
    }
    
    @Test
    void affixTestFixturesTier_createsValidDefinition() {
        AffixTierDefinition tier = AffixTestFixtures.tier(
                1, 40, 100, HEALTH, HyforgedModifier.StackType.FLAT, 50, 75);
        
        assertEquals(1, tier.tier());
        assertEquals(40, tier.itemLevelReq());
        assertEquals(100, tier.weight());
        assertTrue(tier.stats().containsKey(HEALTH));
        
        AffixTierStat stat = tier.stats().get(HEALTH);
        assertEquals(50, stat.minValue());
        assertEquals(75, stat.maxValue());
        assertEquals(HyforgedModifier.StackType.FLAT, stat.stackType());
    }
}
