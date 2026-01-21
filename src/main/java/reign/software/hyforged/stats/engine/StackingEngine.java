package reign.software.hyforged.stats.engine;

import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.ModifierType;
import reign.software.hyforged.stats.component.StatModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Stacking engine for ARPG-style modifier computation.
 * <p>
 * Computes final stat values using deterministic stacking order:
 * 1. Sum all FLAT modifiers
 * 2. Sum all INCREASED (% additive) → apply as (1 + sum/10000)
 * 3. Multiply all MORE (% multiplicative) sequentially
 * 4. Apply CAP/clamps last
 * <p>
 * All math uses integers with long widening to prevent overflow.
 * Basis points: 10000 = 100%
 * Division rounds toward floor.
 * <p>
 * This is a pure computation utility - no state, following ECS principles.
 */
public final class StackingEngine {
    
    private StackingEngine() {} // Static utility class
    
    /** Basis points representing 100% (10000 bps = 100%) */
    public static final int BPS_100_PERCENT = 10000;
    
    /**
     * Compute the final value of a stat given base value and modifiers.
     * <p>
     * This overload does not apply soft/hard caps defined in the StatDefinition.
     * Use {@link #compute(int, List, StatDefinition, ToIntFunction)} to include cap support.
     * 
     * @param baseValue The base value before modifiers
     * @param modifiers List of modifiers to apply (only those matching the stat)
     * @param statDef The stat definition (for bounds clamping)
     * @return The final computed value
     */
    public static int compute(int baseValue, @Nonnull List<StatModifier> modifiers, @Nonnull StatDefinition statDef) {
        return compute(baseValue, modifiers, statDef, null);
    }
    
    /**
     * Compute the final value of a stat given base value and modifiers, with soft/hard cap support.
     * <p>
     * Stacking order:
     * 1. Sum all FLAT modifiers
     * 2. Sum all INCREASED (% additive) → apply as (1 + sum/10000)
     * 3. Multiply all MORE (% multiplicative) sequentially
     * 4. Apply CAP modifiers (min/max clamps from modifiers)
     * 5. Apply soft/hard caps from StatDefinition (if defined)
     * 6. Final clamp to stat definition bounds
     * 
     * @param baseValue The base value before modifiers
     * @param modifiers List of modifiers to apply (only those matching the stat)
     * @param statDef The stat definition (for bounds clamping and caps)
     * @param statValueLookup Function to lookup other stat values (for soft cap bonus stat), can be null
     * @return The final computed value
     */
    public static int compute(int baseValue, @Nonnull List<StatModifier> modifiers, 
                              @Nonnull StatDefinition statDef, 
                              @Nullable ToIntFunction<StatId> statValueLookup) {
        if (modifiers.isEmpty()) {
            int uncapped = clamp(baseValue, statDef.minValue(), statDef.maxValue());
            return applySoftHardCaps(uncapped, statDef, statValueLookup);
        }
        
        // Sort modifiers by type order, then by priority for determinism
        List<StatModifier> sorted = new ArrayList<>(modifiers);
        sorted.sort(Comparator
            .comparingInt((StatModifier m) -> m.modifierType().getOrder())
            .thenComparingInt(StatModifier::priority)
            .thenComparing(StatModifier::sourceId) // Final tie-breaker for determinism
        );
        
        // Step 1: Sum all FLAT modifiers
        long flatSum = 0;
        for (StatModifier mod : sorted) {
            if (mod.modifierType() == ModifierType.FLAT) {
                flatSum += mod.value();
            }
        }
        
        // Apply flat modifiers to base
        long current = (long) baseValue + flatSum;
        
        // Step 2: Sum all INCREASED modifiers and apply as multiplier
        long increasedSum = 0; // In basis points
        for (StatModifier mod : sorted) {
            if (mod.modifierType() == ModifierType.INCREASED) {
                increasedSum += mod.value();
            }
        }
        
        // Apply increased: current * (1 + increasedSum/10000)
        // = current * (10000 + increasedSum) / 10000
        if (increasedSum != 0) {
            current = (current * (BPS_100_PERCENT + increasedSum)) / BPS_100_PERCENT;
        }
        
        // Step 3: Apply each MORE modifier sequentially (multiplicative)
        for (StatModifier mod : sorted) {
            if (mod.modifierType() == ModifierType.MORE) {
                // current * (1 + value/10000)
                // = current * (10000 + value) / 10000
                current = (current * (BPS_100_PERCENT + mod.value())) / BPS_100_PERCENT;
            }
        }
        
        // Step 4: Apply CAP modifiers (min/max clamps)
        // CAP modifiers: positive value = max cap, negative value = min cap
        Integer minCap = null;
        Integer maxCap = null;
        for (StatModifier mod : sorted) {
            if (mod.modifierType() == ModifierType.CAP) {
                if (mod.value() >= 0) {
                    // Max cap - take the lowest max cap
                    if (maxCap == null || mod.value() < maxCap) {
                        maxCap = mod.value();
                    }
                } else {
                    // Min cap (negative value represents min) - take the highest min cap
                    int minVal = -mod.value();
                    if (minCap == null || minVal > minCap) {
                        minCap = minVal;
                    }
                }
            }
        }
        
        // Apply modifier caps
        if (minCap != null && current < minCap) {
            current = minCap;
        }
        if (maxCap != null && current > maxCap) {
            current = maxCap;
        }
        
        // Step 5: Apply soft/hard caps from StatDefinition
        int afterModifierCaps = clamp(current, statDef.minValue(), statDef.maxValue());
        return applySoftHardCaps(afterModifierCaps, statDef, statValueLookup);
    }
    
