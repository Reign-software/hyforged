package reign.software.hyforged.stats.resource;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;

import javax.annotation.Nonnull;

/**
 * Asset class for loading rage decay configuration from JSON.
 * <p>
 * Loaded from: Server/Hyforged/GameplayConfigs/RageDecay.json
 */
public class RageDecayConfigAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, RageDecayConfigAsset>> {

    public static final AssetBuilderCodec<String, RageDecayConfigAsset> CODEC = AssetBuilderCodec
            .builder(
                    RageDecayConfigAsset.class,
                    RageDecayConfigAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .appendInherited(
                    new KeyedCodec<>("OutOfCombatDelaySeconds", Codec.FLOAT),
                    (asset, value) -> asset.outOfCombatDelaySeconds = value != null ? value : 4.0f,
                    asset -> asset.outOfCombatDelaySeconds,
                    (asset, parent) -> asset.outOfCombatDelaySeconds = parent.outOfCombatDelaySeconds
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("DecayPerSecond", Codec.FLOAT),
                    (asset, value) -> asset.decayPerSecond = value != null ? value : 7.0f,
                    asset -> asset.decayPerSecond,
                    (asset, parent) -> asset.decayPerSecond = parent.decayPerSecond
            )
            .add()
            .build();

    private String id;
    private AssetExtraInfo.Data data;
    private float outOfCombatDelaySeconds = 4.0f;
    private float decayPerSecond = 7.0f;

    public RageDecayConfigAsset() {
        // Required for codec
    }

    @Nonnull
    public String getId() {
        return id;
    }

    public float getOutOfCombatDelaySeconds() {
        return outOfCombatDelaySeconds;
    }

    public float getDecayPerSecond() {
        return decayPerSecond;
    }
}
