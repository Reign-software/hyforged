package reign.software.hyforged.stats.asset;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import reign.software.hyforged.stats.CategoryDefinition;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles loading stat and category definitions from JSON assets.
 * <p>
 * This class registers asset stores for StatDefinitionAsset and
 * CategoryDefinitionAsset, then loads and registers them with the 
 * StatDefinitionRegistry on asset load events.
 * <p>
 * Tags are derived from stats (via their "Tags" array) and do not
 * need separate JSON definitions.
 * <p>
 * Conflict Resolution Policy (Phase 9 requirement):
 * - By default, duplicate IDs result in an error log
 * - The first definition wins (core Hyforged loads first, then mods by priority)
 * - Future: may add options for override or merge policies
 */
public final class StatAssetLoader {

    private static final Logger LOGGER = Logger.getLogger(StatAssetLoader.class.getName());

    /** Path for stat definition assets relative to asset root */
    public static final String STAT_ASSET_PATH = "Hyforged/Stats";

    /** Path for category definition assets relative to asset root */
    public static final String CATEGORY_ASSET_PATH = "Hyforged/Categories";

    private static boolean initialized = false;

    private StatAssetLoader() {
        // Utility class
    }

    /**
     * Initialize the asset stores and register event handlers.
     * Should be called during plugin setup.
     */
    public static void initialize(@Nonnull com.hypixel.hytale.server.core.plugin.JavaPlugin plugin) {
        if (initialized) {
            LOGGER.warning("StatAssetLoader already initialized");
            return;
        }

        LOGGER.info("Initializing Hyforged stat asset loading...");

        // Register category definition asset store (must load first)
        registerCategoryAssetStore();

        // Register stat definition asset store
        registerStatAssetStore();

        // Register event handlers for when assets are loaded
        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                CategoryDefinitionAsset.class,
                StatAssetLoader::onCategoryAssetsLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                StatDefinitionAsset.class,
                StatAssetLoader::onStatAssetsLoaded
        );

