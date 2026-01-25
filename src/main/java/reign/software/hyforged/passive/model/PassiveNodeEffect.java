package reign.software.hyforged.passive.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a passive node effect definition.
 * <p>
 * Effects are data-driven and processed by registered effect handlers.
 * Core effect types:
 * <ul>
 *   <li>{@code stat-modifier} - Add modifier to a stat</li>
 *   <li>{@code spell-grant} - Unlock a spell for the player</li>
 *   <li>{@code unlock-flag} - Enable a mechanic or ability</li>
 *   <li>{@code mastery-choice} - Present options when allocated</li>
 * </ul>
 *
 * @param type The effect type (e.g., "stat-modifier", "spell-grant")
 * @param data Additional data for the effect (type-specific)
 */
public record PassiveNodeEffect(
    @Nonnull String type,
    @Nonnull Map<String, Object> data
) {
    
    public PassiveNodeEffect {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(data, "data cannot be null");
        data = Map.copyOf(data); // Defensive copy
    }
    
    /**
     * Create a stat modifier effect.
     *
     * @param statId The stat to modify
     * @param value The modifier value (in basis points for percentages)
     * @return The effect
     */
    public static PassiveNodeEffect statModifier(@Nonnull String statId, int value) {
        return new PassiveNodeEffect("stat-modifier", Map.of(
            "Stat", statId,
            "Value", value
        ));
    }
    
    /**
     * Create a spell grant effect.
     *
     * @param spellId The spell to grant
     * @return The effect
     */
    public static PassiveNodeEffect spellGrant(@Nonnull String spellId) {
        return new PassiveNodeEffect("spell-grant", Map.of(
            "SpellId", spellId
        ));
    }
    
    /**
     * Create an unlock flag effect.
     *
     * @param flagId The flag to set
     * @return The effect
     */
    public static PassiveNodeEffect unlockFlag(@Nonnull String flagId) {
        return new PassiveNodeEffect("unlock-flag", Map.of(
            "FlagId", flagId
        ));
    }
    
    /**
     * Create a mastery choice effect.
     *
     * @param choices List of choice effects (each can be a PassiveNodeEffect)
     * @return The effect
     */
    public static PassiveNodeEffect masteryChoice(@Nonnull List<PassiveNodeEffect> choices) {
        return new PassiveNodeEffect("mastery-choice", Map.of(
            "Choices", choices
        ));
    }
    
    /**
     * Get a string value from effect data.
     *
     * @param key The key
     * @return The value, or null if not present or not a string
     */
    @Nullable
    public String getString(@Nonnull String key) {
        Object value = data.get(key);
        return value instanceof String ? (String) value : null;
    }
    
    /**
     * Get an integer value from effect data.
     *
     * @param key The key
     * @param defaultValue Default value if not present
     * @return The value
     */
    public int getInt(@Nonnull String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    /**
     * Get a list value from effect data.
     *
     * @param key The key
     * @return The value, or empty list if not present or not a list
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public <T> List<T> getList(@Nonnull String key) {
        Object value = data.get(key);
        if (value instanceof List) {
            return (List<T>) value;
        }
        return Collections.emptyList();
    }
}
