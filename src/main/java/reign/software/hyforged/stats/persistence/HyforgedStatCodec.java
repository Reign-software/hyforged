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
 * - Base values (including ability scores) are persisted as parallel arrays
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
     * Schema (v2):
     * <pre>
     * {
     *   "Version": int,              // Schema version for migration
     *   "BaseStatIndices": int[],    // Stat indices with explicit base values
     *   "BaseStatValues": int[]      // Corresponding base values
     * }
     * </pre>
     */
    public static final BuilderCodec<HyforgedStatComponent> CODEC = BuilderCodec
            .builder(HyforgedStatComponent.class, HyforgedStatComponent::new)
            .versioned()
            .codecVersion(HyforgedStatComponent.SCHEMA_VERSION)
            .append(
                    new KeyedCodec<>("BaseStatIndices", Codec.INT_ARRAY),
                    (component, indices) -> {
                        // Stored temporarily for use with values
                        if (indices != null) {
                            component.setTempLoadIndices(indices);
                        }
                    },
                    HyforgedStatComponent::getBaseValueIndices
            )
            .add()
            .append(
                    new KeyedCodec<>("BaseStatValues", Codec.INT_ARRAY),
                    (component, values) -> {
                        // Apply with previously loaded indices
                        int[] indices = component.getTempLoadIndices();
                        if (indices != null && values != null && indices.length == values.length) {
                            for (int i = 0; i < indices.length; i++) {
                                component.setBaseValue(indices[i], values[i]);
                            }
                        }
                        component.clearTempLoadIndices();
                    },
                    HyforgedStatComponent::getBaseValueValues
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
