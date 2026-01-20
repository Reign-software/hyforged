package reign.software.hyforged.affix.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * Defines eligibility constraints for where an affix can appear.
 * <p>
 * Eligibility is checked against item categories, tags, and quality range.
 * An item must match at least one category OR one tag (if specified),
 * must not match any exclude tags, and must fall within the quality range.
 * <p>
 * This is pure immutable data following ECS principles.
 *
 * @param itemCategories Set of item category IDs this affix can appear on (empty = any)
 * @param itemTags Set of item tags this affix can appear on (empty = any)
 * @param excludeTags Set of tags that prevent this affix from appearing
 * @param minQuality Minimum quality tier (null = no minimum)
 * @param maxQuality Maximum quality tier (null = no maximum)
 */
public record AffixEligibility(
    @Nonnull Set<String> itemCategories,
    @Nonnull Set<String> itemTags,
    @Nonnull Set<String> excludeTags,
    @Nullable String minQuality,
    @Nullable String maxQuality
) {
    
    /** An eligibility that accepts all items */
    public static final AffixEligibility ANY = new AffixEligibility(
        Collections.emptySet(),
        Collections.emptySet(),
        Collections.emptySet(),
        null,
        null
    );
    
    public AffixEligibility {
        itemCategories = itemCategories != null ? Set.copyOf(itemCategories) : Collections.emptySet();
        itemTags = itemTags != null ? Set.copyOf(itemTags) : Collections.emptySet();
        excludeTags = excludeTags != null ? Set.copyOf(excludeTags) : Collections.emptySet();
    }
    
    /**
     * Check if this eligibility has any category constraints.
     */
    public boolean hasCategoryConstraints() {
        return !itemCategories.isEmpty();
    }
    
    /**
     * Check if this eligibility has any tag constraints.
     */
    public boolean hasTagConstraints() {
        return !itemTags.isEmpty();
    }
    
    /**
     * Check if this eligibility has any exclusion constraints.
     */
    public boolean hasExcludeConstraints() {
        return !excludeTags.isEmpty();
    }
    
    /**
     * Check if this eligibility has quality constraints.
     */
    public boolean hasQualityConstraints() {
        return minQuality != null || maxQuality != null;
    }
    
    /**
     * Check if an item matches the category/tag inclusion criteria.
     * <p>
     * If no categories or tags are specified, this returns true.
     * Otherwise, the item must match at least one category OR at least one tag.
     *
     * @param categories The item's categories
     * @param tags The item's tags
     * @return true if the item matches the inclusion criteria
     */
    public boolean matchesInclusion(@Nonnull Set<String> categories, @Nonnull Set<String> tags) {
        // No constraints = matches everything
        if (!hasCategoryConstraints() && !hasTagConstraints()) {
            return true;
        }
        
        // Check category match
        if (hasCategoryConstraints()) {
            for (String category : categories) {
                if (itemCategories.contains(category)) {
                    return true;
                }
            }
        }
        
        // Check tag match
        if (hasTagConstraints()) {
            for (String tag : tags) {
                if (itemTags.contains(tag)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if an item matches the exclusion criteria.
     * <p>
     * Returns true if the item should be EXCLUDED (has a matching exclude tag).
     *
     * @param tags The item's tags
     * @return true if the item should be excluded
     */
    public boolean matchesExclusion(@Nonnull Set<String> tags) {
        if (!hasExcludeConstraints()) {
            return false;
        }
        
        for (String tag : tags) {
            if (excludeTags.contains(tag)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a quality falls within the specified range.
     * <p>
     * Quality ordering is determined by the QualityAffixRuleRegistry.
     * This method uses string comparison as a simple fallback; callers should
     * use the registry's quality ordering for accurate checks.
     *
     * @param quality The quality ID to check
     * @param qualityOrder A mapping of quality ID to numeric order (lower = worse)
     * @return true if the quality is within range
     */
    public boolean matchesQuality(@Nonnull String quality, @Nonnull java.util.Map<String, Integer> qualityOrder) {
        if (!hasQualityConstraints()) {
            return true;
        }
        
        Integer targetOrder = qualityOrder.get(quality);
        if (targetOrder == null) {
            // Unknown quality - assume it doesn't match if we have constraints
            return false;
        }
        
        if (minQuality != null) {
            Integer minOrder = qualityOrder.get(minQuality);
            if (minOrder != null && targetOrder < minOrder) {
                return false;
            }
        }
        
        if (maxQuality != null) {
            Integer maxOrder = qualityOrder.get(maxQuality);
            if (maxOrder != null && targetOrder > maxOrder) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Builder for creating AffixEligibility instances.
     */
    public static class Builder {
        private Set<String> itemCategories = Collections.emptySet();
        private Set<String> itemTags = Collections.emptySet();
        private Set<String> excludeTags = Collections.emptySet();
        private String minQuality = null;
        private String maxQuality = null;
        
        public Builder categories(@Nonnull Set<String> categories) {
            this.itemCategories = categories;
            return this;
        }
        
        public Builder tags(@Nonnull Set<String> tags) {
            this.itemTags = tags;
            return this;
        }
        
        public Builder excludeTags(@Nonnull Set<String> excludeTags) {
            this.excludeTags = excludeTags;
            return this;
        }
        
        public Builder qualityRange(@Nullable String min, @Nullable String max) {
            this.minQuality = min;
            this.maxQuality = max;
            return this;
        }
        
        @Nonnull
        public AffixEligibility build() {
            return new AffixEligibility(itemCategories, itemTags, excludeTags, minQuality, maxQuality);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Check if a single category matches the category constraints.
     */
    public boolean matchesCategory(@Nonnull String category) {
        return itemCategories.contains(category);
    }
    
    /**
     * Check if a single tag matches the tag constraints.
     */
    public boolean matchesTag(@Nonnull String tag) {
        return itemTags.contains(tag);
    }
    
    /**
     * Check if a tag should exclude this affix.
     */
    public boolean isExcludedTag(@Nonnull String tag) {
        return excludeTags.contains(tag);
    }
    
    /**
     * Check if a quality is within range using the quality registry for ordering.
     *
     * @param quality The quality to check
     * @param registry The quality registry for ordering
     * @return true if quality is within the eligibility range
     */
    public boolean isQualityInRange(
            @Nonnull String quality,
            @Nonnull reign.software.hyforged.affix.registry.QualityAffixRuleRegistry registry
    ) {
        if (!hasQualityConstraints()) {
            return true;
        }
        
        java.util.Map<String, Integer> qualityOrder = registry.getQualityOrder();
        return matchesQuality(quality, qualityOrder);
    }
}
