package reign.software.hyforged.affix.registry;

import reign.software.hyforged.affix.model.AffixPool;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Central registry for affix pools.
 * <p>
 * Affix pools define which affixes can appear on which item types. Pools are
 * loaded from JSON at {@code Server/Hyforged/AffixPools/*.json}.
 * <p>
 * Pool Resolution: When multiple pools match an item, the pool with the highest
 * priority is selected. Ties are resolved by lexicographic pool ID order.
 * <p>
 * This is a singleton registry loaded at startup, NOT an ECS component.
 * <p>
 * Duplicate ID Policy: When a duplicate ID is registered, the latest entry wins
 * (by load order) and a WARN log is emitted to highlight the override.
 */
public final class AffixPoolRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(AffixPoolRegistry.class.getName());
    private static AffixPoolRegistry instance;
    
    private final Map<String, AffixPool> poolsById = new ConcurrentHashMap<>();
    
    // Sorted list of pools by priority (descending) for efficient resolution
    private List<AffixPool> poolsByPriority = new ArrayList<>();
    private boolean sortDirty = true;
    
    private boolean frozen = false;
    
    private AffixPoolRegistry() {}
    
    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static synchronized AffixPoolRegistry get() {
        if (instance == null) {
            instance = new AffixPoolRegistry();
        }
        return instance;
    }
    
    /**
     * Reset the registry (for testing or reload).
     */
    public static synchronized void reset() {
        instance = new AffixPoolRegistry();
    }
    
    /**
     * Register an affix pool.
     * <p>
     * If a pool with the same ID already exists, it will be replaced and a warning logged.
     *
     * @param pool The affix pool to register
     * @throws IllegalStateException if registry is frozen
     */
    public synchronized void register(@Nonnull AffixPool pool) {
        Objects.requireNonNull(pool, "pool cannot be null");
        
        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot register new affix pools");
        }
        
        String id = pool.id();
        if (poolsById.containsKey(id)) {
            LOGGER.log(Level.WARNING, "Affix pool ''{0}'' is being overridden by a later definition", id);
        }
        
        poolsById.put(id, pool);
        sortDirty = true;
        
        LOGGER.log(Level.FINE, "Registered affix pool: {0} (priority={1}, affixes={2})", 
            new Object[]{id, pool.priority(), pool.getTotalAffixCount()});
    }
    
    /**
     * Get an affix pool by ID.
     *
     * @param id The pool ID
     * @return The affix pool, or null if not found
     */
    @Nullable
    public AffixPool get(@Nonnull String id) {
        return poolsById.get(id);
    }
    
    /**
     * Get an affix pool by ID, throwing if not found.
     *
     * @param id The pool ID
     * @return The affix pool
     * @throws NoSuchElementException if the pool is not found
     */
    @Nonnull
    public AffixPool getRequired(@Nonnull String id) {
        AffixPool pool = poolsById.get(id);
        if (pool == null) {
            throw new NoSuchElementException("Affix pool not found: " + id);
        }
        return pool;
    }
    
    /**
     * Check if an affix pool with the given ID exists.
     */
    public boolean contains(@Nonnull String id) {
        return poolsById.containsKey(id);
    }
    
    /**
     * Resolve the best matching pool for an item with the given categories and tags.
     * <p>
     * Returns the highest-priority pool that matches. If multiple pools have the same
     * priority, the one with the lexicographically smallest ID is returned.
     *
     * @param categories The item's categories
     * @param tags The item's tags
     * @return The best matching pool, or null if no pool matches
     */
    @Nullable
    public AffixPool resolve(@Nonnull Set<String> categories, @Nonnull Set<String> tags) {
        ensureSorted();
        
        for (AffixPool pool : poolsByPriority) {
            if (pool.appliesTo(categories, tags)) {
                return pool;
            }
        }
        return null;
    }
    
    /**
     * Find all pools that match an item with the given categories and tags.
     * <p>
     * Returns pools sorted by priority (descending), then by ID (ascending).
     *
     * @param categories The item's categories
     * @param tags The item's tags
     * @return List of matching pools
     */
    @Nonnull
    public List<AffixPool> findMatching(@Nonnull Set<String> categories, @Nonnull Set<String> tags) {
        ensureSorted();
        
        return poolsByPriority.stream()
            .filter(pool -> pool.appliesTo(categories, tags))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all registered affix pools.
     */
    @Nonnull
    public Collection<AffixPool> getAll() {
        return Collections.unmodifiableCollection(poolsById.values());
    }
    
    /**
     * Get the count of registered affix pools.
     */
    public int size() {
        return poolsById.size();
    }
    
    /**
     * Freeze the registry, preventing further modifications.
     */
    public synchronized void freeze() {
        ensureSorted();
        frozen = true;
        LOGGER.log(Level.INFO, "AffixPoolRegistry frozen with {0} pools", poolsById.size());
    }
    
    /**
     * Check if the registry is frozen.
     */
    public boolean isFrozen() {
        return frozen;
    }
    
    private void ensureSorted() {
        if (sortDirty) {
            poolsByPriority = poolsById.values().stream()
                .sorted(Comparator
                    .comparingInt(AffixPool::priority).reversed()  // Higher priority first
                    .thenComparing(AffixPool::id))                 // Then by ID for tie-breaking
                .collect(Collectors.toList());
            sortDirty = false;
        }
    }
}
