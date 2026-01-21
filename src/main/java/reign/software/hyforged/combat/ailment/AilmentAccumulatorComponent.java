package reign.software.hyforged.combat.ailment;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Component for tracking accumulated elemental damage for ailment triggering.
 * <p>
 * Ailments (freeze, ignite, poison, shock) are triggered when accumulated damage
 * of that element exceeds a threshold within a time window. Once triggered,
 * the accumulator resets for that element.
 * <p>
 * This follows the ARPG pattern where elemental damage builds toward ailment application
 * rather than immediate random chance.
 */
public class AilmentAccumulatorComponent implements Component<EntityStore> {
    
    /** Default accumulation window in milliseconds (5 seconds) */
    public static final long DEFAULT_WINDOW_MS = 5000;
    
    /** Default base threshold in damage points */
    public static final int DEFAULT_THRESHOLD = 100;
    
    /** Per-element accumulation tracking */
    private final Map<String, ElementAccumulation> accumulations = new HashMap<>();
    
    public AilmentAccumulatorComponent() {
    }
    
    public AilmentAccumulatorComponent(@Nonnull AilmentAccumulatorComponent other) {
        for (Map.Entry<String, ElementAccumulation> entry : other.accumulations.entrySet()) {
            this.accumulations.put(entry.getKey(), new ElementAccumulation(entry.getValue()));
        }
    }
    
    /**
     * Accumulate damage for an element.
     * 
     * @param elementTag The element tag (e.g., "fire", "ice", "lightning", "chaos")
     * @param damage The damage amount to accumulate
     * @param currentTime Current time in milliseconds
     * @return true if the threshold was exceeded and ailment should trigger
     */
    public boolean accumulateDamage(@Nonnull String elementTag, float damage, long currentTime) {
        ElementAccumulation acc = accumulations.computeIfAbsent(elementTag, k -> new ElementAccumulation());
        
        // Decay old damage outside the window
        acc.decayOutsideWindow(currentTime);
        
        // Add new damage
        acc.addDamage(damage, currentTime);
        
        // Check threshold
        return acc.isThresholdReached();
    }
    
    /**
     * Reset accumulation for an element after triggering.
     * 
     * @param elementTag The element tag
     */
    public void resetAccumulation(@Nonnull String elementTag) {
        ElementAccumulation acc = accumulations.get(elementTag);
        if (acc != null) {
            acc.reset();
        }
    }
    
    /**
     * Reset all accumulations for all elements.
     */
    public void resetAll() {
        for (ElementAccumulation acc : accumulations.values()) {
            acc.reset();
        }
    }
    
    /**
     * Get current accumulated damage for an element.
     * 
     * @param elementTag The element tag
     * @param currentTime Current time for decay calculation
     * @return Accumulated damage amount
     */
    public float getAccumulatedDamage(@Nonnull String elementTag, long currentTime) {
        ElementAccumulation acc = accumulations.get(elementTag);
        if (acc == null) {
            return 0;
        }
        acc.decayOutsideWindow(currentTime);
        return acc.getTotalDamage();
    }
    
    /**
     * Set the threshold for an element.
     * 
     * @param elementTag The element tag
     * @param threshold The damage threshold
     */
    public void setThreshold(@Nonnull String elementTag, int threshold) {
        ElementAccumulation acc = accumulations.computeIfAbsent(elementTag, k -> new ElementAccumulation());
        acc.setThreshold(threshold);
    }
    
    /**
     * Set the accumulation window for an element.
     * 
     * @param elementTag The element tag
     * @param windowMs The window in milliseconds
     */
    public void setWindow(@Nonnull String elementTag, long windowMs) {
        ElementAccumulation acc = accumulations.computeIfAbsent(elementTag, k -> new ElementAccumulation());
        acc.setWindowMs(windowMs);
    }
    
    @Override
    @Nonnull
    public AilmentAccumulatorComponent clone() {
        return new AilmentAccumulatorComponent(this);
    }
    
    /**
     * Per-element damage accumulation tracking.
     */
    private static class ElementAccumulation {
        private float totalDamage = 0;
        private long lastDamageTime = 0;
        private int threshold = DEFAULT_THRESHOLD;
        private long windowMs = DEFAULT_WINDOW_MS;
        
        ElementAccumulation() {}
        
        ElementAccumulation(ElementAccumulation other) {
            this.totalDamage = other.totalDamage;
            this.lastDamageTime = other.lastDamageTime;
            this.threshold = other.threshold;
            this.windowMs = other.windowMs;
        }
        
        void addDamage(float damage, long currentTime) {
            this.totalDamage += damage;
            this.lastDamageTime = currentTime;
        }
        
        void decayOutsideWindow(long currentTime) {
            if (lastDamageTime > 0 && currentTime - lastDamageTime > windowMs) {
                // Window expired, reset
                reset();
            }
        }
        
        boolean isThresholdReached() {
            return totalDamage >= threshold;
        }
        
        float getTotalDamage() {
            return totalDamage;
        }
        
        void reset() {
            totalDamage = 0;
            lastDamageTime = 0;
        }
        
        void setThreshold(int threshold) {
            this.threshold = threshold;
        }
        
        void setWindowMs(long windowMs) {
            this.windowMs = windowMs;
        }
    }
}
