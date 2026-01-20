package reign.software.hyforged.affix.asset;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.AffixPool;
import reign.software.hyforged.affix.model.AffixType;
import reign.software.hyforged.affix.model.QualityAffixRule;
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.affix.registry.AffixPoolRegistry;
import reign.software.hyforged.affix.registry.AffixTypeRegistry;
import reign.software.hyforged.affix.registry.QualityAffixRuleRegistry;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles loading affix-related assets from JSON files.
 * <p>
 * Registers asset stores for:
 * - AffixTypeAsset (Server/Hyforged/AffixTypes/)
 * - QualityAffixRuleAsset (Server/Hyforged/QualityAffixRules/)
 * - AffixDefinitionAsset (Server/Hyforged/Affixes/)
 * - AffixPoolAsset (Server/Hyforged/AffixPools/)
 * <p>
 * Assets are loaded into their respective registries when LoadedAssetsEvent fires.
 */
public final class AffixAssetLoader {

    private static final Logger LOGGER = Logger.getLogger(AffixAssetLoader.class.getName());

    /** Path for affix type assets */
    public static final String AFFIX_TYPE_PATH = "Hyforged/AffixTypes";

    /** Path for quality affix rule assets */
    public static final String QUALITY_RULE_PATH = "Hyforged/QualityAffixRules";

    /** Path for affix definition assets */
    public static final String AFFIX_DEFINITION_PATH = "Hyforged/Affixes";

    /** Path for affix pool assets */
    public static final String AFFIX_POOL_PATH = "Hyforged/AffixPools";

    private static boolean initialized = false;

    private AffixAssetLoader() {
        // Utility class
    }

    /**
     * Initialize the asset stores and register event handlers.
     * Should be called during plugin setup.
     */
    public static void initialize(@Nonnull JavaPlugin plugin) {
        if (initialized) {
            LOGGER.warning("AffixAssetLoader already initialized");
            return;
        }

        LOGGER.info("Initializing Hyforged affix asset loading...");

        // Register all asset stores
        registerAffixTypeAssetStore();
        registerQualityRuleAssetStore();
        registerAffixDefinitionAssetStore();
        registerAffixPoolAssetStore();

        // Register event handlers for when assets are loaded
        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                AffixTypeAsset.class,
                AffixAssetLoader::onAffixTypeAssetsLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                QualityAffixRuleAsset.class,
                AffixAssetLoader::onQualityRuleAssetsLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                AffixDefinitionAsset.class,
                AffixAssetLoader::onAffixDefinitionAssetsLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                AffixPoolAsset.class,
                AffixAssetLoader::onAffixPoolAssetsLoaded
        );

