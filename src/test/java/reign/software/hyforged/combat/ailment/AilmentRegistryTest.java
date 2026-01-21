package reign.software.hyforged.combat.ailment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AilmentRegistry}.
 */
@DisplayName("AilmentRegistry")
class AilmentRegistryTest {
    
    private AilmentRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = AilmentRegistry.get();
        registry.clear();
    }
    
    private AilmentDefinition createFireAilment() {
        return AilmentDefinition.builder()
                .id("hyforged:fire-ailment")
                .elementTag("fire")
                .entityEffectId("Burn")
                .build();
    }
    
    private AilmentDefinition createIceAilment() {
        return AilmentDefinition.builder()
                .id("hyforged:ice-ailment")
                .elementTag("ice")
                .entityEffectId("Freeze")
                .build();
    }
    
    @Nested
    @DisplayName("Registration")
    class RegistrationTests {
        
        @Test
        @DisplayName("should register an ailment")
        void shouldRegisterAilment() {
            AilmentDefinition ailment = createFireAilment();
            
            registry.register(ailment);
            
            assertEquals(1, registry.size());
            assertEquals(ailment, registry.getById("hyforged:fire-ailment"));
        }
        
        @Test
        @DisplayName("should register multiple ailments")
        void shouldRegisterMultipleAilments() {
            registry.register(createFireAilment());
            registry.register(createIceAilment());
            
            assertEquals(2, registry.size());
        }
        
        @Test
        @DisplayName("should throw on duplicate element registration")
        void shouldThrowOnDuplicateElement() {
            registry.register(createFireAilment());
            
            AilmentDefinition duplicate = AilmentDefinition.builder()
                    .id("hyforged:fire-ailment-2")
                    .elementTag("fire") // Same element tag
                    .entityEffectId("Burn")
                    .build();
            
            assertThrows(IllegalStateException.class, () -> registry.register(duplicate));
        }
        
        @Test
        @DisplayName("should throw on duplicate id registration")
        void shouldThrowOnDuplicateId() {
            registry.register(createFireAilment());
            
            AilmentDefinition duplicate = AilmentDefinition.builder()
                    .id("hyforged:fire-ailment") // Same id
                    .elementTag("chaos") // Different element
                    .entityEffectId("Burn")
                    .build();
            
            assertThrows(IllegalStateException.class, () -> registry.register(duplicate));
        }
    }
    
    @Nested
    @DisplayName("Lookup")
    class LookupTests {
        
        @Test
        @DisplayName("should get ailment by id")
        void shouldGetById() {
            AilmentDefinition ailment = createFireAilment();
            registry.register(ailment);
            
            AilmentDefinition found = registry.getById("hyforged:fire-ailment");
            
            assertEquals(ailment, found);
        }
        
        @Test
        @DisplayName("should return null for unknown id")
        void shouldReturnNullForUnknownId() {
            assertNull(registry.getById("unknown"));
        }
        
        @Test
        @DisplayName("should get ailment by element tag")
        void shouldGetByElement() {
            AilmentDefinition ailment = createFireAilment();
            registry.register(ailment);
            
            AilmentDefinition found = registry.getByElement("fire");
            
            assertEquals(ailment, found);
        }
        
        @Test
        @DisplayName("should return null for unknown element")
        void shouldReturnNullForUnknownElement() {
            assertNull(registry.getByElement("unknown"));
        }
        
        @Test
        @DisplayName("should check if element has ailment")
        void shouldCheckHasAilmentForElement() {
            registry.register(createFireAilment());
            
            assertTrue(registry.hasAilmentForElement("fire"));
            assertFalse(registry.hasAilmentForElement("ice"));
        }
        
        @Test
        @DisplayName("should get all element tags")
        void shouldGetAllElementTags() {
            registry.register(createFireAilment());
            registry.register(createIceAilment());
            
            Set<String> tags = registry.getAilmentElements();
            
            assertEquals(2, tags.size());
            assertTrue(tags.contains("fire"));
            assertTrue(tags.contains("ice"));
        }
    }
    
    @Nested
    @DisplayName("Clear")
    class ClearTests {
        
        @Test
        @DisplayName("should clear all registrations")
        void shouldClear() {
            registry.register(createFireAilment());
            registry.register(createIceAilment());
            
            registry.clear();
            
            assertEquals(0, registry.size());
            assertNull(registry.getById("hyforged:fire-ailment"));
            assertNull(registry.getByElement("fire"));
        }
    }
    
    @Nested
    @DisplayName("Singleton")
    class SingletonTests {
        
        @Test
        @DisplayName("should return same instance")
        void shouldReturnSameInstance() {
            AilmentRegistry instance1 = AilmentRegistry.get();
            AilmentRegistry instance2 = AilmentRegistry.get();
            
            assertSame(instance1, instance2);
        }
    }
}
