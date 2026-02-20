package reign.software.hyforged.stats.damage;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * JSON asset extension for Hytale damage types (DamageCause).
 * <p>
 * This allows Hyforged to define additional data for damage types without modifying
 * Hytale's core DamageCause assets. The ID should match the Hytale DamageCause ID.
 * <p>
 * JSON Schema (place in Server/Hyforged/Stats/Damage/):
 * <pre>
 * {
 *   "Id": "Fire",                                    // Must match DamageCause ID
 *   "Inherits": "Elemental",                         // Parent damage type for stat lookup
 *   "HyforgedResistanceStat": "hyforged:fire-resistance-bps",  // Stat that resists this damage
 *   "HyforgedPenetrationStat": "hyforged:fire-penetration-bps", // Stat that penetrates resistance
 *   "HyforgedElementTag": "fire"                     // Element tag for ailment triggering
 * }
 * </pre>
 * <p>
 * Following ECS principles, the damage type entity defines which resistance stat applies,
 * rather than having the stat define which damage types it resists.
 * 
 * @see <a href="../../.memory_bank/ADRs.md#adr-0006">ADR-0006</a>
 */
public class DamageTypeExtensionAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, DamageTypeExtensionAsset>> {

    /**
     * Codec for loading DamageTypeExtensionAsset from JSON.
     */
    public static final AssetBuilderCodec<String, DamageTypeExtensionAsset> CODEC = AssetBuilderCodec
            .builder(
                    DamageTypeExtensionAsset.class,
                    DamageTypeExtensionAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .appendInherited(
                    new KeyedCodec<>("Inherits", Codec.STRING),
                    (asset, value) -> asset.inherits = value,
                    asset -> asset.inherits,
                    (asset, parent) -> asset.inherits = parent.inherits
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("HyforgedResistanceStat", Codec.STRING),
                    (asset, value) -> asset.resistanceStat = value,
                    asset -> asset.resistanceStat,
                    (asset, parent) -> asset.resistanceStat = parent.resistanceStat
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("HyforgedPenetrationStat", Codec.STRING),
                    (asset, value) -> asset.penetrationStat = value,
                    asset -> asset.penetrationStat,
                    (asset, parent) -> asset.penetrationStat = parent.penetrationStat
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("HyforgedElementTag", Codec.STRING),
                    (asset, value) -> asset.elementTag = value,
                    asset -> asset.elementTag,
                    (asset, parent) -> asset.elementTag = parent.elementTag
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("HyforgedDamageBonusStat", Codec.STRING),
                    (asset, value) -> asset.damageBonusStat = value,
                    asset -> asset.damageBonusStat,
                    (asset, parent) -> {} // no inheritance — chain walked at runtime by registry
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("HyforgedDamageTakenStat", Codec.STRING),
                    (asset, value) -> asset.damageTakenStat = value,
                    asset -> asset.damageTakenStat,
                    (asset, parent) -> {} // no inheritance — chain walked at runtime by registry
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("HyforgedAilmentChanceStat", Codec.STRING),
                    (asset, value) -> asset.ailmentChanceStat = value,
                    asset -> asset.ailmentChanceStat,
                    (asset, parent) -> asset.ailmentChanceStat = parent.ailmentChanceStat
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("HyforgedAilmentDurationStat", Codec.STRING),
                    (asset, value) -> asset.ailmentDurationStat = value,
                    asset -> asset.ailmentDurationStat,
                    (asset, parent) -> asset.ailmentDurationStat = parent.ailmentDurationStat
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("HyforgedAilmentDamageStat", Codec.STRING),
                    (asset, value) -> asset.ailmentDamageStat = value,
                    asset -> asset.ailmentDamageStat,
                    (asset, parent) -> asset.ailmentDamageStat = parent.ailmentDamageStat
            )
            .add()
            .build();

    private static AssetStore<String, DamageTypeExtensionAsset, IndexedLookupTableAssetMap<String, DamageTypeExtensionAsset>> ASSET_STORE;

    // Asset data
    private String id;
    private AssetExtraInfo.Data data;

    // Extension fields
    private String inherits;
    private String resistanceStat;
    private String penetrationStat;
    private String elementTag;
    private String damageBonusStat;
    private String damageTakenStat;
    private String ailmentChanceStat;
    private String ailmentDurationStat;
    private String ailmentDamageStat;

    public DamageTypeExtensionAsset() {
    }

    /**
     * Get the asset store for damage type extensions.
     */
    @Nonnull
    public static AssetStore<String, DamageTypeExtensionAsset, IndexedLookupTableAssetMap<String, DamageTypeExtensionAsset>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(DamageTypeExtensionAsset.class);
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
     * Get the parent damage type ID for inheritance.
     * Used to look up resistance if this damage type doesn't have a specific one.
     */
    @Nullable
    public String getInherits() {
        return inherits;
    }

    /**
     * Get the resistance stat ID that reduces this damage type.
     */
    @Nullable
    public StatId getResistanceStatId() {
        return resistanceStat != null ? StatId.parse(resistanceStat) : null;
    }

    /**
     * Get the resistance stat ID as a raw string.
     */
    @Nullable
    public String getResistanceStat() {
        return resistanceStat;
    }

    /**
     * Get the penetration stat ID that bypasses resistance.
     */
    @Nullable
    public StatId getPenetrationStatId() {
        return penetrationStat != null ? StatId.parse(penetrationStat) : null;
    }

    /**
     * Get the penetration stat ID as a raw string.
     */
    @Nullable
    public String getPenetrationStat() {
        return penetrationStat;
    }

    /**
     * Get the element tag for ailment triggering.
     * Used by the ailment system to determine which ailment to apply.
     */
    @Nullable
    public String getElementTag() {
        return elementTag;
    }

    /**
     * Get the outgoing damage bonus stat for this damage type.
     * Used by {@code HyforgedDamageBonusSystem} to apply element-specific damage increases.
     * Inheritance is intentionally NOT applied here; the registry walks the chain manually
     * to accumulate bonuses at each level (e.g. fire gets both fire-bonus and elemental-bonus).
     */
    @Nullable
    public StatId getDamageBonusStatId() {
        return damageBonusStat != null ? StatId.parse(damageBonusStat) : null;
    }

    /**
     * Get the incoming damage taken multiplier stat for this damage type.
     * Used by {@code HyforgedDamageTakenSystem}.
     * Inheritance is intentionally NOT applied here; the registry walks the chain manually.
     */
    @Nullable
    public StatId getDamageTakenStatId() {
        return damageTakenStat != null ? StatId.parse(damageTakenStat) : null;
    }

    /**
     * Get the per-element ailment trigger chance stat for this damage type.
     * Used by {@code HyforgedAilmentSystem} for direct chance rolls.
     */
    @Nullable
    public StatId getAilmentChanceStatId() {
        return ailmentChanceStat != null ? StatId.parse(ailmentChanceStat) : null;
    }

    /**
     * Get the per-element ailment duration scaling stat for this damage type.
     * Used by {@code HyforgedAilmentSystem} to scale ailment duration.
     */
    @Nullable
    public StatId getAilmentDurationStatId() {
        return ailmentDurationStat != null ? StatId.parse(ailmentDurationStat) : null;
    }

    /**
     * Get the per-element ailment damage scaling stat for this damage type.
     * Used by {@code HyforgedAilmentSystem} and stored as combat meta for DoT systems.
     */
    @Nullable
    public StatId getAilmentDamageStatId() {
        return ailmentDamageStat != null ? StatId.parse(ailmentDamageStat) : null;
    }

    @Override
    public String toString() {
        return "DamageTypeExtensionAsset{" +
                "id='" + id + '\'' +
                ", inherits='" + inherits + '\'' +
                ", resistanceStat='" + resistanceStat + '\'' +
                ", penetrationStat='" + penetrationStat + '\'' +
                ", elementTag='" + elementTag + '\'' +
                '}';
    }
}
