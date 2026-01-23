package reign.software.hyforged.quality.event;

import com.hypixel.hytale.event.IEvent;
import reign.software.hyforged.quality.model.QualityRollContext;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Event fired after quality is rolled for an item, before it is applied.
 * <p>
 * This event is cancellable. Listeners can also modify the rolled quality.
 */
public class QualityRolledEvent implements IEvent<Void> {

    private final QualityRollContext context;
    private final String originalQuality;
    private String rolledQuality;
    private final long seed;
    private boolean cancelled = false;

    public QualityRolledEvent(
            @Nonnull QualityRollContext context,
            @Nonnull String originalQuality,
            @Nonnull String rolledQuality,
            long seed
    ) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.originalQuality = Objects.requireNonNull(originalQuality, "originalQuality cannot be null");
        this.rolledQuality = Objects.requireNonNull(rolledQuality, "rolledQuality cannot be null");
        this.seed = seed;
    }

    @Nonnull
    public QualityRollContext getContext() {
        return context;
    }

    @Nonnull
    public String getItemId() {
        return context.itemId();
    }

    @Nonnull
    public String getOriginalQuality() {
        return originalQuality;
    }

    @Nonnull
    public String getRolledQuality() {
        return rolledQuality;
    }

    public void setRolledQuality(@Nonnull String rolledQuality) {
        this.rolledQuality = Objects.requireNonNull(rolledQuality, "rolledQuality cannot be null");
    }

    public long getSeed() {
        return seed;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
