package reign.software.hyforged.affix.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.AffixEligibility;
import reign.software.hyforged.affix.model.AffixTierDefinition;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffixDefinitionRegistry.
 */
class AffixDefinitionRegistryTest {

    @BeforeEach
    void setUp() {
        AffixDefinitionRegistry.reset();
    }

    private AffixDefinition createAffix(String id, String type, String statName) {
        return new AffixDefinition(id, type, "Test", 
                StatId.hyforged(statName), HyforgedModifier.StackType.FLAT,
                List.of(new AffixTierDefinition(1, 1, 10, 0)),
                AffixEligibility.ANY, 100);
    }

    @Test
    void get_returnsSameInstance() {
        AffixDefinitionRegistry instance1 = AffixDefinitionRegistry.get();
        AffixDefinitionRegistry instance2 = AffixDefinitionRegistry.get();
        assertSame(instance1, instance2);
    }

    @Test
    void register_newDefinition_addsToRegistry() {
        AffixDefinition affix = createAffix("sturdy", "prefix", "armor");
        
        AffixDefinitionRegistry.get().register(affix);
        
        assertNotNull(AffixDefinitionRegistry.get().get("sturdy"));
    }

    @Test
    void register_duplicateId_replacesExisting() {
        AffixDefinition affix1 = createAffix("test", "prefix", "armor");
        AffixDefinition affix2 = createAffix("test", "suffix", "strength");
        
        AffixDefinitionRegistry.get().register(affix1);
        AffixDefinitionRegistry.get().register(affix2);
        
        AffixDefinition result = AffixDefinitionRegistry.get().get("test");
        assertNotNull(result);
        assertEquals("suffix", result.type());
        assertEquals(StatId.hyforged("strength"), result.statId());
    }

    @Test
    void getByType_returnsMatchingAffixes() {
        AffixDefinitionRegistry.get().register(createAffix("a1", "prefix", "stat1"));
        AffixDefinitionRegistry.get().register(createAffix("a2", "prefix", "stat2"));
        AffixDefinitionRegistry.get().register(createAffix("a3", "suffix", "stat3"));
        
        List<AffixDefinition> prefixes = AffixDefinitionRegistry.get().getByType("prefix");
        List<AffixDefinition> suffixes = AffixDefinitionRegistry.get().getByType("suffix");
        
        assertEquals(2, prefixes.size());
        assertEquals(1, suffixes.size());
    }

    @Test
    void getByType_nonExistentType_returnsEmpty() {
        AffixDefinitionRegistry.get().register(createAffix("a1", "prefix", "stat1"));
        
        List<AffixDefinition> result = AffixDefinitionRegistry.get().getByType("forged");
        
        assertTrue(result.isEmpty());
    }

    @Test
    void getByStat_returnsMatchingAffixes() {
        AffixDefinitionRegistry.get().register(createAffix("a1", "prefix", "armor"));
        AffixDefinitionRegistry.get().register(createAffix("a2", "suffix", "armor"));
        AffixDefinitionRegistry.get().register(createAffix("a3", "prefix", "strength"));
        
        List<AffixDefinition> armorAffixes = 
                AffixDefinitionRegistry.get().getByStat(StatId.hyforged("armor"));
        List<AffixDefinition> strengthAffixes = 
                AffixDefinitionRegistry.get().getByStat(StatId.hyforged("strength"));
        
        assertEquals(2, armorAffixes.size());
        assertEquals(1, strengthAffixes.size());
    }

    @Test
    void getByStat_nonExistentStat_returnsEmpty() {
        AffixDefinitionRegistry.get().register(createAffix("a1", "prefix", "armor"));
        
        List<AffixDefinition> result = 
                AffixDefinitionRegistry.get().getByStat(StatId.hyforged("nonexistent"));
        
        assertTrue(result.isEmpty());
    }

    @Test
    void getAll_returnsUnmodifiableCollection() {
        AffixDefinitionRegistry.get().register(createAffix("test", "prefix", "stat"));
        
        Collection<AffixDefinition> all = AffixDefinitionRegistry.get().getAll();
        
        assertThrows(UnsupportedOperationException.class, () -> 
                all.add(createAffix("new", "prefix", "stat")));
    }

    @Test
    void countByType_returnsCorrectCount() {
        AffixDefinitionRegistry.get().register(createAffix("affix1", "prefix", "stat1"));
        AffixDefinitionRegistry.get().register(createAffix("affix2", "prefix", "stat2"));
        AffixDefinitionRegistry.get().register(createAffix("affix3", "suffix", "stat3"));
        
        assertEquals(2, AffixDefinitionRegistry.get().countByType("prefix"));
        assertEquals(1, AffixDefinitionRegistry.get().countByType("suffix"));
        assertEquals(0, AffixDefinitionRegistry.get().countByType("forged"));
    }

    @Test
    void reset_removesAllEntriesAndIndexes() {
        AffixDefinitionRegistry.get().register(createAffix("a1", "prefix", "armor"));
        AffixDefinitionRegistry.get().register(createAffix("a2", "suffix", "strength"));
        
        AffixDefinitionRegistry.reset();
        
        assertEquals(0, AffixDefinitionRegistry.get().size());
        assertTrue(AffixDefinitionRegistry.get().getByType("prefix").isEmpty());
        assertTrue(AffixDefinitionRegistry.get().getByStat(StatId.hyforged("armor")).isEmpty());
    }
}
