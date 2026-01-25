package reign.software.hyforged.affix.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Tracks active triggered effect affixes on an entity at runtime.
 */
public class HyforgedActiveEffectsComponent implements Component<EntityStore> {

    private Map<String, ActiveEffectState> activeEffects;

    public HyforgedActiveEffectsComponent() {
        this.activeEffects = new HashMap<>();
    }

    public HyforgedActiveEffectsComponent(@Nonnull Map<String, ActiveEffectState> activeEffects) {
        this.activeEffects = copyStates(activeEffects);
    }

    public HyforgedActiveEffectsComponent(@Nonnull HyforgedActiveEffectsComponent other) {
        this(other.activeEffects);
    }

    @Nonnull
    public Map<String, ActiveEffectState> getActiveEffects() {
        return activeEffects;
    }

    public void setActiveEffects(@Nonnull Map<String, ActiveEffectState> activeEffects) {
        this.activeEffects = copyStates(activeEffects);
    }

    public boolean isEmpty() {
        return activeEffects == null || activeEffects.isEmpty();
    }

    @Nonnull
    private Map<String, ActiveEffectState> copyStates(@Nonnull Map<String, ActiveEffectState> states) {
        Objects.requireNonNull(states, "activeEffects cannot be null");
        if (states.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, ActiveEffectState> copy = new HashMap<>();
        for (Map.Entry<String, ActiveEffectState> entry : states.entrySet()) {
            copy.put(entry.getKey(), new ActiveEffectState(entry.getValue()));
        }
        return copy;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new HyforgedActiveEffectsComponent(this);
    }

    /**
     * Runtime state for a single active triggered effect.
     */
    public static class ActiveEffectState {
        private String affixId;
        private int effectIndex;
        private String sourceType;
        private String sourceId;
        private long lastTriggeredMs;
        private int stacks;
        private float accumulatedTime;
        private Set<Integer> triggeredHealthThresholds;

        public ActiveEffectState() {
            this.affixId = "";
            this.effectIndex = 0;
            this.sourceType = "";
            this.sourceId = "";
            this.lastTriggeredMs = 0L;
            this.stacks = 1;
            this.accumulatedTime = 0f;
            this.triggeredHealthThresholds = new HashSet<>();
        }

        public ActiveEffectState(
                @Nonnull String affixId,
                int effectIndex,
                @Nonnull String sourceType,
                @Nonnull String sourceId,
                long lastTriggeredMs,
                int stacks,
                float accumulatedTime,
                @Nonnull Set<Integer> triggeredHealthThresholds
        ) {
            this.affixId = Objects.requireNonNull(affixId, "affixId cannot be null");
            this.effectIndex = effectIndex;
            this.sourceType = Objects.requireNonNull(sourceType, "sourceType cannot be null");
            this.sourceId = Objects.requireNonNull(sourceId, "sourceId cannot be null");
            this.lastTriggeredMs = lastTriggeredMs;
            this.stacks = stacks;
            this.accumulatedTime = accumulatedTime;
            this.triggeredHealthThresholds = new HashSet<>(Objects.requireNonNull(triggeredHealthThresholds, "triggeredHealthThresholds cannot be null"));
        }

        public ActiveEffectState(@Nonnull ActiveEffectState other) {
            this(
                    other.affixId,
                    other.effectIndex,
                    other.sourceType,
                    other.sourceId,
                    other.lastTriggeredMs,
                    other.stacks,
                    other.accumulatedTime,
                    other.triggeredHealthThresholds != null ? other.triggeredHealthThresholds : new HashSet<>()
            );
        }

        @Nonnull
        public String getAffixId() {
            return affixId;
        }

        public int getEffectIndex() {
            return effectIndex;
        }

        @Nonnull
        public String getSourceType() {
            return sourceType;
        }

        @Nonnull
        public String getSourceId() {
            return sourceId;
        }

        public long getLastTriggeredMs() {
            return lastTriggeredMs;
        }

        public void setLastTriggeredMs(long lastTriggeredMs) {
            this.lastTriggeredMs = lastTriggeredMs;
        }

        public int getStacks() {
            return stacks;
        }

        public void setStacks(int stacks) {
            this.stacks = stacks;
        }

        public float getAccumulatedTime() {
            return accumulatedTime;
        }

        public void setAccumulatedTime(float accumulatedTime) {
            this.accumulatedTime = accumulatedTime;
        }

        public boolean hasTriggeredHealthThreshold(int threshold) {
            return triggeredHealthThresholds != null && triggeredHealthThresholds.contains(threshold);
        }

        public void markTriggeredHealthThreshold(int threshold) {
            if (triggeredHealthThresholds == null) {
                triggeredHealthThresholds = new HashSet<>();
            }
            triggeredHealthThresholds.add(threshold);
        }

        @Nonnull
        public Set<Integer> getTriggeredHealthThresholds() {
            return triggeredHealthThresholds != null ? new HashSet<>(triggeredHealthThresholds) : new HashSet<>();
        }
    }
}
