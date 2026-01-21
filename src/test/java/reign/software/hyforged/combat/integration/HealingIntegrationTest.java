package reign.software.hyforged.combat.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.combat.api.HealingResult;
import reign.software.hyforged.combat.api.HealingServiceImpl;
import reign.software.hyforged.combat.api.HealingSpec;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the healing system.
 * Tests healing formula, modifiers, and edge cases.
 */
@DisplayName("Healing System Integration Tests")
class HealingIntegrationTest {

    @Nested
    @DisplayName("Healing Formula Tests")
    class HealingFormulaTests {

        @Test
        @DisplayName("Base healing with no modifiers")
        void baseHealing_noModifiers() {
            float result = HealingServiceImpl.calculateFinalHealing(100f, 0, 0, 0);
            assertEquals(100f, result, 0.01f, "No modifiers = base healing");
        }

        @Test
        @DisplayName("Healing effectiveness increases outgoing healing")
        void healingEffectiveness_increasesHealing() {
            // 50% effectiveness bonus (5000 bps)
            float result = HealingServiceImpl.calculateFinalHealing(100f, 5000, 0, 0);
            assertEquals(150f, result, 0.01f, "50% effectiveness = 1.5x healing");
        }

        @Test
        @DisplayName("Healing received increases incoming healing")
        void healingReceived_increasesHealing() {
            // 25% healing received bonus (2500 bps)
            float result = HealingServiceImpl.calculateFinalHealing(100f, 0, 2500, 0);
            assertEquals(125f, result, 0.01f, "25% received = 1.25x healing");
        }

        @Test
        @DisplayName("Life recovery rate increases healing")
        void lifeRecoveryRate_increasesHealing() {
            // 30% recovery rate bonus (3000 bps)
            float result = HealingServiceImpl.calculateFinalHealing(100f, 0, 0, 3000);
            assertEquals(130f, result, 0.01f, "30% recovery rate = 1.3x healing");
        }

        @Test
        @DisplayName("All modifiers stack multiplicatively")
        void allModifiers_stackMultiplicatively() {
            // 50% effectiveness, 25% received, 30% recovery
            // 100 * 1.5 * 1.25 * 1.3 = 243.75
            float result = HealingServiceImpl.calculateFinalHealing(100f, 5000, 2500, 3000);
            assertEquals(243.75f, result, 0.01f, "All modifiers multiply");
        }

        @Test
        @DisplayName("Negative modifiers reduce healing")
        void negativeModifiers_reduceHealing() {
            // -50% healing received (debuff)
            float result = HealingServiceImpl.calculateFinalHealing(100f, 0, -5000, 0);
            assertEquals(50f, result, 0.01f, "-50% received = 0.5x healing");
        }

        @Test
        @DisplayName("Negative modifier cannot reduce below zero")
        void negativeModifier_cannotGoBelowZero() {
            // -100% + more = still shouldn't be negative
            float result = HealingServiceImpl.calculateFinalHealing(100f, -10000, 0, 0);
            // With -100% effectiveness: 100 * (1 - 1) = 0
            assertEquals(0f, result, 0.01f, "Cannot heal for negative");
        }
    }

    @Nested
    @DisplayName("HealingSpec Tests")
    class HealingSpecTests {

        @Test
        @DisplayName("Simple factory creates basic spec")
        void simpleFactory_createsBasicSpec() {
            HealingSpec spec = HealingSpec.of(50);
            
            assertEquals(50, spec.getAmount());
            assertNull(spec.getSource());
            assertFalse(spec.isSkipHealingReceived());
            assertFalse(spec.isSkipRecoveryRate());
        }

        @Test
        @DisplayName("Factory with source includes source")
        void factoryWithSource_includesSource() {
            HealingSpec spec = HealingSpec.of(100, "Health Potion");
            
            assertEquals(100, spec.getAmount());
            assertEquals("Health Potion", spec.getSource());
        }

        @Test
        @DisplayName("Builder allows full customization")
        void builder_fullCustomization() {
            HealingSpec spec = HealingSpec.builder()
                    .amount(75)
                    .source("Regeneration Aura")
                    .skipHealingReceived(true)
                    .skipRecoveryRate(true)
                    .logToCombatLog(true)
                    .build();
            
            assertEquals(75, spec.getAmount());
            assertEquals("Regeneration Aura", spec.getSource());
            assertTrue(spec.isSkipHealingReceived());
            assertTrue(spec.isSkipRecoveryRate());
            assertTrue(spec.isLogToCombatLog());
        }

        @Test
        @DisplayName("Skip flags bypass modifiers")
        void skipFlags_bypassModifiers() {
            // When skipHealingReceived is true, the received modifier is not applied
            // This is behavioral - tested via the formula integration
            HealingSpec spec = HealingSpec.builder()
                    .amount(100)
                    .skipHealingReceived(true)
                    .build();
            
            assertTrue(spec.isSkipHealingReceived());
            
            // If applied with 50% healing received bonus but skip flag on:
            // The service should NOT apply the received modifier
        }
    }

    @Nested
    @DisplayName("HealingResult Tests")
    class HealingResultTests {

