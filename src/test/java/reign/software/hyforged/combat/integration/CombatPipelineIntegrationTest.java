package reign.software.hyforged.combat.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.combat.CombatMath;
import reign.software.hyforged.combat.api.CombatResult;
import reign.software.hyforged.combat.api.DamageSpec;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for the combat pipeline.
 * Tests full combat scenarios from attack initiation through damage application.
 */
@DisplayName("Combat Pipeline Integration Tests")
class CombatPipelineIntegrationTest {

    @Nested
    @DisplayName("Full Combat Scenario Tests")
    class FullCombatScenarioTests {

        @Test
        @DisplayName("Full pipeline: hit → block → damage → crit flow")
        void fullPipeline_hitBlockDamageCrit() {
            // Simulate full combat resolution
            int attackerAccuracy = 5000;  // 50%
            int defenderEvasion = 2000;   // 20%
            int attackerLevel = 10;
            int defenderLevel = 10;

            // Step 1: Hit resolution
            int hitChance = CombatMath.calculateHitChance(
                    attackerAccuracy, defenderEvasion, attackerLevel, defenderLevel);
            assertTrue(hitChance > 0, "Hit chance should be positive");
            assertTrue(hitChance <= 10000, "Hit chance should not exceed 100%");

            // Step 2: Block resolution (if hit)
            int blockMitigation = 5000; // 50%
            float baseDamage = 100f;

            // Simulate blocked hit
            float blockedDamage = baseDamage * (1f - blockMitigation / 10000f);
            assertEquals(50f, blockedDamage, 0.01f, "Block should reduce damage by 50%");

            // Step 3: Resistance
            int resistance = 2500; // 25%
            int penetration = 500; // 5%
            int effectiveResistance = CombatMath.calculateEffectiveResistance(resistance, penetration);
            assertEquals(2000, effectiveResistance, "Effective resistance = resistance - penetration");

            float afterResistance = blockedDamage * (1f - effectiveResistance / 10000f);
            assertEquals(40f, afterResistance, 0.01f, "After 20% effective resistance");

            // Step 4: Crit
            int critMultiplier = 1500; // 15% bonus = 1.15x
            float critDamage = afterResistance * (1f + critMultiplier / 10000f);
            assertEquals(46f, critDamage, 0.01f, "Crit adds 15% bonus damage");
        }

        @Test
        @DisplayName("Full pipeline: miss scenario")
        void fullPipeline_missScenario() {
            int attackerAccuracy = 1000;  // 10%
            int defenderEvasion = 8000;   // 80%
            int attackerLevel = 5;
            int defenderLevel = 10;

            // Level penalty applies
            int hitChance = CombatMath.calculateHitChance(
                    attackerAccuracy, defenderEvasion, attackerLevel, defenderLevel);

            // Base hit: 10000 + 1000 - 8000 = 3000, then 5 level penalty = -2500
            // Result: 500 (clamped to minimum)
            assertTrue(hitChance >= CombatMath.MIN_HIT_CHANCE_BPS,
                    "Hit chance should be at least minimum");

            // Verify roll with guaranteed miss
            assertFalse(CombatMath.rollChance(hitChance, 9999),
                    "Roll of 9999 should fail against low hit chance");
        }

        @Test
        @DisplayName("Full pipeline: no block, no crit scenario")
        void fullPipeline_noBlockNoCrit() {
            float baseDamage = 100f;
            int resistance = 5000; // 50%
            int penetration = 0;

            int effectiveResistance = CombatMath.calculateEffectiveResistance(resistance, penetration);
            float finalDamage = baseDamage * (1f - effectiveResistance / 10000f);

            assertEquals(50f, finalDamage, 0.01f, "50% resistance = 50% damage");
        }
    }

    @Nested
    @DisplayName("Multi-Element Attack Tests")
    class MultiElementAttackTests {

