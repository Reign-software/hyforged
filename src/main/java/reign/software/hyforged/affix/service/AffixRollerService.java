package reign.software.hyforged.affix.service;

import reign.software.hyforged.affix.model.*;
import reign.software.hyforged.affix.registry.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for rolling affixes on items.
 * <p>
 * This implements the core affix rolling algorithm:
 * <ol>
 *   <li>Determine affix capacity from quality tier</li>
 *   <li>Resolve affix pool from item category/tags</li>
 *   <li>Filter tiers by item level</li>
 *   <li>Perform weighted random selection for each slot</li>
 *   <li>Roll tier and value for each selected affix</li>
 * </ol>
 * <p>
 * Supports deterministic rolling via seed for debugging and testing.
 */
public final class AffixRollerService {
    
    private static final Logger LOGGER = Logger.getLogger(AffixRollerService.class.getName());
    
    private final AffixDefinitionRegistry affixRegistry;
    private final AffixPoolRegistry poolRegistry;
    private final QualityAffixRuleRegistry qualityRegistry;
    
    /**
     * Create an AffixRollerService with the default singleton registries.
     */
    public AffixRollerService() {
        this(
            AffixDefinitionRegistry.get(),
            AffixPoolRegistry.get(),
            QualityAffixRuleRegistry.get()
        );
    }
    
    /**
     * Create an AffixRollerService with custom registries (for testing).
     */
    public AffixRollerService(
            @Nonnull AffixDefinitionRegistry affixRegistry,
            @Nonnull AffixPoolRegistry poolRegistry,
            @Nonnull QualityAffixRuleRegistry qualityRegistry
    ) {
        this.affixRegistry = Objects.requireNonNull(affixRegistry, "affixRegistry cannot be null");
        this.poolRegistry = Objects.requireNonNull(poolRegistry, "poolRegistry cannot be null");
        this.qualityRegistry = Objects.requireNonNull(qualityRegistry, "qualityRegistry cannot be null");
    }
    
    /**
     * Roll affixes for an item using a random seed.
     *
     * @param context The roll context containing item properties
     * @return Result containing the rolled affixes
     */
    @Nonnull
    public AffixRollResult rollAffixes(@Nonnull AffixRollContext context) {
        return rollAffixes(context, new Random());
    }
    
    /**
     * Roll affixes for an item with a specific seed (deterministic).
     *
     * @param context The roll context containing item properties
     * @param seed    The random seed for deterministic rolling
     * @return Result containing the rolled affixes
     */
    @Nonnull
    public AffixRollResult rollAffixes(@Nonnull AffixRollContext context, long seed) {
        return rollAffixes(context, new Random(seed));
    }
    