    /**
     * Apply soft/hard caps from a StatDefinition.
     * <p>
     * The effective cap is calculated as:
     * - effectiveCap = min(softCap + bonusStatValue, hardCap)
     * - If no soft cap is defined, only the hard cap is applied
     * - If no hard cap is defined, only the soft cap (+bonus) is applied
     * - Values in basis points (10000 = 100%)
     * 
     * @param value The current value to cap
     * @param statDef The stat definition with cap information
     * @param statValueLookup Function to lookup other stat values, can be null
     * @return The capped value
     */
    private static int applySoftHardCaps(int value, @Nonnull StatDefinition statDef, 
                                          @Nullable ToIntFunction<StatId> statValueLookup) {
        if (!statDef.hasCaps()) {
            return value;
        }
        
        int softCap = statDef.softCapBps();
        int hardCap = statDef.hardCapBps();
        StatId bonusStat = statDef.softCapBonusStat();
        
        // Calculate effective cap
        int effectiveCap;
        
        if (statDef.hasSoftCap()) {
            // Start with soft cap
            int adjustedSoftCap = softCap;
            
            // Add bonus stat value if available
            if (bonusStat != null && statValueLookup != null) {
                int bonusValue = statValueLookup.applyAsInt(bonusStat);
                adjustedSoftCap = softCap + bonusValue;
            }
            
            // Hard cap limits the adjusted soft cap
            if (statDef.hasHardCap()) {
                effectiveCap = Math.min(adjustedSoftCap, hardCap);
            } else {
                effectiveCap = adjustedSoftCap;
            }
        } else if (statDef.hasHardCap()) {
            // Only hard cap, no soft cap
            effectiveCap = hardCap;
        } else {
            // No caps (shouldn't reach here due to hasCaps() check)
            return value;
        }
        
        // Apply the effective cap (only cap from above, don't raise minimum)
        return Math.min(value, effectiveCap);
    }
    
    /**
     * Compute a stat value with breakdown information for UI display.
     * <p>
     * This overload does not apply soft/hard caps defined in the StatDefinition.
     * Use {@link #computeWithBreakdown(int, List, StatDefinition, ToIntFunction)} for cap support.
     */
    @Nonnull
    public static ComputeResult computeWithBreakdown(int baseValue, @Nonnull List<StatModifier> modifiers, @Nonnull StatDefinition statDef) {
        return computeWithBreakdown(baseValue, modifiers, statDef, null);
    }
    
