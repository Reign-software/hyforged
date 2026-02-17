package reign.software.hyforged.stats.resource;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nonnull;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Handles loading the rage decay configuration from JSON assets.
 * <p>
 * Loads: Server/Hyforged/Config/RageDecay/RageDecay.json
 */
public final class RageDecayConfigAssetLoader {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String RAGE_DECAY_ASSET_PATH = "Hyforged/Config/RageDecay";
    private static final String CONFIG_ID = "RageDecay";

    private static boolean initialized = false;

    private RageDecayConfigAssetLoader() {
        // Utility class
    }

    public static void initialize(@Nonnull JavaPlugin plugin) {
        if (initialized) {
            LOGGER.atWarning().log("RageDecayConfigAssetLoader already initialized");
            return;
        }

        registerRageDecayConfigAssetStore();

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                RageDecayConfigAsset.class,
                RageDecayConfigAssetLoader::onRageDecayConfigAssetsLoaded
        );

        initialized = true;
        LOGGER.atInfo().log("Rage decay config asset loading initialized");
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
        LOGGER.at(Level.FINE).log("Registered RageDecayConfigAsset store at path: %s", RAGE_DECAY_ASSET_PATH);
    }

    private static void onRageDecayConfigAssetsLoaded(
            LoadedAssetsEvent<String, RageDecayConfigAsset, IndexedLookupTableAssetMap<String, RageDecayConfigAsset>> event
    ) {
        LOGGER.atInfo().log("Loading rage decay configuration from assets...");

        for (RageDecayConfigAsset configAsset : event.getLoadedAssets().values()) {
            RageDecayConfig.get().applyFromAsset(configAsset);
            LOGGER.atInfo().log("Rage decay configuration loaded successfully");
            return;
        }

        LOGGER.atWarning().log("RageDecay.json not found, using default values");
    }
}
