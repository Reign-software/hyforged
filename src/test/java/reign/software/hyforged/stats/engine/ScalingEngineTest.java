package reign.software.hyforged.stats.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.scaling.DiminishingScaling;
import reign.software.hyforged.stats.scaling.LinearScaling;
import reign.software.hyforged.stats.scaling.ThresholdScaling;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScalingEngine.
 * <p>
 * Tests stat-to-stat scaling interactions to ensure stats properly buff each other.
 */
class ScalingEngineTest {

    // Test stat IDs
    private static final StatId STRENGTH = StatId.hyforged("strength");
    private static final StatId DEXTERITY = StatId.hyforged("dexterity");
    private static final StatId INTELLIGENCE = StatId.hyforged("intelligence");
    private static final StatId LUCK = StatId.hyforged("luck");
    private static final StatId CONSTITUTION = StatId.hyforged("constitution");
    
    private static final StatId ATTACK_POWER = StatId.hyforged("attack-power");
    private static final StatId CRIT_CHANCE = StatId.hyforged("crit-chance-bps");
    private static final StatId MAX_HEALTH = StatId.hyforged("max-health");
    private static final StatId EVASION = StatId.hyforged("evasion-bps");
    
    @Nested
    @DisplayName("Linear Scaling Tests")
    class LinearScalingTests {
        
        @Test
        @DisplayName("Linear scaling with ratio 2.0 doubles source value")
        void linearScaling_ratio2_doublesValue() {
            LinearScaling scaling = new LinearScaling(STRENGTH, 2.0);
            
            int contribution = scaling.computeContribution(10);
            
            assertEquals(20, contribution, "10 STR * 2.0 ratio = 20 Attack Power");
        }
        
        @Test
        @DisplayName("Linear scaling with ratio 0.5 halves source value")
        void linearScaling_ratioHalf_halvesValue() {
            LinearScaling scaling = new LinearScaling(DEXTERITY, 0.5);
            
            int contribution = scaling.computeContribution(20);
            
            assertEquals(10, contribution, "20 DEX * 0.5 ratio = 10");
        }
        
        @Test
        @DisplayName("Linear scaling truncates fractional results")
        void linearScaling_truncatesFractional() {
            LinearScaling scaling = new LinearScaling(STRENGTH, 1.5);
            
            int contribution = scaling.computeContribution(7);
            
            assertEquals(10, contribution, "7 * 1.5 = 10.5 → truncates to 10");
        }
        
        @Test
        @DisplayName("Linear scaling with zero source returns zero")
        void linearScaling_zeroSource_returnsZero() {
            LinearScaling scaling = new LinearScaling(STRENGTH, 5.0);
            
            int contribution = scaling.computeContribution(0);
            
            assertEquals(0, contribution);
        }
        
        @Test
        @DisplayName("Linear scaling with negative ratio creates inverse relationship")
        void linearScaling_negativeRatio_inverseRelationship() {
            LinearScaling scaling = new LinearScaling(STRENGTH, -1.0);
            
            int contribution = scaling.computeContribution(10);
            
            assertEquals(-10, contribution, "Negative ratio creates penalty");
        }
    }
    
    @Nested
    @DisplayName("Threshold Scaling Tests")
    class ThresholdScalingTests {
        
        @Test
        @DisplayName("Threshold scaling: every 5 points gives bonus")
        void thresholdScaling_every5Points_givesBonus() {
            // Every 5 Luck = 100 bps (1%) crit
            ThresholdScaling scaling = new ThresholdScaling(LUCK, 5, 100);
            
            assertEquals(0, scaling.computeContribution(0), "0 Luck = 0 bps");
            assertEquals(0, scaling.computeContribution(4), "4 Luck = 0 bps (under threshold)");
            assertEquals(100, scaling.computeContribution(5), "5 Luck = 100 bps");
            assertEquals(100, scaling.computeContribution(9), "9 Luck = 100 bps");
            assertEquals(200, scaling.computeContribution(10), "10 Luck = 200 bps");
            assertEquals(500, scaling.computeContribution(25), "25 Luck = 500 bps");
        }
        
