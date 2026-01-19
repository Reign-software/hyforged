package reign.software.hyforged.stats;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central registry for stat and category definitions.
 * Tags are simple strings - stats declare their tags, and the registry
 * builds tag-to-stat mappings automatically.
 * <p>
 * This is NOT an ECS component - it's a static registry loaded at startup.
 * <p>
 * Stats are indexed both by their full ID (namespace:name) and by a compact
 * integer index for efficient runtime access.
 */
public final class StatDefinitionRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(StatDefinitionRegistry.class.getName());
    private static StatDefinitionRegistry instance;
    
    private final Map<String, StatDefinition> statsByFullId = new ConcurrentHashMap<>();
    private final Map<String, CategoryDefinition> categoriesById = new ConcurrentHashMap<>();
    private final List<StatDefinition> statsByIndex = new ArrayList<>();
    private final Map<String, Integer> fullIdToIndex = new ConcurrentHashMap<>();
    
    // Reverse lookup: tag -> stats that have that tag (built from stats)
    private final Map<String, Set<Integer>> tagToStatIndices = new ConcurrentHashMap<>();
    
    // Reverse lookup: category -> stats in that category
    private final Map<String, Set<Integer>> categoryToStatIndices = new ConcurrentHashMap<>();
    
    private boolean frozen = false;
    
    private StatDefinitionRegistry() {}
    
    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static synchronized StatDefinitionRegistry get() {
        if (instance == null) {
            instance = new StatDefinitionRegistry();
        }
        return instance;
    }
    
    /**
     * Reset the registry (for testing or reload).
     */
    public static synchronized void reset() {
        instance = new StatDefinitionRegistry();
    }
    
    /**
     * Register a stat definition.
     * @return The index assigned to this stat
     * @throws IllegalStateException if registry is frozen or stat already exists
     */
    public synchronized int registerStat(@Nonnull StatDefinition stat) {
        Objects.requireNonNull(stat, "stat cannot be null");
        
        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot register new stats");
        }
        
        String fullId = stat.id().fullId();
        if (statsByFullId.containsKey(fullId)) {
            throw new IllegalStateException("Stat already registered: " + fullId);
        }
        
        int index = statsByIndex.size();
        statsByFullId.put(fullId, stat);
        statsByIndex.add(stat);
        fullIdToIndex.put(fullId, index);
        
        // Register stat under its tags
        for (String tag : stat.tags()) {
            tagToStatIndices.computeIfAbsent(tag, k -> new HashSet<>()).add(index);
        }
        
        // Register stat under its category
        categoryToStatIndices.computeIfAbsent(stat.category(), k -> new HashSet<>()).add(index);
        
        LOGGER.fine("Registered stat: " + fullId + " at index " + index);
        return index;
    }
    
    /**
     * Register a category definition.
     * @throws IllegalStateException if registry is frozen or category already exists
     */
    public synchronized void registerCategory(@Nonnull CategoryDefinition category) {
        Objects.requireNonNull(category, "category cannot be null");
        
        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot register new categories");
        }
        
        String id = category.id();
        if (categoriesById.containsKey(id)) {
            throw new IllegalStateException("Category already registered: " + id);
        }
        
        categoriesById.put(id, category);
        LOGGER.fine("Registered category: " + id);
    }
    
    /**
     * Freeze the registry to prevent further modifications.
     * Should be called after all stats are loaded.
     */
    public synchronized void freeze() {
        this.frozen = true;
        LOGGER.info("StatDefinitionRegistry frozen with " + statsByIndex.size() + " stats, " + 
                tagToStatIndices.size() + " unique tags, and " + categoriesById.size() + " categories");
    }
    
    /**
     * Check if the registry is frozen.
     */
    public boolean isFrozen() {
        return frozen;
    }
    
    /**
     * Get stat definition by full ID.
     */
    @Nullable
    public StatDefinition getStat(@Nonnull String fullId) {
        return statsByFullId.get(fullId);
    }
    
    /**
     * Get stat definition by StatId.
     */
    @Nullable
    public StatDefinition getStat(@Nonnull StatId id) {
        return statsByFullId.get(id.fullId());
    }
    
    /**
     * Get stat definition by index.
     */
    @Nullable
    public StatDefinition getStat(int index) {
        if (index < 0 || index >= statsByIndex.size()) {
            return null;
        }
        return statsByIndex.get(index);
    }
    
    /**
     * Get the index for a stat ID.
     * @return The index, or -1 if not found
     */
    public int getIndex(@Nonnull String fullId) {
        Integer index = fullIdToIndex.get(fullId);
        return index != null ? index : -1;
    }
    
    /**
     * Get the index for a stat ID.
     * @return The index, or -1 if not found
     */
    public int getIndex(@Nonnull StatId id) {
        return getIndex(id.fullId());
    }
    
    /**
     * Get all stat indices that have a given tag.
     */
    @Nonnull
    public Set<Integer> getStatIndicesForTag(@Nonnull String tagId) {
        Set<Integer> indices = tagToStatIndices.get(tagId);
        return indices != null ? Collections.unmodifiableSet(indices) : Collections.emptySet();
    }
    
    /**
     * Get the total number of registered stats.
     */
    public int getStatCount() {
        return statsByIndex.size();
    }
    
    /**
     * Get all registered stat definitions.
     */
    @Nonnull
    public Collection<StatDefinition> getAllStats() {
        return Collections.unmodifiableCollection(statsByFullId.values());
    }
    
    /**
     * Get all unique tags (derived from stats).
     */
    @Nonnull
    public Set<String> getAllTags() {
        return Collections.unmodifiableSet(tagToStatIndices.keySet());
    }
    
    /**
     * Get category definition by ID.
     */
    @Nullable
    public CategoryDefinition getCategory(@Nonnull String id) {
        return categoriesById.get(id);
    }
    
    /**
     * Get all registered category definitions.
     */
    @Nonnull
    public Collection<CategoryDefinition> getAllCategories() {
        return Collections.unmodifiableCollection(categoriesById.values());
    }
    
    /**
     * Get all stat indices in a category.
     */
    @Nonnull
    public Set<Integer> getStatIndicesForCategory(@Nonnull String categoryId) {
        Set<Integer> indices = categoryToStatIndices.get(categoryId);
        return indices != null ? Collections.unmodifiableSet(indices) : Collections.emptySet();
    }
    
    /**
     * Get all stats in a category.
     */
    @Nonnull
    public Collection<StatDefinition> getStatsInCategory(@Nonnull String categoryId) {
        Set<Integer> indices = categoryToStatIndices.get(categoryId);
        if (indices == null || indices.isEmpty()) {
            return Collections.emptyList();
        }
        List<StatDefinition> stats = new ArrayList<>(indices.size());
        for (int index : indices) {
            StatDefinition stat = getStat(index);
            if (stat != null) {
                stats.add(stat);
            }
        }
        return stats;
    }
    
    /**
     * Check if a category exists.
     */
    public boolean hasCategory(@Nonnull String id) {
        return categoriesById.containsKey(id);
    }
    
    /**
     * Check if a stat exists.
     */
    public boolean hasStat(@Nonnull String fullId) {
        return statsByFullId.containsKey(fullId);
    }
    
    /**
     * Check if a tag exists (has any stats using it).
     */
    public boolean hasTag(@Nonnull String tag) {
        return tagToStatIndices.containsKey(tag);
    }
}
