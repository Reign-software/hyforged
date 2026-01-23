package reign.software.hyforged.affix.resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runtime config for affix tier color mapping.
 */
public final class AffixTierColorConfig {

    private static final Logger LOGGER = Logger.getLogger(AffixTierColorConfig.class.getName());
    private static final AffixTierColorConfig INSTANCE = new AffixTierColorConfig();

    private String defaultColor = "";
    private Map<Integer, String> tierColors = new HashMap<>();

    private AffixTierColorConfig() {
    }

    @Nonnull
    public static AffixTierColorConfig get() {
        return INSTANCE;
    }

    public void applyFromAsset(@Nonnull AffixTierColorConfigAsset asset) {
        defaultColor = sanitize(asset.getDefaultColor());
        tierColors = parseTierColors(asset.getTierColors());
    }

    public void applyOverrides(@Nonnull Map<Integer, String> colors, @Nullable String overrideDefault) {
        defaultColor = sanitize(overrideDefault);
        tierColors = new HashMap<>();
        for (Map.Entry<Integer, String> entry : colors.entrySet()) {
            if (entry.getKey() == null || entry.getKey() < 1) {
                continue;
            }
            String color = sanitize(entry.getValue());
            if (!color.isBlank()) {
                tierColors.put(entry.getKey(), color);
            }
        }
    }

    @Nullable
    public String getTierColor(int tier) {
        if (tier <= 0) {
            return normalize(defaultColor);
        }
        String color = tierColors.get(tier);
        if (color != null && !color.isBlank()) {
            return color;
        }
        return normalize(defaultColor);
    }

    @Nonnull
    private Map<Integer, String> parseTierColors(@Nonnull Map<String, String> rawColors) {
        Map<Integer, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : rawColors.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            try {
                int tier = Integer.parseInt(key.trim());
                if (tier < 1) {
                    continue;
                }
                String color = sanitize(entry.getValue());
                if (!color.isBlank()) {
                    result.put(tier, color);
                }
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.FINE, "Invalid affix tier color key: {0}", key);
            }
        }
        return result;
    }

    @Nonnull
    private static String sanitize(@Nullable String color) {
        return color != null ? color.trim() : "";
    }

    @Nullable
    private static String normalize(@Nonnull String color) {
        return color.isBlank() ? null : color;
    }
}
