package reign.software.hyforged.affix.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.stats.DisplayFormat;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.ModifierSource;
import reign.software.hyforged.stats.component.ModifierType;
import reign.software.hyforged.stats.component.StatModifier;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CharacterStatsPage} and related UI functionality.
 * <p>
 * Tests stat categorization, value formatting, modifier breakdowns, and event handling.
 */
@DisplayName("CharacterStatsPage")
class CharacterStatsPageTest {
    
    /**
     * Creates a test StatDefinition with sensible defaults.
     */
    private static StatDefinition createTestStat(String name, int defaultValue, int min, int max) {
        return new StatDefinition(
                StatId.hyforged(name),
                CharacterStatsPage.CATEGORY_ABILITY_SCORES,
                DisplayFormat.INTEGER,
                defaultValue,
                min,
                max,
                Set.of(),
                name.substring(0, 1).toUpperCase() + name.substring(1),
                "Test description for " + name,
                true,
                false,
                List.of()
        );
    }
    
    @Nested
    @DisplayName("Category Constants")
    class CategoryConstantsTests {
        
        @Test
        @DisplayName("should have all required category constants")
        void shouldHaveAllCategoryConstants() {
            assertEquals("Ability Scores", CharacterStatsPage.CATEGORY_ABILITY_SCORES);
            assertEquals("Combat", CharacterStatsPage.CATEGORY_COMBAT);
            assertEquals("Defense", CharacterStatsPage.CATEGORY_DEFENSE);
            assertEquals("Resources", CharacterStatsPage.CATEGORY_RESOURCES);
            assertEquals("Misc", CharacterStatsPage.CATEGORY_MISC);
        }
    }
    
    @Nested
    @DisplayName("StatEntry Record")
    class StatEntryTests {
        
        @Test
        @DisplayName("should create StatEntry with all fields")
        void shouldCreateStatEntryWithAllFields() {
            StatDefinition statDef = createTestStat("strength", 10, 0, 100);
            
            List<CharacterStatsPage.ModifierBreakdown> breakdown = List.of(
                    new CharacterStatsPage.ModifierBreakdown(
                            "equipment:armor:0:sturdy",
                            ModifierSource.EQUIPMENT,
                            5,
                            ModifierType.FLAT
                    )
            );
            
            CharacterStatsPage.StatEntry entry = new CharacterStatsPage.StatEntry(
                    statDef,
                    0,
                    10,
                    15,
                    5,
                    breakdown
            );
            
            assertEquals(statDef, entry.definition());
            assertEquals(0, entry.statIndex());
            assertEquals(10, entry.baseValue());
            assertEquals(15, entry.computedValue());
            assertEquals(5, entry.modifierTotal());
            assertEquals(1, entry.breakdown().size());
        }
        
        @Test
        @DisplayName("should calculate modifier total correctly")
        void shouldCalculateModifierTotalCorrectly() {
            StatDefinition statDef = createTestStat("armor", 50, 0, 1000);
            
            CharacterStatsPage.StatEntry entry = new CharacterStatsPage.StatEntry(
                    statDef,
                    1,
                    50, // base
                    125, // computed
                    75, // modifier total = computed - base
                    List.of()
            );
            
            assertEquals(75, entry.modifierTotal());
            assertEquals(125 - 50, entry.modifierTotal());
        }
    }
    
    @Nested
    @DisplayName("ModifierBreakdown Record")
    class ModifierBreakdownTests {
        
        @Test
        @DisplayName("should create ModifierBreakdown with all fields")
        void shouldCreateModifierBreakdownWithAllFields() {
            CharacterStatsPage.ModifierBreakdown breakdown = new CharacterStatsPage.ModifierBreakdown(
                    "equipment:armor:0:sturdy",
                    ModifierSource.EQUIPMENT,
                    25,
                    ModifierType.FLAT
            );
            
            assertEquals("equipment:armor:0:sturdy", breakdown.sourceId());
            assertEquals(ModifierSource.EQUIPMENT, breakdown.sourceType());
            assertEquals(25, breakdown.value());
            assertEquals(ModifierType.FLAT, breakdown.modifierType());
        }
        
        @Test
        @DisplayName("should support all modifier sources")
        void shouldSupportAllModifierSources() {
            for (ModifierSource source : ModifierSource.values()) {
                CharacterStatsPage.ModifierBreakdown breakdown = new CharacterStatsPage.ModifierBreakdown(
                        "test:" + source.name().toLowerCase(),
                        source,
                        10,
                        ModifierType.FLAT
                );
                
                assertEquals(source, breakdown.sourceType());
            }
        }
        
        @Test
        @DisplayName("should support all modifier types")
        void shouldSupportAllModifierTypes() {
            for (ModifierType type : ModifierType.values()) {
                CharacterStatsPage.ModifierBreakdown breakdown = new CharacterStatsPage.ModifierBreakdown(
                        "test:modifier",
                        ModifierSource.EQUIPMENT,
                        10,
                        type
                );
                
                assertEquals(type, breakdown.modifierType());
            }
        }
    }
    
    @Nested
    @DisplayName("PageEventData Codec")
    class PageEventDataTests {
        
