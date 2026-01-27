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
 * Visual appearance (frame, icon) references centralized templates by ID.
 * <p>
 * Loaded from JSON files in Server/&lt;Mod&gt;/PassiveTrees/nodes/.
 */
public class NodeTemplateAsset {

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
            new KeyedCodec<>("FrameTemplate", Codec.STRING),
            (asset, value) -> asset.frameTemplate = value,
            asset -> asset.frameTemplate
        )
        .add()
        .append(
            new KeyedCodec<>("Icon", Codec.STRING),
            (asset, value) -> asset.icon = value,
            asset -> asset.icon
        )
        .add()
        .append(
            new KeyedCodec<>("Label", Codec.STRING),
            (asset, value) -> asset.label = value,
            asset -> asset.label
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

    public static final ArrayCodec<NodeTemplateAsset> ARRAY_CODEC =
        new ArrayCodec<>(CODEC, NodeTemplateAsset[]::new);

    private String id;
    private String type;
    private String name;
    private String description;
    private String frameTemplate;
    private String icon;
    private String label;
    private List<PassiveNodeEffectAsset> effects;
    private String keystoneFamily;
    private boolean placeholder;

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
    public String getFrameTemplate() {
        return frameTemplate;
    }

    @Nullable
    public String getIcon() {
        return icon;
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    @Nonnull
    public List<PassiveNodeEffectAsset> getEffects() {
        return effects != null ? effects : new ArrayList<>();
    }

    @Nullable
    public String getKeystoneFamily() {
        return keystoneFamily;
    }

    public boolean isPlaceholder() {
        return placeholder;
    }

    @Nonnull
    public static NodeTemplateAsset createPlaceholder(@Nonnull String id) {
        NodeTemplateAsset asset = new NodeTemplateAsset();
        asset.id = id;

        String slug = id;
        int namespaceIndex = id.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex + 1 < id.length()) {
            slug = id.substring(namespaceIndex + 1);
        }

        asset.type = slug.startsWith("minor-") ? "minor" : "notable";
        asset.name = toTitleCase(slug);
        asset.description = "";
        asset.effects = new ArrayList<>();
        asset.placeholder = true;

        return asset;
    }

    @Nonnull
    private static String toTitleCase(@Nonnull String slug) {
        String[] parts = slug.split("-");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.toString();
    }
}
