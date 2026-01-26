package reign.software.hyforged.combat.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.combat.hud.CombatLogHudSystem;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Command to toggle the combat log HUD visibility.
 * <p>
 * Usage: {@code /hyforged combatloghud}
 * <p>
 * Toggles the WoW-style combat log HUD on/off.
 */
public class CombatLogHudCommand extends AbstractPlayerCommand {

    private static final Message MESSAGE_NO_UUID = Message.raw("Could not find player UUID.");
    private static final Message MESSAGE_HUD_ENABLED = Message.raw("Combat log HUD enabled. Combat events will be displayed in the corner.");
    private static final Message MESSAGE_HUD_DISABLED = Message.raw("Combat log HUD disabled.");

    public CombatLogHudCommand() {
        super("combatloghud", "hyforged.commands.combatloghud.desc");
        this.addAliases("cloghud", "combathud");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        // Get player UUID
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            context.sendMessage(MESSAGE_NO_UUID);
            return;
        }
        
        UUID playerUuid = uuidComponent.getUuid();
        
        // Toggle HUD visibility
        boolean newState = CombatLogHudSystem.toggleHudVisibility(playerUuid);
        
        if (newState) {
            context.sendMessage(MESSAGE_HUD_ENABLED);
        } else {
            context.sendMessage(MESSAGE_HUD_DISABLED);
        }
    }
}
