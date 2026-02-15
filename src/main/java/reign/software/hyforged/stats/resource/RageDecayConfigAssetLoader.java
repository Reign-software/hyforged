package reign.software.hyforged.stats.resource;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Handles loading the rage decay configuration from JSON assets.
 * <p>
 * Loads: Server/Hyforged/GameplayConfigs/RageDecay/RageDecay.json
 */
public final class RageDecayConfigAssetLoader {

    private static final Logger LOGGER = Logger.getLogger(RageDecayConfigAssetLoader.class.getName());

    public static final String RAGE_DECAY_ASSET_PATH = "Hyforged/GameplayConfigs/RageDecay";
    private static final String CONFIG_ID = "RageDecay";

    private static boolean initialized = false;

    private RageDecayConfigAssetLoader() {
        // Utility class
    }

    public static void initialize(@Nonnull JavaPlugin plugin) {
        if (initialized) {
            LOGGER.warning("RageDecayConfigAssetLoader already initialized");
            return;
        }

        registerRageDecayConfigAssetStore();

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                RageDecayConfigAsset.class,
                RageDecayConfigAssetLoader::onRageDecayConfigAssetsLoaded
        );

        initialized = true;
        LOGGER.info("Rage decay config asset loading initialized");
    }

    private static void registerRageDecayConfigAssetStore() {
        AssetStore<String, RageDecayConfigAsset, IndexedLookupTableAssetMap<String, RageDecayConfigAsset>> store =
                ((HytaleAssetStore.Builder<String, RageDecayConfigAsset, IndexedLookupTableAssetMap<String, RageDecayConfigAsset>>)
                        ((HytaleAssetStore.Builder<String, RageDecayConfigAsset, IndexedLookupTableAssetMap<String, RageDecayConfigAsset>>)
                                ((HytaleAssetStore.Builder<String, RageDecayConfigAsset, IndexedLookupTableAssetMap<String, RageDecayConfigAsset>>)
                                        ((HytaleAssetStore.Builder<String, RageDecayConfigAsset, IndexedLookupTableAssetMap<String, RageDecayConfigAsset>>)
                                                HytaleAssetStore.builder(
                                                        RageDecayConfigAsset.class,
                                                        new IndexedLookupTableAssetMap<>(RageDecayConfigAsset[]::new)
                                                )
                                                        .setPath(RAGE_DECAY_ASSET_PATH))
                                                .setReplaceOnRemove(key -> new RageDecayConfigAsset()))
                                .setCodec(RageDecayConfigAsset.CODEC))
                        .setKeyFunction(a -> CONFIG_ID))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered RageDecayConfigAsset store at path: " + RAGE_DECAY_ASSET_PATH);
    }

    private static void onRageDecayConfigAssetsLoaded(
            LoadedAssetsEvent<String, RageDecayConfigAsset, IndexedLookupTableAssetMap<String, RageDecayConfigAsset>> event
    ) {
        LOGGER.info("Loading rage decay configuration from assets...");

        for (RageDecayConfigAsset configAsset : event.getLoadedAssets().values()) {
            RageDecayConfig.get().applyFromAsset(configAsset);
            LOGGER.info("Rage decay configuration loaded successfully");
            return;
        }

        LOGGER.warning("RageDecay.json not found, using default values");
    }
}
