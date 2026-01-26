package reign.software.hyforged.affix.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.affix.api.AffixService;
import reign.software.hyforged.affix.api.AffixSpec;
import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.RolledAffix;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Debug command to add a specific affix to the held item.
 * <p>
 * Usage: {@code /hyforged giveaffix <affixId> [tier]}
 * <p>
 * Adds the specified affix to the item in the player's main hand.
 */
public class GiveAffixCommand extends AbstractPlayerCommand {
    
    private static final Logger LOGGER = Logger.getLogger(GiveAffixCommand.class.getName());
    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("Could not find player component.");
    private static final Message MESSAGE_NO_ITEM = Message.raw("You must hold an item in your main hand.");
    
    @Nonnull
    private final RequiredArg<String> affixIdArg = this.withRequiredArg(
            "affixId",
            "hyforged.commands.giveaffix.arg.affixId.desc",
            ArgTypes.STRING
    );
    
    @Nonnull
    private final DefaultArg<Integer> tierArg = this.withDefaultArg(
            "tier",
            "hyforged.commands.giveaffix.arg.tier.desc",
            ArgTypes.INTEGER,
            1,
            ""
    );
    
    public GiveAffixCommand() {
        super("giveaffix", "hyforged.commands.giveaffix.desc");
        this.addAliases("addaffix", "give");
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
        
        // Get arguments
        String affixId = context.get(affixIdArg);
        Integer tier = context.get(tierArg);
        
        AffixService affixService = AffixService.get();
        
        // Validate affix exists
        AffixDefinition affixDef = affixService.getAffixDefinition(affixId);
        if (affixDef == null) {
            context.sendMessage(Message.raw("Unknown affix ID: " + affixId));
            context.sendMessage(Message.raw("Available affixes: " + String.join(", ", affixService.getAllAffixIds())));
            return;
        }
        
        // Validate tier
        int maxTier = affixDef.tiers().size();
        if (tier < 1 || tier > maxTier) {
            context.sendMessage(Message.raw("Invalid tier: " + tier + ". Valid range: 1-" + maxTier));
            return;
        }
        
        // Create affix spec and add to item
        AffixSpec spec = AffixSpec.of(affixId, tier);
        ItemStack resultItem = affixService.addAffix(heldItem, spec);
        
        // Update the hotbar slot with the new item
        hotbar.replaceItemStackInSlot(selectedSlot, heldItem, resultItem);
        
        // Get updated affixes to show result
        List<RolledAffix> afterAffixes = affixService.getAffixes(resultItem);
        
        // Report success
        context.sendMessage(Message.raw("Added affix: " + affixId + " [T" + tier + "]"));
        context.sendMessage(Message.raw("Item now has " + afterAffixes.size() + " affix(es):"));
        
        for (RolledAffix affix : afterAffixes) {
            String tierInfo = " [T" + affix.tier() + "]";
            String statsInfo = affix.getStatCount() + " stat(s)";
            context.sendMessage(Message.raw(" - " + affix.affixId() + tierInfo + " (" + statsInfo + ")"));
        }
        
        LOGGER.log(Level.FINE, "Added affix {0} tier {1} to held item", new Object[]{affixId, tier});
    }
}
