package reign.software.hyforged.minion.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Component attached to a minion entity that links it back to the summoner.
 * <p>
 * Transient component — no persistence codec. Minions are not persisted across
 * server restarts; they are re-summoned on reconnect via concentration state.
 */
public class SummonerLinkComponent implements Component<EntityStore> {

    private UUID summonerUuid;
    private String minionTypeId;
    private String concentrationAbilityId;
    private long summonTimestamp;

    /** Default constructor required by ECS. */
    public SummonerLinkComponent() {
    }

    /** Copy constructor for clone(). */
    public SummonerLinkComponent(@Nonnull SummonerLinkComponent other) {
        this.summonerUuid = other.summonerUuid;
        this.minionTypeId = other.minionTypeId;
        this.concentrationAbilityId = other.concentrationAbilityId;
        this.summonTimestamp = other.summonTimestamp;
    }

    @Override
    public SummonerLinkComponent clone() {
        return new SummonerLinkComponent(this);
    }

    // --- Getters ---

    @Nullable
    public UUID getSummonerUuid() {
        return summonerUuid;
    }

    @Nullable
    public String getMinionTypeId() {
        return minionTypeId;
    }

    @Nullable
    public String getConcentrationAbilityId() {
        return concentrationAbilityId;
    }

    public long getSummonTimestamp() {
        return summonTimestamp;
    }

    // --- Setters ---

    public void setSummonerUuid(@Nullable UUID summonerUuid) {
        this.summonerUuid = summonerUuid;
    }

    public void setMinionTypeId(@Nullable String minionTypeId) {
        this.minionTypeId = minionTypeId;
    }

    public void setConcentrationAbilityId(@Nullable String concentrationAbilityId) {
        this.concentrationAbilityId = concentrationAbilityId;
    }

    public void setSummonTimestamp(long summonTimestamp) {
        this.summonTimestamp = summonTimestamp;
    }
}
