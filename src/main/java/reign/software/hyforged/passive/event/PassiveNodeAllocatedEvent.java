package reign.software.hyforged.passive.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.passive.model.PassiveNodeEffect;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Event emitted when a player successfully allocates a passive node.
 * <p>
 * This event is dispatched after effects have been applied.
 * Listeners can use this to:
 * - Display allocation effects/sounds
 * - Log allocations for analytics
 * - Trigger dependent systems
 *
 * @param entityRef the player entity reference
 * @param treeId the tree ID (e.g., "hyforged:general-tree" or "hyforged:warrior-tree")
 * @param nodeId the ID of the allocated node
 * @param isKeystone whether the allocated node is a keystone
 * @param effects list of effects that were applied
 * @param remainingPoints passive points remaining after allocation
 */
public record PassiveNodeAllocatedEvent(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String treeId,
        @Nonnull String nodeId,
        boolean isKeystone,
        @Nonnull List<PassiveNodeEffect> effects,
        int remainingPoints
) implements IEvent<Void> {
    
    @Override
    public String toString() {
        return String.format("PassiveNodeAllocatedEvent{tree=%s, node=%s, keystone=%s, effects=%d, remaining=%d}",
                treeId, nodeId, isKeystone, effects.size(), remainingPoints);
    }
}