    /**
     * Roll affixes for an item with a provided Random instance.
     *
     * @param context The roll context containing item properties
     * @param random  The random number generator
     * @return Result containing the rolled affixes
     */
    @Nonnull
    public AffixRollResult rollAffixes(@Nonnull AffixRollContext context, @Nonnull Random random) {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(random, "random cannot be null");
        
        LOGGER.log(Level.FINE, "Rolling affixes for item: {0}, quality: {1}, level: {2}",
                new Object[]{context.itemId(), context.quality(), context.itemLevel()});
        
        // Record metrics
        AffixMetrics.get().recordRollAttempt(context.quality());
        
        // Step 1: Get quality rules to determine capacity
        QualityAffixRule qualityRule = qualityRegistry.getOrEmpty(context.quality());
        if (!qualityRule.allowsAnyAffixes()) {
            LOGGER.log(Level.FINE, "No affix rules found for quality: {0}", context.quality());
            return AffixRollResult.empty(context);
        }
        
        int prefixCapacity = qualityRule.getCapacity("prefix");
        int suffixCapacity = qualityRule.getCapacity("suffix");
        int forgedCapacity = qualityRule.getCapacity("forged");
        
        LOGGER.log(Level.FINER, "Quality capacities - prefix: {0}, suffix: {1}, forged: {2}",
                new Object[]{prefixCapacity, suffixCapacity, forgedCapacity});
        
        // Step 2: Resolve affix pool for this item
        Set<String> categorySet = Set.of(context.itemCategories());
        Set<String> tagSet = Set.of(context.itemTags());
        AffixPool pool = poolRegistry.resolve(categorySet, tagSet);
        if (pool == null) {
            LOGGER.log(Level.FINE, "No affix pool found for item: {0} (categories: {1}, tags: {2})", 
                    new Object[]{context.itemId(), categorySet, tagSet});
            return AffixRollResult.empty(context);
        }
        
        LOGGER.log(Level.FINE, "Using affix pool: {0}", pool.id());
        
        // Step 3: Get all affix definitions referenced in the pool
        List<AffixDefinition> poolAffixes = resolvePoolAffixes(pool);
        if (poolAffixes.isEmpty()) {
            LOGGER.log(Level.FINE, "Pool '{0}' has no valid affixes", pool.id());
            return AffixRollResult.empty(context);
        }
        
        LOGGER.log(Level.FINER, "Pool contains {0} affix definitions", poolAffixes.size());
        
        // Step 4: Separate by type
        Map<String, List<AffixDefinition>> byType = poolAffixes.stream()
                .collect(Collectors.groupingBy(AffixDefinition::type));
        
        List<AffixDefinition> prefixes = byType.getOrDefault("prefix", Collections.emptyList());
        List<AffixDefinition> suffixes = byType.getOrDefault("suffix", Collections.emptyList());
        List<AffixDefinition> forged = byType.getOrDefault("forged", Collections.emptyList());
        
        // Step 6: Roll affixes for each slot type
        // Note: usedAffixIds is global (no duplicate affixes)
        // usedStats is also global to prevent same stat being rolled multiple times
        List<RolledAffix> rolledAffixes = new ArrayList<>();
        Set<String> usedAffixIds = new HashSet<>();
        Set<String> usedStats = new HashSet<>();
        
        // Roll prefixes
        AffixType prefixType = AffixTypeRegistry.get().get("prefix");
        rollForType(prefixes, prefixCapacity, context, random, rolledAffixes, usedAffixIds, usedStats, prefixType);
        
        // Roll suffixes
        AffixType suffixType = AffixTypeRegistry.get().get("suffix");
        rollForType(suffixes, suffixCapacity, context, random, rolledAffixes, usedAffixIds, usedStats, suffixType);
        
        // Roll forged (typically non-stackable)
        AffixType forgedType = AffixTypeRegistry.get().get("forged");
        rollForType(forged, forgedCapacity, context, random, rolledAffixes, usedAffixIds, usedStats, forgedType);
        
        LOGGER.log(Level.FINE, "Rolled {0} affixes for item {1}: {2}", 
                new Object[]{rolledAffixes.size(), context.itemId(), 
                        rolledAffixes.stream().map(RolledAffix::affixId).collect(Collectors.joining(", "))});
        
        // Record success metrics
        if (rolledAffixes.isEmpty()) {
            AffixMetrics.get().recordRollFailure();
        } else {
            AffixMetrics.get().recordRollSuccess(rolledAffixes);
        }
        
        return new AffixRollResult(context, pool.id(), rolledAffixes);
    }
    
    /**
     * Resolve all affix definitions from a pool.
     */
    private List<AffixDefinition> resolvePoolAffixes(@Nonnull AffixPool pool) {
        List<AffixDefinition> result = new ArrayList<>();
        
        // Collect all affix IDs from all types
        List<String> allAffixIds = new ArrayList<>();
        allAffixIds.addAll(pool.prefixes());
        allAffixIds.addAll(pool.suffixes());
        allAffixIds.addAll(pool.forged());
        
        for (String affixId : allAffixIds) {
            AffixDefinition def = affixRegistry.get(affixId);
            if (def != null) {
                result.add(def);
            }
        }
        return result;
    }
    
    /**
     * Roll affixes for a specific type (prefix/suffix/forged).
     * 
     * @param affixType The AffixType definition, used to determine stackable behavior.
     *                  If null or stackable=false, duplicate stats are excluded.
     */
    private void rollForType(
            @Nonnull List<AffixDefinition> available,
            int capacity,
            @Nonnull AffixRollContext context,
            @Nonnull Random random,
            @Nonnull List<RolledAffix> output,
            @Nonnull Set<String> usedAffixIds,
            @Nonnull Set<String> usedStats,
            AffixType affixType
    ) {
        if (available.isEmpty() || capacity <= 0) {
            LOGGER.log(Level.FINEST, "Skipping roll - available: {0}, capacity: {1}", 
                    new Object[]{available.size(), capacity});
            return;
        }
        
        String type = available.isEmpty() ? "unknown" : available.get(0).type();
        boolean isStackable = affixType != null && affixType.stackable();
        LOGGER.log(Level.FINER, "Rolling {0} {1} slots from {2} candidates (stackable: {3})", 
                new Object[]{capacity, type, available.size(), isStackable});
        
        for (int i = 0; i < capacity; i++) {
            // Filter out already used affixes and affixes that share stats with already-used ones
            List<AffixDefinition> candidates = available.stream()
                    .filter(a -> !usedAffixIds.contains(a.id()))
                    .filter(a -> a.getStatIds().stream().noneMatch(usedStats::contains))
                    .collect(Collectors.toList());
            
            if (candidates.isEmpty()) {
                LOGGER.log(Level.FINEST, "No more candidates for slot {0}", i);
                break;
            }
            
            // Weighted random selection
            AffixDefinition selected = weightedSelect(candidates, random);
            if (selected == null) {
                break;
            }
            
            LOGGER.log(Level.FINEST, "Selected affix: {0} (weight: {1})", 
                    new Object[]{selected.id(), selected.weight()});
            
            // Roll tier
            AffixTierDefinition tier = rollTier(selected, context, random);
            if (tier == null) {
                // No eligible tiers for this item level
                LOGGER.log(Level.FINEST, "No eligible tiers for affix {0} at level {1}", 
                        new Object[]{selected.id(), context.itemLevel()});
                continue;
            }
            
            // Roll values for each stat in the tier
            Map<String, RolledAffix.RolledStat> rolledStats = rollAllStats(tier, random);
            
            LOGGER.log(Level.FINEST, "Rolled {0} tier {1} with {2} stats", 
                    new Object[]{selected.id(), tier.tier(), rolledStats.size()});
            
            // Create rolled affix
            RolledAffix rolled = RolledAffix.from(selected, tier.tier(), rolledStats);
            output.add(rolled);
            usedAffixIds.add(selected.id());
            // Track all stats this affix grants
            usedStats.addAll(selected.getStatIds());
        }
    }
    
