package reign.software.hyforged.stats.condition;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Interface for conditional modifier evaluation.
 * <p>
 * Conditions determine whether a modifier should be applied based on
 * the entity's current state. Conditions are evaluated at stat query time,
 * not at modifier application time.
 * <p>
 * Implementations should be stateless and thread-safe.
 *
 * @see QueryContext
 */
@FunctionalInterface
public interface ModifierCondition {

    /**
     * Evaluate whether this condition is met for the given entity and context.
     *
     * @param entityRef Reference to the entity being evaluated
     * @param context The query context containing state information
     * @return true if the condition is met and the modifier should apply
     */
    boolean evaluate(@Nonnull Ref<EntityStore> entityRef, @Nonnull QueryContext context);
    
    /**
     * Create an always-true condition.
     *
     * @return A condition that always evaluates to true
     */
    static ModifierCondition always() {
        return (entityRef, context) -> true;
    }
    
    /**
     * Create an always-false condition.
     *
     * @return A condition that always evaluates to false
     */
    static ModifierCondition never() {
        return (entityRef, context) -> false;
    }
    
    /**
     * Combine this condition with another using AND logic.
     *
     * @param other The other condition
     * @return A new condition that is true only if both conditions are true
     */
    default ModifierCondition and(@Nonnull ModifierCondition other) {
        return (entityRef, context) -> 
            this.evaluate(entityRef, context) && other.evaluate(entityRef, context);
    }
    
    /**
     * Combine this condition with another using OR logic.
     *
     * @param other The other condition
     * @return A new condition that is true if either condition is true
     */
    default ModifierCondition or(@Nonnull ModifierCondition other) {
        return (entityRef, context) -> 
            this.evaluate(entityRef, context) || other.evaluate(entityRef, context);
    }
    
    /**
     * Negate this condition.
     *
     * @return A new condition that is true when this condition is false
     */
    default ModifierCondition negate() {
        return (entityRef, context) -> !this.evaluate(entityRef, context);
    }
}
