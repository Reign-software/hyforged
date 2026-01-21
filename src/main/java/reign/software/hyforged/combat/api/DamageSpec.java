package reign.software.hyforged.combat.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Specification for damage to be applied through the {@link CombatService}.
 * <p>
 * A DamageSpec describes the damage to deal, supporting:
 * <ul>
 *   <li>Single or multi-element damage via multiple entries</li>
 *   <li>Forced crit or no-crit flags for skill effects</li>
 *   <li>Skip evasion check for unavoidable attacks</li>
 *   <li>Skip block check for unblockable attacks</li>
 * </ul>
 * <p>
 * Use the {@link Builder} to construct instances.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple physical damage
 * DamageSpec spec = DamageSpec.builder()
 *     .addDamage("Physical", 50)
 *     .build();
 *
 * // Multi-element attack with forced crit
 * DamageSpec spec = DamageSpec.builder()
 *     .addDamage("Physical", 30)
 *     .addDamage("Fire", 20)
 *     .forceCrit(true)
 *     .build();
 *
 * // Unavoidable spell damage
 * DamageSpec spec = DamageSpec.builder()
 *     .addDamage("Fire", 100)
 *     .skipEvasion(true)
 *     .skipBlock(true)
 *     .build();
 * }</pre>
 *
 * @see CombatService#applyDamage
 * @see DamageEntry
 */
public final class DamageSpec {

    private final List<DamageEntry> damageEntries;
    private final boolean forceCrit;
    private final boolean noCrit;
    private final boolean skipEvasion;
    private final boolean skipBlock;
    private final boolean skipResistance;
    private final boolean skipAilments;
    @Nullable
    private final String sourceDescription;

    private DamageSpec(Builder builder) {
        this.damageEntries = Collections.unmodifiableList(new ArrayList<>(builder.damageEntries));
        this.forceCrit = builder.forceCrit;
        this.noCrit = builder.noCrit;
        this.skipEvasion = builder.skipEvasion;
        this.skipBlock = builder.skipBlock;
        this.skipResistance = builder.skipResistance;
        this.skipAilments = builder.skipAilments;
        this.sourceDescription = builder.sourceDescription;
    }

    /**
     * Get all damage entries in this spec.
     *
     * @return Unmodifiable list of damage entries
     */
    @Nonnull
    public List<DamageEntry> getDamageEntries() {
        return damageEntries;
    }

    /**
     * Get total base damage across all entries.
     *
     * @return Sum of all damage entry amounts
     */
    public float getTotalBaseDamage() {
        return (float) damageEntries.stream()
                .mapToDouble(DamageEntry::amount)
                .sum();
    }

    /**
     * Whether this attack should force a critical hit.
     *
     * @return true if crit is guaranteed
     */
    public boolean isForceCrit() {
        return forceCrit;
    }

    /**
     * Whether this attack cannot critically hit.
     *
     * @return true if crit is disabled
     */
    public boolean isNoCrit() {
        return noCrit;
    }

    /**
     * Whether evasion check should be skipped (unavoidable attack).
     *
     * @return true if attack cannot be evaded
     */
    public boolean isSkipEvasion() {
        return skipEvasion;
    }

    /**
     * Whether block check should be skipped (unblockable attack).
     *
     * @return true if attack cannot be blocked
     */
    public boolean isSkipBlock() {
        return skipBlock;
    }

    /**
     * Whether resistance should be skipped (true damage).
     *
     * @return true if resistance is ignored
     */
    public boolean isSkipResistance() {
        return skipResistance;
    }

    /**
     * Whether ailment accumulation should be skipped.
     *
     * @return true if ailments should not trigger
     */
    public boolean isSkipAilments() {
        return skipAilments;
    }

    /**
     * Optional description for combat log.
     *
     * @return Source description or null
     */
    @Nullable
    public String getSourceDescription() {
        return sourceDescription;
    }

    /**
     * Check if this spec has no damage entries.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return damageEntries.isEmpty();
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
     * Create a simple single-element damage spec.
     *
     * @param damageCauseId The damage type ID (e.g., "Physical", "Fire")
     * @param amount The damage amount
     * @return New DamageSpec with single entry
     */
    @Nonnull
    public static DamageSpec of(@Nonnull String damageCauseId, float amount) {
        return builder().addDamage(damageCauseId, amount).build();
    }

