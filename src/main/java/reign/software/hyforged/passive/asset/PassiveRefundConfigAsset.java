package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;

import javax.annotation.Nonnull;

/**
 * Asset definition for passive tree refund cost configuration.
 * <p>
 * Loaded from JSON file at Server/Hyforged/Config/passive-refund.json.
 * <p>
 * Example JSON:
 * <pre>
 * {
 *   "Id": "hyforged:passive-refund-config",
 *   "BaseCost": 10,
 *   "LevelMultiplier": 2
 * }
 * </pre>
 * <p>
 * Formula: costPerNode = BaseCost + (characterLevel * LevelMultiplier)
 * At level 50: 10 + (50 × 2) = 110 Tradebars per node
 * At level 100: 10 + (100 × 2) = 210 Tradebars per node
 */
public class PassiveRefundConfigAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, PassiveRefundConfigAsset>> {
    
    public static final AssetBuilderCodec<String, PassiveRefundConfigAsset> CODEC = AssetBuilderCodec
        .builder(
            PassiveRefundConfigAsset.class,
            PassiveRefundConfigAsset::new,
            Codec.STRING,
            (asset, id) -> asset.id = id,
            asset -> asset.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data
        )
        .append(
            new KeyedCodec<>("BaseCost", Codec.INTEGER),
            (asset, value) -> asset.baseCost = value,
            asset -> asset.baseCost
        )
        .add()
        .append(
            new KeyedCodec<>("LevelMultiplier", Codec.INTEGER),
            (asset, value) -> asset.levelMultiplier = value,
            asset -> asset.levelMultiplier
        )
        .add()
        .append(
            new KeyedCodec<>("MaxBookPoints", Codec.INTEGER),
            (asset, value) -> asset.maxBookPoints = value,
            asset -> asset.maxBookPoints
        )
        .add()
        .build();
    
    private static AssetStore<String, PassiveRefundConfigAsset, IndexedLookupTableAssetMap<String, PassiveRefundConfigAsset>> ASSET_STORE;
    
    private String id;
    private Integer baseCost;
    private Integer levelMultiplier;
    private Integer maxBookPoints;
    private AssetExtraInfo.Data data;
    
    public PassiveRefundConfigAsset() {
        // Required for codec
    }
    
    /**
     * Get the asset store for passive refund config.
     */
    @Nonnull
    public static AssetStore<String, PassiveRefundConfigAsset, IndexedLookupTableAssetMap<String, PassiveRefundConfigAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(PassiveRefundConfigAsset.class);
        }
        return ASSET_STORE;
    }
    
    @Nonnull
    @Override
    public String getId() {
        return id != null ? id : "hyforged:passive-refund-config";
    }
    
    /**
     * Get the base Tradebar cost per node refund.
     * Default: 10
     */
    public int getBaseCost() {
        return baseCost != null ? baseCost : 10;
    }
    
    /**
     * Get the level multiplier for refund cost.
     * Default: 2
     */
    public int getLevelMultiplier() {
        return levelMultiplier != null ? levelMultiplier : 2;
    }
    
    /**
     * Get the maximum number of Point Book points.
     * Default: 20
     */
    public int getMaxBookPoints() {
        return maxBookPoints != null ? maxBookPoints : 20;
    }
    
    /**
     * Calculate the refund cost per node at a given character level.
     *
     * @param characterLevel The character level
     * @return The Tradebar cost per node
     */
    public int calculateRefundCostPerNode(int characterLevel) {
        return getBaseCost() + (characterLevel * getLevelMultiplier());
    }
}
