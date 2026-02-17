package reign.software.hyforged.system;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.component.PlayerSpellsComponent;
import reign.software.hyforged.passive.component.PlayerUnlocksComponent;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.concentration.ConcentrationPriorityComponent;

import javax.annotation.Nonnull;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.logging.Level;

/**
 * Ensures all player-scoped Hyforged components exist when a player connects.
 * <p>
 * For returning players, persisted components are already on the Holder and
 * {@code ensureComponent} is a no-op. For new players, default instances are
 * created via the registered codec / default constructor.
 * <p>
 * This must be registered <b>before</b> systems that read these components
 * (e.g., PassiveTreeMigrationSystem, ClassTreeStartingNodeSystem).
 */
public class HyforgedPlayerInitSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;
    private final ComponentType<EntityStore, ProgressionComponent> progressionComponentType;
    private final ComponentType<EntityStore, PassiveTreeComponent> passiveTreeComponentType;
    private final ComponentType<EntityStore, PlayerUnlocksComponent> playerUnlocksComponentType;
    private final ComponentType<EntityStore, PlayerSpellsComponent> playerSpellsComponentType;
    private final ComponentType<EntityStore, ConcentrationPriorityComponent> concentrationPriorityComponentType;

    public HyforgedPlayerInitSystem() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.progressionComponentType = plugin.getProgressionComponentType();
        this.passiveTreeComponentType = plugin.getPassiveTreeComponentType();
        this.playerUnlocksComponentType = plugin.getPlayerUnlocksComponentType();
        this.playerSpellsComponentType = plugin.getPlayerSpellsComponentType();
        this.concentrationPriorityComponentType = plugin.getConcentrationPriorityComponentType();

        registerEventHandlers();
    }

    private void registerEventHandlers() {
        HytaleServer.get().getEventBus()
                .register(PlayerConnectEvent.class, this::onPlayerConnect);

        LOGGER.atInfo().log("HyforgedPlayerInitSystem: Registered player connect handler");
    }

    private void onPlayerConnect(@Nonnull PlayerConnectEvent event) {
        Holder<EntityStore> holder = event.getHolder();

        // Ensure all player-scoped components exist.
        // For returning players these are already loaded from persistence (no-op).
        // For new players a default instance is created via the registered codec.
        holder.ensureComponent(statComponentType);
        holder.ensureComponent(progressionComponentType);
        holder.ensureComponent(passiveTreeComponentType);
        holder.ensureComponent(playerUnlocksComponentType);
        holder.ensureComponent(playerSpellsComponentType);
        holder.ensureComponent(concentrationPriorityComponentType);

        LOGGER.at(Level.FINE).log("Ensured Hyforged components for player %s",
                (event.getPlayerRef() != null ? event.getPlayerRef().getUsername() : "unknown"));
    }
}
