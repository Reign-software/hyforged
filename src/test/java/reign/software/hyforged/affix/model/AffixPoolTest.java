package reign.software.hyforged.affix.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffixPool record.
 */
class AffixPoolTest {

    @Test
    void constructor_validInput_createsInstance() {
        AffixPool pool = AffixPool.of(
                "weapon-melee",
                10,
                new AffixPool.AffixPoolAppliesTo(Set.of("Items.Weapons"), Set.of("Type:Weapon")),
                List.of("sharp", "mighty"),
                List.of("of-the-bear"),
                List.of()
        );
        
        assertEquals("weapon-melee", pool.id());
        assertEquals(10, pool.priority());
        assertEquals(2, pool.prefixes().size());
        assertEquals(1, pool.suffixes().size());
        assertEquals(0, pool.forged().size());
    }

    @Test
    void constructor_nullId_throwsException() {
        assertThrows(NullPointerException.class, () -> 
                new AffixPool(null, 0, 
                        new AffixPool.AffixPoolAppliesTo(Set.of(), Set.of()),
                        Map.of()));
    }

    @Test
    void constructor_blankId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                new AffixPool("  ", 0, 
                        new AffixPool.AffixPoolAppliesTo(Set.of(), Set.of()),
                        Map.of()));
    }

    @Test
    void constructor_nullLists_becomeEmptyLists() {
        AffixPool pool = new AffixPool(
                "test", 0,
                new AffixPool.AffixPoolAppliesTo(Set.of(), Set.of()),
                null
        );
        
        assertNotNull(pool.prefixes());
        assertTrue(pool.prefixes().isEmpty());
        assertNotNull(pool.suffixes());
        assertTrue(pool.suffixes().isEmpty());
        assertNotNull(pool.forged());
        assertTrue(pool.forged().isEmpty());
    }

    @Test
    void constructor_makesDefensiveCopies() {
        List<String> prefixes = new java.util.ArrayList<>();
        prefixes.add("sharp");
        
        AffixPool pool = AffixPool.of("test", 0,
                new AffixPool.AffixPoolAppliesTo(Set.of(), Set.of()),
                prefixes, List.of(), List.of());
        
        // Modify original list
        prefixes.add("mighty");
        
        // Pool should not be affected
        assertEquals(1, pool.prefixes().size());
    }

    @Test
    void getAffixesForType_knownTypes_returnsCorrectList() {
        AffixPool pool = AffixPool.of("test", 0,
                new AffixPool.AffixPoolAppliesTo(Set.of(), Set.of()),
                List.of("prefix1", "prefix2"),
                List.of("suffix1"),
                List.of("forged1"));
        
        assertEquals(2, pool.getAffixesForType("prefix").size());
        assertEquals(1, pool.getAffixesForType("suffix").size());
        assertEquals(1, pool.getAffixesForType("forged").size());
    }

    @Test
    void getAffixesForType_exactKeyMatch() {
        AffixPool pool = AffixPool.of("test", 0,
                new AffixPool.AffixPoolAppliesTo(Set.of(), Set.of()),
                List.of("prefix1"), List.of(), List.of());
        
        // Exact key match works
        assertEquals(1, pool.getAffixesForType("prefix").size());
        // Different case does NOT match — types are data-driven and case-sensitive
        assertTrue(pool.getAffixesForType("PREFIX").isEmpty());
        assertTrue(pool.getAffixesForType("Prefix").isEmpty());
    }

    @Test
    void getAffixesForType_customTypes() {
        Map<String, List<String>> affixes = new HashMap<>();
        affixes.put("npc", List.of("fast", "tough"));
        affixes.put("npc_rare", List.of("fire-aura"));
        AffixPool pool = new AffixPool("test", 0,
                new AffixPool.AffixPoolAppliesTo(Set.of(), Set.of()),
                affixes);
        
        assertEquals(2, pool.getAffixesForType("npc").size());
        assertEquals(1, pool.getAffixesForType("npc_rare").size());
        assertTrue(pool.getAffixesForType("prefix").isEmpty());
    }

    @Test
    void getAffixesForType_unknownType_returnsEmptyList() {
        AffixPool pool = AffixPool.of("test", 0,
                new AffixPool.AffixPoolAppliesTo(Set.of(), Set.of()),
                List.of("prefix1"), List.of(), List.of());
        
        assertTrue(pool.getAffixesForType("unknown").isEmpty());
    }

    @Test
    void hasAffixesOfType_withAffixes_returnsTrue() {
        AffixPool pool = AffixPool.of("test", 0,
                new AffixPool.AffixPoolAppliesTo(Set.of(), Set.of()),
                List.of("prefix1"), List.of(), List.of());
        
        assertTrue(pool.hasAffixesOfType("prefix"));
        assertFalse(pool.hasAffixesOfType("suffix"));
        assertFalse(pool.hasAffixesOfType("forged"));
    }

    @Test
    void appliesTo_matchingCategory_returnsTrue() {
        AffixPool pool = AffixPool.of("test", 0,
                new AffixPool.AffixPoolAppliesTo(Set.of("Items.Weapons"), Set.of()),
                List.of(), List.of(), List.of());
        
        assertTrue(pool.appliesTo(Set.of("Items.Weapons"), Set.of()));
        assertFalse(pool.appliesTo(Set.of("Items.Armor"), Set.of()));
    }

    @Test
    void appliesTo_matchingTag_returnsTrue() {
        AffixPool pool = AffixPool.of("test", 0,
                new AffixPool.AffixPoolAppliesTo(Set.of(), Set.of("Type:Weapon")),
                List.of(), List.of(), List.of());
        
        assertTrue(pool.appliesTo(Set.of(), Set.of("Type:Weapon")));
        assertFalse(pool.appliesTo(Set.of(), Set.of("Type:Armor")));
    }

    @Test
    void appliesTo_noConstraints_returnsFalse() {
        AffixPool pool = AffixPool.of("test", 0,
                new AffixPool.AffixPoolAppliesTo(Set.of(), Set.of()),
                List.of(), List.of(), List.of());
        
        // Empty constraints should not match anything
        assertFalse(pool.appliesTo(Set.of("Items.Weapons"), Set.of("Type:Weapon")));
    }

    @Test
    void getTotalAffixCount_sumsAllTypes() {
        AffixPool pool = AffixPool.of("test", 0,
                new AffixPool.AffixPoolAppliesTo(Set.of(), Set.of()),
                List.of("p1", "p2", "p3"),
                List.of("s1", "s2"),
                List.of("f1"));
        
        assertEquals(6, pool.getTotalAffixCount());
    }

    @Test
    void builder_createsValidInstance() {
        AffixPool pool = AffixPool.builder()
                .id("test-pool")
                .priority(5)
                .appliesTo(Set.of("Items.Armor"), Set.of("Type:Armor"))
                .prefixes(List.of("sturdy"))
                .suffixes(List.of("of-defense"))
                .forged(List.of())
                .build();
        
        assertEquals("test-pool", pool.id());
        assertEquals(5, pool.priority());
        assertTrue(pool.appliesTo(Set.of("Items.Armor"), Set.of()));
        assertEquals(1, pool.prefixes().size());
        assertEquals(1, pool.suffixes().size());
    }

    @Test
    void affixPoolAppliesTo_nullSets_becomeEmptySets() {
        AffixPool.AffixPoolAppliesTo appliesTo = new AffixPool.AffixPoolAppliesTo(null, null);
        
        assertNotNull(appliesTo.categories());
        assertTrue(appliesTo.categories().isEmpty());
        assertNotNull(appliesTo.tags());
        assertTrue(appliesTo.tags().isEmpty());
    }

    @Test
    void affixPoolAppliesTo_matches_categoryOrTag() {
        AffixPool.AffixPoolAppliesTo appliesTo = new AffixPool.AffixPoolAppliesTo(
                Set.of("Items.Weapons"), Set.of("Family:Axe"));
        
        // Category match
        assertTrue(appliesTo.matches(Set.of("Items.Weapons"), Set.of()));
        // Tag match
        assertTrue(appliesTo.matches(Set.of(), Set.of("Family:Axe")));
        // Both match
        assertTrue(appliesTo.matches(Set.of("Items.Weapons"), Set.of("Family:Axe")));
        // Neither match
        assertFalse(appliesTo.matches(Set.of("Items.Armor"), Set.of("Family:Sword")));
    }
}
