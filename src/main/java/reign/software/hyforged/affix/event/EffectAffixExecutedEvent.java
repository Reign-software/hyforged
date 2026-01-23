package reign.software.hyforged.affix.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Event fired after a triggered affix effect executes.
 */
public class EffectAffixExecutedEvent implements IEvent<Void> {

    private final Ref<EntityStore> source;
    private final String affixId;
    private final String effectType;
    private final Ref<EntityStore> target;

    public EffectAffixExecutedEvent(
            @Nonnull Ref<EntityStore> source,
            @Nonnull String affixId,
            @Nonnull String effectType,
            @Nullable Ref<EntityStore> target
    ) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.affixId = Objects.requireNonNull(affixId, "affixId cannot be null");
        this.effectType = Objects.requireNonNull(effectType, "effectType cannot be null");
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
    public String getEffectType() {
        return effectType;
    }

    @Nullable
    public Ref<EntityStore> getTarget() {
        return target;
    }
}
