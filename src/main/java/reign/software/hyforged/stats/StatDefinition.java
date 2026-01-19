package reign.software.hyforged.stats;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Definition of a stat (ability score or derived stat).
 * <p>
 * This is pure immutable data - no behavior, following ECS principles.
 * Stat definitions are loaded at startup and stored in the StatDefinitionRegistry.
 */
public record StatDefinition(
    @Nonnull StatId id,
    @Nonnull String category,
    @Nonnull DisplayFormat displayFormat,
    int defaultValue,
    int minValue,
    int maxValue,
    @Nonnull Set<String> tags,
    @Nonnull String displayName,
    @Nonnull String description,
    boolean isAbilityScore,
    boolean isRating
) {
    
    public StatDefinition {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(category, "category cannot be null");
        Objects.requireNonNull(displayFormat, "displayFormat cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        tags = tags != null ? Set.copyOf(tags) : Collections.emptySet();
        
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue cannot be greater than maxValue");
        }
        if (defaultValue < minValue || defaultValue > maxValue) {
            throw new IllegalArgumentException("defaultValue must be between minValue and maxValue");
        }
    }
    
    /**
     * Builder for creating StatDefinition instances.
     */
    public static class Builder {
        private StatId id;
        private String category = CoreCategories.UTILITY;
        private DisplayFormat displayFormat = DisplayFormat.INTEGER;
        private int defaultValue = 0;
        private int minValue = 0;
        private int maxValue = Integer.MAX_VALUE;
        private Set<String> tags = Collections.emptySet();
        private String displayName = "";
        private String description = "";
        private boolean isAbilityScore = false;
        private boolean isRating = false;
        
        public Builder(@Nonnull StatId id) {
            this.id = Objects.requireNonNull(id);
            this.displayName = id.name();
        }
        
        public Builder category(@Nonnull String category) {
            this.category = category;
            return this;
        }
        
        public Builder displayFormat(@Nonnull DisplayFormat format) {
            this.displayFormat = format;
            return this;
        }
        
        public Builder defaultValue(int value) {
            this.defaultValue = value;
            return this;
        }
        
        public Builder bounds(int min, int max) {
            this.minValue = min;
            this.maxValue = max;
            return this;
        }
        
        public Builder tags(@Nonnull Set<String> tags) {
            this.tags = tags;
            return this;
        }
        
        public Builder displayName(@Nonnull String name) {
            this.displayName = name;
            return this;
        }
        
        public Builder description(@Nonnull String desc) {
            this.description = desc;
            return this;
        }
        
        public Builder abilityScore(boolean isAbilityScore) {
            this.isAbilityScore = isAbilityScore;
            if (isAbilityScore) {
                this.category = CoreCategories.ABILITY_SCORE;
            }
            return this;
        }
        
        public Builder rating(boolean isRating) {
            this.isRating = isRating;
            if (isRating) {
                this.displayFormat = DisplayFormat.RATING;
            }
            return this;
        }
        
        public StatDefinition build() {
            return new StatDefinition(
                id, category, displayFormat, defaultValue,
                minValue, maxValue, tags, displayName, description,
                isAbilityScore, isRating
            );
        }
    }
}
