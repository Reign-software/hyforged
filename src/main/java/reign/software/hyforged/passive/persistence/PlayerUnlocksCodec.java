package reign.software.hyforged.passive.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import reign.software.hyforged.passive.component.PlayerUnlocksComponent;

import java.util.*;

/**
 * Codec for serializing and deserializing PlayerUnlocksComponent.
 * <p>
 * Uses Hytale's BuilderCodec pattern for persistence.
 * <p>
 * Persistence Strategy:
 * - Unlock flag IDs and their source node IDs
 * - Stored as parallel arrays: flag IDs, source counts, flattened source node IDs
 * - Schema version for future migration support
 * <p>
 * Schema (v1):
 * <pre>
 * {
 *   "Version": int,              // Schema version for migration
 *   "FlagIds": string[],         // Unlock flag IDs
 *   "FlagSourceCounts": int[],   // Number of sources per flag (parallel)
 *   "FlagSourcesFlat": string[]  // All source node IDs flattened
 * }
 * </pre>
 */
public final class PlayerUnlocksCodec {

    private PlayerUnlocksCodec() {
        // Utility class
    }

    /**
     * The component ID used for registration.
     */
    public static final String COMPONENT_ID = "Hyforged_PlayerUnlocks";

    private static final ArrayCodec<String> STRING_ARRAY_CODEC = new ArrayCodec<>(Codec.STRING, String[]::new);

    /**
     * The codec for PlayerUnlocksComponent.
     */
    public static final BuilderCodec<PlayerUnlocksComponent> CODEC = BuilderCodec
            .builder(PlayerUnlocksComponent.class, PlayerUnlocksComponent::new)
            .versioned()
            .codecVersion(PlayerUnlocksComponent.SCHEMA_VERSION)
            // Flag IDs
            .append(
                    new KeyedCodec<>("FlagIds", STRING_ARRAY_CODEC),
                    (component, flagIds) -> {
                        if (flagIds != null) {
                            component.setTempLoadFlagIds(flagIds);
                        }
                    },
                    component -> component.getUnlockFlags().toArray(new String[0])
            )
            .add()
            // Source counts (parallel to FlagIds)
            .append(
                    new KeyedCodec<>("FlagSourceCounts", Codec.INT_ARRAY),
                    (component, counts) -> {
                        if (counts != null) {
                            component.setTempLoadFlagSourceCounts(counts);
                        }
                    },
                    component -> {
                        Set<String> flags = component.getUnlockFlags();
                        int[] counts = new int[flags.size()];
                        int i = 0;
                        for (String flagId : flags) {
                            counts[i++] = component.getFlagSources(flagId).size();
                        }
                        return counts;
                    }
            )
            .add()
            // Flattened source node IDs
            .append(
                    new KeyedCodec<>("FlagSourcesFlat", STRING_ARRAY_CODEC),
                    (component, flatSources) -> {
                        String[] flagIds = component.getTempLoadFlagIds();
                        int[] counts = component.getTempLoadFlagSourceCounts();
                        if (flagIds != null && counts != null && flatSources != null) {
                            int offset = 0;
                            for (int i = 0; i < flagIds.length && i < counts.length; i++) {
                                String flagId = flagIds[i];
                                int count = counts[i];
                                for (int j = 0; j < count && offset < flatSources.length; j++) {
                                    component.enableFlag(flagId, flatSources[offset++]);
                                }
                            }
                        }
                        component.clearTempLoadData();
                    },
                    component -> {
                        List<String> allSources = new ArrayList<>();
                        for (String flagId : component.getUnlockFlags()) {
                            allSources.addAll(component.getFlagSources(flagId));
                        }
                        return allSources.toArray(new String[0]);
                    }
            )
            .add()
            .afterDecode((component, extraInfo) -> {
                // Mark clean after load
                component.clearDirty();
            })
            .build();
}
