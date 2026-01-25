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

    public static final ArrayCodec<NodePlacementAsset> ARRAY_CODEC =
        new ArrayCodec<>(NodePlacementAsset.CODEC, NodePlacementAsset[]::new);

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
        .build();

    private String nodeId;
    private PassiveNodePositionAsset position;
    private String region;
    private String instanceId;

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
}
