package reign.software.hyforged.stats;

/**
 * Core category ID constants for Hyforged.
 * <p>
 * Category definitions are data-driven and loaded from JSON assets
 * in Server/Hyforged/Categories/ at runtime.
 * <p>
 * This class provides compile-time constants for referencing categories in code.
 */
public final class CoreCategories {
    
    private CoreCategories() {} // Static utility class
    
    /** Primary ability scores (Strength, Dexterity, etc.) */
    public static final String ABILITY_SCORE = "ability-score";
    
    /** Resource stats (Health, Mana, Stamina) */
    public static final String RESOURCE = "resource";
    
    /** Offensive stats (Attack Power, Crit Chance, etc.) */
    public static final String OFFENSE = "offense";
    
    /** Defensive stats (Armor, Resistances, etc.) */
    public static final String DEFENSE = "defense";
    
    /** Utility stats (Movement Speed, Cooldown Recovery, etc.) */
    public static final String UTILITY = "utility";
    
    /** Elemental stats (Fire/Cold/Lightning damage and resistance) */
    public static final String ELEMENTAL = "elemental";
    
    /** Recovery stats (Regen, Leech, etc.) */
    public static final String RECOVERY = "recovery";
}
