package reign.software.hyforged.combat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CombatMath utility class.
 */
class CombatMathTest {

    @Nested
    class HitChanceCalculationTests {
        
        @Test
        void calculateHitChance_noEvasion_returns100Percent() {
            int result = CombatMath.calculateHitChance(0, 0, 10, 10);
            assertEquals(CombatMath.MAX_HIT_CHANCE_BPS, result);
        }
        
        @Test
        void calculateHitChance_fullEvasion_returnsMinHitChance() {
            // 10000 evasion = 100% evasion, should be clamped to MIN
            int result = CombatMath.calculateHitChance(0, 10000, 10, 10);
            assertEquals(CombatMath.MIN_HIT_CHANCE_BPS, result);
        }
        
        @Test
        void calculateHitChance_partialEvasion_reducesHitChance() {
            // 50% evasion (5000 bps)
            int result = CombatMath.calculateHitChance(0, 5000, 10, 10);
            // Base 10000 - 5000 evasion = 5000 bps (50%)
            assertEquals(5000, result);
        }
        
        @Test
        void calculateHitChance_accuracyCountersEvasion() {
            // 50% evasion, 25% accuracy bonus
            int result = CombatMath.calculateHitChance(2500, 5000, 10, 10);
            // Base 10000 - 5000 evasion + 2500 accuracy = 7500 bps (75%)
            assertEquals(7500, result);
        }
        
        @Test
        void calculateHitChance_higherLevelDefender_appliesPenalty() {
            // Defender 5 levels higher = 25% penalty (5 * 500 bps)
            int result = CombatMath.calculateHitChance(0, 0, 10, 15);
            // Base 10000 - 2500 penalty = 7500 bps
            assertEquals(7500, result);
        }
        
        @Test
        void calculateHitChance_lowerLevelDefender_noPenalty() {
            // Defender 5 levels lower = no penalty
            int result = CombatMath.calculateHitChance(0, 0, 15, 10);
            assertEquals(CombatMath.MAX_HIT_CHANCE_BPS, result);
        }
        
        @Test
        void calculateHitChance_combinedFactors() {
            // 30% evasion, 10% accuracy, defender 2 levels higher
            int result = CombatMath.calculateHitChance(1000, 3000, 10, 12);
            // Base 10000 - 3000 evasion + 1000 accuracy - 1000 level penalty = 7000 bps
            assertEquals(7000, result);
        }
        
        @Test
        void calculateHitChance_clampsToMinimum() {
            // Extreme evasion + level difference
            int result = CombatMath.calculateHitChance(0, 9000, 1, 20);
            assertEquals(CombatMath.MIN_HIT_CHANCE_BPS, result);
        }
        
        @Test
        void calculateHitChance_clampsToMaximum() {
            // High accuracy, no evasion
            int result = CombatMath.calculateHitChance(5000, 0, 10, 10);
            assertEquals(CombatMath.MAX_HIT_CHANCE_BPS, result);
        }
    }
    
    @Nested
    class RollChanceTests {
        
        @Test
        void rollChance_zeroChance_alwaysFails() {
            assertFalse(CombatMath.rollChance(0, 0));
            assertFalse(CombatMath.rollChance(0, 5000));
            assertFalse(CombatMath.rollChance(0, 9999));
        }
        
        @Test
        void rollChance_fullChance_alwaysSucceeds() {
            assertTrue(CombatMath.rollChance(10000, 0));
            assertTrue(CombatMath.rollChance(10000, 5000));
            assertTrue(CombatMath.rollChance(10000, 9999));
        }
        
        @Test
        void rollChance_50Percent_dependsOnRoll() {
            assertTrue(CombatMath.rollChance(5000, 0));
            assertTrue(CombatMath.rollChance(5000, 4999));
            assertFalse(CombatMath.rollChance(5000, 5000));
            assertFalse(CombatMath.rollChance(5000, 9999));
        }
        
        @Test
        void rollChance_negativeChance_alwaysFails() {
            assertFalse(CombatMath.rollChance(-1000, 0));
        }
        
        @Test
        void rollChance_overMaxChance_alwaysSucceeds() {
            assertTrue(CombatMath.rollChance(15000, 9999));
        }
    }
    
    @Nested
    class CritChanceTests {
        
        @Test
        void calculateCritChance_sameLevel_noPenalty() {
            int result = CombatMath.calculateCritChance(3000, 10, 10);
            assertEquals(3000, result);
        }
        
        @Test
        void calculateCritChance_higherLevelTarget_reducesCrit() {
            // Target 4 levels higher = 2000 bps penalty
            int result = CombatMath.calculateCritChance(3000, 10, 14);
            assertEquals(1000, result);
        }
        
