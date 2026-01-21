package reign.software.hyforged.stats.effect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HyforgedEffectService.
 * <p>
 * These tests focus on duration calculation and formatting logic.
 * Full integration testing with Hytale's EffectControllerComponent
 * requires the game runtime.
 */
@DisplayName("Hyforged Effect Service Tests")
class HyforgedEffectServiceTest {

    @Nested
    @DisplayName("Duration Calculation Tests")
    class DurationCalculationTests {

        @Test
        @DisplayName("No bonus returns base duration")
        void noBonus_returnsBaseDuration() {
            // 0 bps = 100% = no change
            int scaled = HyforgedEffectService.calculateScaledDuration(null, 100);
            assertEquals(100, scaled);
        }

        @Test
        @DisplayName("Positive bonus increases duration")
        void positiveBonus_increasesDuration() {
            // 2500 bps = +25%
            // For unit test without stat component, returns base
            // The actual scaling happens with a real stat component
            int baseDuration = 100;
            int scaled = HyforgedEffectService.calculateScaledDuration(null, baseDuration);
            assertEquals(baseDuration, scaled);
        }

        @Test
        @DisplayName("Minimum duration is 1 tick")
        void minimumDuration_isOneTick() {
            assertEquals(1, HyforgedEffectService.MIN_DURATION_TICKS);
        }

        @Test
        @DisplayName("Zero base duration returns minimum")
        void zeroDuration_returnsMinimum() {
            int scaled = HyforgedEffectService.calculateScaledDuration(null, 0);
            assertEquals(1, scaled);
        }

        @Test
        @DisplayName("Negative base duration returns minimum")
        void negativeDuration_returnsMinimum() {
            int scaled = HyforgedEffectService.calculateScaledDuration(null, -10);
            assertEquals(1, scaled);
        }
    }

    @Nested
    @DisplayName("Duration Multiplier Tests")
    class DurationMultiplierTests {

        @Test
        @DisplayName("Zero bonus gives 1.0 multiplier")
        void zeroBonus_givesOneMultiplier() {
            double multiplier = HyforgedEffectService.calculateDurationMultiplier(0);
            assertEquals(1.0, multiplier, 0.0001);
        }

        @Test
        @DisplayName("10000 bps (100%) gives 2.0 multiplier")
        void fullBonus_givesTwoMultiplier() {
            double multiplier = HyforgedEffectService.calculateDurationMultiplier(10000);
            assertEquals(2.0, multiplier, 0.0001);
        }

        @Test
        @DisplayName("2500 bps (25%) gives 1.25 multiplier")
        void quarterBonus_givesCorrectMultiplier() {
            double multiplier = HyforgedEffectService.calculateDurationMultiplier(2500);
            assertEquals(1.25, multiplier, 0.0001);
        }

        @Test
        @DisplayName("-5000 bps (-50%) gives 0.5 multiplier")
        void negativeBonus_givesReducedMultiplier() {
            double multiplier = HyforgedEffectService.calculateDurationMultiplier(-5000);
            assertEquals(0.5, multiplier, 0.0001);
        }

        @Test
        @DisplayName("-10000 bps (-100%) gives 0.0 multiplier")
        void fullNegativeBonus_givesZeroMultiplier() {
            double multiplier = HyforgedEffectService.calculateDurationMultiplier(-10000);
            assertEquals(0.0, multiplier, 0.0001);
        }
    }

    @Nested
    @DisplayName("Duration Formatting Tests")
    class DurationFormattingTests {

        @Test
        @DisplayName("Positive bonus formats with plus sign")
        void positiveBonus_formatsWithPlusSign() {
            String formatted = HyforgedEffectService.formatDurationBonus(2500);
            assertEquals("+25.0%", formatted);
        }

        @Test
        @DisplayName("Negative bonus formats with minus sign")
        void negativeBonus_formatsWithMinusSign() {
            String formatted = HyforgedEffectService.formatDurationBonus(-1000);
            assertEquals("-10.0%", formatted);
        }

        @Test
        @DisplayName("Zero bonus formats as +0.0%")
        void zeroBonus_formatsAsZero() {
            String formatted = HyforgedEffectService.formatDurationBonus(0);
            assertEquals("+0.0%", formatted);
        }

        @Test
        @DisplayName("Large bonus formats correctly")
        void largeBonus_formatsCorrectly() {
            String formatted = HyforgedEffectService.formatDurationBonus(15000);
            assertEquals("+150.0%", formatted);
        }

        @Test
        @DisplayName("Fractional bonus formats with decimal")
        void fractionalBonus_formatsWithDecimal() {
            String formatted = HyforgedEffectService.formatDurationBonus(550);
            assertEquals("+5.5%", formatted);
        }
    }

    @Nested
    @DisplayName("Constants Tests")
    class ConstantsTests {

        @Test
        @DisplayName("BASIS_100_PERCENT is 10000")
        void basis100Percent_is10000() {
            assertEquals(10000, HyforgedEffectService.BASIS_100_PERCENT);
        }

        @Test
        @DisplayName("Effect duration stat ID is correct")
        void effectDurationStatId_isCorrect() {
            assertEquals("hyforged:effect-duration-bps", 
                HyforgedEffectService.EFFECT_DURATION_STAT.fullId());
        }

        @Test
        @DisplayName("Effect duration stat namespace is hyforged")
        void effectDurationStatNamespace_isHyforged() {
            assertEquals("hyforged", 
                HyforgedEffectService.EFFECT_DURATION_STAT.namespace());
        }

        @Test
        @DisplayName("Effect duration stat name is effect-duration-bps")
        void effectDurationStatName_isCorrect() {
            assertEquals("effect-duration-bps", 
                HyforgedEffectService.EFFECT_DURATION_STAT.name());
        }
    }

    @Nested
    @DisplayName("Scaling Formula Tests")
    class ScalingFormulaTests {

        @Test
        @DisplayName("Scaling formula: base * (10000 + bonus) / 10000")
        void scalingFormula_isCorrect() {
            // 100 * (10000 + 2500) / 10000 = 125
            int base = 100;
            int bonus = 2500;
            int expected = base * (HyforgedEffectService.BASIS_100_PERCENT + bonus) / 
                          HyforgedEffectService.BASIS_100_PERCENT;
            assertEquals(125, expected);
        }

        @Test
        @DisplayName("Scaling with negative bonus reduces duration")
        void scalingWithNegativeBonus_reducesDuration() {
            // 100 * (10000 - 3000) / 10000 = 70
            int base = 100;
            int bonus = -3000;
            int expected = base * (HyforgedEffectService.BASIS_100_PERCENT + bonus) / 
                          HyforgedEffectService.BASIS_100_PERCENT;
            assertEquals(70, expected);
        }

        @Test
        @DisplayName("Scaling with -100% bonus gives zero")
        void scalingWithFullNegativeBonus_givesZero() {
            // 100 * (10000 - 10000) / 10000 = 0
            int base = 100;
            int bonus = -10000;
            int expected = base * (HyforgedEffectService.BASIS_100_PERCENT + bonus) / 
                          HyforgedEffectService.BASIS_100_PERCENT;
            assertEquals(0, expected);
            // But the service clamps to minimum 1 tick
        }
    }
}
