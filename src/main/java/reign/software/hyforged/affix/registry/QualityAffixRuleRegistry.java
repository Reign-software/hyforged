package reign.software.hyforged.affix.registry;

import reign.software.hyforged.affix.model.QualityAffixRule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Central registry for quality-based affix capacity rules.
 * <p>
 * Quality affix rules define how many affixes of each type an item can have
 * based on its quality tier. Rules are loaded from JSON at
 * {@code Server/Hyforged/Quality/AffixRules/*.json}.
 * <p>
 * This is a singleton registry loaded at startup, NOT an ECS component.
 * <p>
 * Duplicate ID Policy: When a duplicate quality is registered, the latest entry wins
 * (by load order) and a WARN log is emitted to highlight the override.
 */
public final class QualityAffixRuleRegistry {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static QualityAffixRuleRegistry instance;
    
    private final Map<String, QualityAffixRule> rulesByQuality = new ConcurrentHashMap<>();
    
    // Quality ordering for comparisons (lower = worse quality)
    private final Map<String, Integer> qualityOrder = new ConcurrentHashMap<>();
    
    private boolean frozen = false;
    
    private QualityAffixRuleRegistry() {
        // Initialize default quality ordering based on Hytale's Quality values
        // These can be overridden by asset loading
        initializeDefaultQualityOrder();
    }
    
    private void initializeDefaultQualityOrder() {
        qualityOrder.put("Junk", 0);
        qualityOrder.put("Common", 1);
        qualityOrder.put("Uncommon", 2);
        qualityOrder.put("Rare", 3);
        qualityOrder.put("Epic", 4);
        qualityOrder.put("Legendary", 5);
        qualityOrder.put("Tool", -1);      // Non-equipment
        qualityOrder.put("Technical", -2);  // Non-equipment
        qualityOrder.put("Template", -3);   // Non-equipment
        qualityOrder.put("Debug", -4);      // Non-equipment
        qualityOrder.put("Developer", -5);  // Non-equipment
    }
    
    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static synchronized QualityAffixRuleRegistry get() {
        if (instance == null) {
            instance = new QualityAffixRuleRegistry();
        }
        return instance;
    }
    
    /**
     * Reset the registry (for testing or reload).
     */
    public static synchronized void reset() {
        instance = new QualityAffixRuleRegistry();
    }
    
    /**
     * Register a quality affix rule.
     * <p>
     * If a rule for the same quality already exists, it will be replaced and a warning logged.
     *
     * @param rule The quality affix rule to register
     * @throws IllegalStateException if registry is frozen
     */
    public synchronized void register(@Nonnull QualityAffixRule rule) {
        Objects.requireNonNull(rule, "rule cannot be null");
        
        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot register new quality rules");
        }
        
        String quality = rule.quality();
        if (rulesByQuality.containsKey(quality)) {
            LOGGER.atWarning().log("Quality affix rule for '%s' is being overridden by a later definition", quality);
        }
        
        rulesByQuality.put(quality, rule);
        LOGGER.at(Level.FINE).log("Registered quality affix rule: %s (totalCapacity=%s)", quality, rule.getTotalCapacity());
    }
    
    /**
     * Get the affix rule for a specific quality.
     *
     * @param quality The quality ID (e.g., "Common", "Legendary")
     * @return The quality affix rule, or null if not defined
     */
    @Nullable
    public QualityAffixRule get(@Nonnull String quality) {
        return rulesByQuality.get(quality);
    }
    
    /**
     * Get the affix rule for a specific quality, returning empty rule if not found.
     * <p>
     * Qualities without rules (e.g., Tool, Technical) are treated as having zero affix capacity.
     *
     * @param quality The quality ID
     * @return The quality affix rule, or EMPTY if not defined
     */
    @Nonnull
    public QualityAffixRule getOrEmpty(@Nonnull String quality) {
        QualityAffixRule rule = rulesByQuality.get(quality);
        return rule != null ? rule : QualityAffixRule.EMPTY;
    }
    
    /**
     * Get the affix capacity for a specific quality and affix type.
     *
     * @param quality The quality ID
     * @param typeId The affix type ID (e.g., "prefix", "suffix")
     * @return The capacity, or 0 if no rule defined
     */
    public int getCapacity(@Nonnull String quality, @Nonnull String typeId) {
        QualityAffixRule rule = rulesByQuality.get(quality);
        return rule != null ? rule.getCapacity(typeId) : 0;
    }
    
    /**
     * Check if a quality allows any affixes.
     */
    public boolean allowsAffixes(@Nonnull String quality) {
        QualityAffixRule rule = rulesByQuality.get(quality);
        return rule != null && rule.allowsAnyAffixes();
    }
    
    /**
     * Get all registered quality affix rules.
     */
    @Nonnull
    public Collection<QualityAffixRule> getAll() {
        return Collections.unmodifiableCollection(rulesByQuality.values());
    }
    
    /**
     * Get the quality ordering map for comparisons.
     * <p>
     * Lower values indicate worse quality. Non-equipment qualities have negative values.
     */
    @Nonnull
    public Map<String, Integer> getQualityOrder() {
        return Collections.unmodifiableMap(qualityOrder);
    }
    
    /**
     * Get the numeric order value for a quality.
     *
     * @param quality The quality ID
     * @return The order value, or 0 if unknown
     */
    public int getQualityOrderValue(@Nonnull String quality) {
        return qualityOrder.getOrDefault(quality, 0);
    }
    
    /**
     * Compare two qualities by their ordering.
     *
     * @return negative if q1 < q2, 0 if equal, positive if q1 > q2
     */
    public int compareQualities(@Nonnull String q1, @Nonnull String q2) {
        return Integer.compare(getQualityOrderValue(q1), getQualityOrderValue(q2));
    }
    
    /**
     * Get the count of registered quality rules.
     */
    public int size() {
        return rulesByQuality.size();
    }
    
    /**
     * Freeze the registry, preventing further modifications.
     */
    public synchronized void freeze() {
        frozen = true;
        LOGGER.atInfo().log("QualityAffixRuleRegistry frozen with %s rules", rulesByQuality.size());
    }
    
    /**
     * Check if the registry is frozen.
     */
    public boolean isFrozen() {
        return frozen;
    }
}
