package reign.software.hyforged.stats.breakdown;

import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Complete breakdown of a stat's computed value for UI display.
 * <p>
 * Shows the base value, scaling contributions, all modifiers, intermediate values,
 * and final result.
 * <p>
 * This is pure data for UI display - no behavior.
 *
 * @param statId The stat this breakdown is for
 * @param displayName Human-readable name for the stat
 * @param baseValue The stat's base value before any modifiers (including scaling)
 * @param scalingContributions List of scaling contributions from source stats
 * @param scaledBase Total base value including scaling contributions
 * @param entries List of all modifier contributions
 * @param flatTotal Total of all flat modifiers
 * @param afterFlat Value after applying flat modifiers
 * @param increasedTotalBps Total increased % in basis points
 * @param afterIncreased Value after applying increased modifiers
 * @param afterMore Value after applying all more modifiers
 * @param afterCap Value after applying caps
 * @param finalValue The final computed value
 * @param isRating Whether this stat uses rating-to-effectiveness conversion
 * @param effectivenessBps Effectiveness in basis points (if rating stat)
 */
public record StatBreakdown(
    @Nonnull StatId statId,
    @Nonnull String displayName,
    int baseValue,
    @Nonnull List<ScalingContribution> scalingContributions,
    int scaledBase,
    @Nonnull List<BreakdownEntry> entries,
    int flatTotal,
    int afterFlat,
    int increasedTotalBps,
    int afterIncreased,
    int afterMore,
    int afterCap,
    int finalValue,
    boolean isRating,
    int effectivenessBps
) {
    
    public StatBreakdown {
        Objects.requireNonNull(statId, "statId cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        Objects.requireNonNull(scalingContributions, "scalingContributions cannot be null");
        Objects.requireNonNull(entries, "entries cannot be null");
        
        // Make defensive copies
        scalingContributions = List.copyOf(scalingContributions);
        entries = List.copyOf(entries);
    }
    
    /**
     * Check if this stat has scaling from other stats.
     */
    public boolean hasScaling() {
        return !scalingContributions.isEmpty();
    }
    
    /**
     * Get the total scaling contribution.
     */
    public int getScalingTotal() {
        return scalingContributions.stream()
            .mapToInt(ScalingContribution::contribution)
            .sum();
    }
    
    /**
     * Get entries grouped by modifier type.
     */
    @Nonnull
    public List<BreakdownEntry> getFlatEntries() {
        return entries.stream()
            .filter(e -> e.modifierType() == HyforgedModifier.StackType.FLAT)
            .toList();
    }
    
    @Nonnull
    public List<BreakdownEntry> getIncreasedEntries() {
        return entries.stream()
            .filter(e -> e.modifierType() == HyforgedModifier.StackType.INCREASED)
            .toList();
    }
    
    @Nonnull
    public List<BreakdownEntry> getMoreEntries() {
        return entries.stream()
            .filter(e -> e.modifierType() == HyforgedModifier.StackType.MORE)
            .toList();
    }
    
    @Nonnull
    public List<BreakdownEntry> getCapEntries() {
        return entries.stream()
            .filter(e -> e.modifierType() == HyforgedModifier.StackType.CAP)
            .toList();
    }
    
    /**
     * Get a formatted effectiveness string for rating stats.
     * 
     * @return Formatted effectiveness (e.g., "45.5%") or null if not a rating stat
     */
    @Nullable
    public String getFormattedEffectiveness() {
        if (!isRating) {
            return null;
        }
        double percent = effectivenessBps / 100.0;
        return String.format("%.1f%%", percent);
    }
    
    /**
     * Check if this breakdown has any modifiers.
     */
    public boolean hasModifiers() {
        return !entries.isEmpty();
    }
    
    /**
     * Builder for creating StatBreakdown instances.
     */
    public static class Builder {
        private StatId statId;
        private String displayName = "";
        private int baseValue = 0;
        private List<ScalingContribution> scalingContributions = new ArrayList<>();
        private int scaledBase = 0;
        private List<BreakdownEntry> entries = new ArrayList<>();
        private int flatTotal = 0;
        private int afterFlat = 0;
        private int increasedTotalBps = 0;
        private int afterIncreased = 0;
        private int afterMore = 0;
        private int afterCap = 0;
        private int finalValue = 0;
        private boolean isRating = false;
        private int effectivenessBps = 0;
        
        public Builder(@Nonnull StatId statId) {
            this.statId = Objects.requireNonNull(statId);
        }
        
        public Builder from(@Nonnull StatDefinition statDef) {
            this.statId = statDef.id();
            this.displayName = statDef.displayName();
            this.isRating = statDef.isRating();
            return this;
        }
        
        public Builder baseValue(int value) {
            this.baseValue = value;
            return this;
        }
        
        public Builder addScalingContribution(@Nonnull ScalingContribution contribution) {
            this.scalingContributions.add(contribution);
            return this;
        }
        
        public Builder scalingContributions(@Nonnull List<ScalingContribution> contributions) {
            this.scalingContributions = new ArrayList<>(contributions);
            return this;
        }
        
        public Builder scaledBase(int value) {
            this.scaledBase = value;
            return this;
        }
        
        public Builder addEntry(@Nonnull BreakdownEntry entry) {
            this.entries.add(entry);
            return this;
        }
        
        public Builder flatTotal(int value) {
            this.flatTotal = value;
            return this;
        }
        
        public Builder afterFlat(int value) {
            this.afterFlat = value;
            return this;
        }
        
        public Builder increasedTotalBps(int value) {
            this.increasedTotalBps = value;
            return this;
        }
        
        public Builder afterIncreased(int value) {
            this.afterIncreased = value;
            return this;
        }
        
        public Builder afterMore(int value) {
            this.afterMore = value;
            return this;
        }
        
        public Builder afterCap(int value) {
            this.afterCap = value;
            return this;
        }
        
        public Builder finalValue(int value) {
            this.finalValue = value;
            return this;
        }
        
        public Builder isRating(boolean rating) {
            this.isRating = rating;
            return this;
        }
        
        public Builder effectivenessBps(int value) {
            this.effectivenessBps = value;
            return this;
        }
        
        public StatBreakdown build() {
            return new StatBreakdown(
                statId, displayName, baseValue, scalingContributions, scaledBase,
                entries, flatTotal, afterFlat, increasedTotalBps, afterIncreased,
                afterMore, afterCap, finalValue, isRating, effectivenessBps
            );
        }
    }
    
    public static Builder builder(@Nonnull StatId statId) {
        return new Builder(statId);
    }
}
