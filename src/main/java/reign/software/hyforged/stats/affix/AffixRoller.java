package reign.software.hyforged.stats.affix;

import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Utility class for rolling item affixes.
 * <p>
 * Handles:
 * - Weighted tier selection based on item rarity
 * - Uniqueness enforcement (one affix per stat per item)
 * - Forged line rolling with expanded pools and tier bonuses
 * <p>
 * This is a pure computation utility - no state, following ECS principles.
 */
public final class AffixRoller {
    
    private AffixRoller() {} // Static utility class
    
    /**
     * Roll an affix from a pool of available affixes.
     * <p>
     * Selects an affix using weighted random selection based on item rarity,
     * then rolls a tier and value for the selected affix.
     *
     * @param pool Available affixes to roll from
     * @param itemLevel The item's level (affects available tiers)
     * @param itemRarity The item's rarity (e.g., "common", "rare", "legendary")
     * @param excludedStats Stats that cannot be rolled (for uniqueness)
     * @param random Random number generator
     * @return The rolled affix result, or null if no valid affix could be rolled
     */
    @Nullable
    public static AffixRollResult rollAffix(
            @Nonnull List<AffixMetadata> pool,
            int itemLevel,
            @Nonnull String itemRarity,
            @Nonnull Set<StatId> excludedStats,
            @Nonnull Random random
    ) {
        return rollAffix(pool, itemLevel, itemRarity, excludedStats, false, random);
    }
    
    /**
     * Roll an affix with optional forged bonus.
     *
     * @param pool Available affixes to roll from
     * @param itemLevel The item's level
     * @param itemRarity The item's rarity
     * @param excludedStats Stats that cannot be rolled
     * @param forged Whether this is a forged line roll (applies tier bonus)
     * @param random Random number generator
     * @return The rolled affix result, or null if no valid affix could be rolled
     */
    @Nullable
    public static AffixRollResult rollAffix(
            @Nonnull List<AffixMetadata> pool,
            int itemLevel,
            @Nonnull String itemRarity,
            @Nonnull Set<StatId> excludedStats,
            boolean forged,
            @Nonnull Random random
    ) {
        // Filter pool to eligible affixes
        List<AffixMetadata> eligible = new ArrayList<>();
        for (AffixMetadata affix : pool) {
            if (excludedStats.contains(affix.statId())) {
                continue; // Already have this stat
            }
            if (forged && !affix.forgedEligible()) {
                continue; // Not eligible for forged lines
            }
            List<AffixTier> tiers = affix.getAvailableTiers(itemLevel);
            if (!tiers.isEmpty()) {
                eligible.add(affix);
            }
        }
        
        if (eligible.isEmpty()) {
            return null;
        }
        
        // Calculate total weight
        int totalWeight = 0;
        int[] weights = new int[eligible.size()];
        for (int i = 0; i < eligible.size(); i++) {
            AffixMetadata affix = eligible.get(i);
            List<AffixTier> tiers = affix.getAvailableTiers(itemLevel);
            // Use the highest tier's weight for this rarity
            int maxWeight = 0;
            for (AffixTier tier : tiers) {
                int w = tier.getWeight(itemRarity);
                if (w > maxWeight) maxWeight = w;
            }
            weights[i] = Math.max(1, maxWeight); // Minimum weight of 1
            totalWeight += weights[i];
        }
        
        // Select affix using weighted random
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        AffixMetadata selected = null;
        for (int i = 0; i < eligible.size(); i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                selected = eligible.get(i);
                break;
            }
        }
        
        if (selected == null) {
            selected = eligible.get(eligible.size() - 1); // Fallback
        }
        
        // Roll tier
        AffixTier tier = rollTier(selected, itemLevel, itemRarity, forged, random);
        if (tier == null) {
            return null;
        }
        
        // Roll value
        int value = tier.rollValue(random.nextDouble());
        
        // Determine if prefix or suffix
        boolean asPrefix = selected.prefix() && (!selected.suffix() || random.nextBoolean());
        
        return new AffixRollResult(
            selected.statId(),
            selected.getDisplayName(asPrefix),
            asPrefix,
            tier.tier(),
            value,
            forged
        );
    }
    
    /**
     * Roll a tier for an affix.
     */
    @Nullable
    public static AffixTier rollTier(
            @Nonnull AffixMetadata affix,
            int itemLevel,
            @Nonnull String itemRarity,
            boolean forged,
            @Nonnull Random random
    ) {
        List<AffixTier> available = affix.getAvailableTiers(itemLevel);
        if (available.isEmpty()) {
            return null;
        }
        
        // If forged, try to get a higher tier
        if (forged && affix.forgedTierBonus() > 0) {
            int targetTier = available.get(available.size() - 1).tier() + affix.forgedTierBonus();
            Optional<AffixTier> boosted = affix.getTier(targetTier);
            if (boosted.isPresent()) {
                return boosted.get();
            }
            // If boosted tier doesn't exist, use highest available
            return available.get(available.size() - 1);
        }
        
        // Calculate weights for available tiers
        int totalWeight = 0;
        int[] weights = new int[available.size()];
        for (int i = 0; i < available.size(); i++) {
            weights[i] = Math.max(1, available.get(i).getWeight(itemRarity));
            totalWeight += weights[i];
        }
        
        // Roll
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < available.size(); i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                return available.get(i);
            }
        }
        
        return available.get(available.size() - 1); // Fallback
    }
    
    /**
     * Roll a forged line for an item.
     * <p>
     * Forged lines use an expanded pool and apply tier bonuses.
     *
     * @param pool Available affixes (forged-eligible ones will be filtered)
     * @param itemLevel The item's level
     * @param itemRarity The item's rarity
     * @param existingStats Stats already on the item (for uniqueness)
     * @param random Random number generator
     * @return The rolled forged affix, or null if none available
     */
    @Nullable
    public static AffixRollResult rollForgedLine(
            @Nonnull List<AffixMetadata> pool,
            int itemLevel,
            @Nonnull String itemRarity,
            @Nonnull Set<StatId> existingStats,
            @Nonnull Random random
    ) {
        return rollAffix(pool, itemLevel, itemRarity, existingStats, true, random);
    }
    
    /**
     * Roll multiple affixes for an item.
     * <p>
     * Enforces uniqueness - no stat can appear more than once.
     *
     * @param pool Available affixes
     * @param count Number of affixes to roll
     * @param itemLevel The item's level
     * @param itemRarity The item's rarity
     * @param random Random number generator
     * @return List of rolled affixes (may be fewer than requested if pool exhausted)
     */
    @Nonnull
    public static List<AffixRollResult> rollAffixes(
            @Nonnull List<AffixMetadata> pool,
            int count,
            int itemLevel,
            @Nonnull String itemRarity,
            @Nonnull Random random
    ) {
        List<AffixRollResult> results = new ArrayList<>();
        Set<StatId> used = new HashSet<>();
        
        for (int i = 0; i < count; i++) {
            AffixRollResult result = rollAffix(pool, itemLevel, itemRarity, used, random);
            if (result == null) {
                break; // No more valid affixes
            }
            results.add(result);
            used.add(result.statId());
        }
        
        return results;
    }
    
    /**
     * Result of rolling an affix.
     *
     * @param statId The stat being modified
     * @param displayName The display name for the affix
     * @param isPrefix Whether this is a prefix (vs suffix)
     * @param tier The tier that was rolled
     * @param value The value that was rolled
     * @param forged Whether this is a forged line
     */
    public record AffixRollResult(
        @Nonnull StatId statId,
        @Nonnull String displayName,
        boolean isPrefix,
        int tier,
        int value,
        boolean forged
    ) {}
}
