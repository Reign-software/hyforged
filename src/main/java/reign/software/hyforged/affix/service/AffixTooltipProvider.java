package reign.software.hyforged.affix.service;

import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.AffixType;
import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.affix.registry.AffixTypeRegistry;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for generating tooltip content for items with affixes.
 * <p>
 * Generates structured tooltip lines with tier coloring and stat information.
 * The tooltip format for each affix line is:
 * <pre>
 *   "[T{tier}] {affixName}: +{value} {statName}"
 * </pre>
 * <p>
 * Tier colors (for client-side rendering):
 * <ul>
 *   <li>T1 (Legendary): Gold (#FFD700)</li>
 *   <li>T2 (Epic): Purple (#9932CC)</li>
 *   <li>T3 (Rare): Blue (#4169E1)</li>
 *   <li>T4 (Uncommon): Green (#32CD32)</li>
 *   <li>T5 (Common): White (#FFFFFF)</li>
 * </ul>
 * <p>
 * Forged affixes are displayed in a separate section with a different header.
 * <p>
 * <b>Integration Note:</b> Hytale tooltips are rendered client-side using translation
 * keys from {@code ItemTranslationProperties}. This class generates formatted strings
 * that can be stored in item metadata for client-side UI scripts to read and display.
 * Full tooltip integration requires client-side modding.
 *
 * @see AffixNameGenerator for display name generation
 */
public final class AffixTooltipProvider {

    private static final Logger LOGGER = Logger.getLogger(AffixTooltipProvider.class.getName());

    // ======= TIER COLORS (HEX) =======
    /** T1 (Legendary): Gold */
    public static final String TIER_1_COLOR = "#FFD700";
    /** T2 (Epic): Purple */
    public static final String TIER_2_COLOR = "#9932CC";
    /** T3 (Rare): Blue */
    public static final String TIER_3_COLOR = "#4169E1";
    /** T4 (Uncommon): Green */
    public static final String TIER_4_COLOR = "#32CD32";
    /** T5 (Common): White */
    public static final String TIER_5_COLOR = "#FFFFFF";

    // ======= SECTION HEADERS =======
    /** Header for the affixes section */
    public static final String AFFIXES_SECTION_HEADER = "Affixes";
    /** Header for the forged affixes section */
    public static final String FORGED_SECTION_HEADER = "Forged Properties";

    private AffixTooltipProvider() {
        // Utility class - no instantiation
    }

    /**
     * Get the color for a tier number.
     *
     * @param tier The tier number (1-5)
     * @return The hex color code for the tier
     */
    @Nonnull
    public static String getTierColor(int tier) {
        return switch (tier) {
            case 1 -> TIER_1_COLOR;
            case 2 -> TIER_2_COLOR;
            case 3 -> TIER_3_COLOR;
            case 4 -> TIER_4_COLOR;
            default -> TIER_5_COLOR; // T5+ all use white
        };
    }

    /**
     * Get the tier label (e.g., "T1", "T2").
     *
     * @param tier The tier number
     * @return The formatted tier label
     */
    @Nonnull
    public static String getTierLabel(int tier) {
        return "T" + tier;
    }

    /**
     * Represents a single tooltip line with optional color information.
     *
     * @param text      The text content of the line
     * @param color     The hex color for the line (nullable for default color)
     * @param isHeader  True if this is a section header
     */
    public record TooltipLine(
        @Nonnull String text,
        String color,
        boolean isHeader
    ) {
        public TooltipLine {
            Objects.requireNonNull(text, "text cannot be null");
        }

        /**
         * Create a header line.
         */
        @Nonnull
        public static TooltipLine header(@Nonnull String text) {
            return new TooltipLine(text, null, true);
        }

        /**
         * Create a content line with color.
         */
        @Nonnull
        public static TooltipLine content(@Nonnull String text, @Nonnull String color) {
            return new TooltipLine(text, color, false);
        }

        /**
         * Create a content line without explicit color.
         */
        @Nonnull
        public static TooltipLine content(@Nonnull String text) {
            return new TooltipLine(text, null, false);
        }
    }

    /**
     * Represents the complete tooltip content for an item's affixes.
     *
     * @param regularAffixes Lines for regular affixes (prefix/suffix)
     * @param forgedAffixes  Lines for forged affixes
     */
    public record TooltipContent(
        @Nonnull List<TooltipLine> regularAffixes,
        @Nonnull List<TooltipLine> forgedAffixes
    ) {
        public TooltipContent {
            regularAffixes = List.copyOf(regularAffixes);
            forgedAffixes = List.copyOf(forgedAffixes);
        }

        /**
         * Check if there is any tooltip content.
         */
        public boolean hasContent() {
            return !regularAffixes.isEmpty() || !forgedAffixes.isEmpty();
        }

        /**
         * Check if there are regular (non-forged) affixes.
         */
        public boolean hasRegularAffixes() {
            return !regularAffixes.isEmpty();
        }

        /**
         * Check if there are forged affixes.
         */
        public boolean hasForgedAffixes() {
            return !forgedAffixes.isEmpty();
        }

        /**
         * Get all lines combined (regular affixes section, then forged section).
         *
         * @return Combined list of all tooltip lines with section headers
         */
        @Nonnull
        public List<TooltipLine> getAllLines() {
            List<TooltipLine> result = new ArrayList<>();

            if (!regularAffixes.isEmpty()) {
                result.add(TooltipLine.header(AFFIXES_SECTION_HEADER));
                result.addAll(regularAffixes);
            }

            if (!forgedAffixes.isEmpty()) {
                result.add(TooltipLine.header(FORGED_SECTION_HEADER));
                result.addAll(forgedAffixes);
            }

            return result;
        }

        /**
         * Get all lines as plain text (no color formatting).
         *
         * @return List of plain text strings
         */
        @Nonnull
        public List<String> toPlainText() {
            return getAllLines().stream()
                .map(TooltipLine::text)
                .toList();
        }

        /**
         * Empty tooltip content.
         */
        public static final TooltipContent EMPTY = new TooltipContent(List.of(), List.of());
    }

    /**
     * Generate tooltip content for an item's affixes.
     *
     * @param itemData The Hyforged item data containing affixes
     * @return The structured tooltip content
     */
    @Nonnull
    public static TooltipContent generateTooltip(@Nonnull HyforgedItemData itemData) {
        Objects.requireNonNull(itemData, "itemData cannot be null");

        if (!itemData.hasAffixes()) {
            return TooltipContent.EMPTY;
        }

        return generateTooltip(itemData.affixes());
    }

    /**
     * Generate tooltip content from a list of affixes.
     *
     * @param affixes The list of rolled affixes
     * @return The structured tooltip content
     */
    @Nonnull
    public static TooltipContent generateTooltip(@Nonnull List<RolledAffix> affixes) {
        Objects.requireNonNull(affixes, "affixes cannot be null");

        if (affixes.isEmpty()) {
            return TooltipContent.EMPTY;
        }

        AffixDefinitionRegistry affixRegistry = AffixDefinitionRegistry.get();
        AffixTypeRegistry typeRegistry = AffixTypeRegistry.get();
        StatDefinitionRegistry statRegistry = StatDefinitionRegistry.get();

        List<TooltipLine> regularLines = new ArrayList<>();
        List<TooltipLine> forgedLines = new ArrayList<>();

        for (RolledAffix affix : affixes) {
            List<TooltipLine> lines = generateAffixLines(affix, affixRegistry, typeRegistry, statRegistry);
            if (lines.isEmpty()) {
                continue;
            }

            // Determine if this is a forged affix
            AffixDefinition definition = affixRegistry.get(affix.affixId());
            if (definition != null) {
                AffixType type = typeRegistry.get(definition.type());
                if (type != null && type.displayNamePosition() == AffixType.DisplayNamePosition.NONE) {
                    forgedLines.addAll(lines);
                } else {
                    regularLines.addAll(lines);
                }
            } else {
                regularLines.addAll(lines); // Default to regular if unknown
            }
        }

        return new TooltipContent(regularLines, forgedLines);
    }

    /**
     * Generate tooltip lines for an affix (may be multiple lines for multi-stat affixes).
     *
     * @param affix          The rolled affix
     * @param affixRegistry  The affix definition registry
     * @param typeRegistry   The affix type registry
     * @param statRegistry   The stat definition registry
     * @return List of tooltip lines, empty if the affix cannot be resolved
     */
    private static List<TooltipLine> generateAffixLines(
        @Nonnull RolledAffix affix,
        @Nonnull AffixDefinitionRegistry affixRegistry,
        @Nonnull AffixTypeRegistry typeRegistry,
        @Nonnull StatDefinitionRegistry statRegistry
    ) {
        // Look up affix definition
        AffixDefinition definition = affixRegistry.get(affix.affixId());
        if (definition == null) {
            LOGGER.log(Level.WARNING, "Unknown affix for tooltip: {0}", affix.affixId());
            return List.of();
        }

        String color = getTierColor(affix.tier());
        List<TooltipLine> lines = new ArrayList<>();
        
        // First line includes the affix name
        boolean firstStat = true;
        for (Map.Entry<String, RolledAffix.RolledStat> entry : affix.rolledStats().entrySet()) {
            String statIdStr = entry.getKey();
            RolledAffix.RolledStat rolledStat = entry.getValue();
            
            // Get stat display name
            String statName = getStatDisplayName(statIdStr, statRegistry);
            
            String text;
            if (firstStat) {
                // First line includes tier and affix name
                text = formatAffixLine(
                    affix.tier(),
                    definition.displayName(),
                    rolledStat.value(),
                    rolledStat.stackType(),
                    statName
                );
                firstStat = false;
            } else {
                // Subsequent lines just show the stat bonus (indented)
                text = formatStatLine(
                    rolledStat.value(),
                    rolledStat.stackType(),
                    statName
                );
            }
            
            lines.add(TooltipLine.content(text, color));
        }
        
        return lines;
    }

    /**
     * Get the display name for a stat by ID.
     */
    @Nonnull
    private static String getStatDisplayName(
        @Nonnull String statIdStr,
        @Nonnull StatDefinitionRegistry statRegistry
    ) {
        StatId statId = StatId.parse(statIdStr);
        StatDefinition stat = statRegistry.getStat(statId);
        if (stat != null) {
            return stat.displayName();
        }
        // Fallback to stat ID if not found
        return statIdStr;
    }
    
    /**
     * Format an additional stat line (for multi-stat affixes after the first).
     */
    @Nonnull
    private static String formatStatLine(
        int value,
        @Nonnull HyforgedModifier.StackType modifierType,
        @Nonnull String statName
    ) {
        String sign = value >= 0 ? "+" : "";
        String suffix = (modifierType == HyforgedModifier.StackType.INCREASED 
                      || modifierType == HyforgedModifier.StackType.MORE) ? "%" : "";
        return String.format("       %s%d%s %s", sign, value, suffix, statName);
    }

    /**
     * Format an affix line.
     * <p>
     * Format: "[T{tier}] {affixName}: +{value} {statName}"
     * For percentage modifiers: "[T{tier}] {affixName}: +{value}% {statName}"
     *
     * @param tier         The affix tier (1-5)
     * @param affixName    The display name of the affix
     * @param value        The rolled value (stored as int, divide by 100 for percentages)
     * @param modifierType The type of modifier (FLAT, INCREASED, MORE)
     * @param statName     The display name of the affected stat
     * @return The formatted line
     */
    @Nonnull
    public static String formatAffixLine(
        int tier,
        @Nonnull String affixName,
        int value,
        @Nonnull HyforgedModifier.StackType modifierType,
        @Nonnull String statName
    ) {
        StringBuilder sb = new StringBuilder();

        // Tier label
        sb.append("[").append(getTierLabel(tier)).append("] ");

        // Affix name
        sb.append(affixName).append(": ");

        // Value formatting
        String valueStr = formatValue(value, modifierType);
        sb.append(valueStr).append(" ");

        // Stat name
        sb.append(statName);

        return sb.toString();
    }

    /**
     * Format a value based on modifier type.
     *
     * @param value        The raw value (int representation)
     * @param modifierType The modifier type
     * @return Formatted value string (e.g., "+50", "+10%")
     */
    @Nonnull
    public static String formatValue(int value, @Nonnull HyforgedModifier.StackType modifierType) {
        String sign = value >= 0 ? "+" : "";

        return switch (modifierType) {
            case FLAT -> sign + value;
            case INCREASED, MORE -> {
                // Percentage values are stored as basis points (value * 100)
                // So 10% = 1000, we need to divide by 100 to get 10
                double percentage = value / 100.0;
                if (percentage == (int) percentage) {
                    yield sign + (int) percentage + "%";
                } else {
                    yield sign + String.format("%.1f", percentage) + "%";
                }
            }
            case CAP -> {
                // CAP values: positive = max cap, negative = min cap
                if (value >= 0) {
                    yield "max " + value;
                } else {
                    yield "min " + (-value);
                }
            }
        };
    }

    /**
     * Generate a simple text summary of affixes for debugging or simple display.
     *
     * @param itemData The item data
     * @return A newline-separated string of all affix lines
     */
    @Nonnull
    public static String generateTextSummary(@Nonnull HyforgedItemData itemData) {
        TooltipContent content = generateTooltip(itemData);
        if (!content.hasContent()) {
            return "";
        }
        return String.join("\n", content.toPlainText());
    }

    /**
     * Generate a simple text summary from an ItemStack.
     *
     * @param itemStack The item stack
     * @return A newline-separated string of all affix lines
     */
    @Nonnull
    public static String generateTextSummary(
        @Nonnull com.hypixel.hytale.server.core.inventory.ItemStack itemStack
    ) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        HyforgedItemData itemData = HyforgedItemDataService.read(itemStack);
        return generateTextSummary(itemData);
    }
}
