package reign.software.hyforged.combat.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CombatLogService}.
 */
@DisplayName("CombatLogService")
class CombatLogServiceTest {
    
    private UUID playerUuid;
    
    @BeforeEach
    void setUp() {
        CombatLogService.get().clearAll();
        playerUuid = UUID.randomUUID();
    }
    
    @Nested
    @DisplayName("Singleton")
    class SingletonTests {
        
        @Test
        @DisplayName("should return same instance")
        void shouldReturnSameInstance() {
            CombatLogService instance1 = CombatLogService.get();
            CombatLogService instance2 = CombatLogService.get();
            
            assertSame(instance1, instance2);
        }
    }
    
    @Nested
    @DisplayName("recordEvent")
    class RecordEventTests {
        
        @Test
        @DisplayName("should create encounter for new player")
        void shouldCreateEncounterForNewPlayer() {
            CombatEvent event = createEvent(100);
            
            CombatLogService.get().recordEvent(playerUuid, event);
            
            List<CombatEncounter> encounters = CombatLogService.get().getRecentEncounters(playerUuid);
            assertEquals(1, encounters.size());
            assertEquals(1, encounters.get(0).getEvents().size());
        }
        
        @Test
        @DisplayName("should add to existing encounter within timeout")
        void shouldAddToExistingEncounterWithinTimeout() {
            long now = System.currentTimeMillis();
            CombatEvent event1 = createEvent(now, 100);
            CombatEvent event2 = createEvent(now + 5000, 50); // 5 seconds later
            
            CombatLogService.get().recordEvent(playerUuid, event1);
            CombatLogService.get().recordEvent(playerUuid, event2);
            
            List<CombatEncounter> encounters = CombatLogService.get().getRecentEncounters(playerUuid);
            assertEquals(1, encounters.size());
            assertEquals(2, encounters.get(0).getEvents().size());
        }
        
        @Test
        @DisplayName("should create new encounter after timeout")
        void shouldCreateNewEncounterAfterTimeout() {
            long now = System.currentTimeMillis();
            CombatEvent event1 = createEvent(now, 100);
            CombatEvent event2 = createEvent(now + 15000, 50); // 15 seconds later (past 10s timeout)
            
            CombatLogService.get().recordEvent(playerUuid, event1);
            CombatLogService.get().recordEvent(playerUuid, event2);
            
            List<CombatEncounter> encounters = CombatLogService.get().getRecentEncounters(playerUuid);
            assertEquals(2, encounters.size());
        }
        
        @Test
        @DisplayName("should limit encounters per player")
        void shouldLimitEncountersPerPlayer() {
            // Create more than max encounters
            long baseTime = 1000000;
            for (int i = 0; i < 10; i++) {
                CombatEvent event = createEvent(baseTime + (i * 20000), 100); // 20s apart
                CombatLogService.get().recordEvent(playerUuid, event);
            }
            
            List<CombatEncounter> encounters = CombatLogService.get().getRecentEncounters(playerUuid);
            assertEquals(5, encounters.size()); // Default max is 5
        }
        
        @Test
        @DisplayName("should track separate encounters for different players")
        void shouldTrackSeparateEncountersForDifferentPlayers() {
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();
            
            CombatLogService.get().recordEvent(player1, createEvent(100));
            CombatLogService.get().recordEvent(player2, createEvent(50));
            
            List<CombatEncounter> p1Encounters = CombatLogService.get().getRecentEncounters(player1);
            List<CombatEncounter> p2Encounters = CombatLogService.get().getRecentEncounters(player2);
            
            assertEquals(1, p1Encounters.size());
            assertEquals(1, p2Encounters.size());
            assertEquals(100, p1Encounters.get(0).getEvents().get(0).finalDamage());
            assertEquals(50, p2Encounters.get(0).getEvents().get(0).finalDamage());
        }
    }
    
    @Nested
    @DisplayName("getRecentEncounters")
    class GetRecentEncountersTests {
        
        @Test
        @DisplayName("should return empty list for unknown player")
        void shouldReturnEmptyListForUnknownPlayer() {
            List<CombatEncounter> encounters = CombatLogService.get().getRecentEncounters(UUID.randomUUID());
            
            assertTrue(encounters.isEmpty());
        }
        
        @Test
        @DisplayName("should return defensive copy")
        void shouldReturnDefensiveCopy() {
            CombatLogService.get().recordEvent(playerUuid, createEvent(100));
            
            List<CombatEncounter> encounters1 = CombatLogService.get().getRecentEncounters(playerUuid);
            List<CombatEncounter> encounters2 = CombatLogService.get().getRecentEncounters(playerUuid);
            
            assertNotSame(encounters1, encounters2);
        }
    }
    
    @Nested
    @DisplayName("getCurrentEncounter")
    class GetCurrentEncounterTests {
        
        @Test
        @DisplayName("should return null for unknown player")
        void shouldReturnNullForUnknownPlayer() {
            CombatEncounter encounter = CombatLogService.get().getCurrentEncounter(UUID.randomUUID());
            
            assertNull(encounter);
        }
        
        @Test
        @DisplayName("should return most recent encounter")
        void shouldReturnMostRecentEncounter() {
            long now = System.currentTimeMillis();
            CombatEvent event1 = createEvent(now, 100);
            CombatEvent event2 = createEvent(now + 15000, 200); // New encounter
            
            CombatLogService.get().recordEvent(playerUuid, event1);
            CombatLogService.get().recordEvent(playerUuid, event2);
            
            CombatEncounter current = CombatLogService.get().getCurrentEncounter(playerUuid);
            assertNotNull(current);
            assertEquals(1, current.getEvents().size());
            assertEquals(200, current.getEvents().get(0).finalDamage());
        }
    }
    
    @Nested
    @DisplayName("clearLog")
    class ClearLogTests {
        
        @Test
        @DisplayName("should remove player data")
        void shouldRemovePlayerData() {
            CombatLogService.get().recordEvent(playerUuid, createEvent(100));
            
            CombatLogService.get().clearLog(playerUuid);
            
            assertTrue(CombatLogService.get().getRecentEncounters(playerUuid).isEmpty());
        }
        
        @Test
        @DisplayName("should not affect other players")
        void shouldNotAffectOtherPlayers() {
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();
            
            CombatLogService.get().recordEvent(player1, createEvent(100));
            CombatLogService.get().recordEvent(player2, createEvent(50));
            
            CombatLogService.get().clearLog(player1);
            
            assertTrue(CombatLogService.get().getRecentEncounters(player1).isEmpty());
            assertFalse(CombatLogService.get().getRecentEncounters(player2).isEmpty());
        }
    }
    
    @Nested
    @DisplayName("clearAll")
    class ClearAllTests {
        
        @Test
        @DisplayName("should remove all data")
        void shouldRemoveAllData() {
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();
            
            CombatLogService.get().recordEvent(player1, createEvent(100));
            CombatLogService.get().recordEvent(player2, createEvent(50));
            
            CombatLogService.get().clearAll();
            
            assertTrue(CombatLogService.get().getRecentEncounters(player1).isEmpty());
            assertTrue(CombatLogService.get().getRecentEncounters(player2).isEmpty());
        }
    }
    
    // Helper methods
    
    private CombatEvent createEvent(int finalDamage) {
        return createEvent(System.currentTimeMillis(), finalDamage);
    }
    
    private CombatEvent createEvent(long timestamp, int finalDamage) {
        return CombatEvent.builder()
                .timestamp(timestamp)
                .defenderUuid(playerUuid)
                .defenderName("TestPlayer")
                .finalDamage(finalDamage)
                .baseDamage(finalDamage)
                .build();
    }
}
