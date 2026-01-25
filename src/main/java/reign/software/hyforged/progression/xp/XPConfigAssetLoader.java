package reign.software.hyforged.progression.xp;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Handles loading the XP configuration from JSON assets.
 * <p>
 * Loads: Server/Hyforged/Progression/XPConfig.json
 * <p>
 * This loader registers an asset store for XPConfigAsset and
 * applies the loaded configuration to the XPConfig singleton.
 */
public final class XPConfigAssetLoader {

    private static final Logger LOGGER = Logger.getLogger(XPConfigAssetLoader.class.getName());

    /** Path for XP config asset relative to asset root */
    public static final String XP_CONFIG_ASSET_PATH = "Hyforged/Progression";

    /** Asset ID used for the config file */
    private static final String CONFIG_ID = "XPConfig";

    private static boolean initialized = false;

    private XPConfigAssetLoader() {
        // Utility class
    }

    /**
     * Initialize the asset store and register event handlers.
     * Should be called during plugin setup.
     *
     * @param plugin The plugin instance
     */
    public static void initialize(@Nonnull JavaPlugin plugin) {
        if (initialized) {
            LOGGER.warning("XPConfigAssetLoader already initialized");
            return;
        }

        LOGGER.info("Initializing XP config asset loading...");

        // Register XP config asset store
        registerXPConfigAssetStore();

        // Register event handler for when assets are loaded
        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                XPConfigAsset.class,
                XPConfigAssetLoader::onXPConfigAssetsLoaded
        );

        initialized = true;
        LOGGER.info("XP config asset loading initialized");
    }

    /**
     * Register the asset store for XP configuration.
     */
    private static void registerXPConfigAssetStore() {
        AssetStore<String, XPConfigAsset, IndexedLookupTableAssetMap<String, XPConfigAsset>> store =
                ((HytaleAssetStore.Builder<String, XPConfigAsset, IndexedLookupTableAssetMap<String, XPConfigAsset>>)
                        ((HytaleAssetStore.Builder<String, XPConfigAsset, IndexedLookupTableAssetMap<String, XPConfigAsset>>)
                                ((HytaleAssetStore.Builder<String, XPConfigAsset, IndexedLookupTableAssetMap<String, XPConfigAsset>>)
                                        ((HytaleAssetStore.Builder<String, XPConfigAsset, IndexedLookupTableAssetMap<String, XPConfigAsset>>)
                                                HytaleAssetStore.builder(
                                                        XPConfigAsset.class,
                                                        new IndexedLookupTableAssetMap<>(XPConfigAsset[]::new)
                                                )
                                                        .setPath(XP_CONFIG_ASSET_PATH))
                                                .setReplaceOnRemove(key -> new XPConfigAsset()))
                                        .setCodec(XPConfigAsset.CODEC))
                                .setKeyFunction(a -> CONFIG_ID))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered XPConfigAsset store at path: " + XP_CONFIG_ASSET_PATH);
    }

    /**
     * Handle XP config assets loaded event.
     *
     * @param event The loaded assets event
     */
    private static void onXPConfigAssetsLoaded(
            LoadedAssetsEvent<String, XPConfigAsset, IndexedLookupTableAssetMap<String, XPConfigAsset>> event
    ) {
        LOGGER.info("Loading XP configuration from assets...");

        // Look for our config asset in loaded assets
        for (XPConfigAsset configAsset : event.getLoadedAssets().values()) {
            LOGGER.info("Found XP configuration asset, applying...");
            XPConfig.get().applyFromAsset(configAsset);
            LOGGER.info("XP configuration loaded successfully");
            return;
        }
        
        LOGGER.warning("XPConfig.json not found, using default values");
    }
}
