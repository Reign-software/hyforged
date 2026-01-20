package reign.software.hyforged.affix.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AffixSpec}.
 */
@DisplayName("AffixSpec")
class AffixSpecTest {
    
    @Nested
    @DisplayName("Construction")
    class ConstructionTests {
        
        @Test
        @DisplayName("should create spec with all fields")
        void shouldCreateSpecWithAllFields() {
            AffixSpec spec = new AffixSpec("sturdy", 2, 35);
            
            assertEquals("sturdy", spec.affixId());
            assertEquals(2, spec.tier());
            assertEquals(35, spec.value());
            assertTrue(spec.hasTier());
            assertTrue(spec.hasValue());
        }
        
        @Test
        @DisplayName("should create spec with null tier")
        void shouldCreateSpecWithNullTier() {
            AffixSpec spec = new AffixSpec("sturdy", null, 35);
            
            assertEquals("sturdy", spec.affixId());
            assertNull(spec.tier());
            assertEquals(35, spec.value());
            assertFalse(spec.hasTier());
            assertTrue(spec.hasValue());
        }
        
        @Test
        @DisplayName("should create spec with null value")
        void shouldCreateSpecWithNullValue() {
            AffixSpec spec = new AffixSpec("sturdy", 2, null);
            
            assertEquals("sturdy", spec.affixId());
            assertEquals(2, spec.tier());
            assertNull(spec.value());
            assertTrue(spec.hasTier());
            assertFalse(spec.hasValue());
        }
        
        @Test
        @DisplayName("should create spec with null tier and value")
        void shouldCreateSpecWithNullTierAndValue() {
            AffixSpec spec = new AffixSpec("sturdy", null, null);
            
            assertEquals("sturdy", spec.affixId());
            assertNull(spec.tier());
            assertNull(spec.value());
            assertFalse(spec.hasTier());
            assertFalse(spec.hasValue());
        }
        
        @Test
        @DisplayName("should reject null affixId")
        void shouldRejectNullAffixId() {
            assertThrows(NullPointerException.class, () -> {
                new AffixSpec(null, 1, 10);
            });
        }
        
        @Test
        @DisplayName("should reject blank affixId")
        void shouldRejectBlankAffixId() {
            assertThrows(IllegalArgumentException.class, () -> {
                new AffixSpec("", 1, 10);
            });
            assertThrows(IllegalArgumentException.class, () -> {
                new AffixSpec("   ", 1, 10);
            });
        }
        
        @Test
        @DisplayName("should reject tier less than 1")
        void shouldRejectTierLessThan1() {
            assertThrows(IllegalArgumentException.class, () -> {
                new AffixSpec("sturdy", 0, 10);
            });
            assertThrows(IllegalArgumentException.class, () -> {
                new AffixSpec("sturdy", -1, 10);
            });
        }
    }
    
    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {
        
        @Test
        @DisplayName("of(affixId, tier, value) should create full spec")
        void ofWithAllParams() {
            AffixSpec spec = AffixSpec.of("sturdy", 2, 35);
            
            assertEquals("sturdy", spec.affixId());
            assertEquals(2, spec.tier());
            assertEquals(35, spec.value());
        }
        
        @Test
        @DisplayName("of(affixId, tier) should create spec without value")
        void ofWithTierOnly() {
            AffixSpec spec = AffixSpec.of("sturdy", 2);
            
            assertEquals("sturdy", spec.affixId());
            assertEquals(2, spec.tier());
            assertNull(spec.value());
            assertTrue(spec.hasTier());
            assertFalse(spec.hasValue());
        }
        
        @Test
        @DisplayName("of(affixId) should create minimal spec")
        void ofWithAffixIdOnly() {
            AffixSpec spec = AffixSpec.of("sturdy");
            
            assertEquals("sturdy", spec.affixId());
            assertNull(spec.tier());
            assertNull(spec.value());
            assertFalse(spec.hasTier());
            assertFalse(spec.hasValue());
        }
    }
    
    @Nested
    @DisplayName("Require Methods")
    class RequireMethodsTests {
        
        @Test
        @DisplayName("requireTier should return tier when present")
        void requireTierWhenPresent() {
            AffixSpec spec = AffixSpec.of("sturdy", 2, 35);
            assertEquals(2, spec.requireTier());
        }
        
        @Test
        @DisplayName("requireTier should throw when tier is null")
        void requireTierWhenNull() {
            AffixSpec spec = AffixSpec.of("sturdy");
            assertThrows(IllegalStateException.class, spec::requireTier);
        }
        
        @Test
        @DisplayName("requireValue should return value when present")
        void requireValueWhenPresent() {
            AffixSpec spec = AffixSpec.of("sturdy", 2, 35);
            assertEquals(35, spec.requireValue());
        }
        
        @Test
        @DisplayName("requireValue should throw when value is null")
        void requireValueWhenNull() {
            AffixSpec spec = AffixSpec.of("sturdy", 2);
            assertThrows(IllegalStateException.class, spec::requireValue);
        }
    }
    
    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityTests {
        
        @Test
        @DisplayName("equal specs should be equal")
        void equalSpecsShouldBeEqual() {
            AffixSpec spec1 = AffixSpec.of("sturdy", 2, 35);
            AffixSpec spec2 = AffixSpec.of("sturdy", 2, 35);
            
            assertEquals(spec1, spec2);
            assertEquals(spec1.hashCode(), spec2.hashCode());
        }
        
        @Test
        @DisplayName("different specs should not be equal")
        void differentSpecsShouldNotBeEqual() {
            AffixSpec spec1 = AffixSpec.of("sturdy", 2, 35);
            AffixSpec spec2 = AffixSpec.of("sturdy", 3, 35);
            AffixSpec spec3 = AffixSpec.of("mighty", 2, 35);
            
            assertNotEquals(spec1, spec2);
            assertNotEquals(spec1, spec3);
        }
    }
}
