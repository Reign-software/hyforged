package reign.software.hyforged.stats.asset;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Handles loading class definitions from JSON assets.
 * <p>
 * This class registers asset stores for ClassDefinitionAsset,
 * then loads and registers them with the ClassDefinitionRegistry
 * on asset load events.
 * <p>
 * Classes define the base ability score distribution for players.
 * <p>
 * Conflict Resolution Policy:
 * - By default, duplicate IDs result in an error log
 * - The first definition wins (core Hyforged loads first, then mods by priority)
 */
public final class ClassAssetLoader {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Path for class definition assets relative to asset root */
    public static final String CLASS_ASSET_PATH = "Hyforged/Stats/Classes";

    private static boolean initialized = false;

    private ClassAssetLoader() {
        // Utility class
    }

    /**
     * Initialize the asset stores and register event handlers.
     * Should be called during plugin setup, after StatAssetLoader.
     */
    public static void initialize(@Nonnull JavaPlugin plugin) {
        if (initialized) {
            LOGGER.atWarning().log("ClassAssetLoader already initialized");
            return;
        }

        LOGGER.atInfo().log("Initializing Hyforged class asset loading...");

        // Register class definition asset store
        registerClassAssetStore();

        // Register event handlers for when assets are loaded
        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                ClassDefinitionAsset.class,
                ClassAssetLoader::onClassAssetsLoaded
        );

        initialized = true;
        LOGGER.atInfo().log("Hyforged class asset loading initialized");
    }

    /**
     * Register the asset store for class definitions.
     */
    private static void registerClassAssetStore() {
        AssetStore<String, ClassDefinitionAsset, IndexedLookupTableAssetMap<String, ClassDefinitionAsset>> store =
                ((HytaleAssetStore.Builder<String, ClassDefinitionAsset, IndexedLookupTableAssetMap<String, ClassDefinitionAsset>>)
                        ((HytaleAssetStore.Builder<String, ClassDefinitionAsset, IndexedLookupTableAssetMap<String, ClassDefinitionAsset>>)
                                ((HytaleAssetStore.Builder<String, ClassDefinitionAsset, IndexedLookupTableAssetMap<String, ClassDefinitionAsset>>)
                                        ((HytaleAssetStore.Builder<String, ClassDefinitionAsset, IndexedLookupTableAssetMap<String, ClassDefinitionAsset>>)
                                                HytaleAssetStore.builder(
                                                        ClassDefinitionAsset.class,
                                                        new IndexedLookupTableAssetMap<>(ClassDefinitionAsset[]::new)
                                                )
                                                        .setPath(CLASS_ASSET_PATH))
                                                .setReplaceOnRemove(key -> new ClassDefinitionAsset()))
                                        .setCodec(ClassDefinitionAsset.CODEC))
                                .setKeyFunction(ClassDefinitionAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered ClassDefinitionAsset store at path: %s", CLASS_ASSET_PATH);
    }

    /**
     * Handle class assets loaded event.
     */
    private static void onClassAssetsLoaded(
            LoadedAssetsEvent<String, ClassDefinitionAsset, IndexedLookupTableAssetMap<String, ClassDefinitionAsset>> event
    ) {
        LOGGER.atInfo().log("Loading class definitions from assets...");

        ClassDefinitionRegistry registry = ClassDefinitionRegistry.get();
        Map<String, String> conflicts = new HashMap<>();
        int loaded = 0;
        int skipped = 0;

        for (ClassDefinitionAsset asset : event.getLoadedAssets().values()) {
            String id = asset.getId();

            // Check for conflicts
            if (registry.get(id) != null) {
                conflicts.put(id, "Duplicate class ID - skipping");
                skipped++;
                continue;
            }

            try {
                ClassDefinition classDef = asset.toClassDefinition();
                registry.register(classDef);
                loaded++;
                LOGGER.at(Level.FINE).log("Loaded class definition: %s", id);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to load class definition: %s", id);
                skipped++;
            }
        }

        // Log conflicts as errors
        for (Map.Entry<String, String> entry : conflicts.entrySet()) {
            LOGGER.atSevere().log("Class definition conflict: %s - %s", entry.getKey(), entry.getValue());
        }

        LOGGER.atInfo().log("Loaded %s class definitions from assets (%s skipped)", loaded, skipped);
    }

    /**
     * Check if the loader has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
