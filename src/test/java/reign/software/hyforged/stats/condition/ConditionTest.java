package reign.software.hyforged.stats.condition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QueryContext and condition implementations.
 */
class ConditionTest {

    @Test
    void queryContext_builder_createsCorrectContext() {
        QueryContext context = QueryContext.builder()
                .withHealthPercent(5000)
                .inCombat(true)
                .withShield(true)
                .withWeaponType(QueryContext.WeaponType.SWORD)
                .withStatusEffect(QueryContext.StatusEffect.BLEEDING)
                .build();
        
        assertEquals(5000, context.healthPercentBps());
        assertTrue(context.isInCombat());
        assertTrue(context.hasShieldEquipped());
        assertTrue(context.equippedWeaponTypes().contains(QueryContext.WeaponType.SWORD));
        assertTrue(context.statusEffects().contains(QueryContext.StatusEffect.BLEEDING));
    }

    @Test
    void healthThreshold_below_evaluatesCorrectly() {
        HealthThresholdCondition condition = HealthThresholdCondition.below(3000);
        
        // 20% health (below 30%)
        QueryContext lowHealth = QueryContext.builder().withHealthPercent(2000).build();
        assertTrue(condition.evaluate(null, lowHealth));
        
        // 50% health (above 30%)
        QueryContext midHealth = QueryContext.builder().withHealthPercent(5000).build();
        assertFalse(condition.evaluate(null, midHealth));
    }

    @Test
    void healthThreshold_atOrAbove_evaluatesCorrectly() {
        HealthThresholdCondition condition = HealthThresholdCondition.atOrAbove(7000);
        
        // 80% health (above 70%)
        QueryContext highHealth = QueryContext.builder().withHealthPercent(8000).build();
        assertTrue(condition.evaluate(null, highHealth));
        
        // 50% health (below 70%)
        QueryContext midHealth = QueryContext.builder().withHealthPercent(5000).build();
        assertFalse(condition.evaluate(null, midHealth));
    }

    @Test
    void stateCondition_hasEffect_evaluatesCorrectly() {
        StateCondition condition = StateCondition.whileAffectedBy(QueryContext.StatusEffect.POISONED);
        
        QueryContext withPoison = QueryContext.builder()
                .withStatusEffect(QueryContext.StatusEffect.POISONED)
                .build();
        assertTrue(condition.evaluate(null, withPoison));
        
        QueryContext noPoison = QueryContext.builder().build();
        assertFalse(condition.evaluate(null, noPoison));
    }

    @Test
    void equipmentCondition_weaponType_evaluatesCorrectly() {
        EquipmentCondition condition = EquipmentCondition.wielding(QueryContext.WeaponType.BOW);
        
        QueryContext hasBow = QueryContext.builder()
                .withWeaponType(QueryContext.WeaponType.BOW)
                .build();
        assertTrue(condition.evaluate(null, hasBow));
        
        QueryContext hasSword = QueryContext.builder()
                .withWeaponType(QueryContext.WeaponType.SWORD)
                .build();
        assertFalse(condition.evaluate(null, hasSword));
    }

    @Test
    void equipmentCondition_shield_evaluatesCorrectly() {
        EquipmentCondition condition = EquipmentCondition.withShield();
        
        QueryContext withShield = QueryContext.builder().withShield(true).build();
        assertTrue(condition.evaluate(null, withShield));
        
        QueryContext noShield = QueryContext.builder().withShield(false).build();
        assertFalse(condition.evaluate(null, noShield));
    }

    @Test
    void condition_and_combinesProperly() {
        ModifierCondition lowHealth = HealthThresholdCondition.below(3000);
        ModifierCondition poisoned = StateCondition.whileAffectedBy(QueryContext.StatusEffect.POISONED);
        ModifierCondition combined = lowHealth.and(poisoned);
        
        // Both true
        QueryContext bothTrue = QueryContext.builder()
                .withHealthPercent(2000)
                .withStatusEffect(QueryContext.StatusEffect.POISONED)
                .build();
        assertTrue(combined.evaluate(null, bothTrue));
        
        // One false
        QueryContext oneFalse = QueryContext.builder()
                .withHealthPercent(2000)
                .build();
        assertFalse(combined.evaluate(null, oneFalse));
    }

    @Test
    void condition_or_combinesProperly() {
        ModifierCondition lowHealth = HealthThresholdCondition.below(3000);
        ModifierCondition hasShield = EquipmentCondition.withShield();
        ModifierCondition combined = lowHealth.or(hasShield);
        
        // Low health, no shield
        QueryContext lowHealthOnly = QueryContext.builder()
                .withHealthPercent(2000)
                .withShield(false)
                .build();
        assertTrue(combined.evaluate(null, lowHealthOnly));
        
        // High health, has shield
        QueryContext shieldOnly = QueryContext.builder()
                .withHealthPercent(8000)
                .withShield(true)
                .build();
        assertTrue(combined.evaluate(null, shieldOnly));
        
        // Neither
        QueryContext neither = QueryContext.builder()
                .withHealthPercent(8000)
                .withShield(false)
                .build();
        assertFalse(combined.evaluate(null, neither));
    }

    @Test
    void condition_negate_invertsProperly() {
        ModifierCondition poisoned = StateCondition.whileAffectedBy(QueryContext.StatusEffect.POISONED);
        ModifierCondition notPoisoned = poisoned.negate();
        
        QueryContext hasPosion = QueryContext.builder()
                .withStatusEffect(QueryContext.StatusEffect.POISONED)
                .build();
        assertFalse(notPoisoned.evaluate(null, hasPosion));
        
        QueryContext noPoison = QueryContext.builder().build();
        assertTrue(notPoisoned.evaluate(null, noPoison));
    }
}
