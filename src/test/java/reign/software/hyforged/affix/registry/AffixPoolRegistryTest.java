package reign.software.hyforged.affix.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.affix.model.AffixPool;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffixPoolRegistry.
 */
class AffixPoolRegistryTest {

    @BeforeEach
    void setUp() {
        AffixPoolRegistry.reset();
    }

    private AffixPool createPool(String id, int priority, Set<String> categories, Set<String> tags) {
        return new AffixPool(id, priority,
                new AffixPool.AffixPoolAppliesTo(categories, tags),
                List.of("prefix1"), List.of("suffix1"), List.of());
    }

    @Test
    void get_returnsSameInstance() {
        AffixPoolRegistry instance1 = AffixPoolRegistry.get();
        AffixPoolRegistry instance2 = AffixPoolRegistry.get();
        assertSame(instance1, instance2);
    }

    @Test
    void register_newPool_addsToRegistry() {
        AffixPool pool = createPool("weapon-pool", 10, Set.of("Items.Weapons"), Set.of());
        
        AffixPoolRegistry.get().register(pool);
        
        assertNotNull(AffixPoolRegistry.get().get("weapon-pool"));
    }

    @Test
    void register_duplicateId_replacesExisting() {
        AffixPool pool1 = createPool("test", 10, Set.of("Cat1"), Set.of());
        AffixPool pool2 = createPool("test", 20, Set.of("Cat2"), Set.of("Tag1"));
        
        AffixPoolRegistry.get().register(pool1);
        AffixPoolRegistry.get().register(pool2);
        
        AffixPool result = AffixPoolRegistry.get().get("test");
        assertNotNull(result);
        assertEquals(20, result.priority());
    }

    @Test
    void findMatching_singleMatch_returnsSinglePool() {
        AffixPool pool = createPool("armor", 10, Set.of("Items.Armor"), Set.of());
        AffixPoolRegistry.get().register(pool);
        
        List<AffixPool> result = AffixPoolRegistry.get()
                .findMatching(Set.of("Items.Armor"), Set.of());
        
        assertEquals(1, result.size());
        assertEquals("armor", result.get(0).id());
    }

    @Test
    void findMatching_multipleMatches_sortedByPriorityDesc() {
        AffixPoolRegistry.get().register(
                createPool("low", 5, Set.of("Items.Weapons"), Set.of()));
        AffixPoolRegistry.get().register(
                createPool("high", 20, Set.of("Items.Weapons"), Set.of()));
        AffixPoolRegistry.get().register(
                createPool("medium", 10, Set.of("Items.Weapons"), Set.of()));
        
        List<AffixPool> result = AffixPoolRegistry.get()
                .findMatching(Set.of("Items.Weapons"), Set.of());
        
        assertEquals(3, result.size());
        assertEquals("high", result.get(0).id());
        assertEquals("medium", result.get(1).id());
        assertEquals("low", result.get(2).id());
    }

    @Test
    void findMatching_samePriority_sortedByIdAsc() {
        AffixPoolRegistry.get().register(
                createPool("zebra", 10, Set.of("Items.Weapons"), Set.of()));
        AffixPoolRegistry.get().register(
                createPool("alpha", 10, Set.of("Items.Weapons"), Set.of()));
        AffixPoolRegistry.get().register(
                createPool("beta", 10, Set.of("Items.Weapons"), Set.of()));
        
        List<AffixPool> result = AffixPoolRegistry.get()
                .findMatching(Set.of("Items.Weapons"), Set.of());
        
        assertEquals(3, result.size());
        assertEquals("alpha", result.get(0).id());
        assertEquals("beta", result.get(1).id());
        assertEquals("zebra", result.get(2).id());
    }

    @Test
    void findMatching_matchByTag() {
        AffixPool pool = createPool("axe-pool", 10, Set.of(), Set.of("Family:Axe"));
        AffixPoolRegistry.get().register(pool);
        
        List<AffixPool> result = AffixPoolRegistry.get()
                .findMatching(Set.of(), Set.of("Family:Axe"));
        
        assertEquals(1, result.size());
        assertEquals("axe-pool", result.get(0).id());
    }

    @Test
    void findMatching_noMatch_returnsEmpty() {
        AffixPoolRegistry.get().register(
                createPool("weapons", 10, Set.of("Items.Weapons"), Set.of()));
        
        List<AffixPool> result = AffixPoolRegistry.get()
                .findMatching(Set.of("Items.Armor"), Set.of());
        
        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_returnsHighestPriority() {
        AffixPoolRegistry.get().register(
                createPool("low", 5, Set.of("Items.Weapons"), Set.of()));
        AffixPoolRegistry.get().register(
                createPool("high", 20, Set.of("Items.Weapons"), Set.of()));
        
        AffixPool result = AffixPoolRegistry.get()
                .resolve(Set.of("Items.Weapons"), Set.of());
        
        assertNotNull(result);
        assertEquals("high", result.id());
    }

    @Test
    void resolve_noMatch_returnsNull() {
        AffixPool result = AffixPoolRegistry.get()
                .resolve(Set.of("Items.Unknown"), Set.of());
        
        assertNull(result);
    }

    @Test
    void getAll_returnsUnmodifiableCollection() {
        AffixPoolRegistry.get().register(
                createPool("test", 10, Set.of("Items.Weapons"), Set.of()));
        
        var all = AffixPoolRegistry.get().getAll();
        
        assertThrows(UnsupportedOperationException.class, () -> 
                all.add(createPool("new", 5, Set.of(), Set.of())));
    }

    @Test
    void reset_removesAllEntries() {
        AffixPoolRegistry.get().register(
                createPool("pool1", 10, Set.of("Cat1"), Set.of()));
        AffixPoolRegistry.get().register(
                createPool("pool2", 20, Set.of("Cat2"), Set.of()));
        
        AffixPoolRegistry.reset();
        
        assertEquals(0, AffixPoolRegistry.get().size());
    }

    @Test
    void size_returnsCorrectCount() {
        assertEquals(0, AffixPoolRegistry.get().size());
        
        AffixPoolRegistry.get().register(
                createPool("pool1", 10, Set.of(), Set.of()));
        AffixPoolRegistry.get().register(
                createPool("pool2", 20, Set.of(), Set.of()));
        
        assertEquals(2, AffixPoolRegistry.get().size());
    }
}
