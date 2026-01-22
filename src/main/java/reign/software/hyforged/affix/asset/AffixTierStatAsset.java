package reign.software.hyforged.affix.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import reign.software.hyforged.affix.model.AffixTierStat;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;

/**
 * Codec for a stat modifier within a tier's Stats map.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "MinValue": 45,
 *   "MaxValue": 55,
 *   "StackType": "FLAT"
 * }
 * </pre>
 * <p>
 * The stat ID comes from the map key, not from this object.
 */
public class AffixTierStatAsset {

    /**
     * Codec for a single tier stat definition.
     */
    public static final BuilderCodec<AffixTierStatAsset> CODEC = BuilderCodec.builder(
                    AffixTierStatAsset.class,
                    AffixTierStatAsset::new
            )
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
                    new KeyedCodec<>("StackType", Codec.STRING),
                    (asset, value) -> asset.stackType = value != null ? value : "FLAT",
                    asset -> asset.stackType
            )
            .add()
            .build();

    private int minValue = 0;
    private int maxValue = 0;
    private String stackType = "FLAT";

    public AffixTierStatAsset() {
    }

    /**
     * Convert this asset to an AffixTierStat model object.
     *
     * @param statId The stat ID from the map key (e.g., "hyforged:strength")
     */
    @Nonnull
    public AffixTierStat toTierStat(@Nonnull String statId) {
        StatId parsedStatId = StatId.parse(statId);
        HyforgedModifier.StackType parsedStackType = parseStackType(stackType);
        return new AffixTierStat(parsedStatId, parsedStackType, minValue, maxValue);
    }

    private static HyforgedModifier.StackType parseStackType(String value) {
        if (value == null || value.isBlank()) {
            return HyforgedModifier.StackType.FLAT;
        }
        try {
            return HyforgedModifier.StackType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid StackType: " + value + ". Valid values: FLAT, INCREASED, MORE, CAP");
        }
    }

    // ========== Accessors ==========

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public String getStackType() {
        return stackType;
    }
}
