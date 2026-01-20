package reign.software.hyforged.stats.engine;

import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.component.ModifierType;
import reign.software.hyforged.stats.component.StatModifier;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
     * 
     * @param baseValue The base value before modifiers
     * @param modifiers List of modifiers to apply (only those matching the stat)
     * @param statDef The stat definition (for bounds clamping)
     * @return The final computed value
     */
    public static int compute(int baseValue, @Nonnull List<StatModifier> modifiers, @Nonnull StatDefinition statDef) {
        if (modifiers.isEmpty()) {
            return clamp(baseValue, statDef.minValue(), statDef.maxValue());
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
        
        // Apply caps
        if (minCap != null && current < minCap) {
            current = minCap;
        }
        if (maxCap != null && current > maxCap) {
            current = maxCap;
        }
        
        // Final clamp to stat definition bounds and int range
        return clamp(current, statDef.minValue(), statDef.maxValue());
    }
    
    /**
     * Compute a stat value with breakdown information for UI display.
     */
    @Nonnull
    public static ComputeResult computeWithBreakdown(int baseValue, @Nonnull List<StatModifier> modifiers, @Nonnull StatDefinition statDef) {
        ComputeResult result = new ComputeResult();
        result.baseValue = baseValue;
        
        if (modifiers.isEmpty()) {
            result.finalValue = clamp(baseValue, statDef.minValue(), statDef.maxValue());
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
        
        // Apply CAP
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
        
        result.finalValue = clamp(current, statDef.minValue(), statDef.maxValue());
        return result;
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
        public int baseValue;
        public int flatTotal;
        public int afterFlat;
        public int increasedTotalBps;
        public int afterIncreased;
        public int afterMore;
        public int afterCap;
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
    }
}
