package reign.software.hyforged.progression.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Parent command for all Hyforged progression commands.
 * <p>
 * Usage: {@code /hyforged progression <subcommand>}
 * <p>
 * Subcommands:
 * <ul>
 *   <li>{@code info <player>} - Show progression info for a player</li>
 *   <li>{@code xp add <player> <amount>} - Add character XP</li>
 *   <li>{@code xp set <player> <amount>} - Set character XP</li>
 *   <li>{@code level set <player> <level>} - Set character level</li>
 *   <li>{@code classxp add <player> <classId> <amount>} - Add class XP</li>
 *   <li>{@code classlevel set <player> <classId> <level>} - Set class level</li>
 *   <li>{@code reset <player>} - Reset all progression</li>
 *   <li>{@code debug <player>} - Show detailed debug info</li>
 * </ul>
 */
public class ProgressionCommand extends AbstractCommandCollection {

    public ProgressionCommand() {
        super("progression", "hyforged.commands.progression.desc");
        
        // Add subcommands
        this.addSubCommand(new ProgressionInfoCommand());
        this.addSubCommand(new ProgressionXPCommand());
        this.addSubCommand(new ProgressionLevelCommand());
        this.addSubCommand(new ProgressionResetCommand());
        this.addSubCommand(new ProgressionDebugCommand());
    }
}
