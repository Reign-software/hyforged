package reign.software.hyforged.affix;

import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.AffixTierDefinition;
import reign.software.hyforged.affix.model.AffixTierStat;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test fixture helpers for creating AffixDefinition instances.
 * <p>
 * Provides convenient factory methods for common test patterns.
 */
public final class AffixTestFixtures {
    
    private AffixTestFixtures() {}
    
    /**
     * Create a simple single-stat affix for testing.
     *
     * @param id The affix ID
     * @param type The affix type (prefix, suffix, forged)
     * @param displayName The display name
     * @param statId The stat to modify
     * @param stackType The modifier stack type
     * @param tiers The tier definitions
     * @param weight The selection weight
     * @return A new AffixDefinition
     */
    public static AffixDefinition createSingleStat(
            String id,
            String type,
            String displayName,
            StatId statId,
            HyforgedModifier.StackType stackType,
            List<AffixTierDefinition> tiers,
            int weight
    ) {
        return new AffixDefinition(
                id,
                type,
                displayName,
                tiers,
                weight
        );
    }
    
    /**
     * Create a simple single-stat affix with default weight (100).
     */
    public static AffixDefinition createSingleStat(
            String id,
            String type,
            String displayName,
            StatId statId,
            HyforgedModifier.StackType stackType,
            List<AffixTierDefinition> tiers
    ) {
        return createSingleStat(id, type, displayName, statId, stackType, tiers, 100);
    }
    
    /**
     * Create a prefix affix with a single stat.
     */
    public static AffixDefinition prefix(
            String id,
            String displayName,
            StatId statId,
            HyforgedModifier.StackType stackType,
            List<AffixTierDefinition> tiers
    ) {
        return createSingleStat(id, "prefix", displayName, statId, stackType, tiers);
    }
    
    /**
     * Create a suffix affix with a single stat.
     */
    public static AffixDefinition suffix(
            String id,
            String displayName,
            StatId statId,
            HyforgedModifier.StackType stackType,
            List<AffixTierDefinition> tiers
    ) {
        return createSingleStat(id, "suffix", displayName, statId, stackType, tiers);
    }
    
    /**
     * Create a single-stat tier with the new format.
     *
     * @param tierNum The tier number (1 = best)
     * @param itemLevelReq Minimum item level required
     * @param weight Selection weight
     * @param statId The stat ID
     * @param stackType The stack type
     * @param minValue Minimum rolled value
     * @param maxValue Maximum rolled value
     * @return A new AffixTierDefinition
     */
    public static AffixTierDefinition tier(
            int tierNum,
            int itemLevelReq,
            int weight,
            String statId,
            HyforgedModifier.StackType stackType,
            int minValue,
            int maxValue
    ) {
        Map<String, AffixTierStat> stats = new HashMap<>();
        stats.put(statId, new AffixTierStat(StatId.parse(statId), stackType, minValue, maxValue));
        return new AffixTierDefinition(tierNum, itemLevelReq, weight, stats);
    }
    
    /**
     * Create a single-stat tier with default weight.
     */
    public static AffixTierDefinition tier(
            int tierNum,
            int itemLevelReq,
            String statId,
            HyforgedModifier.StackType stackType,
            int minValue,
            int maxValue
    ) {
        return tier(tierNum, itemLevelReq, 100, statId, stackType, minValue, maxValue);
    }
    
    /**
     * Create standard test tiers (T1-T3) for a single stat.
     *
     * @param statId The stat ID for all tiers
     * @param stackType The stack type for all tiers
     * @return List of 3 tiers with standard values
     */
    public static List<AffixTierDefinition> standardTiers(String statId, HyforgedModifier.StackType stackType) {
        return List.of(
                tier(1, 50, 50, statId, stackType, 80, 100),
                tier(2, 25, 100, statId, stackType, 50, 79),
                tier(3, 1, 150, statId, stackType, 20, 49)
        );
    }
    
    /**
     * Create standard test tiers (T1-T3) with custom value ranges.
     *
     * @param statId The stat ID for all tiers
     * @param stackType The stack type for all tiers
     * @param t1Min T1 minimum value
     * @param t1Max T1 maximum value
     * @param t2Min T2 minimum value
     * @param t2Max T2 maximum value
     * @param t3Min T3 minimum value
     * @param t3Max T3 maximum value
     * @return List of 3 tiers with the specified values
     */
    public static List<AffixTierDefinition> tiers(
            String statId,
            HyforgedModifier.StackType stackType,
            int t1Min, int t1Max, 
            int t2Min, int t2Max, 
            int t3Min, int t3Max
    ) {
        return List.of(
                tier(1, 50, 50, statId, stackType, t1Min, t1Max),
                tier(2, 25, 100, statId, stackType, t2Min, t2Max),
                tier(3, 1, 150, statId, stackType, t3Min, t3Max)
        );
    }
    
    /**
     * Create a multi-stat tier for testing.
     *
     * @param tierNum The tier number
     * @param itemLevelReq Minimum item level required
     * @param weight Selection weight
     * @param stats Map of stat ID to AffixTierStat
     * @return A new AffixTierDefinition
     */
    public static AffixTierDefinition multiStatTier(
            int tierNum,
            int itemLevelReq,
            int weight,
            Map<String, AffixTierStat> stats
    ) {
        return new AffixTierDefinition(tierNum, itemLevelReq, weight, stats);
    }
}
