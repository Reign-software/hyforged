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
import reign.software.hyforged.affix.model.AffixEligibility;
import reign.software.hyforged.affix.model.AffixTierDefinition;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON asset definition for affixes.
 * <p>
 * Affixes define stat modifiers that can be rolled on items.
 * Loaded from {@code Server/Hyforged/Affixes/*.json}.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Id": "sturdy",
 *   "Type": "prefix",
 *   "DisplayName": "Sturdy",
 *   "StatId": "hyforged:armor",
 *   "ModifierType": "FLAT",
 *   "Weight": 100,
 *   "Eligibility": {
 *     "ItemCategories": ["Items.Armor"],
 *     "MinQuality": "Common"
 *   },
 *   "Tiers": [
 *     { "Tier": 1, "MinValue": 50, "MaxValue": 75, "ItemLevelReq": 40 },
 *     { "Tier": 2, "MinValue": 35, "MaxValue": 50, "ItemLevelReq": 25 },
 *     { "Tier": 3, "MinValue": 20, "MaxValue": 35, "ItemLevelReq": 10 },
 *     { "Tier": 4, "MinValue": 10, "MaxValue": 20, "ItemLevelReq": 1 },
 *     { "Tier": 5, "MinValue": 1, "MaxValue": 10, "ItemLevelReq": 1 }
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
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
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
                    new KeyedCodec<>("StatId", Codec.STRING),
                    (asset, value) -> asset.statId = value,
                    asset -> asset.statId
            )
            .add()
            .append(
                    new KeyedCodec<>("ModifierType", Codec.STRING),
                    (asset, value) -> asset.modifierType = value != null ? value : "FLAT",
                    asset -> asset.modifierType
            )
            .add()
            .append(
                    new KeyedCodec<>("Weight", Codec.INTEGER),
                    (asset, value) -> asset.weight = value != null ? value : AffixDefinition.DEFAULT_WEIGHT,
                    asset -> asset.weight
            )
            .add()
            .append(
                    new KeyedCodec<>("Eligibility", AffixEligibilityAsset.CODEC),
                    (asset, value) -> asset.eligibility = value,
                    asset -> asset.eligibility
            )
            .add()
            .append(
                    new KeyedCodec<>("Tiers", AffixTierAsset.ARRAY_CODEC),
                    (asset, value) -> asset.tiers = value,
                    asset -> asset.tiers
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
    private String statId;
    private String modifierType = "FLAT";
    private int weight = AffixDefinition.DEFAULT_WEIGHT;
    private AffixEligibilityAsset eligibility;
    private AffixTierAsset[] tiers;

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
        
        // Convert eligibility
        AffixEligibility elig = eligibility != null 
                ? eligibility.toEligibility() 
                : AffixEligibility.ANY;
        
        // Parse modifier type
        HyforgedModifier.StackType stackType;
        try {
            stackType = HyforgedModifier.StackType.valueOf(modifierType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid ModifierType '" + modifierType + "' for affix '" + id + "'. " +
                    "Valid values: FLAT, INCREASED, MORE, CAP");
        }
        
        return new AffixDefinition(
                id,
                type,
                displayName,
                StatId.parse(statId),
                stackType,
                tierList,
                elig,
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

    public String getStatId() {
        return statId;
    }

    @Nonnull
    public String getModifierType() {
        return modifierType;
    }

    public int getWeight() {
        return weight;
    }

    public AffixEligibilityAsset getEligibility() {
        return eligibility;
    }

    public AffixTierAsset[] getTiers() {
        return tiers;
    }
}
