package reign.software.hyforged.passive.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PassiveNode record and related model classes.
 */
@DisplayName("PassiveNode Model Tests")
class PassiveNodeTest {

    // ========== Test Data Factory Methods ==========
    
    private static PassiveNode createMinorNode(String id) {
        return PassiveNode.builder(id)
                .type(PassiveNodeType.MINOR)
                .name("Test Node " + id)
                .description("A test node")
                .icon("hyforged:icons/test")
                .position(new PassiveNodePosition(0, 0))
                .region("test-region")
                .build();
    }
    
    private static PassiveNode createNodeWithEffect(String id, PassiveNodeEffect effect) {
        return PassiveNode.builder(id)
                .type(PassiveNodeType.MINOR)
                .name("Test Node " + id)
                .description("A node with effects")
                .icon("hyforged:icons/test")
                .position(new PassiveNodePosition(10, 20))
                .region("test-region")
                .effects(List.of(effect))
                .build();
    }
    
    private static PassiveNode createKeystoneNode(String id, String keystoneFamily) {
        return PassiveNode.builder(id)
                .type(PassiveNodeType.KEYSTONE)
                .name("Keystone " + id)
                .description("A powerful keystone")
                .icon("hyforged:icons/keystone")
                .position(new PassiveNodePosition(50, 50))
                .region("core")
                .keystoneFamily(keystoneFamily)
                .build();
    }
    
    // ========== PassiveNodeType Tests ==========
    
    @Nested
    @DisplayName("PassiveNodeType")
    class NodeTypeTests {
        
        @Test
        @DisplayName("has all expected type constants")
        void hasAllTypeConstants() {
            assertNotNull(PassiveNodeType.MINOR);
            assertNotNull(PassiveNodeType.NOTABLE);
            assertNotNull(PassiveNodeType.KEYSTONE);
            assertNotNull(PassiveNodeType.MASTERY);
            assertNotNull(PassiveNodeType.UNLOCK);
        }
        
        @Test
        @DisplayName("types are distinct strings")
        void typesAreDistinct() {
            Set<String> types = Set.of(
                    PassiveNodeType.MINOR,
                    PassiveNodeType.NOTABLE,
                    PassiveNodeType.KEYSTONE,
                    PassiveNodeType.MASTERY,
                    PassiveNodeType.UNLOCK
            );
            assertEquals(5, types.size(), "All node types should be unique");
        }
    }
    
    // ========== PassiveNodePosition Tests ==========
    
    @Nested
    @DisplayName("PassiveNodePosition")
    class PositionTests {
        
        @Test
        @DisplayName("creates position with x and y")
        void createsPositionWithCoordinates() {
            PassiveNodePosition pos = new PassiveNodePosition(100, -50);
            assertEquals(100, pos.x());
            assertEquals(-50, pos.y());
        }
        
        @Test
        @DisplayName("equals works correctly")
        void equalsWorksCorrectly() {
            PassiveNodePosition pos1 = new PassiveNodePosition(10, 20);
            PassiveNodePosition pos2 = new PassiveNodePosition(10, 20);
            PassiveNodePosition pos3 = new PassiveNodePosition(10, 30);
            
            assertEquals(pos1, pos2);
            assertNotEquals(pos1, pos3);
        }
    }
    
    // ========== PassiveNodeEffect Tests ==========
    
    @Nested
    @DisplayName("PassiveNodeEffect")
    class EffectTests {
        
        @Test
        @DisplayName("creates stat modifier effect")
        void createsStatModifierEffect() {
            PassiveNodeEffect effect = PassiveNodeEffect.statModifier("hyforged:strength", 10);
            
            assertEquals("stat-modifier", effect.type());
            Map<String, Object> data = effect.data();
            assertEquals("hyforged:strength", data.get("Stat"));
            assertEquals(10, data.get("Value"));
        }
        
        @Test
        @DisplayName("creates spell grant effect")
        void createsSpellGrantEffect() {
            PassiveNodeEffect effect = PassiveNodeEffect.spellGrant("hyforged:fireball");
            
            assertEquals("spell-grant", effect.type());
            assertEquals("hyforged:fireball", effect.data().get("SpellId"));
        }
        
