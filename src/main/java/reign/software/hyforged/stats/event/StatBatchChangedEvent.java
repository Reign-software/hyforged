package reign.software.hyforged.stats.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Event emitted when one or more stat values change on an entity within a tick.
 * <p>
 * This event coalesces all stat changes for an entity into a single event,
 * reducing event spam during bulk operations (e.g., equipping multiple items,
 * applying multiple buffs in the same tick).
 * <p>
 * For performance-sensitive systems (UI updates, combat debuff handling),
 * subscribing to this batch event is recommended over individual {@link StatChangedEvent}s.
 * <p>
 * Implements Hytale's {@link IEvent} interface for integration with the EventRegistry.
 *
 * @see StatChangedEvent
 * @see StatChange
 */
public class StatBatchChangedEvent implements IEvent<Ref<EntityStore>> {

    @Nonnull
    private final Ref<EntityStore> entityRef;

    @Nonnull
    private final List<StatChange> changes;

    /**
     * Create a new batch stat changed event.
     *
     * @param entityRef Reference to the entity whose stats changed
     * @param changes List of all stat changes in this batch
     */
    public StatBatchChangedEvent(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull List<StatChange> changes
    ) {
        this.entityRef = Objects.requireNonNull(entityRef, "entityRef cannot be null");
        this.changes = Collections.unmodifiableList(
                Objects.requireNonNull(changes, "changes cannot be null")
        );
    }

    /**
     * Get the entity reference whose stats changed.
     *
     * @return The entity reference
     */
    @Nonnull
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * Get all stat changes in this batch.
     *
     * @return Unmodifiable list of stat changes
     */
    @Nonnull
    public List<StatChange> getChanges() {
        return changes;
    }

    /**
     * Get the number of stats that changed.
     *
     * @return The change count
     */
    public int getChangeCount() {
        return changes.size();
    }

    /**
     * Check if a specific stat changed in this batch.
     *
     * @param statId The stat to check
     * @return true if the stat changed
     */
    public boolean hasChange(@Nonnull StatId statId) {
        return changes.stream()
                .anyMatch(c -> c.statId().equals(statId));
    }

    /**
     * Get the change for a specific stat, if present.
     *
     * @param statId The stat to look up
     * @return Optional containing the change, or empty if stat didn't change
     */
    @Nonnull
    public Optional<StatChange> getChange(@Nonnull StatId statId) {
        return changes.stream()
                .filter(c -> c.statId().equals(statId))
                .findFirst();
    }

    /**
     * Get the change for a specific stat index, if present.
     *
     * @param statIndex The stat index to look up
     * @return Optional containing the change, or empty if stat didn't change
     */
    @Nonnull
    public Optional<StatChange> getChange(int statIndex) {
        return changes.stream()
                .filter(c -> c.statIndex() == statIndex)
                .findFirst();
    }

    /**
     * Check if any stat increased in this batch.
     *
     * @return true if any stat value increased
     */
    public boolean hasAnyIncrease() {
        return changes.stream().anyMatch(StatChange::isIncrease);
    }

    /**
     * Check if any stat decreased in this batch.
     *
     * @return true if any stat value decreased
     */
    public boolean hasAnyDecrease() {
        return changes.stream().anyMatch(StatChange::isDecrease);
    }

    @Nonnull
    @Override
    public String toString() {
        return String.format("StatBatchChangedEvent{entity=%s, changeCount=%d, changes=%s}",
                entityRef, changes.size(), changes);
    }
}
