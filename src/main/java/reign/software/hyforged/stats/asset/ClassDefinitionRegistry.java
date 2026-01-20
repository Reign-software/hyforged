package reign.software.hyforged.stats.asset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Registry for character class definitions.
 * <p>
 * Provides lookup of class definitions by ID for player stat initialization.
 * This is a singleton registry populated by the ClassAssetLoader.
 */
public final class ClassDefinitionRegistry {

    private static final Logger LOGGER = Logger.getLogger(ClassDefinitionRegistry.class.getName());
    
    private static final ClassDefinitionRegistry INSTANCE = new ClassDefinitionRegistry();
    
    /** Default class ID used when no class is specified */
    public static final String DEFAULT_CLASS_ID = "hyforged:default";
    
    private final Map<String, ClassDefinition> classes = new ConcurrentHashMap<>();

    private ClassDefinitionRegistry() {
        // Singleton
    }

    /**
     * Get the singleton registry instance.
     *
     * @return The class definition registry
     */
    @Nonnull
    public static ClassDefinitionRegistry get() {
        return INSTANCE;
    }

    /**
     * Register a class definition.
     *
     * @param classDef The class definition to register
     */
    public void register(@Nonnull ClassDefinition classDef) {
        String id = classDef.id();
        if (classes.containsKey(id)) {
            LOGGER.warning("Duplicate class definition ID: " + id + " - overwriting");
        }
        classes.put(id, classDef);
        LOGGER.fine("Registered class definition: " + id);
    }

    /**
     * Get a class definition by ID.
     *
     * @param id The class ID
     * @return The class definition, or null if not found
     */
    @Nullable
    public ClassDefinition get(@Nonnull String id) {
        return classes.get(id);
    }

    /**
     * Get a class definition by ID, or the default class if not found.
     *
     * @param id The class ID
     * @return The class definition, or the default class if not found
     */
    @Nonnull
    public ClassDefinition getOrDefault(@Nonnull String id) {
        ClassDefinition classDef = classes.get(id);
        if (classDef == null) {
            classDef = classes.get(DEFAULT_CLASS_ID);
        }
        if (classDef == null) {
            // Emergency fallback - create an empty default
            LOGGER.warning("No default class definition found, using empty fallback");
            return new ClassDefinition(
                DEFAULT_CLASS_ID,
                "Default",
                "Default character class",
                java.util.Collections.emptyMap()
            );
        }
        return classDef;
    }

    /**
     * Get the default class definition.
     *
     * @return The default class definition
     */
    @Nonnull
    public ClassDefinition getDefault() {
        return getOrDefault(DEFAULT_CLASS_ID);
    }

    /**
     * Check if a class definition exists.
     *
     * @param id The class ID
     * @return true if the class exists
     */
    public boolean hasClass(@Nonnull String id) {
        return classes.containsKey(id);
    }

    /**
     * Get all registered class IDs.
     *
     * @return Unmodifiable set of class IDs
     */
    @Nonnull
    public java.util.Set<String> getAllClassIds() {
        return java.util.Collections.unmodifiableSet(classes.keySet());
    }

    /**
     * Get the number of registered classes.
     *
     * @return The class count
     */
    public int getClassCount() {
        return classes.size();
    }

    /**
     * Clear all registered classes.
     * Used for testing and reload.
     */
    public void clear() {
        classes.clear();
    }
}
