package reign.software.hyforged.affix.api;

import org.junit.jupiter.api.*;
import reign.software.hyforged.affix.AffixTestFixtures;
import reign.software.hyforged.affix.model.*;
import reign.software.hyforged.affix.registry.*;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AffixService}.
 */
@DisplayName("AffixService")
class AffixServiceTest {
    
    @BeforeEach
    void setUp() {
        // Reset all registries before each test
        AffixService.reset();
        AffixDefinitionRegistry.reset();
        AffixPoolRegistry.reset();
        AffixTypeRegistry.reset();
        QualityAffixRuleRegistry.reset();
        
        // Register test data
        registerTestTypes();
        registerTestAffixes();
        registerTestPools();
        registerTestQualityRules();
    }
    
    private void registerTestTypes() {
        AffixTypeRegistry registry = AffixTypeRegistry.get();
        registry.register(new AffixType("prefix", AffixType.DisplayNamePosition.BEFORE, "{name}", true));
        registry.register(new AffixType("suffix", AffixType.DisplayNamePosition.AFTER, "{name}", true));
        registry.register(new AffixType("forged", AffixType.DisplayNamePosition.NONE, "{name}", false));
    }
    
    private void registerTestAffixes() {
        AffixDefinitionRegistry registry = AffixDefinitionRegistry.get();
        
        // Sturdy prefix - armor buff
        registry.register(new AffixDefinition(
                "sturdy",
                "prefix",
                "Sturdy",
                List.of(
                        AffixTestFixtures.tier(1, 40, 50, "hyforged:armor", HyforgedModifier.StackType.FLAT, 50, 75),
                        AffixTestFixtures.tier(2, 20, 100, "hyforged:armor", HyforgedModifier.StackType.FLAT, 30, 49),
                        AffixTestFixtures.tier(3, 1, 150, "hyforged:armor", HyforgedModifier.StackType.FLAT, 15, 29)
                ),
                100
        ));
        
        // Mighty prefix - strength buff
        registry.register(new AffixDefinition(
                "mighty",
                "prefix",
                "Mighty",
                List.of(
                        AffixTestFixtures.tier(1, 40, 50, "hyforged:strength", HyforgedModifier.StackType.FLAT, 8, 10),
                        AffixTestFixtures.tier(2, 20, 100, "hyforged:strength", HyforgedModifier.StackType.FLAT, 5, 7),
                        AffixTestFixtures.tier(3, 1, 150, "hyforged:strength", HyforgedModifier.StackType.FLAT, 2, 4)
                ),
                100
        ));
        
        // Of the Bear suffix - strength buff
        registry.register(new AffixDefinition(
                "of-the-bear",
                "suffix",
                "of the Bear",
                List.of(
                        AffixTestFixtures.tier(1, 40, 50, "hyforged:strength", HyforgedModifier.StackType.FLAT, 10, 12),
                        AffixTestFixtures.tier(2, 20, 100, "hyforged:strength", HyforgedModifier.StackType.FLAT, 6, 9),
                        AffixTestFixtures.tier(3, 1, 150, "hyforged:strength", HyforgedModifier.StackType.FLAT, 3, 5)
                ),
                100
        ));
    }
    
    private void registerTestPools() {
        AffixPoolRegistry registry = AffixPoolRegistry.get();
        registry.register(AffixPool.of(
                "test-pool",
                100,  // priority
                new AffixPool.AffixPoolAppliesTo(
                        Set.of("equipment"),
                        Set.of()
                ),
                List.of("sturdy", "mighty"),   // prefixes
                List.of("of-the-bear"),         // suffixes
                List.of()                        // forged
        ));
    }
    
    private void registerTestQualityRules() {
        QualityAffixRuleRegistry registry = QualityAffixRuleRegistry.get();
        registry.register(new QualityAffixRule("Rare", java.util.Map.of(
                "prefix", 2,
                "suffix", 2,
                "forged", 0
        )));
    }
    
    @Nested
    @DisplayName("Singleton")
    class SingletonTests {
        
        @Test
        @DisplayName("get() should return same instance")
        void getShouldReturnSameInstance() {
            AffixService service1 = AffixService.get();
            AffixService service2 = AffixService.get();
            
            assertSame(service1, service2);
        }
        
        @Test
        @DisplayName("reset() should create new instance")
        void resetShouldCreateNewInstance() {
            AffixService service1 = AffixService.get();
            AffixService.reset();
            AffixService service2 = AffixService.get();
            
            assertNotSame(service1, service2);
        }
    }
    
    @Nested
    @DisplayName("Query Methods")
    class QueryMethodsTests {
        
        @Test
        @DisplayName("getAffixDefinition should return definition")
        void getAffixDefinitionShouldReturnDefinition() {
            AffixService service = AffixService.get();
            
            AffixDefinition sturdy = service.getAffixDefinition("sturdy");
            
            assertNotNull(sturdy);
            assertEquals("sturdy", sturdy.id());
            assertEquals("prefix", sturdy.type());
        }
        
