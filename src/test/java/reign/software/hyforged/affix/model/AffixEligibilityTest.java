package reign.software.hyforged.affix.model;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffixEligibility record.
 */
class AffixEligibilityTest {

    @Test
    void anyConstant_matchesEverything() {
        AffixEligibility any = AffixEligibility.ANY;
        
        assertTrue(any.itemCategories().isEmpty());
        assertTrue(any.itemTags().isEmpty());
        assertTrue(any.excludeTags().isEmpty());
        assertNull(any.minQuality());
        assertNull(any.maxQuality());
    }

    @Test
    void constructor_nullCollections_becomesEmptySets() {
        AffixEligibility eligibility = new AffixEligibility(null, null, null, null, null);
        
        assertNotNull(eligibility.itemCategories());
        assertTrue(eligibility.itemCategories().isEmpty());
        assertNotNull(eligibility.itemTags());
        assertTrue(eligibility.itemTags().isEmpty());
        assertNotNull(eligibility.excludeTags());
        assertTrue(eligibility.excludeTags().isEmpty());
    }

    @Test
    void constructor_makesDefensiveCopies() {
        Set<String> categories = new java.util.HashSet<>();
        categories.add("Items.Weapons");
        
        AffixEligibility eligibility = new AffixEligibility(categories, Set.of(), Set.of(), null, null);
        
        // Modify original set
        categories.add("Items.Armor");
        
        // Eligibility should not be affected
        assertEquals(1, eligibility.itemCategories().size());
        assertTrue(eligibility.itemCategories().contains("Items.Weapons"));
        assertFalse(eligibility.itemCategories().contains("Items.Armor"));
    }

    @Test
    void hasCategoryConstraints_withCategories_returnsTrue() {
        AffixEligibility eligibility = new AffixEligibility(
                Set.of("Items.Weapons"), Set.of(), Set.of(), null, null);
        assertTrue(eligibility.hasCategoryConstraints());
    }

    @Test
    void hasCategoryConstraints_noCategories_returnsFalse() {
        AffixEligibility eligibility = new AffixEligibility(
                Set.of(), Set.of("Type:Weapon"), Set.of(), null, null);
        assertFalse(eligibility.hasCategoryConstraints());
    }

    @Test
    void matchesInclusion_noConstraints_matchesAll() {
        AffixEligibility eligibility = AffixEligibility.ANY;
        assertTrue(eligibility.matchesInclusion(Set.of("Items.Weapons"), Set.of("Type:Weapon")));
        assertTrue(eligibility.matchesInclusion(Set.of(), Set.of()));
    }

    @Test
    void matchesInclusion_categoryMatch_returnsTrue() {
        AffixEligibility eligibility = new AffixEligibility(
                Set.of("Items.Weapons"), Set.of(), Set.of(), null, null);
        assertTrue(eligibility.matchesInclusion(Set.of("Items.Weapons"), Set.of()));
        assertFalse(eligibility.matchesInclusion(Set.of("Items.Armor"), Set.of()));
    }

    @Test
    void matchesInclusion_tagMatch_returnsTrue() {
        AffixEligibility eligibility = new AffixEligibility(
                Set.of(), Set.of("Type:Weapon"), Set.of(), null, null);
        assertTrue(eligibility.matchesInclusion(Set.of(), Set.of("Type:Weapon")));
        assertFalse(eligibility.matchesInclusion(Set.of(), Set.of("Type:Armor")));
    }

    @Test
    void matchesInclusion_eitherCategoryOrTag_returnsTrue() {
        AffixEligibility eligibility = new AffixEligibility(
                Set.of("Items.Weapons"), Set.of("Type:Armor"), Set.of(), null, null);
        
        // Category match
        assertTrue(eligibility.matchesInclusion(Set.of("Items.Weapons"), Set.of()));
        // Tag match
        assertTrue(eligibility.matchesInclusion(Set.of(), Set.of("Type:Armor")));
        // Both match
        assertTrue(eligibility.matchesInclusion(Set.of("Items.Weapons"), Set.of("Type:Armor")));
        // Neither match
        assertFalse(eligibility.matchesInclusion(Set.of("Items.Tools"), Set.of("Type:Tool")));
    }