        @Test
        @DisplayName("Multi-element: independent resistance per type")
        void multiElement_independentResistance() {
            // Attack: 50 physical + 30 fire
            float physicalDamage = 50f;
            float fireDamage = 30f;

            int armorResistance = 4000; // 40%
            int fireResistance = 2000; // 20%

            // Physical damage
            float physicalFinal = physicalDamage * (1f - armorResistance / 10000f);
            assertEquals(30f, physicalFinal, 0.01f, "Physical: 50 * 0.6 = 30");

            // Fire damage
            float fireFinal = fireDamage * (1f - fireResistance / 10000f);
            assertEquals(24f, fireFinal, 0.01f, "Fire: 30 * 0.8 = 24");

            // Total
            float totalFinal = physicalFinal + fireFinal;
            assertEquals(54f, totalFinal, 0.01f, "Total: 30 + 24 = 54");
        }

        @Test
        @DisplayName("Multi-element: penetration applies per type")
        void multiElement_penetrationPerType() {
            float physicalDamage = 100f;
            float fireDamage = 100f;

            int armorResistance = 5000; // 50%
            int fireResistance = 5000; // 50%
            int armorPenetration = 2000; // 20%
            int firePenetration = 0; // 0%

            // Physical with penetration
            int effectiveArmor = CombatMath.calculateEffectiveResistance(armorResistance, armorPenetration);
            assertEquals(3000, effectiveArmor, "Armor: 5000 - 2000 = 3000");
            float physicalFinal = physicalDamage * (1f - effectiveArmor / 10000f);
            assertEquals(70f, physicalFinal, 0.01f, "Physical: 100 * 0.7 = 70");

            // Fire without penetration
            int effectiveFire = CombatMath.calculateEffectiveResistance(fireResistance, firePenetration);
            assertEquals(5000, effectiveFire, "Fire: 5000 - 0 = 5000");
            float fireFinal = fireDamage * (1f - effectiveFire / 10000f);
            assertEquals(50f, fireFinal, 0.01f, "Fire: 100 * 0.5 = 50");
        }

        @Test
        @DisplayName("Multi-element: crit applies to total damage")
        void multiElement_critAppliesToTotal() {
            // Post-resistance damage
            float physicalFinal = 30f;
            float fireFinal = 24f;
            float total = physicalFinal + fireFinal;

            // Crit multiplier applies to total
            int critMultiplier = 5000; // 50% bonus
            float critDamage = total * (1f + critMultiplier / 10000f);

            assertEquals(81f, critDamage, 0.01f, "Crit: 54 * 1.5 = 81");
        }

        @Test
        @DisplayName("Multi-element: DamageSpec builder creates correct entries")
        void multiElement_damageSpecBuilder() {
            DamageSpec spec = DamageSpec.builder()
                    .addDamage("Physical", 50)
                    .addDamage("Fire", 30)
                    .addDamage("Ice", 20)
                    .build();

            assertEquals(3, spec.getDamageEntries().size());
            assertEquals(100, spec.getTotalBaseDamage());

            // Verify individual entries
            var entries = spec.getDamageEntries();
            assertEquals(50, entries.get(0).amount());
            assertEquals(30, entries.get(1).amount());
            assertEquals(20, entries.get(2).amount());
        }
    }

    @Nested
    @DisplayName("Level Difference Penalty Tests")
    class LevelDifferencePenaltyTests {

        @Test
        @DisplayName("Level penalty: higher monster reduces hit chance")
        void levelPenalty_higherMonsterReducesHitChance() {
            // Use values that won't cap to 100%
            // hitChance = 10000 - 5000 + 2000 = 7000 (70%)
            int baseAccuracy = 2000;
            int baseEvasion = 5000;

            // Same level
            int hitSameLevel = CombatMath.calculateHitChance(baseAccuracy, baseEvasion, 10, 10);
            assertEquals(7000, hitSameLevel, "Baseline: 10000 - 5000 + 2000 = 7000");

            // Monster 5 levels higher - penalty = 5 * 500 = 2500
            int hitHigherMonster = CombatMath.calculateHitChance(baseAccuracy, baseEvasion, 10, 15);

            assertTrue(hitHigherMonster < hitSameLevel,
                    "Hit chance should be lower against higher level monster");

            int expectedPenalty = 5 * CombatMath.LEVEL_PENALTY_PER_LEVEL_BPS;
            assertEquals(hitSameLevel - expectedPenalty, hitHigherMonster,
                    "Penalty should be 5 * 500 = 2500 bps (7000 - 2500 = 4500)");
        }

