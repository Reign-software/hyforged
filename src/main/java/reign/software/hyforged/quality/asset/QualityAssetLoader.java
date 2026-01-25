package reign.software.hyforged.quality.asset;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import reign.software.hyforged.quality.model.NPCQualityRule;
import reign.software.hyforged.quality.model.QualityEligibilityRule;
import reign.software.hyforged.quality.model.QualityModifierConfig;
import reign.software.hyforged.quality.model.QualityWeightProfile;
import reign.software.hyforged.quality.registry.NPCQualityRegistry;
import reign.software.hyforged.quality.registry.QualityEligibilityRegistry;
import reign.software.hyforged.quality.registry.QualityModifierRegistry;
import reign.software.hyforged.quality.registry.QualityWeightRegistry;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles loading quality-related assets from JSON files.
 */
public final class QualityAssetLoader {

    private static final Logger LOGGER = Logger.getLogger(QualityAssetLoader.class.getName());

    public static final String QUALITY_WEIGHTS_PATH = "Hyforged/QualityWeights";
    public static final String QUALITY_ELIGIBILITY_PATH = "Hyforged/QualityEligibility";
    public static final String QUALITY_MODIFIERS_PATH = "Hyforged/QualityModifiers";
    public static final String NPC_QUALITY_PATH = "Hyforged/NPCQuality";

    private static boolean initialized = false;

    private QualityAssetLoader() {}

    public static void initialize(@Nonnull JavaPlugin plugin) {
        if (initialized) {
            LOGGER.warning("QualityAssetLoader already initialized");
            return;
        }

        registerQualityWeightAssetStore();
        registerQualityEligibilityAssetStore();
        registerQualityModifierAssetStore();
        registerNpcQualityAssetStore();

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                QualityWeightProfileAsset.class,
                QualityAssetLoader::onQualityWeightAssetsLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                QualityModifierAsset.class,
                QualityAssetLoader::onQualityModifierAssetsLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                QualityEligibilityAsset.class,
                QualityAssetLoader::onQualityEligibilityAssetsLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                NPCQualityRuleAsset.class,
                QualityAssetLoader::onNpcQualityAssetsLoaded
        );

