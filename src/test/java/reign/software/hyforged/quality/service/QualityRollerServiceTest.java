package reign.software.hyforged.quality.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.quality.model.QualityEligibilityRule;
import reign.software.hyforged.quality.model.QualityModifierOverrides;
import reign.software.hyforged.quality.model.QualityRollContext;
import reign.software.hyforged.quality.model.QualityWeightProfile;
import reign.software.hyforged.quality.registry.QualityEligibilityRegistry;
import reign.software.hyforged.quality.registry.QualityModifierRegistry;
import reign.software.hyforged.quality.registry.QualityWeightRegistry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QualityRollerService}.
 */
@DisplayName("QualityRollerService")
class QualityRollerServiceTest {

    private QualityRollerService service;

    @BeforeEach
    void setUp() {
        // Reset singletons for test isolation
        QualityEligibilityRegistry.reset();
        QualityWeightRegistry.reset();
        QualityModifierRegistry.reset();

        QualityEligibilityRegistry eligibilityRegistry = QualityEligibilityRegistry.get();
        QualityWeightRegistry weightRegistry = QualityWeightRegistry.get();
        QualityModifierRegistry modifierRegistry = QualityModifierRegistry.get();

        // Register default weight profile
        weightRegistry.register(new QualityWeightProfile(
            "default",
            "Default weights",
            Map.of(
                "Common", 500,
                "Uncommon", 300,
                "Rare", 150,
                "Epic", 40,
                "Legendary", 10
            ),
            List.of("Common", "Uncommon", "Rare", "Epic", "Legendary")
        ));

        // Register an eligibility rule for weapons
        eligibilityRegistry.register(new QualityEligibilityRule(
            "weapons",
            100,
            "All weapons get quality",
            "default",
            new QualityEligibilityRule.AppliesTo(
                List.of("Items.Weapon"),
                List.of(),
                List.of()
            ),
            new QualityEligibilityRule.Excludes(List.of(), List.of()),
            new QualityEligibilityRule.SourceFilter(List.of(), List.of()),
            QualityModifierOverrides.EMPTY
        ));

        service = new QualityRollerService(eligibilityRegistry, weightRegistry, modifierRegistry);
    }

    @Nested
    @DisplayName("rollQuality")
    class RollQualityTests {

        @Test
        @DisplayName("returns null for ineligible items")
        void returnsNullForIneligibleItems() {
            QualityRollContext context = new QualityRollContext(
                "hytale:apple",
                new String[]{"Items.Consumable"},
                new String[]{},
                new String[]{},
                null,
                0,
                null,
                null,
                null,
                null
            );

            String result = service.rollQuality(context);

            assertNull(result);
        }

        @Test
        @DisplayName("returns quality for eligible items")
        void returnsQualityForEligibleItems() {
            QualityRollContext context = new QualityRollContext(
                "hytale:iron_sword",
                new String[]{"Items.Weapon"},
                new String[]{},
                new String[]{},
                null,
                0,
                null,
                null,
                null,
                null
            );

            String result = service.rollQuality(context, 12345L);

            assertNotNull(result);
            assertTrue(
                List.of("Common", "Uncommon", "Rare", "Epic", "Legendary").contains(result),
                "Rolled quality should be one of the eligible qualities: " + result
            );
        }

        @Test
        @DisplayName("deterministic with same seed")
        void deterministicWithSameSeed() {
            QualityRollContext context = new QualityRollContext(
                "hytale:iron_sword",
                new String[]{"Items.Weapon"},
                new String[]{},
                new String[]{},
                null,
                0,
                null,
                null,
                null,
                null
            );

            String first = service.rollQuality(context, 99999L);
            String second = service.rollQuality(context, 99999L);

            assertEquals(first, second);
        }

        @Test
        @DisplayName("different results with different seeds")
        void differentResultsWithDifferentSeeds() {
            QualityRollContext context = new QualityRollContext(
                "hytale:iron_sword",
                new String[]{"Items.Weapon"},
                new String[]{},
                new String[]{},
                null,
                0,
                null,
                null,
                null,
                null
            );

            // Roll many times with different seeds to verify distribution works
            java.util.Set<String> results = new java.util.HashSet<>();
            for (int i = 0; i < 100; i++) {
                String result = service.rollQuality(context, (long) i * 12345);
                if (result != null) {
                    results.add(result);
                }
            }

            // Should get at least 2 different qualities across 100 rolls
            assertTrue(results.size() >= 2, "Should roll different qualities with different seeds");
        }
    }

    @Nested
    @DisplayName("getEligibleQualities")
    class GetEligibleQualitiesTests {

        @Test
        @DisplayName("returns empty for ineligible items")
        void returnsEmptyForIneligibleItems() {
            QualityRollContext context = new QualityRollContext(
                "hytale:apple",
                new String[]{"Items.Consumable"},
                new String[]{},
                new String[]{},
                null,
                0,
                null,
                null,
                null,
                null
            );

            List<String> result = service.getEligibleQualities(context);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns ordered qualities for eligible items")
        void returnsOrderedQualitiesForEligibleItems() {
            QualityRollContext context = new QualityRollContext(
                "hytale:iron_sword",
                new String[]{"Items.Weapon"},
                new String[]{},
                new String[]{},
                null,
                0,
                null,
                null,
                null,
                null
            );

            List<String> result = service.getEligibleQualities(context);

            assertFalse(result.isEmpty());
            assertEquals(5, result.size());
            // Should be ordered by quality order (best first)
            assertEquals("Legendary", result.get(0));
            assertEquals("Epic", result.get(1));
            assertEquals("Rare", result.get(2));
            assertEquals("Uncommon", result.get(3));
            assertEquals("Common", result.get(4));
        }
    }
}
