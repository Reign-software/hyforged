package reign.software.hyforged.affix.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.affix.AffixTestFixtures;
import reign.software.hyforged.affix.model.*;
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.affix.registry.AffixTypeRegistry;
import reign.software.hyforged.affix.service.AffixTooltipProvider.TooltipContent;
import reign.software.hyforged.affix.service.AffixTooltipProvider.TooltipLine;
import reign.software.hyforged.stats.DisplayFormat;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AffixTooltipProvider}.
 */
@DisplayName("AffixTooltipProvider")
class AffixTooltipProviderTest {

    @BeforeEach
    void setUp() {
        // Reset registries
        AffixTypeRegistry.reset();
        AffixDefinitionRegistry.reset();
        StatDefinitionRegistry.reset();

        // Register affix types
        registerAffixTypes();

        // Register stat definitions
        registerStatDefinitions();

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

    private void registerStatDefinitions() {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();

        registry.registerStat(new StatDefinition.Builder(StatId.hyforged("health"))
            .category("combat")
            .displayFormat(DisplayFormat.INTEGER)
            .defaultValue(100)
            .bounds(0, 10000)
            .displayName("Health")
            .description("Maximum health points")
            .build());

        registry.registerStat(new StatDefinition.Builder(StatId.hyforged("physicalDamage"))
            .category("combat")
            .displayFormat(DisplayFormat.INTEGER)
            .defaultValue(10)
            .bounds(0, 1000)
            .displayName("Physical Damage")
            .description("Physical attack damage")
            .build());

        registry.registerStat(new StatDefinition.Builder(StatId.hyforged("movementSpeed"))
            .category("utility")
            .displayFormat(DisplayFormat.PERCENT_BPS)
            .defaultValue(100)
            .bounds(0, 500)
            .displayName("Movement Speed")
            .description("Movement speed bonus")
            .build());

        registry.registerStat(new StatDefinition.Builder(StatId.hyforged("criticalChance"))
            .category("combat")
            .displayFormat(DisplayFormat.PERCENT_BPS)
            .defaultValue(5)
            .bounds(0, 100)
            .displayName("Critical Chance")
            .description("Chance to deal critical damage")
            .build());

        registry.registerStat(new StatDefinition.Builder(StatId.hyforged("quality"))
            .category("crafting")
            .displayFormat(DisplayFormat.INTEGER)
            .defaultValue(0)
            .bounds(0, 100)
            .displayName("Quality")
            .description("Item quality bonus")
            .build());

        registry.freeze();
    }

    private void registerAffixDefinitions() {
        AffixDefinitionRegistry registry = AffixDefinitionRegistry.get();

        // Prefix affixes with flat modifier
        registry.register(new AffixDefinition(
            "sturdy",
            "prefix",
            "Sturdy",
            List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:health", HyforgedModifier.StackType.FLAT, 50, 100)),
            100
        ));

        registry.register(new AffixDefinition(
            "sharp",
            "prefix",
            "Sharp",
            List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:physicalDamage", HyforgedModifier.StackType.FLAT, 10, 25)),
            100
        ));

        // Suffix with percentage modifier
        registry.register(new AffixDefinition(
            "of-speed",
            "suffix",
            "of Speed",
            List.of(
                AffixTestFixtures.tier(1, 1, 100, "hyforged:movementSpeed", HyforgedModifier.StackType.INCREASED, 1500, 2000),  // 15-20%
                AffixTestFixtures.tier(2, 1, 100, "hyforged:movementSpeed", HyforgedModifier.StackType.INCREASED, 1000, 1500),  // 10-15%
                AffixTestFixtures.tier(3, 1, 100, "hyforged:movementSpeed", HyforgedModifier.StackType.INCREASED, 500, 1000)    // 5-10%
            ),
            100
        ));

        registry.register(new AffixDefinition(
            "of-precision",
            "suffix",
            "of Precision",
            List.of(AffixTestFixtures.tier(1, 1, 100, "hyforged:criticalChance", HyforgedModifier.StackType.FLAT, 5, 10)),
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

    private RolledAffix createAffix(String affixId, int tier, int value) {
        AffixDefinition def = AffixDefinitionRegistry.get().get(affixId);
        assertNotNull(def, "Test affix not found: " + affixId);
        
        // Get the first stat from the tier definition
        AffixTierDefinition tierDef = def.tiers().get(0);
        Map.Entry<String, AffixTierStat> firstStat = tierDef.stats().entrySet().iterator().next();
        
        Map<String, RolledAffix.RolledStat> rolledStats = new HashMap<>();
        rolledStats.put(firstStat.getKey(), new RolledAffix.RolledStat(value, firstStat.getValue().stackType()));
        
        return new RolledAffix(
            def.id(),
            def.type(),
            tier,
            rolledStats
        );
    }

    // ======= TIER COLOR TESTS =======

    @Nested
    @DisplayName("Tier Colors")
    class TierColorTests {

        @Test
        @DisplayName("T1 returns gold color")
        void tier1ReturnsGold() {
            assertEquals("#FFD700", AffixTooltipProvider.getTierColor(1));
        }

        @Test
        @DisplayName("T2 returns purple color")
        void tier2ReturnsPurple() {
            assertEquals("#9932CC", AffixTooltipProvider.getTierColor(2));
        }

        @Test
        @DisplayName("T3 returns blue color")
        void tier3ReturnsBlue() {
            assertEquals("#4169E1", AffixTooltipProvider.getTierColor(3));
        }

        @Test
        @DisplayName("T4 returns green color")
        void tier4ReturnsGreen() {
            assertEquals("#32CD32", AffixTooltipProvider.getTierColor(4));
        }

        @Test
        @DisplayName("T5 returns white color")
        void tier5ReturnsWhite() {
            assertEquals("#FFFFFF", AffixTooltipProvider.getTierColor(5));
        }

        @Test
        @DisplayName("T6+ defaults to white color")
        void tier6PlusDefaultsToWhite() {
            assertEquals("#FFFFFF", AffixTooltipProvider.getTierColor(6));
            assertEquals("#FFFFFF", AffixTooltipProvider.getTierColor(99));
        }
    }

    // ======= TIER LABEL TESTS =======

    @Nested
    @DisplayName("Tier Labels")
    class TierLabelTests {

        @Test
        @DisplayName("Tier 1 formats as T1")
        void tier1FormatsAsT1() {
            assertEquals("T1", AffixTooltipProvider.getTierLabel(1));
        }

        @Test
        @DisplayName("Tier 5 formats as T5")
        void tier5FormatsAsT5() {
            assertEquals("T5", AffixTooltipProvider.getTierLabel(5));
        }
    }

    // ======= VALUE FORMATTING TESTS =======

    @Nested
    @DisplayName("Value Formatting")
    class ValueFormattingTests {

        @Test
        @DisplayName("Flat positive value formats with plus sign")
        void flatPositiveValueFormats() {
            assertEquals("+50", AffixTooltipProvider.formatValue(50, HyforgedModifier.StackType.FLAT));
        }

        @Test
        @DisplayName("Flat negative value formats without extra sign")
        void flatNegativeValueFormats() {
            assertEquals("-25", AffixTooltipProvider.formatValue(-25, HyforgedModifier.StackType.FLAT));
        }

        @Test
        @DisplayName("Flat zero formats with plus sign")
        void flatZeroFormats() {
            assertEquals("+0", AffixTooltipProvider.formatValue(0, HyforgedModifier.StackType.FLAT));
        }

        @Test
        @DisplayName("Increased value formats as percentage")
        void increasedValueFormatsAsPercentage() {
            // 1000 basis points = 10%
            assertEquals("+10%", AffixTooltipProvider.formatValue(1000, HyforgedModifier.StackType.INCREASED));
        }

        @Test
        @DisplayName("Increased fractional percentage formats correctly")
        void increasedFractionalPercentage() {
            // 1550 basis points = 15.5%
            assertEquals("+15.5%", AffixTooltipProvider.formatValue(1550, HyforgedModifier.StackType.INCREASED));
        }

        @Test
        @DisplayName("More value formats as percentage")
        void moreValueFormatsAsPercentage() {
            assertEquals("+5%", AffixTooltipProvider.formatValue(500, HyforgedModifier.StackType.MORE));
        }

        @Test
        @DisplayName("Negative percentage formats correctly")
        void negativePercentageFormats() {
            assertEquals("-10%", AffixTooltipProvider.formatValue(-1000, HyforgedModifier.StackType.INCREASED));
        }
    }

    // ======= AFFIX LINE FORMATTING TESTS =======

    @Nested
    @DisplayName("Affix Line Formatting")
    class AffixLineFormattingTests {

        @Test
        @DisplayName("Flat affix line formats correctly")
        void flatAffixLineFormats() {
            String result = AffixTooltipProvider.formatAffixLine(
                1, "Sturdy", 50, HyforgedModifier.StackType.FLAT, "Health"
            );
            assertEquals("[T1] Sturdy: +50 Health", result);
        }

        @Test
        @DisplayName("Percentage affix line formats correctly")
        void percentageAffixLineFormats() {
            String result = AffixTooltipProvider.formatAffixLine(
                2, "of Speed", 1500, HyforgedModifier.StackType.INCREASED, "Movement Speed"
            );
            assertEquals("[T2] of Speed: +15% Movement Speed", result);
        }

        @Test
        @DisplayName("Higher tier formats correctly")
        void higherTierFormats() {
            String result = AffixTooltipProvider.formatAffixLine(
                3, "Swift", 800, HyforgedModifier.StackType.INCREASED, "Attack Speed"
            );
            assertEquals("[T3] Swift: +8% Attack Speed", result);
        }
    }

    // ======= TOOLTIP LINE RECORD TESTS =======

    @Nested
    @DisplayName("TooltipLine Record")
    class TooltipLineTests {

        @Test
        @DisplayName("Header creates header line")
        void headerCreatesHeaderLine() {
            TooltipLine line = TooltipLine.header("Affixes");
            assertEquals("Affixes", line.text());
            assertNull(line.color());
            assertTrue(line.isHeader());
        }

        @Test
        @DisplayName("Content with color creates content line")
        void contentWithColorCreatesLine() {
            TooltipLine line = TooltipLine.content("Test", "#FFD700");
            assertEquals("Test", line.text());
            assertEquals("#FFD700", line.color());
            assertFalse(line.isHeader());
        }

        @Test
        @DisplayName("Content without color creates line")
        void contentWithoutColorCreatesLine() {
            TooltipLine line = TooltipLine.content("Test");
            assertEquals("Test", line.text());
            assertNull(line.color());
            assertFalse(line.isHeader());
        }

        @Test
        @DisplayName("Null text throws exception")
        void nullTextThrows() {
            assertThrows(NullPointerException.class, () -> new TooltipLine(null, null, false));
        }
    }

    // ======= TOOLTIP CONTENT TESTS =======

    @Nested
    @DisplayName("TooltipContent Record")
    class TooltipContentTests {

        @Test
        @DisplayName("Empty content has no content")
        void emptyContentHasNoContent() {
            assertFalse(TooltipContent.EMPTY.hasContent());
            assertFalse(TooltipContent.EMPTY.hasRegularAffixes());
            assertFalse(TooltipContent.EMPTY.hasForgedAffixes());
        }

        @Test
        @DisplayName("Content with regular affixes hasContent")
        void contentWithRegularAffixesHasContent() {
            TooltipContent content = new TooltipContent(
                List.of(TooltipLine.content("test")),
                List.of()
            );
            assertTrue(content.hasContent());
            assertTrue(content.hasRegularAffixes());
            assertFalse(content.hasForgedAffixes());
        }

        @Test
        @DisplayName("Content with forged affixes hasContent")
        void contentWithForgedAffixesHasContent() {
            TooltipContent content = new TooltipContent(
                List.of(),
                List.of(TooltipLine.content("test"))
            );
            assertTrue(content.hasContent());
            assertFalse(content.hasRegularAffixes());
            assertTrue(content.hasForgedAffixes());
        }

        @Test
        @DisplayName("getAllLines includes headers")
        void getAllLinesIncludesHeaders() {
            TooltipContent content = new TooltipContent(
                List.of(TooltipLine.content("regular")),
                List.of(TooltipLine.content("forged"))
            );

            List<TooltipLine> allLines = content.getAllLines();
            assertEquals(4, allLines.size());
            assertTrue(allLines.get(0).isHeader());
            assertEquals(AffixTooltipProvider.AFFIXES_SECTION_HEADER, allLines.get(0).text());
            assertFalse(allLines.get(1).isHeader());
            assertTrue(allLines.get(2).isHeader());
            assertEquals(AffixTooltipProvider.FORGED_SECTION_HEADER, allLines.get(2).text());
        }

        @Test
        @DisplayName("toPlainText returns text strings")
        void toPlainTextReturnsStrings() {
            TooltipContent content = new TooltipContent(
                List.of(TooltipLine.content("regular affix")),
                List.of()
            );

            List<String> plainText = content.toPlainText();
            assertEquals(2, plainText.size());
            assertEquals("Affixes", plainText.get(0));
            assertEquals("regular affix", plainText.get(1));
        }
    }

    // ======= TOOLTIP GENERATION TESTS =======

    @Nested
    @DisplayName("Tooltip Generation")
    class TooltipGenerationTests {

        @Test
        @DisplayName("Empty item data returns empty content")
        void emptyItemDataReturnsEmpty() {
            TooltipContent content = AffixTooltipProvider.generateTooltip(HyforgedItemData.EMPTY);
            assertFalse(content.hasContent());
        }

        @Test
        @DisplayName("Empty affix list returns empty content")
        void emptyAffixListReturnsEmpty() {
            TooltipContent content = AffixTooltipProvider.generateTooltip(List.of());
            assertFalse(content.hasContent());
        }

        @Test
        @DisplayName("Single prefix affix generates regular content")
        void singlePrefixAffixGeneratesRegular() {
            RolledAffix affix = createAffix("sturdy", 1, 75);
            TooltipContent content = AffixTooltipProvider.generateTooltip(List.of(affix));

            assertTrue(content.hasRegularAffixes());
            assertFalse(content.hasForgedAffixes());
            assertEquals(1, content.regularAffixes().size());

            TooltipLine line = content.regularAffixes().get(0);
            assertEquals("[T1] Sturdy: +75 Health", line.text());
            assertEquals(AffixTooltipProvider.TIER_1_COLOR, line.color());
        }

        @Test
        @DisplayName("Single suffix affix generates regular content")
        void singleSuffixAffixGeneratesRegular() {
            RolledAffix affix = createAffix("of-precision", 1, 7);
            TooltipContent content = AffixTooltipProvider.generateTooltip(List.of(affix));

            assertTrue(content.hasRegularAffixes());
            assertEquals(1, content.regularAffixes().size());
            assertEquals("[T1] of Precision: +7 Critical Chance", content.regularAffixes().get(0).text());
        }

        @Test
        @DisplayName("Forged affix generates forged content")
        void forgedAffixGeneratesForgedContent() {
            RolledAffix affix = createAffix("masterwork", 1, 20);
            TooltipContent content = AffixTooltipProvider.generateTooltip(List.of(affix));

            assertFalse(content.hasRegularAffixes());
            assertTrue(content.hasForgedAffixes());
            assertEquals(1, content.forgedAffixes().size());
            assertEquals("[T1] Masterwork: +20 Quality", content.forgedAffixes().get(0).text());
        }

        @Test
        @DisplayName("Mixed affixes are separated correctly")
        void mixedAffixesAreSeparated() {
            RolledAffix prefix = createAffix("sturdy", 1, 50);
            RolledAffix suffix = createAffix("of-precision", 2, 5);
            RolledAffix forged = createAffix("masterwork", 1, 15);

            TooltipContent content = AffixTooltipProvider.generateTooltip(
                List.of(prefix, suffix, forged)
            );

            assertEquals(2, content.regularAffixes().size());
            assertEquals(1, content.forgedAffixes().size());
        }

        @Test
        @DisplayName("Percentage modifier formats correctly")
        void percentageModifierFormatsCorrectly() {
            RolledAffix affix = createAffix("of-speed", 1, 1750); // 17.5%
            TooltipContent content = AffixTooltipProvider.generateTooltip(List.of(affix));

            assertEquals("[T1] of Speed: +17.5% Movement Speed", content.regularAffixes().get(0).text());
        }

        @Test
        @DisplayName("Tier 2 affix has correct color")
        void tier2AffixHasCorrectColor() {
            RolledAffix affix = createAffix("of-speed", 2, 1200);
            TooltipContent content = AffixTooltipProvider.generateTooltip(List.of(affix));

            assertEquals(AffixTooltipProvider.TIER_2_COLOR, content.regularAffixes().get(0).color());
        }

        @Test
        @DisplayName("Tier 3 affix has correct color")
        void tier3AffixHasCorrectColor() {
            RolledAffix affix = createAffix("of-speed", 3, 700);
            TooltipContent content = AffixTooltipProvider.generateTooltip(List.of(affix));

            assertEquals(AffixTooltipProvider.TIER_3_COLOR, content.regularAffixes().get(0).color());
        }
    }

    // ======= TEXT SUMMARY TESTS =======

    @Nested
    @DisplayName("Text Summary Generation")
    class TextSummaryTests {

        @Test
        @DisplayName("Empty item data returns empty string")
        void emptyItemDataReturnsEmptyString() {
            String summary = AffixTooltipProvider.generateTextSummary(HyforgedItemData.EMPTY);
            assertEquals("", summary);
        }

        @Test
        @DisplayName("Single affix generates summary with header")
        void singleAffixGeneratesSummaryWithHeader() {
            HyforgedItemData itemData = new HyforgedItemData(1, List.of(
                createAffix("sturdy", 1, 75)
            ));

            String summary = AffixTooltipProvider.generateTextSummary(itemData);
            assertTrue(summary.contains("Affixes"));
            assertTrue(summary.contains("[T1] Sturdy: +75 Health"));
        }

        @Test
        @DisplayName("Multiple affixes are separated by newlines")
        void multipleAffixesSeparatedByNewlines() {
            HyforgedItemData itemData = new HyforgedItemData(1, List.of(
                createAffix("sturdy", 1, 50),
                createAffix("sharp", 1, 15)
            ));

            String summary = AffixTooltipProvider.generateTextSummary(itemData);
            String[] lines = summary.split("\n");
            assertEquals(3, lines.length); // header + 2 affixes
        }

        @Test
        @DisplayName("Forged affix has separate section")
        void forgedAffixHasSeparateSection() {
            HyforgedItemData itemData = new HyforgedItemData(1, List.of(
                createAffix("sturdy", 1, 50),
                createAffix("masterwork", 1, 20)
            ));

            String summary = AffixTooltipProvider.generateTextSummary(itemData);
            assertTrue(summary.contains("Affixes"));
            assertTrue(summary.contains("Forged Properties"));
        }
    }

    // ======= EDGE CASE TESTS =======

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Null itemData throws exception")
        void nullItemDataThrows() {
            assertThrows(NullPointerException.class, 
                () -> AffixTooltipProvider.generateTooltip((HyforgedItemData) null));
        }

        @Test
        @DisplayName("Null affix list throws exception")
        void nullAffixListThrows() {
            assertThrows(NullPointerException.class, 
                () -> AffixTooltipProvider.generateTooltip((List<RolledAffix>) null));
        }

        @Test
        @DisplayName("Unknown affix is skipped gracefully")
        void unknownAffixIsSkipped() {
            Map<String, RolledAffix.RolledStat> unknownStats = new HashMap<>();
            unknownStats.put("hyforged:unknown-stat", new RolledAffix.RolledStat(50, HyforgedModifier.StackType.FLAT));
            RolledAffix unknownAffix = new RolledAffix(
                "unknown-affix",
                "unknown-type",
                1,
                unknownStats
            );

            // Should not throw, just skip the unknown affix
            TooltipContent content = AffixTooltipProvider.generateTooltip(List.of(unknownAffix));
            assertFalse(content.hasContent());
        }

        @Test
        @DisplayName("Mixed known and unknown affixes handles correctly")
        void mixedKnownUnknownAffixes() {
            RolledAffix known = createAffix("sturdy", 1, 50);
            Map<String, RolledAffix.RolledStat> unknownStats = new HashMap<>();
            unknownStats.put("hyforged:unknown", new RolledAffix.RolledStat(25, HyforgedModifier.StackType.FLAT));
            RolledAffix unknown = new RolledAffix(
                "unknown",
                "unknown",
                1,
                unknownStats
            );

            TooltipContent content = AffixTooltipProvider.generateTooltip(List.of(known, unknown));
            assertEquals(1, content.regularAffixes().size());
        }

        @Test
        @DisplayName("Zero value affix formats correctly")
        void zeroValueAffixFormats() {
            String line = AffixTooltipProvider.formatAffixLine(
                1, "Neutral", 0, HyforgedModifier.StackType.FLAT, "Stat"
            );
            assertEquals("[T1] Neutral: +0 Stat", line);
        }

        @Test
        @DisplayName("Very large value formats correctly")
        void largeValueFormats() {
            String line = AffixTooltipProvider.formatAffixLine(
                1, "Massive", 999999, HyforgedModifier.StackType.FLAT, "Damage"
            );
            assertEquals("[T1] Massive: +999999 Damage", line);
        }
    }
}
