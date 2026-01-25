package reign.software.hyforged.passive.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Event emitted when a player consumes a Point Book.
 * <p>
 * This event is dispatched after the book point has been added.
 * Listeners can use this to:
 * - Display consumption effects
 * - Log book usage for analytics
 * - Trigger notifications
 *
 * @param entityRef the player entity reference
 * @param newBookPointTotal the total book points after consumption
 * @param maxBookPoints the maximum book points allowed
 */
public record PointBookConsumedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        int newBookPointTotal,
        int maxBookPoints
) implements IEvent<Void> {
    
    /**
     * Check if the player is now at the maximum book points.
     */
    public boolean isAtMax() {
        return newBookPointTotal >= maxBookPoints;
    }
    
    @Override
    public String toString() {
        return String.format("PointBookConsumedEvent{total=%d/%d}", newBookPointTotal, maxBookPoints);
    }
}
