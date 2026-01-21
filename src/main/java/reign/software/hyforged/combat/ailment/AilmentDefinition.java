package reign.software.hyforged.combat.ailment;

import javax.annotation.Nonnull;

/**
 * Definition of an elemental ailment that can be triggered by accumulated damage.
 * <p>
 * Ailments are defined in JSON assets under Server/Hyforged/Combat/Ailments/.
 * Each ailment maps an element tag to a Hytale EntityEffect.
 */
public record AilmentDefinition(
    @Nonnull String id,
    @Nonnull String elementTag,
    @Nonnull String entityEffectId,
    int baseThreshold,
    long accumulationWindowMs,
    float baseDurationSeconds,
    @Nonnull String displayName,
    @Nonnull String description
) {
    
    /** Default threshold if not specified */
    public static final int DEFAULT_THRESHOLD = 100;
    
    /** Default window if not specified */
    public static final long DEFAULT_WINDOW_MS = 5000;
    
    /** Default duration if not specified */
    public static final float DEFAULT_DURATION_SECONDS = 4.0f;
    
    public AilmentDefinition {
        if (baseThreshold <= 0) {
            throw new IllegalArgumentException("baseThreshold must be positive");
        }
        if (accumulationWindowMs <= 0) {
            throw new IllegalArgumentException("accumulationWindowMs must be positive");
        }
        if (baseDurationSeconds <= 0) {
            throw new IllegalArgumentException("baseDurationSeconds must be positive");
        }
    }
    
    /**
     * Builder for creating ailment definitions.
     */
    public static class Builder {
        private String id;
        private String elementTag;
        private String entityEffectId;
        private int baseThreshold = DEFAULT_THRESHOLD;
        private long accumulationWindowMs = DEFAULT_WINDOW_MS;
        private float baseDurationSeconds = DEFAULT_DURATION_SECONDS;
        private String displayName = "";
        private String description = "";
        
        public Builder id(@Nonnull String id) {
            this.id = id;
            return this;
        }
        
        public Builder elementTag(@Nonnull String elementTag) {
            this.elementTag = elementTag;
            return this;
        }
        
        public Builder entityEffectId(@Nonnull String entityEffectId) {
            this.entityEffectId = entityEffectId;
            return this;
        }
        
        public Builder baseThreshold(int baseThreshold) {
            this.baseThreshold = baseThreshold;
            return this;
        }
        
        public Builder accumulationWindowMs(long accumulationWindowMs) {
            this.accumulationWindowMs = accumulationWindowMs;
            return this;
        }
        
        public Builder baseDurationSeconds(float baseDurationSeconds) {
            this.baseDurationSeconds = baseDurationSeconds;
            return this;
        }
        
        public Builder displayName(@Nonnull String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public Builder description(@Nonnull String description) {
            this.description = description;
            return this;
        }
        
        @Nonnull
        public AilmentDefinition build() {
            if (id == null || id.isEmpty()) {
                throw new IllegalStateException("id is required");
            }
            if (elementTag == null || elementTag.isEmpty()) {
                throw new IllegalStateException("elementTag is required");
            }
            if (entityEffectId == null || entityEffectId.isEmpty()) {
                throw new IllegalStateException("entityEffectId is required");
            }
            
            return new AilmentDefinition(
                id,
                elementTag,
                entityEffectId,
                baseThreshold,
                accumulationWindowMs,
                baseDurationSeconds,
                displayName,
                description
            );
        }
    }
    
    @Nonnull
    public static Builder builder() {
        return new Builder();
    }
}
