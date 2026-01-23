package reign.software.hyforged.quality.model;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Defines a weight profile for rolling item quality tiers.
 * <p>
 * Profiles are loaded from JSON and referenced by eligibility rules.
 */
public record QualityWeightProfile(
        @Nonnull String id,
        @Nonnull String description,
        @Nonnull Map<String, Integer> weights,
        @Nonnull List<String> eligibleQualities
) {

    public QualityWeightProfile {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(weights, "weights cannot be null");
        Objects.requireNonNull(eligibleQualities, "eligibleQualities cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }

        Map<String, Integer> safeWeights = new HashMap<>();
        int total = 0;
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            int value = entry.getValue() != null ? entry.getValue() : 0;
            if (value < 0) {
                throw new IllegalArgumentException("Weight cannot be negative for quality: " + key);
            }
            safeWeights.put(key, value);
            total += value;
        }
        if (total <= 0) {
            throw new IllegalArgumentException("Quality weight profile must contain at least one positive weight");
        }
        weights = Collections.unmodifiableMap(safeWeights);
        eligibleQualities = List.copyOf(eligibleQualities);
    }

    /**
     * Check if a quality is explicitly allowed by this profile.
     */
    public boolean isQualityAllowed(@Nonnull String qualityId) {
        if (qualityId == null || qualityId.isBlank()) {
            return false;
        }
        if (eligibleQualities.isEmpty()) {
            return weights.containsKey(qualityId);
        }
        return eligibleQualities.contains(qualityId);
    }
}