    /**
     * Roll values for all stats in a tier.
     */
    @Nonnull
    private Map<String, RolledAffix.RolledStat> rollAllStats(
            @Nonnull AffixTierDefinition tier, 
            @Nonnull Random random
    ) {
        Map<String, RolledAffix.RolledStat> result = new HashMap<>();
        for (Map.Entry<String, AffixTierStat> entry : tier.stats().entrySet()) {
            String statId = entry.getKey();
            AffixTierStat tierStat = entry.getValue();
            int rolledValue = rollStatValue(tierStat, random);
            result.put(statId, new RolledAffix.RolledStat(rolledValue, tierStat.stackType()));
        }
        return result;
    }
    
    /**
     * Roll a value for a single stat within its min/max range.
     */
    private int rollStatValue(@Nonnull AffixTierStat stat, @Nonnull Random random) {
        int min = stat.minValue();
        int max = stat.maxValue();
        
        if (min == max) {
            return min;
        }
        
        // Inclusive range: [min, max]
        return min + random.nextInt(max - min + 1);
    }
    
    /**
     * Weighted random selection from candidates.
     */
    private AffixDefinition weightedSelect(
            @Nonnull List<AffixDefinition> candidates,
            @Nonnull Random random
    ) {
        if (candidates.isEmpty()) {
            return null;
        }
        
        int totalWeight = candidates.stream().mapToInt(AffixDefinition::weight).sum();
        if (totalWeight <= 0) {
            // Fallback to uniform selection
            return candidates.get(random.nextInt(candidates.size()));
        }
        
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (AffixDefinition candidate : candidates) {
            cumulative += candidate.weight();
            if (roll < cumulative) {
                return candidate;
            }
        }
        
        // Shouldn't reach here, but return last as fallback
        return candidates.get(candidates.size() - 1);
    }
    
    /**
     * Roll a tier from the affix definition based on item level.
     */
    private AffixTierDefinition rollTier(
            @Nonnull AffixDefinition affix,
            @Nonnull AffixRollContext context,
            @Nonnull Random random
    ) {
        // Filter tiers by item level
        List<AffixTierDefinition> eligibleTiers = affix.tiers().stream()
                .filter(t -> context.itemLevel() >= t.itemLevelReq())
                .collect(Collectors.toList());
        
        if (eligibleTiers.isEmpty()) {
            return null;
        }
        
        // Apply tier weight bonus (positive = better tiers more likely)
        // Better tiers have lower tier numbers, so we boost weight for lower numbers
        int totalWeight = 0;
        int[] adjustedWeights = new int[eligibleTiers.size()];
        for (int i = 0; i < eligibleTiers.size(); i++) {
            AffixTierDefinition tier = eligibleTiers.get(i);
            // Bonus is applied inversely to tier number (T1 gets most bonus)
            int tierBonus = context.tierWeightBonus() * (eligibleTiers.size() - i);
            int weight = Math.max(1, tier.weight() + tierBonus);
            adjustedWeights[i] = weight;
            totalWeight += weight;
        }
        
        if (totalWeight <= 0) {
            return eligibleTiers.get(0);
        }
        
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < eligibleTiers.size(); i++) {
            cumulative += adjustedWeights[i];
            if (roll < cumulative) {
                return eligibleTiers.get(i);
            }
        }
        
        return eligibleTiers.get(eligibleTiers.size() - 1);
    }
}