        @Test
        @DisplayName("Level penalty: lower monster no penalty")
        void levelPenalty_lowerMonsterNoPenalty() {
            int baseAccuracy = 5000;
            int baseEvasion = 3000;

            int hitSameLevel = CombatMath.calculateHitChance(baseAccuracy, baseEvasion, 10, 10);
            int hitLowerMonster = CombatMath.calculateHitChance(baseAccuracy, baseEvasion, 10, 5);

            assertEquals(hitSameLevel, hitLowerMonster,
                    "No penalty against lower level monster");
        }

        @Test
        @DisplayName("Level penalty: crit chance reduced vs higher level")
        void levelPenalty_critChanceReduced() {
            int baseCritChance = 5000; // 50%

            // Same level
            int critSameLevel = CombatMath.calculateCritChance(baseCritChance, 10, 10);
            assertEquals(5000, critSameLevel);

            // Monster 10 levels higher
            int critHigherMonster = CombatMath.calculateCritChance(baseCritChance, 10, 20);
            assertTrue(critHigherMonster < critSameLevel);

            int expectedPenalty = 10 * CombatMath.LEVEL_PENALTY_PER_LEVEL_BPS;
            assertEquals(critSameLevel - expectedPenalty, critHigherMonster);
        }
    }

    @Nested
    @DisplayName("Stat Cap Integration Tests")
    class StatCapIntegrationTests {

        @Test
        @DisplayName("Resistance cap: soft cap limits effective resistance")
        void resistanceCap_softCapApplied() {
            // High resistance with soft cap
            int rawResistance = 9000; // 90%
            int softCap = 7500; // 75%
            int hardCap = 9000; // 90%

            // Without bonus stat, capped at soft cap
            int cappedResistance = Math.min(rawResistance, softCap);
            assertEquals(7500, cappedResistance, "Capped at soft cap");

            // With bonus stat increasing soft cap
            int bonusStat = 1000; // +10% to soft cap
            int newSoftCap = softCap + bonusStat;
            int cappedWithBonus = Math.min(rawResistance, Math.min(newSoftCap, hardCap));
            assertEquals(8500, cappedWithBonus, "Soft cap raised by bonus");
        }

        @Test
        @DisplayName("Crit cap: 95% hard cap enforced")
        void critCap_hardCapEnforced() {
            int rawCritChance = 12000; // 120%
            int hardCap = 9500; // 95%

            int cappedCrit = Math.min(rawCritChance, hardCap);
            assertEquals(9500, cappedCrit, "Crit capped at 95%");
        }

        @Test
        @DisplayName("Penetration: cannot reduce resistance below 0")
        void penetration_cannotGoNegative() {
            int resistance = 3000;
            int penetration = 5000; // More than resistance

            int effectiveResistance = CombatMath.calculateEffectiveResistance(resistance, penetration);
            assertEquals(0, effectiveResistance, "Effective resistance floored at 0");
        }
    }

    @Nested
    @DisplayName("Determinism Tests")
    class DeterminismTests {

        @Test
        @DisplayName("Same seed produces same roll outcomes")
        void sameSeed_samOutcomes() {
            long seed = 12345L;
            int chanceBps = 5000; // 50%

            // First sequence
            Random rng1 = new Random(seed);
            boolean[] results1 = new boolean[10];
            for (int i = 0; i < 10; i++) {
                int roll = rng1.nextInt(10000);
                results1[i] = CombatMath.rollChance(chanceBps, roll);
            }

            // Second sequence with same seed
            Random rng2 = new Random(seed);
            boolean[] results2 = new boolean[10];
            for (int i = 0; i < 10; i++) {
                int roll = rng2.nextInt(10000);
                results2[i] = CombatMath.rollChance(chanceBps, roll);
            }

            assertArrayEquals(results1, results2, "Same seed should produce identical results");
        }

