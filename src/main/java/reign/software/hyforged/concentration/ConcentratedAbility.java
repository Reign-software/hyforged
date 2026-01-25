package reign.software.hyforged.concentration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Represents a concentrated ability bound to an entity.
 *
 * @param abilityId  Namespaced ability ID.
 * @param cost       Concentration cost required to maintain.
 * @param priority   Higher priority abilities are re-enabled first.
 * @param enabled    Whether the ability is currently enabled.
 * @param onDisable  Callback invoked when the ability is disabled.
 * @param onEnable   Callback invoked when the ability is re-enabled.
 */
public record ConcentratedAbility(
        @Nonnull String abilityId,
        int cost,
        int priority,
        boolean enabled,
        @Nullable Runnable onDisable,
        @Nullable Runnable onEnable
) {
    public ConcentratedAbility {
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        if (abilityId.isBlank()) {
            throw new IllegalArgumentException("abilityId cannot be blank");
        }
        if (cost < 0) {
            cost = 0;
        }
    }

    @Nonnull
    public ConcentratedAbility withEnabled(boolean enabled) {
        return new ConcentratedAbility(abilityId, cost, priority, enabled, onDisable, onEnable);
    }

    @Nonnull
    public ConcentratedAbility withPriority(int priority) {
        return new ConcentratedAbility(abilityId, cost, priority, enabled, onDisable, onEnable);
    }

    @Nonnull
    public ConcentratedAbility withCost(int cost) {
        return new ConcentratedAbility(abilityId, Math.max(0, cost), priority, enabled, onDisable, onEnable);
    }

    @Nonnull
    public ConcentratedAbility withCallbacks(@Nullable Runnable onDisable, @Nullable Runnable onEnable) {
        return new ConcentratedAbility(abilityId, cost, priority, enabled, onDisable, onEnable);
    }
}
