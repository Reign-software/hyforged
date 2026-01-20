package reign.software.hyforged.affix.service;

import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.model.RolledAffix;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of an affix rolling operation.
 * <p>
 * Contains the context used for rolling, the selected pool, and all rolled affixes.
 * Provides convenience methods for checking success and converting to item data.
 *
 * @param context  The roll context that was used
 * @param poolId   The affix pool that was selected (null if no pool found)
 * @param affixes  The rolled affixes (empty list if none rolled)
 */
public record AffixRollResult(
        @Nonnull AffixRollContext context,
        @Nullable String poolId,
        @Nonnull List<RolledAffix> affixes
) {
    
    /**
     * Canonical constructor with validation.
     */
    public AffixRollResult {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(affixes, "affixes cannot be null");
        affixes = List.copyOf(affixes);
    }
    
    /**
     * Create an empty result (no affixes rolled).
     */
    public static AffixRollResult empty(@Nonnull AffixRollContext context) {
        return new AffixRollResult(context, null, Collections.emptyList());
    }
    
    /**
     * Check if any affixes were rolled.
     */
    public boolean hasAffixes() {
        return !affixes.isEmpty();
    }
    
    /**
     * Get the number of affixes rolled.
     */
    public int affixCount() {
        return affixes.size();
    }
    
    /**
     * Check if rolling was successful (pool found and at least one affix rolled).
     */
    public boolean isSuccess() {
        return poolId != null && !affixes.isEmpty();
    }
    
    /**
     * Count affixes of a specific type.
     */
    public int countByType(@Nonnull String type) {
        return (int) affixes.stream()
                .filter(a -> a.type().equals(type))
                .count();
    }
    
    /**
     * Get affixes of a specific type.
     */
    public List<RolledAffix> getByType(@Nonnull String type) {
        return affixes.stream()
                .filter(a -> a.type().equals(type))
                .toList();
    }
    
    /**
     * Convert to HyforgedItemData for storage.
     */
    public HyforgedItemData toItemData() {
        return HyforgedItemData.create(affixes);
    }
}
