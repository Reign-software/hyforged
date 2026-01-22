package reign.software.hyforged.stats.component;

import reign.software.hyforged.stats.condition.ModifierCondition;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

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
    @Nonnull HyforgedModifier modifier,
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
            @Nonnull HyforgedModifier modifier,
            @Nonnull ModifierCondition condition
    ) {
        return new ConditionalStatModifier(modifier, condition);
    }
    
    /**
     * Create an unconditional modifier (always applies).
     *
     * @param modifier The base modifier
     */
    public static ConditionalStatModifier unconditional(@Nonnull HyforgedModifier modifier) {
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
        return modifier.getSourceId();
    }
    
    @Nonnull
    public HyforgedModifier.SourceType sourceType() {
        return modifier.getSourceType();
    }
    
    @Nonnull
    public HyforgedModifier.StackType modifierType() {
        return modifier.getStackType();
    }
    
    public int targetStatIndex() {
        return modifier.getTargetStatIndex();
    }
    
    /**
     * Get the target tag index from the underlying modifier.
     * @return The tag index, or {@link HyforgedModifier#NO_TAG} if not targeting a tag
     */
    public int targetTagIndex() {
        return modifier.getTargetTagIndex();
    }
    
    public int value() {
        return modifier.getAmount();
    }
    
    public long expirationTick() {
        return modifier.getExpirationTick();
    }
    
    public int priority() {
        return modifier.getPriority();
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
