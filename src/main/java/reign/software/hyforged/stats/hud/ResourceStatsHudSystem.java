package reign.software.hyforged.stats.hud;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.hud.HyforgedHud;
import reign.software.hyforged.hud.HyforgedHudManager;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Updates the resource stats section of the composite Hyforged HUD.
 * Displays Concentration and Rage bar values.
 */
public class ResourceStatsHudSystem extends DelayedEntitySystem<EntityStore> {

    private static final float UPDATE_INTERVAL_SEC = 0.2f;

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapType;

    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType;

    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType;

    @Nonnull
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    private int concentrationStatIndex = -1;
    private int rageStatIndex = -1;
    private boolean indicesInitialized = false;

    public ResourceStatsHudSystem() {
        super(UPDATE_INTERVAL_SEC);
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        this.entityStatMapType = EntityStatMap.getComponentType();
        this.playerComponentType = Player.getComponentType();
        this.playerRefComponentType = PlayerRef.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
        this.query = Query.and(statComponentType, entityStatMapType, playerComponentType, playerRefComponentType, uuidComponentType);
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
        HyforgedStatComponent stats = archetypeChunk.getComponent(index, statComponentType);
        EntityStatMap statMap = archetypeChunk.getComponent(index, entityStatMapType);
        Player player = archetypeChunk.getComponent(index, playerComponentType);
        PlayerRef playerRef = archetypeChunk.getComponent(index, playerRefComponentType);
        UUIDComponent uuidComponent = archetypeChunk.getComponent(index, uuidComponentType);

        if (stats == null || statMap == null || player == null || playerRef == null || uuidComponent == null) {
            return;
        }

        UUID playerUuid = uuidComponent.getUuid();

        if (!indicesInitialized || concentrationStatIndex < 0 || rageStatIndex < 0) {
            initializeStatIndices();
        }

        EntityStatValue concentrationValue = concentrationStatIndex >= 0 ? statMap.get(concentrationStatIndex) : null;
        EntityStatValue rageValue = rageStatIndex >= 0 ? statMap.get(rageStatIndex) : null;

        boolean showConcentration = concentrationValue != null && concentrationValue.getMax() > 0.0f;
        boolean showRage = rageValue != null && rageValue.getMax() > 0.0f;

        HyforgedHud hud = HyforgedHudManager.getOrCreate(playerUuid, player, playerRef);
        if (hud == null) {
            return; // Client not ready yet
        }

        int concentrationCurrent = concentrationValue != null ? Math.round(concentrationValue.get()) : 0;
        int concentrationMax = concentrationValue != null ? Math.round(concentrationValue.getMax()) : 0;
        int rageCurrent = rageValue != null ? Math.round(rageValue.get()) : 0;
        int rageMax = rageValue != null ? Math.round(rageValue.getMax()) : 0;

        boolean needsUpdate =
                !stats.isLastHudShown()
                        || stats.isLastHudConcentrationVisible() != showConcentration
                        || stats.isLastHudRageVisible() != showRage
                        || stats.getLastHudConcentrationCurrent() != concentrationCurrent
                        || stats.getLastHudConcentrationMax() != concentrationMax
                        || stats.getLastHudRageCurrent() != rageCurrent
                        || stats.getLastHudRageMax() != rageMax;

        if (!needsUpdate) {
            return;
        }

        hud.updateResourceStats(
                showConcentration,
                concentrationCurrent,
                concentrationMax,
                showRage,
                rageCurrent,
                rageMax
        );

        stats.setLastHudShown(true);
        stats.setLastHudConcentrationVisible(showConcentration);
        stats.setLastHudRageVisible(showRage);
        stats.setLastHudConcentrationCurrent(concentrationCurrent);
        stats.setLastHudConcentrationMax(concentrationMax);
        stats.setLastHudRageCurrent(rageCurrent);
        stats.setLastHudRageMax(rageMax);
    }

    private void initializeStatIndices() {
        concentrationStatIndex = EntityStatType.getAssetMap().getIndex("Concentration");
        rageStatIndex = EntityStatType.getAssetMap().getIndex("Rage");
        indicesInitialized = true;
    }
}
