package reign.software.hyforged.affix.service;

import reign.software.hyforged.affix.model.*;
import reign.software.hyforged.affix.registry.*;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.logging.Level;
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
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
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
        
        LOGGER.at(Level.FINE).log("Rolling affixes for item: %s, quality: %s, level: %s",
                context.itemId(), context.quality(), context.itemLevel());
        
        // Record metrics
        AffixMetrics.get().recordRollAttempt(context.quality());
        
        // Step 1: Get quality rules to determine capacity
        QualityAffixRule qualityRule = qualityRegistry.getOrEmpty(context.quality());
        if (!qualityRule.allowsAnyAffixes()) {
            LOGGER.at(Level.FINE).log("No affix rules found for quality: %s", context.quality());
            return AffixRollResult.empty(context);
        }
        
        LOGGER.at(Level.FINER).log("Quality capacities: %s", qualityRule.affixCapacity());
        
        // Step 2: Resolve affix pool for this item
        Set<String> categorySet = Set.of(context.itemCategories());
        Set<String> tagSet = Set.of(context.itemTags());
        AffixPool pool = poolRegistry.resolve(categorySet, tagSet);
        if (pool == null) {
            LOGGER.at(Level.FINE).log("No affix pool found for item: %s (categories: %s, tags: %s)",
                    context.itemId(), categorySet, tagSet);
            return AffixRollResult.empty(context);
        }
        
        LOGGER.at(Level.FINE).log("Using affix pool: %s", pool.id());
        
        // Step 3: Get all affix definitions referenced in the pool
        List<AffixDefinition> poolAffixes = resolvePoolAffixes(pool);
        if (poolAffixes.isEmpty()) {
            LOGGER.at(Level.FINE).log("Pool '%s' has no valid affixes", pool.id());
            return AffixRollResult.empty(context);
        }
        
        LOGGER.at(Level.FINER).log("Pool contains %d affix definitions", poolAffixes.size());
        
        // Step 4: Separate by type
        Map<String, List<AffixDefinition>> byType = poolAffixes.stream()
                .collect(Collectors.groupingBy(AffixDefinition::type));
        
        // Step 5: Roll affixes for each type that has capacity
        // usedAffixIds is global (no duplicate affixes)
        // usedStats is also global to prevent same stat being rolled multiple times
        List<RolledAffix> rolledAffixes = new ArrayList<>();
        Set<String> usedAffixIds = new HashSet<>();
        Set<String> usedStats = new HashSet<>();
        
        // Collect all affix types present in either the pool or the quality rule
        Set<String> allTypes = new HashSet<>(pool.getAllAffixTypes());
        allTypes.addAll(qualityRule.affixCapacity().keySet());
        
        // Roll for each type that has both capacity and available affixes
        for (String typeId : allTypes) {
            int capacity = qualityRule.getCapacity(typeId);
            List<AffixDefinition> available = byType.getOrDefault(typeId, Collections.emptyList());
            AffixType affixType = AffixTypeRegistry.get().get(typeId);
            
            LOGGER.at(Level.FINER).log("Type '%s': capacity=%d, available=%d",
                    typeId, capacity, available.size());
            
            rollForType(available, capacity, context, random, rolledAffixes, usedAffixIds, usedStats, affixType);
        }
        
        LOGGER.at(Level.FINE).log("Rolled %d affixes for item %s: %s",
                rolledAffixes.size(), context.itemId(),
                        rolledAffixes.stream().map(RolledAffix::affixId).collect(Collectors.joining(", ")));
        
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
     * Includes standard types (prefix/suffix/forged) AND custom types from the Affixes map.
     */
    private List<AffixDefinition> resolvePoolAffixes(@Nonnull AffixPool pool) {
        List<AffixDefinition> result = new ArrayList<>();
        
        // Collect all affix IDs from all types in the unified map
        for (Map.Entry<String, List<String>> entry : pool.affixesByType().entrySet()) {
            for (String affixId : entry.getValue()) {
                AffixDefinition def = affixRegistry.get(affixId);
                if (def != null) {
                    result.add(def);
                } else {
                    LOGGER.atWarning().log("Pool '%s' references affix '%s' (type '%s') which does not exist in the registry. "
                            + "Check that the pool ID matches the definition 'Id' field exactly (case-sensitive).",
                            pool.id(), affixId, entry.getKey());
                }
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
            LOGGER.at(Level.FINEST).log("Skipping roll - available: %d, capacity: %d",
                    available.size(), capacity);
            return;
        }
        
        String type = available.isEmpty() ? "unknown" : available.get(0).type();
        boolean isStackable = affixType != null && affixType.stackable();
        LOGGER.at(Level.FINER).log("Rolling %d %s slots from %d candidates (stackable: %b)",
                capacity, type, available.size(), isStackable);
        
        for (int i = 0; i < capacity; i++) {
            // Filter out already used affixes and affixes that share stats with already-used ones
            List<AffixDefinition> candidates = available.stream()
                    .filter(a -> !usedAffixIds.contains(a.id()))
                    .filter(a -> a.getStatIds().stream().noneMatch(usedStats::contains))
                    .collect(Collectors.toList());
            
            if (candidates.isEmpty()) {
                LOGGER.at(Level.FINEST).log("No more candidates for slot %d", i);
                break;
            }
            
            // Weighted random selection
            AffixDefinition selected = weightedSelect(candidates, random);
            if (selected == null) {
                break;
            }
            
            LOGGER.at(Level.FINEST).log("Selected affix: %s (weight: %d)",
                    selected.id(), selected.weight());
            
            // Roll tier
            AffixTierDefinition tier = rollTier(selected, context, random);
            if (tier == null) {
                // No eligible tiers for this item level
                LOGGER.at(Level.FINEST).log("No eligible tiers for affix %s at level %d",
                        selected.id(), context.itemLevel());
                continue;
            }
            
            // Roll values for each stat in the tier
            Map<String, RolledAffix.RolledStat> rolledStats = rollAllStats(tier, random);
            
            LOGGER.at(Level.FINEST).log("Rolled %s tier %d with %d stats",
                    selected.id(), tier.tier(), rolledStats.size());
            
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
