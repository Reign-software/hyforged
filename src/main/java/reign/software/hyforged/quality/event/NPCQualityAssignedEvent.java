package reign.software.hyforged.quality.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.affix.model.RolledAffix;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * Event fired after an NPC quality is assigned.
 */
public class NPCQualityAssignedEvent implements IEvent<Void> {

    private final Ref<EntityStore> entityRef;
    private final String qualityId;
    private final String ruleId;
    private final List<RolledAffix> affixes;

    public NPCQualityAssignedEvent(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull String qualityId,
            @Nonnull String ruleId,
            @Nonnull List<RolledAffix> affixes
    ) {
        this.entityRef = Objects.requireNonNull(entityRef, "entityRef cannot be null");
        this.qualityId = Objects.requireNonNull(qualityId, "qualityId cannot be null");
        this.ruleId = Objects.requireNonNull(ruleId, "ruleId cannot be null");
        this.affixes = List.copyOf(Objects.requireNonNull(affixes, "affixes cannot be null"));
    }

    @Nonnull
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    @Nonnull
    public String getQualityId() {
        return qualityId;
    }

    @Nonnull
    public String getRuleId() {
        return ruleId;
    }

    @Nonnull
    public List<RolledAffix> getAffixes() {
        return affixes;
    }
}
