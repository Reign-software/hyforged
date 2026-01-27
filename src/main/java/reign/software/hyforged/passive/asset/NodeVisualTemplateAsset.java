package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import reign.software.hyforged.passive.model.NodeVisualTemplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Asset for loading node visual templates (frames) from JSON.
 * <p>
 * Loaded from Server/&lt;Mod&gt;/PassiveTrees/templates/frame-templates.json
 */
public class NodeVisualTemplateAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, NodeVisualTemplateAsset>> {

    /**
     * Codec for a single frame template entry.
     */
    public static final BuilderCodec<FrameTemplateEntry> FRAME_ENTRY_CODEC = BuilderCodec.builder(
            FrameTemplateEntry.class,
            FrameTemplateEntry::new
        )
        .append(
            new KeyedCodec<>("Id", Codec.STRING),
            (e, v) -> e.id = v,
            e -> e.id
        )
        .add()
        .append(
            new KeyedCodec<>("Size", Codec.INTEGER),
            (e, v) -> e.size = v != null ? v : 24,
            e -> e.size
        )
        .add()
        .append(
            new KeyedCodec<>("AllocatedTexture", Codec.STRING),
            (e, v) -> e.allocatedTexture = v,
            e -> e.allocatedTexture
        )
        .add()
        .append(
            new KeyedCodec<>("AvailableTexture", Codec.STRING),
            (e, v) -> e.availableTexture = v,
            e -> e.availableTexture
        )
        .add()
        .append(
            new KeyedCodec<>("LockedTexture", Codec.STRING),
            (e, v) -> e.lockedTexture = v,
            e -> e.lockedTexture
        )
        .add()
        .build();

    public static final ArrayCodec<FrameTemplateEntry> FRAME_ARRAY_CODEC =
        new ArrayCodec<>(FRAME_ENTRY_CODEC, FrameTemplateEntry[]::new);

    /**
     * Main codec for the template file.
     */
    public static final AssetBuilderCodec<String, NodeVisualTemplateAsset> CODEC = AssetBuilderCodec
        .builder(
            NodeVisualTemplateAsset.class,
            NodeVisualTemplateAsset::new,
            Codec.STRING,
            (asset, id) -> asset.id = id,
            asset -> asset.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data
        )
        .append(
            new KeyedCodec<>("FrameTemplates", FRAME_ARRAY_CODEC),
            (a, v) -> a.frameTemplates = v != null ? Arrays.asList(v) : new ArrayList<>(),
            a -> a.frameTemplates != null ? a.frameTemplates.toArray(new FrameTemplateEntry[0]) : null
        )
        .add()
        .append(
            new KeyedCodec<>("TypeDefaults", new MapCodec<>(Codec.STRING, HashMap::new)),
            (a, v) -> a.typeDefaults = v != null ? v : new HashMap<>(),
            a -> a.typeDefaults
        )
        .add()
        .build();

    private String id;
    private AssetExtraInfo.Data data;
    private List<FrameTemplateEntry> frameTemplates = new ArrayList<>();
    private Map<String, String> typeDefaults = new HashMap<>();

    public NodeVisualTemplateAsset() {
    }

    @Override
    @Nonnull
    public String getId() {
        return id != null ? id : "frame-templates";
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    @Nonnull
    public List<FrameTemplateEntry> getFrameTemplates() {
        return frameTemplates != null ? frameTemplates : Collections.emptyList();
    }

    @Nonnull
    public Map<String, String> getTypeDefaults() {
        return typeDefaults != null ? typeDefaults : Collections.emptyMap();
    }

    /**
     * Convert a frame entry to a NodeVisualTemplate.
     */
    @Nullable
    public static NodeVisualTemplate toTemplate(@Nonnull FrameTemplateEntry entry) {
        if (entry.id == null || entry.id.isEmpty()) {
            return null;
        }
        return new NodeVisualTemplate(
            entry.id,
            entry.size,
            entry.allocatedTexture != null ? entry.allocatedTexture : "",
            entry.availableTexture != null ? entry.availableTexture : "",
            entry.lockedTexture != null ? entry.lockedTexture : ""
        );
    }

    /**
     * Inner class for frame template entries.
     */
    public static class FrameTemplateEntry {
        String id;
        int size = 24;
        String allocatedTexture;
        String availableTexture;
        String lockedTexture;

        public FrameTemplateEntry() {
        }
    }
}
