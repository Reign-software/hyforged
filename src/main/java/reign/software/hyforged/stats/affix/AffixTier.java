package reign.software.hyforged.stats.affix;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a tier level for an affix.
 * <p>
 * Each tier has a range of values, rarity weights for different item rarities,
 * and an item level requirement.
 * <p>
 * This is pure data - no behavior, following ECS principles.
 * 
 * @param tier The tier level (1 = lowest, higher = better)
 * @param minValue Minimum value for this tier (inclusive)
 * @param maxValue Maximum value for this tier (inclusive)
 * @param rarityWeights Map of item rarity → selection weight (higher = more likely)
 * @param itemLevelReq Minimum item level required to roll this tier
 */
public record AffixTier(
    int tier,
    int minValue,
    int maxValue,
    @Nonnull Map<String, Integer> rarityWeights,
    int itemLevelReq
) {
    
    public AffixTier {
        Objects.requireNonNull(rarityWeights, "rarityWeights cannot be null");
        
        if (tier < 1) {
            throw new IllegalArgumentException("tier must be >= 1");
        }
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue cannot be greater than maxValue");
        }
        if (itemLevelReq < 0) {
            throw new IllegalArgumentException("itemLevelReq cannot be negative");
        }
        
        // Make defensive copy
        rarityWeights = Map.copyOf(rarityWeights);
    }
    
    /**
     * Get the weight for a specific item rarity.
     * 
     * @param rarity The item rarity (e.g., "common", "rare", "legendary")
     * @return The weight, or 0 if this tier is not available for the rarity
     */
    public int getWeight(@Nonnull String rarity) {
        return rarityWeights.getOrDefault(rarity, 0);
    }
    
    /**
     * Check if this tier can be rolled at the given item level.
     */
    public boolean canRollAt(int itemLevel) {
        return itemLevel >= itemLevelReq;
    }
    
    /**
     * Roll a random value within this tier's range.
     * <p>
     * Note: This is a pure calculation, the caller provides the random value.
     * 
     * @param randomFraction A random value 0.0-1.0
     * @return A value between minValue and maxValue
     */
    public int rollValue(double randomFraction) {
        int range = maxValue - minValue;
        return minValue + (int) (randomFraction * (range + 1));
    }
    
    /**
     * Builder for creating AffixTier instances.
     */
    public static class Builder {
        private int tier = 1;
        private int minValue = 0;
        private int maxValue = 0;
        private java.util.HashMap<String, Integer> rarityWeights = new java.util.HashMap<>();
        private int itemLevelReq = 0;
        
        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }
        
        public Builder valueRange(int min, int max) {
            this.minValue = min;
            this.maxValue = max;
            return this;
        }
        
        public Builder weight(@Nonnull String rarity, int weight) {
            this.rarityWeights.put(rarity, weight);
            return this;
        }
        
        public Builder itemLevelReq(int level) {
            this.itemLevelReq = level;
            return this;
        }
        
        public AffixTier build() {
            return new AffixTier(tier, minValue, maxValue, rarityWeights, itemLevelReq);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
