package reign.software.hyforged.affix.service;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Context object for affix rolling, carrying item properties and roll modifiers.
 * <p>
 * This provides all the information needed to determine:
 * <ul>
 *   <li>Which pools are eligible based on item category/tags</li>
 *   <li>How many affixes can roll based on quality</li>
 *   <li>Which affix tiers are eligible based on item level</li>
 *   <li>Any bonuses to tier weight (e.g., from mob difficulty)</li>
 * </ul>
 *
 * @param itemId         The item type identifier
 * @param quality        The item's quality tier (e.g., "common", "rare")
 * @param itemLevel      The item's level for tier eligibility
 * @param itemCategories Array of item category IDs for pool matching
 * @param itemTags       Array of item tags for pool matching
 * @param tierWeightBonus Bonus applied to higher tier weights (positive = better tiers more likely)
 */
public record AffixRollContext(
        @Nonnull String itemId,
        @Nonnull String quality,
        int itemLevel,
        @Nonnull String[] itemCategories,
        @Nonnull String[] itemTags,
        int tierWeightBonus
) {
    
    /**
     * Canonical constructor with validation.
     */
    public AffixRollContext {
        Objects.requireNonNull(itemId, "itemId cannot be null");
        Objects.requireNonNull(quality, "quality cannot be null");
        Objects.requireNonNull(itemCategories, "itemCategories cannot be null");
        Objects.requireNonNull(itemTags, "itemTags cannot be null");
        
        if (itemId.isBlank()) {
            throw new IllegalArgumentException("itemId cannot be blank");
        }
        if (quality.isBlank()) {
            throw new IllegalArgumentException("quality cannot be blank");
        }
        if (itemLevel < 0) {
            throw new IllegalArgumentException("itemLevel cannot be negative: " + itemLevel);
        }
    }
    
    /**
     * Create a context with no tier weight bonus.
     */
    public static AffixRollContext of(
            @Nonnull String itemId,
            @Nonnull String quality,
            int itemLevel,
            @Nonnull String[] itemCategories,
            @Nonnull String[] itemTags
    ) {
        return new AffixRollContext(itemId, quality, itemLevel, itemCategories, itemTags, 0);
    }
    
    /**
     * Create a context with tier weight bonus.
     */
    public static AffixRollContext withBonus(
            @Nonnull String itemId,
            @Nonnull String quality,
            int itemLevel,
            @Nonnull String[] itemCategories,
            @Nonnull String[] itemTags,
            int tierWeightBonus
    ) {
        return new AffixRollContext(itemId, quality, itemLevel, itemCategories, itemTags, tierWeightBonus);
    }
    
    /**
     * Check if this context has the given category.
     */
    public boolean hasCategory(@Nonnull String category) {
        for (String c : itemCategories) {
            if (c.equals(category)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if this context has the given tag.
     */
    public boolean hasTag(@Nonnull String tag) {
        for (String t : itemTags) {
            if (t.equals(tag)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Create a new context with an updated tier weight bonus.
     */
    public AffixRollContext withTierWeightBonus(int bonus) {
        return new AffixRollContext(itemId, quality, itemLevel, itemCategories, itemTags, bonus);
    }
}
