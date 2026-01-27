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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.service.PassiveTreeService;
import reign.software.hyforged.util.MessageColors;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Command to list a player's passive tree allocations.
 * <p>
 * Usage: {@code /passive list <player>}
 * <p>
 * Shows:
 * <ul>
 *   <li>Tree name and starting node (for general)</li>
 *   <li>Allocated node count</li>
 *   <li>Available points</li>
 * </ul>
 */
public class PassiveListCommand extends CommandBase {

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("Player not found or not in world.").color(MessageColors.ERROR);
    private static final Message MESSAGE_NO_COMPONENT = Message.raw("Player has no passive tree data.").color(MessageColors.ERROR);
    private static final Message MESSAGE_GENERAL_TREE = Message.raw("[General Tree]").color(MessageColors.WARNING);

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player", "hyforged.commands.passive.list.player.desc", ArgTypes.PLAYER_REF);

    public PassiveListCommand() {
        super("list", "hyforged.commands.passive.list.desc");
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

            PassiveTreeComponent passiveComponent = store.getComponent(ref, 
                    reign.software.hyforged.HyforgedPlugin.getInstance().getPassiveTreeComponentType());
            
            if (passiveComponent == null) {
                context.sendMessage(MESSAGE_NO_COMPONENT);
                return;
            }

            // Get username
            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            String username = playerRefComponent != null ? playerRefComponent.getUsername() : "Unknown";
            
            // Header
            context.sendMessage(Message.raw(String.format("=== Passive Tree Allocations for %s ===", username)).color(MessageColors.GOLD));

            // General tree info
            context.sendMessage(MESSAGE_GENERAL_TREE);
            
            String startingNode = passiveComponent.getGeneralStartingNode();
            context.sendMessage(Message.raw(String.format("  Starting Node: %s", 
                    startingNode != null ? startingNode : "(not chosen)")).color(MessageColors.GRAY));
            
            int generalAllocated = passiveComponent.getGeneralAllocatedCount();
            context.sendMessage(Message.raw(String.format("  Allocated Nodes: %d", generalAllocated)).color(MessageColors.GRAY));
            
            int availableGeneral = PassiveTreeService.get().getAvailableGeneralPoints(ref);
            context.sendMessage(Message.raw(String.format("  Available Points: %d", availableGeneral)).color(MessageColors.SUCCESS));
            
            context.sendMessage(Message.raw(String.format("  Book Points Used: %d", 
                    passiveComponent.getBookPointsUsed())).color(MessageColors.GRAY));

            // Class tree info
            Set<String> classIds = passiveComponent.getClassIdsWithAllocations();
            if (!classIds.isEmpty()) {
                for (String classId : classIds) {
                    context.sendMessage(Message.raw(String.format("[Class Tree: %s]", classId)).color(MessageColors.WARNING));
                    
                    int classAllocated = passiveComponent.getClassAllocatedCount(classId);
                    context.sendMessage(Message.raw(String.format("  Allocated Nodes: %d", classAllocated)).color(MessageColors.GRAY));
                    
                    int availableClass = PassiveTreeService.get().getAvailableClassPoints(ref, classId);
                    context.sendMessage(Message.raw(String.format("  Available Points: %d", availableClass)).color(MessageColors.SUCCESS));
                }
            }
        });
    }
}
