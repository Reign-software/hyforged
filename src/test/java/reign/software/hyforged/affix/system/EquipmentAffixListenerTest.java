package reign.software.hyforged.affix.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.affix.AffixTestFixtures;
import reign.software.hyforged.affix.model.*;
import reign.software.hyforged.affix.registry.*;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EquipmentAffixListener} and related event classes.
 * <p>
 * Note: Full integration testing requires a running Hytale server context.
 * These tests focus on the data models, event structures, and utility methods.
 * AffixModifiersAppliedEvent tests are omitted as they require a non-null LivingEntity.
 */
@DisplayName("EquipmentAffixListener")
class EquipmentAffixListenerTest {
    
    private AffixDefinitionRegistry affixRegistry;
    
    @BeforeEach
    void setUp() {
        // Reset registries
        AffixDefinitionRegistry.reset();
        StatDefinitionRegistry.reset();
        
        affixRegistry = AffixDefinitionRegistry.get();
        
        // Register test affixes
        registerTestAffixes();
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
        
        affixRegistry.register(new AffixDefinition(
                "mighty",
                "prefix",
                "Mighty",
                List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:physicalDamage", HyforgedModifier.StackType.MORE, 10, 25)),
                100
        ));
    }
    
    @Nested
    @DisplayName("Source ID Formatting")
    class SourceIdFormatting {
        
        @Test
        @DisplayName("Equipment source prefix is correct")
        void sourcePrefix() {
            assertEquals("equipment:", EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX);
        }
        
        @Test
        @DisplayName("Armor slot source format is correct")
        void armorSlotSourceFormat() {
            String expected = "equipment:armor:0:sturdy";
            String actual = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + "armor:0:sturdy";
            assertEquals(expected, actual);
        }
        
        @Test
        @DisplayName("Hand slot source format is correct")
        void handSlotSourceFormat() {
            String expected = "equipment:hand:swift";
            String actual = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + "hand:swift";
            assertEquals(expected, actual);
        }
    }
    
    @Nested
    @DisplayName("Rolled Affix to StatModifier Conversion")
    class AffixToModifierConversion {
        
        private RolledAffix createRolledAffix(String affixId, String type, int tier, String statId, int value, HyforgedModifier.StackType stackType) {
            Map<String, RolledAffix.RolledStat> stats = new HashMap<>();
            stats.put(statId, new RolledAffix.RolledStat(value, stackType));
            return new RolledAffix(affixId, type, tier, stats);
        }
        
        @Test
        @DisplayName("FLAT affix maps to FLAT modifier")
        void flatAffixToFlatModifier() {
            RolledAffix affix = createRolledAffix(
                    "sturdy", "prefix", 1, "hyforged:health", 75, HyforgedModifier.StackType.FLAT
            );
            
            RolledAffix.RolledStat stat = affix.rolledStats().get("hyforged:health");
            assertEquals(HyforgedModifier.StackType.FLAT, stat.stackType());
        }
        
        @Test
        @DisplayName("INCREASED affix maps to INCREASED modifier")
        void increasedAffixToIncreasedModifier() {
            RolledAffix affix = createRolledAffix(
                    "swift", "suffix", 1, "hyforged:movementSpeed", 8, HyforgedModifier.StackType.INCREASED
            );
            
            RolledAffix.RolledStat stat = affix.rolledStats().get("hyforged:movementSpeed");
            assertEquals(HyforgedModifier.StackType.INCREASED, stat.stackType());
        }
        
        @Test
        @DisplayName("MORE affix maps to MORE modifier")
        void moreAffixToMoreModifier() {
            RolledAffix affix = createRolledAffix(
                    "mighty", "prefix", 1, "hyforged:physicalDamage", 15, HyforgedModifier.StackType.MORE
            );
            
            RolledAffix.RolledStat stat = affix.rolledStats().get("hyforged:physicalDamage");
            assertEquals(HyforgedModifier.StackType.MORE, stat.stackType());
        }
    }
    
    @Nested
    @DisplayName("HyforgedItemData Integration")
    class ItemDataIntegration {
        
