package reign.software.hyforged.affix.integration;

import org.junit.jupiter.api.*;
import reign.software.hyforged.affix.api.AffixService;
import reign.software.hyforged.affix.api.AffixSpec;
import reign.software.hyforged.affix.model.*;
import reign.software.hyforged.affix.registry.*;
import reign.software.hyforged.affix.service.*;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete affix system flow.
 * <p>
 * These tests verify end-to-end behavior across multiple components.
 */
class AffixSystemIntegrationTest {
    
    private AffixDefinitionRegistry affixRegistry;
    private AffixTypeRegistry typeRegistry;
    private AffixPoolRegistry poolRegistry;
    private QualityAffixRuleRegistry qualityRegistry;
    private AffixRollerService rollerService;
    
    // Stat IDs
    private static final StatId ARMOR = StatId.hyforged("armor");
    private static final StatId DAMAGE = StatId.hyforged("damage");
    private static final StatId HEALTH = StatId.hyforged("health");
    private static final StatId MOVEMENT_SPEED = StatId.hyforged("movementSpeed");
    
    @BeforeEach
    void setUp() {
        // Reset all singletons for isolated testing
        AffixDefinitionRegistry.reset();
        AffixTypeRegistry.reset();
        AffixPoolRegistry.reset();
        QualityAffixRuleRegistry.reset();
        AffixService.reset();
        AffixMetrics.get().reset();
        
        affixRegistry = AffixDefinitionRegistry.get();
        typeRegistry = AffixTypeRegistry.get();
        poolRegistry = AffixPoolRegistry.get();
        qualityRegistry = QualityAffixRuleRegistry.get();
        rollerService = new AffixRollerService(affixRegistry, poolRegistry, qualityRegistry);
        
        // Set up test data
        setUpTestTypes();
        setUpTestAffixes();
        setUpTestPool();
        setUpQualityRules();
    }
    
    private void setUpTestTypes() {
        typeRegistry.register(new AffixType("prefix", AffixType.DisplayNamePosition.BEFORE, "{name}", true));
        typeRegistry.register(new AffixType("suffix", AffixType.DisplayNamePosition.AFTER, "{name}", true));
        typeRegistry.register(new AffixType("forged", AffixType.DisplayNamePosition.NONE, "{name}", false));
    }
    
    private void setUpTestAffixes() {
        // Prefix: Sturdy (+armor)
        affixRegistry.register(new AffixDefinition(
                "sturdy",
                "prefix",
                "Sturdy",
                ARMOR,
                HyforgedModifier.StackType.FLAT,
                List.of(
                        new AffixTierDefinition(1, 15, 20, 10),
                        new AffixTierDefinition(2, 10, 14, 20),
                        new AffixTierDefinition(3, 5, 9, 30)
                ),
                AffixEligibility.ANY,
                1000
        ));
        
        // Prefix: Sharp (+damage)
        affixRegistry.register(new AffixDefinition(
                "sharp",
                "prefix",
                "Sharp",
                DAMAGE,
                HyforgedModifier.StackType.FLAT,
                List.of(
                        new AffixTierDefinition(1, 8, 12, 10),
                        new AffixTierDefinition(2, 4, 7, 20),
                        new AffixTierDefinition(3, 1, 3, 30)
                ),
                AffixEligibility.ANY,
                1000
        ));
        
        // Suffix: Of the Bear (+health)
        affixRegistry.register(new AffixDefinition(
                "of-the-bear",
                "suffix",
                "of the Bear",
                HEALTH,
                HyforgedModifier.StackType.FLAT,
                List.of(
                        new AffixTierDefinition(1, 50, 75, 10),
                        new AffixTierDefinition(2, 25, 49, 20),
                        new AffixTierDefinition(3, 10, 24, 30)
                ),
                AffixEligibility.ANY,
                1000
        ));
        
        // Suffix: Of Speed (+movement_speed as percentage)
        affixRegistry.register(new AffixDefinition(
                "of-speed",
                "suffix",
                "of Speed",
                MOVEMENT_SPEED,
                HyforgedModifier.StackType.INCREASED,
                List.of(
                        new AffixTierDefinition(1, 1500, 2000, 15), // 15-20%
                        new AffixTierDefinition(2, 1000, 1499, 25)  // 10-14.99%
                ),
                AffixEligibility.ANY,
                800
        ));
    }
    
    private void setUpTestPool() {
        poolRegistry.register(new AffixPool(
                "equipment-pool",
                100,
                new AffixPool.AffixPoolAppliesTo(Set.of("equipment"), Set.of()),
                List.of("sturdy", "sharp"),
                List.of("of-the-bear", "of-speed"),
                List.of()
        ));
    }
    
