package reign.software.hyforged.combat.log;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A combat encounter is a collection of combat events grouped by time proximity.
 * <p>
 * An encounter starts with the first combat event and ends when no combat events
 * have occurred for a configurable timeout period.
 */
public class CombatEncounter {
    
    /** Encounter ends after this many milliseconds of inactivity */
    public static final long ENCOUNTER_TIMEOUT_MS = 10_000; // 10 seconds
    
    /** Maximum events per encounter to prevent memory issues */
    public static final int MAX_EVENTS_PER_ENCOUNTER = 1000;
    
    private final long startTime;
    private long lastEventTime;
    private final List<CombatEvent> events;
    private boolean ended;
    
    /**
     * Create a new encounter starting at the given time.
     */
    public CombatEncounter(long startTime) {
        this.startTime = startTime;
        this.lastEventTime = startTime;
        this.events = new ArrayList<>();
        this.ended = false;
    }
    
    /**
     * Add an event to this encounter.
     * 
     * @param event The combat event
     * @return true if added, false if encounter is full or ended
     */
    public boolean addEvent(@Nonnull CombatEvent event) {
        if (ended || events.size() >= MAX_EVENTS_PER_ENCOUNTER) {
            return false;
        }
        
        events.add(event);
        lastEventTime = event.timestamp();
        return true;
    }
    
    /**
     * Check if this encounter should be considered ended based on time.
     * 
     * @param currentTime Current server time in ms
     * @return true if the encounter has timed out
     */
    public boolean isTimedOut(long currentTime) {
        return currentTime - lastEventTime > ENCOUNTER_TIMEOUT_MS;
    }
    
    /**
     * Mark this encounter as ended.
     */
    public void end() {
        this.ended = true;
    }
    
    /**
     * Check if this encounter has been explicitly ended.
     */
    public boolean isEnded() {
        return ended;
    }
    
    /**
     * Get the start time of this encounter.
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * Get the time of the last event.
     */
    public long getLastEventTime() {
        return lastEventTime;
    }
    
    /**
     * Get the duration of this encounter in milliseconds.
     */
    public long getDuration() {
        return lastEventTime - startTime;
    }
    
    /**
     * Get all events in this encounter (unmodifiable).
     */
    @Nonnull
    public List<CombatEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }
    
    /**
     * Get the number of events in this encounter.
     */
    public int getEventCount() {
        return events.size();
    }
    
    /**
     * Calculate total damage dealt to a specific defender in this encounter.
     * 
     * @param defenderUuid The defender's UUID
     * @return Total final damage dealt
     */
    public float getTotalDamageToDefender(@Nonnull UUID defenderUuid) {
        return (float) events.stream()
                .filter(e -> defenderUuid.equals(e.defenderUuid()))
                .mapToDouble(CombatEvent::finalDamage)
                .sum();
    }
    
    /**
     * Calculate total damage dealt by a specific attacker in this encounter.
     * 
     * @param attackerUuid The attacker's UUID
     * @return Total final damage dealt
     */
    public float getTotalDamageByAttacker(@Nonnull UUID attackerUuid) {
        return (float) events.stream()
                .filter(e -> attackerUuid.equals(e.attackerUuid()))
                .mapToDouble(CombatEvent::finalDamage)
                .sum();
    }
    
    /**
     * Count critical hits in this encounter.
     */
    public int getCritCount() {
        return (int) events.stream()
                .filter(CombatEvent::criticalHit)
                .count();
    }
    
    /**
     * Count misses in this encounter.
     */
    public int getMissCount() {
        return (int) events.stream()
                .filter(CombatEvent::missed)
                .count();
    }
    
    /**
     * Count blocks in this encounter.
     */
    public int getBlockCount() {
        return (int) events.stream()
                .filter(CombatEvent::blocked)
                .count();
    }
}
