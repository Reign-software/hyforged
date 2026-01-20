package reign.software.hyforged.stats.asset;

import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain object representing a character class definition.
 * <p>
 * Classes define the base ability score distribution for players.
 * This is a lightweight immutable value object used at runtime.
 *
 * @param id The class identifier (e.g., "hyforged:warrior")
 * @param displayName The localized display name
 * @param description Description text for tooltips
 * @param abilityScores Map of stat ID to base value for ability scores
 */
public record ClassDefinition(
    @Nonnull String id,
    @Nonnull String displayName,
    @Nonnull String description,
    @Nonnull Map<StatId, Integer> abilityScores
) {
    
    /** Default ability score value for stats not specified in class definition */
    public static final int DEFAULT_ABILITY_SCORE = 1;
    
    public ClassDefinition {
        // Defensive copy
        abilityScores = Collections.unmodifiableMap(new HashMap<>(abilityScores));
    }
    
    /**
     * Get the base value for a specific ability score.
     * Returns the default value if not specified.
     *
     * @param statId The stat ID to look up
     * @return The base value for this class, or DEFAULT_ABILITY_SCORE if not specified
     */
    public int getAbilityScore(@Nonnull StatId statId) {
        return abilityScores.getOrDefault(statId, DEFAULT_ABILITY_SCORE);
    }
    
    /**
     * Get the base value for a specific ability score by index.
     * Returns the default value if not specified.
     *
     * @param statId The stat ID to look up
     * @param defaultValue The default value if not specified
     * @return The base value for this class
     */
    public int getAbilityScoreOrDefault(@Nonnull StatId statId, int defaultValue) {
        return abilityScores.getOrDefault(statId, defaultValue);
    }
    
    /**
     * Check if this class defines a specific ability score.
     *
     * @param statId The stat ID to check
     * @return true if this class defines this ability score
     */
    public boolean hasAbilityScore(@Nonnull StatId statId) {
        return abilityScores.containsKey(statId);
    }
}
