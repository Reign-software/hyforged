package reign.software.hyforged.stats.damage;

import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Registry for Hyforged damage type extensions.
 * <p>
 * This registry stores additional data for Hytale's DamageCause assets, such as
 * which resistance stat should reduce the damage. It follows ECS principles by
 * having the damage type entity define its resistance relationship.
 * <p>
 * Extensions are loaded from JSON files in Server/Hyforged/Damage/ and registered
 * by {@link DamageTypeAssetLoader}.
 */
public final class DamageTypeExtensionRegistry {

    private static final Logger LOGGER = Logger.getLogger(DamageTypeExtensionRegistry.class.getName());

    private static final DamageTypeExtensionRegistry INSTANCE = new DamageTypeExtensionRegistry();

    // Mapping from damage cause ID to extension data
    private final Map<String, DamageTypeExtension> extensions = new HashMap<>();

    // Cached resolved mappings (damage cause ID -> resistance stat ID)
    private final Map<String, StatId> resolvedResistances = new HashMap<>();
    private final Map<String, StatId> resolvedPenetrations = new HashMap<>();

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
            LOGGER.warning("Duplicate damage type extension for: " + damageTypeId + " (ignoring duplicate)");
            return;
        }
        extensions.put(damageTypeId, extension);
        // Clear resolved caches since new data is available
        resolvedResistances.clear();
        resolvedPenetrations.clear();
        LOGGER.fine("Registered damage type extension: " + damageTypeId);
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
     * Clear all registered extensions.
     * Called on asset reload.
     */
    public void clear() {
        extensions.clear();
        resolvedResistances.clear();
        resolvedPenetrations.clear();
        LOGGER.fine("Cleared damage type extension registry");
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
}
