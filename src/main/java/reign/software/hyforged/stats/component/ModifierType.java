package reign.software.hyforged.stats.component;

/**
 * Type of modifier determining how it stacks with other modifiers.
 * Follows ARPG stacking order: FLAT → INCREASED → MORE → CAP
 * <p>
 * This is pure data - no behavior, following ECS principles.
 */
public enum ModifierType {
    /**
     * Flat addition/subtraction.
     * Applied first, all flat modifiers are summed.
     * Example: +50 Health, -10 Armor
     */
    FLAT(0),
    
    /**
     * Percentage increase/decrease (additive with other increased).
     * Applied second, all increased values are summed then applied.
     * Value is in basis points (1000 = 100%).
     * Example: 100 bps = +10% increased damage
     */
    INCREASED(1),
    
    /**
     * Percentage more/less (multiplicative).
     * Applied third, each more modifier is applied sequentially.
     * Value is in basis points (1000 = 100%).
     * Example: 200 bps = 20% more damage
     */
    MORE(2),
    
    /**
     * Cap/clamp value.
     * Applied last, enforces minimum or maximum bounds.
     * Positive value = max cap, negative value = min cap (by convention).
     */
    CAP(3);
    
    private final int order;
    
    ModifierType(int order) {
        this.order = order;
    }
    
    /**
     * Get the stacking order (lower = applied earlier).
     */
    public int getOrder() {
        return order;
    }
}
