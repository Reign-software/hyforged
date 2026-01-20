package reign.software.hyforged.progression.xp;

import reign.software.hyforged.progression.CharacterProgression;
import reign.software.hyforged.progression.ClassProgression;
import reign.software.hyforged.progression.XPCurve;
import reign.software.hyforged.progression.asset.XPCurveRegistry;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for processing level-ups from XP gains.
 * <p>
 * Handles:
 * - Calculating levels gained from XP addition
 * - Respecting level caps
 * - Supporting multi-level gains from single XP award
 * <p>
 * This class is stateless and provides static methods.
 */
public final class LevelUpProcessor {
    
    private static final Logger LOGGER = Logger.getLogger(LevelUpProcessor.class.getName());
    
    private LevelUpProcessor() {
        // Utility class
    }
    
    /**
     * Process character XP addition and calculate level-ups.
     * <p>
     * Does not modify the component - returns the result for the caller to apply.
     *
     * @param currentLevel current character level
     * @param currentXp current character XP
     * @param xpToAdd XP being added
     * @return result containing new level, new XP, and levels gained
     */
    @Nonnull
    public static LevelUpResult processCharacterXp(int currentLevel, long currentXp, long xpToAdd) {
        if (xpToAdd <= 0) {
            return LevelUpResult.noChange(currentLevel, currentXp);
        }
        
        // Cap check
        if (currentLevel >= CharacterProgression.MAX_LEVEL) {
            return LevelUpResult.noChange(currentLevel, currentXp);
        }
        
        XPCurve curve = XPCurveRegistry.get().getCharacterCurve();
        long newXp = currentXp + xpToAdd;
        int oldLevel = currentLevel;
        List<Integer> levelsGained = new ArrayList<>();
        
        // Process level-ups
        while (currentLevel < CharacterProgression.MAX_LEVEL) {
            long xpForNext = curve.getXpForLevel(currentLevel + 1);
            if (xpForNext <= 0 || newXp < xpForNext) {
                break;
            }
            currentLevel++;
            levelsGained.add(currentLevel);
            
            LOGGER.fine(String.format("Character level-up: %d -> %d (XP: %d/%d)",
                    currentLevel - 1, currentLevel, newXp, xpForNext));
        }
        
        return new LevelUpResult(levelsGained, oldLevel, currentLevel, newXp);
    }
    
    /**
     * Process class XP addition and calculate level-ups.
     * <p>
     * Does not modify the component - returns the result for the caller to apply.
     *
     * @param classId the class ID
     * @param currentLevel current class level
     * @param currentXp current class XP
     * @param xpToAdd XP being added
     * @return result containing new level, new XP, and levels gained
     */
    @Nonnull
    public static LevelUpResult processClassXp(String classId, int currentLevel, long currentXp, long xpToAdd) {
        if (xpToAdd <= 0) {
            return LevelUpResult.noChange(currentLevel, currentXp);
        }
        
        // Cap check
        if (currentLevel >= ClassProgression.MAX_LEVEL) {
            return LevelUpResult.noChange(currentLevel, currentXp);
        }
        
        XPCurve curve = XPCurveRegistry.get().getClassCurve();
        long newXp = currentXp + xpToAdd;
        int oldLevel = currentLevel;
        List<Integer> levelsGained = new ArrayList<>();
        
        // Process level-ups
        while (currentLevel < ClassProgression.MAX_LEVEL) {
            long xpForNext = curve.getXpForLevel(currentLevel + 1);
            if (xpForNext <= 0 || newXp < xpForNext) {
                break;
            }
            currentLevel++;
            levelsGained.add(currentLevel);
            
            LOGGER.fine(String.format("Class level-up: %s %d -> %d (XP: %d/%d)",
                    classId, currentLevel - 1, currentLevel, newXp, xpForNext));
        }
        
        return new LevelUpResult(levelsGained, oldLevel, currentLevel, newXp);
    }
    
    /**
     * Calculate passive points granted for character levels.
     * <p>
     * Formula: 1 passive point per level, starting from level 2.
     * Level 1 = 0 points, Level 2 = 1 point, etc.
     *
     * @param levelsGained list of levels gained
     * @return total passive points granted
     */
    public static int calculatePassivePointsForLevels(@Nonnull List<Integer> levelsGained) {
        // Each level grants 1 passive point
        return levelsGained.size();
    }
    
    /**
     * Calculate class passive points granted for class levels.
     * <p>
     * Formula: 1 class passive point per class level (spec FR-3).
     * At class level 20, player has 20 class passive points total.
     *
     * @param levelsGained list of class levels gained
     * @return total class passive points granted (1 per level)
     */
    public static int calculateClassPassivePointsForLevels(@Nonnull List<Integer> levelsGained) {
        // Each class level grants 1 class passive point (per spec FR-3)
        return levelsGained.size();
    }
}
