package reign.software.hyforged.affix.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import reign.software.hyforged.affix.model.AffixTierDefinition;

import javax.annotation.Nonnull;

/**
 * Codec for AffixTierDefinition within affix asset JSON.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Tier": 1,
 *   "MinValue": 50,
 *   "MaxValue": 75,
 *   "ItemLevelReq": 40,
 *   "Weight": 100
 * }
 * </pre>
 */
public class AffixTierAsset {

    /**
     * Codec for a single tier definition.
     */
    public static final BuilderCodec<AffixTierAsset> CODEC = BuilderCodec.builder(
                    AffixTierAsset.class,
                    AffixTierAsset::new
            )
            .append(
                    new KeyedCodec<>("Tier", Codec.INTEGER),
                    (asset, value) -> asset.tier = value != null ? value : 1,
                    asset -> asset.tier
            )
            .add()
            .append(
                    new KeyedCodec<>("MinValue", Codec.INTEGER),
                    (asset, value) -> asset.minValue = value != null ? value : 0,
                    asset -> asset.minValue
            )
            .add()
            .append(
                    new KeyedCodec<>("MaxValue", Codec.INTEGER),
                    (asset, value) -> asset.maxValue = value != null ? value : 0,
                    asset -> asset.maxValue
            )
            .add()
            .append(
                    new KeyedCodec<>("ItemLevelReq", Codec.INTEGER),
                    (asset, value) -> asset.itemLevelReq = value != null ? value : 0,
                    asset -> asset.itemLevelReq
            )
            .add()
            .append(
                    new KeyedCodec<>("Weight", Codec.INTEGER),
                    (asset, value) -> {
                        if (value != null) {
                            asset.weight = value;
                            asset.weightExplicitlySet = true;
                        } else {
                            asset.weight = -1; // Will be computed based on tier
                            asset.weightExplicitlySet = false;
                        }
                    },
                    asset -> asset.weightExplicitlySet ? asset.weight : null
            )
            .add()
            .build();

    /**
     * Array codec for multiple tier definitions.
     */
    public static final ArrayCodec<AffixTierAsset> ARRAY_CODEC = new ArrayCodec<>(CODEC, AffixTierAsset[]::new);

    private int tier = 1;
    private int minValue = 0;
    private int maxValue = 0;
    private int itemLevelReq = 0;
    private int weight = -1; // -1 means compute from tier
    private boolean weightExplicitlySet = false;

    public AffixTierAsset() {
    }

    /**
     * Convert this asset to an AffixTierDefinition model object.
     * <p>
     * If weight was not explicitly set in JSON, computes a linear weight based on tier number.
     * This implements a "linear curve favoring lower tier numbers" - T1 (best) is rarer,
     * higher tiers are more common.
     * <p>
     * Linear curve formula: weight = BASE_WEIGHT * tier
     * This gives T1=50, T2=100, T3=150, T4=200, T5=250 (with BASE_WEIGHT=50)
     */
    @Nonnull
    public AffixTierDefinition toTierDefinition() {
        int effectiveWeight;
        if (weightExplicitlySet) {
            effectiveWeight = weight;
        } else {
            // Linear curve: higher tier numbers get higher weights (more common)
            // T1 (best tier) is rarer, T5 (weaker tier) is more common
            effectiveWeight = AffixTierDefinition.LINEAR_WEIGHT_BASE * tier;
        }
        return new AffixTierDefinition(tier, minValue, maxValue, itemLevelReq, effectiveWeight);
    }

    // ========== Accessors ==========

    public int getTier() {
        return tier;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public int getItemLevelReq() {
        return itemLevelReq;
    }

    public int getWeight() {
        return weight;
    }
}
