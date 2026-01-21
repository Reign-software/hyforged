package reign.software.hyforged.combat.scaling;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

import javax.annotation.Nonnull;

/**
 * JSON asset definition for world-level scaling configuration.
 * <p>
 * Defines how monster levels are calculated based on distance from world spawn.
 * Loaded from {@code Server/Hyforged/Combat/WorldScaling/*.json}.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Id": "hyforged:default-scaling",
 *   "Curve": "LINEAR",
 *   "BlocksPerLevel": 500,
 *   "MinLevel": 1,
 *   "MaxLevel": 100
 * }
 * </pre>
 */
public class WorldScalingConfigAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, WorldScalingConfigAsset>> {

    /** Codec for ScalingCurve enum */
    private static final EnumCodec<WorldScalingConfig.ScalingCurve> CURVE_CODEC = 
            new EnumCodec<>(WorldScalingConfig.ScalingCurve.class);

    /**
     * Codec for loading WorldScalingConfigAsset from JSON.
     */
    public static final AssetBuilderCodec<String, WorldScalingConfigAsset> CODEC = AssetBuilderCodec
            .builder(
                    WorldScalingConfigAsset.class,
                    WorldScalingConfigAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .append(
                    new KeyedCodec<>("Curve", CURVE_CODEC),
                    (asset, value) -> asset.curve = value != null ? value : WorldScalingConfig.ScalingCurve.LINEAR,
                    asset -> asset.curve
            )
            .add()
            .append(
                    new KeyedCodec<>("BlocksPerLevel", Codec.INTEGER),
                    (asset, value) -> asset.blocksPerLevel = value != null ? value : WorldScalingConfig.DEFAULT_BLOCKS_PER_LEVEL,
                    asset -> asset.blocksPerLevel
            )
            .add()
            .append(
                    new KeyedCodec<>("MinLevel", Codec.INTEGER),
                    (asset, value) -> asset.minLevel = value != null ? value : WorldScalingConfig.DEFAULT_MIN_LEVEL,
                    asset -> asset.minLevel
            )
            .add()
            .append(
                    new KeyedCodec<>("MaxLevel", Codec.INTEGER),
                    (asset, value) -> asset.maxLevel = value != null ? value : WorldScalingConfig.DEFAULT_MAX_LEVEL,
                    asset -> asset.maxLevel
            )
            .add()
            .build();

    private static AssetStore<String, WorldScalingConfigAsset, IndexedLookupTableAssetMap<String, WorldScalingConfigAsset>> ASSET_STORE;

    // Asset data
    private String id;
    private AssetExtraInfo.Data data;

    // Configuration fields
    private WorldScalingConfig.ScalingCurve curve = WorldScalingConfig.ScalingCurve.LINEAR;
    private int blocksPerLevel = WorldScalingConfig.DEFAULT_BLOCKS_PER_LEVEL;
    private int minLevel = WorldScalingConfig.DEFAULT_MIN_LEVEL;
    private int maxLevel = WorldScalingConfig.DEFAULT_MAX_LEVEL;

    public WorldScalingConfigAsset() {
    }

    /**
     * Get the asset store for world scaling configurations.
     */
    @Nonnull
    public static AssetStore<String, WorldScalingConfigAsset, IndexedLookupTableAssetMap<String, WorldScalingConfigAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(WorldScalingConfigAsset.class);
        }
        return ASSET_STORE;
    }

    // ========== JsonAssetWithMap Interface ==========

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    // ========== Conversion ==========

    /**
     * Convert this asset to a WorldScalingConfig model object.
     *
     * @return The WorldScalingConfig model
     */
    @Nonnull
    public WorldScalingConfig toWorldScalingConfig() {
        return new WorldScalingConfig(
                id,
                curve,
                blocksPerLevel,
                minLevel,
                maxLevel
        );
    }

    // ========== Accessors ==========

    @Nonnull
    public WorldScalingConfig.ScalingCurve getCurve() {
        return curve;
    }

    public int getBlocksPerLevel() {
        return blocksPerLevel;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }
}