        @Test
        void calculateCritChance_lowerLevelTarget_noPenalty() {
            int result = CombatMath.calculateCritChance(3000, 14, 10);
            assertEquals(3000, result);
        }
        
        @Test
        void calculateCritChance_largeLevelDiff_clampsToZero() {
            // Target 10 levels higher = 5000 bps penalty
            int result = CombatMath.calculateCritChance(3000, 10, 20);
            assertEquals(0, result);
        }
    }
    
    @Nested
    class DamageCalculationTests {
        
        @Test
        void applyMultiplier_1x_noChange() {
            float result = CombatMath.applyMultiplier(100f, 10000);
            assertEquals(100f, result, 0.001f);
        }
        
        @Test
        void applyMultiplier_1_5x_increases50Percent() {
            float result = CombatMath.applyMultiplier(100f, 15000);
            assertEquals(150f, result, 0.001f);
        }
        
        @Test
        void applyMultiplier_2x_doubles() {
            float result = CombatMath.applyMultiplier(100f, 20000);
            assertEquals(200f, result, 0.001f);
        }
        
        @Test
        void applyReduction_50Percent_halvesDamage() {
            float result = CombatMath.applyReduction(100f, 5000);
            assertEquals(50f, result, 0.001f);
        }
        
        @Test
        void applyReduction_75Percent_quartersOfDamage() {
            float result = CombatMath.applyReduction(100f, 7500);
            assertEquals(25f, result, 0.001f);
        }
        
        @Test
        void applyReduction_0Percent_noChange() {
            float result = CombatMath.applyReduction(100f, 0);
            assertEquals(100f, result, 0.001f);
        }
        
        @Test
        void applyReduction_100Percent_zeroDamage() {
            float result = CombatMath.applyReduction(100f, 10000);
            assertEquals(0f, result, 0.001f);
        }
    }
    
    @Nested
    class ClampTests {
        
        @Test
        void clamp_withinRange_noChange() {
            assertEquals(50, CombatMath.clamp(50, 0, 100));
        }
        
        @Test
        void clamp_belowMin_clampsToMin() {
            assertEquals(0, CombatMath.clamp(-10, 0, 100));
        }
        
        @Test
        void clamp_aboveMax_clampsToMax() {
            assertEquals(100, CombatMath.clamp(150, 0, 100));
        }
    }
    
    @Nested
    class PenetrationTests {
        
        @Test
        void calculateEffectiveResistance_noPenetration_returnsFullResistance() {
            int result = CombatMath.calculateEffectiveResistance(5000, 0);
            assertEquals(5000, result);
        }
        
        @Test
        void calculateEffectiveResistance_partialPenetration_reducesResistance() {
            // 50% resistance, 20% penetration = 30% effective resistance
            int result = CombatMath.calculateEffectiveResistance(5000, 2000);
            assertEquals(3000, result);
        }
        
        @Test
        void calculateEffectiveResistance_fullPenetration_zeroResistance() {
            // 50% resistance, 50% penetration = 0% effective resistance
            int result = CombatMath.calculateEffectiveResistance(5000, 5000);
            assertEquals(0, result);
        }
        
        @Test
        void calculateEffectiveResistance_excessPenetration_clampedToZero() {
            // 50% resistance, 80% penetration = 0% (not negative)
            int result = CombatMath.calculateEffectiveResistance(5000, 8000);
            assertEquals(0, result);
        }
        
        @Test
        void calculateEffectiveResistance_zeroResistance_staysZero() {
            // 0% resistance, 50% penetration = 0%
            int result = CombatMath.calculateEffectiveResistance(0, 5000);
            assertEquals(0, result);
        }
        
        @Test
        void damageWithPenetration_reducesEffectiveMitigation() {
            // Defender has 50% resistance, attacker has 30% penetration
            // Effective resistance = 50% - 30% = 20%
            int effectiveResistance = CombatMath.calculateEffectiveResistance(5000, 3000);
            assertEquals(2000, effectiveResistance);
            
            // 100 damage with 20% resistance = 80 damage
            float damage = CombatMath.applyReduction(100f, effectiveResistance);
            assertEquals(80f, damage, 0.001f);
        }
        
        @Test
        void damageWithFullPenetration_dealsFullDamage() {
            // Defender has 50% resistance, attacker has 50%+ penetration
            int effectiveResistance = CombatMath.calculateEffectiveResistance(5000, 6000);
            assertEquals(0, effectiveResistance);
            
            // 100 damage with 0% resistance = 100 damage
            float damage = CombatMath.applyReduction(100f, effectiveResistance);
            assertEquals(100f, damage, 0.001f);
        }
    }
}
