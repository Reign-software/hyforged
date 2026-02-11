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
import reign.software.hyforged.passive.service.PassiveTreeService;
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

    private static final Message MESSAGE_NO_COMPONENT = Message.raw("Player has no progression component.");
    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("Player not found or not in a world.");

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

            showDebugInfo(context, playerName, progression, ref);
        });
    }

    /**
     * Display detailed debug information.
     */
    private void showDebugInfo(
            @Nonnull CommandContext context,
            @Nonnull String playerName,
            @Nonnull ProgressionComponent progression,
            @Nonnull Ref<EntityStore> entityRef
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Progression Debug: ").append(playerName).append(" ===\n");
        
        // Character progression
        sb.append("\n[Character Progression]\n");
        sb.append("  Level: ").append(progression.getCharacterLevel());
        sb.append("/").append(CharacterProgression.MAX_LEVEL).append("\n");
        sb.append("  XP: ").append(String.format("%,d", progression.getCharacterXp())).append("\n");
        sb.append("  XP to Next: ").append(String.format("%,d", progression.getCharacterXpToNext())).append("\n");
        int availablePoints = PassiveTreeService.get().getAvailableGeneralPoints(entityRef);
        sb.append("  General Passive Points: ").append(availablePoints).append("\n");
        sb.append("  Allocated Points: ").append(progression.getGeneralPassivePointsAllocated()).append("\n");
        sb.append("  Dirty Flag: ").append(progression.isDirty()).append("\n");
        
        // Active class
        sb.append("\n[Active Class]\n");
        String activeClassId = progression.getActiveClassId();
        if (activeClassId != null) {
            sb.append("  ID: ").append(activeClassId).append("\n");
            
            ClassDefinition classDef = ClassDefinitionRegistry.get().get(activeClassId);
            if (classDef != null) {
                sb.append("  Display Name: ").append(classDef.displayName()).append("\n");
                sb.append("  Weapon Tags: ").append(String.join(", ", classDef.weaponTagFamilies())).append("\n");
            } else {
                sb.append("  (Class definition not found)\n");
            }
        } else {
            sb.append("  (None set)\n");
        }
        
        // Class progressions
        sb.append("\n[Class Progressions]\n");
        var classIds = progression.getClassIds();
        if (classIds.isEmpty()) {
            sb.append("  (No class progressions)\n");
        } else {
            for (String classId : classIds) {
                var classData = progression.getClassProgression(classId);
                if (classData != null) {
                    boolean isActive = classId.equals(activeClassId);
                    String prefix = isActive ? "► " : "  ";
                    sb.append(prefix).append("").append(classId).append("\n");
                    sb.append("    Level: ").append(classData.level);
                    sb.append("/").append(ClassProgression.MAX_LEVEL).append("\n");
                    sb.append("    XP: ").append(String.format("%,d", classData.xp)).append("\n");
                }
            }
        }
        
        // Registries status
        sb.append("\n[Registries]\n");
        XPCurveRegistry curveRegistry = XPCurveRegistry.get();
        int curveCount = curveRegistry != null ? curveRegistry.getCurveCount() : 0;
        sb.append("  XP Curves Loaded: ").append(curveCount).append("\n");
        sb.append("  Class Definitions: ").append(ClassDefinitionRegistry.get().getClassCount()).append("\n");
        
        context.sendMessage(Message.raw(sb.toString()));
    }
}
