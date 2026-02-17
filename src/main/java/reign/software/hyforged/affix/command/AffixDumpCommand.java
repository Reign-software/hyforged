package reign.software.hyforged.affix.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.affix.api.AffixService;
import reign.software.hyforged.affix.model.RolledAffix;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.logging.Level;
import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Debug command to dump all affixes on a player's equipped items.
 * <p>
 * Usage: {@code /hyforged affixes}
 * <p>
 * Outputs all equipped items and their affixes to chat.
 */
public class AffixDumpCommand extends AbstractPlayerCommand {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("Could not find player component.");
    private static final Message MESSAGE_NO_AFFIXES = Message.raw("No affixes found on equipped items.");
    
    public AffixDumpCommand() {
        super("affixes", "hyforged.commands.affixes.desc");
        this.addAliases("dump-affixes", "list-affixes");
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
            context.sendMessage(MESSAGE_NO_AFFIXES);
            return;
        }
        
        AffixService affixService = AffixService.get();
        boolean foundAny = false;
        
        context.sendMessage(Message.raw("=== Equipped Affix Dump ==="));
        
        // Check armor container
        ItemContainer armorContainer = inventory.getArmor();
        if (armorContainer != null) {
            short capacity = armorContainer.getCapacity();
            for (short i = 0; i < capacity; i++) {
                ItemStack itemStack = armorContainer.getItemStack(i);
                if (!ItemStack.isEmpty(itemStack)) {
                    if (dumpItemAffixes(context, affixService, itemStack, "Armor[" + i + "]")) {
                        foundAny = true;
                    }
                }
            }
        }
        
        // Check hotbar
        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar != null) {
            short capacity = hotbar.getCapacity();
            for (short i = 0; i < capacity; i++) {
                ItemStack itemStack = hotbar.getItemStack(i);
                if (!ItemStack.isEmpty(itemStack)) {
                    if (dumpItemAffixes(context, affixService, itemStack, "Hotbar[" + i + "]")) {
                        foundAny = true;
                    }
                }
            }
        }
        
        if (!foundAny) {
            context.sendMessage(MESSAGE_NO_AFFIXES);
        }
        
        LOGGER.at(Level.FINE).log("Affix dump completed for player");
    }
    
    /**
     * Dump affixes for a single item.
     * @return true if the item has any affixes
     */
    private boolean dumpItemAffixes(
            @Nonnull CommandContext context,
            @Nonnull AffixService affixService,
            @Nonnull ItemStack itemStack,
            @Nonnull String slotName
    ) {
        List<RolledAffix> affixes = affixService.getAffixes(itemStack);
        
        if (affixes.isEmpty()) {
            return false;
        }
        
        context.sendMessage(Message.raw("" + slotName + ": " + itemStack.getItemId()));
        
        for (RolledAffix affix : affixes) {
            String typeColor = switch (affix.type()) {
                case "prefix" -> "";
                case "suffix" -> "";
                case "forged" -> "";
                default -> "";
            };
            
            String tierColor = getTierColor(affix.tier());
            
            // Show affix header
            context.sendMessage(Message.raw(String.format(
                    "  %s[T%d] %s%s:",
                    tierColor,
                    affix.tier(),
                    typeColor,
                    affix.affixId()
            )));
            
            // Show each stat in the affix
            for (var entry : affix.rolledStats().entrySet()) {
                String statId = entry.getKey();
                RolledAffix.RolledStat stat = entry.getValue();
                context.sendMessage(Message.raw(String.format(
                        "    +%d %s (%s)",
                        stat.value(),
                        statId,
                        stat.stackType().name()
                )));
            }
        }
        
        return true;
    }
    
    private String getTierColor(int tier) {
        return switch (tier) {
            case 1 -> ""; // Gold
            case 2 -> ""; // Purple
            case 3 -> ""; // Blue
            case 4 -> ""; // Green
            default -> ""; // White
        };
    }
}
