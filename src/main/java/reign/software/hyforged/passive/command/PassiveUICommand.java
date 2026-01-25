package reign.software.hyforged.passive.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.passive.ui.PassiveTreePage;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Command to open the passive tree UI for the player.
 * <p>
 * Usage: {@code /hyforged passive ui}
 * <p>
 * Opens the interactive passive tree page where players can view and allocate nodes.
 */
public class PassiveUICommand extends AbstractAsyncCommand {

    private static final Message MESSAGE_PLAYER_NOT_IN_WORLD = Message.raw("§cPlayer is not in a world.");

    public PassiveUICommand() {
        super("ui", "hyforged.commands.passive.ui.desc");
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        CommandSender sender = context.sender();
        if (sender instanceof Player player) {
            player.getWorldMapTracker().tick(0);
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                return CompletableFuture.runAsync(() -> {
                    PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRefComponent != null) {
                        PassiveTreePage page = new PassiveTreePage(playerRefComponent);
                        player.getPageManager().openCustomPage(ref, store, page);
                    }
                }, world);
            } else {
                context.sendMessage(MESSAGE_PLAYER_NOT_IN_WORLD);
                return CompletableFuture.completedFuture(null);
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}
