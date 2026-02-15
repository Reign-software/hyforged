package reign.software.hyforged.options;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player runtime options for Hyforged features.
 * <p>
 * Stores user preferences (combat log visibility, damage numbers, etc.)
 * in memory. Options are not persisted across sessions — they reset to
 * defaults on reconnect.
 * <p>
 * Thread-safe: all access goes through a {@link ConcurrentHashMap}.
 */
public final class HyforgedPlayerOptions {

    /** Default state for all toggleable options. */
    public static final boolean DEFAULT_COMBAT_LOG_HUD = true;
    public static final boolean DEFAULT_COMBAT_TEXT = true;
    public static final boolean DEFAULT_XP_NOTIFICATIONS = true;
    public static final boolean DEFAULT_DAMAGE_NUMBERS = true;
    public static final boolean DEFAULT_DEBUG_MODE = false;
    public static final boolean DEFAULT_QUALITY_DEBUG = false;

    private static final ConcurrentHashMap<UUID, PlayerOptions> OPTIONS = new ConcurrentHashMap<>();

    private HyforgedPlayerOptions() {
    }

    /**
     * Get or create the options for a player.
     */
    @Nonnull
    public static PlayerOptions get(@Nonnull UUID playerUuid) {
        return OPTIONS.computeIfAbsent(playerUuid, k -> new PlayerOptions());
    }

    /**
     * Remove a player's options (call on disconnect).
     */
    public static void remove(@Nonnull UUID playerUuid) {
        OPTIONS.remove(playerUuid);
    }

    /**
     * Mutable options container for a single player.
     * All fields are volatile for safe cross-thread reads.
     */
    public static final class PlayerOptions {
        private volatile boolean combatLogHud = DEFAULT_COMBAT_LOG_HUD;
        private volatile boolean combatText = DEFAULT_COMBAT_TEXT;
        private volatile boolean xpNotifications = DEFAULT_XP_NOTIFICATIONS;
        private volatile boolean damageNumbers = DEFAULT_DAMAGE_NUMBERS;
        private volatile boolean debugMode = DEFAULT_DEBUG_MODE;
        private volatile boolean qualityDebug = DEFAULT_QUALITY_DEBUG;

        PlayerOptions() {
        }

        public boolean isCombatLogHud() {
            return combatLogHud;
        }

        public void setCombatLogHud(boolean value) {
            this.combatLogHud = value;
        }

        public boolean toggleCombatLogHud() {
            this.combatLogHud = !this.combatLogHud;
            return this.combatLogHud;
        }

        public boolean isCombatText() {
            return combatText;
        }

        public void setCombatText(boolean value) {
            this.combatText = value;
        }

        public boolean toggleCombatText() {
            this.combatText = !this.combatText;
            return this.combatText;
        }

        public boolean isXpNotifications() {
            return xpNotifications;
        }

        public void setXpNotifications(boolean value) {
            this.xpNotifications = value;
        }

        public boolean toggleXpNotifications() {
            this.xpNotifications = !this.xpNotifications;
            return this.xpNotifications;
        }

        public boolean isDamageNumbers() {
            return damageNumbers;
        }

        public void setDamageNumbers(boolean value) {
            this.damageNumbers = value;
        }

        public boolean toggleDamageNumbers() {
            this.damageNumbers = !this.damageNumbers;
            return this.damageNumbers;
        }

        public boolean isDebugMode() {
            return debugMode;
        }

        public void setDebugMode(boolean value) {
            this.debugMode = value;
        }

        public boolean toggleDebugMode() {
            this.debugMode = !this.debugMode;
            return this.debugMode;
        }

        public boolean isQualityDebug() {
            return qualityDebug;
        }

        public void setQualityDebug(boolean value) {
            this.qualityDebug = value;
        }

        public boolean toggleQualityDebug() {
            this.qualityDebug = !this.qualityDebug;
            return this.qualityDebug;
        }
    }
}
