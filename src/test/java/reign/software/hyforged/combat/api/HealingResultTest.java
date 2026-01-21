package reign.software.hyforged.combat.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HealingResult}.
 */
@DisplayName("HealingResult Tests")
class HealingResultTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("invalidTarget creates result with INVALID_TARGET outcome")
        void invalidTargetCreatesResult() {
            HealingResult result = HealingResult.invalidTarget();

            assertEquals(HealingResult.Outcome.INVALID_TARGET, result.getOutcome());
            assertFalse(result.wasHealed());
        }

        @Test
        @DisplayName("targetDead creates result with TARGET_DEAD outcome")
        void targetDeadCreatesResult() {
            HealingResult result = HealingResult.targetDead();

            assertEquals(HealingResult.Outcome.TARGET_DEAD, result.getOutcome());
            assertFalse(result.wasHealed());
        }

        @Test
        @DisplayName("alreadyFull creates result with ALREADY_FULL outcome")
        void alreadyFullCreatesResult() {
            HealingResult result = HealingResult.alreadyFull(50f);

            assertEquals(HealingResult.Outcome.ALREADY_FULL, result.getOutcome());
            assertEquals(50f, result.getBaseAmount());
            assertEquals(0f, result.getFinalAmount());
            assertEquals(0f, result.getActualHealing());
            assertTrue(result.wasAlreadyFull());
            assertFalse(result.wasHealed());
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Builder creates result with all fields")
        void builderCreatesFullResult() {
            HealingResult result = HealingResult.builder()
                    .outcome(HealingResult.Outcome.HEALED)
                    .baseAmount(100f)
                    .finalAmount(120f)
                    .actualHealing(80f)
                    .overheal(40f)
                    .healerEffectivenessBps(1000)
                    .targetHealingReceivedBps(500)
                    .targetRecoveryRateBps(250)
                    .source("Test Heal")
                    .build();

            assertEquals(HealingResult.Outcome.HEALED, result.getOutcome());
            assertEquals(100f, result.getBaseAmount());
            assertEquals(120f, result.getFinalAmount());
            assertEquals(80f, result.getActualHealing());
            assertEquals(40f, result.getOverheal());
            assertEquals(1000, result.getHealerEffectivenessBps());
            assertEquals(500, result.getTargetHealingReceivedBps());
            assertEquals(250, result.getTargetRecoveryRateBps());
            assertEquals("Test Heal", result.getSource());
            assertTrue(result.wasHealed());
        }

        @Test
        @DisplayName("Preview outcome returns false for wasHealed")
        void previewOutcomeReturnsFalseForWasHealed() {
            HealingResult result = HealingResult.builder()
                    .outcome(HealingResult.Outcome.PREVIEW)
                    .baseAmount(100f)
                    .finalAmount(100f)
                    .build();

            assertFalse(result.wasHealed());
        }
    }

    @Nested
    @DisplayName("Total Multiplier Tests")
    class TotalMultiplierTests {

        @Test
        @DisplayName("getTotalMultiplier calculates correctly")
        void getTotalMultiplierCalculatesCorrectly() {
            HealingResult result = HealingResult.builder()
                    .baseAmount(100f)
                    .finalAmount(150f)
                    .build();

            assertEquals(1.5f, result.getTotalMultiplier(), 0.001f);
        }

        @Test
        @DisplayName("getTotalMultiplier returns 1.0 for zero base amount")
        void getTotalMultiplierReturnsOneForZeroBase() {
            HealingResult result = HealingResult.builder()
                    .baseAmount(0f)
                    .finalAmount(0f)
                    .build();

            assertEquals(1.0f, result.getTotalMultiplier(), 0.001f);
        }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString includes key fields")
        void toStringIncludesFields() {
            HealingResult result = HealingResult.builder()
                    .outcome(HealingResult.Outcome.HEALED)
                    .baseAmount(100f)
                    .finalAmount(120f)
                    .actualHealing(80f)
                    .source("Holy Light")
                    .build();

            String str = result.toString();
            assertTrue(str.contains("HEALED"));
            assertTrue(str.contains("100"));
            assertTrue(str.contains("120"));
            assertTrue(str.contains("80"));
            assertTrue(str.contains("Holy Light"));
        }
    }
}
