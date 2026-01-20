package reign.software.hyforged.stats.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Event emitted when a single stat value changes on an entity.
 * <p>
 * This event is fired for each individual stat change. For bulk operations
 * or performance-sensitive systems, prefer subscribing to {@link StatBatchChangedEvent}
 * which coalesces all stat changes for an entity within a tick.
 * <p>
 * Implements Hytale's {@link IEvent} interface for integration with the EventRegistry.
 *
 * @see StatBatchChangedEvent
 */
public class StatChangedEvent implements IEvent<Ref<EntityStore>> {

    @Nonnull
    private final Ref<EntityStore> entityRef;

    @Nonnull
    private final StatChange change;

    /**
     * Create a new stat changed event.
     *
     * @param entityRef Reference to the entity whose stat changed
     * @param change The stat change details
     */
    public StatChangedEvent(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull StatChange change
    ) {
        this.entityRef = Objects.requireNonNull(entityRef, "entityRef cannot be null");
        this.change = Objects.requireNonNull(change, "change cannot be null");
    }

    /**
     * Create a new stat changed event with explicit values.
     *
     * @param entityRef Reference to the entity whose stat changed
     * @param statId The stat that changed
     * @param statIndex The stat index in the registry
     * @param oldValue The previous effective value
     * @param newValue The new effective value
     * @param sourceId Optional source identifier
     */
    public StatChangedEvent(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull StatId statId,
            int statIndex,
            int oldValue,
            int newValue,
            @Nullable String sourceId
    ) {
        this(entityRef, new StatChange(statId, statIndex, oldValue, newValue, sourceId));
    }

    /**
     * Get the entity reference whose stat changed.
     *
     * @return The entity reference
     */
    @Nonnull
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * Get the stat change details.
     *
     * @return The stat change record
     */
    @Nonnull
    public StatChange getChange() {
        return change;
    }

    /**
     * Get the stat ID that changed.
     *
     * @return The stat ID
     */
    @Nonnull
    public StatId getStatId() {
        return change.statId();
    }

    /**
     * Get the stat index in the registry.
     *
     * @return The stat index
     */
    public int getStatIndex() {
        return change.statIndex();
    }

    /**
     * Get the previous effective value.
     *
     * @return The old value
     */
    public int getOldValue() {
        return change.oldValue();
    }

    /**
     * Get the new effective value.
     *
     * @return The new value
     */
    public int getNewValue() {
        return change.newValue();
    }

    /**
     * Get the source identifier that caused this change, if known.
     *
     * @return The source ID, or null if not specified
     */
    @Nullable
    public String getSourceId() {
        return change.sourceId();
    }

    /**
     * Get the change amount (newValue - oldValue).
     *
     * @return The delta
     */
    public int getDelta() {
        return change.delta();
    }

    @Nonnull
    @Override
    public String toString() {
        return String.format("StatChangedEvent{entity=%s, %s}",
                entityRef, change);
    }
}
