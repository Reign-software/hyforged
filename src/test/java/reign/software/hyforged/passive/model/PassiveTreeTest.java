package reign.software.hyforged.passive.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PassiveTree class.
 */
@DisplayName("PassiveTree Tests")
class PassiveTreeTest {

    private PassiveTree generalTree;
    private PassiveTree classTree;
    
    @BeforeEach
    void setUp() {
        // Create a simple general tree
        //   start
        //     |
        //   node1
        //   /   \
        // node2  node3
        //         |
        //      keystone
        
        PassiveNode start = PassiveNode.builder("start")
                .type(PassiveNodeType.MINOR)
                .name("Start")
                .description("Starting point")
                .position(0, 0)
                .build();
        
        PassiveNode node1 = PassiveNode.builder("node1")
                .type(PassiveNodeType.MINOR)
                .name("Node 1")
                .description("First node")
                .position(0, -20)
                .effects(List.of(PassiveNodeEffect.statModifier("hyforged:strength", 5)))
                .build();
        
        PassiveNode node2 = PassiveNode.builder("node2")
                .type(PassiveNodeType.NOTABLE)
                .name("Node 2 Notable")
                .description("A notable node")
                .position(-20, -40)
                .effects(List.of(PassiveNodeEffect.statModifier("hyforged:armor", 10)))
                .build();
        
        PassiveNode node3 = PassiveNode.builder("node3")
                .type(PassiveNodeType.MINOR)
                .name("Node 3")
                .description("Another node")
                .position(20, -40)
                .build();
        
        PassiveNode keystone = PassiveNode.builder("keystone")
                .type(PassiveNodeType.KEYSTONE)
                .name("Mighty Keystone")
                .description("Powerful effect")
                .position(20, -60)
                .keystoneFamily("test-family")
                .effects(List.of(PassiveNodeEffect.unlockFlag("hyforged:test-flag")))
                .build();
        
        List<PassiveConnection> connections = List.of(
                new PassiveConnection("start", "node1"),
                new PassiveConnection("node1", "node2"),
                new PassiveConnection("node1", "node3"),
                new PassiveConnection("node3", "keystone")
        );
        
        generalTree = new PassiveTree(
                "hyforged:test-general",
                PassiveTreeType.GENERAL,
                null,
                Set.of("start"),
                Map.of(
                        "start", start,
                        "node1", node1,
                        "node2", node2,
                        "node3", node3,
                        "keystone", keystone
                ),
                connections,
                List.of(),
                1
        );
        
        // Create a simple class tree
        PassiveNode classStart = PassiveNode.builder("class-start")
                .type(PassiveNodeType.MINOR)
                .name("Warrior Start")
                .description("Starting point for warrior")
                .position(0, 0)
                .build();
        
        PassiveNode classNode1 = PassiveNode.builder("class-node1")
                .type(PassiveNodeType.MINOR)
                .name("Warrior Node 1")
                .description("First warrior node")
                .position(0, -20)
                .build();
        
        classTree = new PassiveTree(
                "hyforged:test-warrior",
                PassiveTreeType.CLASS,
                "hyforged:warrior",
                Set.of("class-start"),
                Map.of(
                        "class-start", classStart,
                        "class-node1", classNode1
                ),
                List.of(new PassiveConnection("class-start", "class-node1")),
                List.of(),
                1
        );
    }
    
    // ========== Basic Properties Tests ==========
    
    @Nested
    @DisplayName("Basic Properties")
    class BasicPropertiesTests {
        
        @Test
        @DisplayName("general tree has correct properties")
        void generalTreeProperties() {
            assertEquals("hyforged:test-general", generalTree.getId());
            assertEquals(PassiveTreeType.GENERAL, generalTree.getTreeType());
            assertTrue(generalTree.isGeneralTree());
            assertNull(generalTree.getClassId());
            assertEquals(1, generalTree.getVersion());
        }
        
        @Test
        @DisplayName("class tree has correct properties")
        void classTreeProperties() {
            assertEquals("hyforged:test-warrior", classTree.getId());
            assertEquals(PassiveTreeType.CLASS, classTree.getTreeType());
            assertFalse(classTree.isGeneralTree());
            assertEquals("hyforged:warrior", classTree.getClassId());
        }
        
        @Test
        @DisplayName("starting nodes are correct")
        void startingNodesAreCorrect() {
            Set<String> generalStarts = generalTree.getStartingNodeIds();
            assertEquals(1, generalStarts.size());
            assertTrue(generalStarts.contains("start"));
            
            Set<String> classStarts = classTree.getStartingNodeIds();
            assertEquals(1, classStarts.size());
            assertTrue(classStarts.contains("class-start"));
        }
    }
    
