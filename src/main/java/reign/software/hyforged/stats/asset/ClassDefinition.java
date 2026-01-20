package reign.software.hyforged.stats.asset;

import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Domain object representing a character class definition.
 * <p>
 * Classes define the base ability score distribution for players,
 * weapon tag families for class XP activation, and per-level ability rewards.
 * This is a lightweight immutable value object used at runtime.
 *
 * @param id The class identifier (e.g., "hyforged:warrior")
 * @param displayName The localized display name
 * @param description Description text for tooltips
 * @param abilityScores Map of stat ID to base value for ability scores
 * @param weaponTagFamilies Set of weapon tags that activate this class for XP (e.g., "weapon:sword")
 * @param levelRewards Map of level to ability score bonuses granted at that level
 */
public record ClassDefinition(
    @Nonnull String id,
    @Nonnull String displayName,
    @Nonnull String description,
    @Nonnull Map<StatId, Integer> abilityScores,
    @Nonnull Set<String> weaponTagFamilies,
    @Nonnull Map<Integer, Map<StatId, Integer>> levelRewards
) {
    
    /** Default ability score value for stats not specified in class definition */
    public static final int DEFAULT_ABILITY_SCORE = 1;
    
    public ClassDefinition {
        // Defensive copies
        abilityScores = Collections.unmodifiableMap(new HashMap<>(abilityScores));
        weaponTagFamilies = Collections.unmodifiableSet(new HashSet<>(weaponTagFamilies));
        
        // Deep copy of level rewards
        Map<Integer, Map<StatId, Integer>> rewardsCopy = new HashMap<>();
        for (Map.Entry<Integer, Map<StatId, Integer>> entry : levelRewards.entrySet()) {
            rewardsCopy.put(entry.getKey(), Collections.unmodifiableMap(new HashMap<>(entry.getValue())));
        }
        levelRewards = Collections.unmodifiableMap(rewardsCopy);
    }
    
    /**
     * Legacy constructor for backward compatibility.
     */
    public ClassDefinition(
        @Nonnull String id,
        @Nonnull String displayName,
        @Nonnull String description,
        @Nonnull Map<StatId, Integer> abilityScores
    ) {
        this(id, displayName, description, abilityScores, Collections.emptySet(), Collections.emptyMap());
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
    
    /**
     * Check if this class matches any of the given weapon tags.
     *
     * @param weaponTags Set of tags from the player's main-hand weapon
     * @return true if any tag matches this class's weapon tag families
     */
    public boolean matchesWeaponTags(@Nonnull Set<String> weaponTags) {
        if (weaponTagFamilies.isEmpty()) {
            return false;
        }
        for (String tag : weaponTags) {
            if (weaponTagFamilies.contains(tag)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the ability score bonuses for a specific class level.
     *
     * @param level The class level (1-20)
     * @return Map of ability score bonuses, or empty map if none defined
     */
    @Nonnull
    public Map<StatId, Integer> getLevelReward(int level) {
        Map<StatId, Integer> reward = levelRewards.get(level);
        return reward != null ? reward : Collections.emptyMap();
    }
    
    /**
     * Get cumulative ability score bonuses from level 1 to the specified level.
     *
     * @param upToLevel The maximum level to accumulate (inclusive)
     * @return Map of accumulated ability score bonuses
     */
    @Nonnull
    public Map<StatId, Integer> getCumulativeLevelRewards(int upToLevel) {
        Map<StatId, Integer> cumulative = new HashMap<>();
        for (int level = 1; level <= upToLevel; level++) {
            Map<StatId, Integer> reward = levelRewards.get(level);
            if (reward != null) {
                for (Map.Entry<StatId, Integer> entry : reward.entrySet()) {
                    cumulative.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }
        return cumulative;
    }
    
    /**
     * Check if this class has weapon tag families defined.
     *
     * @return true if weapon tag families are configured
     */
    public boolean hasWeaponTagFamilies() {
        return !weaponTagFamilies.isEmpty();
    }
    
    /**
     * Check if this class has level rewards defined.
     *
     * @return true if any level rewards are configured
     */
    public boolean hasLevelRewards() {
        return !levelRewards.isEmpty();
    }
}
