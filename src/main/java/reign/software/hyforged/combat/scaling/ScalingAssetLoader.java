package reign.software.hyforged.combat.scaling;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles loading scaling-related assets from JSON files.
 * <p>
 * Registers asset stores for:
 * <ul>
 *   <li>WorldScalingConfigAsset (Server/Hyforged/Combat/WorldScaling/) - Level calculation</li>
 *   <li>MonsterScalingConfigAsset (Server/Hyforged/Combat/MonsterScaling/) - Per-NPC stat scaling</li>
 * </ul>
 * <p>
 * Assets are loaded and registered with {@link MonsterScalingService} when LoadedAssetsEvent fires.
 */
public final class ScalingAssetLoader {

    private static final Logger LOGGER = Logger.getLogger(ScalingAssetLoader.class.getName());

    /** Path for world scaling config assets */
    public static final String WORLD_SCALING_PATH = "Hyforged/Combat/WorldScaling";

    /** Path for monster scaling config assets */
    public static final String MONSTER_SCALING_PATH = "Hyforged/Combat/MonsterScaling";

    private static boolean initialized = false;

    private ScalingAssetLoader() {
        // Utility class
    }

    /**
     * Initialize the asset stores and register event handlers.
     * Should be called during plugin setup.
     * 
     * @param plugin The plugin instance
     */
    public static void initialize(@Nonnull JavaPlugin plugin) {
        if (initialized) {
            LOGGER.warning("ScalingAssetLoader already initialized");
            return;
        }

        LOGGER.info("Initializing Hyforged scaling asset loading...");

        // Register asset stores
        registerWorldScalingAssetStore();
        registerMonsterScalingAssetStore();

        // Register event handlers for when assets are loaded
        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                WorldScalingConfigAsset.class,
                ScalingAssetLoader::onWorldScalingAssetsLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                MonsterScalingConfigAsset.class,
                ScalingAssetLoader::onMonsterScalingAssetsLoaded
        );

        initialized = true;
        LOGGER.info("Hyforged scaling asset loading initialized");
    }

    // ========== Asset Store Registration ==========

    private static void registerWorldScalingAssetStore() {
        AssetStore<String, WorldScalingConfigAsset, IndexedLookupTableAssetMap<String, WorldScalingConfigAsset>> store =
                ((HytaleAssetStore.Builder<String, WorldScalingConfigAsset, IndexedLookupTableAssetMap<String, WorldScalingConfigAsset>>)
                        ((HytaleAssetStore.Builder<String, WorldScalingConfigAsset, IndexedLookupTableAssetMap<String, WorldScalingConfigAsset>>)
                                ((HytaleAssetStore.Builder<String, WorldScalingConfigAsset, IndexedLookupTableAssetMap<String, WorldScalingConfigAsset>>)
                                        HytaleAssetStore.builder(
                                                WorldScalingConfigAsset.class,
                                                new IndexedLookupTableAssetMap<>(WorldScalingConfigAsset[]::new)
                                        )
                                                .setPath(WORLD_SCALING_PATH))
                                        .setCodec(WorldScalingConfigAsset.CODEC))
                                .setKeyFunction(WorldScalingConfigAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered WorldScalingConfigAsset store at path: " + WORLD_SCALING_PATH);
    }

    private static void registerMonsterScalingAssetStore() {
        AssetStore<String, MonsterScalingConfigAsset, IndexedLookupTableAssetMap<String, MonsterScalingConfigAsset>> store =
                ((HytaleAssetStore.Builder<String, MonsterScalingConfigAsset, IndexedLookupTableAssetMap<String, MonsterScalingConfigAsset>>)
                        ((HytaleAssetStore.Builder<String, MonsterScalingConfigAsset, IndexedLookupTableAssetMap<String, MonsterScalingConfigAsset>>)
                                ((HytaleAssetStore.Builder<String, MonsterScalingConfigAsset, IndexedLookupTableAssetMap<String, MonsterScalingConfigAsset>>)
                                        HytaleAssetStore.builder(
                                                MonsterScalingConfigAsset.class,
                                                new IndexedLookupTableAssetMap<>(MonsterScalingConfigAsset[]::new)
                                        )
                                                .setPath(MONSTER_SCALING_PATH))
                                        .setCodec(MonsterScalingConfigAsset.CODEC))
                                .setKeyFunction(MonsterScalingConfigAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered MonsterScalingConfigAsset store at path: " + MONSTER_SCALING_PATH);
    }

    // ========== Asset Load Event Handlers ==========

    private static void onWorldScalingAssetsLoaded(
            LoadedAssetsEvent<String, WorldScalingConfigAsset, IndexedLookupTableAssetMap<String, WorldScalingConfigAsset>> event
    ) {
        LOGGER.info("Loading world scaling configurations from assets...");

        MonsterScalingService service = MonsterScalingService.get();
        int loaded = 0;

        for (WorldScalingConfigAsset asset : event.getLoadedAssets().values()) {
            try {
                WorldScalingConfig config = asset.toWorldScalingConfig();
                
                // Use the first one found (or could use naming convention like "default-scaling")
                if (loaded == 0 || asset.getId().contains("default")) {
                    service.setActiveConfig(config);
                    LOGGER.info("Set active world scaling config: " + config.id());
                }
                
                loaded++;
                LOGGER.fine("Loaded world scaling config: " + config.id());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load world scaling config: " + asset.getId(), e);
            }
        }

        LOGGER.info(String.format("Loaded %d world scaling configurations", loaded));
    }

    private static void onMonsterScalingAssetsLoaded(
            LoadedAssetsEvent<String, MonsterScalingConfigAsset, IndexedLookupTableAssetMap<String, MonsterScalingConfigAsset>> event
    ) {
        LOGGER.info("Loading monster scaling configurations from assets...");

        MonsterScalingService service = MonsterScalingService.get();
        int loaded = 0;
        int rolesRegistered = 0;

        for (MonsterScalingConfigAsset asset : event.getLoadedAssets().values()) {
            try {
                service.registerScalingConfig(asset);
                rolesRegistered += asset.getAppliesTo().size();
                loaded++;
                LOGGER.fine("Loaded monster scaling config: " + asset.getId() 
                        + " (applies to " + asset.getAppliesTo().size() + " roles)");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load monster scaling config: " + asset.getId(), e);
            }
        }

        LOGGER.info(String.format("Loaded %d monster scaling configurations (%d NPC roles registered)", 
                loaded, rolesRegistered));
    }

    /**
     * Check if the loader has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Reset initialization state (for testing).
     */
    public static void resetForTesting() {
        initialized = false;
    }
}
