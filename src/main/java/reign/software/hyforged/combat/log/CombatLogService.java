package reign.software.hyforged.combat.log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for recording and retrieving combat events per player.
 * <p>
 * This singleton manages combat encounters for all players, automatically
 * grouping events into encounters based on time proximity.
 * <p>
 * Thread-safe for concurrent access from multiple damage event handlers.
 */
public final class CombatLogService {
    
    /** Maximum encounters to keep per player */
    public static final int MAX_ENCOUNTERS_PER_PLAYER = 5;
    
    private static final CombatLogService INSTANCE = new CombatLogService();
    
    /** Per-player combat history: UUID -> recent encounters */
    private final Map<UUID, PlayerCombatLog> playerLogs = new ConcurrentHashMap<>();
    
    private CombatLogService() {
    }
    
    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static CombatLogService get() {
        return INSTANCE;
    }
    
    /**
     * Record a combat event for a player.
     * <p>
     * The event will be added to the player's current encounter, or
     * a new encounter will be started if the previous one has timed out.
     * 
     * @param playerUuid The player's UUID (attacker or defender)
     * @param event The combat event
     */
    public void recordEvent(@Nonnull UUID playerUuid, @Nonnull CombatEvent event) {
        PlayerCombatLog log = playerLogs.computeIfAbsent(playerUuid, k -> new PlayerCombatLog());
        log.addEvent(event);
    }
    
    /**
     * Get recent encounters for a player.
     * 
     * @param playerUuid The player's UUID
     * @return List of recent encounters (newest first), or empty list if none
     */
    @Nonnull
    public List<CombatEncounter> getRecentEncounters(@Nonnull UUID playerUuid) {
        PlayerCombatLog log = playerLogs.get(playerUuid);
        if (log == null) {
            return Collections.emptyList();
        }
        return log.getEncounters();
    }
    
    /**
     * Get the current (active) encounter for a player, if any.
     * 
     * @param playerUuid The player's UUID
     * @return The current encounter, or null if none active
     */
    @Nullable
    public CombatEncounter getCurrentEncounter(@Nonnull UUID playerUuid) {
        PlayerCombatLog log = playerLogs.get(playerUuid);
        if (log == null) {
            return null;
        }
        return log.getCurrentEncounter();
    }
    
    /**
     * Get the last completed encounter for a player.
     * 
     * @param playerUuid The player's UUID
     * @return The last completed encounter, or null if none
     */
    @Nullable
    public CombatEncounter getLastEncounter(@Nonnull UUID playerUuid) {
        List<CombatEncounter> encounters = getRecentEncounters(playerUuid);
        // Find first ended encounter (skip current)
        for (CombatEncounter encounter : encounters) {
            if (encounter.isEnded()) {
                return encounter;
            }
        }
        return null;
    }
    
    /**
     * Clear combat log for a player.
     * <p>
     * Should be called when a player disconnects to prevent unbounded memory growth.
     * 
     * @param playerUuid The player's UUID
     */
    public void clearLog(@Nonnull UUID playerUuid) {
        playerLogs.remove(playerUuid);
    }
    
    /**
     * Called when a player disconnects to free memory.
     * Alias for {@link #clearLog(UUID)}.
     * 
     * @param playerUuid The disconnecting player's UUID
     */
    public void onPlayerDisconnect(@Nonnull UUID playerUuid) {
        clearLog(playerUuid);
    }
    
    /**
     * Clear all combat logs.
     */
    public void clearAll() {
        playerLogs.clear();
    }
    
    /**
     * Per-player combat log storage.
     */
    private static class PlayerCombatLog {
        private final LinkedList<CombatEncounter> encounters = new LinkedList<>();
        private final Object lock = new Object();
        
        void addEvent(@Nonnull CombatEvent event) {
            synchronized (lock) {
                long now = event.timestamp();
                
                // Check if we need a new encounter
                CombatEncounter current = encounters.isEmpty() ? null : encounters.getFirst();
                
                if (current == null || current.isEnded() || current.isTimedOut(now)) {
                    // End the previous encounter if it exists
                    if (current != null && !current.isEnded()) {
                        current.end();
                    }
                    
                    // Start a new encounter
                    current = new CombatEncounter(now);
                    encounters.addFirst(current);
                    
                    // Trim old encounters
                    while (encounters.size() > MAX_ENCOUNTERS_PER_PLAYER) {
                        encounters.removeLast();
                    }
                }
                
                current.addEvent(event);
            }
        }
        
        @Nonnull
        List<CombatEncounter> getEncounters() {
            synchronized (lock) {
                // Return a copy
                return new ArrayList<>(encounters);
            }
        }
        
        @Nullable
        CombatEncounter getCurrentEncounter() {
            synchronized (lock) {
                if (encounters.isEmpty()) {
                    return null;
                }
                CombatEncounter first = encounters.getFirst();
                if (!first.isEnded() && !first.isTimedOut(System.currentTimeMillis())) {
                    return first;
                }
                return null;
            }
        }
    }
}
