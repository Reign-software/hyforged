package reign.software.hyforged.stats.debug;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;

/**
 * Metrics tracking for the Hyforged stat system.
 * <p>
 * Collects performance and usage statistics for debugging and monitoring:
 * - Stat recompute counts (total and per-tick)
 * - Modifier counts per entity
 * - Bridge update counts
 * - System tick durations
 * <p>
 * All metrics are thread-safe and can be queried at any time.
 */
public final class StatMetrics {
    
    private StatMetrics() {} // Static utility class
    
    // ========== RECOMPUTE METRICS ==========
    
    /** Total number of stat recomputes across all time */
    private static final AtomicLong totalRecomputes = new AtomicLong(0);
    
    /** Recomputes in the current tick window */
    private static final AtomicInteger currentTickRecomputes = new AtomicInteger(0);
    
    /** Peak recomputes seen in a single tick */
    private static final AtomicInteger peakTickRecomputes = new AtomicInteger(0);
    
    /** Number of entities processed this tick */
    private static final AtomicInteger entitiesProcessedThisTick = new AtomicInteger(0);
    
    // ========== MODIFIER METRICS ==========
    
    /** Total modifier additions across all time */
    private static final AtomicLong totalModifierAdditions = new AtomicLong(0);
    
    /** Total modifier removals across all time */
    private static final AtomicLong totalModifierRemovals = new AtomicLong(0);
    
    /** Peak modifier count seen on a single entity */
    private static final AtomicInteger peakModifierCount = new AtomicInteger(0);
    
    // ========== BRIDGE METRICS ==========
    
    /** Total bridge updates to Hytale stats */
    private static final AtomicLong totalBridgeUpdates = new AtomicLong(0);
    
    // ========== TIMING METRICS ==========
    
    /** Last compute system tick duration in nanoseconds */
    private static volatile long lastComputeTickNanos = 0;
    
    /** Peak compute system tick duration in nanoseconds */
    private static final AtomicLong peakComputeTickNanos = new AtomicLong(0);
    
    /** Last bridge system tick duration in nanoseconds */
    private static volatile long lastBridgeTickNanos = 0;
    
    /** Peak bridge system tick duration in nanoseconds */
    private static final AtomicLong peakBridgeTickNanos = new AtomicLong(0);
    
    // ========== RECOMPUTE TRACKING ==========
    
    /**
     * Record a stat recomputation.
     * Called by HyforgedStatComputeSystem for each stat recomputed.
     */
    public static void recordRecompute() {
        totalRecomputes.incrementAndGet();
        currentTickRecomputes.incrementAndGet();
    }
    
    /**
     * Record multiple stat recomputations.
     */
    public static void recordRecomputes(int count) {
        totalRecomputes.addAndGet(count);
        currentTickRecomputes.addAndGet(count);
    }
    
    /**
     * Record an entity being processed by the compute system.
     */
    public static void recordEntityProcessed() {
        entitiesProcessedThisTick.incrementAndGet();
    }
    
    /**
     * Called at the end of each compute system tick.
     * Updates peak values and resets per-tick counters.
     */
    public static void endComputeTick() {
        int recomputes = currentTickRecomputes.getAndSet(0);
        entitiesProcessedThisTick.set(0);
        
        // Update peaks
        int currentPeak = peakTickRecomputes.get();
        while (recomputes > currentPeak) {
            if (peakTickRecomputes.compareAndSet(currentPeak, recomputes)) {
                break;
            }
            currentPeak = peakTickRecomputes.get();
        }
    }
    
    // ========== MODIFIER TRACKING ==========
    
    /**
     * Record a modifier addition.
     */
    public static void recordModifierAdded() {
        totalModifierAdditions.incrementAndGet();
    }
    
    /**
     * Record a modifier removal.
     */
    public static void recordModifierRemoved() {
        totalModifierRemovals.incrementAndGet();
    }
    
    /**
     * Record multiple modifier removals.
     */
    public static void recordModifiersRemoved(int count) {
        totalModifierRemovals.addAndGet(count);
    }
    
    /**
     * Update peak modifier count if the given count exceeds current peak.
     */
    public static void updatePeakModifierCount(int count) {
        int currentPeak = peakModifierCount.get();
        while (count > currentPeak) {
            if (peakModifierCount.compareAndSet(currentPeak, count)) {
                break;
            }
            currentPeak = peakModifierCount.get();
        }
    }
    
    // ========== BRIDGE TRACKING ==========
    
    /**
     * Record a bridge update to Hytale stats.
     */
    public static void recordBridgeUpdate() {
        totalBridgeUpdates.incrementAndGet();
    }
    
    // ========== TIMING TRACKING ==========
    
    /**
     * Record compute system tick duration.
     * @param nanos Duration in nanoseconds
     */
    public static void recordComputeTickDuration(long nanos) {
        lastComputeTickNanos = nanos;
        
        long currentPeak = peakComputeTickNanos.get();
        while (nanos > currentPeak) {
            if (peakComputeTickNanos.compareAndSet(currentPeak, nanos)) {
                break;
            }
            currentPeak = peakComputeTickNanos.get();
        }
    }
    
