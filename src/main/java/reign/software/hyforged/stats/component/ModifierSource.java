package reign.software.hyforged.stats.component;

/**
 * Source type for a stat modifier.
 * Determines the category of the modifier for UI grouping and priority.
 * <p>
 * This is pure data - no behavior, following ECS principles.
 */
public enum ModifierSource {
    /** Base stat value (e.g., from stat definition defaults) */
    BASE,
    
    /** Modifier from ability score contribution */
    ABILITY_SCORE,
    
    /** Modifier from equipped items */
    EQUIPMENT,
    
    /** Modifier from active buffs/debuffs */
    BUFF,
    
    /** Modifier from passive skills/traits */
    PASSIVE,
    
    /** Modifier from class/progression system */
    CLASS,
    
    /** Modifier from temporary effects */
    EFFECT,
    
    /** Modifier from admin/debug commands */
    ADMIN
}
