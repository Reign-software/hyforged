package reign.software.hyforged.combat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for auto-block system formulas and logic.
 * <p>
 * Tests the mathematical formulas used by HyforgedAutoBlockSystem.
 * Full integration tests would require mocking the ECS, so these focus
 * on the CombatMath utilities used by the system.
 * <p>
 * Note: Uses deprecated CombatMath.calculateBlockChance(int) for backwards compatibility testing.
 */
@SuppressWarnings("deprecation")
class HyforgedAutoBlockSystemTest {
    
    @Nested
    @DisplayName("Block Chance Calculations")
    class BlockChanceTests {
        
        @Test
        @DisplayName("block chance is preserved as-is (no level penalty for defensive)")
        void blockChanceNotPenalized() {
            // Block chance should just be returned as-is since it's defensive
            assertEquals(5000, CombatMath.calculateBlockChance(5000));
            assertEquals(7500, CombatMath.calculateBlockChance(7500));
            assertEquals(10000, CombatMath.calculateBlockChance(10000));
        }
        
        @Test
        @DisplayName("negative block chance is clamped to zero")
        void negativeBlockChanceClamped() {
            assertEquals(0, CombatMath.calculateBlockChance(-100));
            assertEquals(0, CombatMath.calculateBlockChance(-5000));
        }
        
        @Test
        @DisplayName("zero block chance returns zero")
        void zeroBlockChance() {
            assertEquals(0, CombatMath.calculateBlockChance(0));
        }
    }
    
    @Nested
    @DisplayName("Block Mitigation Damage Reduction")
    class BlockMitigationTests {
        
        @Test
        @DisplayName("50% mitigation halves damage")
        void fiftyPercentMitigation() {
            // 5000 bps = 50% mitigation
            float result = CombatMath.applyReduction(100f, 5000);
            assertEquals(50f, result, 0.01f);
        }
        
        @Test
        @DisplayName("75% mitigation reduces damage to 25%")
        void highMitigation() {
            // 7500 bps = 75% mitigation
            float result = CombatMath.applyReduction(100f, 7500);
            assertEquals(25f, result, 0.01f);
        }
        
        @Test
        @DisplayName("100% mitigation reduces damage to zero")
        void fullMitigation() {
            // 10000 bps = 100% mitigation
            float result = CombatMath.applyReduction(100f, 10000);
            assertEquals(0f, result, 0.01f);
        }
        
        @Test
        @DisplayName("0% mitigation does not reduce damage")
        void noMitigation() {
            float result = CombatMath.applyReduction(100f, 0);
            assertEquals(100f, result, 0.01f);
        }
        
        @Test
        @DisplayName("mitigation applies correctly to various damage amounts")
        void variousDamageAmounts() {
            // 50% mitigation on 200 damage = 100 damage
            assertEquals(100f, CombatMath.applyReduction(200f, 5000), 0.01f);
            
            // 30% mitigation on 50 damage = 35 damage
            assertEquals(35f, CombatMath.applyReduction(50f, 3000), 0.01f);
        }
    }
    
    @Nested
    @DisplayName("Stamina Cost Calculations")
    class StaminaCostTests {
        
        @Test
        @DisplayName("10% stamina cost modifier reduces base cost correctly")
        void tenPercentStaminaCost() {
            // Default auto-block stamina cost is 10% = 1000 bps
            // Base stamina cost of 10 * 1000 / 10000 = 1.0
            float baseCost = 10.0f;
            int staminaCostBps = 1000;
            float result = baseCost * staminaCostBps / CombatMath.BPS_100;
            assertEquals(1.0f, result, 0.01f);
        }
        
        @Test
        @DisplayName("25% stamina cost modifier")
        void twentyFivePercentStaminaCost() {
            float baseCost = 10.0f;
            int staminaCostBps = 2500;
            float result = baseCost * staminaCostBps / CombatMath.BPS_100;
            assertEquals(2.5f, result, 0.01f);
        }
        
        @Test
        @DisplayName("100% stamina cost modifier equals full base cost")
        void fullStaminaCost() {
            float baseCost = 10.0f;
            int staminaCostBps = 10000;
            float result = baseCost * staminaCostBps / CombatMath.BPS_100;
            assertEquals(10.0f, result, 0.01f);
        }
    }
    
    @Nested
    @DisplayName("Roll Chance for Block")
    class RollChanceTests {
        
        @Test
        @DisplayName("50% block chance succeeds on roll < 5000")
        void fiftyPercentBlockSuccess() {
            assertTrue(CombatMath.rollChance(5000, 4999));
            assertTrue(CombatMath.rollChance(5000, 2500));
            assertTrue(CombatMath.rollChance(5000, 0));
        }
        
        @Test
        @DisplayName("50% block chance fails on roll >= 5000")
        void fiftyPercentBlockFail() {
            assertFalse(CombatMath.rollChance(5000, 5000));
            assertFalse(CombatMath.rollChance(5000, 7500));
            assertFalse(CombatMath.rollChance(5000, 9999));
        }
        
        @Test
        @DisplayName("75% block chance boundary conditions")
        void seventyFivePercentBlockBoundary() {
            assertTrue(CombatMath.rollChance(7500, 7499));
            assertFalse(CombatMath.rollChance(7500, 7500));
        }
        
        @Test
        @DisplayName("0% block chance always fails")
        void zeroBlockChanceAlwaysFails() {
            assertFalse(CombatMath.rollChance(0, 0));
            assertFalse(CombatMath.rollChance(0, 5000));
        }
        
        @Test
        @DisplayName("100% block chance always succeeds")
        void fullBlockChanceAlwaysSucceeds() {
            assertTrue(CombatMath.rollChance(10000, 0));
            assertTrue(CombatMath.rollChance(10000, 9999));
        }
    }
    
    @Nested
    @DisplayName("Auto-Block System Behavior Documentation")
    class BehaviorDocTests {
        
        @Test
        @DisplayName("auto-block uses reduced stamina (10% of manual block by default)")
        void documentAutoBlockStaminaCost() {
            // The default auto-block stamina cost is 1000 bps = 10%
            // This test documents the expected behavior
            float manualBlockCost = 10.0f;
            int autoBlockCostBps = 1000; // 10%
            
            float autoBlockCost = manualBlockCost * autoBlockCostBps / CombatMath.BPS_100;
            
            // Auto-block costs 10% of manual block
            assertEquals(1.0f, autoBlockCost, 0.01f);
            assertEquals(manualBlockCost * 0.10f, autoBlockCost, 0.01f);
        }
        
        @Test
        @DisplayName("auto-block applies partial mitigation (50% by default)")
        void documentAutoBlockMitigation() {
            // The default block mitigation is 5000 bps = 50%
            float incomingDamage = 100f;
            int mitigationBps = 5000; // 50%
            
            float mitigatedDamage = CombatMath.applyReduction(incomingDamage, mitigationBps);
            
            // Auto-block reduces damage by 50% by default
            assertEquals(50f, mitigatedDamage, 0.01f);
        }
    }
}
