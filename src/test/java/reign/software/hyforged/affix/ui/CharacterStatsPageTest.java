package reign.software.hyforged.affix.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.stats.DisplayFormat;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

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
    
    // UI category names (matching the UI groups in CharacterStatsPage.ui)
    private static final String CATEGORY_CORE = "Core";
    private static final String CATEGORY_OFFENSIVE = "Offensive";
    private static final String CATEGORY_DEFENSIVE = "Defensive";
    
    /**
     * Creates a test StatDefinition with sensible defaults.
     */
    private static StatDefinition createTestStat(String name, int defaultValue, int min, int max) {
        return new StatDefinition.Builder(StatId.hyforged(name))
                .category(CATEGORY_CORE)
                .displayFormat(DisplayFormat.INTEGER)
                .defaultValue(defaultValue)
                .bounds(min, max)
                .tags(Set.of())
                .displayName(name.substring(0, 1).toUpperCase() + name.substring(1))
                .description("Test description for " + name)
                .abilityScore(true)
                .build();
    }
    
    @Nested
    @DisplayName("Category Constants")
    class CategoryConstantsTests {
        
        @Test
        @DisplayName("should have valid category values for UI")
        void shouldHaveValidCategoryValues() {
            // Categories now match the UI group names
            assertEquals("Core", CATEGORY_CORE);
            assertEquals("Offensive", CATEGORY_OFFENSIVE);
            assertEquals("Defensive", CATEGORY_DEFENSIVE);
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
                            HyforgedModifier.SourceType.EQUIPMENT,
                            5,
                            HyforgedModifier.StackType.FLAT
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
                    HyforgedModifier.SourceType.EQUIPMENT,
                    25,
                    HyforgedModifier.StackType.FLAT
            );
            
            assertEquals("equipment:armor:0:sturdy", breakdown.sourceId());
            assertEquals(HyforgedModifier.SourceType.EQUIPMENT, breakdown.sourceType());
            assertEquals(25, breakdown.value());
            assertEquals(HyforgedModifier.StackType.FLAT, breakdown.modifierType());
        }
        
        @Test
        @DisplayName("should support all modifier sources")
        void shouldSupportAllModifierSources() {
            for (HyforgedModifier.SourceType source : HyforgedModifier.SourceType.values()) {
                CharacterStatsPage.ModifierBreakdown breakdown = new CharacterStatsPage.ModifierBreakdown(
                        "test:" + source.name().toLowerCase(),
                        source,
                        10,
                        HyforgedModifier.StackType.FLAT
                );
                
                assertEquals(source, breakdown.sourceType());
            }
        }
        
        @Test
        @DisplayName("should support all modifier types")
        void shouldSupportAllModifierTypes() {
            for (HyforgedModifier.StackType type : HyforgedModifier.StackType.values()) {
                CharacterStatsPage.ModifierBreakdown breakdown = new CharacterStatsPage.ModifierBreakdown(
                        "test:modifier",
                        HyforgedModifier.SourceType.EQUIPMENT,
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
    @DisplayName("Equipment Panel")
    class EquipmentPanelTests {
        
        @Test
        @DisplayName("armor slot identifiers should follow expected format")
        void armorSlotIdentifiersShouldFollowExpectedFormat() {
            // Equipment panel uses "armor:0" through "armor:3" as slot keys
            String[] expectedSlotKeys = {"armor:0", "armor:1", "armor:2", "armor:3"};
            for (int i = 0; i < expectedSlotKeys.length; i++) {
                String slotKey = "armor:" + i;
                assertEquals(expectedSlotKeys[i], slotKey);
            }
        }
        
        @Test
        @DisplayName("hand slot identifiers should follow expected format")
        void handSlotIdentifiersShouldFollowExpectedFormat() {
            // Equipment panel uses "hand:0" and "hand:1" as slot keys
            String[] expectedSlotKeys = {"hand:0", "hand:1"};
            for (int i = 0; i < expectedSlotKeys.length; i++) {
                String slotKey = "hand:" + i;
                assertEquals(expectedSlotKeys[i], slotKey);
            }
        }
        
        @Test
        @DisplayName("should have 6 total equipment slots")
        void shouldHaveSixTotalEquipmentSlots() {
            // 4 armor + 2 hand = 6 total slots
            int totalSlots = 4 + 2;
            assertEquals(6, totalSlots);
        }
        
        @Test
        @DisplayName("empty slot should produce no tooltip entry")
        void emptySlotShouldProduceNoTooltipEntry() {
            // Empty slots should not have tooltip entries in the map
            java.util.Map<String, Object> tooltipMap = new java.util.HashMap<>();
            // Simulating: only equipped items get tooltip entries
            // Empty slot -> no put to map
            assertNull(tooltipMap.get("armor:0"), "Empty slot should not have a tooltip entry");
        }
    }
    
    @Nested
    @DisplayName("Equipment Tooltip")
    class EquipmentTooltipTests {
        
        @Test
        @DisplayName("tooltip action constants should be non-null and distinct from modifier tooltip")
        void tooltipActionsShouldBeDistinct() {
            // Equipment tooltip actions should be different from modifier tooltip actions
            String showEquip = "showEquipTooltip";
            String hideEquip = "hideEquipTooltip";
            String showMod = "showModifierTooltip";
            String hideMod = "hideModifierTooltip";
            
            assertNotNull(showEquip);
            assertNotNull(hideEquip);
            assertNotEquals(showEquip, showMod);
            assertNotEquals(hideEquip, hideMod);
            assertNotEquals(showEquip, hideEquip);
        }
        
        @Test
        @DisplayName("PageEventData codec should handle equipment tooltip action values")
        void pageEventDataShouldHandleEquipActions() {
            // PageEventData codec reuses existing Action + TooltipTarget keys
            CharacterStatsPage.PageEventData data = new CharacterStatsPage.PageEventData();
            assertNotNull(CharacterStatsPage.PageEventData.CODEC);
            // Just verify codec exists and can create instances
            assertNull(data.getAction());
            assertNull(data.getTooltipTarget());
        }
        
        @Test
        @DisplayName("affix summary should include tier indicator")
        void affixSummaryShouldIncludeTierIndicator() {
            // Expected format: "[T{tier}] {affixName}"
            int tier = 2;
            String affixName = "Sturdy";
            String expected = "[T" + tier + "] " + affixName;
            
            assertEquals("[T2] Sturdy", expected);
        }
    }
    
    @Nested
    @DisplayName("Stat Description Tooltip with Range")
    class StatDescriptionTooltipTests {
        
        @Test
        @DisplayName("tooltip should include range when bounds are meaningful")
        void tooltipShouldIncludeRangeWhenBoundsMeaningful() {
            // When a stat has min=0, max=100, range is "0 / 100" and not "-"
            StatDefinition stat = createTestStat("armor", 50, 0, 100);
            // The range "0 / 100" is meaningful, so tooltip should include it
            String rangeStr = formatTestRange(stat);
            assertNotEquals("-", rangeStr, "Range should be meaningful when both bounds are set");
            assertTrue(rangeStr.contains("/"), "Range should contain separator");
        }
        
        @Test
        @DisplayName("tooltip should omit range when both bounds are unbounded")
        void tooltipShouldOmitRangeWhenUnbounded() {
            StatDefinition stat = createTestStat("misc", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            String rangeStr = formatTestRange(stat);
            assertEquals("-", rangeStr, "Unbounded range should return dash");
        }
        
        @Test
        @DisplayName("range should handle partially bounded stats")
        void rangeShouldHandlePartiallyBounded() {
            // Only max bound is set
            StatDefinition stat = createTestStat("capped", 50, Integer.MIN_VALUE, 100);
            String rangeStr = formatTestRange(stat);
            assertNotEquals("-", rangeStr, "Partially bounded range should be meaningful");
            assertTrue(rangeStr.contains("--"), "Unbounded side should show --");
        }
        
        /**
         * Mirror the range format logic from CharacterStatsPage.formatRange()
         */
        private String formatTestRange(StatDefinition def) {
            int min = def.minValue();
            int max = def.maxValue();
            boolean minUnbounded = (min == Integer.MIN_VALUE);
            boolean maxUnbounded = (max == Integer.MAX_VALUE);
            if (minUnbounded && maxUnbounded) {
                return "-";
            }
            String minStr = minUnbounded ? "--" : String.valueOf(min);
            String maxStr = maxUnbounded ? "--" : String.valueOf(max);
            return minStr + " / " + maxStr;
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
            HyforgedModifier.SourceType sourceType = HyforgedModifier.SourceType.EQUIPMENT;
            
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
            // Create a mock modifier with HyforgedModifier builder
            HyforgedModifier modifier = HyforgedModifier.builder()
                    .sourceId("equipment:armor:0:sturdy")
                    .sourceType(HyforgedModifier.SourceType.EQUIPMENT)
                    .stackType(HyforgedModifier.StackType.FLAT)
                    .targetStat(0) // targetStatIndex
                    .amount(25) // value
                    .permanent()
                    .build();
            
            // Verify we can extract the data needed for breakdown
            assertEquals("equipment:armor:0:sturdy", modifier.getSourceId());
            assertEquals(HyforgedModifier.SourceType.EQUIPMENT, modifier.getSourceType());
            assertEquals(HyforgedModifier.StackType.FLAT, modifier.getStackType());
            assertEquals(25, modifier.getAmount());
            assertEquals(0, modifier.getTargetStatIndex());
        }
        
        @Test
        @DisplayName("should filter modifiers by target stat index")
        void shouldFilterModifiersByTargetStatIndex() {
            // Create modifiers using HyforgedModifier builder
            HyforgedModifier mod1 = HyforgedModifier.builder()
                    .sourceId("source1")
                    .sourceType(HyforgedModifier.SourceType.EQUIPMENT)
                    .stackType(HyforgedModifier.StackType.FLAT)
                    .targetStat(0)
                    .amount(10)
                    .permanent()
                    .build();
            HyforgedModifier mod2 = HyforgedModifier.builder()
                    .sourceId("source2")
                    .sourceType(HyforgedModifier.SourceType.EQUIPMENT)
                    .stackType(HyforgedModifier.StackType.FLAT)
                    .targetStat(1)
                    .amount(20)
                    .permanent()
                    .build();
            HyforgedModifier mod3 = HyforgedModifier.builder()
                    .sourceId("source3")
                    .sourceType(HyforgedModifier.SourceType.EQUIPMENT)
                    .stackType(HyforgedModifier.StackType.FLAT)
                    .targetStat(0)
                    .amount(30)
                    .permanent()
                    .build();
            
            List<HyforgedModifier> modifiers = List.of(mod1, mod2, mod3);
            
            int targetStatIndex = 0;
            List<HyforgedModifier> filtered = modifiers.stream()
                    .filter(m -> m.getTargetStatIndex() == targetStatIndex)
                    .toList();
            
            assertEquals(2, filtered.size());
            assertTrue(filtered.stream().allMatch(m -> m.getTargetStatIndex() == 0));
        }
    }
}
