package reign.software.hyforged.affix.model;

import com.hypixel.hytale.math.vector.Vector3d;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Defines the effect executed when an affix trigger activates.
 * <p>
 * All fields are optional and interpreted based on the effect {@code type}.
 */
public record AffixEffect(
        @Nonnull String type,
        @Nonnull String projectileId,
        int count,
        @Nonnull String pattern,
        float velocity,
        float spreadAngle,
        float orbitRadius,
        float rotationSpeed,
        float durationSeconds,
        @Nonnull String prefabPath,
        @Nonnull Vector3d offset,
        @Nonnull String effectId,
        @Nonnull String target,
        float radius,
        int damage,
        @Nonnull String damageCause,
        boolean excludeSelf,
        @Nonnull String interactionId,
        @Nonnull String interactionType,
        @Nonnull String statId,
        int amount,
        @Nonnull String stackType,
    float statDurationSeconds,
    @Nonnull Map<String, Integer> statModifiers,
    float damageScaling,
    @Nonnull String applyEffectId,
    float applyEffectDurationSeconds,
    float spawnRadius,
    boolean invulnerability,
    float invulnerabilityDuration
) {

    public AffixEffect {
        Objects.requireNonNull(type, "type cannot be null");
        if (type.isBlank()) {
            throw new IllegalArgumentException("type cannot be blank");
        }

        projectileId = projectileId != null ? projectileId : "";
        pattern = pattern != null ? pattern : "";
        prefabPath = prefabPath != null ? prefabPath : "";
        offset = offset != null ? new Vector3d(offset) : Vector3d.ZERO;
        effectId = effectId != null ? effectId : "";
        target = target != null ? target : "";
        damageCause = damageCause != null ? damageCause : "";
        interactionId = interactionId != null ? interactionId : "";
        interactionType = interactionType != null ? interactionType : "";
        statId = statId != null ? statId : "";
        stackType = stackType != null ? stackType : "";
        statModifiers = statModifiers != null ? Map.copyOf(statModifiers) : Collections.emptyMap();
        applyEffectId = applyEffectId != null ? applyEffectId : "";

        if (count <= 0) {
            count = 1;
        }
        if (radius < 0) {
            radius = 0;
        }
        if (velocity < 0) {
            velocity = 0;
        }
        if (spreadAngle < 0) {
            spreadAngle = 0;
        }
        if (orbitRadius < 0) {
            orbitRadius = 0;
        }
        if (rotationSpeed < 0) {
            rotationSpeed = 0;
        }
        if (durationSeconds < 0) {
            durationSeconds = 0;
        }
        if (statDurationSeconds < 0) {
            statDurationSeconds = 0;
        }
        if (damageScaling < 0) {
            damageScaling = 0;
        }
        if (applyEffectDurationSeconds < 0) {
            applyEffectDurationSeconds = 0;
        }
        if (spawnRadius < 0) {
            spawnRadius = 0;
        }
        if (invulnerabilityDuration < 0) {
            invulnerabilityDuration = 0;
        }
    }
}
