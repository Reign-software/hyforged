package reign.software.hyforged.progression.xp;

/**
 * Categories of XP award sources for tracking and potentially different scaling.
 * <p>
 * Used to identify the origin of XP for audit logging and UI display.
 */
public enum XPSource {
    /**
     * XP from defeating enemies in combat.
     * Scaled by enemy level/difficulty.
     */
    COMBAT("combat"),
    
    /**
     * XP from discovering new biomes.
     * Fixed amount per biome type.
     */
    DISCOVERY("discovery"),
    
    /**
     * XP from completing objectives/quests.
     * Amount defined by objective tier.
     */
    OBJECTIVE("objective"),
    
    /**
     * XP from admin commands or debugging.
     * Not scaled, bypasses validation.
     */
    ADMIN("admin");
    
    private final String id;
    
    XPSource(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    /**
     * Parse XP source from string ID, case-insensitive.
     * 
     * @param id the source ID
     * @return the matching XPSource or null if not found
     */
    public static XPSource fromId(String id) {
        if (id == null) return null;
        for (XPSource source : values()) {
            if (source.id.equalsIgnoreCase(id)) {
                return source;
            }
        }
        return null;
    }
}
