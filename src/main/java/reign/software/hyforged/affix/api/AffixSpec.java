package reign.software.hyforged.affix.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Specification for creating an affix programmatically.
 * <p>
 * Used with {@link AffixService#createWithAffixes} to create items with specific affixes.
 * This allows plugins to precisely control which affixes are applied to an item.
 * <p>
 * Example usage:
 * <pre>
 * // Create an item with specific affixes
 * List&lt;AffixSpec&gt; specs = List.of(
 *     AffixSpec.of("sturdy", 2, 35),          // Tier 2 Sturdy with value 35
 *     AffixSpec.of("of-the-bear", 1)          // Tier 1 Of the Bear with random value
 * );
 * ItemStack item = AffixService.get().createWithAffixes("Items.Weapons.Sword", specs);
 * </pre>
 *
 * @param affixId   The affix definition ID (must exist in AffixDefinitionRegistry)
 * @param tier      The tier to apply (1 = best). If null, tier will be rolled randomly.
 * @param value     The exact value to apply. If null, value will be rolled within tier range.
 */
public record AffixSpec(
        @Nonnull String affixId,
        @Nullable Integer tier,
        @Nullable Integer value
) {
    
    public AffixSpec {
        Objects.requireNonNull(affixId, "affixId cannot be null");
        
        if (affixId.isBlank()) {
            throw new IllegalArgumentException("affixId cannot be blank");
        }
        
        if (tier != null && tier < 1) {
            throw new IllegalArgumentException("tier must be >= 1, got: " + tier);
        }
    }
    
    /**
     * Create a spec with a specific affix, tier, and value.
     *
     * @param affixId The affix definition ID
     * @param tier    The tier to apply (1 = best)
     * @param value   The exact value to apply
     * @return A new AffixSpec
     */
    @Nonnull
    public static AffixSpec of(@Nonnull String affixId, int tier, int value) {
        return new AffixSpec(affixId, tier, value);
    }
    
    /**
     * Create a spec with a specific affix and tier, with random value.
     *
     * @param affixId The affix definition ID
     * @param tier    The tier to apply (1 = best)
     * @return A new AffixSpec with value to be rolled
     */
    @Nonnull
    public static AffixSpec of(@Nonnull String affixId, int tier) {
        return new AffixSpec(affixId, tier, null);
    }
    
    /**
     * Create a spec with a specific affix, with random tier and value.
     *
     * @param affixId The affix definition ID
     * @return A new AffixSpec with tier and value to be rolled
     */
    @Nonnull
    public static AffixSpec of(@Nonnull String affixId) {
        return new AffixSpec(affixId, null, null);
    }
    
    /**
     * Check if this spec has a fixed tier.
     *
     * @return true if tier is specified
     */
    public boolean hasTier() {
        return tier != null;
    }
    
    /**
     * Check if this spec has a fixed value.
     *
     * @return true if value is specified
     */
    public boolean hasValue() {
        return value != null;
    }
    
    /**
     * Get the tier, throwing if not specified.
     *
     * @return The tier value
     * @throws IllegalStateException if tier is not specified
     */
    public int requireTier() {
        if (tier == null) {
            throw new IllegalStateException("Tier not specified in AffixSpec for: " + affixId);
        }
        return tier;
    }
    
    /**
     * Get the value, throwing if not specified.
     *
     * @return The value
     * @throws IllegalStateException if value is not specified
     */
    public int requireValue() {
        if (value == null) {
            throw new IllegalStateException("Value not specified in AffixSpec for: " + affixId);
        }
        return value;
    }
}