    /**
     * Record bridge system tick duration.
     * @param nanos Duration in nanoseconds
     */
    public static void recordBridgeTickDuration(long nanos) {
        lastBridgeTickNanos = nanos;
        
        long currentPeak = peakBridgeTickNanos.get();
        while (nanos > currentPeak) {
            if (peakBridgeTickNanos.compareAndSet(currentPeak, nanos)) {
                break;
            }
            currentPeak = peakBridgeTickNanos.get();
        }
    }
    
    // ========== GETTERS ==========
    
    /** Get total stat recomputes across all time. */
    public static long getTotalRecomputes() {
        return totalRecomputes.get();
    }
    
    /** Get peak recomputes seen in a single tick. */
    public static int getPeakTickRecomputes() {
        return peakTickRecomputes.get();
    }
    
    /** Get total modifier additions. */
    public static long getTotalModifierAdditions() {
        return totalModifierAdditions.get();
    }
    
    /** Get total modifier removals. */
    public static long getTotalModifierRemovals() {
        return totalModifierRemovals.get();
    }
    
    /** Get peak modifier count seen on any entity. */
    public static int getPeakModifierCount() {
        return peakModifierCount.get();
    }
    
    /** Get total bridge updates. */
    public static long getTotalBridgeUpdates() {
        return totalBridgeUpdates.get();
    }
    
    /** Get last compute tick duration in milliseconds. */
    public static double getLastComputeTickMs() {
        return lastComputeTickNanos / 1_000_000.0;
    }
    
    /** Get peak compute tick duration in milliseconds. */
    public static double getPeakComputeTickMs() {
        return peakComputeTickNanos.get() / 1_000_000.0;
    }
    
    /** Get last bridge tick duration in milliseconds. */
    public static double getLastBridgeTickMs() {
        return lastBridgeTickNanos / 1_000_000.0;
    }
    
    /** Get peak bridge tick duration in milliseconds. */
    public static double getPeakBridgeTickMs() {
        return peakBridgeTickNanos.get() / 1_000_000.0;
    }
    
    // ========== SNAPSHOT ==========
    
    /**
     * Get a snapshot of all metrics.
     * @return Map of metric names to values
     */
    @Nonnull
    public static Map<String, Object> getSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        
        // Recompute metrics
        snapshot.put("totalRecomputes", getTotalRecomputes());
        snapshot.put("peakTickRecomputes", getPeakTickRecomputes());
        
        // Modifier metrics
        snapshot.put("totalModifierAdditions", getTotalModifierAdditions());
        snapshot.put("totalModifierRemovals", getTotalModifierRemovals());
        snapshot.put("peakModifierCount", getPeakModifierCount());
        
        // Bridge metrics
        snapshot.put("totalBridgeUpdates", getTotalBridgeUpdates());
        
        // Timing metrics (in ms)
        snapshot.put("lastComputeTickMs", getLastComputeTickMs());
        snapshot.put("peakComputeTickMs", getPeakComputeTickMs());
        snapshot.put("lastBridgeTickMs", getLastBridgeTickMs());
        snapshot.put("peakBridgeTickMs", getPeakBridgeTickMs());
        
        return snapshot;
    }
    
    /**
     * Format all metrics as a human-readable string.
     */
    @Nonnull
    public static String formatMetrics() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== HYFORGED STAT METRICS ==========\n");
        
        sb.append("\n--- Recompute Metrics ---\n");
        sb.append(String.format("Total Recomputes: %,d\n", getTotalRecomputes()));
        sb.append(String.format("Peak Recomputes/Tick: %,d\n", getPeakTickRecomputes()));
        
        sb.append("\n--- Modifier Metrics ---\n");
        sb.append(String.format("Total Modifier Additions: %,d\n", getTotalModifierAdditions()));
        sb.append(String.format("Total Modifier Removals: %,d\n", getTotalModifierRemovals()));
        sb.append(String.format("Peak Modifier Count (single entity): %,d\n", getPeakModifierCount()));
        
        sb.append("\n--- Bridge Metrics ---\n");
        sb.append(String.format("Total Bridge Updates: %,d\n", getTotalBridgeUpdates()));
        
        sb.append("\n--- Timing Metrics ---\n");
        sb.append(String.format("Last Compute Tick: %.3f ms\n", getLastComputeTickMs()));
        sb.append(String.format("Peak Compute Tick: %.3f ms\n", getPeakComputeTickMs()));
        sb.append(String.format("Last Bridge Tick: %.3f ms\n", getLastBridgeTickMs()));
        sb.append(String.format("Peak Bridge Tick: %.3f ms\n", getPeakBridgeTickMs()));
        
        sb.append("=============================================\n");
        return sb.toString();
    }
    
    /**
     * Reset all metrics to initial values.
     * Useful for benchmarking or starting fresh.
     */
    public static void reset() {
        totalRecomputes.set(0);
        currentTickRecomputes.set(0);
        peakTickRecomputes.set(0);
        entitiesProcessedThisTick.set(0);
        
        totalModifierAdditions.set(0);
        totalModifierRemovals.set(0);
        peakModifierCount.set(0);
        
        totalBridgeUpdates.set(0);
        
        lastComputeTickNanos = 0;
        peakComputeTickNanos.set(0);
        lastBridgeTickNanos = 0;
        peakBridgeTickNanos.set(0);
    }
}
