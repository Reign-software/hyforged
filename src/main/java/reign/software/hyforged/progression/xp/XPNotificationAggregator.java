package reign.software.hyforged.progression.xp;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.progression.event.XPGainNotificationEvent;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ECS ticking system that aggregates XP gains and dispatches notifications.
 * <p>
 * XP awards can happen rapidly (e.g., hitting multiple enemies per tick). Instead
 * of spamming the player with individual notifications, this system collects
 * XP gains over an aggregation window and dispatches a single notification.
 * <p>
 * Features:
 * - Collects XP per player per tick
 * - Aggregates by source category for breakdown display
 * - Configurable aggregation window (default: 20 ticks = 1 second)
 * - Rate limiting to prevent notification spam
 * <p>
 * This system listens for XP awards by being notified directly from XPAwardSystem.
 * The static methods allow XPAwardSystem to record XP gains that will be batched.
 */
public class XPNotificationAggregator extends TickingSystem<EntityStore> {
    
    private static final Logger LOGGER = Logger.getLogger(XPNotificationAggregator.class.getName());
    
    // Resource type reference for the aggregation resource
    private static ResourceType<EntityStore, AggregationResource> resourceType;
    
    private final ResourceType<EntityStore, AggregationResource> aggregationResourceType;
    
    /**
     * Create the aggregator system with the given resource type.
     * 
     * @param resourceType the resource type for aggregation data
     */
    public XPNotificationAggregator(@Nonnull ResourceType<EntityStore, AggregationResource> resourceType) {
        this.aggregationResourceType = resourceType;
        XPNotificationAggregator.resourceType = resourceType;
    }
    
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        // Run after XPAwardSystem has processed all XP events
        return Collections.emptySet();
    }
    
    @Override
    public void tick(float dt, int systemIndex, @Nonnull Store<EntityStore> store) {
        AggregationResource resource = store.getResource(aggregationResourceType);
        if (resource == null) {
            return;
        }
        
        resource.ticksSinceLastNotification++;
        
        int aggregationTicks = XPConfig.get().getNotificationAggregationTicks();
        
        // Check if it's time to dispatch notifications
        if (resource.ticksSinceLastNotification >= aggregationTicks) {
            dispatchNotifications(resource, store);
            resource.ticksSinceLastNotification = 0;
        }
    }
    
    /**
     * Dispatch all pending XP notifications.
     */
    private void dispatchNotifications(@Nonnull AggregationResource resource, @Nonnull Store<EntityStore> store) {
        if (resource.pendingNotifications.isEmpty()) {
            return;
        }
        
        for (Map.Entry<Ref<EntityStore>, PendingXPNotification> entry : resource.pendingNotifications.entrySet()) {
            Ref<EntityStore> entityRef = entry.getKey();
            PendingXPNotification pending = entry.getValue();
            
            // Skip if entity is no longer valid
            if (!entityRef.isValid()) {
                continue;
            }
            
            // Only notify if there's XP to show
            if (pending.totalCharacterXp > 0 || pending.totalClassXp > 0) {
                XPGainNotificationEvent event = new XPGainNotificationEvent(
                        entityRef,
                        pending.totalCharacterXp,
                        pending.totalClassXp,
                        pending.activeClassId,
                        Collections.unmodifiableMap(new EnumMap<>(pending.sourceBreakdown))
                );
                
                HytaleServer.get().getEventBus()
                        .dispatchFor(XPGainNotificationEvent.class)
                        .dispatch(event);
                
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(String.format("Dispatched XP notification: entity=%s, charXP=%d, classXP=%d",
                            entityRef, pending.totalCharacterXp, pending.totalClassXp));
                }
            }
        }
        
        // Clear pending notifications
        resource.pendingNotifications.clear();
    }
    
    /**
     * Record an XP gain for later notification dispatch.
     * <p>
     * Called by XPAwardSystem after processing an XP award.
     * 
     * @param store the entity store
     * @param entityRef the entity receiving XP
     * @param characterXp character XP amount
     * @param classXp class XP amount
     * @param source the XP source category
     * @param activeClassId the player's active class (may be null)
     */
    public static void recordXPGain(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            long characterXp,
            long classXp,
            @Nonnull XPSource source,
            String activeClassId
    ) {
        if (resourceType == null) {
            // System not initialized yet
            return;
        }
        
        AggregationResource resource = store.getResource(resourceType);
        if (resource == null) {
            return;
        }
        
        PendingXPNotification pending = resource.pendingNotifications.computeIfAbsent(
                entityRef, 
                k -> new PendingXPNotification()
        );
        
        pending.totalCharacterXp += characterXp;
        pending.totalClassXp += classXp;
        pending.activeClassId = activeClassId;
        pending.sourceBreakdown.merge(source, characterXp + classXp, Long::sum);
    }
    
    /**
     * Get the resource type for the aggregation data.
     * <p>
     * Used by HyforgedPlugin to register the resource.
     */
    @Nonnull
    public ResourceType<EntityStore, AggregationResource> getResourceType() {
        return aggregationResourceType;
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Pending XP notification data for a single entity.
     */
    static class PendingXPNotification {
        long totalCharacterXp = 0;
        long totalClassXp = 0;
        String activeClassId = null;
        final EnumMap<XPSource, Long> sourceBreakdown = new EnumMap<>(XPSource.class);
    }
    
    /**
     * Resource holding aggregation state across ticks.
     */
    public static class AggregationResource implements Resource<EntityStore> {
        final Map<Ref<EntityStore>, PendingXPNotification> pendingNotifications = new HashMap<>();
        int ticksSinceLastNotification = 0;
        
        public AggregationResource() {
        }
        
        @Nonnull
        @Override
        public Resource<EntityStore> clone() {
            AggregationResource clone = new AggregationResource();
            // Note: We don't clone pending notifications since they're transient per-world
            clone.ticksSinceLastNotification = this.ticksSinceLastNotification;
            return clone;
        }
    }
}
