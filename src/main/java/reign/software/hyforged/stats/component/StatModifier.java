package reign.software.hyforged.stats.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Immutable data record representing a stat modifier.
 * <p>
 * This is pure data - no behavior, following ECS principles.
 * <p>
 * A modifier can target either:
 * - A specific stat (by index) via targetStatIndex
 * - All stats in a tag via targetTagId
 * 
 * @param sourceId Unique identifier for the source (e.g., item UUID, buff ID)
 * @param sourceType Category of the source for UI grouping
 * @param modifierType How this modifier stacks (FLAT/INCREASED/MORE/CAP)
 * @param targetStatIndex Index of the target stat (-1 if targeting a tag)
 * @param targetTagId ID of the target tag (null if targeting a specific stat)
 * @param value The modifier value (interpretation depends on modifierType)
 * @param expirationTick Game tick when this modifier expires (0 = permanent)
 * @param priority Tie-breaker for modifiers of the same type (lower = first)
 */
public record StatModifier(
    @Nonnull String sourceId,
    @Nonnull ModifierSource sourceType,
    @Nonnull ModifierType modifierType,
    int targetStatIndex,
    @Nullable String targetTagId,
    int value,
    long expirationTick,
    int priority
) {
    
    public StatModifier {
        Objects.requireNonNull(sourceId, "sourceId cannot be null");
        Objects.requireNonNull(sourceType, "sourceType cannot be null");
        Objects.requireNonNull(modifierType, "modifierType cannot be null");
        
        // Must target either a stat or a tag
        if (targetStatIndex < 0 && targetTagId == null) {
            throw new IllegalArgumentException("Modifier must target either a stat index or a tag");
        }
    }
    
    /**
     * Check if this modifier targets a tag (affects multiple stats).
     */
    public boolean isTagModifier() {
        return targetTagId != null;
    }
    
    /**
     * Check if this modifier is expired.
     */
    public boolean isExpired(long currentTick) {
        return expirationTick > 0 && expirationTick <= currentTick;
    }
    
    /**
     * Check if this modifier is permanent (never expires).
     */
    public boolean isPermanent() {
        return expirationTick == 0;
    }
    
    /**
     * Builder for creating StatModifier instances.
     */
    public static class Builder {
        private String sourceId;
        private ModifierSource sourceType = ModifierSource.EQUIPMENT;
        private ModifierType modifierType = ModifierType.FLAT;
        private int targetStatIndex = -1;
        private String targetTagId = null;
        private int value = 0;
        private long expirationTick = 0;
        private int priority = 0;
        
        public Builder(@Nonnull String sourceId) {
            this.sourceId = Objects.requireNonNull(sourceId);
        }
        
        public Builder sourceType(@Nonnull ModifierSource type) {
            this.sourceType = type;
            return this;
        }
        
        public Builder modifierType(@Nonnull ModifierType type) {
            this.modifierType = type;
            return this;
        }
        
        public Builder targetStat(int statIndex) {
            this.targetStatIndex = statIndex;
            this.targetTagId = null;
            return this;
        }
        
        public Builder targetTag(@Nonnull String tagId) {
            this.targetTagId = tagId;
            this.targetStatIndex = -1;
            return this;
        }
        
        public Builder value(int value) {
            this.value = value;
            return this;
        }
        
        public Builder expiresAt(long tick) {
            this.expirationTick = tick;
            return this;
        }
        
        public Builder permanent() {
            this.expirationTick = 0;
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public StatModifier build() {
            return new StatModifier(
                sourceId, sourceType, modifierType,
                targetStatIndex, targetTagId, value,
                expirationTick, priority
            );
        }
        
        /**
         * Build as a conditional modifier with the given condition.
         *
         * @param condition The condition for when this modifier applies
         * @return A ConditionalStatModifier
         */
        public ConditionalStatModifier buildConditional(
                @Nonnull reign.software.hyforged.stats.condition.ModifierCondition condition
        ) {
            return ConditionalStatModifier.conditional(build(), condition);
        }
        
        /**
         * Build as an unconditional modifier (always applies).
         *
         * @return A ConditionalStatModifier that always applies
         */
        public ConditionalStatModifier buildUnconditional() {
            return ConditionalStatModifier.unconditional(build());
        }
    }
    
    /**
     * Create a flat modifier targeting a specific stat.
     */
    public static StatModifier flat(@Nonnull String sourceId, @Nonnull ModifierSource sourceType, int statIndex, int value) {
        return new Builder(sourceId)
            .sourceType(sourceType)
            .modifierType(ModifierType.FLAT)
            .targetStat(statIndex)
            .value(value)
            .build();
    }
    
    /**
     * Create an increased modifier targeting a specific stat.
    * @param valueBps Value in basis points (10000 = 100%)
     */
    public static StatModifier increased(@Nonnull String sourceId, @Nonnull ModifierSource sourceType, int statIndex, int valueBps) {
        return new Builder(sourceId)
            .sourceType(sourceType)
            .modifierType(ModifierType.INCREASED)
            .targetStat(statIndex)
            .value(valueBps)
            .build();
    }
    
    /**
     * Create a more modifier targeting a specific stat.
    * @param valueBps Value in basis points (10000 = 100%)
     */
    public static StatModifier more(@Nonnull String sourceId, @Nonnull ModifierSource sourceType, int statIndex, int valueBps) {
        return new Builder(sourceId)
            .sourceType(sourceType)
            .modifierType(ModifierType.MORE)
            .targetStat(statIndex)
            .value(valueBps)
            .build();
    }
}
