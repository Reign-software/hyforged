package reign.software.hyforged.quality.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.quality.model.QualityWeightTable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QualityWeightTable}.
 */
@DisplayName("QualityWeightTable")
class QualityWeightTableTest {

    @Nested
    @DisplayName("fromWeights")
    class FromWeightsTests {

        @Test
        @DisplayName("creates table from valid weights")
        void createsTableFromValidWeights() {
            Map<String, Integer> weights = Map.of(
                "Common", 500,
                "Rare", 100,
                "Epic", 50
            );

            QualityWeightTable table = QualityWeightTable.fromWeights(weights);

            assertEquals(650, table.totalWeight());
        }

        @Test
        @DisplayName("filters out null keys")
        void filtersNullKeys() {
            Map<String, Integer> weights = new HashMap<>();
            weights.put("Common", 500);
            weights.put(null, 100);

            QualityWeightTable table = QualityWeightTable.fromWeights(weights);

            assertEquals(500, table.totalWeight());
        }

        @Test
        @DisplayName("filters out blank keys")
        void filtersBlankKeys() {
            Map<String, Integer> weights = new HashMap<>();
            weights.put("Common", 500);
            weights.put("", 100);
            weights.put("  ", 50);

            QualityWeightTable table = QualityWeightTable.fromWeights(weights);

            assertEquals(500, table.totalWeight());
        }

        @Test
        @DisplayName("filters out zero weights")
        void filtersZeroWeights() {
            Map<String, Integer> weights = Map.of(
                "Common", 500,
                "Rare", 0
            );

            QualityWeightTable table = QualityWeightTable.fromWeights(weights);

            assertEquals(500, table.totalWeight());
        }

        @Test
        @DisplayName("filters out negative weights")
        void filtersNegativeWeights() {
            Map<String, Integer> weights = new HashMap<>();
            weights.put("Common", 500);
            weights.put("Rare", -100);

            QualityWeightTable table = QualityWeightTable.fromWeights(weights);

            assertEquals(500, table.totalWeight());
        }

        @Test
        @DisplayName("handles empty map")
        void handlesEmptyMap() {
            QualityWeightTable table = QualityWeightTable.fromWeights(Collections.emptyMap());

            assertEquals(0, table.totalWeight());
            assertNull(table.roll(new Random()));
        }
    }

    @Nested
    @DisplayName("roll")
    class RollTests {

        @Test
        @DisplayName("returns null for empty table")
        void returnsNullForEmptyTable() {
            QualityWeightTable table = QualityWeightTable.fromWeights(Collections.emptyMap());

            assertNull(table.roll(new Random()));
        }

        @Test
        @DisplayName("returns only quality for single-entry table")
        void returnsOnlyQualityForSingleEntry() {
            QualityWeightTable table = QualityWeightTable.fromWeights(Map.of("Common", 100));

            for (int i = 0; i < 10; i++) {
                assertEquals("Common", table.roll(new Random()));
            }
        }

        @Test
        @DisplayName("deterministic with seeded random")
        void deterministicWithSeededRandom() {
            Map<String, Integer> weights = Map.of(
                "Common", 500,
                "Uncommon", 300,
                "Rare", 150,
                "Epic", 40,
                "Legendary", 10
            );
            QualityWeightTable table = QualityWeightTable.fromWeights(weights);

            String first = table.roll(new Random(12345L));
            String second = table.roll(new Random(12345L));

            assertEquals(first, second);
        }

        @Test
        @DisplayName("respects weight distribution")
        void respectsWeightDistribution() {
            Map<String, Integer> weights = Map.of(
                "Common", 900,
                "Rare", 100
            );
            QualityWeightTable table = QualityWeightTable.fromWeights(weights);

            int commonCount = 0;
            int rareCount = 0;
            Random random = new Random(42L);

            for (int i = 0; i < 1000; i++) {
                String result = table.roll(random);
                if ("Common".equals(result)) {
                    commonCount++;
                } else if ("Rare".equals(result)) {
                    rareCount++;
                }
            }

            // Common should be ~90%, Rare ~10%
            assertTrue(commonCount > 800, "Common should be rolled >80% of the time, got " + commonCount);
            assertTrue(rareCount > 50, "Rare should be rolled >5% of the time, got " + rareCount);
            assertEquals(1000, commonCount + rareCount);
        }

        @Test
        @DisplayName("all qualities are rollable")
        void allQualitiesAreRollable() {
            Map<String, Integer> weights = Map.of(
                "Common", 100,
                "Uncommon", 100,
                "Rare", 100
            );
            QualityWeightTable table = QualityWeightTable.fromWeights(weights);

            Map<String, Integer> counts = new HashMap<>();
            Random random = new Random(999L);

            for (int i = 0; i < 300; i++) {
                String result = table.roll(random);
                counts.merge(result, 1, Integer::sum);
            }

            assertTrue(counts.containsKey("Common"), "Common should be rolled at least once");
            assertTrue(counts.containsKey("Uncommon"), "Uncommon should be rolled at least once");
            assertTrue(counts.containsKey("Rare"), "Rare should be rolled at least once");
        }
    }

    @Nested
    @DisplayName("totalWeight")
    class TotalWeightTests {

        @Test
        @DisplayName("sums all valid weights")
        void sumsAllValidWeights() {
            Map<String, Integer> weights = Map.of(
                "Common", 500,
                "Uncommon", 300,
                "Rare", 150,
                "Epic", 40,
                "Legendary", 10
            );

            QualityWeightTable table = QualityWeightTable.fromWeights(weights);

            assertEquals(1000, table.totalWeight());
        }

        @Test
        @DisplayName("returns zero for all-filtered weights")
        void returnsZeroForAllFilteredWeights() {
            Map<String, Integer> weights = Map.of(
                "Common", 0,
                "Rare", 0
            );

            QualityWeightTable table = QualityWeightTable.fromWeights(weights);

            assertEquals(0, table.totalWeight());
        }
    }
}
