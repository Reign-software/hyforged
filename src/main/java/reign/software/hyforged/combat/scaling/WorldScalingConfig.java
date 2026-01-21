package reign.software.hyforged.combat.scaling;

import javax.annotation.Nonnull;

/**
 * Configuration for distance-based monster level calculation.
 * <p>
 * Determines how monster levels are calculated based on distance from world spawn.
 * Loaded from JSON assets in {@code Server/Hyforged/Combat/WorldScaling/}.
 * <p>
 * Note: This only handles level calculation. Actual stat scaling per monster type
 * is defined in {@link MonsterScalingConfigAsset} which maps to specific NPCs.
 */
public record WorldScalingConfig(
    @Nonnull String id,
    @Nonnull ScalingCurve curve,
    int blocksPerLevel,
    int minLevel,
    int maxLevel
) {
    
    /**
     * Available scaling curve types.
     */
    public enum ScalingCurve {
        /** Level = distance / blocksPerLevel (clamped) */
        LINEAR,
        /** Level = log(distance / blocksPerLevel + 1) * factor (clamped) */
        LOGARITHMIC,
        /** Level = floor(distance / blocksPerLevel) with discrete steps */
        STEPPED
    }
    
    /** Default blocks per level (500 blocks = 1 level) */
    public static final int DEFAULT_BLOCKS_PER_LEVEL = 500;
    
    /** Default minimum level */
    public static final int DEFAULT_MIN_LEVEL = 1;
    
    /** Default maximum level */
    public static final int DEFAULT_MAX_LEVEL = 100;
    
    public WorldScalingConfig {
        if (blocksPerLevel <= 0) {
            throw new IllegalArgumentException("blocksPerLevel must be positive");
        }
        if (minLevel < 1) {
            throw new IllegalArgumentException("minLevel must be at least 1");
        }
        if (maxLevel < minLevel) {
            throw new IllegalArgumentException("maxLevel must be >= minLevel");
        }
    }
    
    /**
     * Create a default world scaling configuration.
     * 
     * @param id The configuration ID
     * @return Default configuration
     */
    public static WorldScalingConfig createDefault(@Nonnull String id) {
        return new WorldScalingConfig(
            id,
            ScalingCurve.LINEAR,
            DEFAULT_BLOCKS_PER_LEVEL,
            DEFAULT_MIN_LEVEL,
            DEFAULT_MAX_LEVEL
        );
    }
    
    /**
     * Calculate monster level from distance.
     * 
     * @param distanceFromSpawn Distance in blocks from world spawn
     * @return Monster level, clamped to [minLevel, maxLevel]
     */
    public int calculateLevel(double distanceFromSpawn) {
        if (distanceFromSpawn <= 0) {
            return minLevel;
        }
        
        int rawLevel = switch (curve) {
            case LINEAR -> (int) (distanceFromSpawn / blocksPerLevel) + 1;
            case LOGARITHMIC -> (int) (Math.log(distanceFromSpawn / blocksPerLevel + 1) * 10) + 1;
            case STEPPED -> (int) (Math.floor(distanceFromSpawn / blocksPerLevel)) + 1;
        };
        
        return Math.max(minLevel, Math.min(maxLevel, rawLevel));
    }
}
