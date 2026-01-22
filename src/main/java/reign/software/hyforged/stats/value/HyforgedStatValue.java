package reign.software.hyforged.stats.value;

import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Hyforged's extension of Hytale's EntityStatValue that implements ARPG-style modifier stacking.
 * <p>
 * This class extends EntityStatValue and overrides {@code computeModifiers()} to implement
 * the ARPG stacking order: FLAT → INCREASED → MORE → CAP.
 * <p>
 * Key features:
 * <ul>
 *   <li>ARPG stacking semantics for HyforgedModifier instances</li>
 *   <li>Backward compatibility with Hytale's StaticModifier</li>
 *   <li>Optional base bonus from Hyforged systems (ability scores, etc.)</li>
 *   <li>Change listeners for reactive systems</li>
 *   <li>Link to StatDefinition for soft/hard cap support</li>
 * </ul>
 * <p>
 * This class adds NO persistent fields - all extensions are transient and recomputed
 * after deserialization via the installer system.
 * <p>
 * Basis points: 10000 = 100%
 * 
 * @see HyforgedStatValueInstaller for the system that swaps EntityStatValue → HyforgedStatValue
 * @see HyforgedModifier for ARPG-style modifiers
 */
public class HyforgedStatValue extends EntityStatValue {
    
    /** Basis points representing 100% (10000 bps = 100%) */
    public static final int BPS_100_PERCENT = 10000;
    
    // ========== TRANSIENT FIELDS (not persisted) ==========
    
    /**
     * Additional base value bonus from Hyforged systems (ability scores, class bonuses, etc.).
     * Applied before modifier stacking.
     */
    private transient int hyforgedBaseBonus = 0;
    
    /**
     * Reference to the StatDefinition for this stat.
     * Used for soft/hard cap support and other Hyforged-specific features.
     */
    @Nullable
    private transient StatDefinition statDefinition = null;
    
    /**
     * Listeners notified when the stat value changes.
     * Uses CopyOnWriteArrayList for thread-safe iteration during modification.
     */
    private transient final List<Consumer<HyforgedStatValue>> changeListeners = new CopyOnWriteArrayList<>();
    
    /**
     * Cached previous value for change detection.
     */
    private transient float previousValue = 0;
    
    // ========== CONSTRUCTORS ==========
    
    /**
     * Default constructor for codec deserialization.
     */
    protected HyforgedStatValue() {
        super();
    }
    
    /**
     * Constructor matching EntityStatValue's initialization pattern.
     * 
     * @param index The stat index in EntityStatMap
     * @param asset The EntityStatType asset defining this stat
     */
    public HyforgedStatValue(int index, @Nonnull EntityStatType asset) {
        super(index, asset);
        linkStatDefinition(index);
    }
    
    /**
     * Copy constructor for creating HyforgedStatValue from an existing EntityStatValue.
     * Preserves all modifiers and current value.
     * 
     * @param original The EntityStatValue to copy from
     */
    public HyforgedStatValue(@Nonnull EntityStatValue original) {
        super();
        copyFrom(original);
    }
    
    // ========== HYFORGED BASE BONUS ==========
    
    /**
     * Get the Hyforged base bonus applied to this stat.
     * 
     * @return The base bonus value
     */
    public int getHyforgedBaseBonus() {
        return hyforgedBaseBonus;
    }
    
    /**
     * Set the Hyforged base bonus for this stat.
     * Triggers recomputation if the value changes.
     * 
     * @param bonus The base bonus to set
     */
    public void setHyforgedBaseBonus(int bonus) {
        if (this.hyforgedBaseBonus != bonus) {
            this.hyforgedBaseBonus = bonus;
            recompute();
        }
    }
    
    /**
     * Add to the Hyforged base bonus.
     * Triggers recomputation.
     * 
     * @param delta The amount to add (can be negative)
     */
    public void addBaseBonus(int delta) {
        if (delta != 0) {
            this.hyforgedBaseBonus += delta;
            recompute();
        }
    }
    
    // ========== STAT DEFINITION LINK ==========
    
    /**
     * Get the linked StatDefinition, if available.
     * 
     * @return The StatDefinition, or null if not found in registry
     */
    @Nullable
    public StatDefinition getStatDefinition() {
        return statDefinition;
    }
    
