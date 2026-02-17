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
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

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

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Path for NPC stat template assets relative to asset root */
    public static final String NPC_STATS_ASSET_PATH = "Hyforged/Stats/NPCTemplates";

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
            LOGGER.atWarning().log("NPCStatTemplateLoader already initialized");
            return;
        }

        LOGGER.atInfo().log("Initializing Hyforged NPC stat template loading...");

        // Register NPC stat template asset store
        registerNPCStatTemplateAssetStore();

        // Register event handlers for when assets are loaded
        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                NPCStatTemplateAsset.class,
                NPCStatTemplateLoader::onNPCTemplateAssetsLoaded
        );

        initialized = true;
        LOGGER.atInfo().log("Hyforged NPC stat template loading initialized");
    }

    /**
     * Register the asset store for NPC stat templates.
     */
    private static void registerNPCStatTemplateAssetStore() {
        AssetStore<String, NPCStatTemplateAsset, IndexedLookupTableAssetMap<String, NPCStatTemplateAsset>> store =
                ((HytaleAssetStore.Builder<String, NPCStatTemplateAsset, IndexedLookupTableAssetMap<String, NPCStatTemplateAsset>>)
                        ((HytaleAssetStore.Builder<String, NPCStatTemplateAsset, IndexedLookupTableAssetMap<String, NPCStatTemplateAsset>>)
                                ((HytaleAssetStore.Builder<String, NPCStatTemplateAsset, IndexedLookupTableAssetMap<String, NPCStatTemplateAsset>>)
                                        ((HytaleAssetStore.Builder<String, NPCStatTemplateAsset, IndexedLookupTableAssetMap<String, NPCStatTemplateAsset>>)
                                                HytaleAssetStore.builder(
                                                        NPCStatTemplateAsset.class,
                                                        new IndexedLookupTableAssetMap<>(NPCStatTemplateAsset[]::new)
                                                )
                                                        .setPath(NPC_STATS_ASSET_PATH))
                                                .setReplaceOnRemove(key -> new NPCStatTemplateAsset()))
                                        .setCodec(NPCStatTemplateAsset.CODEC))
                                .setKeyFunction(NPCStatTemplateAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered NPCStatTemplateAsset store at path: %s", NPC_STATS_ASSET_PATH);
    }

    /**
     * Handle NPC template assets loaded event.
     */
    private static void onNPCTemplateAssetsLoaded(
            LoadedAssetsEvent<String, NPCStatTemplateAsset, IndexedLookupTableAssetMap<String, NPCStatTemplateAsset>> event
    ) {
        LOGGER.atInfo().log("Loading NPC stat templates from assets...");

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
                LOGGER.at(Level.FINE).log("Loaded NPC stat template: %s", id);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to load NPC stat template: %s", id);
                skipped++;
            }
        }

        // Log conflicts as errors
        for (Map.Entry<String, String> entry : conflicts.entrySet()) {
            LOGGER.atSevere().log("NPC stat template conflict: %s - %s", entry.getKey(), entry.getValue());
        }

        // Resolve inheritance after all templates are loaded
        registry.resolveInheritance();

        LOGGER.atInfo().log("Loaded %s NPC stat templates from assets (%s skipped)", loaded, skipped);
    }

    /**
     * Check if the loader has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
