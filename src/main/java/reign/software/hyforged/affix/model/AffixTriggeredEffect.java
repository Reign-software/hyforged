package reign.software.hyforged.affix.model;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Defines a trigger/effect pair for an affix proc.
 */
public record AffixTriggeredEffect(
        @Nonnull AffixTrigger trigger,
        @Nonnull AffixEffect effect,
        @Nonnull String stackBehavior,
        int maxStacks,
        float cooldownSeconds,
        @Nonnull String sharedCooldownGroup
) {

    public static final String STACK_BEHAVIOR_INDEPENDENT = "independent";
    public static final String STACK_BEHAVIOR_SHARED = "shared";

    public AffixTriggeredEffect {
        Objects.requireNonNull(trigger, "trigger cannot be null");
        Objects.requireNonNull(effect, "effect cannot be null");

        if (stackBehavior == null || stackBehavior.isBlank()) {
            stackBehavior = STACK_BEHAVIOR_INDEPENDENT;
        }
        if (maxStacks <= 0) {
            maxStacks = 1;
        }
        if (cooldownSeconds < 0) {
            cooldownSeconds = 0;
        }
        sharedCooldownGroup = sharedCooldownGroup != null ? sharedCooldownGroup : "";
    }

    public boolean isSharedCooldown() {
        return STACK_BEHAVIOR_SHARED.equalsIgnoreCase(stackBehavior);
    }
}
