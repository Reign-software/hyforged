package reign.software.hyforged.minion.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.concentration.ConcentrationService;
import reign.software.hyforged.minion.MinionSummonService;
import reign.software.hyforged.minion.component.MinionTrackerComponent;
import reign.software.hyforged.minion.component.SummonerLinkComponent;
import reign.software.hyforged.util.MessageColors;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles minion death cleanup: releases concentration, cleans the summoner's
 * {@link MinionTrackerComponent}, removes duration timers, and notifies the
 * summoner when a minion entity dies.
 * <p>
 * Extends {@link DeathSystems.OnDeathSystem} (a {@code RefChangeSystem<EntityStore, DeathComponent>}).
 * The {@link #getQuery()} filter ensures only entities with {@link SummonerLinkComponent}
 * (i.e., minions) trigger this system when they receive a {@link DeathComponent}.
 * <p>
 * Death performs a <b>full release</b> of concentration (distinct from disable/FR-4),
 * removing the ability entry entirely from the priority queue.
 */
public class MinionDeathSystem extends DeathSystems.OnDeathSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ComponentType<EntityStore, SummonerLinkComponent> summonerLinkType;
    private final ComponentType<EntityStore, MinionTrackerComponent> minionTrackerType;

    public MinionDeathSystem(
            @Nonnull ComponentType<EntityStore, SummonerLinkComponent> summonerLinkType,
            @Nonnull ComponentType<EntityStore, MinionTrackerComponent> minionTrackerType
    ) {
        this.summonerLinkType = summonerLinkType;
        this.minionTrackerType = minionTrackerType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        // Only match entities that have SummonerLinkComponent (minions).
        // The OnDeathSystem base already filters for DeathComponent via componentType().
        return summonerLinkType;
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull DeathComponent death,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (!entityRef.isValid()) {
            return;
        }

        // Read SummonerLinkComponent from the dying minion
        SummonerLinkComponent link = store.getComponent(entityRef, summonerLinkType);
        if (link == null) {
            return;
        }

        String abilityId = link.getConcentrationAbilityId();
        UUID summonerUuid = link.getSummonerUuid();
        String minionTypeId = link.getMinionTypeId();

        if (abilityId == null || summonerUuid == null) {
            LOGGER.atWarning().log("MinionDeathSystem: SummonerLinkComponent missing abilityId or summonerUuid");
            return;
        }

        // Clean up duration timer regardless of summoner validity
        MinionSummonService.get().removeDurationTimer(abilityId);

        // Resolve summoner entity
        Ref<EntityStore> summonerRef = store.getExternalData().getRefFromUUID(summonerUuid);
        if (summonerRef == null || !summonerRef.isValid()) {
            LOGGER.at(Level.FINE).log(
                    "MinionDeathSystem: summoner %s not found/invalid, skipping concentration release",
                    summonerUuid);
            return;
        }

        // FULL release of concentration (death = permanent removal).
        // This removes the ability entry entirely from the priority queue,
        // distinct from FR-4 disable which keeps the entry but marks it disabled.
        ConcentrationService.get().releaseConcentration(summonerRef, abilityId);

        // Clean up MinionTrackerComponent on summoner
        MinionTrackerComponent tracker = store.getComponent(summonerRef, minionTrackerType);
        if (tracker != null) {
            tracker.removeMinion(abilityId);
        }

        // Notify summoner of minion death
        String displayName = minionTypeId != null ? minionTypeId : "minion";
        PlayerRef playerRef = store.getComponent(summonerRef, PlayerRef.getComponentType());
        if (playerRef != null) {
            playerRef.sendMessage(
                    Message.translation("minion.died")
                            .param("name", displayName)
                            .color(MessageColors.WARNING)
            );
        }

        LOGGER.at(Level.FINE).log("MinionDeathSystem: minion died (abilityId=%s, type=%s, summoner=%s)",
                abilityId, minionTypeId, summonerUuid);
    }
}
