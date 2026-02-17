package reign.software.hyforged.affix.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.affix.AffixTestFixtures;
import reign.software.hyforged.affix.event.AffixesRolledEvent;
import reign.software.hyforged.affix.model.*;
import reign.software.hyforged.affix.registry.*;
import reign.software.hyforged.affix.service.AffixRollContext;
import reign.software.hyforged.affix.service.AffixRollerService;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LootAffixSystem} and related event classes.
 * <p>
 * Note: Full integration testing requires a running Hytale server context.
 * These tests focus on the data models and event structures.
 */
@DisplayName("LootAffixSystem")
class LootAffixSystemTest {
    
    private AffixDefinitionRegistry affixRegistry;
    private AffixPoolRegistry poolRegistry;
    private QualityAffixRuleRegistry qualityRegistry;
    private AffixRollerService rollerService;
    
    @BeforeEach
    void setUp() {
        // Reset registries
        AffixDefinitionRegistry.reset();
        AffixPoolRegistry.reset();
        QualityAffixRuleRegistry.reset();
        
        affixRegistry = AffixDefinitionRegistry.get();
        poolRegistry = AffixPoolRegistry.get();
        qualityRegistry = QualityAffixRuleRegistry.get();
        
        rollerService = new AffixRollerService(affixRegistry, poolRegistry, qualityRegistry);
        
        // Register test data
        registerTestAffixes();
        registerTestPool();
        registerTestQualityRules();
    }
    
    private void registerTestAffixes() {
        affixRegistry.register(new AffixDefinition(
                "sturdy",
                "prefix",
                "Sturdy",
                List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:health", HyforgedModifier.StackType.FLAT, 50, 100)),
                100
        ));
        
        affixRegistry.register(new AffixDefinition(
                "swift",
                "suffix",
                "of Swiftness",
                List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:movementSpeed", HyforgedModifier.StackType.INCREASED, 5, 10)),
                100
        ));
    }
    
    private void registerTestPool() {
        poolRegistry.register(AffixPool.of(
                "equipment_pool",
                AffixPool.DEFAULT_PRIORITY,
                new AffixPool.AffixPoolAppliesTo(Set.of("equipment"), Set.of("weapon")),
                List.of("sturdy"),
                List.of("swift"),
                List.of()
        ));
    }
    
    private void registerTestQualityRules() {
        // Magic: 1-2 prefix, 1 suffix
        qualityRegistry.register(new QualityAffixRule(
                "Magic",
                Map.of("prefix", 2, "suffix", 1)
        ));
        // Rare: 2-3 prefix, 2-3 suffix
        qualityRegistry.register(new QualityAffixRule(
                "Rare",
                Map.of("prefix", 3, "suffix", 3)
        ));
    }
    
    @Nested
    @DisplayName("AffixesRolledEvent")
    class AffixesRolledEventTests {
        
        @Test
        @DisplayName("event should contain all rolled affixes")
        void eventContainsAllAffixes() {
            AffixRollContext context = AffixRollContext.of(
                    "test_sword",
                    "Rare",
                    10,
                    new String[]{"equipment"},
                    new String[]{"weapon"}
            );
            
            Map<String, RolledAffix.RolledStat> sturdyStats = new HashMap<>();
            sturdyStats.put("hyforged:health", new RolledAffix.RolledStat(75, HyforgedModifier.StackType.FLAT));
            Map<String, RolledAffix.RolledStat> swiftStats = new HashMap<>();
            swiftStats.put("hyforged:movementSpeed", new RolledAffix.RolledStat(7, HyforgedModifier.StackType.INCREASED));
            
            List<RolledAffix> affixes = List.of(
                    new RolledAffix("sturdy", "prefix", 1, sturdyStats),
                    new RolledAffix("swift", "suffix", 1, swiftStats)
            );
            
            AffixesRolledEvent event = new AffixesRolledEvent(
                    context,
                    "equipment_pool",
                    affixes,
                    12345L
            );
            
            assertEquals("test_sword", event.getItemId());
            assertEquals("equipment_pool", event.getPoolId());
            assertEquals(2, event.getAffixes().size());
            assertEquals(12345L, event.getSeed());
            assertEquals("Rare", event.getQuality());
            assertEquals(10, event.getItemLevel());
        }
        
        @Test
        @DisplayName("event affixes list should be immutable")
        void eventAffixesAreImmutable() {
            AffixRollContext context = AffixRollContext.of(
                    "test_item", "Magic", 1, new String[]{"equipment"}, new String[]{}
            );
            
            Map<String, RolledAffix.RolledStat> sturdyStats = new HashMap<>();
            sturdyStats.put("hyforged:health", new RolledAffix.RolledStat(50, HyforgedModifier.StackType.FLAT));
            
            List<RolledAffix> mutableList = new java.util.ArrayList<>();
            mutableList.add(new RolledAffix("sturdy", "prefix", 1, sturdyStats));
            
            AffixesRolledEvent event = new AffixesRolledEvent(
                    context, "pool", mutableList, 0
            );
            
            // Modifying original list should not affect event
            mutableList.clear();
            assertEquals(1, event.getAffixes().size());
            
            // Event's list should be immutable
            assertThrows(UnsupportedOperationException.class, () -> 
                    event.getAffixes().add(null));
        }
        
