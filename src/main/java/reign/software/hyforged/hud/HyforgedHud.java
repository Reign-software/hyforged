package reign.software.hyforged.hud;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import reign.software.hyforged.affix.service.AffixTooltipProvider;
import reign.software.hyforged.concentration.ConcentrationBreakpoint;

import javax.annotation.Nonnull;
import java.util.List;

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

    // ── Concentration Breakpoints & Regen Rate ──────────────────────

    /** Reference width (px) for computing segment bar widths */
    private static final int SEGMENT_BAR_WIDTH = 200;

    /** Selector for the container that holds dynamically appended segment bars */
    private static final String SEGMENTS_CONTAINER = "#ConcentrationSegments";

    /** Active ability bar color (cyan-blue) */
    private static final String SEGMENT_COLOR_ACTIVE = "#4488CC";

    /** Disabled ability bar color (dimmed dark blue) */
    private static final String SEGMENT_COLOR_DISABLED = "#333344";

    /**
     * Update the concentration breakpoint segments and regen rate display.
     * <p>
     * Each breakpoint is rendered as a colored bar segment whose width is
     * proportional to the ability's concentration cost relative to max.
     * Enabled segments use an active color; disabled use a dimmed color.
     *
     * @param breakpoints      Breakpoints ordered by priority (highest first)
     * @param maxConcentration The entity's maximum concentration
     * @param regenPerSecond   The effective concentration regen rate per second
     */
    public void updateConcentrationBreakpoints(
            @Nonnull List<ConcentrationBreakpoint> breakpoints,
            int maxConcentration,
            float regenPerSecond
    ) {
        UICommandBuilder b = new UICommandBuilder();

        // Clear existing segments and rebuild
        b.clear(SEGMENTS_CONTAINER);

        if (!breakpoints.isEmpty() && maxConcentration > 0) {
            b.set(SEGMENTS_CONTAINER + ".Visible", true);

            for (ConcentrationBreakpoint bp : breakpoints) {
                int widthPx = Math.max(2, Math.round((float) bp.cost() / maxConcentration * SEGMENT_BAR_WIDTH));
                String color = bp.enabled() ? SEGMENT_COLOR_ACTIVE : SEGMENT_COLOR_DISABLED;

                String segmentUI = String.format(
                        "Group { Anchor: (Width: %d, Height: 6); Background: (Color: %s); Margin: (Right: 1); }",
                        widthPx, color
                );
                b.appendInline(SEGMENTS_CONTAINER, segmentUI);
            }
        } else {
            b.set(SEGMENTS_CONTAINER + ".Visible", false);
        }

        // Regen rate display
        if (regenPerSecond > 0.01f) {
            b.set("#ConcentrationRegenRate.Visible", true);
            b.set("#ConcentrationRegenRate.Text", String.format("+%.1f/s", regenPerSecond));
        } else {
            b.set("#ConcentrationRegenRate.Visible", false);
        }

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

    // ── Progression Section ──────────────────────────────────────────

    /**
     * Update the progression section (character level, class, XP bar).
     *
     * @param charLevel       Current character level
     * @param className       Display name of the active class (empty string if none)
     * @param classLevel      Level in the active class
     * @param xpProgress      XP earned toward the next character level
     * @param xpToNext        Total XP required for the next character level
     * @param classXpProgress XP earned toward the next class level
     * @param classXpToNext   Total XP required for the next class level
     */
    public void updateProgression(int charLevel,
                                  @Nonnull String className,
                                  int classLevel,
                                  long xpProgress,
                                  long xpToNext,
                                  long classXpProgress,
                                  long classXpToNext) {
        UICommandBuilder b = new UICommandBuilder();
        b.set("#Progression.Visible", true);

        // "Lv 5"
        b.set("#CharLevelValue.Text", "Lv " + charLevel);

        // Class name with level: "Warrior Lv 1" or empty
        if (className != null && !className.isEmpty() && classLevel > 0) {
            b.set("#ClassName.Text", className + " Lv " + classLevel);
        } else {
            b.set("#ClassName.Text", "");
        }

        // Character XP bar + overlaid text
        b.set("#XpText.Text", formatXp(xpProgress) + " / " + formatXp(xpToNext));
        b.set("#ExpBarFill.Value", computeFillRatio(xpProgress, xpToNext));

        // Class XP bar (visible only when a class is active)
        boolean hasClass = className != null && !className.isEmpty() && classLevel > 0;
        b.set("#ClassXpBarContainer.Visible", hasClass);
        if (hasClass) {
            b.set("#ClassXpText.Text", formatXp(classXpProgress) + " / " + formatXp(classXpToNext));
            b.set("#ClassExpBarFill.Value", computeFillRatio(classXpProgress, classXpToNext));
        }

        update(false, b);
    }

    /**
     * Hide the progression section entirely.
     */
    public void hideProgression() {
        UICommandBuilder b = new UICommandBuilder();
        b.set("#Progression.Visible", false);
        update(false, b);
    }

    // ── Item Affix Display Section ─────────────────────────────────

    /** Selector for the container that holds dynamically appended affix lines */
    private static final String AFFIX_LINES_CONTAINER = "#AffixLinesContainer";

    /**
     * Update the item affix display section with the held item's affixes.
     * <p>
     * Clears the lines container and appends inline Labels for each section
     * header and affix line. Only creates exactly as many elements as needed,
     * driven entirely by the data-driven affix types and their tooltip content.
     *
     * @param itemName       Display name of the item
     * @param tooltipContent The structured tooltip content from AffixTooltipProvider
     */
    /** Approximate heights for computing the tooltip box size dynamically */
    private static final int ITEM_NAME_HEIGHT = 24;      // header label + pad
    private static final int SEPARATOR_HEIGHT = 4;        // 1px line + spacing
    private static final int SECTION_HEADER_HEIGHT = 22;  // section name row
    private static final int AFFIX_LINE_HEIGHT = 19;      // single stat line
    private static final int VERTICAL_PADDING = 16;       // top + bottom pad

    public void updateItemAffixes(
            @Nonnull String itemName,
            @Nonnull AffixTooltipProvider.TooltipContent tooltipContent
    ) {
        UICommandBuilder b = new UICommandBuilder();
        b.set("#AffixItemName.Text", itemName);

        // Clear previous lines and rebuild from current affix data
        b.clear(AFFIX_LINES_CONTAINER);

        int totalLines = 0;
        int totalHeaders = 0;

        for (AffixTooltipProvider.TooltipSection section : tooltipContent.sections()) {
            if (section.lines().isEmpty()) {
                continue;
            }

            totalHeaders++;

            // Section header — bold, section color
            String headerUI = String.format(
                "Label { Padding: (Top: 3); Text: \"%s\"; Style: (FontSize: 16, RenderBold: true, TextColor: %s); }",
                escapeUI(section.sectionName()), section.hudColor()
            );
            b.appendInline(AFFIX_LINES_CONTAINER, headerUI);

            // Content lines — each affix stat line
            for (AffixTooltipProvider.TooltipLine line : section.lines()) {
                String color = (line.color() != null && !line.color().isBlank())
                        ? line.color()
                        : section.hudColor();
                String lineUI = String.format(
                    "Label { Text: \"%s\"; Style: (FontSize: 14, TextColor: %s, Wrap: true); }",
                    escapeUI(line.text()), color
                );
                b.appendInline(AFFIX_LINES_CONTAINER, lineUI);
                totalLines++;
            }
        }

        // Compute a tight height for the tooltip box so it only wraps the content
        int height = VERTICAL_PADDING + ITEM_NAME_HEIGHT + SEPARATOR_HEIGHT
                + (totalHeaders * SECTION_HEADER_HEIGHT)
                + (totalLines * AFFIX_LINE_HEIGHT);
        Anchor anchor = new Anchor();
        anchor.setBottom(Value.of(224));
        anchor.setWidth(Value.of(702));
        anchor.setHeight(Value.of(height));
        b.setObject("#ItemAffixes.Anchor", anchor);
        b.set("#ItemAffixes.Visible", true);

        update(false, b);
    }

    /**
     * Hide the item affix section entirely and clear dynamically appended lines.
     */
    public void hideItemAffixes() {
        UICommandBuilder b = new UICommandBuilder();
        b.set("#ItemAffixes.Visible", false);
        b.clear(AFFIX_LINES_CONTAINER);
        update(false, b);
    }

    // ── Utilities ───────────────────────────────────────────────────

    private static float computeFillRatio(long current, long max) {
        if (max <= 0) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, (float) current / (float) max));
    }

    private static float computeFillRatio(int current, int max) {
        if (max <= 0) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, (float) current / (float) max));
    }

    @Nonnull
    private static String formatNumber(int value) {
        return String.format("%,d", value);
    }

    @Nonnull
    private static String formatXp(long value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        } else if (value >= 10_000) {
            return String.format("%.1fK", value / 1_000.0);
        }
        return String.format("%,d", value);
    }

    /**
     * Escape a string for safe embedding inside an inline .ui {@code Text: "...";} attribute.
     * Replaces backslashes and double-quotes so they don't break the markup parser.
     */
    @Nonnull
    private static String escapeUI(@Nonnull String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
