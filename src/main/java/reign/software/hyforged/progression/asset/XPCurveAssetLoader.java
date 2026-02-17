package reign.software.hyforged.progression.asset;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Handles loading XP curves from JSON assets.
 * <p>
 * This class registers asset stores for XPCurveAsset,
 * then loads and registers them with the XPCurveRegistry
 * on asset load events.
 * <p>
 * XP curves define the experience required for level progression.
 */
public final class XPCurveAssetLoader {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Path for XP curve assets relative to asset root */
    public static final String XP_CURVE_ASSET_PATH = "Hyforged/Progression/Curves";

    private static boolean initialized = false;

    private XPCurveAssetLoader() {
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
            LOGGER.atWarning().log("XPCurveAssetLoader already initialized");
            return;
        }

        LOGGER.atInfo().log("Initializing Hyforged XP curve asset loading...");

        // Register XP curve asset store
        registerXPCurveAssetStore();

        // Register event handlers for when assets are loaded
        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                XPCurveAsset.class,
                XPCurveAssetLoader::onXPCurveAssetsLoaded
        );

        initialized = true;
        LOGGER.atInfo().log("Hyforged XP curve asset loading initialized");
    }

    /**
     * Register the asset store for XP curves.
     */
    private static void registerXPCurveAssetStore() {
        AssetStore<String, XPCurveAsset, IndexedLookupTableAssetMap<String, XPCurveAsset>> store =
                ((HytaleAssetStore.Builder<String, XPCurveAsset, IndexedLookupTableAssetMap<String, XPCurveAsset>>)
                        ((HytaleAssetStore.Builder<String, XPCurveAsset, IndexedLookupTableAssetMap<String, XPCurveAsset>>)
                                ((HytaleAssetStore.Builder<String, XPCurveAsset, IndexedLookupTableAssetMap<String, XPCurveAsset>>)
                                        ((HytaleAssetStore.Builder<String, XPCurveAsset, IndexedLookupTableAssetMap<String, XPCurveAsset>>)
                                                HytaleAssetStore.builder(
                                                        XPCurveAsset.class,
                                                        new IndexedLookupTableAssetMap<>(XPCurveAsset[]::new)
                                                )
                                                        .setPath(XP_CURVE_ASSET_PATH))
                                                .setReplaceOnRemove(key -> new XPCurveAsset()))
                                        .setCodec(XPCurveAsset.CODEC))
                                .setKeyFunction(XPCurveAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered XPCurveAsset store at path: %s", XP_CURVE_ASSET_PATH);
    }

    /**
     * Handle XP curve assets loaded event.
     *
     * @param event The loaded assets event
     */
    private static void onXPCurveAssetsLoaded(
            LoadedAssetsEvent<String, XPCurveAsset, IndexedLookupTableAssetMap<String, XPCurveAsset>> event
    ) {
        LOGGER.atInfo().log("Loading XP curves from assets...");

        XPCurveRegistry registry = XPCurveRegistry.get();
        Map<String, String> conflicts = new HashMap<>();
        int loaded = 0;
        int skipped = 0;

        for (XPCurveAsset asset : event.getLoadedAssets().values()) {
            String id = asset.getId();

            // Check for conflicts
            if (registry.contains(id)) {
                String existingSource = "previous load";
                conflicts.put(id, existingSource);
                LOGGER.atWarning().log("XP curve conflict: '%s' already registered by %s, skipping",
                        id, existingSource);
                skipped++;
                continue;
            }

            // Convert asset to domain object and register
            try {
                registry.register(asset.toXPCurve());
                loaded++;
                LOGGER.at(Level.FINE).log("Loaded XP curve: %s", id);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to load XP curve: %s", id);
                skipped++;
            }
        }

        // Log summary
        LOGGER.atInfo().log("XP curve loading complete: %d loaded, %d skipped, %d conflicts",
                loaded, skipped, conflicts.size());

        // Log conflicts at debug level
        if (!conflicts.isEmpty()) {
            LOGGER.at(Level.FINE).log("XP curve conflicts: %s", conflicts);
        }
    }
}
