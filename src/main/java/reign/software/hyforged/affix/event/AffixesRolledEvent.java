package reign.software.hyforged.affix.event;

import com.hypixel.hytale.event.IEvent;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.service.AffixRollContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Event fired after affixes are rolled for an item, before they are applied.
 * <p>
 * This event is cancellable. If cancelled, the rolled affixes will not be
 * applied to the item. Listeners can also modify the affixes list before
 * application by providing replacement affixes.
 * <p>
 * Usage:
 * <pre>
 * eventRegistry.register(AffixesRolledEvent.class, event -> {
 *     // React to affixes being rolled on an item
 *     String itemId = event.getContext().itemId();
 *     List&lt;RolledAffix&gt; affixes = event.getAffixes();
 *     
 *     // Cancel to prevent affixes from being applied
 *     if (shouldPreventAffixes(itemId)) {
 *         event.setCancelled(true);
 *     }
 *     
 *     // Or replace the affixes
 *     event.setReplacementAffixes(myCustomAffixes);
 * });
 * </pre>
 * <p>
 * Implements {@link IEvent} with {@code Void} key type for global (non-keyed) dispatch.
 */
public class AffixesRolledEvent implements IEvent<Void> {
    
    private final AffixRollContext context;
    private final String poolId;
    private final List<RolledAffix> affixes;
    private final long seed;
    
    private boolean cancelled = false;
    private List<RolledAffix> replacementAffixes = null;
    
    /**
     * Create a new AffixesRolledEvent.
     *
     * @param context  The roll context containing item properties
     * @param poolId   The affix pool that was selected
     * @param affixes  The affixes that were rolled
     * @param seed     The random seed used for rolling (0 if not deterministic)
     */
    public AffixesRolledEvent(
            @Nonnull AffixRollContext context,
            @Nonnull String poolId,
            @Nonnull List<RolledAffix> affixes,
            long seed
    ) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.poolId = Objects.requireNonNull(poolId, "poolId cannot be null");
        this.affixes = List.copyOf(Objects.requireNonNull(affixes, "affixes cannot be null"));
        this.seed = seed;
    }
    
    /**
     * Get the roll context containing item properties.
     */
    @Nonnull
    public AffixRollContext getContext() {
        return context;
    }
    
    /**
     * Get the item ID that received affixes.
     */
    @Nonnull
    public String getItemId() {
        return context.itemId();
    }
    
    /**
     * Get the affix pool that was used.
     */
    @Nonnull
    public String getPoolId() {
        return poolId;
    }
    
    /**
     * Get the affixes that were rolled.
     */
    @Nonnull
    public List<RolledAffix> getAffixes() {
        return affixes;
    }
    
    /**
     * Get the random seed used for rolling.
     *
     * @return The seed, or 0 if random
     */
    public long getSeed() {
        return seed;
    }
    
    /**
     * Get the item quality.
     */
    @Nonnull
    public String getQuality() {
        return context.quality();
    }
    
    /**
     * Get the item level.
     */
    public int getItemLevel() {
        return context.itemLevel();
    }
    
    /**
     * Check if this event has been cancelled.
     * <p>
     * If cancelled, the rolled affixes will not be applied to the item.
     *
     * @return true if the event is cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * Set whether this event is cancelled.
     * <p>
     * If cancelled, the rolled affixes will not be applied to the item.
     *
     * @param cancelled true to cancel the event
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    /**
     * Get any replacement affixes set by event listeners.
     * <p>
     * If set, these affixes will be used instead of the originally rolled ones.
     *
     * @return The replacement affixes, or null if not set
     */
    @Nullable
    public List<RolledAffix> getReplacementAffixes() {
        return replacementAffixes;
    }
    
    /**
     * Set replacement affixes to be used instead of the rolled ones.
     * <p>
     * This allows listeners to modify the affixes before they are applied.
     * If null, the original rolled affixes will be used.
     *
     * @param affixes The replacement affixes, or null to use original
     */
    public void setReplacementAffixes(@Nullable List<RolledAffix> affixes) {
        this.replacementAffixes = affixes != null ? List.copyOf(affixes) : null;
    }
    
    /**
     * Check if replacement affixes have been set.
     *
     * @return true if replacement affixes are available
     */
    public boolean hasReplacementAffixes() {
        return replacementAffixes != null;
    }
    
    /**
     * Get the effective affixes to apply.
     * <p>
     * Returns replacement affixes if set, otherwise the original rolled affixes.
     *
     * @return The affixes to apply (never null)
     */
    @Nonnull
    public List<RolledAffix> getEffectiveAffixes() {
        return replacementAffixes != null ? replacementAffixes : affixes;
    }
}
