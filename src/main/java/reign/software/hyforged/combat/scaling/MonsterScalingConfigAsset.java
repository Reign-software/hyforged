package reign.software.hyforged.combat.scaling;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JSON asset definition for per-NPC/monster scaling configuration.
 * <p>
 * Each monster type can define which stats scale with level and how.
 * Loaded from {@code Server/Hyforged/Combat/MonsterScaling/*.json}.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Id": "hyforged:shadow-knight",
 *   "AppliesTo": ["Shadow_Knight", "Shadow_Knight_Elite"],
 *   "ScaledStats": [
 *     { "StatId": "hyforged:max-health", "ModifierType": "INCREASED", "ScalePerLevel": 15 },
 *     { "StatId": "hyforged:physical-damage-bps", "ModifierType": "INCREASED", "ScalePerLevel": 8 },
 *     { "StatId": "hyforged:armor-bps", "ModifierType": "FLAT", "ScalePerLevel": 100 }
 *   ]
 * }
 * </pre>
 * <p>
 * The "AppliesTo" field contains NPC role names that this scaling applies to.
 * Multiple monsters can share the same scaling configuration.
 */
public class MonsterScalingConfigAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, MonsterScalingConfigAsset>> {

    /**
     * Codec for loading MonsterScalingConfigAsset from JSON.
     */
    public static final AssetBuilderCodec<String, MonsterScalingConfigAsset> CODEC = AssetBuilderCodec
            .builder(
                    MonsterScalingConfigAsset.class,
                    MonsterScalingConfigAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .append(
                    new KeyedCodec<>("AppliesTo", Codec.STRING_ARRAY),
                    (asset, value) -> asset.appliesTo = value != null ? new ArrayList<>(Arrays.asList(value)) : new ArrayList<>(),
                    asset -> asset.appliesTo.toArray(new String[0])
            )
            .add()
            .append(
                    new KeyedCodec<>("ScaledStats", ScaledStatEntry.ARRAY_CODEC),
                    (asset, value) -> asset.scaledStats = value != null ? new ArrayList<>(Arrays.asList(value)) : new ArrayList<>(),
                    asset -> asset.scaledStats.toArray(new ScaledStatEntry[0])
            )
            .add()
            .build();

    private static AssetStore<String, MonsterScalingConfigAsset, IndexedLookupTableAssetMap<String, MonsterScalingConfigAsset>> ASSET_STORE;

    // Asset data
    private String id;
    private AssetExtraInfo.Data data;

    // Configuration fields
    private List<String> appliesTo = new ArrayList<>();
    private List<ScaledStatEntry> scaledStats = new ArrayList<>();

    public MonsterScalingConfigAsset() {
    }

    /**
     * Get the asset store for monster scaling configurations.
     */
    @Nonnull
    public static AssetStore<String, MonsterScalingConfigAsset, IndexedLookupTableAssetMap<String, MonsterScalingConfigAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(MonsterScalingConfigAsset.class);
        }
        return ASSET_STORE;
    }

    // ========== JsonAssetWithMap Interface ==========

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    // ========== Accessors ==========

    /**
     * Get the list of NPC role names this scaling applies to.
     */
    @Nonnull
    public List<String> getAppliesTo() {
        return Collections.unmodifiableList(appliesTo);
    }

    /**
     * Get the list of stats that scale with level.
     */
    @Nonnull
    public List<ScaledStatEntry> getScaledStats() {
        return Collections.unmodifiableList(scaledStats);
    }
}
