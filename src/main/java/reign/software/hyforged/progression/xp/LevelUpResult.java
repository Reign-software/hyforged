package reign.software.hyforged.progression.xp;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * Result of processing XP and checking for level-ups.
 * <p>
 * Contains information about levels gained, which can be used to:
 * - Emit level-up events
 * - Apply rewards (passive points, ability bonuses)
 * - Trigger UI notifications
 *
 * @param levelsGained list of levels gained (e.g., [5, 6, 7] if went from 4 to 7)
 * @param oldLevel the level before XP was added
 * @param newLevel the level after XP was added
 * @param totalXp the total XP after addition
 */
public record LevelUpResult(
        @Nonnull List<Integer> levelsGained,
        int oldLevel,
        int newLevel,
        long totalXp
) {
    /**
     * Check if any levels were gained.
     *
     * @return true if at least one level was gained
     */
    public boolean leveledUp() {
        return !levelsGained.isEmpty();
    }
    
    /**
     * Get the number of levels gained.
     *
     * @return count of levels gained
     */
    public int getLevelCount() {
        return levelsGained.size();
    }
    
    /**
     * Create a result for no level-up.
     *
     * @param level current level
     * @param totalXp current total XP
     * @return result with no levels gained
     */
    @Nonnull
    public static LevelUpResult noChange(int level, long totalXp) {
        return new LevelUpResult(Collections.emptyList(), level, level, totalXp);
    }
}
