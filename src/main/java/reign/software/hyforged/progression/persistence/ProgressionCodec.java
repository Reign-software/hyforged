package reign.software.hyforged.progression.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import reign.software.hyforged.progression.component.ProgressionComponent;

/**
 * Codec for serializing and deserializing ProgressionComponent.
 * <p>
 * Uses Hytale's BuilderCodec pattern for persistence.
 * <p>
 * Persistence Strategy:
 * - Character level and XP are persisted
 * - Class progressions (levels and XP per class) are persisted
 * - Passive point allocations are persisted
 * - Active class ID is NOT persisted (derived from weapon on load)
 * - Dirty flags are NOT persisted (always mark dirty on load)
 */
public final class ProgressionCodec {

    private ProgressionCodec() {
        // Utility class
    }

    /**
     * The component ID used for registration.
     */
    public static final String COMPONENT_ID = "Hyforged_Progression";

    /**
     * The codec for ProgressionComponent.
     * <p>
     * Schema (v1):
     * <pre>
     * {
     *   "Version": int,                   // Schema version for migration
     *   "CharacterLevel": int,            // Current character level
     *   "CharacterXP": long,              // Current character XP
     *   "ClassIds": String[],             // Class IDs with progression
     *   "ClassLevels": int[],             // Corresponding class levels
     *   "ClassXPs": long[],               // Corresponding class XPs
     *   "GeneralPointsAllocated": int     // General passive points allocated
     * }
     * </pre>
     */
    public static final BuilderCodec<ProgressionComponent> CODEC = BuilderCodec
            .builder(ProgressionComponent.class, ProgressionComponent::new)
            .versioned()
            .codecVersion(ProgressionComponent.SCHEMA_VERSION)
            // Character progression
            .append(
                    new KeyedCodec<>("CharacterLevel", Codec.INTEGER),
                    (component, level) -> {
                        if (level != null) {
                            component.setCharacterLevel(level);
                        }
                    },
                    ProgressionComponent::getCharacterLevel
            )
            .add()
            .append(
                    new KeyedCodec<>("CharacterXP", Codec.LONG),
                    (component, xp) -> {
                        if (xp != null) {
                            component.setCharacterXp(xp);
                        }
                    },
                    ProgressionComponent::getCharacterXp
            )
            .add()
            // Class progressions - stored as parallel arrays
            .append(
                    new KeyedCodec<>("ClassIds", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (component, ids) -> {
                        if (ids != null) {
                            component.setTempClassIds(ids);
                        }
                    },
                    ProgressionComponent::getClassProgressionIds
            )
            .add()
            .append(
                    new KeyedCodec<>("ClassLevels", Codec.INT_ARRAY),
                    (component, levels) -> {
                        if (levels != null) {
                            component.setTempClassLevels(levels);
                        }
                    },
                    ProgressionComponent::getClassProgressionLevels
            )
            .add()
            .append(
                    new KeyedCodec<>("ClassXPs", TolerantLongArrayCodec.INSTANCE),
                    (component, xps) -> {
                        if (xps != null) {
                            component.setTempClassXps(xps);
                        }
                    },
                    ProgressionComponent::getClassProgressionXps
            )
            .add()
            // Passive point allocation
            .append(
                    new KeyedCodec<>("GeneralPointsAllocated", Codec.INTEGER),
                    (component, points) -> {
                        if (points != null) {
                            component.setGeneralPassivePointsAllocated(points);
                        }
                    },
                    ProgressionComponent::getGeneralPassivePointsAllocated
            )
            .add()
            .afterDecode((component, extraInfo) -> {
                // Apply temporarily stored class data
                component.applyTempClassData();
                
                // Mark as dirty to ensure systems process on load
                component.markDirty();
            })
            .build();
}
