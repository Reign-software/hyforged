package reign.software.hyforged.hub.system;

import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import reign.software.hyforged.util.MessageColors;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * System that sends a welcome message to players when they connect.
 * <p>
 * Informs players about available Hyforged commands like the character hub
 * and passive tree.
 */
public class WelcomeMessageSystem {

    private static final Logger LOGGER = Logger.getLogger(WelcomeMessageSystem.class.getName());

    private static final Message WELCOME_HEADER = Message.raw("=== Welcome to Hyforged! ===").color(MessageColors.GOLD);
    private static final Message WELCOME_HUB = Message.raw("  /hyforged hub").color(MessageColors.SUCCESS)
            .insert(Message.raw(" - Open the Character Hub (stats, passives, resources)").color(MessageColors.GRAY));
    private static final Message WELCOME_PASSIVE = Message.raw("  /hyforged passive ui").color(MessageColors.SUCCESS)
            .insert(Message.raw(" - Open the Passive Tree directly").color(MessageColors.GRAY));
    private static final Message WELCOME_ALIASES = Message.raw("  Tip: ").color(MessageColors.GRAY)
            .insert(Message.raw("/hyforged c").color(MessageColors.SUCCESS))
            .insert(Message.raw(" is a shortcut for the hub!").color(MessageColors.GRAY));

    @SuppressWarnings("unused")
    private EventRegistration<Void, PlayerConnectEvent> connectRegistration;

    public WelcomeMessageSystem() {
        registerEventHandlers();
    }

    private void registerEventHandlers() {
        connectRegistration = HytaleServer.get().getEventBus()
                .register(PlayerConnectEvent.class, this::onPlayerConnect);
        
        LOGGER.info("WelcomeMessageSystem: Registered player connect event handler");
    }

    /**
     * Handle player connect events by sending welcome messages.
     *
     * @param event The player connect event
     */
    private void onPlayerConnect(@Nonnull PlayerConnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        
        if (playerRef == null) {
            return;
        }
        
        // Send welcome messages
        playerRef.sendMessage(WELCOME_HEADER);
        playerRef.sendMessage(WELCOME_HUB);
        playerRef.sendMessage(WELCOME_PASSIVE);
        playerRef.sendMessage(WELCOME_ALIASES);
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
