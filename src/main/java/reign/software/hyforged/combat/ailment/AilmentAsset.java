package reign.software.hyforged.combat.ailment;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * JSON asset for ailment definitions.
 * <p>
 * Ailments define threshold-based status effects that trigger when
 * accumulated elemental damage exceeds a threshold.
 * <p>
 * JSON Schema (place in Server/Hyforged/Combat/Ailments/):
 * <pre>
 * {
 *   "Id": "hyforged:fire-ailment",
 *   "ElementTag": "fire",
 *   "EntityEffectId": "Burn",
 *   "BaseThreshold": 100,
 *   "AccumulationWindowMs": 5000,
 *   "BaseDurationSeconds": 4.0,
 *   "DisplayName": "Ignite",
 *   "Description": "Deals fire damage over time."
 * }
 * </pre>
 */
public class AilmentAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, AilmentAsset>> {

    /**
     * Codec for loading AilmentAsset from JSON.
     */
    public static final AssetBuilderCodec<String, AilmentAsset> CODEC = AssetBuilderCodec
            .builder(
                    AilmentAsset.class,
                    AilmentAsset::new,
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
            .append(
                    new KeyedCodec<>("ElementTag", Codec.STRING),
                    (asset, value) -> asset.elementTag = value,
                    asset -> asset.elementTag
            )
            .add()
            .append(
                    new KeyedCodec<>("EntityEffectId", Codec.STRING),
                    (asset, value) -> asset.entityEffectId = value,
                    asset -> asset.entityEffectId
            )
            .add()
            .append(
                    new KeyedCodec<>("BaseThreshold", Codec.INTEGER),
                    (asset, value) -> asset.baseThreshold = value,
                    asset -> asset.baseThreshold
            )
            .add()
            .append(
                    new KeyedCodec<>("AccumulationWindowMs", Codec.LONG),
                    (asset, value) -> asset.accumulationWindowMs = value,
                    asset -> asset.accumulationWindowMs
            )
            .add()
            .append(
                    new KeyedCodec<>("BaseDurationSeconds", Codec.FLOAT),
                    (asset, value) -> asset.baseDurationSeconds = value,
                    asset -> asset.baseDurationSeconds
            )
            .add()
            .append(
                    new KeyedCodec<>("DisplayName", Codec.STRING),
                    (asset, value) -> asset.displayName = value,
                    asset -> asset.displayName
            )
            .add()
            .append(
                    new KeyedCodec<>("Description", Codec.STRING),
                    (asset, value) -> asset.description = value,
                    asset -> asset.description
            )
            .add()
            .build();

    private static AssetStore<String, AilmentAsset, IndexedLookupTableAssetMap<String, AilmentAsset>> ASSET_STORE;

    // Asset data
    private String id;
    private AssetExtraInfo.Data data;

    // Ailment fields with defaults
    private String elementTag;
    private String entityEffectId;
    private int baseThreshold = 100;
    private long accumulationWindowMs = 5000L;
    private float baseDurationSeconds = 4.0f;
    private String displayName;
    private String description = "";

    public AilmentAsset() {
    }

    /**
     * Get the asset store for ailments.
     */
    @Nonnull
    public static AssetStore<String, AilmentAsset, IndexedLookupTableAssetMap<String, AilmentAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(AilmentAsset.class);
        }
        return ASSET_STORE;
    }

    // ========== JsonAssetWithMap Interface ==========

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    // ========== Accessors ==========

    @Nonnull
    public String getElementTag() {
        return elementTag;
    }

    @Nonnull
    public String getEntityEffectId() {
        return entityEffectId;
    }

    public int getBaseThreshold() {
        return baseThreshold;
    }

    public long getAccumulationWindowMs() {
        return accumulationWindowMs;
    }

    public float getBaseDurationSeconds() {
        return baseDurationSeconds;
    }

    @Nullable
    public String getDisplayName() {
        return displayName != null ? displayName : elementTag;
    }

    @Nonnull
    public String getDescription() {
        return description != null ? description : "";
    }

    /**
     * Convert this asset to an AilmentDefinition.
     */
    @Nonnull
    public AilmentDefinition toDefinition() {
        return AilmentDefinition.builder()
                .id(id)
                .elementTag(elementTag)
                .entityEffectId(entityEffectId)
                .baseThreshold(baseThreshold)
                .accumulationWindowMs(accumulationWindowMs)
                .baseDurationSeconds(baseDurationSeconds)
                .displayName(getDisplayName())
                .description(getDescription())
                .build();
    }

    @Override
    public String toString() {
        return "AilmentAsset{" +
                "id='" + id + '\'' +
                ", elementTag='" + elementTag + '\'' +
                ", entityEffectId='" + entityEffectId + '\'' +
                ", baseThreshold=" + baseThreshold +
                '}';
    }
}
