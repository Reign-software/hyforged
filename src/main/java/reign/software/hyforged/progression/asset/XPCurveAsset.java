package reign.software.hyforged.progression.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import reign.software.hyforged.progression.XPCurve;

import javax.annotation.Nonnull;

/**
 * JSON asset definition for XP curves.
 * <p>
 * XP curves define the experience required for level progression using an exponential formula.
 * Both character (cap 100) and class (cap 20) curves are supported.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Id": "hyforged:character_xp",
 *   "Type": "character",
 *   "BaseXP": 100,
 *   "ExponentFactor": 1.15,
 *   "MaxLevel": 100
 * }
 * </pre>
 * <p>
 * Formula: XP(n) = BaseXP * (ExponentFactor ^ (n-2)) for level n >= 2
 */
public class XPCurveAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, XPCurveAsset>> {

    /**
     * Codec for loading XPCurveAsset from JSON.
     */
    public static final AssetBuilderCodec<String, XPCurveAsset> CODEC = AssetBuilderCodec
            .builder(
                    XPCurveAsset.class,
                    XPCurveAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .appendInherited(
                    new KeyedCodec<>("Type", Codec.STRING),
                    (asset, value) -> asset.type = value != null ? value : "character",
                    asset -> asset.type,
                    (asset, parent) -> asset.type = parent.type
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("BaseXP", Codec.INTEGER),
                    (asset, value) -> asset.baseXp = value != null ? value : XPCurve.DEFAULT_CHARACTER_BASE_XP,
                    asset -> asset.baseXp,
                    (asset, parent) -> asset.baseXp = parent.baseXp
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("ExponentFactor", Codec.DOUBLE),
                    (asset, value) -> asset.exponentFactor = value != null ? value : XPCurve.DEFAULT_EXPONENT_FACTOR,
                    asset -> asset.exponentFactor,
                    (asset, parent) -> asset.exponentFactor = parent.exponentFactor
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("MaxLevel", Codec.INTEGER),
                    (asset, value) -> asset.maxLevel = value != null ? value : 100,
                    asset -> asset.maxLevel,
                    (asset, parent) -> asset.maxLevel = parent.maxLevel
            )
            .add()
            .build();

    // Asset metadata
    private String id;
    private AssetExtraInfo.Data data;
    
    // Curve properties
    private String type = "character";
    private int baseXp = XPCurve.DEFAULT_CHARACTER_BASE_XP;
    private double exponentFactor = XPCurve.DEFAULT_EXPONENT_FACTOR;
    private int maxLevel = 100;

    public XPCurveAsset() {
        // Required for codec
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getType() {
        return type;
    }

    public int getBaseXp() {
        return baseXp;
    }

    public double getExponentFactor() {
        return exponentFactor;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Convert to an XPCurve domain object.
     *
     * @return Immutable XPCurve instance
     */
    @Nonnull
    public XPCurve toXPCurve() {
        return new XPCurve(
            id,
            XPCurve.CurveType.fromString(type),
            baseXp,
            exponentFactor,
            maxLevel
        );
    }
}
