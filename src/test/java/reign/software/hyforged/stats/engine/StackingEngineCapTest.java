package reign.software.hyforged.stats.engine;

import org.junit.jupiter.api.Test;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.ModifierType;
import reign.software.hyforged.stats.component.StatModifier;

import java.util.Collections;
import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StackingEngine soft/hard cap functionality.
 */
class StackingEngineCapTest {

    private static final StatId CRIT_CHANCE = StatId.hyforged("crit-chance");
    private static final StatId CRIT_CAP_BONUS = StatId.hyforged("crit-cap-bonus");
    private static final int CRIT_STAT_INDEX = 0;
    
    /** Helper to create a FLAT modifier for tests */
    private static StatModifier flat(int value) {
        return new StatModifier.Builder("test")
                .targetStat(CRIT_STAT_INDEX)
                .modifierType(ModifierType.FLAT)
                .value(value)
                .build();
    }
    
    /** Helper to create a CAP modifier for tests */
    private static StatModifier cap(int value) {
        return new StatModifier.Builder("test-cap")
                .targetStat(CRIT_STAT_INDEX)
                .modifierType(ModifierType.CAP)
                .value(value)
                .build();
    }
    
    // ========== Basic Cap Tests ==========
    
    @Test
    void compute_noModifiers_noCaps_returnsBaseValue() {
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .build();
        
        int result = StackingEngine.compute(5000, Collections.emptyList(), statDef, null);
        
        assertEquals(5000, result);
    }
    
    @Test
    void compute_noCaps_returnsComputedValue() {
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .build();
        
        List<StatModifier> modifiers = List.of(flat(3000));
        
        int result = StackingEngine.compute(5000, modifiers, statDef, null);
        
        assertEquals(8000, result);
    }
    
    @Test
    void compute_softCapOnly_capsValueAtSoftCap() {
        // Soft cap at 7500 bps (75%)
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .softCapBps(7500)
                .build();
        
        // Base 5000 + 4000 flat = 9000, should be capped to 7500
        List<StatModifier> modifiers = List.of(flat(4000));
        
        int result = StackingEngine.compute(5000, modifiers, statDef, null);
        
        assertEquals(7500, result);
    }
    
    @Test
    void compute_hardCapOnly_capsValueAtHardCap() {
        // Hard cap at 9000 bps (90%)
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .hardCapBps(9000)
                .build();
        
        // Base 5000 + 5000 flat = 10000, should be capped to 9000
        List<StatModifier> modifiers = List.of(flat(5000));
        
        int result = StackingEngine.compute(5000, modifiers, statDef, null);
        
        assertEquals(9000, result);
    }
    
    @Test
    void compute_valueUnderSoftCap_notCapped() {
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .softCapBps(7500)
                .build();
        
        // Base 5000 + 1000 flat = 6000, under soft cap of 7500
        List<StatModifier> modifiers = List.of(flat(1000));
        
        int result = StackingEngine.compute(5000, modifiers, statDef, null);
        
        assertEquals(6000, result);
    }
    
    // ========== Soft Cap Bonus Stat Tests ==========
    
    @Test
    void compute_softCapWithBonus_raisesCapByBonusValue() {
        // Soft cap 7500, hard cap 10000
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .caps(7500, 10000, CRIT_CAP_BONUS)
                .build();
        
        // Base 5000 + 4000 flat = 9000
        // Bonus stat value = 1000, so effective cap = 7500 + 1000 = 8500
        // Result should be 8500 (capped)
        List<StatModifier> modifiers = List.of(flat(4000));
        
        ToIntFunction<StatId> lookup = statId -> {
            if (statId.equals(CRIT_CAP_BONUS)) {
                return 1000;
            }
            return 0;
        };
        
        int result = StackingEngine.compute(5000, modifiers, statDef, lookup);
        
        assertEquals(8500, result);
    }
    
    @Test
    void compute_softCapWithBonus_cannotExceedHardCap() {
        // Soft cap 7500, hard cap 9000
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .caps(7500, 9000, CRIT_CAP_BONUS)
                .build();
        
        // Base 5000 + 5000 flat = 10000
        // Bonus stat value = 3000, so adjusted soft cap = 7500 + 3000 = 10500
        // But hard cap is 9000, so effective cap = min(10500, 9000) = 9000
        List<StatModifier> modifiers = List.of(flat(5000));
        
        ToIntFunction<StatId> lookup = statId -> {
            if (statId.equals(CRIT_CAP_BONUS)) {
                return 3000;
            }
            return 0;
        };
        
        int result = StackingEngine.compute(5000, modifiers, statDef, lookup);
        
        assertEquals(9000, result);
    }
    
    @Test
    void compute_softCapWithNullLookup_usesBaseSoftCap() {
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .caps(7500, 10000, CRIT_CAP_BONUS)
                .build();
        
        // Base 5000 + 4000 flat = 9000
        // No lookup, so effective cap = soft cap = 7500
        List<StatModifier> modifiers = List.of(flat(4000));
        
        int result = StackingEngine.compute(5000, modifiers, statDef, null);
        
        assertEquals(7500, result);
    }
    
