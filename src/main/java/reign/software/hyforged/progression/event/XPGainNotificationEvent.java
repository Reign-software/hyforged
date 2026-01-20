package reign.software.hyforged.progression.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.progression.xp.XPSource;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Event dispatched when aggregated XP gains should be shown to the player.
 * <p>
 * This event is emitted by the {@link reign.software.hyforged.progression.xp.XPNotificationAggregator}
 * after collecting XP gains over an aggregation window (configurable ticks).
 * <p>
 * UI systems can listen for this event to display floating XP text or update
 * the player's XP bar.
 * <p>
 * Note: This is a notification event only - XP has already been applied to
 * the player's ProgressionComponent before this event is dispatched.
 *
 * @param entityRef         the entity receiving the XP notification
 * @param totalCharacterXp  total aggregated character XP
 * @param totalClassXp      total aggregated class XP
 * @param activeClassId     the player's active class ID (may be null)
 * @param sourceBreakdown   breakdown of XP by source category
 */
public record XPGainNotificationEvent(
        @Nonnull Ref<EntityStore> entityRef,
        long totalCharacterXp,
        long totalClassXp,
        String activeClassId,
        @Nonnull Map<XPSource, Long> sourceBreakdown
) implements IEvent<Void> {
    
    /**
     * Get the combined total XP (character + class).
     */
    public long getTotalCombinedXp() {
        return totalCharacterXp + totalClassXp;
    }
    
    /**
     * Check if there is any character XP in this notification.
     */
    public boolean hasCharacterXp() {
        return totalCharacterXp > 0;
    }
    
    /**
     * Check if there is any class XP in this notification.
     */
    public boolean hasClassXp() {
        return totalClassXp > 0 && activeClassId != null;
    }
    
    /**
     * Get XP from a specific source.
     */
    public long getXpFromSource(@Nonnull XPSource source) {
        return sourceBreakdown.getOrDefault(source, 0L);
    }
}
