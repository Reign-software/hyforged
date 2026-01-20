package reign.software.hyforged.progression.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Event emitted when a player's character level increases.
 * <p>
 * This event is dispatched after XP has been processed and level(s) have been gained.
 * Listeners can use this to:
 * - Display level-up UI/effects
 * - Grant rewards (passive points are tracked in component)
 * - Log the level-up
 *
 * @param entityRef the player entity reference
 * @param oldLevel the character level before the level-up
 * @param newLevel the character level after the level-up
 * @param levelsGained list of individual levels gained (for multi-level jumps)
 * @param passivePointsGranted total passive points granted from this level-up
 */
public record CharacterLevelUpEvent(
        @Nonnull Ref<EntityStore> entityRef,
        int oldLevel,
        int newLevel,
        @Nonnull List<Integer> levelsGained,
        int passivePointsGranted
) implements IEvent<Void> {
    
    /**
     * Check if multiple levels were gained at once.
     *
     * @return true if more than one level was gained
     */
    public boolean isMultiLevelUp() {
        return levelsGained.size() > 1;
    }
    
    /**
     * Get the number of levels gained.
     *
     * @return count of levels gained
     */
    public int getLevelCount() {
        return levelsGained.size();
    }
    
    @Override
    public String toString() {
        return String.format("CharacterLevelUpEvent{%d->%d, levels=%s, passivePoints=%d}",
                oldLevel, newLevel, levelsGained, passivePointsGranted);
    }
}
