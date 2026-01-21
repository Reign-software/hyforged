package reign.software.hyforged.combat.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CombatEncounter}.
 */
@DisplayName("CombatEncounter")
class CombatEncounterTest {
    
    private CombatEncounter encounter;
    private UUID defenderUuid;
    private long startTime;
    
    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        encounter = new CombatEncounter(startTime);
        defenderUuid = UUID.randomUUID();
    }
    
    @Nested
    @DisplayName("addEvent")
    class AddEventTests {
        
        @Test
        @DisplayName("should add first event")
        void shouldAddFirstEvent() {
            CombatEvent event = createEvent(100);
            
            encounter.addEvent(event);
            
            List<CombatEvent> events = encounter.getEvents();
            assertEquals(1, events.size());
            assertEquals(event, events.get(0));
        }
        
        @Test
        @DisplayName("should add multiple events")
        void shouldAddMultipleEvents() {
            encounter.addEvent(createEvent(100));
            encounter.addEvent(createEvent(50));
            encounter.addEvent(createEvent(75));
            
            assertEquals(3, encounter.getEvents().size());
        }
        
        @Test
        @DisplayName("should update last event time on each event")
        void shouldUpdateLastEventTimeOnEachEvent() {
            long time1 = 1000L;
            long time2 = 5000L;
            
            CombatEncounter enc = new CombatEncounter(time1);
            enc.addEvent(createEvent(time1, 100));
            long firstEnd = enc.getLastEventTime();
            
            enc.addEvent(createEvent(time2, 50));
            long secondEnd = enc.getLastEventTime();
            
            assertEquals(time1, firstEnd);
            assertEquals(time2, secondEnd);
        }
    }
    
    @Nested
    @DisplayName("isTimedOut")
    class IsTimedOutTests {
        
        @Test
        @DisplayName("should not timeout for fresh encounter")
        void shouldNotTimeoutForFreshEncounter() {
            // Fresh encounter hasn't timed out yet
            assertFalse(encounter.isTimedOut(startTime + 5000));
        }
        
        @Test
        @DisplayName("should not timeout within timeout period")
        void shouldNotTimeoutWithinTimeoutPeriod() {
            long eventTime = System.currentTimeMillis();
            CombatEncounter enc = new CombatEncounter(eventTime);
            enc.addEvent(createEvent(eventTime, 100));
            
            // 5 seconds later
            assertFalse(enc.isTimedOut(eventTime + 5000));
        }
        
        @Test
        @DisplayName("should timeout after timeout period")
        void shouldTimeoutAfterTimeoutPeriod() {
            long eventTime = System.currentTimeMillis();
            CombatEncounter enc = new CombatEncounter(eventTime);
            enc.addEvent(createEvent(eventTime, 100));
            
            // 15 seconds later (past 10s default timeout)
            assertTrue(enc.isTimedOut(eventTime + 15000));
        }
        
        @Test
        @DisplayName("should use most recent event for timeout check")
        void shouldUseMostRecentEventForTimeoutCheck() {
            long time1 = 1000L;
            long time2 = 5000L;
            
            CombatEncounter enc = new CombatEncounter(time1);
            enc.addEvent(createEvent(time1, 100));
            enc.addEvent(createEvent(time2, 50));
            
            // 8 seconds after first event but only 3 after second
            assertFalse(enc.isTimedOut(time1 + 8000));
            // 15 seconds after second event
            assertTrue(enc.isTimedOut(time2 + 15000));
        }
    }
    
    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {
        
        @Test
        @DisplayName("should track total damage to defender")
        void shouldTrackTotalDamageToDefender() {
            encounter.addEvent(createEvent(100));
            encounter.addEvent(createEvent(50));
            encounter.addEvent(createEvent(25));
            
            assertEquals(175, encounter.getTotalDamageToDefender(defenderUuid));
        }
        
        @Test
        @DisplayName("should count events")
        void shouldCountEvents() {
            encounter.addEvent(createEvent(100));
            encounter.addEvent(createEvent(50));
            
            assertEquals(2, encounter.getEventCount());
        }
        
        @Test
        @DisplayName("should calculate duration")
        void shouldCalculateDuration() {
            long start = 1000L;
            long end = 6000L;
            
            CombatEncounter enc = new CombatEncounter(start);
            enc.addEvent(createEvent(start, 100));
            enc.addEvent(createEvent(end, 50));
            
            assertEquals(5000, enc.getDuration());
        }
        
        @Test
        @DisplayName("duration should be zero for single event")
        void durationShouldBeZeroForSingleEvent() {
            long eventTime = startTime;
            encounter.addEvent(createEvent(eventTime, 100));
            
            assertEquals(0, encounter.getDuration());
        }
        
        @Test
        @DisplayName("should return zero total for empty encounter")
        void shouldReturnZeroTotalForEmptyEncounter() {
            assertEquals(0, encounter.getTotalDamageToDefender(defenderUuid));
            assertEquals(0, encounter.getEventCount());
            assertEquals(0, encounter.getDuration());
        }
    }
    
    @Nested
    @DisplayName("isEnded")
    class IsEndedTests {
        
        @Test
        @DisplayName("should not be ended when first created")
        void shouldNotBeEndedWhenFirstCreated() {
            assertFalse(encounter.isEnded());
        }
        
        @Test
        @DisplayName("should be ended when marked ended")
        void shouldBeEndedWhenMarkedEnded() {
            encounter.end();
            
            assertTrue(encounter.isEnded());
        }
    }
    
    @Nested
    @DisplayName("getEvents")
    class GetEventsTests {
        
        @Test
        @DisplayName("should return defensive copy")
        void shouldReturnDefensiveCopy() {
            encounter.addEvent(createEvent(100));
            
            List<CombatEvent> events1 = encounter.getEvents();
            List<CombatEvent> events2 = encounter.getEvents();
            
            assertNotSame(events1, events2);
        }
        
        @Test
        @DisplayName("should return events in order added")
        void shouldReturnEventsInOrderAdded() {
            CombatEvent event1 = createEvent(100);
            CombatEvent event2 = createEvent(50);
            CombatEvent event3 = createEvent(75);
            
            encounter.addEvent(event1);
            encounter.addEvent(event2);
            encounter.addEvent(event3);
            
            List<CombatEvent> events = encounter.getEvents();
            assertEquals(event1, events.get(0));
            assertEquals(event2, events.get(1));
            assertEquals(event3, events.get(2));
        }
    }
    
    // Helper methods
    
    private CombatEvent createEvent(int finalDamage) {
        return createEvent(System.currentTimeMillis(), finalDamage);
    }
    
    private CombatEvent createEvent(long timestamp, int finalDamage) {
        return CombatEvent.builder()
                .timestamp(timestamp)
                .defenderUuid(defenderUuid)
                .defenderName("TestPlayer")
                .finalDamage(finalDamage)
                .baseDamage(finalDamage)
                .build();
    }
}
