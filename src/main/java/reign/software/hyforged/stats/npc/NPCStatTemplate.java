package reign.software.hyforged.stats.npc;

import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolved NPC stat template with inheritance flattened.
 * <p>
 * Contains the final set of stat scalings and modifier pools
 * after merging with all parent templates.
 *
 * @param id            Template identifier (e.g., "hyforged:hostile")
 * @param parentId      Optional parent template ID (null if no parent)
 * @param stats         Map of stat ID → scaling definition
 * @param modifierPools Map of pool name → list of modifier IDs for elite rolling
 */
public record NPCStatTemplate(
        @Nonnull String id,
        @Nullable String parentId,
        @Nonnull Map<StatId, NPCStatScaling> stats,
        @Nonnull Map<String, List<String>> modifierPools
) {
    
    public NPCStatTemplate {
        stats = Map.copyOf(stats);
        modifierPools = modifierPools.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> List.copyOf(e.getValue())
                ));
    }
    
    /**
     * Resolve all stat base values for a given level.
     *
     * @param level The NPC level
     * @return Map of stat ID → resolved base value
     */
    @Nonnull
    public Map<StatId, Integer> resolveStats(int level) {
        Map<StatId, Integer> resolved = new HashMap<>();
        for (Map.Entry<StatId, NPCStatScaling> entry : stats.entrySet()) {
            resolved.put(entry.getKey(), entry.getValue().resolveAt(level));
        }
        return resolved;
    }
    
    /**
     * Get the modifier pool by name.
     *
     * @param poolName The pool name (e.g., "elite", "boss")
     * @return List of modifier IDs in the pool, or empty list if not found
     */
    @Nonnull
    public List<String> getModifierPool(@Nonnull String poolName) {
        return modifierPools.getOrDefault(poolName, Collections.emptyList());
    }
    
    /**
     * Check if this template has a parent.
     */
    public boolean hasParent() {
        return parentId != null && !parentId.isEmpty();
    }
    
    /**
     * Create a builder for constructing templates.
     */
    public static Builder builder(@Nonnull String id) {
        return new Builder(id);
    }
    
    /**
     * Builder for NPCStatTemplate.
     */
    public static class Builder {
        private final String id;
        private String parentId;
        private final Map<StatId, NPCStatScaling> stats = new HashMap<>();
        private final Map<String, List<String>> modifierPools = new HashMap<>();
        
        public Builder(@Nonnull String id) {
            this.id = id;
        }
        
        public Builder parent(@Nullable String parentId) {
            this.parentId = parentId;
            return this;
        }
        
        public Builder stat(@Nonnull StatId statId, int base, int perLevel) {
            this.stats.put(statId, new NPCStatScaling(base, perLevel));
            return this;
        }
        
        public Builder stat(@Nonnull StatId statId, @Nonnull NPCStatScaling scaling) {
            this.stats.put(statId, scaling);
            return this;
        }
        
        public Builder modifierPool(@Nonnull String poolName, @Nonnull List<String> modifiers) {
            this.modifierPools.put(poolName, modifiers);
            return this;
        }
        
        public Builder mergeStats(@Nonnull Map<StatId, NPCStatScaling> parentStats) {
            // Parent stats are added if not already present
            for (Map.Entry<StatId, NPCStatScaling> entry : parentStats.entrySet()) {
                this.stats.putIfAbsent(entry.getKey(), entry.getValue());
            }
            return this;
        }
        
        public Builder mergeModifierPools(@Nonnull Map<String, List<String>> parentPools) {
            // Parent pools are added if not already present
            for (Map.Entry<String, List<String>> entry : parentPools.entrySet()) {
                this.modifierPools.putIfAbsent(entry.getKey(), entry.getValue());
            }
            return this;
        }
        
        public NPCStatTemplate build() {
            return new NPCStatTemplate(id, parentId, stats, modifierPools);
        }
    }
}
