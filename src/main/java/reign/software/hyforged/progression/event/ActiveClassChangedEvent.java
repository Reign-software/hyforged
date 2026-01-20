package reign.software.hyforged.progression.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Event emitted when a player's active class changes.
 * <p>
 * This event is dispatched by the ActiveClassResolutionSystem when the player's
 * main-hand weapon changes to a weapon with different class tags.
 * <p>
 * Can be used by UI systems to update displayed class information.
 * <p>
 * Implements IEvent&lt;Void&gt; for global (non-keyed) event dispatch.
 */
public record ActiveClassChangedEvent(
    /**
     * Reference to the player entity whose class changed.
     */
    @Nonnull Ref<EntityStore> entityRef,
    
    /**
     * The previous active class ID, or null if no class was active.
     */
    @Nullable String previousClassId,
    
    /**
     * The new active class ID, or null if no class is now active.
     */
    @Nullable String newClassId
) implements IEvent<Void> {
    
    /**
     * Check if a class was deactivated (had a class, now has none).
     *
     * @return true if transitioned from a class to no class
     */
    public boolean wasDeactivated() {
        return previousClassId != null && newClassId == null;
    }
    
    /**
     * Check if a class was activated (had no class, now has one).
     *
     * @return true if transitioned from no class to a class
     */
    public boolean wasActivated() {
        return previousClassId == null && newClassId != null;
    }
    
    /**
     * Check if the class was switched (had a class, now has a different one).
     *
     * @return true if switched between classes
     */
    public boolean wasSwitched() {
        return previousClassId != null && newClassId != null && !previousClassId.equals(newClassId);
    }
}
