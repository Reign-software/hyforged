package reign.software.hyforged.stats.condition;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Condition that checks for specific status effects.
 * <p>
 * Examples:
 * - "while bleeding"
 * - "when poisoned"
 * - "while stunned"
 */
public class StateCondition implements ModifierCondition {

    @Nonnull
    private final QueryContext.StatusEffect requiredEffect;
    private final boolean requirePresent;

    /**
     * Create a condition that checks for a status effect.
     *
     * @param requiredEffect The status effect to check for
     * @param requirePresent If true, condition passes when effect is present;
     *                       if false, condition passes when effect is absent
     */
    public StateCondition(@Nonnull QueryContext.StatusEffect requiredEffect, boolean requirePresent) {
        this.requiredEffect = Objects.requireNonNull(requiredEffect, "requiredEffect cannot be null");
        this.requirePresent = requirePresent;
    }

    /**
     * Create a condition that requires a status effect to be present.
     *
     * @param effect The required status effect
     * @return A new StateCondition
     */
    public static StateCondition whileAffectedBy(@Nonnull QueryContext.StatusEffect effect) {
        return new StateCondition(effect, true);
    }

    /**
     * Create a condition that requires a status effect to be absent.
     *
     * @param effect The status effect that must not be present
     * @return A new StateCondition
     */
    public static StateCondition whileNotAffectedBy(@Nonnull QueryContext.StatusEffect effect) {
        return new StateCondition(effect, false);
    }

    @Override
    public boolean evaluate(@Nonnull Ref<EntityStore> entityRef, @Nonnull QueryContext context) {
        boolean hasEffect = context.hasStatusEffect(requiredEffect);
        return requirePresent == hasEffect;
    }

    @Nonnull
    public QueryContext.StatusEffect getRequiredEffect() {
        return requiredEffect;
    }

    public boolean isRequirePresent() {
        return requirePresent;
    }

    @Override
    public String toString() {
        return String.format("StateCondition[%s %s]",
            requirePresent ? "while" : "while not",
            requiredEffect.name().toLowerCase());
    }
}