    /**
     * Link to a StatDefinition by stat index.
     * Called during initialization.
     */
    private void linkStatDefinition(int statIndex) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        this.statDefinition = registry.getStat(statIndex);
    }
    
    // ========== CHANGE LISTENERS ==========
    
    /**
     * Add a listener to be notified when the stat value changes.
     * 
     * @param listener The listener to add
     */
    public void addChangeListener(@Nonnull Consumer<HyforgedStatValue> listener) {
        changeListeners.add(listener);
    }
    
    /**
     * Remove a change listener.
     * 
     * @param listener The listener to remove
     */
    public void removeChangeListener(@Nonnull Consumer<HyforgedStatValue> listener) {
        changeListeners.remove(listener);
    }
    
    /**
     * Clear all change listeners.
     */
    public void clearChangeListeners() {
        changeListeners.clear();
    }
    
    /**
     * Notify listeners of a value change.
     */
    private void notifyListeners() {
        for (Consumer<HyforgedStatValue> listener : changeListeners) {
            listener.accept(this);
        }
    }
    
    // ========== MODIFIER COMPUTATION (ARPG STACKING) ==========
    
    /**
     * Compute modifiers using ARPG stacking order.
     * <p>
     * Stacking order:
     * <ol>
     *   <li>Start with base value from asset + hyforgedBaseBonus</li>
     *   <li>Sum all FLAT modifiers</li>
     *   <li>Sum all INCREASED modifiers, apply as (1 + sum/10000)</li>
     *   <li>Apply each MORE modifier sequentially as (1 + amount/10000)</li>
     *   <li>Apply CAP modifiers (min/max bounds)</li>
     *   <li>Apply StaticModifiers via parent class for backward compatibility</li>
     *   <li>Apply soft/hard caps from StatDefinition if defined</li>
     *   <li>Clamp to min/max bounds</li>
     * </ol>
     * 
     * @param asset The EntityStatType asset for this stat
     */
    @Override
    protected void computeModifiers(@Nonnull EntityStatType asset) {
        // Cache previous value for change detection
        float oldValue = this.get();

        // Let parent handle min/max initialization and StaticModifier processing
        // but exclude HyforgedModifiers from min/max application
        Map<String, Modifier> modifiers = getModifiers();
        Map<String, Modifier> hyforgedMods = null;
        if (modifiers != null && !modifiers.isEmpty()) {
            for (Map.Entry<String, Modifier> entry : modifiers.entrySet()) {
                if (entry.getValue() instanceof HyforgedModifier) {
                    if (hyforgedMods == null) {
                        hyforgedMods = new HashMap<>();
                    }
                    hyforgedMods.put(entry.getKey(), entry.getValue());
                }
            }
            if (hyforgedMods != null) {
                for (String key : hyforgedMods.keySet()) {
                    modifiers.remove(key);
                }
            }
        }

        super.computeModifiers(asset);

        if (hyforgedMods != null) {
            modifiers.putAll(hyforgedMods);
        }

        // Now apply ARPG stacking for HyforgedModifiers
        applyArpgStacking(asset);
        
        // Check for value change and notify listeners
        float newValue = this.get();
        if (newValue != oldValue) {
            previousValue = oldValue;
            notifyListeners();
        }
    }
    
    /**
     * Apply ARPG stacking logic for HyforgedModifier instances.
     */
    private void applyArpgStacking(@Nonnull EntityStatType asset) {
        Map<String, Modifier> modifiers = getModifiers();
        if (modifiers == null || modifiers.isEmpty()) {
            // Just apply base bonus and return
            if (hyforgedBaseBonus != 0) {
                float base = statDefinition != null ? asset.getInitialValue() : get();
                float adjusted = base + hyforgedBaseBonus;
                adjusted = clamp(adjusted, getMin(), getMax());
                set(adjusted);
            }
            return;
        }
        
        // Collect HyforgedModifiers, grouped by StackType
        List<HyforgedModifier> flatMods = new ArrayList<>();
        List<HyforgedModifier> increasedMods = new ArrayList<>();
        List<HyforgedModifier> moreMods = new ArrayList<>();
        List<HyforgedModifier> capMods = new ArrayList<>();
        
        for (Modifier modifier : modifiers.values()) {
            if (modifier instanceof HyforgedModifier hm) {
                switch (hm.getStackType()) {
                    case FLAT -> flatMods.add(hm);
                    case INCREASED -> increasedMods.add(hm);
                    case MORE -> moreMods.add(hm);
                    case CAP -> capMods.add(hm);
                }
            }
        }
        
        // If no HyforgedModifiers, just apply base bonus
        if (flatMods.isEmpty() && increasedMods.isEmpty() && moreMods.isEmpty() && capMods.isEmpty()) {
            if (hyforgedBaseBonus != 0) {
                float base = statDefinition != null ? asset.getInitialValue() : get();
                float adjusted = base + hyforgedBaseBonus;
                adjusted = clamp(adjusted, getMin(), getMax());
                set(adjusted);
            }
            return;
        }
        
        // Start with base value + base bonus
        // For Hyforged-defined stats, prefer asset initial to avoid stacking drift
        long current;
        if (statDefinition != null) {
            current = Math.round(asset.getInitialValue()) + (long) hyforgedBaseBonus;
        } else {
            current = (long) get() + hyforgedBaseBonus;
        }
        
        // Step 1: Sum all FLAT modifiers
        long flatSum = 0;
        for (HyforgedModifier mod : flatMods) {
            flatSum += mod.getAmount();
        }
        current += flatSum;
        
        // Step 2: Sum all INCREASED modifiers and apply as multiplier
        long increasedSum = 0;
        for (HyforgedModifier mod : increasedMods) {
            increasedSum += mod.getAmount();
        }
        if (increasedSum != 0) {
            // current * (1 + increasedSum/10000) = current * (10000 + increasedSum) / 10000
            current = (current * (BPS_100_PERCENT + increasedSum)) / BPS_100_PERCENT;
        }
        
        // Step 3: Apply each MORE modifier sequentially (multiplicative)
        for (HyforgedModifier mod : moreMods) {
            // current * (1 + value/10000) = current * (10000 + value) / 10000
            current = (current * (BPS_100_PERCENT + mod.getAmount())) / BPS_100_PERCENT;
        }
        
        // Step 4: Apply CAP modifiers (min/max clamps from modifiers)
        Integer minCap = null;
        Integer maxCap = null;
        for (HyforgedModifier mod : capMods) {
            int capValue = mod.getAmount();
            if (capValue >= 0) {
                // Max cap - take the lowest max cap
                if (maxCap == null || capValue < maxCap) {
                    maxCap = capValue;
                }
            } else {
                // Min cap (negative value represents min) - take the highest min cap
                int minVal = -capValue;
                if (minCap == null || minVal > minCap) {
                    minCap = minVal;
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
        
        // Step 5: Apply soft/hard caps from StatDefinition if defined
        if (statDefinition != null && statDefinition.hasCaps()) {
            current = applySoftHardCaps((int) current);
        }
        
        // Step 6: Final clamp to asset min/max
        float finalValue = clamp(current, getMin(), getMax());
        set(finalValue);
    }
    
    /**
     * Apply soft/hard caps from the linked StatDefinition.
     */
    private long applySoftHardCaps(int value) {
        if (statDefinition == null || !statDefinition.hasCaps()) {
            return value;
        }
        
        int softCap = statDefinition.softCapBps();
        int hardCap = statDefinition.hardCapBps();
        
        // Calculate effective cap
        int effectiveCap;
        
        if (statDefinition.hasSoftCap()) {
            // For soft caps, the value beyond soft cap is reduced, not eliminated
            // For now, we use soft cap as a simpler cap (full cap support requires curve logic)
            if (statDefinition.hasHardCap()) {
                effectiveCap = Math.min(softCap, hardCap);
            } else {
                effectiveCap = softCap;
            }
        } else if (statDefinition.hasHardCap()) {
            effectiveCap = hardCap;
        } else {
            return value;
        }
        
        return Math.min(value, effectiveCap);
    }
    
    /**
     * Clamp a long value to min/max bounds.
     */
    private static float clamp(long value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return (float) value;
    }

    /**
     * Clamp a float value to min/max bounds.
     */
    private static float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
    
    // ========== UTILITY METHODS ==========
    
    /**
     * Force recomputation of modifiers.
     * Called when base bonus or other transient state changes.
     */
    public void recompute() {
        EntityStatType asset = EntityStatType.getAssetMap().getAsset(getIndex());
        if (asset != null) {
            computeModifiers(asset);
        }
    }
    
    /**
     * Copy state from an existing EntityStatValue.
     * Used during swap from EntityStatValue to HyforgedStatValue.
     * 
     * @param original The EntityStatValue to copy from
     */
    private void copyFrom(@Nonnull EntityStatValue original) {
        // Use synchronizeAsset to copy id, index, min, max and trigger computation
        EntityStatType asset = EntityStatType.getAssetMap().getAsset(original.getIndex());
        if (asset != null) {
            synchronizeAsset(original.getIndex(), asset);
        }

        // Preserve current value (important for resource stats)
        set(original.get());
        
        // Copy existing modifiers
        Map<String, Modifier> originalModifiers = original.getModifiers();
        if (originalModifiers != null && !originalModifiers.isEmpty()) {
            for (Map.Entry<String, Modifier> entry : originalModifiers.entrySet()) {
                putModifier(entry.getKey(), entry.getValue());
            }
        }
        
        // Link to StatDefinition
        linkStatDefinition(original.getIndex());
        
        // Recompute with our ARPG stacking
        if (asset != null) {
            computeModifiers(asset);
        }
    }
    
    /**
     * Get the previous value before the last change.
     * Useful for calculating deltas in UI/events.
     * 
     * @return The previous value
     */
    public float getPreviousValue() {
        return previousValue;
    }
    
    /**
     * Get the change delta from the last computation.
     * 
     * @return The difference between current and previous value
     */
    public float getChangeDelta() {
        return get() - previousValue;
    }
    
    @Nonnull
    @Override
    public String toString() {
        return "HyforgedStatValue{" +
            "id='" + getId() + "'" +
            ", index=" + getIndex() +
            ", value=" + get() +
            ", min=" + getMin() +
            ", max=" + getMax() +
            ", baseBonus=" + hyforgedBaseBonus +
            ", statDef=" + (statDefinition != null ? statDefinition.id() : "null") +
            ", modifiers=" + (getModifiers() != null ? getModifiers().size() : 0) +
            "}";
    }
}
