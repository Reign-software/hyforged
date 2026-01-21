package reign.software.hyforged.combat;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Combat random number generator with optional seeding for deterministic replay.
 * <p>
 * In normal gameplay, uses ThreadLocalRandom for performance.
 * When seeded (for testing or replay), uses a deterministic Random instance.
 * <p>
 * Usage:
 * <pre>{@code
 * // Normal gameplay (non-deterministic)
 * int roll = CombatRandom.nextInt(10000);
 * 
 * // Seeded mode for testing
 * CombatRandom.setSeed(12345L);
 * int roll1 = CombatRandom.nextInt(10000); // Always same value for seed
 * CombatRandom.clearSeed(); // Return to normal mode
 * }</pre>
 */
public final class CombatRandom {

    private CombatRandom() {
        // Utility class
    }

    /** Thread-local seeded random for deterministic mode */
    private static final ThreadLocal<Random> seededRandom = new ThreadLocal<>();
    
    /** Tracks whether seeded mode is active */
    private static final ThreadLocal<Boolean> seededMode = ThreadLocal.withInitial(() -> false);
    
    /** Global seed for replay (set via setSeed) */
    private static final AtomicLong currentSeed = new AtomicLong(0L);
    
    /** Last roll value for debugging/logging */
    private static final ThreadLocal<Integer> lastRoll = ThreadLocal.withInitial(() -> -1);

    // ==================== Seed Management ====================

    /**
     * Set a seed for deterministic random generation.
     * <p>
     * All subsequent calls to nextInt/rollChance will use this seed
     * until clearSeed() is called.
     * 
     * @param seed The seed value
     */
    public static void setSeed(long seed) {
        currentSeed.set(seed);
        seededRandom.set(new Random(seed));
        seededMode.set(true);
    }

    /**
     * Clear the seed and return to non-deterministic mode.
     */
    public static void clearSeed() {
        seededRandom.remove();
        seededMode.set(false);
    }

    /**
     * Check if seeded mode is active.
     * 
     * @return true if using seeded random
     */
    public static boolean isSeeded() {
        return seededMode.get();
    }

    /**
     * Get the current seed (0 if not in seeded mode).
     * 
     * @return The current seed
     */
    public static long getCurrentSeed() {
        return isSeeded() ? currentSeed.get() : 0L;
    }

    // ==================== Random Generation ====================

    /**
     * Generate a random integer in [0, bound).
     * 
     * @param bound Upper bound (exclusive)
     * @return Random integer
     */
    public static int nextInt(int bound) {
        int value;
        if (seededMode.get()) {
            Random random = seededRandom.get();
            value = random != null ? random.nextInt(bound) : ThreadLocalRandom.current().nextInt(bound);
        } else {
            value = ThreadLocalRandom.current().nextInt(bound);
        }
        lastRoll.set(value);
        return value;
    }

    /**
     * Generate a random float in [0.0, 1.0).
     * 
     * @return Random float
     */
    public static float nextFloat() {
        if (seededMode.get()) {
            Random random = seededRandom.get();
            return random != null ? random.nextFloat() : ThreadLocalRandom.current().nextFloat();
        }
        return ThreadLocalRandom.current().nextFloat();
    }

    /**
     * Get the last roll value (for debugging/logging).
     * 
     * @return The last roll value, or -1 if none
     */
    public static int getLastRoll() {
        return lastRoll.get();
    }

    // ==================== Combat Roll Helpers ====================

    /**
     * Roll a chance-based check.
     * <p>
     * Use this instead of CombatMath.rollChance for deterministic combat.
     * 
     * @param chanceBps The chance in basis points (10000 = 100%)
     * @return true if the roll succeeded
     */
    public static boolean rollChance(int chanceBps) {
        if (chanceBps <= 0) return false;
        if (chanceBps >= CombatMath.BPS_100) return true;
        
        int roll = nextInt(CombatMath.BPS_100);
        return roll < chanceBps;
    }

    /**
     * Roll a chance-based check and return the roll value.
     * <p>
     * Useful when you need both the roll result and the actual roll value
     * for combat logging.
     * 
     * @param chanceBps The chance in basis points (10000 = 100%)
     * @return RollResult containing success and roll value
     */
    public static RollResult rollChanceWithValue(int chanceBps) {
        if (chanceBps <= 0) return new RollResult(false, -1);
        if (chanceBps >= CombatMath.BPS_100) return new RollResult(true, -1);
        
        int roll = nextInt(CombatMath.BPS_100);
        return new RollResult(roll < chanceBps, roll);
    }

    /**
     * Result of a chance roll, including both success and the roll value.
     */
    public record RollResult(boolean success, int rollValue) {}
}
