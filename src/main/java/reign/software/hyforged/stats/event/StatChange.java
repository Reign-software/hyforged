package reign.software.hyforged.stats.event;

import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Record representing a single stat value change.
 * <p>
 * Used within {@link StatBatchChangedEvent} to describe individual stat changes.
 *
 * @param statId The stat that changed
 * @param statIndex The stat index in the registry
 * @param oldValue The previous effective value
 * @param newValue The new effective value
 * @param sourceId Optional identifier of the source that caused the change (modifier ID, etc.)
 */
public record StatChange(
    @Nonnull StatId statId,
    int statIndex,
    int oldValue,
    int newValue,
    @Nullable String sourceId
) {
    
    /**
     * Get the difference between the new and old values.
     *
     * @return The change amount (positive if increased, negative if decreased)
     */
    public int delta() {
        return newValue - oldValue;
    }
    
    /**
     * Check if this represents an increase.
     *
     * @return true if newValue > oldValue
     */
    public boolean isIncrease() {
        return newValue > oldValue;
    }
    
    /**
     * Check if this represents a decrease.
     *
     * @return true if newValue < oldValue
     */
    public boolean isDecrease() {
        return newValue < oldValue;
    }
    
    @Override
    public String toString() {
        return String.format("StatChange[%s: %d → %d (%+d)]", 
            statId.fullId(), oldValue, newValue, delta());
    }
}
