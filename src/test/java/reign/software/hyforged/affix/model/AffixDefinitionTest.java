package reign.software.hyforged.affix.model;

import org.junit.jupiter.api.Test;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffixDefinition record.
 */
class AffixDefinitionTest {

    private static AffixDefinition createTestAffix() {
        return new AffixDefinition(
                "sturdy",
                "prefix",
                "Sturdy",
                StatId.hyforged("armor"),
                HyforgedModifier.StackType.FLAT,
                List.of(
                        new AffixTierDefinition(1, 50, 75, 40),
                        new AffixTierDefinition(2, 35, 50, 25),
                        new AffixTierDefinition(3, 20, 35, 10),
                        new AffixTierDefinition(4, 10, 20, 5),
                        new AffixTierDefinition(5, 1, 10, 1)
                ),
                AffixEligibility.ANY,
                100
        );
    }

    @Test
    void constructor_validInput_createsInstance() {
        AffixDefinition affix = createTestAffix();
        
        assertEquals("sturdy", affix.id());
        assertEquals("prefix", affix.type());
        assertEquals("Sturdy", affix.displayName());
        assertEquals(StatId.hyforged("armor"), affix.statId());
        assertEquals(HyforgedModifier.StackType.FLAT, affix.modifierType());
        assertEquals(5, affix.tiers().size());
        assertEquals(100, affix.weight());
    }

    @Test
    void constructor_nullId_throwsException() {
        assertThrows(NullPointerException.class, () -> 
                new AffixDefinition(null, "prefix", "Test", StatId.hyforged("armor"),
                        HyforgedModifier.StackType.FLAT, List.of(new AffixTierDefinition(1, 1, 10, 0)),
                        AffixEligibility.ANY, 100));
    }

    @Test
    void constructor_blankId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixDefinition("  ", "prefix", "Test", StatId.hyforged("armor"),
                        HyforgedModifier.StackType.FLAT, List.of(new AffixTierDefinition(1, 1, 10, 0)),
                        AffixEligibility.ANY, 100));
    }

    @Test
    void constructor_emptyTiers_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixDefinition("test", "prefix", "Test", StatId.hyforged("armor"),
                        HyforgedModifier.StackType.FLAT, List.of(),
                        AffixEligibility.ANY, 100));
    }

    @Test
    void constructor_negativeWeight_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixDefinition("test", "prefix", "Test", StatId.hyforged("armor"),
                        HyforgedModifier.StackType.FLAT, List.of(new AffixTierDefinition(1, 1, 10, 0)),
                        AffixEligibility.ANY, -1));
    }

    @Test
    void constructor_makesDefensiveCopy() {
        List<AffixTierDefinition> tiers = new java.util.ArrayList<>();
        tiers.add(new AffixTierDefinition(1, 1, 10, 0));
        
        AffixDefinition affix = new AffixDefinition("test", "prefix", "Test", 
                StatId.hyforged("armor"), HyforgedModifier.StackType.FLAT, tiers,
                AffixEligibility.ANY, 100);
        
        // Modify original list
        tiers.add(new AffixTierDefinition(2, 1, 5, 0));
        
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
        assertEquals(50, tier1.get().minValue());
        
        Optional<AffixTierDefinition> tier5 = affix.getTier(5);
        assertTrue(tier5.isPresent());
        assertEquals(1, tier5.get().minValue());
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
                .statId(StatId.hyforged("strength"))
                .modifierType(HyforgedModifier.StackType.INCREASED)
                .tiers(List.of(new AffixTierDefinition(1, 100, 200, 0)))
                .eligibility(AffixEligibility.ANY)
                .weight(150)
                .build();
        
        assertEquals("test", affix.id());
        assertEquals("suffix", affix.type());
        assertEquals(HyforgedModifier.StackType.INCREASED, affix.modifierType());
        assertEquals(150, affix.weight());
    }
}
