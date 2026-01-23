package reign.software.hyforged.affix.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.affix.AffixTestFixtures;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RolledAffix} record.
 */
@DisplayName("RolledAffix")
class RolledAffixTest {
    
    private static final String HEALTH = "hyforged:health";
    private static final String ARMOR = "hyforged:armor";
    private static final String DAMAGE = "hyforged:damage";
    
    private static Map<String, RolledAffix.RolledStat> singleStat(String statId, int value, HyforgedModifier.StackType stackType) {
        Map<String, RolledAffix.RolledStat> stats = new HashMap<>();
        stats.put(statId, new RolledAffix.RolledStat(value, stackType));
        return stats;
    }
    
    @Nested
    @DisplayName("Construction")
    class Construction {
        
        @Test
        @DisplayName("should create with valid parameters")
        void shouldCreateWithValidParameters() {
            Map<String, RolledAffix.RolledStat> stats = singleStat(HEALTH, 50, HyforgedModifier.StackType.FLAT);
            
            RolledAffix affix = new RolledAffix(
                    "sturdy",
                    "prefix",
                    1,
                    stats
            );
            
            assertEquals("sturdy", affix.affixId());
            assertEquals("prefix", affix.type());
            assertEquals(1, affix.tier());
            assertEquals(1, affix.getStatCount());
            assertTrue(affix.grantsStat(HEALTH));
            assertEquals(50, affix.rolledStats().get(HEALTH).value());
        }
        
        @Test
        @DisplayName("should create with multiple stats")
        void shouldCreateWithMultipleStats() {
            Map<String, RolledAffix.RolledStat> stats = new HashMap<>();
            stats.put(HEALTH, new RolledAffix.RolledStat(100, HyforgedModifier.StackType.FLAT));
            stats.put(ARMOR, new RolledAffix.RolledStat(25, HyforgedModifier.StackType.FLAT));
            
            RolledAffix affix = new RolledAffix(
                    "titan",
                    "suffix",
                    1,
                    stats
            );
            
            assertEquals(2, affix.getStatCount());
            assertTrue(affix.grantsStat(HEALTH));
            assertTrue(affix.grantsStat(ARMOR));
            assertEquals(100, affix.rolledStats().get(HEALTH).value());
            assertEquals(25, affix.rolledStats().get(ARMOR).value());
        }
        
        @Test
        @DisplayName("should reject null affixId")
        void shouldRejectNullAffixId() {
            Map<String, RolledAffix.RolledStat> stats = singleStat(HEALTH, 50, HyforgedModifier.StackType.FLAT);
            
            assertThrows(NullPointerException.class, () -> new RolledAffix(
                    null,
                    "prefix",
                    1,
                    stats
            ));
        }
        
        @Test
        @DisplayName("should reject blank affixId")
        void shouldRejectBlankAffixId() {
            Map<String, RolledAffix.RolledStat> stats = singleStat(HEALTH, 50, HyforgedModifier.StackType.FLAT);
            
            assertThrows(IllegalArgumentException.class, () -> new RolledAffix(
                    "",
                    "prefix",
                    1,
                    stats
            ));
        }
        
        @Test
        @DisplayName("should reject null type")
        void shouldRejectNullType() {
            Map<String, RolledAffix.RolledStat> stats = singleStat(HEALTH, 50, HyforgedModifier.StackType.FLAT);
            
            assertThrows(NullPointerException.class, () -> new RolledAffix(
                    "sturdy",
                    null,
                    1,
                    stats
            ));
        }
        
        @Test
        @DisplayName("should reject blank type")
        void shouldRejectBlankType() {
            Map<String, RolledAffix.RolledStat> stats = singleStat(HEALTH, 50, HyforgedModifier.StackType.FLAT);
            
            assertThrows(IllegalArgumentException.class, () -> new RolledAffix(
                    "sturdy",
                    "",
                    1,
                    stats
            ));
        }
        
