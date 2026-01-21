package reign.software.hyforged.combat.ailment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AilmentAccumulatorComponent}.
 */
@DisplayName("AilmentAccumulatorComponent")
class AilmentAccumulatorComponentTest {
    
    private AilmentAccumulatorComponent component;
    private static final long BASE_TIME = 1000000L;
    
    @BeforeEach
    void setUp() {
        component = new AilmentAccumulatorComponent();
    }
    
    @Nested
    @DisplayName("Damage Accumulation")
    class DamageAccumulationTests {
        
        @Test
        @DisplayName("should accumulate damage for an element")
        void shouldAccumulateDamage() {
            component.setThreshold("fire", 100);
            
            // Below threshold - should not trigger
            boolean triggered = component.accumulateDamage("fire", 50f, BASE_TIME);
            
            assertFalse(triggered);
            assertEquals(50f, component.getAccumulatedDamage("fire", BASE_TIME), 0.001f);
        }
        
        @Test
        @DisplayName("should trigger when threshold is reached")
        void shouldTriggerAtThreshold() {
            component.setThreshold("fire", 100);
            
            component.accumulateDamage("fire", 60f, BASE_TIME);
            boolean triggered = component.accumulateDamage("fire", 50f, BASE_TIME + 100);
            
            assertTrue(triggered);
        }
        
        @Test
        @DisplayName("should use default threshold when not set")
        void shouldUseDefaultThreshold() {
            // Default threshold is 100
            component.accumulateDamage("ice", 60f, BASE_TIME);
            boolean triggered = component.accumulateDamage("ice", 50f, BASE_TIME + 100);
            
            assertTrue(triggered);
        }
        
        @Test
        @DisplayName("should track multiple elements independently")
        void shouldTrackElementsIndependently() {
            component.setThreshold("fire", 100);
            component.setThreshold("ice", 150);
            
            // Add damage to fire
            component.accumulateDamage("fire", 80f, BASE_TIME);
            // Add damage to ice
            component.accumulateDamage("ice", 80f, BASE_TIME);
            
            assertEquals(80f, component.getAccumulatedDamage("fire", BASE_TIME), 0.001f);
            assertEquals(80f, component.getAccumulatedDamage("ice", BASE_TIME), 0.001f);
            
            // Trigger fire (20 more pushes to 100)
            boolean fireTriggered = component.accumulateDamage("fire", 30f, BASE_TIME + 100);
            assertTrue(fireTriggered);
            
            // Ice should not be affected
            boolean iceTriggered = component.accumulateDamage("ice", 30f, BASE_TIME + 100);
            assertFalse(iceTriggered); // 110 < 150
        }
    }
    
    @Nested
    @DisplayName("Time Window")
    class TimeWindowTests {
        
        @Test
        @DisplayName("should expire damage outside window")
        void shouldExpireDamageOutsideWindow() {
            component.setThreshold("fire", 100);
            component.setWindow("fire", 5000); // 5 second window
            
            component.accumulateDamage("fire", 80f, BASE_TIME);
            
            // Within window - should still have damage
            assertEquals(80f, component.getAccumulatedDamage("fire", BASE_TIME + 4000), 0.001f);
            
            // Outside window - should be expired
            assertEquals(0f, component.getAccumulatedDamage("fire", BASE_TIME + 6000), 0.001f);
        }
        
        @Test
        @DisplayName("should reset window on new damage")
        void shouldResetWindowOnNewDamage() {
            component.setThreshold("fire", 100);
            component.setWindow("fire", 5000);
            
            component.accumulateDamage("fire", 40f, BASE_TIME);
            component.accumulateDamage("fire", 40f, BASE_TIME + 4000);
            
            // At 4000ms, both damages are still valid
            assertEquals(80f, component.getAccumulatedDamage("fire", BASE_TIME + 4000), 0.001f);
        }
        
        @Test
        @DisplayName("should use default window when not set")
        void shouldUseDefaultWindow() {
            // Default window is 5000ms
            component.accumulateDamage("fire", 80f, BASE_TIME);
            
            // Within default window
            assertEquals(80f, component.getAccumulatedDamage("fire", BASE_TIME + 4000), 0.001f);
            
            // Outside default window
            assertEquals(0f, component.getAccumulatedDamage("fire", BASE_TIME + 6000), 0.001f);
        }
    }
    
    @Nested
    @DisplayName("Reset Operations")
    class ResetTests {
        
        @Test
        @DisplayName("should reset specific element")
        void shouldResetSpecificElement() {
            component.accumulateDamage("fire", 80f, BASE_TIME);
            component.accumulateDamage("ice", 60f, BASE_TIME);
            
            component.resetAccumulation("fire");
            
            assertEquals(0f, component.getAccumulatedDamage("fire", BASE_TIME), 0.001f);
            assertEquals(60f, component.getAccumulatedDamage("ice", BASE_TIME), 0.001f);
        }
        
        @Test
        @DisplayName("should reset all elements")
        void shouldResetAllElements() {
            component.accumulateDamage("fire", 80f, BASE_TIME);
            component.accumulateDamage("ice", 60f, BASE_TIME);
            component.accumulateDamage("lightning", 50f, BASE_TIME);
            
            component.resetAll();
            
            assertEquals(0f, component.getAccumulatedDamage("fire", BASE_TIME), 0.001f);
            assertEquals(0f, component.getAccumulatedDamage("ice", BASE_TIME), 0.001f);
            assertEquals(0f, component.getAccumulatedDamage("lightning", BASE_TIME), 0.001f);
        }
    }
    
    @Nested
    @DisplayName("Clone Operation")
    class CloneTests {
        
        @Test
        @DisplayName("should deep copy accumulated state")
        void shouldDeepCopyState() {
            component.accumulateDamage("fire", 80f, BASE_TIME);
            
            AilmentAccumulatorComponent clone = component.clone();
            
            // Clone should have copied state
            assertEquals(80f, clone.getAccumulatedDamage("fire", BASE_TIME), 0.001f);
            
            // Modifying clone should not affect original
            clone.resetAccumulation("fire");
            assertEquals(0f, clone.getAccumulatedDamage("fire", BASE_TIME), 0.001f);
            assertEquals(80f, component.getAccumulatedDamage("fire", BASE_TIME), 0.001f);
        }
    }
}
