package reign.software.hyforged.currency.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SellValueConfig}.
 * <p>
 * Tests sell value calculation formula and rarity/affix tier lookups.
 */
@DisplayName("SellValueConfig")
class SellValueConfigTest {

    private SellValueConfig config;

    @BeforeEach
    void setUp() {
        // Get the singleton (uses fallback defaults when no asset is loaded)
        config = SellValueConfig.get();
    }

    @Nested
    @DisplayName("Rarity Multipliers (fallback defaults)")
    class RarityMultiplierTests {

        @Test
        @DisplayName("returns 0 for junk items")
        void junkRarity() {
            assertEquals(0, config.getRarityMultiplier("junk"));
            assertEquals(0, config.getRarityMultiplier("Junk"));
            assertEquals(0, config.getRarityMultiplier("JUNK"));
        }

        @Test
        @DisplayName("returns 1 for common items")
        void commonRarity() {
            assertEquals(1, config.getRarityMultiplier("common"));
            assertEquals(1, config.getRarityMultiplier("Common"));
        }

        @Test
        @DisplayName("returns 2 for uncommon items")
        void uncommonRarity() {
            assertEquals(2, config.getRarityMultiplier("uncommon"));
            assertEquals(2, config.getRarityMultiplier("Uncommon"));
        }

        @Test
        @DisplayName("returns 5 for rare items")
        void rareRarity() {
            assertEquals(5, config.getRarityMultiplier("rare"));
            assertEquals(5, config.getRarityMultiplier("Rare"));
        }

        @Test
        @DisplayName("returns 15 for epic items")
        void epicRarity() {
            assertEquals(15, config.getRarityMultiplier("epic"));
            assertEquals(15, config.getRarityMultiplier("Epic"));
        }

        @Test
        @DisplayName("returns 50 for legendary items")
        void legendaryRarity() {
            assertEquals(50, config.getRarityMultiplier("legendary"));
            assertEquals(50, config.getRarityMultiplier("Legendary"));
        }

        @Test
        @DisplayName("returns 1 for unknown rarity")
        void unknownRarity() {
            assertEquals(1, config.getRarityMultiplier("mythic"));
            assertEquals(1, config.getRarityMultiplier("Unknown"));
            assertEquals(1, config.getRarityMultiplier(""));
        }
    }

    @Nested
    @DisplayName("Affix Tier Values (fallback defaults)")
    class AffixTierValueTests {

        @Test
        @DisplayName("tier 1 affixes are worth 100")
        void tier1() {
            assertEquals(100, config.getAffixValueForTier(1));
        }

        @Test
        @DisplayName("tier 2 affixes are worth 50")
        void tier2() {
            assertEquals(50, config.getAffixValueForTier(2));
        }

        @Test
        @DisplayName("tier 3 affixes are worth 25")
        void tier3() {
            assertEquals(25, config.getAffixValueForTier(3));
        }

        @Test
        @DisplayName("tier 4 affixes are worth 10")
        void tier4() {
            assertEquals(10, config.getAffixValueForTier(4));
        }

        @Test
        @DisplayName("tier 5 affixes are worth 5")
        void tier5() {
            assertEquals(5, config.getAffixValueForTier(5));
        }

        @Test
        @DisplayName("invalid tiers are worth 0")
        void invalidTiers() {
            assertEquals(0, config.getAffixValueForTier(0));
            assertEquals(0, config.getAffixValueForTier(-1));
            assertEquals(0, config.getAffixValueForTier(6));
            assertEquals(0, config.getAffixValueForTier(100));
        }
    }

    @Nested
    @DisplayName("Sell Value Calculation")
    class SellValueCalculationTests {

        @Test
        @DisplayName("override value takes precedence over formula")
        void overrideTakesPrecedence() {
            // Override value should be returned directly
            assertEquals(999, config.calculateSellValue("Common", 5, 1, 999));
            assertEquals(0, config.calculateSellValue("Legendary", 10, 1, 0));
            assertEquals(1, config.calculateSellValue("Rare", 3, 2, 1));
        }

