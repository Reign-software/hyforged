package reign.software.hyforged.passive.model;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Requirements for allocating a passive node.
 *
 * @param allocatedNodes Minimum number of nodes that must be allocated in the tree
 * @param requiredTags Tags that the player must have (unlock flags, etc.)
 */
public record PassiveNodeRequirements(
    int allocatedNodes,
    @Nonnull List<String> requiredTags
) {
    
    public static final PassiveNodeRequirements NONE = new PassiveNodeRequirements(0, Collections.emptyList());
    
    public PassiveNodeRequirements {
        Objects.requireNonNull(requiredTags, "requiredTags cannot be null");
        requiredTags = List.copyOf(requiredTags); // Defensive copy
    }
    
    /**
     * Check if there are any requirements.
     *
     * @return true if there are no requirements
     */
    public boolean isEmpty() {
        return allocatedNodes == 0 && requiredTags.isEmpty();
    }
}
