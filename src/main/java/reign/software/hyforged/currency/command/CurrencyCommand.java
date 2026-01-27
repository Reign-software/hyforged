package reign.software.hyforged.currency.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Parent command for Hyforged currency (Tradebar) commands.
 * <p>
 * Usage: {@code /hyforged currency <subcommand>}
 * <p>
 * Subcommands:
 * <ul>
 *   <li>{@code balance} - View player Tradebar balance</li>
 *   <li>{@code grant} - Admin grant Tradebars to a player</li>
 *   <li>{@code audit} - View recent transactions for a player</li>
 * </ul>
 */
public class CurrencyCommand extends AbstractCommandCollection {

    public CurrencyCommand() {
        super("currency", "hyforged.commands.currency.desc");

        this.addSubCommand(new CurrencyBalanceCommand());
        this.addSubCommand(new CurrencyGrantCommand());
        this.addSubCommand(new CurrencyAuditCommand());
    }
}
