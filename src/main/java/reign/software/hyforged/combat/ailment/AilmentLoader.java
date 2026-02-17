package reign.software.hyforged.combat.ailment;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;

import javax.annotation.Nonnull;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Handles loading ailment assets from JSON via Hytale's asset system.
 * <p>
 * This class registers an asset store for {@link AilmentAsset} and loads them
 * into the {@link AilmentRegistry} on asset load events.
 * <p>
 * Ailments are loaded from Server/Hyforged/Combat/Ailments/ and define
 * threshold-based status effects that trigger from elemental damage.
 */
public final class AilmentLoader {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    /** Path for ailment assets relative to asset root */
    public static final String AILMENT_ASSET_PATH = "Hyforged/Combat/Ailments";
    
    private static boolean initialized = false;
    
    private AilmentLoader() {
        // Utility class
    }
    
    /**
     * Initialize the asset store and register event handlers.
     * Should be called during plugin setup.
     */
    public static void initialize(@Nonnull com.hypixel.hytale.server.core.plugin.JavaPlugin plugin) {
        if (initialized) {
            LOGGER.atWarning().log("AilmentLoader already initialized");
            return;
        }
        
        LOGGER.atInfo().log("Initializing Hyforged ailment loading...");
        
        // Register ailment asset store
        registerAilmentAssetStore();
        
        // Register event handler for when assets are loaded
        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                AilmentAsset.class,
                AilmentLoader::onAilmentAssetsLoaded
        );
        
        initialized = true;
        LOGGER.atInfo().log("Hyforged ailment loading initialized");
    }
    
    /**
     * Register the asset store for ailments.
     */
    private static void registerAilmentAssetStore() {
        AssetStore<String, AilmentAsset, IndexedLookupTableAssetMap<String, AilmentAsset>> store =
                ((HytaleAssetStore.Builder<String, AilmentAsset, IndexedLookupTableAssetMap<String, AilmentAsset>>)
                        ((HytaleAssetStore.Builder<String, AilmentAsset, IndexedLookupTableAssetMap<String, AilmentAsset>>)
                                ((HytaleAssetStore.Builder<String, AilmentAsset, IndexedLookupTableAssetMap<String, AilmentAsset>>)
                                        ((HytaleAssetStore.Builder<String, AilmentAsset, IndexedLookupTableAssetMap<String, AilmentAsset>>)
                                                HytaleAssetStore.builder(
                                                        AilmentAsset.class,
                                                        new IndexedLookupTableAssetMap<>(AilmentAsset[]::new)
                                                )
                                                        .setPath(AILMENT_ASSET_PATH))
                                                .setReplaceOnRemove(key -> new AilmentAsset()))
                                        .setCodec(AilmentAsset.CODEC))
                                .setKeyFunction(AilmentAsset::getId))
                        .build();
        
        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered AilmentAsset store at path: %s", AILMENT_ASSET_PATH);
    }
    
    /**
     * Handle ailment assets loaded event.
     */
    private static void onAilmentAssetsLoaded(
            LoadedAssetsEvent<String, AilmentAsset, IndexedLookupTableAssetMap<String, AilmentAsset>> event
    ) {
        LOGGER.atInfo().log("Loading ailment definitions from assets...");
        
        AilmentRegistry registry = AilmentRegistry.get();
        
        // Clear existing ailments on reload
        registry.clear();
        
        int loaded = 0;
        int errors = 0;
        
        for (AilmentAsset asset : event.getLoadedAssets().values()) {
            try {
                AilmentDefinition definition = asset.toDefinition();
                registry.register(definition);
                loaded++;
                LOGGER.at(Level.FINE).log("Loaded ailment: %s (element: %s, effect: %s)",
                        definition.id(), definition.elementTag(), definition.entityEffectId());
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to register ailment: %s", asset.getId());
                errors++;
            }
        }
        
        LOGGER.atInfo().log("Loaded %s ailment definitions%s", loaded, (errors > 0 ? " (" + errors + " errors)" : ""));
    }
    
    /**
     * Check if the loader has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
