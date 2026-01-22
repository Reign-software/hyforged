package reign.software.hyforged.combat.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.combat.scaling.ScaledStatEntry;
import reign.software.hyforged.combat.scaling.WorldScalingConfig;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for monster scaling system.
 * Tests distance-based level calculation and stat scaling.
 */
@DisplayName("Monster Scaling Integration Tests")
class MonsterScalingIntegrationTest {

    @Nested
    @DisplayName("World Scaling Config Tests")
    class WorldScalingConfigTests {

        @Test
        @DisplayName("Linear scaling: 500 blocks per level")
        void linearScaling_500BlocksPerLevel() {
            WorldScalingConfig config = new WorldScalingConfig(
                    "test",
                    WorldScalingConfig.ScalingCurve.LINEAR,
                    500,
                    1,
                    100
            );

            // At spawn
            assertEquals(1, config.calculateLevel(0), "Level 1 at spawn");

            // 500 blocks = level 2
            assertEquals(2, config.calculateLevel(500), "Level 2 at 500 blocks");

            // 2500 blocks = level 6
            assertEquals(6, config.calculateLevel(2500), "Level 6 at 2500 blocks");

            // 50000 blocks = level 100 (capped)
            assertEquals(100, config.calculateLevel(50000), "Capped at level 100");
        }

        @Test
        @DisplayName("Logarithmic scaling: slower progression at distance")
        void logarithmicScaling_slowerAtDistance() {
            WorldScalingConfig config = new WorldScalingConfig(
                    "test-log",
                    WorldScalingConfig.ScalingCurve.LOGARITHMIC,
                    500,
                    1,
                    100
            );

            int level1 = config.calculateLevel(0);
            int level2 = config.calculateLevel(1000);
            int level3 = config.calculateLevel(10000);
            int level4 = config.calculateLevel(50000);

            // Verify progression
            assertTrue(level1 <= level2);
            assertTrue(level2 <= level3);
            assertTrue(level3 <= level4);
        }

        @Test
        @DisplayName("Stepped scaling: discrete level bands")
        void steppedScaling_discreteLevelBands() {
            WorldScalingConfig config = new WorldScalingConfig(
                    "test-stepped",
                    WorldScalingConfig.ScalingCurve.STEPPED,
                    1000,
                    1,
                    50
            );

            // Within first band
            assertEquals(1, config.calculateLevel(0));
            assertEquals(1, config.calculateLevel(500));
            assertEquals(1, config.calculateLevel(999));

            // Second band
            assertEquals(2, config.calculateLevel(1000));
            assertEquals(2, config.calculateLevel(1999));

            // Third band
            assertEquals(3, config.calculateLevel(2000));
        }

        @Test
        @DisplayName("Level bounds: respects min and max")
        void levelBounds_respectsMinMax() {
            WorldScalingConfig config = new WorldScalingConfig(
                    "test-bounds",
                    WorldScalingConfig.ScalingCurve.LINEAR,
                    100,
                    5,
                    20
            );

            // Minimum level is 5
            assertEquals(5, config.calculateLevel(0), "Minimum level is 5");

            // Maximum level is 20
            assertEquals(20, config.calculateLevel(10000), "Maximum level is 20");
        }

        @Test
        @DisplayName("Negative distance treated as min level")
        void negativeDistance_treatedAsMinLevel() {
            WorldScalingConfig config = new WorldScalingConfig(
                    "test-neg",
                    WorldScalingConfig.ScalingCurve.LINEAR,
                    500,
                    1,
                    100
            );

            assertEquals(1, config.calculateLevel(-500), "Negative distance = min level");
        }
        
        @Test
        @DisplayName("Default factory creates sensible config")
        void createDefault_sensibleConfig() {
            WorldScalingConfig config = WorldScalingConfig.createDefault("default-test");
            
            assertEquals("default-test", config.id());
            assertEquals(WorldScalingConfig.ScalingCurve.LINEAR, config.curve());
            assertEquals(WorldScalingConfig.DEFAULT_BLOCKS_PER_LEVEL, config.blocksPerLevel());
            assertEquals(WorldScalingConfig.DEFAULT_MIN_LEVEL, config.minLevel());
            assertEquals(WorldScalingConfig.DEFAULT_MAX_LEVEL, config.maxLevel());
        }
    }