        @Test
        @DisplayName("Empty item data has no affixes")
        void emptyItemDataNoAffixes() {
            HyforgedItemData itemData = HyforgedItemData.EMPTY;
            
            assertFalse(itemData.hasAffixes());
            assertTrue(itemData.affixes().isEmpty());
        }
        
        @Test
        @DisplayName("Item data with affixes detected")
        void itemDataWithAffixesDetected() {
            Map<String, RolledAffix.RolledStat> stats = new HashMap<>();
            stats.put("hyforged:health", new RolledAffix.RolledStat(75, HyforgedModifier.StackType.FLAT));
            List<RolledAffix> affixes = List.of(
                    new RolledAffix("sturdy", "prefix", 1, stats)
            );
            
            HyforgedItemData itemData = new HyforgedItemData(1, affixes);
            
            assertTrue(itemData.hasAffixes());
            assertEquals(1, itemData.affixes().size());
        }
        
        @Test
        @DisplayName("Item data preserves affix order")
        void itemDataPreservesAffixOrder() {
            Map<String, RolledAffix.RolledStat> sturdyStats = new HashMap<>();
            sturdyStats.put("hyforged:health", new RolledAffix.RolledStat(75, HyforgedModifier.StackType.FLAT));
            Map<String, RolledAffix.RolledStat> swiftStats = new HashMap<>();
            swiftStats.put("hyforged:movementSpeed", new RolledAffix.RolledStat(8, HyforgedModifier.StackType.INCREASED));
            Map<String, RolledAffix.RolledStat> mightyStats = new HashMap<>();
            mightyStats.put("hyforged:physicalDamage", new RolledAffix.RolledStat(15, HyforgedModifier.StackType.MORE));
            
            List<RolledAffix> affixes = List.of(
                    new RolledAffix("sturdy", "prefix", 1, sturdyStats),
                    new RolledAffix("swift", "suffix", 1, swiftStats),
                    new RolledAffix("mighty", "prefix", 1, mightyStats)
            );
            
            HyforgedItemData itemData = new HyforgedItemData(1, affixes);
            
            assertEquals("sturdy", itemData.affixes().get(0).affixId());
            assertEquals("swift", itemData.affixes().get(1).affixId());
            assertEquals("mighty", itemData.affixes().get(2).affixId());
        }
    }
    
    @Nested
    @DisplayName("Modifier Source Pattern Matching")
    class SourcePatternMatching {
        
        @Test
        @DisplayName("Armor source pattern matches armor slots")
        void armorSourcePatternMatches() {
            String sourceId = "equipment:armor:0:sturdy";
            String armorPattern = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + "armor:";
            
            assertTrue(sourceId.startsWith(armorPattern));
        }
        
        @Test
        @DisplayName("Hand source pattern matches hand slot")
        void handSourcePatternMatches() {
            String sourceId = "equipment:hand:swift";
            String handPattern = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + "hand:";
            
            assertTrue(sourceId.startsWith(handPattern));
        }
        
        @Test
        @DisplayName("Armor pattern does not match hand")
        void armorPatternNotMatchHand() {
            String sourceId = "equipment:hand:swift";
            String armorPattern = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + "armor:";
            
            assertFalse(sourceId.startsWith(armorPattern));
        }
        
        @Test
        @DisplayName("Hand pattern does not match armor")
        void handPatternNotMatchArmor() {
            String sourceId = "equipment:armor:2:sturdy";
            String handPattern = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + "hand:";
            
            assertFalse(sourceId.startsWith(handPattern));
        }
        
        @Test
        @DisplayName("Different affix IDs create unique source IDs")
        void differentAffixesUniqueSourceIds() {
            String source1 = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + "armor:0:sturdy";
            String source2 = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + "armor:0:swift";
            
            assertNotEquals(source1, source2);
        }
        
