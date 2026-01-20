package reign.software.hyforged.stats.condition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static reign.software.hyforged.stats.condition.TestFixtures.StatusEffects;
import static reign.software.hyforged.stats.condition.TestFixtures.WeaponTypes;

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
                .withWeaponType(WeaponTypes.SWORD)
                .withStatusEffect(StatusEffects.BLEEDING)
                .build();
        
        assertEquals(5000, context.healthPercentBps());
        assertTrue(context.isInCombat());
        assertTrue(context.hasShieldEquipped());
        assertTrue(context.equippedWeaponTypes().contains(WeaponTypes.SWORD));
        assertTrue(context.statusEffects().contains(StatusEffects.BLEEDING));
    }

    @Test
    void queryContext_customModIds_work() {
        // Test that custom mod-defined IDs work
        String customEffect = "mymod:radiation";
        String customWeapon = "mymod:laser_rifle";
        
        QueryContext context = QueryContext.builder()
                .withStatusEffect(customEffect)
                .withWeaponType(customWeapon)
                .build();
        
        assertTrue(context.hasStatusEffect(customEffect));
        assertTrue(context.hasWeaponType(customWeapon));
        assertFalse(context.hasStatusEffect(StatusEffects.BLEEDING));
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
        StateCondition condition = StateCondition.whileAffectedBy(StatusEffects.POISONED);
        
        QueryContext withPoison = QueryContext.builder()
                .withStatusEffect(StatusEffects.POISONED)
                .build();
        assertTrue(condition.evaluate(null, withPoison));
        
        QueryContext noPoison = QueryContext.builder().build();
        assertFalse(condition.evaluate(null, noPoison));
    }

    @Test
    void stateCondition_customEffect_evaluatesCorrectly() {
        // Test with a mod-defined custom effect
        String customEffect = "mymod:irradiated";
        StateCondition condition = StateCondition.whileAffectedBy(customEffect);
        
        QueryContext withEffect = QueryContext.builder()
                .withStatusEffect(customEffect)
                .build();
        assertTrue(condition.evaluate(null, withEffect));
        
        QueryContext noEffect = QueryContext.builder().build();
        assertFalse(condition.evaluate(null, noEffect));
    }

    @Test
    void equipmentCondition_weaponType_evaluatesCorrectly() {
        EquipmentCondition condition = EquipmentCondition.wielding(WeaponTypes.BOW);
        
        QueryContext hasBow = QueryContext.builder()
                .withWeaponType(WeaponTypes.BOW)
                .build();
        assertTrue(condition.evaluate(null, hasBow));
        
        QueryContext hasSword = QueryContext.builder()
                .withWeaponType(WeaponTypes.SWORD)
                .build();
        assertFalse(condition.evaluate(null, hasSword));
    }

    @Test
    void equipmentCondition_customWeapon_evaluatesCorrectly() {
        // Test with a mod-defined custom weapon
        String customWeapon = "mymod:plasma_cannon";
        EquipmentCondition condition = EquipmentCondition.wielding(customWeapon);
        
        QueryContext hasCustom = QueryContext.builder()
                .withWeaponType(customWeapon)
                .build();
        assertTrue(condition.evaluate(null, hasCustom));
        
        QueryContext hasSword = QueryContext.builder()
                .withWeaponType(WeaponTypes.SWORD)
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
        ModifierCondition poisoned = StateCondition.whileAffectedBy(StatusEffects.POISONED);
        ModifierCondition combined = lowHealth.and(poisoned);
        
        // Both true
        QueryContext bothTrue = QueryContext.builder()
                .withHealthPercent(2000)
                .withStatusEffect(StatusEffects.POISONED)
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
        ModifierCondition poisoned = StateCondition.whileAffectedBy(StatusEffects.POISONED);
        ModifierCondition notPoisoned = poisoned.negate();
        
        QueryContext hasPosion = QueryContext.builder()
                .withStatusEffect(StatusEffects.POISONED)
                .build();
        assertFalse(notPoisoned.evaluate(null, hasPosion));
        
        QueryContext noPoison = QueryContext.builder().build();
        assertTrue(notPoisoned.evaluate(null, noPoison));
    }
}