    /**
     * Compute a stat value with breakdown information for UI display, including soft/hard cap support.
     * 
     * @param baseValue The base value before modifiers
     * @param modifiers List of modifiers to apply
     * @param statDef The stat definition
     * @param statValueLookup Function to lookup other stat values (for soft cap bonus stat), can be null
     * @return Detailed breakdown of the computation
     */
    @Nonnull
    public static ComputeResult computeWithBreakdown(int baseValue, @Nonnull List<StatModifier> modifiers, 
                                                      @Nonnull StatDefinition statDef,
                                                      @Nullable ToIntFunction<StatId> statValueLookup) {
        ComputeResult result = new ComputeResult();
        result.baseValue = baseValue;
        
        if (modifiers.isEmpty()) {
            int clamped = clamp(baseValue, statDef.minValue(), statDef.maxValue());
            result.afterCap = clamped;
            result.finalValue = applySoftHardCapsWithBreakdown(clamped, statDef, statValueLookup, result);
            return result;
        }
        
        // Sort modifiers
        List<StatModifier> sorted = new ArrayList<>(modifiers);
        sorted.sort(Comparator
            .comparingInt((StatModifier m) -> m.modifierType().getOrder())
            .thenComparingInt(StatModifier::priority)
            .thenComparing(StatModifier::sourceId)
        );
        
        // Sum FLAT
        long flatSum = 0;
        for (StatModifier mod : sorted) {
            if (mod.modifierType() == ModifierType.FLAT) {
                flatSum += mod.value();
                result.flatModifiers.add(mod);
            }
        }
        result.flatTotal = (int) flatSum;
        
        long current = (long) baseValue + flatSum;
        result.afterFlat = (int) current;
        
        // Sum INCREASED
        long increasedSum = 0;
        for (StatModifier mod : sorted) {
            if (mod.modifierType() == ModifierType.INCREASED) {
                increasedSum += mod.value();
                result.increasedModifiers.add(mod);
            }
        }
        result.increasedTotalBps = (int) increasedSum;
        
        if (increasedSum != 0) {
            current = (current * (BPS_100_PERCENT + increasedSum)) / BPS_100_PERCENT;
        }
        result.afterIncreased = (int) current;
        
        // Apply MORE
        for (StatModifier mod : sorted) {
            if (mod.modifierType() == ModifierType.MORE) {
                current = (current * (BPS_100_PERCENT + mod.value())) / BPS_100_PERCENT;
                result.moreModifiers.add(mod);
            }
        }
        result.afterMore = (int) current;
        
        // Apply CAP modifiers
        Integer minCap = null;
        Integer maxCap = null;
        for (StatModifier mod : sorted) {
            if (mod.modifierType() == ModifierType.CAP) {
                result.capModifiers.add(mod);
                if (mod.value() >= 0) {
                    if (maxCap == null || mod.value() < maxCap) {
                        maxCap = mod.value();
                    }
                } else {
                    int minVal = -mod.value();
                    if (minCap == null || minVal > minCap) {
                        minCap = minVal;
                    }
                }
            }
        }
        
        if (minCap != null && current < minCap) {
            current = minCap;
        }
        if (maxCap != null && current > maxCap) {
            current = maxCap;
        }
        result.afterCap = (int) current;
        
        // Apply soft/hard caps from StatDefinition
        int clamped = clamp(current, statDef.minValue(), statDef.maxValue());
        result.finalValue = applySoftHardCapsWithBreakdown(clamped, statDef, statValueLookup, result);
        return result;
    }
    