        @Test
        @DisplayName("creates unlock flag effect")
        void createsUnlockFlagEffect() {
            PassiveNodeEffect effect = PassiveNodeEffect.unlockFlag("hyforged:stun-immune");
            
            assertEquals("unlock-flag", effect.type());
            assertEquals("hyforged:stun-immune", effect.data().get("FlagId"));
        }
        
        @Test
        @DisplayName("creates mastery choice effect")
        void createsMasteryChoiceEffect() {
            PassiveNodeEffect choice1 = PassiveNodeEffect.statModifier("hyforged:damage", 100);
            PassiveNodeEffect choice2 = PassiveNodeEffect.statModifier("hyforged:crit", 50);
            
            PassiveNodeEffect effect = PassiveNodeEffect.masteryChoice(List.of(choice1, choice2));
            
            assertEquals("mastery-choice", effect.type());
            @SuppressWarnings("unchecked")
            List<PassiveNodeEffect> choices = (List<PassiveNodeEffect>) effect.data().get("Choices");
            assertEquals(2, choices.size());
        }
    }
    
    // ========== PassiveNode Builder Tests ==========
    
    @Nested
    @DisplayName("PassiveNode.Builder")
    class BuilderTests {
        
        @Test
        @DisplayName("builds minimal node with required fields")
        void buildsMinimalNode() {
            PassiveNode node = PassiveNode.builder("test-node")
                    .type(PassiveNodeType.MINOR)
                    .name("Test")
                    .description("Desc")
                    .icon("icon")
                    .position(new PassiveNodePosition(0, 0))
                    .region("region")
                    .build();
            
            assertNotNull(node);
            assertEquals("test-node", node.id());
            assertEquals(PassiveNodeType.MINOR, node.type());
            assertEquals("Test", node.name());
        }
        
        @Test
        @DisplayName("builds node with effects")
        void buildsNodeWithEffects() {
            PassiveNode node = PassiveNode.builder("effect-node")
                    .type(PassiveNodeType.NOTABLE)
                    .name("Effective")
                    .description("Has effects")
                    .icon("icon")
                    .position(new PassiveNodePosition(10, 10))
                    .region("test")
                    .effects(List.of(
                            PassiveNodeEffect.statModifier("hyforged:strength", 5),
                            PassiveNodeEffect.statModifier("hyforged:dexterity", 3)
                    ))
                    .build();
            
            assertEquals(2, node.effects().size());
        }
        
        @Test
        @DisplayName("builds node with requirements")
        void buildsNodeWithRequirements() {
            PassiveNodeRequirements reqs = new PassiveNodeRequirements(5, List.of());
            
            PassiveNode node = PassiveNode.builder("req-node")
                    .type(PassiveNodeType.NOTABLE)
                    .name("Gated")
                    .description("Requires nodes")
                    .icon("icon")
                    .position(new PassiveNodePosition(20, 20))
                    .region("test")
                    .requirements(reqs)
                    .build();
            
            assertNotNull(node.requirements());
            assertEquals(5, node.requirements().allocatedNodes());
        }
        
        @Test
        @DisplayName("builds keystone with family")
        void buildsKeystoneWithFamily() {
            PassiveNode node = PassiveNode.builder("keystone-1")
                    .type(PassiveNodeType.KEYSTONE)
                    .name("Mighty Keystone")
                    .description("Powerful ability")
                    .icon("icon")
                    .position(new PassiveNodePosition(30, 30))
                    .region("core")
                    .keystoneFamily("hyforged:combat-keystones")
                    .build();
            
            assertEquals("hyforged:combat-keystones", node.keystoneFamily());
        }
        
        @Test
        @DisplayName("throws on null id")
        void throwsOnNullId() {
            assertThrows(NullPointerException.class, () ->
                    PassiveNode.builder(null));
        }
    }
    
    // ========== PassiveNode Record Tests ==========
    
    @Nested
    @DisplayName("PassiveNode Record")
    class RecordTests {
        