        initialized = true;
        LOGGER.info("Hyforged affix asset loading initialized");
    }

    // ========== Asset Store Registration ==========

    private static void registerAffixTypeAssetStore() {
        AssetStore<String, AffixTypeAsset, IndexedLookupTableAssetMap<String, AffixTypeAsset>> store =
                ((HytaleAssetStore.Builder<String, AffixTypeAsset, IndexedLookupTableAssetMap<String, AffixTypeAsset>>)
                        ((HytaleAssetStore.Builder<String, AffixTypeAsset, IndexedLookupTableAssetMap<String, AffixTypeAsset>>)
                                ((HytaleAssetStore.Builder<String, AffixTypeAsset, IndexedLookupTableAssetMap<String, AffixTypeAsset>>)
                                        HytaleAssetStore.builder(
                                                AffixTypeAsset.class,
                                                new IndexedLookupTableAssetMap<>(AffixTypeAsset[]::new)
                                        )
                                                .setPath(AFFIX_TYPE_PATH))
                                        .setCodec(AffixTypeAsset.CODEC))
                                .setKeyFunction(AffixTypeAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered AffixTypeAsset store at path: " + AFFIX_TYPE_PATH);
    }

    private static void registerQualityRuleAssetStore() {
        AssetStore<String, QualityAffixRuleAsset, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>> store =
                ((HytaleAssetStore.Builder<String, QualityAffixRuleAsset, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>>)
                        ((HytaleAssetStore.Builder<String, QualityAffixRuleAsset, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>>)
                                ((HytaleAssetStore.Builder<String, QualityAffixRuleAsset, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>>)
                                        HytaleAssetStore.builder(
                                                QualityAffixRuleAsset.class,
                                                new IndexedLookupTableAssetMap<>(QualityAffixRuleAsset[]::new)
                                        )
                                                .setPath(QUALITY_RULE_PATH))
                                        .setCodec(QualityAffixRuleAsset.CODEC))
                                .setKeyFunction(QualityAffixRuleAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered QualityAffixRuleAsset store at path: " + QUALITY_RULE_PATH);
    }

    private static void registerAffixDefinitionAssetStore() {
        AssetStore<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>> store =
                ((HytaleAssetStore.Builder<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>>)
                        ((HytaleAssetStore.Builder<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>>)
                                ((HytaleAssetStore.Builder<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>>)
                                        HytaleAssetStore.builder(
                                                AffixDefinitionAsset.class,
                                                new IndexedLookupTableAssetMap<>(AffixDefinitionAsset[]::new)
                                        )
                                                .setPath(AFFIX_DEFINITION_PATH))
                                        .setCodec(AffixDefinitionAsset.CODEC))
                                .setKeyFunction(AffixDefinitionAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered AffixDefinitionAsset store at path: " + AFFIX_DEFINITION_PATH);
    }

    private static void registerAffixPoolAssetStore() {
        AssetStore<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>> store =
                ((HytaleAssetStore.Builder<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>>)
                        ((HytaleAssetStore.Builder<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>>)
                                ((HytaleAssetStore.Builder<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>>)
                                        HytaleAssetStore.builder(
                                                AffixPoolAsset.class,
                                                new IndexedLookupTableAssetMap<>(AffixPoolAsset[]::new)
                                        )
                                                .setPath(AFFIX_POOL_PATH))
                                        .setCodec(AffixPoolAsset.CODEC))
                                .setKeyFunction(AffixPoolAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered AffixPoolAsset store at path: " + AFFIX_POOL_PATH);
    }

    // ========== Asset Load Event Handlers ==========

    private static void onAffixTypeAssetsLoaded(
            LoadedAssetsEvent<String, AffixTypeAsset, IndexedLookupTableAssetMap<String, AffixTypeAsset>> event
    ) {
        LOGGER.info("Loading affix type definitions from assets...");

        AffixTypeRegistry registry = AffixTypeRegistry.get();
        int loaded = 0;
        int failed = 0;

        for (AffixTypeAsset asset : event.getLoadedAssets().values()) {
            try {
                AffixType type = asset.toAffixType();
                registry.register(type);
                loaded++;
                LOGGER.fine("Loaded affix type: " + type.id());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load affix type: " + asset.getId(), e);
                failed++;
            }
        }

        LOGGER.info(String.format("Loaded %d affix types (%d failed)", loaded, failed));
    }

    private static void onQualityRuleAssetsLoaded(
            LoadedAssetsEvent<String, QualityAffixRuleAsset, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>> event
    ) {
        LOGGER.info("Loading quality affix rules from assets...");

        QualityAffixRuleRegistry registry = QualityAffixRuleRegistry.get();
        int loaded = 0;
        int failed = 0;

        for (QualityAffixRuleAsset asset : event.getLoadedAssets().values()) {
            try {
                QualityAffixRule rule = asset.toQualityAffixRule();
                registry.register(rule);
                loaded++;
                LOGGER.fine("Loaded quality affix rule: " + rule.quality());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load quality affix rule: " + asset.getId(), e);
                failed++;
            }
        }

        LOGGER.info(String.format("Loaded %d quality affix rules (%d failed)", loaded, failed));
    }

    private static void onAffixDefinitionAssetsLoaded(
            LoadedAssetsEvent<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>> event
    ) {
        LOGGER.info("Loading affix definitions from assets...");

        AffixDefinitionRegistry registry = AffixDefinitionRegistry.get();
        int loaded = 0;
        int failed = 0;

        for (AffixDefinitionAsset asset : event.getLoadedAssets().values()) {
            try {
                AffixDefinition affix = asset.toAffixDefinition();
                registry.register(affix);
                loaded++;
                LOGGER.fine("Loaded affix: " + affix.id());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load affix: " + asset.getId(), e);
                failed++;
            }
        }

        LOGGER.info(String.format("Loaded %d affixes (%d failed)", loaded, failed));
    }

    private static void onAffixPoolAssetsLoaded(
            LoadedAssetsEvent<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>> event
    ) {
        LOGGER.info("Loading affix pools from assets...");

        AffixPoolRegistry registry = AffixPoolRegistry.get();
        int loaded = 0;
        int failed = 0;

        for (AffixPoolAsset asset : event.getLoadedAssets().values()) {
            try {
                AffixPool pool = asset.toAffixPool();
                registry.register(pool);
                loaded++;
                LOGGER.fine("Loaded affix pool: " + pool.id());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load affix pool: " + asset.getId(), e);
                failed++;
            }
        }

        LOGGER.info(String.format("Loaded %d affix pools (%d failed)", loaded, failed));
    }
}
