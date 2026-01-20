package reign.software.hyforged.stats.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Root command for all Hyforged plugin commands.
 * <p>
 * Usage: {@code /hyforged <subcommand>}
 * <p>
 * Subcommands:
 * <ul>
 *   <li>{@code stats} - Stat management commands</li>
 * </ul>
 */
public class HyforgedCommand extends AbstractCommandCollection {

    public HyforgedCommand() {
        super("hyforged", "hyforged.commands.desc");
        
        // Add subcommand collections
        this.addSubCommand(new HyforgedStatsCommand());
    }
}
