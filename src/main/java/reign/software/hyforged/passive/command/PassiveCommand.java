package reign.software.hyforged.passive.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Root command for passive tree management.
 * <p>
 * Usage: {@code /passive <subcommand>}
 * <p>
 * Subcommands:
 * <ul>
 *   <li>{@code list <player>} - Show player's passive tree allocations</li>
 *   <li>{@code grant-point <player> [tree]} - Grant a free passive point</li>
 *   <li>{@code reset <player> [tree]} - Free reset (no Tradebar cost)</li>
 *   <li>{@code debug <player>} - Dump full passive tree state</li>
 * </ul>
 */
public class PassiveCommand extends AbstractCommandCollection {

    public PassiveCommand() {
        super("passive", "hyforged.commands.passive.desc");
        
        // Add subcommands
        this.addSubCommand(new PassiveListCommand());
        this.addSubCommand(new PassiveGrantPointCommand());
        this.addSubCommand(new PassiveResetCommand());
        this.addSubCommand(new PassiveDebugCommand());
    }
    
    @Override
    protected boolean canGeneratePermission() {
        // Allow all players to access /passive; individual subcommands control their own permissions
        return false;
    }
}
