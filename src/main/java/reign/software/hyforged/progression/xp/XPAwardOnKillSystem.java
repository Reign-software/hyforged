package reign.software.hyforged.progression.xp;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.HyforgedPlugin;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * System that awards XP to players when they kill entities.
 * <p>
 * Extends Hytale's OnDeathSystem to react when DeathComponent is added to an entity.
 * Checks if the killer is a player with a ProgressionComponent and dispatches
 * an XPAwardEvent based on the killed entity's configured XP value.
 * <p>
 * XP values are currently static but will be data-driven via JSON config in future phases.
 */
public class XPAwardOnKillSystem extends DeathSystems.OnDeathSystem {
    
    private static final Logger LOGGER = Logger.getLogger(XPAwardOnKillSystem.class.getName());
    
    public XPAwardOnKillSystem() {
        super();
    }
    
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        // We want to process all entities with DeathComponent (killed entities)
        // The base OnDeathSystem handles the DeathComponent filter already
        return DeathComponent.getComponentType();
    }
    
    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> victimRef,
            @Nonnull DeathComponent deathComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Get the damage info from the death to find the killer
        Damage deathInfo = deathComponent.getDeathInfo();
        if (deathInfo == null) {
            return;
        }
        
        Damage.Source source = deathInfo.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            // Not killed by an entity (e.g., environment, command)
            return;
        }
        
        Ref<EntityStore> killerRef = entitySource.getRef();
        if (killerRef == null || !killerRef.isValid()) {
            return;
        }
        
        // Check if the killer is a player
        Player killerPlayer = store.getComponent(killerRef, Player.getComponentType());
        if (killerPlayer == null) {
            return;
        }
        
        // Check if the killer has a ProgressionComponent
        ProgressionComponent killerProgression = store.getComponent(killerRef, 
                HyforgedPlugin.getInstance().getProgressionComponentType());
        if (killerProgression == null) {
            return;
        }
        
        // Calculate XP to award
        long xpAmount = calculateXpForKill(victimRef, store);
        if (xpAmount <= 0) {
            return;
        }
        
        // Dispatch XP award event to the killer
        XPAwardEvent xpEvent = XPAwardEvent.combat(xpAmount, victimRef);
        commandBuffer.invoke(killerRef, xpEvent);
        
        LOGGER.fine(String.format("Awarding %d combat XP to player for killing entity", xpAmount));
    }
    
    /**
     * Calculate XP to award for killing an entity.
     * <p>
     * Uses XPConfig for base values and scaling.
     * TODO: Future enhancement - read level/difficulty from NPC stat template
     * 
     * @param victimRef the killed entity
     * @param store the entity store
     * @return XP amount to award
     */
    private long calculateXpForKill(Ref<EntityStore> victimRef, Store<EntityStore> store) {
        // Get base XP from config with level/difficulty scaling
        // TODO: Look up victim's level and difficulty from NPC template or tags
        int enemyLevel = 1;  // Default level
        String difficulty = "normal";  // Default difficulty
        
        return XPConfig.get().calculateCombatXp(enemyLevel, difficulty);
    }
}
