package reign.software.hyforged.passive.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Event emitted when a player performs a full respec of a passive tree.
 * <p>
 * This event is dispatched after all nodes have been refunded.
 * Listeners can use this to:
 * - Display respec effects
 * - Log respecs for analytics
 * - Trigger dependent systems
 *
 * @param entityRef the player entity reference
 * @param treeId the tree ID
 * @param nodeCount number of nodes that were refunded
 * @param tradebarCost the total Tradebar cost paid
 * @param pointsReturned number of points returned to available pool
 */
public record PassiveTreeRespecEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String treeId,
        int nodeCount,
        int tradebarCost,
        int pointsReturned
) implements IEvent<Void> {
    
    @Override
    public String toString() {
        return String.format("PassiveTreeRespecEvent{tree=%s, nodes=%d, cost=%d, points=%d}",
                treeId, nodeCount, tradebarCost, pointsReturned);
    }
}
