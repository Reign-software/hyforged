package reign.software.hyforged.stats;

/**
 * Core tag ID constants for Hyforged.
 * <p>
 * Tag definitions are data-driven and loaded from JSON assets
 * in Server/Hyforged/Tags/ at runtime.
 * <p>
 * This class provides compile-time constants for referencing tags in code.
 */
public final class CoreTags {
    
    private CoreTags() {} // Static utility class
    
    // ========== STAT GROUP TAGS ==========
    
    /** All ability scores (STR, DEX, INT, CON, WIS, SPI, LCK) */
    public static final String ATTRIBUTES = "attributes";
    
    /** Armor and evasion ratings */
    public static final String DEFENCES = "defences";
    
    /** All elemental resistance ratings */
    public static final String ELEMENTAL_RESISTANCES = "elemental-resistances";
    
    // ========== RECOVERY TAGS ==========
    
    /** Life leech effects */
    public static final String LIFE_LEECH = "life-leech";
    
    /** Mana leech effects */
    public static final String MANA_LEECH = "mana-leech";
    
    // ========== COMBAT CLASSIFICATION TAGS ==========
    
    /** Spell-based abilities and damage */
    public static final String SPELL = "spell";
    
    /** Attack-based abilities and damage */
    public static final String ATTACK = "attack";
    
    /** Melee attacks */
    public static final String MELEE = "melee";
    
    /** Ranged attacks */
    public static final String RANGED = "ranged";
    
    /** Projectile-based effects */
    public static final String PROJECTILE = "projectile";
    
    // ========== ELEMENT TAGS ==========
    
    /** Physical damage type */
    public static final String PHYSICAL = "physical";
    
    /** Fire damage type */
    public static final String FIRE = "fire";
    
    /** Cold damage type */
    public static final String COLD = "cold";
    
    /** Lightning damage type */
    public static final String LIGHTNING = "lightning";
    
    /** Poison damage type */
    public static final String POISON = "poison";
}
