package reign.software.hyforged.passive.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a text label on the passive tree canvas.
 * <p>
 * Text labels are static text elements placed at specific positions
 * in the tree for region headers, decorative text, or informational labels.
 *
 * @param text The text content to display
 * @param x X coordinate in tree space
 * @param y Y coordinate in tree space
 * @param fontSize Font size in pixels
 * @param color Text color (hex format, e.g., "#FFCC00")
 * @param anchor Text anchor/alignment ("left", "center", "right")
 * @param region Optional region grouping
 * @param fontWeight Font weight ("normal", "bold")
 * @param opacity Opacity (0.0 to 1.0)
 * @param rotation Rotation in degrees
 */
public record TextLabel(
    @Nonnull String text,
    int x,
    int y,
    int fontSize,
    @Nonnull String color,
    @Nonnull String anchor,
    @Nullable String region,
    @Nonnull String fontWeight,
    float opacity,
    float rotation
) {
    
    /**
     * Create a simple text label with defaults.
     */
    public static TextLabel simple(@Nonnull String text, int x, int y) {
        return new TextLabel(text, x, y, 14, "#FFFFFF", "center", null, "normal", 1.0f, 0.0f);
    }
    
    /**
     * Create a text label with custom color and size.
     */
    public static TextLabel styled(@Nonnull String text, int x, int y, int fontSize, @Nonnull String color) {
        return new TextLabel(text, x, y, fontSize, color, "center", null, "normal", 1.0f, 0.0f);
    }
    
    /**
     * Create a region header label.
     */
    public static TextLabel regionHeader(@Nonnull String text, int x, int y, @Nonnull String region, @Nonnull String color) {
        return new TextLabel(text, x, y, 16, color, "center", region, "bold", 1.0f, 0.0f);
    }
}
