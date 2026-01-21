package reign.software.hyforged.stats.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.component.ModifierType;
import reign.software.hyforged.stats.component.StatModifier;
import reign.software.hyforged.stats.engine.ScalingEngine;
import reign.software.hyforged.stats.engine.StackingEngine;
import reign.software.hyforged.stats.scaling.LinearScaling;
import reign.software.hyforged.stats.scaling.ThresholdScaling;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete stat interaction system.
 * <p>
 * These tests verify that stats properly buff each other through:
 * - Scaling chains (attribute → derived stat)
 * - Modifier propagation (modifying source affects derived)
 * - Dependency dirty flag expansion
 * - Full computation pipeline
 */
class StatInteractionIntegrationTest {

    // Test stat IDs
    private static final StatId STRENGTH = StatId.hyforged("test-strength");
    private static final StatId DEXTERITY = StatId.hyforged("test-dexterity");
    private static final StatId INTELLIGENCE = StatId.hyforged("test-intelligence");
    private static final StatId LUCK = StatId.hyforged("test-luck");
    private static final StatId CONSTITUTION = StatId.hyforged("test-constitution");
    
    private static final StatId ATTACK_POWER = StatId.hyforged("test-attack-power");
    private static final StatId SPELL_POWER = StatId.hyforged("test-spell-power");
    private static final StatId CRIT_CHANCE = StatId.hyforged("test-crit-chance-bps");
    private static final StatId MAX_HEALTH = StatId.hyforged("test-max-health");
    private static final StatId EVASION = StatId.hyforged("test-evasion-bps");
    
    private StatDefinitionRegistry registry;
    
    @BeforeEach
    void setUp() {
        StatDefinitionRegistry.reset();
        registry = StatDefinitionRegistry.get();
    }
    
    /**
     * Helper to simulate stat computation for a component.
     * This mimics what HyforgedStatComputeSystem does.
     */
    private void recomputeStats(HyforgedStatComponent component) {
        int[] evalOrder = registry.getEvaluationOrder();
        List<StatModifier> modifiers = component.getModifiers();
        
        for (int statIdx : evalOrder) {
            StatDefinition statDef = registry.getStat(statIdx);
            if (statDef == null) continue;
            
            // Compute base value
            int baseValue;
            if (statDef.hasScaling()) {
                baseValue = ScalingEngine.computeScaledBase(
                        statDef,
                        component::getCachedValue,
                        registry
                );
            } else {
                baseValue = component.getBaseValue(statIdx);
            }
            
            // Collect applicable modifiers
            List<StatModifier> applicable = new ArrayList<>();
            for (StatModifier mod : modifiers) {
                if (mod.targetStatIndex() == statIdx) {
                    applicable.add(mod);
                }
            }
            
            // Compute final value
            int finalValue = StackingEngine.compute(baseValue, applicable, statDef);
            component.setCachedValue(statIdx, finalValue);
        }
        
        component.clearAllDirtyFlags();
    }
    
    @Nested
    @DisplayName("Basic Stat Chain Tests")
    class BasicStatChainTests {
        
        @Test
        @DisplayName("STR → Attack Power chain: increasing STR increases Attack Power")
        void strengthToAttackPower_chain() {
            // Setup: STR (base 10) → Attack Power (linear x2)
            StatDefinition strength = new StatDefinition.Builder(STRENGTH)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition attackPower = new StatDefinition.Builder(ATTACK_POWER)
                    .defaultValue(0)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 2.0))
                    .build();
            
            registry.registerStat(strength);
            registry.registerStat(attackPower);
            registry.freeze();
            
            HyforgedStatComponent component = new HyforgedStatComponent();
            
            // Initial computation with default STR=10
            recomputeStats(component);
            
            int strIdx = registry.getIndex(STRENGTH);
            int apIdx = registry.getIndex(ATTACK_POWER);
            
