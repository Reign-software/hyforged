package reign.software.hyforged.combat.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Result of a combat action performed through {@link CombatService}.
 * <p>
 * Contains all details about what happened during the damage application:
 * <ul>
 *   <li>Whether the attack hit, missed, or was blocked</li>
 *   <li>Base and final damage amounts</li>
 *   <li>Critical hit information</li>
 *   <li>Per-element damage breakdown</li>
 *   <li>Any ailments triggered</li>
 * </ul>
 * <p>
 * CombatResult is immutable and can be safely shared.
 *
 * @see CombatService#applyDamage
 */
public final class CombatResult {

    /**
     * Overall outcome of the combat action.
     */
    public enum Outcome {
        /** Attack hit and dealt damage */
        HIT,
        /** Attack was evaded (missed) */
        EVADED,
        /** Attack was blocked (reduced or prevented) */
        BLOCKED,
        /** Attack was cancelled before processing */
        CANCELLED,
        /** Defender was already dead */
        TARGET_DEAD,
        /** Invalid attacker or defender reference */
        INVALID_ENTITY
    }

    private final Outcome outcome;
    @Nullable
    private final UUID attackerUuid;
    @Nullable
    private final UUID defenderUuid;
    private final float totalBaseDamage;
    private final float totalFinalDamage;
    private final boolean criticalHit;
    private final int critMultiplierBps;
    private final boolean blocked;
    private final boolean autoBlocked;
    private final int blockMitigationBps;
    private final List<DamageBreakdown> damageBreakdown;
    private final List<String> ailmentsTriggered;
    private final long timestamp;

    private CombatResult(Builder builder) {
        this.outcome = builder.outcome;
        this.attackerUuid = builder.attackerUuid;
        this.defenderUuid = builder.defenderUuid;
        this.totalBaseDamage = builder.totalBaseDamage;
        this.totalFinalDamage = builder.totalFinalDamage;
        this.criticalHit = builder.criticalHit;
        this.critMultiplierBps = builder.critMultiplierBps;
        this.blocked = builder.blocked;
        this.autoBlocked = builder.autoBlocked;
        this.blockMitigationBps = builder.blockMitigationBps;
        this.damageBreakdown = Collections.unmodifiableList(new ArrayList<>(builder.damageBreakdown));
        this.ailmentsTriggered = Collections.unmodifiableList(new ArrayList<>(builder.ailmentsTriggered));
        this.timestamp = builder.timestamp;
    }

    /**
     * Get the overall outcome of the combat action.
     *
     * @return The outcome
     */
    @Nonnull
    public Outcome getOutcome() {
        return outcome;
    }

    /**
     * Check if the attack successfully dealt damage.
     *
     * @return true if outcome is HIT
     */
    public boolean wasHit() {
        return outcome == Outcome.HIT;
    }

    /**
     * Check if the attack was evaded.
     *
     * @return true if outcome is EVADED
     */
    public boolean wasEvaded() {
        return outcome == Outcome.EVADED;
    }

    /**
     * Check if the attack was blocked.
     *
     * @return true if outcome is BLOCKED (full block)
     */
    public boolean wasFullyBlocked() {
        return outcome == Outcome.BLOCKED;
    }

    /**
     * Get the attacker's UUID if available.
     *
     * @return Attacker UUID or null for environmental damage
     */
    @Nullable
    public UUID getAttackerUuid() {
        return attackerUuid;
    }

    /**
     * Get the defender's UUID.
     *
     * @return Defender UUID or null if invalid
     */
    @Nullable
    public UUID getDefenderUuid() {
        return defenderUuid;
    }

    /**
     * Get total base damage before any modifications.
     *
     * @return Total base damage
     */
    public float getTotalBaseDamage() {
        return totalBaseDamage;
    }

    /**
     * Get total final damage after all modifications.
     *
     * @return Total final damage dealt
     */
    public float getTotalFinalDamage() {
        return totalFinalDamage;
    }

    /**
     * Check if this was a critical hit.
     *
     * @return true if crit
     */
    public boolean isCriticalHit() {
        return criticalHit;
    }

    /**
     * Get the crit multiplier in basis points.
     *
     * @return Crit multiplier (0 if no crit)
     */
    public int getCritMultiplierBps() {
        return critMultiplierBps;
    }

    /**
     * Check if the attack was blocked (partial or full).
     *
     * @return true if any blocking occurred
     */
    public boolean wasBlocked() {
        return blocked;
    }

    /**
     * Check if auto-block triggered.
     *
     * @return true if auto-block
     */
    public boolean wasAutoBlocked() {
        return autoBlocked;
    }

    /**
     * Get block mitigation in basis points.
     *
     * @return Block mitigation (0 if not blocked)
     */
    public int getBlockMitigationBps() {
        return blockMitigationBps;
    }

    /**
     * Get per-element damage breakdown.
     *
     * @return Unmodifiable list of damage breakdowns
     */
    @Nonnull
    public List<DamageBreakdown> getDamageBreakdown() {
        return damageBreakdown;
    }

    /**
     * Get IDs of ailments triggered by this damage.
     *
     * @return Unmodifiable list of ailment IDs
     */
    @Nonnull
    public List<String> getAilmentsTriggered() {
        return ailmentsTriggered;
    }

    /**
     * Get the timestamp when this result was created.
     *
     * @return Timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Calculate damage reduction percentage.
     *
     * @return Reduction as 0-100 percentage
     */
    public float getDamageReductionPercent() {
        if (totalBaseDamage <= 0) {
            return 0;
        }
        return ((totalBaseDamage - totalFinalDamage) / totalBaseDamage) * 100;
    }

