package reign.software.hyforged.passive.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import reign.software.hyforged.passive.component.PlayerSpellsComponent;

import java.util.*;

/**
 * Codec for serializing and deserializing PlayerSpellsComponent.
 * <p>
 * Uses Hytale's BuilderCodec pattern for persistence.
 * <p>
 * Persistence Strategy:
 * - Granted spell IDs and their source node IDs
 * - Stored as parallel arrays: spell IDs, source counts, flattened source node IDs
 * - Schema version for future migration support
 * <p>
 * Schema (v1):
 * <pre>
 * {
 *   "Version": int,               // Schema version for migration
 *   "SpellIds": string[],         // Granted spell IDs
 *   "SpellSourceCounts": int[],   // Number of sources per spell (parallel)
 *   "SpellSourcesFlat": string[]  // All source node IDs flattened
 * }
 * </pre>
 */
public final class PlayerSpellsCodec {

    private PlayerSpellsCodec() {
        // Utility class
    }

    /**
     * The component ID used for registration.
     */
    public static final String COMPONENT_ID = "Hyforged_PlayerSpells";

    private static final ArrayCodec<String> STRING_ARRAY_CODEC = new ArrayCodec<>(Codec.STRING, String[]::new);

    /**
     * The codec for PlayerSpellsComponent.
     */
    public static final BuilderCodec<PlayerSpellsComponent> CODEC = BuilderCodec
            .builder(PlayerSpellsComponent.class, PlayerSpellsComponent::new)
            .versioned()
            .codecVersion(PlayerSpellsComponent.SCHEMA_VERSION)
            // Spell IDs
            .append(
                    new KeyedCodec<>("SpellIds", STRING_ARRAY_CODEC),
                    (component, spellIds) -> {
                        if (spellIds != null) {
                            component.setTempLoadSpellIds(spellIds);
                        }
                    },
                    component -> component.getGrantedSpells().toArray(new String[0])
            )
            .add()
            // Source counts (parallel to SpellIds)
            .append(
                    new KeyedCodec<>("SpellSourceCounts", Codec.INT_ARRAY),
                    (component, counts) -> {
                        if (counts != null) {
                            component.setTempLoadSpellSourceCounts(counts);
                        }
                    },
                    component -> {
                        Set<String> spells = component.getGrantedSpells();
                        int[] counts = new int[spells.size()];
                        int i = 0;
                        for (String spellId : spells) {
                            counts[i++] = component.getSpellSources(spellId).size();
                        }
                        return counts;
                    }
            )
            .add()
            // Flattened source node IDs
            .append(
                    new KeyedCodec<>("SpellSourcesFlat", STRING_ARRAY_CODEC),
                    (component, flatSources) -> {
                        String[] spellIds = component.getTempLoadSpellIds();
                        int[] counts = component.getTempLoadSpellSourceCounts();
                        if (spellIds != null && counts != null && flatSources != null) {
                            int offset = 0;
                            for (int i = 0; i < spellIds.length && i < counts.length; i++) {
                                String spellId = spellIds[i];
                                int count = counts[i];
                                for (int j = 0; j < count && offset < flatSources.length; j++) {
                                    component.grantSpell(spellId, flatSources[offset++]);
                                }
                            }
                        }
                        component.clearTempLoadData();
                    },
                    component -> {
                        List<String> allSources = new ArrayList<>();
                        for (String spellId : component.getGrantedSpells()) {
                            allSources.addAll(component.getSpellSources(spellId));
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
