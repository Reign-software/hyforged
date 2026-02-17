package reign.software.hyforged.affix.model;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Defines a pool of affixes that can appear on specific item/entity types.
 * <p>
 * Affix pools are loaded from JSON at {@code Server/Hyforged/Affixes/Pools/*.json}.
 * Each pool maps a set of categories/tags to lists of eligible affixes
 * organized by type. Types are fully data-driven (defined in AffixTypes/*.json)
 * and not hardcoded.
 * <p>
 * This is pure immutable data following ECS principles.
 *
 * @param id Unique identifier for this pool (e.g., "WeaponMelee", "NPCHostile")
 * @param priority Pool resolution priority (higher = checked first)
 * @param appliesTo Categories and tags this pool applies to
 * @param affixesByType Map of affix type ID to list of affix IDs for that type
 */
public record AffixPool(
    @Nonnull String id,
    int priority,
    @Nonnull AffixPoolAppliesTo appliesTo,
    @Nonnull Map<String, List<String>> affixesByType
) {
    
    /** Default priority for pools if not specified */
    public static final int DEFAULT_PRIORITY = 0;
    
    public AffixPool {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(appliesTo, "appliesTo cannot be null");
        
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        
        // Deep-copy to ensure immutability, filtering out empty lists
        if (affixesByType != null && !affixesByType.isEmpty()) {
            Map<String, List<String>> copy = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : affixesByType.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    copy.put(entry.getKey(), List.copyOf(entry.getValue()));
                }
            }
            affixesByType = Map.copyOf(copy);
        } else {
            affixesByType = Collections.emptyMap();
        }
    }
    
    // ========== Convenience Accessors ==========
    
    /**
     * Get prefix affix IDs. Convenience shorthand for {@code getAffixesForType("prefix")}.
     */
    @Nonnull
    public List<String> prefixes() {
        return affixesByType.getOrDefault("prefix", Collections.emptyList());
    }
    
    /**
     * Get suffix affix IDs. Convenience shorthand for {@code getAffixesForType("suffix")}.
     */
    @Nonnull
    public List<String> suffixes() {
        return affixesByType.getOrDefault("suffix", Collections.emptyList());
    }
    
    /**
     * Get forged affix IDs. Convenience shorthand for {@code getAffixesForType("forged")}.
     */
    @Nonnull
    public List<String> forged() {
        return affixesByType.getOrDefault("forged", Collections.emptyList());
    }
    
    // ========== Type-Generic Accessors ==========
    
    /**
     * Get the list of affix IDs for a specific type.
     *
     * @param typeId The affix type ID (e.g., "prefix", "suffix", "npc", "npc_rare")
     * @return The list of affix IDs, or empty list if type not present
     */
    @Nonnull
    public List<String> getAffixesForType(@Nonnull String typeId) {
        return affixesByType.getOrDefault(typeId, Collections.emptyList());
    }
    
    /**
     * Check if this pool has any affixes of the given type.
     */
    public boolean hasAffixesOfType(@Nonnull String typeId) {
        return !getAffixesForType(typeId).isEmpty();
    }
    
    /**
     * Check if this pool applies to an item/entity with the given categories and tags.
     */
    public boolean appliesTo(@Nonnull Set<String> categories, @Nonnull Set<String> tags) {
        return appliesTo.matches(categories, tags);
    }
    
    /**
     * Get the total number of affixes defined in this pool across all types.
     */
    public int getTotalAffixCount() {
        int count = 0;
        for (List<String> ids : affixesByType.values()) {
            count += ids.size();
        }
        return count;
    }
    
    /**
     * Get all affix type keys present in this pool that have at least one affix.
     */
    @Nonnull
    public Set<String> getAllAffixTypes() {
        return affixesByType.keySet();
    }
    
    /**
     * Defines what items/entities this pool applies to.
     *
     * @param categories Item categories this pool applies to
     * @param tags Item tags this pool applies to
     */
    public record AffixPoolAppliesTo(
        @Nonnull Set<String> categories,
        @Nonnull Set<String> tags
    ) {
        public AffixPoolAppliesTo {
            categories = categories != null ? Set.copyOf(categories) : Collections.emptySet();
            tags = tags != null ? Set.copyOf(tags) : Collections.emptySet();
        }
        
        /**
         * Check if this criteria matches an item with the given categories and tags.
         * <p>
         * Matches if the item has at least one matching category OR at least one matching tag.
         * If no categories or tags are specified, matches nothing.
         */
        public boolean matches(@Nonnull Set<String> itemCategories, @Nonnull Set<String> itemTags) {
            if (categories.isEmpty() && tags.isEmpty()) {
                return false;
            }
            
            for (String category : itemCategories) {
                if (categories.contains(category)) {
                    return true;
                }
            }
            
            for (String tag : itemTags) {
                if (tags.contains(tag)) {
                    return true;
                }
            }
            
            return false;
        }
    }
    
    // ========== Factory Methods ==========
    
    /**
     * Create an AffixPool with standard prefix/suffix/forged lists.
     * <p>
     * Convenience factory for pools that use the standard item affix types.
     */
    public static AffixPool of(
            @Nonnull String id,
            int priority,
            @Nonnull AffixPoolAppliesTo appliesTo,
            @Nonnull List<String> prefixes,
            @Nonnull List<String> suffixes,
            @Nonnull List<String> forged
    ) {
        Map<String, List<String>> map = new HashMap<>();
        if (prefixes != null && !prefixes.isEmpty()) map.put("prefix", prefixes);
        if (suffixes != null && !suffixes.isEmpty()) map.put("suffix", suffixes);
        if (forged != null && !forged.isEmpty()) map.put("forged", forged);
        return new AffixPool(id, priority, appliesTo, map);
    }
    
    /**
     * Builder for creating AffixPool instances.
     */
    public static class Builder {
        private String id;
        private int priority = DEFAULT_PRIORITY;
        private AffixPoolAppliesTo appliesTo = new AffixPoolAppliesTo(Collections.emptySet(), Collections.emptySet());
        private final Map<String, List<String>> affixesByType = new HashMap<>();
        
        public Builder id(@Nonnull String id) {
            this.id = id;
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder appliesTo(@Nonnull Set<String> categories, @Nonnull Set<String> tags) {
            this.appliesTo = new AffixPoolAppliesTo(categories, tags);
            return this;
        }
        
        public Builder prefixes(@Nonnull List<String> prefixes) {
            this.affixesByType.put("prefix", prefixes);
            return this;
        }
        
        public Builder suffixes(@Nonnull List<String> suffixes) {
            this.affixesByType.put("suffix", suffixes);
            return this;
        }
        
        public Builder forged(@Nonnull List<String> forged) {
            this.affixesByType.put("forged", forged);
            return this;
        }
        
        /**
         * Add affixes of any type.
         *
         * @param typeId The affix type ID (e.g., "npc", "npc_rare")
         * @param affixIds The list of affix IDs
         */
        public Builder affixes(@Nonnull String typeId, @Nonnull List<String> affixIds) {
            this.affixesByType.put(typeId, affixIds);
            return this;
        }
        
        /**
         * Merge an entire affixes-by-type map into the builder.
         */
        public Builder affixesByType(@Nonnull Map<String, List<String>> map) {
            this.affixesByType.putAll(map);
            return this;
        }
        
        @Nonnull
        public AffixPool build() {
            return new AffixPool(id, priority, appliesTo, affixesByType);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
