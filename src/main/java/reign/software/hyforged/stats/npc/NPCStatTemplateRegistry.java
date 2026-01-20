package reign.software.hyforged.stats.npc;

import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Singleton registry for NPC stat templates.
 * <p>
 * Stores resolved templates (with inheritance flattened) and provides
 * lookup methods for NPC stat initialization.
 */
public final class NPCStatTemplateRegistry {

    private static final Logger LOGGER = Logger.getLogger(NPCStatTemplateRegistry.class.getName());

    /** Default base template ID */
    public static final String BASE_TEMPLATE_ID = "hyforged:base";
    
    /** Default hostile template ID */
    public static final String HOSTILE_TEMPLATE_ID = "hyforged:hostile";

    private static NPCStatTemplateRegistry instance;

    private final Map<String, NPCStatTemplate> templates = new HashMap<>();
    private final Map<String, NPCStatTemplate> unresolvedTemplates = new HashMap<>();
    private boolean inheritanceResolved = false;

    private NPCStatTemplateRegistry() {
        // Private constructor for singleton
    }

    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static synchronized NPCStatTemplateRegistry get() {
        if (instance == null) {
            instance = new NPCStatTemplateRegistry();
        }
        return instance;
    }

    /**
     * Reset the registry (for testing).
     */
    public static synchronized void reset() {
        instance = null;
    }

    /**
     * Register an unresolved template (inheritance not yet processed).
     * <p>
     * Call resolveInheritance() after all templates are registered.
     *
     * @param template The template to register
     */
    public void registerUnresolved(@Nonnull NPCStatTemplate template) {
        unresolvedTemplates.put(template.id(), template);
        inheritanceResolved = false;
        LOGGER.fine("Registered unresolved NPC template: " + template.id());
    }

    /**
     * Resolve inheritance for all registered templates.
     * <p>
     * This flattens the inheritance hierarchy, merging parent stats
     * into child templates. Detects and logs circular references.
     */
    public void resolveInheritance() {
        if (inheritanceResolved) {
            return;
        }
        
        templates.clear();
        
        for (NPCStatTemplate template : unresolvedTemplates.values()) {
            NPCStatTemplate resolved = resolveTemplate(template, new HashSet<>());
            if (resolved != null) {
                templates.put(resolved.id(), resolved);
            }
        }
        
        inheritanceResolved = true;
        LOGGER.info("Resolved inheritance for " + templates.size() + " NPC templates");
    }

    /**
     * Recursively resolve a template's inheritance.
     */
    @Nullable
    private NPCStatTemplate resolveTemplate(
            @Nonnull NPCStatTemplate template,
            @Nonnull Set<String> visited
    ) {
        // Check for circular reference
        if (visited.contains(template.id())) {
            LOGGER.severe("Circular inheritance detected for NPC template: " + template.id());
            return null;
        }
        visited.add(template.id());
        
        // If no parent, return as-is
        if (!template.hasParent()) {
            return template;
        }
        
        // Find parent
        NPCStatTemplate parent = unresolvedTemplates.get(template.parentId());
        if (parent == null) {
            LOGGER.warning("NPC template '" + template.id() + "' references unknown parent: " + template.parentId());
            // Return template without parent resolution
            return template;
        }
        
        // Resolve parent first
        NPCStatTemplate resolvedParent = resolveTemplate(parent, visited);
        if (resolvedParent == null) {
            return template;
        }
        
        // Merge parent into child
        return NPCStatTemplate.builder(template.id())
                .parent(template.parentId())
                .mergeStats(resolvedParent.stats())
                .mergeModifierPools(resolvedParent.modifierPools())
                // Now overlay child stats (they take precedence)
                .mergeStats(template.stats())
                .mergeModifierPools(template.modifierPools())
                .build();
    }

    /**
     * Get a resolved template by ID.
     *
     * @param templateId The template ID
     * @return The template, or null if not found
     */
    @Nullable
    public NPCStatTemplate getTemplate(@Nonnull String templateId) {
        if (!inheritanceResolved) {
            resolveInheritance();
        }
        return templates.get(templateId);
    }

    /**
     * Get a resolved template, falling back to base template if not found.
     *
     * @param templateId The template ID
     * @return The template, or base template if not found
     */
    @Nonnull
    public NPCStatTemplate getTemplateOrBase(@Nonnull String templateId) {
        NPCStatTemplate template = getTemplate(templateId);
        if (template != null) {
            return template;
        }
        
        template = getTemplate(BASE_TEMPLATE_ID);
        if (template != null) {
            return template;
        }
        
        // Create fallback base template
        return NPCStatTemplate.builder(BASE_TEMPLATE_ID)
                .stat(StatId.hyforged("max-health"), 100, 10)
                .stat(StatId.hyforged("strength"), 10, 1)
                .build();
    }

    /**
     * Resolve stats for a template at a given level.
     *
     * @param templateId The template ID
     * @param level      The NPC level
     * @return Map of stat ID → resolved value
     */
    @Nonnull
    public Map<StatId, Integer> resolveStats(@Nonnull String templateId, int level) {
        NPCStatTemplate template = getTemplateOrBase(templateId);
        return template.resolveStats(level);
    }

    /**
     * Check if a template exists.
     */
    public boolean hasTemplate(@Nonnull String templateId) {
        if (!inheritanceResolved) {
            resolveInheritance();
        }
        return templates.containsKey(templateId);
    }

    /**
     * Get all registered template IDs.
     */
    @Nonnull
    public Set<String> getTemplateIds() {
        if (!inheritanceResolved) {
            resolveInheritance();
        }
        return Set.copyOf(templates.keySet());
    }
}
