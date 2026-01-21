package reign.software.hyforged.combat.ailment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AilmentDefinition}.
 */
@DisplayName("AilmentDefinition")
class AilmentDefinitionTest {
    
    @Nested
    @DisplayName("Builder")
    class BuilderTests {
        
        @Test
        @DisplayName("should build with all required fields")
        void shouldBuildWithRequiredFields() {
            AilmentDefinition ailment = AilmentDefinition.builder()
                    .id("hyforged:fire-ailment")
                    .elementTag("fire")
                    .entityEffectId("Burn")
                    .build();
            
            assertEquals("hyforged:fire-ailment", ailment.id());
            assertEquals("fire", ailment.elementTag());
            assertEquals("Burn", ailment.entityEffectId());
        }
        
        @Test
        @DisplayName("should use default values for optional fields")
        void shouldUseDefaultValues() {
            AilmentDefinition ailment = AilmentDefinition.builder()
                    .id("test")
                    .elementTag("fire")
                    .entityEffectId("Burn")
                    .build();
            
            assertEquals(100, ailment.baseThreshold());
            assertEquals(5000, ailment.accumulationWindowMs());
            assertEquals(4.0f, ailment.baseDurationSeconds(), 0.001f);
        }
        
        @Test
        @DisplayName("should allow custom values for optional fields")
        void shouldAllowCustomValues() {
            AilmentDefinition ailment = AilmentDefinition.builder()
                    .id("test")
                    .elementTag("ice")
                    .entityEffectId("Freeze")
                    .baseThreshold(150)
                    .accumulationWindowMs(8000)
                    .baseDurationSeconds(5.0f)
                    .displayName("Frozen")
                    .description("Freezes the target")
                    .build();
            
            assertEquals(150, ailment.baseThreshold());
            assertEquals(8000, ailment.accumulationWindowMs());
            assertEquals(5.0f, ailment.baseDurationSeconds(), 0.001f);
            assertEquals("Frozen", ailment.displayName());
            assertEquals("Freezes the target", ailment.description());
        }
        
        @Test
        @DisplayName("should throw when id is missing")
        void shouldThrowWhenIdMissing() {
            AilmentDefinition.Builder builder = AilmentDefinition.builder()
                    .elementTag("fire")
                    .entityEffectId("Burn");
            
            assertThrows(IllegalStateException.class, builder::build);
        }
        
        @Test
        @DisplayName("should throw when elementTag is missing")
        void shouldThrowWhenElementTagMissing() {
            AilmentDefinition.Builder builder = AilmentDefinition.builder()
                    .id("test")
                    .entityEffectId("Burn");
            
            assertThrows(IllegalStateException.class, builder::build);
        }
        
        @Test
        @DisplayName("should throw when entityEffectId is missing")
        void shouldThrowWhenEntityEffectIdMissing() {
            AilmentDefinition.Builder builder = AilmentDefinition.builder()
                    .id("test")
                    .elementTag("fire");
            
            assertThrows(IllegalStateException.class, builder::build);
        }
    }
    
    @Nested
    @DisplayName("Record Equality")
    class EqualityTests {
        
        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenFieldsMatch() {
            AilmentDefinition a = AilmentDefinition.builder()
                    .id("test")
                    .elementTag("fire")
                    .entityEffectId("Burn")
                    .build();
            
            AilmentDefinition b = AilmentDefinition.builder()
                    .id("test")
                    .elementTag("fire")
                    .entityEffectId("Burn")
                    .build();
            
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }
}
