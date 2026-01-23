package reign.software.hyforged.quality.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Partial overrides for quality modifier configuration.
 */
public record QualityModifierOverrides(
        @Nullable LevelScalingOverride levelScaling,
        @Nullable ItemRarityOverride itemRarity,
        @Nullable NpcQualityBonusOverride npcQualityBonus
) {

    public static final QualityModifierOverrides EMPTY = new QualityModifierOverrides(null, null, null);

    public record LevelScalingOverride(
            @Nullable Boolean enabled,
            @Nullable String curveId,
            @Nullable Map<String, Double> qualityBonusPerLevel
    ) {
        @Nonnull
        public Map<String, Double> normalizedBonusPerLevel() {
            if (qualityBonusPerLevel == null) {
                return Collections.emptyMap();
            }
            Map<String, Double> result = new HashMap<>();
            for (Map.Entry<String, Double> entry : qualityBonusPerLevel.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isBlank()) {
                    continue;
                }
                double value = entry.getValue() != null ? entry.getValue() : 0.0;
                result.put(key, value);
            }
            return result;
        }
    }

    public record ItemRarityOverride(
            @Nullable Boolean enabled,
            @Nullable String statId,
            @Nullable Double scalingFactor,
            @Nullable Integer maxBonus,
            @Nullable Integer fallbackValue
    ) {}

    public record NpcQualityBonusOverride(
            @Nullable Boolean enabled,
            @Nullable Map<String, Integer> bonusPerTier
    ) {
        @Nonnull
        public Map<String, Integer> normalizedBonusPerTier() {
            if (bonusPerTier == null) {
                return Collections.emptyMap();
            }
            Map<String, Integer> result = new HashMap<>();
            for (Map.Entry<String, Integer> entry : bonusPerTier.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isBlank()) {
                    continue;
                }
                int value = entry.getValue() != null ? entry.getValue() : 0;
                result.put(key, value);
            }
            return result;
        }
    }
}
