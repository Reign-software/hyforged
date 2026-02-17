package reign.software.hyforged.combat.ailment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.logging.Level;

/**
 * Registry for ailment definitions.
 * <p>
 * Maps element tags to their corresponding ailments. Each element can have
 * at most one ailment, but multiple elements can share the same EntityEffect.
 */
public final class AilmentRegistry {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final AilmentRegistry INSTANCE = new AilmentRegistry();
    
    /** Ailments by ID */
    private final Map<String, AilmentDefinition> ailmentsById = new ConcurrentHashMap<>();
    
    /** Ailments by element tag (primary lookup) */
    private final Map<String, AilmentDefinition> ailmentsByElement = new ConcurrentHashMap<>();
    
    private AilmentRegistry() {
    }
    
    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static AilmentRegistry get() {
        return INSTANCE;
    }
    
    /**
     * Register an ailment definition.
     * 
     * @param ailment The ailment definition
     * @throws IllegalStateException if an ailment for this element is already registered
     */
    public void register(@Nonnull AilmentDefinition ailment) {
        Objects.requireNonNull(ailment, "ailment cannot be null");
        
        if (ailmentsById.containsKey(ailment.id())) {
            throw new IllegalStateException("Ailment already registered: " + ailment.id());
        }
        
        if (ailmentsByElement.containsKey(ailment.elementTag())) {
            throw new IllegalStateException("Ailment for element already registered: " + ailment.elementTag());
        }
        
        ailmentsById.put(ailment.id(), ailment);
        ailmentsByElement.put(ailment.elementTag(), ailment);
        
        LOGGER.at(Level.FINE).log("Registered ailment: %s for element %s", ailment.id(), ailment.elementTag());
    }
    
    /**
     * Get an ailment by ID.
     * 
     * @param id The ailment ID
     * @return The ailment definition, or null if not found
     */
    @Nullable
    public AilmentDefinition getById(@Nonnull String id) {
        return ailmentsById.get(id);
    }
    
    /**
     * Get an ailment by element tag.
     * 
     * @param elementTag The element tag (e.g., "fire", "ice")
     * @return The ailment definition, or null if no ailment for this element
     */
    @Nullable
    public AilmentDefinition getByElement(@Nonnull String elementTag) {
        return ailmentsByElement.get(elementTag);
    }
    
    /**
     * Check if an element has an associated ailment.
     * 
     * @param elementTag The element tag
     * @return true if there is an ailment for this element
     */
    public boolean hasAilmentForElement(@Nonnull String elementTag) {
        return ailmentsByElement.containsKey(elementTag);
    }
    
    /**
     * Get all registered ailments.
     * 
     * @return Unmodifiable collection of all ailments
     */
    @Nonnull
    public Collection<AilmentDefinition> getAll() {
        return Collections.unmodifiableCollection(ailmentsById.values());
    }
    
    /**
     * Get all element tags that have ailments.
     * 
     * @return Unmodifiable set of element tags
     */
    @Nonnull
    public Set<String> getAilmentElements() {
        return Collections.unmodifiableSet(ailmentsByElement.keySet());
    }
    
    /**
     * Clear all registered ailments.
     * <p>
     * Used for testing or reload.
     */
    public void clear() {
        ailmentsById.clear();
        ailmentsByElement.clear();
    }
    
    /**
     * Get the count of registered ailments.
     */
    public int size() {
        return ailmentsById.size();
    }
}
