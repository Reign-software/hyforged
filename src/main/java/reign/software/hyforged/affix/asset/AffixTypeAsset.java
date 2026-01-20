package reign.software.hyforged.affix.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import reign.software.hyforged.affix.model.AffixType;

import javax.annotation.Nonnull;

/**
 * JSON asset definition for affix types.
 * <p>
 * Affix types define how affixes behave and are displayed.
 * Loaded from {@code Server/Hyforged/AffixTypes/*.json}.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Id": "prefix",
 *   "DisplayNamePosition": "before",  // "before", "after", or "none"
 *   "DisplayFormat": "{name}",        // Template for tooltip display
 *   "Stackable": true                 // Whether multiple affixes of this type can coexist
 * }
 * </pre>
 */
public class AffixTypeAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, AffixTypeAsset>> {

    /**
     * Codec for loading AffixTypeAsset from JSON.
     */
    public static final AssetBuilderCodec<String, AffixTypeAsset> CODEC = AssetBuilderCodec
            .builder(
                    AffixTypeAsset.class,
                    AffixTypeAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .append(
                    new KeyedCodec<>("DisplayNamePosition", Codec.STRING),
                    (asset, value) -> asset.displayNamePosition = value != null ? value : "none",
                    asset -> asset.displayNamePosition
            )
            .add()
            .append(
                    new KeyedCodec<>("DisplayFormat", Codec.STRING),
                    (asset, value) -> asset.displayFormat = value != null ? value : "{name}",
                    asset -> asset.displayFormat
            )
            .add()
            .append(
                    new KeyedCodec<>("Stackable", Codec.BOOLEAN),
                    (asset, value) -> asset.stackable = value != null && value,
                    asset -> asset.stackable
            )
            .add()
            .build();

    private static AssetStore<String, AffixTypeAsset, IndexedLookupTableAssetMap<String, AffixTypeAsset>> ASSET_STORE;

    // Asset data
    private String id;
    private AssetExtraInfo.Data data;

    // Affix type fields
    private String displayNamePosition = "none";
    private String displayFormat = "{name}";
    private boolean stackable = true;

    public AffixTypeAsset() {
    }

    /**
     * Get the asset store for affix type definitions.
     */
    @Nonnull
    public static AssetStore<String, AffixTypeAsset, IndexedLookupTableAssetMap<String, AffixTypeAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(AffixTypeAsset.class);
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
     * Convert this asset to an AffixType model object.
     *
     * @return The AffixType model
     */
    @Nonnull
    public AffixType toAffixType() {
        return new AffixType(
                id,
                AffixType.DisplayNamePosition.fromJson(displayNamePosition),
                displayFormat,
                stackable
        );
    }

    // ========== Accessors ==========

    @Nonnull
    public String getDisplayNamePosition() {
        return displayNamePosition;
    }

    @Nonnull
    public String getDisplayFormat() {
        return displayFormat;
    }

    public boolean isStackable() {
        return stackable;
    }
}
