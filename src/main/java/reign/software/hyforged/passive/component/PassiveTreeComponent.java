package reign.software.hyforged.passive.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * ECS Component holding passive tree allocation data for a player entity.
 * <p>
 * This is PURE DATA - no behavior, following ECS principles.
 * All computation is done by Systems that process this component.
 * <p>
 * Data stored:
 * - General tree starting node (chosen region)
 * - General tree allocated nodes
 * - Point Book points used
 * - Class tree allocated nodes (per class)
 * - Schema version for persistence migration
 */
public class PassiveTreeComponent implements Component<EntityStore> {

    /** Schema version for persistence migration */
    public static final int SCHEMA_VERSION = 1;

    // ========== GENERAL TREE ALLOCATION ==========

    /** The starting node chosen by the player (determines starting region) */
    private String generalStartingNode = null;

    /** Set of allocated node IDs in the general tree */
    private final Set<String> generalAllocatedNodes = new HashSet<>();

    /** Number of Point Book points used (adds to available general points) */
    private int bookPointsUsed = 0;

    // ========== CLASS TREE ALLOCATIONS ==========

    /** Maps class ID to set of allocated node IDs for that class tree */
    private final Map<String, Set<String>> classAllocatedNodes = new HashMap<>();

    // ========== MASTERY CHOICES ==========

    /** Maps mastery node ID to chosen option ID */
    private final Map<String, String> masteryChoices = new HashMap<>();

    /** Set of mastery node IDs that are pending choice (recently allocated, awaiting UI selection) */
    private final Set<String> pendingMasteryChoices = new HashSet<>();

    // ========== TREE VERSION TRACKING ==========

    /** Maps tree ID to the version of the tree when allocations were made */
    private final Map<String, Integer> treeVersions = new HashMap<>();

    // ========== PERSISTENCE ==========

    private int schemaVersion = SCHEMA_VERSION;
    private boolean dirty = false;

    public PassiveTreeComponent() {
        // Required for codec
    }

