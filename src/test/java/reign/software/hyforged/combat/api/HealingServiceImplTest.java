package reign.software.hyforged.combat.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HealingServiceImpl} static calculation methods.
 */
@DisplayName("HealingServiceImpl Tests")
class HealingServiceImplTest {

    @Nested
    @DisplayName("calculateFinalHealing Tests")
    class CalculateFinalHealingTests {

        @Test
        @DisplayName("Returns base amount when all modifiers are zero")
        void noModifiersReturnsBaseAmount() {
            float result = HealingServiceImpl.calculateFinalHealing(100f, 0, 0, 0);
            assertEquals(100f, result, 0.001f);
        }

        @Test
        @DisplayName("Healer effectiveness increases healing")
        void healerEffectivenessIncreasesHealing() {
            // 20% increased healing effectiveness (2000 bps)
            float result = HealingServiceImpl.calculateFinalHealing(100f, 2000, 0, 0);
            assertEquals(120f, result, 0.001f);
        }

        @Test
        @DisplayName("Target healing received increases healing")
        void healingReceivedIncreasesHealing() {
            // 10% increased healing received (1000 bps)
            float result = HealingServiceImpl.calculateFinalHealing(100f, 0, 1000, 0);
            assertEquals(110f, result, 0.001f);
        }

        @Test
        @DisplayName("Life recovery rate increases healing")
        void recoveryRateIncreasesHealing() {
            // 15% increased life recovery (1500 bps)
            float result = HealingServiceImpl.calculateFinalHealing(100f, 0, 0, 1500);
            assertEquals(115f, result, 0.001f);
        }

        @Test
        @DisplayName("All modifiers stack multiplicatively")
        void allModifiersStackMultiplicatively() {
            // 20% effectiveness * 10% received * 15% recovery
            // 1.2 * 1.1 * 1.15 = 1.518
            float result = HealingServiceImpl.calculateFinalHealing(100f, 2000, 1000, 1500);
            assertEquals(151.8f, result, 0.1f);
        }

        @Test
        @DisplayName("Negative modifiers reduce healing")
        void negativeModifiersReduceHealing() {
            // -50% healing received (e.g., from debuff)
            float result = HealingServiceImpl.calculateFinalHealing(100f, 0, -5000, 0);
            assertEquals(50f, result, 0.001f);
        }

        @Test
        @DisplayName("Extreme negative modifiers clamp to zero")
        void extremeNegativeModifiersClampsToZero() {
            // -120% healing received should result in 0 (not negative)
            float result = HealingServiceImpl.calculateFinalHealing(100f, 0, -12000, 0);
            assertEquals(0f, result, 0.001f);
        }

        @Test
        @DisplayName("Zero base amount returns zero")
        void zeroBaseAmountReturnsZero() {
            float result = HealingServiceImpl.calculateFinalHealing(0f, 2000, 1000, 500);
            assertEquals(0f, result, 0.001f);
        }

        @Test
        @DisplayName("Large positive modifiers significantly boost healing")
        void largeModifiersBoostHealing() {
            // 100% effectiveness + 50% received + 30% recovery
            // 2.0 * 1.5 * 1.3 = 3.9
            float result = HealingServiceImpl.calculateFinalHealing(100f, 10000, 5000, 3000);
            assertEquals(390f, result, 0.1f);
        }

        @Test
        @DisplayName("Multiple small modifiers compound")
        void smallModifiersCompound() {
            // 5% + 5% + 5% = 1.05^3 ≈ 1.157
            float result = HealingServiceImpl.calculateFinalHealing(100f, 500, 500, 500);
            assertEquals(115.7625f, result, 0.01f);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Very small healing amounts are preserved")
        void verySmallAmountsPreserved() {
            float result = HealingServiceImpl.calculateFinalHealing(0.001f, 1000, 0, 0);
            assertEquals(0.0011f, result, 0.0001f);
        }

        @Test
        @DisplayName("Negative base amount returns zero")
        void negativeBaseAmountReturnsZero() {
            // Negative healing shouldn't be allowed (handled by spec)
            float result = HealingServiceImpl.calculateFinalHealing(-100f, 0, 0, 0);
            assertEquals(0f, result, 0.001f);
        }
    }
}