            assertEquals(10, component.getCachedValue(strIdx), "Default STR should be 10");
            assertEquals(20, component.getCachedValue(apIdx), "10 STR * 2 = 20 Attack Power");
            
            // Now increase STR base value to 25
            component.setBaseValue(strIdx, 25);
            recomputeStats(component);
            
            assertEquals(25, component.getCachedValue(strIdx), "STR should now be 25");
            assertEquals(50, component.getCachedValue(apIdx), "25 STR * 2 = 50 Attack Power");
        }
        
        @Test
        @DisplayName("INT → Spell Power chain: increasing INT increases Spell Power")
        void intelligenceToSpellPower_chain() {
            StatDefinition intelligence = new StatDefinition.Builder(INTELLIGENCE)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition spellPower = new StatDefinition.Builder(SPELL_POWER)
                    .defaultValue(0)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(INTELLIGENCE, 3.0))
                    .build();
            
            registry.registerStat(intelligence);
            registry.registerStat(spellPower);
            registry.freeze();
            
            HyforgedStatComponent component = new HyforgedStatComponent();
            
            int intIdx = registry.getIndex(INTELLIGENCE);
            int spIdx = registry.getIndex(SPELL_POWER);
            
            // Set INT to 20
            component.setBaseValue(intIdx, 20);
            recomputeStats(component);
            
            assertEquals(20, component.getCachedValue(intIdx));
            assertEquals(60, component.getCachedValue(spIdx), "20 INT * 3 = 60 Spell Power");
        }
        
        @Test
        @DisplayName("LUCK → Crit Chance (threshold): every 5 LUCK adds 1% crit")
        void luckToCritChance_threshold() {
            StatDefinition luck = new StatDefinition.Builder(LUCK)
                    .defaultValue(5)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition critChance = new StatDefinition.Builder(CRIT_CHANCE)
                    .defaultValue(0)
                    .bounds(0, 10000)
                    .addScaling(new ThresholdScaling(LUCK, 5, 100)) // 100 bps per 5 LUCK
                    .build();
            
            registry.registerStat(luck);
            registry.registerStat(critChance);
            registry.freeze();
            
            HyforgedStatComponent component = new HyforgedStatComponent();
            
            int luckIdx = registry.getIndex(LUCK);
            int critIdx = registry.getIndex(CRIT_CHANCE);
            
            // Set LUCK to 23
            component.setBaseValue(luckIdx, 23);
            recomputeStats(component);
            
            assertEquals(23, component.getCachedValue(luckIdx));
            // 23 / 5 = 4 steps → 4 * 100 = 400 bps (4% crit)
            assertEquals(400, component.getCachedValue(critIdx), "23 LUCK = 4 steps * 100 = 400 bps");
        }
    }
    
    @Nested
    @DisplayName("Modifier Propagation Tests")
    class ModifierPropagationTests {
        
        @Test
        @DisplayName("Flat modifier on source stat propagates to derived stat")
        void flatModifierOnSource_propagatesToDerived() {
            StatDefinition strength = new StatDefinition.Builder(STRENGTH)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition attackPower = new StatDefinition.Builder(ATTACK_POWER)
                    .defaultValue(0)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 2.0))
                    .build();
            
            registry.registerStat(strength);
            registry.registerStat(attackPower);
            registry.freeze();
            
            HyforgedStatComponent component = new HyforgedStatComponent();
            
            int strIdx = registry.getIndex(STRENGTH);
            int apIdx = registry.getIndex(ATTACK_POWER);
            
            // Add +5 STR modifier (from equipment)
            StatModifier strBuff = new StatModifier.Builder("sword")
                    .targetStat(strIdx)
                    .modifierType(ModifierType.FLAT)
                    .value(5)
                    .build();
            
            component.addModifier(strBuff);
            recomputeStats(component);
            
            // Base 10 + 5 modifier = 15 STR
            assertEquals(15, component.getCachedValue(strIdx), "10 base + 5 flat = 15 STR");
            // 15 STR * 2 = 30 Attack Power
            assertEquals(30, component.getCachedValue(apIdx), "15 STR * 2 = 30 Attack Power");
        }
        
        @Test
        @DisplayName("Percentage modifier on source stat propagates to derived stat")
        void percentModifierOnSource_propagatesToDerived() {
            StatDefinition strength = new StatDefinition.Builder(STRENGTH)
                    .defaultValue(20)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition attackPower = new StatDefinition.Builder(ATTACK_POWER)
                    .defaultValue(0)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 2.0))
                    .build();
            
            registry.registerStat(strength);
            registry.registerStat(attackPower);
            registry.freeze();
            
            HyforgedStatComponent component = new HyforgedStatComponent();
            
            int strIdx = registry.getIndex(STRENGTH);
            int apIdx = registry.getIndex(ATTACK_POWER);
            
            // Add +50% STR modifier (5000 bps)
            StatModifier strBuff = new StatModifier.Builder("buff")
                    .targetStat(strIdx)
                    .modifierType(ModifierType.INCREASED)
                    .value(5000) // +50%
                    .build();
            
            component.addModifier(strBuff);
            recomputeStats(component);
            
            // Base 20 * 1.5 = 30 STR
            assertEquals(30, component.getCachedValue(strIdx), "20 base * 1.5 = 30 STR");
            // 30 STR * 2 = 60 Attack Power
            assertEquals(60, component.getCachedValue(apIdx), "30 STR * 2 = 60 Attack Power");
        }
        
        @Test
        @DisplayName("Modifiers on both source and derived stack correctly")
        void modifiersOnBothSourceAndDerived_stackCorrectly() {
            StatDefinition strength = new StatDefinition.Builder(STRENGTH)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition attackPower = new StatDefinition.Builder(ATTACK_POWER)
                    .defaultValue(0)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 2.0))
                    .build();
            
            registry.registerStat(strength);
            registry.registerStat(attackPower);
            registry.freeze();
            
            HyforgedStatComponent component = new HyforgedStatComponent();
            
            int strIdx = registry.getIndex(STRENGTH);
            int apIdx = registry.getIndex(ATTACK_POWER);
            
            // Add +10 STR modifier
            StatModifier strBuff = new StatModifier.Builder("belt")
                    .targetStat(strIdx)
                    .modifierType(ModifierType.FLAT)
                    .value(10)
                    .build();
            
            // Add +50 Attack Power modifier (flat bonus to AP itself)
            StatModifier apBuff = new StatModifier.Builder("ring")
                    .targetStat(apIdx)
                    .modifierType(ModifierType.FLAT)
                    .value(50)
                    .build();
            
            component.addModifier(strBuff);
            component.addModifier(apBuff);
            recomputeStats(component);
            
            // Base 10 + 10 = 20 STR
            assertEquals(20, component.getCachedValue(strIdx), "10 base + 10 flat = 20 STR");
            // 20 STR * 2 = 40 base AP + 50 flat AP = 90 Attack Power
            assertEquals(90, component.getCachedValue(apIdx), "40 from scaling + 50 flat = 90 AP");
        }
    }
    
    @Nested
    @DisplayName("Multi-Source Scaling Tests")
    class MultiSourceScalingTests {
        
        @Test
        @DisplayName("Derived stat with multiple sources sums contributions")
        void multipleSourceStats_sumContributions() {
            StatDefinition dexterity = new StatDefinition.Builder(DEXTERITY)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition luck = new StatDefinition.Builder(LUCK)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            // Crit scales from both DEX and LUCK
            StatDefinition critChance = new StatDefinition.Builder(CRIT_CHANCE)
                    .defaultValue(500) // 5% base crit
                    .bounds(0, 10000)
                    .addScaling(new ThresholdScaling(DEXTERITY, 10, 50))  // 50 bps per 10 DEX
                    .addScaling(new ThresholdScaling(LUCK, 5, 100))       // 100 bps per 5 LUCK
                    .build();
            
            registry.registerStat(dexterity);
            registry.registerStat(luck);
            registry.registerStat(critChance);
            registry.freeze();
            
            HyforgedStatComponent component = new HyforgedStatComponent();
            
            int dexIdx = registry.getIndex(DEXTERITY);
            int luckIdx = registry.getIndex(LUCK);
            int critIdx = registry.getIndex(CRIT_CHANCE);
            
            // Set DEX=30, LUCK=20
            component.setBaseValue(dexIdx, 30);
            component.setBaseValue(luckIdx, 20);
            recomputeStats(component);
            
            assertEquals(30, component.getCachedValue(dexIdx));
            assertEquals(20, component.getCachedValue(luckIdx));
            
            // 30 DEX / 10 = 3 steps * 50 = 150 bps from DEX
            // 20 LUCK / 5 = 4 steps * 100 = 400 bps from LUCK
            // Total = 150 + 400 = 550 bps (base 500 is ignored since we have scaling)
            assertEquals(550, component.getCachedValue(critIdx), 
                    "150 bps from DEX + 400 bps from LUCK = 550 bps");
        }
        
        @Test
        @DisplayName("Single source stat affects multiple derived stats")
        void singleSource_affectsMultipleDerived() {
            StatDefinition strength = new StatDefinition.Builder(STRENGTH)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition attackPower = new StatDefinition.Builder(ATTACK_POWER)
                    .defaultValue(0)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 2.0))
                    .build();
            
            StatDefinition maxHealth = new StatDefinition.Builder(MAX_HEALTH)
                    .defaultValue(100)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 5.0)) // +5 HP per STR
                    .build();
            
            registry.registerStat(strength);
            registry.registerStat(attackPower);
            registry.registerStat(maxHealth);
            registry.freeze();
            
            HyforgedStatComponent component = new HyforgedStatComponent();
            
            int strIdx = registry.getIndex(STRENGTH);
            int apIdx = registry.getIndex(ATTACK_POWER);
            int hpIdx = registry.getIndex(MAX_HEALTH);
            
            // Set STR=20
            component.setBaseValue(strIdx, 20);
            recomputeStats(component);
            
            assertEquals(20, component.getCachedValue(strIdx));
            assertEquals(40, component.getCachedValue(apIdx), "20 STR * 2 = 40 AP");
            assertEquals(100, component.getCachedValue(hpIdx), "20 STR * 5 = 100 HP (scaling only)");
        }
    }
    
    @Nested
    @DisplayName("Dependency Graph Tests")
    class DependencyGraphTests {
        
        @Test
        @DisplayName("Modifying source stat marks derived stat as dependent")
        void sourceStat_hasDependents() {
            StatDefinition strength = new StatDefinition.Builder(STRENGTH)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition attackPower = new StatDefinition.Builder(ATTACK_POWER)
                    .defaultValue(0)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 2.0))
                    .build();
            
            registry.registerStat(strength);
            registry.registerStat(attackPower);
            registry.freeze();
            
            int strIdx = registry.getIndex(STRENGTH);
            int apIdx = registry.getIndex(ATTACK_POWER);
            
            // STR should have Attack Power as a dependent
            var dependents = registry.getDependentStats(strIdx);
            assertTrue(dependents.contains(apIdx), "STR should have Attack Power as dependent");
            
            // Attack Power should have STR as a dependency
            var dependencies = registry.getDependencies(apIdx);
            assertTrue(dependencies.contains(strIdx), "Attack Power should depend on STR");
        }
        
        @Test
        @DisplayName("Evaluation order respects dependencies")
        void evaluationOrder_respectsDependencies() {
            StatDefinition strength = new StatDefinition.Builder(STRENGTH)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition attackPower = new StatDefinition.Builder(ATTACK_POWER)
                    .defaultValue(0)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 2.0))
                    .build();
            
            registry.registerStat(strength);
            registry.registerStat(attackPower);
            registry.freeze();
            
            int[] evalOrder = registry.getEvaluationOrder();
            int strIdx = registry.getIndex(STRENGTH);
            int apIdx = registry.getIndex(ATTACK_POWER);
            
            int strPos = -1;
            int apPos = -1;
            for (int i = 0; i < evalOrder.length; i++) {
                if (evalOrder[i] == strIdx) strPos = i;
                if (evalOrder[i] == apIdx) apPos = i;
            }
            
            assertTrue(strPos < apPos, 
                    "STR (pos " + strPos + ") should be evaluated before Attack Power (pos " + apPos + ")");
        }
        
        @Test
        @DisplayName("Complex dependency chain evaluates in correct order")
        void complexDependencyChain_correctOrder() {
            // Chain: LUCK → CRIT_CHANCE
            //        STR → ATTACK_POWER
            //        CON → MAX_HEALTH
            StatDefinition luck = new StatDefinition.Builder(LUCK)
                    .defaultValue(5)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition strength = new StatDefinition.Builder(STRENGTH)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition constitution = new StatDefinition.Builder(CONSTITUTION)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition critChance = new StatDefinition.Builder(CRIT_CHANCE)
                    .defaultValue(0)
                    .bounds(0, 10000)
                    .addScaling(new ThresholdScaling(LUCK, 5, 100))
                    .build();
            
            StatDefinition attackPower = new StatDefinition.Builder(ATTACK_POWER)
                    .defaultValue(0)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 2.0))
                    .build();
            
            StatDefinition maxHealth = new StatDefinition.Builder(MAX_HEALTH)
                    .defaultValue(100)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(CONSTITUTION, 5.0))
                    .build();
            
            registry.registerStat(luck);
            registry.registerStat(strength);
            registry.registerStat(constitution);
            registry.registerStat(critChance);
            registry.registerStat(attackPower);
            registry.registerStat(maxHealth);
            registry.freeze();
            
            int[] evalOrder = registry.getEvaluationOrder();
            
            int luckIdx = registry.getIndex(LUCK);
            int strIdx = registry.getIndex(STRENGTH);
            int conIdx = registry.getIndex(CONSTITUTION);
            int critIdx = registry.getIndex(CRIT_CHANCE);
            int apIdx = registry.getIndex(ATTACK_POWER);
            int hpIdx = registry.getIndex(MAX_HEALTH);
            
            // Find positions
            int[] positions = new int[6];
            for (int i = 0; i < evalOrder.length; i++) {
                if (evalOrder[i] == luckIdx) positions[0] = i;
                if (evalOrder[i] == strIdx) positions[1] = i;
                if (evalOrder[i] == conIdx) positions[2] = i;
                if (evalOrder[i] == critIdx) positions[3] = i;
                if (evalOrder[i] == apIdx) positions[4] = i;
                if (evalOrder[i] == hpIdx) positions[5] = i;
            }
            
            // Sources should come before derived
            assertTrue(positions[0] < positions[3], "LUCK should come before CRIT_CHANCE");
            assertTrue(positions[1] < positions[4], "STR should come before ATTACK_POWER");
            assertTrue(positions[2] < positions[5], "CON should come before MAX_HEALTH");
        }
    }
    
    @Nested
    @DisplayName("Circular Dependency Prevention Tests")
    class CircularDependencyTests {
        
        @Test
        @DisplayName("Circular dependency throws exception on freeze")
        void circularDependency_throwsOnFreeze() {
            // Create circular: A → B → A
            StatId statA = StatId.hyforged("test-stat-a");
            StatId statB = StatId.hyforged("test-stat-b");
            
            StatDefinition defA = new StatDefinition.Builder(statA)
                    .defaultValue(10)
                    .bounds(0, 100)
                    .addScaling(new LinearScaling(statB, 1.0))
                    .build();
            
            StatDefinition defB = new StatDefinition.Builder(statB)
                    .defaultValue(10)
                    .bounds(0, 100)
                    .addScaling(new LinearScaling(statA, 1.0))
                    .build();
            
            registry.registerStat(defA);
            registry.registerStat(defB);
            
            assertThrows(IllegalStateException.class, () -> registry.freeze(),
                    "Circular dependency should throw on freeze");
        }
    }
    
    @Nested
    @DisplayName("Full Pipeline Integration Tests")
    class FullPipelineTests {
        
        @Test
        @DisplayName("Complete ARPG stat chain works end-to-end")
        void completeArpgStatChain() {
            // Setup realistic ARPG stat chain:
            // STR(base) → Attack Power(scaled) + modifiers
            // DEX(base) → Crit Chance(scaled) + modifiers
            
            StatDefinition strength = new StatDefinition.Builder(STRENGTH)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition dexterity = new StatDefinition.Builder(DEXTERITY)
                    .defaultValue(10)
                    .bounds(1, 999)
                    .build();
            
            StatDefinition attackPower = new StatDefinition.Builder(ATTACK_POWER)
                    .defaultValue(0)
                    .bounds(0, Integer.MAX_VALUE)
                    .addScaling(new LinearScaling(STRENGTH, 2.0))
                    .build();
            
            StatDefinition critChance = new StatDefinition.Builder(CRIT_CHANCE)
                    .defaultValue(500) // 5% base
                    .bounds(0, 7500)   // 75% cap
                    .softCapBps(7500)
                    .addScaling(new ThresholdScaling(DEXTERITY, 5, 50)) // +0.5% per 5 DEX
                    .build();
            
            registry.registerStat(strength);
            registry.registerStat(dexterity);
            registry.registerStat(attackPower);
            registry.registerStat(critChance);
            registry.freeze();
            
            HyforgedStatComponent component = new HyforgedStatComponent();
            
            int strIdx = registry.getIndex(STRENGTH);
            int dexIdx = registry.getIndex(DEXTERITY);
            int apIdx = registry.getIndex(ATTACK_POWER);
            int critIdx = registry.getIndex(CRIT_CHANCE);
            
            // Base stats
            component.setBaseValue(strIdx, 25);  // 25 STR
            component.setBaseValue(dexIdx, 40);  // 40 DEX
            
            // Equipment modifiers
            StatModifier strBuff = new StatModifier.Builder("mighty_sword")
                    .targetStat(strIdx)
                    .modifierType(ModifierType.FLAT)
                    .value(10) // +10 STR
                    .build();
            
            StatModifier apBuff = new StatModifier.Builder("attack_ring")
                    .targetStat(apIdx)
                    .modifierType(ModifierType.INCREASED)
                    .value(2000) // +20% Attack Power
                    .build();
            
            StatModifier critBuff = new StatModifier.Builder("crit_amulet")
                    .targetStat(critIdx)
                    .modifierType(ModifierType.FLAT)
                    .value(200) // +2% crit
                    .build();
            
            component.addModifier(strBuff);
            component.addModifier(apBuff);
            component.addModifier(critBuff);
            
            recomputeStats(component);
            
            // Verify STR: 25 base + 10 flat = 35
            assertEquals(35, component.getCachedValue(strIdx));
            
            // Verify Attack Power: 
            // Scaling: 35 STR * 2 = 70 base
            // Modifiers: 70 * 1.2 = 84
            assertEquals(84, component.getCachedValue(apIdx));
            
            // Verify DEX: 40 (no modifiers)
            assertEquals(40, component.getCachedValue(dexIdx));
            
            // Verify Crit Chance:
            // Scaling: 40 DEX / 5 = 8 steps * 50 = 400 bps
            // + 200 flat bps = 600 bps (6%)
            assertEquals(600, component.getCachedValue(critIdx));
        }
    }
}