        @Test
        @DisplayName("Threshold scaling handles exact multiples")
        void thresholdScaling_exactMultiples() {
            ThresholdScaling scaling = new ThresholdScaling(CONSTITUTION, 10, 50);
            
            assertEquals(50, scaling.computeContribution(10));
            assertEquals(100, scaling.computeContribution(20));
            assertEquals(500, scaling.computeContribution(100));
        }
        
        @Test
        @DisplayName("Threshold scaling with perPoints=1 gives bonus per point")
        void thresholdScaling_perPoint() {
            ThresholdScaling scaling = new ThresholdScaling(DEXTERITY, 1, 25);
            
            assertEquals(25, scaling.computeContribution(1));
            assertEquals(250, scaling.computeContribution(10));
            assertEquals(2500, scaling.computeContribution(100));
        }
    }
    
    @Nested
    @DisplayName("ScalingEngine.computeScaledBase Tests")
    class ComputeScaledBaseTests {
        
        private StatDefinitionRegistry registry;
        
        @BeforeEach
        void setUp() {
            StatDefinitionRegistry.reset();
            registry = StatDefinitionRegistry.get();
            
            // Register source stats (ability scores)
            registry.registerStat(new StatDefinition.Builder(STRENGTH)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build());
            
            registry.registerStat(new StatDefinition.Builder(DEXTERITY)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build());
            
            registry.registerStat(new StatDefinition.Builder(INTELLIGENCE)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build());
            
            registry.registerStat(new StatDefinition.Builder(LUCK)
                    .defaultValue(5)
                    .bounds(1, 999)
                    .build());
            
            registry.registerStat(new StatDefinition.Builder(CONSTITUTION)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build());
        }
        
        @Test
        @DisplayName("Stat with no scaling returns 0 (uses default value)")
        void noScaling_returnsZero() {
            StatDefinition statDef = new StatDefinition.Builder(ATTACK_POWER)
                    .defaultValue(100)
                    .bounds(0, Integer.MAX_VALUE)
                    .build();
            
            int result = ScalingEngine.computeScaledBase(
                    statDef,
                    idx -> 10,
                    registry
            );
            
            assertEquals(0, result, "Stats without scaling return 0 from computeScaledBase");
        }
        
        @Test
        @DisplayName("Single linear scaling from STR to Attack Power")
        void singleLinearScaling_strToAttackPower() {
            StatDefinition attackPower = new StatDefinition.Builder(ATTACK_POWER)
                    .defaultValue(0)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 2.0))
                    .build();
            
            registry.registerStat(attackPower);
            registry.freeze();
            
            int strIndex = registry.getIndex(STRENGTH);
            
            // Provider returns 15 for Strength
            int result = ScalingEngine.computeScaledBase(
                    attackPower,
                    idx -> idx == strIndex ? 15 : 0,
                    registry
            );
            
