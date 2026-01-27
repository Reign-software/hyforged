package reign.software.hyforged.currency.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Logger;

/**
 * Configuration for vault upgrade tiers.
 * <p>
 * Loaded from: Server/Hyforged/Config/VaultUpgrades.json
 * <p>
 * Defines vault capacity tiers and upgrade costs.
 */
public class VaultUpgradesConfig {

    private static final Logger LOGGER = Logger.getLogger(VaultUpgradesConfig.class.getName());

    private static VaultUpgradesConfig instance;

    // Config reference
    private VaultUpgradesConfigAsset asset;

    private VaultUpgradesConfig() {
        // Private constructor
    }

    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static VaultUpgradesConfig get() {
        if (instance == null) {
            instance = new VaultUpgradesConfig();
        }
        return instance;
    }

    /**
     * Apply loaded config values.
     */
    public static void apply(@Nonnull VaultUpgradesConfigAsset asset) {
        VaultUpgradesConfig config = get();
        config.asset = asset;

        LOGGER.info("Applied VaultUpgradesConfig: " + asset.getMaxTier() + " tiers loaded");
    }

    /**
     * Get the maximum tier number.
     */
    public int getMaxTier() {
        return asset != null ? asset.getMaxTier() : 4;
    }

    /**
     * Get the capacity for a given tier.
     *
     * @param tier The tier number (1-based)
     * @return The capacity
     */
    public int getCapacity(int tier) {
        if (asset != null) {
            return asset.getCapacity(tier);
        }
        // Fallback defaults
        return switch (tier) {
            case 1 -> 50000;
            case 2 -> 100000;
            case 3 -> 250000;
            case 4 -> 500000;
            default -> 0;
        };
    }

    /**
     * Get the upgrade cost to reach a given tier.
     *
     * @param tier The target tier number
     * @return The Tradebar cost
     */
    public int getUpgradeCost(int tier) {
        if (asset != null) {
            return asset.getUpgradeCost(tier);
        }
        // Fallback defaults
        return switch (tier) {
            case 2 -> 5000;
            case 3 -> 15000;
            case 4 -> 50000;
            default -> 0;
        };
    }

    /**
     * Get the required upgrade item for a given tier.
     *
     * @param tier The target tier number
     * @return The item ID, or null if none required
     */
    @Nullable
    public String getUpgradeItem(int tier) {
        if (asset != null) {
            return asset.getUpgradeItem(tier);
        }
        // Fallback defaults
        return switch (tier) {
            case 2 -> "hyforged:vault_upgrade_1";
            case 3 -> "hyforged:vault_upgrade_2";
            case 4 -> "hyforged:vault_upgrade_3";
            default -> null;
        };
    }

    /**
     * Check if a tier can be upgraded to the next tier.
     *
     * @param currentTier The current tier
     * @return True if upgrade is available
     */
    public boolean canUpgrade(int currentTier) {
        return currentTier < getMaxTier();
    }

    /**
     * Record representing a vault tier.
     */
    public record VaultTier(
        int tier,
        int capacity,
        int upgradeCost,
        @Nullable String upgradeItem
    ) {}
}
