package reign.software.hyforged.quality.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QualityEligibilityRule}.
 */
@DisplayName("QualityEligibilityRule")
class QualityEligibilityRuleTest {

    @Nested
    @DisplayName("matches")
    class MatchesTests {

        @Test
        @DisplayName("matches when all conditions met")
        void matchesWhenAllConditionsMet() {
            QualityEligibilityRule rule = createRule(
                new QualityEligibilityRule.AppliesTo(
                    List.of("Items.Weapon"),
                    List.of(),
                    List.of()
                ),
                new QualityEligibilityRule.Excludes(List.of(), List.of()),
                new QualityEligibilityRule.SourceFilter(List.of(), List.of())
            );

            QualityRollContext context = new QualityRollContext(
                "hytale:iron_sword",
                new String[]{"Items.Weapon"},
                new String[]{},
                new String[]{},
                null, 0, null, null, null, null
            );

            assertTrue(rule.matches(context));
        }

        @Test
        @DisplayName("does not match when category missing")
        void doesNotMatchWhenCategoryMissing() {
            QualityEligibilityRule rule = createRule(
                new QualityEligibilityRule.AppliesTo(
                    List.of("Items.Weapon"),
                    List.of(),
                    List.of()
                ),
                new QualityEligibilityRule.Excludes(List.of(), List.of()),
                new QualityEligibilityRule.SourceFilter(List.of(), List.of())
            );

            QualityRollContext context = new QualityRollContext(
                "hytale:apple",
                new String[]{"Items.Consumable"},
                new String[]{},
                new String[]{},
                null, 0, null, null, null, null
            );

            assertFalse(rule.matches(context));
        }

        @Test
        @DisplayName("matches with tag filter")
        void matchesWithTagFilter() {
            QualityEligibilityRule rule = createRule(
                new QualityEligibilityRule.AppliesTo(
                    List.of(),
                    List.of("Type:Weapon"),
                    List.of()
                ),
                new QualityEligibilityRule.Excludes(List.of(), List.of()),
                new QualityEligibilityRule.SourceFilter(List.of(), List.of())
            );

            QualityRollContext context = new QualityRollContext(
                "hytale:iron_sword",
                new String[]{"Items.Weapon"},
                new String[]{"Type:Weapon", "Material:Iron"},
                new String[]{},
                null, 0, null, null, null, null
            );

            assertTrue(rule.matches(context));
        }

        @Test
        @DisplayName("excluded by tag")
        void excludedByTag() {
            QualityEligibilityRule rule = createRule(
                new QualityEligibilityRule.AppliesTo(
                    List.of("Items.Weapon"),
                    List.of(),
                    List.of()
                ),
                new QualityEligibilityRule.Excludes(List.of("NoQuality"), List.of()),
                new QualityEligibilityRule.SourceFilter(List.of(), List.of())
            );

            QualityRollContext context = new QualityRollContext(
                "hytale:training_sword",
                new String[]{"Items.Weapon"},
                new String[]{"NoQuality"},
                new String[]{},
                null, 0, null, null, null, null
            );

            assertFalse(rule.matches(context));
        }

        @Test
        @DisplayName("excluded by item ID pattern")
        void excludedByItemIdPattern() {
            QualityEligibilityRule rule = createRule(
                new QualityEligibilityRule.AppliesTo(
                    List.of("Items.Weapon"),
                    List.of(),
                    List.of()
                ),
                new QualityEligibilityRule.Excludes(List.of(), List.of("hytale:debug_*")),
                new QualityEligibilityRule.SourceFilter(List.of(), List.of())
            );

            QualityRollContext context = new QualityRollContext(
                "hytale:debug_sword",
                new String[]{"Items.Weapon"},
                new String[]{},
                new String[]{},
                null, 0, null, null, null, null
            );

            assertFalse(rule.matches(context));
        }

        @Test
        @DisplayName("matches source filter")
        void matchesSourceFilter() {
            QualityEligibilityRule rule = createRule(
                new QualityEligibilityRule.AppliesTo(
                    List.of("Items.Weapon"),
                    List.of(),
                    List.of()
                ),
                new QualityEligibilityRule.Excludes(List.of(), List.of()),
                new QualityEligibilityRule.SourceFilter(List.of("boss"), List.of())
            );

            QualityRollContext context = new QualityRollContext(
                "hytale:iron_sword",
                new String[]{"Items.Weapon"},
                new String[]{},
                new String[]{"boss", "dungeon"},
                null, 0, null, null, null, null
            );

            assertTrue(rule.matches(context));
        }

        @Test
        @DisplayName("does not match when source filter fails")
        void doesNotMatchWhenSourceFilterFails() {
            QualityEligibilityRule rule = createRule(
                new QualityEligibilityRule.AppliesTo(
                    List.of("Items.Weapon"),
                    List.of(),
                    List.of()
                ),
                new QualityEligibilityRule.Excludes(List.of(), List.of()),
                new QualityEligibilityRule.SourceFilter(List.of("boss"), List.of())
            );

            QualityRollContext context = new QualityRollContext(
                "hytale:iron_sword",
                new String[]{"Items.Weapon"},
                new String[]{},
                new String[]{"normal", "dungeon"},
                null, 0, null, null, null, null
            );

            assertFalse(rule.matches(context));
        }