    /**
     * Single damage entry with a type and amount.
     *
     * @param damageCauseId The damage cause ID (matches Hytale's DamageCause assets)
     * @param amount The base damage amount
     */
    public record DamageEntry(
            @Nonnull String damageCauseId,
            float amount
    ) {
        public DamageEntry {
            Objects.requireNonNull(damageCauseId, "damageCauseId cannot be null");
            if (amount < 0) {
                throw new IllegalArgumentException("Damage amount cannot be negative: " + amount);
            }
        }
    }

    /**
     * Builder for constructing {@link DamageSpec} instances.
     */
    public static final class Builder {
        private final List<DamageEntry> damageEntries = new ArrayList<>();
        private boolean forceCrit;
        private boolean noCrit;
        private boolean skipEvasion;
        private boolean skipBlock;
        private boolean skipResistance;
        private boolean skipAilments;
        @Nullable
        private String sourceDescription;

        private Builder() {
        }

        /**
         * Add a damage entry.
         *
         * @param damageCauseId The damage type ID
         * @param amount The damage amount
         * @return this builder
         */
        @Nonnull
        public Builder addDamage(@Nonnull String damageCauseId, float amount) {
            damageEntries.add(new DamageEntry(damageCauseId, amount));
            return this;
        }

        /**
         * Force this attack to critically hit.
         * Mutually exclusive with {@link #noCrit(boolean)}.
         *
         * @param forceCrit true to guarantee crit
         * @return this builder
         */
        @Nonnull
        public Builder forceCrit(boolean forceCrit) {
            this.forceCrit = forceCrit;
            if (forceCrit) {
                this.noCrit = false;
            }
            return this;
        }

        /**
         * Prevent this attack from critically hitting.
         * Mutually exclusive with {@link #forceCrit(boolean)}.
         *
         * @param noCrit true to disable crit
         * @return this builder
         */
        @Nonnull
        public Builder noCrit(boolean noCrit) {
            this.noCrit = noCrit;
            if (noCrit) {
                this.forceCrit = false;
            }
            return this;
        }

        /**
         * Skip the evasion check (unavoidable attack).
         *
         * @param skipEvasion true to skip evasion
         * @return this builder
         */
        @Nonnull
        public Builder skipEvasion(boolean skipEvasion) {
            this.skipEvasion = skipEvasion;
            return this;
        }

        /**
         * Skip the block check (unblockable attack).
         *
         * @param skipBlock true to skip block
         * @return this builder
         */
        @Nonnull
        public Builder skipBlock(boolean skipBlock) {
            this.skipBlock = skipBlock;
            return this;
        }

        /**
         * Skip resistance calculation (true damage).
         *
         * @param skipResistance true to ignore resistance
         * @return this builder
         */
        @Nonnull
        public Builder skipResistance(boolean skipResistance) {
            this.skipResistance = skipResistance;
            return this;
        }

        /**
         * Skip ailment accumulation.
         *
         * @param skipAilments true to skip ailment triggers
         * @return this builder
         */
        @Nonnull
        public Builder skipAilments(boolean skipAilments) {
            this.skipAilments = skipAilments;
            return this;
        }

        /**
         * Set optional source description for combat log.
         *
         * @param sourceDescription Description like "Fireball" or "Whirlwind"
         * @return this builder
         */
        @Nonnull
        public Builder sourceDescription(@Nullable String sourceDescription) {
            this.sourceDescription = sourceDescription;
            return this;
        }

        /**
         * Build the DamageSpec.
         *
         * @return New DamageSpec instance
         * @throws IllegalStateException if no damage entries were added
         */
        @Nonnull
        public DamageSpec build() {
            if (damageEntries.isEmpty()) {
                throw new IllegalStateException("DamageSpec must have at least one damage entry");
            }
            return new DamageSpec(this);
        }
    }

    @Override
    public String toString() {
        return "DamageSpec{" +
                "entries=" + damageEntries +
                ", forceCrit=" + forceCrit +
                ", noCrit=" + noCrit +
                ", skipEvasion=" + skipEvasion +
                ", skipBlock=" + skipBlock +
                ", skipResistance=" + skipResistance +
                "}";
    }
}
