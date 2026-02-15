package reign.software.hyforged.stats.resource;

import com.hypixel.hytale.common.util.TimeUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.stats.StatAccessor;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RageDecaySystem extends DelayedEntitySystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(RageDecaySystem.class.getName());
    private static final float UPDATE_INTERVAL_SEC = 0.2f;

    @Nonnull
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapType;

    @Nonnull
    private final ComponentType<EntityStore, DamageDataComponent> damageDataComponentType;

    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    private int rageStatIndex = -1;
    private boolean indicesInitialized = false;

    public RageDecaySystem() {
        super(UPDATE_INTERVAL_SEC);
        this.entityStatMapType = EntityStatMap.getComponentType();
        this.damageDataComponentType = DamageDataComponent.getComponentType();
        this.playerComponentType = Player.getComponentType();
        this.query = Query.and(entityStatMapType, damageDataComponentType, playerComponentType);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        EntityStatMap statMap = archetypeChunk.getComponent(index, entityStatMapType);
        DamageDataComponent damageDataComponent = archetypeChunk.getComponent(index, damageDataComponentType);

        if (statMap == null || damageDataComponent == null) {
            return;
        }

        if (!indicesInitialized || rageStatIndex < 0) {
            rageStatIndex = EntityStatType.getAssetMap().getIndex("Rage");
            indicesInitialized = true;
        }

        if (rageStatIndex < 0) {
            return;
        }

        if (!StatAccessor.hasStatSlot(statMap, rageStatIndex)) {
            return;
        }

        EntityStatValue rageValue = statMap.get(rageStatIndex);
        if (rageValue == null) {
            return;
        }

        RageDecayConfig config = RageDecayConfig.get();
        float decayPerSecond = config.getDecayPerSecond();
        if (decayPerSecond <= 0.0f) {
            return;
        }

        Instant now = store.getResource(TimeResource.getResourceType()).getNow();
        Duration outOfCombatDelay = Duration.ofMillis(Math.round(config.getOutOfCombatDelaySeconds() * 1000.0f));
        Instant lastCombatAction = damageDataComponent.getLastCombatAction();

        if (TimeUtil.compareDifference(lastCombatAction, now, outOfCombatDelay) < 0) {
            return;
        }

        float current = rageValue.get();
        if (current <= 0.0f) {
            return;
        }

        float decayAmount = decayPerSecond * dt;
        float newValue = Math.max(0.0f, current - decayAmount);
        if (newValue != current) {
            statMap.setStatValue(rageStatIndex, newValue);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("Rage decay applied: %.2f -> %.2f (dt=%.2f)", current, newValue, dt));
            }
        }
    }
}
