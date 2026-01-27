package reign.software.hyforged.currency.config;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Asset class for loading VaultUpgradesConfig from JSON.
 * <p>
 * Uses a flat structure for vault tiers to avoid complex nested codecs.
 */
public class VaultUpgradesConfigAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, VaultUpgradesConfigAsset>> {

    public static final AssetBuilderCodec<String, VaultUpgradesConfigAsset> CODEC = AssetBuilderCodec
        .builder(
            VaultUpgradesConfigAsset.class,
            VaultUpgradesConfigAsset::new,
            Codec.STRING,
            (asset, id) -> asset.id = id,
            asset -> asset.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data
        )
        .append(
            new KeyedCodec<>("Id", Codec.STRING),
            (asset, value) -> asset.id = value != null ? value : asset.id,
            asset -> asset.id
        )
        .add()
        // Tier 1
        .appendInherited(
            new KeyedCodec<>("Tier1Capacity", Codec.INTEGER),
            (asset, value) -> asset.tier1Capacity = value != null ? value : 50000,
            asset -> asset.tier1Capacity,
            (asset, parent) -> asset.tier1Capacity = parent.tier1Capacity
        )
        .add()
        // Tier 2
        .appendInherited(
            new KeyedCodec<>("Tier2Capacity", Codec.INTEGER),
            (asset, value) -> asset.tier2Capacity = value != null ? value : 100000,
            asset -> asset.tier2Capacity,
            (asset, parent) -> asset.tier2Capacity = parent.tier2Capacity
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("Tier2Cost", Codec.INTEGER),
            (asset, value) -> asset.tier2Cost = value != null ? value : 5000,
            asset -> asset.tier2Cost,
            (asset, parent) -> asset.tier2Cost = parent.tier2Cost
        )
        .add()
        .append(
            new KeyedCodec<>("Tier2Item", Codec.STRING),
            (asset, value) -> asset.tier2Item = value,
            asset -> asset.tier2Item
        )
        .add()
        // Tier 3
        .appendInherited(
            new KeyedCodec<>("Tier3Capacity", Codec.INTEGER),
            (asset, value) -> asset.tier3Capacity = value != null ? value : 250000,
            asset -> asset.tier3Capacity,
            (asset, parent) -> asset.tier3Capacity = parent.tier3Capacity
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("Tier3Cost", Codec.INTEGER),
            (asset, value) -> asset.tier3Cost = value != null ? value : 15000,
            asset -> asset.tier3Cost,
            (asset, parent) -> asset.tier3Cost = parent.tier3Cost
        )
        .add()
        .append(
            new KeyedCodec<>("Tier3Item", Codec.STRING),
            (asset, value) -> asset.tier3Item = value,
            asset -> asset.tier3Item
        )
        .add()
        // Tier 4
        .appendInherited(
            new KeyedCodec<>("Tier4Capacity", Codec.INTEGER),
            (asset, value) -> asset.tier4Capacity = value != null ? value : 500000,
            asset -> asset.tier4Capacity,
            (asset, parent) -> asset.tier4Capacity = parent.tier4Capacity
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("Tier4Cost", Codec.INTEGER),
            (asset, value) -> asset.tier4Cost = value != null ? value : 50000,
            asset -> asset.tier4Cost,
            (asset, parent) -> asset.tier4Cost = parent.tier4Cost
        )
        .add()
        .append(
            new KeyedCodec<>("Tier4Item", Codec.STRING),
            (asset, value) -> asset.tier4Item = value,
            asset -> asset.tier4Item
        )
        .add()
        .build();

    // Asset metadata
    private String id = "hyforged:vault-upgrades";
    private AssetExtraInfo.Data data;

    // Tier 1 (base tier, no upgrade cost)
    private int tier1Capacity = 50000;
    
    // Tier 2
    private int tier2Capacity = 100000;
    private int tier2Cost = 5000;
    private String tier2Item = "hyforged:vault_upgrade_1";
    
    // Tier 3
    private int tier3Capacity = 250000;
    private int tier3Cost = 15000;
    private String tier3Item = "hyforged:vault_upgrade_2";
    
    // Tier 4
    private int tier4Capacity = 500000;
    private int tier4Cost = 50000;
    private String tier4Item = "hyforged:vault_upgrade_3";

    public VaultUpgradesConfigAsset() {
        // Required for codec
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    /**
     * Get the capacity for a given tier.
     */
    public int getCapacity(int tier) {
        return switch (tier) {
            case 1 -> tier1Capacity;
            case 2 -> tier2Capacity;
            case 3 -> tier3Capacity;
            case 4 -> tier4Capacity;
            default -> 0;
        };
    }

    /**
     * Get the upgrade cost to reach a given tier.
     */
    public int getUpgradeCost(int tier) {
        return switch (tier) {
            case 2 -> tier2Cost;
            case 3 -> tier3Cost;
            case 4 -> tier4Cost;
            default -> 0;
        };
    }

    /**
     * Get the required upgrade item for a given tier.
     */
    @Nullable
    public String getUpgradeItem(int tier) {
        return switch (tier) {
            case 2 -> tier2Item;
            case 3 -> tier3Item;
            case 4 -> tier4Item;
            default -> null;
        };
    }

    /**
     * Get the maximum tier number.
     */
    public int getMaxTier() {
        return 4;
    }
}
