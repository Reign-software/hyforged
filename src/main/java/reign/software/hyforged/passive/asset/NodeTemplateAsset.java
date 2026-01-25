package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Asset definition for a node template.
 * <p>
 * Node templates define what a node does (effects, type, name) without position.
 * Position is defined separately in layout files.
 * <p>
 * Loaded from JSON files in Server/&lt;Mod&gt;/PassiveTrees/nodes/.
 */
public class NodeTemplateAsset {

    public static final ArrayCodec<NodeTemplateAsset> ARRAY_CODEC =
        new ArrayCodec<>(NodeTemplateAsset.CODEC, NodeTemplateAsset[]::new);

    public static final BuilderCodec<NodeTemplateAsset> CODEC = BuilderCodec.builder(
            NodeTemplateAsset.class,
            NodeTemplateAsset::new
        )
        .append(
            new KeyedCodec<>("Id", Codec.STRING),
            (asset, value) -> asset.id = value,
            asset -> asset.id
        )
        .add()
        .append(
            new KeyedCodec<>("Type", Codec.STRING),
            (asset, value) -> asset.type = value,
            asset -> asset.type
        )
        .add()
        .append(
            new KeyedCodec<>("Name", Codec.STRING),
            (asset, value) -> asset.name = value,
            asset -> asset.name
        )
        .add()
        .append(
            new KeyedCodec<>("Description", Codec.STRING),
            (asset, value) -> asset.description = value,
            asset -> asset.description
        )
        .add()
        .append(
            new KeyedCodec<>("Icon", Codec.STRING),
            (asset, value) -> asset.icon = value,
            asset -> asset.icon
        )
        .add()
        .append(
            new KeyedCodec<>("Effects", PassiveNodeEffectAsset.ARRAY_CODEC),
            (asset, value) -> asset.effects = value != null ? Arrays.asList(value) : null,
            asset -> asset.effects != null ? asset.effects.toArray(new PassiveNodeEffectAsset[0]) : null
        )
        .add()
        .append(
            new KeyedCodec<>("KeystoneFamily", Codec.STRING),
            (asset, value) -> asset.keystoneFamily = value,
            asset -> asset.keystoneFamily
        )
        .add()
        .build();

    private String id;
    private String type;
    private String name;
    private String description;
    private String icon;
    private List<PassiveNodeEffectAsset> effects;
    private String keystoneFamily;

    public NodeTemplateAsset() {
        // Required for codec
    }

    @Nonnull
    public String getId() {
        return id != null ? id : "";
    }

    @Nonnull
    public String getType() {
        return type != null ? type : "minor";
    }

    @Nonnull
    public String getName() {
        return name != null ? name : "";
    }

    @Nonnull
    public String getDescription() {
        return description != null ? description : "";
    }

    @Nullable
    public String getIcon() {
        return icon;
    }

    @Nonnull
    public List<PassiveNodeEffectAsset> getEffects() {
        return effects != null ? effects : new ArrayList<>();
    }

    @Nullable
    public String getKeystoneFamily() {
        return keystoneFamily;
    }
}
