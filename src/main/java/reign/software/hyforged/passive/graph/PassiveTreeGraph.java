package reign.software.hyforged.passive.graph;

import reign.software.hyforged.passive.model.PassiveTree;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Graph algorithms for passive tree operations.
 * <p>
 * Provides path finding, connectivity validation, and orphan detection
 * for passive tree allocation and refund operations.
 * <p>
 * Performance requirement: All operations should complete in < 5ms (NFR-1).
 */
public final class PassiveTreeGraph {

    private PassiveTreeGraph() {
        // Utility class
    }

    /**
     * Find the shortest path from any allocated node to a target node.
     * Uses BFS for unweighted shortest path.
     *
     * @param tree The passive tree
     * @param allocatedNodes Currently allocated node IDs
     * @param targetNodeId The target node ID
     * @return List of node IDs representing the path (empty if no path exists)
     */
    @Nonnull
    public static List<String> findShortestPath(
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull String targetNodeId
    ) {
        // If target is already allocated, return empty path
        if (allocatedNodes.contains(targetNodeId)) {
            return List.of(targetNodeId);
        }

        // If no allocated nodes, check if target is a starting node
        if (allocatedNodes.isEmpty()) {
            if (tree.isStartingNode(targetNodeId)) {
                return List.of(targetNodeId);
            }
            return Collections.emptyList();
        }

        // BFS from all allocated nodes
        Queue<String> queue = new LinkedList<>(allocatedNodes);
        Map<String, String> parent = new HashMap<>();
        Set<String> visited = new HashSet<>(allocatedNodes);
        
        // Mark allocated nodes as already visited
        for (String node : allocatedNodes) {
            parent.put(node, null);
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            
            if (current.equals(targetNodeId)) {
                // Reconstruct path
                return reconstructPath(parent, targetNodeId, allocatedNodes);
            }

            for (String neighbor : tree.getAdjacentNodes(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                    queue.offer(neighbor);
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    /**
     * Reconstruct path from BFS parent map.
     */
    private static List<String> reconstructPath(
            Map<String, String> parent,
            String target,
            Set<String> allocatedNodes
    ) {
        List<String> path = new ArrayList<>();
        String current = target;
        
        while (current != null && !allocatedNodes.contains(current)) {
            path.add(current);
            current = parent.get(current);
        }
        
        Collections.reverse(path);
        return path;
    }

    /**
     * Check if a set of nodes maintains connectivity to a starting node.
     *
     * @param tree The passive tree
     * @param allocatedNodes The set of allocated node IDs
     * @param startingNodeId The starting node ID
     * @return true if all allocated nodes are connected to the starting node
     */
    public static boolean isConnectedToStart(
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull String startingNodeId
    ) {
        if (allocatedNodes.isEmpty()) {
            return true;
        }

        if (!allocatedNodes.contains(startingNodeId)) {
            return false;
        }

        // BFS from starting node
        Set<String> reachable = getReachableFromStart(tree, allocatedNodes, startingNodeId);
        return reachable.equals(allocatedNodes);
    }

    /**
     * Get all allocated nodes reachable from the starting node.
     */
    @Nonnull
    public static Set<String> getReachableFromStart(
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull String startingNodeId
    ) {
        if (!allocatedNodes.contains(startingNodeId)) {
            return Collections.emptySet();
        }

        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(startingNodeId);
        reachable.add(startingNodeId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            
            for (String neighbor : tree.getAdjacentNodes(current)) {
                if (allocatedNodes.contains(neighbor) && !reachable.contains(neighbor)) {
                    reachable.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }

        return reachable;
    }

    /**
     * Get nodes that would become orphaned if a node is removed.
     * Orphaned nodes are allocated nodes no longer connected to the starting node.
     *
     * @param tree The passive tree
     * @param allocatedNodes Currently allocated node IDs
     * @param startingNodeId The starting node ID
     * @param nodeToRemove The node being considered for removal
     * @return Set of node IDs that would become orphaned (includes the removed node)
     */
    @Nonnull
    public static Set<String> getOrphanedNodes(
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull String startingNodeId,
            @Nonnull String nodeToRemove
    ) {
        // Cannot remove starting node if there are other allocations
        if (nodeToRemove.equals(startingNodeId)) {
            if (allocatedNodes.size() > 1) {
                return new HashSet<>(allocatedNodes); // All nodes would be orphaned
            }
            return Set.of(startingNodeId);
        }

        // Simulate removal
        Set<String> remaining = new HashSet<>(allocatedNodes);
        remaining.remove(nodeToRemove);

        // Find what's still reachable
        Set<String> reachable = getReachableFromStart(tree, remaining, startingNodeId);

        // Orphaned = allocated - reachable (plus the removed node)
        Set<String> orphaned = new HashSet<>(allocatedNodes);
        orphaned.removeAll(reachable);
        
        return orphaned;
    }

    /**
     * Get all nodes reachable from currently allocated nodes (for UI highlighting).
     * This includes nodes that could be allocated next.
     *
     * @param tree The passive tree
     * @param allocatedNodes Currently allocated node IDs
     * @return Set of node IDs that are adjacent to allocated nodes but not yet allocated
     */
    @Nonnull
    public static Set<String> getReachableUnallocatedNodes(
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes
    ) {
        Set<String> reachable = new HashSet<>();

        if (allocatedNodes.isEmpty()) {
            // If no allocations, only starting nodes are reachable
            reachable.addAll(tree.getStartingNodeIds());
            return reachable;
        }

        for (String allocated : allocatedNodes) {
            for (String neighbor : tree.getAdjacentNodes(allocated)) {
                if (!allocatedNodes.contains(neighbor)) {
                    reachable.add(neighbor);
                }
            }
        }

        return reachable;
    }

    /**
     * Check if a node can be allocated (is adjacent to allocated nodes or is a starting node).
     *
     * @param tree The passive tree
     * @param allocatedNodes Currently allocated node IDs
     * @param nodeId The node to check
     * @return true if the node can be allocated
     */
    public static boolean canAllocateNode(
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull String nodeId
    ) {
        // Already allocated
        if (allocatedNodes.contains(nodeId)) {
            return false;
        }

        // No allocations yet - only starting nodes allowed
        if (allocatedNodes.isEmpty()) {
            return tree.isStartingNode(nodeId);
        }

        // Check adjacency to any allocated node
        for (String allocated : allocatedNodes) {
            if (tree.getAdjacentNodes(allocated).contains(nodeId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a node can be safely deallocated without orphaning other nodes.
     *
     * @param tree The passive tree
     * @param allocatedNodes Currently allocated node IDs
     * @param startingNodeId The starting node ID
     * @param nodeId The node to check
     * @return true if the node can be deallocated without orphaning others
     */
    public static boolean canDeallocateNode(
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull String startingNodeId,
            @Nonnull String nodeId
    ) {
        if (!allocatedNodes.contains(nodeId)) {
            return false;
        }

        // Starting node can only be deallocated if it's the only allocation
        if (nodeId.equals(startingNodeId)) {
            return allocatedNodes.size() == 1;
        }

        // Check if removal would orphan any nodes
        Set<String> orphaned = getOrphanedNodes(tree, allocatedNodes, startingNodeId, nodeId);
        return orphaned.size() == 1 && orphaned.contains(nodeId);
    }

    /**
     * Get the allocation order for a path (for auto-path allocation).
     * Returns nodes in the order they should be allocated.
     *
     * @param tree The passive tree
     * @param allocatedNodes Currently allocated node IDs
     * @param path The path to allocate
     * @return List of nodes to allocate in order (may be empty if path invalid)
     */
    @Nonnull
    public static List<String> getPathAllocationOrder(
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull List<String> path
    ) {
        if (path.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> order = new ArrayList<>();
        Set<String> simulated = new HashSet<>(allocatedNodes);

        for (String nodeId : path) {
            if (simulated.contains(nodeId)) {
                continue; // Already allocated
            }

            if (canAllocateNode(tree, simulated, nodeId)) {
                order.add(nodeId);
                simulated.add(nodeId);
            } else {
                // Path is invalid
                return Collections.emptyList();
            }
        }

        return order;
    }
}
