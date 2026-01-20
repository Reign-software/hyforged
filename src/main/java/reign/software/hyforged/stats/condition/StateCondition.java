package reign.software.hyforged.stats.condition;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Condition that checks for specific status effects.
 * <p>
 * Uses String-based effect IDs for moddability. Convention: use namespaced IDs
 * like "hyforged:bleeding" or "mymod:frozen".
 * <p>
 * Examples:
 * - "while bleeding" → StateCondition.whileAffectedBy(StatusEffects.BLEEDING)
 * - "when poisoned" → StateCondition.whileAffectedBy("mymod:custom_poison")
 */
public class StateCondition implements ModifierCondition {

    @Nonnull
    private final String requiredEffectId;
    private final boolean requirePresent;

    /**
     * Create a condition that checks for a status effect.
     *
     * @param requiredEffectId The status effect ID to check for (e.g., "hyforged:bleeding")
     * @param requirePresent If true, condition passes when effect is present;
     *                       if false, condition passes when effect is absent
     */
    public StateCondition(@Nonnull String requiredEffectId, boolean requirePresent) {
        this.requiredEffectId = Objects.requireNonNull(requiredEffectId, "requiredEffectId cannot be null");
        this.requirePresent = requirePresent;
    }

    /**
     * Create a condition that requires a status effect to be present.
     *
     * @param effectId The required status effect ID (e.g., "hyforged:bleeding")
     * @return A new StateCondition
     */
    public static StateCondition whileAffectedBy(@Nonnull String effectId) {
        return new StateCondition(effectId, true);
    }

    /**
     * Create a condition that requires a status effect to be absent.
     *
     * @param effectId The status effect ID that must not be present
     * @return A new StateCondition
     */
    public static StateCondition whileNotAffectedBy(@Nonnull String effectId) {
        return new StateCondition(effectId, false);
    }

    @Override
    public boolean evaluate(@Nonnull Ref<EntityStore> entityRef, @Nonnull QueryContext context) {
        boolean hasEffect = context.hasStatusEffect(requiredEffectId);
        return requirePresent == hasEffect;
    }

    @Nonnull
    public String getRequiredEffectId() {
        return requiredEffectId;
    }

    public boolean isRequirePresent() {
        return requirePresent;
    }

    @Override
    public String toString() {
        return String.format("StateCondition[%s %s]",
            requirePresent ? "while" : "while not",
            requiredEffectId);
    }
}
