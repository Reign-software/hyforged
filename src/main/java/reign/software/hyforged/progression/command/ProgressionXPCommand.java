package reign.software.hyforged.progression.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Parent command for XP management.
 * <p>
 * Usage: {@code /hyforged progression xp <subcommand>}
 * <p>
 * Subcommands:
 * <ul>
 *   <li>{@code add <player> <amount>} - Add character XP</li>
 *   <li>{@code set <player> <amount>} - Set character XP</li>
 *   <li>{@code classadd <player> <classId> <amount>} - Add class XP</li>
 * </ul>
 */
public class ProgressionXPCommand extends AbstractCommandCollection {

    public ProgressionXPCommand() {
        super("xp", "hyforged.commands.progression.xp.desc");
        
        // Add subcommands
        this.addSubCommand(new XPAddCommand());
        this.addSubCommand(new XPSetCommand());
        this.addSubCommand(new ClassXPAddCommand());
    }
}
