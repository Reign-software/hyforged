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
 * Command to add character XP to a player.
 * <p>
 * Usage: {@code /hyforged progression xp add <player> <amount>}
 */
public class XPAddCommand extends CommandBase {

    private static final Logger LOGGER = Logger.getLogger(XPAddCommand.class.getName());

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("§cPlayer not found or not in a world.");
    private static final Message MESSAGE_NO_COMPONENT = Message.raw("§cPlayer does not have a progression component.");
    private static final Message MESSAGE_INVALID_AMOUNT = Message.raw("§cXP amount must be positive.");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player",
            "hyforged.commands.progression.xp.add.player.desc",
            ArgTypes.PLAYER_REF
    );

    @Nonnull
    private final RequiredArg<Integer> amountArg = this.withRequiredArg(
            "amount",
            "hyforged.commands.progression.xp.add.amount.desc",
            ArgTypes.INTEGER
    );

    public XPAddCommand() {
        super("add", "hyforged.commands.progression.xp.add.desc");
        this.requirePermission("hyforged.admin.progression.xp");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef targetPlayerRef = this.playerArg.get(context);
        int amount = this.amountArg.get(context);

        if (amount <= 0) {
            context.sendMessage(MESSAGE_INVALID_AMOUNT);
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
            long oldXp = progression.getCharacterXp();
            
            // Add XP directly
            progression.addCharacterXp(amount);
            
            int newLevel = progression.getCharacterLevel();
            long newXp = progression.getCharacterXp();

            // Audit log
            LOGGER.info(String.format("[AUDIT] Admin %s added %d XP to player %s (was: Lv%d/%dXP, now: Lv%d/%dXP)",
                    adminName, amount, playerName, oldLevel, oldXp, newLevel, newXp));

            context.sendMessage(Message.raw(String.format(
                    "§aAdded §f%d§a XP to §f%s§a. Level: §f%d§a → §f%d§a",
                    amount, playerName, oldLevel, newLevel)));
        });
    }
}
