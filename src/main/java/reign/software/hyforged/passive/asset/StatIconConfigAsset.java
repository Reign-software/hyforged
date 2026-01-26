package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Asset definition for stat-to-icon mappings.
 * Loaded from JSON at Server/Hyforged/Config/stat-icons.json.
 * <p>
 * Allows data-driven configuration of which icons display for which stats.
 */
public class StatIconConfigAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, StatIconConfigAsset>> {
    
    private static final MapCodec<String, Map<String, String>> STRING_MAP_CODEC =
            new MapCodec<>(Codec.STRING, HashMap::new);
    
    public static final AssetBuilderCodec<String, StatIconConfigAsset> CODEC = AssetBuilderCodec
        .builder(
            StatIconConfigAsset.class,
            StatIconConfigAsset::new,
            Codec.STRING,
            (asset, id) -> asset.id = id,
            asset -> asset.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data
        )
        .append(
            new KeyedCodec<>("StatIconMappings", StatIconMappingAsset.ARRAY_CODEC),
            (asset, value) -> asset.mappings = value != null ? Arrays.asList(value) : new ArrayList<>(),
            asset -> asset.mappings != null ? asset.mappings.toArray(new StatIconMappingAsset[0]) : new StatIconMappingAsset[0]
        )
        .add()
        .append(
            new KeyedCodec<>("NodeTypeIcons", STRING_MAP_CODEC),
            (asset, value) -> asset.nodeTypeIcons = value != null ? new HashMap<>(value) : new HashMap<>(),
            asset -> asset.nodeTypeIcons
        )
        .add()
        .append(
            new KeyedCodec<>("DefaultIcon", Codec.STRING),
            (asset, value) -> asset.defaultIcon = value,
            asset -> asset.defaultIcon
        )
        .add()
        .build();
    
    private static AssetStore<String, StatIconConfigAsset, IndexedLookupTableAssetMap<String, StatIconConfigAsset>> ASSET_STORE;
    
    private String id;
    private AssetExtraInfo.Data data;
    private List<StatIconMappingAsset> mappings = new ArrayList<>();
    private Map<String, String> nodeTypeIcons = new HashMap<>();
    private String defaultIcon = "Hyforged/Textures/Passive.png";
    
    public StatIconConfigAsset() {
        // Required for codec
    }
    
    /**
     * Get the asset store for stat icon config.
     */
    @Nonnull
    public static AssetStore<String, StatIconConfigAsset, IndexedLookupTableAssetMap<String, StatIconConfigAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(StatIconConfigAsset.class);
        }
        return ASSET_STORE;
    }
    
    @Nonnull
    @Override
    public String getId() {
        return id != null ? id : "stat-icons";
    }
    
    @Nonnull
    public List<StatIconMappingAsset> getMappings() {
        return mappings != null ? mappings : List.of();
    }
    
    @Nonnull
    public Map<String, String> getNodeTypeIcons() {
        return nodeTypeIcons != null ? nodeTypeIcons : Map.of();
    }
    
    @Nullable
    public String getNodeTypeIcon(@Nonnull String nodeType) {
        return nodeTypeIcons != null ? nodeTypeIcons.get(nodeType.toLowerCase()) : null;
    }
    
    @Nonnull
    public String getDefaultIcon() {
        return defaultIcon != null ? defaultIcon : "Hyforged/Textures/Passive.png";
    }
    
    /**
     * Individual stat-to-icon mapping entry.
     */
    public static class StatIconMappingAsset {
        
        public static final BuilderCodec<StatIconMappingAsset> CODEC = BuilderCodec.builder(
                StatIconMappingAsset.class,
                StatIconMappingAsset::new
            )
            .append(
                new KeyedCodec<>("Pattern", Codec.STRING),
                (asset, value) -> asset.pattern = value,
                asset -> asset.pattern
            )
            .add()
            .append(
                new KeyedCodec<>("Icon", Codec.STRING),
                (asset, value) -> asset.icon = value,
                asset -> asset.icon
            )
            .add()
            .append(
                new KeyedCodec<>("Priority", Codec.INTEGER),
                (asset, value) -> asset.priority = value != null ? value : 50,
                asset -> asset.priority
            )
            .add()
            .build();
        
        public static final ArrayCodec<StatIconMappingAsset> ARRAY_CODEC = 
            new ArrayCodec<>(CODEC, StatIconMappingAsset[]::new);
        
        private String pattern;
        private String icon;
        private int priority = 50;
        
        public StatIconMappingAsset() {
            // Required for codec
        }
        
        @Nullable
        public String getPattern() {
            return pattern;
        }
        
        @Nullable
        public String getIcon() {
            return icon;
        }
        
        public int getPriority() {
            return priority;
        }
    }
}
