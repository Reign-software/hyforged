package reign.software.hyforged.affix.model;

import org.junit.jupiter.api.Test;
import reign.software.hyforged.affix.AffixTestFixtures;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffixDefinition record.
 */
class AffixDefinitionTest {
    
    private static final String ARMOR = "hyforged:armor";
    private static final String STRENGTH = "hyforged:strength";

    private static AffixDefinition createTestAffix() {
        return new AffixDefinition(
                "sturdy",
                "prefix",
                "Sturdy",
                List.of(
                        AffixTestFixtures.tier(1, 40, 50, ARMOR, HyforgedModifier.StackType.FLAT, 50, 75),
                        AffixTestFixtures.tier(2, 25, 100, ARMOR, HyforgedModifier.StackType.FLAT, 35, 50),
                        AffixTestFixtures.tier(3, 10, 150, ARMOR, HyforgedModifier.StackType.FLAT, 20, 35),
                        AffixTestFixtures.tier(4, 5, 200, ARMOR, HyforgedModifier.StackType.FLAT, 10, 20),
                        AffixTestFixtures.tier(5, 1, 250, ARMOR, HyforgedModifier.StackType.FLAT, 1, 10)
                ),
                100
        );
    }

    @Test
    void constructor_validInput_createsInstance() {
        AffixDefinition affix = createTestAffix();
        
        assertEquals("sturdy", affix.id());
        assertEquals("prefix", affix.type());
        assertEquals("Sturdy", affix.displayName());
        assertEquals(5, affix.tiers().size());
        assertEquals(100, affix.weight());
        
        // Check that stat IDs are properly collected
        Set<String> statIds = affix.getStatIds();
        assertEquals(1, statIds.size());
        assertTrue(statIds.contains(ARMOR));
    }

    @Test
    void constructor_nullId_throwsException() {
        assertThrows(NullPointerException.class, () -> 
                new AffixDefinition(null, "prefix", "Test",
                        List.of(AffixTestFixtures.tier(1, 1, 100, ARMOR, HyforgedModifier.StackType.FLAT, 1, 10)),
                        100));
    }

    @Test
    void constructor_blankId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixDefinition("  ", "prefix", "Test",
                        List.of(AffixTestFixtures.tier(1, 1, 100, ARMOR, HyforgedModifier.StackType.FLAT, 1, 10)),
                        100));
    }

    @Test
    void constructor_emptyTiers_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixDefinition("test", "prefix", "Test",
                        List.of(),
                        100));
    }

    @Test
    void constructor_negativeWeight_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixDefinition("test", "prefix", "Test",
                        List.of(AffixTestFixtures.tier(1, 1, 100, ARMOR, HyforgedModifier.StackType.FLAT, 1, 10)),
                        -1));
    }

    @Test
    void constructor_makesDefensiveCopy() {
        List<AffixTierDefinition> tiers = new ArrayList<>();
        tiers.add(AffixTestFixtures.tier(1, 1, 100, ARMOR, HyforgedModifier.StackType.FLAT, 1, 10));
        
        AffixDefinition affix = new AffixDefinition("test", "prefix", "Test", tiers, 100);
        
        // Modify original list
        tiers.add(AffixTestFixtures.tier(2, 1, 100, ARMOR, HyforgedModifier.StackType.FLAT, 1, 5));
        
        // Affix should not be affected
        assertEquals(1, affix.tiers().size());
    }

    @Test
    void getAvailableTiers_highLevel_returnsAll() {
        AffixDefinition affix = createTestAffix();
        List<AffixTierDefinition> available = affix.getAvailableTiers(50);
        assertEquals(5, available.size());
    }

    @Test
    void getAvailableTiers_lowLevel_returnsSubset() {
        AffixDefinition affix = createTestAffix();
        List<AffixTierDefinition> available = affix.getAvailableTiers(15);
        
        // Tiers with itemLevelReq <= 15: tier 3 (10), tier 4 (5), tier 5 (1)
        assertEquals(3, available.size());
        assertTrue(available.stream().allMatch(t -> t.itemLevelReq() <= 15));
    }

    @Test
    void getAvailableTiers_zeroLevel_returnsLowestOnly() {
        AffixDefinition affix = createTestAffix();
        List<AffixTierDefinition> available = affix.getAvailableTiers(0);
        
        // No tiers should be available at level 0
        assertEquals(0, available.size());
    }

    @Test
    void getBestAvailableTier_returnsLowestTierNumber() {
        AffixDefinition affix = createTestAffix();
        Optional<AffixTierDefinition> best = affix.getBestAvailableTier(50);
        
        assertTrue(best.isPresent());
        assertEquals(1, best.get().tier());
    }

    @Test
    void getBestAvailableTier_limitedLevel_returnsAvailableBest() {
        AffixDefinition affix = createTestAffix();
        Optional<AffixTierDefinition> best = affix.getBestAvailableTier(15);
        
        assertTrue(best.isPresent());
        assertEquals(3, best.get().tier()); // Tier 3 requires level 10
    }

    @Test
    void getBestAvailableTier_noAvailable_returnsEmpty() {
        AffixDefinition affix = createTestAffix();
        Optional<AffixTierDefinition> best = affix.getBestAvailableTier(0);
        
        assertFalse(best.isPresent());
    }

    @Test
    void getTier_existingTier_returnsTier() {
        AffixDefinition affix = createTestAffix();
        
        Optional<AffixTierDefinition> tier1 = affix.getTier(1);
        assertTrue(tier1.isPresent());
        // Check first stat's min value
        AffixTierStat stat1 = tier1.get().stats().values().iterator().next();
        assertEquals(50, stat1.minValue());
        
        Optional<AffixTierDefinition> tier5 = affix.getTier(5);
        assertTrue(tier5.isPresent());
        AffixTierStat stat5 = tier5.get().stats().values().iterator().next();
        assertEquals(1, stat5.minValue());
    }

    @Test
    void getTier_nonExistentTier_returnsEmpty() {
        AffixDefinition affix = createTestAffix();
        Optional<AffixTierDefinition> tier = affix.getTier(6);
        assertFalse(tier.isPresent());
    }

    @Test
    void hasAvailableTiers_withAvailable_returnsTrue() {
        AffixDefinition affix = createTestAffix();
        assertTrue(affix.hasAvailableTiers(50));
        assertTrue(affix.hasAvailableTiers(1));
    }

    @Test
    void hasAvailableTiers_noneAvailable_returnsFalse() {
        AffixDefinition affix = createTestAffix();
        assertFalse(affix.hasAvailableTiers(0));
    }

    @Test
    void getTierCount_returnsCorrectCount() {
        AffixDefinition affix = createTestAffix();
        assertEquals(5, affix.getTierCount());
    }

    @Test
    void builder_createsValidInstance() {
        AffixDefinition affix = AffixDefinition.builder()
                .id("test")
                .type("suffix")
                .displayName("of Testing")
                .tiers(List.of(AffixTestFixtures.tier(1, 1, 100, STRENGTH, HyforgedModifier.StackType.INCREASED, 100, 200)))
                .weight(150)
                .build();
        
        assertEquals("test", affix.id());
        assertEquals("suffix", affix.type());
        assertEquals(150, affix.weight());
        
        Set<String> statIds = affix.getStatIds();
        assertTrue(statIds.contains(STRENGTH));
    }
}
