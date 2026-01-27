package reign.software.hyforged.passive.model;

import javax.annotation.Nonnull;

/**
 * Template defining the visual appearance of a node frame.
 * <p>
 * Centralized definition allows consistent styling across node types
 * and reduces duplication in individual node definitions.
 * <p>
 * Loaded from JSON in Server/&lt;Mod&gt;/PassiveTrees/templates/frame-templates.json
 *
 * @param id Unique identifier (e.g., "hyforged:frame-minor")
 * @param size Display size in pixels
 * @param allocatedTexture Texture path when node is allocated
 * @param availableTexture Texture path when node is available (reachable)
 * @param lockedTexture Texture path when node is locked (unreachable)
 */
public record NodeVisualTemplate(
    @Nonnull String id,
    int size,
    @Nonnull String allocatedTexture,
    @Nonnull String availableTexture,
    @Nonnull String lockedTexture
) {
    
    /**
     * Get the appropriate texture based on node state.
     *
     * @param allocated Whether the node is allocated
     * @param available Whether the node is available (reachable)
     * @return The texture path to use
     */
    @Nonnull
    public String getTexture(boolean allocated, boolean available) {
        if (allocated) return allocatedTexture;
        if (available) return availableTexture;
        return lockedTexture;
    }
    
    /**
     * Create a fallback template with placeholder texture.
     * Used when no template is found and no default is configured.
     */
    @Nonnull
    public static NodeVisualTemplate fallback(@Nonnull String nodeType) {
        int size = switch (nodeType.toLowerCase()) {
            case "notable" -> 32;
            case "keystone" -> 48;
            case "mastery", "unlock", "starting" -> 36;
            default -> 24;
        };
        return new NodeVisualTemplate(
            "fallback:" + nodeType,
            size,
            "Hyforged/Textures/PassiveSkillScreenAscendancyFrameSmallAllocated.png",
            "Hyforged/Textures/PassiveSkillScreenAscendancyFrameSmallCanAllocate.png",
            "Hyforged/Textures/PassiveSkillScreenAscendancyFrameSmallNormal.png"
        );
    }
}
