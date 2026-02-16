package reign.software.hyforged.stats.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import reign.software.hyforged.affix.asset.AffixTierTemplateAsset;
import reign.software.hyforged.affix.model.AffixTierTemplate;
import reign.software.hyforged.stats.DisplayFormat;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.scaling.ScalingRule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON asset definition for Hyforged stats.
 * <p>
 * This allows mods to define stats via JSON files in their asset packs.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Id": "hyforged:strength",           // Namespaced stat ID
 *   "Category": "ability-score",         // Category ID (data-driven)
 *   "DisplayName": "Strength",           // Display name for UI
 *   "Description": "Physical power...",  // Description for tooltips
 *   "DefaultValue": 10,                  // Default value
 *   "MinValue": 0,                       // Minimum value
 *   "MaxValue": 999,                     // Maximum value
 *   "DisplayFormat": "INTEGER",          // Display format (INTEGER, PERCENT_BPS, PERCENT, RATING, FLAT_BONUS, MULTIPLIER)
 *   "Tags": {                            // Tags using Hytale's hierarchical format
 *     "Domain": ["attributes"],          // Creates tags: Domain, attributes, Domain=attributes
 *     "Type": ["ability-score"]          // Creates tags: Type, ability-score, Type=ability-score
 *   }
 * }
 * </pre>
 * <p>
 * Tags use Hytale's hierarchical format where each key-value pair creates multiple tags:
 * the key itself, each value, and key=value combinations. This enables flexible tag matching
 * using Hytale's AssetRegistry tag system.
 */
public class StatDefinitionAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, StatDefinitionAsset>> {

    /**
     * Codec for loading StatDefinitionAsset from JSON.
     */
    public static final AssetBuilderCodec<String, StatDefinitionAsset> CODEC = AssetBuilderCodec
            .builder(
                StatDefinitionAsset.class,
                StatDefinitionAsset::new,
                Codec.STRING,
                    (asset, id) -> {
                    if (asset.id == null || asset.id.isBlank()) {
                        asset.id = id;
                    }
                    },
                asset -> asset.id,
                (asset, data) -> asset.data = data,
                asset -> asset.data
            )
                .append(
                    new KeyedCodec<>("Id", Codec.STRING),
                    (asset, value) -> asset.id = value != null ? value : asset.id,
                    asset -> asset.id
                )
                .add()
            .appendInherited(
                new KeyedCodec<>("Category", Codec.STRING),
                (asset, value) -> asset.category = value != null ? value : "utility",
                asset -> asset.category,
                (asset, parent) -> asset.category = parent.category
            )
            .add()
            .appendInherited(
                new KeyedCodec<>("DisplayName", Codec.STRING),
                (asset, value) -> asset.displayName = value,
                asset -> asset.displayName,
                (asset, parent) -> asset.displayName = parent.displayName
            )
            .add()
            .appendInherited(
                new KeyedCodec<>("Description", Codec.STRING),
                (asset, value) -> asset.description = value,
                asset -> asset.description,
                (asset, parent) -> asset.description = parent.description
            )
            .add()
            .appendInherited(
                new KeyedCodec<>("DefaultValue", Codec.INTEGER),
                (asset, value) -> asset.defaultValue = value != null ? value : 0,
                asset -> asset.defaultValue,
                (asset, parent) -> asset.defaultValue = parent.defaultValue
            )
            .add()
            .appendInherited(
                new KeyedCodec<>("MinValue", Codec.INTEGER),
                (asset, value) -> asset.minValue = value != null ? value : Integer.MIN_VALUE,
                asset -> asset.minValue,
                (asset, parent) -> asset.minValue = parent.minValue
            )
            .add()
            .appendInherited(
                new KeyedCodec<>("MaxValue", Codec.INTEGER),
                (asset, value) -> asset.maxValue = value != null ? value : Integer.MAX_VALUE,
                asset -> asset.maxValue,
                (asset, parent) -> asset.maxValue = parent.maxValue
            )
            .add()
            .appendInherited(
                new KeyedCodec<>("DisplayFormat", Codec.STRING),
                (asset, value) -> asset.displayFormat = DisplayFormat.fromString(value),
                asset -> asset.displayFormat != null ? asset.displayFormat.name() : null,
                (asset, parent) -> asset.displayFormat = parent.displayFormat
            )
            .add()
            .appendInherited(
                new KeyedCodec<>("RatingK", Codec.INTEGER),
                (asset, value) -> asset.ratingK = value != null ? value : StatDefinition.DEFAULT_RATING_K,
                asset -> asset.ratingK,
                (asset, parent) -> asset.ratingK = parent.ratingK
            )
            .add()
            .appendInherited(
                new KeyedCodec<>("Scaling", ScalingRuleAssetCodec.ARRAY_CODEC),
                (asset, value) -> asset.scalingAssets = value,
                asset -> asset.scalingAssets,
                (asset, parent) -> asset.scalingAssets = parent.scalingAssets
            )
            .add()
            .appendInherited(
                new KeyedCodec<>("AffixTierTemplate", AffixTierTemplateAsset.CODEC),
                (asset, value) -> asset.affixTierTemplateAsset = value,
                asset -> asset.affixTierTemplateAsset,
                (asset, parent) -> asset.affixTierTemplateAsset = parent.affixTierTemplateAsset
            )
            .add()
            .appendInherited(
                new KeyedCodec<>("SoftCapBps", Codec.INTEGER),
                (asset, value) -> asset.softCapBps = value != null ? value : StatDefinition.NO_CAP,
                asset -> asset.softCapBps,
                (asset, parent) -> asset.softCapBps = parent.softCapBps
            )
            .add()
            .appendInherited(
                new KeyedCodec<>("HardCapBps", Codec.INTEGER),
                (asset, value) -> asset.hardCapBps = value != null ? value : StatDefinition.NO_CAP,
                asset -> asset.hardCapBps,
                (asset, parent) -> asset.hardCapBps = parent.hardCapBps
            )
            .add()
            .appendInherited(
                new KeyedCodec<>("SoftCapBonusStat", Codec.STRING),
                (asset, value) -> asset.softCapBonusStatId = value,
                asset -> asset.softCapBonusStatId,
                (asset, parent) -> asset.softCapBonusStatId = parent.softCapBonusStatId
            )
            .add()
            .build();

