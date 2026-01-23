package reign.software.hyforged.affix.resource;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Asset class for loading affix tier color configuration from JSON.
 * <p>
 * Loaded from: Server/Hyforged/GameplayConfigs/AffixTierColors.json
 */
public class AffixTierColorConfigAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, AffixTierColorConfigAsset>> {

    private static final MapCodec<String, Map<String, String>> STRING_MAP_CODEC =
            new MapCodec<>(Codec.STRING, HashMap::new);

    public static final AssetBuilderCodec<String, AffixTierColorConfigAsset> CODEC = AssetBuilderCodec
            .builder(
                    AffixTierColorConfigAsset.class,
                    AffixTierColorConfigAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .appendInherited(
                    new KeyedCodec<>("DefaultColor", Codec.STRING),
                    (asset, value) -> asset.defaultColor = value != null ? value : "",
                    asset -> asset.defaultColor,
                    (asset, parent) -> asset.defaultColor = parent.defaultColor
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("TierColors", STRING_MAP_CODEC),
                    (asset, value) -> asset.tierColors = value != null ? new HashMap<>(value) : new HashMap<>(),
                    asset -> asset.tierColors,
                    (asset, parent) -> asset.tierColors = parent.tierColors
            )
            .add()
            .build();

    private String id;
    private AssetExtraInfo.Data data;
    private String defaultColor = "";
    private Map<String, String> tierColors = new HashMap<>();

    public AffixTierColorConfigAsset() {
        // Required for codec
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getDefaultColor() {
        return defaultColor != null ? defaultColor : "";
    }

    @Nonnull
    public Map<String, String> getTierColors() {
        return tierColors != null ? Map.copyOf(tierColors) : Map.of();
    }
}
