package reign.software.hyforged.stats;

import com.hypixel.hytale.assetstore.AssetRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import reign.software.hyforged.stats.scaling.ScalingRule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central registry for stat and category definitions.
 * <p>
 * Tags use the Hytale {@link AssetRegistry} tag system for efficient integer-based
 * lookups. Stats declare their tags as strings, which are converted to integer indices
 * via {@link AssetRegistry#getOrCreateTagIndex(String)}.
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
    
    // Reverse lookup: tagIndex -> stats that have that tag (uses Hytale AssetRegistry tag indices)
    private final Int2ObjectMap<IntSet> tagIndexToStatIndices = new Int2ObjectOpenHashMap<>();
    
    // Reverse lookup: category -> stats in that category
    private final Map<String, Set<Integer>> categoryToStatIndices = new ConcurrentHashMap<>();
    
    // Dependency graph: stat index -> set of stat indices that depend on it (reverse lookup)
    private final Map<Integer, Set<Integer>> dependents = new HashMap<>();
    
    // Dependency graph: stat index -> set of stat indices it depends on (forward lookup)
    private final Map<Integer, Set<Integer>> dependencies = new HashMap<>();
    
    // Topological order for evaluating stats (computed at freeze time)
    private int[] evaluationOrder = new int[0];
    
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
        
        // Register stat under its tags using Hytale's AssetRegistry tag system
        for (String tag : stat.tags()) {
            int tagIndex = AssetRegistry.getOrCreateTagIndex(tag);
            tagIndexToStatIndices.computeIfAbsent(tagIndex, k -> new IntOpenHashSet()).add(index);
        }
        
        // Register stat under its category
        categoryToStatIndices.computeIfAbsent(stat.category(), k -> new HashSet<>()).add(index);
        
        // Build dependency graph edges from scaling rules
        // Note: source stats may not be registered yet, so we store StatIds and resolve later
        if (stat.hasScaling()) {
            for (ScalingRule rule : stat.scaling()) {
                String sourceFullId = rule.source().fullId();
                Integer sourceIndex = fullIdToIndex.get(sourceFullId);
                if (sourceIndex != null) {
                    // Source already registered - add edge now
                    addDependencyEdge(sourceIndex, index);
                }
                // If source not registered yet, edge will be added when we call resolvePendingDependencies()
            }
        }
        
        // Check if any already-registered stats depend on this newly registered stat
        resolvePendingDependencies(stat.id(), index);
        
        LOGGER.fine("Registered stat: " + fullId + " at index " + index);
        return index;
    }
    
    /**
     * Add a dependency edge: targetIndex depends on sourceIndex.
     */
    private void addDependencyEdge(int sourceIndex, int targetIndex) {
        dependents.computeIfAbsent(sourceIndex, k -> new HashSet<>()).add(targetIndex);
        dependencies.computeIfAbsent(targetIndex, k -> new HashSet<>()).add(sourceIndex);
    }
    
    /**
     * Check if any already-registered stats have scaling rules referencing the newly registered stat.
     */
    private void resolvePendingDependencies(StatId newStatId, int newIndex) {
        String newFullId = newStatId.fullId();
        for (int i = 0; i < statsByIndex.size(); i++) {
            if (i == newIndex) continue;
            StatDefinition stat = statsByIndex.get(i);
            if (stat.hasScaling()) {
                for (ScalingRule rule : stat.scaling()) {
                    if (rule.source().fullId().equals(newFullId)) {
                        addDependencyEdge(newIndex, i);
                    }
                }
            }
        }
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
     * Builds the topological evaluation order and validates no circular dependencies exist.
     * Should be called after all stats are loaded.
     * @throws IllegalStateException if circular dependencies are detected
     */
    public synchronized void freeze() {
        validateDependencies();
        buildEvaluationOrder();
        this.frozen = true;
        LOGGER.info("StatDefinitionRegistry frozen with " + statsByIndex.size() + " stats, " + 
                tagIndexToStatIndices.size() + " unique tags, and " + categoriesById.size() + " categories");
    }
    
    /**
     * Validate that all scaling rule sources reference registered stats.
     */
    private void validateDependencies() {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < statsByIndex.size(); i++) {
            StatDefinition stat = statsByIndex.get(i);
            if (stat.hasScaling()) {
                for (ScalingRule rule : stat.scaling()) {
                    String sourceFullId = rule.source().fullId();
                    if (!fullIdToIndex.containsKey(sourceFullId)) {
                        errors.add("Stat '" + stat.id().fullId() + "' references unknown source stat: " + sourceFullId);
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid scaling dependencies:\n" + String.join("\n", errors));
        }
    }
    
    /**
     * Build topological evaluation order using Kahn's algorithm (BFS).
     * Stats with no dependencies come first, then stats that depend on them, etc.
     * @throws IllegalStateException if a circular dependency is detected
     */
    private void buildEvaluationOrder() {
        int n = statsByIndex.size();
        if (n == 0) {
            evaluationOrder = new int[0];
            return;
        }
        
        // Compute in-degree for each stat (number of stats it depends on)
        int[] inDegree = new int[n];
        for (int i = 0; i < n; i++) {
            Set<Integer> deps = dependencies.get(i);
            inDegree[i] = deps != null ? deps.size() : 0;
        }
        
        // Queue of stats with no remaining dependencies
        Queue<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                queue.add(i);
            }
        }
        
        // Process in topological order
        int[] result = new int[n];
        int count = 0;
        
        while (!queue.isEmpty()) {
            int current = queue.poll();
            result[count++] = current;
            
            // Reduce in-degree for all stats that depend on this one
            Set<Integer> deps = dependents.get(current);
            if (deps != null) {
                for (int dependent : deps) {
                    inDegree[dependent]--;
                    if (inDegree[dependent] == 0) {
                        queue.add(dependent);
                    }
                }
            }
        }
        
        // Check for cycles
        if (count != n) {
            // Find the cycle for error reporting
            List<String> cycleStats = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (inDegree[i] > 0) {
                    cycleStats.add(statsByIndex.get(i).id().fullId());
                }
            }
            throw new IllegalStateException("Circular dependency detected among stats: " + cycleStats);
        }
        
        evaluationOrder = result;
        LOGGER.fine("Built evaluation order for " + n + " stats");
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
     * Get the Hytale tag index for a tag string.
     * Uses the global {@link AssetRegistry} tag system.
     * 
     * @param tag The tag string
     * @return The tag index, or {@link Integer#MIN_VALUE} if not found
     */
    public int getTagIndex(@Nonnull String tag) {
        return AssetRegistry.getTagIndex(tag);
    }
    
    /**
     * Get or create the Hytale tag index for a tag string.
     * Uses the global {@link AssetRegistry} tag system.
     * 
     * @param tag The tag string
     * @return The tag index (created if not existing)
     */
    public int getOrCreateTagIndex(@Nonnull String tag) {
        return AssetRegistry.getOrCreateTagIndex(tag);
    }
    
    /**
     * Get all stat indices that have a given tag (by tag index).
     * 
     * @param tagIndex The Hytale tag index from {@link AssetRegistry}
     * @return Unmodifiable set of stat indices with this tag
     */
    @Nonnull
    public IntSet getStatIndicesForTagIndex(int tagIndex) {
        IntSet indices = tagIndexToStatIndices.get(tagIndex);
        return indices != null ? IntSets.unmodifiable(indices) : IntSets.EMPTY_SET;
    }
    
    /**
     * Get all stat indices that have a given tag (by tag string).
     * Convenience method that resolves the tag to an index first.
     * 
     * @param tag The tag string
     * @return Set of stat indices with this tag (empty if tag not found)
     */
    @Nonnull
    public Set<Integer> getStatIndicesForTag(@Nonnull String tag) {
        int tagIndex = AssetRegistry.getTagIndex(tag);
        if (tagIndex == Integer.MIN_VALUE) {
            return Collections.emptySet();
        }
        IntSet indices = tagIndexToStatIndices.get(tagIndex);
        if (indices == null || indices.isEmpty()) {
            return Collections.emptySet();
        }
        // Convert IntSet to Set<Integer> for compatibility
        Set<Integer> result = new HashSet<>(indices.size());
        for (int idx : indices) {
            result.add(idx);
        }
        return result;
    }
    
    /**
     * Get all stats that have a given tag.
     */
    @Nonnull
    public Collection<StatDefinition> getStatsForTag(@Nonnull String tag) {
        int tagIndex = AssetRegistry.getTagIndex(tag);
        if (tagIndex == Integer.MIN_VALUE) {
            return Collections.emptyList();
        }
        IntSet indices = tagIndexToStatIndices.get(tagIndex);
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
     * Get all stat IDs that have a given tag.
     */
    @Nonnull
    public List<StatId> getStatIdsForTag(@Nonnull String tag) {
        int tagIndex = AssetRegistry.getTagIndex(tag);
        if (tagIndex == Integer.MIN_VALUE) {
            return Collections.emptyList();
        }
        IntSet indices = tagIndexToStatIndices.get(tagIndex);
        if (indices == null || indices.isEmpty()) {
            return Collections.emptyList();
        }
        List<StatId> statIds = new ArrayList<>(indices.size());
        for (int index : indices) {
            StatDefinition stat = getStat(index);
            if (stat != null) {
                statIds.add(stat.id());
            }
        }
        return statIds;
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
     * Get all unique tags used by stats.
     * <p>
     * Note: This iterates through all stats to collect tags. For performance-critical
     * code, prefer working with tag indices directly via {@link #getTagIndex(String)}.
     */
    @Nonnull
    public Set<String> getAllTags() {
        Set<String> allTags = new HashSet<>();
        for (StatDefinition stat : statsByIndex) {
            allTags.addAll(stat.tags());
        }
        return Collections.unmodifiableSet(allTags);
    }
    
    /**
     * Get the number of unique tag indices registered for stats.
     */
    public int getTagCount() {
        return tagIndexToStatIndices.size();
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
     * <p>
     * This checks for the exact tag string. For hierarchical tags, you can use:
     * <ul>
     *   <li>{@code hasTag("fire")} - matches any stat with "fire" as a tag value</li>
     *   <li>{@code hasTag("Element=fire")} - matches only stats with {@code "Element": ["fire"]}</li>
     *   <li>{@code hasTag("Element")} - matches any stat with an Element category</li>
     * </ul>
     * 
     * @see #hasTagValue(String, String) for a cleaner category-based API
     */
    public boolean hasTag(@Nonnull String tag) {
        int tagIndex = AssetRegistry.getTagIndex(tag);
        return tagIndex != Integer.MIN_VALUE && tagIndexToStatIndices.containsKey(tagIndex);
    }
    
    /**
     * Check if any stat has a specific value in a tag category.
     * <p>
     * For example: {@code hasTagValue("Type", "resistance")} checks if any stat
     * has {@code "Type": ["resistance", ...]} in its tag definition.
     * 
     * @param category The tag category (e.g., "Type", "Element", "Domain")
     * @param value The value to check for within that category
     * @return true if any stat has this category=value combination
     */
    public boolean hasTagValue(@Nonnull String category, @Nonnull String value) {
        return hasTag(category + "=" + value);
    }
    
    /**
     * Get all stats that have a specific value in a tag category.
     * <p>
     * For example: {@code getStatsForTagValue("Type", "resistance")} returns all stats
     * with {@code "Type": ["resistance", ...]} in their tag definition.
     * 
     * @param category The tag category (e.g., "Type", "Element", "Domain")
     * @param value The value to match within that category
     * @return Collection of matching stat definitions
     */
    @Nonnull
    public Collection<StatDefinition> getStatsForTagValue(@Nonnull String category, @Nonnull String value) {
        return getStatsForTag(category + "=" + value);
    }
    
    /**
     * Get all stat indices that have a specific value in a tag category.
     * <p>
     * For example: {@code getStatIndicesForTagValue("Type", "resistance")} returns indices
     * of all stats with {@code "Type": ["resistance", ...]} in their tag definition.
     * 
     * @param category The tag category (e.g., "Type", "Element", "Domain")
     * @param value The value to match within that category
     * @return Set of matching stat indices
     */
    @Nonnull
    public Set<Integer> getStatIndicesForTagValue(@Nonnull String category, @Nonnull String value) {
        return getStatIndicesForTag(category + "=" + value);
    }
    
    /**
     * Get all stat IDs that have a specific value in a tag category.
     * 
     * @param category The tag category (e.g., "Type", "Element", "Domain")
     * @param value The value to match within that category
     * @return List of matching stat IDs
     */
    @Nonnull
    public List<StatId> getStatIdsForTagValue(@Nonnull String category, @Nonnull String value) {
        return getStatIdsForTag(category + "=" + value);
    }
    
    // ========== Dependency Graph Methods ==========
    
    /**
     * Get the set of stat indices that depend on the given stat.
     * These are stats that have scaling rules referencing the source stat.
     * 
     * @param statIndex The source stat index
     * @return Unmodifiable set of dependent stat indices (may be empty)
     */
    @Nonnull
    public Set<Integer> getDependentStats(int statIndex) {
        Set<Integer> deps = dependents.get(statIndex);
        return deps != null ? Collections.unmodifiableSet(deps) : Collections.emptySet();
    }
    
    /**
     * Get the set of stat indices that the given stat depends on.
     * These are the source stats referenced in the stat's scaling rules.
     * 
     * @param statIndex The target stat index
     * @return Unmodifiable set of dependency stat indices (may be empty)
     */
    @Nonnull
    public Set<Integer> getDependencies(int statIndex) {
        Set<Integer> deps = dependencies.get(statIndex);
        return deps != null ? Collections.unmodifiableSet(deps) : Collections.emptySet();
    }
    
    /**
     * Get the topological evaluation order for stats.
     * Stats with no dependencies come first, followed by stats that depend on them.
     * This order ensures that when evaluating a stat, all its source stats have
     * already been computed.
     * 
     * @return Array of stat indices in evaluation order
     * @throws IllegalStateException if registry is not frozen
     */
    @Nonnull
    public int[] getEvaluationOrder() {
        if (!frozen) {
            throw new IllegalStateException("Registry must be frozen before getting evaluation order");
        }
        return evaluationOrder.clone();
    }
    
    /**
     * Check if a stat has scaling rules defined.
     * 
     * @param statIndex The stat index
     * @return true if the stat has scaling rules
     */
    public boolean hasScaling(int statIndex) {
        StatDefinition stat = getStat(statIndex);
        return stat != null && stat.hasScaling();
    }
    
    /**
     * Check if a stat has any dependents (other stats that scale from it).
     * 
     * @param statIndex The stat index
     * @return true if any stats depend on this stat
     */
    public boolean hasDependents(int statIndex) {
        Set<Integer> deps = dependents.get(statIndex);
        return deps != null && !deps.isEmpty();
    }
}
