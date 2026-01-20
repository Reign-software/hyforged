package reign.software.hyforged.affix.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RolledAffix} record.
 */
@DisplayName("RolledAffix")
class RolledAffixTest {
    
    private static final StatId TEST_STAT = StatId.hyforged("health");
    
    @Nested
    @DisplayName("Construction")
    class Construction {
        
        @Test
        @DisplayName("should create with valid parameters")
        void shouldCreateWithValidParameters() {
            RolledAffix affix = new RolledAffix(
                    "sturdy",
                    "prefix",
                    1,
                    50,
                    TEST_STAT,
                    HyforgedModifier.StackType.FLAT
            );
            
            assertEquals("sturdy", affix.affixId());
            assertEquals("prefix", affix.type());
            assertEquals(1, affix.tier());
            assertEquals(50, affix.value());
            assertEquals(TEST_STAT, affix.statId());
            assertEquals(HyforgedModifier.StackType.FLAT, affix.modifierType());
        }
        
        @Test
        @DisplayName("should reject null affixId")
        void shouldRejectNullAffixId() {
            assertThrows(NullPointerException.class, () -> new RolledAffix(
                    null,
                    "prefix",
                    1,
                    50,
                    TEST_STAT,
                    HyforgedModifier.StackType.FLAT
            ));
        }
        
        @Test
        @DisplayName("should reject blank affixId")
        void shouldRejectBlankAffixId() {
            assertThrows(IllegalArgumentException.class, () -> new RolledAffix(
                    "",
                    "prefix",
                    1,
                    50,
                    TEST_STAT,
                    HyforgedModifier.StackType.FLAT
            ));
        }
        
        @Test
        @DisplayName("should reject null type")
        void shouldRejectNullType() {
            assertThrows(NullPointerException.class, () -> new RolledAffix(
                    "sturdy",
                    null,
                    1,
                    50,
                    TEST_STAT,
                    HyforgedModifier.StackType.FLAT
            ));
        }
        
        @Test
        @DisplayName("should reject blank type")
        void shouldRejectBlankType() {
            assertThrows(IllegalArgumentException.class, () -> new RolledAffix(
                    "sturdy",
                    "",
                    1,
                    50,
                    TEST_STAT,
                    HyforgedModifier.StackType.FLAT
            ));
        }
        
        @Test
        @DisplayName("should reject null statId")
        void shouldRejectNullStatId() {
            assertThrows(NullPointerException.class, () -> new RolledAffix(
                    "sturdy",
                    "prefix",
                    1,
                    50,
                    null,
                    HyforgedModifier.StackType.FLAT
            ));
        }
        
        @Test
        @DisplayName("should reject null modifierType")
        void shouldRejectNullModifierType() {
            assertThrows(NullPointerException.class, () -> new RolledAffix(
                    "sturdy",
                    "prefix",
                    1,
                    50,
                    TEST_STAT,
                    null
            ));
        }
        
        @Test
        @DisplayName("should reject tier less than 1")
        void shouldRejectTierLessThanOne() {
            assertThrows(IllegalArgumentException.class, () -> new RolledAffix(
                    "sturdy",
                    "prefix",
                    0,
                    50,
                    TEST_STAT,
                    HyforgedModifier.StackType.FLAT
            ));
        }
        
        @Test
        @DisplayName("should allow negative tier error")
        void shouldRejectNegativeTier() {
            assertThrows(IllegalArgumentException.class, () -> new RolledAffix(
                    "sturdy",
                    "prefix",
                    -1,
                    50,
                    TEST_STAT,
                    HyforgedModifier.StackType.FLAT
            ));
        }
        
        @Test
        @DisplayName("should allow negative values")
        void shouldAllowNegativeValues() {
            RolledAffix affix = new RolledAffix(
                    "weak",
                    "prefix",
                    1,
                    -10,
                    TEST_STAT,
                    HyforgedModifier.StackType.FLAT
            );
            assertEquals(-10, affix.value());
        }
    }
    
    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {
        
        @Test
        @DisplayName("from() should create from AffixDefinition")
        void fromShouldCreateFromDefinition() {
            AffixTierDefinition tier = new AffixTierDefinition(1, 50, 100, 1);
            AffixDefinition definition = new AffixDefinition(
                    "sturdy",
                    "prefix",
                    "Sturdy",
                    TEST_STAT,
                    HyforgedModifier.StackType.FLAT,
                    java.util.List.of(tier),
                    AffixEligibility.ANY,
                    100
            );
            
            RolledAffix affix = RolledAffix.from(definition, 1, 75);
            
            assertEquals("sturdy", affix.affixId());
            assertEquals("prefix", affix.type());
            assertEquals(1, affix.tier());
            assertEquals(75, affix.value());
            assertEquals(TEST_STAT, affix.statId());
            assertEquals(HyforgedModifier.StackType.FLAT, affix.modifierType());
        }
    }
    