        @Test
        @DisplayName("event should expose context properties")
        void eventExposesContextProperties() {
            AffixRollContext context = AffixRollContext.of(
                    "legendary_sword",
                    "Legendary",
                    50,
                    new String[]{"equipment", "weapon"},
                    new String[]{"sword", "twohanded"}
            );
            
            AffixesRolledEvent event = new AffixesRolledEvent(
                    context, "pool", List.of(), 0
            );
            
            assertEquals("legendary_sword", event.getItemId());
            assertEquals("Legendary", event.getQuality());
            assertEquals(50, event.getItemLevel());
            assertSame(context, event.getContext());
        }
        
        @Test
        @DisplayName("event should reject null parameters")
        void eventRejectsNulls() {
            AffixRollContext context = AffixRollContext.of(
                    "item", "Common", 1, new String[]{}, new String[]{}
            );
            
            assertThrows(NullPointerException.class, () ->
                    new AffixesRolledEvent(null, "pool", List.of(), 0));
            assertThrows(NullPointerException.class, () ->
                    new AffixesRolledEvent(context, null, List.of(), 0));
            assertThrows(NullPointerException.class, () ->
                    new AffixesRolledEvent(context, "pool", null, 0));
        }
    }
    
    @Nested
    @DisplayName("LootAffixSystem construction")
    class LootAffixSystemConstructionTests {
        
        @Test
        @DisplayName("system should be constructable with custom services")
        void customServicesConstructor() {
            LootAffixSystem system = new LootAffixSystem(
                    rollerService,
                    poolRegistry
            );
            assertNotNull(system);
        }
        
        // Note: Tests that access ItemComponent.getComponentType() require 
        // Hytale server context and are skipped in unit tests.
        // Full integration tests would run these in a server environment.
    }
    
    @Nested
    @DisplayName("AffixRollContext for loot")
    class AffixRollContextTests {
        
        @Test
        @DisplayName("context factory method creates valid context")
        void contextCreation() {
            AffixRollContext context = AffixRollContext.of(
                    "iron_sword",
                    "Magic",
                    15,
                    new String[]{"equipment"},
                    new String[]{"sword", "weapon"}
            );
            
            assertEquals("iron_sword", context.itemId());
            assertEquals("Magic", context.quality());
            assertEquals(15, context.itemLevel());
            assertArrayEquals(new String[]{"equipment"}, context.itemCategories());
            assertArrayEquals(new String[]{"sword", "weapon"}, context.itemTags());
            assertEquals(0, context.tierWeightBonus());
        }
        
        @Test
        @DisplayName("context with tier bonus")
        void contextWithTierBonus() {
            AffixRollContext context = AffixRollContext.withBonus(
                    "rare_drop",
                    "Rare",
                    20,
                    new String[]{"equipment"},
                    new String[]{"armor"},
                    25  // +25% tier bonus from boss
            );
            
            assertEquals(25, context.tierWeightBonus());
        }
    }
    
    @Nested
    @DisplayName("Integration with roller service")
    class RollerServiceIntegrationTests {
        
        @Test
        @DisplayName("roller produces affixes for loot context")
        void rollerProducesValidAffixes() {
            AffixRollContext context = AffixRollContext.of(
                    "test_weapon",
                    "Magic",
                    10,
                    new String[]{"equipment"},
                    new String[]{"weapon"}
            );
            
            var result = rollerService.rollAffixes(context, new java.util.Random(42));
            
            // With only 1 prefix and 1 suffix in test pool, we get at most 2 affixes
            // Magic quality: up to 2 prefix, 1 suffix (but limited by pool)
            assertTrue(result.hasAffixes());
            assertTrue(result.affixCount() >= 1);
            assertTrue(result.affixCount() <= 2);
        }
        
        @Test
        @DisplayName("roller respects quality capacity limits")
        void rollerRespectsQualityLimits() {
            AffixRollContext context = AffixRollContext.of(
                    "magic_item",
                    "Magic",
                    5,
                    new String[]{"equipment"},
                    new String[]{"weapon"}
            );
            
            var result = rollerService.rollAffixes(context, new java.util.Random(123));
            
            // Magic quality: up to 2 prefixes, 1 suffix (limited by pool to 1 each)
            long prefixCount = result.affixes().stream()
                    .filter(a -> "prefix".equals(a.type()))
                    .count();
            long suffixCount = result.affixes().stream()
                    .filter(a -> "suffix".equals(a.type()))
                    .count();
            
            assertTrue(prefixCount <= 1); // Pool only has 1 prefix
            assertTrue(suffixCount <= 1); // Pool only has 1 suffix
        }
    }
}
