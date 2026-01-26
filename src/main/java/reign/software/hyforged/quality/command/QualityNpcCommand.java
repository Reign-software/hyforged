package reign.software.hyforged.quality.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.quality.component.HyforgedNPCQualityComponent;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Debug command to inspect NPC quality on the targeted entity.
 * <p>
 * Usage: {@code /hyforged quality npc}
 */
public class QualityNpcCommand extends AbstractPlayerCommand {

    private static final Message MESSAGE_NO_TARGET = Message.raw("No target entity found.");
    private static final Message MESSAGE_NO_QUALITY = Message.raw("Target has no NPC quality assigned.");

    public QualityNpcCommand() {
        super("npc", "hyforged.commands.quality.npc.desc");
        this.addAliases("npcquality", "inspectnpc");
        this.requirePermission("hyforged.admin.quality.npc");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        Ref<EntityStore> targetRef = TargetUtil.getTargetEntity(ref, 8.0f, store);
        if (targetRef == null || !targetRef.isValid()) {
            context.sendMessage(MESSAGE_NO_TARGET);
            return;
        }

        HyforgedNPCQualityComponent component = store.getComponent(
                targetRef,
                HyforgedPlugin.getInstance().getNpcQualityComponentType()
        );
        if (component == null || component.getQualityId() == null || component.getQualityId().isBlank()) {
            context.sendMessage(MESSAGE_NO_QUALITY);
            return;
        }

        NPCEntity npcEntity = store.getComponent(targetRef, NPCEntity.getComponentType());
        String roleName = npcEntity != null ? npcEntity.getRoleName() : "Unknown";

        context.sendMessage(Message.raw("NPC Quality: " + component.getQualityId()));
        context.sendMessage(Message.raw("Role: " + roleName));

        List<RolledAffix> affixes = component.getAffixes();
        int affixCount = affixes != null ? affixes.size() : 0;
        context.sendMessage(Message.raw("Affixes: " + affixCount));

        if (affixCount > 0) {
            for (RolledAffix affix : affixes) {
                context.sendMessage(Message.raw("  - " + affix.affixId() + " [T" + affix.tier() + "]"));
            }
        }
    }
}
