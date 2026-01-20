package reign.software.hyforged.affix.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffixType record.
 */
class AffixTypeTest {

    @Test
    void constructor_validInput_createsInstance() {
        AffixType type = new AffixType(
                "prefix",
                AffixType.DisplayNamePosition.BEFORE,
                "{name}",
                true
        );
        
        assertEquals("prefix", type.id());
        assertEquals(AffixType.DisplayNamePosition.BEFORE, type.displayNamePosition());
        assertEquals("{name}", type.displayFormat());
        assertTrue(type.stackable());
    }

    @Test
    void constructor_nullId_throwsException() {
        assertThrows(NullPointerException.class, () -> 
            new AffixType(null, AffixType.DisplayNamePosition.BEFORE, "{name}", true));
    }

    @Test
    void constructor_blankId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            new AffixType("  ", AffixType.DisplayNamePosition.BEFORE, "{name}", true));
    }

    @Test
    void constructor_nullPosition_throwsException() {
        assertThrows(NullPointerException.class, () -> 
            new AffixType("prefix", null, "{name}", true));
    }

    @Test
    void isPrefix_beforePosition_returnsTrue() {
        AffixType type = new AffixType("test", AffixType.DisplayNamePosition.BEFORE, "{name}", true);
        assertTrue(type.isPrefix());
        assertFalse(type.isSuffix());
    }

    @Test
    void isSuffix_afterPosition_returnsTrue() {
        AffixType type = new AffixType("test", AffixType.DisplayNamePosition.AFTER, "{name}", true);
        assertFalse(type.isPrefix());
        assertTrue(type.isSuffix());
    }

    @Test
    void formatDisplay_replacesPlaceholders() {
        AffixType type = new AffixType("test", AffixType.DisplayNamePosition.BEFORE, "[T{tier}] {name}", true);
        String result = type.formatDisplay("Sturdy", 2);
        assertEquals("[T2] Sturdy", result);
    }

    @Test
    void displayNamePosition_fromJson_validValues() {
        assertEquals(AffixType.DisplayNamePosition.BEFORE, 
                AffixType.DisplayNamePosition.fromJson("before"));
        assertEquals(AffixType.DisplayNamePosition.AFTER, 
                AffixType.DisplayNamePosition.fromJson("after"));
        assertEquals(AffixType.DisplayNamePosition.NONE, 
                AffixType.DisplayNamePosition.fromJson("none"));
    }

    @Test
    void displayNamePosition_fromJson_caseInsensitive() {
        assertEquals(AffixType.DisplayNamePosition.BEFORE, 
                AffixType.DisplayNamePosition.fromJson("BEFORE"));
        assertEquals(AffixType.DisplayNamePosition.AFTER, 
                AffixType.DisplayNamePosition.fromJson("After"));
    }

    @Test
    void displayNamePosition_fromJson_invalidValue_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                AffixType.DisplayNamePosition.fromJson("invalid"));
    }

    @Test
    void displayNamePosition_getJsonValue_returnsCorrectValue() {
        assertEquals("before", AffixType.DisplayNamePosition.BEFORE.getJsonValue());
        assertEquals("after", AffixType.DisplayNamePosition.AFTER.getJsonValue());
        assertEquals("none", AffixType.DisplayNamePosition.NONE.getJsonValue());
    }
}
