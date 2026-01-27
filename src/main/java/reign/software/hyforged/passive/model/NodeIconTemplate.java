package reign.software.hyforged.passive.model;

import javax.annotation.Nonnull;

/**
 * Template defining the visual appearance of a node icon.
 * <p>
 * Centralized definition allows consistent icon styling and scaling
 * across nodes that share the same icon.
 *
 * @param id Unique identifier (e.g., "hyforged:icon-health")
 * @param texture Texture path for the icon
 * @param scale Scale factor relative to node size (0.0-1.0, typically 0.6-0.8)
 */
public record NodeIconTemplate(
    @Nonnull String id,
    @Nonnull String texture,
    float scale
) {
    
    public NodeIconTemplate {
        if (scale <= 0 || scale > 1.0f) {
            scale = 0.8f; // Default to 80%
        }
    }
    
    /**
     * Calculate the icon size based on the node size.
     *
     * @param nodeSize The node frame size in pixels
     * @return The icon size in pixels
     */
    public int getIconSize(int nodeSize) {
        return (int) (nodeSize * scale);
    }
    
    /**
     * Default scale for icons.
     */
    public static final float DEFAULT_SCALE = 0.8f;
}
