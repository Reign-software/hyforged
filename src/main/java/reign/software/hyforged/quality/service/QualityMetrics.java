package reign.software.hyforged.quality.service;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Metrics collection for quality rolling.
 */
public final class QualityMetrics {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final QualityMetrics INSTANCE = new QualityMetrics();

    private final AtomicLong rollAttempts = new AtomicLong(0);
    private final AtomicLong rollSuccesses = new AtomicLong(0);

    private final Map<String, AtomicLong> rollsByQuality = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> rollsBySourceType = new ConcurrentHashMap<>();

    private QualityMetrics() {}

    @Nonnull
    public static QualityMetrics get() {
        return INSTANCE;
    }

    public void recordRollAttempt(@Nonnull String sourceType) {
        rollAttempts.incrementAndGet();
        rollsBySourceType.computeIfAbsent(sourceType, key -> new AtomicLong(0)).incrementAndGet();
        LOGGER.at(Level.FINEST).log("Quality roll attempt recorded for source: %s", sourceType);
    }

    public void recordRollSuccess(@Nonnull String qualityId) {
        rollSuccesses.incrementAndGet();
        rollsByQuality.computeIfAbsent(qualityId, key -> new AtomicLong(0)).incrementAndGet();
    }

    public long getRollAttempts() {
        return rollAttempts.get();
    }

    public long getRollSuccesses() {
        return rollSuccesses.get();
    }

    @Nonnull
    public Map<String, Long> getRollsByQuality() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        rollsByQuality.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    @Nonnull
    public Map<String, Long> getRollsBySourceType() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        rollsBySourceType.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    public void reset() {
        rollAttempts.set(0);
        rollSuccesses.set(0);
        rollsByQuality.clear();
        rollsBySourceType.clear();
        LOGGER.at(Level.FINE).log("Quality metrics reset");
    }
}
