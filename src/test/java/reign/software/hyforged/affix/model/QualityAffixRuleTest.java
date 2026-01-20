package reign.software.hyforged.affix.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QualityAffixRule record.
 */
class QualityAffixRuleTest {

    @Test
    void constructor_validInput_createsInstance() {
        QualityAffixRule rule = new QualityAffixRule("Legendary", Map.of(
                "prefix", 4,
                "suffix", 4,
                "forged", 0
        ));
        
        assertEquals("Legendary", rule.quality());
        assertEquals(4, rule.getCapacity("prefix"));
        assertEquals(4, rule.getCapacity("suffix"));
        assertEquals(0, rule.getCapacity("forged"));
    }

    @Test
    void constructor_nullQuality_throwsException() {
        assertThrows(NullPointerException.class, () -> 
                new QualityAffixRule(null, Map.of("prefix", 1)));
    }

    @Test
    void constructor_nullMap_becomesEmptyMap() {
        QualityAffixRule rule = new QualityAffixRule("Test", null);
        assertNotNull(rule.affixCapacity());
        assertTrue(rule.affixCapacity().isEmpty());
    }

    @Test
    void constructor_negativeCapacity_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                new QualityAffixRule("Test", Map.of("prefix", -1)));
    }

    @Test
    void constructor_makesDefensiveCopy() {
        Map<String, Integer> capacity = new java.util.HashMap<>();
        capacity.put("prefix", 2);
        
        QualityAffixRule rule = new QualityAffixRule("Test", capacity);
        
        // Modify original map
        capacity.put("suffix", 3);
        
        // Rule should not be affected
        assertEquals(0, rule.getCapacity("suffix"));
    }

    @Test
    void emptyConstant_hasZeroCapacity() {
        QualityAffixRule empty = QualityAffixRule.EMPTY;
        assertEquals(0, empty.getCapacity("prefix"));
        assertEquals(0, empty.getCapacity("suffix"));
        assertEquals(0, empty.getTotalCapacity());
    }

    @Test
    void getCapacity_existingType_returnsValue() {
        QualityAffixRule rule = new QualityAffixRule("Rare", Map.of("prefix", 2, "suffix", 2));
        assertEquals(2, rule.getCapacity("prefix"));
        assertEquals(2, rule.getCapacity("suffix"));
    }

    @Test
    void getCapacity_unknownType_returnsZero() {
        QualityAffixRule rule = new QualityAffixRule("Rare", Map.of("prefix", 2));
        assertEquals(0, rule.getCapacity("unknown"));
    }

    @Test
    void getTotalCapacity_sumsAllTypes() {
        QualityAffixRule rule = new QualityAffixRule("Epic", Map.of(
                "prefix", 3,
                "suffix", 3,
                "forged", 1
        ));
        assertEquals(7, rule.getTotalCapacity());
    }

    @Test
    void getTotalCapacity_emptyMap_returnsZero() {
        QualityAffixRule rule = new QualityAffixRule("Common", Map.of());
        assertEquals(0, rule.getTotalCapacity());
    }

    @Test
    void allowsType_withCapacity_returnsTrue() {
        QualityAffixRule rule = new QualityAffixRule("Uncommon", Map.of("prefix", 1, "suffix", 1));
        assertTrue(rule.allowsType("prefix"));
        assertTrue(rule.allowsType("suffix"));
    }

    @Test
    void allowsType_zeroCapacity_returnsFalse() {
        QualityAffixRule rule = new QualityAffixRule("Common", Map.of("prefix", 1, "forged", 0));
        assertTrue(rule.allowsType("prefix"));
        assertFalse(rule.allowsType("forged"));
        assertFalse(rule.allowsType("unknown"));
    }

    @Test
    void allowsAnyAffixes_withCapacity_returnsTrue() {
        QualityAffixRule rule = new QualityAffixRule("Common", Map.of("prefix", 1));
        assertTrue(rule.allowsAnyAffixes());
    }

    @Test
    void allowsAnyAffixes_noCapacity_returnsFalse() {
        QualityAffixRule rule = new QualityAffixRule("Junk", Map.of());
        assertFalse(rule.allowsAnyAffixes());
    }

    @Test
    void allowsAnyAffixes_allZero_returnsFalse() {
        QualityAffixRule rule = new QualityAffixRule("Tool", Map.of("prefix", 0, "suffix", 0));
        assertFalse(rule.allowsAnyAffixes());
    }

    @Test
    void builder_createsValidInstance() {
        QualityAffixRule rule = QualityAffixRule.builder()
                .quality("Epic")
                .prefixCapacity(3)
                .suffixCapacity(3)
                .forgedCapacity(1)
                .build();
        
        assertEquals("Epic", rule.quality());
        assertEquals(3, rule.getCapacity("prefix"));
        assertEquals(3, rule.getCapacity("suffix"));
        assertEquals(1, rule.getCapacity("forged"));
    }

    @Test
    void builder_customType_works() {
        QualityAffixRule rule = QualityAffixRule.builder()
                .quality("Custom")
                .capacity("custom-type", 5)
                .build();
        
        assertEquals(5, rule.getCapacity("custom-type"));
    }
}
