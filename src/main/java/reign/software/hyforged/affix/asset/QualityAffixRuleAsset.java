package reign.software.hyforged.affix.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import reign.software.hyforged.affix.model.QualityAffixRule;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON asset definition for quality affix capacity rules.
 * <p>
 * Defines how many affixes of each type an item can have based on its quality.
 * Loaded from {@code Server/Hyforged/Quality/AffixRules/*.json}.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Quality": "Legendary",
 *   "AffixCapacity": {
 *     "prefix": 4,
 *     "suffix": 4,
 *     "forged": 0
 *   }
 * }
 * </pre>
 */
public class QualityAffixRuleAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>> {

    /**
     * Codec for loading QualityAffixRuleAsset from JSON.
     */
    public static final AssetBuilderCodec<String, QualityAffixRuleAsset> CODEC = AssetBuilderCodec
            .builder(
                    QualityAffixRuleAsset.class,
                    QualityAffixRuleAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .append(
                    new KeyedCodec<>("Quality", Codec.STRING),
                    (asset, value) -> asset.quality = value,
                    asset -> asset.quality
            )
            .add()
            .append(
                    new KeyedCodec<>("AffixCapacity", new MapCodec<>(Codec.INTEGER, HashMap::new)),
                    (asset, value) -> asset.affixCapacity = value != null ? value : new HashMap<>(),
                    asset -> asset.affixCapacity
            )
            .add()
            .build();

    private static AssetStore<String, QualityAffixRuleAsset, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>> ASSET_STORE;

    // Asset data
    private String id;
    private AssetExtraInfo.Data data;

    // Quality rule fields
    private String quality = "";
    private Map<String, Integer> affixCapacity = new HashMap<>();

    public QualityAffixRuleAsset() {
    }

    /**
     * Get the asset store for quality affix rule definitions.
     */
    @Nonnull
    public static AssetStore<String, QualityAffixRuleAsset, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(QualityAffixRuleAsset.class);
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
     * Convert this asset to a QualityAffixRule model object.
     *
     * @return The QualityAffixRule model
     */
    @Nonnull
    public QualityAffixRule toQualityAffixRule() {
        // Use the quality field, or fall back to the asset ID if quality is empty
        String qualityId = quality != null && !quality.isEmpty() ? quality : id;
        return new QualityAffixRule(qualityId, affixCapacity);
    }

    // ========== Accessors ==========

    @Nonnull
    public String getQuality() {
        return quality;
    }

    @Nonnull
    public Map<String, Integer> getAffixCapacity() {
        return affixCapacity;
    }
}
