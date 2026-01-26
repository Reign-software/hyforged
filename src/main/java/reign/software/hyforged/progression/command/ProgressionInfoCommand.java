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

/**
 * Command to display progression info for a player.
 * <p>
 * Usage: {@code /hyforged progression info <player>}
 */
public class ProgressionInfoCommand extends CommandBase {

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("Player not found or not in a world.");
    private static final Message MESSAGE_NO_COMPONENT = Message.raw("Player does not have a progression component.");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player",
            "hyforged.commands.progression.info.player.desc",
            ArgTypes.PLAYER_REF
    );

    public ProgressionInfoCommand() {
        super("info", "hyforged.commands.progression.info.desc");
        this.requirePermission("hyforged.admin.progression.info");
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

            showProgressionInfo(context, playerName, progression);
        });
    }

    /**
     * Display progression information for a player.
     */
    private void showProgressionInfo(
            @Nonnull CommandContext context,
            @Nonnull String playerName,
            @Nonnull ProgressionComponent progression
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════ Progression for ").append(playerName).append(" ═══════\n");

        // Character progression
        sb.append("\n▸ Character\n");
        sb.append("  Level: ").append(progression.getCharacterLevel())
          .append("/").append(CharacterProgression.MAX_LEVEL).append("\n");
        sb.append("  XP: ").append(progression.getCharacterXp())
          .append("/").append(progression.getCharacterXpToNext()).append("\n");
        sb.append("  General Passive Points: ")
          .append(progression.getAvailableGeneralPassivePoints())
          .append(" (").append(progression.getGeneralPassivePointsAllocated())
          .append(" allocated)\n");

        // Active class
        String activeClass = progression.getActiveClassId();
        sb.append("\n▸ Active Class\n");
        if (activeClass != null) {
            ProgressionComponent.ClassProgressionData classData = progression.getClassProgression(activeClass);
            if (classData != null) {
                sb.append("  Class: ").append(activeClass).append("\n");
                sb.append("  Level: ").append(classData.level).append("/20\n");
                sb.append("  XP: ").append(classData.xp).append("\n");
            } else {
                sb.append("  Class: ").append(activeClass).append(" (no progression data)\n");
            }
        } else {
            sb.append("  None\n");
        }

        // All class progressions
        java.util.Set<String> classIds = progression.getClassIds();
        if (!classIds.isEmpty()) {
            sb.append("\n▸ All Class Progressions\n");
            for (String classId : classIds) {
                ProgressionComponent.ClassProgressionData data = progression.getClassProgression(classId);
                if (data != null) {
                    String marker = classId.equals(activeClass) ? "» " : "  ";
                    sb.append(marker).append("").append(classId)
                      .append(": Lv.").append(data.level)
                      .append(" (").append(data.xp).append(" XP)\n");
                }
            }
        }

        context.sendMessage(Message.raw(sb.toString()));
    }
}
