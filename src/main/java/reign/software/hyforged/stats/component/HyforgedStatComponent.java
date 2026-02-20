package reign.software.hyforged.stats.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import reign.software.hyforged.stats.DisplayFormat;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * ECS Component holding Hyforged stat data for an entity.
 * <p>
 * This is PURE DATA - no behavior, following ECS principles.
 * All computation is done by Systems that process this component.
 * <p>
 * Data stored:
 * - Ability score base values (player-allocated)
 * - Modifier list (equipment, buffs, passives, etc.)
 * - Cached computed values per stat
 * - Dirty flags for cache invalidation
 */
public class HyforgedStatComponent implements Component<EntityStore> {
    
    /** Schema version for persistence migration */
    public static final int SCHEMA_VERSION = 2;
    
    /** Maximum number of modifiers per entity (guard against explosion) */
    public static final int MAX_MODIFIERS = 256;
    
    // ========== BASE VALUES ==========
    // Base values for stats without scaling (e.g., ability scores, manually-set bases)
    // Keyed by stat index; stats not in this map use their definition's defaultValue
    
    private final Int2IntMap baseValues = new Int2IntOpenHashMap();
    
    // ========== MODIFIERS ==========
    // List of all active modifiers on this entity
    
    private final List<HyforgedModifier> modifiers = new ArrayList<>();
    
    // Conditional modifiers that require context evaluation
    private final List<ConditionalStatModifier> conditionalModifiers = new ArrayList<>();
    
    // ========== CACHED COMPUTED VALUES ==========
    // Computed stat values indexed by stat registry index
    // These are recomputed when dirty flags are set
    
    private int[] cachedValues;
    
    // ========== DIRTY FLAGS ==========
    // BitSet tracking which stats need recomputation
    
    private final BitSet dirtyFlags = new BitSet();
    private boolean allDirty = true; // Initially all stats need computation
    
    // ========== BRIDGE STATE ==========
    // Track last values sent to Hytale bridge for delta updates
    
    private int lastBridgedMaxHealth = 0;
    private int lastBridgedMaxMana = 0;
    private int lastBridgedMaxStamina = 0;
    private int lastBridgedMaxConcentration = 0;
    private int lastBridgedMaxRage = 0;
    private int lastBridgedMovementSpeedBps = 0;

    // ========== HUD STATE ==========
    // Track last resource HUD values to avoid redundant UI updates

    private boolean lastHudShown = false;
    private boolean lastHudConcentrationVisible = false;
    private boolean lastHudRageVisible = false;
    private int lastHudConcentrationCurrent = 0;
    private int lastHudConcentrationMax = 0;
    private int lastHudRageCurrent = 0;
    private int lastHudRageMax = 0;
    private int lastHudBreakpointHash = 0;
    private float lastHudRegenRate = Float.NaN;
    
    // ========== EVENT COALESCING BUFFER ==========
    // Collects stat changes during a tick for batch event emission
    // Maps stat index → old value (before recomputation)
    
    private final Int2IntMap changeBuffer = new Int2IntOpenHashMap();
    private boolean isBufferingChanges = false;
    
    public HyforgedStatComponent() {
        // Initialize cache based on registry size
        int statCount = StatDefinitionRegistry.get().getStatCount();
        cachedValues = new int[Math.max(statCount, 64)]; // Minimum size for safety
    }
    
    // ========== BASE VALUE ACCESSORS ==========
    