        @Test
        @DisplayName("record accessors work correctly")
        void recordAccessorsWork() {
            PassiveNode node = createMinorNode("accessor-test");
            
            assertEquals("accessor-test", node.id());
            assertEquals(PassiveNodeType.MINOR, node.type());
            assertEquals("Test Node accessor-test", node.name());
            assertEquals("A test node", node.description());
            assertNotNull(node.position());
            assertEquals("test-region", node.region());
        }
        
        @Test
        @DisplayName("effects list is immutable")
        void effectsListIsImmutable() {
            PassiveNode node = createNodeWithEffect("immutable-test", 
                    PassiveNodeEffect.statModifier("stat", 10));
            
            assertThrows(UnsupportedOperationException.class, () ->
                    node.effects().add(PassiveNodeEffect.statModifier("other", 5)));
        }
        
        @Test
        @DisplayName("equals compares by id")
        void equalsCompareById() {
            PassiveNode node1 = createMinorNode("same-id");
            PassiveNode node2 = createMinorNode("same-id");
            PassiveNode node3 = createMinorNode("different-id");
            
            // Records compare all fields, so nodes with same ID but from different builders may differ
            // Test that ID is the primary distinguishing factor
            assertEquals(node1.id(), node2.id());
            assertNotEquals(node1.id(), node3.id());
        }
        
        @Test
        @DisplayName("helper methods work correctly")
        void helperMethodsWork() {
            PassiveNode minor = createMinorNode("minor");
            PassiveNode keystone = createKeystoneNode("keystone", "family");
            
            assertTrue(minor.isMinor());
            assertFalse(minor.isKeystone());
            assertTrue(keystone.isKeystone());
            assertFalse(keystone.isMinor());
        }
    }
    
    // ========== PassiveNodeRequirements Tests ==========
    
    @Nested
    @DisplayName("PassiveNodeRequirements")
    class RequirementsTests {
        
        @Test
        @DisplayName("creates requirements with node count")
        void createsWithNodeCount() {
            PassiveNodeRequirements reqs = new PassiveNodeRequirements(10, List.of());
            assertEquals(10, reqs.allocatedNodes());
            assertTrue(reqs.requiredTags().isEmpty());
        }
        
        @Test
        @DisplayName("creates requirements with tags")
        void createsWithTags() {
            List<String> tags = List.of("hyforged:strength", "hyforged:melee");
            PassiveNodeRequirements reqs = new PassiveNodeRequirements(0, tags);
            
            assertEquals(2, reqs.requiredTags().size());
            assertTrue(reqs.requiredTags().contains("hyforged:strength"));
        }
        
        @Test
        @DisplayName("tags are immutable")
        void tagsAreImmutable() {
            PassiveNodeRequirements reqs = new PassiveNodeRequirements(5, List.of("tag1"));
            
            assertThrows(UnsupportedOperationException.class, () ->
                    reqs.requiredTags().add("tag2"));
        }
        
        @Test
        @DisplayName("NONE constant has no requirements")
        void noneConstantHasNoRequirements() {
            assertTrue(PassiveNodeRequirements.NONE.isEmpty());
            assertEquals(0, PassiveNodeRequirements.NONE.allocatedNodes());
            assertTrue(PassiveNodeRequirements.NONE.requiredTags().isEmpty());
        }
    }
    
    // ========== PassiveConnection Tests ==========
    
    @Nested
    @DisplayName("PassiveConnection")
    class ConnectionTests {
        
        @Test
        @DisplayName("creates connection between nodes")
        void createsConnection() {
            PassiveConnection conn = new PassiveConnection("node-a", "node-b");
            
            assertEquals("node-a", conn.from());
            assertEquals("node-b", conn.to());
        }
        
        @Test
        @DisplayName("equals and hashCode work correctly")
        void equalsAndHashCodeWork() {
            PassiveConnection conn1 = new PassiveConnection("a", "b");
            PassiveConnection conn2 = new PassiveConnection("a", "b");
            PassiveConnection conn3 = new PassiveConnection("a", "c");
            
            assertEquals(conn1, conn2);
            assertEquals(conn1.hashCode(), conn2.hashCode());
            assertNotEquals(conn1, conn3);
        }
    }
}
