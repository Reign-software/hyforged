package reign.software.hyforged.stats.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import reign.software.hyforged.stats.CoreCategories;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.scaling.ScalingRule;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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
 *   "IsRating": false,                   // Whether this is a rating stat
 *   "Tags": ["attributes", "primary"]    // Tags this stat belongs to
 * }
 * </pre>
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
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .appendInherited(
                    new KeyedCodec<>("Category", Codec.STRING),
                    (asset, value) -> asset.category = value != null ? value : CoreCategories.UTILITY,
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
                    new KeyedCodec<>("IsRating", Codec.BOOLEAN),
                    (asset, value) -> asset.isRating = value != null && value,
                    asset -> asset.isRating,
                    (asset, parent) -> asset.isRating = parent.isRating
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Tags", Codec.STRING_ARRAY),
                    (asset, value) -> asset.tags = value,
                    asset -> asset.tags,
                    (asset, parent) -> asset.tags = parent.tags
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Scaling", ScalingRuleAssetCodec.ARRAY_CODEC),
                    (asset, value) -> asset.scalingAssets = value,
                    asset -> asset.scalingAssets,
                    (asset, parent) -> asset.scalingAssets = parent.scalingAssets
            )
            .add()
            .build();

    private static AssetStore<String, StatDefinitionAsset, IndexedLookupTableAssetMap<String, StatDefinitionAsset>> ASSET_STORE;

    // Asset data
    private String id;
    private AssetExtraInfo.Data data;

    // Stat definition fields
    private String category = CoreCategories.UTILITY;
    private String displayName = "";
    private String description = "";
    private int defaultValue = 0;
    private int minValue = 0;
    private int maxValue = Integer.MAX_VALUE;
    private boolean isRating = false;
    private String[] tags = new String[0];
    private ScalingRuleAsset[] scalingAssets = new ScalingRuleAsset[0];

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

    public boolean isRating() {
        return isRating;
    }

    @Nonnull
    public String[] getTags() {
        return tags;
    }

    /**
     * Get the scaling rule assets (raw, unconverted).
     */
    @Nonnull
    public ScalingRuleAsset[] getScalingAssets() {
        return scalingAssets;
    }

    /**
     * Convert this asset to a StatDefinition for registration.
     * <p>
     * Scaling rules are converted from their asset representations.
     * Invalid or unresolvable scaling rules are skipped with a warning.
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
        
        return new StatDefinition.Builder(statId)
                .category(category)
                .displayName(displayName)
                .description(description)
                .defaultValue(defaultValue)
                .bounds(minValue, maxValue)
                .rating(isRating)
                .tags(new HashSet<>(Arrays.asList(tags)))
                .scaling(scalingRules)
                .build();
    }
}
