package reign.software.hyforged.passive.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a passive tree containing nodes and connections.
 * <p>
 * Trees can be either General (shared by all characters) or Class (per-class).
 */
public class PassiveTree {
    
    private final String id;
    private final String treeType;
    private final String classId; // Only for class trees
    private final Set<String> startingNodeIds;
    private final Map<String, PassiveNode> nodes;
    private final List<PassiveConnection> connections;
    private final List<TextLabel> textLabels;
    private final int version;
    
    // Adjacency list for efficient graph traversal
    private final Map<String, Set<String>> adjacencyList;
    
    /**
     * Create a new PassiveTree.
     *
     * @param id Unique identifier for the tree
     * @param treeType Tree type (general or class)
     * @param classId Class ID for class trees (null for general)
     * @param startingNodeIds IDs of starting nodes
     * @param nodes Map of node ID to node
     * @param connections List of connections between nodes
     * @param textLabels List of text labels to display on the tree
     * @param version Schema version for migration
     */
    public PassiveTree(
        @Nonnull String id,
        @Nonnull String treeType,
        @Nullable String classId,
        @Nonnull Collection<String> startingNodeIds,
        @Nonnull Map<String, PassiveNode> nodes,
        @Nonnull List<PassiveConnection> connections,
        @Nonnull List<TextLabel> textLabels,
        int version
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.treeType = Objects.requireNonNull(treeType, "treeType cannot be null");
        this.classId = classId;
        this.startingNodeIds = Set.copyOf(startingNodeIds);
        this.nodes = Map.copyOf(nodes);
        this.connections = List.copyOf(connections);
        this.textLabels = List.copyOf(textLabels);
        this.version = version;
        
        // Build adjacency list
        this.adjacencyList = buildAdjacencyList();
    }
    
    private Map<String, Set<String>> buildAdjacencyList() {
        Map<String, Set<String>> adj = new HashMap<>();
        
        // Initialize all nodes with empty sets
        for (String nodeId : nodes.keySet()) {
            adj.put(nodeId, new HashSet<>());
        }
        
        // Add edges (undirected)
        for (PassiveConnection conn : connections) {
            adj.computeIfAbsent(conn.from(), k -> new HashSet<>()).add(conn.to());
            adj.computeIfAbsent(conn.to(), k -> new HashSet<>()).add(conn.from());
        }
        
        return adj;
    }
    
    // ========== ACCESSORS ==========
    
    @Nonnull
    public String getId() {
        return id;
    }
    
    @Nonnull
    public String getTreeType() {
        return treeType;
    }
    
    @Nullable
    public String getClassId() {
        return classId;
    }
    
    public boolean isGeneralTree() {
        return PassiveTreeType.GENERAL.equalsIgnoreCase(treeType);
    }
    
    public boolean isClassTree() {
        return PassiveTreeType.CLASS.equalsIgnoreCase(treeType);
    }
    
    @Nonnull
    public Set<String> getStartingNodeIds() {
        return startingNodeIds;
    }
    
    /**
     * Check if a node ID is a starting node.
     */
    public boolean isStartingNode(@Nonnull String nodeId) {
        return startingNodeIds.contains(nodeId);
    }
    
    @Nullable
    public PassiveNode getNode(@Nonnull String nodeId) {
        return nodes.get(nodeId);
    }
    
    @Nonnull
    public Map<String, PassiveNode> getNodes() {
        return nodes;
    }
    
    public int getNodeCount() {
        return nodes.size();
    }
    
    @Nonnull
    public List<PassiveConnection> getConnections() {
        return connections;
    }
    
    /**
     * Get all text labels in the tree.
     */
    @Nonnull
    public List<TextLabel> getTextLabels() {
        return textLabels;
    }
    
    public int getVersion() {
        return version;
    }
    
    // ========== GRAPH OPERATIONS ==========
    
    /**
     * Get all nodes adjacent to a given node.
     *
     * @param nodeId The node ID
     * @return Set of adjacent node IDs (empty if node not found)
     */
    @Nonnull
    public Set<String> getAdjacentNodes(@Nonnull String nodeId) {
        Set<String> adjacent = adjacencyList.get(nodeId);
        return adjacent != null ? Collections.unmodifiableSet(adjacent) : Collections.emptySet();
    }
    
