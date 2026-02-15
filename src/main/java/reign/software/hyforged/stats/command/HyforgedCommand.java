package reign.software.hyforged.stats.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import reign.software.hyforged.affix.command.AffixDumpCommand;
import reign.software.hyforged.affix.command.AffixMetricsCommand;
import reign.software.hyforged.affix.command.GiveAffixCommand;
import reign.software.hyforged.affix.command.RollAffixCommand;
import reign.software.hyforged.combat.command.CombatLogCommand;

import reign.software.hyforged.currency.command.CurrencyCommand;
import reign.software.hyforged.progression.command.ProgressionCommand;
import reign.software.hyforged.quality.command.QualityCommand;

/**
 * Root command for all Hyforged plugin commands.
 * <p>
 * Usage: {@code /hyforged <subcommand>}
 * <p>
 * Subcommands:
 * <ul>
 *   <li>{@code stats} - Stat management commands</li>
 *   <li>{@code progression} - Progression management commands</li>

 *   <li>{@code affixes} - Dump equipped item affixes</li>
 *   <li>{@code rollaffix} - Roll affixes on held item</li>
 *   <li>{@code giveaffix} - Add a specific affix to held item</li>
 *   <li>{@code affixmetrics} - View affix system metrics</li>
 *   <li>{@code quality} - Quality debug commands</li>
 *   <li>{@code combatlog} - View recent combat history</li>

 *   <li>{@code currency} - Currency (Tradebar) management commands</li>
 * </ul>
 */
public class HyforgedCommand extends AbstractCommandCollection {

    public HyforgedCommand() {
        super("hyforged", "hyforged.commands.desc");
        
        // Add subcommand collections
        this.addSubCommand(new HyforgedStatsCommand());
        this.addSubCommand(new ProgressionCommand());

        
        // Add affix debug commands
        this.addSubCommand(new AffixDumpCommand());
        this.addSubCommand(new RollAffixCommand());
        this.addSubCommand(new GiveAffixCommand());
        this.addSubCommand(new AffixMetricsCommand());

        // Add quality debug commands
        this.addSubCommand(new QualityCommand());
        
        // Add combat commands
        this.addSubCommand(new CombatLogCommand());
        
        // Add currency commands
        this.addSubCommand(new CurrencyCommand());
    }
    
    @Override
    protected boolean canGeneratePermission() {
        // Allow all players to access /hyforged; individual subcommands control their own permissions
        return false;
    }
}
