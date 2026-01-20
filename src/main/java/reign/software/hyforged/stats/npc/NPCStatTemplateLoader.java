package reign.software.hyforged.stats.npc;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles loading NPC stat templates from JSON assets.
 * <p>
 * This class registers asset stores for NPCStatTemplateAsset,
 * then loads and registers them with the NPCStatTemplateRegistry
 * on asset load events.
 * <p>
 * After all templates are loaded, inheritance is resolved automatically.
 */
public final class NPCStatTemplateLoader {

    private static final Logger LOGGER = Logger.getLogger(NPCStatTemplateLoader.class.getName());

    /** Path for NPC stat template assets relative to asset root */
    public static final String NPC_STATS_ASSET_PATH = "Hyforged/NPCStats";

    private static boolean initialized = false;

    private NPCStatTemplateLoader() {
        // Utility class
    }

    /**
     * Initialize the asset stores and register event handlers.
     * Should be called during plugin setup.
     */
    public static void initialize(@Nonnull JavaPlugin plugin) {
        if (initialized) {
            LOGGER.warning("NPCStatTemplateLoader already initialized");
            return;
        }

        LOGGER.info("Initializing Hyforged NPC stat template loading...");

        // Register NPC stat template asset store
        registerNPCStatTemplateAssetStore();

        // Register event handlers for when assets are loaded
        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                NPCStatTemplateAsset.class,
                NPCStatTemplateLoader::onNPCTemplateAssetsLoaded
        );

        initialized = true;
        LOGGER.info("Hyforged NPC stat template loading initialized");
    }

    /**
     * Register the asset store for NPC stat templates.
     */
    private static void registerNPCStatTemplateAssetStore() {
        AssetStore<String, NPCStatTemplateAsset, IndexedLookupTableAssetMap<String, NPCStatTemplateAsset>> store =
                ((HytaleAssetStore.Builder<String, NPCStatTemplateAsset, IndexedLookupTableAssetMap<String, NPCStatTemplateAsset>>)
                        ((HytaleAssetStore.Builder<String, NPCStatTemplateAsset, IndexedLookupTableAssetMap<String, NPCStatTemplateAsset>>)
                                ((HytaleAssetStore.Builder<String, NPCStatTemplateAsset, IndexedLookupTableAssetMap<String, NPCStatTemplateAsset>>)
                                        HytaleAssetStore.builder(
                                                NPCStatTemplateAsset.class,
                                                new IndexedLookupTableAssetMap<>(NPCStatTemplateAsset[]::new)
                                        )
                                                .setPath(NPC_STATS_ASSET_PATH))
                                        .setCodec(NPCStatTemplateAsset.CODEC))
                                .setKeyFunction(NPCStatTemplateAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered NPCStatTemplateAsset store at path: " + NPC_STATS_ASSET_PATH);
    }

    /**
     * Handle NPC template assets loaded event.
     */
    private static void onNPCTemplateAssetsLoaded(
            LoadedAssetsEvent<String, NPCStatTemplateAsset, IndexedLookupTableAssetMap<String, NPCStatTemplateAsset>> event
    ) {
        LOGGER.info("Loading NPC stat templates from assets...");

        NPCStatTemplateRegistry registry = NPCStatTemplateRegistry.get();
        Map<String, String> conflicts = new HashMap<>();
        int loaded = 0;
        int skipped = 0;

        for (NPCStatTemplateAsset asset : event.getLoadedAssets().values()) {
            String id = asset.getId();

            // Check for conflicts (if already registered)
            if (registry.hasTemplate(id)) {
                conflicts.put(id, "Duplicate NPC template ID - skipping");
                skipped++;
                continue;
            }

            try {
                NPCStatTemplate template = asset.toTemplate();
                registry.registerUnresolved(template);
                loaded++;
                LOGGER.fine("Loaded NPC stat template: " + id);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load NPC stat template: " + id, e);
                skipped++;
            }
        }

        // Log conflicts as errors
        for (Map.Entry<String, String> entry : conflicts.entrySet()) {
            LOGGER.severe("NPC stat template conflict: " + entry.getKey() + " - " + entry.getValue());
        }

        // Resolve inheritance after all templates are loaded
        registry.resolveInheritance();

        LOGGER.info("Loaded " + loaded + " NPC stat templates from assets (" + skipped + " skipped)");
    }

    /**
     * Check if the loader has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
