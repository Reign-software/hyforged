package reign.software.hyforged.passive.registry;

import reign.software.hyforged.passive.asset.PassiveRefundConfigAsset;
import reign.software.hyforged.passive.model.PassiveNode;
import reign.software.hyforged.passive.model.PassiveTree;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central registry for passive tree definitions.
 * <p>
 * Provides lookup for:
 * - General passive tree
 * - Class-specific passive trees
 * - Individual nodes across all trees
 * - Refund cost configuration
 * <p>
 * This is NOT an ECS component - it's a static registry loaded at startup.
 */
public final class PassiveTreeRegistry {

    private static final Logger LOGGER = Logger.getLogger(PassiveTreeRegistry.class.getName());
    private static PassiveTreeRegistry instance;

    /** The general passive tree (only one allowed) */
    private PassiveTree generalTree;

    /** Class trees by class ID */
    private final Map<String, PassiveTree> classTreesByClassId = new ConcurrentHashMap<>();

    /** All trees by tree ID */
    private final Map<String, PassiveTree> treesById = new ConcurrentHashMap<>();

    /** Global node lookup: nodeId -> (treeId, node) */
    private final Map<String, NodeReference> nodeIndex = new ConcurrentHashMap<>();

    /** Refund configuration */
    private PassiveRefundConfigAsset refundConfig;

    private boolean frozen = false;

    private PassiveTreeRegistry() {
    }

    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static synchronized PassiveTreeRegistry get() {
        if (instance == null) {
            instance = new PassiveTreeRegistry();
        }
        return instance;
    }

    /**
     * Reset the registry (for testing or reload).
     */
    public static synchronized void reset() {
        instance = new PassiveTreeRegistry();
    }

    // ========== Registration ==========

    /**
     * Register a passive tree.
     *
     * @param tree The tree to register
     * @throws IllegalStateException if registry is frozen or tree already exists
     */
    public synchronized void register(@Nonnull PassiveTree tree) {
        Objects.requireNonNull(tree, "tree cannot be null");

        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot register new trees");
        }

        String treeId = tree.getId();
        if (treesById.containsKey(treeId)) {
            throw new IllegalStateException("Tree already registered: " + treeId);
        }

        // Register by ID
        treesById.put(treeId, tree);

        // Track tree type
        if (tree.isGeneralTree()) {
            if (generalTree != null) {
                throw new IllegalStateException("Multiple general trees not allowed. " +
                        "Existing: " + generalTree.getId() + ", New: " + treeId);
            }
            generalTree = tree;
            LOGGER.info("Registered general passive tree: " + treeId);
        } else if (tree.isClassTree()) {
            String classId = tree.getClassId();
            if (classId == null || classId.isBlank()) {
                throw new IllegalStateException("Class tree must have a classId: " + treeId);
            }
            if (classTreesByClassId.containsKey(classId)) {
                throw new IllegalStateException("Class tree already registered for class: " + classId);
            }
            classTreesByClassId.put(classId, tree);
            LOGGER.info("Registered class passive tree: " + treeId + " for class: " + classId);
        }

