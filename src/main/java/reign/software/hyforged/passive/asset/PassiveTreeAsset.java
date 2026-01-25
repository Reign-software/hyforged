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
import javax.annotation.Nullable;

/**
 * Asset definition for a passive tree.
 * <p>
 * Tree definitions contain only metadata about the tree (type, class association).
 * Nodes and connections are loaded separately from node template and layout files.
 * <p>
 * Loaded from JSON files in Server/&lt;Mod&gt;/PassiveTrees/trees/.
 * <p>
 * Example JSON:
 * <pre>
 * {
 *   "Id": "hyforged:passive-tree-general",
 *   "TreeType": "general",
 *   "Version": 1
 * }
 * </pre>
 * <p>
 * For class trees:
 * <pre>
 * {
 *   "Id": "yourmod:passive-tree-warrior",
 *   "TreeType": "class",
 *   "ClassId": "yourmod:warrior",
 *   "Version": 1
 * }
 * </pre>
 */
public class PassiveTreeAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, PassiveTreeAsset>> {

    public static final AssetBuilderCodec<String, PassiveTreeAsset> CODEC = AssetBuilderCodec
        .builder(
            PassiveTreeAsset.class,
            PassiveTreeAsset::new,
            Codec.STRING,
            (asset, id) -> { if (asset.id == null || asset.id.isEmpty()) asset.id = id; },
            asset -> asset.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data
        )
        .append(
            new KeyedCodec<>("Id", Codec.STRING),
            (asset, value) -> asset.id = value,
            asset -> asset.id
        )
        .add()
        .append(
            new KeyedCodec<>("TreeType", Codec.STRING),
            (asset, value) -> asset.treeType = value,
            asset -> asset.treeType
        )
        .add()
        .append(
            new KeyedCodec<>("ClassId", Codec.STRING),
            (asset, value) -> asset.classId = value,
            asset -> asset.classId
        )
        .add()
        .append(
            new KeyedCodec<>("Version", Codec.INTEGER),
            (asset, value) -> asset.version = value,
            asset -> asset.version
        )
        .add()
        .build();

    private static AssetStore<String, PassiveTreeAsset, IndexedLookupTableAssetMap<String, PassiveTreeAsset>> ASSET_STORE;

    private String id;
    private String treeType;
    private String classId;
    private Integer version;
    private AssetExtraInfo.Data data;

    public PassiveTreeAsset() {
        // Required for codec
    }

    /**
     * Get the asset store for passive tree definitions.
     */
    @Nonnull
    public static AssetStore<String, PassiveTreeAsset, IndexedLookupTableAssetMap<String, PassiveTreeAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(PassiveTreeAsset.class);
        }
        return ASSET_STORE;
    }

    @Nonnull
    @Override
    public String getId() {
        return id != null ? id : "";
    }

    @Nonnull
    public String getTreeType() {
        return treeType != null ? treeType : "general";
    }

    @Nullable
    public String getClassId() {
        return classId;
    }

    public int getVersion() {
        return version != null ? version : 1;
    }

    /**
     * Check if this is a general tree.
     */
    public boolean isGeneralTree() {
        return "general".equalsIgnoreCase(getTreeType());
    }

    /**
     * Check if this is a class tree.
     */
    public boolean isClassTree() {
        return "class".equalsIgnoreCase(getTreeType());
    }
}
