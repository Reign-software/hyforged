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
import reign.software.hyforged.progression.CharacterProgression;
import reign.software.hyforged.progression.component.ProgressionComponent;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Command to set character level for a player.
 * <p>
 * Usage: {@code /hyforged progression level set <player> <level>}
 */
public class LevelSetCommand extends CommandBase {

    private static final Logger LOGGER = Logger.getLogger(LevelSetCommand.class.getName());

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("Player not found or not in a world.");
    private static final Message MESSAGE_NO_COMPONENT = Message.raw("Player does not have a progression component.");
    private static final Message MESSAGE_INVALID_LEVEL = Message.raw("Level must be between 1 and " + CharacterProgression.MAX_LEVEL + ".");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player",
            "hyforged.commands.progression.level.set.player.desc",
            ArgTypes.PLAYER_REF
    );

    @Nonnull
    private final RequiredArg<Integer> levelArg = this.withRequiredArg(
            "level",
            "hyforged.commands.progression.level.set.level.desc",
            ArgTypes.INTEGER
    );

    public LevelSetCommand() {
        super("set", "hyforged.commands.progression.level.set.desc");
        this.requirePermission("hyforged.admin.progression.level");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef targetPlayerRef = this.playerArg.get(context);
        int level = this.levelArg.get(context);

        if (level < 1 || level > CharacterProgression.MAX_LEVEL) {
            context.sendMessage(MESSAGE_INVALID_LEVEL);
            return;
        }

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

            int oldLevel = progression.getCharacterLevel();
            
            // Set level directly
            progression.setCharacterLevel(level);

            // Audit log
            LOGGER.info(String.format("[AUDIT] Admin %s set character level for player %s from %d to %d",
                    adminName, playerName, oldLevel, level));

            context.sendMessage(Message.raw(String.format(
                    "Set character level for %s from %d to %d.",
                    playerName, oldLevel, level)));
        });
    }
}
