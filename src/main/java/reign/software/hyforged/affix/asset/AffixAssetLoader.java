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
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Handles loading affix-related assets from JSON files.
 * <p>
 * Registers asset stores for:
 * - AffixTypeAsset (Server/Hyforged/Affixes/Types/)
 * - QualityAffixRuleAsset (Server/Hyforged/Quality/AffixRules/)
 * - AffixDefinitionAsset (Server/Hyforged/Affixes/Definitions/)
 * - AffixPoolAsset (Server/Hyforged/Affixes/Pools/)
 * <p>
 * Assets are loaded into their respective registries when LoadedAssetsEvent fires.
 */
public final class AffixAssetLoader {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Path for affix type assets */
    public static final String AFFIX_TYPE_PATH = "Hyforged/Affixes/Types";

    /** Path for quality affix rule assets */
    public static final String QUALITY_RULE_PATH = "Hyforged/Quality/AffixRules";

    /** Path for affix definition assets */
    public static final String AFFIX_DEFINITION_PATH = "Hyforged/Affixes/Definitions";

    /** Path for affix pool assets */
    public static final String AFFIX_POOL_PATH = "Hyforged/Affixes/Pools";

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
            LOGGER.atWarning().log("AffixAssetLoader already initialized");
            return;
        }

        LOGGER.atInfo().log("Initializing Hyforged affix asset loading...");

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
        LOGGER.atInfo().log("Hyforged affix asset loading initialized");
    }

    // ========== Asset Store Registration ==========

    private static void registerAffixTypeAssetStore() {
        AssetStore<String, AffixTypeAsset, IndexedLookupTableAssetMap<String, AffixTypeAsset>> store =
                ((HytaleAssetStore.Builder<String, AffixTypeAsset, IndexedLookupTableAssetMap<String, AffixTypeAsset>>)
                        ((HytaleAssetStore.Builder<String, AffixTypeAsset, IndexedLookupTableAssetMap<String, AffixTypeAsset>>)
                                ((HytaleAssetStore.Builder<String, AffixTypeAsset, IndexedLookupTableAssetMap<String, AffixTypeAsset>>)
                                        ((HytaleAssetStore.Builder<String, AffixTypeAsset, IndexedLookupTableAssetMap<String, AffixTypeAsset>>)
                                                HytaleAssetStore.builder(
                                                        AffixTypeAsset.class,
                                                        new IndexedLookupTableAssetMap<>(AffixTypeAsset[]::new)
                                                )
                                                        .setPath(AFFIX_TYPE_PATH))
                                                .setReplaceOnRemove(key -> new AffixTypeAsset()))
                                        .setCodec(AffixTypeAsset.CODEC))
                                .setKeyFunction(AffixTypeAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered AffixTypeAsset store at path: %s", AFFIX_TYPE_PATH);
    }

    private static void registerQualityRuleAssetStore() {
        AssetStore<String, QualityAffixRuleAsset, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>> store =
                ((HytaleAssetStore.Builder<String, QualityAffixRuleAsset, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>>)
                        ((HytaleAssetStore.Builder<String, QualityAffixRuleAsset, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>>)
                                ((HytaleAssetStore.Builder<String, QualityAffixRuleAsset, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>>)
                                        ((HytaleAssetStore.Builder<String, QualityAffixRuleAsset, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>>)
                                                HytaleAssetStore.builder(
                                                        QualityAffixRuleAsset.class,
                                                        new IndexedLookupTableAssetMap<>(QualityAffixRuleAsset[]::new)
                                                )
                                                        .setPath(QUALITY_RULE_PATH))
                                                .setReplaceOnRemove(key -> new QualityAffixRuleAsset()))
                                        .setCodec(QualityAffixRuleAsset.CODEC))
                                .setKeyFunction(QualityAffixRuleAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered QualityAffixRuleAsset store at path: %s", QUALITY_RULE_PATH);
    }

    private static void registerAffixDefinitionAssetStore() {
        AssetStore<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>> store =
                ((HytaleAssetStore.Builder<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>>)
                        ((HytaleAssetStore.Builder<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>>)
                                ((HytaleAssetStore.Builder<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>>)
                                        ((HytaleAssetStore.Builder<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>>)
                                                HytaleAssetStore.builder(
                                                        AffixDefinitionAsset.class,
                                                        new IndexedLookupTableAssetMap<>(AffixDefinitionAsset[]::new)
                                                )
                                                        .setPath(AFFIX_DEFINITION_PATH))
                                                .setReplaceOnRemove(key -> new AffixDefinitionAsset()))
                                        .setCodec(AffixDefinitionAsset.CODEC))
                                .setKeyFunction(AffixDefinitionAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered AffixDefinitionAsset store at path: %s", AFFIX_DEFINITION_PATH);
    }

    private static void registerAffixPoolAssetStore() {
        AssetStore<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>> store =
                ((HytaleAssetStore.Builder<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>>)
                        ((HytaleAssetStore.Builder<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>>)
                                ((HytaleAssetStore.Builder<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>>)
                                        ((HytaleAssetStore.Builder<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>>)
                                                HytaleAssetStore.builder(
                                                        AffixPoolAsset.class,
                                                        new IndexedLookupTableAssetMap<>(AffixPoolAsset[]::new)
                                                )
                                                        .setPath(AFFIX_POOL_PATH))
                                                .setReplaceOnRemove(key -> new AffixPoolAsset()))
                                        .setCodec(AffixPoolAsset.CODEC))
                                .setKeyFunction(AffixPoolAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered AffixPoolAsset store at path: %s", AFFIX_POOL_PATH);
    }

    // ========== Asset Load Event Handlers ==========

    private static void onAffixTypeAssetsLoaded(
            LoadedAssetsEvent<String, AffixTypeAsset, IndexedLookupTableAssetMap<String, AffixTypeAsset>> event
    ) {
        LOGGER.atInfo().log("Loading affix type definitions from assets...");

        AffixTypeRegistry registry = AffixTypeRegistry.get();
        int loaded = 0;
        int failed = 0;

        for (AffixTypeAsset asset : event.getLoadedAssets().values()) {
            try {
                AffixType type = asset.toAffixType();
                registry.register(type);
                loaded++;
                LOGGER.at(Level.FINE).log("Loaded affix type: %s", type.id());
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to load affix type: %s", asset.getId());
                failed++;
            }
        }

        LOGGER.atInfo().log("Loaded %s affix types (%s failed)", loaded, failed);
    }

    private static void onQualityRuleAssetsLoaded(
            LoadedAssetsEvent<String, QualityAffixRuleAsset, IndexedLookupTableAssetMap<String, QualityAffixRuleAsset>> event
    ) {
        LOGGER.atInfo().log("Loading quality affix rules from assets...");

        QualityAffixRuleRegistry registry = QualityAffixRuleRegistry.get();
        int loaded = 0;
        int failed = 0;

        for (QualityAffixRuleAsset asset : event.getLoadedAssets().values()) {
            try {
                QualityAffixRule rule = asset.toQualityAffixRule();
                registry.register(rule);
                loaded++;
                LOGGER.at(Level.FINE).log("Loaded quality affix rule: %s", rule.quality());
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to load quality affix rule: %s", asset.getId());
                failed++;
            }
        }

        LOGGER.atInfo().log("Loaded %s quality affix rules (%s failed)", loaded, failed);
    }

    private static void onAffixDefinitionAssetsLoaded(
            LoadedAssetsEvent<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>> event
    ) {
        LOGGER.atInfo().log("Loading affix definitions from assets...");

        AffixDefinitionRegistry registry = AffixDefinitionRegistry.get();
        int loaded = 0;
        int failed = 0;

        for (AffixDefinitionAsset asset : event.getLoadedAssets().values()) {
            try {
                AffixDefinition affix = asset.toAffixDefinition();
                registry.register(affix);
                loaded++;
                LOGGER.at(Level.FINE).log("Loaded affix: %s", affix.id());
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to load affix: %s", asset.getId());
                failed++;
            }
        }

        LOGGER.atInfo().log("Loaded %s affixes (%s failed)", loaded, failed);
    }

    private static void onAffixPoolAssetsLoaded(
            LoadedAssetsEvent<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>> event
    ) {
        LOGGER.atInfo().log("Loading affix pools from assets...");

        AffixPoolRegistry registry = AffixPoolRegistry.get();
        int loaded = 0;
        int failed = 0;

        for (AffixPoolAsset asset : event.getLoadedAssets().values()) {
            try {
                AffixPool pool = asset.toAffixPool();
                registry.register(pool);
                loaded++;
                LOGGER.at(Level.FINE).log("Loaded affix pool: %s", pool.id());
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to load affix pool: %s", asset.getId());
                failed++;
            }
        }

        LOGGER.atInfo().log("Loaded %s affix pools (%s failed)", loaded, failed);
    }
}
