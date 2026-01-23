package reign.software.hyforged.affix.model;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Defines when a triggered affix effect should activate.
 * <p>
 * All values are data-driven and use basis points for chance (10000 = 100%).
 */
public record AffixTrigger(
        @Nonnull String type,
        int chance,
        @Nonnull List<String> damageCauses,
        @Nonnull List<String> targetTags,
        int minDamage,
        float intervalSeconds,
        boolean requireCombat,
        @Nonnull List<String> interactionTypes
) {

    public static final int DEFAULT_CHANCE_BPS = 10000;

    public AffixTrigger {
        Objects.requireNonNull(type, "type cannot be null");
        if (type.isBlank()) {
            throw new IllegalArgumentException("type cannot be blank");
        }
        if (chance < 0) {
            throw new IllegalArgumentException("chance cannot be negative: " + chance);
        }
        if (minDamage < 0) {
            throw new IllegalArgumentException("minDamage cannot be negative: " + minDamage);
        }
        if (intervalSeconds < 0) {
            throw new IllegalArgumentException("intervalSeconds cannot be negative: " + intervalSeconds);
        }

        damageCauses = damageCauses != null ? List.copyOf(damageCauses) : Collections.emptyList();
        targetTags = targetTags != null ? List.copyOf(targetTags) : Collections.emptyList();
        interactionTypes = interactionTypes != null ? List.copyOf(interactionTypes) : Collections.emptyList();
    }

    public boolean hasDamageCauseFilter() {
        return !damageCauses.isEmpty();
    }

    public boolean hasTargetTags() {
        return !targetTags.isEmpty();
    }

    public boolean hasInteractionTypes() {
        return !interactionTypes.isEmpty();
    }
}