    // ========== Node Access Tests ==========
    
    @Nested
    @DisplayName("Node Access")
    class NodeAccessTests {
        
        @Test
        @DisplayName("getNode returns correct node")
        void getNodeReturnsCorrectNode() {
            PassiveNode node = generalTree.getNode("node1");
            assertNotNull(node);
            assertEquals("node1", node.id());
            assertEquals("Node 1", node.name());
        }
        
        @Test
        @DisplayName("getNode returns null for missing node")
        void getNodeReturnsNullForMissing() {
            PassiveNode node = generalTree.getNode("nonexistent");
            assertNull(node);
        }
        
        @Test
        @DisplayName("getNodes returns all nodes")
        void getNodesReturnsAllNodes() {
            Map<String, PassiveNode> nodes = generalTree.getNodes();
            assertEquals(5, nodes.size());
            assertTrue(nodes.containsKey("start"));
            assertTrue(nodes.containsKey("keystone"));
        }
        
        @Test
        @DisplayName("nodes map is unmodifiable")
        void nodesMapIsUnmodifiable() {
            Map<String, PassiveNode> nodes = generalTree.getNodes();
            PassiveNode newNode = PassiveNode.builder("new")
                    .name("New")
                    .description("New node")
                    .position(100, 100)
                    .build();
            
            assertThrows(UnsupportedOperationException.class, () ->
                    nodes.put("new", newNode));
        }
    }
    
    // ========== Adjacency Tests ==========
    
    @Nested
    @DisplayName("Adjacency")
    class AdjacencyTests {
        
        @Test
        @DisplayName("getAdjacentNodes returns correct neighbors")
        void getAdjacentNodesReturnsNeighbors() {
            Set<String> startNeighbors = generalTree.getAdjacentNodes("start");
            assertEquals(1, startNeighbors.size());
            assertTrue(startNeighbors.contains("node1"));
            
            Set<String> node1Neighbors = generalTree.getAdjacentNodes("node1");
            assertEquals(3, node1Neighbors.size()); // start, node2, node3 (bidirectional)
            assertTrue(node1Neighbors.contains("start"));
            assertTrue(node1Neighbors.contains("node2"));
            assertTrue(node1Neighbors.contains("node3"));
        }
        
        @Test
        @DisplayName("getAdjacentNodes returns empty for leaf with no outgoing")
        void getAdjacentNodesForLeaf() {
            // node2 only connects to node1
            Set<String> node2Neighbors = generalTree.getAdjacentNodes("node2");
            assertEquals(1, node2Neighbors.size());
            assertTrue(node2Neighbors.contains("node1"));
        }
        
        @Test
        @DisplayName("getAdjacentNodes returns empty for unknown node")
        void getAdjacentNodesForUnknown() {
            Set<String> neighbors = generalTree.getAdjacentNodes("unknown");
            assertTrue(neighbors.isEmpty());
        }
        
        @Test
        @DisplayName("areAdjacent returns correct result")
        void areAdjacentReturnsCorrectResult() {
            assertTrue(generalTree.areAdjacent("start", "node1"));
            assertTrue(generalTree.areAdjacent("node1", "start")); // Bidirectional
            assertTrue(generalTree.areAdjacent("node1", "node2"));
            assertFalse(generalTree.areAdjacent("start", "node2")); // Not directly connected
            assertFalse(generalTree.areAdjacent("start", "keystone")); // Not connected
        }
    }
    
    // ========== Connection Tests ==========
    
    @Nested
    @DisplayName("Connections")
    class ConnectionTests {
        
        @Test
        @DisplayName("getConnections returns all connections")
        void getConnectionsReturnsAll() {
            List<PassiveConnection> connections = generalTree.getConnections();
            assertEquals(4, connections.size());
        }
        
        @Test
        @DisplayName("connections list is unmodifiable")
        void connectionsListIsUnmodifiable() {
            List<PassiveConnection> connections = generalTree.getConnections();
            
            assertThrows(UnsupportedOperationException.class, () ->
                    connections.add(new PassiveConnection("a", "b")));
        }
    }
    
    // ========== Node Type Query Tests ==========
    
    @Nested
    @DisplayName("Node Type Queries")
    class NodeTypeQueryTests {
        
        @Test
        @DisplayName("finds keystones correctly")
        void findsKeystones() {
            PassiveNode node = generalTree.getNode("keystone");
            assertNotNull(node);
            assertTrue(node.isKeystone());
            assertEquals("Mighty Keystone", node.name());
        }
        
        @Test
        @DisplayName("finds notables correctly")
        void findsNotables() {
            PassiveNode node = generalTree.getNode("node2");
            assertNotNull(node);
            assertTrue(node.isNotable());
        }
    }
}
