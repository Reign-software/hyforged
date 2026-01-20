package reign.software.hyforged.stats.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import reign.software.hyforged.affix.command.AffixDumpCommand;
import reign.software.hyforged.affix.command.AffixMetricsCommand;
import reign.software.hyforged.affix.command.CharacterStatsCommand;
import reign.software.hyforged.affix.command.GiveAffixCommand;
import reign.software.hyforged.affix.command.RollAffixCommand;
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
 *   <li>{@code character} - Character stats screen</li>
 *   <li>{@code affixes} - Dump equipped item affixes</li>
 *   <li>{@code rollaffix} - Roll affixes on held item</li>
 *   <li>{@code giveaffix} - Add a specific affix to held item</li>
 *   <li>{@code affixmetrics} - View affix system metrics</li>
 * </ul>
 */
public class HyforgedCommand extends AbstractCommandCollection {

    public HyforgedCommand() {
        super("hyforged", "hyforged.commands.desc");
        
        // Add subcommand collections
        this.addSubCommand(new HyforgedStatsCommand());
        this.addSubCommand(new ProgressionCommand());
        this.addSubCommand(new CharacterStatsCommand());
        
        // Add affix debug commands
        this.addSubCommand(new AffixDumpCommand());
        this.addSubCommand(new RollAffixCommand());
        this.addSubCommand(new GiveAffixCommand());
        this.addSubCommand(new AffixMetricsCommand());
    }
}