        @Test
        @DisplayName("should allow empty rolledStats")
        void shouldAllowEmptyRolledStats() {
            RolledAffix affix = new RolledAffix(
                "sturdy",
                "prefix",
                1,
                Map.of()
            );

            assertEquals(0, affix.getStatCount());
            assertTrue(affix.rolledStats().isEmpty());
        }
        
        @Test
        @DisplayName("should reject tier less than 1")
        void shouldRejectTierLessThanOne() {
            Map<String, RolledAffix.RolledStat> stats = singleStat(HEALTH, 50, HyforgedModifier.StackType.FLAT);
            
            assertThrows(IllegalArgumentException.class, () -> new RolledAffix(
                    "sturdy",
                    "prefix",
                    0,
                    stats
            ));
        }
        
        @Test
        @DisplayName("should reject negative tier")
        void shouldRejectNegativeTier() {
            Map<String, RolledAffix.RolledStat> stats = singleStat(HEALTH, 50, HyforgedModifier.StackType.FLAT);
            
            assertThrows(IllegalArgumentException.class, () -> new RolledAffix(
                    "sturdy",
                    "prefix",
                    -1,
                    stats
            ));
        }
        
        @Test
        @DisplayName("should allow negative values")
        void shouldAllowNegativeValues() {
            Map<String, RolledAffix.RolledStat> stats = singleStat(HEALTH, -10, HyforgedModifier.StackType.FLAT);
            
            RolledAffix affix = new RolledAffix(
                    "weak",
                    "prefix",
                    1,
                    stats
            );
            assertEquals(-10, affix.rolledStats().get(HEALTH).value());
        }
    }
    
    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {
        
        @Test
        @DisplayName("from() should create from AffixDefinition")
        void fromShouldCreateFromDefinition() {
            AffixDefinition definition = new AffixDefinition(
                    "sturdy",
                    "prefix",
                    "Sturdy",
                    List.of(AffixTestFixtures.tier(1, 1, 100, HEALTH, HyforgedModifier.StackType.FLAT, 50, 100)),
                    100
            );
            
            Map<String, RolledAffix.RolledStat> stats = singleStat(HEALTH, 75, HyforgedModifier.StackType.FLAT);
            RolledAffix affix = RolledAffix.from(definition, 1, stats);
            
            assertEquals("sturdy", affix.affixId());
            assertEquals("prefix", affix.type());
            assertEquals(1, affix.tier());
            assertEquals(75, affix.rolledStats().get(HEALTH).value());
        }
    }
    
    @Nested
    @DisplayName("toModifiers()")
    class ToModifiers {
        
        @Test
        @DisplayName("should create modifier with correct values for single stat")
        void shouldCreateModifierWithCorrectValues() {
            Map<String, RolledAffix.RolledStat> stats = singleStat(HEALTH, 50, HyforgedModifier.StackType.FLAT);
            
            RolledAffix affix = new RolledAffix(
                    "sturdy",
                    "prefix",
                    1,
                    stats
            );
            
            List<HyforgedModifier> modifiers = affix.toModifiers("test-source");
            
            assertEquals(1, modifiers.size());
            HyforgedModifier modifier = modifiers.get(0);
            assertEquals(HyforgedModifier.StackType.FLAT, modifier.getStackType());
            assertEquals(50, modifier.getAmount());
            assertEquals(HyforgedModifier.SourceType.EQUIPMENT, modifier.getSourceType());
            assertEquals("test-source", modifier.getSourceId());
        }
        
        @Test
        @DisplayName("should create modifiers for multiple stats")
        void shouldCreateModifiersForMultipleStats() {
            Map<String, RolledAffix.RolledStat> stats = new HashMap<>();
            stats.put(HEALTH, new RolledAffix.RolledStat(100, HyforgedModifier.StackType.FLAT));
            stats.put(ARMOR, new RolledAffix.RolledStat(25, HyforgedModifier.StackType.FLAT));
            
            RolledAffix affix = new RolledAffix(
                    "titan",
                    "suffix",
                    1,
                    stats
            );
            
            List<HyforgedModifier> modifiers = affix.toModifiers("src");
            assertEquals(2, modifiers.size());
        }
        
