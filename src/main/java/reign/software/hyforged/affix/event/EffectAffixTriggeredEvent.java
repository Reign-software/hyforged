package reign.software.hyforged.affix.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Event fired before a triggered affix effect executes.
 * <p>
 * This event is cancellable.
 */
public class EffectAffixTriggeredEvent implements IEvent<Void> {

    private final Ref<EntityStore> source;
    private final String affixId;
    private final String triggerType;
    private final Ref<EntityStore> target;
    private boolean cancelled = false;

    public EffectAffixTriggeredEvent(
            @Nonnull Ref<EntityStore> source,
            @Nonnull String affixId,
            @Nonnull String triggerType,
            @Nullable Ref<EntityStore> target
    ) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.affixId = Objects.requireNonNull(affixId, "affixId cannot be null");
        this.triggerType = Objects.requireNonNull(triggerType, "triggerType cannot be null");
        this.target = target;
    }

    @Nonnull
    public Ref<EntityStore> getSource() {
        return source;
    }

    @Nonnull
    public String getAffixId() {
        return affixId;
    }

    @Nonnull
    public String getTriggerType() {
        return triggerType;
    }

    @Nullable
    public Ref<EntityStore> getTarget() {
        return target;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
