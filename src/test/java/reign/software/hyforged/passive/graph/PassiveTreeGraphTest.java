package reign.software.hyforged.passive.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.passive.model.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PassiveTreeGraph algorithms.
 */
@DisplayName("PassiveTreeGraph Tests")
class PassiveTreeGraphTest {

    private PassiveTree tree;
    private static final String START = "start";
    private static final String NODE1 = "node1";
    private static final String NODE2 = "node2";
    private static final String NODE3 = "node3";
    private static final String NODE4 = "node4";
    private static final String KEYSTONE = "keystone";
    
    @BeforeEach
    void setUp() {
        // Create a test tree with this structure:
        //
        //        start
        //          |
        //        node1
        //       /     \
        //    node2   node3
        //              |
        //           node4
        //              |
        //          keystone
        
        PassiveNode startNode = PassiveNode.builder(START)
                .type(PassiveNodeType.MINOR)
                .name("Start")
                .description("Starting point")
                .position(0, 0)
                .build();
        
        PassiveNode node1 = PassiveNode.builder(NODE1)
                .type(PassiveNodeType.MINOR)
                .name("Node 1")
                .description("First branch")
                .position(0, -20)
                .build();
        
        PassiveNode node2 = PassiveNode.builder(NODE2)
                .type(PassiveNodeType.NOTABLE)
                .name("Node 2")
                .description("Left branch")
                .position(-20, -40)
                .build();
        
        PassiveNode node3 = PassiveNode.builder(NODE3)
                .type(PassiveNodeType.MINOR)
                .name("Node 3")
                .description("Right branch")
                .position(20, -40)
                .build();
        
        PassiveNode node4 = PassiveNode.builder(NODE4)
                .type(PassiveNodeType.MINOR)
                .name("Node 4")
                .description("Deeper node")
                .position(20, -60)
                .build();
        
        PassiveNode keystoneNode = PassiveNode.builder(KEYSTONE)
                .type(PassiveNodeType.KEYSTONE)
                .name("Keystone")
                .description("Powerful effect")
                .position(20, -80)
                .keystoneFamily("test-family")
                .build();
        
        List<PassiveConnection> connections = List.of(
                new PassiveConnection(START, NODE1),
                new PassiveConnection(NODE1, NODE2),
                new PassiveConnection(NODE1, NODE3),
                new PassiveConnection(NODE3, NODE4),
                new PassiveConnection(NODE4, KEYSTONE)
        );
        
        tree = new PassiveTree(
                "hyforged:test-tree",
                PassiveTreeType.GENERAL,
                null,
                Set.of(START),
                Map.of(
                        START, startNode,
                        NODE1, node1,
                        NODE2, node2,
                        NODE3, node3,
                        NODE4, node4,
                        KEYSTONE, keystoneNode
                ),
                connections,
                1
        );
    }

    // ========== Path Finding Tests ==========

    @Nested
    @DisplayName("findShortestPath")
    class FindShortestPathTests {

        @Test
        @DisplayName("returns empty list for already allocated target")
        void alreadyAllocated() {
            Set<String> allocated = Set.of(START, NODE1);
            List<String> path = PassiveTreeGraph.findShortestPath(tree, allocated, NODE1);
            assertEquals(List.of(NODE1), path);
        }

        @Test
        @DisplayName("returns single node path for adjacent target")
        void adjacentTarget() {
            Set<String> allocated = Set.of(START);
            List<String> path = PassiveTreeGraph.findShortestPath(tree, allocated, NODE1);
            assertEquals(List.of(NODE1), path);
        }

        @Test
        @DisplayName("returns multi-node path for distant target")
        void distantTarget() {
            Set<String> allocated = Set.of(START);
            List<String> path = PassiveTreeGraph.findShortestPath(tree, allocated, KEYSTONE);
            // Path should be: node1 -> node3 -> node4 -> keystone
            assertEquals(4, path.size());
            assertEquals(NODE1, path.get(0));
            assertEquals(KEYSTONE, path.get(path.size() - 1));
        }

        @Test
        @DisplayName("returns starting node when no allocations and target is start")
        void emptyAllocationsStartingNode() {
            Set<String> allocated = Collections.emptySet();
            List<String> path = PassiveTreeGraph.findShortestPath(tree, allocated, START);
            assertEquals(List.of(START), path);
        }

        @Test
        @DisplayName("returns empty for non-start target with no allocations")
        void emptyAllocationsNonStart() {
            Set<String> allocated = Collections.emptySet();
            List<String> path = PassiveTreeGraph.findShortestPath(tree, allocated, NODE1);
            assertTrue(path.isEmpty());
        }
    }

    // ========== Connectivity Tests ==========

    @Nested
    @DisplayName("isConnectedToStart")
    class IsConnectedToStartTests {