        @Test
        @DisplayName("getAffixDefinition should return null for unknown")
        void getAffixDefinitionShouldReturnNullForUnknown() {
            AffixService service = AffixService.get();
            
            assertNull(service.getAffixDefinition("unknown-affix"));
        }
        
        @Test
        @DisplayName("getAffixType should return type")
        void getAffixTypeShouldReturnType() {
            AffixService service = AffixService.get();
            
            AffixType prefix = service.getAffixType("prefix");
            
            assertNotNull(prefix);
            assertEquals("prefix", prefix.id());
            assertEquals(AffixType.DisplayNamePosition.BEFORE, prefix.displayNamePosition());
        }
        
        @Test
        @DisplayName("getAllAffixIds should return all registered IDs")
        void getAllAffixIdsShouldReturnAllIds() {
            AffixService service = AffixService.get();
            
            Set<String> ids = service.getAllAffixIds();
            
            assertEquals(3, ids.size());
            assertTrue(ids.contains("sturdy"));
            assertTrue(ids.contains("mighty"));
            assertTrue(ids.contains("of-the-bear"));
        }
        
        @Test
        @DisplayName("getAllTypeIds should return all type IDs")
        void getAllTypeIdsShouldReturnAllTypeIds() {
            AffixService service = AffixService.get();
            
            Set<String> ids = service.getAllTypeIds();
            
            assertEquals(3, ids.size());
            assertTrue(ids.contains("prefix"));
            assertTrue(ids.contains("suffix"));
            assertTrue(ids.contains("forged"));
        }
        
        @Test
        @DisplayName("getAllPoolIds should return all pool IDs")
        void getAllPoolIdsShouldReturnAllPoolIds() {
            AffixService service = AffixService.get();
            
            Set<String> ids = service.getAllPoolIds();
            
            assertEquals(1, ids.size());
            assertTrue(ids.contains("test-pool"));
        }
    }
    
    @Nested
    @DisplayName("Registration Methods")
    class RegistrationMethodsTests {
        
        @Test
        @DisplayName("registerAffix should add new affix")
        void registerAffixShouldAddNewAffix() {
            AffixService service = AffixService.get();
            
            AffixDefinition newAffix = new AffixDefinition(
                    "sharp",
                    "prefix",
                    "Sharp",
                    List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:attack", HyforgedModifier.StackType.FLAT, 10, 20)),
                    100
            );
            
            service.registerAffix(newAffix);
            
            AffixDefinition retrieved = service.getAffixDefinition("sharp");
            assertNotNull(retrieved);
            assertEquals("sharp", retrieved.id());
        }
        
        @Test
        @DisplayName("registerPool should add new pool")
        void registerPoolShouldAddNewPool() {
            AffixService service = AffixService.get();
            
            AffixPool newPool = AffixPool.of(
                    "weapons-pool",
                    200,  // priority
                    new AffixPool.AffixPoolAppliesTo(
                            Set.of("weapons"),
                            Set.of()
                    ),
                    List.of("mighty"),   // prefixes
                    List.of(),           // suffixes
                    List.of()            // forged
            );
            
            service.registerPool(newPool);
            
            Set<String> poolIds = service.getAllPoolIds();
            assertTrue(poolIds.contains("weapons-pool"));
        }
        
        @Test
        @DisplayName("registerType should add new type")
        void registerTypeShouldAddNewType() {
            AffixService service = AffixService.get();
            
            AffixType newType = new AffixType(
                    "enchant",
                    AffixType.DisplayNamePosition.NONE,
                    "Enchanted: {name}",
                    true
            );
            
            service.registerType(newType);
            
            AffixType retrieved = service.getAffixType("enchant");
            assertNotNull(retrieved);
            assertEquals("enchant", retrieved.id());
        }
    }
    
    @Nested
    @DisplayName("AffixSpec Integration")
    class AffixSpecIntegrationTests {
        
        @Test
        @DisplayName("should create spec for registered affix")
        void shouldCreateSpecForRegisteredAffix() {
            AffixService service = AffixService.get();
            
            // Verify affix exists
            assertNotNull(service.getAffixDefinition("sturdy"));
            
            // Create spec
            AffixSpec spec = AffixSpec.of("sturdy", 2, 35);
            
            assertEquals("sturdy", spec.affixId());
            assertEquals(2, spec.requireTier());
            assertEquals(35, spec.requireValue());
        }
        
        @Test
        @DisplayName("should validate affix ID exists")
        void shouldValidateAffixIdExists() {
            AffixService service = AffixService.get();
            
            // Can create spec for any ID (validation happens at usage)
            AffixSpec spec = AffixSpec.of("nonexistent");
            assertNotNull(spec);
            
            // But lookup will return null
            assertNull(service.getAffixDefinition("nonexistent"));
        }
    }
}
