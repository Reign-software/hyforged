package reign.software.hyforged.quality.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.affix.model.RolledAffix;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * ECS component storing NPC quality tier and any rolled affixes.
 */
public class HyforgedNPCQualityComponent implements Component<EntityStore> {

    private String qualityId;
    private List<RolledAffix> affixes;

    /**
     * Default constructor required for ECS.
     */
    public HyforgedNPCQualityComponent() {
        this.qualityId = "";
        this.affixes = List.of();
    }

    public HyforgedNPCQualityComponent(@Nonnull String qualityId) {
        this(qualityId, List.of());
    }

    public HyforgedNPCQualityComponent(@Nonnull String qualityId, @Nonnull List<RolledAffix> affixes) {
        this.qualityId = Objects.requireNonNull(qualityId, "qualityId cannot be null");
        this.affixes = List.copyOf(Objects.requireNonNull(affixes, "affixes cannot be null"));
    }

    public HyforgedNPCQualityComponent(@Nonnull HyforgedNPCQualityComponent other) {
        this(other.qualityId, other.affixes);
    }

    @Nonnull
    public String getQualityId() {
        return qualityId;
    }

    public void setQualityId(@Nonnull String qualityId) {
        this.qualityId = Objects.requireNonNull(qualityId, "qualityId cannot be null");
    }

    @Nonnull
    public List<RolledAffix> getAffixes() {
        return affixes;
    }

    public void setAffixes(@Nonnull List<RolledAffix> affixes) {
        this.affixes = List.copyOf(Objects.requireNonNull(affixes, "affixes cannot be null"));
    }

    public boolean hasAffixes() {
        return affixes != null && !affixes.isEmpty();
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new HyforgedNPCQualityComponent(this);
    }
}
