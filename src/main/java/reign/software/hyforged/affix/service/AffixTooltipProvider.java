package reign.software.hyforged.affix.service;

import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.AffixTierDefinition;
import reign.software.hyforged.affix.model.AffixTierStat;
import reign.software.hyforged.affix.model.AffixType;
import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.affix.registry.AffixTypeRegistry;
import reign.software.hyforged.affix.resource.AffixTierColorConfig;
import reign.software.hyforged.quality.service.HyforgedQualityService;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for generating tooltip content for items with affixes.
 * <p>
 * Generates Path of Exile-style tooltip lines with tier, rolled value, and roll range.
 * The tooltip format for each affix line is:
 * <pre>
 *   "[T{tier}] +{value} {statName} ({min}-{max})"
 * </pre>
 * <p>
 * This shows the player:
 * <ul>
 *   <li>The tier of the affix (T1 = best)</li>
 *   <li>The actual rolled value they received</li>
 *   <li>The possible roll range for that tier, so they know how good their roll is</li>
 * </ul>
 * <p>
 * Tier colors (for client-side rendering) are data-driven via
 * {@code Server/Hyforged/GameplayConfigs/AffixTierColors.json}.
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

    // ======= SECTION HEADERS =======
    /** Header for the affixes section */
    public static final String AFFIXES_SECTION_HEADER = "Affixes";
    /** Header for the forged affixes section */
    public static final String FORGED_SECTION_HEADER = "Forged Properties";
    /** Label for quality line */
    public static final String QUALITY_LABEL = "Quality";

    private AffixTooltipProvider() {
        // Utility class - no instantiation
    }

    /**
     * Get the color for a tier number.
     *
     * @param tier The tier number (1-5)
    * @return The hex color code for the tier, or null to use the default tooltip color
     */
    @Nullable
    public static String getTierColor(int tier) {
        return AffixTierColorConfig.get().getTierColor(tier);
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
     * <p>
     * Format: "[T{tier}] +{value} {statName} ({min}-{max})"
     * <p>
     * Shows the rolled value and the possible range for that tier so players know
     * how good their roll is.
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

        // Get the tier definition to access roll ranges
        AffixTierDefinition tierDef = definition.getTier(affix.tier()).orElse(null);

        String color = getTierColor(affix.tier());
        List<TooltipLine> lines = new ArrayList<>();
        
        for (Map.Entry<String, RolledAffix.RolledStat> entry : affix.rolledStats().entrySet()) {
            String statIdStr = entry.getKey();
            RolledAffix.RolledStat rolledStat = entry.getValue();
            
            // Get stat display name
            String statName = getStatDisplayName(statIdStr, statRegistry);
            
            // Get roll range from tier definition
            int minValue = 0;
            int maxValue = 0;
            if (tierDef != null) {
                AffixTierStat tierStat = tierDef.getStat(statIdStr);
                if (tierStat != null) {
                    minValue = tierStat.minValue();
                    maxValue = tierStat.maxValue();
                }
            }
            
            String text = formatAffixLine(
                affix.tier(),
                rolledStat.value(),
                rolledStat.stackType(),
                statName,
                minValue,
                maxValue
            );
            
            if (color != null && !color.isBlank()) {
                lines.add(TooltipLine.content(text, color));
            } else {
                lines.add(TooltipLine.content(text));
            }
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
     * Format an affix line in PoE style.
     * <p>
     * Format: "[T{tier}] +{value} {statName} ({min}-{max})"
     * <p>
     * Examples:
     * <ul>
     *   <li>[T1] +52 Armor (45-55)</li>
     *   <li>[T2] +8% Increased Physical Damage (5-10)</li>
     * </ul>
     *
     * @param tier         The affix tier (1 = best)
     * @param value        The rolled value
     * @param modifierType The type of modifier (FLAT, INCREASED, MORE)
     * @param statName     The display name of the affected stat
     * @param minValue     The minimum possible roll for this tier
     * @param maxValue     The maximum possible roll for this tier
     * @return The formatted line
     */
    @Nonnull
    public static String formatAffixLine(
        int tier,
        int value,
        @Nonnull HyforgedModifier.StackType modifierType,
        @Nonnull String statName,
        int minValue,
        int maxValue
    ) {
        StringBuilder sb = new StringBuilder();

        // Tier label
        sb.append("[").append(getTierLabel(tier)).append("] ");

        // Value formatting
        String valueStr = formatValue(value, modifierType);
        sb.append(valueStr).append(" ");

        // Stat name
        sb.append(statName);

        // Roll range (only show if there's a range, i.e., min != max)
        if (minValue != maxValue) {
            String rangeStr = formatRange(minValue, maxValue, modifierType);
            sb.append(" ").append(rangeStr);
        }

        return sb.toString();
    }

    /**
     * Format a roll range based on modifier type.
     *
     * @param minValue     The minimum value
     * @param maxValue     The maximum value
     * @param modifierType The modifier type
     * @return Formatted range string (e.g., "(35-50)", "(5%-10%)")
     */
    @Nonnull
    public static String formatRange(int minValue, int maxValue, @Nonnull HyforgedModifier.StackType modifierType) {
        return switch (modifierType) {
            case FLAT -> "(" + minValue + "-" + maxValue + ")";
            case INCREASED, MORE -> {
                // Percentage values stored as basis points
                double minPct = minValue / 100.0;
                double maxPct = maxValue / 100.0;
                String minStr = (minPct == (int) minPct) ? String.valueOf((int) minPct) : String.format("%.1f", minPct);
                String maxStr = (maxPct == (int) maxPct) ? String.valueOf((int) maxPct) : String.format("%.1f", maxPct);
                yield "(" + minStr + "%-" + maxStr + "%)";
            }
            case CAP -> "(" + minValue + "-" + maxValue + ")";
        };
    }

    /**
     * Format an affix line (legacy method for backwards compatibility).
     *
     * @param tier         The affix tier (1-5)
     * @param affixName    The display name of the affix (no longer used in PoE style)
     * @param value        The rolled value
     * @param modifierType The type of modifier (FLAT, INCREASED, MORE)
     * @param statName     The display name of the affected stat
     * @return The formatted line
     * @deprecated Use {@link #formatAffixLine(int, int, HyforgedModifier.StackType, String, int, int)} instead
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    @Nonnull
    public static String formatAffixLine(
        int tier,
        @Nonnull String affixName,
        int value,
        @Nonnull HyforgedModifier.StackType modifierType,
        @Nonnull String statName
    ) {
        // Legacy format without range
        return formatAffixLine(tier, value, modifierType, statName, value, value);
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
        @Nonnull ItemStack itemStack
    ) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        List<TooltipLine> lines = generateTooltipLines(itemStack);
        if (lines.isEmpty()) {
            return "";
        }
        List<String> textLines = lines.stream()
                .map(TooltipLine::text)
                .toList();
        return String.join("\n", textLines);
    }

    /**
     * Generate tooltip lines for an ItemStack, including quality and affix sections.
     *
     * @param itemStack The item stack
     * @return List of tooltip lines (empty if none)
     */
    @Nonnull
    public static List<TooltipLine> generateTooltipLines(@Nonnull ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");

        List<TooltipLine> result = new ArrayList<>();
        result.addAll(buildQualityLines(itemStack));

        HyforgedItemData itemData = HyforgedItemDataService.read(itemStack);
        TooltipContent content = generateTooltip(itemData);
        result.addAll(content.getAllLines());

        return result;
    }

    @Nonnull
    private static List<TooltipLine> buildQualityLines(@Nonnull ItemStack itemStack) {
        String qualityId = HyforgedQualityService.getEffectiveQuality(itemStack);
        if (qualityId == null || qualityId.isBlank()) {
            return List.of();
        }

        ItemQuality quality = ItemQuality.getAssetMap().getAsset(qualityId);
        String text = QUALITY_LABEL + ": " + qualityId;
        if (quality != null && quality.getTextColor() != null) {
            return List.of(TooltipLine.content(text, toHexColor(quality.getTextColor())));
        }

        return List.of(TooltipLine.content(text));
    }

    @Nonnull
    private static String toHexColor(@Nonnull Color color) {
        int r = Byte.toUnsignedInt(color.red);
        int g = Byte.toUnsignedInt(color.green);
        int b = Byte.toUnsignedInt(color.blue);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
