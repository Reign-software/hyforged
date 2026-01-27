package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import reign.software.hyforged.passive.model.NodeIconTemplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Asset for loading node icon templates from JSON.
 * <p>
 * Loaded from Server/&lt;Mod&gt;/PassiveTrees/templates/icon-templates.json
 */
public class NodeIconTemplateAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, NodeIconTemplateAsset>> {

    /**
     * Codec for a single icon template entry.
     */
    public static final BuilderCodec<IconTemplateEntry> ICON_ENTRY_CODEC = BuilderCodec.builder(
            IconTemplateEntry.class,
            IconTemplateEntry::new
        )
        .append(
            new KeyedCodec<>("Id", Codec.STRING),
            (e, v) -> e.id = v,
            e -> e.id
        )
        .add()
        .append(
            new KeyedCodec<>("Texture", Codec.STRING),
            (e, v) -> e.texture = v,
            e -> e.texture
        )
        .add()
        .append(
            new KeyedCodec<>("Scale", Codec.FLOAT),
            (e, v) -> e.scale = v != null ? v : 0.8f,
            e -> e.scale
        )
        .add()
        .build();

    public static final ArrayCodec<IconTemplateEntry> ICON_ARRAY_CODEC =
        new ArrayCodec<>(ICON_ENTRY_CODEC, IconTemplateEntry[]::new);

    /**
     * Main codec for the template file.
     */
    public static final AssetBuilderCodec<String, NodeIconTemplateAsset> CODEC = AssetBuilderCodec
        .builder(
            NodeIconTemplateAsset.class,
            NodeIconTemplateAsset::new,
            Codec.STRING,
            (asset, id) -> asset.id = id,
            asset -> asset.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data
        )
        .append(
            new KeyedCodec<>("IconTemplates", ICON_ARRAY_CODEC),
            (a, v) -> a.iconTemplates = v != null ? Arrays.asList(v) : new ArrayList<>(),
            a -> a.iconTemplates != null ? a.iconTemplates.toArray(new IconTemplateEntry[0]) : null
        )
        .add()
        .append(
            new KeyedCodec<>("DefaultScale", Codec.FLOAT),
            (a, v) -> a.defaultScale = v != null ? v : 0.8f,
            a -> a.defaultScale
        )
        .add()
        .build();

    private String id;
    private AssetExtraInfo.Data data;
    private List<IconTemplateEntry> iconTemplates = new ArrayList<>();
    private float defaultScale = 0.8f;

    public NodeIconTemplateAsset() {
    }

    @Override
    @Nonnull
    public String getId() {
        return id != null ? id : "icon-templates";
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    @Nonnull
    public List<IconTemplateEntry> getIconTemplates() {
        return iconTemplates != null ? iconTemplates : Collections.emptyList();
    }

    public float getDefaultScale() {
        return defaultScale;
    }

    /**
     * Convert an icon entry to a NodeIconTemplate.
     */
    @Nullable
    public static NodeIconTemplate toTemplate(@Nonnull IconTemplateEntry entry) {
        if (entry.id == null || entry.id.isEmpty()) {
            return null;
        }
        return new NodeIconTemplate(
            entry.id,
            entry.texture != null ? entry.texture : "",
            entry.scale
        );
    }

    /**
     * Inner class for icon template entries.
     */
    public static class IconTemplateEntry {
        String id;
        String texture;
        float scale = 0.8f;

        public IconTemplateEntry() {
        }
    }
}
