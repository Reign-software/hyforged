package reign.software.hyforged.passive.system;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.migration.PassiveTreeMigrationService;
import reign.software.hyforged.passive.service.PassiveTreeService;
import reign.software.hyforged.util.MessageColors;

import javax.annotation.Nonnull;
import java.util.logging.Level;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * System that runs passive tree migrations when a player connects.
 * <p>
 * When tree definitions change (nodes removed, restructured), this system
 * checks for affected allocations and auto-refunds them without cost.
 * Players are notified of any changes on login.
 */
public class PassiveTreeMigrationSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private EventRegistration<Void, PlayerConnectEvent> connectRegistration;

    public PassiveTreeMigrationSystem() {
        registerEventHandlers();
    }

    private void registerEventHandlers() {
        connectRegistration = HytaleServer.get().getEventBus()
                .register(PlayerConnectEvent.class, this::onPlayerConnect);
        
        LOGGER.atInfo().log("PassiveTreeMigrationSystem: Registered player connect event handler");
    }

    /**
     * Handle player connect events by checking for and performing migrations.
     *
     * @param event The player connect event
     */
    private void onPlayerConnect(@Nonnull PlayerConnectEvent event) {
        Holder<EntityStore> holder = event.getHolder();
        PlayerRef playerRef = event.getPlayerRef();
        
        if (playerRef == null) {
            return;
        }
        
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null) {
            return;
        }
        
        // Get the passive tree component
        var componentType = HyforgedPlugin.getInstance().getPassiveTreeComponentType();
        if (componentType == null) {
            LOGGER.at(Level.FINE).log("PassiveTreeMigrationSystem: Component type not available");
            return;
        }
        
        PassiveTreeComponent passiveComponent = holder.getComponent(componentType);
        if (passiveComponent == null) {
            LOGGER.at(Level.FINE).log("Player %s has no passive tree component", playerRef.getUsername());
            return;
        }
        
        // Check for and perform migrations
        PassiveTreeMigrationService.MigrationResult result = 
                PassiveTreeMigrationService.get().checkAndMigrate(entityRef, passiveComponent);
        
        if (result.migrationsPerformed()) {
            LOGGER.atInfo().log(
                    "PassiveTreeMigrationSystem: Migrated %d trees for player %s, refunded %d nodes",
                    result.treesMigrated().size(),
                    playerRef.getUsername(),
                    result.totalNodesRefunded());
            
            // Send migration messages to player
            for (Message message : result.messages()) {
                sendPlayerMessage(entityRef, message);
            }
        }

        // Always restore passive effects on connect as a final consistency pass.
        // This guarantees passive stats/spells/unlocks are reapplied even if ECS
        // add-order timing skips the RefSystem-based restore path.
        int restored = PassiveTreeService.get().restoreAllEffects(entityRef, passiveComponent);
        if (restored > 0) {
            LOGGER.at(Level.FINE).log(
                    "PassiveTreeMigrationSystem: Restored %d passive nodes for player %s",
                    restored,
                    playerRef.getUsername()
            );
        }
    }

    /**
     * Send a message to the player about migration results.
     */
    private void sendPlayerMessage(@Nonnull Ref<EntityStore> entityRef, @Nonnull Message message) {
        if (!entityRef.isValid()) {
            return;
        }

        Player player = entityRef.getStore().getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        player.sendMessage(message.color(MessageColors.AQUA));
    }

    /**
     * Shutdown the system and unregister event handlers.
     */
    public void shutdown() {
        if (connectRegistration != null) {
            connectRegistration.unregister();
            connectRegistration = null;
        }
    }
}
