package reign.software.hyforged.combat.log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Immutable record of a single combat event (one attack).
 * <p>
 * Captures all details of a damage event including damage type,
 * amount, and any special flags (crit, block, miss).
 */
public record CombatEvent(
        /** When this event occurred (server tick or timestamp) */
        long timestamp,
        
        /** UUID of the attacker (may be null for environmental damage) */
        @Nullable UUID attackerUuid,
        
        /** UUID of the defender */
        @Nonnull UUID defenderUuid,
        
        /** Name/ID of the attacker for display */
        @Nullable String attackerName,
        
        /** Name/ID of the defender for display */
        @Nonnull String defenderName,
        
        /** Damage cause ID (e.g., "Physical", "Fire") */
        @Nonnull String damageCauseId,
        
        /** Base damage before reductions */
        float baseDamage,
        
        /** Final damage after all modifications */
        float finalDamage,
        
        /** True if the attack missed */
        boolean missed,
        
        /** True if the attack was blocked */
        boolean blocked,
        
        /** True if auto-block triggered */
        boolean autoBlocked,
        
        /** True if this was a critical hit */
        boolean criticalHit,
        
        /** Crit multiplier applied (in bps, 0 if no crit) */
        int critMultiplierBps,
        
        /** Resistance applied (in bps) */
        int resistanceAppliedBps,
        
        /** Penetration applied (in bps) */
        int penetrationAppliedBps
) {
    /**
     * Builder for creating CombatEvent instances.
     */
    public static class Builder {
        private long timestamp;
        private UUID attackerUuid;
        private UUID defenderUuid;
        private String attackerName;
        private String defenderName;
        private String damageCauseId = "Unknown";
        private float baseDamage;
        private float finalDamage;
        private boolean missed;
        private boolean blocked;
        private boolean autoBlocked;
        private boolean criticalHit;
        private int critMultiplierBps;
        private int resistanceAppliedBps;
        private int penetrationAppliedBps;
        
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder attackerUuid(@Nullable UUID attackerUuid) {
            this.attackerUuid = attackerUuid;
            return this;
        }
        
        public Builder defenderUuid(@Nonnull UUID defenderUuid) {
            this.defenderUuid = defenderUuid;
            return this;
        }
        
        public Builder attackerName(@Nullable String attackerName) {
            this.attackerName = attackerName;
            return this;
        }
        
        public Builder defenderName(@Nonnull String defenderName) {
            this.defenderName = defenderName;
            return this;
        }
        
        public Builder damageCauseId(@Nonnull String damageCauseId) {
            this.damageCauseId = damageCauseId;
            return this;
        }
        
        public Builder baseDamage(float baseDamage) {
            this.baseDamage = baseDamage;
            return this;
        }
        
        public Builder finalDamage(float finalDamage) {
            this.finalDamage = finalDamage;
            return this;
        }
        
        public Builder missed(boolean missed) {
            this.missed = missed;
            return this;
        }
        
        public Builder blocked(boolean blocked) {
            this.blocked = blocked;
            return this;
        }
        
        public Builder autoBlocked(boolean autoBlocked) {
            this.autoBlocked = autoBlocked;
            return this;
        }
        
        public Builder criticalHit(boolean criticalHit) {
            this.criticalHit = criticalHit;
            return this;
        }
        
        public Builder critMultiplierBps(int critMultiplierBps) {
            this.critMultiplierBps = critMultiplierBps;
            return this;
        }
        
        public Builder resistanceAppliedBps(int resistanceAppliedBps) {
            this.resistanceAppliedBps = resistanceAppliedBps;
            return this;
        }
        
        public Builder penetrationAppliedBps(int penetrationAppliedBps) {
            this.penetrationAppliedBps = penetrationAppliedBps;
            return this;
        }
        
        public CombatEvent build() {
            return new CombatEvent(
                    timestamp,
                    attackerUuid,
                    defenderUuid,
                    attackerName,
                    defenderName,
                    damageCauseId,
                    baseDamage,
                    finalDamage,
                    missed,
                    blocked,
                    autoBlocked,
                    criticalHit,
                    critMultiplierBps,
                    resistanceAppliedBps,
                    penetrationAppliedBps
            );
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