    private void setUpQualityRules() {
        qualityRegistry.register(new QualityAffixRule("Common", Map.of("prefix", 0, "suffix", 0, "forged", 0)));
        qualityRegistry.register(new QualityAffixRule("Uncommon", Map.of("prefix", 1, "suffix", 0, "forged", 0)));
        qualityRegistry.register(new QualityAffixRule("Rare", Map.of("prefix", 1, "suffix", 1, "forged", 0)));
        qualityRegistry.register(new QualityAffixRule("Epic", Map.of("prefix", 2, "suffix", 1, "forged", 0)));
        qualityRegistry.register(new QualityAffixRule("Legendary", Map.of("prefix", 2, "suffix", 2, "forged", 1)));
    }
    
    // =========================================================================
    // End-to-End Rolling Tests
    // =========================================================================
    
    @Test
    @DisplayName("End-to-end: Common quality should roll no affixes")
    void commonQuality_shouldRollNoAffixes() {
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "Common",
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        AffixRollResult result = rollerService.rollAffixes(context, 12345L);
        
        assertFalse(result.hasAffixes());
        assertEquals(0, result.affixCount());
    }
    
    @Test
    @DisplayName("End-to-end: Rare quality should roll 1 prefix + 1 suffix")
    void rareQuality_shouldRollOneOfEach() {
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "Rare",
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        AffixRollResult result = rollerService.rollAffixes(context, 12345L);
        
        assertTrue(result.hasAffixes());
        assertEquals(2, result.affixCount());
        assertEquals(1, result.countByType("prefix"));
        assertEquals(1, result.countByType("suffix"));
    }
    
    @Test
    @DisplayName("End-to-end: Epic quality should roll up to 2 prefixes + 1 suffix")
    void epicQuality_shouldRollCorrectDistribution() {
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "Epic",
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        AffixRollResult result = rollerService.rollAffixes(context, 12345L);
        
        assertTrue(result.hasAffixes());
        // Epic allows up to 2 prefix + 1 suffix = 3, but pool availability may limit this
        assertTrue(result.affixCount() >= 2 && result.affixCount() <= 3, 
                "Epic should roll 2-3 affixes, got: " + result.affixCount());
        assertTrue(result.countByType("prefix") <= 2, "Max 2 prefixes allowed");
        assertTrue(result.countByType("suffix") <= 1, "Max 1 suffix allowed");
    }
    
    @Test
    @DisplayName("Deterministic rolling: same seed produces same results")
    void deterministic_sameSeedSameResults() {
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "Epic",
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        long seed = 99999L;
        
        AffixRollResult result1 = rollerService.rollAffixes(context, seed);
        AffixRollResult result2 = rollerService.rollAffixes(context, seed);
        
        assertEquals(result1.affixCount(), result2.affixCount());
        assertEquals(result1.affixes().size(), result2.affixes().size());
        
        for (int i = 0; i < result1.affixes().size(); i++) {
            RolledAffix a1 = result1.affixes().get(i);
            RolledAffix a2 = result2.affixes().get(i);
            assertEquals(a1.affixId(), a2.affixId());
            assertEquals(a1.tier(), a2.tier());
            assertEquals(a1.value(), a2.value());
        }
    }
    
    @Test
    @DisplayName("Different seeds produce different results")
    void differentSeeds_differentResults() {
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "Epic",
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        // Try multiple seed pairs to find one that produces different results
        boolean foundDifferent = false;
        for (long seed1 = 1; seed1 < 100; seed1++) {
            AffixRollResult result1 = rollerService.rollAffixes(context, seed1);
            AffixRollResult result2 = rollerService.rollAffixes(context, seed1 + 1000000);
            
            if (!affixListsEqual(result1.affixes(), result2.affixes())) {
                foundDifferent = true;
                break;
            }
        }
        
        assertTrue(foundDifferent, "Different seeds should eventually produce different results");
    }
    