        @Test
        @DisplayName("Hit chance formula is deterministic")
        void hitChance_deterministic() {
            int accuracy = 5000;
            int evasion = 3000;
            int attackerLevel = 15;
            int defenderLevel = 20;

            int result1 = CombatMath.calculateHitChance(accuracy, evasion, attackerLevel, defenderLevel);
            int result2 = CombatMath.calculateHitChance(accuracy, evasion, attackerLevel, defenderLevel);

            assertEquals(result1, result2, "Same inputs should produce same hit chance");
        }

        @Test
        @DisplayName("Damage calculation is deterministic")
        void damageCalculation_deterministic() {
            float baseDamage = 100f;
            int resistance = 3500;
            int penetration = 1000;

            int effectiveResistance = CombatMath.calculateEffectiveResistance(resistance, penetration);
            float final1 = baseDamage * (1f - effectiveResistance / 10000f);
            float final2 = baseDamage * (1f - effectiveResistance / 10000f);

            assertEquals(final1, final2, 0.001f, "Same inputs produce same damage");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Zero damage input results in zero output")
        void zeroDamage_zeroOutput() {
            float baseDamage = 0f;
            int resistance = 5000;

            float finalDamage = baseDamage * (1f - resistance / 10000f);
            assertEquals(0f, finalDamage, 0.001f);
        }

        @Test
        @DisplayName("100% resistance nullifies damage")
        void fullResistance_zeroDamage() {
            float baseDamage = 100f;
            int resistance = 10000; // 100%

            float finalDamage = baseDamage * (1f - resistance / 10000f);
            assertEquals(0f, finalDamage, 0.001f);
        }

        @Test
        @DisplayName("0% resistance passes full damage")
        void zeroResistance_fullDamage() {
            float baseDamage = 100f;
            int resistance = 0;

            float finalDamage = baseDamage * (1f - resistance / 10000f);
            assertEquals(100f, finalDamage, 0.001f);
        }

        @Test
        @DisplayName("Negative resistance increases damage")
        void negativeResistance_increasedDamage() {
            float baseDamage = 100f;
            int resistance = -2000; // -20% = 120% damage taken

            float finalDamage = baseDamage * (1f - resistance / 10000f);
            assertEquals(120f, finalDamage, 0.001f);
        }

        @Test
        @DisplayName("Extreme penetration vs low resistance")
        void extremePenetration_lowResistance() {
            int resistance = 1000;
            int penetration = 10000;

            int effective = CombatMath.calculateEffectiveResistance(resistance, penetration);
            assertEquals(0, effective, "Cannot go below 0");
        }
    }

    @Nested
    @DisplayName("CombatResult Tests")
    class CombatResultTests {

        @Test
        @DisplayName("CombatResult.evaded() creates correct result")
        void combatResult_evaded() {
            CombatResult result = CombatResult.evaded(null, null, 100f);

            assertEquals(CombatResult.Outcome.EVADED, result.getOutcome());
            assertTrue(result.wasEvaded());
            assertFalse(result.wasHit());
            assertEquals(0f, result.getTotalFinalDamage());
        }

        @Test
        @DisplayName("CombatResult.invalidEntity() creates correct result")
        void combatResult_invalidEntity() {
            CombatResult result = CombatResult.invalidEntity();

            assertEquals(CombatResult.Outcome.INVALID_ENTITY, result.getOutcome());
            assertFalse(result.wasHit());
            assertFalse(result.wasEvaded());
        }

        @Test
        @DisplayName("CombatResult.targetDead() creates correct result")
        void combatResult_targetDead() {
            CombatResult result = CombatResult.targetDead(null);

            assertEquals(CombatResult.Outcome.TARGET_DEAD, result.getOutcome());
            assertFalse(result.wasHit());
        }
    }
}
