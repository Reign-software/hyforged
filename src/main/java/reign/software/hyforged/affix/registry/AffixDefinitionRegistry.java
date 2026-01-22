package reign.software.hyforged.affix.registry;

import reign.software.hyforged.affix.model.AffixDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Central registry for affix definitions.
 * <p>
 * Affix definitions describe the affixes that can be rolled on items and are
 * loaded from JSON at {@code Server/Hyforged/Affixes/*.json}.
 * <p>
 * This is a singleton registry loaded at startup, NOT an ECS component.
 * <p>
 * Duplicate ID Policy: When a duplicate ID is registered, the latest entry wins
 * (by load order) and a WARN log is emitted to highlight the override.
 */
public final class AffixDefinitionRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(AffixDefinitionRegistry.class.getName());
    private static AffixDefinitionRegistry instance;
    
    private final Map<String, AffixDefinition> affixesById = new ConcurrentHashMap<>();
    
    // Indexes for efficient lookup
    private final Map<String, Set<String>> affixesByType = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> affixesByStat = new ConcurrentHashMap<>(); // Key: stat ID string
    
    private boolean frozen = false;
    
    private AffixDefinitionRegistry() {}
    
    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static synchronized AffixDefinitionRegistry get() {
        if (instance == null) {
            instance = new AffixDefinitionRegistry();
        }
        return instance;
    }
    
    /**
     * Reset the registry (for testing or reload).
     */
    public static synchronized void reset() {
        instance = new AffixDefinitionRegistry();
    }
    
    /**
     * Register an affix definition.
     * <p>
     * If an affix with the same ID already exists, it will be replaced and a warning logged.
     *
     * @param affix The affix definition to register
     * @throws IllegalStateException if registry is frozen
     */
    public synchronized void register(@Nonnull AffixDefinition affix) {
        Objects.requireNonNull(affix, "affix cannot be null");
        
        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot register new affixes");
        }
        
        String id = affix.id();
        
        // Check for duplicate and log warning
        AffixDefinition existing = affixesById.get(id);
        if (existing != null) {
            LOGGER.log(Level.WARNING, "Affix ''{0}'' is being overridden by a later definition", id);
            
            // Remove from old indexes
            affixesByType.computeIfPresent(existing.type(), (k, v) -> {
                v.remove(id);
                return v.isEmpty() ? null : v;
            });
            // Remove from all stat indexes for the old affix
            for (String statId : existing.getStatIds()) {
                affixesByStat.computeIfPresent(statId, (k, v) -> {
                    v.remove(id);
                    return v.isEmpty() ? null : v;
                });
            }
        }
        
        // Register the affix
        affixesById.put(id, affix);
        
        // Update indexes
        affixesByType.computeIfAbsent(affix.type(), k -> ConcurrentHashMap.newKeySet()).add(id);
        // Index by all stats this affix grants
        for (String statId : affix.getStatIds()) {
            affixesByStat.computeIfAbsent(statId, k -> ConcurrentHashMap.newKeySet()).add(id);
        }
        
        LOGGER.log(Level.FINE, "Registered affix: {0} (type={1}, stats={2})", 
            new Object[]{id, affix.type(), affix.getStatIds()});
    }
    
    /**
     * Get an affix definition by ID.
     *
     * @param id The affix ID
     * @return The affix definition, or null if not found
     */
    @Nullable
    public AffixDefinition get(@Nonnull String id) {
        return affixesById.get(id);
    }
    
    /**
     * Get an affix definition by ID, throwing if not found.
     *
     * @param id The affix ID
     * @return The affix definition
     * @throws NoSuchElementException if the affix is not found
     */
    @Nonnull
    public AffixDefinition getRequired(@Nonnull String id) {
        AffixDefinition affix = affixesById.get(id);
        if (affix == null) {
            throw new NoSuchElementException("Affix not found: " + id);
        }
        return affix;
    }
    
    /**
     * Check if an affix with the given ID exists.
     */
    public boolean contains(@Nonnull String id) {
        return affixesById.containsKey(id);
    }
    
    /**
     * Get all affixes of a specific type.
     *
     * @param typeId The affix type ID (e.g., "prefix", "suffix")
     * @return List of affix definitions of that type
     */
    @Nonnull
    public List<AffixDefinition> getByType(@Nonnull String typeId) {
        Set<String> ids = affixesByType.get(typeId);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
            .map(affixesById::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all affixes that modify a specific stat.
     *
     * @param statId The stat ID (e.g., "hyforged:strength")
     * @return List of affix definitions that modify that stat
     */
    @Nonnull
    public List<AffixDefinition> getByStat(@Nonnull String statId) {
        Set<String> ids = affixesByStat.get(statId);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
            .map(affixesById::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all registered affix definitions.
     */
    @Nonnull
    public Collection<AffixDefinition> getAll() {
        return Collections.unmodifiableCollection(affixesById.values());
    }
    
    /**
     * Get the count of registered affixes.
     */
    public int size() {
        return affixesById.size();
    }
    
    /**
     * Get the count of affixes for a specific type.
     */
    public int countByType(@Nonnull String typeId) {
        Set<String> ids = affixesByType.get(typeId);
        return ids != null ? ids.size() : 0;
    }
    
    /**
     * Freeze the registry, preventing further modifications.
     */
    public synchronized void freeze() {
        frozen = true;
        LOGGER.log(Level.INFO, "AffixDefinitionRegistry frozen with {0} affixes", affixesById.size());
    }
    
    /**
     * Check if the registry is frozen.
     */
    public boolean isFrozen() {
        return frozen;
    }
}
