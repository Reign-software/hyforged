package reign.software.hyforged.affix.model;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Defines affix capacity rules for a specific Quality tier.
 * <p>
 * Quality affix rules are loaded from JSON at {@code Server/Hyforged/Quality/AffixRules/*.json}.
 * Each rule specifies how many affixes of each type an item of that quality can have.
 * <p>
 * This is pure immutable data following ECS principles.
 *
 * @param quality The quality ID (e.g., "Common", "Legendary")
 * @param affixCapacity Map of affix type ID to maximum count for that type
 */
public record QualityAffixRule(
    @Nonnull String quality,
    @Nonnull Map<String, Integer> affixCapacity
) {
    
    /** A rule with zero capacity for all affix types */
    public static final QualityAffixRule EMPTY = new QualityAffixRule("", Collections.emptyMap());
    
    public QualityAffixRule {
        Objects.requireNonNull(quality, "quality cannot be null");
        affixCapacity = affixCapacity != null ? Map.copyOf(affixCapacity) : Collections.emptyMap();
        
        // Validate no negative capacities
        for (Map.Entry<String, Integer> entry : affixCapacity.entrySet()) {
            if (entry.getValue() < 0) {
                throw new IllegalArgumentException(
                    "Capacity for type '" + entry.getKey() + "' cannot be negative: " + entry.getValue());
            }
        }
    }
    
    /**
     * Get the capacity for a specific affix type.
     *
     * @param typeId The affix type ID (e.g., "prefix", "suffix")
     * @return The maximum number of affixes of this type, or 0 if not specified
     */
    public int getCapacity(@Nonnull String typeId) {
        return affixCapacity.getOrDefault(typeId, 0);
    }
    
    /**
     * Get the total affix capacity across all types.
     */
    public int getTotalCapacity() {
        return affixCapacity.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Check if this quality allows any affixes of the given type.
     */
    public boolean allowsType(@Nonnull String typeId) {
        return getCapacity(typeId) > 0;
    }
    
    /**
     * Check if this quality allows any affixes at all.
     */
    public boolean allowsAnyAffixes() {
        return getTotalCapacity() > 0;
    }
    
    /**
     * Builder for creating QualityAffixRule instances.
     */
    public static class Builder {
        private String quality;
        private final Map<String, Integer> affixCapacity = new HashMap<>();
        
        public Builder quality(@Nonnull String quality) {
            this.quality = quality;
            return this;
        }
        
        public Builder capacity(@Nonnull String typeId, int count) {
            this.affixCapacity.put(typeId, count);
            return this;
        }
        
        public Builder prefixCapacity(int count) {
            return capacity("prefix", count);
        }
        
        public Builder suffixCapacity(int count) {
            return capacity("suffix", count);
        }
        
        public Builder forgedCapacity(int count) {
            return capacity("forged", count);
        }
        
        @Nonnull
        public QualityAffixRule build() {
            return new QualityAffixRule(quality, affixCapacity);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