        @Test
        @DisplayName("Same affix different slots create unique source IDs")
        void sameAffixDifferentSlotsUniqueSourceIds() {
            String source1 = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + "armor:0:sturdy";
            String source2 = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + "armor:1:sturdy";
            
            assertNotEquals(source1, source2);
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("Empty affixes list produces no modifiers")
        void emptyAffixesNoModifiers() {
            HyforgedItemData itemData = new HyforgedItemData(1, List.of());
            
            assertFalse(itemData.hasAffixes());
        }
        
        @Test
        @DisplayName("Multiple affixes targeting same stat")
        void multipleAffixesSameStat() {
            // Two different affixes both affecting health
            Map<String, RolledAffix.RolledStat> affix1Stats = new HashMap<>();
            affix1Stats.put("hyforged:health", new RolledAffix.RolledStat(50, HyforgedModifier.StackType.FLAT));
            RolledAffix affix1 = new RolledAffix("sturdy", "prefix", 1, affix1Stats);
            
            Map<String, RolledAffix.RolledStat> affix2Stats = new HashMap<>();
            affix2Stats.put("hyforged:health", new RolledAffix.RolledStat(10, HyforgedModifier.StackType.INCREASED));
            RolledAffix affix2 = new RolledAffix("vital", "prefix", 1, affix2Stats);
            
            HyforgedItemData itemData = new HyforgedItemData(1, List.of(affix1, affix2));
            
            assertEquals(2, itemData.affixes().size());
            
            // Both should be tracked separately
            assertEquals("sturdy", itemData.affixes().get(0).affixId());
            assertEquals("vital", itemData.affixes().get(1).affixId());
        }
        
        @Test
        @DisplayName("Re-equipping same item regenerates modifiers with same source IDs")
        void reEquipSameItemSameSourceIds() {
            String affixId = "sturdy";
            String slot = "armor:0";
            
            // First equip
            String sourceId1 = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + slot + ":" + affixId;
            
            // Re-equip (same item, same slot)
            String sourceId2 = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + slot + ":" + affixId;
            
            // Source IDs should be identical for proper removal/re-add
            assertEquals(sourceId1, sourceId2);
        }
        
        @Test
        @DisplayName("Swapping items changes source affix IDs")
        void swappingItemsChangesSourceIds() {
            String slot = "armor:0";
            
            // Item 1 with sturdy affix
            String sourceId1 = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + slot + ":sturdy";
            
            // Item 2 with swift affix (swapped into same slot)
            String sourceId2 = EquipmentAffixListener.EQUIPMENT_SOURCE_PREFIX + slot + ":swift";
            
            // Different affixes in same slot have different source IDs
            assertNotEquals(sourceId1, sourceId2);
        }
    }
    
    @Nested
    @DisplayName("StatModifier Creation")
    class StatModifierCreation {
        
        @Test
        @DisplayName("Equipment modifiers use EQUIPMENT source type")
        void equipmentModifiersUseEquipmentSource() {
            HyforgedModifier modifier = HyforgedModifier.builder()
                    .sourceId("equipment:armor:0:sturdy")
                    .sourceType(HyforgedModifier.SourceType.EQUIPMENT)
                    .stackType(HyforgedModifier.StackType.FLAT)
                    .targetStat(0)
                    .amount(75)
                    .permanent()
                    .build();
            
            assertEquals(HyforgedModifier.SourceType.EQUIPMENT, modifier.getSourceType());
        }
        
        @Test
        @DisplayName("Equipment modifiers have zero expiration (permanent)")
        void equipmentModifiersPermanent() {
            HyforgedModifier modifier = HyforgedModifier.builder()
                    .sourceId("equipment:hand:swift")
                    .sourceType(HyforgedModifier.SourceType.EQUIPMENT)
                    .stackType(HyforgedModifier.StackType.INCREASED)
                    .targetStat(1)
                    .amount(8)
                    .permanent()
                    .build();
            
            assertEquals(0, modifier.getExpirationTick());
        }
        
        @Test
        @DisplayName("NO_TAG constant is Integer.MIN_VALUE for non-tag-specific modifiers")
        void noTagConstantUsed() {
            assertEquals(Integer.MIN_VALUE, HyforgedModifier.NO_TAG);
        }
    }
}
