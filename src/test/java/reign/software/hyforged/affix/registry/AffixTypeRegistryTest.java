package reign.software.hyforged.affix.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.affix.model.AffixType;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffixTypeRegistry.
 */
class AffixTypeRegistryTest {

    @BeforeEach
    void setUp() {
        // Reset registry before each test
        AffixTypeRegistry.reset();
    }

    @Test
    void get_returnsSameInstance() {
        AffixTypeRegistry instance1 = AffixTypeRegistry.get();
        AffixTypeRegistry instance2 = AffixTypeRegistry.get();
        assertSame(instance1, instance2);
    }

    @Test
    void register_newType_addsToRegistry() {
        AffixType type = new AffixType("prefix", 
                AffixType.DisplayNamePosition.BEFORE, "{affix} {item}", true);
        
        AffixTypeRegistry.get().register(type);
        
        assertNotNull(AffixTypeRegistry.get().get("prefix"));
    }

    @Test
    void register_duplicateId_replacesExisting() {
        AffixType type1 = new AffixType("test", 
                AffixType.DisplayNamePosition.BEFORE, "{affix} {item}", true);
        AffixType type2 = new AffixType("test", 
                AffixType.DisplayNamePosition.AFTER, "{item} {affix}", false);
        
        AffixTypeRegistry.get().register(type1);
        AffixTypeRegistry.get().register(type2);
        
        AffixType result = AffixTypeRegistry.get().get("test");
        assertNotNull(result);
        assertEquals(AffixType.DisplayNamePosition.AFTER, result.displayNamePosition());
        assertFalse(result.stackable());
    }

    @Test
    void get_existingId_returnsType() {
        AffixType type = new AffixType("suffix", 
                AffixType.DisplayNamePosition.AFTER, "{item} {affix}", true);
        AffixTypeRegistry.get().register(type);
        
        AffixType result = AffixTypeRegistry.get().get("suffix");
        
        assertNotNull(result);
        assertEquals("suffix", result.id());
    }

    @Test
    void get_nonExistentId_returnsNull() {
        AffixType result = AffixTypeRegistry.get().get("nonexistent");
        
        assertNull(result);
    }

    @Test
    void contains_existingId_returnsTrue() {
        AffixTypeRegistry.get().register(
                new AffixType("forged", AffixType.DisplayNamePosition.NONE, "", false));
        
        assertTrue(AffixTypeRegistry.get().contains("forged"));
    }

    @Test
    void contains_nonExistentId_returnsFalse() {
        assertFalse(AffixTypeRegistry.get().contains("notregistered"));
    }

    @Test
    void getAll_returnsAllRegistered() {
        AffixTypeRegistry.get().register(
                new AffixType("type1", AffixType.DisplayNamePosition.BEFORE, "", true));
        AffixTypeRegistry.get().register(
                new AffixType("type2", AffixType.DisplayNamePosition.AFTER, "", true));
        AffixTypeRegistry.get().register(
                new AffixType("type3", AffixType.DisplayNamePosition.NONE, "", false));
        
        Collection<AffixType> all = AffixTypeRegistry.get().getAll();
        
        assertEquals(3, all.size());
    }

    @Test
    void getAll_returnsUnmodifiableCollection() {
        AffixTypeRegistry.get().register(
                new AffixType("test", AffixType.DisplayNamePosition.BEFORE, "", true));
        
        Collection<AffixType> all = AffixTypeRegistry.get().getAll();
        
        assertThrows(UnsupportedOperationException.class, () -> 
                all.add(new AffixType("new", AffixType.DisplayNamePosition.BEFORE, "", true)));
    }

    @Test
    void size_returnsCorrectCount() {
        assertEquals(0, AffixTypeRegistry.get().size());
        
        AffixTypeRegistry.get().register(
                new AffixType("test", AffixType.DisplayNamePosition.BEFORE, "", true));
        
        assertEquals(1, AffixTypeRegistry.get().size());
    }

    @Test
    void reset_createsNewRegistry() {
        AffixTypeRegistry.get().register(
                new AffixType("test1", AffixType.DisplayNamePosition.BEFORE, "", true));
        AffixTypeRegistry.get().register(
                new AffixType("test2", AffixType.DisplayNamePosition.AFTER, "", false));
        
        AffixTypeRegistry.reset();
        
        assertEquals(0, AffixTypeRegistry.get().size());
        assertFalse(AffixTypeRegistry.get().contains("test1"));
    }
}