    /**
     * Check if two nodes are adjacent (connected directly).
     *
     * @param nodeId1 First node
     * @param nodeId2 Second node
     * @return true if adjacent
     */
    public boolean areAdjacent(@Nonnull String nodeId1, @Nonnull String nodeId2) {
        Set<String> adjacent = adjacencyList.get(nodeId1);
        return adjacent != null && adjacent.contains(nodeId2);
    }
    
    /**
     * Get all nodes in a specific region.
     *
     * @param region The region name
     * @return List of nodes in the region
     */
    @Nonnull
    public List<PassiveNode> getNodesInRegion(@Nonnull String region) {
        return nodes.values().stream()
            .filter(n -> region.equals(n.region()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all regions in the tree.
     *
     * @return Set of region names
     */
    @Nonnull
    public Set<String> getRegions() {
        return nodes.values().stream()
            .map(PassiveNode::region)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
    
    /**
     * Get all keystones in the tree.
     *
     * @return List of keystone nodes
     */
    @Nonnull
    public List<PassiveNode> getKeystones() {
        return nodes.values().stream()
            .filter(PassiveNode::isKeystone)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all nodes of a specific type.
     *
     * @param nodeType The node type
     * @return List of matching nodes
     */
    @Nonnull
    public List<PassiveNode> getNodesByType(@Nonnull String nodeType) {
        return nodes.values().stream()
            .filter(n -> nodeType.equalsIgnoreCase(n.type()))
            .collect(Collectors.toList());
    }
    
    /**
     * Create a builder for constructing PassiveTree instances.
     *
     * @param id The tree ID
     * @return A new builder
     */
    public static Builder builder(@Nonnull String id) {
        return new Builder(id);
    }
    
    /**
     * Builder for PassiveTree.
     */
    public static class Builder {
        private final String id;
        private String treeType = PassiveTreeType.GENERAL;
        private String classId = null;
        private final Set<String> startingNodeIds = new HashSet<>();
        private final Map<String, PassiveNode> nodes = new HashMap<>();
        private final List<PassiveConnection> connections = new ArrayList<>();
        private final List<TextLabel> textLabels = new ArrayList<>();
        private int version = 1;
        
        private Builder(@Nonnull String id) {
            this.id = Objects.requireNonNull(id, "id cannot be null");
        }
        
        public Builder treeType(@Nonnull String treeType) {
            this.treeType = treeType;
            return this;
        }
        
        public Builder classId(@Nullable String classId) {
            this.classId = classId;
            return this;
        }
        
        public Builder addStartingNode(@Nonnull String nodeId) {
            this.startingNodeIds.add(nodeId);
            return this;
        }
        
        public Builder addStartingNodes(@Nonnull Collection<String> nodeIds) {
            this.startingNodeIds.addAll(nodeIds);
            return this;
        }
        
        public Builder addNode(@Nonnull PassiveNode node) {
            this.nodes.put(node.id(), node);
            return this;
        }
        
        public Builder addNodes(@Nonnull Collection<PassiveNode> nodes) {
            for (PassiveNode node : nodes) {
                this.nodes.put(node.id(), node);
            }
            return this;
        }
        
        public Builder addConnection(@Nonnull String from, @Nonnull String to) {
            this.connections.add(new PassiveConnection(from, to));
            return this;
        }
        
        public Builder addConnection(@Nonnull PassiveConnection connection) {
            this.connections.add(connection);
            return this;
        }
        
        public Builder addConnections(@Nonnull Collection<PassiveConnection> connections) {
            this.connections.addAll(connections);
            return this;
        }
        
        public Builder addTextLabel(@Nonnull TextLabel label) {
            this.textLabels.add(label);
            return this;
        }
        
        public Builder addTextLabels(@Nonnull Collection<TextLabel> labels) {
            this.textLabels.addAll(labels);
            return this;
        }
        
        public Builder version(int version) {
            this.version = version;
            return this;
        }
        
        public PassiveTree build() {
            return new PassiveTree(id, treeType, classId, startingNodeIds, nodes, connections, textLabels, version);
        }
    }
}
