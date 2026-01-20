package reign.software.hyforged.affix.model;

import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Complete definition of an affix that can be rolled on items.
 * <p>
 * Affix definitions are loaded from JSON at {@code Server/Hyforged/Affixes/*.json}.
 * Each definition specifies the stat it modifies, available tiers, eligibility
 * constraints, and rolling weight.
 * <p>
 * This is pure immutable data following ECS principles.
 *
 * @param id Unique identifier (e.g., "sturdy", "of-the-bear")
 * @param type Affix type reference ("prefix", "suffix", "forged")
 * @param displayName Localization key or display name for the affix
 * @param statId The Hyforged stat this affix modifies
 * @param modifierType The modifier stack type (FLAT, INCREASED, MORE)
 * @param tiers List of tier definitions (T1 = best)
 * @param eligibility Constraints for where this affix can appear
 * @param weight Base selection weight for rolling (higher = more likely)
 */
public record AffixDefinition(
    @Nonnull String id,
    @Nonnull String type,
    @Nonnull String displayName,
    @Nonnull StatId statId,
    @Nonnull HyforgedModifier.StackType modifierType,
    @Nonnull List<AffixTierDefinition> tiers,
    @Nonnull AffixEligibility eligibility,
    int weight
) {
    
    /** Default weight for affix selection if not specified */
    public static final int DEFAULT_WEIGHT = 100;
    
    public AffixDefinition {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        Objects.requireNonNull(statId, "statId cannot be null");
        Objects.requireNonNull(modifierType, "modifierType cannot be null");
        Objects.requireNonNull(eligibility, "eligibility cannot be null");
        
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        if (type.isBlank()) {
            throw new IllegalArgumentException("type cannot be blank");
        }
        
        tiers = tiers != null ? List.copyOf(tiers) : Collections.emptyList();
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("Affix must have at least one tier");
        }
        
        if (weight < 0) {
            throw new IllegalArgumentException("weight cannot be negative: " + weight);
        }
    }
    
    /**
     * Get tiers that can be rolled at the given item level.
     *
     * @param itemLevel The item's level
     * @return List of eligible tiers (may be empty)
     */
    @Nonnull
    public List<AffixTierDefinition> getAvailableTiers(int itemLevel) {
        return tiers.stream()
            .filter(tier -> tier.canRollAt(itemLevel))
            .toList();
    }
    
    /**
     * Get the best (lowest tier number) tier available at the given item level.
     *
     * @param itemLevel The item's level
     * @return The best available tier, or empty if none are eligible
     */
    @Nonnull
    public Optional<AffixTierDefinition> getBestAvailableTier(int itemLevel) {
        return tiers.stream()
            .filter(tier -> tier.canRollAt(itemLevel))
            .min((a, b) -> Integer.compare(a.tier(), b.tier()));
    }
    
    /**
     * Get a specific tier by its tier number.
     *
     * @param tierNumber The tier number (1 = best)
     * @return The tier definition, or empty if not found
     */
    @Nonnull
    public Optional<AffixTierDefinition> getTier(int tierNumber) {
        return tiers.stream()
            .filter(tier -> tier.tier() == tierNumber)
            .findFirst();
    }
    
    /**
     * Check if this affix has any tiers available at the given item level.
     */
    public boolean hasAvailableTiers(int itemLevel) {
        return tiers.stream().anyMatch(tier -> tier.canRollAt(itemLevel));
    }
    
    /**
     * Get the total number of tiers defined for this affix.
     */
    public int getTierCount() {
        return tiers.size();
    }
    
    /**
     * Builder for creating AffixDefinition instances.
     */
    public static class Builder {
        private String id;
        private String type;
        private String displayName = "";
        private StatId statId;
        private HyforgedModifier.StackType modifierType = HyforgedModifier.StackType.FLAT;
        private List<AffixTierDefinition> tiers = Collections.emptyList();
        private AffixEligibility eligibility = AffixEligibility.ANY;
        private int weight = DEFAULT_WEIGHT;
        
        public Builder id(@Nonnull String id) {
            this.id = id;
            return this;
        }
        
        public Builder type(@Nonnull String type) {
            this.type = type;
            return this;
        }
        
        public Builder displayName(@Nonnull String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public Builder statId(@Nonnull StatId statId) {
            this.statId = statId;
            return this;
        }
        
        public Builder modifierType(@Nonnull HyforgedModifier.StackType modifierType) {
            this.modifierType = modifierType;
            return this;
        }
        
        public Builder tiers(@Nonnull List<AffixTierDefinition> tiers) {
            this.tiers = tiers;
            return this;
        }
        
        public Builder eligibility(@Nonnull AffixEligibility eligibility) {
            this.eligibility = eligibility;
            return this;
        }
        
        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }
        
        @Nonnull
        public AffixDefinition build() {
            return new AffixDefinition(id, type, displayName, statId, modifierType, tiers, eligibility, weight);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
