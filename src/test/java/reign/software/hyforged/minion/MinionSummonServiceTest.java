package reign.software.hyforged.minion;

import reign.software.hyforged.minion.component.MinionTrackerComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MinionSummonService")
class MinionSummonServiceTest {

    @Nested
    @DisplayName("Ability ID generation")
    class AbilityIdGeneration {

        @Test
        @DisplayName("generates first ability ID when tracker is null")
        void firstIdWithNullTracker() {
            MinionSummonService service = MinionSummonService.get();
            String id = service.resolveNextAbilityId(null, "hyforged:skeleton-warrior");
            assertEquals("minion:hyforged:skeleton-warrior:0", id);
        }

        @Test
        @DisplayName("generates first ability ID when tracker is empty")
        void firstIdWithEmptyTracker() {
            MinionSummonService service = MinionSummonService.get();
            MinionTrackerComponent tracker = new MinionTrackerComponent();
            String id = service.resolveNextAbilityId(tracker, "hyforged:skeleton-warrior");
            assertEquals("minion:hyforged:skeleton-warrior:0", id);
        }

        // Note: Tests for index incrementing, gap finding, and independent type indices
        // require a populated MinionTrackerComponent, which needs Ref<EntityStore> (ECS infrastructure).
        // These are deferred to integration tests.
    }

    @Nested
    @DisplayName("Ability ID prefix")
    class AbilityIdPrefix {

        @Test
        @DisplayName("MINION_ABILITY_PREFIX is correct")
        void prefixValue() {
            assertEquals("minion:", MinionSummonService.MINION_ABILITY_PREFIX);
        }

        @Test
        @DisplayName("generated IDs start with MINION_ABILITY_PREFIX")
        void generatedIdStartsWithPrefix() {
            MinionSummonService service = MinionSummonService.get();
            String id = service.resolveNextAbilityId(null, "hyforged:test");
            assertTrue(id.startsWith(MinionSummonService.MINION_ABILITY_PREFIX));
        }
    }

    @Nested
    @DisplayName("parseMinionTypeId")
    class ParseMinionTypeId {

        @Test
        @DisplayName("parses typeId from standard ability ID")
        void parsesStandardId() {
            String typeId = MinionSummonService.parseMinionTypeId("minion:hyforged:skeleton-warrior:0");
            assertEquals("hyforged:skeleton-warrior", typeId);
        }

        @Test
        @DisplayName("parses typeId with multi-digit index")
        void parsesMultiDigitIndex() {
            String typeId = MinionSummonService.parseMinionTypeId("minion:hyforged:mage:12");
            assertEquals("hyforged:mage", typeId);
        }

        @Test
        @DisplayName("returns null for malformed ID with no index separator")
        void returnsNullForMalformedId() {
            String typeId = MinionSummonService.parseMinionTypeId("minion:noindex");
            assertNull(typeId);
        }

        @Test
        @DisplayName("handles typeId with multiple colons correctly")
        void handlesMultipleColons() {
            // "minion:ns:sub:type:3" → typeId = "ns:sub:type"
            String typeId = MinionSummonService.parseMinionTypeId("minion:ns:sub:type:3");
            assertEquals("ns:sub:type", typeId);
        }
    }
}