    @Nested
    @DisplayName("Scaled Stat Entry Tests")
    class ScaledStatEntryTests {

        @Test
        @DisplayName("Flat modifier: linear scaling per level")
        void flatModifier_linearScaling() {
            ScaledStatEntry entry = ScaledStatEntry.flat("hyforged:max-health", 100);

            // Level 1 (min level 1) = 0 levels above min = 0 bonus
            assertEquals(0, entry.calculateModifierValue(1, 1));

            // Level 10 (min level 1) = 9 levels above min = 900 bonus
            assertEquals(900, entry.calculateModifierValue(10, 1));

            // Level 50 (min level 1) = 49 levels above min = 4900 bonus
            assertEquals(4900, entry.calculateModifierValue(50, 1));
        }

        @Test
        @DisplayName("Increased modifier: percentage scaling converted to bps")
        void increasedModifier_percentageScaling() {
            // 2% per level -> 200 bps per level
            ScaledStatEntry entry = ScaledStatEntry.increased("hyforged:physical-damage-bps", 2);

            // Level 1 = 0 bonus
            assertEquals(0, entry.calculateModifierValue(1, 1));

            // Level 10 = 9 levels * 2 * 100 = 1800 bps (18%)
            assertEquals(1800, entry.calculateModifierValue(10, 1));
        }

        @Test
        @DisplayName("More modifier: multiplicative scaling")
        void moreModifier_multiplicativeScaling() {
            // 1% more per level -> 100 bps per level
            ScaledStatEntry entry = ScaledStatEntry.more("hyforged:damage-taken-bps", 1);

            // Level 1 = 0 bonus
            assertEquals(0, entry.calculateModifierValue(1, 1));

            // Level 20 = 19 levels * 1 * 100 = 1900 bps
            assertEquals(1900, entry.calculateModifierValue(20, 1));
        }

        @Test
        @DisplayName("Modifier type getter works")
        void modifierType_getter() {
            ScaledStatEntry flatEntry = ScaledStatEntry.flat("test", 100);
            ScaledStatEntry incEntry = ScaledStatEntry.increased("test", 10);
            ScaledStatEntry moreEntry = ScaledStatEntry.more("test", 5);
            
            assertEquals(HyforgedModifier.StackType.FLAT, flatEntry.getModifierType());
            assertEquals(HyforgedModifier.StackType.INCREASED, incEntry.getModifierType());
            assertEquals(HyforgedModifier.StackType.MORE, moreEntry.getModifierType());
        }
        
        @Test
        @DisplayName("StatId getter works")
        void statId_getter() {
            ScaledStatEntry entry = ScaledStatEntry.flat("hyforged:max-health", 100);
            assertEquals("hyforged:max-health", entry.getStatId());
        }
        
        @Test
        @DisplayName("ScalePerLevel getter works")
        void scalePerLevel_getter() {
            ScaledStatEntry entry = ScaledStatEntry.flat("hyforged:max-health", 150);
            assertEquals(150, entry.getScalePerLevel());
        }
    }

    @Nested
    @DisplayName("Monster Level Component Tests")
    class MonsterLevelComponentTests {

        @Test
        @DisplayName("Level calculation from world position")
        void levelCalculation_fromWorldPosition() {
            WorldScalingConfig config = new WorldScalingConfig(
                    "test-pos",
                    WorldScalingConfig.ScalingCurve.LINEAR,
                    500,
                    1,
                    100
            );

            // Spawn at (0, 0, 0)
            double spawnX = 0, spawnZ = 0;

            // Monster at (1500, y, 2000)
            double monsterX = 1500, monsterZ = 2000;

            // Distance = sqrt(1500^2 + 2000^2) = sqrt(2250000 + 4000000) = sqrt(6250000) = 2500
            double distance = Math.sqrt(
                    Math.pow(monsterX - spawnX, 2) + Math.pow(monsterZ - spawnZ, 2)
            );
            assertEquals(2500, distance, 0.01);

            // Level = distance / blocksPerLevel + 1 = 2500 / 500 + 1 = 6
            int level = config.calculateLevel(distance);
            assertEquals(6, level);
        }

