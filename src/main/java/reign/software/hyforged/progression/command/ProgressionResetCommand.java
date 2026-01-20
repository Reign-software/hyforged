package reign.software.hyforged.progression.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.progression.component.ProgressionComponent;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Command to reset all progression for a player.
 * <p>
 * Usage: {@code /hyforged progression reset <player>}
 */
public class ProgressionResetCommand extends CommandBase {

    private static final Logger LOGGER = Logger.getLogger(ProgressionResetCommand.class.getName());

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("§cPlayer not found or not in a world.");
    private static final Message MESSAGE_NO_COMPONENT = Message.raw("§cPlayer does not have a progression component.");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player",
            "hyforged.commands.progression.reset.player.desc",
            ArgTypes.PLAYER_REF
    );

    public ProgressionResetCommand() {
        super("reset", "hyforged.commands.progression.reset.desc");
        this.requirePermission("hyforged.admin.progression.reset");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef targetPlayerRef = this.playerArg.get(context);
        Ref<EntityStore> ref = targetPlayerRef.getReference();

        if (ref == null || !ref.isValid()) {
            context.sendMessage(MESSAGE_PLAYER_NOT_FOUND);
            return;
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null) {
                context.sendMessage(MESSAGE_PLAYER_NOT_FOUND);
                return;
            }

            ProgressionComponent progression = store.getComponent(
                    ref,
                    HyforgedPlugin.getInstance().getProgressionComponentType()
            );

            if (progression == null) {
                context.sendMessage(MESSAGE_NO_COMPONENT);
                return;
            }

            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            String playerName = playerRefComponent != null ? playerRefComponent.getUsername() : "Unknown";
            String adminName = context.isPlayer() 
                    ? context.senderAs(Player.class).getDisplayName()
                    : "Console";

            // Capture old state for logging
            int oldLevel = progression.getCharacterLevel();
            long oldXp = progression.getCharacterXp();
            int classCount = progression.getClassIds().size();

            // Reset progression
            progression.reset();

            // Audit log
            LOGGER.info(String.format("[AUDIT] Admin %s reset progression for player %s (was: Lv%d/%dXP, %d classes)",
                    adminName, playerName, oldLevel, oldXp, classCount));

            context.sendMessage(Message.raw(String.format(
                    "§aReset all progression for §f%s§a. Character level reset to 1, all class progressions cleared.",
                    playerName)));
        });
    }
}