    /**
     * Copy constructor for clone().
     */
    public PassiveTreeComponent(PassiveTreeComponent other) {
        this.generalStartingNode = other.generalStartingNode;
        this.generalAllocatedNodes.addAll(other.generalAllocatedNodes);
        this.bookPointsUsed = other.bookPointsUsed;
        
        for (Map.Entry<String, Set<String>> entry : other.classAllocatedNodes.entrySet()) {
            this.classAllocatedNodes.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        
        this.masteryChoices.putAll(other.masteryChoices);
        this.pendingMasteryChoices.addAll(other.pendingMasteryChoices);
        this.treeVersions.putAll(other.treeVersions);
        
        this.schemaVersion = other.schemaVersion;
        this.dirty = other.dirty;
    }

    @Override
    public PassiveTreeComponent clone() {
        return new PassiveTreeComponent(this);
    }

    // ========== GENERAL TREE ACCESSORS ==========

    /**
     * Get the starting node ID for the general tree.
     * Returns null if no starting node has been chosen yet.
     */
    @Nullable
    public String getGeneralStartingNode() {
        return generalStartingNode;
    }

    /**
     * Set the starting node for the general tree.
     * This determines the player's starting region.
     */
    public void setGeneralStartingNode(@Nullable String nodeId) {
        if (!Objects.equals(this.generalStartingNode, nodeId)) {
            this.generalStartingNode = nodeId;
            this.dirty = true;
        }
    }

    /**
     * Check if a starting node has been chosen.
     */
    public boolean hasChosenStartingNode() {
        return generalStartingNode != null && !generalStartingNode.isBlank();
    }

    /**
     * Get all allocated node IDs in the general tree.
     */
    @Nonnull
    public Set<String> getGeneralAllocatedNodes() {
        return Collections.unmodifiableSet(generalAllocatedNodes);
    }

    /**
     * Get the count of allocated nodes in the general tree.
     */
    public int getGeneralAllocatedCount() {
        return generalAllocatedNodes.size();
    }

    /**
     * Check if a node is allocated in the general tree.
     */
    public boolean isGeneralNodeAllocated(@Nonnull String nodeId) {
        return generalAllocatedNodes.contains(nodeId);
    }

    /**
     * Allocate a node in the general tree.
     *
     * @param nodeId The node ID to allocate
     * @return true if the node was newly allocated, false if already allocated
     */
    public boolean allocateGeneralNode(@Nonnull String nodeId) {
        if (generalAllocatedNodes.add(nodeId)) {
            dirty = true;
            return true;
        }
        return false;
    }

    /**
     * Deallocate a node from the general tree.
     *
     * @param nodeId The node ID to deallocate
     * @return true if the node was deallocated, false if not allocated
     */
    public boolean deallocateGeneralNode(@Nonnull String nodeId) {
        if (generalAllocatedNodes.remove(nodeId)) {
            dirty = true;
            return true;
        }
        return false;
    }

    /**
     * Clear all general tree allocations.
     */
    public void clearGeneralAllocations() {
        if (!generalAllocatedNodes.isEmpty()) {
            generalAllocatedNodes.clear();
            dirty = true;
        }
    }

    // ========== POINT BOOK ACCESSORS ==========

    /**
     * Get the number of Point Book points used.
     */
    public int getBookPointsUsed() {
        return bookPointsUsed;
    }

    /**
     * Set the number of Point Book points used.
     */
    public void setBookPointsUsed(int points) {
        if (points < 0) {
            points = 0;
        }
        if (this.bookPointsUsed != points) {
            this.bookPointsUsed = points;
            this.dirty = true;
        }
    }

    /**
     * Add a Point Book point.
     *
     * @return The new total
     */
    public int addBookPoint() {
        bookPointsUsed++;
        dirty = true;
        return bookPointsUsed;
    }

    // ========== CLASS TREE ACCESSORS ==========

    /**
     * Get all class IDs with allocations.
     */
    @Nonnull
    public Set<String> getClassIdsWithAllocations() {
        return Collections.unmodifiableSet(classAllocatedNodes.keySet());
    }

    /**
     * Get allocated node IDs for a specific class tree.
     *
     * @param classId The class ID
     * @return Set of allocated node IDs (empty if none)
     */
    @Nonnull
    public Set<String> getClassAllocatedNodes(@Nonnull String classId) {
        Set<String> nodes = classAllocatedNodes.get(classId);
        return nodes != null ? Collections.unmodifiableSet(nodes) : Collections.emptySet();
    }

    /**
     * Get the count of allocated nodes for a specific class tree.
     */
    public int getClassAllocatedCount(@Nonnull String classId) {
        Set<String> nodes = classAllocatedNodes.get(classId);
        return nodes != null ? nodes.size() : 0;
    }

    /**
     * Check if a node is allocated in a class tree.
     */
    public boolean isClassNodeAllocated(@Nonnull String classId, @Nonnull String nodeId) {
        Set<String> nodes = classAllocatedNodes.get(classId);
        return nodes != null && nodes.contains(nodeId);
    }

    /**
     * Allocate a node in a class tree.
     *
     * @param classId The class ID
     * @param nodeId The node ID to allocate
     * @return true if the node was newly allocated, false if already allocated
     */
    public boolean allocateClassNode(@Nonnull String classId, @Nonnull String nodeId) {
        Set<String> nodes = classAllocatedNodes.computeIfAbsent(classId, k -> new HashSet<>());
        if (nodes.add(nodeId)) {
            dirty = true;
            return true;
        }
        return false;
    }

    /**
     * Deallocate a node from a class tree.
     *
     * @param classId The class ID
     * @param nodeId The node ID to deallocate
     * @return true if the node was deallocated, false if not allocated
     */
    public boolean deallocateClassNode(@Nonnull String classId, @Nonnull String nodeId) {
        Set<String> nodes = classAllocatedNodes.get(classId);
        if (nodes != null && nodes.remove(nodeId)) {
            if (nodes.isEmpty()) {
                classAllocatedNodes.remove(classId);
            }
            dirty = true;
            return true;
        }
        return false;
    }

    /**
     * Clear all allocations for a specific class tree.
     */
    public void clearClassAllocations(@Nonnull String classId) {
        if (classAllocatedNodes.remove(classId) != null) {
            dirty = true;
        }
    }

    // ========== MASTERY CHOICE ACCESSORS ==========

    /**
     * Get the chosen option ID for a mastery node.
     *
     * @param nodeId The mastery node ID
     * @return The chosen option ID, or null if no choice has been made
     */
    @Nullable
    public String getMasteryChoice(@Nonnull String nodeId) {
        return masteryChoices.get(nodeId);
    }

    /**
     * Check if a mastery node has a choice made.
     */
    public boolean hasMasteryChoice(@Nonnull String nodeId) {
        return masteryChoices.containsKey(nodeId);
    }

    /**
     * Set the chosen option for a mastery node.
     *
     * @param nodeId The mastery node ID
     * @param optionId The chosen option ID
     */
    public void setMasteryChoice(@Nonnull String nodeId, @Nonnull String optionId) {
        masteryChoices.put(nodeId, optionId);
        pendingMasteryChoices.remove(nodeId);
        dirty = true;
    }

    /**
     * Clear the mastery choice for a node (used on deallocation).
     */
    public void clearMasteryChoice(@Nonnull String nodeId) {
        if (masteryChoices.remove(nodeId) != null || pendingMasteryChoices.remove(nodeId)) {
            dirty = true;
        }
    }

    /**
     * Mark a mastery node as pending choice (awaiting UI selection).
     */
    public void markMasteryPending(@Nonnull String nodeId) {
        if (pendingMasteryChoices.add(nodeId)) {
            dirty = true;
        }
    }

    /**
     * Check if a mastery node is pending choice.
     */
    public boolean isMasteryPending(@Nonnull String nodeId) {
        return pendingMasteryChoices.contains(nodeId);
    }

    /**
     * Get all pending mastery choices.
     */
    @Nonnull
    public Set<String> getPendingMasteryChoices() {
        return Collections.unmodifiableSet(pendingMasteryChoices);
    }

    /**
     * Get all mastery choices made.
     */
    @Nonnull
    public Map<String, String> getAllMasteryChoices() {
        return Collections.unmodifiableMap(masteryChoices);
    }

    // ========== TREE VERSION TRACKING ==========

    /**
     * Get the stored version for a tree.
     *
     * @param treeId The tree ID
     * @return The version, or 0 if not tracked
     */
    public int getTreeVersion(@Nonnull String treeId) {
        return treeVersions.getOrDefault(treeId, 0);
    }

    /**
     * Set the stored version for a tree.
     *
     * @param treeId The tree ID
     * @param version The version to store
     */
    public void setTreeVersion(@Nonnull String treeId, int version) {
        if (treeVersions.getOrDefault(treeId, 0) != version) {
            treeVersions.put(treeId, version);
            markDirty();
        }
    }

    /**
     * Check if a tree version is stored.
     *
     * @param treeId The tree ID
     * @return true if version is tracked
     */
    public boolean hasTreeVersion(@Nonnull String treeId) {
        return treeVersions.containsKey(treeId);
    }

    /**
     * Get all stored tree versions.
     */
    @Nonnull
    public Map<String, Integer> getAllTreeVersions() {
        return Collections.unmodifiableMap(treeVersions);
    }

    // ========== DIRTY FLAG ==========

    /**
     * Check if this component has been modified since last save.
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Mark this component as clean (after saving).
     */
    public void clearDirty() {
        dirty = false;
    }

    /**
     * Mark this component as dirty (needs saving).
     */
    public void markDirty() {
        dirty = true;
    }

    // ========== SCHEMA VERSION ==========

    /**
     * Get the schema version for migration support.
     */
    public int getSchemaVersion() {
        return schemaVersion;
    }

    /**
     * Set the schema version (used during deserialization).
     */
    public void setSchemaVersion(int version) {
        this.schemaVersion = version;
    }

    // ========== TEMP LOAD HELPERS (for codec deserialization) ==========

    private transient String[] tempLoadClassIds;
    private transient int[] tempLoadClassNodeCounts;
    private transient String[] tempLoadMasteryNodeIds;
    private transient String[] tempLoadTreeIds;

    public void setTempLoadClassIds(String[] classIds) {
        this.tempLoadClassIds = classIds;
    }

    public String[] getTempLoadClassIds() {
        return tempLoadClassIds;
    }

    public void clearTempLoadClassIds() {
        this.tempLoadClassIds = null;
    }

    public void setTempLoadClassNodeCounts(int[] counts) {
        this.tempLoadClassNodeCounts = counts;
    }

    public int[] getTempLoadClassNodeCounts() {
        return tempLoadClassNodeCounts;
    }

    public void clearTempLoadClassNodeCounts() {
        this.tempLoadClassNodeCounts = null;
    }

    public void setTempLoadMasteryNodeIds(String[] nodeIds) {
        this.tempLoadMasteryNodeIds = nodeIds;
    }

    public String[] getTempLoadMasteryNodeIds() {
        return tempLoadMasteryNodeIds;
    }

    public void clearTempLoadMasteryNodeIds() {
        this.tempLoadMasteryNodeIds = null;
    }

    public void setTempLoadTreeIds(String[] treeIds) {
        this.tempLoadTreeIds = treeIds;
    }

    public String[] getTempLoadTreeIds() {
        return tempLoadTreeIds;
    }

    public void clearTempLoadTreeIds() {
        this.tempLoadTreeIds = null;
    }
}
