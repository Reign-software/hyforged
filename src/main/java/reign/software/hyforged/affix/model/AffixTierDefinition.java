package reign.software.hyforged.affix.model;

import javax.annotation.Nonnull;

/**
 * Defines a single tier within an affix definition.
 * <p>
 * Affix tiers follow ARPG convention: Tier 1 is the strongest roll,
 * higher tier numbers represent weaker rolls.
 * <p>
 * This is pure immutable data following ECS principles.
 *
 * @param tier The tier number (1 = best, higher = weaker)
 * @param minValue Minimum rolled value for this tier (inclusive)
 * @param maxValue Maximum rolled value for this tier (inclusive)
 * @param itemLevelReq Minimum item level required to roll this tier
 * @param weight Selection weight for this tier during rolling (higher = more likely)
 */
public record AffixTierDefinition(
    int tier,
    int minValue,
    int maxValue,
    int itemLevelReq,
    int weight
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
        if (minValue > maxValue) {
            throw new IllegalArgumentException(
                "minValue (" + minValue + ") cannot be greater than maxValue (" + maxValue + ")");
        }
        if (itemLevelReq < 0) {
            throw new IllegalArgumentException("itemLevelReq cannot be negative: " + itemLevelReq);
        }
        if (weight < 0) {
            throw new IllegalArgumentException("weight cannot be negative: " + weight);
        }
    }
    
    /**
     * Create a tier definition with default weight.
     */
    public AffixTierDefinition(int tier, int minValue, int maxValue, int itemLevelReq) {
        this(tier, minValue, maxValue, itemLevelReq, DEFAULT_WEIGHT);
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
     * Roll a random value within this tier's range.
     *
     * @param randomFraction A random value in [0.0, 1.0)
     * @return A value between minValue and maxValue (inclusive)
     */
    public int rollValue(double randomFraction) {
        if (minValue == maxValue) {
            return minValue;
        }
        int range = maxValue - minValue + 1;
        return minValue + (int) (randomFraction * range);
    }
    
    /**
     * Get the midpoint value of this tier's range.
     * Useful for display or comparison purposes.
     */
    public int getMidValue() {
        return (minValue + maxValue) / 2;
    }
    
    /**
     * Builder for creating AffixTierDefinition instances.
     */
    public static class Builder {
        private int tier = 1;
        private int minValue = 0;
        private int maxValue = 0;
        private int itemLevelReq = 0;
        private int weight = DEFAULT_WEIGHT;
        
        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }
        
        public Builder valueRange(int min, int max) {
            this.minValue = min;
            this.maxValue = max;
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
        
        @Nonnull
        public AffixTierDefinition build() {
            return new AffixTierDefinition(tier, minValue, maxValue, itemLevelReq, weight);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
