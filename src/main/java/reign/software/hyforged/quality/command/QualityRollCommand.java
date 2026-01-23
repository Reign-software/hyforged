package reign.software.hyforged.quality.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.quality.model.QualityRollContext;
import reign.software.hyforged.quality.service.HyforgedQualityService;
import reign.software.hyforged.quality.service.QualityContextBuilder;
import reign.software.hyforged.quality.service.QualityRollerService;

import javax.annotation.Nonnull;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Debug command to roll quality on the held item.
 * <p>
 * Usage: {@code /hyforged quality roll [seed]}
 */
public class QualityRollCommand extends AbstractPlayerCommand {

    private static final Logger LOGGER = Logger.getLogger(QualityRollCommand.class.getName());
    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("§cCould not find player component.");
    private static final Message MESSAGE_NO_ITEM = Message.raw("§cYou must hold an item in your main hand.");
    private static final Message MESSAGE_NOT_ELIGIBLE = Message.raw("§7Item is not eligible for quality rolling.");
    private static final Message MESSAGE_NO_QUALITY = Message.raw("§7No quality was rolled for this item.");

    @Nonnull
    private final DefaultArg<String> seedArg = this.withDefaultArg(
            "seed",
            "hyforged.commands.quality.roll.seed.desc",
            ArgTypes.STRING,
            null,
            ""
    );

    private final QualityRollerService rollerService = new QualityRollerService();

    public QualityRollCommand() {
        super("roll", "hyforged.commands.quality.roll.desc");
        this.addAliases("rollquality", "qualityroll");
        this.requirePermission("hyforged.admin.quality.roll");
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

        var inventory = playerComponent.getInventory();
        if (inventory == null) {
            context.sendMessage(MESSAGE_NO_ITEM);
            return;
        }

        var hotbar = inventory.getHotbar();
        if (hotbar == null) {
            context.sendMessage(MESSAGE_NO_ITEM);
            return;
        }

        byte activeSlot = inventory.getActiveHotbarSlot();
        if (activeSlot < 0) {
            context.sendMessage(MESSAGE_NO_ITEM);
            return;
        }
        short selectedSlot = activeSlot;
        ItemStack heldItem = hotbar.getItemStack(selectedSlot);
        if (ItemStack.isEmpty(heldItem)) {
            context.sendMessage(MESSAGE_NO_ITEM);
            return;
        }

        QualityRollContext rollContext = QualityContextBuilder.fromItemStack(
                heldItem,
                store,
                ref,
                ref,
                "command",
                null,
                new String[0]
        );
        if (rollContext == null) {
            context.sendMessage(MESSAGE_NOT_ELIGIBLE);
            return;
        }

        String seedStr = context.get(seedArg);
        long seed = new Random().nextLong();
        boolean usedProvidedSeed = false;
        if (seedStr != null && !seedStr.isBlank()) {
            try {
                seed = Long.parseLong(seedStr);
                usedProvidedSeed = true;
            } catch (NumberFormatException e) {
                context.sendMessage(Message.raw("§cInvalid seed: " + seedStr + ". Using random seed."));
            }
        }

        String originalQuality = HyforgedQualityService.getEffectiveQuality(heldItem);
        String rolledQuality = usedProvidedSeed
                ? rollerService.rollQuality(rollContext, seed)
                : rollerService.rollQuality(rollContext);

        if (rolledQuality == null || rolledQuality.isBlank()) {
            context.sendMessage(MESSAGE_NO_QUALITY);
            return;
        }

        if (!rolledQuality.equals(originalQuality)) {
            ItemStack updated = HyforgedQualityService.withQuality(heldItem, rolledQuality);
            hotbar.replaceItemStackInSlot(selectedSlot, heldItem, updated);
        }

        if (usedProvidedSeed) {
            context.sendMessage(Message.raw("§6Rolled quality with seed: §e" + seed));
        } else {
            context.sendMessage(Message.raw("§6Rolled quality (seed: §e" + seed + "§6)"));
        }
        context.sendMessage(Message.raw("§7" + originalQuality + " → §a" + rolledQuality));

        LOGGER.log(Level.FINE, "Quality roll completed for item {0} (seed: {1})",
                new Object[]{heldItem.getItemId(), seed});
    }
}
