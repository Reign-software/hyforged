package reign.software.hyforged.combat.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DamageSpec}.
 */
@DisplayName("DamageSpec")
class DamageSpecTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("of() creates spec with single damage entry")
        void ofCreatesSingleEntry() {
            DamageSpec spec = DamageSpec.of("Physical", 50);

            assertEquals(1, spec.getDamageEntries().size());
            assertEquals("Physical", spec.getDamageEntries().get(0).damageCauseId());
            assertEquals(50, spec.getDamageEntries().get(0).amount());
        }

        @Test
        @DisplayName("of() returns spec with default flags")
        void ofHasDefaultFlags() {
            DamageSpec spec = DamageSpec.of("Fire", 100);

            assertFalse(spec.isForceCrit());
            assertFalse(spec.isNoCrit());
            assertFalse(spec.isSkipEvasion());
            assertFalse(spec.isSkipBlock());
            assertFalse(spec.isSkipResistance());
            assertFalse(spec.isSkipAilments());
            assertNull(spec.getSourceDescription());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builder creates empty spec that throws on build")
        void emptyBuilderThrows() {
            assertThrows(IllegalStateException.class, () -> 
                DamageSpec.builder().build()
            );
        }

        @Test
        @DisplayName("builder with single damage entry builds successfully")
        void singleEntryBuilds() {
            DamageSpec spec = DamageSpec.builder()
                    .addDamage("Physical", 25)
                    .build();

            assertFalse(spec.isEmpty());
            assertEquals(25, spec.getTotalBaseDamage());
        }

        @Test
        @DisplayName("builder with multiple damage entries sums total")
        void multipleDamageEntriesSum() {
            DamageSpec spec = DamageSpec.builder()
                    .addDamage("Physical", 30)
                    .addDamage("Fire", 20)
                    .addDamage("Lightning", 10)
                    .build();

            assertEquals(3, spec.getDamageEntries().size());
            assertEquals(60, spec.getTotalBaseDamage());
        }

        @Test
        @DisplayName("forceCrit enables crit and disables noCrit")
        void forceCritDisablesNoCrit() {
            DamageSpec spec = DamageSpec.builder()
                    .addDamage("Physical", 50)
                    .noCrit(true)
                    .forceCrit(true)
                    .build();

            assertTrue(spec.isForceCrit());
            assertFalse(spec.isNoCrit());
        }

        @Test
        @DisplayName("noCrit enables noCrit and disables forceCrit")
        void noCritDisablesForceCrit() {
            DamageSpec spec = DamageSpec.builder()
                    .addDamage("Physical", 50)
                    .forceCrit(true)
                    .noCrit(true)
                    .build();

            assertFalse(spec.isForceCrit());
            assertTrue(spec.isNoCrit());
        }

        @Test
        @DisplayName("skipEvasion flag is set correctly")
        void skipEvasionFlag() {
            DamageSpec spec = DamageSpec.builder()
                    .addDamage("Magic", 100)
                    .skipEvasion(true)
                    .build();

            assertTrue(spec.isSkipEvasion());
        }

        @Test
        @DisplayName("skipBlock flag is set correctly")
        void skipBlockFlag() {
            DamageSpec spec = DamageSpec.builder()
                    .addDamage("Physical", 100)
                    .skipBlock(true)
                    .build();

            assertTrue(spec.isSkipBlock());
        }

        @Test
        @DisplayName("skipResistance flag is set correctly")
        void skipResistanceFlag() {
            DamageSpec spec = DamageSpec.builder()
                    .addDamage("Physical", 100)
                    .skipResistance(true)
                    .build();

            assertTrue(spec.isSkipResistance());
        }

        @Test
        @DisplayName("skipAilments flag is set correctly")
        void skipAilmentsFlag() {
            DamageSpec spec = DamageSpec.builder()
                    .addDamage("Fire", 100)
                    .skipAilments(true)
                    .build();

            assertTrue(spec.isSkipAilments());
        }

        @Test
        @DisplayName("sourceDescription is set correctly")
        void sourceDescriptionSet() {
            DamageSpec spec = DamageSpec.builder()
                    .addDamage("Fire", 75)
                    .sourceDescription("Fireball")
                    .build();

            assertEquals("Fireball", spec.getSourceDescription());
        }
    }

    @Nested
    @DisplayName("DamageEntry")
    class DamageEntryTests {

        @Test
        @DisplayName("DamageEntry requires non-null damageCauseId")
        void requiresNonNullId() {
            assertThrows(NullPointerException.class, () ->
                new DamageSpec.DamageEntry(null, 50)
            );
        }

        @Test
        @DisplayName("DamageEntry rejects negative amount")
        void rejectsNegativeAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                new DamageSpec.DamageEntry("Physical", -10)
            );
        }

        @Test
        @DisplayName("DamageEntry accepts zero amount")
        void acceptsZeroAmount() {
            DamageSpec.DamageEntry entry = new DamageSpec.DamageEntry("Physical", 0);
            assertEquals(0, entry.amount());
        }

        @Test
        @DisplayName("DamageEntry stores values correctly")
        void storesValues() {
            DamageSpec.DamageEntry entry = new DamageSpec.DamageEntry("Fire", 123.5f);
            assertEquals("Fire", entry.damageCauseId());
            assertEquals(123.5f, entry.amount());
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("getDamageEntries returns unmodifiable list")
        void entriesUnmodifiable() {
            DamageSpec spec = DamageSpec.of("Physical", 50);
            
            assertThrows(UnsupportedOperationException.class, () ->
                spec.getDamageEntries().add(new DamageSpec.DamageEntry("Fire", 25))
            );
        }
    }
}
