package reign.software.hyforged.currency.config;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Configuration for sell value calculation.
 * <p>
 * Loaded from: Server/Hyforged/Config/SellValueConfig.json
 * <p>
 * Defines:
 * - Base value for items
 * - Rarity multipliers
 * - Affix value per tier
 * - Minimum sell value
 */
public class SellValueConfig {

    private static final Logger LOGGER = Logger.getLogger(SellValueConfig.class.getName());

    private static SellValueConfig instance;

    // Config reference
    private SellValueConfigAsset asset;

    // Default values for fallback
    private int baseValue = 1;
    private int minSellValue = 1;

    private SellValueConfig() {
        // Private constructor
    }

    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static SellValueConfig get() {
        if (instance == null) {
            instance = new SellValueConfig();
        }
        return instance;
    }

    /**
     * Apply loaded config values.
     */
    public static void apply(@Nonnull SellValueConfigAsset asset) {
        SellValueConfig config = get();
        config.asset = asset;
        config.baseValue = asset.getBaseValue();
        config.minSellValue = asset.getMinSellValue();

        LOGGER.info("Applied SellValueConfig: baseValue=" + config.baseValue + 
                    ", minSellValue=" + config.minSellValue);
    }

    public int getBaseValue() {
        return baseValue;
    }

    public int getMinSellValue() {
        return minSellValue;
    }

    /**
     * Get the rarity multiplier for a given rarity.
     *
     * @param rarity The rarity name (e.g., "Common", "Rare")
     * @return The multiplier
     */
    public int getRarityMultiplier(@Nonnull String rarity) {
        if (asset != null) {
            return asset.getRarityMultiplier(rarity);
        }
        // Fallback defaults
        return switch (rarity.toLowerCase()) {
            case "junk" -> 0;
            case "common" -> 1;
            case "uncommon" -> 2;
            case "rare" -> 5;
            case "epic" -> 15;
            case "legendary" -> 50;
            default -> 1;
        };
    }

    /**
     * Get the affix value for a given tier.
     *
     * @param tier The affix tier (1-5)
     * @return The value per affix of this tier
     */
    public int getAffixValueForTier(int tier) {
        if (asset != null) {
            return asset.getAffixValueForTier(tier);
        }
        // Fallback defaults
        return switch (tier) {
            case 1 -> 100;
            case 2 -> 50;
            case 3 -> 25;
            case 4 -> 10;
            case 5 -> 5;
            default -> 0;
        };
    }

    /**
     * Calculate the sell value for an item.
     *
     * @param rarity The item's rarity
     * @param affixCount The number of affixes
     * @param averageAffixTier The average affix tier (1-5)
     * @param overrideValue Optional per-item override value (-1 to use formula)
     * @return The calculated sell value in Tradebars
     */
    public int calculateSellValue(
            @Nonnull String rarity,
            int affixCount,
            int averageAffixTier,
            int overrideValue
    ) {
        // Check for override
        if (overrideValue >= 0) {
            return overrideValue;
        }

        // Formula: baseSellValue + (rarityMultiplier × baseValue) + (affixValue × affixCount)
        int rarityMult = getRarityMultiplier(rarity);
        int affixValue = getAffixValueForTier(averageAffixTier);

        int value = baseValue + (rarityMult * baseValue) + (affixValue * affixCount);
        return Math.max(value, minSellValue);
    }
}
