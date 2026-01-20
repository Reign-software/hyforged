package reign.software.hyforged.stats.affix;

import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Metadata describing how a stat can appear as an item affix.
 * <p>
 * Affixes can be:
 * - Prefixes: Appear before the item name (e.g., "Sturdy Sword")
 * - Suffixes: Appear after the item name (e.g., "Sword of the Bear")
 * <p>
 * This is pure data - no behavior, following ECS principles.
 * 
 * @param statId The stat this affix modifies
 * @param prefix Whether this can appear as a prefix
 * @param suffix Whether this can appear as a suffix
 * @param prefixName Display name when used as prefix (e.g., "Sturdy")
 * @param suffixName Display name when used as suffix (e.g., "of the Bear")
 * @param eligibleSlots Set of equipment slot IDs where this affix can appear
 * @param tiers List of tier definitions for this affix
 * @param forgedEligible Whether this can appear as a forged line
 * @param forgedTierBonus Bonus tiers added when rolled as a forged line
 */
public record AffixMetadata(
    @Nonnull StatId statId,
    boolean prefix,
    boolean suffix,
    @Nonnull String prefixName,
    @Nonnull String suffixName,
    @Nonnull Set<String> eligibleSlots,
    @Nonnull List<AffixTier> tiers,
    boolean forgedEligible,
    int forgedTierBonus
) {
    
    public AffixMetadata {
        Objects.requireNonNull(statId, "statId cannot be null");
        Objects.requireNonNull(prefixName, "prefixName cannot be null");
        Objects.requireNonNull(suffixName, "suffixName cannot be null");
        Objects.requireNonNull(eligibleSlots, "eligibleSlots cannot be null");
        Objects.requireNonNull(tiers, "tiers cannot be null");
        
        if (!prefix && !suffix) {
            throw new IllegalArgumentException("Affix must be either prefix or suffix (or both)");
        }
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("Affix must have at least one tier");
        }
        
        // Make defensive copies
        eligibleSlots = Set.copyOf(eligibleSlots);
        tiers = List.copyOf(tiers);
    }
    
    /**
     * Check if this affix can appear on a specific equipment slot.
     */
    public boolean canAppearOnSlot(@Nonnull String slotId) {
        return eligibleSlots.isEmpty() || eligibleSlots.contains(slotId);
    }
    
    /**
     * Get the display name for this affix based on whether it's a prefix or suffix.
     */
    @Nonnull
    public String getDisplayName(boolean asPrefix) {
        return asPrefix ? prefixName : suffixName;
    }
    
    /**
     * Get tiers that can be rolled at the given item level.
     */
    @Nonnull
    public List<AffixTier> getAvailableTiers(int itemLevel) {
        List<AffixTier> available = new ArrayList<>();
        for (AffixTier tier : tiers) {
            if (tier.canRollAt(itemLevel)) {
                available.add(tier);
            }
        }
        return available;
    }
    
    /**
     * Get the highest tier available at the given item level.
     */
    @Nonnull
    public Optional<AffixTier> getHighestTier(int itemLevel) {
        AffixTier highest = null;
        for (AffixTier tier : tiers) {
            if (tier.canRollAt(itemLevel)) {
                if (highest == null || tier.tier() > highest.tier()) {
                    highest = tier;
                }
            }
        }
        return Optional.ofNullable(highest);
    }
    
    /**
     * Get a specific tier by tier number.
     */
    @Nonnull
    public Optional<AffixTier> getTier(int tierNumber) {
        for (AffixTier tier : tiers) {
            if (tier.tier() == tierNumber) {
                return Optional.of(tier);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Builder for creating AffixMetadata instances.
     */
    public static class Builder {
        private StatId statId;
        private boolean prefix = false;
        private boolean suffix = false;
        private String prefixName = "";
        private String suffixName = "";
        private Set<String> eligibleSlots = new HashSet<>();
        private List<AffixTier> tiers = new ArrayList<>();
        private boolean forgedEligible = true;
        private int forgedTierBonus = 1;
        
        public Builder(@Nonnull StatId statId) {
            this.statId = Objects.requireNonNull(statId);
        }
        
        public Builder prefix(@Nonnull String name) {
            this.prefix = true;
            this.prefixName = name;
            return this;
        }
        
        public Builder suffix(@Nonnull String name) {
            this.suffix = true;
            this.suffixName = name;
            return this;
        }
        
        public Builder eligibleSlot(@Nonnull String slot) {
            this.eligibleSlots.add(slot);
            return this;
        }
        
        public Builder eligibleSlots(@Nonnull Set<String> slots) {
            this.eligibleSlots.addAll(slots);
            return this;
        }
        
        public Builder addTier(@Nonnull AffixTier tier) {
            this.tiers.add(tier);
            return this;
        }
        
        public Builder forgedEligible(boolean eligible) {
            this.forgedEligible = eligible;
            return this;
        }
        
        public Builder forgedTierBonus(int bonus) {
            this.forgedTierBonus = bonus;
            return this;
        }
        
        public AffixMetadata build() {
            return new AffixMetadata(
                statId, prefix, suffix, prefixName, suffixName,
                eligibleSlots, tiers, forgedEligible, forgedTierBonus
            );
        }
    }
    
    public static Builder builder(@Nonnull StatId statId) {
        return new Builder(statId);
    }
}
