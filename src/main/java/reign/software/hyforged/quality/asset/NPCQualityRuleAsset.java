package reign.software.hyforged.quality.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import reign.software.hyforged.quality.model.NPCQualityRule;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON asset definition for NPC quality rules.
 */
public class NPCQualityRuleAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, NPCQualityRuleAsset>> {

    private static final MapCodec<Integer, Map<String, Integer>> INT_MAP_CODEC =
            new MapCodec<>(Codec.INTEGER, HashMap::new);

    private static final MapCodec<Double, Map<String, Double>> DOUBLE_MAP_CODEC =
            new MapCodec<>(Codec.DOUBLE, HashMap::new);

    public static final AssetBuilderCodec<String, NPCQualityRuleAsset> CODEC = AssetBuilderCodec
            .builder(
                    NPCQualityRuleAsset.class,
                    NPCQualityRuleAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .append(new KeyedCodec<>("Description", Codec.STRING), (asset, value) -> asset.description = value != null ? value : "", asset -> asset.description)
            .add()
            .append(new KeyedCodec<>("Weights", INT_MAP_CODEC), (asset, value) -> asset.weights = value != null ? value : new HashMap<>(), asset -> asset.weights)
            .add()
            .append(new KeyedCodec<>("StatMultipliers", DOUBLE_MAP_CODEC), (asset, value) -> asset.statMultipliers = value != null ? value : new HashMap<>(), asset -> asset.statMultipliers)
            .add()
            .append(new KeyedCodec<>("LootQualityBonus", INT_MAP_CODEC), (asset, value) -> asset.lootQualityBonus = value != null ? value : new HashMap<>(), asset -> asset.lootQualityBonus)
            .add()
            .build();

    private static AssetStore<String, NPCQualityRuleAsset, IndexedLookupTableAssetMap<String, NPCQualityRuleAsset>> ASSET_STORE;

    private String id;
    private AssetExtraInfo.Data data;
    private String description = "";
    private Map<String, Integer> weights = new HashMap<>();
    private Map<String, Double> statMultipliers = new HashMap<>();
    private Map<String, Integer> lootQualityBonus = new HashMap<>();

    public NPCQualityRuleAsset() {}

    @Nonnull
    public static AssetStore<String, NPCQualityRuleAsset, IndexedLookupTableAssetMap<String, NPCQualityRuleAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(NPCQualityRuleAsset.class);
        }
        return ASSET_STORE;
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    public NPCQualityRule toRule() {
        return new NPCQualityRule(id, description, weights, statMultipliers, lootQualityBonus);
    }
}
