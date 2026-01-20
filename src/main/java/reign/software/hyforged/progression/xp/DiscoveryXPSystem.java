package reign.software.hyforged.progression.xp;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.DiscoverZoneEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.progression.component.ProgressionComponent;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * ECS event system that awards discovery XP when a player discovers a new zone.
 * <p>
 * Hooks into Hytale's {@link DiscoverZoneEvent.Display} event to dispatch XP awards
 * when players discover new map zones/regions.
 * <p>
 * XP amounts are determined by the zone's major/minor status from XPConfig.
 */
public class DiscoveryXPSystem extends EntityEventSystem<EntityStore, DiscoverZoneEvent.Display> {
    
    private static final Logger LOGGER = Logger.getLogger(DiscoveryXPSystem.class.getName());
    
    private final ComponentType<EntityStore, ProgressionComponent> progressionComponentType;
    
    public DiscoveryXPSystem() {
        super(DiscoverZoneEvent.Display.class);
        this.progressionComponentType = HyforgedPlugin.getInstance().getProgressionComponentType();
    }
    
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        // Match entities with ProgressionComponent (players)
        return progressionComponentType;
    }
    
    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull DiscoverZoneEvent.Display event
    ) {
        // Don't award XP if the event was cancelled
        if (event.isCancelled()) {
            return;
        }
        
        // Get the progression component to verify the entity can receive XP
        ProgressionComponent progression = archetypeChunk.getComponent(index, progressionComponentType);
        if (progression == null) {
            return;
        }
        
        Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
        
        // Determine XP based on zone type (major vs minor)
        var discoveryInfo = event.getDiscoveryInfo();
        boolean isMajor = discoveryInfo.major();
        
        XPConfig config = XPConfig.get();
        long xpAmount = isMajor ? config.getDiscoveryBiomeXp() : config.getDiscoveryLandmarkXp();
        
        String zoneName = discoveryInfo.zoneName();
        String regionName = discoveryInfo.regionName();
        String sourceId = String.format("discovery:%s/%s", regionName, zoneName);
        
        LOGGER.info(String.format("Awarding discovery XP: %d for zone %s (major=%b)", 
                xpAmount, sourceId, isMajor));
        
        // Create and dispatch XP award event
        XPAwardEvent xpAward = new XPAwardEvent(xpAmount, XPSource.DISCOVERY, sourceId);
        commandBuffer.invoke(entityRef, xpAward);
    }
}
