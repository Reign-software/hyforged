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
import com.hypixel.hytale.logger.HytaleLogger;

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

    // ========== REFLECTION ACCESS TO PARENT PRIVATE FIELDS ==========
    // EntityStatValue.min and .max are private. We need to update them
    // after ARPG stacking so that maximizeStatValue() and set() clamping
    // use the correct bounds.
    
    private static final java.lang.reflect.Field minField;
    private static final java.lang.reflect.Field maxField;
    private static final HytaleLogger LOGGER_INIT = HytaleLogger.forEnclosingClass();
    
    static {
        java.lang.reflect.Field tmpMin = null;
        java.lang.reflect.Field tmpMax = null;
        try {
            tmpMin = EntityStatValue.class.getDeclaredField("min");
            tmpMin.setAccessible(true);
            tmpMax = EntityStatValue.class.getDeclaredField("max");
            tmpMax.setAccessible(true);
        } catch (NoSuchFieldException e) {
            LOGGER_INIT.atSevere().withCause(e).log(
                "Failed to find EntityStatValue.min/max fields. "
                + "ARPG MAX/MIN modifier stacking will not function correctly. "
                + "This likely means a Hytale server update changed EntityStatValue internals.");
        }
        minField = tmpMin;
        maxField = tmpMax;
    }
    
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
     * Link to a StatDefinition by stat ID.
     * <p>
     * Uses the EntityStatType string ID at the given index to look up the
     * corresponding Hyforged stat definition. This avoids index-space collisions
     * between Hytale's EntityStatType indices and Hyforged's StatDefinitionRegistry
     * indices. For Hytale native stats (Health, Mana, etc.) that have no Hyforged
     * definition, statDefinition remains null.
     */
    private void linkStatDefinition(int statIndex) {
        EntityStatType asset = EntityStatType.getAssetMap().getAsset(statIndex);
        if (asset == null || asset.isUnknown()) {
            this.statDefinition = null;
            return;
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        this.statDefinition = registry.getStat(asset.getId());
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
     * <p>
     * All Hytale modifiers target either MIN or MAX. HyforgedModifiers
     * are excluded from super.computeModifiers() to control stacking order.
     * This method applies them using ARPG stacking (FLAT → INCREASED → MORE → CAP)
     * to the appropriate target (MAX or MIN), then reclamps the current value.
     */
    private void applyArpgStacking(@Nonnull EntityStatType asset) {
        Map<String, Modifier> modifiers = getModifiers();
        if (modifiers == null || modifiers.isEmpty()) {
            // Just apply base bonus to MAX and return
            if (hyforgedBaseBonus != 0) {
                applyBaseBonusToMax();
            }
            return;
        }
        
        // Collect HyforgedModifiers, grouped by target and StackType
        List<HyforgedModifier> maxFlatMods = new ArrayList<>();
        List<HyforgedModifier> maxIncreasedMods = new ArrayList<>();
        List<HyforgedModifier> maxMoreMods = new ArrayList<>();
        List<HyforgedModifier> maxCapMods = new ArrayList<>();
        List<HyforgedModifier> minFlatMods = new ArrayList<>();
        List<HyforgedModifier> minIncreasedMods = new ArrayList<>();
        List<HyforgedModifier> minMoreMods = new ArrayList<>();
        List<HyforgedModifier> minCapMods = new ArrayList<>();
        
        for (Modifier modifier : modifiers.values()) {
            if (modifier instanceof HyforgedModifier hm) {
                boolean isMin = hm.getTarget() == Modifier.ModifierTarget.MIN;
                switch (hm.getStackType()) {
                    case FLAT -> (isMin ? minFlatMods : maxFlatMods).add(hm);
                    case INCREASED -> (isMin ? minIncreasedMods : maxIncreasedMods).add(hm);
                    case MORE -> (isMin ? minMoreMods : maxMoreMods).add(hm);
                    case CAP -> (isMin ? minCapMods : maxCapMods).add(hm);
                }
            }
        }
        
        boolean hasMaxMods = !maxFlatMods.isEmpty() || !maxIncreasedMods.isEmpty()
                || !maxMoreMods.isEmpty() || !maxCapMods.isEmpty();
        boolean hasMinMods = !minFlatMods.isEmpty() || !minIncreasedMods.isEmpty()
                || !minMoreMods.isEmpty() || !minCapMods.isEmpty();
        
        // If no HyforgedModifiers, just apply base bonus to MAX
        if (!hasMaxMods && !hasMinMods) {
            if (hyforgedBaseBonus != 0) {
                applyBaseBonusToMax();
            }
            return;
        }
        
        // ---- Apply ARPG stacking to MAX ----
        if (hasMaxMods || hyforgedBaseBonus != 0) {
            long maxValue = (long) Math.round(getMax()) + hyforgedBaseBonus;
            maxValue = applyArpgStackingPipeline(maxValue,
                    maxFlatMods, maxIncreasedMods, maxMoreMods, maxCapMods);
            
            // Apply soft/hard caps from StatDefinition if defined
            if (statDefinition != null && statDefinition.hasCaps()) {
                maxValue = applySoftHardCaps((int) maxValue);
            }
            
            writeMax((float) maxValue);
        }
        
        // ---- Apply ARPG stacking to MIN ----
        if (hasMinMods) {
            long minValue = (long) Math.round(getMin());
            minValue = applyArpgStackingPipeline(minValue,
                    minFlatMods, minIncreasedMods, minMoreMods, minCapMods);
            writeMin((float) minValue);
        }
        
        // Reclamp current value to new [min, max] bounds
        set(get());
    }
    
    /**
     * Apply ARPG stacking pipeline: FLAT → INCREASED → MORE → CAP.
     * 
     * @param base The starting value (current max or min)
     * @param flatMods FLAT modifiers to sum
     * @param increasedMods INCREASED modifiers to sum and apply as multiplier
     * @param moreMods MORE modifiers to apply sequentially
     * @param capMods CAP modifiers to clamp
     * @return The stacked result
     */
    private long applyArpgStackingPipeline(
            long base,
            @Nonnull List<HyforgedModifier> flatMods,
            @Nonnull List<HyforgedModifier> increasedMods,
            @Nonnull List<HyforgedModifier> moreMods,
            @Nonnull List<HyforgedModifier> capMods
    ) {
        long current = base;
        
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
            current = (current * (BPS_100_PERCENT + increasedSum)) / BPS_100_PERCENT;
        }
        
        // Step 3: Apply each MORE modifier sequentially (multiplicative)
        for (HyforgedModifier mod : moreMods) {
            current = (current * (BPS_100_PERCENT + mod.getAmount())) / BPS_100_PERCENT;
        }
        
        // Step 4: Apply CAP modifiers (min/max clamps from modifiers)
        Integer minCap = null;
        Integer maxCap = null;
        for (HyforgedModifier mod : capMods) {
            int capValue = mod.getAmount();
            if (capValue >= 0) {
                if (maxCap == null || capValue < maxCap) {
                    maxCap = capValue;
                }
            } else {
                int minVal = -capValue;
                if (minCap == null || minVal > minCap) {
                    minCap = minVal;
                }
            }
        }
        if (minCap != null && current < minCap) {
            current = minCap;
        }
        if (maxCap != null && current > maxCap) {
            current = maxCap;
        }
        
        return current;
    }
    
    /**
     * Apply the hyforgedBaseBonus to the MAX value.
     * Used when there are no HyforgedModifiers but a base bonus exists.
     */
    private void applyBaseBonusToMax() {
        float newMax = getMax() + hyforgedBaseBonus;
        writeMax(newMax);
        // Reclamp current value to new bounds
        set(get());
    }
    
    /**
     * Write to the parent's private max field via reflection.
     */
    private void writeMax(float value) {
        if (maxField != null) {
            try {
                maxField.setFloat(this, value);
            } catch (IllegalAccessException e) {
                // Should not happen since we called setAccessible
            }
        }
    }
    
    /**
     * Write to the parent's private min field via reflection.
     */
    private void writeMin(float value) {
        if (minField != null) {
            try {
                minField.setFloat(this, value);
            } catch (IllegalAccessException e) {
                // Should not happen since we called setAccessible
            }
        }
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
