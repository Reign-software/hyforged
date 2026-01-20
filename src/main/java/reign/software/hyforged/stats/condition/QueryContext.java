package reign.software.hyforged.stats.condition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Context record containing state information for conditional modifier evaluation.
 * <p>
 * This context is passed to stat queries to determine which conditional modifiers
 * should be applied. It captures the entity's current state at query time.
 * <p>
 * Context is immutable and should be created fresh for each query or cached
 * within a tick for performance.
 *
 * @param statusEffects Set of active status effects on the entity
 * @param healthPercentBps Current health as percentage in basis points (10000 = 100%)
 * @param equippedWeaponTypes Set of weapon types currently equipped
 * @param hasShieldEquipped Whether a shield is currently equipped
 * @param isInCombat Whether the entity is currently in combat
 * @param extraFlags Additional boolean flags for extensibility
 */
public record QueryContext(
    @Nonnull Set<StatusEffect> statusEffects,
    int healthPercentBps,
    @Nonnull Set<WeaponType> equippedWeaponTypes,
    boolean hasShieldEquipped,
    boolean isInCombat,
    @Nonnull Set<String> extraFlags
) {
    
    /**
     * Default context with no status effects, full health, and no equipment.
     */
    public static final QueryContext EMPTY = new QueryContext(
        Collections.emptySet(),
        10000, // 100% health
        Collections.emptySet(),
        false,
        false,
        Collections.emptySet()
    );
    
    public QueryContext {
        Objects.requireNonNull(statusEffects, "statusEffects cannot be null");
        Objects.requireNonNull(equippedWeaponTypes, "equippedWeaponTypes cannot be null");
        Objects.requireNonNull(extraFlags, "extraFlags cannot be null");
        
        // Clamp health percentage
        if (healthPercentBps < 0) healthPercentBps = 0;
        if (healthPercentBps > 10000) healthPercentBps = 10000;
    }
    
    /**
     * Check if the entity has a specific status effect.
     *
     * @param effect The status effect to check
     * @return true if the entity has this status effect
     */
    public boolean hasStatusEffect(@Nonnull StatusEffect effect) {
        return statusEffects.contains(effect);
    }
    
    /**
     * Check if health is below a threshold.
     *
     * @param thresholdBps The threshold in basis points (e.g., 5000 = 50%)
     * @return true if current health is below the threshold
     */
    public boolean isHealthBelow(int thresholdBps) {
        return healthPercentBps < thresholdBps;
    }
    
    /**
     * Check if health is at or above a threshold.
     *
     * @param thresholdBps The threshold in basis points (e.g., 5000 = 50%)
     * @return true if current health is at or above the threshold
     */
    public boolean isHealthAtOrAbove(int thresholdBps) {
        return healthPercentBps >= thresholdBps;
    }
    
    /**
     * Check if a weapon type is equipped.
     *
     * @param weaponType The weapon type to check
     * @return true if this weapon type is equipped
     */
    public boolean hasWeaponType(@Nonnull WeaponType weaponType) {
        return equippedWeaponTypes.contains(weaponType);
    }
    
    /**
     * Check if an extra flag is set.
     *
     * @param flag The flag name to check
     * @return true if the flag is present
     */
    public boolean hasFlag(@Nonnull String flag) {
        return extraFlags.contains(flag);
    }
    
    /**
     * Status effects that can affect modifier conditions.
     */
    public enum StatusEffect {
        BLEEDING,
        POISONED,
        BURNING,
        FROZEN,
        CHILLED,
        SHOCKED,
        STUNNED,
        SLOWED,
        SILENCED,
        WEAKENED,
        FORTIFIED,
        ENRAGED,
        INVISIBLE,
        INVULNERABLE
    }
    
    /**
     * Weapon types for equipment-based conditions.
     */
    public enum WeaponType {
        SWORD,
        AXE,
        MACE,
        DAGGER,
        STAFF,
        WAND,
        BOW,
        CROSSBOW,
        SPEAR,
        POLEARM,
        UNARMED,
        SHIELD
    }
    
    /**
     * Builder for creating QueryContext instances.
     */
    public static class Builder {
        private final Set<StatusEffect> statusEffects = EnumSet.noneOf(StatusEffect.class);
        private int healthPercentBps = 10000;
        private final Set<WeaponType> equippedWeaponTypes = EnumSet.noneOf(WeaponType.class);
        private boolean hasShieldEquipped = false;
        private boolean isInCombat = false;
        private final Set<String> extraFlags = new java.util.HashSet<>();
        
        public Builder() {}
        
        public Builder withStatusEffect(@Nonnull StatusEffect effect) {
            statusEffects.add(effect);
            return this;
        }
        
        public Builder withStatusEffects(@Nonnull Set<StatusEffect> effects) {
            statusEffects.addAll(effects);
            return this;
        }
        
        public Builder withHealthPercent(int bps) {
            this.healthPercentBps = bps;
            return this;
        }
        
        public Builder withWeaponType(@Nonnull WeaponType weaponType) {
            equippedWeaponTypes.add(weaponType);
            return this;
        }
        
        public Builder withShield(boolean hasShield) {
            this.hasShieldEquipped = hasShield;
            return this;
        }
        
        public Builder inCombat(boolean inCombat) {
            this.isInCombat = inCombat;
            return this;
        }
        
        public Builder withFlag(@Nonnull String flag) {
            extraFlags.add(flag);
            return this;
        }
        
        public QueryContext build() {
            return new QueryContext(
                statusEffects.isEmpty() ? Collections.emptySet() : EnumSet.copyOf(statusEffects),
                healthPercentBps,
                equippedWeaponTypes.isEmpty() ? Collections.emptySet() : EnumSet.copyOf(equippedWeaponTypes),
                hasShieldEquipped,
                isInCombat,
                extraFlags.isEmpty() ? Collections.emptySet() : Set.copyOf(extraFlags)
            );
        }
    }
    
    /**
     * Create a new builder.
     *
     * @return A new QueryContext.Builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
