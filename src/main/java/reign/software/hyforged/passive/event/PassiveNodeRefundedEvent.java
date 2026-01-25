package reign.software.hyforged.passive.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Event emitted when a player refunds one or more passive nodes.
 * <p>
 * This event is dispatched after effects have been removed.
 * Listeners can use this to:
 * - Display refund effects/sounds
 * - Log refunds for analytics
 * - Trigger dependent systems
 *
 * @param entityRef the player entity reference
 * @param treeId the tree ID
 * @param refundedNodes list of node IDs that were refunded
 * @param tradebarCost the Tradebar cost paid (0 for free refunds)
 * @param pointsReturned number of points returned to available pool
 * @param isFreeRefund whether this was a free refund (migration/admin)
 */
public record PassiveNodeRefundedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String treeId,
        @Nonnull List<String> refundedNodes,
        int tradebarCost,
        int pointsReturned,
        boolean isFreeRefund
) implements IEvent<Void> {
    
    /**
     * Check if this was a single-node refund.
     */
    public boolean isSingleNode() {
        return refundedNodes.size() == 1;
    }
    
    /**
     * Check if orphaned nodes were included in the refund.
     */
    public boolean hasOrphanedNodes() {
        return refundedNodes.size() > 1;
    }
    
    @Override
    public String toString() {
        return String.format("PassiveNodeRefundedEvent{tree=%s, nodes=%s, cost=%d, points=%d, free=%s}",
                treeId, refundedNodes, tradebarCost, pointsReturned, isFreeRefund);
    }
}
