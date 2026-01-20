package reign.software.hyforged.progression;

import javax.annotation.Nonnull;

/**
 * Immutable snapshot of a player's class progression state.
 * <p>
 * Class progression tracks level (1-20) and XP for a specific class.
 * Class level grants ability score bonuses (per class definition) and class passive points.
 *
 * @param classId The class identifier (e.g., "hyforged:warrior")
 * @param level Current class level (1-20)
 * @param currentXp Current XP toward next class level
 * @param xpToNext XP required to reach next class level (computed from curve)
 */
public record ClassProgression(
    @Nonnull String classId,
    int level,
    long currentXp,
    long xpToNext
) {
    /** Minimum class level */
    public static final int MIN_LEVEL = 1;
    
    /** Maximum class level */
    public static final int MAX_LEVEL = 20;

    public ClassProgression {
        if (classId == null) {
            classId = "";
        }
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
     * Create initial class progression at level 1 with 0 XP.
     *
     * @param classId The class identifier
     * @param xpToNext XP required to reach level 2
     * @return Initial class progression
     */
    @Nonnull
    public static ClassProgression initial(@Nonnull String classId, long xpToNext) {
        return new ClassProgression(classId, MIN_LEVEL, 0, xpToNext);
    }

    /**
     * Get the number of class passive points granted by class level.
     * Formula: class level (so level 1 grants 1, level 20 grants 20)
     *
     * @return Class passive points available
     */
    public int getClassPassivePoints() {
        return level;
    }

    /**
     * Get progress percentage toward next class level (0.0 - 1.0).
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
     * Check if class has reached max level.
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
     * @return New ClassProgression instance
     */
    @Nonnull
    public ClassProgression withValues(int newLevel, long newCurrentXp, long newXpToNext) {
        return new ClassProgression(classId, newLevel, newCurrentXp, newXpToNext);
    }

    @Override
    public String toString() {
        return String.format("ClassProgression{class=%s, level=%d, xp=%d/%d (%.1f%%), passivePoints=%d}",
            classId, level, currentXp, xpToNext, getProgressPercent() * 100, getClassPassivePoints());
    }
}
