package reign.software.hyforged.combat.hud;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.combat.log.CombatEncounter;
import reign.software.hyforged.combat.log.CombatEvent;
import reign.software.hyforged.combat.log.CombatLogService;
import reign.software.hyforged.hud.HyforgedHud;
import reign.software.hyforged.hud.HyforgedHudManager;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Updates the combat log section of the composite Hyforged HUD.
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

    /** Update interval in seconds */
    private static final float UPDATE_INTERVAL_SEC = 0.2f;

    /** Maximum events to display in the log */
    private static final int MAX_DISPLAY_EVENTS = 12;

    /** Maximum extra (non-combat) lines to keep per player */
    private static final int MAX_EXTRA_LINES = 8;

    /** Per-player HUD visibility state */
    private static final Map<UUID, Boolean> hudVisibility = new ConcurrentHashMap<>();

    /** Per-player extra messages (XP gains, etc.) — newest at the end */
    private static final Map<UUID, Deque<Message>> extraLines = new ConcurrentHashMap<>();

    /** Monotonic counter to detect extra-line changes */
    private static final Map<UUID, Long> extraLineVersion = new ConcurrentHashMap<>();

    /** Per-player last total event count across encounters (for dirty checking) */
    private final Map<UUID, Integer> lastTotalEventCount = new ConcurrentHashMap<>();

    /** Per-player last extra-line version seen */
    private final Map<UUID, Long> lastExtraVersion = new ConcurrentHashMap<>();

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

        HyforgedHud hud = HyforgedHudManager.getOrCreate(playerUuid, player, playerRef);
        if (hud == null) {
            return; // Client not ready yet
        }

        if (!shouldShow) {
            // Hide combat log section if it was visible
            if (lastTotalEventCount.containsKey(playerUuid)) {
                hud.hideCombatLog();
                lastTotalEventCount.remove(playerUuid);
            }
            return;
        }

        // Get combat data
        CombatEncounter currentEncounter = CombatLogService.get().getCurrentEncounter(playerUuid);
        List<CombatEncounter> encounters = CombatLogService.get().getRecentEncounters(playerUuid);
        
        // Count total events across all encounters (stable dirty signal that grows monotonically)
        int totalEventCount = 0;
        for (CombatEncounter enc : encounters) {
            totalEventCount += enc.getEventCount();
        }
        
        List<CombatEvent> events = gatherRecentEvents(encounters);

        // Check if update needed (dirty check)
        long currentExtraVer = extraLineVersion.getOrDefault(playerUuid, 0L);
        Integer lastCount = lastTotalEventCount.get(playerUuid);
        Long lastExtraVer = lastExtraVersion.get(playerUuid);
        boolean needsUpdate = lastCount == null
                || lastCount != totalEventCount
                || lastExtraVer == null
                || lastExtraVer != currentExtraVer;

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

        // Build formatted lines for the HUD (combat events + extra lines)
        List<Message> allLines = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            allLines.add(CombatLogFormatter.formatEventMessage(events.get(events.size() - 1 - i)));
        }

        // Append extra lines (XP gains, etc.)
        Deque<Message> extras = extraLines.get(playerUuid);
        if (extras != null && !extras.isEmpty()) {
            allLines.addAll(extras);
        }

        // Trim to max display
        if (allLines.size() > MAX_DISPLAY_EVENTS) {
            allLines = allLines.subList(allLines.size() - MAX_DISPLAY_EVENTS, allLines.size());
        }

        Message[] lines = allLines.toArray(new Message[0]);

        String dpsText = dps >= 0 ? String.format("DPS: %.1f", dps) : "DPS: ----";
        hud.updateCombatLog(lines, dpsText, "Hits: " + totalHits, "Crits: " + totalCrits);
        lastTotalEventCount.put(playerUuid, totalEventCount);
        lastExtraVersion.put(playerUuid, currentExtraVer);
    }

    /**
     * Gather recent combat events for display (across all encounters).
     */
    @Nonnull
    private List<CombatEvent> gatherRecentEvents(@Nonnull List<CombatEncounter> encounters) {
        List<CombatEvent> result = new ArrayList<>();

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
     * @return true if visible (default: true)
     */
    public static boolean isHudVisible(@Nonnull UUID playerUuid) {
        return hudVisibility.getOrDefault(playerUuid, true);
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
        extraLines.remove(playerUuid);
        extraLineVersion.remove(playerUuid);
    }

    // --- Extra lines management (XP gains, etc.) ---

    /**
     * Add an extra message line to a player's combat log display.
     * Used for XP gain notifications and other non-combat events.
     *
     * @param playerUuid The player's UUID
     * @param message    The formatted message to display
     */
    public static void addExtraLine(@Nonnull UUID playerUuid, @Nonnull Message message) {
        Deque<Message> queue = extraLines.computeIfAbsent(playerUuid, k -> new ArrayDeque<>());
        synchronized (queue) {
            queue.addLast(message);
            while (queue.size() > MAX_EXTRA_LINES) {
                queue.pollFirst();
            }
        }
        extraLineVersion.merge(playerUuid, 1L, Long::sum);
    }
}
