package reign.software.hyforged.stats.damage;

import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Registry for Hyforged damage type extensions.
 * <p>
 * This registry stores additional data for Hytale's DamageCause assets, such as
 * which resistance stat should reduce the damage. It follows ECS principles by
 * having the damage type entity define its resistance relationship.
 * <p>
 * Extensions are loaded from JSON files in Server/Hyforged/Stats/Damage/ and registered
 * by {@link DamageTypeAssetLoader}.
 */
public final class DamageTypeExtensionRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final DamageTypeExtensionRegistry INSTANCE = new DamageTypeExtensionRegistry();

    // Mapping from damage cause ID to extension data
    private final Map<String, DamageTypeExtension> extensions = new HashMap<>();

    // Cached resolved mappings (damage cause ID -> resistance stat ID)
    private final Map<String, StatId> resolvedResistances = new HashMap<>();
    private final Map<String, StatId> resolvedPenetrations = new HashMap<>();
    private final Map<String, String> resolvedElementTags = new HashMap<>();
    /** Cached per-damage-type bonus stat lists (all levels of inheritance chain). */
    private final Map<String, List<StatId>> resolvedDamageBonusStats = new HashMap<>();
    /** Cached per-damage-type taken stat lists (all levels of inheritance chain). */
    private final Map<String, List<StatId>> resolvedDamageTakenStats = new HashMap<>();
    /** Cached per-damage-type ailment chance stat (first match in inheritance chain). */
    private final Map<String, StatId> resolvedAilmentChanceStats = new HashMap<>();
    /** Cached per-damage-type ailment duration stat (first match in inheritance chain). */
    private final Map<String, StatId> resolvedAilmentDurationStats = new HashMap<>();
    /** Cached per-damage-type ailment damage stat (first match in inheritance chain). */
    private final Map<String, StatId> resolvedAilmentDamageStats = new HashMap<>();

    private DamageTypeExtensionRegistry() {
    }

    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static DamageTypeExtensionRegistry get() {
        return INSTANCE;
    }

    /**
     * Register a damage type extension.
     *
     * @param damageTypeId The ID of the DamageCause this extends
     * @param extension    The extension data
     */
    public void register(@Nonnull String damageTypeId, @Nonnull DamageTypeExtension extension) {
        if (extensions.containsKey(damageTypeId)) {
            LOGGER.atWarning().log("Duplicate damage type extension for: %s (ignoring duplicate)", damageTypeId);
            return;
        }
        extensions.put(damageTypeId, extension);
        // Clear resolved caches since new data is available
        resolvedResistances.clear();
        resolvedPenetrations.clear();
        resolvedElementTags.clear();
        LOGGER.at(Level.FINE).log("Registered damage type extension: %s", damageTypeId);
    }

    /**
     * Get the extension data for a damage type.
     *
     * @param damageTypeId The DamageCause ID
     * @return The extension data, or null if none exists
     */
    @Nullable
    public DamageTypeExtension getExtension(@Nonnull String damageTypeId) {
        return extensions.get(damageTypeId);
    }

    /**
     * Get the resistance stat for a damage cause, following inheritance chain.
     * <p>
     * This will first check if the damage type has a direct Hyforged extension,
     * then follow the Hytale DamageCause inheritance chain.
     *
     * @param damageCause The damage cause
     * @return The resistance stat ID, or null if none defined
     */
    @Nullable
    public StatId getResistanceStatForDamage(@Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        
        // Check cache first
        if (resolvedResistances.containsKey(id)) {
            return resolvedResistances.get(id);
        }
        
        // Resolve from extension or parent
        StatId resolved = resolveResistanceStat(damageCause);
        resolvedResistances.put(id, resolved);
        return resolved;
    }

    /**
     * Get the penetration stat for a damage cause, following inheritance chain.
     *
     * @param damageCause The damage cause
     * @return The penetration stat ID, or null if none defined
     */
    @Nullable
    public StatId getPenetrationStatForDamage(@Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        
        // Check cache first
        if (resolvedPenetrations.containsKey(id)) {
            return resolvedPenetrations.get(id);
        }
        
        // Resolve from extension or parent
        StatId resolved = resolvePenetrationStat(damageCause);
        resolvedPenetrations.put(id, resolved);
        return resolved;
    }

    /**
     * Get the element tag for a damage cause, following inheritance chain.
     * <p>
     * Element tags are used by the ailment system to determine which ailment
     * effect should be applied when damage of this type is dealt.
     *
     * @param damageCause The damage cause
     * @return The element tag (e.g., "fire", "ice"), or null if none defined
     */
    @Nullable
    public String getElementTagForDamage(@Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        
        // Check cache first
        if (resolvedElementTags.containsKey(id)) {
            return resolvedElementTags.get(id);
        }
        
        // Resolve from extension or parent
        String resolved = resolveElementTag(damageCause);
        resolvedElementTags.put(id, resolved);
        return resolved;
    }

    /**
     * Resolve resistance stat following inheritance chain.
     */
    @Nullable
    private StatId resolveResistanceStat(@Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        
        // Check for direct Hyforged extension
        DamageTypeExtension ext = extensions.get(id);
        if (ext != null && ext.resistanceStat() != null) {
            return ext.resistanceStat();
        }
        
        // Check if extension defines an inheritance override
        String parentId = ext != null ? ext.inherits() : damageCause.getInherits();
        
        if (parentId != null) {
            DamageCause parentCause = DamageCause.getAssetMap().getAsset(parentId);
            if (parentCause != null) {
                return resolveResistanceStat(parentCause);
            }
        }
        
        return null;
    }

    /**
     * Resolve penetration stat following inheritance chain.
     */
    @Nullable
    private StatId resolvePenetrationStat(@Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        
        // Check for direct Hyforged extension
        DamageTypeExtension ext = extensions.get(id);
        if (ext != null && ext.penetrationStat() != null) {
            return ext.penetrationStat();
        }
        
        // Check if extension defines an inheritance override
        String parentId = ext != null ? ext.inherits() : damageCause.getInherits();
        
        if (parentId != null) {
            DamageCause parentCause = DamageCause.getAssetMap().getAsset(parentId);
            if (parentCause != null) {
                return resolvePenetrationStat(parentCause);
            }
        }
        
        return null;
    }

    /**
     * Resolve element tag following inheritance chain.
     */
    @Nullable
    private String resolveElementTag(@Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        
        // Check for direct Hyforged extension
        DamageTypeExtension ext = extensions.get(id);
        if (ext != null && ext.elementTag() != null) {
            return ext.elementTag();
        }
        
        // Check if extension defines an inheritance override
        String parentId = ext != null ? ext.inherits() : damageCause.getInherits();
        
        if (parentId != null) {
            DamageCause parentCause = DamageCause.getAssetMap().getAsset(parentId);
            if (parentCause != null) {
                return resolveElementTag(parentCause);
            }
        }
        
        return null;
    }

    /**
     * Clear all registered extensions.
     * Called on asset reload.
     */
    public void clear() {
        extensions.clear();
        resolvedResistances.clear();
        resolvedPenetrations.clear();
        resolvedElementTags.clear();
        resolvedDamageBonusStats.clear();
        resolvedDamageTakenStats.clear();
        resolvedAilmentChanceStats.clear();
        resolvedAilmentDurationStats.clear();
        resolvedAilmentDamageStats.clear();
        LOGGER.at(Level.FINE).log("Cleared damage type extension registry");
    }

    /**
     * Get the number of registered extensions.
     */
    public int size() {
        return extensions.size();
    }

    /**
     * Check if an extension exists for a damage type.
     */
    public boolean hasExtension(@Nonnull String damageTypeId) {
        return extensions.containsKey(damageTypeId);
    }

    /**
     * Get all outgoing damage bonus stats for a damage cause, walking the full inheritance chain.
     * <p>
     * Each level that explicitly declares a {@code HyforgedDamageBonusStat} contributes one stat
     * to the returned list. For example, {@code Fire} damage returns both
     * {@code fire-damage-increased-bps} (from Fire.json) and
     * {@code elemental-damage-increased-bps} (from Elemental.json).
     *
     * @param damageCause The damage cause
     * @return Immutable list of bonus stat IDs (may be empty)
     */
    @Nonnull
    public List<StatId> getDamageBonusStatsForDamage(@Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        List<StatId> cached = resolvedDamageBonusStats.get(id);
        if (cached != null) {
            return cached;
        }
        List<StatId> result = new ArrayList<>();
        collectDamageBonusStats(damageCause, result);
        List<StatId> immutable = Collections.unmodifiableList(result);
        resolvedDamageBonusStats.put(id, immutable);
        return immutable;
    }

    /**
     * Get all incoming damage taken modifier stats for a damage cause, walking the full inheritance chain.
     * <p>
     * Each level that explicitly declares a {@code HyforgedDamageTakenStat} contributes one stat.
     *
     * @param damageCause The damage cause
     * @return Immutable list of taken stat IDs (may be empty)
     */
    @Nonnull
    public List<StatId> getDamageTakenStatsForDamage(@Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        List<StatId> cached = resolvedDamageTakenStats.get(id);
        if (cached != null) {
            return cached;
        }
        List<StatId> result = new ArrayList<>();
        collectDamageTakenStats(damageCause, result);
        List<StatId> immutable = Collections.unmodifiableList(result);
        resolvedDamageTakenStats.put(id, immutable);
        return immutable;
    }

    /** Accumulate damageBonusStat at each level of the inheritance chain. */
    private void collectDamageBonusStats(@Nonnull DamageCause damageCause, @Nonnull List<StatId> acc) {
        DamageTypeExtension ext = extensions.get(damageCause.getId());
        if (ext != null && ext.damageBonusStat() != null) {
            acc.add(ext.damageBonusStat());
        }
        String parentId = ext != null ? ext.inherits() : damageCause.getInherits();
        if (parentId != null) {
            DamageCause parentCause = DamageCause.getAssetMap().getAsset(parentId);
            if (parentCause != null) {
                collectDamageBonusStats(parentCause, acc);
            }
        }
    }

    /** Accumulate damageTakenStat at each level of the inheritance chain. */
    private void collectDamageTakenStats(@Nonnull DamageCause damageCause, @Nonnull List<StatId> acc) {
        DamageTypeExtension ext = extensions.get(damageCause.getId());
        if (ext != null && ext.damageTakenStat() != null) {
            acc.add(ext.damageTakenStat());
        }
        String parentId = ext != null ? ext.inherits() : damageCause.getInherits();
        if (parentId != null) {
            DamageCause parentCause = DamageCause.getAssetMap().getAsset(parentId);
            if (parentCause != null) {
                collectDamageTakenStats(parentCause, acc);
            }
        }
    }

    /**
     * Get the ailment trigger chance stat for a damage cause, following inheritance chain.
     * Returns the first non-null match walking up the chain.
     */
    @Nullable
    public StatId getAilmentChanceStatForDamage(@Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        if (resolvedAilmentChanceStats.containsKey(id)) {
            return resolvedAilmentChanceStats.get(id);
        }
        StatId resolved = resolveAilmentStat(damageCause, ext -> ext.ailmentChanceStat());
        resolvedAilmentChanceStats.put(id, resolved);
        return resolved;
    }

    /**
     * Get the ailment duration scaling stat for a damage cause, following inheritance chain.
     * Returns the first non-null match walking up the chain.
     */
    @Nullable
    public StatId getAilmentDurationStatForDamage(@Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        if (resolvedAilmentDurationStats.containsKey(id)) {
            return resolvedAilmentDurationStats.get(id);
        }
        StatId resolved = resolveAilmentStat(damageCause, ext -> ext.ailmentDurationStat());
        resolvedAilmentDurationStats.put(id, resolved);
        return resolved;
    }

    /**
     * Get the ailment damage scaling stat for a damage cause, following inheritance chain.
     * Returns the first non-null match walking up the chain.
     */
    @Nullable
    public StatId getAilmentDamageStatForDamage(@Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        if (resolvedAilmentDamageStats.containsKey(id)) {
            return resolvedAilmentDamageStats.get(id);
        }
        StatId resolved = resolveAilmentStat(damageCause, ext -> ext.ailmentDamageStat());
        resolvedAilmentDamageStats.put(id, resolved);
        return resolved;
    }

    /**
     * Generic chain-walk resolver for ailment-related stats.
     * Follows the same inheritance pattern as {@link #resolveResistanceStat}.
     */
    @Nullable
    private StatId resolveAilmentStat(
            @Nonnull DamageCause damageCause,
            @Nonnull java.util.function.Function<DamageTypeExtension, StatId> accessor
    ) {
        String id = damageCause.getId();
        DamageTypeExtension ext = extensions.get(id);
        if (ext != null && accessor.apply(ext) != null) {
            return accessor.apply(ext);
        }
        String parentId = ext != null ? ext.inherits() : damageCause.getInherits();
        if (parentId != null) {
            DamageCause parentCause = DamageCause.getAssetMap().getAsset(parentId);
            if (parentCause != null) {
                return resolveAilmentStat(parentCause, accessor);
            }
        }
        return null;
    }
}
