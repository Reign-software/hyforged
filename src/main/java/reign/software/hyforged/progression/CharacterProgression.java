package reign.software.hyforged.progression;

import javax.annotation.Nonnull;

/**
 * Immutable snapshot of a player's character progression state.
 * <p>
 * Character progression tracks overall player level (1-100) and XP.
 * Character level grants general passive points (level - 1).
 *
 * @param level Current character level (1-100)
 * @param currentXp Current XP toward next level
 * @param xpToNext XP required to reach next level (computed from curve)
 */
public record CharacterProgression(
    int level,
    long currentXp,
    long xpToNext
) {
    /** Minimum character level */
    public static final int MIN_LEVEL = 1;
    
    /** Maximum character level */
    public static final int MAX_LEVEL = 100;

    public CharacterProgression {
        if (level < MIN_LEVEL) {
            level = MIN_LEVEL;
        }
        if (level > MAX_LEVEL) {
            level = MAX_LEVEL;
        }
        if (currentXp < 0) {
            currentXp = 0;
        }
        if (xpToNext < 0) {
            xpToNext = 0;
        }
    }

    /**
     * Create initial character progression at level 1 with 0 XP.
     *
     * @param xpToNext XP required to reach level 2
     * @return Initial character progression
     */
    @Nonnull
    public static CharacterProgression initial(long xpToNext) {
        return new CharacterProgression(MIN_LEVEL, 0, xpToNext);
    }

    /**
     * Get the number of general passive points granted by character level.
     * Formula: level - 1 (so level 1 grants 0, level 100 grants 99)
     *
     * @return General passive points available
     */
    public int getGeneralPassivePoints() {
        return level - 1;
    }

    /**
     * Get progress percentage toward next level (0.0 - 1.0).
     *
     * @return Progress percentage
     */
    public double getProgressPercent() {
        if (xpToNext <= 0) {
            return level >= MAX_LEVEL ? 1.0 : 0.0;
        }
        return Math.min(1.0, (double) currentXp / xpToNext);
    }

    /**
     * Check if character has reached max level.
     *
     * @return true if at max level
     */
    public boolean isMaxLevel() {
        return level >= MAX_LEVEL;
    }

    /**
     * Create a new progression with updated values.
     *
     * @param newLevel New level
     * @param newCurrentXp New current XP
     * @param newXpToNext New XP to next level
     * @return New CharacterProgression instance
     */
    @Nonnull
    public CharacterProgression withValues(int newLevel, long newCurrentXp, long newXpToNext) {
        return new CharacterProgression(newLevel, newCurrentXp, newXpToNext);
    }

    @Override
    public String toString() {
        return String.format("CharacterProgression{level=%d, xp=%d/%d (%.1f%%), passivePoints=%d}",
            level, currentXp, xpToNext, getProgressPercent() * 100, getGeneralPassivePoints());
    }
}
