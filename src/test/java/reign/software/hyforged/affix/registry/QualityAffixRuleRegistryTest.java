package reign.software.hyforged.affix.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.affix.model.QualityAffixRule;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QualityAffixRuleRegistry.
 */
class QualityAffixRuleRegistryTest {

    @BeforeEach
    void setUp() {
        QualityAffixRuleRegistry.reset();
    }

    @Test
    void get_returnsSameInstance() {
        QualityAffixRuleRegistry instance1 = QualityAffixRuleRegistry.get();
        QualityAffixRuleRegistry instance2 = QualityAffixRuleRegistry.get();
        assertSame(instance1, instance2);
    }

    @Test
    void register_newRule_addsToRegistry() {
        QualityAffixRule rule = new QualityAffixRule("Rare", Map.of("prefix", 2, "suffix", 2));
        
        QualityAffixRuleRegistry.get().register(rule);
        
        assertNotNull(QualityAffixRuleRegistry.get().get("Rare"));
    }

    @Test
    void register_duplicateQuality_replacesExisting() {
        QualityAffixRule rule1 = new QualityAffixRule("Test", Map.of("prefix", 1));
        QualityAffixRule rule2 = new QualityAffixRule("Test", Map.of("prefix", 5, "suffix", 3));
        
        QualityAffixRuleRegistry.get().register(rule1);
        QualityAffixRuleRegistry.get().register(rule2);
        
        QualityAffixRule result = QualityAffixRuleRegistry.get().get("Test");
        assertNotNull(result);
        assertEquals(5, result.getCapacity("prefix"));
        assertEquals(3, result.getCapacity("suffix"));
    }

    @Test
    void get_existingQuality_returnsRule() {
        QualityAffixRuleRegistry.get().register(
                new QualityAffixRule("Epic", Map.of("prefix", 3, "suffix", 3)));
        
        QualityAffixRule result = QualityAffixRuleRegistry.get().get("Epic");
        
        assertNotNull(result);
        assertEquals("Epic", result.quality());
    }

    @Test
    void get_nonExistentQuality_returnsNull() {
        QualityAffixRule result = QualityAffixRuleRegistry.get().get("NonExistent");
        
        assertNull(result);
    }

    @Test
    void getOrEmpty_existingQuality_returnsRule() {
        QualityAffixRuleRegistry.get().register(
                new QualityAffixRule("Legendary", Map.of("prefix", 4, "suffix", 4)));
        
        QualityAffixRule result = QualityAffixRuleRegistry.get().getOrEmpty("Legendary");
        
        assertEquals("Legendary", result.quality());
        assertEquals(4, result.getCapacity("prefix"));
    }

    @Test
    void getOrEmpty_nonExistentQuality_returnsEmptyRule() {
        QualityAffixRule result = QualityAffixRuleRegistry.get().getOrEmpty("Unknown");
        
        assertEquals(0, result.getCapacity("prefix"));
        assertEquals(0, result.getCapacity("suffix"));
        assertEquals(0, result.getTotalCapacity());
    }

    @Test
    void getCapacity_delegatesToRule() {
        QualityAffixRuleRegistry.get().register(
                new QualityAffixRule("Uncommon", Map.of("prefix", 1, "suffix", 1)));
        
        assertEquals(1, QualityAffixRuleRegistry.get().getCapacity("Uncommon", "prefix"));
        assertEquals(1, QualityAffixRuleRegistry.get().getCapacity("Uncommon", "suffix"));
        assertEquals(0, QualityAffixRuleRegistry.get().getCapacity("Uncommon", "forged"));
    }

    @Test
    void getCapacity_unknownQuality_returnsZero() {
        assertEquals(0, QualityAffixRuleRegistry.get().getCapacity("Unknown", "prefix"));
    }

    @Test
    void allowsAffixes_unknownQuality_returnsFalse() {
        assertFalse(QualityAffixRuleRegistry.get().allowsAffixes("Unknown"));
    }

    @Test
    void getAll_returnsUnmodifiableCollection() {
        QualityAffixRuleRegistry.get().register(
                new QualityAffixRule("Test", Map.of()));
        
        var all = QualityAffixRuleRegistry.get().getAll();
        
        assertThrows(UnsupportedOperationException.class, () -> 
                all.add(new QualityAffixRule("New", Map.of())));
    }

    @Test
    void reset_removesAllEntries() {
        QualityAffixRuleRegistry.get().register(
                new QualityAffixRule("Rule1", Map.of()));
        QualityAffixRuleRegistry.get().register(
                new QualityAffixRule("Rule2", Map.of()));
        
        QualityAffixRuleRegistry.reset();
        
        assertEquals(0, QualityAffixRuleRegistry.get().size());
    }
}