    /**
     * Apply soft/hard caps and record breakdown information.
     */
    private static int applySoftHardCapsWithBreakdown(int value, @Nonnull StatDefinition statDef,
                                                       @Nullable ToIntFunction<StatId> statValueLookup,
                                                       @Nonnull ComputeResult result) {
        if (!statDef.hasCaps()) {
            return value;
        }
        
        int softCap = statDef.softCapBps();
        int hardCap = statDef.hardCapBps();
        StatId bonusStat = statDef.softCapBonusStat();
        
        // Record cap info in result
        result.softCapBps = softCap;
        result.hardCapBps = hardCap;
        result.softCapBonusStat = bonusStat;
        
        // Calculate effective cap
        int effectiveCap;
        
        if (statDef.hasSoftCap()) {
            int adjustedSoftCap = softCap;
            
            if (bonusStat != null && statValueLookup != null) {
                int bonusValue = statValueLookup.applyAsInt(bonusStat);
                result.softCapBonusValue = bonusValue;
                adjustedSoftCap = softCap + bonusValue;
            }
            
            if (statDef.hasHardCap()) {
                effectiveCap = Math.min(adjustedSoftCap, hardCap);
            } else {
                effectiveCap = adjustedSoftCap;
            }
        } else if (statDef.hasHardCap()) {
            effectiveCap = hardCap;
        } else {
            return value;
        }
        
        result.effectiveCapBps = effectiveCap;
        result.wasCapped = value > effectiveCap;
        result.afterSoftHardCap = Math.min(value, effectiveCap);
        
        return result.afterSoftHardCap;
    }
    
    /**
     * Clamp a long value to int range and stat bounds.
     */
    private static int clamp(long value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) value;
    }
    
    /**
     * Result of stat computation with breakdown for UI.
     */
    public static class ComputeResult {
        /** The base value before any modifiers */
        public int baseValue;
        /** Sum of all FLAT modifiers */
        public int flatTotal;
        /** Value after applying FLAT modifiers */
        public int afterFlat;
        /** Sum of all INCREASED modifiers in basis points */
        public int increasedTotalBps;
        /** Value after applying INCREASED modifiers */
        public int afterIncreased;
        /** Value after applying MORE modifiers */
        public int afterMore;
        /** Value after applying CAP modifiers */
        public int afterCap;
        
        // Soft/Hard cap breakdown
        /** Soft cap from StatDefinition in basis points, or NO_CAP if not defined */
        public int softCapBps = StatDefinition.NO_CAP;
        /** Hard cap from StatDefinition in basis points, or NO_CAP if not defined */
        public int hardCapBps = StatDefinition.NO_CAP;
        /** The stat that provides bonus to soft cap, or null if none */
        @Nullable
        public StatId softCapBonusStat = null;
        /** The value of the soft cap bonus stat */
        public int softCapBonusValue = 0;
        /** The effective cap after adjusting soft cap with bonus stat */
        public int effectiveCapBps = StatDefinition.NO_CAP;
        /** Whether the value was reduced by soft/hard cap */
        public boolean wasCapped = false;
        /** Value after applying soft/hard caps */
        public int afterSoftHardCap;
        
        /** The final computed value */
        public int finalValue;
        
        public final List<StatModifier> flatModifiers = new ArrayList<>();
        public final List<StatModifier> increasedModifiers = new ArrayList<>();
        public final List<StatModifier> moreModifiers = new ArrayList<>();
        public final List<StatModifier> capModifiers = new ArrayList<>();
        
        /**
         * Get all modifiers in stacking order.
         */
        @Nonnull
        public List<StatModifier> getAllModifiers() {
            List<StatModifier> all = new ArrayList<>();
            all.addAll(flatModifiers);
            all.addAll(increasedModifiers);
            all.addAll(moreModifiers);
            all.addAll(capModifiers);
            return all;
        }
        
        /**
         * Check if this stat has soft/hard caps defined.
         */
        public boolean hasCaps() {
            return softCapBps != StatDefinition.NO_CAP || hardCapBps != StatDefinition.NO_CAP;
        }
    }
}
