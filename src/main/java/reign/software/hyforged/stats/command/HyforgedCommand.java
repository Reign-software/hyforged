package reign.software.hyforged.stats.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import reign.software.hyforged.progression.command.ProgressionCommand;

/**
 * Root command for all Hyforged plugin commands.
 * <p>
 * Usage: {@code /hyforged <subcommand>}
 * <p>
 * Subcommands:
 * <ul>
 *   <li>{@code stats} - Stat management commands</li>
 *   <li>{@code progression} - Progression management commands</li>
 * </ul>
 */
public class HyforgedCommand extends AbstractCommandCollection {

    public HyforgedCommand() {
        super("hyforged", "hyforged.commands.desc");
        
        // Add subcommand collections
        this.addSubCommand(new HyforgedStatsCommand());
        this.addSubCommand(new ProgressionCommand());
    }
}
