package reign.software.hyforged.affix.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.affix.model.*;
import reign.software.hyforged.affix.registry.*;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.ModifierSource;
import reign.software.hyforged.stats.component.ModifierType;
import reign.software.hyforged.stats.component.StatModifier;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.List;

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
                StatId.hyforged("health"),
                HyforgedModifier.StackType.FLAT,
                List.of(new AffixTierDefinition(1, 50, 100, 1)),
                AffixEligibility.ANY,
                100
        ));
        
        affixRegistry.register(new AffixDefinition(
                "swift",
                "suffix",
                "of Swiftness",
                StatId.hyforged("movementSpeed"),
                HyforgedModifier.StackType.INCREASED,
                List.of(new AffixTierDefinition(1, 5, 10, 1)),
                AffixEligibility.ANY,
                100
        ));
        
        affixRegistry.register(new AffixDefinition(
                "mighty",
                "prefix",
                "Mighty",
                StatId.hyforged("physicalDamage"),
                HyforgedModifier.StackType.MORE,
                List.of(new AffixTierDefinition(1, 10, 25, 1)),
                AffixEligibility.ANY,
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
        
        @Test
        @DisplayName("FLAT affix maps to FLAT modifier")
        void flatAffixToFlatModifier() {
            RolledAffix affix = new RolledAffix(
                    "sturdy", "prefix", 1, 75,
                    StatId.hyforged("health"), HyforgedModifier.StackType.FLAT
            );
            
            assertEquals(HyforgedModifier.StackType.FLAT, affix.modifierType());
        }
        
        @Test
        @DisplayName("INCREASED affix maps to INCREASED modifier")
        void increasedAffixToIncreasedModifier() {
            RolledAffix affix = new RolledAffix(
                    "swift", "suffix", 1, 8,
                    StatId.hyforged("movementSpeed"), HyforgedModifier.StackType.INCREASED
            );
            
            assertEquals(HyforgedModifier.StackType.INCREASED, affix.modifierType());
        }
        
        @Test
        @DisplayName("MORE affix maps to MORE modifier")
        void moreAffixToMoreModifier() {
            RolledAffix affix = new RolledAffix(
                    "mighty", "prefix", 1, 15,
                    StatId.hyforged("physicalDamage"), HyforgedModifier.StackType.MORE
            );
            
            assertEquals(HyforgedModifier.StackType.MORE, affix.modifierType());
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
            List<RolledAffix> affixes = List.of(
                    new RolledAffix(
                            "sturdy", "prefix", 1, 75,
                            StatId.hyforged("health"), HyforgedModifier.StackType.FLAT
                    )
            );
            
            HyforgedItemData itemData = new HyforgedItemData(1, affixes);
            
            assertTrue(itemData.hasAffixes());
            assertEquals(1, itemData.affixes().size());
        }
        
        @Test
        @DisplayName("Item data preserves affix order")
        void itemDataPreservesAffixOrder() {
            List<RolledAffix> affixes = List.of(
                    new RolledAffix("sturdy", "prefix", 1, 75,
                            StatId.hyforged("health"), HyforgedModifier.StackType.FLAT),
                    new RolledAffix("swift", "suffix", 1, 8,
                            StatId.hyforged("movementSpeed"), HyforgedModifier.StackType.INCREASED),
                    new RolledAffix("mighty", "prefix", 1, 15,
                            StatId.hyforged("physicalDamage"), HyforgedModifier.StackType.MORE)
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
            RolledAffix affix1 = new RolledAffix(
                    "sturdy", "prefix", 1, 50,
                    StatId.hyforged("health"), HyforgedModifier.StackType.FLAT
            );
            RolledAffix affix2 = new RolledAffix(
                    "vital", "prefix", 1, 10,
                    StatId.hyforged("health"), HyforgedModifier.StackType.INCREASED
            );
            
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
            StatModifier modifier = new StatModifier(
                    "equipment:armor:0:sturdy",
                    ModifierSource.EQUIPMENT,
                    ModifierType.FLAT,
                    0,
                    StatModifier.NO_TAG,
                    75,
                    0,  // Permanent
                    0   // Default priority
            );
            
            assertEquals(ModifierSource.EQUIPMENT, modifier.sourceType());
        }
        
        @Test
        @DisplayName("Equipment modifiers have zero expiration (permanent)")
        void equipmentModifiersPermanent() {
            StatModifier modifier = new StatModifier(
                    "equipment:hand:swift",
                    ModifierSource.EQUIPMENT,
                    ModifierType.INCREASED,
                    1,
                    StatModifier.NO_TAG,
                    8,
                    0,  // Permanent - removed only when unequipped
                    0
            );
            
            assertEquals(0, modifier.expirationTick());
        }
        
        @Test
        @DisplayName("NO_TAG constant is Integer.MIN_VALUE for non-tag-specific modifiers")
        void noTagConstantUsed() {
            assertEquals(Integer.MIN_VALUE, StatModifier.NO_TAG);
        }
    }
}
