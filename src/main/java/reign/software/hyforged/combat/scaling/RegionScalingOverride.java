package reign.software.hyforged.combat.scaling;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Region-based override for monster scaling.
 * <p>
 * Allows dungeons, events, and special areas to override the default distance-based
 * monster level calculation. Regions are identified by tags or explicit region IDs.
 * <p>
 * Overrides can:
 * <ul>
 *   <li>Set a fixed level for all monsters in the region</li>
 *   <li>Apply a level modifier (+/- levels from calculated level)</li>
 *   <li>Use a different scaling config for the region</li>
 *   <li>Override with a minimum level floor</li>
 * </ul>
 */
public record RegionScalingOverride(
        /** Unique ID for this override */
        @Nonnull String id,
        
        /** Region tag pattern (e.g., "dungeon/*", "event/world_boss") */
        @Nonnull String regionPattern,
        
        /** Override type: FIXED, MODIFIER, CONFIG, or FLOOR */
        @Nonnull OverrideType type,
        
        /** Value for the override (level, modifier, or floor value) */
        int value,
        
        /** Alternative scaling config ID (for CONFIG type) */
        @Nullable String scalingConfigId,
        
        /** Priority (higher = checked first) */
        int priority
) {
    
    /**
     * Types of region scaling overrides.
     */
    public enum OverrideType {
        /** All monsters in region are set to a fixed level */
        FIXED,
        /** Add/subtract levels from calculated level */
        MODIFIER,
        /** Use a different WorldScalingConfig */
        CONFIG,
        /** Set a minimum level floor (calculated level if higher) */
        FLOOR
    }
    
    public RegionScalingOverride {
        if (type == OverrideType.CONFIG && scalingConfigId == null) {
            throw new IllegalArgumentException("CONFIG override type requires scalingConfigId");
        }
    }
    
    /**
     * Create a fixed level override (all monsters in region are this level).
     *
     * @param id Override ID
     * @param regionPattern Region tag pattern
     * @param level Fixed level to apply
     * @return The override
     */
    public static RegionScalingOverride fixed(@Nonnull String id, @Nonnull String regionPattern, int level) {
        return new RegionScalingOverride(id, regionPattern, OverrideType.FIXED, level, null, 0);
    }
    
    /**
     * Create a fixed level override with priority.
     *
     * @param id Override ID
     * @param regionPattern Region tag pattern
     * @param level Fixed level to apply
     * @param priority Priority (higher = checked first)
     * @return The override
     */
    public static RegionScalingOverride fixed(@Nonnull String id, @Nonnull String regionPattern, int level, int priority) {
        return new RegionScalingOverride(id, regionPattern, OverrideType.FIXED, level, null, priority);
    }
    
    /**
     * Create a level modifier override (add/subtract from calculated level).
     *
     * @param id Override ID
     * @param regionPattern Region tag pattern
     * @param modifier Level modifier (+5, -3, etc.)
     * @return The override
     */
    public static RegionScalingOverride modifier(@Nonnull String id, @Nonnull String regionPattern, int modifier) {
        return new RegionScalingOverride(id, regionPattern, OverrideType.MODIFIER, modifier, null, 0);
    }
    
    /**
     * Create a floor level override (minimum level for region).
     *
     * @param id Override ID
     * @param regionPattern Region tag pattern
     * @param floor Minimum level
     * @return The override
     */
    public static RegionScalingOverride floor(@Nonnull String id, @Nonnull String regionPattern, int floor) {
        return new RegionScalingOverride(id, regionPattern, OverrideType.FLOOR, floor, null, 0);
    }
    
    /**
     * Check if this override matches a region tag.
     *
     * @param regionTag The region tag to check (e.g., "dungeon/crypt")
     * @return true if this override applies to the region
     */
    public boolean matches(@Nonnull String regionTag) {
        // Exact match
        if (regionPattern.equals(regionTag)) {
            return true;
        }
        
        // Wildcard match (pattern "dungeon/*" matches "dungeon/crypt")
        if (regionPattern.endsWith("/*")) {
            String prefix = regionPattern.substring(0, regionPattern.length() - 1);
            return regionTag.startsWith(prefix);
        }
        
        // Wildcard match (pattern "*/boss" matches "event/boss")
        if (regionPattern.startsWith("*/")) {
            String suffix = regionPattern.substring(1);
            return regionTag.endsWith(suffix);
        }
        
        return false;
    }
    
    /**
     * Apply this override to a calculated level.
     *
     * @param calculatedLevel The level calculated by distance-based scaling
     * @param minLevel Minimum allowed level
     * @param maxLevel Maximum allowed level
     * @return The overridden level
     */
    public int apply(int calculatedLevel, int minLevel, int maxLevel) {
        int result = switch (type) {
            case FIXED -> value;
            case MODIFIER -> calculatedLevel + value;
            case FLOOR -> Math.max(calculatedLevel, value);
            case CONFIG -> calculatedLevel; // Config type handled externally
        };
        
        // Clamp to valid range
        return Math.max(minLevel, Math.min(maxLevel, result));
    }
}