        initialized = true;
        LOGGER.info("Hyforged stat asset loading initialized");
    }

    /**
     * Register the asset store for category definitions.
     */
    private static void registerCategoryAssetStore() {
        AssetStore<String, CategoryDefinitionAsset, IndexedLookupTableAssetMap<String, CategoryDefinitionAsset>> store =
                ((HytaleAssetStore.Builder<String, CategoryDefinitionAsset, IndexedLookupTableAssetMap<String, CategoryDefinitionAsset>>)
                        ((HytaleAssetStore.Builder<String, CategoryDefinitionAsset, IndexedLookupTableAssetMap<String, CategoryDefinitionAsset>>)
                                ((HytaleAssetStore.Builder<String, CategoryDefinitionAsset, IndexedLookupTableAssetMap<String, CategoryDefinitionAsset>>)
                                        ((HytaleAssetStore.Builder<String, CategoryDefinitionAsset, IndexedLookupTableAssetMap<String, CategoryDefinitionAsset>>)
                                                HytaleAssetStore.builder(
                                                        CategoryDefinitionAsset.class,
                                                        new IndexedLookupTableAssetMap<>(CategoryDefinitionAsset[]::new)
                                                )
                                                        .setPath(CATEGORY_ASSET_PATH))
                                                .setReplaceOnRemove(key -> new CategoryDefinitionAsset()))
                                        .setCodec(CategoryDefinitionAsset.CODEC))
                                .setKeyFunction(CategoryDefinitionAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered CategoryDefinitionAsset store at path: " + CATEGORY_ASSET_PATH);
    }

    /**
     * Register the asset store for stat definitions.
     */
    private static void registerStatAssetStore() {
        AssetStore<String, StatDefinitionAsset, IndexedLookupTableAssetMap<String, StatDefinitionAsset>> store =
                ((HytaleAssetStore.Builder<String, StatDefinitionAsset, IndexedLookupTableAssetMap<String, StatDefinitionAsset>>)
                        ((HytaleAssetStore.Builder<String, StatDefinitionAsset, IndexedLookupTableAssetMap<String, StatDefinitionAsset>>)
                                ((HytaleAssetStore.Builder<String, StatDefinitionAsset, IndexedLookupTableAssetMap<String, StatDefinitionAsset>>)
                                        ((HytaleAssetStore.Builder<String, StatDefinitionAsset, IndexedLookupTableAssetMap<String, StatDefinitionAsset>>)
                                                HytaleAssetStore.builder(
                                                        StatDefinitionAsset.class,
                                                        new IndexedLookupTableAssetMap<>(StatDefinitionAsset[]::new)
                                                )
                                                        .setPath(STAT_ASSET_PATH))
                                                .setReplaceOnRemove(key -> new StatDefinitionAsset()))
                                        .setCodec(StatDefinitionAsset.CODEC))
                                .setKeyFunction(StatDefinitionAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered StatDefinitionAsset store at path: " + STAT_ASSET_PATH);
    }

    /**
     * Handle category assets loaded event.
     */
    private static void onCategoryAssetsLoaded(
            LoadedAssetsEvent<String, CategoryDefinitionAsset, IndexedLookupTableAssetMap<String, CategoryDefinitionAsset>> event
    ) {
        LOGGER.info("Loading category definitions from assets...");

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        Map<String, String> conflicts = new HashMap<>();
        int loaded = 0;
        int skipped = 0;

        for (CategoryDefinitionAsset asset : event.getLoadedAssets().values()) {
            String id = asset.getId();

            // Check for conflicts
            if (registry.hasCategory(id)) {
                conflicts.put(id, "Duplicate category ID - skipping");
                skipped++;
                continue;
            }

            try {
                CategoryDefinition categoryDef = asset.toCategoryDefinition();
                registry.registerCategory(categoryDef);
                loaded++;
                LOGGER.fine("Loaded category definition: " + id);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load category definition: " + id, e);
                skipped++;
            }
        }

        // Log conflicts as errors (per Phase 9 requirement)
        for (Map.Entry<String, String> entry : conflicts.entrySet()) {
            LOGGER.severe("Category definition conflict: " + entry.getKey() + " - " + entry.getValue());
        }

        LOGGER.info("Loaded " + loaded + " category definitions from assets (" + skipped + " skipped)");
    }

    /**
     * Handle stat assets loaded event.
     */
    private static void onStatAssetsLoaded(
            LoadedAssetsEvent<String, StatDefinitionAsset, IndexedLookupTableAssetMap<String, StatDefinitionAsset>> event
    ) {
        LOGGER.info("Loading stat definitions from assets...");

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        Map<String, String> conflicts = new HashMap<>();
        int loaded = 0;
        int skipped = 0;

        for (StatDefinitionAsset asset : event.getLoadedAssets().values()) {
            String id = asset.getId();

            // Check for conflicts
            if (registry.hasStat(id)) {
                conflicts.put(id, "Duplicate stat ID - skipping");
                skipped++;
                continue;
            }

            try {
                StatDefinition statDef = asset.toStatDefinition();
                registry.registerStat(statDef);
                loaded++;
                LOGGER.fine("Loaded stat definition: " + id);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load stat definition: " + id, e);
                skipped++;
            }
        }

        // Log conflicts as errors (per Phase 9 requirement)
        for (Map.Entry<String, String> entry : conflicts.entrySet()) {
            LOGGER.severe("Stat definition conflict: " + entry.getKey() + " - " + entry.getValue());
        }

        LOGGER.info("Loaded " + loaded + " stat definitions from assets (" + skipped + " skipped)");
        
        // Validate scaling rules now that all stats are loaded
        validateScalingRules(registry);
    }
    
    /**
     * Validate scaling rules after all stats are loaded.
     * <p>
     * This checks that all source stats referenced in scaling rules exist.
     * Called after all stats are registered to avoid false positives from load order.
     */
    private static void validateScalingRules(StatDefinitionRegistry registry) {
        int missingRefs = 0;
        
        for (StatDefinition stat : registry.getAllStats()) {
            for (var rule : stat.scaling()) {
                String sourceId = rule.source().toString();
                if (!registry.hasStat(sourceId)) {
                    LOGGER.warning("Stat '" + stat.id() + "' has scaling rule referencing missing stat: " + sourceId);
                    missingRefs++;
                }
            }
        }
        
        if (missingRefs > 0) {
            LOGGER.warning("Found " + missingRefs + " scaling rules with missing source stats");
        }
    }

    /**
     * Check if the loader has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
