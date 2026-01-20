package reign.software.hyforged.affix.service;

import reign.software.hyforged.affix.model.RolledAffix;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Metrics collection stubs for affix system observability.
 * <p>
 * Currently stores counters in memory. In production, these could be
 * exported to external monitoring systems (Prometheus, StatsD, etc.).
 * <p>
 * Thread-safe for concurrent access.
 */
public final class AffixMetrics {
    
    private static final Logger LOGGER = Logger.getLogger(AffixMetrics.class.getName());
    private static final AffixMetrics INSTANCE = new AffixMetrics();
    
    // Rolling metrics
    private final AtomicLong rollAttempts = new AtomicLong(0);
    private final AtomicLong rollSuccesses = new AtomicLong(0);
    private final AtomicLong rollFailures = new AtomicLong(0);
    private final AtomicLong totalAffixesRolled = new AtomicLong(0);
    
    // Quality distribution
    private final Map<String, AtomicLong> rollsByQuality = new ConcurrentHashMap<>();
    
    // Tier distribution
    private final Map<Integer, AtomicLong> rollsByTier = new ConcurrentHashMap<>();
    
    // Type distribution
    private final Map<String, AtomicLong> rollsByType = new ConcurrentHashMap<>();
    
    private AffixMetrics() {}
    
    /**
     * Get the singleton metrics instance.
     */
    @Nonnull
    public static AffixMetrics get() {
        return INSTANCE;
    }
    
    /**
     * Record a roll attempt.
     *
     * @param quality The quality tier being rolled
     */
    public void recordRollAttempt(@Nonnull String quality) {
        rollAttempts.incrementAndGet();
        rollsByQuality.computeIfAbsent(quality, k -> new AtomicLong(0)).incrementAndGet();
        
        LOGGER.log(Level.FINEST, "Roll attempt recorded for quality: {0}", quality);
    }
    
    /**
     * Record a successful roll result.
     *
     * @param affixes The affixes that were rolled
     */
    public void recordRollSuccess(@Nonnull List<RolledAffix> affixes) {
        rollSuccesses.incrementAndGet();
        totalAffixesRolled.addAndGet(affixes.size());
        
        for (RolledAffix affix : affixes) {
            // Track tier distribution
            rollsByTier.computeIfAbsent(affix.tier(), k -> new AtomicLong(0)).incrementAndGet();
            
            // Track type distribution
            rollsByType.computeIfAbsent(affix.type(), k -> new AtomicLong(0)).incrementAndGet();
        }
        
        LOGGER.log(Level.FINEST, "Roll success recorded: {0} affixes", affixes.size());
    }
    
    /**
     * Record a failed roll (no affixes rolled).
     */
    public void recordRollFailure() {
        rollFailures.incrementAndGet();
        
        LOGGER.log(Level.FINEST, "Roll failure recorded");
    }
    
    // =========================================================================
    // Metrics Getters
    // =========================================================================
    
    /**
     * Get total roll attempts.
     */
    public long getRollAttempts() {
        return rollAttempts.get();
    }
    
    /**
     * Get successful roll count.
     */
    public long getRollSuccesses() {
        return rollSuccesses.get();
    }
    
    /**
     * Get failed roll count.
     */
    public long getRollFailures() {
        return rollFailures.get();
    }
    
    /**
     * Get total affixes rolled.
     */
    public long getTotalAffixesRolled() {
        return totalAffixesRolled.get();
    }
    
    /**
     * Get success rate as percentage.
     */
    public double getSuccessRate() {
        long attempts = rollAttempts.get();
        if (attempts == 0) return 0.0;
        return (double) rollSuccesses.get() / attempts * 100.0;
    }
    
    /**
     * Get average affixes per successful roll.
     */
    public double getAverageAffixesPerRoll() {
        long successes = rollSuccesses.get();
        if (successes == 0) return 0.0;
        return (double) totalAffixesRolled.get() / successes;
    }
    
    /**
     * Get roll count by quality tier.
     */
    @Nonnull
    public Map<String, Long> getRollsByQuality() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        rollsByQuality.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
    
    /**
     * Get affix count by tier.
     */
    @Nonnull
    public Map<Integer, Long> getRollsByTier() {
        Map<Integer, Long> result = new ConcurrentHashMap<>();
        rollsByTier.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
    
    /**
     * Get affix count by type (prefix/suffix/forged).
     */
    @Nonnull
    public Map<String, Long> getRollsByType() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        rollsByType.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
    
    /**
     * Reset all metrics (for testing).
     */
    public void reset() {
        rollAttempts.set(0);
        rollSuccesses.set(0);
        rollFailures.set(0);
        totalAffixesRolled.set(0);
        rollsByQuality.clear();
        rollsByTier.clear();
        rollsByType.clear();
        
        LOGGER.log(Level.FINE, "Metrics reset");
    }
    
    /**
     * Generate a summary of current metrics.
     */
    @Nonnull
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Affix Metrics ===\n");
        sb.append("Roll Attempts: ").append(rollAttempts.get()).append("\n");
        sb.append("Roll Successes: ").append(rollSuccesses.get()).append("\n");
        sb.append("Roll Failures: ").append(rollFailures.get()).append("\n");
        sb.append("Success Rate: ").append(String.format("%.1f%%", getSuccessRate())).append("\n");
        sb.append("Total Affixes Rolled: ").append(totalAffixesRolled.get()).append("\n");
        sb.append("Avg Affixes/Roll: ").append(String.format("%.2f", getAverageAffixesPerRoll())).append("\n");
        
        if (!rollsByQuality.isEmpty()) {
            sb.append("\nBy Quality:\n");
            rollsByQuality.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v.get()).append("\n"));
        }
        
        if (!rollsByTier.isEmpty()) {
            sb.append("\nBy Tier:\n");
            rollsByTier.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> sb.append("  T").append(e.getKey()).append(": ").append(e.getValue().get()).append("\n"));
        }
        
        if (!rollsByType.isEmpty()) {
            sb.append("\nBy Type:\n");
            rollsByType.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v.get()).append("\n"));
        }
        
        return sb.toString();
    }
}
