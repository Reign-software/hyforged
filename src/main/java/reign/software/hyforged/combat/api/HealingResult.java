package reign.software.hyforged.combat.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Immutable result of a healing operation.
 * <p>
 * Contains the calculated and actual healing amounts, along with modifier breakdowns.
 */
public final class HealingResult {

    /**
     * Outcome of the healing operation.
     */
    public enum Outcome {
        /** Healing was applied successfully */
        HEALED,
        /** Target was already at full health, no healing applied */
        ALREADY_FULL,
        /** Target entity reference was invalid */
        INVALID_TARGET,
        /** Target is dead and cannot be healed */
        TARGET_DEAD,
        /** Healing calculation only (not applied) */
        PREVIEW
    }

    private final Outcome outcome;
    private final float baseAmount;
    private final float finalAmount;
    private final float actualHealing;
    private final float overheal;
    private final int healerEffectivenessBps;
    private final int targetHealingReceivedBps;
    private final int targetRecoveryRateBps;
    @Nullable
    private final String source;

    private HealingResult(Builder builder) {
        this.outcome = builder.outcome;
        this.baseAmount = builder.baseAmount;
        this.finalAmount = builder.finalAmount;
        this.actualHealing = builder.actualHealing;
        this.overheal = builder.overheal;
        this.healerEffectivenessBps = builder.healerEffectivenessBps;
        this.targetHealingReceivedBps = builder.targetHealingReceivedBps;
        this.targetRecoveryRateBps = builder.targetRecoveryRateBps;
        this.source = builder.source;
    }

    /**
     * Create a result for invalid target reference.
     */
    @Nonnull
    public static HealingResult invalidTarget() {
        return new Builder().outcome(Outcome.INVALID_TARGET).build();
    }

    /**
     * Create a result for dead target.
     */
    @Nonnull
    public static HealingResult targetDead() {
        return new Builder().outcome(Outcome.TARGET_DEAD).build();
    }

    /**
     * Create a result for target already at full health.
     */
    @Nonnull
    public static HealingResult alreadyFull(float baseAmount) {
        return new Builder()
                .outcome(Outcome.ALREADY_FULL)
                .baseAmount(baseAmount)
                .finalAmount(0)
                .actualHealing(0)
                .build();
    }

    /**
     * Create a builder for constructing successful healing results.
     */
    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the outcome of the healing operation.
     */
    @Nonnull
    public Outcome getOutcome() {
        return outcome;
    }

    /**
     * Whether healing was successfully applied.
     */
    public boolean wasHealed() {
        return outcome == Outcome.HEALED;
    }

    /**
     * Whether target was already at full health.
     */
    public boolean wasAlreadyFull() {
        return outcome == Outcome.ALREADY_FULL;
    }

    /**
     * Get the base healing amount before modifiers.
     */
    public float getBaseAmount() {
        return baseAmount;
    }

    /**
     * Get the calculated healing amount after all modifiers.
     * <p>
     * This is the amount that would have been healed if the target
     * wasn't already partially or fully healed.
     */
    public float getFinalAmount() {
        return finalAmount;
    }

    /**
     * Get the actual healing applied to the target.
     * <p>
     * This may be less than {@link #getFinalAmount()} if the target
     * reached max health (capped by missing health).
     */
    public float getActualHealing() {
        return actualHealing;
    }

    /**
     * Get the overheal amount (healing above max health).
     * <p>
     * Only tracked if {@link HealingSpec#isOverhealAllowed()} was set.
     */
    public float getOverheal() {
        return overheal;
    }

    /**
     * Get the healer's healing-effectiveness-bps stat value.
     */
    public int getHealerEffectivenessBps() {
        return healerEffectivenessBps;
    }

    /**
     * Get the target's healing-received-bps stat value.
     */
    public int getTargetHealingReceivedBps() {
        return targetHealingReceivedBps;
    }

    /**
     * Get the target's life-recovery-rate-bps stat value.
     */
    public int getTargetRecoveryRateBps() {
        return targetRecoveryRateBps;
    }

    /**
     * Get the source description for this healing.
     */
    @Nullable
    public String getSource() {
        return source;
    }

    /**
     * Get the total multiplier applied to healing.
     * <p>
     * Calculated as: (1 + effectiveness/10000) * (1 + received/10000) * (1 + recovery/10000)
     */
    public float getTotalMultiplier() {
        if (baseAmount == 0) return 1.0f;
        return finalAmount / baseAmount;
    }

    @Override
    public String toString() {
        return "HealingResult{" +
                "outcome=" + outcome +
                ", baseAmount=" + baseAmount +
                ", finalAmount=" + finalAmount +
                ", actualHealing=" + actualHealing +
                ", overheal=" + overheal +
                ", source='" + source + '\'' +
                '}';
    }

    /**
     * Builder for constructing HealingResult instances.
     */
    public static final class Builder {
        private Outcome outcome = Outcome.HEALED;
        private float baseAmount;
        private float finalAmount;
        private float actualHealing;
        private float overheal;
        private int healerEffectivenessBps;
        private int targetHealingReceivedBps;
        private int targetRecoveryRateBps;
        @Nullable
        private String source;

        private Builder() {}

        @Nonnull
        public Builder outcome(@Nonnull Outcome outcome) {
            this.outcome = outcome;
            return this;
        }

        @Nonnull
        public Builder baseAmount(float baseAmount) {
            this.baseAmount = baseAmount;
            return this;
        }

        @Nonnull
        public Builder finalAmount(float finalAmount) {
            this.finalAmount = finalAmount;
            return this;
        }

        @Nonnull
        public Builder actualHealing(float actualHealing) {
            this.actualHealing = actualHealing;
            return this;
        }

        @Nonnull
        public Builder overheal(float overheal) {
            this.overheal = overheal;
            return this;
        }

        @Nonnull
        public Builder healerEffectivenessBps(int bps) {
            this.healerEffectivenessBps = bps;
            return this;
        }

        @Nonnull
        public Builder targetHealingReceivedBps(int bps) {
            this.targetHealingReceivedBps = bps;
            return this;
        }

        @Nonnull
        public Builder targetRecoveryRateBps(int bps) {
            this.targetRecoveryRateBps = bps;
            return this;
        }

        @Nonnull
        public Builder source(@Nullable String source) {
            this.source = source;
            return this;
        }

        @Nonnull
        public HealingResult build() {
            return new HealingResult(this);
        }
    }
}
