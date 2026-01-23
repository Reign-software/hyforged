package reign.software.hyforged.quality.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Defines modifier rules that adjust quality weights based on roll context.
 */
public record QualityModifierConfig(
        @Nonnull String id,
        @Nonnull String description,
        @Nonnull LevelScalingConfig levelScaling,
        @Nonnull ItemRarityConfig itemRarity,
        @Nonnull NpcQualityBonusConfig npcQualityBonus
) {

    public QualityModifierConfig {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(levelScaling, "levelScaling cannot be null");
        Objects.requireNonNull(itemRarity, "itemRarity cannot be null");
        Objects.requireNonNull(npcQualityBonus, "npcQualityBonus cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
    }

    /**
     * Apply overrides on top of this config.
     */
    @Nonnull
    public QualityModifierConfig applyOverrides(@Nonnull QualityModifierOverrides overrides) {
        Objects.requireNonNull(overrides, "overrides cannot be null");

        LevelScalingConfig resolvedLevelScaling = levelScaling.applyOverrides(overrides.levelScaling());
        ItemRarityConfig resolvedItemRarity = itemRarity.applyOverrides(overrides.itemRarity());
        NpcQualityBonusConfig resolvedNpcQualityBonus = npcQualityBonus.applyOverrides(overrides.npcQualityBonus());

        return new QualityModifierConfig(id, description, resolvedLevelScaling, resolvedItemRarity, resolvedNpcQualityBonus);
    }

    public record LevelScalingConfig(
            boolean enabled,
            @Nonnull String curveId,
            @Nonnull Map<String, Double> qualityBonusPerLevel
    ) {
        public LevelScalingConfig {
            Objects.requireNonNull(curveId, "curveId cannot be null");
            Objects.requireNonNull(qualityBonusPerLevel, "qualityBonusPerLevel cannot be null");
            qualityBonusPerLevel = normalizeDoubleMap(qualityBonusPerLevel);
        }

        @Nonnull
        LevelScalingConfig applyOverrides(@Nullable QualityModifierOverrides.LevelScalingOverride override) {
            if (override == null) {
                return this;
            }
            boolean resolvedEnabled = override.enabled() != null ? override.enabled() : enabled;
            String resolvedCurveId = override.curveId() != null ? override.curveId() : curveId;
            Map<String, Double> resolvedBonus = override.qualityBonusPerLevel() != null
                    ? normalizeDoubleMap(override.qualityBonusPerLevel())
                    : qualityBonusPerLevel;
            return new LevelScalingConfig(resolvedEnabled, resolvedCurveId, resolvedBonus);
        }
    }

    public record ItemRarityConfig(
            boolean enabled,
            @Nonnull String statId,
            double scalingFactor,
            int maxBonus,
            int fallbackValue
    ) {
        public ItemRarityConfig {
            Objects.requireNonNull(statId, "statId cannot be null");
        }

        @Nonnull
        ItemRarityConfig applyOverrides(@Nullable QualityModifierOverrides.ItemRarityOverride override) {
            if (override == null) {
                return this;
            }
            boolean resolvedEnabled = override.enabled() != null ? override.enabled() : enabled;
            String resolvedStatId = override.statId() != null ? override.statId() : statId;
            double resolvedScalingFactor = override.scalingFactor() != null ? override.scalingFactor() : scalingFactor;
            int resolvedMaxBonus = override.maxBonus() != null ? override.maxBonus() : maxBonus;
            int resolvedFallback = override.fallbackValue() != null ? override.fallbackValue() : fallbackValue;
            return new ItemRarityConfig(resolvedEnabled, resolvedStatId, resolvedScalingFactor, resolvedMaxBonus, resolvedFallback);
        }
    }

    public record NpcQualityBonusConfig(
            boolean enabled,
            @Nonnull Map<String, Integer> bonusPerTier
    ) {
        public NpcQualityBonusConfig {
            Objects.requireNonNull(bonusPerTier, "bonusPerTier cannot be null");
            bonusPerTier = normalizeIntMap(bonusPerTier);
        }

        @Nonnull
        NpcQualityBonusConfig applyOverrides(@Nullable QualityModifierOverrides.NpcQualityBonusOverride override) {
            if (override == null) {
                return this;
            }
            boolean resolvedEnabled = override.enabled() != null ? override.enabled() : enabled;
            Map<String, Integer> resolvedBonus = override.bonusPerTier() != null
                    ? normalizeIntMap(override.bonusPerTier())
                    : bonusPerTier;
            return new NpcQualityBonusConfig(resolvedEnabled, resolvedBonus);
        }
    }

    private static Map<String, Integer> normalizeIntMap(@Nonnull Map<String, Integer> input) {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, Integer> entry : input.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            int value = entry.getValue() != null ? entry.getValue() : 0;
            result.put(key, value);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Double> normalizeDoubleMap(@Nonnull Map<String, Double> input) {
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Double> entry : input.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            double value = entry.getValue() != null ? entry.getValue() : 0.0;
            result.put(key, value);
        }
        return Collections.unmodifiableMap(result);
    }
}
