package reign.software.hyforged.combat.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CombatEvent}.
 */
@DisplayName("CombatEvent")
class CombatEventTest {
    
    @Nested
    @DisplayName("Builder")
    class BuilderTests {
        
        @Test
        @DisplayName("should create event with required fields")
        void shouldCreateEventWithRequiredFields() {
            long timestamp = System.currentTimeMillis();
            UUID defenderUuid = UUID.randomUUID();
            
            CombatEvent event = CombatEvent.builder()
                    .timestamp(timestamp)
                    .defenderUuid(defenderUuid)
                    .defenderName("TestPlayer")
                    .finalDamage(100)
                    .baseDamage(150)
                    .build();
            
            assertEquals(timestamp, event.timestamp());
            assertEquals(defenderUuid, event.defenderUuid());
            assertEquals("TestPlayer", event.defenderName());
            assertEquals(100, event.finalDamage());
            assertEquals(150, event.baseDamage());
        }
        
        @Test
        @DisplayName("should create event with all fields")
        void shouldCreateEventWithAllFields() {
            long timestamp = System.currentTimeMillis();
            UUID defenderUuid = UUID.randomUUID();
            UUID attackerUuid = UUID.randomUUID();
            
            CombatEvent event = CombatEvent.builder()
                    .timestamp(timestamp)
                    .defenderUuid(defenderUuid)
                    .defenderName("Defender")
                    .attackerUuid(attackerUuid)
                    .attackerName("Attacker")
                    .damageCauseId("physical")
                    .baseDamage(200)
                    .finalDamage(100)
                    .missed(false)
                    .blocked(true)
                    .autoBlocked(true)
                    .criticalHit(false)
                    .critMultiplierBps(0)
                    .build();
            
            assertEquals(attackerUuid, event.attackerUuid());
            assertEquals("Attacker", event.attackerName());
            assertEquals("physical", event.damageCauseId());
            assertTrue(event.blocked());
            assertTrue(event.autoBlocked());
            assertFalse(event.missed());
            assertFalse(event.criticalHit());
        }
        
        @Test
        @DisplayName("should default boolean fields to false")
        void shouldDefaultBooleanFieldsToFalse() {
            CombatEvent event = CombatEvent.builder()
                    .timestamp(System.currentTimeMillis())
                    .defenderUuid(UUID.randomUUID())
                    .defenderName("Test")
                    .finalDamage(50)
                    .baseDamage(50)
                    .build();
            
            assertFalse(event.missed());
            assertFalse(event.blocked());
            assertFalse(event.autoBlocked());
            assertFalse(event.criticalHit());
            assertEquals(0, event.critMultiplierBps());
        }
        
        @Test
        @DisplayName("should create event for miss")
        void shouldCreateEventForMiss() {
            CombatEvent event = CombatEvent.builder()
                    .timestamp(System.currentTimeMillis())
                    .defenderUuid(UUID.randomUUID())
                    .defenderName("Test")
                    .finalDamage(0)
                    .baseDamage(100)
                    .missed(true)
                    .build();
            
            assertTrue(event.missed());
            assertEquals(0, event.finalDamage());
            assertEquals(100, event.baseDamage());
        }
        
        @Test
        @DisplayName("should create event for critical hit")
        void shouldCreateEventForCriticalHit() {
            CombatEvent event = CombatEvent.builder()
                    .timestamp(System.currentTimeMillis())
                    .defenderUuid(UUID.randomUUID())
                    .defenderName("Test")
                    .finalDamage(300)
                    .baseDamage(100)
                    .criticalHit(true)
                    .critMultiplierBps(20000) // 200% crit damage
                    .build();
            
            assertTrue(event.criticalHit());
            assertEquals(20000, event.critMultiplierBps());
        }
    }
    
    @Nested
    @DisplayName("Record properties")
    class RecordPropertiesTests {
        
        @Test
        @DisplayName("should be immutable record")
        void shouldBeImmutableRecord() {
            CombatEvent event = CombatEvent.builder()
                    .timestamp(1000L)
                    .defenderUuid(UUID.randomUUID())
                    .defenderName("Test")
                    .finalDamage(50)
                    .baseDamage(50)
                    .build();
            
            // Verify record equals/hashCode works
            CombatEvent same = CombatEvent.builder()
                    .timestamp(1000L)
                    .defenderUuid(event.defenderUuid())
                    .defenderName("Test")
                    .finalDamage(50)
                    .baseDamage(50)
                    .build();
            
            assertEquals(event, same);
            assertEquals(event.hashCode(), same.hashCode());
        }
        
        @Test
        @DisplayName("different events should not be equal")
        void differentEventsShouldNotBeEqual() {
            UUID defenderUuid = UUID.randomUUID();
            
            CombatEvent event1 = CombatEvent.builder()
                    .timestamp(1000L)
                    .defenderUuid(defenderUuid)
                    .defenderName("Test")
                    .finalDamage(50)
                    .baseDamage(50)
                    .build();
            
            CombatEvent event2 = CombatEvent.builder()
                    .timestamp(1000L)
                    .defenderUuid(defenderUuid)
                    .defenderName("Test")
                    .finalDamage(100) // Different damage
                    .baseDamage(50)
                    .build();
            
            assertNotEquals(event1, event2);
        }
    }
}