        @Test
        @DisplayName("2D distance ignores Y coordinate")
        void distance_ignoresYCoordinate() {
            double spawnX = 0, spawnZ = 0;

            // Monster high in the air but close horizontally
            double monsterX = 100, monsterZ = 0;
            @SuppressWarnings("unused")
            double monsterY = 500; // Y is intentionally ignored

            double distance2D = Math.sqrt(
                    Math.pow(monsterX - spawnX, 2) + Math.pow(monsterZ - spawnZ, 2)
            );

            // Y coordinate ignored
            assertEquals(100, distance2D, 0.01);
        }
    }

    @Nested
    @DisplayName("Stat Scaling Application Tests")
    class StatScalingApplicationTests {

        @Test
        @DisplayName("Tank monster: high health scaling")
        void tankMonster_highHealthScaling() {
            // Tank config: 200 health per level
            ScaledStatEntry healthEntry = ScaledStatEntry.flat("hyforged:max-health", 200);

            // Level 25 monster
            int healthBonus = healthEntry.calculateModifierValue(25, 1);

            // (25 - 1) * 200 = 4800
            assertEquals(4800, healthBonus);
        }

        @Test
        @DisplayName("Glass cannon: high damage, low health scaling")
        void glassCannon_highDamageLowHealth() {
            // 3% damage per level, 50 flat health per level
            ScaledStatEntry damageEntry = ScaledStatEntry.increased("hyforged:physical-damage-bps", 3);
            ScaledStatEntry healthEntry = ScaledStatEntry.flat("hyforged:max-health", 50);

            int level = 20;
            int damageBonus = damageEntry.calculateModifierValue(level, 1);
            int healthBonus = healthEntry.calculateModifierValue(level, 1);

            // Damage: (20 - 1) * 3 * 100 = 5700 bps (57%)
            assertEquals(5700, damageBonus);

            // Health: (20 - 1) * 50 = 950
            assertEquals(950, healthBonus);

            // Verify glass cannon has more damage scaling than health
            assertTrue(damageBonus > healthBonus * 5, "Glass cannon should have high damage:health ratio");
        }

        @Test
        @DisplayName("Resistance scaling: flat addition")
        void resistanceScaling_flatAddition() {
            ScaledStatEntry resistEntry = ScaledStatEntry.flat("hyforged:fire-resistance-bps", 100);

            // Level 30 monster
            int resistBonus = resistEntry.calculateModifierValue(30, 1);

            // (30 - 1) * 100 = 2900 bps = 29% resistance
            assertEquals(2900, resistBonus);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Level 1 monster: no stat bonuses")
        void level1Monster_noStatBonuses() {
            ScaledStatEntry entry = ScaledStatEntry.flat("hyforged:max-health", 100);

            int bonus = entry.calculateModifierValue(1, 1);
            assertEquals(0, bonus, "Level 1 (min) has no bonus");
        }

        @Test
        @DisplayName("Very far distance: capped at max level")
        void veryFarDistance_cappedAtMaxLevel() {
            WorldScalingConfig config = new WorldScalingConfig(
                    "test-far",
                    WorldScalingConfig.ScalingCurve.LINEAR,
                    500,
                    1,
                    50
            );

            // 100000 blocks would be level 201, but capped at 50
            assertEquals(50, config.calculateLevel(100000));
        }

        @Test
        @DisplayName("Small blocksPerLevel still works")
        void smallBlocksPerLevel_stillWorks() {
            WorldScalingConfig config = new WorldScalingConfig(
                    "test-small",
                    WorldScalingConfig.ScalingCurve.LINEAR,
                    1, // 1 block per level
                    1,
                    100
            );

            // Should work normally - 100 blocks = level 101, capped at 100
            assertEquals(100, config.calculateLevel(100));
        }
        
        @Test
        @DisplayName("Level below min still at min")
        void levelBelowMin_atMin() {
            ScaledStatEntry entry = ScaledStatEntry.flat("hyforged:max-health", 100);
            
            // Level 3 with minLevel 5 = no bonus
            int bonus = entry.calculateModifierValue(3, 5);
            assertEquals(0, bonus, "Below min level = no bonus");
        }
    }
}