        @Test
        @DisplayName("empty allocations are connected")
        void emptyAllocations() {
            assertTrue(PassiveTreeGraph.isConnectedToStart(tree, Collections.emptySet(), START));
        }

        @Test
        @DisplayName("single start allocation is connected")
        void singleStart() {
            assertTrue(PassiveTreeGraph.isConnectedToStart(tree, Set.of(START), START));
        }

        @Test
        @DisplayName("connected chain is valid")
        void connectedChain() {
            Set<String> allocated = Set.of(START, NODE1, NODE3, NODE4);
            assertTrue(PassiveTreeGraph.isConnectedToStart(tree, allocated, START));
        }

        @Test
        @DisplayName("disconnected allocation is invalid")
        void disconnectedAllocation() {
            // NODE4 without NODE3 connecting it
            Set<String> allocated = Set.of(START, NODE1, NODE4);
            assertFalse(PassiveTreeGraph.isConnectedToStart(tree, allocated, START));
        }

        @Test
        @DisplayName("missing start is invalid")
        void missingStart() {
            Set<String> allocated = Set.of(NODE1, NODE3);
            assertFalse(PassiveTreeGraph.isConnectedToStart(tree, allocated, START));
        }
    }

    // ========== Reachable From Start Tests ==========

    @Nested
    @DisplayName("getReachableFromStart")
    class GetReachableFromStartTests {

        @Test
        @DisplayName("returns all connected allocated nodes")
        void allConnected() {
            Set<String> allocated = Set.of(START, NODE1, NODE2, NODE3);
            Set<String> reachable = PassiveTreeGraph.getReachableFromStart(tree, allocated, START);
            assertEquals(allocated, reachable);
        }

        @Test
        @DisplayName("excludes disconnected nodes")
        void excludesDisconnected() {
            // NODE4 is not connected because NODE3 is not allocated
            Set<String> allocated = Set.of(START, NODE1, NODE2, NODE4);
            Set<String> reachable = PassiveTreeGraph.getReachableFromStart(tree, allocated, START);
            assertEquals(Set.of(START, NODE1, NODE2), reachable);
        }

        @Test
        @DisplayName("returns empty when start not allocated")
        void startNotAllocated() {
            Set<String> allocated = Set.of(NODE1, NODE2);
            Set<String> reachable = PassiveTreeGraph.getReachableFromStart(tree, allocated, START);
            assertTrue(reachable.isEmpty());
        }
    }

    // ========== Orphaned Nodes Tests ==========

    @Nested
    @DisplayName("getOrphanedNodes")
    class GetOrphanedNodesTests {

        @Test
        @DisplayName("removing leaf node orphans only itself")
        void removeLeaf() {
            Set<String> allocated = Set.of(START, NODE1, NODE2, NODE3);
            Set<String> orphaned = PassiveTreeGraph.getOrphanedNodes(tree, allocated, START, NODE2);
            assertEquals(Set.of(NODE2), orphaned);
        }

        @Test
        @DisplayName("removing bridge node orphans downstream")
        void removeBridge() {
            Set<String> allocated = Set.of(START, NODE1, NODE3, NODE4, KEYSTONE);
            Set<String> orphaned = PassiveTreeGraph.getOrphanedNodes(tree, allocated, START, NODE3);
            // Removing NODE3 orphans NODE3, NODE4, and KEYSTONE
            assertEquals(Set.of(NODE3, NODE4, KEYSTONE), orphaned);
        }

        @Test
        @DisplayName("removing start with other allocations orphans all")
        void removeStartWithOthers() {
            Set<String> allocated = Set.of(START, NODE1);
            Set<String> orphaned = PassiveTreeGraph.getOrphanedNodes(tree, allocated, START, START);
            assertEquals(allocated, orphaned);
        }

        @Test
        @DisplayName("removing start alone orphans only start")
        void removeStartAlone() {
            Set<String> allocated = Set.of(START);
            Set<String> orphaned = PassiveTreeGraph.getOrphanedNodes(tree, allocated, START, START);
            assertEquals(Set.of(START), orphaned);
        }
    }

    // ========== Reachable Unallocated Tests ==========

    @Nested
    @DisplayName("getReachableUnallocatedNodes")
    class GetReachableUnallocatedNodesTests {

        @Test
        @DisplayName("empty allocations returns starting nodes")
        void emptyAllocations() {
            Set<String> reachable = PassiveTreeGraph.getReachableUnallocatedNodes(tree, Collections.emptySet());
            assertEquals(tree.getStartingNodeIds(), reachable);
        }

        @Test
        @DisplayName("returns adjacent unallocated nodes")
        void adjacentUnallocated() {
            Set<String> allocated = Set.of(START, NODE1);
            Set<String> reachable = PassiveTreeGraph.getReachableUnallocatedNodes(tree, allocated);
            assertEquals(Set.of(NODE2, NODE3), reachable);
        }

