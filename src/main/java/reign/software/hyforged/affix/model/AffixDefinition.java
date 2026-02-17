package reign.software.hyforged.affix.model;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Complete definition of an affix that can be rolled on items.
 * <p>
 * Affix definitions are loaded from JSON at {@code Server/Hyforged/Affixes/Definitions/<Type>/*.json}.
 * Each tier specifies the stat modifiers it grants with their value ranges.
 * <p>
 * Pool targeting is handled by {@link AffixPool}, allowing the same affix
 * to appear in multiple pools with different item targeting.
 * <p>
 * Each tier can grant multiple stats with individual value ranges. For example:
 * <pre>
 * "of the Titan" T1 might grant:
 *   +45-55 Strength (FLAT)
 *   +100-150 Max Health (FLAT)
 * </pre>
 * <p>
 * This is pure immutable data following ECS principles.
 *
 * @param id Unique identifier (e.g., "hyforged:sturdy", "hyforged:of-the-bear")
 * @param type Affix type reference ("prefix", "suffix", "forged")
 * @param displayName Localization key or display name for the affix
 * @param tiers List of tier definitions (T1 = best), each containing stats with value ranges
 * @param triggeredEffects List of triggered effect definitions (optional)
 * @param weight Base selection weight for rolling (higher = more likely)
 */
public record AffixDefinition(
    @Nonnull String id,
    @Nonnull String type,
    @Nonnull String displayName,
    @Nonnull List<AffixTierDefinition> tiers,
    @Nonnull List<AffixTriggeredEffect> triggeredEffects,
    int weight
) {
    
    /** Default weight for affix selection if not specified */
    public static final int DEFAULT_WEIGHT = 100;
    
    public AffixDefinition {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        
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
        boolean hasStatModifiers = tiers.stream().anyMatch(tier -> !tier.stats().isEmpty());
        boolean hasTriggeredEffects = triggeredEffects != null && !triggeredEffects.isEmpty();
        if (!hasStatModifiers && !hasTriggeredEffects) {
            throw new IllegalArgumentException("Affix must have stat modifiers or triggered effects");
        }
        
        if (weight < 0) {
            throw new IllegalArgumentException("weight cannot be negative: " + weight);
        }

        triggeredEffects = triggeredEffects != null ? List.copyOf(triggeredEffects) : Collections.emptyList();
    }

    /**
     * Backwards-compatible constructor for stat-only affixes.
     */
    public AffixDefinition(
            @Nonnull String id,
            @Nonnull String type,
            @Nonnull String displayName,
            @Nonnull List<AffixTierDefinition> tiers,
            int weight
    ) {
        this(id, type, displayName, tiers, Collections.emptyList(), weight);
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
     * Get all stat IDs that this affix can modify (across all tiers).
     * <p>
     * All tiers should grant the same stats, but this collects from all tiers
     * for safety.
     *
     * @return Set of all stat IDs this affix can grant
     */
    @Nonnull
    public Set<String> getStatIds() {
        return tiers.stream()
            .flatMap(tier -> tier.stats().keySet().stream())
            .collect(Collectors.toSet());
    }

    /**
     * Check if this affix has any triggered effects.
     */
    public boolean hasTriggeredEffects() {
        return triggeredEffects != null && !triggeredEffects.isEmpty();
    }
    
    /**
     * Check if this affix modifies a specific stat (in any tier).
     *
     * @param statId The stat ID to check (e.g., "hyforged:strength")
     * @return true if any tier grants this stat
     */
    public boolean modifiesStat(@Nonnull String statId) {
        return tiers.stream().anyMatch(tier -> tier.grantsStat(statId));
    }
    
    /**
     * Get the number of stats granted per tier.
     * <p>
     * Uses the first tier as reference - all tiers should have the same stats.
     */
    public int getStatCount() {
        return tiers.isEmpty() ? 0 : tiers.get(0).getStatCount();
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    /**
     * Builder for creating AffixDefinition instances.
     */
    public static class Builder {
        private String id;
        private String type;
        private String displayName = "";
        private List<AffixTierDefinition> tiers = Collections.emptyList();
        private List<AffixTriggeredEffect> triggeredEffects = Collections.emptyList();
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
        
        public Builder tiers(@Nonnull List<AffixTierDefinition> tiers) {
            this.tiers = tiers;
            return this;
        }

        public Builder triggeredEffects(@Nonnull List<AffixTriggeredEffect> triggeredEffects) {
            this.triggeredEffects = triggeredEffects;
            return this;
        }
        
        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }
        
        @Nonnull
        public AffixDefinition build() {
            return new AffixDefinition(id, type, displayName, tiers, triggeredEffects, weight);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
