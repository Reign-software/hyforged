package reign.software.hyforged.affix.resource;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Handles loading affix tier color configuration from JSON assets.
 * <p>
 * Loads: Server/Hyforged/GameplayConfigs/AffixTierColors.json
 */
public final class AffixTierColorConfigAssetLoader {

    private static final Logger LOGGER = Logger.getLogger(AffixTierColorConfigAssetLoader.class.getName());

    public static final String CONFIG_ASSET_PATH = "Hyforged/GameplayConfigs";
    private static final String CONFIG_ID = "AffixTierColors";

    private static boolean initialized = false;

    private AffixTierColorConfigAssetLoader() {
    }

    public static void initialize(@Nonnull JavaPlugin plugin) {
        if (initialized) {
            LOGGER.warning("AffixTierColorConfigAssetLoader already initialized");
            return;
        }

        registerConfigAssetStore();

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                AffixTierColorConfigAsset.class,
                AffixTierColorConfigAssetLoader::onAssetsLoaded
        );

        initialized = true;
        LOGGER.info("Affix tier color config asset loading initialized");
    }

    private static void registerConfigAssetStore() {
        AssetStore<String, AffixTierColorConfigAsset, IndexedLookupTableAssetMap<String, AffixTierColorConfigAsset>> store =
                ((HytaleAssetStore.Builder<String, AffixTierColorConfigAsset, IndexedLookupTableAssetMap<String, AffixTierColorConfigAsset>>)
                        ((HytaleAssetStore.Builder<String, AffixTierColorConfigAsset, IndexedLookupTableAssetMap<String, AffixTierColorConfigAsset>>)
                                ((HytaleAssetStore.Builder<String, AffixTierColorConfigAsset, IndexedLookupTableAssetMap<String, AffixTierColorConfigAsset>>)
                                        ((HytaleAssetStore.Builder<String, AffixTierColorConfigAsset, IndexedLookupTableAssetMap<String, AffixTierColorConfigAsset>>)
                                                HytaleAssetStore.builder(
                                                        AffixTierColorConfigAsset.class,
                                                        new IndexedLookupTableAssetMap<>(AffixTierColorConfigAsset[]::new)
                                                )
                                                        .setPath(CONFIG_ASSET_PATH))
                                                .setReplaceOnRemove(key -> new AffixTierColorConfigAsset()))
                                        .setCodec(AffixTierColorConfigAsset.CODEC))
                                .setKeyFunction(asset -> CONFIG_ID))
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered AffixTierColorConfigAsset store at path: " + CONFIG_ASSET_PATH);
    }

    private static void onAssetsLoaded(
            LoadedAssetsEvent<String, AffixTierColorConfigAsset, IndexedLookupTableAssetMap<String, AffixTierColorConfigAsset>> event
    ) {
        LOGGER.info("Loading affix tier color configuration from assets...");

        for (AffixTierColorConfigAsset asset : event.getLoadedAssets().values()) {
            AffixTierColorConfig.get().applyFromAsset(asset);
            LOGGER.info("Affix tier color configuration loaded successfully");
            return;
        }

        LOGGER.warning("AffixTierColors.json not found, using default tooltip colors");
    }
}