        // Index all nodes
        for (Map.Entry<String, PassiveNode> entry : tree.getNodes().entrySet()) {
            String nodeId = entry.getKey();
            PassiveNode node = entry.getValue();

            if (nodeIndex.containsKey(nodeId)) {
                LOGGER.warning("Duplicate node ID across trees: " + nodeId +
                        " (existing: " + nodeIndex.get(nodeId).treeId() +
                        ", new: " + treeId + ")");
            } else {
                nodeIndex.put(nodeId, new NodeReference(treeId, node));
            }
        }
    }

    /**
     * Set the refund configuration.
     *
     * @param config The refund config
     */
    public synchronized void setRefundConfig(@Nonnull PassiveRefundConfigAsset config) {
        Objects.requireNonNull(config, "config cannot be null");

        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot set refund config");
        }

        if (this.refundConfig != null) {
            LOGGER.warning("Overwriting existing refund config: " + this.refundConfig.getId() +
                    " with: " + config.getId());
        }

        this.refundConfig = config;
    }

    /**
     * Freeze the registry to prevent further modifications.
     */
    public synchronized void freeze() {
        if (frozen) {
            return;
        }

        frozen = true;
        LOGGER.info("PassiveTreeRegistry frozen with " + treesById.size() + " trees, " +
                nodeIndex.size() + " nodes");
    }

    /**
     * Check if the registry is frozen.
     */
    public boolean isFrozen() {
        return frozen;
    }

    // ========== Tree Lookups ==========

    /**
     * Check if a tree with the given ID exists.
     */
    public boolean hasTree(@Nonnull String treeId) {
        return treesById.containsKey(treeId);
    }

    /**
     * Get the general passive tree.
     *
     * @return The general tree, or null if not loaded
     */
    @Nullable
    public PassiveTree getGeneralTree() {
        return generalTree;
    }

    /**
     * Get a passive tree by ID.
     *
     * @param treeId The tree ID
     * @return The tree, or null if not found
     */
    @Nullable
    public PassiveTree getTree(@Nonnull String treeId) {
        return treesById.get(treeId);
    }

    /**
     * Get a class passive tree by class ID.
     *
     * @param classId The class ID (e.g., "hyforged:warrior")
     * @return The class tree, or null if not found
     */
    @Nullable
    public PassiveTree getClassTree(@Nonnull String classId) {
        return classTreesByClassId.get(classId);
    }

    /**
     * Get all registered trees.
     *
     * @return Unmodifiable collection of all trees
     */
    @Nonnull
    public Collection<PassiveTree> getAllTrees() {
        return Collections.unmodifiableCollection(treesById.values());
    }

    /**
     * Get the count of registered trees.
     */
    public int getTreeCount() {
        return treesById.size();
    }

    /**
     * Get all class IDs that have registered trees.
     *
     * @return Unmodifiable set of class IDs
     */
    @Nonnull
    public Set<String> getRegisteredClassIds() {
        return Collections.unmodifiableSet(classTreesByClassId.keySet());
    }

    // ========== Node Lookups ==========

    /**
     * Check if a node with the given ID exists in any tree.
     */
    public boolean hasNode(@Nonnull String nodeId) {
        return nodeIndex.containsKey(nodeId);
    }

    /**
     * Get a node by ID from any tree.
     *
     * @param nodeId The node ID
     * @return The node, or null if not found
     */
    @Nullable
    public PassiveNode getNode(@Nonnull String nodeId) {
        NodeReference ref = nodeIndex.get(nodeId);
        return ref != null ? ref.node() : null;
    }

    /**
     * Get a node reference (node + tree ID) by node ID.
     *
     * @param nodeId The node ID
     * @return The node reference, or null if not found
     */
    @Nullable
    public NodeReference getNodeReference(@Nonnull String nodeId) {
        return nodeIndex.get(nodeId);
    }

    /**
     * Get the tree that contains a specific node.
     *
     * @param nodeId The node ID
     * @return The tree containing the node, or null if not found
     */
    @Nullable
    public PassiveTree getTreeForNode(@Nonnull String nodeId) {
        NodeReference ref = nodeIndex.get(nodeId);
        return ref != null ? treesById.get(ref.treeId()) : null;
    }

    /**
     * Get the count of indexed nodes across all trees.
     */
    public int getNodeCount() {
        return nodeIndex.size();
    }

    // ========== Refund Configuration ==========

    /**
     * Get the refund configuration.
     *
     * @return The refund config, or null if not loaded
     */
    @Nullable
    public PassiveRefundConfigAsset getRefundConfig() {
        return refundConfig;
    }

    /**
     * Calculate the refund cost per node at a given character level.
     *
     * @param characterLevel The character level
     * @return The Tradebar cost per node (uses defaults if config not loaded)
     */
    public int calculateRefundCost(int characterLevel) {
        if (refundConfig == null) {
            // Default: 10 + (level * 2)
            return 10 + (characterLevel * 2);
        }
        return refundConfig.calculateRefundCostPerNode(characterLevel);
    }

    /**
     * Get the maximum Point Book points.
     *
     * @return Maximum book points (uses default if config not loaded)
     */
    public int getMaxBookPoints() {
        if (refundConfig == null) {
            return 20;
        }
        return refundConfig.getMaxBookPoints();
    }

    // ========== Extensibility API ==========

    /**
     * Add a node to an existing tree.
     * <p>
     * This creates a new version of the tree with the node added.
     * Use addConnection() to connect the node to existing nodes.
     *
     * @param treeId The tree ID to modify
     * @param node The node to add
     * @throws IllegalStateException if registry is frozen or tree not found
     * @throws IllegalArgumentException if node ID already exists
     */
    public synchronized void addNode(@Nonnull String treeId, @Nonnull PassiveNode node) {
        Objects.requireNonNull(treeId, "treeId cannot be null");
        Objects.requireNonNull(node, "node cannot be null");

        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot add nodes");
        }

        PassiveTree existingTree = treesById.get(treeId);
        if (existingTree == null) {
            throw new IllegalStateException("Tree not found: " + treeId);
        }

        if (existingTree.getNode(node.id()) != null) {
            throw new IllegalArgumentException("Node already exists in tree: " + node.id());
        }

        // Build a new tree with the node added
        PassiveTree.Builder builder = PassiveTree.builder(treeId)
                .treeType(existingTree.getTreeType())
                .classId(existingTree.getClassId())
                .addStartingNodes(existingTree.getStartingNodeIds())
                .addNodes(existingTree.getNodes().values())
                .addConnections(existingTree.getConnections())
                .version(existingTree.getVersion() + 1)
                .addNode(node);

        PassiveTree newTree = builder.build();

        // Replace in registry
        replaceTree(existingTree, newTree);

        // Index the new node
        nodeIndex.put(node.id(), new NodeReference(treeId, node));

        LOGGER.fine("Added node " + node.id() + " to tree " + treeId);
    }

    /**
     * Add a connection between two nodes in an existing tree.
     *
     * @param treeId The tree ID to modify
     * @param fromNodeId The source node ID
     * @param toNodeId The target node ID
     * @throws IllegalStateException if registry is frozen or tree not found
     * @throws IllegalArgumentException if either node doesn't exist
     */
    public synchronized void addConnection(@Nonnull String treeId, @Nonnull String fromNodeId, @Nonnull String toNodeId) {
        Objects.requireNonNull(treeId, "treeId cannot be null");
        Objects.requireNonNull(fromNodeId, "fromNodeId cannot be null");
        Objects.requireNonNull(toNodeId, "toNodeId cannot be null");

        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot add connections");
        }

        PassiveTree existingTree = treesById.get(treeId);
        if (existingTree == null) {
            throw new IllegalStateException("Tree not found: " + treeId);
        }

        if (existingTree.getNode(fromNodeId) == null) {
            throw new IllegalArgumentException("Source node not found in tree: " + fromNodeId);
        }

        if (existingTree.getNode(toNodeId) == null) {
            throw new IllegalArgumentException("Target node not found in tree: " + toNodeId);
        }

        // Check if connection already exists
        if (existingTree.areAdjacent(fromNodeId, toNodeId)) {
            LOGGER.fine("Connection already exists between " + fromNodeId + " and " + toNodeId);
            return;
        }

        // Build a new tree with the connection added
        PassiveTree.Builder builder = PassiveTree.builder(treeId)
                .treeType(existingTree.getTreeType())
                .classId(existingTree.getClassId())
                .addStartingNodes(existingTree.getStartingNodeIds())
                .addNodes(existingTree.getNodes().values())
                .addConnections(existingTree.getConnections())
                .version(existingTree.getVersion() + 1)
                .addConnection(fromNodeId, toNodeId);

        PassiveTree newTree = builder.build();

        // Replace in registry
        replaceTree(existingTree, newTree);

        LOGGER.fine("Added connection from " + fromNodeId + " to " + toNodeId + " in tree " + treeId);
    }

    /**
     * Replace a tree in all registry mappings.
     * <p>
     * Used when adding nodes, connections, or starting nodes to a tree.
     *
     * @param oldTree The existing tree to replace
     * @param newTree The new tree with updates
     */
    public void replaceTree(@Nonnull PassiveTree oldTree, @Nonnull PassiveTree newTree) {
        String treeId = newTree.getId();

        // Replace in main map
        treesById.put(treeId, newTree);

        // Update general tree reference
        if (newTree.isGeneralTree() && generalTree != null && generalTree.getId().equals(treeId)) {
            generalTree = newTree;
        }

        // Update class tree reference
        if (newTree.isClassTree() && newTree.getClassId() != null) {
            classTreesByClassId.put(newTree.getClassId(), newTree);
        }
    }

    // ========== Node Reference ==========

    /**
     * Reference to a node and its containing tree.
     */
    public record NodeReference(
            @Nonnull String treeId,
            @Nonnull PassiveNode node
    ) {
        public NodeReference {
            Objects.requireNonNull(treeId, "treeId cannot be null");
            Objects.requireNonNull(node, "node cannot be null");
        }
    }
}
