package reign.software.hyforged.affix.service;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import reign.software.hyforged.quality.service.HyforgedQualityService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Utility class for extracting affix-relevant context from ItemStack instances.
 * <p>
 * This centralizes the logic for extracting quality, item level, categories, and tags
 * from Hytale's item system for use in affix rolling.
 */
public final class ItemContextExtractor {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    private static final String DEFAULT_QUALITY = "Common";
    private static final int DEFAULT_ITEM_LEVEL = 1;
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    
    private ItemContextExtractor() {
        // Utility class
    }
    
    /**
     * Build an AffixRollContext from an ItemStack.
     * <p>
     * Extracts item ID, quality, level, categories, and tags from the item's
     * configuration in Hytale's asset system.
     *
     * @param itemStack The item to create context for
     * @return The roll context, or null if item is not eligible for affixes
     */
    @Nullable
    public static AffixRollContext buildContext(@Nonnull ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return null;
        }
        
        String itemId = itemStack.getItemId();
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        
        // Get the Item asset configuration
        Item item = itemStack.getItem();
        if (item == null || item == Item.UNKNOWN) {
            LOGGER.at(Level.FINE).log("Unknown item type: %s", itemId);
            return null;
        }
        
        // Extract quality from item configuration
        String quality = HyforgedQualityService.getEffectiveQuality(itemStack);
        
        // Extract item level
        int itemLevel = extractItemLevel(item);
        
        // Extract categories
        String[] categories = extractCategories(item);
        
        // Extract tags (currently using categories as proxy, as Hytale items use categories)
        String[] tags = extractTags(item);
        
        // Check if item is eligible for affixes (needs at least some way to match pools)
        if (categories.length == 0 && tags.length == 0) {
            LOGGER.at(Level.FINER).log("Item %s has no categories or tags for pool matching", itemId);
            return null;
        }
        
        return AffixRollContext.of(itemId, quality, itemLevel, categories, tags);
    }
    
    /**
     * Extract quality tier name from an Item.
     *
     * @param item The item configuration
     * @return The quality name, or "Common" as default
     */
    @Nonnull
    public static String extractQuality(@Nonnull Item item) {
        try {
            int qualityIndex = item.getQualityIndex();
            if (qualityIndex >= 0) {
                ItemQuality quality = ItemQuality.getAssetMap().getAsset(qualityIndex);
                if (quality != null) {
                    return quality.getId();
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.FINE).withCause(e).log("Could not extract quality from item");
        }
        return DEFAULT_QUALITY;
    }
    
    /**
     * Extract item level from an Item.
     *
     * @param item The item configuration
     * @return The item level, or 1 as default
     */
    public static int extractItemLevel(@Nonnull Item item) {
        try {
            int level = item.getItemLevel();
            return level > 0 ? level : DEFAULT_ITEM_LEVEL;
        } catch (Exception e) {
            LOGGER.at(Level.FINE).withCause(e).log("Could not extract item level");
            return DEFAULT_ITEM_LEVEL;
        }
    }
    
    /**
     * Extract categories from an Item.
     *
     * @param item The item configuration
     * @return Array of category IDs, never null
     */
    @Nonnull
    public static String[] extractCategories(@Nonnull Item item) {
        try {
            String[] categories = item.getCategories();
            return categories != null ? categories : EMPTY_STRING_ARRAY;
        } catch (Exception e) {
            LOGGER.at(Level.FINE).withCause(e).log("Could not extract categories");
            return EMPTY_STRING_ARRAY;
        }
    }
    
    /**
     * Extract tags from an Item.
     * <p>
     * Hytale items store tags via the AssetExtraInfo.Data system. Tags are
     * derived from the asset hierarchy, explicit Tag fields, and Categories.
     * This method extracts the raw tag keys from the item's data.
     *
     * @param item The item configuration
     * @return Array of tag IDs, never null
     */
    @Nonnull
    public static String[] extractTags(@Nonnull Item item) {
        try {
            AssetExtraInfo.Data data = item.getData();
            if (data == null) {
                return EMPTY_STRING_ARRAY;
            }
            
            // getRawTags() returns Map<String, String[]> where keys are tag names
            // and values are tag values (e.g., "Type" -> ["Weapon", "Melee"])
            Map<String, String[]> rawTags = data.getRawTags();
            if (rawTags == null || rawTags.isEmpty()) {
                return EMPTY_STRING_ARRAY;
            }

            Set<String> expandedTags = expandRawTags(rawTags);
            return expandedTags.toArray(EMPTY_STRING_ARRAY);
        } catch (Exception e) {
            LOGGER.at(Level.FINE).withCause(e).log("Could not extract tags from item");
            return EMPTY_STRING_ARRAY;
        }
    }

    @Nonnull
    private static Set<String> expandRawTags(@Nonnull Map<String, String[]> rawTags) {
        Set<String> expanded = new LinkedHashSet<>();

        for (Map.Entry<String, String[]> entry : rawTags.entrySet()) {
            String category = entry.getKey();
            if (category == null || category.isBlank()) {
                continue;
            }

            String[] values = entry.getValue();
            if (values == null || values.length == 0) {
                expanded.add(category);
                continue;
            }

            expanded.add(category);
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                expanded.add(value);
                expanded.add(category + "=" + value);
                expanded.add(category + ":" + value);
            }
        }

        return expanded;
    }
    
    /**
     * Check if an ItemStack is potentially eligible for affixes.
     * <p>
     * This is a quick check that doesn't build the full context.
     *
     * @param itemStack The item to check
     * @return true if the item might be eligible for affixes
     */
    public static boolean isPotentiallyEligible(@Nonnull ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        
        Item item = itemStack.getItem();
        if (item == null || item == Item.UNKNOWN) {
            return false;
        }
        
        // Check if has categories (required for pool matching)
        String[] categories = item.getCategories();
        return categories != null && categories.length > 0;
    }
}
