package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Asset definition for a text label in a layout file.
 * <p>
 * Text labels allow placing static text elements on the passive tree canvas
 * for region headers, decorative text, or informational labels.
 * <p>
 * Example JSON:
 * <pre>
 * {
 *   "Text": "STRENGTH",
 *   "Position": { "X": -200, "Y": 20 },
 *   "FontSize": 16,
 *   "Color": "#FFCC00",
 *   "Anchor": "center",
 *   "Region": "strength"
 * }
 * </pre>
 */
public class TextLabelAsset {

    public static final BuilderCodec<TextLabelAsset> CODEC = BuilderCodec.builder(
            TextLabelAsset.class,
            TextLabelAsset::new
        )
        .append(
            new KeyedCodec<>("Text", Codec.STRING),
            (asset, value) -> asset.text = value,
            asset -> asset.text
        )
        .add()
        .append(
            new KeyedCodec<>("Position", PassiveNodePositionAsset.CODEC),
            (asset, value) -> asset.position = value,
            asset -> asset.position
        )
        .add()
        .append(
            new KeyedCodec<>("FontSize", Codec.INTEGER),
            (asset, value) -> asset.fontSize = value != null ? value : 14,
            asset -> asset.fontSize != 14 ? asset.fontSize : null
        )
        .add()
        .append(
            new KeyedCodec<>("Color", Codec.STRING),
            (asset, value) -> asset.color = value,
            asset -> asset.color
        )
        .add()
        .append(
            new KeyedCodec<>("Anchor", Codec.STRING),
            (asset, value) -> asset.anchor = value,
            asset -> asset.anchor
        )
        .add()
        .append(
            new KeyedCodec<>("Region", Codec.STRING),
            (asset, value) -> asset.region = value,
            asset -> asset.region
        )
        .add()
        .append(
            new KeyedCodec<>("FontWeight", Codec.STRING),
            (asset, value) -> asset.fontWeight = value,
            asset -> asset.fontWeight
        )
        .add()
        .append(
            new KeyedCodec<>("Opacity", Codec.FLOAT),
            (asset, value) -> asset.opacity = value != null ? value : 1.0f,
            asset -> asset.opacity != 1.0f ? asset.opacity : null
        )
        .add()
        .append(
            new KeyedCodec<>("Rotation", Codec.FLOAT),
            (asset, value) -> asset.rotation = value != null ? value : 0.0f,
            asset -> asset.rotation != 0.0f ? asset.rotation : null
        )
        .add()
        .build();

    public static final ArrayCodec<TextLabelAsset> ARRAY_CODEC =
        new ArrayCodec<>(CODEC, TextLabelAsset[]::new);

    private String text;
    private PassiveNodePositionAsset position;
    private int fontSize = 14;
    private String color;
    private String anchor;
    private String region;
    private String fontWeight;
    private float opacity = 1.0f;
    private float rotation = 0.0f;

    public TextLabelAsset() {
        // Required for codec
    }

    /**
     * Get the text content to display.
     */
    @Nonnull
    public String getText() {
        return text != null ? text : "";
    }

    /**
     * Get the position in tree coordinates.
     */
    @Nullable
    public PassiveNodePositionAsset getPosition() {
        return position;
    }

    /**
     * Get the font size in pixels.
     * Default: 14
     */
    public int getFontSize() {
        return fontSize;
    }

    /**
     * Get the text color.
     * Supports hex colors (e.g., "#FFCC00") or named colors.
     * Default: white
     */
    @Nonnull
    public String getColor() {
        return color != null ? color : "#FFFFFF";
    }

    /**
     * Get the text anchor/alignment.
     * Values: "left", "center", "right"
     * Default: "center"
     */
    @Nonnull
    public String getAnchor() {
        return anchor != null ? anchor : "center";
    }

    /**
     * Get the region this label belongs to (for filtering/grouping).
     */
    @Nullable
    public String getRegion() {
        return region;
    }

    /**
     * Get the font weight.
     * Values: "normal", "bold"
     * Default: "normal"
     */
    @Nonnull
    public String getFontWeight() {
        return fontWeight != null ? fontWeight : "normal";
    }

    /**
     * Get the opacity (0.0 to 1.0).
     * Default: 1.0
     */
    public float getOpacity() {
        return opacity;
    }

    /**
     * Get the rotation in degrees.
     * Default: 0.0
     */
    public float getRotation() {
        return rotation;
    }
}
