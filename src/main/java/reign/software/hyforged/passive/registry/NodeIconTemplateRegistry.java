package reign.software.hyforged.passive.registry;

import reign.software.hyforged.passive.model.NodeIconTemplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Registry for node icon templates.
 * <p>
 * Templates define the icon textures and scaling for nodes.
 * Templates are loaded from JSON and can be referenced by ID in node definitions.
 */
public class NodeIconTemplateRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(NodeIconTemplateRegistry.class.getName());
    private static final NodeIconTemplateRegistry INSTANCE = new NodeIconTemplateRegistry();
    
    private final Map<String, NodeIconTemplate> templates = new ConcurrentHashMap<>();
    
    private NodeIconTemplateRegistry() {
    }
    
    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static NodeIconTemplateRegistry get() {
        return INSTANCE;
    }
    
    /**
     * Register a template.
     *
     * @param template The template to register
     */
    public void register(@Nonnull NodeIconTemplate template) {
        templates.put(template.id(), template);
        LOGGER.fine(() -> "Registered node icon template: " + template.id());
    }
    
    /**
     * Get a template by ID.
     *
     * @param id The template ID
     * @return The template, or null if not found
     */
    @Nullable
    public NodeIconTemplate get(@Nonnull String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return templates.get(id);
    }
    
    /**
     * Clear all registered templates.
     */
    public void clear() {
        templates.clear();
    }
    
    /**
     * Get the number of registered templates.
     */
    public int size() {
        return templates.size();
    }
}
