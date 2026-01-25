package reign.software.hyforged.passive.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.passive.ui.PassiveTreePage;

import javax.annotation.Nonnull;

/**
 * Command to open the passive tree UI for a player.
 * <p>
 * Usage: {@code /passive ui <player>}
 * <p>
 * Opens the interactive passive tree page where players can view and allocate nodes.
 * <p>
 * This command extends {@link AbstractWorldCommand} to ensure it runs on the world thread,
 * which is required to safely access the entity store.
 */
public class PassiveUICommand extends AbstractWorldCommand {

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("§cPlayer not found or not in world.");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player", "hyforged.commands.passive.ui.player.desc", ArgTypes.PLAYER_REF);

    public PassiveUICommand() {
        super("ui", "hyforged.commands.passive.ui.desc");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
        PlayerRef targetPlayerRef = this.playerArg.get(context);
        Ref<EntityStore> ref = targetPlayerRef.getReference();
        
        if (ref == null || !ref.isValid()) {
            context.sendMessage(MESSAGE_PLAYER_NOT_FOUND);
            return;
        }
        
        Player player = store.getComponent(ref, Player.getComponentType());
        
        if (player == null) {
            context.sendMessage(MESSAGE_PLAYER_NOT_FOUND);
            return;
        }
        
        // Create and open the passive tree page for the player
        PassiveTreePage page = new PassiveTreePage(targetPlayerRef);
        player.getPageManager().openCustomPage(ref, store, page);
        
        context.sendMessage(Message.raw("§aOpened passive tree UI for " + targetPlayerRef.getUsername()));
    }
}
