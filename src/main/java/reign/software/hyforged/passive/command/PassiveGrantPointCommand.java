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

import javax.annotation.Nonnull;

/**
 * Command to grant a free passive point to a player.
 * <p>
 * Usage: {@code /passive grant-point <player> [tree]}
 * <p>
 * If tree is not specified or is "general", grants a book point (adds to general tree).
 * If tree is a class ID, grants a class point (not yet implemented - requires class level manipulation).
 */
public class PassiveGrantPointCommand extends CommandBase {

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("§cPlayer not found or not in world.");
    private static final Message MESSAGE_NO_COMPONENT = Message.raw("§cPlayer has no passive tree data.");
    private static final Message MESSAGE_GRANTED_GENERAL = Message.raw("§aGranted 1 general passive point to %s (new book points: %d)");
    private static final Message MESSAGE_CLASS_NOT_SUPPORTED = Message.raw("§cClass tree point grants are not yet supported. Use level commands instead.");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player", "hyforged.commands.passive.grant.player.desc", ArgTypes.PLAYER_REF);
    
    @Nonnull
    private final OptionalArg<String> treeArg = this.withOptionalArg(
            "tree", "hyforged.commands.passive.grant.tree.desc", ArgTypes.STRING);

    public PassiveGrantPointCommand() {
        super("grant-point", "hyforged.commands.passive.grant.desc");
        this.addAliases("grant", "grantpoint");
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
        String treeId = this.treeArg.provided(context) ? this.treeArg.get(context) : "general";

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

            // Handle general tree point grant
            if (treeId.equalsIgnoreCase("general")) {
                int newTotal = passiveComponent.addBookPoint();
                context.sendMessage(Message.raw(String.format(
                        "§aGranted 1 general passive point to %s (new book points: %d)", 
                        username, newTotal)));
                return;
            }

            // Check if it's a valid class tree
            PassiveTree classTree = PassiveTreeRegistry.get().getClassTree(treeId);
            if (classTree != null) {
                // Class tree - would need to modify class level which is handled by progression system
                context.sendMessage(MESSAGE_CLASS_NOT_SUPPORTED);
                return;
            }

            // Unknown tree
            context.sendMessage(Message.raw("§cUnknown tree: " + treeId + ". Use 'general' or a valid class ID."));
        });
    }
}
