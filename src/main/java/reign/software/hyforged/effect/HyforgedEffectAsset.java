package reign.software.hyforged.effect;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JSON asset defining a Hyforged effect with optional Hyforged stat modifiers.
 * <p>
 * This asset can embed a full Hytale {@link EntityEffect} definition to avoid
 * needing separate JSON files. The contained EntityEffect will be loaded into
 * the core EntityEffect registry automatically.
 * <p>
 * JSON Schema (place in Server/Hyforged/Effects/):
 * <pre>
 * {
 *   "Id": "hyforged:haste",
 *   "EntityEffect": {
 *     "Duration": 6,
 *     "Debuff": false,
 *     "StatusEffectIcon": "UI/StatusEffects/Haste.png"
 *   },
 *   "HyforgedModifiers": [
 *     { "StatId": "hyforged:movement-speed-bps", "StackType": "INCREASED", "Amount": 1500 }
 *   ]
 * }
 * </pre>
 */
public class HyforgedEffectAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, HyforgedEffectAsset>> {

    public static final AssetBuilderCodec<String, HyforgedEffectAsset> CODEC = AssetBuilderCodec
            .builder(
                    HyforgedEffectAsset.class,
                    HyforgedEffectAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .append(
                    new KeyedCodec<>(
                            "EntityEffect",
                            new ContainedAssetCodec<>(EntityEffect.class, EntityEffect.CODEC, ContainedAssetCodec.Mode.INHERIT_ID)
                    ),
                    (asset, value) -> asset.entityEffectId = value,
                    asset -> asset.entityEffectId
            )
            .add()
            .append(
                    new KeyedCodec<>("HyforgedModifiers", new ArrayCodec<>(HyforgedEffectModifierSpec.CODEC, HyforgedEffectModifierSpec[]::new)),
                    (asset, value) -> asset.hyforgedModifiers = value,
                    asset -> asset.hyforgedModifiers
            )
            .add()
            .build();

    private static AssetStore<String, HyforgedEffectAsset, IndexedLookupTableAssetMap<String, HyforgedEffectAsset>> ASSET_STORE;

    private String id;
    private AssetExtraInfo.Data data;

    private String entityEffectId;
    private HyforgedEffectModifierSpec[] hyforgedModifiers = new HyforgedEffectModifierSpec[0];

    public HyforgedEffectAsset() {
    }

    @Nonnull
    public static AssetStore<String, HyforgedEffectAsset, IndexedLookupTableAssetMap<String, HyforgedEffectAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(HyforgedEffectAsset.class);
        }
        return ASSET_STORE;
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    /**
     * Get the EntityEffect ID to apply when this effect is active.
     * If the EntityEffect is embedded, this will match {@link #getId()}.
     */
    @Nullable
    public String getEntityEffectId() {
        return entityEffectId != null ? entityEffectId : id;
    }

    @Nonnull
    public List<HyforgedEffectModifierSpec> getHyforgedModifiers() {
        if (hyforgedModifiers == null || hyforgedModifiers.length == 0) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(hyforgedModifiers));
    }
}
