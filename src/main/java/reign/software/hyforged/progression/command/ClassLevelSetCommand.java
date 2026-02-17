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
import reign.software.hyforged.progression.ClassProgression;
import reign.software.hyforged.progression.component.ProgressionComponent;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;

/**
 * Command to set class level for a player.
 * <p>
 * Usage: {@code /hyforged progression level classset <player> <classId> <level>}
 */
public class ClassLevelSetCommand extends CommandBase {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("Player not found or not in a world.");
    private static final Message MESSAGE_NO_COMPONENT = Message.raw("Player does not have a progression component.");
    private static final Message MESSAGE_INVALID_LEVEL = Message.raw("Class level must be between 1 and " + ClassProgression.MAX_LEVEL + ".");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player",
            "hyforged.commands.progression.level.classset.player.desc",
            ArgTypes.PLAYER_REF
    );

    @Nonnull
    private final RequiredArg<String> classIdArg = this.withRequiredArg(
            "classId",
            "hyforged.commands.progression.level.classset.class.desc",
            ArgTypes.STRING
    );

    @Nonnull
    private final RequiredArg<Integer> levelArg = this.withRequiredArg(
            "level",
            "hyforged.commands.progression.level.classset.level.desc",
            ArgTypes.INTEGER
    );

    public ClassLevelSetCommand() {
        super("classset", "hyforged.commands.progression.level.classset.desc");
        this.requirePermission("hyforged.admin.progression.level");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef targetPlayerRef = this.playerArg.get(context);
        String classId = this.classIdArg.get(context);
        int level = this.levelArg.get(context);

        if (level < 1 || level > ClassProgression.MAX_LEVEL) {
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

            ProgressionComponent.ClassProgressionData classData = progression.getOrCreateClassProgression(classId);
            int oldLevel = classData.level;
            
            // Set level directly
            classData.level = level;
            progression.markDirty();

            // Audit log
            LOGGER.atInfo().log("[AUDIT] Admin %s set class level for player %s class %s from %d to %d",
                    adminName, playerName, classId, oldLevel, level);

            context.sendMessage(Message.raw(String.format(
                    "Set class level for %s's class %s from %d to %d.",
                    playerName, classId, oldLevel, level)));
        });
    }
}
