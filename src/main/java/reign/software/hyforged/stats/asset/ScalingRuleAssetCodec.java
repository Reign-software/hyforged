package reign.software.hyforged.stats.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

/**
 * Codec for deserializing {@link ScalingRuleAsset} from JSON.
 * <p>
 * Handles all three scaling types (linear, threshold, diminishing) with their
 * respective fields. Unknown fields are ignored for forward compatibility.
 */
public final class ScalingRuleAssetCodec {
    
    /**
     * Codec for a single ScalingRuleAsset.
     */
    public static final BuilderCodec<ScalingRuleAsset> INSTANCE = BuilderCodec
            .builder(ScalingRuleAsset.class, ScalingRuleAsset::new)
            // Common fields
            .append(
                    new KeyedCodec<>("Type", Codec.STRING),
                    ScalingRuleAsset::setType,
                    ScalingRuleAsset::getType
            ).add()
            .append(
                    new KeyedCodec<>("Source", Codec.STRING),
                    ScalingRuleAsset::setSource,
                    ScalingRuleAsset::getSource
            ).add()
            // Linear scaling fields
            .append(
                    new KeyedCodec<>("Ratio", Codec.DOUBLE),
                    ScalingRuleAsset::setRatio,
                    ScalingRuleAsset::getRatio
            ).add()
            // Threshold scaling fields
            .append(
                    new KeyedCodec<>("PerPoints", Codec.INTEGER),
                    ScalingRuleAsset::setPerPoints,
                    ScalingRuleAsset::getPerPoints
            ).add()
            .append(
                    new KeyedCodec<>("BonusBps", Codec.INTEGER),
                    ScalingRuleAsset::setBonusBps,
                    ScalingRuleAsset::getBonusBps
            ).add()
            // Diminishing scaling fields
            .append(
                    new KeyedCodec<>("Curve", Codec.STRING),
                    ScalingRuleAsset::setCurve,
                    ScalingRuleAsset::getCurve
            ).add()
            .append(
                    new KeyedCodec<>("Scale", Codec.DOUBLE),
                    ScalingRuleAsset::setScale,
                    ScalingRuleAsset::getScale
            ).add()
            .append(
                    new KeyedCodec<>("CapBps", Codec.INTEGER),
                    ScalingRuleAsset::setCapBps,
                    ScalingRuleAsset::getCapBps
            ).add()
            .build();
    
    /**
     * Codec for an array of ScalingRuleAssets.
     */
    public static final Codec<ScalingRuleAsset[]> ARRAY_CODEC = 
            new ArrayCodec<>(INSTANCE, ScalingRuleAsset[]::new);
    
    private ScalingRuleAssetCodec() {
        // Utility class
    }
}
