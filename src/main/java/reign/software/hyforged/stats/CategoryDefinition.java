package reign.software.hyforged.stats;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Definition of a category that groups related stats for UI organization.
 * <p>
 * Categories are data-driven and loaded from JSON assets in Server/Hyforged/Categories/.
 * This follows the same pattern as tags, allowing mods to define custom categories.
 * <p>
 * This is pure immutable data - no behavior, following ECS principles.
 */
public record CategoryDefinition(
    @Nonnull String id,
    @Nonnull String displayName,
    @Nonnull String description,
    int sortOrder
) {
    
    public CategoryDefinition {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
    }
    
    /**
     * Builder for creating CategoryDefinition instances.
     */
    public static class Builder {
        private String id;
        private String displayName = "";
        private String description = "";
        private int sortOrder = 0;
        
        public Builder(@Nonnull String id) {
            this.id = Objects.requireNonNull(id);
            this.displayName = id;
        }
        
        public Builder displayName(@Nonnull String name) {
            this.displayName = name;
            return this;
        }
        
        public Builder description(@Nonnull String desc) {
            this.description = desc;
            return this;
        }
        
        public Builder sortOrder(int order) {
            this.sortOrder = order;
            return this;
        }
        
        public CategoryDefinition build() {
            return new CategoryDefinition(id, displayName, description, sortOrder);
        }
    }
}
