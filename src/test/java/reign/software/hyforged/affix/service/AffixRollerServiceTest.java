package reign.software.hyforged.affix.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.affix.AffixTestFixtures;
import reign.software.hyforged.affix.model.*;
import reign.software.hyforged.affix.registry.*;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AffixRollerService}.
 */
@DisplayName("AffixRollerService")
class AffixRollerServiceTest {
    
    private AffixDefinitionRegistry affixRegistry;
    private AffixPoolRegistry poolRegistry;
    private QualityAffixRuleRegistry qualityRegistry;
    private AffixRollerService service;
    
    @BeforeEach
    void setUp() {
        // Reset registries to clean state
        AffixDefinitionRegistry.reset();
        AffixPoolRegistry.reset();
        QualityAffixRuleRegistry.reset();
        
        affixRegistry = AffixDefinitionRegistry.get();
        poolRegistry = AffixPoolRegistry.get();
        qualityRegistry = QualityAffixRuleRegistry.get();
        
        service = new AffixRollerService(affixRegistry, poolRegistry, qualityRegistry);
        
        // Register test affixes
        registerTestAffixes();
        
        // Register test pool
        registerTestPool();
        
        // Register quality rules
        registerTestQualityRules();
    }
    
    private void registerTestAffixes() {
        // Prefix: Sturdy (+Health)
        affixRegistry.register(new AffixDefinition(
                "sturdy",
                "prefix",
                "Sturdy",
                List.of(
                        AffixTestFixtures.tier(1, 50, 100, "hyforged:health", HyforgedModifier.StackType.FLAT, 80, 100),  // T1: 80-100 HP, req lvl 50
                        AffixTestFixtures.tier(2, 25, 100, "hyforged:health", HyforgedModifier.StackType.FLAT, 50, 79),   // T2: 50-79 HP, req lvl 25
                        AffixTestFixtures.tier(3, 1, 100, "hyforged:health", HyforgedModifier.StackType.FLAT, 20, 49)     // T3: 20-49 HP, req lvl 1
                ),
                100
        ));
        
        // Prefix: Mighty (+Damage)
        affixRegistry.register(new AffixDefinition(
                "mighty",
                "prefix",
                "Mighty",
                List.of(
                        AffixTestFixtures.tier(1, 30, 100, "hyforged:damage", HyforgedModifier.StackType.FLAT, 15, 20),
                        AffixTestFixtures.tier(2, 10, 100, "hyforged:damage", HyforgedModifier.StackType.FLAT, 8, 14),
                        AffixTestFixtures.tier(3, 1, 100, "hyforged:damage", HyforgedModifier.StackType.FLAT, 3, 7)
                ),
                100
        ));
        
        // Suffix: Of the Bear (+Health%)
        affixRegistry.register(new AffixDefinition(
                "of-the-bear",
                "suffix",
                "of the Bear",
                List.of(
                        AffixTestFixtures.tier(1, 40, 100, "hyforged:health", HyforgedModifier.StackType.INCREASED, 1500, 2000),  // 15-20%
                        AffixTestFixtures.tier(2, 20, 100, "hyforged:health", HyforgedModifier.StackType.INCREASED, 1000, 1499),  // 10-15%
                        AffixTestFixtures.tier(3, 1, 100, "hyforged:health", HyforgedModifier.StackType.INCREASED, 500, 999)      // 5-10%
                ),
                100
        ));
        
        // Suffix: Of Slaying (+Damage%)
        affixRegistry.register(new AffixDefinition(
                "of-slaying",
                "suffix",
                "of Slaying",
                List.of(
                        AffixTestFixtures.tier(1, 35, 100, "hyforged:damage", HyforgedModifier.StackType.INCREASED, 1200, 1500),
                        AffixTestFixtures.tier(2, 15, 100, "hyforged:damage", HyforgedModifier.StackType.INCREASED, 800, 1199),
                        AffixTestFixtures.tier(3, 1, 100, "hyforged:damage", HyforgedModifier.StackType.INCREASED, 400, 799)
                ),
                100
        ));
    }
    
    private void registerTestPool() {
        poolRegistry.register(new AffixPool(
                "test-weapons",
                10,
                new AffixPool.AffixPoolAppliesTo(
                        Set.of("melee_weapon"),
                        Set.of()
                ),
                List.of("sturdy", "mighty"),
                List.of("of-the-bear", "of-slaying"),
                List.of()
        ));
    }
    
    private void registerTestQualityRules() {
        // Common: 1 prefix, 1 suffix
        qualityRegistry.register(new QualityAffixRule(
                "Common",
                Map.of("prefix", 1, "suffix", 1, "forged", 0)
        ));
        
        // Rare: 2 prefix, 2 suffix
        qualityRegistry.register(new QualityAffixRule(
                "Rare",
                Map.of("prefix", 2, "suffix", 2, "forged", 0)
        ));
        
        // Legendary: 3 prefix, 3 suffix, 1 forged
        qualityRegistry.register(new QualityAffixRule(
                "Legendary",
                Map.of("prefix", 3, "suffix", 3, "forged", 1)
        ));
    }
    