        @Test
        @DisplayName("excludes already allocated nodes")
        void excludesAllocated() {
            Set<String> allocated = Set.of(START, NODE1, NODE2, NODE3);
            Set<String> reachable = PassiveTreeGraph.getReachableUnallocatedNodes(tree, allocated);
            assertEquals(Set.of(NODE4), reachable);
        }
    }

    // ========== Can Allocate Tests ==========

    @Nested
    @DisplayName("canAllocateNode")
    class CanAllocateNodeTests {

        @Test
        @DisplayName("can allocate starting node with no allocations")
        void canAllocateStarting() {
            assertTrue(PassiveTreeGraph.canAllocateNode(tree, Collections.emptySet(), START));
        }

        @Test
        @DisplayName("cannot allocate non-start with no allocations")
        void cannotAllocateNonStart() {
            assertFalse(PassiveTreeGraph.canAllocateNode(tree, Collections.emptySet(), NODE1));
        }

        @Test
        @DisplayName("can allocate adjacent node")
        void canAllocateAdjacent() {
            assertTrue(PassiveTreeGraph.canAllocateNode(tree, Set.of(START), NODE1));
        }

        @Test
        @DisplayName("cannot allocate non-adjacent node")
        void cannotAllocateNonAdjacent() {
            assertFalse(PassiveTreeGraph.canAllocateNode(tree, Set.of(START), NODE3));
        }

        @Test
        @DisplayName("cannot allocate already allocated node")
        void cannotAllocateAlready() {
            assertFalse(PassiveTreeGraph.canAllocateNode(tree, Set.of(START, NODE1), NODE1));
        }
    }

    // ========== Can Deallocate Tests ==========

    @Nested
    @DisplayName("canDeallocateNode")
    class CanDeallocateNodeTests {

        @Test
        @DisplayName("can deallocate start when alone")
        void canDeallocateStartAlone() {
            assertTrue(PassiveTreeGraph.canDeallocateNode(tree, Set.of(START), START, START));
        }

        @Test
        @DisplayName("cannot deallocate start with other allocations")
        void cannotDeallocateStartWithOthers() {
            assertFalse(PassiveTreeGraph.canDeallocateNode(tree, Set.of(START, NODE1), START, START));
        }

        @Test
        @DisplayName("can deallocate leaf node")
        void canDeallocateLeaf() {
            Set<String> allocated = Set.of(START, NODE1, NODE2, NODE3);
            assertTrue(PassiveTreeGraph.canDeallocateNode(tree, allocated, START, NODE2));
            assertTrue(PassiveTreeGraph.canDeallocateNode(tree, allocated, START, NODE3));
        }

        @Test
        @DisplayName("cannot deallocate bridge node")
        void cannotDeallocateBridge() {
            Set<String> allocated = Set.of(START, NODE1, NODE3, NODE4);
            assertFalse(PassiveTreeGraph.canDeallocateNode(tree, allocated, START, NODE3));
        }

        @Test
        @DisplayName("cannot deallocate unallocated node")
        void cannotDeallocateUnallocated() {
            assertFalse(PassiveTreeGraph.canDeallocateNode(tree, Set.of(START, NODE1), START, NODE3));
        }
    }

    // ========== Path Allocation Order Tests ==========

    @Nested
    @DisplayName("getPathAllocationOrder")
    class GetPathAllocationOrderTests {

        @Test
        @DisplayName("returns empty for empty path")
        void emptyPath() {
            List<String> order = PassiveTreeGraph.getPathAllocationOrder(
                    tree, Set.of(START), Collections.emptyList());
            assertTrue(order.isEmpty());
        }

        @Test
        @DisplayName("returns path excluding already allocated")
        void excludesAllocated() {
            Set<String> allocated = Set.of(START, NODE1);
            List<String> path = List.of(NODE1, NODE3, NODE4);
            List<String> order = PassiveTreeGraph.getPathAllocationOrder(tree, allocated, path);
            assertEquals(List.of(NODE3, NODE4), order);
        }

        @Test
        @DisplayName("returns valid order for full path")
        void validFullPath() {
            Set<String> allocated = Set.of(START);
            List<String> path = List.of(NODE1, NODE3, NODE4, KEYSTONE);
            List<String> order = PassiveTreeGraph.getPathAllocationOrder(tree, allocated, path);
            assertEquals(path, order);
        }

        @Test
        @DisplayName("returns empty for invalid path")
        void invalidPath() {
            Set<String> allocated = Set.of(START);
            // NODE4 cannot be allocated before NODE3
            List<String> path = List.of(NODE1, NODE4);
            List<String> order = PassiveTreeGraph.getPathAllocationOrder(tree, allocated, path);
            assertTrue(order.isEmpty());
        }
    }
}
