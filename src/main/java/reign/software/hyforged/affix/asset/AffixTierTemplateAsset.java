package reign.software.hyforged.affix.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import reign.software.hyforged.affix.model.AffixTierTemplate;

import javax.annotation.Nonnull;

/**
 * Codec for loading AffixTierTemplate from JSON.
 * <p>
 * Used within StatDefinitionAsset to load optional tier templates.
 * <p>
 * JSON format:
 * <pre>
 * {
 *   "TierCount": 5,
 *   "T1Range": [8, 10],
 *   "T5Range": [1, 2],
 *   "ScalingCurve": "linear"
 * }
 * </pre>
 */
public class AffixTierTemplateAsset {
    
    /**
     * Codec for loading from JSON.
     */
    public static final BuilderCodec<AffixTierTemplateAsset> CODEC = BuilderCodec
            .builder(AffixTierTemplateAsset.class, AffixTierTemplateAsset::new)
            .append(
                    new KeyedCodec<>("TierCount", Codec.INTEGER),
                    (asset, value) -> asset.tierCount = value != null ? value : 5,
                    asset -> asset.tierCount
            )
            .add()
            .append(
                    new KeyedCodec<>("T1Range", Codec.INT_ARRAY),
                    (asset, value) -> asset.t1Range = value != null && value.length >= 2 ? value : new int[]{10, 15},
                    asset -> asset.t1Range
            )
            .add()
            .append(
                    new KeyedCodec<>("T5Range", Codec.INT_ARRAY),
                    (asset, value) -> asset.t5Range = value != null && value.length >= 2 ? value : new int[]{1, 3},
                    asset -> asset.t5Range
            )
            .add()
            .append(
                    new KeyedCodec<>("ScalingCurve", Codec.STRING),
                    (asset, value) -> asset.scalingCurve = value != null ? value : "linear",
                    asset -> asset.scalingCurve
            )
            .add()
            .build();
    
    private int tierCount = 5;
    private int[] t1Range = new int[]{10, 15};
    private int[] t5Range = new int[]{1, 3};
    private String scalingCurve = "linear";
    
    public AffixTierTemplateAsset() {}
    
    public int getTierCount() {
        return tierCount;
    }
    
    public int[] getT1Range() {
        return t1Range;
    }
    
    public int[] getT5Range() {
        return t5Range;
    }
    
    public String getScalingCurve() {
        return scalingCurve;
    }
    
    /**
     * Convert this asset to an AffixTierTemplate model.
     *
     * @return The tier template model
     */
    @Nonnull
    public AffixTierTemplate toModel() {
        AffixTierTemplate.ScalingCurve curve = switch (scalingCurve.toLowerCase()) {
            case "exponential", "exp" -> AffixTierTemplate.ScalingCurve.EXPONENTIAL;
            case "logarithmic", "log" -> AffixTierTemplate.ScalingCurve.LOGARITHMIC;
            default -> AffixTierTemplate.ScalingCurve.LINEAR;
        };
        
        return new AffixTierTemplate(
                tierCount,
                t1Range[0],
                t1Range.length > 1 ? t1Range[1] : t1Range[0],
                t5Range[0],
                t5Range.length > 1 ? t5Range[1] : t5Range[0],
                curve
        );
    }
}
