package reign.software.hyforged.combat.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HealingSpec}.
 */
@DisplayName("HealingSpec Tests")
class HealingSpecTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("of(amount) creates spec with default options")
        void ofWithAmountCreatesDefaultSpec() {
            HealingSpec spec = HealingSpec.of(100f);

            assertEquals(100f, spec.getAmount());
            assertNull(spec.getSource());
            assertFalse(spec.isSkipHealingReceived());
            assertFalse(spec.isSkipRecoveryRate());
            assertFalse(spec.isLogToCombatLog());
            assertFalse(spec.isOverhealAllowed());
        }

        @Test
        @DisplayName("of(amount, source) creates spec with source")
        void ofWithSourceCreatesSpec() {
            HealingSpec spec = HealingSpec.of(50f, "Healing Potion");

            assertEquals(50f, spec.getAmount());
            assertEquals("Healing Potion", spec.getSource());
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Builder creates spec with all options")
        void builderCreatesFullSpec() {
            HealingSpec spec = HealingSpec.builder()
                    .amount(75f)
                    .source("Holy Light")
                    .skipHealingReceived(true)
                    .skipRecoveryRate(true)
                    .logToCombatLog(true)
                    .trackOverheal(true)
                    .build();

            assertEquals(75f, spec.getAmount());
            assertEquals("Holy Light", spec.getSource());
            assertTrue(spec.isSkipHealingReceived());
            assertTrue(spec.isSkipRecoveryRate());
            assertTrue(spec.isLogToCombatLog());
            assertTrue(spec.isOverhealAllowed());
        }

        @Test
        @DisplayName("Builder rejects negative amount")
        void builderRejectsNegativeAmount() {
            assertThrows(IllegalArgumentException.class, () -> {
                HealingSpec.builder()
                        .amount(-10f)
                        .build();
            });
        }

        @Test
        @DisplayName("Builder allows zero amount")
        void builderAllowsZeroAmount() {
            HealingSpec spec = HealingSpec.builder()
                    .amount(0f)
                    .build();

            assertEquals(0f, spec.getAmount());
        }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString includes key fields")
        void toStringIncludesFields() {
            HealingSpec spec = HealingSpec.of(100f, "Test Source");
            String str = spec.toString();

            assertTrue(str.contains("100"));
            assertTrue(str.contains("Test Source"));
        }
    }
}
