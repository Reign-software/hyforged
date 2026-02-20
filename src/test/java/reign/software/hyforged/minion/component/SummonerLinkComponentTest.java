package reign.software.hyforged.minion.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SummonerLinkComponent")
class SummonerLinkComponentTest {

    @Test
    @DisplayName("default constructor initializes all fields to null/defaults")
    void defaultConstructorNullFields() {
        SummonerLinkComponent link = new SummonerLinkComponent();
        assertNull(link.getSummonerUuid());
        assertNull(link.getMinionTypeId());
        assertNull(link.getConcentrationAbilityId());
        assertEquals(0L, link.getSummonTimestamp());
    }

    @Test
    @DisplayName("setters and getters round-trip correctly")
    void settersAndGettersRoundTrip() {
        SummonerLinkComponent link = new SummonerLinkComponent();
        UUID uuid = UUID.randomUUID();

        link.setSummonerUuid(uuid);
        link.setMinionTypeId("hyforged:skeleton-warrior");
        link.setConcentrationAbilityId("minion:hyforged:skeleton-warrior:0");
        link.setSummonTimestamp(123456789L);

        assertEquals(uuid, link.getSummonerUuid());
        assertEquals("hyforged:skeleton-warrior", link.getMinionTypeId());
        assertEquals("minion:hyforged:skeleton-warrior:0", link.getConcentrationAbilityId());
        assertEquals(123456789L, link.getSummonTimestamp());
    }

    @Test
    @DisplayName("clone produces independent copy")
    void cloneProducesIndependentCopy() {
        SummonerLinkComponent original = new SummonerLinkComponent();
        UUID uuid = UUID.randomUUID();
        original.setSummonerUuid(uuid);
        original.setMinionTypeId("hyforged:kweebec-sapling");
        original.setConcentrationAbilityId("minion:hyforged:kweebec-sapling:0");
        original.setSummonTimestamp(999L);

        SummonerLinkComponent copy = original.clone();

        assertEquals(uuid, copy.getSummonerUuid());
        assertEquals("hyforged:kweebec-sapling", copy.getMinionTypeId());
        assertEquals("minion:hyforged:kweebec-sapling:0", copy.getConcentrationAbilityId());
        assertEquals(999L, copy.getSummonTimestamp());

        // Mutate original — copy should be unaffected
        original.setSummonerUuid(UUID.randomUUID());
        original.setMinionTypeId("changed");
        assertNotEquals(original.getSummonerUuid(), copy.getSummonerUuid());
        assertEquals("hyforged:kweebec-sapling", copy.getMinionTypeId());
    }

    @Test
    @DisplayName("nullable fields accept null after being set")
    void nullableFieldsAcceptNull() {
        SummonerLinkComponent link = new SummonerLinkComponent();
        link.setSummonerUuid(UUID.randomUUID());
        link.setMinionTypeId("test");
        link.setConcentrationAbilityId("test:id");

        link.setSummonerUuid(null);
        link.setMinionTypeId(null);
        link.setConcentrationAbilityId(null);

        assertNull(link.getSummonerUuid());
        assertNull(link.getMinionTypeId());
        assertNull(link.getConcentrationAbilityId());
    }

    @Nested
    @DisplayName("Copy constructor")
    class CopyConstructor {

        @Test
        @DisplayName("copy constructor duplicates all fields")
        void copyConstructorDuplicatesAllFields() {
            SummonerLinkComponent original = new SummonerLinkComponent();
            UUID uuid = UUID.randomUUID();
            original.setSummonerUuid(uuid);
            original.setMinionTypeId("type1");
            original.setConcentrationAbilityId("ability1");
            original.setSummonTimestamp(42L);

            SummonerLinkComponent copy = new SummonerLinkComponent(original);

            assertEquals(uuid, copy.getSummonerUuid());
            assertEquals("type1", copy.getMinionTypeId());
            assertEquals("ability1", copy.getConcentrationAbilityId());
            assertEquals(42L, copy.getSummonTimestamp());
        }

        @Test
        @DisplayName("copy constructor handles null fields")
        void copyConstructorHandlesNullFields() {
            SummonerLinkComponent original = new SummonerLinkComponent();
            SummonerLinkComponent copy = new SummonerLinkComponent(original);

            assertNull(copy.getSummonerUuid());
            assertNull(copy.getMinionTypeId());
            assertNull(copy.getConcentrationAbilityId());
            assertEquals(0L, copy.getSummonTimestamp());
        }
    }
}
