package reign.software.hyforged.currency.config;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;

import javax.annotation.Nonnull;

/**
 * Asset class for loading SellValueConfig from JSON.
 */
public class SellValueConfigAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, SellValueConfigAsset>> {

    public static final AssetBuilderCodec<String, SellValueConfigAsset> CODEC = AssetBuilderCodec
        .builder(
            SellValueConfigAsset.class,
            SellValueConfigAsset::new,
            Codec.STRING,
            (asset, id) -> asset.id = id,
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
            new KeyedCodec<>("BaseValue", Codec.INTEGER),
            (asset, value) -> asset.baseValue = value != null ? value : 1,
            asset -> asset.baseValue,
            (asset, parent) -> asset.baseValue = parent.baseValue
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("MinSellValue", Codec.INTEGER),
            (asset, value) -> asset.minSellValue = value != null ? value : 1,
            asset -> asset.minSellValue,
            (asset, parent) -> asset.minSellValue = parent.minSellValue
        )
        .add()
        // Rarity multipliers as individual fields
        .appendInherited(
            new KeyedCodec<>("RarityJunk", Codec.INTEGER),
            (asset, value) -> asset.rarityJunk = value != null ? value : 0,
            asset -> asset.rarityJunk,
            (asset, parent) -> asset.rarityJunk = parent.rarityJunk
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("RarityCommon", Codec.INTEGER),
            (asset, value) -> asset.rarityCommon = value != null ? value : 1,
            asset -> asset.rarityCommon,
            (asset, parent) -> asset.rarityCommon = parent.rarityCommon
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("RarityUncommon", Codec.INTEGER),
            (asset, value) -> asset.rarityUncommon = value != null ? value : 2,
            asset -> asset.rarityUncommon,
            (asset, parent) -> asset.rarityUncommon = parent.rarityUncommon
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("RarityRare", Codec.INTEGER),
            (asset, value) -> asset.rarityRare = value != null ? value : 5,
            asset -> asset.rarityRare,
            (asset, parent) -> asset.rarityRare = parent.rarityRare
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("RarityEpic", Codec.INTEGER),
            (asset, value) -> asset.rarityEpic = value != null ? value : 15,
            asset -> asset.rarityEpic,
            (asset, parent) -> asset.rarityEpic = parent.rarityEpic
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("RarityLegendary", Codec.INTEGER),
            (asset, value) -> asset.rarityLegendary = value != null ? value : 50,
            asset -> asset.rarityLegendary,
            (asset, parent) -> asset.rarityLegendary = parent.rarityLegendary
        )
        .add()
        // Affix tier values
        .appendInherited(
            new KeyedCodec<>("AffixTier1Value", Codec.INTEGER),
            (asset, value) -> asset.affixTier1Value = value != null ? value : 100,
            asset -> asset.affixTier1Value,
            (asset, parent) -> asset.affixTier1Value = parent.affixTier1Value
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("AffixTier2Value", Codec.INTEGER),
            (asset, value) -> asset.affixTier2Value = value != null ? value : 50,
            asset -> asset.affixTier2Value,
            (asset, parent) -> asset.affixTier2Value = parent.affixTier2Value
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("AffixTier3Value", Codec.INTEGER),
            (asset, value) -> asset.affixTier3Value = value != null ? value : 25,
            asset -> asset.affixTier3Value,
            (asset, parent) -> asset.affixTier3Value = parent.affixTier3Value
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("AffixTier4Value", Codec.INTEGER),
            (asset, value) -> asset.affixTier4Value = value != null ? value : 10,
            asset -> asset.affixTier4Value,
            (asset, parent) -> asset.affixTier4Value = parent.affixTier4Value
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("AffixTier5Value", Codec.INTEGER),
            (asset, value) -> asset.affixTier5Value = value != null ? value : 5,
            asset -> asset.affixTier5Value,
            (asset, parent) -> asset.affixTier5Value = parent.affixTier5Value
        )
        .add()
        .build();

    // Asset metadata
    private String id = "hyforged:sell-value-config";
    private AssetExtraInfo.Data data;

    // Config fields
    private int baseValue = 1;
    private int minSellValue = 1;
    
    // Rarity multipliers
    private int rarityJunk = 0;
    private int rarityCommon = 1;
    private int rarityUncommon = 2;
    private int rarityRare = 5;
    private int rarityEpic = 15;
    private int rarityLegendary = 50;
    
    // Affix tier values
    private int affixTier1Value = 100;
    private int affixTier2Value = 50;
    private int affixTier3Value = 25;
    private int affixTier4Value = 10;
    private int affixTier5Value = 5;

    public SellValueConfigAsset() {
        // Required for codec
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    public int getBaseValue() {
        return baseValue;
    }

    public int getMinSellValue() {
        return minSellValue;
    }

    /**
     * Get the rarity multiplier for a given rarity name.
     */
    public int getRarityMultiplier(@Nonnull String rarity) {
        return switch (rarity.toLowerCase()) {
            case "junk" -> rarityJunk;
            case "common" -> rarityCommon;
            case "uncommon" -> rarityUncommon;
            case "rare" -> rarityRare;
            case "epic" -> rarityEpic;
            case "legendary" -> rarityLegendary;
            default -> rarityCommon;
        };
    }

    /**
     * Get the affix value for a given tier.
     */
    public int getAffixValueForTier(int tier) {
        return switch (tier) {
            case 1 -> affixTier1Value;
            case 2 -> affixTier2Value;
            case 3 -> affixTier3Value;
            case 4 -> affixTier4Value;
            case 5 -> affixTier5Value;
            default -> 0;
        };
    }
}
