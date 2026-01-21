package reign.software.hyforged.stats.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.stats.component.EffectBridgeComponent;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Effect Bridge system.
 * <p>
 * These tests focus on the EffectBridgeComponent's tracking functionality
 * and the HyforgedEffectBridgeSystem's source ID patterns.
 * <p>
 * Full integration testing with Hytale's EffectControllerComponent requires
 * the game runtime and is done via in-game testing.
 */
@DisplayName("Effect Bridge System Tests")
class EffectBridgeSystemTest {

    @Nested
    @DisplayName("EffectBridgeComponent Tests")
    class EffectBridgeComponentTests {
        
        private EffectBridgeComponent component;
        
        @BeforeEach
        void setUp() {
            component = new EffectBridgeComponent();
        }
        
        @Test
        @DisplayName("New component has no bridged effects")
        void newComponentIsEmpty() {
            assertTrue(component.getBridgedEffectIndices().isEmpty());
        }
        
        @Test
        @DisplayName("Can mark effect as bridged")
        void canMarkEffectBridged() {
            component.markBridged(42);
            
            assertTrue(component.isBridged(42));
            assertEquals(1, component.getBridgedEffectIndices().size());
        }
        
        @Test
        @DisplayName("Can unmark effect")
        void canUnmarkEffect() {
            component.markBridged(42);
            component.unmarkBridged(42);
            
            assertFalse(component.isBridged(42));
            assertTrue(component.getBridgedEffectIndices().isEmpty());
        }
        
        @Test
        @DisplayName("Can track multiple effects")
        void canTrackMultipleEffects() {
            component.markBridged(1);
            component.markBridged(2);
            component.markBridged(3);
            
            assertTrue(component.isBridged(1));
            assertTrue(component.isBridged(2));
            assertTrue(component.isBridged(3));
            assertFalse(component.isBridged(4));
            assertEquals(3, component.getBridgedEffectIndices().size());
        }
        
        @Test
        @DisplayName("Marking same effect twice is idempotent")
        void markingTwiceIsIdempotent() {
            component.markBridged(42);
            component.markBridged(42);
            
            assertTrue(component.isBridged(42));
            assertEquals(1, component.getBridgedEffectIndices().size());
        }
        
        @Test
        @DisplayName("Unmarking non-existent effect is safe")
        void unmarkingNonExistentIsSafe() {
            assertDoesNotThrow(() -> component.unmarkBridged(999));
            assertTrue(component.getBridgedEffectIndices().isEmpty());
        }
        
        @Test
        @DisplayName("Clear removes all tracked effects")
        void clearRemovesAllEffects() {
            component.markBridged(1);
            component.markBridged(2);
            component.markBridged(3);
            
            component.clear();
            
            assertTrue(component.getBridgedEffectIndices().isEmpty());
            assertFalse(component.isBridged(1));
            assertFalse(component.isBridged(2));
            assertFalse(component.isBridged(3));
        }
        
        @Test
        @DisplayName("Copy constructor creates independent copy")
        void copyConstructorCreatesIndependentCopy() {
            component.markBridged(1);
            component.markBridged(2);
            
            EffectBridgeComponent copy = new EffectBridgeComponent(component);
            
            // Copy has same effects
            assertTrue(copy.isBridged(1));
            assertTrue(copy.isBridged(2));
            assertEquals(2, copy.getBridgedEffectIndices().size());
            
            // Modifications to original don't affect copy
            component.markBridged(3);
            assertFalse(copy.isBridged(3));
            
            // Modifications to copy don't affect original
            copy.unmarkBridged(1);
            assertTrue(component.isBridged(1));
        }
        
        @Test
        @DisplayName("Clone creates independent copy")
        void cloneCreatesIndependentCopy() {
            component.markBridged(10);
            component.markBridged(20);
            
            EffectBridgeComponent clone = (EffectBridgeComponent) component.clone();
            
            assertEquals(2, clone.getBridgedEffectIndices().size());
            assertTrue(clone.isBridged(10));
            assertTrue(clone.isBridged(20));
            
            // Clone is independent
            clone.clear();
            assertTrue(component.isBridged(10));
        }
    }
    
    @Nested
    @DisplayName("Source ID Pattern Tests")
    class SourceIdPatternTests {
        
        @Test
        @DisplayName("Effect source prefix is 'effect:'")
        void effectSourcePrefixIsCorrect() {
            assertEquals("effect:", HyforgedEffectBridgeSystem.EFFECT_SOURCE_PREFIX);
        }
        
        @Test
        @DisplayName("Effect source ID follows pattern")
        void effectSourceIdPattern() {
            String sourceId = HyforgedEffectBridgeSystem.EFFECT_SOURCE_PREFIX + "poison";
            
            assertEquals("effect:poison", sourceId);
            assertTrue(sourceId.startsWith("effect:"));
        }
        
        @Test
        @DisplayName("Can extract effect ID from source ID")
        void canExtractEffectId() {
            String sourceId = "effect:burning";
            String prefix = HyforgedEffectBridgeSystem.EFFECT_SOURCE_PREFIX;
            
            assertTrue(sourceId.startsWith(prefix));
            
            String effectId = sourceId.substring(prefix.length());
            assertEquals("burning", effectId);
        }
        
        @Test
        @DisplayName("Effect source IDs are unique per effect")
        void effectSourceIdsAreUnique() {
            String sourceId1 = HyforgedEffectBridgeSystem.EFFECT_SOURCE_PREFIX + "effect1";
            String sourceId2 = HyforgedEffectBridgeSystem.EFFECT_SOURCE_PREFIX + "effect2";
            
            assertNotEquals(sourceId1, sourceId2);
        }
    }
    
    @Nested
    @DisplayName("Effect Index Handling Tests")
    class EffectIndexHandlingTests {
        
        private EffectBridgeComponent component;
        
        @BeforeEach
        void setUp() {
            component = new EffectBridgeComponent();
        }
        
        @Test
        @DisplayName("Handles zero index")
        void handlesZeroIndex() {
            component.markBridged(0);
            assertTrue(component.isBridged(0));
        }
        
        @Test
        @DisplayName("Handles large indices")
        void handlesLargeIndices() {
            component.markBridged(10000);
            component.markBridged(50000);
            
            assertTrue(component.isBridged(10000));
            assertTrue(component.isBridged(50000));
            assertFalse(component.isBridged(10001));
        }
        
        @Test
        @DisplayName("Handles negative indices gracefully")
        void handlesNegativeIndices() {
            // Implementation should not throw, but negative indices
            // aren't valid effect indices from Hytale
            assertDoesNotThrow(() -> component.markBridged(-1));
            assertDoesNotThrow(() -> component.isBridged(-1));
        }
        
        @Test
        @DisplayName("getBridgedEffectIndices returns the internal set (caller should not modify)")
        void getBridgedEffectIndicesReturnsInternalSet() {
            component.markBridged(1);
            component.markBridged(2);
            
            // Returns the internal set directly for efficiency
            // Caller is responsible for not modifying it
            var indices = component.getBridgedEffectIndices();
            
            assertTrue(indices.contains(1));
            assertTrue(indices.contains(2));
            assertEquals(2, indices.size());
        }
    }
}
