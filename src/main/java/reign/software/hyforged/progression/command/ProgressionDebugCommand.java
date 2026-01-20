package reign.software.hyforged.progression.command;

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
import reign.software.hyforged.progression.CharacterProgression;
import reign.software.hyforged.progression.ClassProgression;
import reign.software.hyforged.progression.asset.XPCurveRegistry;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.stats.asset.ClassDefinition;
import reign.software.hyforged.stats.asset.ClassDefinitionRegistry;

import javax.annotation.Nonnull;

/**
 * Command to display detailed debug information about a player's progression state.
 * <p>
 * Usage: /hyforged progression debug <player>
 */
public class ProgressionDebugCommand extends CommandBase {

    private static final Message MESSAGE_NO_COMPONENT = Message.raw("§cPlayer has no progression component.");
    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("§cPlayer not found or not in a world.");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player",
            "Target player",
            ArgTypes.PLAYER_REF
    );

    public ProgressionDebugCommand() {
        super("debug", "Show detailed progression debug info");
        this.requirePermission("hyforged.admin.progression.debug");
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

            ProgressionComponent progression = store.getComponent(
                    ref,
                    HyforgedPlugin.getInstance().getProgressionComponentType()
            );

            if (progression == null) {
                context.sendMessage(MESSAGE_NO_COMPONENT);
                return;
            }

            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            String playerName = playerRefComponent != null ? playerRefComponent.getUsername() : "Unknown";

            showDebugInfo(context, playerName, progression);
        });
    }

    /**
     * Display detailed debug information.
     */
    private void showDebugInfo(
            @Nonnull CommandContext context,
            @Nonnull String playerName,
            @Nonnull ProgressionComponent progression
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("§6=== Progression Debug: §e").append(playerName).append(" §6===\n");
        
        // Character progression
        sb.append("\n§b[Character Progression]§r\n");
        sb.append("  Level: §f").append(progression.getCharacterLevel());
        sb.append("§7/§f").append(CharacterProgression.MAX_LEVEL).append("§r\n");
        sb.append("  XP: §f").append(String.format("%,d", progression.getCharacterXp())).append("§r\n");
        sb.append("  XP to Next: §f").append(String.format("%,d", progression.getCharacterXpToNext())).append("§r\n");
        sb.append("  General Passive Points: §f").append(progression.getAvailableGeneralPassivePoints()).append("§r\n");
        sb.append("  Allocated Points: §f").append(progression.getGeneralPassivePointsAllocated()).append("§r\n");
        sb.append("  Dirty Flag: §f").append(progression.isDirty()).append("§r\n");
        
        // Active class
        sb.append("\n§b[Active Class]§r\n");
        String activeClassId = progression.getActiveClassId();
        if (activeClassId != null) {
            sb.append("  ID: §f").append(activeClassId).append("§r\n");
            
            ClassDefinition classDef = ClassDefinitionRegistry.get().get(activeClassId);
            if (classDef != null) {
                sb.append("  Display Name: §f").append(classDef.displayName()).append("§r\n");
                sb.append("  Weapon Tags: §7").append(String.join(", ", classDef.weaponTagFamilies())).append("§r\n");
            } else {
                sb.append("  §c(Class definition not found)§r\n");
            }
        } else {
            sb.append("  §7(None set)§r\n");
        }
        
        // Class progressions
        sb.append("\n§b[Class Progressions]§r\n");
        var classIds = progression.getClassIds();
        if (classIds.isEmpty()) {
            sb.append("  §7(No class progressions)§r\n");
        } else {
            for (String classId : classIds) {
                var classData = progression.getClassProgression(classId);
                if (classData != null) {
                    boolean isActive = classId.equals(activeClassId);
                    String prefix = isActive ? "§a► " : "  ";
                    sb.append(prefix).append("§e").append(classId).append("§r\n");
                    sb.append("    Level: §f").append(classData.level);
                    sb.append("§7/§f").append(ClassProgression.MAX_LEVEL).append("§r\n");
                    sb.append("    XP: §f").append(String.format("%,d", classData.xp)).append("§r\n");
                }
            }
        }
        
        // Registries status
        sb.append("\n§b[Registries]§r\n");
        XPCurveRegistry curveRegistry = XPCurveRegistry.get();
        int curveCount = curveRegistry != null ? curveRegistry.getCurveCount() : 0;
        sb.append("  XP Curves Loaded: §f").append(curveCount).append("§r\n");
        sb.append("  Class Definitions: §f").append(ClassDefinitionRegistry.get().getClassCount()).append("§r\n");
        
        context.sendMessage(Message.raw(sb.toString()));
    }
}
