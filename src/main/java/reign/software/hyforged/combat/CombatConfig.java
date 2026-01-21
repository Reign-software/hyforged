package reign.software.hyforged.combat;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Configuration for the combat system.
 * <p>
 * Provides runtime toggles for debug and balance testing features.
 *
 * <h2>Debug Mode</h2>
 * When enabled, debug mode logs verbose information about combat calculations:
 * <ul>
 *   <li>Hit/evasion rolls with intermediate values</li>
 *   <li>Block checks and stamina costs</li>
 *   <li>Damage breakdown per element</li>
 *   <li>Critical hit rolls and multipliers</li>
 *   <li>Resistance and penetration calculations</li>
 *   <li>Ailment accumulation and triggers</li>
 *   <li>Healing calculations and modifiers</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Enable debug mode for balance testing
 * CombatConfig.setDebugEnabled(true);
 *
 * // Check if debug is enabled before expensive logging
 * if (CombatConfig.isDebugEnabled()) {
 *     CombatConfig.debug("Hit check: accuracy=%d evasion=%d result=%s",
 *         accuracy, evasion, didHit);
 * }
 * }</pre>
 */
public final class CombatConfig {

    private static final AtomicBoolean debugEnabled = new AtomicBoolean(false);
    private static final AtomicBoolean verboseLogging = new AtomicBoolean(false);

    private CombatConfig() {
        // Utility class
    }

    // ==================== Debug Mode ====================

    /**
     * Check if debug mode is enabled.
     *
     * @return true if debug logging is active
     */
    public static boolean isDebugEnabled() {
        return debugEnabled.get();
    }

    /**
     * Enable or disable debug mode.
     *
     * @param enabled true to enable debug logging
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled.set(enabled);
        if (enabled) {
            debug("Combat debug mode ENABLED");
        }
    }

    /**
     * Check if verbose logging is enabled.
     * <p>
     * Verbose mode logs even more detail than debug mode, including
     * per-tick updates and internal state changes.
     *
     * @return true if verbose logging is active
     */
    public static boolean isVerboseEnabled() {
        return verboseLogging.get();
    }

    /**
     * Enable or disable verbose logging.
     *
     * @param enabled true to enable verbose logging
     */
    public static void setVerboseEnabled(boolean enabled) {
        verboseLogging.set(enabled);
        if (enabled) {
            debug("Combat verbose mode ENABLED");
        }
    }

    // ==================== Logging Helpers ====================

    /**
     * Log a debug message if debug mode is enabled.
     *
     * @param message The message to log
     */
    public static void debug(String message) {
        if (debugEnabled.get()) {
            System.out.println("[COMBAT DEBUG] " + message);
        }
    }

    /**
     * Log a debug message with format arguments if debug mode is enabled.
     *
     * @param format The format string
     * @param args The format arguments
     */
    public static void debug(String format, Object... args) {
        if (debugEnabled.get()) {
            System.out.println("[COMBAT DEBUG] " + String.format(format, args));
        }
    }

    /**
     * Log a verbose message if verbose mode is enabled.
     *
     * @param message The message to log
     */
    public static void verbose(String message) {
        if (verboseLogging.get()) {
            System.out.println("[COMBAT VERBOSE] " + message);
        }
    }

    /**
     * Log a verbose message with format arguments if verbose mode is enabled.
     *
     * @param format The format string
     * @param args The format arguments
     */
    public static void verbose(String format, Object... args) {
        if (verboseLogging.get()) {
            System.out.println("[COMBAT VERBOSE] " + String.format(format, args));
        }
    }

    // ==================== Calculation Logging ====================

    /**
     * Log a hit calculation result.
     *
     * @param attackerAccuracy Attacker accuracy in bps
     * @param defenderEvasion Defender evasion in bps
     * @param hitChance Calculated hit chance in bps
     * @param roll The random roll (0-9999)
     * @param didHit Whether the attack hit
     */
    public static void logHitCalc(int attackerAccuracy, int defenderEvasion,
            int hitChance, int roll, boolean didHit) {
        if (debugEnabled.get()) {
            debug("HIT CHECK: acc=%d eva=%d -> hitChance=%d%% roll=%d -> %s",
                    attackerAccuracy / 100, defenderEvasion / 100,
                    hitChance / 100, roll, didHit ? "HIT" : "MISS");
        }
    }

    /**
     * Log a block calculation result.
     *
     * @param blockChance Block chance in bps
     * @param stamina Current stamina
     * @param staminaCost Block stamina cost
     * @param blocked Whether the attack was blocked
     */
    public static void logBlockCalc(int blockChance, float stamina,
            float staminaCost, boolean blocked) {
        if (debugEnabled.get()) {
            debug("BLOCK CHECK: chance=%d%% stamina=%.1f cost=%.1f -> %s",
                    blockChance / 100, stamina, staminaCost,
                    blocked ? "BLOCKED" : "NOT BLOCKED");
        }
    }

    /**
     * Log damage calculation details.
     *
     * @param element The damage element
     * @param baseDamage Base damage before modifiers
     * @param resistance Resistance in bps
     * @param penetration Penetration in bps
     * @param finalDamage Final damage dealt
     */
    public static void logDamageCalc(String element, float baseDamage,
            int resistance, int penetration, float finalDamage) {
        if (debugEnabled.get()) {
            debug("DAMAGE [%s]: base=%.2f res=%d%% pen=%d%% -> final=%.2f",
                    element, baseDamage, resistance / 100, penetration / 100, finalDamage);
        }
    }

    /**
     * Log a critical hit calculation result.
     *
     * @param critChance Crit chance in bps
     * @param roll The random roll (0-9999)
     * @param isCrit Whether it was a critical hit
     * @param multiplier The crit multiplier (if crit)
     */
    public static void logCritCalc(int critChance, int roll, boolean isCrit, int multiplier) {
        if (debugEnabled.get()) {
            if (isCrit) {
                debug("CRIT CHECK: chance=%d%% roll=%d -> CRITICAL x%.2f",
                        critChance / 100, roll, multiplier / 10000f);
            } else {
                debug("CRIT CHECK: chance=%d%% roll=%d -> NO CRIT",
                        critChance / 100, roll);
            }
        }
    }

    /**
     * Log a healing calculation result.
     *
     * @param baseHealing Base healing amount
     * @param effectiveness Healer effectiveness in bps
     * @param received Target healing received in bps
     * @param recoveryRate Target recovery rate in bps
     * @param finalHealing Final healing applied
     */
    public static void logHealCalc(float baseHealing, int effectiveness,
            int received, int recoveryRate, float finalHealing) {
        if (debugEnabled.get()) {
            debug("HEAL: base=%.2f eff=%d%% recv=%d%% recov=%d%% -> final=%.2f",
                    baseHealing, effectiveness / 100, received / 100,
                    recoveryRate / 100, finalHealing);
        }
    }
}
