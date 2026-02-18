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

    /**
     * A timestamped log line combining messages from any source (combat, XP, death, etc.).
     */
    public record TimestampedLine(long timestamp, @Nonnull Message message) {}

    /** Update interval in seconds */
    private static final float UPDATE_INTERVAL_SEC = 0.2f;

    /** Maximum lines to display in the log (combat + extra combined) */
    private static final int MAX_DISPLAY_LINES = 12;

    /** Maximum total lines to retain per player before trimming */
    private static final int MAX_RETAINED_LINES = 50;

    /** Per-player HUD visibility state */
    private static final Map<UUID, Boolean> hudVisibility = new ConcurrentHashMap<>();

    /** Per-player unified log lines (combat + extra), sorted chronologically — newest at end */
    private static final Map<UUID, List<TimestampedLine>> unifiedLines = new ConcurrentHashMap<>();

    /** Monotonic counter to detect any line changes */
    private static final Map<UUID, Long> lineVersion = new ConcurrentHashMap<>();

    /** Per-player last version seen (for dirty checking) */
    private final Map<UUID, Long> lastVersion = new ConcurrentHashMap<>();

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
            if (lastVersion.containsKey(playerUuid)) {
                hud.hideCombatLog();
                lastVersion.remove(playerUuid);
            }
            return;
        }

        // Get combat data from CombatLogService
        CombatEncounter currentEncounter = CombatLogService.get().getCurrentEncounter(playerUuid);
        List<CombatEncounter> encounters = CombatLogService.get().getRecentEncounters(playerUuid);

        // Count total combat events (grows monotonically) for dirty checking
        int totalCombatEvents = 0;
        for (CombatEncounter enc : encounters) {
            totalCombatEvents += enc.getEventCount();
        }

        // Dirty check: combine combat event count + extra line version
        long extraVer = lineVersion.getOrDefault(playerUuid, 0L);
        long combinedVersion = ((long) totalCombatEvents << 32) | (extraVer & 0xFFFFFFFFL);
        Long prevVersion = lastVersion.get(playerUuid);
        if (prevVersion != null && prevVersion == combinedVersion) {
            return;
        }

        // Build unified timeline: combat events + extra lines
        List<TimestampedLine> timeline = new ArrayList<>();

        // Add formatted combat events
        for (CombatEncounter enc : encounters) {
            for (CombatEvent event : enc.getEvents()) {
                timeline.add(new TimestampedLine(event.timestamp(), CombatLogFormatter.formatEventMessage(event)));
            }
        }

        // Add extra lines (XP, death, etc.)
        List<TimestampedLine> extras = unifiedLines.get(playerUuid);
        if (extras != null) {
            synchronized (extras) {
                timeline.addAll(extras);
            }
        }

        // Sort chronologically (oldest first at top, newest at bottom)
        timeline.sort(Comparator.comparingLong(TimestampedLine::timestamp));

        // Keep only the most recent lines
        if (timeline.size() > MAX_DISPLAY_LINES) {
            timeline = timeline.subList(timeline.size() - MAX_DISPLAY_LINES, timeline.size());
        }

        // Calculate stats from combat events only
        List<CombatEvent> combatEvents = gatherRecentEvents(encounters);
        float dps = calculateDps(currentEncounter);
        int totalHits = 0;
        int totalCrits = 0;
        for (CombatEvent event : combatEvents) {
            if (!event.missed()) {
                totalHits++;
                if (event.criticalHit()) {
                    totalCrits++;
                }
            }
        }

        // Push to HUD
        Message[] lines = new Message[timeline.size()];
        for (int i = 0; i < timeline.size(); i++) {
            lines[i] = timeline.get(i).message();
        }

        String dpsText = dps >= 0 ? String.format("DPS: %.1f", dps) : "DPS: ----";
        hud.updateCombatLog(lines, dpsText, "Hits: " + totalHits, "Crits: " + totalCrits);
        lastVersion.put(playerUuid, combinedVersion);
    }

    /**
     * Gather recent combat events for display (across all encounters).
     */
    /**
     * Gather recent combat events for stats calculation (hits, crits, DPS).
     */
    @Nonnull
    private List<CombatEvent> gatherRecentEvents(@Nonnull List<CombatEncounter> encounters) {
        List<CombatEvent> result = new ArrayList<>();

        for (CombatEncounter encounter : encounters) {
            result.addAll(encounter.getEvents());
        }

        result.sort(Comparator.comparingLong(CombatEvent::timestamp));

        if (result.size() > MAX_DISPLAY_LINES) {
            result = new ArrayList<>(result.subList(result.size() - MAX_DISPLAY_LINES, result.size()));
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
        unifiedLines.remove(playerUuid);
        lineVersion.remove(playerUuid);
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
        List<TimestampedLine> lines = unifiedLines.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        synchronized (lines) {
            lines.add(new TimestampedLine(System.currentTimeMillis(), message));
            while (lines.size() > MAX_RETAINED_LINES) {
                lines.remove(0);
            }
        }
        lineVersion.merge(playerUuid, 1L, Long::sum);
    }
}