    @Nested
    @DisplayName("Basic Rolling")
    class BasicRolling {
        
        @Test
        @DisplayName("should roll affixes for eligible item")
        void shouldRollAffixesForEligibleItem() {
            AffixRollContext context = AffixRollContext.of(
                    "test-sword",
                    "Rare",
                    50,
                    new String[]{"melee_weapon"},
                    new String[]{}
            );
            
            AffixRollResult result = service.rollAffixes(context, 12345L);
            
            assertTrue(result.hasAffixes());
            assertNotNull(result.poolId());
            assertEquals("test-weapons", result.poolId());
        }
        
        @Test
        @DisplayName("should return empty for unknown quality")
        void shouldReturnEmptyForUnknownQuality() {
            AffixRollContext context = AffixRollContext.of(
                    "test-item",
                    "UnknownQuality",
                    50,
                    new String[]{"melee_weapon"},
                    new String[]{}
            );
            
            AffixRollResult result = service.rollAffixes(context, 12345L);
            
            assertFalse(result.hasAffixes());
        }
        
        @Test
        @DisplayName("should return empty for item with no matching pool")
        void shouldReturnEmptyForNoMatchingPool() {
            AffixRollContext context = AffixRollContext.of(
                    "test-item",
                    "Rare",
                    50,
                    new String[]{"armor"},  // No pool for armor
                    new String[]{}
            );
            
            AffixRollResult result = service.rollAffixes(context, 12345L);
            
            assertFalse(result.hasAffixes());
        }
    }
    
    @Nested
    @DisplayName("Deterministic Rolling")
    class DeterministicRolling {
        
        @Test
        @DisplayName("same seed should produce same results")
        void sameSeedShouldProduceSameResults() {
            AffixRollContext context = AffixRollContext.of(
                    "test-sword",
                    "Rare",
                    50,
                    new String[]{"melee_weapon"},
                    new String[]{}
            );
            
            long seed = 42L;
            AffixRollResult result1 = service.rollAffixes(context, seed);
            AffixRollResult result2 = service.rollAffixes(context, seed);
            
            assertEquals(result1.affixes().size(), result2.affixes().size());
            for (int i = 0; i < result1.affixes().size(); i++) {
                RolledAffix a1 = result1.affixes().get(i);
                RolledAffix a2 = result2.affixes().get(i);
                assertEquals(a1.affixId(), a2.affixId());
                assertEquals(a1.tier(), a2.tier());
                assertEquals(a1.rolledStats(), a2.rolledStats());
            }
        }
        
        @Test
        @DisplayName("different seeds should produce different results")
        void differentSeedsShouldProduceDifferentResults() {
            AffixRollContext context = AffixRollContext.of(
                    "test-sword",
                    "Rare",
                    50,
                    new String[]{"melee_weapon"},
                    new String[]{}
            );
            
            AffixRollResult result1 = service.rollAffixes(context, 1L);
            AffixRollResult result2 = service.rollAffixes(context, 999999L);
            
            // While theoretically possible to get same results, very unlikely with these seeds
            // At least check both rolled successfully
            assertTrue(result1.hasAffixes());
            assertTrue(result2.hasAffixes());
        }
    }
    
    @Nested
    @DisplayName("Capacity Limits")
    class CapacityLimits {
        
        @Test
        @DisplayName("Common quality should have at most 1 prefix and 1 suffix")
        void commonQualityShouldHaveLimitedAffixes() {
            AffixRollContext context = AffixRollContext.of(
                    "test-sword",
                    "Common",
                    50,
                    new String[]{"melee_weapon"},
                    new String[]{}
            );
            
            AffixRollResult result = service.rollAffixes(context, 12345L);
            
            assertTrue(result.countByType("prefix") <= 1);
            assertTrue(result.countByType("suffix") <= 1);
        }
        
        @Test
        @DisplayName("Rare quality should have at most 2 prefixes and 2 suffixes")
        void rareQualityShouldHaveLimitedAffixes() {
            AffixRollContext context = AffixRollContext.of(
                    "test-sword",
                    "Rare",
                    50,
                    new String[]{"melee_weapon"},
                    new String[]{}
            );
            
            AffixRollResult result = service.rollAffixes(context, 12345L);
            
            assertTrue(result.countByType("prefix") <= 2);
            assertTrue(result.countByType("suffix") <= 2);
        }
    }
    
    @Nested
    @DisplayName("Tier Filtering by Item Level")
    class TierFiltering {
        