        @Test
        @DisplayName("handles null values in context arrays")
        void handlesNullValuesInContextArrays() {
            QualityEligibilityRule rule = createRule(
                new QualityEligibilityRule.AppliesTo(
                    List.of("Items.Weapon"),
                    List.of(),
                    List.of()
                ),
                new QualityEligibilityRule.Excludes(List.of(), List.of()),
                new QualityEligibilityRule.SourceFilter(List.of(), List.of())
            );

            // Arrays with null/blank elements should be safely filtered
            QualityRollContext context = new QualityRollContext(
                "hytale:iron_sword",
                new String[]{"Items.Weapon", null, ""},
                new String[]{null, "", "  "},
                new String[]{null},
                null, 0, null, null, null, null
            );

            assertTrue(rule.matches(context));
        }

        @Test
        @DisplayName("handles duplicate values in context arrays")
        void handlesDuplicateValuesInContextArrays() {
            QualityEligibilityRule rule = createRule(
                new QualityEligibilityRule.AppliesTo(
                    List.of("Items.Weapon"),
                    List.of(),
                    List.of()
                ),
                new QualityEligibilityRule.Excludes(List.of(), List.of()),
                new QualityEligibilityRule.SourceFilter(List.of(), List.of())
            );

            // Duplicate values should not cause Set.of() to throw
            QualityRollContext context = new QualityRollContext(
                "hytale:iron_sword",
                new String[]{"Items.Weapon", "Items.Weapon"},
                new String[]{"Tag1", "Tag1"},
                new String[]{"source", "source"},
                null, 0, null, null, null, null
            );

            assertTrue(rule.matches(context));
        }
    }

    @Nested
    @DisplayName("AppliesTo")
    class AppliesToTests {

        @Test
        @DisplayName("empty filters match anything")
        void emptyFiltersMatchAnything() {
            var appliesTo = new QualityEligibilityRule.AppliesTo(
                List.of(), List.of(), List.of()
            );

            assertTrue(appliesTo.matches(
                java.util.Set.of("Items.Weapon"),
                java.util.Set.of("Type:Sword"),
                "hytale:iron_sword"
            ));
        }

        @Test
        @DisplayName("wildcard item pattern matches all")
        void wildcardItemPatternMatchesAll() {
            var appliesTo = new QualityEligibilityRule.AppliesTo(
                List.of(), List.of(), List.of("*")
            );

            assertTrue(appliesTo.matches(
                java.util.Set.of(),
                java.util.Set.of(),
                "anything:here"
            ));
        }
    }

    @Nested
    @DisplayName("construction")
    class ConstructionTests {

        @Test
        @DisplayName("rejects null id")
        void rejectsNullId() {
            assertThrows(NullPointerException.class, () -> new QualityEligibilityRule(
                null,
                100,
                "description",
                "default",
                new QualityEligibilityRule.AppliesTo(List.of(), List.of(), List.of()),
                new QualityEligibilityRule.Excludes(List.of(), List.of()),
                new QualityEligibilityRule.SourceFilter(List.of(), List.of()),
                QualityModifierOverrides.EMPTY
            ));
        }

        @Test
        @DisplayName("rejects blank id")
        void rejectsBlankId() {
            assertThrows(IllegalArgumentException.class, () -> new QualityEligibilityRule(
                "  ",
                100,
                "description",
                "default",
                new QualityEligibilityRule.AppliesTo(List.of(), List.of(), List.of()),
                new QualityEligibilityRule.Excludes(List.of(), List.of()),
                new QualityEligibilityRule.SourceFilter(List.of(), List.of()),
                QualityModifierOverrides.EMPTY
            ));
        }

        @Test
        @DisplayName("rejects blank weightProfileId")
        void rejectsBlankWeightProfileId() {
            assertThrows(IllegalArgumentException.class, () -> new QualityEligibilityRule(
                "test",
                100,
                "description",
                "",
                new QualityEligibilityRule.AppliesTo(List.of(), List.of(), List.of()),
                new QualityEligibilityRule.Excludes(List.of(), List.of()),
                new QualityEligibilityRule.SourceFilter(List.of(), List.of()),
                QualityModifierOverrides.EMPTY
            ));
        }
    }

    private QualityEligibilityRule createRule(
            QualityEligibilityRule.AppliesTo appliesTo,
            QualityEligibilityRule.Excludes excludes,
            QualityEligibilityRule.SourceFilter sourceFilter
    ) {
        return new QualityEligibilityRule(
            "test-rule",
            100,
            "Test rule",
            "default",
            appliesTo,
            excludes,
            sourceFilter,
            QualityModifierOverrides.EMPTY
        );
    }
}
