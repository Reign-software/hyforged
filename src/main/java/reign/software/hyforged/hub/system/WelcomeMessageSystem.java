package reign.software.hyforged.hub.system;

import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import reign.software.hyforged.hub.resource.WelcomeMessagesConfigAsset;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * System that sends welcome messages to players when they connect.
 * <p>
 * Messages are loaded from JSON files in:
 * {@code Server/Hyforged/WelcomeMessages/}
 */
public class WelcomeMessageSystem {

    private static final Logger LOGGER = Logger.getLogger(WelcomeMessageSystem.class.getName());

    private static final List<Message> welcomeMessages = new ArrayList<>();
    
    private EventRegistration<Void, PlayerConnectEvent> connectRegistration;

    public WelcomeMessageSystem() {
        // Always register — assets load asynchronously after construction,
        // so hasMessages is typically false here. onPlayerConnect already
        // guards against an empty message list.
        registerEventHandlers();
    }

    /**
     * Apply configuration from loaded message assets.
     * Called by WelcomeMessagesConfigAssetLoader when assets are loaded.
     *
     * @param assets List of message assets, already sorted by order
     */
    public static void applyConfig(@Nonnull List<WelcomeMessagesConfigAsset> assets) {
        welcomeMessages.clear();

        for (WelcomeMessagesConfigAsset asset : assets) {
            Message msg = buildMessage(asset);
            if (msg != null) {
                welcomeMessages.add(msg);
            }
        }

        LOGGER.info("WelcomeMessageSystem: Loaded " + welcomeMessages.size() + " welcome messages");
    }

    /**
     * Build a Message from an asset's segments.
     * <p>
     * Each segment may specify either a {@code TranslationKey} (resolved via
     * {@link Message#translation(String)}) or a raw {@code Text} value.
     * When a translation key is present, the {@code Text} field is ignored.
     */
    private static Message buildMessage(@Nonnull WelcomeMessagesConfigAsset asset) {
        WelcomeMessagesConfigAsset.MessageSegment[] segments = asset.getSegments();
        if (segments.length == 0) {
            return null;
        }

        Message result = null;
        for (WelcomeMessagesConfigAsset.MessageSegment segment : segments) {
            Message part;
            if (segment.getTranslationKey() != null && !segment.getTranslationKey().isEmpty()) {
                part = Message.translation(segment.getTranslationKey());
            } else {
                part = Message.raw(segment.getText());
            }

            if (segment.getColor() != null) {
                part = part.color(segment.getColor());
            }

            if (result == null) {
                result = part;
            } else {
                result = result.insert(part);
            }
        }

        return result;
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

        if (playerRef == null || welcomeMessages.isEmpty()) {
            return;
        }

        for (Message msg : welcomeMessages) {
            playerRef.sendMessage(msg);
        }
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
