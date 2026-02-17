package reign.software.hyforged.currency.config;

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
 * Handles loading currency configuration from JSON assets.
 * <p>
 * Loads:
 * - Server/Hyforged/Config/SellValue/SellValueConfig.json
 * - Server/Hyforged/Config/VaultUpgrades/VaultUpgrades.json
 */
public final class CurrencyConfigAssetLoader {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Path for sell value config assets relative to asset root */
    public static final String SELL_VALUE_ASSET_PATH = "Hyforged/Config/SellValue";

    /** Path for vault upgrades config assets relative to asset root */
    public static final String VAULT_UPGRADES_ASSET_PATH = "Hyforged/Config/VaultUpgrades";

    private static boolean initialized = false;

    private CurrencyConfigAssetLoader() {
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
            LOGGER.atWarning().log("CurrencyConfigAssetLoader already initialized");
            return;
        }

        LOGGER.atInfo().log("Initializing currency config asset loading...");

        // Register asset stores
        registerSellValueConfigAssetStore();
        registerVaultUpgradesAssetStore();

        // Register event handlers
        plugin.getEventRegistry().register(
            LoadedAssetsEvent.class,
            SellValueConfigAsset.class,
            CurrencyConfigAssetLoader::onSellValueConfigLoaded
        );

        plugin.getEventRegistry().register(
            LoadedAssetsEvent.class,
            VaultUpgradesConfigAsset.class,
            CurrencyConfigAssetLoader::onVaultUpgradesLoaded
        );

        initialized = true;
        LOGGER.atInfo().log("Currency config asset loading initialized");
    }

    private static void registerSellValueConfigAssetStore() {
        AssetStore<String, SellValueConfigAsset, IndexedLookupTableAssetMap<String, SellValueConfigAsset>> store =
            ((HytaleAssetStore.Builder<String, SellValueConfigAsset, IndexedLookupTableAssetMap<String, SellValueConfigAsset>>)
                ((HytaleAssetStore.Builder<String, SellValueConfigAsset, IndexedLookupTableAssetMap<String, SellValueConfigAsset>>)
                    ((HytaleAssetStore.Builder<String, SellValueConfigAsset, IndexedLookupTableAssetMap<String, SellValueConfigAsset>>)
                        ((HytaleAssetStore.Builder<String, SellValueConfigAsset, IndexedLookupTableAssetMap<String, SellValueConfigAsset>>)
                            HytaleAssetStore.builder(
                                SellValueConfigAsset.class,
                                new IndexedLookupTableAssetMap<>(SellValueConfigAsset[]::new)
                            )
                            .setPath(SELL_VALUE_ASSET_PATH))
                        .setReplaceOnRemove(key -> new SellValueConfigAsset()))
                    .setCodec(SellValueConfigAsset.CODEC))
                .setKeyFunction(SellValueConfigAsset::getId))
            .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered SellValueConfigAsset store at path: %s", SELL_VALUE_ASSET_PATH);
    }

    private static void registerVaultUpgradesAssetStore() {
        AssetStore<String, VaultUpgradesConfigAsset, IndexedLookupTableAssetMap<String, VaultUpgradesConfigAsset>> store =
            ((HytaleAssetStore.Builder<String, VaultUpgradesConfigAsset, IndexedLookupTableAssetMap<String, VaultUpgradesConfigAsset>>)
                ((HytaleAssetStore.Builder<String, VaultUpgradesConfigAsset, IndexedLookupTableAssetMap<String, VaultUpgradesConfigAsset>>)
                    ((HytaleAssetStore.Builder<String, VaultUpgradesConfigAsset, IndexedLookupTableAssetMap<String, VaultUpgradesConfigAsset>>)
                        ((HytaleAssetStore.Builder<String, VaultUpgradesConfigAsset, IndexedLookupTableAssetMap<String, VaultUpgradesConfigAsset>>)
                            HytaleAssetStore.builder(
                                VaultUpgradesConfigAsset.class,
                                new IndexedLookupTableAssetMap<>(VaultUpgradesConfigAsset[]::new)
                            )
                            .setPath(VAULT_UPGRADES_ASSET_PATH))
                        .setReplaceOnRemove(key -> new VaultUpgradesConfigAsset()))
                    .setCodec(VaultUpgradesConfigAsset.CODEC))
                .setKeyFunction(VaultUpgradesConfigAsset::getId))
            .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered VaultUpgradesConfigAsset store at path: %s", VAULT_UPGRADES_ASSET_PATH);
    }

    private static void onSellValueConfigLoaded(
        LoadedAssetsEvent<String, SellValueConfigAsset, IndexedLookupTableAssetMap<String, SellValueConfigAsset>> event
    ) {
        LOGGER.atInfo().log("Loading sell value configuration from assets...");

        for (SellValueConfigAsset asset : event.getLoadedAssets().values()) {
            LOGGER.atInfo().log("Found SellValueConfig asset: %s", asset.getId());
            SellValueConfig.apply(asset);
        }
    }

    private static void onVaultUpgradesLoaded(
        LoadedAssetsEvent<String, VaultUpgradesConfigAsset, IndexedLookupTableAssetMap<String, VaultUpgradesConfigAsset>> event
    ) {
        LOGGER.atInfo().log("Loading vault upgrades configuration from assets...");

        for (VaultUpgradesConfigAsset asset : event.getLoadedAssets().values()) {
            LOGGER.atInfo().log("Found VaultUpgradesConfig asset: %s", asset.getId());
            VaultUpgradesConfig.apply(asset);
        }
    }
}
