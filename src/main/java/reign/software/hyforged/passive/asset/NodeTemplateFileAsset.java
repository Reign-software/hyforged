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
import java.util.Arrays;
import java.util.List;

/**
 * Asset file containing an array of node templates.
 * <p>
 * Loaded from JSON files in Server/&lt;Mod&gt;/PassiveTrees/nodes/.
 * <p>
 * Example JSON:
 * <pre>
 * {
 *   "Nodes": [
 *     { "Id": "yourmod:strength-5", "Type": "minor", "Name": "Strength", ... },
 *     { "Id": "yourmod:strength-10", "Type": "minor", "Name": "Strength", ... }
 *   ]
 * }
 * </pre>
 */
public class NodeTemplateFileAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, NodeTemplateFileAsset>> {

    public static final AssetBuilderCodec<String, NodeTemplateFileAsset> CODEC = AssetBuilderCodec
        .builder(
            NodeTemplateFileAsset.class,
            NodeTemplateFileAsset::new,
            Codec.STRING,
            (asset, id) -> asset.id = id,
            asset -> asset.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data
        )
        .append(
            new KeyedCodec<>("Nodes", NodeTemplateAsset.ARRAY_CODEC),
            (asset, value) -> asset.nodes = value != null ? Arrays.asList(value) : null,
            asset -> asset.nodes != null ? asset.nodes.toArray(new NodeTemplateAsset[0]) : null
        )
        .add()
        .build();

    private static AssetStore<String, NodeTemplateFileAsset, IndexedLookupTableAssetMap<String, NodeTemplateFileAsset>> ASSET_STORE;

    private String id;
    private List<NodeTemplateAsset> nodes;
    private AssetExtraInfo.Data data;

    public NodeTemplateFileAsset() {
        // Required for codec
    }

    /**
     * Get the asset store for node template files.
     */
    @Nonnull
    public static AssetStore<String, NodeTemplateFileAsset, IndexedLookupTableAssetMap<String, NodeTemplateFileAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(NodeTemplateFileAsset.class);
        }
        return ASSET_STORE;
    }

    @Nonnull
    @Override
    public String getId() {
        return id != null ? id : "";
    }

    @Nonnull
    public List<NodeTemplateAsset> getNodes() {
        return nodes != null ? nodes : List.of();
    }
}
