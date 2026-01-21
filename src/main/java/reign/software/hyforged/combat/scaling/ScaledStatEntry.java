package reign.software.hyforged.combat.scaling;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.ModifierType;

import javax.annotation.Nonnull;

/**
 * Configuration for a single stat that scales with monster level.
 * <p>
 * Each entry specifies:
 * <ul>
 *   <li>Which stat to modify (by StatId string)</li>
 *   <li>How the modifier stacks (FLAT, INCREASED, MORE)</li>
 *   <li>The scaling value per level</li>
 * </ul>
 * <p>
 * For INCREASED/MORE types, the value is in percent (10 = 10% per level).
 * For FLAT type, the value is in basis points directly added per level.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "StatId": "hyforged:max-health",
 *   "ModifierType": "INCREASED",
 *   "ScalePerLevel": 10
 * }
 * </pre>
 */
public class ScaledStatEntry {
    
    /** Codec for ModifierType enum */
    private static final EnumCodec<ModifierType> MODIFIER_TYPE_CODEC = 
            new EnumCodec<>(ModifierType.class);
    
    /** Codec for loading a single ScaledStatEntry from JSON */
    public static final BuilderCodec<ScaledStatEntry> CODEC = BuilderCodec
            .builder(ScaledStatEntry.class, ScaledStatEntry::new)
            .append(
                    new KeyedCodec<>("StatId", Codec.STRING),
                    (entry, value) -> entry.statId = value != null ? value : "",
                    entry -> entry.statId
            )
            .add()
            .append(
                    new KeyedCodec<>("ModifierType", MODIFIER_TYPE_CODEC),
                    (entry, value) -> entry.modifierType = value != null ? value : ModifierType.FLAT,
                    entry -> entry.modifierType
            )
            .add()
            .append(
                    new KeyedCodec<>("ScalePerLevel", Codec.INTEGER),
                    (entry, value) -> entry.scalePerLevel = value != null ? value : 0,
                    entry -> entry.scalePerLevel
            )
            .add()
            .build();
    
    /** Array codec for loading multiple entries */
    public static final ArrayCodec<ScaledStatEntry> ARRAY_CODEC = 
            new ArrayCodec<>(CODEC, ScaledStatEntry[]::new);
    
    private String statId = "";
    private ModifierType modifierType = ModifierType.FLAT;
    private int scalePerLevel = 0;
    
    /** Default constructor required for codec */
    public ScaledStatEntry() {
    }
    
    /** Full constructor for programmatic creation */
    public ScaledStatEntry(@Nonnull String statId, @Nonnull ModifierType modifierType, int scalePerLevel) {
        this.statId = statId;
        this.modifierType = modifierType;
        this.scalePerLevel = scalePerLevel;
    }
    
    @Nonnull
    public String getStatId() {
        return statId;
    }
    
    @Nonnull
    public ModifierType getModifierType() {
        return modifierType;
    }
    
    public int getScalePerLevel() {
        return scalePerLevel;
    }
    
    /**
     * Calculate the modifier value for a given level.
     * 
     * @param level The monster level
     * @param minLevel The minimum level (levels below this get no scaling)
     * @return The modifier value in basis points
     */
    public int calculateModifierValue(int level, int minLevel) {
        if (level <= minLevel) {
            return 0;
        }
        int levelsAboveMin = level - minLevel;
        
        return switch (modifierType) {
            case FLAT, CAP -> levelsAboveMin * scalePerLevel;
            case INCREASED, MORE -> levelsAboveMin * scalePerLevel * 100; // Convert percent to bps
        };
    }
    
    /**
     * Parse a StatId from the string representation.
     */
    @Nonnull
    public StatId toStatId() {
        return StatId.parse(statId);
    }
    
    /**
     * Create a FLAT scaling entry.
     * 
     * @param statId The stat ID string
     * @param bpsPerLevel Basis points added per level
     */
    public static ScaledStatEntry flat(@Nonnull String statId, int bpsPerLevel) {
        return new ScaledStatEntry(statId, ModifierType.FLAT, bpsPerLevel);
    }
    
    /**
     * Create an INCREASED scaling entry (percent increase per level).
     * 
     * @param statId The stat ID string
     * @param percentPerLevel Percent increase per level (10 = 10%/level)
     */
    public static ScaledStatEntry increased(@Nonnull String statId, int percentPerLevel) {
        return new ScaledStatEntry(statId, ModifierType.INCREASED, percentPerLevel);
    }
    
    /**
     * Create a MORE scaling entry (multiplicative percent per level).
     * 
     * @param statId The stat ID string
     * @param percentPerLevel Percent multiplier per level
     */
    public static ScaledStatEntry more(@Nonnull String statId, int percentPerLevel) {
        return new ScaledStatEntry(statId, ModifierType.MORE, percentPerLevel);
    }
}
