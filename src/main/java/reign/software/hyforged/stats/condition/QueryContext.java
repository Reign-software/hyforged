package reign.software.hyforged.stats.condition;

import javax.annotation.Nonnull;
import java.util.Collections;
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
 * <p>
 * Status effects and weapon types use String identifiers for moddability.
 * Convention: use namespaced IDs like "hyforged:bleeding" or "mymod:frozen".
 *
 * @param statusEffects Set of active status effect IDs on the entity (e.g., "hyforged:bleeding")
 * @param healthPercentBps Current health as percentage in basis points (10000 = 100%)
 * @param equippedWeaponTypes Set of weapon type IDs currently equipped (e.g., "hyforged:sword")
 * @param hasShieldEquipped Whether a shield is currently equipped
 * @param isInCombat Whether the entity is currently in combat
 * @param extraFlags Additional boolean flags for extensibility
 */
public record QueryContext(
    @Nonnull Set<String> statusEffects,
    int healthPercentBps,
    @Nonnull Set<String> equippedWeaponTypes,
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
     * @param effectId The status effect ID to check (e.g., "hyforged:bleeding")
     * @return true if the entity has this status effect
     */
    public boolean hasStatusEffect(@Nonnull String effectId) {
        return statusEffects.contains(effectId);
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
     * @param weaponTypeId The weapon type ID to check (e.g., "hyforged:sword")
     * @return true if this weapon type is equipped
     */
    public boolean hasWeaponType(@Nonnull String weaponTypeId) {
        return equippedWeaponTypes.contains(weaponTypeId);
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
     * Builder for creating QueryContext instances.
     */
    public static class Builder {
        private final Set<String> statusEffects = new java.util.HashSet<>();
        private int healthPercentBps = 10000;
        private final Set<String> equippedWeaponTypes = new java.util.HashSet<>();
        private boolean hasShieldEquipped = false;
        private boolean isInCombat = false;
        private final Set<String> extraFlags = new java.util.HashSet<>();
        
        public Builder() {}
        
        /**
         * Add a status effect by ID.
         *
         * @param effectId The status effect ID (e.g., "hyforged:bleeding" or StatusEffects.BLEEDING)
         * @return this builder
         */
        public Builder withStatusEffect(@Nonnull String effectId) {
            statusEffects.add(effectId);
            return this;
        }
        
        /**
         * Add multiple status effects by ID.
         *
         * @param effectIds The status effect IDs
         * @return this builder
         */
        public Builder withStatusEffects(@Nonnull Set<String> effectIds) {
            statusEffects.addAll(effectIds);
            return this;
        }
        
        public Builder withHealthPercent(int bps) {
            this.healthPercentBps = bps;
            return this;
        }
        
        /**
         * Add a weapon type by ID.
         *
         * @param weaponTypeId The weapon type ID (e.g., "hyforged:sword" or WeaponTypes.SWORD)
         * @return this builder
         */
        public Builder withWeaponType(@Nonnull String weaponTypeId) {
            equippedWeaponTypes.add(weaponTypeId);
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
                statusEffects.isEmpty() ? Collections.emptySet() : Set.copyOf(statusEffects),
                healthPercentBps,
                equippedWeaponTypes.isEmpty() ? Collections.emptySet() : Set.copyOf(equippedWeaponTypes),
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
