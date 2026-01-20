package reign.software.hyforged.affix.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.affix.ui.CharacterStatsPage;

import javax.annotation.Nonnull;

/**
 * Command to open the Character Stats screen.
 * <p>
 * Usage: {@code /hyforged character} or {@code /hyforged char}
 * <p>
 * Opens the character stats page showing:
 * <ul>
 *   <li>All character stats with breakdowns</li>
 *   <li>Equipment affix contributions</li>
 *   <li>Base values and effective values</li>
 * </ul>
 */
public class CharacterStatsCommand extends AbstractPlayerCommand {
    
    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("§cCould not find player component.");
    
    public CharacterStatsCommand() {
        super("character", "hyforged.commands.character.desc");
        this.addAliases("char", "stats-screen");
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
        
        // Open the character stats page
        CharacterStatsPage page = new CharacterStatsPage(playerRef);
        playerComponent.getPageManager().openCustomPage(ref, store, page);
    }
}
