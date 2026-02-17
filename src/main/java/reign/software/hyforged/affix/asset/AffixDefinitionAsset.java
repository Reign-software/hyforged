package reign.software.hyforged.affix.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.AffixTierDefinition;
import reign.software.hyforged.affix.model.AffixTriggeredEffect;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON asset definition for affixes.
 * <p>
 * Affixes define stat modifiers that can be rolled on items.
 * Loaded from {@code Server/Hyforged/Affixes/Definitions/<Type>/*.json}.
 * <p>
 * Each tier contains its own Stats map with per-stat value ranges.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Id": "hyforged:of-the-titan",
 *   "Type": "suffix",
 *   "DisplayName": "of the Titan",
 *   "Weight": 100,
 *   "TriggeredEffects": [
 *     {
 *       "Trigger": { "Type": "on_hit", "Chance": 1500 },
 *       "Effect": { "Type": "spawn_projectile", "ProjectileId": "hyforged:orbiting_flame" }
 *     }
 *   ],
 *   "Tiers": [
 *     {
 *       "Tier": 1,
 *       "ItemLevelReq": 70,
 *       "Weight": 35,
 *       "Stats": {
 *         "hyforged:strength": { "MinValue": 45, "MaxValue": 55, "StackType": "FLAT" },
 *         "hyforged:max-health": { "MinValue": 100, "MaxValue": 150, "StackType": "FLAT" }
 *       }
 *     },
 *     {
 *       "Tier": 2,
 *       "ItemLevelReq": 50,
 *       "Weight": 50,
 *       "Stats": {
 *         "hyforged:strength": { "MinValue": 30, "MaxValue": 40, "StackType": "FLAT" },
 *         "hyforged:max-health": { "MinValue": 60, "MaxValue": 90, "StackType": "FLAT" }
 *       }
 *     }
 *   ]
 * }
 * </pre>
 */
public class AffixDefinitionAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>> {

    /**
     * Codec for loading AffixDefinitionAsset from JSON.
     */
    public static final AssetBuilderCodec<String, AffixDefinitionAsset> CODEC = AssetBuilderCodec
            .builder(
                    AffixDefinitionAsset.class,
                    AffixDefinitionAsset::new,
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
            .append(
                    new KeyedCodec<>("Type", Codec.STRING),
                    (asset, value) -> asset.type = value != null ? value : "prefix",
                    asset -> asset.type
            )
            .add()
            .append(
                    new KeyedCodec<>("DisplayName", Codec.STRING),
                    (asset, value) -> asset.displayName = value != null ? value : "",
                    asset -> asset.displayName
            )
            .add()
                .append(
                    new KeyedCodec<>("Description", Codec.STRING),
                    (asset, value) -> asset.description = value != null ? value : "",
                    asset -> asset.description
                )
                .add()
            .append(
                    new KeyedCodec<>("Weight", Codec.INTEGER),
                    (asset, value) -> asset.weight = value != null ? value : AffixDefinition.DEFAULT_WEIGHT,
                    asset -> asset.weight
            )
            .add()
            .append(
                    new KeyedCodec<>("Tiers", AffixTierAsset.ARRAY_CODEC),
                    (asset, value) -> asset.tiers = value,
                    asset -> asset.tiers
            )
            .add()
                .append(
                    new KeyedCodec<>("TriggeredEffects", AffixTriggeredEffectAsset.ARRAY_CODEC),
                    (asset, value) -> asset.triggeredEffects = value,
                    asset -> asset.triggeredEffects
                )
                .add()
            .build();

    private static AssetStore<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>> ASSET_STORE;

    // Asset data
    private String id;
    private AssetExtraInfo.Data data;

    // Affix definition fields
    private String type = "prefix";
    private String displayName = "";
    private String description = "";
    private int weight = AffixDefinition.DEFAULT_WEIGHT;
    private AffixTierAsset[] tiers;
    private AffixTriggeredEffectAsset[] triggeredEffects = new AffixTriggeredEffectAsset[0];

    public AffixDefinitionAsset() {
    }

    /**
     * Get the asset store for affix definitions.
     */
    @Nonnull
    public static AssetStore<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(AffixDefinitionAsset.class);
        }
        return ASSET_STORE;
    }

    // ========== JsonAssetWithMap Interface ==========

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    // ========== Conversion ==========

    /**
     * Convert this asset to an AffixDefinition model object.
     * <p>
     * Stats are embedded in each tier with their own value ranges.
     *
     * @return The AffixDefinition model
     * @throws IllegalArgumentException if the asset has invalid data
     */
    @Nonnull
    public AffixDefinition toAffixDefinition() {
        // Convert tiers
        List<AffixTierDefinition> tierList = new ArrayList<>();
        if (tiers != null) {
            for (AffixTierAsset tierAsset : tiers) {
                tierList.add(tierAsset.toTierDefinition());
            }
        }

        List<AffixTriggeredEffect> effectList = new ArrayList<>();
        if (triggeredEffects != null) {
            for (AffixTriggeredEffectAsset effectAsset : triggeredEffects) {
                effectList.add(effectAsset.toTriggeredEffect());
            }
        }
        
        if (tierList.isEmpty()) {
            throw new IllegalArgumentException(
                    "Affix '" + id + "' must have at least one tier");
        }
        
        return new AffixDefinition(
                id,
                type,
                displayName,
                tierList,
            effectList,
                weight
        );
    }

    // ========== Accessors ==========

    @Nonnull
    public String getType() {
        return type;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    public int getWeight() {
        return weight;
    }

    public AffixTierAsset[] getTiers() {
        return tiers;
    }

    public AffixTriggeredEffectAsset[] getTriggeredEffects() {
        return triggeredEffects;
    }
}
