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

import javax.annotation.Nonnull;

/**
 * Command to view a player's Tradebar balance.
 * <p>
 * Usage: {@code /hyforged currency balance <player>}
 */
public class CurrencyBalanceCommand extends CommandBase {

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("Player not found or not in a world.");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player",
            "hyforged.commands.currency.balance.player.desc",
            ArgTypes.PLAYER_REF
    );

    public CurrencyBalanceCommand() {
        super("balance", "hyforged.commands.currency.balance.desc");
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

            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            String playerName = playerRefComponent != null ? playerRefComponent.getUsername() : "Unknown";

            int balance = CurrencyService.get().getBalance(ref);

            context.sendMessage(Message.raw(String.format(
                    "%s has %,d Tradebars",
                    playerName, balance)));
        });
    }
}
