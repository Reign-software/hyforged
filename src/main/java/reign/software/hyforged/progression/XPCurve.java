package reign.software.hyforged.progression;

import javax.annotation.Nonnull;

/**
 * Domain object representing an XP curve for level progression.
 * <p>
 * XP curves define the experience required for each level using an exponential formula:
 * {@code XP(n) = BaseXP * (ExponentFactor ^ (n-1))}
 * <p>
 * This supports both character (cap 100) and class (cap 20) progression curves.
 *
 * @param id Unique identifier for this curve (e.g., "hyforged:character_xp")
 * @param type The type of curve ("character" or "class")
 * @param baseXp Base XP required for level 2
 * @param exponentFactor Exponential growth factor per level
 * @param maxLevel Maximum level for this curve
 */
public record XPCurve(
    @Nonnull String id,
    @Nonnull CurveType type,
    int baseXp,
    double exponentFactor,
    int maxLevel
) {
    /** Default base XP for character progression */
    public static final int DEFAULT_CHARACTER_BASE_XP = 100;
    
    /** Default base XP for class progression */
    public static final int DEFAULT_CLASS_BASE_XP = 50;
    
    /** Default exponent factor */
    public static final double DEFAULT_EXPONENT_FACTOR = 1.15;

    public XPCurve {
        if (id == null) {
            id = "unknown";
        }
        if (type == null) {
            type = CurveType.CHARACTER;
        }
        if (baseXp < 1) {
            baseXp = 1;
        }
        if (exponentFactor < 1.0) {
            exponentFactor = 1.0;
        }
        if (maxLevel < 1) {
            maxLevel = 1;
        }
    }

    /**
     * Calculate XP required to reach a specific level from the previous level.
     * Formula: XP(n) = BaseXP * (ExponentFactor ^ (n-2)) for n >= 2
     * Level 1 requires 0 XP (starting level).
     *
     * @param level Target level (1 to maxLevel)
     * @return XP required to reach this level from level - 1
     */
    public long getXpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        if (level > maxLevel) {
            return Long.MAX_VALUE;
        }
        // XP(n) = BaseXP * (ExponentFactor ^ (n-2))
        // For level 2, exponent is 0, so XP = BaseXP
        return Math.round(baseXp * Math.pow(exponentFactor, level - 2));
    }

    /**
     * Calculate total XP required to reach a specific level from level 1.
     *
     * @param level Target level (1 to maxLevel)
     * @return Total cumulative XP required
     */
    public long getTotalXpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        if (level > maxLevel) {
            return Long.MAX_VALUE;
        }
        long total = 0;
        for (int i = 2; i <= level; i++) {
            total += getXpForLevel(i);
        }
        return total;
    }

    /**
     * Calculate the level achieved given a total XP amount.
     *
     * @param totalXp Total accumulated XP
     * @return The level achieved (1 to maxLevel)
     */
    public int getLevelForTotalXp(long totalXp) {
        if (totalXp <= 0) {
            return 1;
        }
        int level = 1;
        long accumulated = 0;
        while (level < maxLevel) {
            long xpForNext = getXpForLevel(level + 1);
            if (accumulated + xpForNext > totalXp) {
                break;
            }
            accumulated += xpForNext;
            level++;
        }
        return level;
    }

    /**
     * Calculate remaining XP after reaching a level given total XP.
     *
     * @param totalXp Total accumulated XP
     * @return XP remaining toward next level (never negative)
     */
    public long getRemainingXp(long totalXp) {
        if (totalXp <= 0) {
            return 0;
        }
        int level = getLevelForTotalXp(totalXp);
        if (level >= maxLevel) {
            return 0;
        }
        long xpAtLevel = getTotalXpForLevel(level);
        return Math.max(0, totalXp - xpAtLevel);
    }

    /**
     * Curve type enumeration.
     */
    public enum CurveType {
        CHARACTER("character"),
        CLASS("class");

        private final String value;

        CurveType(String value) {
            this.value = value;
        }

        @Nonnull
        public String getValue() {
            return value;
        }

        @Nonnull
        public static CurveType fromString(@Nonnull String value) {
            for (CurveType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return CHARACTER;
        }
    }

    @Override
    public String toString() {
        return String.format("XPCurve{id=%s, type=%s, baseXp=%d, exponent=%.3f, maxLevel=%d}",
            id, type, baseXp, exponentFactor, maxLevel);
    }
}
