package reign.software.hyforged.passive.effect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Registry for passive effect handlers.
 * <p>
 * Handlers are registered by effect type (e.g., "stat-modifier", "spell-grant")
 * and looked up when effects need to be applied or removed.
 */
public final class PassiveEffectRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(PassiveEffectRegistry.class.getName());
    
    private static PassiveEffectRegistry instance;
    
    private final Map<String, PassiveEffectHandler> handlers = new HashMap<>();
    
    private PassiveEffectRegistry() {
    }
    
    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static synchronized PassiveEffectRegistry get() {
        if (instance == null) {
            instance = new PassiveEffectRegistry();
        }
        return instance;
    }
    
    /**
     * Reset the registry (for testing).
     */
    public static synchronized void reset() {
        instance = new PassiveEffectRegistry();
    }
    
    /**
     * Register a handler for an effect type.
     *
     * @param effectType The effect type (e.g., "stat-modifier")
     * @param handler The handler
     * @throws IllegalStateException if a handler is already registered for this type
     */
    public void register(@Nonnull String effectType, @Nonnull PassiveEffectHandler handler) {
        if (handlers.containsKey(effectType)) {
            throw new IllegalStateException("Handler already registered for effect type: " + effectType);
        }
        handlers.put(effectType, handler);
        LOGGER.info("Registered passive effect handler for type: " + effectType);
    }
    
    /**
     * Register a handler for an effect type, replacing any existing handler.
     *
     * @param effectType The effect type
     * @param handler The handler
     */
    public void registerOrReplace(@Nonnull String effectType, @Nonnull PassiveEffectHandler handler) {
        handlers.put(effectType, handler);
        LOGGER.info("Registered passive effect handler for type: " + effectType);
    }
    
    /**
     * Get the handler for an effect type.
     *
     * @param effectType The effect type
     * @return The handler, or null if not registered
     */
    @Nullable
    public PassiveEffectHandler getHandler(@Nonnull String effectType) {
        return handlers.get(effectType);
    }
    
    /**
     * Check if a handler is registered for an effect type.
     *
     * @param effectType The effect type
     * @return true if a handler is registered
     */
    public boolean hasHandler(@Nonnull String effectType) {
        return handlers.containsKey(effectType);
    }
    
    /**
     * Get the number of registered handlers.
     */
    public int getHandlerCount() {
        return handlers.size();
    }
}
