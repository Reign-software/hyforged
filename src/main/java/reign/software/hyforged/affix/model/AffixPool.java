package reign.software.hyforged.affix.model;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Defines a pool of affixes that can appear on specific item types.
 * <p>
 * Affix pools are loaded from JSON at {@code Server/Hyforged/AffixPools/*.json}.
 * Each pool maps a set of item categories/tags to lists of eligible affixes
 * by type.
 * <p>
 * This is pure immutable data following ECS principles.
 *
 * @param id Unique identifier for this pool (e.g., "weapon-melee", "armor-heavy")
 * @param priority Pool resolution priority (higher = checked first)
 * @param appliesTo Categories and tags this pool applies to
 * @param prefixes List of prefix affix IDs eligible from this pool
 * @param suffixes List of suffix affix IDs eligible from this pool
 * @param forged List of forged affix IDs eligible from this pool
 */
public record AffixPool(
    @Nonnull String id,
    int priority,
    @Nonnull AffixPoolAppliesTo appliesTo,
    @Nonnull List<String> prefixes,
    @Nonnull List<String> suffixes,
    @Nonnull List<String> forged
) {
    
    /** Default priority for pools if not specified */
    public static final int DEFAULT_PRIORITY = 0;
    
    public AffixPool {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(appliesTo, "appliesTo cannot be null");
        
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        
        prefixes = prefixes != null ? List.copyOf(prefixes) : Collections.emptyList();
        suffixes = suffixes != null ? List.copyOf(suffixes) : Collections.emptyList();
        forged = forged != null ? List.copyOf(forged) : Collections.emptyList();
    }
    
    /**
     * Get the list of affix IDs for a specific type.
     *
     * @param typeId The affix type ID ("prefix", "suffix", "forged")
     * @return The list of affix IDs, or empty list if type not recognized
     */
    @Nonnull
    public List<String> getAffixesForType(@Nonnull String typeId) {
        return switch (typeId.toLowerCase()) {
            case "prefix" -> prefixes;
            case "suffix" -> suffixes;
            case "forged" -> forged;
            default -> Collections.emptyList();
        };
    }
    
    /**
     * Check if this pool has any affixes of the given type.
     */
    public boolean hasAffixesOfType(@Nonnull String typeId) {
        return !getAffixesForType(typeId).isEmpty();
    }
    
    /**
     * Check if this pool applies to an item with the given categories and tags.
     *
     * @param categories The item's categories
     * @param tags The item's tags
     * @return true if this pool applies to the item
     */
    public boolean appliesTo(@Nonnull Set<String> categories, @Nonnull Set<String> tags) {
        return appliesTo.matches(categories, tags);
    }
    
    /**
     * Get the total number of affixes defined in this pool across all types.
     */
    public int getTotalAffixCount() {
        return prefixes.size() + suffixes.size() + forged.size();
    }
    
    /**
     * Defines what items this pool applies to.
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
            // Must have at least one constraint
            if (categories.isEmpty() && tags.isEmpty()) {
                return false;
            }
            
            // Check category match
            for (String category : itemCategories) {
                if (categories.contains(category)) {
                    return true;
                }
            }
            
            // Check tag match
            for (String tag : itemTags) {
                if (tags.contains(tag)) {
                    return true;
                }
            }
            
            return false;
        }
    }
    
    /**
     * Builder for creating AffixPool instances.
     */
    public static class Builder {
        private String id;
        private int priority = DEFAULT_PRIORITY;
        private AffixPoolAppliesTo appliesTo = new AffixPoolAppliesTo(Collections.emptySet(), Collections.emptySet());
        private List<String> prefixes = Collections.emptyList();
        private List<String> suffixes = Collections.emptyList();
        private List<String> forged = Collections.emptyList();
        
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
            this.prefixes = prefixes;
            return this;
        }
        
        public Builder suffixes(@Nonnull List<String> suffixes) {
            this.suffixes = suffixes;
            return this;
        }
        
        public Builder forged(@Nonnull List<String> forged) {
            this.forged = forged;
            return this;
        }
        
        @Nonnull
        public AffixPool build() {
            return new AffixPool(id, priority, appliesTo, prefixes, suffixes, forged);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
