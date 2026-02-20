package reign.software.hyforged;

import javax.annotation.Nonnull;

/**
 * Plugin-level feature flags for Hyforged.
 * <p>
 * Each flag defaults to {@code false} so that stub systems have zero gameplay
 * impact until explicitly enabled by a server operator.
 * <p>
 * Access via {@link #get()} from any system or service.
 */
public final class HyforgedConfig {

    private static final HyforgedConfig INSTANCE = new HyforgedConfig();

    /**
     * Guard flag for {@link reign.software.hyforged.combat.HyforgedSpellBlockSystem}.
     * Defaults to {@code false} — no gameplay impact until set to {@code true}.
     */
    private boolean spellBlockEnabled = false;

    /**
     * Guard flag for {@link reign.software.hyforged.combat.HyforgedDodgeSystem}.
     * Defaults to {@code false} — no gameplay impact until set to {@code true}.
     */
    private boolean dodgeEnabled = false;

    /**
     * Regen interval in ticks (default 20 = 1 second at 20 TPS).
     * Drives {@link reign.software.hyforged.stats.system.HyforgedRegenSystem}.
     */
    private int regenIntervalTicks = 20;

    /**
     * Base knockback velocity in blocks/second applied when knockback is triggered.
     * Scaled by {@code hyforged:knockback-distance-bps} and reduced by
     * {@code hyforged:knockback-resistance-bps}.
     */
    private float baseKnockbackVelocity = 1.0f;

    /**
     * Duration in seconds that a knockback effect lasts.
     */
    private float baseKnockbackDurationSeconds = 0.3f;

    private HyforgedConfig() {
    }

    /**
     * Get the singleton config instance.
     */
    @Nonnull
    public static HyforgedConfig get() {
        return INSTANCE;
    }

    /**
     * Returns {@code true} if the spell-block / suppression system is active.
     *
     * @see reign.software.hyforged.combat.HyforgedSpellBlockSystem
     */
    public boolean isSpellBlockEnabled() {
        return spellBlockEnabled;
    }

    /**
     * Enable or disable the spell-block system.
     *
     * @param spellBlockEnabled {@code true} to activate the system
     */
    public void setSpellBlockEnabled(boolean spellBlockEnabled) {
        this.spellBlockEnabled = spellBlockEnabled;
    }

    /**
     * Returns {@code true} if the dodge system is active.
     *
     * @see reign.software.hyforged.combat.HyforgedDodgeSystem
     */
    public boolean isDodgeEnabled() {
        return dodgeEnabled;
    }

    /**
     * Enable or disable the dodge system.
     *
     * @param dodgeEnabled {@code true} to activate the system
     */
    public void setDodgeEnabled(boolean dodgeEnabled) {
        this.dodgeEnabled = dodgeEnabled;
    }

    /**
     * Returns the regen interval in ticks.
     *
     * @see reign.software.hyforged.stats.system.HyforgedRegenSystem
     */
    public int getRegenIntervalTicks() {
        return regenIntervalTicks;
    }

    /**
     * Sets the regen interval in ticks.
     *
     * @param regenIntervalTicks ticks between regen pulses (default 20 = 1 second)
     */
    public void setRegenIntervalTicks(int regenIntervalTicks) {
        this.regenIntervalTicks = regenIntervalTicks;
    }

    /**
     * Returns the base knockback velocity in blocks/second.
     *
     * @see reign.software.hyforged.combat.HyforgedKnockbackSystem
     */
    public float getBaseKnockbackVelocity() {
        return baseKnockbackVelocity;
    }

    /**
     * Sets the base knockback velocity in blocks/second.
     */
    public void setBaseKnockbackVelocity(float baseKnockbackVelocity) {
        this.baseKnockbackVelocity = baseKnockbackVelocity;
    }

    /**
     * Returns the base knockback duration in seconds.
     *
     * @see reign.software.hyforged.combat.HyforgedKnockbackSystem
     */
    public float getBaseKnockbackDurationSeconds() {
        return baseKnockbackDurationSeconds;
    }

    /**
     * Sets the base knockback duration in seconds.
     */
    public void setBaseKnockbackDurationSeconds(float baseKnockbackDurationSeconds) {
        this.baseKnockbackDurationSeconds = baseKnockbackDurationSeconds;
    }
}