    /**
     * Create a new builder.
     *
     * @return New builder instance
     */
    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a result for an evaded attack.
     *
     * @param attackerUuid Attacker UUID
     * @param defenderUuid Defender UUID
     * @param baseDamage Original base damage
     * @return CombatResult with EVADED outcome
     */
    @Nonnull
    public static CombatResult evaded(@Nullable UUID attackerUuid, @Nullable UUID defenderUuid, float baseDamage) {
        return builder()
                .outcome(Outcome.EVADED)
                .attackerUuid(attackerUuid)
                .defenderUuid(defenderUuid)
                .totalBaseDamage(baseDamage)
                .totalFinalDamage(0)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Create a result for an invalid entity.
     *
     * @return CombatResult with INVALID_ENTITY outcome
     */
    @Nonnull
    public static CombatResult invalidEntity() {
        return builder()
                .outcome(Outcome.INVALID_ENTITY)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Create a result for a dead target.
     *
     * @param defenderUuid Defender UUID
     * @return CombatResult with TARGET_DEAD outcome
     */
    @Nonnull
    public static CombatResult targetDead(@Nullable UUID defenderUuid) {
        return builder()
                .outcome(Outcome.TARGET_DEAD)
                .defenderUuid(defenderUuid)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Per-element damage breakdown.
     *
     * @param damageCauseId The damage type ID
     * @param baseDamage Base damage before reduction
     * @param finalDamage Final damage after reduction
     * @param resistanceBps Resistance applied in bps
     * @param penetrationBps Penetration applied in bps
     */
    public record DamageBreakdown(
            @Nonnull String damageCauseId,
            float baseDamage,
            float finalDamage,
            int resistanceBps,
            int penetrationBps
    ) {
        public DamageBreakdown {
            Objects.requireNonNull(damageCauseId, "damageCauseId cannot be null");
        }

        /**
         * Calculate effective resistance after penetration.
         *
         * @return Effective resistance in bps
         */
        public int getEffectiveResistanceBps() {
            return Math.max(0, resistanceBps - penetrationBps);
        }
    }

    /**
     * Builder for constructing {@link CombatResult} instances.
     */
    public static final class Builder {
        private Outcome outcome = Outcome.HIT;
        @Nullable
        private UUID attackerUuid;
        @Nullable
        private UUID defenderUuid;
        private float totalBaseDamage;
        private float totalFinalDamage;
        private boolean criticalHit;
        private int critMultiplierBps;
        private boolean blocked;
        private boolean autoBlocked;
        private int blockMitigationBps;
        private final List<DamageBreakdown> damageBreakdown = new ArrayList<>();
        private final List<String> ailmentsTriggered = new ArrayList<>();
        private long timestamp = System.currentTimeMillis();

        private Builder() {
        }

        @Nonnull
        public Builder outcome(@Nonnull Outcome outcome) {
            this.outcome = Objects.requireNonNull(outcome);
            return this;
        }

        @Nonnull
        public Builder attackerUuid(@Nullable UUID attackerUuid) {
            this.attackerUuid = attackerUuid;
            return this;
        }

        @Nonnull
        public Builder defenderUuid(@Nullable UUID defenderUuid) {
            this.defenderUuid = defenderUuid;
            return this;
        }

        @Nonnull
        public Builder totalBaseDamage(float totalBaseDamage) {
            this.totalBaseDamage = totalBaseDamage;
            return this;
        }

        @Nonnull
        public Builder totalFinalDamage(float totalFinalDamage) {
            this.totalFinalDamage = totalFinalDamage;
            return this;
        }

        @Nonnull
        public Builder criticalHit(boolean criticalHit) {
            this.criticalHit = criticalHit;
            return this;
        }

        @Nonnull
        public Builder critMultiplierBps(int critMultiplierBps) {
            this.critMultiplierBps = critMultiplierBps;
            return this;
        }

        @Nonnull
        public Builder blocked(boolean blocked) {
            this.blocked = blocked;
            return this;
        }

        @Nonnull
        public Builder autoBlocked(boolean autoBlocked) {
            this.autoBlocked = autoBlocked;
            return this;
        }

        @Nonnull
        public Builder blockMitigationBps(int blockMitigationBps) {
            this.blockMitigationBps = blockMitigationBps;
            return this;
        }

        @Nonnull
        public Builder addDamageBreakdown(@Nonnull DamageBreakdown breakdown) {
            this.damageBreakdown.add(Objects.requireNonNull(breakdown));
            return this;
        }

        @Nonnull
        public Builder addDamageBreakdown(
                @Nonnull String damageCauseId,
                float baseDamage,
                float finalDamage,
                int resistanceBps,
                int penetrationBps
        ) {
            return addDamageBreakdown(new DamageBreakdown(
                    damageCauseId, baseDamage, finalDamage, resistanceBps, penetrationBps));
        }

        @Nonnull
        public Builder addAilmentTriggered(@Nonnull String ailmentId) {
            this.ailmentsTriggered.add(Objects.requireNonNull(ailmentId));
            return this;
        }

        @Nonnull
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @Nonnull
        public CombatResult build() {
            return new CombatResult(this);
        }
    }

    @Override
    public String toString() {
        return "CombatResult{" +
                "outcome=" + outcome +
                ", baseDamage=" + totalBaseDamage +
                ", finalDamage=" + totalFinalDamage +
                ", crit=" + criticalHit +
                ", blocked=" + blocked +
                ", ailments=" + ailmentsTriggered.size() +
                "}";
    }
}
