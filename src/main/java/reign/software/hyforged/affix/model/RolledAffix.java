package reign.software.hyforged.affix.model;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Runtime representation of an affix that has been rolled on an item.
 * <p>
 * This is the serialized form of an affix instance, containing the specific
 * tier and value that was rolled. It is stored in the item's metadata and
 * used to reconstruct the affix's stat modifier at runtime.
 * <p>
 * Schema version is tracked at the container level ({@link HyforgedItemData}),
 * not per-affix.
 *
 * @param affixId       The affix definition ID (references AffixDefinitionRegistry)
 * @param type          The affix type ID (prefix, suffix, forged)
 * @param tier          The tier that was rolled (1 = best, higher = worse)
 * @param value         The rolled value within the tier's min/max range
 * @param statId        The stat this affix modifies (denormalized for fast lookup)
 * @param modifierType  The modifier stack type (FLAT, INCREASED, MORE, CAP)
 */
public record RolledAffix(
        @Nonnull String affixId,
        @Nonnull String type,
        int tier,
        int value,
        @Nonnull StatId statId,
        @Nonnull HyforgedModifier.StackType modifierType
) {
    
    /**
     * Codec for serializing RolledAffix to/from BSON.
     * Uses an intermediate mutable class for the builder pattern.
     */
    public static final BuilderCodec<RolledAffixData> CODEC = BuilderCodec.builder(
            RolledAffixData.class,
            RolledAffixData::new
    )
    .append(
            new KeyedCodec<>("AffixId", Codec.STRING),
            (data, value) -> data.affixId = value != null ? value : "",
            data -> data.affixId
    )
    .add()
    .append(
            new KeyedCodec<>("Type", Codec.STRING),
            (data, value) -> data.type = value != null ? value : "",
            data -> data.type
    )
    .add()
    .append(
            new KeyedCodec<>("Tier", Codec.INTEGER),
            (data, value) -> data.tier = value != null ? value : 1,
            data -> data.tier
    )
    .add()
    .append(
            new KeyedCodec<>("Value", Codec.INTEGER),
            (data, value) -> data.value = value != null ? value : 0,
            data -> data.value
    )
    .add()
    .append(
            new KeyedCodec<>("StatId", Codec.STRING),
            (data, value) -> data.statIdStr = value != null ? value : "",
            data -> data.statIdStr
    )
    .add()
    .append(
            new KeyedCodec<>("ModifierType", new EnumCodec<>(HyforgedModifier.StackType.class)),
            (data, value) -> data.modifierType = value != null ? value : HyforgedModifier.StackType.FLAT,
            data -> data.modifierType
    )
    .add()
    .build();
    
    /**
     * Array codec for lists of rolled affixes.
     */
    public static final ArrayCodec<RolledAffixData> ARRAY_CODEC = new ArrayCodec<>(CODEC, RolledAffixData[]::new);
    
    /**
     * Mutable data class for codec serialization.
     */
    public static final class RolledAffixData {
        public String affixId = "";
        public String type = "";
        public int tier = 1;
        public int value = 0;
        public String statIdStr = "";
        public HyforgedModifier.StackType modifierType = HyforgedModifier.StackType.FLAT;
        
        public RolledAffixData() {}
        
        public RolledAffixData(RolledAffix affix) {
            this.affixId = affix.affixId();
            this.type = affix.type();
            this.tier = affix.tier();
            this.value = affix.value();
            this.statIdStr = affix.statId().toString();
            this.modifierType = affix.modifierType();
        }
        
        public RolledAffix toRolledAffix() {
            return new RolledAffix(
                    affixId,
                    type,
                    tier,
                    value,
                    StatId.parse(statIdStr),
                    modifierType
            );
        }
    }
    
    /**
     * Canonical constructor with validation.
     */
    public RolledAffix {
        Objects.requireNonNull(affixId, "affixId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(statId, "statId cannot be null");
        Objects.requireNonNull(modifierType, "modifierType cannot be null");
        
        if (affixId.isBlank()) {
            throw new IllegalArgumentException("affixId cannot be blank");
        }
        if (type.isBlank()) {
            throw new IllegalArgumentException("type cannot be blank");
        }
        if (tier < 1) {
            throw new IllegalArgumentException("tier must be >= 1, got: " + tier);
        }
    }
    
    /**
     * Create a RolledAffix from an AffixDefinition with a specific tier and value.
     *
     * @param definition The affix definition
     * @param tier       The tier number rolled
     * @param value      The value rolled within the tier
     * @return A new RolledAffix instance
     */
    public static RolledAffix from(
            @Nonnull AffixDefinition definition,
            int tier,
            int value
    ) {
        return new RolledAffix(
                definition.id(),
                definition.type(),
                tier,
                value,
                definition.statId(),
                definition.modifierType()
        );
    }
    
    /**
     * Create a HyforgedModifier from this rolled affix.
     *
     * @param sourceId Identifier for the source of this modifier (usually item ID or affix ID)
     * @return A new HyforgedModifier representing this affix's stat bonus
     */
    public HyforgedModifier toModifier(@Nonnull String sourceId) {
        return new HyforgedModifier(
                HyforgedModifier.ModifierTarget.MAX,
                modifierType,
                value,
                HyforgedModifier.SourceType.EQUIPMENT,
                sourceId,
                0 // No priority for affix modifiers
        );
    }
    
    /**
     * Get a display-friendly description of this affix.
     */
    public String toDisplayString() {
        String sign = value >= 0 ? "+" : "";
        String suffix = modifierType == HyforgedModifier.StackType.INCREASED 
                || modifierType == HyforgedModifier.StackType.MORE ? "%" : "";
        return String.format("%s%d%s %s", sign, value, suffix, statId.name());
    }
    
    /**
     * Convert to mutable data for serialization.
     */
    public RolledAffixData toData() {
        return new RolledAffixData(this);
    }
}
