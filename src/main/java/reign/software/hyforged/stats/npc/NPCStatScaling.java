package reign.software.hyforged.stats.npc;

/**
 * Defines how a stat scales with NPC level.
 * <p>
 * The final base value is: base + (perLevel * level)
 * <p>
 * Example: For "health" with base=100, perLevel=20, at level 5:
 * - Final base = 100 + (20 * 5) = 200
 *
 * @param base     The starting value at level 0
 * @param perLevel Additional value per level
 */
public record NPCStatScaling(int base, int perLevel) {
    
    /**
     * Create scaling with no per-level increase (flat value).
     */
    public static NPCStatScaling flat(int value) {
        return new NPCStatScaling(value, 0);
    }
    
    /**
     * Calculate the resolved value at a given level.
     *
     * @param level The NPC's level (0+)
     * @return The computed base stat value
     */
    public int resolveAt(int level) {
        return base + (perLevel * Math.max(0, level));
    }
    
    /**
     * Merge with parent scaling (child values override parent).
     */
    public NPCStatScaling merge(NPCStatScaling parent) {
        // Child always overrides parent completely
        return this;
    }
}
