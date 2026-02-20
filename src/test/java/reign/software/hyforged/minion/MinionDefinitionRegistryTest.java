package reign.software.hyforged.minion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MinionDefinitionRegistry")
class MinionDefinitionRegistryTest {

    @BeforeEach
    void setUp() {
        MinionDefinitionRegistry.get().clear();
    }

    @Test
    @DisplayName("registers and retrieves a definition by ID")
    void registerAndRetrieve() {
        MinionDefinition def = new MinionDefinition(
                "hyforged:skeleton-warrior", "Template_SkeletonWarrior",
                25, 10, 0, 2.0f, 0f, 0f, List.of("undead", "melee")
        );
        MinionDefinitionRegistry.get().register(def);

        MinionDefinition result = MinionDefinitionRegistry.get().get("hyforged:skeleton-warrior");
        assertNotNull(result);
        assertEquals("hyforged:skeleton-warrior", result.getId());
        assertEquals("Template_SkeletonWarrior", result.getNpcTemplate());
        assertEquals(25, result.getConcentrationCost());
        assertEquals(10, result.getDefaultPriority());
    }

    @Test
    @DisplayName("returns null for unknown ID")
    void unknownIdReturnsNull() {
        assertNull(MinionDefinitionRegistry.get().get("hyforged:nonexistent"));
    }

    @Test
    @DisplayName("ignores duplicate registrations")
    void duplicateRegistrationIgnored() {
        MinionDefinition def1 = new MinionDefinition(
                "hyforged:warrior", "Template_A", 10, 1, 0, 0f, 0f, 0f, List.of()
        );
        MinionDefinition def2 = new MinionDefinition(
                "hyforged:warrior", "Template_B", 20, 2, 0, 0f, 0f, 0f, List.of()
        );
        MinionDefinitionRegistry.get().register(def1);
        MinionDefinitionRegistry.get().register(def2);

        assertEquals(1, MinionDefinitionRegistry.get().size());
        assertEquals("Template_A", MinionDefinitionRegistry.get().get("hyforged:warrior").getNpcTemplate());
    }

    @Test
    @DisplayName("clear removes all definitions")
    void clearRemovesAll() {
        MinionDefinitionRegistry.get().register(new MinionDefinition(
                "hyforged:a", "T_A", 10, 1, 0, 0f, 0f, 0f, List.of()
        ));
        MinionDefinitionRegistry.get().register(new MinionDefinition(
                "hyforged:b", "T_B", 20, 2, 0, 0f, 0f, 0f, List.of()
        ));

        assertEquals(2, MinionDefinitionRegistry.get().size());
        MinionDefinitionRegistry.get().clear();
        assertEquals(0, MinionDefinitionRegistry.get().size());
    }

    @Test
    @DisplayName("loads definitions from JSON resources")
    void loadFromJsonResources() {
        MinionDefinitionRegistry.get().loadFromResources(List.of(
                "Server/Hyforged/Minions/SkeletonWarrior.json",
                "Server/Hyforged/Minions/KweebecSapling.json"
        ));

        assertTrue(MinionDefinitionRegistry.get().size() >= 2,
                "Should load at least 2 minion definitions");

        // Verify one of the loaded definitions
        MinionDefinition skeletonWarrior = MinionDefinitionRegistry.get().get("hyforged:skeleton-warrior");
        if (skeletonWarrior != null) {
            assertNotNull(skeletonWarrior.getNpcTemplate(), "NPC template should not be null");
            assertTrue(skeletonWarrior.getConcentrationCost() > 0, "Cost should be positive");
        }
    }

    @Test
    @DisplayName("handles missing resource files gracefully")
    void missingResourceFileHandledGracefully() {
        assertDoesNotThrow(() ->
                MinionDefinitionRegistry.get().loadFromResources(List.of(
                        "Server/Hyforged/Minions/NonExistent.json"
                ))
        );
        assertEquals(0, MinionDefinitionRegistry.get().size());
    }

    @Test
    @DisplayName("getAll returns unmodifiable map")
    void getAllReturnsUnmodifiableMap() {
        MinionDefinitionRegistry.get().register(new MinionDefinition(
                "hyforged:test", "T", 10, 1, 0, 0f, 0f, 0f, List.of()
        ));

        var all = MinionDefinitionRegistry.get().getAll();
        assertThrows(UnsupportedOperationException.class, () ->
                all.put("hyforged:hack", null)
        );
    }

    @Test
    @DisplayName("definition exposes spawn offset and tags")
    void definitionFieldAccess() {
        MinionDefinition def = new MinionDefinition(
                "hyforged:mage", "Template_Mage", 30, 5, 60,
                1.5f, 0.5f, -1.0f, List.of("magic", "ranged")
        );

        assertEquals(1.5f, def.getSpawnOffsetX(), 0.001f);
        assertEquals(0.5f, def.getSpawnOffsetY(), 0.001f);
        assertEquals(-1.0f, def.getSpawnOffsetZ(), 0.001f);
        assertEquals(60, def.getBaseDuration());
        assertEquals(List.of("magic", "ranged"), def.getTags());
    }
}
