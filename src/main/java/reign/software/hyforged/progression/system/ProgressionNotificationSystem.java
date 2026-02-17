package reign.software.hyforged.progression.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import reign.software.hyforged.progression.event.LevelUpNotificationEvent;
import reign.software.hyforged.util.MessageColors;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * System that sends in-game notifications for level-up events.
 * <p>
 * Listens for:
 * - {@link LevelUpNotificationEvent}: Shows character and class level-up notifications
 *   including passive points granted
 * <p>
 * Notifications appear as toast-style messages (like item pickup) with appropriate icons.
 */
public class ProgressionNotificationSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Icons for notifications
    private static final String ICON_LEVEL_UP = "Item_ScribblesAndNotes_Paper_Scroll";

    private EventRegistration<Void, LevelUpNotificationEvent> levelUpRegistration;

    public ProgressionNotificationSystem() {
        registerEventHandlers();
    }

    private void registerEventHandlers() {
        levelUpRegistration = HytaleServer.get().getEventBus()
                .register(LevelUpNotificationEvent.class, this::onLevelUp);
        
        LOGGER.atInfo().log("ProgressionNotificationSystem: Registered notification event handlers");
    }

    /**
     * Handle level-up notification events.
     */
    private void onLevelUp(@Nonnull LevelUpNotificationEvent event) {
        Ref<EntityStore> entityRef = event.entityRef();
        
        if (!entityRef.isValid()) {
            return;
        }
        
        Store<EntityStore> store = entityRef.getStore();
        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        
        if (playerRef == null) {
            return;
        }
        
        Message primaryMessage;
        Message secondaryMessage;
        
        if (event.isCharacterLevel()) {
            // Character level-up
            primaryMessage = Message.translation("hyforged.notification.levelUp.character.title")
                    .color(MessageColors.GOLD);
            
            if (event.rewardsGranted() > 0) {
                secondaryMessage = Message.translation("hyforged.notification.levelUp.character.withPoints")
                        .param("level", event.newLevel())
                        .param("points", event.rewardsGranted())
                        .color(MessageColors.WHITE);
            } else {
                secondaryMessage = Message.translation("hyforged.notification.levelUp.character.simple")
                        .param("level", event.newLevel())
                        .color(MessageColors.WHITE);
            }
        } else {
            // Class level-up
            String classId = event.classId() != null ? event.classId() : "unknown";
            
            primaryMessage = Message.translation("hyforged.notification.levelUp.class.title")
                    .param("classId", classId)
                    .color(MessageColors.GOLD);
            
            if (event.rewardsGranted() > 0) {
                secondaryMessage = Message.translation("hyforged.notification.levelUp.class.withPoints")
                        .param("level", event.newLevel())
                        .param("points", event.rewardsGranted())
                        .color(MessageColors.WHITE);
            } else {
                secondaryMessage = Message.translation("hyforged.notification.levelUp.class.simple")
                        .param("level", event.newLevel())
                        .color(MessageColors.WHITE);
            }
        }
        
        NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                primaryMessage,
                secondaryMessage,
                ICON_LEVEL_UP
        );
        
        LOGGER.at(Level.FINE).log("Sent level-up notification: type=%s, level=%d, rewards=%d",
                event.levelType(), event.newLevel(), event.rewardsGranted());
    }

    /**
     * Cleanup registrations when the system is disposed.
     */
    public void dispose() {
        if (levelUpRegistration != null) {
            levelUpRegistration.unregister();
        }
        LOGGER.atInfo().log("ProgressionNotificationSystem: Unregistered event handlers");
    }
}
