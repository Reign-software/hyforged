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

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("§cPlayer not found or not in world.");
    private static final Message MESSAGE_NO_COMPONENT = Message.raw("§cPlayer has no passive tree data.");
    private static final Message MESSAGE_HEADER = Message.raw("§6=== Passive Tree Allocations for %s ===");
    private static final Message MESSAGE_GENERAL_TREE = Message.raw("§e[General Tree]");
    private static final Message MESSAGE_STARTING_NODE = Message.raw("  §7Starting Node: §f%s");
    private static final Message MESSAGE_ALLOCATED = Message.raw("  §7Allocated Nodes: §f%d");
    private static final Message MESSAGE_AVAILABLE_POINTS = Message.raw("  §7Available Points: §a%d");
    private static final Message MESSAGE_BOOK_POINTS = Message.raw("  §7Book Points Used: §f%d");
    private static final Message MESSAGE_CLASS_TREE = Message.raw("§e[Class Tree: %s]");
    private static final Message MESSAGE_NO_ALLOCATIONS = Message.raw("  §7(No allocations)");

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
            context.sendMessage(Message.raw(String.format("§6=== Passive Tree Allocations for %s ===", username)));

            // General tree info
            context.sendMessage(MESSAGE_GENERAL_TREE);
            
            String startingNode = passiveComponent.getGeneralStartingNode();
            context.sendMessage(Message.raw(String.format("  §7Starting Node: §f%s", 
                    startingNode != null ? startingNode : "(not chosen)")));
            
            int generalAllocated = passiveComponent.getGeneralAllocatedCount();
            context.sendMessage(Message.raw(String.format("  §7Allocated Nodes: §f%d", generalAllocated)));
            
            int availableGeneral = PassiveTreeService.get().getAvailableGeneralPoints(ref);
            context.sendMessage(Message.raw(String.format("  §7Available Points: §a%d", availableGeneral)));
            
            context.sendMessage(Message.raw(String.format("  §7Book Points Used: §f%d", 
                    passiveComponent.getBookPointsUsed())));

            // Class tree info
            Set<String> classIds = passiveComponent.getClassIdsWithAllocations();
            if (!classIds.isEmpty()) {
                for (String classId : classIds) {
                    context.sendMessage(Message.raw(String.format("§e[Class Tree: %s]", classId)));
                    
                    int classAllocated = passiveComponent.getClassAllocatedCount(classId);
                    context.sendMessage(Message.raw(String.format("  §7Allocated Nodes: §f%d", classAllocated)));
                    
                    int availableClass = PassiveTreeService.get().getAvailableClassPoints(ref, classId);
                    context.sendMessage(Message.raw(String.format("  §7Available Points: §a%d", availableClass)));
                }
            }
        });
    }
}
