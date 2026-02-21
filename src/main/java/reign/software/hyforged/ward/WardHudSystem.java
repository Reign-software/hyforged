package reign.software.hyforged.ward;

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
 * Updates the Ward bar section of the composite Hyforged HUD at a fixed interval.
 * <p>
 * The bar is only shown when the entity has a non-zero Ward maximum, which is
 * driven by the {@code hyforged:max-ward-flat} stat bridged by {@link reign.software.hyforged.stats.system.HyforgedBridgeSystem}.
 * <p>
 * Uses dirty-flag tracking via {@link HyforgedStatComponent} to skip redundant HUD updates.
 */
public class WardHudSystem extends DelayedEntitySystem<EntityStore> {

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

    private int wardEntityStatIndex = -1;
    private boolean indexInitialized = false;

    public WardHudSystem() {
        super(UPDATE_INTERVAL_SEC);
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        this.entityStatMapType = EntityStatMap.getComponentType();
        this.playerComponentType = Player.getComponentType();
        this.playerRefComponentType = PlayerRef.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
        this.query = Query.and(
                statComponentType,
                entityStatMapType,
                playerComponentType,
                playerRefComponentType,
                uuidComponentType
        );
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

        if (!indexInitialized) {
            wardEntityStatIndex = EntityStatType.getAssetMap().getIndex("Ward");
            indexInitialized = true;
        }

        if (wardEntityStatIndex < 0) {
            return;
        }

        EntityStatValue wardValue = statMap.get(wardEntityStatIndex);
        boolean showWard = wardValue != null && wardValue.getMax() > 0.0f;
        int wardCurrent = wardValue != null ? Math.round(wardValue.get()) : 0;
        int wardMax = wardValue != null ? Math.round(wardValue.getMax()) : 0;

        boolean needsUpdate =
                stats.isLastHudWardVisible() != showWard
                        || stats.getLastHudWardCurrent() != wardCurrent
                        || stats.getLastHudWardMax() != wardMax;

        if (!needsUpdate) {
            return;
        }

        UUID playerUuid = uuidComponent.getUuid();
        HyforgedHud hud = HyforgedHudManager.getOrCreate(playerUuid, player, playerRef);
        if (hud == null) {
            return;
        }

        if (showWard) {
            hud.updateWard(wardCurrent, wardMax);
        } else {
            hud.hideWard();
        }

        stats.setLastHudWardVisible(showWard);
        stats.setLastHudWardCurrent(wardCurrent);
        stats.setLastHudWardMax(wardMax);
    }
}
