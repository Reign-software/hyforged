package reign.software.hyforged.passive.model;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Represents a connection between two passive nodes in a tree.
 *
 * @param from Source node ID
 * @param to Target node ID
 */
public record PassiveConnection(
    @Nonnull String from,
    @Nonnull String to
) {
    
    public PassiveConnection {
        Objects.requireNonNull(from, "from cannot be null");
        Objects.requireNonNull(to, "to cannot be null");
    }
    
    /**
     * Check if this connection involves a specific node.
     *
     * @param nodeId The node ID to check
     * @return true if the connection involves this node
     */
    public boolean involves(@Nonnull String nodeId) {
        return from.equals(nodeId) || to.equals(nodeId);
    }
    
    /**
     * Get the other end of the connection given one node.
     *
     * @param nodeId One end of the connection
     * @return The other end, or null if the node is not part of this connection
     */
    public String getOther(@Nonnull String nodeId) {
        if (from.equals(nodeId)) {
            return to;
        }
        if (to.equals(nodeId)) {
            return from;
        }
        return null;
    }
}
