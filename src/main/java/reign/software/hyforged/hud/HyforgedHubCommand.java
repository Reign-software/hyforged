package reign.software.hyforged.hud;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Command to open the Hyforged Hub page.
 * <p>
 * Usage: {@code /hf}
 * <p>
 * Opens the central Hyforged menu for navigating to
 * Character Stats, Passive Tree, Concentration, and Options.
 */
public class HyforgedHubCommand extends AbstractPlayerCommand {

    public HyforgedHubCommand() {
        super("hf", "hyforged.commands.hub.desc");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        player.getPageManager().openCustomPage(ref, store, new HyforgedHubPage(playerRef));
    }
}
