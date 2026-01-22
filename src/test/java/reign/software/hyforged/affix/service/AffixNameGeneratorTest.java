package reign.software.hyforged.affix.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.affix.AffixTestFixtures;
import reign.software.hyforged.affix.model.*;
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.affix.registry.AffixTypeRegistry;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AffixNameGenerator}.
 */
@DisplayName("AffixNameGenerator")
class AffixNameGeneratorTest {
    
    @BeforeEach
    void setUp() {
        // Reset registries
        AffixTypeRegistry.reset();
        AffixDefinitionRegistry.reset();
        
        // Register affix types
        registerAffixTypes();
        
        // Register affix definitions
        registerAffixDefinitions();
    }
    
    private void registerAffixTypes() {
        AffixTypeRegistry registry = AffixTypeRegistry.get();
        
        registry.register(new AffixType(
                "prefix",
                AffixType.DisplayNamePosition.BEFORE,
                "{name} (T{tier})",
                true
        ));
        
        registry.register(new AffixType(
                "suffix",
                AffixType.DisplayNamePosition.AFTER,
                "{name} (T{tier})",
                true
        ));
        
        registry.register(new AffixType(
                "forged",
                AffixType.DisplayNamePosition.NONE,
                "{name} (T{tier})",
                false
        ));
    }
    
    private void registerAffixDefinitions() {
        AffixDefinitionRegistry registry = AffixDefinitionRegistry.get();
        
        // Prefix affixes
        registry.register(new AffixDefinition(
                "sturdy",
                "prefix",
                "Sturdy",
                List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:health", HyforgedModifier.StackType.FLAT, 50, 100)),
                100
        ));
        
        registry.register(new AffixDefinition(
                "gleaming",
                "prefix",
                "Gleaming",
                List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:physicalDamage", HyforgedModifier.StackType.FLAT, 10, 20)),
                100
        ));
        
        registry.register(new AffixDefinition(
                "sharp",
                "prefix",
                "Sharp",
                List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:criticalChance", HyforgedModifier.StackType.FLAT, 5, 10)),
                100
        ));
        
        // Suffix affixes
        registry.register(new AffixDefinition(
                "of-the-bear",
                "suffix",
                "of the Bear",
                List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:strength", HyforgedModifier.StackType.FLAT, 5, 10)),
                100
        ));
        
        registry.register(new AffixDefinition(
                "of-speed",
                "suffix",
                "of Speed",
                List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:movementSpeed", HyforgedModifier.StackType.INCREASED, 5, 10)),
                100
        ));
        
        registry.register(new AffixDefinition(
                "of-precision",
                "suffix",
                "of Precision",
                List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:accuracy", HyforgedModifier.StackType.FLAT, 10, 20)),
                100
        ));
        
        // Forged affix (no name modification)
        registry.register(new AffixDefinition(
                "masterwork",
                "forged",
                "Masterwork",
                List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:quality", HyforgedModifier.StackType.FLAT, 10, 25)),
                100
        ));
    }
    
    private RolledAffix createAffix(String affixId) {
        AffixDefinition def = AffixDefinitionRegistry.get().get(affixId);
        assertNotNull(def, "Test affix not found: " + affixId);
        
        // Get the first stat from the tier definition
        AffixTierDefinition tierDef = def.tiers().get(0);
        Map.Entry<String, AffixTierStat> firstStat = tierDef.stats().entrySet().iterator().next();
        
        Map<String, RolledAffix.RolledStat> rolledStats = new HashMap<>();
        rolledStats.put(firstStat.getKey(), new RolledAffix.RolledStat(75, firstStat.getValue().stackType()));
        
        return new RolledAffix(
                def.id(),
                def.type(),
                1,
                rolledStats
        );
    }
    
    @Nested
    @DisplayName("Basic Name Generation")
    class BasicNameGeneration {
        
        @Test
        @DisplayName("No affixes returns base name unchanged")
        void noAffixesReturnsBaseName() {
            String result = AffixNameGenerator.generateDisplayName("Iron Sword", List.of());
            assertEquals("Iron Sword", result);
        }
        
        @Test
        @DisplayName("Empty item data returns base name unchanged")
        void emptyItemDataReturnsBaseName() {
            HyforgedItemData itemData = HyforgedItemData.EMPTY;
            String result = AffixNameGenerator.generateDisplayName("Iron Sword", itemData);
            assertEquals("Iron Sword", result);
        }
        
