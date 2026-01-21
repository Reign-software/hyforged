package reign.software.hyforged.combat.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CombatResult}.
 */
@DisplayName("CombatResult")
class CombatResultTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("evaded() creates EVADED result with correct values")
        void evadedCreatesCorrectResult() {
            UUID attacker = UUID.randomUUID();
            UUID defender = UUID.randomUUID();

            CombatResult result = CombatResult.evaded(attacker, defender, 100);

            assertEquals(CombatResult.Outcome.EVADED, result.getOutcome());
            assertTrue(result.wasEvaded());
            assertFalse(result.wasHit());
            assertEquals(attacker, result.getAttackerUuid());
            assertEquals(defender, result.getDefenderUuid());
            assertEquals(100, result.getTotalBaseDamage());
            assertEquals(0, result.getTotalFinalDamage());
        }

        @Test
        @DisplayName("invalidEntity() creates INVALID_ENTITY result")
        void invalidEntityCreatesCorrectResult() {
            CombatResult result = CombatResult.invalidEntity();

            assertEquals(CombatResult.Outcome.INVALID_ENTITY, result.getOutcome());
            assertFalse(result.wasHit());
            assertNull(result.getAttackerUuid());
            assertNull(result.getDefenderUuid());
        }

        @Test
        @DisplayName("targetDead() creates TARGET_DEAD result")
        void targetDeadCreatesCorrectResult() {
            UUID defender = UUID.randomUUID();

            CombatResult result = CombatResult.targetDead(defender);

            assertEquals(CombatResult.Outcome.TARGET_DEAD, result.getOutcome());
            assertEquals(defender, result.getDefenderUuid());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("default outcome is HIT")
        void defaultOutcomeIsHit() {
            CombatResult result = CombatResult.builder()
                    .totalBaseDamage(100)
                    .totalFinalDamage(80)
                    .build();

            assertEquals(CombatResult.Outcome.HIT, result.getOutcome());
            assertTrue(result.wasHit());
        }

        @Test
        @DisplayName("builder sets all fields correctly")
        void builderSetsAllFields() {
            UUID attacker = UUID.randomUUID();
            UUID defender = UUID.randomUUID();

            CombatResult result = CombatResult.builder()
                    .outcome(CombatResult.Outcome.HIT)
                    .attackerUuid(attacker)
                    .defenderUuid(defender)
                    .totalBaseDamage(100)
                    .totalFinalDamage(75)
                    .criticalHit(true)
                    .critMultiplierBps(1500)
                    .blocked(true)
                    .autoBlocked(true)
                    .blockMitigationBps(5000)
                    .timestamp(12345L)
                    .build();

            assertEquals(CombatResult.Outcome.HIT, result.getOutcome());
            assertEquals(attacker, result.getAttackerUuid());
            assertEquals(defender, result.getDefenderUuid());
            assertEquals(100, result.getTotalBaseDamage());
            assertEquals(75, result.getTotalFinalDamage());
            assertTrue(result.isCriticalHit());
            assertEquals(1500, result.getCritMultiplierBps());
            assertTrue(result.wasBlocked());
            assertTrue(result.wasAutoBlocked());
            assertEquals(5000, result.getBlockMitigationBps());
            assertEquals(12345L, result.getTimestamp());
        }

        @Test
        @DisplayName("builder adds damage breakdowns")
        void builderAddsDamageBreakdowns() {
            CombatResult result = CombatResult.builder()
                    .addDamageBreakdown("Physical", 50, 40, 2000, 500)
                    .addDamageBreakdown("Fire", 30, 20, 3000, 1000)
                    .totalBaseDamage(80)
                    .totalFinalDamage(60)
                    .build();

            assertEquals(2, result.getDamageBreakdown().size());
            
            CombatResult.DamageBreakdown physical = result.getDamageBreakdown().get(0);
            assertEquals("Physical", physical.damageCauseId());
            assertEquals(50, physical.baseDamage());
            assertEquals(40, physical.finalDamage());
            assertEquals(2000, physical.resistanceBps());
            assertEquals(500, physical.penetrationBps());
            assertEquals(1500, physical.getEffectiveResistanceBps());
        }

        @Test
        @DisplayName("builder adds ailments triggered")
        void builderAddsAilments() {
            CombatResult result = CombatResult.builder()
                    .addDamageBreakdown("Fire", 100, 80, 0, 0)
                    .totalBaseDamage(100)
                    .totalFinalDamage(80)
                    .addAilmentTriggered("hyforged:ignite")
                    .addAilmentTriggered("hyforged:burn")
                    .build();

            assertEquals(2, result.getAilmentsTriggered().size());
            assertTrue(result.getAilmentsTriggered().contains("hyforged:ignite"));
            assertTrue(result.getAilmentsTriggered().contains("hyforged:burn"));
        }
    }

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethodTests {

        @Test
        @DisplayName("wasHit returns true only for HIT outcome")
        void wasHitOnlyForHit() {
            assertTrue(CombatResult.builder()
                    .outcome(CombatResult.Outcome.HIT)
                    .addDamageBreakdown("Physical", 50, 50, 0, 0)
                    .build().wasHit());
            
            assertFalse(CombatResult.builder()
                    .outcome(CombatResult.Outcome.EVADED)
                    .build().wasHit());
        }

        @Test
        @DisplayName("wasEvaded returns true only for EVADED outcome")
        void wasEvadedOnlyForEvaded() {
            assertTrue(CombatResult.builder()
                    .outcome(CombatResult.Outcome.EVADED)
                    .build().wasEvaded());
            
            assertFalse(CombatResult.builder()
                    .outcome(CombatResult.Outcome.HIT)
                    .addDamageBreakdown("Physical", 50, 50, 0, 0)
                    .build().wasEvaded());
        }

        @Test
        @DisplayName("wasFullyBlocked returns true only for BLOCKED outcome")
        void wasFullyBlockedOnlyForBlocked() {
            assertTrue(CombatResult.builder()
                    .outcome(CombatResult.Outcome.BLOCKED)
                    .build().wasFullyBlocked());
            
            assertFalse(CombatResult.builder()
                    .outcome(CombatResult.Outcome.HIT)
                    .blocked(true)
                    .addDamageBreakdown("Physical", 50, 50, 0, 0)
                    .build().wasFullyBlocked());
        }
    }

    @Nested
    @DisplayName("Calculations")
    class CalculationTests {

        @Test
        @DisplayName("getDamageReductionPercent calculates correctly")
        void damageReductionPercent() {
            CombatResult result = CombatResult.builder()
                    .totalBaseDamage(100)
                    .totalFinalDamage(75)
                    .addDamageBreakdown("Physical", 100, 75, 2500, 0)
                    .build();

            assertEquals(25, result.getDamageReductionPercent());
        }

        @Test
        @DisplayName("getDamageReductionPercent returns 0 for zero base damage")
        void damageReductionZeroBase() {
            CombatResult result = CombatResult.builder()
                    .totalBaseDamage(0)
                    .totalFinalDamage(0)
                    .addDamageBreakdown("Physical", 0, 0, 0, 0)
                    .build();

            assertEquals(0, result.getDamageReductionPercent());
        }

        @Test
        @DisplayName("DamageBreakdown.getEffectiveResistanceBps calculates correctly")
        void effectiveResistance() {
            CombatResult.DamageBreakdown breakdown = 
                    new CombatResult.DamageBreakdown("Physical", 100, 80, 3000, 1000);

            assertEquals(2000, breakdown.getEffectiveResistanceBps());
        }

        @Test
        @DisplayName("DamageBreakdown.getEffectiveResistanceBps clamps to zero")
        void effectiveResistanceClampsToZero() {
            CombatResult.DamageBreakdown breakdown = 
                    new CombatResult.DamageBreakdown("Physical", 100, 100, 1000, 5000);

            assertEquals(0, breakdown.getEffectiveResistanceBps());
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("getDamageBreakdown returns unmodifiable list")
        void breakdownUnmodifiable() {
            CombatResult result = CombatResult.builder()
                    .addDamageBreakdown("Physical", 50, 40, 0, 0)
                    .build();

            assertThrows(UnsupportedOperationException.class, () ->
                result.getDamageBreakdown().add(
                        new CombatResult.DamageBreakdown("Fire", 25, 20, 0, 0))
            );
        }

        @Test
        @DisplayName("getAilmentsTriggered returns unmodifiable list")
        void ailmentsUnmodifiable() {
            CombatResult result = CombatResult.builder()
                    .addDamageBreakdown("Fire", 100, 80, 0, 0)
                    .addAilmentTriggered("ignite")
                    .build();

            assertThrows(UnsupportedOperationException.class, () ->
                result.getAilmentsTriggered().add("burn")
            );
        }
    }
}
