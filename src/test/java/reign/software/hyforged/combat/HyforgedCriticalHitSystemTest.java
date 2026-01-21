package reign.software.hyforged.combat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for critical hit system formulas and logic.
 * <p>
 * Tests the mathematical formulas used by HyforgedCriticalHitSystem.
 * Full integration tests would require mocking the ECS.
 */
class HyforgedCriticalHitSystemTest {
    
    @Nested
    @DisplayName("Crit Chance Calculations")
    class CritChanceTests {
        
        @Test
        @DisplayName("crit chance preserved when attacker level equals defender")
        void sameLevel_noChangeToCritChance() {
            int result = CombatMath.calculateCritChance(5000, 10, 10);
            assertEquals(5000, result);
        }
        
        @Test
        @DisplayName("crit chance preserved when attacker is higher level")
        void attackerHigherLevel_noChangeToCritChance() {
            int result = CombatMath.calculateCritChance(5000, 15, 10);
            assertEquals(5000, result);
        }
        
        @Test
        @DisplayName("crit chance reduced when defender is higher level")
        void defenderHigherLevel_reducesCritChance() {
            // Defender 3 levels higher = 15% penalty (3 * 500 bps)
            int result = CombatMath.calculateCritChance(5000, 10, 13);
            assertEquals(3500, result);
        }
        
        @Test
        @DisplayName("crit chance clamped to zero when penalty exceeds chance")
        void excessPenalty_clampedToZero() {
            // Defender 15 levels higher = 75% penalty (15 * 500 bps)
            // 50% crit - 75% penalty = 0 (not negative)
            int result = CombatMath.calculateCritChance(5000, 5, 20);
            assertEquals(0, result);
        }
        
        @Test
        @DisplayName("zero crit chance stays zero")
        void zeroCritChance() {
            int result = CombatMath.calculateCritChance(0, 10, 10);
            assertEquals(0, result);
        }
    }
    
    @Nested
    @DisplayName("Crit Multiplier Application")
    class CritMultiplierTests {
        
        @Test
        @DisplayName("default 15% crit multiplier increases damage by 15%")
        void defaultCritMultiplier() {
            // 1500 bps = 15% bonus = 1.15x multiplier
            float baseDamage = 100f;
            int critMultiplierBps = 1500; // 15% bonus
            float totalMultiplier = 1.0f + (critMultiplierBps / (float) CombatMath.BPS_100);
            float critDamage = baseDamage * totalMultiplier;
            
            assertEquals(1.15f, totalMultiplier, 0.001f);
            assertEquals(115f, critDamage, 0.01f);
        }
        
        @Test
        @DisplayName("50% crit multiplier increases damage by 50%")
        void fiftyPercentCritMultiplier() {
            float baseDamage = 100f;
            int critMultiplierBps = 5000; // 50% bonus
            float totalMultiplier = 1.0f + (critMultiplierBps / (float) CombatMath.BPS_100);
            float critDamage = baseDamage * totalMultiplier;
            
            assertEquals(1.50f, totalMultiplier, 0.001f);
            assertEquals(150f, critDamage, 0.01f);
        }
        
        @Test
        @DisplayName("100% crit multiplier doubles damage")
        void fullCritMultiplier() {
            float baseDamage = 100f;
            int critMultiplierBps = 10000; // 100% bonus = 2x
            float totalMultiplier = 1.0f + (critMultiplierBps / (float) CombatMath.BPS_100);
            float critDamage = baseDamage * totalMultiplier;
            
            assertEquals(2.0f, totalMultiplier, 0.001f);
            assertEquals(200f, critDamage, 0.01f);
        }
        
        @Test
        @DisplayName("200% crit multiplier triples damage")
        void massiveCritMultiplier() {
            float baseDamage = 100f;
            int critMultiplierBps = 20000; // 200% bonus = 3x
            float totalMultiplier = 1.0f + (critMultiplierBps / (float) CombatMath.BPS_100);
            float critDamage = baseDamage * totalMultiplier;
            
            assertEquals(3.0f, totalMultiplier, 0.001f);
            assertEquals(300f, critDamage, 0.01f);
        }
    }
    
    @Nested
    @DisplayName("Crit Roll Tests")
    class CritRollTests {
        
        @Test
        @DisplayName("50% crit chance succeeds on roll < 5000")
        void fiftyPercentCritSuccess() {
            assertTrue(CombatMath.rollChance(5000, 4999));
            assertTrue(CombatMath.rollChance(5000, 2500));
            assertTrue(CombatMath.rollChance(5000, 0));
        }
        
        @Test
        @DisplayName("50% crit chance fails on roll >= 5000")
        void fiftyPercentCritFail() {
            assertFalse(CombatMath.rollChance(5000, 5000));
            assertFalse(CombatMath.rollChance(5000, 7500));
            assertFalse(CombatMath.rollChance(5000, 9999));
        }
        
        @Test
        @DisplayName("0% crit chance always fails")
        void zeroCritChanceAlwaysFails() {
            assertFalse(CombatMath.rollChance(0, 0));
            assertFalse(CombatMath.rollChance(0, 5000));
        }
        
        @Test
        @DisplayName("100% crit chance always succeeds")
        void fullCritChanceAlwaysSucceeds() {
            assertTrue(CombatMath.rollChance(10000, 0));
            assertTrue(CombatMath.rollChance(10000, 9999));
        }
    }
    
    @Nested
    @DisplayName("Combat Scenario Documentation")
    class ScenarioTests {
        
        @Test
        @DisplayName("typical crit scenario with level penalty")
        void typicalCritScenario() {
            // Player level 10 attacks monster level 12
            // Base crit chance: 30% (3000 bps)
            // Level penalty: 2 levels * 5% = 10% (1000 bps)
            // Effective crit chance: 20% (2000 bps)
            int effectiveCritChance = CombatMath.calculateCritChance(3000, 10, 12);
            assertEquals(2000, effectiveCritChance);
            
            // If crit succeeds with 50% bonus multiplier:
            // 100 damage * 1.50 = 150 damage
            float baseDamage = 100f;
            int critMultiplierBps = 5000;
            float critDamage = baseDamage * (1.0f + critMultiplierBps / (float) CombatMath.BPS_100);
            assertEquals(150f, critDamage, 0.01f);
        }
        
        @Test
        @DisplayName("crit against much higher level target is rare")
        void highLevelTargetCrit() {
            // Player level 10 attacks boss level 20
            // Base crit chance: 50% (5000 bps)
            // Level penalty: 10 levels * 5% = 50% (5000 bps)
            // Effective crit chance: 0%
            int effectiveCritChance = CombatMath.calculateCritChance(5000, 10, 20);
            assertEquals(0, effectiveCritChance);
        }
    }
}
