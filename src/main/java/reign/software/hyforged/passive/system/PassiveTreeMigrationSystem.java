package reign.software.hyforged.passive.system;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.migration.PassiveTreeMigrationService;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * System that runs passive tree migrations when a player connects.
 * <p>
 * When tree definitions change (nodes removed, restructured), this system
 * checks for affected allocations and auto-refunds them without cost.
 * Players are notified of any changes on login.
 */
public class PassiveTreeMigrationSystem {

    private static final Logger LOGGER = Logger.getLogger(PassiveTreeMigrationSystem.class.getName());

    @SuppressWarnings("unused")
    private EventRegistration<Void, PlayerConnectEvent> connectRegistration;

    public PassiveTreeMigrationSystem() {
        registerEventHandlers();
    }

    private void registerEventHandlers() {
        connectRegistration = HytaleServer.get().getEventBus()
                .register(PlayerConnectEvent.class, this::onPlayerConnect);
        
        LOGGER.info("PassiveTreeMigrationSystem: Registered player connect event handler");
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
            LOGGER.fine("PassiveTreeMigrationSystem: Component type not available");
            return;
        }
        
        PassiveTreeComponent passiveComponent = holder.getComponent(componentType);
        if (passiveComponent == null) {
            LOGGER.fine(() -> "Player " + playerRef.getUsername() + " has no passive tree component");
            return;
        }
        
        // Check for and perform migrations
        PassiveTreeMigrationService.MigrationResult result = 
                PassiveTreeMigrationService.get().checkAndMigrate(entityRef, passiveComponent);
        
        if (result.migrationsPerformed()) {
            LOGGER.info(() -> String.format(
                    "PassiveTreeMigrationSystem: Migrated %d trees for player %s, refunded %d nodes",
                    result.treesMigrated().size(),
                    playerRef.getUsername(),
                    result.totalNodesRefunded()));
            
            // Send migration messages to player
            for (String message : result.messages()) {
                sendPlayerMessage(playerRef, message);
            }
        }
    }

    /**
     * Send a message to the player about migration results.
     */
    private void sendPlayerMessage(@Nonnull PlayerRef playerRef, @Nonnull String message) {
        // TODO: Use chat/notification system when available
        LOGGER.log(Level.FINE, "Migration message for {0}: {1}", 
                new Object[]{playerRef.getUsername(), message});
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
