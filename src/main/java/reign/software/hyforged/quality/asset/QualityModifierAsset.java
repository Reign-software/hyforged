package reign.software.hyforged.quality.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import reign.software.hyforged.quality.model.QualityModifierConfig;
import reign.software.hyforged.quality.model.QualityModifierOverrides;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON asset definition for quality modifiers.
 */
public class QualityModifierAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, QualityModifierAsset>> {

    private static final MapCodec<Integer, Map<String, Integer>> INT_MAP_CODEC =
            new MapCodec<>(Codec.INTEGER, HashMap::new);

    private static final MapCodec<Double, Map<String, Double>> DOUBLE_MAP_CODEC =
            new MapCodec<>(Codec.DOUBLE, HashMap::new);

    public static final BuilderCodec<LevelScalingAsset> LEVEL_SCALING_CODEC = BuilderCodec.builder(
                    LevelScalingAsset.class,
                    LevelScalingAsset::new
            )
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (asset, value) -> asset.enabled = value, asset -> asset.enabled)
            .add()
            .append(new KeyedCodec<>("CurveId", Codec.STRING), (asset, value) -> asset.curveId = value, asset -> asset.curveId)
            .add()
            .append(new KeyedCodec<>("QualityBonusPerLevel", DOUBLE_MAP_CODEC),
                    (asset, value) -> asset.qualityBonusPerLevel = value != null ? value : new HashMap<>(),
                    asset -> asset.qualityBonusPerLevel)
            .add()
            .build();

    public static final BuilderCodec<ItemRarityAsset> ITEM_RARITY_CODEC = BuilderCodec.builder(
                    ItemRarityAsset.class,
                    ItemRarityAsset::new
            )
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (asset, value) -> asset.enabled = value, asset -> asset.enabled)
            .add()
            .append(new KeyedCodec<>("StatId", Codec.STRING), (asset, value) -> asset.statId = value, asset -> asset.statId)
            .add()
            .append(new KeyedCodec<>("ScalingFactor", Codec.DOUBLE), (asset, value) -> asset.scalingFactor = value, asset -> asset.scalingFactor)
            .add()
            .append(new KeyedCodec<>("MaxBonus", Codec.INTEGER), (asset, value) -> asset.maxBonus = value, asset -> asset.maxBonus)
            .add()
            .append(new KeyedCodec<>("FallbackValue", Codec.INTEGER), (asset, value) -> asset.fallbackValue = value, asset -> asset.fallbackValue)
            .add()
            .build();

    public static final BuilderCodec<NpcQualityBonusAsset> NPC_QUALITY_BONUS_CODEC = BuilderCodec.builder(
                    NpcQualityBonusAsset.class,
                    NpcQualityBonusAsset::new
            )
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (asset, value) -> asset.enabled = value, asset -> asset.enabled)
            .add()
            .append(new KeyedCodec<>("BonusPerTier", INT_MAP_CODEC),
                    (asset, value) -> asset.bonusPerTier = value != null ? value : new HashMap<>(),
                    asset -> asset.bonusPerTier)
            .add()
            .build();

    public static final BuilderCodec<ModifierOverridesAsset> MODIFIER_OVERRIDES_CODEC = BuilderCodec.builder(
                    ModifierOverridesAsset.class,
                    ModifierOverridesAsset::new
            )
            .append(new KeyedCodec<>("LevelScaling", LEVEL_SCALING_CODEC), (asset, value) -> asset.levelScaling = value, asset -> asset.levelScaling)
            .add()
            .append(new KeyedCodec<>("ItemRarity", ITEM_RARITY_CODEC), (asset, value) -> asset.itemRarity = value, asset -> asset.itemRarity)
            .add()
            .append(new KeyedCodec<>("NpcQualityBonus", NPC_QUALITY_BONUS_CODEC), (asset, value) -> asset.npcQualityBonus = value, asset -> asset.npcQualityBonus)
            .add()
            .build();

    public static final AssetBuilderCodec<String, QualityModifierAsset> CODEC = AssetBuilderCodec
            .builder(
                    QualityModifierAsset.class,
                    QualityModifierAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .append(new KeyedCodec<>("Description", Codec.STRING), (asset, value) -> asset.description = value != null ? value : "", asset -> asset.description)
            .add()
            .append(new KeyedCodec<>("LevelScaling", LEVEL_SCALING_CODEC), (asset, value) -> asset.levelScaling = value, asset -> asset.levelScaling)
            .add()
            .append(new KeyedCodec<>("ItemRarity", ITEM_RARITY_CODEC), (asset, value) -> asset.itemRarity = value, asset -> asset.itemRarity)
            .add()
            .append(new KeyedCodec<>("NpcQualityBonus", NPC_QUALITY_BONUS_CODEC), (asset, value) -> asset.npcQualityBonus = value, asset -> asset.npcQualityBonus)
            .add()
            .build();

    private static AssetStore<String, QualityModifierAsset, IndexedLookupTableAssetMap<String, QualityModifierAsset>> ASSET_STORE;

    private String id;
    private AssetExtraInfo.Data data;
    private String description = "";
    private LevelScalingAsset levelScaling = new LevelScalingAsset();
    private ItemRarityAsset itemRarity = new ItemRarityAsset();
    private NpcQualityBonusAsset npcQualityBonus = new NpcQualityBonusAsset();

    public QualityModifierAsset() {}

    @Nonnull
    public static AssetStore<String, QualityModifierAsset, IndexedLookupTableAssetMap<String, QualityModifierAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(QualityModifierAsset.class);
        }
        return ASSET_STORE;
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    public QualityModifierConfig toModifierConfig() {
        return new QualityModifierConfig(
                id,
                description,
                levelScaling.toConfig(),
                itemRarity.toConfig(),
                npcQualityBonus.toConfig()
        );
    }

    @Nonnull
    public static QualityModifierOverrides toOverrides(@Nonnull ModifierOverridesAsset overrides) {
        return overrides.toOverrides();
    }

    public static class LevelScalingAsset {
        private boolean enabled = true;
        private String curveId = "";
        private Map<String, Double> qualityBonusPerLevel = new HashMap<>();

        @Nonnull
        public QualityModifierConfig.LevelScalingConfig toConfig() {
            return new QualityModifierConfig.LevelScalingConfig(enabled, curveId != null ? curveId : "", qualityBonusPerLevel);
        }

        @Nonnull
        public QualityModifierOverrides.LevelScalingOverride toOverride() {
            return new QualityModifierOverrides.LevelScalingOverride(enabled, curveId, qualityBonusPerLevel);
        }
    }

    public static class ItemRarityAsset {
        private boolean enabled = true;
        private String statId = "";
        private double scalingFactor = 0.0;
        private int maxBonus = 0;
        private int fallbackValue = 0;

        @Nonnull
        public QualityModifierConfig.ItemRarityConfig toConfig() {
            return new QualityModifierConfig.ItemRarityConfig(enabled, statId != null ? statId : "", scalingFactor, maxBonus, fallbackValue);
        }

        @Nonnull
        public QualityModifierOverrides.ItemRarityOverride toOverride() {
            return new QualityModifierOverrides.ItemRarityOverride(enabled, statId, scalingFactor, maxBonus, fallbackValue);
        }
    }

    public static class NpcQualityBonusAsset {
        private boolean enabled = true;
        private Map<String, Integer> bonusPerTier = new HashMap<>();

        @Nonnull
        public QualityModifierConfig.NpcQualityBonusConfig toConfig() {
            return new QualityModifierConfig.NpcQualityBonusConfig(enabled, bonusPerTier);
        }

        @Nonnull
        public QualityModifierOverrides.NpcQualityBonusOverride toOverride() {
            return new QualityModifierOverrides.NpcQualityBonusOverride(enabled, bonusPerTier);
        }
    }

    public static class ModifierOverridesAsset {
        private LevelScalingAsset levelScaling;
        private ItemRarityAsset itemRarity;
        private NpcQualityBonusAsset npcQualityBonus;

        @Nonnull
        public QualityModifierOverrides toOverrides() {
            QualityModifierOverrides.LevelScalingOverride level = levelScaling != null ? levelScaling.toOverride() : null;
            QualityModifierOverrides.ItemRarityOverride item = itemRarity != null ? itemRarity.toOverride() : null;
            QualityModifierOverrides.NpcQualityBonusOverride npc = npcQualityBonus != null ? npcQualityBonus.toOverride() : null;
            return new QualityModifierOverrides(level, item, npc);
        }
    }
}
