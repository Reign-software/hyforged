package reign.software.hyforged.stats.damage;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles loading damage type extension assets from JSON.
 * <p>
 * This class registers an asset store for {@link DamageTypeExtensionAsset}
 * and loads them into the {@link DamageTypeExtensionRegistry} on asset load events.
 * <p>
 * Extensions are loaded from Server/Hyforged/Damage/ and provide Hyforged-specific
 * data for Hytale's DamageCause assets (resistance stats, penetration stats).
 */
public final class DamageTypeAssetLoader {

    private static final Logger LOGGER = Logger.getLogger(DamageTypeAssetLoader.class.getName());

    /** Path for damage type extension assets relative to asset root */
    public static final String DAMAGE_TYPE_ASSET_PATH = "Hyforged/Damage";

    private static boolean initialized = false;

    private DamageTypeAssetLoader() {
        // Utility class
    }

    /**
     * Initialize the asset store and register event handlers.
     * Should be called during plugin setup.
     */
    public static void initialize(@Nonnull com.hypixel.hytale.server.core.plugin.JavaPlugin plugin) {
        if (initialized) {
            LOGGER.warning("DamageTypeAssetLoader already initialized");
            return;
        }

        LOGGER.info("Initializing Hyforged damage type extension loading...");

        // Register damage type extension asset store
        registerDamageTypeAssetStore();

        // Register event handler for when assets are loaded
        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                DamageTypeExtensionAsset.class,
                DamageTypeAssetLoader::onDamageTypeAssetsLoaded
        );

        initialized = true;
        LOGGER.info("Hyforged damage type extension loading initialized");
    }

    /**
     * Register the asset store for damage type extensions.
     */
    private static void registerDamageTypeAssetStore() {
        AssetStore<String, DamageTypeExtensionAsset, IndexedLookupTableAssetMap<String, DamageTypeExtensionAsset>> store =
                ((HytaleAssetStore.Builder<String, DamageTypeExtensionAsset, IndexedLookupTableAssetMap<String, DamageTypeExtensionAsset>>)
                        ((HytaleAssetStore.Builder<String, DamageTypeExtensionAsset, IndexedLookupTableAssetMap<String, DamageTypeExtensionAsset>>)
                                ((HytaleAssetStore.Builder<String, DamageTypeExtensionAsset, IndexedLookupTableAssetMap<String, DamageTypeExtensionAsset>>)
                                        ((HytaleAssetStore.Builder<String, DamageTypeExtensionAsset, IndexedLookupTableAssetMap<String, DamageTypeExtensionAsset>>)
                                                HytaleAssetStore.builder(
                                                        DamageTypeExtensionAsset.class,
                                                        new IndexedLookupTableAssetMap<>(DamageTypeExtensionAsset[]::new)
                                                )
                                                        .setPath(DAMAGE_TYPE_ASSET_PATH))
                                                .setReplaceOnRemove(key -> new DamageTypeExtensionAsset()))
                                        .setCodec(DamageTypeExtensionAsset.CODEC))
                                .setKeyFunction(DamageTypeExtensionAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered DamageTypeExtensionAsset store at path: " + DAMAGE_TYPE_ASSET_PATH);
    }

    /**
     * Handle damage type extension assets loaded event.
     */
    private static void onDamageTypeAssetsLoaded(
            LoadedAssetsEvent<String, DamageTypeExtensionAsset, IndexedLookupTableAssetMap<String, DamageTypeExtensionAsset>> event
    ) {
        LOGGER.info("Loading damage type extensions from assets...");

        DamageTypeExtensionRegistry registry = DamageTypeExtensionRegistry.get();
        
        // Clear existing extensions on reload
        registry.clear();
        
        int loaded = 0;
        int skipped = 0;

        for (DamageTypeExtensionAsset asset : event.getLoadedAssets().values()) {
            String id = asset.getId();

            try {
                DamageTypeExtension extension = DamageTypeExtension.fromAsset(asset);
                registry.register(id, extension);
                loaded++;
                LOGGER.fine("Loaded damage type extension: " + id + 
                           " -> resistance: " + asset.getResistanceStat() +
                           ", penetration: " + asset.getPenetrationStat());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load damage type extension: " + id, e);
                skipped++;
            }
        }

        LOGGER.info("Loaded " + loaded + " damage type extensions from assets (" + skipped + " skipped)");
    }

    /**
     * Check if the loader has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
