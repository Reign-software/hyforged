package reign.software.hyforged.affix.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import reign.software.hyforged.affix.model.AffixTierDefinition;
import reign.software.hyforged.affix.model.AffixTierStat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Codec for AffixTierDefinition within affix asset JSON.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Tier": 1,
 *   "ItemLevelReq": 70,
 *   "Weight": 35,
 *   "Stats": {
 *     "hyforged:strength": { "MinValue": 45, "MaxValue": 55, "StackType": "FLAT" },
 *     "hyforged:max-health": { "MinValue": 100, "MaxValue": 150, "StackType": "FLAT" }
 *   },
 *   "Condition": {
 *     "Type": "health_below_percent",
 *     "Value": 30
 *   }
 * }
 * </pre>
 */
public class AffixTierAsset {

    /**
     * Codec for stat map: String -> AffixTierStatAsset
     * MapCodec takes (valueCodec, mapSupplier) - keys are always strings
     */
    private static final MapCodec<AffixTierStatAsset, Map<String, AffixTierStatAsset>> STATS_MAP_CODEC = 
            new MapCodec<>(AffixTierStatAsset.CODEC, HashMap::new);

    /**
     * Codec for an optional condition that must be met for this tier's stats to apply.
     */
    public static final BuilderCodec<ConditionAsset> CONDITION_CODEC = BuilderCodec.builder(
                    ConditionAsset.class,
                    ConditionAsset::new
            )
            .append(new KeyedCodec<>("Type", Codec.STRING), (asset, value) -> asset.type = value, asset -> asset.type)
            .add()
            .append(new KeyedCodec<>("Value", Codec.INTEGER), (asset, value) -> asset.value = value != null ? value : 0, asset -> asset.value)
            .add()
            .build();

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
            .append(
                    new KeyedCodec<>("Stats", STATS_MAP_CODEC),
                    (asset, value) -> asset.stats = value != null ? new HashMap<>(value) : new HashMap<>(),
                    asset -> asset.stats
            )
            .add()
            .append(
                    new KeyedCodec<>("Condition", CONDITION_CODEC),
                    (asset, value) -> asset.condition = value,
                    asset -> asset.condition
            )
            .add()
            .build();

    /**
     * Array codec for multiple tier definitions.
     */
    public static final ArrayCodec<AffixTierAsset> ARRAY_CODEC = new ArrayCodec<>(CODEC, AffixTierAsset[]::new);

    private int tier = 1;
    private int itemLevelReq = 0;
    private int weight = -1; // -1 means compute from tier
    private boolean weightExplicitlySet = false;
    private Map<String, AffixTierStatAsset> stats = new HashMap<>();
    @Nullable
    private ConditionAsset condition = null;

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
        
        // Convert stats map
        Map<String, AffixTierStat> convertedStats = new HashMap<>();
        for (Map.Entry<String, AffixTierStatAsset> entry : stats.entrySet()) {
            convertedStats.put(entry.getKey(), entry.getValue().toTierStat(entry.getKey()));
        }
        
        return new AffixTierDefinition(tier, itemLevelReq, effectiveWeight, convertedStats);
    }

    // ========== Accessors ==========

    public int getTier() {
        return tier;
    }

    public int getItemLevelReq() {
        return itemLevelReq;
    }

    public int getWeight() {
        return weight;
    }

    public Map<String, AffixTierStatAsset> getStats() {
        return stats;
    }

    @Nullable
    public ConditionAsset getCondition() {
        return condition;
    }

    /**
     * Optional condition that must be met for this tier's stats to apply.
     * For example, {@code "Type": "health_below_percent", "Value": 30} means
     * stats only activate when the entity's health is below 30%.
     */
    public static class ConditionAsset {
        private String type = "";
        private int value = 0;

        public ConditionAsset() {
        }

        @Nonnull
        public String getType() {
            return type != null ? type : "";
        }

        public int getValue() {
            return value;
        }
    }
}
