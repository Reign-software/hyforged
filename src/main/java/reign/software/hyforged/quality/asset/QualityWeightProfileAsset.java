package reign.software.hyforged.quality.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import reign.software.hyforged.quality.model.QualityWeightProfile;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON asset definition for quality weight profiles.
 */
public class QualityWeightProfileAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, QualityWeightProfileAsset>> {

    private static final MapCodec<Integer, Map<String, Integer>> WEIGHTS_CODEC =
            new MapCodec<>(Codec.INTEGER, HashMap::new);

    private static final ArrayCodec<String> STRING_ARRAY_CODEC = new ArrayCodec<>(Codec.STRING, String[]::new);

    public static final AssetBuilderCodec<String, QualityWeightProfileAsset> CODEC = AssetBuilderCodec
            .builder(
                    QualityWeightProfileAsset.class,
                    QualityWeightProfileAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .append(
                    new KeyedCodec<>("Description", Codec.STRING),
                    (asset, value) -> asset.description = value != null ? value : "",
                    asset -> asset.description
            )
            .add()
            .append(
                    new KeyedCodec<>("Weights", WEIGHTS_CODEC),
                    (asset, value) -> asset.weights = value != null ? value : new HashMap<>(),
                    asset -> asset.weights
            )
            .add()
            .append(
                    new KeyedCodec<>("EligibleQualities", STRING_ARRAY_CODEC),
                    (asset, value) -> asset.eligibleQualities = value != null ? value : new String[0],
                    asset -> asset.eligibleQualities
            )
            .add()
            .build();

    private static AssetStore<String, QualityWeightProfileAsset, IndexedLookupTableAssetMap<String, QualityWeightProfileAsset>> ASSET_STORE;

    private String id;
    private AssetExtraInfo.Data data;
    private String description = "";
    private Map<String, Integer> weights = new HashMap<>();
    private String[] eligibleQualities = new String[0];

    public QualityWeightProfileAsset() {}

    @Nonnull
    public static AssetStore<String, QualityWeightProfileAsset, IndexedLookupTableAssetMap<String, QualityWeightProfileAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(QualityWeightProfileAsset.class);
        }
        return ASSET_STORE;
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    public QualityWeightProfile toProfile() {
        return new QualityWeightProfile(id, description, weights, java.util.List.of(eligibleQualities));
    }
}