            assertEquals(30, result, "15 STR * 2.0 = 30 Attack Power");
        }
        
        @Test
        @DisplayName("Single threshold scaling from LUCK to Crit Chance")
        void singleThresholdScaling_luckToCrit() {
            StatDefinition critChance = new StatDefinition.Builder(CRIT_CHANCE)
                    .defaultValue(0)
                    .bounds(0, 10000)
                    .addScaling(new ThresholdScaling(LUCK, 5, 100))
                    .build();
            
            registry.registerStat(critChance);
            registry.freeze();
            
            int luckIndex = registry.getIndex(LUCK);
            
            // Provider returns 23 for Luck → floor(23/5) = 4 steps → 400 bps
            int result = ScalingEngine.computeScaledBase(
                    critChance,
                    idx -> idx == luckIndex ? 23 : 0,
                    registry
            );
            
            assertEquals(400, result, "23 Luck / 5 = 4 steps * 100 bps = 400 bps");
        }
        
        @Test
        @DisplayName("Multiple scaling rules sum their contributions")
        void multipleScalingRules_sumContributions() {
            // Crit chance scales from both DEX and LUCK
            StatDefinition critChance = new StatDefinition.Builder(CRIT_CHANCE)
                    .defaultValue(0)
                    .bounds(0, 10000)
                    .addScaling(new ThresholdScaling(DEXTERITY, 10, 50))  // 50 bps per 10 DEX
                    .addScaling(new ThresholdScaling(LUCK, 5, 100))       // 100 bps per 5 LCK
                    .build();
            
            registry.registerStat(critChance);
            registry.freeze();
            
            int dexIndex = registry.getIndex(DEXTERITY);
            int luckIndex = registry.getIndex(LUCK);
            
            // 25 DEX = 2 steps * 50 = 100 bps
            // 15 Luck = 3 steps * 100 = 300 bps
            // Total = 400 bps
            int result = ScalingEngine.computeScaledBase(
                    critChance,
                    idx -> {
                        if (idx == dexIndex) return 25;
                        if (idx == luckIndex) return 15;
                        return 0;
                    },
                    registry
            );
            
            assertEquals(400, result, "100 bps from DEX + 300 bps from LUCK = 400 bps");
        }
        
        @Test
        @DisplayName("Mixed linear and threshold scaling sum correctly")
        void mixedScalingTypes_sumCorrectly() {
            // Max Health: linear from CON, threshold from STR
            StatDefinition maxHealth = new StatDefinition.Builder(MAX_HEALTH)
                    .defaultValue(100)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(CONSTITUTION, 5.0))     // +5 HP per CON
                    .addScaling(new ThresholdScaling(STRENGTH, 10, 20))   // +20 HP per 10 STR
                    .build();
            
            registry.registerStat(maxHealth);
            registry.freeze();
            
            int conIndex = registry.getIndex(CONSTITUTION);
            int strIndex = registry.getIndex(STRENGTH);
            
            // 20 CON * 5 = 100 HP from CON
            // 35 STR / 10 = 3 steps * 20 = 60 HP from STR
            // Total = 160 HP (note: defaultValue is not included in scaling calculation)
            int result = ScalingEngine.computeScaledBase(
                    maxHealth,
                    idx -> {
                        if (idx == conIndex) return 20;
                        if (idx == strIndex) return 35;
                        return 0;
                    },
                    registry
            );
            
            assertEquals(160, result, "100 from CON + 60 from STR = 160");
        }
    }
    
    @Nested
    @DisplayName("Stat Chain Scaling Tests")
    class StatChainTests {
        
        private StatDefinitionRegistry registry;
        
        @BeforeEach
        void setUp() {
            StatDefinitionRegistry.reset();
            registry = StatDefinitionRegistry.get();
        }
        
        @Test
        @DisplayName("Attribute → Derived stat chain propagates correctly")
        void attributeToDerivedChain() {
            // Setup: STR → Attack Power (linear x2)
            StatDefinition strength = new StatDefinition.Builder(STRENGTH)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition attackPower = new StatDefinition.Builder(ATTACK_POWER)
                    .defaultValue(0)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 2.0))
                    .build();
            
            registry.registerStat(strength);
            registry.registerStat(attackPower);
            registry.freeze();
            
            int strIndex = registry.getIndex(STRENGTH);
            
            // With 25 STR, Attack Power should be 50
            int result = ScalingEngine.computeScaledBase(
                    attackPower,
                    idx -> idx == strIndex ? 25 : 0,
                    registry
            );
            
            assertEquals(50, result, "25 STR * 2.0 = 50 Attack Power");
        }
        
        @Test
        @DisplayName("Multiple attributes scale to single derived stat")
        void multipleAttributesToSingleDerived() {
            // Setup: Both DEX and LUCK scale to Evasion
            StatDefinition dexterity = new StatDefinition.Builder(DEXTERITY)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition luck = new StatDefinition.Builder(LUCK)
                    .defaultValue(5)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition evasion = new StatDefinition.Builder(EVASION)
                    .defaultValue(0)
                    .bounds(0, 10000)
                    .addScaling(new LinearScaling(DEXTERITY, 1.5))  // 1.5 bps per DEX
                    .addScaling(new ThresholdScaling(LUCK, 3, 25)) // 25 bps per 3 LUCK
                    .build();
            
            registry.registerStat(dexterity);
            registry.registerStat(luck);
            registry.registerStat(evasion);
            registry.freeze();
            
            int dexIndex = registry.getIndex(DEXTERITY);
            int luckIndex = registry.getIndex(LUCK);
            
            // 20 DEX * 1.5 = 30 bps
            // 12 LUCK / 3 = 4 steps * 25 bps = 100 bps
            // Total = 130 bps
            int result = ScalingEngine.computeScaledBase(
                    evasion,
                    idx -> {
                        if (idx == dexIndex) return 20;
                        if (idx == luckIndex) return 12;
                        return 0;
                    },
                    registry
            );
            
            assertEquals(130, result, "30 bps from DEX + 100 bps from LUCK = 130 bps");
        }
        
        @Test
        @DisplayName("Single attribute scales to multiple derived stats")
        void singleAttributeToMultipleDerived() {
            // STR scales to both Attack Power and Max Health
            StatDefinition strength = new StatDefinition.Builder(STRENGTH)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition attackPower = new StatDefinition.Builder(ATTACK_POWER)
                    .defaultValue(0)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 2.0))
                    .build();
            
            StatDefinition maxHealth = new StatDefinition.Builder(MAX_HEALTH)
                    .defaultValue(100)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 3.0))
                    .build();
            
            registry.registerStat(strength);
            registry.registerStat(attackPower);
            registry.registerStat(maxHealth);
            registry.freeze();
            
            int strIndex = registry.getIndex(STRENGTH);
            
            // 20 STR → 40 Attack Power (20 * 2)
            int attackResult = ScalingEngine.computeScaledBase(
                    attackPower,
                    idx -> idx == strIndex ? 20 : 0,
                    registry
            );
            
            // 20 STR → 60 Max Health (20 * 3, plus default 100 handled elsewhere)
            int healthResult = ScalingEngine.computeScaledBase(
                    maxHealth,
                    idx -> idx == strIndex ? 20 : 0,
                    registry
            );
            
            assertEquals(40, attackResult, "20 STR * 2.0 = 40 Attack Power");
            assertEquals(60, healthResult, "20 STR * 3.0 = 60 Max Health (scaling only)");
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Missing source stat returns 0 contribution")
        void missingSourceStat_returnsZero() {
            StatDefinitionRegistry.reset();
            
            // Note: We can't freeze with invalid dependencies in real code,
            // but for this test we'll test the computeContribution directly
            LinearScaling scaling = new LinearScaling(STRENGTH, 2.0);
            
            // Source value provider returns 0 for unknown index
            int result = ScalingEngine.computeContribution(scaling, 0);
            
            assertEquals(0, result, "Zero source value = 0 contribution");
        }
        
        @Test
        @DisplayName("Large values don't overflow")
        void largeValues_noOverflow() {
            LinearScaling scaling = new LinearScaling(STRENGTH, 100.0);
            
            // 10000 * 100 = 1,000,000 - well within int range
            int result = scaling.computeContribution(10000);
            
            assertEquals(1000000, result);
        }
        
        @Test
        @DisplayName("Very small ratio preserves precision")
        void smallRatio_preservesPrecision() {
            LinearScaling scaling = new LinearScaling(DEXTERITY, 0.01);
            
            assertEquals(1, scaling.computeContribution(100), "100 * 0.01 = 1");
            assertEquals(0, scaling.computeContribution(50), "50 * 0.01 = 0.5 → truncates to 0");
            assertEquals(10, scaling.computeContribution(1000), "1000 * 0.01 = 10");
        }
    }
    
    @Nested
    @DisplayName("Diminishing Scaling Tests")
    class DiminishingScalingTests {
        
        @Test
        @DisplayName("Diminishing scaling applies cap")
        void diminishingScaling_appliesCap() {
            // Diminishing returns with 5000 bps cap (50%)
            DiminishingScaling scaling = new DiminishingScaling(
                    DEXTERITY,
                    "evasion",
                    1.0,
                    5000
            );
            
            // Even with very high input, should be capped
            int contribution = scaling.computeContribution(9999);
            
            assertTrue(contribution <= 5000, "Should be capped at 5000 bps");
        }
        
        @Test
        @DisplayName("Diminishing scaling with zero source returns zero")
        void diminishingScaling_zeroSource_returnsZero() {
            DiminishingScaling scaling = new DiminishingScaling(
                    DEXTERITY,
                    "evasion",
                    1.0,
                    5000
            );
            
            int contribution = ScalingEngine.computeDiminishingContribution(scaling, 0);
            
            assertEquals(0, contribution);
        }
    }
}