        initialized = true;
        LOGGER.info("Hyforged quality asset loading initialized");
    }

    private static void registerQualityWeightAssetStore() {
        AssetStore<String, QualityWeightProfileAsset, IndexedLookupTableAssetMap<String, QualityWeightProfileAsset>> store =
                ((HytaleAssetStore.Builder<String, QualityWeightProfileAsset, IndexedLookupTableAssetMap<String, QualityWeightProfileAsset>>)
                        ((HytaleAssetStore.Builder<String, QualityWeightProfileAsset, IndexedLookupTableAssetMap<String, QualityWeightProfileAsset>>)
                                ((HytaleAssetStore.Builder<String, QualityWeightProfileAsset, IndexedLookupTableAssetMap<String, QualityWeightProfileAsset>>)
                                        ((HytaleAssetStore.Builder<String, QualityWeightProfileAsset, IndexedLookupTableAssetMap<String, QualityWeightProfileAsset>>)
                                                HytaleAssetStore.builder(
                                                        QualityWeightProfileAsset.class,
                                                        new IndexedLookupTableAssetMap<>(QualityWeightProfileAsset[]::new)
                                                )
                                                        .setPath(QUALITY_WEIGHTS_PATH))
                                                .setReplaceOnRemove(key -> new QualityWeightProfileAsset()))
                                        .setCodec(QualityWeightProfileAsset.CODEC))
                                .setKeyFunction(QualityWeightProfileAsset::getId))
                        .build();

        AssetRegistry.register(store);
    }

    private static void registerQualityEligibilityAssetStore() {
        AssetStore<String, QualityEligibilityAsset, IndexedLookupTableAssetMap<String, QualityEligibilityAsset>> store =
                ((HytaleAssetStore.Builder<String, QualityEligibilityAsset, IndexedLookupTableAssetMap<String, QualityEligibilityAsset>>)
                        ((HytaleAssetStore.Builder<String, QualityEligibilityAsset, IndexedLookupTableAssetMap<String, QualityEligibilityAsset>>)
                                ((HytaleAssetStore.Builder<String, QualityEligibilityAsset, IndexedLookupTableAssetMap<String, QualityEligibilityAsset>>)
                                        ((HytaleAssetStore.Builder<String, QualityEligibilityAsset, IndexedLookupTableAssetMap<String, QualityEligibilityAsset>>)
                                                HytaleAssetStore.builder(
                                                        QualityEligibilityAsset.class,
                                                        new IndexedLookupTableAssetMap<>(QualityEligibilityAsset[]::new)
                                                )
                                                        .setPath(QUALITY_ELIGIBILITY_PATH))
                                                .setReplaceOnRemove(key -> new QualityEligibilityAsset()))
                                        .setCodec(QualityEligibilityAsset.CODEC))
                                .setKeyFunction(QualityEligibilityAsset::getId))
                        .build();

        AssetRegistry.register(store);
    }

    private static void registerQualityModifierAssetStore() {
        AssetStore<String, QualityModifierAsset, IndexedLookupTableAssetMap<String, QualityModifierAsset>> store =
                ((HytaleAssetStore.Builder<String, QualityModifierAsset, IndexedLookupTableAssetMap<String, QualityModifierAsset>>)
                        ((HytaleAssetStore.Builder<String, QualityModifierAsset, IndexedLookupTableAssetMap<String, QualityModifierAsset>>)
                                ((HytaleAssetStore.Builder<String, QualityModifierAsset, IndexedLookupTableAssetMap<String, QualityModifierAsset>>)
                                        ((HytaleAssetStore.Builder<String, QualityModifierAsset, IndexedLookupTableAssetMap<String, QualityModifierAsset>>)
                                                HytaleAssetStore.builder(
                                                        QualityModifierAsset.class,
                                                        new IndexedLookupTableAssetMap<>(QualityModifierAsset[]::new)
                                                )
                                                        .setPath(QUALITY_MODIFIERS_PATH))
                                                .setReplaceOnRemove(key -> new QualityModifierAsset()))
                                        .setCodec(QualityModifierAsset.CODEC))
                                .setKeyFunction(QualityModifierAsset::getId))
                        .build();

        AssetRegistry.register(store);
    }

    private static void registerNpcQualityAssetStore() {
        AssetStore<String, NPCQualityRuleAsset, IndexedLookupTableAssetMap<String, NPCQualityRuleAsset>> store =
                ((HytaleAssetStore.Builder<String, NPCQualityRuleAsset, IndexedLookupTableAssetMap<String, NPCQualityRuleAsset>>)
                        ((HytaleAssetStore.Builder<String, NPCQualityRuleAsset, IndexedLookupTableAssetMap<String, NPCQualityRuleAsset>>)
                                ((HytaleAssetStore.Builder<String, NPCQualityRuleAsset, IndexedLookupTableAssetMap<String, NPCQualityRuleAsset>>)
                                        ((HytaleAssetStore.Builder<String, NPCQualityRuleAsset, IndexedLookupTableAssetMap<String, NPCQualityRuleAsset>>)
                                                HytaleAssetStore.builder(
                                                        NPCQualityRuleAsset.class,
                                                        new IndexedLookupTableAssetMap<>(NPCQualityRuleAsset[]::new)
                                                )
                                                        .setPath(NPC_QUALITY_PATH))
                                                .setReplaceOnRemove(key -> new NPCQualityRuleAsset()))
                                        .setCodec(NPCQualityRuleAsset.CODEC))
                                .setKeyFunction(NPCQualityRuleAsset::getId))
                        .build();

        AssetRegistry.register(store);
    }

    private static void onQualityWeightAssetsLoaded(
            LoadedAssetsEvent<String, QualityWeightProfileAsset, IndexedLookupTableAssetMap<String, QualityWeightProfileAsset>> event
    ) {
        QualityWeightRegistry registry = QualityWeightRegistry.get();
        int loaded = 0;
        int failed = 0;

        for (QualityWeightProfileAsset asset : event.getLoadedAssets().values()) {
            try {
                QualityWeightProfile profile = asset.toProfile();
                registry.register(profile);
                loaded++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load quality weight profile: " + asset.getId(), e);
                failed++;
            }
        }

        LOGGER.info(String.format("Loaded %d quality weight profiles (%d failed)", loaded, failed));
    }

    private static void onQualityModifierAssetsLoaded(
            LoadedAssetsEvent<String, QualityModifierAsset, IndexedLookupTableAssetMap<String, QualityModifierAsset>> event
    ) {
        QualityModifierRegistry registry = QualityModifierRegistry.get();
        int loaded = 0;
        int failed = 0;

        for (QualityModifierAsset asset : event.getLoadedAssets().values()) {
            try {
                QualityModifierConfig config = asset.toModifierConfig();
                registry.register(config);
                loaded++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load quality modifier config: " + asset.getId(), e);
                failed++;
            }
        }

        LOGGER.info(String.format("Loaded %d quality modifier configs (%d failed)", loaded, failed));
    }

    private static void onQualityEligibilityAssetsLoaded(
            LoadedAssetsEvent<String, QualityEligibilityAsset, IndexedLookupTableAssetMap<String, QualityEligibilityAsset>> event
    ) {
        QualityEligibilityRegistry registry = QualityEligibilityRegistry.get();
        QualityWeightRegistry weightRegistry = QualityWeightRegistry.get();
        int loaded = 0;
        int skipped = 0;

        for (QualityEligibilityAsset asset : event.getLoadedAssets().values()) {
            try {
                QualityEligibilityRule rule = asset.toRule();
                if (!weightRegistry.contains(rule.weightProfileId())) {
                    LOGGER.log(Level.WARNING, "Quality eligibility rule {0} references missing profile: {1}",
                            new Object[]{rule.id(), rule.weightProfileId()});
                    skipped++;
                    continue;
                }
                registry.register(rule);
                loaded++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load quality eligibility rule: " + asset.getId(), e);
                skipped++;
            }
        }

        LOGGER.info(String.format("Loaded %d quality eligibility rules (%d skipped)", loaded, skipped));
    }

    private static void onNpcQualityAssetsLoaded(
            LoadedAssetsEvent<String, NPCQualityRuleAsset, IndexedLookupTableAssetMap<String, NPCQualityRuleAsset>> event
    ) {
        NPCQualityRegistry registry = NPCQualityRegistry.get();
        int loaded = 0;
        int failed = 0;

        for (NPCQualityRuleAsset asset : event.getLoadedAssets().values()) {
            try {
                NPCQualityRule rule = asset.toRule();
                registry.register(rule);
                loaded++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load NPC quality rule: " + asset.getId(), e);
                failed++;
            }
        }

        LOGGER.info(String.format("Loaded %d NPC quality rules (%d failed)", loaded, failed));
    }
}