    private static AssetStore<String, StatDefinitionAsset, IndexedLookupTableAssetMap<String, StatDefinitionAsset>> ASSET_STORE;

    // Asset data
    private String id;
    private AssetExtraInfo.Data data;

    // Stat definition fields
    private String category = "utility";
    private String displayName = "";
    private String description = "";
    private int defaultValue = 0;
    private int minValue = 0;
    private int maxValue = Integer.MAX_VALUE;
    private DisplayFormat displayFormat = null;
    private int ratingK = StatDefinition.DEFAULT_RATING_K;
    private ScalingRuleAsset[] scalingAssets = new ScalingRuleAsset[0];
    private AffixTierTemplateAsset affixTierTemplateAsset = null;
    private int softCapBps = StatDefinition.NO_CAP;
    private int hardCapBps = StatDefinition.NO_CAP;
    private String softCapBonusStatId = null;

    public StatDefinitionAsset() {
    }

    /**
     * Get the asset store for stat definitions.
     */
    @Nonnull
    public static AssetStore<String, StatDefinitionAsset, IndexedLookupTableAssetMap<String, StatDefinitionAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(StatDefinitionAsset.class);
        }
        return ASSET_STORE;
    }

    // ========== JsonAssetWithMap Interface ==========

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    // ========== Accessors ==========

    @Nonnull
    public String getCategory() {
        return category;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    public int getDefaultValue() {
        return defaultValue;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    @Nullable
    public DisplayFormat getDisplayFormat() {
        return displayFormat;
    }

    /**
     * Get the raw tags map (hierarchical format).
     * Use {@link #getExpandedTags()} to get the flattened tag set.
     */
    @Nonnull
    public Map<String, String[]> getRawTags() {
        return data != null ? data.getRawTags() : Collections.emptyMap();
    }
    
    /**
     * Expand the hierarchical tags into a flat set.
     * Following Hytale's tag expansion pattern, each entry in the map
     * generates: the key itself, each value, and key=value combinations.
     * <p>
     * For example: {"Domain": ["attributes"], "Type": ["ability-score"]}
     * expands to: [Domain, attributes, Domain=attributes, Type, ability-score, Type=ability-score]
     */
    @Nonnull
    public Set<String> getExpandedTags() {
        Set<String> expandedTags = new HashSet<>();
        for (Map.Entry<String, String[]> entry : getRawTags().entrySet()) {
            String key = entry.getKey();
            expandedTags.add(key);
            for (String value : entry.getValue()) {
                expandedTags.add(value);
                expandedTags.add(key + "=" + value);
            }
        }
        return expandedTags;
    }

    /**
     * Get the scaling rule assets (raw, unconverted).
     */
    @Nonnull
    public ScalingRuleAsset[] getScalingAssets() {
        return scalingAssets;
    }
    
    /**
     * Check if this stat has an affix tier template.
     *
     * @return true if an affix tier template is defined
     */
    public boolean hasAffixTierTemplate() {
        return affixTierTemplateAsset != null;
    }
    
    /**
     * Get the affix tier template for this stat, if defined.
     * <p>
     * Affix tier templates allow stats to define default tier progressions
     * that can be reused across multiple affixes using this stat.
     *
     * @return The affix tier template, or null if not defined
     */
    @Nullable
    public AffixTierTemplate getAffixTierTemplate() {
        return affixTierTemplateAsset != null ? affixTierTemplateAsset.toModel() : null;
    }

    /**
     * Convert this asset to a StatDefinition for registration.
     * <p>
     * Scaling rules are converted from their asset representations.
     * Invalid or unresolvable scaling rules are skipped with a warning.
     * <p>
     * Tags are expanded from the hierarchical format to a flat set.
     */
    @Nonnull
    public StatDefinition toStatDefinition() {
        StatId statId = StatId.parse(id);
        
        // Convert scaling rules
        List<ScalingRule> scalingRules = new ArrayList<>();
        if (scalingAssets != null) {
            for (ScalingRuleAsset scalingAsset : scalingAssets) {
                scalingAsset.toScalingRule(id).ifPresent(scalingRules::add);
            }
        }
        
        // Parse soft cap bonus stat if specified
        StatId bonusStat = softCapBonusStatId != null ? StatId.parse(softCapBonusStatId) : null;
        
        return new StatDefinition.Builder(statId)
                .category(category)
                .displayName(displayName)
                .description(description)
                .defaultValue(defaultValue)
                .bounds(minValue, maxValue)
                .displayFormat(displayFormat != null ? displayFormat : DisplayFormat.INTEGER)
                .ratingK(ratingK)
                .tags(getExpandedTags())
                .scaling(scalingRules)
                .softCapBps(softCapBps)
                .hardCapBps(hardCapBps)
                .softCapBonusStat(bonusStat)
                .build();
    }
}
