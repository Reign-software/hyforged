package reign.software.hyforged.stats.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.scaling.ScalingRule;
import reign.software.hyforged.stats.engine.ScalingEngine;

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
    
    private final List<StatModifier> modifiers = new ArrayList<>();
    
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
        if (baseValues.remove(statIndex) != baseValues.defaultReturnValue()) {
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
    public List<StatModifier> getModifiers() {
        return List.copyOf(modifiers);
    }
    
    /**
     * Add a modifier to this entity.
     * @return true if added, false if at max capacity
     */
    public boolean addModifier(@Nonnull StatModifier modifier) {
        if (modifiers.size() >= MAX_MODIFIERS) {
            return false;
        }
        modifiers.add(modifier);
        markAffectedStatsDirty(modifier);
        return true;
    }
    
    /**
     * Remove a modifier by source ID.
     * @return true if any modifiers were removed
     */
    public boolean removeModifiersBySource(@Nonnull String sourceId) {
        // Collect affected stats before removal
        for (StatModifier m : modifiers) {
            if (m.sourceId().equals(sourceId)) {
                markAffectedStatsDirty(m);
            }
        }
        return modifiers.removeIf(m -> m.sourceId().equals(sourceId));
    }
    
    /**
     * Remove all modifiers with the given source type.
     */
    public boolean removeModifiersBySourceType(@Nonnull ModifierSource sourceType) {
        // Collect affected stats before removal
        for (StatModifier m : modifiers) {
            if (m.sourceType() == sourceType) {
                markAffectedStatsDirty(m);
            }
        }
        return modifiers.removeIf(m -> m.sourceType() == sourceType);
    }
    
    /**
     * Remove expired modifiers based on current game tick.
     * @return number of modifiers removed
     */
    public int removeExpiredModifiers(long currentTick) {
        // Collect affected stats before removal
        int count = 0;
        for (StatModifier m : modifiers) {
            if (m.expirationTick() > 0 && m.expirationTick() <= currentTick) {
                markAffectedStatsDirty(m);
                count++;
            }
        }
        if (count > 0) {
            modifiers.removeIf(m -> m.expirationTick() > 0 && m.expirationTick() <= currentTick);
        }
        return count;
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
     * Mark all stats affected by a modifier as dirty.
     * Handles both direct stat targeting and tag targeting.
     */
    private void markAffectedStatsDirty(StatModifier modifier) {
        if (modifier.targetStatIndex() >= 0) {
            markStatDirty(modifier.targetStatIndex());
        }
        if (modifier.targetTagId() != null) {
            for (int statIdx : StatDefinitionRegistry.get().getStatIndicesForTag(modifier.targetTagId())) {
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
     * @return Effectiveness in basis points (1000 = 100%)
     */
    public int getEffectiveness(int statIndex, int targetLevel) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        StatDefinition statDef = registry.getStat(statIndex);
        
        if (statDef == null) {
            return 0;
        }
        
        int rating = getCachedValue(statIndex);
        
        if (!statDef.isRating()) {
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
    
    // ========== BREAKDOWN HELPERS ==========
    
    /**
     * Get a detailed breakdown of a stat's value for UI display.
     * <p>
     * This computes the stat value with full breakdown information,
     * showing all contributors, scaling contributions, and intermediate values.
     *
     * @param statIndex The stat index
     * @param targetLevel The target level (for rating effectiveness calculation)
     * @return The stat breakdown, or null if stat not found
     */
    @Nullable
    public reign.software.hyforged.stats.breakdown.StatBreakdown getStatBreakdown(int statIndex, int targetLevel) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        StatDefinition statDef = registry.getStat(statIndex);
        
        if (statDef == null) {
            return null;
        }
        
        // Calculate base value and scaling contributions
        int rawBase = 0;
        List<reign.software.hyforged.stats.breakdown.ScalingContribution> scalingContribs = new ArrayList<>();
        
        if (statDef.hasScaling()) {
            // Compute scaling contributions from source stats
            for (ScalingRule rule : statDef.scaling()) {
                int sourceIndex = registry.getIndex(rule.source());
                if (sourceIndex >= 0) {
                    int sourceValue = getCachedValue(sourceIndex);
                    int contribution = ScalingEngine.computeContribution(rule, sourceValue);
                    
                    StatDefinition sourceDef = registry.getStat(sourceIndex);
                    String sourceDisplayName = sourceDef != null ? sourceDef.displayName() : rule.source().fullId();
                    
                    scalingContribs.add(new reign.software.hyforged.stats.breakdown.ScalingContribution(
                        rule.source(),
                        sourceDisplayName,
                        contribution,
                        rule.type()
                    ));
                }
            }
            // Raw base for scaling stats is the sum of contributions
            rawBase = scalingContribs.stream()
                .mapToInt(reign.software.hyforged.stats.breakdown.ScalingContribution::contribution)
                .sum();
        } else {
            // Non-scaling stat: use base value or default
            rawBase = getBaseValue(statIndex);
        }
        
        // Add any explicit base value on top of scaling
        int explicitBase = hasBaseValue(statIndex) ? getBaseValue(statIndex) : 0;
        int scaledBase = rawBase + (statDef.hasScaling() ? explicitBase : 0);
        
        // Get applicable modifiers
        List<StatModifier> applicable = new ArrayList<>();
        for (StatModifier mod : modifiers) {
            if (mod.targetStatIndex() == statIndex) {
                applicable.add(mod);
            } else if (mod.targetTagId() != null) {
                if (statDef.tags().contains(mod.targetTagId()) ||
                    registry.getStatIndicesForTag(mod.targetTagId()).contains(statIndex)) {
                    applicable.add(mod);
                }
            }
        }
        
        // Compute with breakdown using the scaled base
        reign.software.hyforged.stats.engine.StackingEngine.ComputeResult result =
            reign.software.hyforged.stats.engine.StackingEngine.computeWithBreakdown(
                scaledBase, applicable, statDef
            );
        
        // Build breakdown entries
        reign.software.hyforged.stats.breakdown.StatBreakdown.Builder builder = 
            reign.software.hyforged.stats.breakdown.StatBreakdown.builder(statDef.id())
                .from(statDef)
                .baseValue(statDef.hasScaling() ? 0 : rawBase)
                .scalingContributions(scalingContribs)
                .scaledBase(scaledBase)
                .flatTotal(result.flatTotal)
                .afterFlat(result.afterFlat)
                .increasedTotalBps(result.increasedTotalBps)
                .afterIncreased(result.afterIncreased)
                .afterMore(result.afterMore)
                .afterCap(result.afterCap)
                .finalValue(result.finalValue);
        
        // Add entries for each modifier
        for (StatModifier mod : result.getAllModifiers()) {
            builder.addEntry(new reign.software.hyforged.stats.breakdown.BreakdownEntry(
                mod.sourceId(),
                mod.sourceType(),
                mod.modifierType(),
                mod.value(),
                mod.sourceId() // Use sourceId as display name for now
            ));
        }
        
        // Add effectiveness for rating stats
        if (statDef.isRating()) {
            int effectiveness = getEffectiveness(statIndex, targetLevel);
            builder.effectivenessBps(effectiveness);
        }
        
        return builder.build();
    }
    
    /**
     * Get a detailed breakdown of a stat by StatId.
     */
    @Nullable
    public reign.software.hyforged.stats.breakdown.StatBreakdown getStatBreakdown(@Nonnull StatId statId, int targetLevel) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int statIndex = registry.getIndex(statId);
        if (statIndex < 0) {
            return null;
        }
        return getStatBreakdown(statIndex, targetLevel);
    }
    
    // ========== COMPONENT INTERFACE ==========
    
    @Nullable
    @Override
    public Component<EntityStore> clone() {
        HyforgedStatComponent copy = new HyforgedStatComponent();
        copy.baseValues.putAll(this.baseValues);
        copy.modifiers.addAll(this.modifiers);
        copy.cachedValues = this.cachedValues.clone();
        copy.dirtyFlags.or(this.dirtyFlags);
        copy.allDirty = this.allDirty;
        copy.lastBridgedMaxHealth = this.lastBridgedMaxHealth;
        copy.lastBridgedMaxMana = this.lastBridgedMaxMana;
        copy.lastBridgedMaxStamina = this.lastBridgedMaxStamina;
        return copy;
    }
}
