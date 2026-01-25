package reign.software.hyforged.passive.model;

import javax.annotation.Nonnull;

/**
 * Position of a passive node in the tree UI.
 *
 * @param x The X coordinate
 * @param y The Y coordinate
 */
public record PassiveNodePosition(int x, int y) {
    
    public static final PassiveNodePosition ORIGIN = new PassiveNodePosition(0, 0);
    
    /**
     * Calculate distance to another position.
     *
     * @param other The other position
     * @return The Euclidean distance
     */
    public double distanceTo(@Nonnull PassiveNodePosition other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
