package reign.software.hyforged.affix.model;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Defines the type of an affix (e.g., prefix, suffix, forged).
 * <p>
 * Affix types are fully data-driven and loaded from JSON at
 * {@code Server/Hyforged/AffixTypes/*.json}.
 * <p>
 * This is pure immutable data following ECS principles.
 *
 * @param id Unique identifier for this type (e.g., "prefix", "suffix", "forged")
 * @param displayNamePosition Where the affix name appears relative to item name: "before", "after", or "none"
 * @param displayFormat Template for tooltip display (e.g., "{name} (T{tier})")
 * @param stackable Whether multiple affixes of this type can coexist on an item
 */
public record AffixType(
    @Nonnull String id,
    @Nonnull DisplayNamePosition displayNamePosition,
    @Nonnull String displayFormat,
    boolean stackable
) {
    
    /**
     * Position of the affix name relative to the item's base name.
     */
    public enum DisplayNamePosition {
        /** Affix name appears before the item name (e.g., "Sturdy Sword") */
        BEFORE("before"),
        /** Affix name appears after the item name (e.g., "Sword of the Bear") */
        AFTER("after"),
        /** Affix name does not modify the item name (shown in tooltip only) */
        NONE("none");
        
        private final String jsonValue;
        
        DisplayNamePosition(String jsonValue) {
            this.jsonValue = jsonValue;
        }
        
        public String getJsonValue() {
            return jsonValue;
        }
        
        /**
         * Parse a DisplayNamePosition from its JSON string value.
         *
         * @param value The JSON value ("before", "after", or "none")
         * @return The corresponding enum value
         * @throws IllegalArgumentException if the value is not recognized
         */
        @Nonnull
        public static DisplayNamePosition fromJson(@Nonnull String value) {
            for (DisplayNamePosition pos : values()) {
                if (pos.jsonValue.equalsIgnoreCase(value)) {
                    return pos;
                }
            }
            throw new IllegalArgumentException("Unknown display name position: " + value);
        }
    }
    
    public AffixType {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(displayNamePosition, "displayNamePosition cannot be null");
        Objects.requireNonNull(displayFormat, "displayFormat cannot be null");
        
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
    }
    
    /**
     * Check if this affix type places its name before the item name.
     */
    public boolean isPrefix() {
        return displayNamePosition == DisplayNamePosition.BEFORE;
    }
    
    /**
     * Check if this affix type places its name after the item name.
     */
    public boolean isSuffix() {
        return displayNamePosition == DisplayNamePosition.AFTER;
    }
    
    /**
     * Format an affix display line using this type's format template.
     *
     * @param name The affix's display name
     * @param tier The rolled tier number
     * @return The formatted display string
     */
    @Nonnull
    public String formatDisplay(@Nonnull String name, int tier) {
        return displayFormat
            .replace("{name}", name)
            .replace("{tier}", String.valueOf(tier));
    }
}
