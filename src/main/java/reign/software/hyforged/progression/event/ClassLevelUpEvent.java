package reign.software.hyforged.progression.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * Event emitted when a player's class level increases.
 * <p>
 * This event is dispatched after class XP has been processed and level(s) have been gained.
 * Listeners can use this to:
 * - Display level-up UI/effects
 * - Apply ability score bonuses (via HyforgedStatComponent)
 * - Grant class passive points
 * - Log the level-up
 *
 * @param entityRef the player entity reference
 * @param classId the class that leveled up
 * @param oldLevel the class level before the level-up
 * @param newLevel the class level after the level-up
 * @param levelsGained list of individual levels gained (for multi-level jumps)
 * @param abilityBonuses map of ability score ID to bonus granted (from levelRewards)
 * @param classPassivePointsGranted class-specific passive points granted
 */
public record ClassLevelUpEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String classId,
        int oldLevel,
        int newLevel,
        @Nonnull List<Integer> levelsGained,
        @Nonnull Map<String, Integer> abilityBonuses,
        int classPassivePointsGranted
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
    
    /**
     * Check if any ability bonuses were granted.
     *
     * @return true if ability bonuses are present
     */
    public boolean hasAbilityBonuses() {
        return !abilityBonuses.isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("ClassLevelUpEvent{class=%s, %d->%d, levels=%s, abilityBonuses=%s, passivePoints=%d}",
                classId, oldLevel, newLevel, levelsGained, abilityBonuses, classPassivePointsGranted);
    }
}
