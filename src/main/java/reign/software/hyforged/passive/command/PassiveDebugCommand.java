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
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.model.PassiveNode;
import reign.software.hyforged.passive.model.PassiveTree;
import reign.software.hyforged.passive.registry.PassiveTreeRegistry;
import reign.software.hyforged.passive.service.PassiveTreeService;
import reign.software.hyforged.progression.component.ProgressionComponent;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

/**
 * Command to dump full passive tree state for debugging.
 * <p>
 * Usage: {@code /passive debug <player>}
 * <p>
 * Shows detailed information including:
 * <ul>
 *   <li>All allocated nodes with their types and effects</li>
 *   <li>Tree versions</li>
 *   <li>Point calculations breakdown</li>
 *   <li>Mastery choices</li>
 * </ul>
 */
public class PassiveDebugCommand extends CommandBase {

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("§cPlayer not found or not in world.");
    private static final Message MESSAGE_NO_COMPONENT = Message.raw("§cPlayer has no passive tree data.");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player", "hyforged.commands.passive.debug.player.desc", ArgTypes.PLAYER_REF);

    public PassiveDebugCommand() {
        super("debug", "hyforged.commands.passive.debug.desc");
        this.addAliases("dump", "info");
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
                    HyforgedPlugin.getInstance().getPassiveTreeComponentType());
            
            if (passiveComponent == null) {
                context.sendMessage(MESSAGE_NO_COMPONENT);
                return;
            }

            ProgressionComponent progressionComponent = store.getComponent(ref,
                    HyforgedPlugin.getInstance().getProgressionComponentType());

            // Get username
            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            String username = playerRefComponent != null ? playerRefComponent.getUsername() : "Unknown";
            
            // Header
            context.sendMessage(Message.raw("§6========================================"));
            context.sendMessage(Message.raw("§6Passive Tree Debug for: §e" + username));
            context.sendMessage(Message.raw("§6========================================"));

            // Character Level
            int characterLevel = progressionComponent != null ? progressionComponent.getCharacterLevel() : 0;
            context.sendMessage(Message.raw("§7Character Level: §f" + characterLevel));
            context.sendMessage(Message.raw("§7Schema Version: §f" + PassiveTreeComponent.SCHEMA_VERSION));
            context.sendMessage(Message.raw(""));

            // === GENERAL TREE ===
            context.sendMessage(Message.raw("§e[GENERAL TREE]"));
            
            String startingNode = passiveComponent.getGeneralStartingNode();
            context.sendMessage(Message.raw("  §7Starting Node: §f" + (startingNode != null ? startingNode : "(none)")));
            
            int bookPoints = passiveComponent.getBookPointsUsed();
            int generalAllocated = passiveComponent.getGeneralAllocatedCount();
            int earnedPoints = characterLevel - 1;
            int availableGeneral = PassiveTreeService.get().getAvailableGeneralPoints(ref);
            
            context.sendMessage(Message.raw("  §7Points Breakdown:"));
            context.sendMessage(Message.raw("    §8- Earned (level-1): §f" + earnedPoints));
            context.sendMessage(Message.raw("    §8- Book Points: §f" + bookPoints));
            context.sendMessage(Message.raw("    §8- Allocated: §c-" + generalAllocated));
            context.sendMessage(Message.raw("    §8= Available: §a" + availableGeneral));
            
            // Tree version
            int generalVersion = passiveComponent.getTreeVersion("general");
            context.sendMessage(Message.raw("  §7Tree Version: §f" + (generalVersion > 0 ? generalVersion : "(not tracked)")));
            
            // Allocated nodes
            Set<String> generalNodes = passiveComponent.getGeneralAllocatedNodes();
            context.sendMessage(Message.raw("  §7Allocated Nodes (" + generalNodes.size() + "):"));
            
            PassiveTree generalTree = PassiveTreeRegistry.get().getGeneralTree();
            if (generalTree != null && !generalNodes.isEmpty()) {
                int count = 0;
                for (String nodeId : generalNodes) {
                    if (count >= 20) {
                        context.sendMessage(Message.raw("    §8... and " + (generalNodes.size() - 20) + " more"));
                        break;
                    }
                    PassiveNode node = generalTree.getNode(nodeId);
                    String type = node != null ? node.type() : "?";
                    String effects = node != null ? String.valueOf(node.effects().size()) : "?";
                    context.sendMessage(Message.raw("    §f" + nodeId + " §8[" + type + ", " + effects + " effects]"));
                    count++;
                }
            }
            context.sendMessage(Message.raw(""));

            // === CLASS TREES ===
            Set<String> classIds = passiveComponent.getClassIdsWithAllocations();
            if (!classIds.isEmpty()) {
                for (String classId : classIds) {
                    context.sendMessage(Message.raw("§e[CLASS TREE: " + classId + "]"));
                    
                    int classAllocated = passiveComponent.getClassAllocatedCount(classId);
                    int availableClass = PassiveTreeService.get().getAvailableClassPoints(ref, classId);
                    
                    // Get class level from progression
                    int classLevel = 0;
                    if (progressionComponent != null) {
                        ProgressionComponent.ClassProgressionData classData = progressionComponent.getClassProgression(classId);
                        classLevel = classData != null ? classData.level : 0;
                    }
                    
                    context.sendMessage(Message.raw("  §7Class Level: §f" + classLevel));
                    context.sendMessage(Message.raw("  §7Allocated: §f" + classAllocated));
                    context.sendMessage(Message.raw("  §7Available: §a" + availableClass));
                    
                    // Tree version
                    int classVersion = passiveComponent.getTreeVersion(classId);
                    context.sendMessage(Message.raw("  §7Tree Version: §f" + (classVersion > 0 ? classVersion : "(not tracked)")));
                    
                    // Allocated nodes
                    Set<String> classNodes = passiveComponent.getClassAllocatedNodes(classId);
                    context.sendMessage(Message.raw("  §7Allocated Nodes (" + classNodes.size() + "):"));
                    
                    PassiveTree classTree = PassiveTreeRegistry.get().getClassTree(classId);
                    if (classTree != null) {
                        int count = 0;
                        for (String nodeId : classNodes) {
                            if (count >= 10) {
                                context.sendMessage(Message.raw("    §8... and " + (classNodes.size() - 10) + " more"));
                                break;
                            }
                            PassiveNode node = classTree.getNode(nodeId);
                            String type = node != null ? node.type() : "?";
                            context.sendMessage(Message.raw("    §f" + nodeId + " §8[" + type + "]"));
                            count++;
                        }
                    }
                    context.sendMessage(Message.raw(""));
                }
            }

            // === MASTERY CHOICES ===
            Map<String, String> masteryChoices = passiveComponent.getAllMasteryChoices();
            if (!masteryChoices.isEmpty()) {
                context.sendMessage(Message.raw("§e[MASTERY CHOICES]"));
                for (Map.Entry<String, String> entry : masteryChoices.entrySet()) {
                    context.sendMessage(Message.raw("  §f" + entry.getKey() + " §8-> §f" + entry.getValue()));
                }
                context.sendMessage(Message.raw(""));
            }

            // === PENDING MASTERY ===
            Set<String> pendingMastery = passiveComponent.getPendingMasteryChoices();
            if (!pendingMastery.isEmpty()) {
                context.sendMessage(Message.raw("§e[PENDING MASTERY CHOICES]"));
                for (String masteryId : pendingMastery) {
                    context.sendMessage(Message.raw("  §c" + masteryId + " §8(awaiting selection)"));
                }
            }

            context.sendMessage(Message.raw("§6========================================"));
        });
    }
}