        @Test
        @DisplayName("calculates correctly for common item with no affixes")
        void commonNoAffixes() {
            // Formula: baseValue(1) + (rarityMult(1) × baseValue(1)) + (affixValue(0) × affixCount(0))
            // = 1 + 1 + 0 = 2
            int value = config.calculateSellValue("Common", 0, 1, -1);
            assertEquals(2, value);
        }

        @Test
        @DisplayName("calculates correctly for rare item with tier 1 affixes")
        void rareWithTier1Affixes() {
            // Formula: baseValue(1) + (rarityMult(5) × baseValue(1)) + (affixValue(100) × affixCount(3))
            // = 1 + 5 + 300 = 306
            int value = config.calculateSellValue("Rare", 3, 1, -1);
            assertEquals(306, value);
        }

        @Test
        @DisplayName("calculates correctly for legendary item with tier 2 affixes")
        void legendaryWithTier2Affixes() {
            // Formula: baseValue(1) + (rarityMult(50) × baseValue(1)) + (affixValue(50) × affixCount(4))
            // = 1 + 50 + 200 = 251
            int value = config.calculateSellValue("Legendary", 4, 2, -1);
            assertEquals(251, value);
        }

        @Test
        @DisplayName("calculates correctly for epic item with tier 3 affixes")
        void epicWithTier3Affixes() {
            // Formula: baseValue(1) + (rarityMult(15) × baseValue(1)) + (affixValue(25) × affixCount(5))
            // = 1 + 15 + 125 = 141
            int value = config.calculateSellValue("Epic", 5, 3, -1);
            assertEquals(141, value);
        }

        @Test
        @DisplayName("junk items have minimal value")
        void junkItemsMinimalValue() {
            // Formula: baseValue(1) + (rarityMult(0) × baseValue(1)) + (affixValue(0) × affixCount(0))
            // = 1 + 0 + 0 = 1
            int value = config.calculateSellValue("Junk", 0, 1, -1);
            assertEquals(1, value);
        }

        @Test
        @DisplayName("respects minimum sell value")
        void respectsMinimumSellValue() {
            // Even with 0 rarity multiplier, min sell value should apply
            int value = config.calculateSellValue("Junk", 0, 1, -1);
            assertTrue(value >= config.getMinSellValue());
        }

        @Test
        @DisplayName("affixes scale with count")
        void affixesScaleWithCount() {
            int value1Affix = config.calculateSellValue("Common", 1, 1, -1);
            int value3Affix = config.calculateSellValue("Common", 3, 1, -1);
            int value5Affix = config.calculateSellValue("Common", 5, 1, -1);

            assertTrue(value3Affix > value1Affix, "3 affixes should be worth more than 1");
            assertTrue(value5Affix > value3Affix, "5 affixes should be worth more than 3");
            
            // Difference should be proportional: tier 1 affix = 100 each
            assertEquals(200, value3Affix - value1Affix);
            assertEquals(200, value5Affix - value3Affix);
        }

        @Test
        @DisplayName("higher tier affixes are worth less")
        void higherTierAffixesWorthLess() {
            // Same item, same affix count, different tiers
            int tier1Value = config.calculateSellValue("Common", 2, 1, -1);
            int tier3Value = config.calculateSellValue("Common", 2, 3, -1);
            int tier5Value = config.calculateSellValue("Common", 2, 5, -1);

            assertTrue(tier1Value > tier3Value, "Tier 1 affixes should be worth more than tier 3");
            assertTrue(tier3Value > tier5Value, "Tier 3 affixes should be worth more than tier 5");
        }
    }

    @Nested
    @DisplayName("Getters")
    class GetterTests {

        @Test
        @DisplayName("getBaseValue returns default value")
        void getBaseValue() {
            assertEquals(1, config.getBaseValue());
        }

        @Test
        @DisplayName("getMinSellValue returns default value")
        void getMinSellValue() {
            assertEquals(1, config.getMinSellValue());
        }
    }
}
