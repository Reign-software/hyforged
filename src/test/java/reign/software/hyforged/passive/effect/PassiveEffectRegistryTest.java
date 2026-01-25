package reign.software.hyforged.passive.effect;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.*;
import reign.software.hyforged.passive.model.*;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PassiveEffectRegistry.
 */
@DisplayName("PassiveEffectRegistry Tests")
class PassiveEffectRegistryTest {

    private PassiveEffectRegistry registry;
    
    @BeforeEach
    void setUp() {
        PassiveEffectRegistry.reset();
        registry = PassiveEffectRegistry.get();
    }

    // ========== Registration Tests ==========

    @Nested
    @DisplayName("Registration")
    class RegistrationTests {

        @Test
        @DisplayName("registers handler successfully")
        void registersHandler() {
            PassiveEffectHandler handler = new MockEffectHandler();
            registry.register("test-effect", handler);
            
            assertTrue(registry.hasHandler("test-effect"));
            assertEquals(handler, registry.getHandler("test-effect"));
            assertEquals(1, registry.getHandlerCount());
        }

        @Test
        @DisplayName("throws on duplicate registration")
        void throwsOnDuplicate() {
            PassiveEffectHandler handler1 = new MockEffectHandler();
            PassiveEffectHandler handler2 = new MockEffectHandler();
            
            registry.register("test-effect", handler1);
            
            assertThrows(IllegalStateException.class, () ->
                    registry.register("test-effect", handler2));
        }

        @Test
        @DisplayName("registerOrReplace replaces existing handler")
        void registerOrReplaceWorks() {
            PassiveEffectHandler handler1 = new MockEffectHandler();
            PassiveEffectHandler handler2 = new MockEffectHandler();
            
            registry.register("test-effect", handler1);
            registry.registerOrReplace("test-effect", handler2);
            
            assertEquals(handler2, registry.getHandler("test-effect"));
            assertEquals(1, registry.getHandlerCount());
        }

        @Test
        @DisplayName("multiple handlers can be registered")
        void multipleHandlers() {
            registry.register("effect-1", new MockEffectHandler());
            registry.register("effect-2", new MockEffectHandler());
            registry.register("effect-3", new MockEffectHandler());
            
            assertEquals(3, registry.getHandlerCount());
            assertTrue(registry.hasHandler("effect-1"));
            assertTrue(registry.hasHandler("effect-2"));
            assertTrue(registry.hasHandler("effect-3"));
        }
    }

    // ========== Query Tests ==========

    @Nested
    @DisplayName("Queries")
    class QueryTests {

        @Test
        @DisplayName("hasHandler returns false for missing")
        void hasHandlerReturnsFalseForMissing() {
            assertFalse(registry.hasHandler("nonexistent"));
        }

        @Test
        @DisplayName("getHandler returns null for missing")
        void getHandlerReturnsNullForMissing() {
            assertNull(registry.getHandler("nonexistent"));
        }

        @Test
        @DisplayName("getHandlerCount returns zero initially")
        void countZeroInitially() {
            assertEquals(0, registry.getHandlerCount());
        }
    }

    // ========== Reset Tests ==========

    @Nested
    @DisplayName("Reset")
    class ResetTests {

        @Test
        @DisplayName("reset clears all handlers")
        void resetClearsHandlers() {
            registry.register("effect-1", new MockEffectHandler());
            registry.register("effect-2", new MockEffectHandler());
            
            assertEquals(2, registry.getHandlerCount());
            
            PassiveEffectRegistry.reset();
            registry = PassiveEffectRegistry.get();
            
            assertEquals(0, registry.getHandlerCount());
        }
    }

    // ========== Mock Handler ==========

    private static class MockEffectHandler implements PassiveEffectHandler {
        
        @Override
        public void apply(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect) {
            // No-op for testing
        }

        @Override
        public void remove(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect) {
            // No-op for testing
        }

        @Override
        @Nonnull
        public String getTooltipText(@Nonnull PassiveNodeEffect effect) {
            return "Mock tooltip";
        }
    }
}