        @Test
        @DisplayName("Single prefix prepends to name")
        void singlePrefixPrepends() {
            List<RolledAffix> affixes = List.of(createAffix("sturdy"));
            String result = AffixNameGenerator.generateDisplayName("Iron Sword", affixes);
            assertEquals("Sturdy Iron Sword", result);
        }
        
        @Test
        @DisplayName("Single suffix appends to name")
        void singleSuffixAppends() {
            List<RolledAffix> affixes = List.of(createAffix("of-the-bear"));
            String result = AffixNameGenerator.generateDisplayName("Iron Sword", affixes);
            assertEquals("Iron Sword of the Bear", result);
        }
        
        @Test
        @DisplayName("Prefix and suffix combine correctly")
        void prefixAndSuffixCombine() {
            List<RolledAffix> affixes = List.of(
                    createAffix("sturdy"),
                    createAffix("of-the-bear")
            );
            String result = AffixNameGenerator.generateDisplayName("Iron Sword", affixes);
            assertEquals("Sturdy Iron Sword of the Bear", result);
        }
    }
    
    @Nested
    @DisplayName("Multiple Affixes")
    class MultipleAffixes {
        
        @Test
        @DisplayName("Multiple prefixes are space-separated")
        void multiplePrefixesSpaceSeparated() {
            List<RolledAffix> affixes = List.of(
                    createAffix("gleaming"),
                    createAffix("sharp")
            );
            String result = AffixNameGenerator.generateDisplayName("Dagger", affixes);
            assertEquals("Gleaming Sharp Dagger", result);
        }
        
        @Test
        @DisplayName("Multiple suffixes are space-separated")
        void multipleSuffixesSpaceSeparated() {
            List<RolledAffix> affixes = List.of(
                    createAffix("of-speed"),
                    createAffix("of-precision")
            );
            String result = AffixNameGenerator.generateDisplayName("Dagger", affixes);
            assertEquals("Dagger of Speed of Precision", result);
        }
        
        @Test
        @DisplayName("Complex combination with multiple prefixes and suffixes")
        void complexCombination() {
            List<RolledAffix> affixes = List.of(
                    createAffix("gleaming"),
                    createAffix("sharp"),
                    createAffix("of-speed"),
                    createAffix("of-precision")
            );
            String result = AffixNameGenerator.generateDisplayName("Dagger", affixes);
            assertEquals("Gleaming Sharp Dagger of Speed of Precision", result);
        }
    }
    
    @Nested
    @DisplayName("Forged Affixes")
    class ForgedAffixes {
        
        @Test
        @DisplayName("Forged affix alone does not modify name")
        void forgedAloneNoModification() {
            List<RolledAffix> affixes = List.of(createAffix("masterwork"));
            String result = AffixNameGenerator.generateDisplayName("Iron Sword", affixes);
            assertEquals("Iron Sword", result);
        }
        
        @Test
        @DisplayName("Forged affix with prefix still shows prefix")
        void forgedWithPrefix() {
            List<RolledAffix> affixes = List.of(
                    createAffix("sturdy"),
                    createAffix("masterwork")
            );
            String result = AffixNameGenerator.generateDisplayName("Iron Sword", affixes);
            assertEquals("Sturdy Iron Sword", result);
        }
        