    /**
     * Get the base value for a stat.
     * <p>
     * For stats without scaling, this returns the stored base value (e.g., allocated
     * ability score points). If no base value is set, returns the stat's defaultValue.
     * <p>
     * For stats with scaling, the base value is computed from source stats by the
     * ScalingEngine - this method returns 0 for such stats (they don't have stored bases).
     *
     * @param statIndex The stat index
     * @return The base value, or the stat's defaultValue if not set
     */
    public int getBaseValue(int statIndex) {
        if (baseValues.containsKey(statIndex)) {
            return baseValues.get(statIndex);
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        StatDefinition statDef = registry.getStat(statIndex);
        if (statDef != null) {
            return statDef.defaultValue();
        }
        return 0;
    }
    
    /**
     * Set the base value for a stat.
     * <p>
     * This is used for stats without scaling (e.g., ability scores, manually-set bases).
     * Setting a base value marks the stat and its dependents as dirty.
     *
     * @param statIndex The stat index
     * @param value The base value to set
     */
    public void setBaseValue(int statIndex, int value) {
        baseValues.put(statIndex, value);
        markStatDirty(statIndex);
        // Also mark any stats that depend on this stat
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        for (int dependent : registry.getDependentStats(statIndex)) {
            markStatDirty(dependent);
        }
    }
    
    /**
     * Check if a base value is explicitly set for a stat.
     *
     * @param statIndex The stat index
     * @return true if a base value is set (not using defaultValue)
     */
    public boolean hasBaseValue(int statIndex) {
        return baseValues.containsKey(statIndex);
    }
    
    /**
     * Remove the explicit base value for a stat, reverting to defaultValue.
     *
     * @param statIndex The stat index
     */
    public void removeBaseValue(int statIndex) {
        if (baseValues.containsKey(statIndex)) {
            baseValues.remove(statIndex);
            markStatDirty(statIndex);
            StatDefinitionRegistry registry = StatDefinitionRegistry.get();
            for (int dependent : registry.getDependentStats(statIndex)) {
                markStatDirty(dependent);
            }
        }
    }
    
    // ========== BASE VALUE PERSISTENCE HELPERS ==========
    // Temporary storage for codec deserialization
    
    private transient int[] tempLoadIndices = null;
    
    /**
     * Get all stat indices with explicit base values (for persistence).
     */
    @Nonnull
    public int[] getBaseValueIndices() {
        return baseValues.keySet().toIntArray();
    }
    
    /**
     * Get all base values in the same order as getBaseValueIndices() (for persistence).
     */
    @Nonnull
    public int[] getBaseValueValues() {
        int[] indices = getBaseValueIndices();
        int[] values = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            values[i] = baseValues.get(indices[i]);
        }
        return values;
    }
    
    /**
     * Set temporary indices for codec load (internal use).
     */
    public void setTempLoadIndices(int[] indices) {
        this.tempLoadIndices = indices;
    }
    
    /**
     * Get temporary indices for codec load (internal use).
     */
    @Nullable
    public int[] getTempLoadIndices() {
        return tempLoadIndices;
    }
    
    /**
     * Clear temporary indices after codec load (internal use).
     */
    public void clearTempLoadIndices() {
        this.tempLoadIndices = null;
    }
    
    // ========== MODIFIER ACCESSORS ==========
    
    /**
     * Get all modifiers (unmodifiable view).
     */
    @Nonnull
    public List<HyforgedModifier> getModifiers() {
        return List.copyOf(modifiers);
    }
    
    /**
     * Add a modifier to this entity.
     * If a modifier with the same source/target/type already exists, it is replaced.
     * @return true if added or replaced, false if at max capacity
     */
    public boolean addModifier(@Nonnull HyforgedModifier modifier) {
        return upsertModifier(modifier);
    }

    /**
     * Add or replace a modifier using a stable key (source + target + type).
     * This prevents duplicate stacking when a source is reapplied.
     *
     * @return true if added or replaced, false if at max capacity
     */
    public boolean upsertModifier(@Nonnull HyforgedModifier modifier) {
        int existingIndex = findMatchingModifierIndex(modifier);
        if (existingIndex >= 0) {
            HyforgedModifier existing = modifiers.set(existingIndex, modifier);
            markAffectedStatsDirty(existing);
            markAffectedStatsDirty(modifier);
            return true;
        }

        if (modifiers.size() >= MAX_MODIFIERS) {
            return false;
        }
        modifiers.add(modifier);
        markAffectedStatsDirty(modifier);
        return true;
    }