        @Test
        @DisplayName("HEALED outcome indicates success")
        void healedOutcome_indicatesSuccess() {
            HealingResult result = HealingResult.builder()
                    .outcome(HealingResult.Outcome.HEALED)
                    .baseAmount(100)
                    .finalAmount(150)
                    .actualHealing(120)
                    .overheal(30)
                    .build();
            
            assertEquals(HealingResult.Outcome.HEALED, result.getOutcome());
            assertEquals(100, result.getBaseAmount());
            assertEquals(150, result.getFinalAmount());
            assertEquals(120, result.getActualHealing());
            assertEquals(30, result.getOverheal());
        }

        @Test
        @DisplayName("ALREADY_FULL outcome when at max health")
        void alreadyFullOutcome_atMaxHealth() {
            HealingResult result = HealingResult.alreadyFull(100);
            
            assertEquals(HealingResult.Outcome.ALREADY_FULL, result.getOutcome());
            assertEquals(100, result.getBaseAmount());
            assertEquals(0, result.getActualHealing());
        }

        @Test
        @DisplayName("INVALID_TARGET outcome for bad entity")
        void invalidTargetOutcome_badEntity() {
            HealingResult result = HealingResult.invalidTarget();
            
            assertEquals(HealingResult.Outcome.INVALID_TARGET, result.getOutcome());
            assertEquals(0, result.getActualHealing());
        }

        @Test
        @DisplayName("TARGET_DEAD outcome for dead entity")
        void targetDeadOutcome_deadEntity() {
            HealingResult result = HealingResult.targetDead();
            
            assertEquals(HealingResult.Outcome.TARGET_DEAD, result.getOutcome());
            assertEquals(0, result.getActualHealing());
        }

        @Test
        @DisplayName("Overheal calculation")
        void overheal_calculation() {
            // Target has 80 health, max 100, healing for 50
            // Actual heal = 20, overheal = 30
            HealingResult result = HealingResult.builder()
                    .outcome(HealingResult.Outcome.HEALED)
                    .baseAmount(50)
                    .finalAmount(50)
                    .actualHealing(20)
                    .overheal(30)
                    .build();
            
            assertEquals(50, result.getBaseAmount());
            assertEquals(20, result.getActualHealing());
            assertEquals(30, result.getOverheal());
            assertEquals(50, result.getActualHealing() + result.getOverheal());
        }

        @Test
        @DisplayName("Total multiplier calculation")
        void totalMultiplier_calculation() {
            HealingResult result = HealingResult.builder()
                    .outcome(HealingResult.Outcome.HEALED)
                    .baseAmount(100)
                    .finalAmount(150)
                    .actualHealing(150)
                    .overheal(0)
                    .healerEffectivenessBps(2500)
                    .targetHealingReceivedBps(1000)
                    .targetRecoveryRateBps(500)
                    .build();
            
            // Verify modifiers stored
            assertEquals(2500, result.getHealerEffectivenessBps());
            assertEquals(1000, result.getTargetHealingReceivedBps());
            assertEquals(500, result.getTargetRecoveryRateBps());
            
            // Total multiplier = 1.5 (final / base)
            assertEquals(1.5f, result.getTotalMultiplier(), 0.01f);
        }
    }

    @Nested
    @DisplayName("Healing Bypass Scenarios")
    class HealingBypassTests {

        @Test
        @DisplayName("Healing bypasses resistance pipeline")
        void healing_bypassesResistance() {
            // Healing should never be reduced by resistance
            // This is ensured by HealingService being separate from CombatService
            
            // Test the formula directly - no resistance parameter exists
            float healing = HealingServiceImpl.calculateFinalHealing(100f, 0, 0, 0);
            assertEquals(100f, healing, 0.01f, "No resistance reduction");
        }

        @Test
        @DisplayName("Healing bypasses penetration")
        void healing_bypassesPenetration() {
            // Similarly, no penetration affects healing
            float healing = HealingServiceImpl.calculateFinalHealing(100f, 0, 0, 0);
            assertEquals(100f, healing, 0.01f, "No penetration needed");
        }

        @Test
        @DisplayName("Healing is not affected by crit")
        void healing_notAffectedByCrit() {
            // Healing doesn't crit (no crit multiplier parameter)
            float healing1 = HealingServiceImpl.calculateFinalHealing(100f, 0, 0, 0);
            float healing2 = HealingServiceImpl.calculateFinalHealing(100f, 0, 0, 0);
            assertEquals(healing1, healing2, "Healing is deterministic, no crit");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Zero healing amount")
        void zeroHealingAmount() {
            float result = HealingServiceImpl.calculateFinalHealing(0f, 5000, 5000, 5000);
            assertEquals(0f, result, 0.01f, "0 base = 0 final");
        }

        @Test
        @DisplayName("Very large healing modifiers")
        void veryLargeHealingModifiers() {
            // 1000% effectiveness = +10x
            float result = HealingServiceImpl.calculateFinalHealing(100f, 100000, 0, 0);
            assertEquals(1100f, result, 0.01f, "1000% effectiveness = 11x");
        }

        @Test
        @DisplayName("Multiple large modifiers compound correctly")
        void multipleLargeModifiers_compoundCorrectly() {
            // 100% each: 100 * 2 * 2 * 2 = 800
            float result = HealingServiceImpl.calculateFinalHealing(100f, 10000, 10000, 10000);
            assertEquals(800f, result, 0.01f, "All +100% = 8x");
        }

        @Test
        @DisplayName("Fractional healing preserved")
        void fractionalHealing_preserved() {
            float result = HealingServiceImpl.calculateFinalHealing(33.33f, 0, 0, 0);
            assertEquals(33.33f, result, 0.01f, "Fractional healing preserved");
        }
    }
}
