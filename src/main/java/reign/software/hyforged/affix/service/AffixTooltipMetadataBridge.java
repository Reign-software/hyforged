package reign.software.hyforged.affix.service;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import reign.software.hyforged.affix.model.HyforgedItemData;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * Writes Hyforged affix tooltip payloads into item metadata.
 * <p>
 * This is a server-side bridge for client tooltip systems to consume without
 * re-rolling or re-formatting affix data.
 */
public final class AffixTooltipMetadataBridge {

    /** Metadata key containing tooltip lines as a string array. */
    public static final String TOOLTIP_LINES_KEY = "Hyforged.AffixTooltipLines";

    /** Metadata key containing tooltip lines joined by newlines. */
    public static final String TOOLTIP_SUMMARY_KEY = "Hyforged.AffixTooltipSummary";

    private AffixTooltipMetadataBridge() {
    }

    /**
     * Write tooltip bridge metadata for an item's current Hyforged affix data.
     */
    @Nonnull
    public static ItemStack writeFromItemData(@Nonnull ItemStack itemStack, @Nonnull HyforgedItemData itemData) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        Objects.requireNonNull(itemData, "itemData cannot be null");

        if (!itemData.hasAffixes()) {
            return clear(itemStack);
        }

        List<String> lines = AffixTooltipProvider.generateTooltip(itemData).toPlainText().stream()
                .filter(line -> line != null && !line.isBlank())
                .toList();

        if (lines.isEmpty()) {
            return clear(itemStack);
        }

        String[] lineArray = lines.toArray(String[]::new);
        ItemStack withLines = itemStack.withMetadata(TOOLTIP_LINES_KEY, Codec.STRING_ARRAY, lineArray);
        return withLines.withMetadata(TOOLTIP_SUMMARY_KEY, Codec.STRING, String.join("\n", lines));
    }

    /**
     * Remove all tooltip bridge metadata keys from an item.
     */
    @Nonnull
    public static ItemStack clear(@Nonnull ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");

        ItemStack noLines = itemStack.withMetadata(TOOLTIP_LINES_KEY, Codec.STRING_ARRAY, null);
        return noLines.withMetadata(TOOLTIP_SUMMARY_KEY, Codec.STRING, null);
    }
}