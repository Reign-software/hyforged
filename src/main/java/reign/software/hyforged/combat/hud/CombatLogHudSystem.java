package reign.software.hyforged.combat.hud;

import com.buuz135.mhud.MultipleHUD;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.combat.log.CombatEncounter;
import reign.software.hyforged.combat.log.CombatEvent;
import reign.software.hyforged.combat.log.CombatLogService;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System that manages the combat log HUD for all players.
 * <p>
 * Uses the MultipleHUD library to display the combat log alongside
 * other custom HUDs (like resource stats).
 * <p>
 * Features:
 * <ul>
 *   <li>Real-time updates every 200ms</li>
 *   <li>Per-player visibility toggle</li>
 *   <li>Calculates DPS from current encounter</li>
 *   <li>Tracks hits and crits for footer display</li>
 * </ul>
 */
public class CombatLogHudSystem extends DelayedEntitySystem<EntityStore> {

    /** Unique identifier for this HUD in MultipleHUD */
    public static final String HUD_ID = "hyforged:combat_log";

    /** Update interval in seconds */
    private static final float UPDATE_INTERVAL_SEC = 0.2f;

    /** Maximum events to display in the log */
    private static final int MAX_DISPLAY_EVENTS = 12;

    /** Per-player HUD visibility state */
    private static final Map<UUID, Boolean> hudVisibility = new ConcurrentHashMap<>();

    /** Per-player HUD instances for updates */
    private static final Map<UUID, CombatLogHud> playerHuds = new ConcurrentHashMap<>();

    /** Per-player last event count (for dirty checking) */
    private final Map<UUID, Integer> lastEventCount = new ConcurrentHashMap<>();

    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType;

    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType;

    @Nonnull
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    public CombatLogHudSystem() {
        super(UPDATE_INTERVAL_SEC);
        this.playerComponentType = Player.getComponentType();
        this.playerRefComponentType = PlayerRef.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
        this.query = Query.and(playerComponentType, playerRefComponentType, uuidComponentType);
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
        Player player = archetypeChunk.getComponent(index, playerComponentType);
        PlayerRef playerRef = archetypeChunk.getComponent(index, playerRefComponentType);
        UUIDComponent uuidComponent = archetypeChunk.getComponent(index, uuidComponentType);

        if (player == null || playerRef == null || uuidComponent == null) {
            return;
        }

        UUID playerUuid = uuidComponent.getUuid();
        boolean shouldShow = isHudVisible(playerUuid);

        MultipleHUD multipleHUD = MultipleHUD.getInstance();
        CombatLogHud existingHud = playerHuds.get(playerUuid);

        if (!shouldShow) {
            // Hide HUD if visible
            if (existingHud != null) {
                multipleHUD.hideCustomHud(player, playerRef, HUD_ID);
                playerHuds.remove(playerUuid);
                lastEventCount.remove(playerUuid);
            }
            return;
        }

        // Get combat data
        CombatEncounter currentEncounter = CombatLogService.get().getCurrentEncounter(playerUuid);
        List<CombatEvent> events = gatherRecentEvents(playerUuid);

        // Check if update needed (dirty check)
        int currentEventCount = events.size();
        Integer lastCount = lastEventCount.get(playerUuid);
        boolean needsUpdate = lastCount == null || lastCount != currentEventCount;

        // Create HUD if not exists
        CombatLogHud hud;
        if (existingHud == null) {
            hud = new CombatLogHud(playerRef);
            multipleHUD.setCustomHud(player, playerRef, HUD_ID, hud);
            playerHuds.put(playerUuid, hud);
            needsUpdate = true;
        } else {
            hud = existingHud;
        }

        if (!needsUpdate) {
            return;
        }

        // Calculate stats
        float dps = calculateDps(currentEncounter);
        int totalHits = 0;
        int totalCrits = 0;

        for (CombatEvent event : events) {
            if (!event.missed()) {
                totalHits++;
                if (event.criticalHit()) {
                    totalCrits++;
                }
            }
        }

        // Update HUD
        hud.updateLog(events, dps, totalHits, totalCrits);
        lastEventCount.put(playerUuid, currentEventCount);
    }

    /**
     * Gather recent combat events for display (across all encounters).
     */
    @Nonnull
    private List<CombatEvent> gatherRecentEvents(@Nonnull UUID playerUuid) {
        List<CombatEvent> result = new ArrayList<>();
        List<CombatEncounter> encounters = CombatLogService.get().getRecentEncounters(playerUuid);

        for (CombatEncounter encounter : encounters) {
            result.addAll(encounter.getEvents());
            if (result.size() >= MAX_DISPLAY_EVENTS) {
                break;
            }
        }

        // Sort by timestamp (newest last for bottom-to-top display)
        result.sort(Comparator.comparingLong(CombatEvent::timestamp));

        // Limit to max entries
        if (result.size() > MAX_DISPLAY_EVENTS) {
            result = new ArrayList<>(result.subList(result.size() - MAX_DISPLAY_EVENTS, result.size()));
        }

        return result;
    }

    /**
     * Calculate DPS for the current encounter.
     *
     * @return DPS value, or -1 if no active encounter
     */
    private float calculateDps(@javax.annotation.Nullable CombatEncounter encounter) {
        if (encounter == null) {
            return -1f;
        }

        long durationMs = encounter.getDuration();
        if (durationMs <= 0) {
            return -1f;
        }

        float totalDamage = 0f;
        for (CombatEvent event : encounter.getEvents()) {
            totalDamage += event.finalDamage();
        }

        float durationSec = durationMs / 1000.0f;
        return totalDamage / durationSec;
    }

    // --- Static visibility management ---

    /**
     * Check if combat log HUD is visible for a player.
     *
     * @param playerUuid The player's UUID
     * @return true if visible (default: false)
     */
    public static boolean isHudVisible(@Nonnull UUID playerUuid) {
        return hudVisibility.getOrDefault(playerUuid, false);
    }

    /**
     * Set combat log HUD visibility for a player.
     *
     * @param playerUuid The player's UUID
     * @param visible    true to show, false to hide
     */
    public static void setHudVisible(@Nonnull UUID playerUuid, boolean visible) {
        hudVisibility.put(playerUuid, visible);
    }

    /**
     * Toggle combat log HUD visibility for a player.
     *
     * @param playerUuid The player's UUID
     * @return new visibility state
     */
    public static boolean toggleHudVisibility(@Nonnull UUID playerUuid) {
        boolean newState = !isHudVisible(playerUuid);
        setHudVisible(playerUuid, newState);
        return newState;
    }

    /**
     * Clear visibility state for a player (on disconnect).
     *
     * @param playerUuid The player's UUID
     */
    public static void clearPlayerState(@Nonnull UUID playerUuid) {
        hudVisibility.remove(playerUuid);
        playerHuds.remove(playerUuid);
    }
}
