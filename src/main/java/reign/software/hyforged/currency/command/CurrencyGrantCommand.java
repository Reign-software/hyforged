package reign.software.hyforged.currency.command;

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
import reign.software.hyforged.currency.service.CurrencyService;
import reign.software.hyforged.currency.service.TransactionResult;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Command to grant Tradebars to a player (admin command).
 * <p>
 * Usage: {@code /hyforged currency grant <player> <amount>}
 */
public class CurrencyGrantCommand extends CommandBase {

    private static final Logger LOGGER = Logger.getLogger(CurrencyGrantCommand.class.getName());

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("Player not found or not in a world.");
    private static final Message MESSAGE_INVALID_AMOUNT = Message.raw("Amount must be positive.");
    private static final Message MESSAGE_GRANT_FAILED = Message.raw("Failed to grant Tradebars. Check player inventory space.");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player",
            "hyforged.commands.currency.grant.player.desc",
            ArgTypes.PLAYER_REF
    );

    @Nonnull
    private final RequiredArg<Integer> amountArg = this.withRequiredArg(
            "amount",
            "hyforged.commands.currency.grant.amount.desc",
            ArgTypes.INTEGER
    );

    public CurrencyGrantCommand() {
        super("grant", "hyforged.commands.currency.grant.desc");
        this.requirePermission("hyforged.admin.currency");
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

            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            String playerName = playerRefComponent != null ? playerRefComponent.getUsername() : "Unknown";
            String adminName = context.isPlayer()
                    ? context.senderAs(Player.class).getDisplayName()
                    : "Console";

            int oldBalance = CurrencyService.get().getBalance(ref);
            
            TransactionResult result = CurrencyService.get().deposit(ref, amount, "admin_grant:" + adminName);
            
            if (!result.success()) {
                context.sendMessage(MESSAGE_GRANT_FAILED);
                LOGGER.warning(String.format("[AUDIT] Admin %s failed to grant %d Tradebars to %s: %s",
                        adminName, amount, playerName, result.failureReason()));
                return;
            }

            int newBalance = CurrencyService.get().getBalance(ref);

            LOGGER.info(String.format("[AUDIT] Admin %s granted %d Tradebars to %s (was: %d, now: %d)",
                    adminName, amount, playerName, oldBalance, newBalance));

            context.sendMessage(Message.raw(String.format(
                    "Granted %,d Tradebars to %s. Balance: %,d → %,d",
                    amount, playerName, oldBalance, newBalance)));
        });
    }
}
