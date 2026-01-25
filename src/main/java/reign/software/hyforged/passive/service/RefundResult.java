package reign.software.hyforged.passive.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Result of a passive node refund attempt.
 *
 * @param success Whether the refund succeeded
 * @param refundedNodes List of node IDs that were refunded
 * @param totalCost Total Tradebar cost of the refund
 * @param pointsReturned Points returned to the available pool
 * @param reason Failure reason if not successful
 */
public record RefundResult(
        boolean success,
        @Nonnull List<String> refundedNodes,
        int totalCost,
        int pointsReturned,
        @Nullable String reason
) {

    public RefundResult {
        Objects.requireNonNull(refundedNodes, "refundedNodes cannot be null");
    }

    /**
     * Create a successful single node refund result.
     */
    public static RefundResult success(@Nonnull String nodeId, int cost, int pointsReturned) {
        return new RefundResult(true, List.of(nodeId), cost, pointsReturned, null);
    }

    /**
     * Create a successful multi-node refund result (includes orphaned nodes).
     */
    public static RefundResult success(@Nonnull List<String> refundedNodes, int totalCost, int pointsReturned) {
        return new RefundResult(true, refundedNodes, totalCost, pointsReturned, null);
    }

    /**
     * Create a free refund result (migration/admin).
     */
    public static RefundResult successFree(@Nonnull List<String> refundedNodes, int pointsReturned) {
        return new RefundResult(true, refundedNodes, 0, pointsReturned, null);
    }

    /**
     * Create a failure result.
     */
    public static RefundResult failure(@Nonnull String reason) {
        return new RefundResult(false, List.of(), 0, 0, reason);
    }

    // Common failure reasons
    public static final String REASON_NODE_NOT_FOUND = "Node not found in tree";
    public static final String REASON_NODE_NOT_ALLOCATED = "Node is not allocated";
    public static final String REASON_INSUFFICIENT_TRADEBARS = "Not enough Tradebars";
    public static final String REASON_CANNOT_REFUND_STARTING_NODE = "Cannot refund starting node while other nodes are allocated";
    public static final String REASON_TREE_NOT_FOUND = "Passive tree not found";
    public static final String REASON_NO_COMPONENT = "Player has no passive tree component";
    public static final String REASON_NOTHING_TO_REFUND = "No nodes to refund";
    public static final String REASON_CLASS_STARTING_NODE = "Cannot refund class tree starting node";
}
