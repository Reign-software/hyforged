package reign.software.hyforged.passive.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a passive node in a passive tree.
 * <p>
 * Nodes are immutable data structures loaded from JSON.
 * Visual properties (frame, icon) are defined per-node for consistency.
 *
 * @param id Unique identifier for the node (namespaced, e.g., "hyforged:node-brutal-force")
 * @param type Node type (minor, notable, keystone, mastery, unlock)
 * @param name Display name
 * @param description Description text
 * @param frameTemplate ID of the visual template for the frame (null = use type default)
 * @param icon Direct texture path for the icon (null = use default icon)
 * @param label Optional label text displayed above the node
 * @param position Position in the tree UI
 * @param region Region/cluster this node belongs to
 * @param effects List of effects granted by this node
 * @param requirements Requirements to allocate this node
 * @param keystoneFamily For keystones, the family ID (mutual exclusion group)
 */
public record PassiveNode(
    @Nonnull String id,
    @Nonnull String type,
    @Nonnull String name,
    @Nonnull String description,
    @Nullable String frameTemplate,
    @Nullable String icon,
    @Nullable String label,
    @Nonnull PassiveNodePosition position,
    @Nullable String region,
    @Nonnull List<PassiveNodeEffect> effects,
    @Nonnull PassiveNodeRequirements requirements,
    @Nullable String keystoneFamily
) {
    
    public PassiveNode {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(position, "position cannot be null");
        Objects.requireNonNull(effects, "effects cannot be null");
        Objects.requireNonNull(requirements, "requirements cannot be null");
        effects = List.copyOf(effects); // Defensive copy
    }
    
    /**
     * Check if this is a minor node.
     */
    public boolean isMinor() {
        return PassiveNodeType.MINOR.equalsIgnoreCase(type);
    }
    
    /**
     * Check if this is a notable node.
     */
    public boolean isNotable() {
        return PassiveNodeType.NOTABLE.equalsIgnoreCase(type);
    }
    
    /**
     * Check if this is a keystone node.
     */
    public boolean isKeystone() {
        return PassiveNodeType.KEYSTONE.equalsIgnoreCase(type);
    }
    
    /**
     * Check if this is a mastery node.
     */
    public boolean isMastery() {
        return PassiveNodeType.MASTERY.equalsIgnoreCase(type);
    }
    
    /**
     * Check if this is an unlock node.
     */
    public boolean isUnlock() {
        return PassiveNodeType.UNLOCK.equalsIgnoreCase(type);
    }
    
    /**
     * Create a builder for constructing PassiveNode instances.
     *
     * @param id The node ID
     * @return A new builder
     */
    public static Builder builder(@Nonnull String id) {
        return new Builder(id);
    }
    
    /**
     * Builder for PassiveNode.
     */
    public static class Builder {
        private final String id;
        private String type = PassiveNodeType.MINOR;
        private String name = "";
        private String description = "";
        private String frameTemplate = null;
        private String icon = null;
        private String label = null;
        private PassiveNodePosition position = PassiveNodePosition.ORIGIN;
        private String region = null;
        private List<PassiveNodeEffect> effects = Collections.emptyList();
        private PassiveNodeRequirements requirements = PassiveNodeRequirements.NONE;
        private String keystoneFamily = null;
        
        private Builder(@Nonnull String id) {
            this.id = Objects.requireNonNull(id, "id cannot be null");
        }
        
        public Builder type(@Nonnull String type) {
            this.type = type;
            return this;
        }
        
        public Builder name(@Nonnull String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(@Nonnull String description) {
            this.description = description;
            return this;
        }
        
        public Builder frameTemplate(@Nullable String frameTemplate) {
            this.frameTemplate = frameTemplate;
            return this;
        }
        
        public Builder icon(@Nullable String icon) {
            this.icon = icon;
            return this;
        }
        
        public Builder label(@Nullable String label) {
            this.label = label;
            return this;
        }
        
        public Builder position(@Nonnull PassiveNodePosition position) {
            this.position = position;
            return this;
        }
        
        public Builder position(int x, int y) {
            this.position = new PassiveNodePosition(x, y);
            return this;
        }
        
        public Builder region(@Nullable String region) {
            this.region = region;
            return this;
        }
        
        public Builder effects(@Nonnull List<PassiveNodeEffect> effects) {
            this.effects = effects;
            return this;
        }
        
        public Builder requirements(@Nonnull PassiveNodeRequirements requirements) {
            this.requirements = requirements;
            return this;
        }
        
        public Builder keystoneFamily(@Nullable String keystoneFamily) {
            this.keystoneFamily = keystoneFamily;
            return this;
        }
        
        public PassiveNode build() {
            return new PassiveNode(id, type, name, description, frameTemplate, icon,
                                   label, position, region, effects, requirements, keystoneFamily);
        }
    }
}
