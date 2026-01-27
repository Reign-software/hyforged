package reign.software.hyforged.hub.resource;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;

/**
 * Asset class for loading a single welcome message from JSON.
 * <p>
 * Each message is a separate file in: Server/Hyforged/WelcomeMessages/
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Id": "hyforged:welcome-header",
 *   "Order": 0,
 *   "Enabled": true,
 *   "Segments": [
 *     { "Text": "Hello", "Color": "#FFFFFF" },
 *     { "Text": " World", "Color": "#55FF55" }
 *   ]
 * }
 * </pre>
 */
public class WelcomeMessagesConfigAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, WelcomeMessagesConfigAsset>> {

    /**
     * A single text segment with optional color.
     */
    public static class MessageSegment {
        private String text = "";
        private String color = null;

        public MessageSegment() {
        }

        public MessageSegment(@Nonnull String text, String color) {
            this.text = text;
            this.color = color;
        }

        @Nonnull
        public String getText() {
            return text != null ? text : "";
        }

        public String getColor() {
            return color;
        }
    }

    /** Codec for a single segment */
    private static final BuilderCodec<MessageSegment> SEGMENT_CODEC = BuilderCodec
            .builder(MessageSegment.class, MessageSegment::new)
            .append(
                    new KeyedCodec<>("Text", Codec.STRING),
                    (seg, value) -> seg.text = value != null ? value : "",
                    seg -> seg.text
            )
            .add()
            .append(
                    new KeyedCodec<>("Color", Codec.STRING),
                    (seg, value) -> seg.color = value,
                    seg -> seg.color
            )
            .add()
            .build();

    /** Array codec for segments */
    private static final ArrayCodec<MessageSegment> SEGMENTS_ARRAY_CODEC =
            new ArrayCodec<>(SEGMENT_CODEC, MessageSegment[]::new);

    public static final AssetBuilderCodec<String, WelcomeMessagesConfigAsset> CODEC = AssetBuilderCodec
            .builder(
                    WelcomeMessagesConfigAsset.class,
                    WelcomeMessagesConfigAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .appendInherited(
                    new KeyedCodec<>("Order", Codec.INTEGER),
                    (asset, value) -> asset.order = value != null ? value : 0,
                    asset -> asset.order,
                    (asset, parent) -> asset.order = parent.order
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                    (asset, value) -> asset.enabled = value != null ? value : true,
                    asset -> asset.enabled,
                    (asset, parent) -> asset.enabled = parent.enabled
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Segments", SEGMENTS_ARRAY_CODEC),
                    (asset, value) -> asset.segments = value != null ? value : new MessageSegment[0],
                    asset -> asset.segments,
                    (asset, parent) -> asset.segments = parent.segments.clone()
            )
            .add()
            .build();

    private String id;
    private AssetExtraInfo.Data data;
    private int order = 0;
    private boolean enabled = true;
    private MessageSegment[] segments = new MessageSegment[0];

    public WelcomeMessagesConfigAsset() {
        // Required for codec
    }

    @Nonnull
    public String getId() {
        return id != null ? id : "";
    }

    public int getOrder() {
        return order;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Nonnull
    public MessageSegment[] getSegments() {
        return segments != null ? segments.clone() : new MessageSegment[0];
    }
}
