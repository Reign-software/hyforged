package reign.software.hyforged.stats;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Namespaced identifier for a stat.
 * Format: "namespace:name" (e.g., "hyforged:strength", "hyforged:armor-rating")
 * <p>
 * This is pure data - no behavior, following ECS principles.
 */
public record StatId(@Nonnull String namespace, @Nonnull String name) {
    
    public static final String HYFORGED_NAMESPACE = "hyforged";
    
    public StatId {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        if (namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace cannot be empty");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty");
        }
        if (namespace.contains(":") || name.contains(":")) {
            throw new IllegalArgumentException("namespace and name cannot contain ':'");
        }
    }
    
    /**
     * Create a StatId in the hyforged namespace.
     */
    @Nonnull
    public static StatId hyforged(@Nonnull String name) {
        return new StatId(HYFORGED_NAMESPACE, name);
    }
    
    /**
     * Parse a StatId from string format "namespace:name".
     */
    @Nonnull
    public static StatId parse(@Nonnull String fullId) {
        Objects.requireNonNull(fullId, "fullId cannot be null");
        int colonIndex = fullId.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException("Invalid stat ID format, expected 'namespace:name': " + fullId);
        }
        String ns = fullId.substring(0, colonIndex);
        String n = fullId.substring(colonIndex + 1);
        return new StatId(ns, n);
    }
    
    /**
     * Returns the full ID in "namespace:name" format.
     */
    @Nonnull
    public String fullId() {
        return namespace + ":" + name;
    }
    
    @Override
    public String toString() {
        return fullId();
    }
}
