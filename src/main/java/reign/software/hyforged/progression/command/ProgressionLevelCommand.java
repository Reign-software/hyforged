package reign.software.hyforged.progression.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Parent command for level management.
 * <p>
 * Usage: {@code /hyforged progression level <subcommand>}
 * <p>
 * Subcommands:
 * <ul>
 *   <li>{@code set <player> <level>} - Set character level</li>
 *   <li>{@code classset <player> <classId> <level>} - Set class level</li>
 * </ul>
 */
public class ProgressionLevelCommand extends AbstractCommandCollection {

    public ProgressionLevelCommand() {
        super("level", "hyforged.commands.progression.level.desc");
        
        // Add subcommands
        this.addSubCommand(new LevelSetCommand());
        this.addSubCommand(new ClassLevelSetCommand());
    }
}
