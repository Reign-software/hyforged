package reign.software.hyforged.affix.model;

import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Defines a single tier within an affix definition.
 * <p>
 * Affix tiers follow ARPG convention: Tier 1 is the strongest roll,
 * higher tier numbers represent weaker rolls.
 * <p>
 * Each tier can grant multiple stats, with each stat having its own
 * value range. For example, a T1 "of the Titan" suffix might grant:
 * <ul>
 *   <li>+45-55 Strength (FLAT)</li>
 *   <li>+100-150 Max Health (FLAT)</li>
 * </ul>
 * <p>
 * This is pure immutable data following ECS principles.
 *
 * @param tier The tier number (1 = best, higher = weaker)
 * @param itemLevelReq Minimum item level required to roll this tier
 * @param weight Selection weight for this tier during rolling (higher = more likely)
 * @param stats Map of stat ID to stat configuration (value range, stack type)
 */
public record AffixTierDefinition(
    int tier,
    int itemLevelReq,
    int weight,
    @Nonnull Map<String, AffixTierStat> stats
) {
    
    /** Default weight for tier selection if not specified */
    public static final int DEFAULT_WEIGHT = 100;
    
    /**
     * Base weight for linear tier curve calculation.
     * <p>
     * When weight is not explicitly set, tiers use a linear curve where:
     * weight = LINEAR_WEIGHT_BASE * tier
     * <p>
     * This gives T1=50 (rarer), T2=100, T3=150, T4=200, T5=250 (more common).
     * Lower tier numbers represent better rolls and are less likely to occur.
     */
    public static final int LINEAR_WEIGHT_BASE = 50;
    
    public AffixTierDefinition {
        if (tier < 1) {
            throw new IllegalArgumentException("tier must be >= 1, got: " + tier);
        }
        if (itemLevelReq < 0) {
            throw new IllegalArgumentException("itemLevelReq cannot be negative: " + itemLevelReq);
        }
        if (weight < 0) {
            throw new IllegalArgumentException("weight cannot be negative: " + weight);
        }
        Objects.requireNonNull(stats, "stats cannot be null");
        if (stats.isEmpty()) {
            throw new IllegalArgumentException("stats cannot be empty - a tier must grant at least one stat");
        }
        // Make defensive copy to ensure immutability
        stats = Collections.unmodifiableMap(new HashMap<>(stats));
    }
    
    /**
     * Create a tier definition with default weight.
     */
    public AffixTierDefinition(int tier, int itemLevelReq, @Nonnull Map<String, AffixTierStat> stats) {
        this(tier, itemLevelReq, DEFAULT_WEIGHT, stats);
    }
    
    /**
     * Check if this tier can be rolled at the given item level.
     *
     * @param itemLevel The item's level
     * @return true if the item level meets the requirement
     */
    public boolean canRollAt(int itemLevel) {
        return itemLevel >= itemLevelReq;
    }
    
    /**
     * Get the number of stats this tier grants.
     */
    public int getStatCount() {
        return stats.size();
    }
    
    /**
     * Check if this tier grants a specific stat.
     *
     * @param statId The stat ID to check (e.g., "hyforged:strength")
     * @return true if this tier grants the stat
     */
    public boolean grantsStat(@Nonnull String statId) {
        return stats.containsKey(statId);
    }
    
    /**
     * Get the stat configuration for a specific stat.
     *
     * @param statId The stat ID to get
     * @return The stat configuration, or null if not present
     */
    public AffixTierStat getStat(@Nonnull String statId) {
        return stats.get(statId);
    }
    
    /**
     * Builder for creating AffixTierDefinition instances.
     */
    public static class Builder {
        private int tier = 1;
        private int itemLevelReq = 0;
        private int weight = DEFAULT_WEIGHT;
        private final Map<String, AffixTierStat> stats = new HashMap<>();
        
        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }
        
        public Builder itemLevelReq(int itemLevelReq) {
            this.itemLevelReq = itemLevelReq;
            return this;
        }
        
        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }
        
        /**
         * Add a stat to this tier with a value range.
         *
         * @param statId The stat ID (e.g., "hyforged:strength")
         * @param stackType How this modifier stacks
         * @param minValue Minimum rolled value
         * @param maxValue Maximum rolled value
         */
        public Builder stat(@Nonnull String statId, 
                           @Nonnull HyforgedModifier.StackType stackType,
                           int minValue, int maxValue) {
            this.stats.put(statId, new AffixTierStat(
                StatId.parse(statId), stackType, minValue, maxValue));
            return this;
        }
        
        /**
         * Add a stat to this tier with a fixed value.
         *
         * @param statId The stat ID (e.g., "hyforged:strength")
         * @param stackType How this modifier stacks
         * @param value The fixed value
         */
        public Builder stat(@Nonnull String statId,
                           @Nonnull HyforgedModifier.StackType stackType,
                           int value) {
            return stat(statId, stackType, value, value);
        }
        
        /**
         * Add a pre-built AffixTierStat.
         */
        public Builder stat(@Nonnull String statId, @Nonnull AffixTierStat stat) {
            this.stats.put(statId, stat);
            return this;
        }
        
        @Nonnull
        public AffixTierDefinition build() {
            return new AffixTierDefinition(tier, itemLevelReq, weight, stats);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
