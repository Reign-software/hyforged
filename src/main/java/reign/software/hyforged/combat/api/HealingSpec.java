package reign.software.hyforged.combat.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Immutable specification for a healing operation.
 * <p>
 * Use the static factory methods for simple healing or the builder for complex cases.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple healing
 * HealingSpec spec = HealingSpec.of(50);
 *
 * // Complex healing with options
 * HealingSpec spec = HealingSpec.builder()
 *     .amount(100)
 *     .source("Holy Light")
 *     .skipRecoveryRate(false)
 *     .logToCombatLog(true)
 *     .build();
 * }</pre>
 */
public final class HealingSpec {

    private final float amount;
    @Nullable
    private final String source;
    private final boolean skipHealingReceived;
    private final boolean skipRecoveryRate;
    private final boolean logToCombatLog;
    private final boolean isOverhealAllowed;

    private HealingSpec(Builder builder) {
        this.amount = builder.amount;
        this.source = builder.source;
        this.skipHealingReceived = builder.skipHealingReceived;
        this.skipRecoveryRate = builder.skipRecoveryRate;
        this.logToCombatLog = builder.logToCombatLog;
        this.isOverhealAllowed = builder.isOverhealAllowed;
    }

    /**
     * Create a simple healing spec with just an amount.
     *
     * @param amount Base healing amount
     * @return HealingSpec with default options
     */
    @Nonnull
    public static HealingSpec of(float amount) {
        return builder().amount(amount).build();
    }

    /**
     * Create a healing spec with amount and source description.
     *
     * @param amount Base healing amount
     * @param source Source description for logging (e.g., "Healing Potion", "Holy Light")
     * @return HealingSpec with source
     */
    @Nonnull
    public static HealingSpec of(float amount, @Nonnull String source) {
        return builder().amount(amount).source(source).build();
    }

    /**
     * Create a builder for complex healing specifications.
     *
     * @return New Builder instance
     */
    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the base healing amount before modifiers.
     */
    public float getAmount() {
        return amount;
    }

    /**
     * Get the source description for combat logging.
     */
    @Nullable
    public String getSource() {
        return source;
    }

    /**
     * Whether to skip the healing-received-bps modifier.
     */
    public boolean isSkipHealingReceived() {
        return skipHealingReceived;
    }

    /**
     * Whether to skip the life-recovery-rate-bps modifier.
     */
    public boolean isSkipRecoveryRate() {
        return skipRecoveryRate;
    }

    /**
     * Whether to log this healing event to combat log.
     */
    public boolean isLogToCombatLog() {
        return logToCombatLog;
    }

    /**
     * Whether overhealing is tracked (healing above max health).
     * <p>
     * Note: Overhealing never actually increases health above max,
     * but this flag enables tracking the overflow amount in the result.
     */
    public boolean isOverhealAllowed() {
        return isOverhealAllowed;
    }

    @Override
    public String toString() {
        return "HealingSpec{" +
                "amount=" + amount +
                ", source='" + source + '\'' +
                ", skipHealingReceived=" + skipHealingReceived +
                ", skipRecoveryRate=" + skipRecoveryRate +
                ", logToCombatLog=" + logToCombatLog +
                '}';
    }

    /**
     * Builder for constructing HealingSpec instances.
     */
    public static final class Builder {
        private float amount;
        @Nullable
        private String source;
        private boolean skipHealingReceived = false;
        private boolean skipRecoveryRate = false;
        private boolean logToCombatLog = false;
        private boolean isOverhealAllowed = false;

        private Builder() {}

        /**
         * Set the base healing amount.
         */
        @Nonnull
        public Builder amount(float amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Set the source description for combat logging.
         */
        @Nonnull
        public Builder source(@Nullable String source) {
            this.source = source;
            return this;
        }

        /**
         * Skip the healing-received-bps modifier on the target.
         * <p>
         * Use this for raw healing that shouldn't be modified by target stats.
         */
        @Nonnull
        public Builder skipHealingReceived(boolean skip) {
            this.skipHealingReceived = skip;
            return this;
        }

        /**
         * Skip the life-recovery-rate-bps modifier on the target.
         * <p>
         * Use this for healing that shouldn't scale with recovery rate.
         */
        @Nonnull
        public Builder skipRecoveryRate(boolean skip) {
            this.skipRecoveryRate = skip;
            return this;
        }

        /**
         * Enable combat log recording for this healing event.
         * <p>
         * By default, healing is not logged to combat log.
         */
        @Nonnull
        public Builder logToCombatLog(boolean log) {
            this.logToCombatLog = log;
            return this;
        }

        /**
         * Track overheal amount in the result.
         * <p>
         * Note: This doesn't allow health to exceed max, just tracks overflow.
         */
        @Nonnull
        public Builder trackOverheal(boolean track) {
            this.isOverhealAllowed = track;
            return this;
        }

        /**
         * Build the HealingSpec.
         */
        @Nonnull
        public HealingSpec build() {
            if (amount < 0) {
                throw new IllegalArgumentException("Healing amount cannot be negative: " + amount);
            }
            return new HealingSpec(this);
        }
    }
}
