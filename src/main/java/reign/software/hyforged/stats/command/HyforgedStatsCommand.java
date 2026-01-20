package reign.software.hyforged.stats.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Parent command for all Hyforged stats commands.
 * <p>
 * Usage: {@code /hyforged stats <subcommand>}
 * <p>
 * Subcommands:
 * <ul>
 *   <li>{@code debug <player>} - Show stat breakdown for a player</li>
 *   <li>{@code set <player> <stat> <value>} - Set a player's stat value</li>
 * </ul>
 */
public class HyforgedStatsCommand extends AbstractCommandCollection {

    public HyforgedStatsCommand() {
        super("stats", "hyforged.commands.stats.desc");
        
        // Add subcommands
        this.addSubCommand(new StatsDebugCommand());
        this.addSubCommand(new StatsSetCommand());
    }
}
