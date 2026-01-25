package reign.software.hyforged.passive.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Result of a passive node allocation attempt.
 *
 * @param success Whether the allocation succeeded
 * @param nodeId The node ID that was allocated (or attempted)
 * @param reason Failure reason if not successful
 * @param allocatedPath List of nodes that were allocated (for path allocation)
 * @param pointsRemaining Points remaining after allocation
 */
public record AllocationResult(
        boolean success,
        @Nonnull String nodeId,
        @Nullable String reason,
        @Nonnull List<String> allocatedPath,
        int pointsRemaining
) {

    public AllocationResult {
        Objects.requireNonNull(nodeId, "nodeId cannot be null");
        Objects.requireNonNull(allocatedPath, "allocatedPath cannot be null");
    }

    /**
     * Create a successful allocation result.
     */
    public static AllocationResult success(@Nonnull String nodeId, int pointsRemaining) {
        return new AllocationResult(true, nodeId, null, List.of(nodeId), pointsRemaining);
    }

    /**
     * Create a successful path allocation result.
     */
    public static AllocationResult successPath(@Nonnull String targetNodeId, @Nonnull List<String> allocatedPath, int pointsRemaining) {
        return new AllocationResult(true, targetNodeId, null, allocatedPath, pointsRemaining);
    }

    /**
     * Create a failure result.
     */
    public static AllocationResult failure(@Nonnull String nodeId, @Nonnull String reason) {
        return new AllocationResult(false, nodeId, reason, List.of(), -1);
    }

    // Common failure reasons
    public static final String REASON_NODE_NOT_FOUND = "Node not found in tree";
    public static final String REASON_ALREADY_ALLOCATED = "Node is already allocated";
    public static final String REASON_NOT_CONNECTED = "Node is not adjacent to allocated nodes";
    public static final String REASON_INSUFFICIENT_POINTS = "Not enough passive points";
    public static final String REASON_REQUIREMENTS_NOT_MET = "Node requirements not met";
    public static final String REASON_NO_PATH = "No path to node from allocated nodes";
    public static final String REASON_TREE_NOT_FOUND = "Passive tree not found";
    public static final String REASON_NO_STARTING_NODE = "No starting node chosen";
    public static final String REASON_KEYSTONE_CONFLICT = "Another keystone from the same family is already allocated";
    public static final String REASON_CLASS_NOT_UNLOCKED = "Class tree not unlocked";
}
