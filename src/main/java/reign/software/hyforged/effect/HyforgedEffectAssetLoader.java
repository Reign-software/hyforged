package reign.software.hyforged.effect;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles loading Hyforged effect assets from JSON.
 */
public final class HyforgedEffectAssetLoader {

    private static final Logger LOGGER = Logger.getLogger(HyforgedEffectAssetLoader.class.getName());

    /** Path for Hyforged effect assets relative to asset root */
    public static final String HYFORGED_EFFECT_PATH = "Hyforged/Effects";

    private static boolean initialized = false;

    private HyforgedEffectAssetLoader() {
        // Utility class
    }

    /**
     * Initialize the asset store and register event handlers.
     * Should be called during plugin setup.
     */
    public static void initialize(@Nonnull com.hypixel.hytale.server.core.plugin.JavaPlugin plugin) {
        if (initialized) {
            LOGGER.warning("HyforgedEffectAssetLoader already initialized");
            return;
        }

        LOGGER.info("Initializing Hyforged effect loading...");

        registerHyforgedEffectAssetStore();

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                HyforgedEffectAsset.class,
                HyforgedEffectAssetLoader::onHyforgedEffectAssetsLoaded
        );

        initialized = true;
        LOGGER.info("Hyforged effect loading initialized");
    }

    private static void registerHyforgedEffectAssetStore() {
        AssetStore<String, HyforgedEffectAsset, IndexedLookupTableAssetMap<String, HyforgedEffectAsset>> store =
                ((HytaleAssetStore.Builder<String, HyforgedEffectAsset, IndexedLookupTableAssetMap<String, HyforgedEffectAsset>>)
                        ((HytaleAssetStore.Builder<String, HyforgedEffectAsset, IndexedLookupTableAssetMap<String, HyforgedEffectAsset>>)
                                ((HytaleAssetStore.Builder<String, HyforgedEffectAsset, IndexedLookupTableAssetMap<String, HyforgedEffectAsset>>)
                                        HytaleAssetStore.builder(
                                                HyforgedEffectAsset.class,
                                                new IndexedLookupTableAssetMap<>(HyforgedEffectAsset[]::new)
                                        )
                                                .setPath(HYFORGED_EFFECT_PATH)
                                                .loadsBefore(EntityEffect.class))
                                        .setCodec(HyforgedEffectAsset.CODEC))
                                .setKeyFunction(HyforgedEffectAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered HyforgedEffectAsset store at path: " + HYFORGED_EFFECT_PATH);
    }

    private static void onHyforgedEffectAssetsLoaded(
            LoadedAssetsEvent<String, HyforgedEffectAsset, IndexedLookupTableAssetMap<String, HyforgedEffectAsset>> event
    ) {
        LOGGER.info("Loading Hyforged effects from assets...");

        HyforgedEffectRegistry registry = HyforgedEffectRegistry.get();
        registry.clear();

        int loaded = 0;
        int errors = 0;

        for (HyforgedEffectAsset asset : event.getLoadedAssets().values()) {
            try {
                String effectId = asset.getEntityEffectId();
                if (effectId == null || effectId.isEmpty()) {
                    throw new IllegalStateException("EntityEffect is required for Hyforged effect: " + asset.getId());
                }

                List<HyforgedEffectModifierSpec> modifiers = asset.getHyforgedModifiers();
                registry.register(
                    effectId,
                    modifiers,
                    asset.getConcentrationCost(),
                    asset.getConcentrationAbilityId(),
                    asset.getConcentrationPriority()
                );
                loaded++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to register Hyforged effect: " + asset.getId(), e);
                errors++;
            }
        }

        LOGGER.info("Loaded " + loaded + " Hyforged effects" + (errors > 0 ? " (" + errors + " errors)" : ""));
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
