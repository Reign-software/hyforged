package reign.software.hyforged.passive.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.passive.ui.PassiveTreePage;

import javax.annotation.Nonnull;

/**
 * Command to open the passive tree UI for a player.
 * <p>
 * Usage: {@code /passive ui <player>}
 * <p>
 * Opens the interactive passive tree page where players can view and allocate nodes.
 */
public class PassiveUICommand extends CommandBase {

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("§cPlayer not found or not in world.");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player", "hyforged.commands.passive.ui.player.desc", ArgTypes.PLAYER_REF);

    public PassiveUICommand() {
        super("ui", "hyforged.commands.passive.ui.desc");
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
