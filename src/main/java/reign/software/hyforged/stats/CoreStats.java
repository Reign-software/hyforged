package reign.software.hyforged.stats;

/**
 * Core stat ID constants for Hyforged.
 * <p>
 * Stat definitions are data-driven and loaded from JSON assets
 * in Server/Hyforged/Stats/ at runtime.
 * <p>
 * This class provides compile-time constants for referencing stats in code.
 */
public final class CoreStats {
    
    private CoreStats() {} // Static utility class
    
    // ========== ABILITY SCORES (Primary Stats) ==========
    
    public static final StatId STRENGTH = StatId.hyforged("strength");
    public static final StatId DEXTERITY = StatId.hyforged("dexterity");
    public static final StatId INTELLIGENCE = StatId.hyforged("intelligence");
    public static final StatId CONSTITUTION = StatId.hyforged("constitution");
    public static final StatId WISDOM = StatId.hyforged("wisdom");
    public static final StatId SPIRIT = StatId.hyforged("spirit");
    public static final StatId LUCK = StatId.hyforged("luck");
    
    // ========== RESOURCES ==========
    
    public static final StatId MAX_HEALTH_FLAT = StatId.hyforged("max-health-flat");
    public static final StatId MAX_MANA_FLAT = StatId.hyforged("max-mana-flat");
    public static final StatId MAX_STAMINA_FLAT = StatId.hyforged("max-stamina-flat");
    public static final StatId HEALTH_REGEN_FLAT = StatId.hyforged("health-regen-flat");
    public static final StatId MANA_REGEN_FLAT = StatId.hyforged("mana-regen-flat");
    public static final StatId STAMINA_REGEN_FLAT = StatId.hyforged("stamina-regen-flat");
    
    // ========== OFFENSE (General) ==========
    
    public static final StatId ATTACK_POWER = StatId.hyforged("attack-power");
    public static final StatId SPELL_POWER = StatId.hyforged("spell-power");
    public static final StatId DAMAGE_INCREASED_BPS = StatId.hyforged("damage-increased-bps");
    public static final StatId ATTACK_DAMAGE_INCREASED_BPS = StatId.hyforged("attack-damage-increased-bps");
    public static final StatId SPELL_DAMAGE_INCREASED_BPS = StatId.hyforged("spell-damage-increased-bps");
    public static final StatId MELEE_DAMAGE_INCREASED_BPS = StatId.hyforged("melee-damage-increased-bps");
    public static final StatId RANGED_DAMAGE_INCREASED_BPS = StatId.hyforged("ranged-damage-increased-bps");
    public static final StatId ATTACK_SPEED_BPS = StatId.hyforged("attack-speed-bps");
    public static final StatId CAST_SPEED_BPS = StatId.hyforged("cast-speed-bps");
    public static final StatId ACCURACY_RATING = StatId.hyforged("accuracy-rating");
    
    // ========== CRITICAL ==========
    
    public static final StatId CRIT_CHANCE_BPS = StatId.hyforged("crit-chance-bps");
    public static final StatId CRIT_MULTIPLIER_BPS = StatId.hyforged("crit-multiplier-bps");
    
    // ========== DEFENSE (Ratings) ==========
    
    public static final StatId ARMOR_RATING = StatId.hyforged("armor-rating");
    public static final StatId EVASION_RATING = StatId.hyforged("evasion-rating");
    public static final StatId BLOCK_CHANCE_BPS = StatId.hyforged("block-chance-bps");
    public static final StatId DODGE_CHANCE_BPS = StatId.hyforged("dodge-chance-bps");
    
    // ========== ELEMENTAL RESISTANCES (Ratings) ==========
    
    public static final StatId FIRE_RESISTANCE_RATING = StatId.hyforged("fire-resistance-rating");
    public static final StatId COLD_RESISTANCE_RATING = StatId.hyforged("cold-resistance-rating");
    public static final StatId LIGHTNING_RESISTANCE_RATING = StatId.hyforged("lightning-resistance-rating");
    public static final StatId POISON_RESISTANCE_RATING = StatId.hyforged("poison-resistance-rating");
    
    // ========== ELEMENTAL DAMAGE ==========
    
    public static final StatId PHYSICAL_DAMAGE_INCREASED_BPS = StatId.hyforged("physical-damage-increased-bps");
    public static final StatId FIRE_DAMAGE_INCREASED_BPS = StatId.hyforged("fire-damage-increased-bps");
    public static final StatId COLD_DAMAGE_INCREASED_BPS = StatId.hyforged("cold-damage-increased-bps");
    public static final StatId LIGHTNING_DAMAGE_INCREASED_BPS = StatId.hyforged("lightning-damage-increased-bps");
    public static final StatId POISON_DAMAGE_INCREASED_BPS = StatId.hyforged("poison-damage-increased-bps");
    
    // ========== RECOVERY ==========
    
    public static final StatId LIFE_ON_KILL_FLAT = StatId.hyforged("life-on-kill-flat");
    public static final StatId MANA_ON_KILL_BPS = StatId.hyforged("mana-on-kill-bps");
    public static final StatId HEALTH_REGEN_PERCENT_BPS = StatId.hyforged("health-regen-percent-bps");
    public static final StatId MANA_REGEN_PERCENT_BPS = StatId.hyforged("mana-regen-percent-bps");
    
    // ========== UTILITY ==========
    
    public static final StatId COOLDOWN_RECOVERY_RATE_BPS = StatId.hyforged("cooldown-recovery-rate-bps");
    public static final StatId EFFECT_DURATION_BPS = StatId.hyforged("effect-duration-bps");
    public static final StatId AREA_OF_EFFECT_BPS = StatId.hyforged("area-of-effect-bps");
    
    // ========== LOOT ==========
    
    public static final StatId ITEM_RARITY_INCREASED_BPS = StatId.hyforged("item-rarity-increased-bps");
    public static final StatId ITEM_QUANTITY_INCREASED_BPS = StatId.hyforged("item-quantity-increased-bps");
}
