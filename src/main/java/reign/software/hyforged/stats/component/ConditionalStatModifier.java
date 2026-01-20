package reign.software.hyforged.stats.component;

import reign.software.hyforged.stats.condition.ModifierCondition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Extended stat modifier that includes an optional condition for context-aware evaluation.
 * <p>
 * This record extends the base modifier concept with conditional logic. When a condition
 * is present, the modifier only applies if the condition evaluates to true for the
 * given entity and query context.
 * <p>
 * This is pure data - no behavior, following ECS principles.
 *
 * @param modifier The base stat modifier
 * @param condition Optional condition; null means always applies
 */
public record ConditionalStatModifier(
    @Nonnull StatModifier modifier,
    @Nullable ModifierCondition condition
) {
    
    public ConditionalStatModifier {
        Objects.requireNonNull(modifier, "modifier cannot be null");
    }
    
    /**
     * Create a conditional modifier with a condition.
     *
     * @param modifier The base modifier
     * @param condition The condition for when this modifier applies
     */
    public static ConditionalStatModifier conditional(
            @Nonnull StatModifier modifier,
            @Nonnull ModifierCondition condition
    ) {
        return new ConditionalStatModifier(modifier, condition);
    }
    
    /**
     * Create an unconditional modifier (always applies).
     *
     * @param modifier The base modifier
     */
    public static ConditionalStatModifier unconditional(@Nonnull StatModifier modifier) {
        return new ConditionalStatModifier(modifier, null);
    }
    
    /**
     * Check if this modifier has a condition.
     *
     * @return true if a condition is present
     */
    public boolean hasCondition() {
        return condition != null;
    }
    
    /**
     * Check if this modifier is unconditional (always applies).
     *
     * @return true if no condition is present
     */
    public boolean isUnconditional() {
        return condition == null;
    }
    
    // Delegate methods for convenience
    
    @Nonnull
    public String sourceId() {
        return modifier.sourceId();
    }
    
    @Nonnull
    public ModifierSource sourceType() {
        return modifier.sourceType();
    }
    
    @Nonnull
    public ModifierType modifierType() {
        return modifier.modifierType();
    }
    
    public int targetStatIndex() {
        return modifier.targetStatIndex();
    }
    
    /**
     * Get the target tag index from the underlying modifier.
     * @return The tag index, or {@link StatModifier#NO_TAG} if not targeting a tag
     */
    public int targetTagIndex() {
        return modifier.targetTagIndex();
    }
    
    public int value() {
        return modifier.value();
    }
    
    public long expirationTick() {
        return modifier.expirationTick();
    }
    
    public int priority() {
        return modifier.priority();
    }
    
    public boolean isTagModifier() {
        return modifier.isTagModifier();
    }
    
    public boolean isExpired(long currentTick) {
        return modifier.isExpired(currentTick);
    }
    
    public boolean isPermanent() {
        return modifier.isPermanent();
    }
}
