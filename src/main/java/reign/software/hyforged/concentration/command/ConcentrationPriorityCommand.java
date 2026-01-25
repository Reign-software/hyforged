package reign.software.hyforged.concentration.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.concentration.ui.ConcentrationPriorityPage;

import javax.annotation.Nonnull;

/**
 * Command to open the concentration priority queue UI.
 * <p>
 * Usage: {@code /hyforged concentration}
 */
public class ConcentrationPriorityCommand extends AbstractPlayerCommand {

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("§cCould not find player component.");

    public ConcentrationPriorityCommand() {
        super("concentration", "hyforged.commands.concentration.desc");
        this.addAliases("concentration-priority", "concentrationqueue", "priority", "conc");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            context.sendMessage(MESSAGE_PLAYER_NOT_FOUND);
            return;
        }

        ConcentrationPriorityPage page = new ConcentrationPriorityPage(playerRef);
        playerComponent.getPageManager().openCustomPage(ref, store, page);
    }
}
