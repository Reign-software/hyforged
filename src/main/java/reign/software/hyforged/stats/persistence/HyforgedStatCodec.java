package reign.software.hyforged.stats.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

/**
 * Codec for serializing and deserializing HyforgedStatComponent.
 * <p>
 * Uses Hytale's BuilderCodec pattern for persistence.
 * <p>
 * Persistence Strategy:
 * - Only ability score allocations are persisted (player-owned state)
 * - Modifiers are NOT persisted (reapplied by equipment/buffs systems on load)
 * - Computed/cached values are NOT persisted (recomputed by systems on load)
 * <p>
 * This minimizes save data size and ensures derived stats stay in sync.
 */
public final class HyforgedStatCodec {

    private HyforgedStatCodec() {
        // Utility class
    }

    /**
     * The component ID used for registration.
     */
    public static final String COMPONENT_ID = "Hyforged_Stats";

    /**
     * The codec for HyforgedStatComponent.
     * <p>
     * Schema:
     * <pre>
     * {
     *   "Version": int,           // Schema version for migration
     *   "AbilityScores": int[7]   // STR, DEX, INT, CON, WIS, SPI, LCK
     * }
     * </pre>
     */
    public static final BuilderCodec<HyforgedStatComponent> CODEC = BuilderCodec
            .builder(HyforgedStatComponent.class, HyforgedStatComponent::new)
            .versioned()
            .codecVersion(HyforgedStatComponent.SCHEMA_VERSION)
            .append(
                    new KeyedCodec<>("AbilityScores", Codec.INT_ARRAY),
                    // Setter: apply loaded ability scores
                    (component, scores) -> {
                        if (scores != null && scores.length == 7) {
                            component.setAbilityScores(scores);
                        }
                    },
                    // Getter: retrieve ability scores for saving
                    HyforgedStatComponent::getAbilityScores
            )
            .add()
            .afterDecode((component, extraInfo) -> {
                // Get the persisted version from extraInfo
                // Integer.MAX_VALUE means unset (no version field in old data)
                int persistedVersion = extraInfo.getVersion();
                int currentVersion = HyforgedStatComponent.SCHEMA_VERSION;

                // Treat unset as version 0 (legacy data)
                if (persistedVersion == Integer.MAX_VALUE) {
                    persistedVersion = 0;
                }

                // Apply migrations if needed
                if (persistedVersion < currentVersion) {
                    StatDataMigrator.migrate(component, persistedVersion, currentVersion);
                }

                // Validate and repair any invalid data
                StatDataMigrator.validateAndRepair(component);

                // Ensure all stats are marked dirty for recomputation
                component.markAllDirty();
            })
            .build();
}
