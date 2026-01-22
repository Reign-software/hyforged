package reign.software.hyforged.affix.model;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime representation of an affix that has been rolled on an item.
 * <p>
 * This is the serialized form of an affix instance, containing the specific
 * tier and rolled values for each stat. It is stored in the item's metadata and
 * used to reconstruct the affix's stat modifiers at runtime.
 * <p>
 * Supports multi-stat affixes where each stat has its own rolled value.
 * <p>
 * Schema version is tracked at the container level ({@link HyforgedItemData}),
 * not per-affix.
 *
 * @param affixId       The affix definition ID (references AffixDefinitionRegistry)
 * @param type          The affix type ID (prefix, suffix, forged)
 * @param tier          The tier that was rolled (1 = best, higher = worse)
 * @param rolledStats   Map of stat ID to RolledStat (value + stack type)
 */
public record RolledAffix(
        @Nonnull String affixId,
        @Nonnull String type,
        int tier,
        @Nonnull Map<String, RolledStat> rolledStats
) {
    
    /**
     * A single rolled stat value within the affix.
     */
    public record RolledStat(
            int value,
            @Nonnull HyforgedModifier.StackType stackType
    ) {
        public RolledStat {
            Objects.requireNonNull(stackType, "stackType cannot be null");
        }
    }
    
    /**
     * Mutable data for RolledStat codec.
     */
    public static final class RolledStatData {
        public int value = 0;
        public HyforgedModifier.StackType stackType = HyforgedModifier.StackType.FLAT;
        
        public RolledStatData() {}
        
        public RolledStatData(RolledStat stat) {
            this.value = stat.value();
            this.stackType = stat.stackType();
        }
        
        public RolledStat toRolledStat() {
            return new RolledStat(value, stackType);
        }
    }
    
    /**
     * Codec for RolledStatData.
     */
    public static final BuilderCodec<RolledStatData> STAT_CODEC = BuilderCodec.builder(
            RolledStatData.class,
            RolledStatData::new
    )
    .append(
            new KeyedCodec<>("Value", Codec.INTEGER),
            (data, value) -> data.value = value != null ? value : 0,
            data -> data.value
    )
    .add()
    .append(
            new KeyedCodec<>("StackType", new EnumCodec<>(HyforgedModifier.StackType.class)),
            (data, value) -> data.stackType = value != null ? value : HyforgedModifier.StackType.FLAT,
            data -> data.stackType
    )
    .add()
    .build();
    
    /**
     * Map codec for stat ID -> RolledStatData
     */
    private static final MapCodec<RolledStatData, Map<String, RolledStatData>> STATS_MAP_CODEC = 
            new MapCodec<>(STAT_CODEC, HashMap::new);
    
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
            new KeyedCodec<>("Stats", STATS_MAP_CODEC),
            (data, value) -> data.stats = value != null ? new HashMap<>(value) : new HashMap<>(),
            data -> data.stats
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
        public Map<String, RolledStatData> stats = new HashMap<>();
        
        public RolledAffixData() {}
        
        public RolledAffixData(RolledAffix affix) {
            this.affixId = affix.affixId();
            this.type = affix.type();
            this.tier = affix.tier();
            for (Map.Entry<String, RolledStat> entry : affix.rolledStats().entrySet()) {
                this.stats.put(entry.getKey(), new RolledStatData(entry.getValue()));
            }
        }
        
        public RolledAffix toRolledAffix() {
            Map<String, RolledStat> converted = new HashMap<>();
            for (Map.Entry<String, RolledStatData> entry : stats.entrySet()) {
                converted.put(entry.getKey(), entry.getValue().toRolledStat());
            }
            return new RolledAffix(affixId, type, tier, converted);
        }
    }
    
    /**
     * Canonical constructor with validation.
     */
    public RolledAffix {
        Objects.requireNonNull(affixId, "affixId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(rolledStats, "rolledStats cannot be null");
        
        if (affixId.isBlank()) {
            throw new IllegalArgumentException("affixId cannot be blank");
        }
        if (type.isBlank()) {
            throw new IllegalArgumentException("type cannot be blank");
        }
        if (tier < 1) {
            throw new IllegalArgumentException("tier must be >= 1, got: " + tier);
        }
        if (rolledStats.isEmpty()) {
            throw new IllegalArgumentException("rolledStats cannot be empty");
        }
        // Make defensive copy
        rolledStats = Collections.unmodifiableMap(new HashMap<>(rolledStats));
    }
    
    /**
     * Create a RolledAffix from an AffixDefinition with a specific tier and rolled values.
     *
     * @param definition  The affix definition
     * @param tier        The tier number rolled
     * @param rolledStats Map of stat ID to rolled value (each stat rolled independently)
     * @return A new RolledAffix instance
     */
    public static RolledAffix from(
            @Nonnull AffixDefinition definition,
            int tier,
            @Nonnull Map<String, RolledStat> rolledStats
    ) {
        return new RolledAffix(
                definition.id(),
                definition.type(),
                tier,
                rolledStats
        );
    }
    
    /**
     * Create HyforgedModifiers from this rolled affix.
     *
     * @param sourceId Identifier for the source of these modifiers (usually item ID or affix ID)
     * @return List of HyforgedModifiers for each stat in this affix
     */
    @Nonnull
    public List<HyforgedModifier> toModifiers(@Nonnull String sourceId) {
        List<HyforgedModifier> modifiers = new ArrayList<>();
        for (Map.Entry<String, RolledStat> entry : rolledStats.entrySet()) {
            RolledStat stat = entry.getValue();
            modifiers.add(new HyforgedModifier(
                    HyforgedModifier.ModifierTarget.MAX,
                    stat.stackType(),
                    stat.value(),
                    HyforgedModifier.SourceType.EQUIPMENT,
                    sourceId,
                    0 // No priority for affix modifiers
            ));
        }
        return modifiers;
    }
    
    /**
     * Get a display-friendly description of this affix.
     * Returns multi-line for multi-stat affixes.
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, RolledStat> entry : rolledStats.entrySet()) {
            if (!first) sb.append("\n");
            first = false;
            
            String statId = entry.getKey();
            RolledStat stat = entry.getValue();
            int value = stat.value();
            String sign = value >= 0 ? "+" : "";
            String suffix = stat.stackType() == HyforgedModifier.StackType.INCREASED 
                    || stat.stackType() == HyforgedModifier.StackType.MORE ? "%" : "";
            
            // Extract stat name from full ID
            String statName = statId.contains(":") ? statId.substring(statId.indexOf(':') + 1) : statId;
            sb.append(String.format("%s%d%s %s", sign, value, suffix, statName));
        }
        return sb.toString();
    }
    
    /**
     * Get the number of stats in this affix.
     */
    public int getStatCount() {
        return rolledStats.size();
    }
    
    /**
     * Check if this affix grants a specific stat.
     */
    public boolean grantsStat(@Nonnull String statId) {
        return rolledStats.containsKey(statId);
    }
    
    /**
     * Get the rolled value for a specific stat.
     *
     * @param statId The stat ID
     * @return The rolled stat, or null if not present
     */
    public RolledStat getStat(@Nonnull String statId) {
        return rolledStats.get(statId);
    }
    
    /**
     * Convert to mutable data for serialization.
     */
    public RolledAffixData toData() {
        return new RolledAffixData(this);
    }
}
