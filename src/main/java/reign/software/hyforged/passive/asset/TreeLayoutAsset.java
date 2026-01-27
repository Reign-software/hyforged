package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * Asset file for tree layout definitions.
 * <p>
 * Layout files define where nodes are placed and how they connect.
 * Multiple layout files can contribute to the same tree (additive).
 * <p>
 * Loaded from JSON files in Server/&lt;Mod&gt;/PassiveTrees/layouts/.
 * <p>
 * Example JSON:
 * <pre>
 * {
 *   "TreeId": "hyforged:passive-tree-general",
 *   "Placements": [
 *     { "NodeId": "yourmod:strength-5", "Position": { "X": 0, "Y": 80 }, "Region": "strength" },
 *     { "NodeId": "yourmod:strength-5", "Position": { "X": 20, "Y": 80 }, "InstanceId": "yourmod:str-b" }
 *   ],
 *   "Connections": [
 *     { "From": "hyforged:start-strength", "To": "yourmod:strength-5" },
 *     { "From": "yourmod:strength-5", "To": "yourmod:str-b" }
 *   ],
 *   "StartingNodes": ["yourmod:custom-start"],
 *   "TextLabels": [
 *     { "Text": "STRENGTH", "Position": { "X": -200, "Y": 20 }, "FontSize": 16, "Color": "#FFCC00" }
 *   ]
 * }
 * </pre>
 */
public class TreeLayoutAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, TreeLayoutAsset>> {

    private static final ArrayCodec<PassiveConnectionAsset> CONNECTION_ARRAY_CODEC =
        new ArrayCodec<>(PassiveConnectionAsset.CODEC, PassiveConnectionAsset[]::new);

    public static final AssetBuilderCodec<String, TreeLayoutAsset> CODEC = AssetBuilderCodec
        .builder(
            TreeLayoutAsset.class,
            TreeLayoutAsset::new,
            Codec.STRING,
            (asset, id) -> asset.id = id,
            asset -> asset.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data
        )
        .append(
            new KeyedCodec<>("TreeId", Codec.STRING),
            (asset, value) -> asset.treeId = value,
            asset -> asset.treeId
        )
        .add()
        .append(
            new KeyedCodec<>("Placements", NodePlacementAsset.ARRAY_CODEC),
            (asset, value) -> asset.placements = value != null ? Arrays.asList(value) : null,
            asset -> asset.placements != null ? asset.placements.toArray(new NodePlacementAsset[0]) : null
        )
        .add()
        .append(
            new KeyedCodec<>("Connections", CONNECTION_ARRAY_CODEC),
            (asset, value) -> asset.connections = value != null ? Arrays.asList(value) : null,
            asset -> asset.connections != null ? asset.connections.toArray(new PassiveConnectionAsset[0]) : null
        )
        .add()
        .append(
            new KeyedCodec<>("StartingNodes", Codec.STRING_ARRAY),
            (asset, value) -> asset.startingNodes = value != null ? Arrays.asList(value) : null,
            asset -> asset.startingNodes != null ? asset.startingNodes.toArray(new String[0]) : null
        )
        .add()
        .append(
            new KeyedCodec<>("TextLabels", TextLabelAsset.ARRAY_CODEC),
            (asset, value) -> asset.textLabels = value != null ? Arrays.asList(value) : null,
            asset -> asset.textLabels != null ? asset.textLabels.toArray(new TextLabelAsset[0]) : null
        )
        .add()
        .build();

    private static AssetStore<String, TreeLayoutAsset, IndexedLookupTableAssetMap<String, TreeLayoutAsset>> ASSET_STORE;

    private String id;
    private String treeId;
    private List<NodePlacementAsset> placements;
    private List<PassiveConnectionAsset> connections;
    private List<String> startingNodes;
    private List<TextLabelAsset> textLabels;
    private AssetExtraInfo.Data data;

    public TreeLayoutAsset() {
        // Required for codec
    }

    /**
     * Get the asset store for tree layout files.
     */
    @Nonnull
    public static AssetStore<String, TreeLayoutAsset, IndexedLookupTableAssetMap<String, TreeLayoutAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(TreeLayoutAsset.class);
        }
        return ASSET_STORE;
    }

    /**
     * Get the file ID (auto-generated from path).
     */
    @Nonnull
    @Override
    public String getId() {
        return id != null ? id : "";
    }

    /**
     * Get the tree ID this layout contributes to.
     */
    @Nonnull
    public String getTreeId() {
        return treeId != null ? treeId : "";
    }

    /**
     * Get the node placements in this layout.
     */
    @Nonnull
    public List<NodePlacementAsset> getPlacements() {
        return placements != null ? placements : List.of();
    }

    /**
     * Get the connections defined in this layout.
     */
    @Nonnull
    public List<PassiveConnectionAsset> getConnections() {
        return connections != null ? connections : List.of();
    }

    /**
     * Get the starting nodes defined in this layout.
     */
    @Nonnull
    public List<String> getStartingNodes() {
        return startingNodes != null ? startingNodes : List.of();
    }

    /**
     * Get the text labels defined in this layout.
     */
    @Nonnull
    public List<TextLabelAsset> getTextLabels() {
        return textLabels != null ? textLabels : List.of();
    }
}