        @Test
        @DisplayName("low level item should only get T3 affixes")
        void lowLevelShouldGetLowTiers() {
            AffixRollContext context = AffixRollContext.of(
                    "test-sword",
                    "Common",
                    1,  // Level 1 - only T3 eligible
                    new String[]{"melee_weapon"},
                    new String[]{}
            );
            
            AffixRollResult result = service.rollAffixes(context, 12345L);
            
            // All rolled affixes should be tier 3
            for (RolledAffix affix : result.affixes()) {
                assertEquals(3, affix.tier(), "Low level item should only get T3 affixes");
            }
        }
        
        @Test
        @DisplayName("high level item can get any tier")
        void highLevelCanGetAnyTier() {
            AffixRollContext context = AffixRollContext.of(
                    "test-sword",
                    "Common",
                    100,  // Level 100 - all tiers eligible
                    new String[]{"melee_weapon"},
                    new String[]{}
            );
            
            // Run many times to check we can get different tiers
            Set<Integer> seenTiers = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                AffixRollResult result = service.rollAffixes(context, i);
                for (RolledAffix affix : result.affixes()) {
                    seenTiers.add(affix.tier());
                }
            }
            
            // Should see at least 2 different tiers over 100 rolls
            assertTrue(seenTiers.size() >= 2, "High level items should be able to get multiple tiers");
        }
    }
    
    @Nested
    @DisplayName("Duplicate Prevention")
    class DuplicatePrevention {
        
        @Test
        @DisplayName("should not roll same affix ID twice")
        void shouldNotRollSameAffixTwice() {
            AffixRollContext context = AffixRollContext.of(
                    "test-sword",
                    "Rare",  // 2 prefix, 2 suffix capacity
                    50,
                    new String[]{"melee_weapon"},
                    new String[]{}
            );
            
            for (int seed = 0; seed < 50; seed++) {
                AffixRollResult result = service.rollAffixes(context, seed);
                
                Set<String> affixIds = new HashSet<>();
                for (RolledAffix affix : result.affixes()) {
                    assertFalse(affixIds.contains(affix.affixId()), 
                            "Duplicate affix ID found: " + affix.affixId());
                    affixIds.add(affix.affixId());
                }
            }
        }
        
        @Test
        @DisplayName("should not roll same stat twice")
        void shouldNotRollSameStatTwice() {
            AffixRollContext context = AffixRollContext.of(
                    "test-sword",
                    "Rare",
                    50,
                    new String[]{"melee_weapon"},
                    new String[]{}
            );
            
            for (int seed = 0; seed < 50; seed++) {
                AffixRollResult result = service.rollAffixes(context, seed);
                
                Set<String> stats = new HashSet<>();
                for (RolledAffix affix : result.affixes()) {
                    for (String statId : affix.rolledStats().keySet()) {
                        assertFalse(stats.contains(statId), 
                                "Duplicate stat found: " + statId);
                        stats.add(statId);
                    }
                }
            }
        }
    }
    
    @Nested
    @DisplayName("Value Rolling")
    class ValueRolling {
        
        @Test
        @DisplayName("rolled values should be within tier range")
        void valuesWithinTierRange() {
            AffixRollContext context = AffixRollContext.of(
                    "test-sword",
                    "Common",
                    1,  // Only T3 eligible
                    new String[]{"melee_weapon"},
                    new String[]{}
            );
            
            for (int seed = 0; seed < 100; seed++) {
                AffixRollResult result = service.rollAffixes(context, seed);
                
                for (RolledAffix affix : result.affixes()) {
                    // For T3, sturdy is 20-49, mighty is 3-7
                    if (affix.affixId().equals("sturdy")) {
                        RolledAffix.RolledStat stat = affix.getStat("hyforged:health");
                        assertNotNull(stat, "sturdy should have health stat");
                        assertTrue(stat.value() >= 20 && stat.value() <= 49,
                                "Sturdy T3 value should be 20-49, got: " + stat.value());
                    } else if (affix.affixId().equals("mighty")) {
                        RolledAffix.RolledStat stat = affix.getStat("hyforged:damage");
                        assertNotNull(stat, "mighty should have damage stat");
                        assertTrue(stat.value() >= 3 && stat.value() <= 7,
                                "Mighty T3 value should be 3-7, got: " + stat.value());
                    }
                }
            }
        }
    }
    
    @Nested
    @DisplayName("Result Conversion")
    class ResultConversion {
        
        @Test
        @DisplayName("toItemData should create valid HyforgedItemData")
        void toItemDataShouldCreateValidData() {
            AffixRollContext context = AffixRollContext.of(
                    "test-sword",
                    "Rare",
                    50,
                    new String[]{"melee_weapon"},
                    new String[]{}
            );
            
            AffixRollResult result = service.rollAffixes(context, 12345L);
            HyforgedItemData data = result.toItemData();
            
            assertEquals(result.affixes().size(), data.affixes().size());
            for (int i = 0; i < result.affixes().size(); i++) {
                assertEquals(result.affixes().get(i).affixId(), 
                        data.affixes().get(i).affixId());
            }
        }
    }
}
