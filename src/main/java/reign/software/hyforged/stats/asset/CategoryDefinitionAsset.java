package reign.software.hyforged.stats.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import reign.software.hyforged.stats.CategoryDefinition;

import javax.annotation.Nonnull;

/**
 * JSON asset definition for Hyforged categories.
 * <p>
 * This allows mods to define categories via JSON files in their asset packs.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Id": "ability-score",
 *   "DisplayName": "Ability Scores",
 *   "Description": "Primary character attributes",
 *   "SortOrder": 0
 * }
 * </pre>
 */
public class CategoryDefinitionAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, CategoryDefinitionAsset>> {

    /**
     * Codec for loading CategoryDefinitionAsset from JSON.
     */
    public static final AssetBuilderCodec<String, CategoryDefinitionAsset> CODEC = AssetBuilderCodec
            .builder(
                    CategoryDefinitionAsset.class,
                    CategoryDefinitionAsset::new,
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
            .appendInherited(
                    new KeyedCodec<>("DisplayName", Codec.STRING),
                    (asset, value) -> asset.displayName = value,
                    asset -> asset.displayName,
                    (asset, parent) -> asset.displayName = parent.displayName
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Description", Codec.STRING),
                    (asset, value) -> asset.description = value,
                    asset -> asset.description,
                    (asset, parent) -> asset.description = parent.description
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("SortOrder", Codec.INTEGER),
                    (asset, value) -> asset.sortOrder = value != null ? value : 0,
                    asset -> asset.sortOrder,
                    (asset, parent) -> asset.sortOrder = parent.sortOrder
            )
            .add()
            .build();

    private static AssetStore<String, CategoryDefinitionAsset, IndexedLookupTableAssetMap<String, CategoryDefinitionAsset>> ASSET_STORE;

    // Asset data
    private String id;
    private AssetExtraInfo.Data data;

    // Category definition fields
    private String displayName = "";
    private String description = "";
    private int sortOrder = 0;

    public CategoryDefinitionAsset() {
    }

    /**
     * Get the asset store for category definitions.
     */
    @Nonnull
    public static AssetStore<String, CategoryDefinitionAsset, IndexedLookupTableAssetMap<String, CategoryDefinitionAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(CategoryDefinitionAsset.class);
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
    public String getDisplayName() {
        return displayName;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * Convert this asset to a CategoryDefinition for registration.
     */
    @Nonnull
    public CategoryDefinition toCategoryDefinition() {
        return new CategoryDefinition.Builder(id)
                .displayName(displayName)
                .description(description)
                .sortOrder(sortOrder)
                .build();
    }
}
