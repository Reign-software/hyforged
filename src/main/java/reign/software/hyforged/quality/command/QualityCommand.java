package reign.software.hyforged.quality.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Parent command for Hyforged quality debug commands.
 * <p>
 * Usage: {@code /hyforged quality <subcommand>}
 * <p>
 * Subcommands:
 * <ul>
 *   <li>{@code roll} - Roll quality on the held item</li>
 *   <li>{@code npc} - Inspect NPC quality on the targeted entity</li>
 *   <li>{@code metrics} - View quality roll metrics</li>
 * </ul>
 */
public class QualityCommand extends AbstractCommandCollection {

    public QualityCommand() {
        super("quality", "hyforged.commands.quality.desc");

        this.addSubCommand(new QualityRollCommand());
        this.addSubCommand(new QualityNpcCommand());
        this.addSubCommand(new QualityMetricsCommand());
    }
}
