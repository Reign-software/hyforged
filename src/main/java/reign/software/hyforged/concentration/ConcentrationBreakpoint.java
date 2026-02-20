package reign.software.hyforged.concentration;

import javax.annotation.Nonnull;

/**
 * Represents a single breakpoint in the concentration bar.
 * Each breakpoint corresponds to a concentrated ability's cost,
 * ordered by priority (highest first).
 *
 * @param abilityId      The ability identifier
 * @param cost           The concentration cost of this ability
 * @param cumulativeCost The total concentration cost including this and all higher-priority abilities
 * @param enabled        Whether this ability is currently enabled
 */
public record ConcentrationBreakpoint(
        @Nonnull String abilityId,
        int cost,
        int cumulativeCost,
        boolean enabled
) {}
