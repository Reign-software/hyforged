package reign.software.hyforged.passive.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.model.PassiveTree;
import reign.software.hyforged.passive.registry.PassiveTreeRegistry;
import reign.software.hyforged.passive.service.PassiveTreeService;
import reign.software.hyforged.passive.service.RefundResult;

import javax.annotation.Nonnull;

/**
 * Command to reset a player's passive tree allocations (free, no Tradebar cost).
 * <p>
 * Usage: {@code /passive reset <player> [tree]}
 * <p>
 * If tree is not specified, resets all trees.
 * If tree is "general", resets only the general tree.
 * If tree is a class ID, resets only that class tree.
 */
public class PassiveResetCommand extends CommandBase {

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("§cPlayer not found or not in world.");
    private static final Message MESSAGE_NO_COMPONENT = Message.raw("§cPlayer has no passive tree data.");
    private static final Message MESSAGE_TREE_NOT_FOUND = Message.raw("§cTree not found: %s");
    private static final Message MESSAGE_RESET_GENERAL = Message.raw("§aReset general tree for %s (%d nodes refunded, %d points returned)");
    private static final Message MESSAGE_RESET_CLASS = Message.raw("§aReset class tree '%s' for %s (%d nodes refunded, %d points returned)");
    private static final Message MESSAGE_RESET_ALL = Message.raw("§aReset all trees for %s");
    private static final Message MESSAGE_RESET_FAILED = Message.raw("§cReset failed: %s");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player", "hyforged.commands.passive.reset.player.desc", ArgTypes.PLAYER_REF);
    
    @Nonnull
    private final OptionalArg<String> treeArg = this.withOptionalArg(
            "tree", "hyforged.commands.passive.reset.tree.desc", ArgTypes.STRING);

    public PassiveResetCommand() {
        super("reset", "hyforged.commands.passive.reset.desc");
        this.addAliases("respec", "clear");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef targetPlayerRef = this.playerArg.get(context);
        Ref<EntityStore> ref = targetPlayerRef.getReference();
        
        if (ref == null || !ref.isValid()) {
            context.sendMessage(MESSAGE_PLAYER_NOT_FOUND);
            return;
        }

        // Check if tree argument provided
        String treeId = this.treeArg.provided(context) ? this.treeArg.get(context) : null;

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        
        world.execute(() -> {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null) {
                context.sendMessage(MESSAGE_PLAYER_NOT_FOUND);
                return;
            }

            PassiveTreeComponent passiveComponent = store.getComponent(ref, 
                    HyforgedPlugin.getInstance().getPassiveTreeComponentType());
            
            if (passiveComponent == null) {
                context.sendMessage(MESSAGE_NO_COMPONENT);
                return;
            }

            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            String username = playerRefComponent != null ? playerRefComponent.getUsername() : "Unknown";

            PassiveTreeService service = PassiveTreeService.get();

            // If no tree specified, reset all
            if (treeId == null) {
                resetAllTrees(context, ref, passiveComponent, username, service);
                return;
            }

            // Handle specific tree
            if (treeId.equalsIgnoreCase("general")) {
                PassiveTree generalTree = PassiveTreeRegistry.get().getGeneralTree();
                if (generalTree == null) {
                    context.sendMessage(Message.raw("§cGeneral tree not loaded."));
                    return;
                }
                
                RefundResult result = service.refundAllFree(ref, generalTree.getId());
                if (result.success()) {
                    context.sendMessage(Message.raw(String.format(
                            "§aReset general tree for %s (%d nodes refunded, %d points returned)",
                            username, result.refundedNodes().size(), result.pointsReturned())));
                } else {
                    context.sendMessage(Message.raw("§cReset failed: " + result.reason()));
                }
                return;
            }

            // Check for class tree
            PassiveTree classTree = PassiveTreeRegistry.get().getClassTree(treeId);
            if (classTree != null) {
                RefundResult result = service.refundAllFree(ref, classTree.getId());
                if (result.success()) {
                    context.sendMessage(Message.raw(String.format(
                            "§aReset class tree '%s' for %s (%d nodes refunded, %d points returned)",
                            treeId, username, result.refundedNodes().size(), result.pointsReturned())));
                } else {
                    context.sendMessage(Message.raw("§cReset failed: " + result.reason()));
                }
                return;
            }

            // Unknown tree
            context.sendMessage(Message.raw(String.format("§cTree not found: %s", treeId)));
        });
    }

    private void resetAllTrees(
            @Nonnull CommandContext context,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PassiveTreeComponent passiveComponent,
            @Nonnull String username,
            @Nonnull PassiveTreeService service
    ) {
        int totalNodesRefunded = 0;
        int totalPointsReturned = 0;

        // Reset general tree
        PassiveTree generalTree = PassiveTreeRegistry.get().getGeneralTree();
        if (generalTree != null && passiveComponent.getGeneralAllocatedCount() > 0) {
            RefundResult result = service.refundAllFree(ref, generalTree.getId());
            if (result.success()) {
                totalNodesRefunded += result.refundedNodes().size();
                totalPointsReturned += result.pointsReturned();
            }
        }

        // Reset all class trees
        for (String classId : passiveComponent.getClassIdsWithAllocations()) {
            PassiveTree classTree = PassiveTreeRegistry.get().getClassTree(classId);
            if (classTree != null) {
                RefundResult result = service.refundAllFree(ref, classTree.getId());
                if (result.success()) {
                    totalNodesRefunded += result.refundedNodes().size();
                    totalPointsReturned += result.pointsReturned();
                }
            }
        }

        context.sendMessage(Message.raw(String.format(
                "§aReset all trees for %s (%d nodes refunded, %d points returned)",
                username, totalNodesRefunded, totalPointsReturned)));
    }
}
