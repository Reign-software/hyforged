package reign.software.hyforged.affix.command;

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
import reign.software.hyforged.affix.api.AffixService;
import reign.software.hyforged.affix.model.RolledAffix;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Debug command to roll affixes on the held item.
 * <p>
 * Usage: {@code /hyforged rollaffix [seed]}
 * <p>
 * If a seed is provided, it will be used for deterministic rolling.
 * Otherwise, a random seed will be used.
 */
public class RollAffixCommand extends AbstractPlayerCommand {
    
    private static final Logger LOGGER = Logger.getLogger(RollAffixCommand.class.getName());
    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("Could not find player component.");
    private static final Message MESSAGE_NO_ITEM = Message.raw("You must hold an item in your main hand.");
    private static final Message MESSAGE_NO_AFFIXES_ROLLED = Message.raw("No affixes were rolled for this item (may not be eligible).");
    
    @Nonnull
    private final DefaultArg<String> seedArg = this.withDefaultArg(
            "seed", 
            "hyforged.commands.rollaffix.arg.seed.desc", 
            ArgTypes.STRING, 
            null, 
            ""
    );
    
    public RollAffixCommand() {
        super("rollaffix", "hyforged.commands.rollaffix.desc");
        this.addAliases("roll");
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
        
        // Get the held item
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
        
        // Get selected slot - assume slot 0 for now
        short selectedSlot = 0;
        ItemStack heldItem = hotbar.getItemStack(selectedSlot);
        
        if (ItemStack.isEmpty(heldItem)) {
            context.sendMessage(MESSAGE_NO_ITEM);
            return;
        }
        
        // Get seed (optional - use random if not provided)
        String seedStr = context.get(seedArg);
        long seed;
        boolean usedProvidedSeed = false;
        
        if (seedStr != null && !seedStr.isEmpty()) {
            try {
                seed = Long.parseLong(seedStr);
                usedProvidedSeed = true;
            } catch (NumberFormatException e) {
                context.sendMessage(Message.raw("Invalid seed: " + seedStr + ". Using random seed."));
                seed = new Random().nextLong();
            }
        } else {
            seed = new Random().nextLong();
        }
        
        AffixService affixService = AffixService.get();
        
        // Roll affixes - returns a new ItemStack
        ItemStack resultItem = affixService.rollAffixes(heldItem, seed);
        
        // Get affixes after rolling
        List<RolledAffix> afterAffixes = affixService.getAffixes(resultItem);
        
        // Check if anything was rolled
        if (afterAffixes.isEmpty()) {
            context.sendMessage(MESSAGE_NO_AFFIXES_ROLLED);
            LOGGER.log(Level.FINE, "No affixes rolled for item (seed: {0})", seed);
            return;
        }
        
        // Update the hotbar slot with the new item
        hotbar.replaceItemStackInSlot(selectedSlot, heldItem, resultItem);
        
        // Report results
        if (usedProvidedSeed) {
            context.sendMessage(Message.raw("Rolled affixes with seed: " + seed));
        } else {
            context.sendMessage(Message.raw("Rolled affixes (seed: " + seed + ")"));
        }
        
        for (RolledAffix affix : afterAffixes) {
            String tierInfo = " [T" + affix.tier() + "]";
            String statsInfo = affix.getStatCount() + " stat(s)";
            context.sendMessage(Message.raw(" - " + affix.affixId() + tierInfo + " (" + statsInfo + ")"));
        }
        
        LOGGER.log(Level.FINE, "Rolled {0} affixes on held item (seed: {1})", 
                new Object[]{afterAffixes.size(), seed});
    }
}
