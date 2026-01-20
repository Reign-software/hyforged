package reign.software.hyforged.progression.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Event dispatched when a level-up notification should be shown to the player.
 * <p>
 * This event is emitted immediately when a level-up occurs (not aggregated like
 * XP notifications). Level-up is a significant milestone that deserves
 * immediate feedback.
 * <p>
 * UI systems can listen for this event to display level-up effects, play sounds,
 * or show milestone congratulations.
 * <p>
 * Note: This is a notification event - rewards have already been applied before
 * this event is dispatched.
 */
public record LevelUpNotificationEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull LevelType levelType,
        @Nullable String classId,
        int oldLevel,
        int newLevel,
        @Nonnull List<Integer> levelsGained,
        int rewardsGranted
) implements IEvent<Void> {
    
    /**
     * Type of level that was gained.
     */
    public enum LevelType {
        /** Character level (general progression) */
        CHARACTER,
        /** Class level (class-specific progression) */
        CLASS
    }
    
    /**
     * Create a character level-up notification.
     */
    public static LevelUpNotificationEvent character(
            @Nonnull Ref<EntityStore> entityRef,
            int oldLevel,
            int newLevel,
            @Nonnull List<Integer> levelsGained,
            int passivePointsGranted
    ) {
        return new LevelUpNotificationEvent(
                entityRef,
                LevelType.CHARACTER,
                null,
                oldLevel,
                newLevel,
                levelsGained,
                passivePointsGranted
        );
    }
    
    /**
     * Create a class level-up notification.
     */
    public static LevelUpNotificationEvent classLevel(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull String classId,
            int oldLevel,
            int newLevel,
            @Nonnull List<Integer> levelsGained,
            int classPassivePointsGranted
    ) {
        return new LevelUpNotificationEvent(
                entityRef,
                LevelType.CLASS,
                classId,
                oldLevel,
                newLevel,
                levelsGained,
                classPassivePointsGranted
        );
    }
    
    /**
     * Check if this is a character level-up.
     */
    public boolean isCharacterLevel() {
        return levelType == LevelType.CHARACTER;
    }
    
    /**
     * Check if this is a class level-up.
     */
    public boolean isClassLevel() {
        return levelType == LevelType.CLASS;
    }
    
    /**
     * Get the number of levels gained in this level-up.
     */
    public int getLevelCount() {
        return levelsGained.size();
    }
    
    /**
     * Check if multiple levels were gained at once.
     */
    public boolean isMultiLevelUp() {
        return levelsGained.size() > 1;
    }
}
