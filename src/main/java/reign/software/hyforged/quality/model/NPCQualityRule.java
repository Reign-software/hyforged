package reign.software.hyforged.quality.model;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Defines NPC quality roll weights and scaling bonuses.
 */
public record NPCQualityRule(
        @Nonnull String id,
        @Nonnull String description,
        @Nonnull Map<String, Integer> weights,
        @Nonnull Map<String, Double> statMultipliers,
        @Nonnull Map<String, Integer> lootQualityBonus
) {
    public NPCQualityRule {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(weights, "weights cannot be null");
        Objects.requireNonNull(statMultipliers, "statMultipliers cannot be null");
        Objects.requireNonNull(lootQualityBonus, "lootQualityBonus cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }

        weights = Collections.unmodifiableMap(normalizeIntMap(weights));
        statMultipliers = Collections.unmodifiableMap(normalizeDoubleMap(statMultipliers));
        lootQualityBonus = Collections.unmodifiableMap(normalizeIntMap(lootQualityBonus));
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
        return result;
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
        return result;
    }
}
