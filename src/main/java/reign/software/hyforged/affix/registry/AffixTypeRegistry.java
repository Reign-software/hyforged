package reign.software.hyforged.affix.registry;

import reign.software.hyforged.affix.model.AffixType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Central registry for affix type definitions.
 * <p>
 * Affix types define how affixes behave (prefix, suffix, forged, etc.) and are
 * loaded from JSON at {@code Server/Hyforged/Affixes/Types/*.json}.
 * <p>
 * This is a singleton registry loaded at startup, NOT an ECS component.
 * <p>
 * Duplicate ID Policy: When a duplicate ID is registered, the latest entry wins
 * (by load order) and a WARN log is emitted to highlight the override.
 */
public final class AffixTypeRegistry {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static AffixTypeRegistry instance;
    
    private final Map<String, AffixType> typesById = new ConcurrentHashMap<>();
    private boolean frozen = false;
    
    private AffixTypeRegistry() {}
    
    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static synchronized AffixTypeRegistry get() {
        if (instance == null) {
            instance = new AffixTypeRegistry();
        }
        return instance;
    }
    
    /**
     * Reset the registry (for testing or reload).
     */
    public static synchronized void reset() {
        instance = new AffixTypeRegistry();
    }
    
    /**
     * Register an affix type.
     * <p>
     * If a type with the same ID already exists, it will be replaced and a warning logged.
     *
     * @param type The affix type to register
     * @throws IllegalStateException if registry is frozen
     */
    public synchronized void register(@Nonnull AffixType type) {
        Objects.requireNonNull(type, "type cannot be null");
        
        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot register new affix types");
        }
        
        String id = type.id();
        if (typesById.containsKey(id)) {
            LOGGER.atWarning().log("Affix type '%s' is being overridden by a later definition", id);
        }
        
        typesById.put(id, type);
        LOGGER.at(Level.FINE).log("Registered affix type: %s", id);
    }
    
    /**
     * Get an affix type by ID.
     *
     * @param id The type ID
     * @return The affix type, or null if not found
     */
    @Nullable
    public AffixType get(@Nonnull String id) {
        return typesById.get(id);
    }
    
    /**
     * Get an affix type by ID, throwing if not found.
     *
     * @param id The type ID
     * @return The affix type
     * @throws NoSuchElementException if the type is not found
     */
    @Nonnull
    public AffixType getRequired(@Nonnull String id) {
        AffixType type = typesById.get(id);
        if (type == null) {
            throw new NoSuchElementException("Affix type not found: " + id);
        }
        return type;
    }
    
    /**
     * Check if an affix type with the given ID exists.
     */
    public boolean contains(@Nonnull String id) {
        return typesById.containsKey(id);
    }
    
    /**
     * Get all registered affix types.
     */
    @Nonnull
    public Collection<AffixType> getAll() {
        return Collections.unmodifiableCollection(typesById.values());
    }
    
    /**
     * Get the count of registered affix types.
     */
    public int size() {
        return typesById.size();
    }
    
    /**
     * Freeze the registry, preventing further modifications.
     */
    public synchronized void freeze() {
        frozen = true;
        LOGGER.atInfo().log("AffixTypeRegistry frozen with %s types", typesById.size());
    }
    
    /**
     * Check if the registry is frozen.
     */
    public boolean isFrozen() {
        return frozen;
    }
}