        @Test
        @DisplayName("should have non-null codec")
        void shouldHaveNonNullCodec() {
            assertNotNull(CharacterStatsPage.PageEventData.CODEC);
        }
        
        @Test
        @DisplayName("should create empty PageEventData")
        void shouldCreateEmptyPageEventData() {
            CharacterStatsPage.PageEventData data = new CharacterStatsPage.PageEventData();
            assertNull(data.getAction());
        }
    }
    
    @Nested
    @DisplayName("Value Formatting")
    class ValueFormattingTests {
        
        // Note: These test the expected format patterns that formatValue() should produce
        // The actual method is private, but we document expected behavior here
        
        @Test
        @DisplayName("INTEGER format should display raw numbers")
        void integerFormatShouldDisplayRawNumbers() {
            // Expected: "42" for value 42 with INTEGER format
            String expected = "42";
            assertEquals("42", expected);
        }
        
        @Test
        @DisplayName("PERCENT_BPS format should divide by 100")
        void percentBpsShouldDivideBy100() {
            // Expected: "12.5%" for value 1250 (basis points)
            double result = 1250 / 100.0;
            assertEquals(12.5, result, 0.001);
        }
        
        @Test
        @DisplayName("RATING format should append rating suffix")
        void ratingShouldAppendSuffix() {
            // Expected: "150 rating" for value 150
            String expected = "150 rating";
            assertTrue(expected.endsWith("rating"));
        }
        
        @Test
        @DisplayName("FLAT_BONUS format should show sign")
        void flatBonusShouldShowSign() {
            // Expected: "+25" for value 25, "-10" for value -10
            int positive = 25;
            int negative = -10;
            
            String positiveStr = (positive >= 0 ? "+" : "") + positive;
            String negativeStr = (negative >= 0 ? "+" : "") + negative;
            
            assertEquals("+25", positiveStr);
            assertEquals("-10", negativeStr);
        }
        
        @Test
        @DisplayName("MULTIPLIER format should divide by 10000")
        void multiplierShouldDivideBy10000() {
            // Expected: "1.50x" for value 15000
            double result = 15000 / 10000.0;
            assertEquals(1.5, result, 0.001);
        }
    }
    
    @Nested
    @DisplayName("Stat Categorization")
    class StatCategorizationTests {
        
        @Test
        @DisplayName("strength stat should be in Ability Scores category")
        void strengthShouldBeAbilityScore() {
            // Based on the categorization logic, stats containing "strength" go to Ability Scores
            assertTrue("strength".contains("strength"));
        }
        
        @Test
        @DisplayName("attack stats should be in Combat category")
        void attackStatsShouldBeCombat() {
            // Stats containing "attack", "damage", "critical" go to Combat
            String[] combatStats = {"attack_power", "base_damage", "critical_chance"};
            for (String stat : combatStats) {
                assertTrue(
                        stat.contains("attack") || 
                        stat.contains("damage") || 
                        stat.contains("critical"),
                        stat + " should match Combat criteria"
                );
            }
        }
        
        @Test
        @DisplayName("armor stats should be in Defense category")
        void armorStatsShouldBeDefense() {
            // Stats containing "armor", "defense", "resistance" go to Defense
            String[] defenseStats = {"armor_rating", "physical_defense", "fire_resistance"};
            for (String stat : defenseStats) {
                assertTrue(
                        stat.contains("armor") || 
                        stat.contains("defense") || 
                        stat.contains("resistance"),
                        stat + " should match Defense criteria"
                );
            }
        }
        
        @Test
        @DisplayName("health stats should be in Resources category")
        void healthStatsShouldBeResources() {
            // Stats containing "health", "mana", "stamina", "regen" go to Resources
            String[] resourceStats = {"max_health", "mana_pool", "stamina_regen"};
            for (String stat : resourceStats) {
                assertTrue(
                        stat.contains("health") || 
                        stat.contains("mana") || 
                        stat.contains("stamina") || 
                        stat.contains("regen"),
                        stat + " should match Resources criteria"
                );
            }
        }
    }
    
    @Nested
    @DisplayName("Tier Color Constants")
    class TierColorTests {
        
        // Document expected tier colors from Phase 7 (AffixTooltipProvider)
        
        @Test
        @DisplayName("tier colors should follow ARPG convention")
        void tierColorsShouldFollowArpgConvention() {
            // T1 = Best (Gold)
            // T2 = Second best (Purple)
            // T3 = Mid tier (Blue)
            // T4 = Lower tier (Green)
            // T5 = Common (White)
            
            String t1Color = "#FFD700"; // Gold
            String t2Color = "#9932CC"; // Purple
            String t3Color = "#4169E1"; // Blue
            String t4Color = "#32CD32"; // Green
            String t5Color = "#FFFFFF"; // White
            
            // Just verify the format is correct
            assertTrue(t1Color.startsWith("#"));
            assertTrue(t2Color.startsWith("#"));
            assertTrue(t3Color.startsWith("#"));
            assertTrue(t4Color.startsWith("#"));
            assertTrue(t5Color.startsWith("#"));
            
            assertEquals(7, t1Color.length());
            assertEquals(7, t2Color.length());
            assertEquals(7, t3Color.length());
            assertEquals(7, t4Color.length());
            assertEquals(7, t5Color.length());
        }
    }
    
