package reign.software.hyforged.progression.xp.objective;

import com.hypixel.hytale.builtin.adventure.objectives.Objective;
import com.hypixel.hytale.builtin.adventure.objectives.completion.ObjectiveCompletion;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.progression.xp.XPAwardEvent;
import reign.software.hyforged.progression.xp.XPConfig;
import reign.software.hyforged.progression.xp.XPSource;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Objective completion handler that awards XP to players when an objective is completed.
 * <p>
 * This handler is registered with Hytale's ObjectivePlugin and invoked when
 * objectives with the "hyforged:xp_award" completion type are completed.
 * <p>
 * XP amount can be specified explicitly or calculated from a tier.
 */
public class XPAwardCompletion extends ObjectiveCompletion {
    
    private static final Logger LOGGER = Logger.getLogger(XPAwardCompletion.class.getName());
    
    public XPAwardCompletion(@Nonnull XPAwardCompletionAsset asset) {
        super(asset);
    }
    
    @Nonnull
    public XPAwardCompletionAsset getAsset() {
        return (XPAwardCompletionAsset) super.getAsset();
    }
    
    @Override
    public void handle(@Nonnull Objective objective, @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
        XPAwardCompletionAsset xpAsset = getAsset();
        
        // Calculate XP amount
        long xpAmount = xpAsset.getXpAmount();
        if (xpAmount <= 0) {
            // Use tier-based calculation
            xpAmount = XPConfig.get().getObjectiveXp(xpAsset.getTier());
        }
        
        String objectiveId = objective.getObjectiveId();
        String sourceId = String.format("objective:%s", objectiveId);
        
        LOGGER.info(String.format("Awarding objective XP: %d for objective %s (tier=%s)",
                xpAmount, objectiveId, xpAsset.getTier()));
        
        // Get the world and store for dispatching events
        World world = Universe.get().getWorld(objective.getWorldUUID());
        if (world == null) {
            LOGGER.warning("Cannot award objective XP - world not found: " + objective.getWorldUUID());
            return;
        }
        
        Store<EntityStore> store = world.getEntityStore().getStore();
        final long finalXpAmount = xpAmount;
        
        // Award XP to all participants in the objective
        objective.forEachParticipant(participantRef -> {
            // Check if participant is a player with progression component
            Player playerComponent = componentAccessor.getComponent(participantRef, Player.getComponentType());
            if (playerComponent == null) {
                return; // Not a player
            }
            
            ProgressionComponent progression = store.getComponent(participantRef,
                    HyforgedPlugin.getInstance().getProgressionComponentType());
            if (progression == null) {
                return; // No progression component
            }
            
            // Dispatch XP award event directly via store
            XPAwardEvent xpAward = new XPAwardEvent(finalXpAmount, XPSource.OBJECTIVE, sourceId);
            store.invoke(participantRef, xpAward);
            
            PlayerRef playerRef = componentAccessor.getComponent(participantRef, PlayerRef.getComponentType());
            if (playerRef != null) {
                LOGGER.fine(String.format("Awarded %d XP to player %s for objective completion",
                        finalXpAmount, playerRef.getUuid()));
            }
        });
    }
}
