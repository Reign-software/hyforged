package reign.software.hyforged.passive.registry;

import reign.software.hyforged.passive.model.NodeVisualTemplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Registry for node visual templates (frames).
 * <p>
 * Templates define the frame textures and sizes for different node types.
 * Templates are loaded from JSON files in Server/&lt;Mod&gt;/PassiveTrees/templates/.
 * <p>
 * This registry is populated by the asset loader - no programmatic registration.
 */
public class NodeVisualTemplateRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(NodeVisualTemplateRegistry.class.getName());
    private static final NodeVisualTemplateRegistry INSTANCE = new NodeVisualTemplateRegistry();
    
    private final Map<String, NodeVisualTemplate> templates = new ConcurrentHashMap<>();
    private final Map<String, String> typeDefaults = new ConcurrentHashMap<>();
    
    private NodeVisualTemplateRegistry() {
    }
    
    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static NodeVisualTemplateRegistry get() {
        return INSTANCE;
    }
    
    /**
     * Register a template. Called by the asset loader.
     *
     * @param template The template to register
     */
    public void register(@Nonnull NodeVisualTemplate template) {
        templates.put(template.id(), template);
        LOGGER.fine(() -> "Registered node visual template: " + template.id());
    }
    
    /**
     * Set the default template for a node type. Called by the asset loader.
     *
     * @param nodeType The node type (e.g., "minor", "notable")
     * @param templateId The template ID to use as default for this type
     */
    public void setTypeDefault(@Nonnull String nodeType, @Nonnull String templateId) {
        typeDefaults.put(nodeType.toLowerCase(), templateId);
        LOGGER.fine(() -> "Set type default: " + nodeType + " -> " + templateId);
    }
    
    /**
     * Get a template by ID.
     *
     * @param id The template ID
     * @return The template, or null if not found
     */
    @Nullable
    public NodeVisualTemplate get(@Nonnull String id) {
        return templates.get(id);
    }
    
    /**
     * Get a template by ID, falling back to a default based on node type.
     *
     * @param id The template ID (may be null)
     * @param nodeType The node type for fallback
     * @param isStarting Whether this is a starting node
     * @return The resolved template
     */
    @Nonnull
    public NodeVisualTemplate resolve(@Nullable String id, @Nonnull String nodeType, boolean isStarting) {
        // Try explicit ID first
        if (id != null && !id.isEmpty()) {
            NodeVisualTemplate template = templates.get(id);
            if (template != null) {
                return template;
            }
            LOGGER.warning("Node visual template not found: " + id + ", falling back to type default");
        }
        
        // Fall back to starting node template
        String effectiveType = isStarting ? "starting" : nodeType.toLowerCase();
        
        // Check type defaults from JSON
        String defaultTemplateId = typeDefaults.get(effectiveType);
        if (defaultTemplateId != null) {
            NodeVisualTemplate template = templates.get(defaultTemplateId);
            if (template != null) {
                return template;
            }
        }
        
        // Final fallback - generate a placeholder template
        LOGGER.warning("No template found for type: " + effectiveType + ", using fallback");
        return NodeVisualTemplate.fallback(effectiveType);
    }
    
    /**
     * Clear all registered templates.
     */
    public void clear() {
        templates.clear();
        typeDefaults.clear();
    }
    
    /**
     * Get the number of registered templates.
     */
    public int size() {
        return templates.size();
    }
}
