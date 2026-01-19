package reign.software.hyforged.stats.persistence;

import reign.software.hyforged.stats.CoreStats;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Handles schema version migrations for HyforgedStatComponent data.
 * <p>
 * When the component schema changes (new fields, removed fields, changed semantics),
 * this migrator updates old data to match the new schema.
 * <p>
 * Migration is applied after decode based on the persisted version field.
 */
public final class StatDataMigrator {

    private static final Logger LOGGER = Logger.getLogger(StatDataMigrator.class.getName());

    /**
     * Default base value for ability scores.
     */
    private static final int DEFAULT_ABILITY_BASE = 10;

    private StatDataMigrator() {
        // Utility class
    }

    /**
     * Migrate component data from an old schema version to the current version.
     * <p>
     * This is called after decode when the persisted version differs from the current.
     *
     * @param component The component with data from the old schema
     * @param fromVersion The schema version the data was persisted with
     * @param toVersion The current schema version
     */
    public static void migrate(
            @Nonnull HyforgedStatComponent component,
            int fromVersion,
            int toVersion
    ) {
        if (fromVersion >= toVersion) {
            return; // No migration needed
        }

        LOGGER.info(() -> String.format(
                "Migrating HyforgedStatComponent from version %d to %d",
                fromVersion, toVersion
        ));

        // Apply migrations incrementally
        int currentVersion = fromVersion;

        while (currentVersion < toVersion) {
            currentVersion = applyMigration(component, currentVersion);
        }
    }

    /**
     * Apply a single migration step and return the new version.
     */
    private static int applyMigration(@Nonnull HyforgedStatComponent component, int fromVersion) {
        return switch (fromVersion) {
            case 0 -> migrateV0ToV1(component);
            case 1 -> migrateV1ToV2(component);
            default -> {
                // Unknown version, try to continue
                LOGGER.warning(() -> String.format(
                        "Unknown schema version %d, attempting to continue",
                        fromVersion
                ));
                yield fromVersion + 1;
            }
        };
    }

    /**
     * Migrate from version 0 (or missing version) to version 1.
     * <p>
     * Version 0: Legacy data without version field
     * Version 1: First versioned schema with ability scores array
     */
    private static int migrateV0ToV1(@Nonnull HyforgedStatComponent component) {
        // V0 -> V1: Just ensure data exists, nothing special to migrate
        LOGGER.fine("V0->V1 migration complete (minimal changes)");
        return 1;
    }

    /**
     * Migrate from version 1 to version 2.
     * <p>
     * Version 1: Used fixed-size ability scores array
     * Version 2: Uses baseValues map (Int2IntOpenHashMap) with stat indices
     * <p>
     * This migration is a no-op for new projects since no V1 data exists.
     */
    private static int migrateV1ToV2(@Nonnull HyforgedStatComponent component) {
        // V1 -> V2: Old ability scores array replaced by baseValues map
        // For new project, there's no legacy data to migrate.
        // If baseValues is empty, set defaults for ability scores.
        
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int[] abilityStats = {
            registry.getIndex(CoreStats.STRENGTH),
            registry.getIndex(CoreStats.DEXTERITY),
            registry.getIndex(CoreStats.INTELLIGENCE),
            registry.getIndex(CoreStats.CONSTITUTION),
            registry.getIndex(CoreStats.WISDOM),
            registry.getIndex(CoreStats.SPIRIT),
            registry.getIndex(CoreStats.LUCK)
        };
        
        for (int statIndex : abilityStats) {
            if (statIndex >= 0 && component.getBaseValue(statIndex) == 0) {
                component.setBaseValue(statIndex, DEFAULT_ABILITY_BASE);
            }
        }
        
        LOGGER.fine("V1->V2 migration complete (baseValues map now used)");
        return 2;
    }

    /**
     * Validate component data after load.
     * <p>
     * Ensures all required data is present and within valid ranges.
     *
     * @param component The loaded component to validate
     * @return true if valid, false if data was corrected
     */
    public static boolean validateAndRepair(@Nonnull HyforgedStatComponent component) {
        boolean wasRepaired = false;
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();

        // Validate ability score base values
        int[] abilityStats = {
            registry.getIndex(CoreStats.STRENGTH),
            registry.getIndex(CoreStats.DEXTERITY),
            registry.getIndex(CoreStats.INTELLIGENCE),
            registry.getIndex(CoreStats.CONSTITUTION),
            registry.getIndex(CoreStats.WISDOM),
            registry.getIndex(CoreStats.SPIRIT),
            registry.getIndex(CoreStats.LUCK)
        };
        
        for (int statIndex : abilityStats) {
            if (statIndex < 0) continue;
            
            int value = component.getBaseValue(statIndex);
            if (value < 1) {
                component.setBaseValue(statIndex, 1); // Minimum
                wasRepaired = true;
            } else if (value > 999) {
                component.setBaseValue(statIndex, 999); // Maximum
                wasRepaired = true;
            }
        }
        
        if (wasRepaired) {
            LOGGER.warning("Repaired invalid ability score base values");
        }

        // Mark all stats dirty to ensure recomputation after load
        component.markAllDirty();

        return !wasRepaired;
    }
}