    private int findMatchingModifierIndex(@Nonnull HyforgedModifier modifier) {
        for (int i = 0; i < modifiers.size(); i++) {
            HyforgedModifier existing = modifiers.get(i);
            if (matchesModifierKey(existing, modifier)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean matchesModifierKey(@Nonnull HyforgedModifier existing, @Nonnull HyforgedModifier incoming) {
        return existing.getSourceId().equals(incoming.getSourceId())
            && existing.getSourceType() == incoming.getSourceType()
            && existing.getStackType() == incoming.getStackType()
            && existing.getTargetStatIndex() == incoming.getTargetStatIndex()
            && existing.getTargetTagIndex() == incoming.getTargetTagIndex();
    }
    
    /**
     * Remove a modifier by source ID.
     * @return true if any modifiers were removed
     */
    public boolean removeModifiersBySource(@Nonnull String sourceId) {
        // Collect affected stats before removal
        for (HyforgedModifier m : modifiers) {
            if (m.getSourceId().equals(sourceId)) {
                markAffectedStatsDirty(m);
            }
        }
        return modifiers.removeIf(m -> m.getSourceId().equals(sourceId));
    }
    
    /**
     * Remove all modifiers with the given source type.
     */
    public boolean removeModifiersBySourceType(@Nonnull HyforgedModifier.SourceType sourceType) {
        // Collect affected stats before removal
        for (HyforgedModifier m : modifiers) {
            if (m.getSourceType() == sourceType) {
                markAffectedStatsDirty(m);
            }
        }
        return modifiers.removeIf(m -> m.getSourceType() == sourceType);
    }

    /**
     * Remove modifiers matching a predicate.
     * <p>
     * This is a generic helper for systems to perform data cleanup while
     * providing their own removal criteria and dirty-flag handling.
     *
     * @param predicate Matcher for modifiers to remove
     * @param onRemoved Callback for each removed modifier
     * @return number of modifiers removed
     */
    public int removeModifiersIf(
            @Nonnull Predicate<HyforgedModifier> predicate,
            @Nonnull Consumer<HyforgedModifier> onRemoved
    ) {
        int removed = 0;
        for (java.util.Iterator<HyforgedModifier> it = modifiers.iterator(); it.hasNext(); ) {
            HyforgedModifier modifier = it.next();
            if (predicate.test(modifier)) {
                it.remove();
                removed++;
                onRemoved.accept(modifier);
            }
        }
        return removed;
    }
    
    /**
     * Clear all modifiers.
     */
    public void clearModifiers() {
        modifiers.clear();
        markAllDirty();
    }
    
    /**
     * Get modifier count.
     */
    public int getModifierCount() {
        return modifiers.size();
    }
    
    /**
     * Get conditional modifier count.
     */
    public int getConditionalModifierCount() {
        return conditionalModifiers.size();
    }

    // ========== CONDITIONAL MODIFIER ACCESSORS ==========

    /**
     * Get all conditional modifiers (unmodifiable view).
     */
    @Nonnull
    public List<ConditionalStatModifier> getConditionalModifiers() {
        return List.copyOf(conditionalModifiers);
    }

    /**
     * Add a conditional modifier to this entity.
     * If a modifier with the same source/target/type already exists, it is replaced.
     *
     * @param conditionalMod The conditional modifier to add
     * @return true if added or replaced, false if at max capacity
     */
    public boolean addConditionalModifier(@Nonnull ConditionalStatModifier conditionalMod) {
        // Check if we have room (conditional modifiers share the cap)
        if (modifiers.size() + conditionalModifiers.size() >= MAX_MODIFIERS) {
            return false;
        }

        // Find and replace if exists
        int existingIndex = findMatchingConditionalModifierIndex(conditionalMod);
        if (existingIndex >= 0) {
            ConditionalStatModifier existing = conditionalModifiers.set(existingIndex, conditionalMod);
            markAffectedStatsDirty(existing.modifier());
            markAffectedStatsDirty(conditionalMod.modifier());
            return true;
        }

        conditionalModifiers.add(conditionalMod);
        markAffectedStatsDirty(conditionalMod.modifier());
        return true;
    }

    private int findMatchingConditionalModifierIndex(@Nonnull ConditionalStatModifier conditionalMod) {
        for (int i = 0; i < conditionalModifiers.size(); i++) {
            ConditionalStatModifier existing = conditionalModifiers.get(i);
            if (matchesModifierKey(existing.modifier(), conditionalMod.modifier())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Remove conditional modifiers by source ID.
     *
     * @param sourceId The source ID to match
     * @return true if any modifiers were removed
     */
    public boolean removeConditionalModifiersBySource(@Nonnull String sourceId) {
        boolean removed = false;
        for (ConditionalStatModifier m : conditionalModifiers) {
            if (m.sourceId().equals(sourceId)) {
                markAffectedStatsDirty(m.modifier());
                removed = true;
            }
        }
        conditionalModifiers.removeIf(m -> m.sourceId().equals(sourceId));
        return removed;
    }

    /**
     * Mark all stats affected by a modifier as dirty.
     * Handles both direct stat targeting and tag targeting.
     */
    private void markAffectedStatsDirty(HyforgedModifier modifier) {
        if (modifier.getTargetStatIndex() >= 0) {
            markStatDirty(modifier.getTargetStatIndex());
        }
        int tagIndex = modifier.getTargetTagIndex();
        if (tagIndex != HyforgedModifier.NO_TAG) {
            for (int statIdx : StatDefinitionRegistry.get().getStatIndicesForTagIndex(tagIndex)) {
                markStatDirty(statIdx);
            }
        }
    }

    // ========== CACHED VALUE ACCESSORS ==========

    /**
     * Get cached computed value for a stat.
     * Note: May be stale if stat is dirty - systems should check dirty flags.
     */
    public int getCachedValue(int statIndex) {
        if (statIndex < 0 || statIndex >= cachedValues.length) {
            return 0;
        }
        return cachedValues[statIndex];
    }

    /**
     * Set cached computed value for a stat.
     * Called by computation systems after recomputing.
     */
    public void setCachedValue(int statIndex, int value) {
        ensureCacheCapacity(statIndex + 1);
        cachedValues[statIndex] = value;
    }

    private void ensureCacheCapacity(int minCapacity) {
        if (cachedValues.length < minCapacity) {
            int[] newCache = new int[Math.max(minCapacity, cachedValues.length * 2)];
            System.arraycopy(cachedValues, 0, newCache, 0, cachedValues.length);
            cachedValues = newCache;
        }
    }

    // ========== DIRTY FLAG ACCESSORS ==========

    /**
     * Check if a specific stat needs recomputation.
     */
    public boolean isStatDirty(int statIndex) {
        return allDirty || dirtyFlags.get(statIndex);
    }

    /**
     * Check if any stat is dirty.
     */
    public boolean hasAnyDirty() {
        return allDirty || !dirtyFlags.isEmpty();
    }

    /**
     * Mark a specific stat as needing recomputation.
     */
    public void markStatDirty(int statIndex) {
        if (statIndex >= 0) {
            dirtyFlags.set(statIndex);
        }
    }

    /**
     * Mark all stats as needing recomputation.
     */
    public void markAllDirty() {
        allDirty = true;
    }

    /**
     * Clear dirty flag for a specific stat.
     */
    public void clearDirtyFlag(int statIndex) {
        dirtyFlags.clear(statIndex);
    }

    /**
     * Clear all dirty flags.
     */
    public void clearAllDirtyFlags() {
        allDirty = false;
        dirtyFlags.clear();
    }

    /**
     * Get indices of all dirty stats.
     */
    @Nonnull
    public int[] getDirtyStatIndices() {
        if (allDirty) {
            int count = StatDefinitionRegistry.get().getStatCount();
            int[] all = new int[count];
            for (int i = 0; i < count; i++) {
                all[i] = i;
            }
            return all;
        }
        return dirtyFlags.stream().toArray();
    }

    // ========== CHANGE BUFFER ACCESSORS ==========
    // Used for coalescing stat changes during a tick for batch event emission

    /**
     * Begin buffering stat changes. Call before recomputation to capture old values.
     * <p>
     * Must be paired with {@link #endBufferingChanges()} or {@link #clearChangeBuffer()}.
     */
    public void beginBufferingChanges() {
        isBufferingChanges = true;
        changeBuffer.clear();
    }

    /**
     * Record the old value for a stat before recomputation.
     * Only records if buffering is active.
     *
     * @param statIndex The stat index
     * @param oldValue The value before recomputation
     */
    public void recordOldValue(int statIndex, int oldValue) {
        if (isBufferingChanges && !changeBuffer.containsKey(statIndex)) {
            changeBuffer.put(statIndex, oldValue);
        }
    }

    /**
     * End buffering and return changes as a map of statIndex → oldValue.
     * The returned map should be compared against current cached values
     * to determine which stats actually changed.
     *
     * @return Map of stat index to old value for stats that were recomputed
     */
    @Nonnull
    public Int2IntMap endBufferingChanges() {
        isBufferingChanges = false;
        Int2IntMap result = new Int2IntOpenHashMap(changeBuffer);
        changeBuffer.clear();
        return result;
    }

    /**
     * Check if change buffering is currently active.
     *
     * @return true if buffering
     */
    public boolean isBufferingChanges() {
        return isBufferingChanges;
    }

    /**
     * Clear the change buffer without returning changes.
     */
    public void clearChangeBuffer() {
        isBufferingChanges = false;
        changeBuffer.clear();
    }

    // ========== BRIDGE STATE ACCESSORS ==========

    public int getLastBridgedMaxHealth() {
        return lastBridgedMaxHealth;
    }

    public void setLastBridgedMaxHealth(int value) {
        lastBridgedMaxHealth = value;
    }

    public int getLastBridgedMaxMana() {
        return lastBridgedMaxMana;
    }

    public void setLastBridgedMaxMana(int value) {
        lastBridgedMaxMana = value;
    }

    public int getLastBridgedMaxStamina() {
        return lastBridgedMaxStamina;
    }

    public void setLastBridgedMaxStamina(int value) {
        lastBridgedMaxStamina = value;
    }

    public int getLastBridgedMaxConcentration() {
        return lastBridgedMaxConcentration;
    }

    public void setLastBridgedMaxConcentration(int value) {
        lastBridgedMaxConcentration = value;
    }

    public int getLastBridgedMaxRage() {
        return lastBridgedMaxRage;
    }

    public void setLastBridgedMaxRage(int value) {
        lastBridgedMaxRage = value;
    }

    public int getLastBridgedMovementSpeedBps() {
        return lastBridgedMovementSpeedBps;
    }

    public void setLastBridgedMovementSpeedBps(int value) {
        lastBridgedMovementSpeedBps = value;
    }

    // ========== HUD STATE ACCESSORS ==========

    public boolean isLastHudShown() {
        return lastHudShown;
    }

    public void setLastHudShown(boolean shown) {
        lastHudShown = shown;
    }

    public boolean isLastHudConcentrationVisible() {
        return lastHudConcentrationVisible;
    }

    public void setLastHudConcentrationVisible(boolean visible) {
        lastHudConcentrationVisible = visible;
    }

    public boolean isLastHudRageVisible() {
        return lastHudRageVisible;
    }

    public void setLastHudRageVisible(boolean visible) {
        lastHudRageVisible = visible;
    }

    public int getLastHudConcentrationCurrent() {
        return lastHudConcentrationCurrent;
    }

    public void setLastHudConcentrationCurrent(int value) {
        lastHudConcentrationCurrent = value;
    }

    public int getLastHudConcentrationMax() {
        return lastHudConcentrationMax;
    }

    public void setLastHudConcentrationMax(int value) {
        lastHudConcentrationMax = value;
    }

    public int getLastHudRageCurrent() {
        return lastHudRageCurrent;
    }

    public void setLastHudRageCurrent(int value) {
        lastHudRageCurrent = value;
    }

    public int getLastHudRageMax() {
        return lastHudRageMax;
    }

    public void setLastHudRageMax(int value) {
        lastHudRageMax = value;
    }

    public int getLastHudBreakpointHash() {
        return lastHudBreakpointHash;
    }

    public void setLastHudBreakpointHash(int hash) {
        lastHudBreakpointHash = hash;
    }

    public float getLastHudRegenRate() {
        return lastHudRegenRate;
    }

    public void setLastHudRegenRate(float rate) {
        lastHudRegenRate = rate;
    }

    // ========== EFFECTIVENESS HELPERS ==========

    /**
     * Get the effectiveness of a rating stat against a target level.
     * <p>
     * For rating stats (Armor, Evasion, Resistances), this applies the
     * PoE-style diminishing returns formula to convert the raw rating
     * to an effectiveness percentage in basis points.
     * <p>
     * For non-rating stats, returns the cached value directly.
     *
     * @param statIndex The stat index
     * @param targetLevel The level of the target (attacker for defense, defender for offense)
    * @return Effectiveness in basis points (10000 = 100%)
     */
    public int getEffectiveness(int statIndex, int targetLevel) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        StatDefinition statDef = registry.getStat(statIndex);

        if (statDef == null) {
            return 0;
        }

        int rating = getCachedValue(statIndex);

        if (statDef.displayFormat() != DisplayFormat.RATING) {
            // Not a rating stat, return the cached value as-is
            return rating;
        }

        // Use RatingConverter to convert rating to effectiveness
        return reign.software.hyforged.stats.engine.RatingConverter.getEffectivenessForStat(
            statDef.id(), rating, targetLevel
        );
    }

    /**
     * Get the effectiveness of a rating stat by StatId.
     */
    public int getEffectiveness(@Nonnull StatId statId, int targetLevel) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int statIndex = registry.getIndex(statId);
        if (statIndex < 0) {
            return 0;
        }
        return getEffectiveness(statIndex, targetLevel);
    }

    // Context-aware queries and breakdown computation are handled by
    // HyforgedStatQueryService to keep this component as pure data.
    
    // ========== COMPONENT INTERFACE ==========
    
    @Nullable
    @Override
    public Component<EntityStore> clone() {
        HyforgedStatComponent copy = new HyforgedStatComponent();
        copy.baseValues.putAll(this.baseValues);
        copy.modifiers.addAll(this.modifiers);
        copy.conditionalModifiers.addAll(this.conditionalModifiers);
        copy.cachedValues = this.cachedValues.clone();
        copy.dirtyFlags.or(this.dirtyFlags);
        copy.allDirty = this.allDirty;
        // isBufferingChanges and changeBuffer are intentionally NOT copied.
        // Cloning mid-buffer would produce an inconsistent state; the clone
        // starts with a clean (non-buffering) state and an empty buffer.
        copy.lastBridgedMaxHealth = this.lastBridgedMaxHealth;
        copy.lastBridgedMaxMana = this.lastBridgedMaxMana;
        copy.lastBridgedMaxStamina = this.lastBridgedMaxStamina;
        copy.lastBridgedMaxConcentration = this.lastBridgedMaxConcentration;
        copy.lastBridgedMaxRage = this.lastBridgedMaxRage;
        copy.lastBridgedMovementSpeedBps = this.lastBridgedMovementSpeedBps;
        copy.lastHudShown = this.lastHudShown;
        copy.lastHudConcentrationVisible = this.lastHudConcentrationVisible;
        copy.lastHudRageVisible = this.lastHudRageVisible;
        copy.lastHudConcentrationCurrent = this.lastHudConcentrationCurrent;
        copy.lastHudConcentrationMax = this.lastHudConcentrationMax;
        copy.lastHudRageCurrent = this.lastHudRageCurrent;
        copy.lastHudRageMax = this.lastHudRageMax;
        return copy;
    }
}
