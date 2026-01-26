package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Asset definition for a node placement in a layout file.
 * <p>
 * Placements define where a node template is placed in a tree
 * and optionally provide an instance ID for reuse.
 */
public class NodePlacementAsset {

    public static final BuilderCodec<NodePlacementAsset> CODEC = BuilderCodec.builder(
            NodePlacementAsset.class,
            NodePlacementAsset::new
        )
        .append(
            new KeyedCodec<>("NodeId", Codec.STRING),
            (asset, value) -> asset.nodeId = value,
            asset -> asset.nodeId
        )
        .add()
        .append(
            new KeyedCodec<>("Position", PassiveNodePositionAsset.CODEC),
            (asset, value) -> asset.position = value,
            asset -> asset.position
        )
        .add()
        .append(
            new KeyedCodec<>("Region", Codec.STRING),
            (asset, value) -> asset.region = value,
            asset -> asset.region
        )
        .add()
        .append(
            new KeyedCodec<>("InstanceId", Codec.STRING),
            (asset, value) -> asset.instanceId = value,
            asset -> asset.instanceId
        )
        .add()
        .append(
            new KeyedCodec<>("IsStarting", Codec.BOOLEAN),
            (asset, value) -> asset.isStarting = value != null && value,
            asset -> asset.isStarting ? asset.isStarting : null
        )
        .add()
        .build();

    public static final ArrayCodec<NodePlacementAsset> ARRAY_CODEC =
        new ArrayCodec<>(CODEC, NodePlacementAsset[]::new);

    private String nodeId;
    private PassiveNodePositionAsset position;
    private String region;
    private String instanceId;
    private boolean isStarting;

    public NodePlacementAsset() {
        // Required for codec
    }

    /**
     * Get the node template ID this placement references.
     */
    @Nonnull
    public String getNodeId() {
        return nodeId != null ? nodeId : "";
    }

    /**
     * Get the position in the tree.
     */
    @Nullable
    public PassiveNodePositionAsset getPosition() {
        return position;
    }

    /**
     * Get the region this placement belongs to.
     */
    @Nullable
    public String getRegion() {
        return region;
    }

    /**
     * Get the instance ID for this placement.
     * <p>
     * When placing the same node template multiple times, use InstanceId
     * to give each placement a unique ID for connections.
     * <p>
     * If null, the NodeId is used as the placement ID.
     */
    @Nullable
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Get the effective ID for this placement.
     * Returns InstanceId if set, otherwise NodeId.
     */
    @Nonnull
    public String getEffectiveId() {
        return instanceId != null && !instanceId.isBlank() ? instanceId : getNodeId();
    }

    /**
     * Check if this placement marks the node as a starting node.
     * <p>
     * Starting nodes are entry points into the tree that players can allocate
     * without any prerequisites. This is an alternative to listing node IDs
     * in the layout's StartingNodes array.
     */
    public boolean isStarting() {
        return isStarting;
    }
}
