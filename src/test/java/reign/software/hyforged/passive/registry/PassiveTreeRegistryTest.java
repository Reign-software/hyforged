package reign.software.hyforged.passive.registry;

import org.junit.jupiter.api.*;
import reign.software.hyforged.passive.model.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PassiveTreeRegistry.
 */
@DisplayName("PassiveTreeRegistry Tests")
class PassiveTreeRegistryTest {

    private PassiveTreeRegistry registry;
    private PassiveTree generalTree;
    private PassiveTree warriorTree;
    private PassiveTree rangerTree;
    
    @BeforeEach
    void setUp() {
        PassiveTreeRegistry.reset();
        registry = PassiveTreeRegistry.get();
        
        // Create test trees
        generalTree = createSimpleTree("hyforged:general", PassiveTreeType.GENERAL, null, "start");
        warriorTree = createSimpleTree("hyforged:warrior", PassiveTreeType.CLASS, "hyforged:warrior", "warrior-start");
        rangerTree = createSimpleTree("hyforged:ranger", PassiveTreeType.CLASS, "hyforged:ranger", "ranger-start");
    }
    
    private PassiveTree createSimpleTree(String id, String treeType, String classId, String startNodeId) {
        PassiveNode startNode = PassiveNode.builder(startNodeId)
                .type(PassiveNodeType.MINOR)
                .name("Start")
                .description("Starting point")
                .position(0, 0)
                .build();
        
        PassiveNode node1 = PassiveNode.builder(id + "-node1")
                .type(PassiveNodeType.MINOR)
                .name("Node 1")
                .description("First node")
                .position(0, -20)
                .build();
        
        return new PassiveTree(
                id,
                treeType,
                classId,
                Set.of(startNodeId),
                Map.of(
                        startNodeId, startNode,
                        id + "-node1", node1
                ),
                List.of(new PassiveConnection(startNodeId, id + "-node1")),
                List.of(),
                1
        );
    }

    // ========== Registration Tests ==========

    @Nested
    @DisplayName("Registration")
    class RegistrationTests {

        @Test
        @DisplayName("registers general tree correctly")
        void registersGeneralTree() {
            registry.register(generalTree);
            
            assertEquals(generalTree, registry.getGeneralTree());
            assertEquals(generalTree, registry.getTree("hyforged:general"));
            assertEquals(1, registry.getTreeCount());
        }

        @Test
        @DisplayName("registers class tree correctly")
        void registersClassTree() {
            registry.register(warriorTree);
            
            assertEquals(warriorTree, registry.getClassTree("hyforged:warrior"));
            assertEquals(warriorTree, registry.getTree("hyforged:warrior"));
            assertEquals(1, registry.getTreeCount());
        }

        @Test
        @DisplayName("registers multiple trees")
        void registersMultipleTrees() {
            registry.register(generalTree);
            registry.register(warriorTree);
            registry.register(rangerTree);
            
            assertEquals(3, registry.getTreeCount());
            assertEquals(generalTree, registry.getGeneralTree());
            assertEquals(warriorTree, registry.getClassTree("hyforged:warrior"));
            assertEquals(rangerTree, registry.getClassTree("hyforged:ranger"));
        }

        @Test
        @DisplayName("hasTree returns correct value")
        void hasTreeReturnsCorrect() {
            assertFalse(registry.hasTree("hyforged:general"));
            
            registry.register(generalTree);
            
            assertTrue(registry.hasTree("hyforged:general"));
            assertFalse(registry.hasTree("hyforged:nonexistent"));
        }

        @Test
        @DisplayName("getAllTrees returns all registered trees")
        void getAllTreesReturnsAll() {
            registry.register(generalTree);
            registry.register(warriorTree);
            
            Collection<PassiveTree> all = registry.getAllTrees();
            assertEquals(2, all.size());
            assertTrue(all.contains(generalTree));
            assertTrue(all.contains(warriorTree));
        }

        @Test
        @DisplayName("getRegisteredClassIds returns class IDs")
        void getRegisteredClassIdsReturnsCorrect() {
            registry.register(generalTree);
            registry.register(warriorTree);
            registry.register(rangerTree);
            
            Set<String> classIds = registry.getRegisteredClassIds();
            assertEquals(2, classIds.size());
            assertTrue(classIds.contains("hyforged:warrior"));
            assertTrue(classIds.contains("hyforged:ranger"));
        }
    }

    // ========== Node Lookup Tests ==========

    @Nested
    @DisplayName("Node Lookup")
    class NodeLookupTests {

        @BeforeEach
        void registerTrees() {
            registry.register(generalTree);
            registry.register(warriorTree);
        }

        @Test
        @DisplayName("hasNode returns correct value")
        void hasNodeReturnsCorrect() {
            assertTrue(registry.hasNode("start"));
            assertTrue(registry.hasNode("hyforged:general-node1"));
            assertTrue(registry.hasNode("warrior-start"));
            assertFalse(registry.hasNode("nonexistent"));
        }

        @Test
        @DisplayName("getNode returns correct node")
        void getNodeReturnsCorrect() {
            PassiveNode node = registry.getNode("start");
            assertNotNull(node);
            assertEquals("start", node.id());
            assertEquals("Start", node.name());
        }

        @Test
        @DisplayName("getNode returns null for missing")
        void getNodeReturnsNullForMissing() {
            assertNull(registry.getNode("nonexistent"));
        }

        @Test
        @DisplayName("getTreeForNode returns correct tree")
        void getTreeForNodeReturnsCorrect() {
            assertEquals(generalTree, registry.getTreeForNode("start"));
            assertEquals(warriorTree, registry.getTreeForNode("warrior-start"));
        }

        @Test
        @DisplayName("getTreeForNode returns null for missing")
        void getTreeForNodeReturnsNullForMissing() {
            assertNull(registry.getTreeForNode("nonexistent"));
        }

        @Test
        @DisplayName("getNodeCount returns total nodes")
        void getNodeCountReturnsTotal() {
            // General tree: 2 nodes, Warrior tree: 2 nodes
            assertEquals(4, registry.getNodeCount());
        }
    }

    // ========== Freeze Tests ==========

    @Nested
    @DisplayName("Freeze Behavior")
    class FreezeBehaviorTests {

        @Test
        @DisplayName("registry is not frozen initially")
        void notFrozenInitially() {
            assertFalse(registry.isFrozen());
        }

        @Test
        @DisplayName("freeze locks registration")
        void freezeLocksRegistration() {
            registry.register(generalTree);
            registry.freeze();
            
            assertTrue(registry.isFrozen());
            
            assertThrows(IllegalStateException.class, () ->
                    registry.register(warriorTree));
        }
    }

    // ========== Refund Config Tests ==========

    @Nested
    @DisplayName("Refund Config")
    class RefundConfigTests {

        @Test
        @DisplayName("returns default cost when no config")
        void defaultCostWhenNoConfig() {
            // Should return a reasonable default (likely 0 or similar)
            int cost = registry.calculateRefundCost(10);
            // Just verify it doesn't throw
            assertTrue(cost >= 0);
        }
    }
}