    @Nested
    @DisplayName("toModifier()")
    class ToModifier {
        
        @Test
        @DisplayName("should create modifier with correct values")
        void shouldCreateModifierWithCorrectValues() {
            RolledAffix affix = new RolledAffix(
                    "sturdy",
                    "prefix",
                    1,
                    50,
                    TEST_STAT,
                    HyforgedModifier.StackType.FLAT
            );
            
            HyforgedModifier modifier = affix.toModifier("test-source");
            
            assertEquals(HyforgedModifier.StackType.FLAT, modifier.getStackType());
            assertEquals(50, modifier.getAmount());
            assertEquals(HyforgedModifier.SourceType.EQUIPMENT, modifier.getSourceType());
            assertEquals("test-source", modifier.getSourceId());
        }
        
        @Test
        @DisplayName("should preserve modifier type")
        void shouldPreserveModifierType() {
            RolledAffix affix = new RolledAffix(
                    "increased",
                    "suffix",
                    2,
                    1500,
                    TEST_STAT,
                    HyforgedModifier.StackType.INCREASED
            );
            
            HyforgedModifier modifier = affix.toModifier("src");
            assertEquals(HyforgedModifier.StackType.INCREASED, modifier.getStackType());
        }
    }
    
    @Nested
    @DisplayName("toDisplayString()")
    class ToDisplayString {
        
        @Test
        @DisplayName("should format FLAT positive value")
        void shouldFormatFlatPositive() {
            RolledAffix affix = new RolledAffix(
                    "sturdy",
                    "prefix",
                    1,
                    50,
                    StatId.hyforged("health"),
                    HyforgedModifier.StackType.FLAT
            );
            
            assertEquals("+50 health", affix.toDisplayString());
        }
        
        @Test
        @DisplayName("should format FLAT negative value")
        void shouldFormatFlatNegative() {
            RolledAffix affix = new RolledAffix(
                    "weak",
                    "prefix",
                    1,
                    -10,
                    StatId.hyforged("armor"),
                    HyforgedModifier.StackType.FLAT
            );
            
            assertEquals("-10 armor", affix.toDisplayString());
        }
        
        @Test
        @DisplayName("should format INCREASED with percent")
        void shouldFormatIncreasedWithPercent() {
            RolledAffix affix = new RolledAffix(
                    "mighty",
                    "prefix",
                    1,
                    15,
                    StatId.hyforged("damage"),
                    HyforgedModifier.StackType.INCREASED
            );
            
            assertEquals("+15% damage", affix.toDisplayString());
        }
        
        @Test
        @DisplayName("should format MORE with percent")
        void shouldFormatMoreWithPercent() {
            RolledAffix affix = new RolledAffix(
                    "empowered",
                    "forged",
                    1,
                    10,
                    StatId.hyforged("crit"),
                    HyforgedModifier.StackType.MORE
            );
            
            assertEquals("+10% crit", affix.toDisplayString());
        }
    }
    
    @Nested
    @DisplayName("Data Conversion")
    class DataConversion {
        
        @Test
        @DisplayName("toData() should create RolledAffixData")
        void toDataShouldCreateData() {
            RolledAffix affix = new RolledAffix(
                    "sturdy",
                    "prefix",
                    1,
                    50,
                    TEST_STAT,
                    HyforgedModifier.StackType.FLAT
            );
            
            RolledAffix.RolledAffixData data = affix.toData();
            
            assertEquals("sturdy", data.affixId);
            assertEquals("prefix", data.type);
            assertEquals(1, data.tier);
            assertEquals(50, data.value);
            assertEquals(TEST_STAT.toString(), data.statIdStr);
            assertEquals(HyforgedModifier.StackType.FLAT, data.modifierType);
        }
        
        @Test
        @DisplayName("RolledAffixData.toRolledAffix() should round-trip")
        void dataToAffixShouldRoundTrip() {
            RolledAffix original = new RolledAffix(
                    "sharp",
                    "suffix",
                    3,
                    25,
                    StatId.hyforged("damage"),
                    HyforgedModifier.StackType.INCREASED
            );
            
            RolledAffix.RolledAffixData data = original.toData();
            RolledAffix restored = data.toRolledAffix();
            
            assertEquals(original.affixId(), restored.affixId());
            assertEquals(original.type(), restored.type());
            assertEquals(original.tier(), restored.tier());
            assertEquals(original.value(), restored.value());
            assertEquals(original.statId(), restored.statId());
            assertEquals(original.modifierType(), restored.modifierType());
        }
    }
}