    private boolean affixListsEqual(List<RolledAffix> a, List<RolledAffix> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).affixId().equals(b.get(i).affixId())) return false;
            if (a.get(i).tier() != b.get(i).tier()) return false;
            if (a.get(i).value() != b.get(i).value()) return false;
        }
        return true;
    }
    
    // =========================================================================
    // Tooltip Integration Tests
    // =========================================================================
    
    @Test
    @DisplayName("Tooltip generation from rolled affixes")
    void tooltipGeneration_fromRolledAffixes() {
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "Rare",
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        AffixRollResult result = rollerService.rollAffixes(context, 12345L);
        assertTrue(result.hasAffixes());
        
        // Generate tooltip
        AffixTooltipProvider.TooltipContent tooltip = AffixTooltipProvider.generateTooltip(result.affixes());
        
        assertNotNull(tooltip);
        // Tooltip should have lines for affixes (may be in regular or forged sections)
        assertFalse(tooltip.regularAffixes().isEmpty() && tooltip.forgedAffixes().isEmpty());
    }
    
    // =========================================================================
    // Name Generation Integration Tests
    // =========================================================================
    
    @Test
    @DisplayName("Name generation from rolled affixes")
    void nameGeneration_fromRolledAffixes() {
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "Rare",
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        AffixRollResult result = rollerService.rollAffixes(context, 12345L);
        assertTrue(result.hasAffixes());
        
        String baseName = "Iron Chestplate";
        String displayName = AffixNameGenerator.generateDisplayName(baseName, result.affixes());
        
        assertNotNull(displayName);
        assertTrue(displayName.contains(baseName));
        
        // Name should have prefix before and/or suffix after base name
        boolean hasPrefix = result.countByType("prefix") > 0;
        boolean hasSuffix = result.countByType("suffix") > 0;
        
        if (hasPrefix || hasSuffix) {
            assertNotEquals(baseName, displayName, "Name should be modified with affixes");
        }
    }
    
    // =========================================================================
    // Metrics Integration Tests
    // =========================================================================
    
    @Test
    @DisplayName("Metrics are recorded during rolling")
    void metrics_recordedDuringRolling() {
        AffixMetrics metrics = AffixMetrics.get();
        metrics.reset();
        
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "Epic",
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        // Roll multiple times
        for (int i = 0; i < 10; i++) {
            rollerService.rollAffixes(context, i * 1000L);
        }
        
        assertEquals(10, metrics.getRollAttempts());
        assertTrue(metrics.getRollSuccesses() > 0);
        assertTrue(metrics.getTotalAffixesRolled() > 0);
        assertFalse(metrics.getRollsByQuality().isEmpty());
        assertEquals(10L, metrics.getRollsByQuality().get("Epic"));
    }
    
    // =========================================================================
    // Edge Case Tests
    // =========================================================================
    
    @Test
    @DisplayName("No affixes for unknown quality")
    void unknownQuality_noAffixes() {
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "MythicalQualityThatDoesNotExist",
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        AffixRollResult result = rollerService.rollAffixes(context, 12345L);
        
        assertFalse(result.hasAffixes());
    }
    
    @Test
    @DisplayName("No affixes for item without matching pool")
    void noMatchingPool_noAffixes() {
        AffixRollContext context = AffixRollContext.of(
                "Items.Consumables.Potion",
                "Rare",
                10,
                new String[]{"consumable"},  // No pool for this category
                new String[]{}
        );
        
        AffixRollResult result = rollerService.rollAffixes(context, 12345L);
        
        assertFalse(result.hasAffixes());
    }
    
    @Test
    @DisplayName("Rolled affixes have valid tier ranges")
    void rolledAffixes_validTierRanges() {
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "Legendary",
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        // Roll many times to check tier distribution
        for (int i = 0; i < 100; i++) {
            AffixRollResult result = rollerService.rollAffixes(context, i * 123L);
            
            for (RolledAffix affix : result.affixes()) {
                assertTrue(affix.tier() >= 1, "Tier should be at least 1");
                assertTrue(affix.tier() <= 5, "Tier should be at most 5");
                
                // Get the definition and verify value is in range
                AffixDefinition def = affixRegistry.get(affix.affixId());
                assertNotNull(def, "Should find affix definition");
                
                AffixTierDefinition tierDef = def.tiers().stream()
                        .filter(t -> t.tier() == affix.tier())
                        .findFirst()
                        .orElse(null);
                assertNotNull(tierDef, "Should find tier definition");
                
                assertTrue(affix.value() >= tierDef.minValue(),
                        "Value " + affix.value() + " should be >= " + tierDef.minValue());
                assertTrue(affix.value() <= tierDef.maxValue(),
                        "Value " + affix.value() + " should be <= " + tierDef.maxValue());
            }
        }
    }
    
    @Test
    @DisplayName("Duplicate affixes are prevented")
    void duplicateAffixes_prevented() {
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "Legendary",  // 2 prefix + 2 suffix + 1 forged
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        // Roll many times to verify no duplicates
        for (int i = 0; i < 50; i++) {
            AffixRollResult result = rollerService.rollAffixes(context, i * 999L);
            
            Set<String> seenAffixIds = new HashSet<>();
            for (RolledAffix affix : result.affixes()) {
                assertFalse(seenAffixIds.contains(affix.affixId()),
                        "Affix " + affix.affixId() + " should not appear twice");
                seenAffixIds.add(affix.affixId());
            }
        }
    }
    
    @Test
    @DisplayName("Duplicate stats are prevented")
    void duplicateStats_prevented() {
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "Legendary",
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        // Roll many times to verify no duplicate stats
        for (int i = 0; i < 50; i++) {
            AffixRollResult result = rollerService.rollAffixes(context, i * 777L);
            
            Set<StatId> seenStats = new HashSet<>();
            for (RolledAffix affix : result.affixes()) {
                assertFalse(seenStats.contains(affix.statId()),
                        "Stat " + affix.statId() + " should not appear twice");
                seenStats.add(affix.statId());
            }
        }
    }
    
    // =========================================================================
    // Backward Compatibility Tests
    // =========================================================================
    
    @Test
    @DisplayName("Empty HyforgedItemData is handled gracefully")
    void emptyItemData_handledGracefully() {
        HyforgedItemData empty = HyforgedItemData.EMPTY;
        
        assertNotNull(empty);
        assertTrue(empty.affixes().isEmpty());
        assertEquals(HyforgedItemData.CURRENT_SCHEMA_VERSION, empty.schemaVersion());
    }
    
    @Test
    @DisplayName("HyforgedItemData with affixes preserves data")
    void itemDataWithAffixes_preservesData() {
        List<RolledAffix> affixes = List.of(
                RolledAffix.from(affixRegistry.get("sturdy"), 1, 18),
                RolledAffix.from(affixRegistry.get("of-the-bear"), 2, 35)
        );
        
        HyforgedItemData data = new HyforgedItemData(
                HyforgedItemData.CURRENT_SCHEMA_VERSION,
                affixes
        );
        
        assertEquals(2, data.affixes().size());
        assertEquals("sturdy", data.affixes().get(0).affixId());
        assertEquals(1, data.affixes().get(0).tier());
        assertEquals(18, data.affixes().get(0).value());
    }
    
    // =========================================================================
    // API Integration Tests
    // =========================================================================
    
    @Test
    @DisplayName("AffixService can query registries")
    void affixService_queryRegistries() {
        AffixService service = AffixService.get();
        
        // Should be able to get all registered affixes
        Set<String> affixIds = service.getAllAffixIds();
        assertTrue(affixIds.contains("sturdy"));
        assertTrue(affixIds.contains("sharp"));
        assertTrue(affixIds.contains("of-the-bear"));
        assertTrue(affixIds.contains("of-speed"));
        
        // Should be able to get type
        assertNotNull(service.getAffixType("prefix"));
        assertNotNull(service.getAffixType("suffix"));
        
        // Should be able to get definitions
        AffixDefinition sturdy = service.getAffixDefinition("sturdy");
        assertNotNull(sturdy);
        assertEquals("Sturdy", sturdy.displayName());
    }
    
    @Test
    @DisplayName("AffixSpec creates valid specs")
    void affixSpec_createsValidSpecs() {
        // Just affix ID
        AffixSpec spec1 = AffixSpec.of("sturdy");
        assertEquals("sturdy", spec1.affixId());
        assertFalse(spec1.hasTier());
        assertFalse(spec1.hasValue());
        
        // With tier
        AffixSpec spec2 = AffixSpec.of("sturdy", 2);
        assertEquals("sturdy", spec2.affixId());
        assertTrue(spec2.hasTier());
        assertEquals(2, spec2.requireTier());
        assertFalse(spec2.hasValue());
        
        // With tier and value
        AffixSpec spec3 = AffixSpec.of("sturdy", 1, 18);
        assertEquals("sturdy", spec3.affixId());
        assertTrue(spec3.hasTier());
        assertTrue(spec3.hasValue());
        assertEquals(1, spec3.requireTier());
        assertEquals(18, spec3.requireValue());
    }
    
    // =========================================================================
    // Performance Tests
    // =========================================================================
    
    @Test
    @DisplayName("Performance: bulk rolling 1000 items should complete quickly")
    void performance_bulkRolling() {
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "Epic",
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            AffixRollResult result = rollerService.rollAffixes(context, i);
            assertNotNull(result);
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        
        // Should complete 1000 rolls in under 1 second
        assertTrue(elapsed < 1000, "1000 rolls took " + elapsed + "ms, should be < 1000ms");
    }
    
    @Test
    @DisplayName("Performance: tooltip generation for many items")
    void performance_tooltipGeneration() {
        // First roll 100 items with affixes
        List<List<RolledAffix>> itemAffixes = new java.util.ArrayList<>();
        AffixRollContext context = AffixRollContext.of(
                "Items.Armor.ChestPlate",
                "Rare",
                10,
                new String[]{"equipment"},
                new String[]{}
        );
        
        for (int i = 0; i < 100; i++) {
            AffixRollResult result = rollerService.rollAffixes(context, i);
            itemAffixes.add(result.affixes());
        }
        
        long startTime = System.currentTimeMillis();
        
        for (List<RolledAffix> affixes : itemAffixes) {
            AffixTooltipProvider.TooltipContent tooltip = AffixTooltipProvider.generateTooltip(affixes);
            assertNotNull(tooltip);
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        
        // Should complete 100 tooltip generations in under 100ms
        assertTrue(elapsed < 100, "100 tooltips took " + elapsed + "ms, should be < 100ms");
    }
}