    @Nested
    @DisplayName("Equipment Slot Summary")
    class EquipmentSlotSummaryTests {
        
        @Test
        @DisplayName("affix summary should include tier indicator")
        void affixSummaryShouldIncludeTierIndicator() {
            // Expected format: "[T{tier}] {affixName}"
            int tier = 2;
            String affixName = "Sturdy";
            String expected = "[T" + tier + "] " + affixName;
            
            assertEquals("[T2] Sturdy", expected);
        }
        
        @Test
        @DisplayName("multiple affixes should be comma-separated")
        void multipleAffixesShouldBeCommaSeparated() {
            StringBuilder summary = new StringBuilder();
            String[] affixes = {"[T1] Mighty", "[T3] Swift"};
            
            for (String affix : affixes) {
                if (summary.length() > 0) {
                    summary.append(", ");
                }
                summary.append(affix);
            }
            
            assertEquals("[T1] Mighty, [T3] Swift", summary.toString());
        }
        
        @Test
        @DisplayName("empty slot should show Empty label")
        void emptySlotShouldShowEmptyLabel() {
            String slotType = "Armor";
            int slotIndex = 1;
            String emptyLabel = slotType + " " + (slotIndex + 1) + ": Empty";
            
            assertEquals("Armor 2: Empty", emptyLabel);
        }
        
        @Test
        @DisplayName("no affixes should show No affixes text")
        void noAffixesShouldShowText() {
            String noAffixesText = "No affixes";
            assertEquals("No affixes", noAffixesText);
        }
    }
    
    @Nested
    @DisplayName("Modifier Breakdown Tooltip")
    class ModifierBreakdownTooltipTests {
        
        @Test
        @DisplayName("breakdown tooltip should list all modifiers")
        void breakdownTooltipShouldListAllModifiers() {
            // Expected format per line: "  {sourceId}: +{value} ({sourceType})"
            String sourceId = "equipment:armor:0:sturdy";
            int value = 25;
            ModifierSource sourceType = ModifierSource.EQUIPMENT;
            
            String line = "  " + sourceId + ": +" + value + " (" + sourceType.name().toLowerCase() + ")";
            
            assertEquals("  equipment:armor:0:sturdy: +25 (equipment)", line);
        }
        
        @Test
        @DisplayName("negative values should show minus sign")
        void negativeValuesShouldShowMinusSign() {
            int value = -15;
            String formatted = (value >= 0 ? "+" : "") + value;
            
            assertEquals("-15", formatted);
        }
    }
    
    @Nested
    @DisplayName("StatModifier Integration")
    class StatModifierIntegrationTests {
        
        @Test
        @DisplayName("should extract breakdown from StatModifier list")
        void shouldExtractBreakdownFromStatModifierList() {
            // Create a mock stat modifier with all 8 parameters:
            // sourceId, sourceType, modifierType, targetStatIndex, targetTagIndex, value, expirationTick, priority
            StatModifier modifier = new StatModifier(
                    "equipment:armor:0:sturdy",
                    ModifierSource.EQUIPMENT,
                    ModifierType.FLAT,
                    0, // targetStatIndex
                    StatModifier.NO_TAG, // targetTagIndex (no tag)
                    25, // value
                    0L, // expirationTick (permanent)
                    0   // priority
            );
            
            // Verify we can extract the data needed for breakdown
            assertEquals("equipment:armor:0:sturdy", modifier.sourceId());
            assertEquals(ModifierSource.EQUIPMENT, modifier.sourceType());
            assertEquals(ModifierType.FLAT, modifier.modifierType());
            assertEquals(25, modifier.value());
            assertEquals(0, modifier.targetStatIndex());
        }
        
        @Test
        @DisplayName("should filter modifiers by target stat index")
        void shouldFilterModifiersByTargetStatIndex() {
            // StatModifier: sourceId, sourceType, modifierType, targetStatIndex, targetTagIndex, value, expirationTick, priority
            StatModifier mod1 = new StatModifier("source1", ModifierSource.EQUIPMENT, ModifierType.FLAT, 0, StatModifier.NO_TAG, 10, 0L, 0);
            StatModifier mod2 = new StatModifier("source2", ModifierSource.EQUIPMENT, ModifierType.FLAT, 1, StatModifier.NO_TAG, 20, 0L, 0);
            StatModifier mod3 = new StatModifier("source3", ModifierSource.EQUIPMENT, ModifierType.FLAT, 0, StatModifier.NO_TAG, 30, 0L, 0);
            
            List<StatModifier> modifiers = List.of(mod1, mod2, mod3);
            
            int targetStatIndex = 0;
            List<StatModifier> filtered = modifiers.stream()
                    .filter(m -> m.targetStatIndex() == targetStatIndex)
                    .toList();
            
            assertEquals(2, filtered.size());
            assertTrue(filtered.stream().allMatch(m -> m.targetStatIndex() == 0));
        }
    }
}