        @Test
        @DisplayName("Forged affix with suffix still shows suffix")
        void forgedWithSuffix() {
            List<RolledAffix> affixes = List.of(
                    createAffix("masterwork"),
                    createAffix("of-the-bear")
            );
            String result = AffixNameGenerator.generateDisplayName("Iron Sword", affixes);
            assertEquals("Iron Sword of the Bear", result);
        }
    }
    
    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethods {
        
        @Test
        @DisplayName("hasVisibleNameModifiers returns false for empty")
        void hasVisibleFalseForEmpty() {
            assertFalse(AffixNameGenerator.hasVisibleNameModifiers(HyforgedItemData.EMPTY));
        }
        
        @Test
        @DisplayName("hasVisibleNameModifiers returns true for prefix")
        void hasVisibleTrueForPrefix() {
            HyforgedItemData itemData = new HyforgedItemData(1, List.of(createAffix("sturdy")));
            assertTrue(AffixNameGenerator.hasVisibleNameModifiers(itemData));
        }
        
        @Test
        @DisplayName("hasVisibleNameModifiers returns true for suffix")
        void hasVisibleTrueForSuffix() {
            HyforgedItemData itemData = new HyforgedItemData(1, List.of(createAffix("of-the-bear")));
            assertTrue(AffixNameGenerator.hasVisibleNameModifiers(itemData));
        }
        
        @Test
        @DisplayName("hasVisibleNameModifiers returns false for forged only")
        void hasVisibleFalseForForgedOnly() {
            HyforgedItemData itemData = new HyforgedItemData(1, List.of(createAffix("masterwork")));
            assertFalse(AffixNameGenerator.hasVisibleNameModifiers(itemData));
        }
        
        @Test
        @DisplayName("getPrefixString returns empty for no prefixes")
        void getPrefixStringEmpty() {
            List<RolledAffix> affixes = List.of(createAffix("of-the-bear"));
            assertEquals("", AffixNameGenerator.getPrefixString(affixes));
        }
        
        @Test
        @DisplayName("getPrefixString returns space-separated prefixes")
        void getPrefixStringMultiple() {
            List<RolledAffix> affixes = List.of(
                    createAffix("gleaming"),
                    createAffix("sharp"),
                    createAffix("of-speed") // suffix, should be excluded
            );
            assertEquals("Gleaming Sharp", AffixNameGenerator.getPrefixString(affixes));
        }
        
        @Test
        @DisplayName("getSuffixString returns empty for no suffixes")
        void getSuffixStringEmpty() {
            List<RolledAffix> affixes = List.of(createAffix("sturdy"));
            assertEquals("", AffixNameGenerator.getSuffixString(affixes));
        }
        
        @Test
        @DisplayName("getSuffixString returns space-separated suffixes")
        void getSuffixStringMultiple() {
            List<RolledAffix> affixes = List.of(
                    createAffix("sturdy"), // prefix, should be excluded
                    createAffix("of-speed"),
                    createAffix("of-precision")
            );
            assertEquals("of Speed of Precision", AffixNameGenerator.getSuffixString(affixes));
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("Unknown affix ID is skipped gracefully")
        void unknownAffixSkipped() {
            // Create affix with unknown ID
            Map<String, RolledAffix.RolledStat> unknownStats = new HashMap<>();
            unknownStats.put("hyforged:health", new RolledAffix.RolledStat(50, HyforgedModifier.StackType.FLAT));
            RolledAffix unknownAffix = new RolledAffix(
                    "unknown-affix",
                    "prefix",
                    1,
                    unknownStats
            );
            
            List<RolledAffix> affixes = List.of(
                    createAffix("sturdy"),
                    unknownAffix
            );
            
            // Should still work, just skip the unknown affix
            String result = AffixNameGenerator.generateDisplayName("Iron Sword", affixes);
            assertEquals("Sturdy Iron Sword", result);
        }
        
        @Test
        @DisplayName("Affix order is preserved")
        void affixOrderPreserved() {
            // Order: sharp, sturdy, gleaming
            List<RolledAffix> affixes = List.of(
                    createAffix("sharp"),
                    createAffix("sturdy"),
                    createAffix("gleaming")
            );
            String result = AffixNameGenerator.generateDisplayName("Sword", affixes);
            assertEquals("Sharp Sturdy Gleaming Sword", result);
        }
        
        @Test
        @DisplayName("Base name with spaces works correctly")
        void baseNameWithSpaces() {
            List<RolledAffix> affixes = List.of(
                    createAffix("sturdy"),
                    createAffix("of-the-bear")
            );
            String result = AffixNameGenerator.generateDisplayName("Iron Long Sword", affixes);
            assertEquals("Sturdy Iron Long Sword of the Bear", result);
        }
        
        @Test
        @DisplayName("Using HyforgedItemData wrapper works")
        void hyforgedItemDataWrapper() {
            HyforgedItemData itemData = new HyforgedItemData(1, List.of(
                    createAffix("sturdy"),
                    createAffix("of-speed")
            ));
            
            String result = AffixNameGenerator.generateDisplayName("Dagger", itemData);
            assertEquals("Sturdy Dagger of Speed", result);
        }
    }
}