        @Test
        @DisplayName("should preserve modifier type")
        void shouldPreserveModifierType() {
            Map<String, RolledAffix.RolledStat> stats = singleStat(HEALTH, 1500, HyforgedModifier.StackType.INCREASED);
            
            RolledAffix affix = new RolledAffix(
                    "increased",
                    "suffix",
                    2,
                    stats
            );
            
            List<HyforgedModifier> modifiers = affix.toModifiers("src");
            assertEquals(HyforgedModifier.StackType.INCREASED, modifiers.get(0).getStackType());
        }
    }
    
    @Nested
    @DisplayName("toDisplayString()")
    class ToDisplayString {
        
        @Test
        @DisplayName("should format single stat")
        void shouldFormatSingleStat() {
            Map<String, RolledAffix.RolledStat> stats = singleStat(HEALTH, 50, HyforgedModifier.StackType.FLAT);
            
            RolledAffix affix = new RolledAffix(
                    "sturdy",
                    "prefix",
                    1,
                    stats
            );
            
            String display = affix.toDisplayString();
            assertNotNull(display);
            assertTrue(display.contains("50"));
        }
        
        @Test
        @DisplayName("should format negative value")
        void shouldFormatNegativeValue() {
            Map<String, RolledAffix.RolledStat> stats = singleStat(ARMOR, -10, HyforgedModifier.StackType.FLAT);
            
            RolledAffix affix = new RolledAffix(
                    "weak",
                    "prefix",
                    1,
                    stats
            );
            
            String display = affix.toDisplayString();
            assertNotNull(display);
            assertTrue(display.contains("-10"));
        }
    }
    
    @Nested
    @DisplayName("Data Conversion")
    class DataConversion {
        
        @Test
        @DisplayName("RolledAffixData should convert to RolledAffix")
        void dataToAffixShouldWork() {
            RolledAffix.RolledAffixData data = new RolledAffix.RolledAffixData();
            data.affixId = "sturdy";
            data.type = "prefix";
            data.tier = 1;
            data.stats.put(HEALTH, new RolledAffix.RolledStatData(new RolledAffix.RolledStat(50, HyforgedModifier.StackType.FLAT)));
            
            RolledAffix affix = data.toRolledAffix();
            
            assertEquals("sturdy", affix.affixId());
            assertEquals("prefix", affix.type());
            assertEquals(1, affix.tier());
            assertEquals(50, affix.rolledStats().get(HEALTH).value());
        }
        
        @Test
        @DisplayName("RolledAffixData round-trip should preserve data")
        void roundTripShouldPreserveData() {
            Map<String, RolledAffix.RolledStat> stats = new HashMap<>();
            stats.put(HEALTH, new RolledAffix.RolledStat(100, HyforgedModifier.StackType.FLAT));
            stats.put(DAMAGE, new RolledAffix.RolledStat(25, HyforgedModifier.StackType.INCREASED));
            
            RolledAffix original = new RolledAffix(
                    "sharp",
                    "suffix",
                    3,
                    stats
            );
            
            RolledAffix.RolledAffixData data = new RolledAffix.RolledAffixData(original);
            RolledAffix restored = data.toRolledAffix();
            
            assertEquals(original.affixId(), restored.affixId());
            assertEquals(original.type(), restored.type());
            assertEquals(original.tier(), restored.tier());
            assertEquals(original.getStatCount(), restored.getStatCount());
            assertEquals(original.rolledStats().get(HEALTH).value(), restored.rolledStats().get(HEALTH).value());
            assertEquals(original.rolledStats().get(DAMAGE).value(), restored.rolledStats().get(DAMAGE).value());
        }
    }
}