    @Test
    void matchesExclusion_noExcludeTags_returnsFalse() {
        AffixEligibility eligibility = AffixEligibility.ANY;
        assertFalse(eligibility.matchesExclusion(Set.of("Tool", "Debug")));
    }

    @Test
    void matchesExclusion_hasExcludeMatch_returnsTrue() {
        AffixEligibility eligibility = new AffixEligibility(
                Set.of(), Set.of(), Set.of("Tool", "Debug"), null, null);
        assertTrue(eligibility.matchesExclusion(Set.of("Tool")));
        assertTrue(eligibility.matchesExclusion(Set.of("Debug")));
        assertTrue(eligibility.matchesExclusion(Set.of("Weapon", "Tool")));
    }

    @Test
    void matchesExclusion_noMatch_returnsFalse() {
        AffixEligibility eligibility = new AffixEligibility(
                Set.of(), Set.of(), Set.of("Tool"), null, null);
        assertFalse(eligibility.matchesExclusion(Set.of("Weapon")));
        assertFalse(eligibility.matchesExclusion(Set.of()));
    }

    @Test
    void matchesQuality_noConstraints_returnsTrue() {
        AffixEligibility eligibility = AffixEligibility.ANY;
        Map<String, Integer> order = Map.of("Common", 1, "Legendary", 5);
        assertTrue(eligibility.matchesQuality("Common", order));
        assertTrue(eligibility.matchesQuality("Legendary", order));
    }

    @Test
    void matchesQuality_withinRange_returnsTrue() {
        AffixEligibility eligibility = new AffixEligibility(
                Set.of(), Set.of(), Set.of(), "Rare", "Epic");
        Map<String, Integer> order = Map.of(
                "Common", 1, "Uncommon", 2, "Rare", 3, "Epic", 4, "Legendary", 5);
        
        assertTrue(eligibility.matchesQuality("Rare", order));
        assertTrue(eligibility.matchesQuality("Epic", order));
    }

    @Test
    void matchesQuality_belowMin_returnsFalse() {
        AffixEligibility eligibility = new AffixEligibility(
                Set.of(), Set.of(), Set.of(), "Rare", null);
        Map<String, Integer> order = Map.of(
                "Common", 1, "Uncommon", 2, "Rare", 3, "Epic", 4);
        
        assertFalse(eligibility.matchesQuality("Common", order));
        assertFalse(eligibility.matchesQuality("Uncommon", order));
        assertTrue(eligibility.matchesQuality("Rare", order));
        assertTrue(eligibility.matchesQuality("Epic", order));
    }

    @Test
    void matchesQuality_aboveMax_returnsFalse() {
        AffixEligibility eligibility = new AffixEligibility(
                Set.of(), Set.of(), Set.of(), null, "Rare");
        Map<String, Integer> order = Map.of(
                "Common", 1, "Uncommon", 2, "Rare", 3, "Epic", 4);
        
        assertTrue(eligibility.matchesQuality("Common", order));
        assertTrue(eligibility.matchesQuality("Rare", order));
        assertFalse(eligibility.matchesQuality("Epic", order));
    }

    @Test
    void matchesQuality_unknownQuality_returnsFalse() {
        AffixEligibility eligibility = new AffixEligibility(
                Set.of(), Set.of(), Set.of(), "Common", null);
        Map<String, Integer> order = Map.of("Common", 1);
        
        assertFalse(eligibility.matchesQuality("Unknown", order));
    }

    @Test
    void builder_createsValidInstance() {
        AffixEligibility eligibility = AffixEligibility.builder()
                .categories(Set.of("Items.Weapons"))
                .tags(Set.of("Type:Weapon"))
                .excludeTags(Set.of("Tool"))
                .qualityRange("Rare", "Epic")
                .build();
        
        assertTrue(eligibility.itemCategories().contains("Items.Weapons"));
        assertTrue(eligibility.itemTags().contains("Type:Weapon"));
        assertTrue(eligibility.excludeTags().contains("Tool"));
        assertEquals("Rare", eligibility.minQuality());
        assertEquals("Epic", eligibility.maxQuality());
    }
}
