package reign.software.hyforged.hub.resource;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import reign.software.hyforged.hub.system.WelcomeMessageSystem;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Handles loading welcome message assets from JSON files.
 * <p>
 * Loads from: Server/Hyforged/WelcomeMessages/*.json
 * <p>
 * Each file defines a single message with segments. Messages are sorted by Order field.
 */
public final class WelcomeMessagesConfigAssetLoader {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ASSET_PATH = "Hyforged/WelcomeMessages";

    private static boolean initialized = false;

    private WelcomeMessagesConfigAssetLoader() {
    }

    public static void initialize(@Nonnull JavaPlugin plugin) {
        if (initialized) {
            LOGGER.atWarning().log("WelcomeMessagesConfigAssetLoader already initialized");
            return;
        }

        registerAssetStore();

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                WelcomeMessagesConfigAsset.class,
                WelcomeMessagesConfigAssetLoader::onAssetsLoaded
        );

        initialized = true;
        LOGGER.atInfo().log("Welcome messages asset loading initialized");
    }

    private static void registerAssetStore() {
        AssetStore<String, WelcomeMessagesConfigAsset, IndexedLookupTableAssetMap<String, WelcomeMessagesConfigAsset>> store =
                ((HytaleAssetStore.Builder<String, WelcomeMessagesConfigAsset, IndexedLookupTableAssetMap<String, WelcomeMessagesConfigAsset>>)
                        ((HytaleAssetStore.Builder<String, WelcomeMessagesConfigAsset, IndexedLookupTableAssetMap<String, WelcomeMessagesConfigAsset>>)
                                ((HytaleAssetStore.Builder<String, WelcomeMessagesConfigAsset, IndexedLookupTableAssetMap<String, WelcomeMessagesConfigAsset>>)
                                        ((HytaleAssetStore.Builder<String, WelcomeMessagesConfigAsset, IndexedLookupTableAssetMap<String, WelcomeMessagesConfigAsset>>)
                                                HytaleAssetStore.builder(
                                                        WelcomeMessagesConfigAsset.class,
                                                        new IndexedLookupTableAssetMap<>(WelcomeMessagesConfigAsset[]::new)
                                                )
                                                        .setPath(ASSET_PATH))
                                                .setReplaceOnRemove(key -> new WelcomeMessagesConfigAsset()))
                                        .setCodec(WelcomeMessagesConfigAsset.CODEC))
                                .setKeyFunction(WelcomeMessagesConfigAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered WelcomeMessagesConfigAsset store at path: %s", ASSET_PATH);
    }

    private static void onAssetsLoaded(
            LoadedAssetsEvent<String, WelcomeMessagesConfigAsset, IndexedLookupTableAssetMap<String, WelcomeMessagesConfigAsset>> event
    ) {
        LOGGER.atInfo().log("Loading welcome messages from assets...");

        List<WelcomeMessagesConfigAsset> messages = new ArrayList<>();
        for (WelcomeMessagesConfigAsset asset : event.getLoadedAssets().values()) {
            if (asset.isEnabled()) {
                messages.add(asset);
            }
        }

        // Sort by order
        messages.sort(Comparator.comparingInt(WelcomeMessagesConfigAsset::getOrder));

        WelcomeMessageSystem.applyConfig(messages);
        LOGGER.atInfo().log("Welcome messages loaded: %s messages", messages.size());
    }
}