    // ========== Cap Modifier vs StatDefinition Cap Tests ==========
    
    @Test
    void compute_capModifiersAppliedBeforeSoftHardCap() {
        // Soft cap 8000
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .softCapBps(8000)
                .build();
        
        // Base 5000 + 5000 flat = 10000
        // CAP modifier at 9000 → 9000
        // Soft cap at 8000 → 8000
        List<StatModifier> modifiers = List.of(flat(5000), cap(9000));
        
        int result = StackingEngine.compute(5000, modifiers, statDef, null);
        
        assertEquals(8000, result);
    }
    
    // ========== ComputeResult Breakdown Tests ==========
    
    @Test
    void computeWithBreakdown_recordsCapInfo() {
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .caps(7500, 9000, CRIT_CAP_BONUS)
                .build();
        
        List<StatModifier> modifiers = List.of(flat(4000));
        
        ToIntFunction<StatId> lookup = statId -> {
            if (statId.equals(CRIT_CAP_BONUS)) {
                return 500;
            }
            return 0;
        };
        
        StackingEngine.ComputeResult result = StackingEngine.computeWithBreakdown(5000, modifiers, statDef, lookup);
        
        // Verify cap breakdown
        assertEquals(7500, result.softCapBps);
        assertEquals(9000, result.hardCapBps);
        assertEquals(CRIT_CAP_BONUS, result.softCapBonusStat);
        assertEquals(500, result.softCapBonusValue);
        assertEquals(8000, result.effectiveCapBps); // 7500 + 500
        assertTrue(result.wasCapped); // 9000 > 8000
        assertEquals(8000, result.afterSoftHardCap);
        assertEquals(8000, result.finalValue);
    }
    
    @Test
    void computeWithBreakdown_noCapsDefined_noCapsInResult() {
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .build();
        
        List<StatModifier> modifiers = List.of(flat(4000));
        
        StackingEngine.ComputeResult result = StackingEngine.computeWithBreakdown(5000, modifiers, statDef, null);
        
        assertFalse(result.hasCaps());
        assertFalse(result.wasCapped);
        assertEquals(9000, result.finalValue);
    }
    
    @Test
    void computeWithBreakdown_valueUnderCap_wasCappedIsFalse() {
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .softCapBps(9500)
                .build();
        
        List<StatModifier> modifiers = List.of(flat(4000));
        
        StackingEngine.ComputeResult result = StackingEngine.computeWithBreakdown(5000, modifiers, statDef, null);
        
        assertTrue(result.hasCaps());
        assertFalse(result.wasCapped); // 9000 < 9500
        assertEquals(9000, result.finalValue);
    }
    
    // ========== StatDefinition Helper Method Tests ==========
    
    @Test
    void statDefinition_hasSoftCap_returnsTrueWhenDefined() {
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .softCapBps(7500)
                .build();
        
        assertTrue(statDef.hasSoftCap());
        assertFalse(statDef.hasHardCap());
        assertTrue(statDef.hasCaps());
    }
    
    @Test
    void statDefinition_hasHardCap_returnsTrueWhenDefined() {
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .hardCapBps(9000)
                .build();
        
        assertFalse(statDef.hasSoftCap());
        assertTrue(statDef.hasHardCap());
        assertTrue(statDef.hasCaps());
    }
    
    @Test
    void statDefinition_noCaps_hasCapsReturnsFalse() {
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .build();
        
        assertFalse(statDef.hasSoftCap());
        assertFalse(statDef.hasHardCap());
        assertFalse(statDef.hasCaps());
    }
    
    @Test
    void statDefinition_bothCaps_hasCapsReturnsTrue() {
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .caps(7500, 9000, null)
                .build();
        
        assertTrue(statDef.hasSoftCap());
        assertTrue(statDef.hasHardCap());
        assertTrue(statDef.hasCaps());
    }
    
    // ========== Edge Cases ==========
    
    @Test
    void compute_zeroBonusStat_usesBaseSoftCap() {
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .caps(7500, 10000, CRIT_CAP_BONUS)
                .build();
        
        List<StatModifier> modifiers = List.of(flat(4000));
        
        ToIntFunction<StatId> lookup = statId -> 0; // Bonus stat is 0
        
        int result = StackingEngine.compute(5000, modifiers, statDef, lookup);
        
        assertEquals(7500, result); // Capped at soft cap + 0 = 7500
    }
    
    @Test
    void compute_emptyModifiers_stillAppliesCaps() {
        StatDefinition statDef = new StatDefinition.Builder(CRIT_CHANCE)
                .defaultValue(0)
                .bounds(0, 100000)
                .softCapBps(4000)
                .build();
        
        int result = StackingEngine.compute(5000, Collections.emptyList(), statDef, null);
        
        assertEquals(4000, result); // Base 5000 capped to 4000
    }
}
