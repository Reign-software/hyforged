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
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import reign.software.hyforged.quality.model.QualityEligibilityRule;
import reign.software.hyforged.quality.model.QualityModifierOverrides;

import javax.annotation.Nonnull;

/**
 * JSON asset definition for quality eligibility rules.
 */
public class QualityEligibilityAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, QualityEligibilityAsset>> {

    private static final ArrayCodec<String> STRING_ARRAY_CODEC = new ArrayCodec<>(Codec.STRING, String[]::new);

    public static final BuilderCodec<AppliesToAsset> APPLIES_TO_CODEC = BuilderCodec.builder(
                    AppliesToAsset.class,
                    AppliesToAsset::new
            )
            .append(new KeyedCodec<>("Categories", STRING_ARRAY_CODEC), (asset, value) -> asset.categories = value, asset -> asset.categories)
            .add()
            .append(new KeyedCodec<>("Tags", STRING_ARRAY_CODEC), (asset, value) -> asset.tags = value, asset -> asset.tags)
            .add()
            .append(new KeyedCodec<>("ItemIds", STRING_ARRAY_CODEC), (asset, value) -> asset.itemIds = value, asset -> asset.itemIds)
            .add()
            .build();

    public static final BuilderCodec<ExcludesAsset> EXCLUDES_CODEC = BuilderCodec.builder(
                    ExcludesAsset.class,
                    ExcludesAsset::new
            )
            .append(new KeyedCodec<>("Tags", STRING_ARRAY_CODEC), (asset, value) -> asset.tags = value, asset -> asset.tags)
            .add()
            .append(new KeyedCodec<>("ItemIds", STRING_ARRAY_CODEC), (asset, value) -> asset.itemIds = value, asset -> asset.itemIds)
            .add()
            .build();

    public static final BuilderCodec<SourceFilterAsset> SOURCE_FILTER_CODEC = BuilderCodec.builder(
                    SourceFilterAsset.class,
                    SourceFilterAsset::new
            )
            .append(new KeyedCodec<>("SourceTags", STRING_ARRAY_CODEC), (asset, value) -> asset.sourceTags = value, asset -> asset.sourceTags)
            .add()
            .append(new KeyedCodec<>("ExcludeSourceTags", STRING_ARRAY_CODEC), (asset, value) -> asset.excludeSourceTags = value, asset -> asset.excludeSourceTags)
            .add()
            .build();

    public static final AssetBuilderCodec<String, QualityEligibilityAsset> CODEC = AssetBuilderCodec
            .builder(
                    QualityEligibilityAsset.class,
                    QualityEligibilityAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .append(new KeyedCodec<>("Priority", Codec.INTEGER), (asset, value) -> asset.priority = value != null ? value : 0, asset -> asset.priority)
            .add()
            .append(new KeyedCodec<>("Description", Codec.STRING), (asset, value) -> asset.description = value != null ? value : "", asset -> asset.description)
            .add()
            .append(new KeyedCodec<>("WeightProfileId", Codec.STRING), (asset, value) -> asset.weightProfileId = value != null ? value : "", asset -> asset.weightProfileId)
            .add()
            .append(new KeyedCodec<>("AppliesTo", APPLIES_TO_CODEC), (asset, value) -> asset.appliesTo = value, asset -> asset.appliesTo)
            .add()
            .append(new KeyedCodec<>("Excludes", EXCLUDES_CODEC), (asset, value) -> asset.excludes = value, asset -> asset.excludes)
            .add()
            .append(new KeyedCodec<>("SourceFilter", SOURCE_FILTER_CODEC), (asset, value) -> asset.sourceFilter = value, asset -> asset.sourceFilter)
            .add()
            .append(new KeyedCodec<>("ModifierOverrides", QualityModifierAsset.MODIFIER_OVERRIDES_CODEC), (asset, value) -> asset.modifierOverrides = value, asset -> asset.modifierOverrides)
            .add()
            .build();

    private static AssetStore<String, QualityEligibilityAsset, IndexedLookupTableAssetMap<String, QualityEligibilityAsset>> ASSET_STORE;

    private String id;
    private AssetExtraInfo.Data data;
    private int priority = 0;
    private String description = "";
    private String weightProfileId = "";
    private AppliesToAsset appliesTo = new AppliesToAsset();
    private ExcludesAsset excludes = new ExcludesAsset();
    private SourceFilterAsset sourceFilter = new SourceFilterAsset();
    private QualityModifierAsset.ModifierOverridesAsset modifierOverrides;

    public QualityEligibilityAsset() {}

    @Nonnull
    public static AssetStore<String, QualityEligibilityAsset, IndexedLookupTableAssetMap<String, QualityEligibilityAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(QualityEligibilityAsset.class);
        }
        return ASSET_STORE;
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    public QualityEligibilityRule toRule() {
        QualityModifierOverrides overrides = modifierOverrides != null
                ? modifierOverrides.toOverrides()
                : QualityModifierOverrides.EMPTY;

        return new QualityEligibilityRule(
                id,
                priority,
                description,
                weightProfileId,
                appliesTo.toModel(),
                excludes.toModel(),
                sourceFilter.toModel(),
                overrides
        );
    }

    public static class AppliesToAsset {
        private String[] categories = new String[0];
        private String[] tags = new String[0];
        private String[] itemIds = new String[0];

        @Nonnull
        public QualityEligibilityRule.AppliesTo toModel() {
            return new QualityEligibilityRule.AppliesTo(
                    java.util.List.of(categories != null ? categories : new String[0]),
                    java.util.List.of(tags != null ? tags : new String[0]),
                    java.util.List.of(itemIds != null ? itemIds : new String[0])
            );
        }
    }

    public static class ExcludesAsset {
        private String[] tags = new String[0];
        private String[] itemIds = new String[0];

        @Nonnull
        public QualityEligibilityRule.Excludes toModel() {
            return new QualityEligibilityRule.Excludes(
                    java.util.List.of(tags != null ? tags : new String[0]),
                    java.util.List.of(itemIds != null ? itemIds : new String[0])
            );
        }
    }

    public static class SourceFilterAsset {
        private String[] sourceTags = new String[0];
        private String[] excludeSourceTags = new String[0];

        @Nonnull
        public QualityEligibilityRule.SourceFilter toModel() {
            return new QualityEligibilityRule.SourceFilter(
                    java.util.List.of(sourceTags != null ? sourceTags : new String[0]),
                    java.util.List.of(excludeSourceTags != null ? excludeSourceTags : new String[0])
            );
        }
    }
}
