package reign.software.hyforged.affix.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import reign.software.hyforged.affix.model.AffixPool;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON asset definition for affix pools.
 * <p>
 * Affix pools define which affixes can appear on which item types.
 * Loaded from {@code Server/Hyforged/AffixPools/*.json}.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Id": "weapon-melee",
 *   "Priority": 10,
 *   "AppliesTo": {
 *     "Categories": ["Items.Weapons"],
 *     "Tags": ["Type:Weapon"]
 *   },
 *   "Prefixes": ["sturdy", "sharp", "mighty"],
 *   "Suffixes": ["of-the-bear", "of-precision"],
 *   "Forged": ["legendary-might"]
 * }
 * </pre>
 */
public class AffixPoolAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, AffixPoolAsset>> {

    /**
     * Codec for the AppliesTo nested object.
     */
    public static final BuilderCodec<AppliesToAsset> APPLIES_TO_CODEC = BuilderCodec.builder(
                    AppliesToAsset.class,
                    AppliesToAsset::new
            )
            .append(
                    new KeyedCodec<>("Categories", Codec.STRING_ARRAY),
                    (asset, value) -> asset.categories = value,
                    asset -> asset.categories
            )
            .add()
            .append(
                    new KeyedCodec<>("Tags", Codec.STRING_ARRAY),
                    (asset, value) -> asset.tags = value,
                    asset -> asset.tags
            )
            .add()
            .build();

    private static final MapCodec<String[], Map<String, String[]>> AFFIXES_CODEC =
            new MapCodec<>(Codec.STRING_ARRAY, HashMap::new);

    /**
     * Codec for loading AffixPoolAsset from JSON.
     */
    public static final AssetBuilderCodec<String, AffixPoolAsset> CODEC = AssetBuilderCodec
            .builder(
                    AffixPoolAsset.class,
                    AffixPoolAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
                .append(
                    new KeyedCodec<>("Description", Codec.STRING),
                    (asset, value) -> asset.description = value != null ? value : "",
                    asset -> asset.description
                )
                .add()
            .append(
                    new KeyedCodec<>("Priority", Codec.INTEGER),
                    (asset, value) -> asset.priority = value != null ? value : AffixPool.DEFAULT_PRIORITY,
                    asset -> asset.priority
            )
            .add()
            .append(
                    new KeyedCodec<>("AppliesTo", APPLIES_TO_CODEC),
                    (asset, value) -> asset.appliesTo = value,
                    asset -> asset.appliesTo
            )
            .add()
            .append(
                    new KeyedCodec<>("Prefixes", Codec.STRING_ARRAY),
                    (asset, value) -> asset.prefixes = value,
                    asset -> asset.prefixes
            )
            .add()
            .append(
                    new KeyedCodec<>("Suffixes", Codec.STRING_ARRAY),
                    (asset, value) -> asset.suffixes = value,
                    asset -> asset.suffixes
            )
            .add()
            .append(
                    new KeyedCodec<>("Forged", Codec.STRING_ARRAY),
                    (asset, value) -> asset.forged = value,
                    asset -> asset.forged
            )
                .add()
                .append(
                    new KeyedCodec<>("Affixes", AFFIXES_CODEC),
                    (asset, value) -> asset.affixes = value != null ? value : new HashMap<>(),
                    asset -> asset.affixes
                )
            .add()
            .build();

    private static AssetStore<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>> ASSET_STORE;

    // Asset data
    private String id;
    private AssetExtraInfo.Data data;

    // Pool fields
    private int priority = AffixPool.DEFAULT_PRIORITY;
    private String description = "";
    private AppliesToAsset appliesTo;
    private String[] prefixes;
    private String[] suffixes;
    private String[] forged;
    private Map<String, String[]> affixes = new HashMap<>();

    public AffixPoolAsset() {
    }

    /**
     * Get the asset store for affix pool definitions.
     */
    @Nonnull
    public static AssetStore<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(AffixPoolAsset.class);
        }
        return ASSET_STORE;
    }

    // ========== JsonAssetWithMap Interface ==========

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    // ========== Conversion ==========

    /**
     * Convert this asset to an AffixPool model object.
     *
     * @return The AffixPool model
     */
    @Nonnull
    public AffixPool toAffixPool() {
        AffixPool.AffixPoolAppliesTo appliesToModel;
        if (appliesTo != null) {
            appliesToModel = new AffixPool.AffixPoolAppliesTo(
                    toSet(appliesTo.categories),
                    toSet(appliesTo.tags)
            );
        } else {
            appliesToModel = new AffixPool.AffixPoolAppliesTo(
                    Collections.emptySet(),
                    Collections.emptySet()
            );
        }

        // Build unified affixes-by-type map from all sources
        Map<String, List<String>> allAffixes = new HashMap<>();

        // Add standard types from Prefixes/Suffixes/Forged JSON fields
        if (prefixes != null && prefixes.length > 0) {
            allAffixes.put("prefix", toList(prefixes));
        }
        if (suffixes != null && suffixes.length > 0) {
            allAffixes.put("suffix", toList(suffixes));
        }
        if (forged != null && forged.length > 0) {
            allAffixes.put("forged", toList(forged));
        }

        // Add custom types from generic "Affixes" map (e.g., npc, npc_rare, npc_legendary)
        if (affixes != null && !affixes.isEmpty()) {
            for (Map.Entry<String, String[]> entry : affixes.entrySet()) {
                List<String> ids = toList(entry.getValue());
                if (!ids.isEmpty()) {
                    allAffixes.put(entry.getKey(), ids);
                }
            }
        }

        return new AffixPool(
                id,
                priority,
                appliesToModel,
                allAffixes
        );
    }

    private static Set<String> toSet(String[] array) {
        if (array == null || array.length == 0) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(array));
    }

    private static List<String> toList(String[] array) {
        if (array == null || array.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(array);
    }

    // ========== Accessors ==========

    public int getPriority() {
        return priority;
    }

    public AppliesToAsset getAppliesTo() {
        return appliesTo;
    }

    public String[] getPrefixes() {
        return prefixes;
    }

    public String[] getSuffixes() {
        return suffixes;
    }

    public String[] getForged() {
        return forged;
    }

    /**
     * Nested class for the AppliesTo object.
     */
    public static class AppliesToAsset {
        private String[] categories;
        private String[] tags;

        public AppliesToAsset() {
        }

        public String[] getCategories() {
            return categories;
        }

        public String[] getTags() {
            return tags;
        }
    }
}
