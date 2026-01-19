package reign.software.hyforged.stats.persistence;

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
        switch (fromVersion) {
            case 0:
                // Version 0 -> 1: Initial schema, no migration needed
                // This handles very old data that might not have version field
                return migrateV0ToV1(component);
            
            // Future migrations go here:
            // case 1:
            //     return migrateV1ToV2(component);
            
            default:
                // Unknown version, try to continue
                LOGGER.warning(() -> String.format(
                        "Unknown schema version %d, attempting to continue",
                        fromVersion
                ));
                return fromVersion + 1;
        }
    }

    /**
     * Migrate from version 0 (or missing version) to version 1.
     * <p>
     * Version 0: Legacy data without version field
     * Version 1: First versioned schema with ability scores array
     */
    private static int migrateV0ToV1(@Nonnull HyforgedStatComponent component) {
        // Version 0 data might have:
        // - Missing ability scores (use defaults)
        // - Ability scores stored differently
        
        int[] scores = component.getAbilityScores();
        boolean needsDefaults = true;
        
        // Check if any ability scores are set
        for (int score : scores) {
            if (score != 0) {
                needsDefaults = false;
                break;
            }
        }
        
        if (needsDefaults) {
            // Set default ability scores (10 each)
            int[] defaults = {10, 10, 10, 10, 10, 10, 10};
            component.setAbilityScores(defaults);
            LOGGER.fine("Applied default ability scores during V0->V1 migration");
        }
        
        // Clear any stale modifiers that might have persisted incorrectly
        // In V0, modifiers might have been persisted when they shouldn't be
        // V1+ only persists ability score allocations
        if (component.getModifierCount() > 0) {
            component.clearModifiers();
            LOGGER.fine("Cleared legacy modifiers during V0->V1 migration");
        }
        
        return 1;
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

        // Validate ability scores
        int[] scores = component.getAbilityScores();
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] < 1) {
                scores[i] = 1; // Minimum ability score
                wasRepaired = true;
            } else if (scores[i] > 999) {
                scores[i] = 999; // Maximum ability score
                wasRepaired = true;
            }
        }
        
        if (wasRepaired) {
            component.setAbilityScores(scores);
            LOGGER.warning("Repaired invalid ability scores");
        }

        // Mark all stats dirty to ensure recomputation after load
        component.markAllDirty();

        return !wasRepaired;
    }
}
