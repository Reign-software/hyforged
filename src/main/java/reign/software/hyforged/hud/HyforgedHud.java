package reign.software.hyforged.hud;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * Composite HUD that contains all Hyforged HUD sections in a single
 * {@link CustomUIHud} instance.
 * <p>
 * Hytale only supports one CustomUIHud per player. Instead of depending
 * on a third-party wrapper library, this class owns a single .ui file
 * with all sections and exposes typed update methods for each section.
 * Individual sections are toggled visible/invisible server-side.
 */
public class HyforgedHud extends CustomUIHud {

    /** Path to the composite .ui file (relative to Common/UI/Custom/) */
    public static final String UI_PATH = "Hyforged/HyforgedHud.ui";

    public HyforgedHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append(UI_PATH);
    }

    // ── Resource Stats Section ──────────────────────────────────────

    /**
     * Update the resource stats section (Concentration and Rage bars).
     */
    public void updateResourceStats(
            boolean showConcentration,
            int concentrationCurrent,
            int concentrationMax,
            boolean showRage,
            int rageCurrent,
            int rageMax
    ) {
        UICommandBuilder b = new UICommandBuilder();
        b.set("#ResourceStats.Visible", true);
        b.set("#ConcentrationContainer.Visible", showConcentration);
        b.set("#RageContainer.Visible", showRage);
        b.set("#ConcentrationValue.Text", concentrationCurrent + "/" + concentrationMax);
        b.set("#RageValue.Text", rageCurrent + "/" + rageMax);
        b.set("#ConcentrationFill.Value", computeFillRatio(concentrationCurrent, concentrationMax));
        b.set("#RageFill.Value", computeFillRatio(rageCurrent, rageMax));
        update(false, b);
    }

    /**
     * Hide the resource stats section entirely.
     */
    public void hideResourceStats() {
        UICommandBuilder b = new UICommandBuilder();
        b.set("#ResourceStats.Visible", false);
        update(false, b);
    }

    // ── Currency Section ────────────────────────────────────────────

    /**
     * Update the currency section (Tradebar balance).
     */
    public void updateCurrency(int inventoryBalance, int vaultBalance, boolean hasVault) {
        UICommandBuilder b = new UICommandBuilder();
        b.set("#Currency.Visible", true);
        b.set("#InventoryBalance.Text", formatNumber(inventoryBalance));
        b.set("#VaultSection.Visible", hasVault);
        if (hasVault) {
            b.set("#VaultBalance.Text", formatNumber(vaultBalance));
            int total = inventoryBalance + vaultBalance;
            b.set("#TotalSection.Visible", true);
            b.set("#TotalBalance.Text", formatNumber(total));
        } else {
            b.set("#TotalSection.Visible", false);
        }
        update(false, b);
    }

    /**
     * Hide the currency section entirely.
     */
    public void hideCurrency() {
        UICommandBuilder b = new UICommandBuilder();
        b.set("#Currency.Visible", false);
        update(false, b);
    }

    // ── Combat Log Section ──────────────────────────────────────────

    /** Maximum number of log entries in the .ui file */
    public static final int MAX_LOG_ENTRIES = 12;

    /**
     * Update the combat log section with rich-text event lines and footer stats.
     *
     * @param lines  Array of pre-formatted Message objects (index 0 = topmost entry).
     *               Unused slots are cleared.
     * @param dps    Current DPS text (e.g. "DPS: 123.4")
     * @param hits   Hits text (e.g. "Hits: 5")
     * @param crits  Crits text (e.g. "Crits: 2")
     */
    public void updateCombatLog(@Nonnull Message[] lines, @Nonnull String dps,
                                @Nonnull String hits, @Nonnull String crits) {
        UICommandBuilder b = new UICommandBuilder();
        b.set("#CombatLog.Visible", true);
        for (int i = 0; i < MAX_LOG_ENTRIES; i++) {
            if (i < lines.length) {
                b.set("#LogEntry" + i + ".TextSpans", lines[i]);
            } else {
                b.set("#LogEntry" + i + ".Text", "");
            }
        }
        b.set("#DpsLabel.Text", dps);
        b.set("#HitsLabel.Text", hits);
        b.set("#CritsLabel.Text", crits);
        update(false, b);
    }

    /**
     * Hide the combat log section entirely.
     */
    public void hideCombatLog() {
        UICommandBuilder b = new UICommandBuilder();
        b.set("#CombatLog.Visible", false);
        update(false, b);
    }

    // ── Utilities ───────────────────────────────────────────────────

    private static float computeFillRatio(int current, int max) {
        if (max <= 0) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, (float) current / (float) max));
    }

    @Nonnull
    private static String formatNumber(int value) {
        return String.format("%,d", value);
    }
}
