package reign.software.hyforged.currency.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command to view recent currency transactions for a player.
 * <p>
 * Usage: {@code /hyforged currency audit <player> [count]}
 * <p>
 * Reads from the currency audit log files.
 */
public class CurrencyAuditCommand extends CommandBase {

    private static final Logger LOGGER = Logger.getLogger(CurrencyAuditCommand.class.getName());

    private static final String LOG_DIR = "logs/hyforged";
    private static final String LOG_FILE_PREFIX = "currency_audit";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DEFAULT_COUNT = 10;
    private static final int MAX_COUNT = 50;

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("Player not found or not in a world.");
    private static final Message MESSAGE_NO_TRANSACTIONS = Message.raw("No transactions found for this player.");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player",
            "hyforged.commands.currency.audit.player.desc",
            ArgTypes.PLAYER_REF
    );

    @Nonnull
    private final OptionalArg<Integer> countArg = this.withOptionalArg(
            "count",
            "hyforged.commands.currency.audit.count.desc",
            ArgTypes.INTEGER
    );

    public CurrencyAuditCommand() {
        super("audit", "hyforged.commands.currency.audit.desc");
        this.requirePermission("hyforged.admin.currency");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef targetPlayerRef = this.playerArg.get(context);
        Integer countValue = this.countArg.get(context);
        int count = countValue != null ? Math.min(countValue, MAX_COUNT) : DEFAULT_COUNT;

        Ref<EntityStore> ref = targetPlayerRef.getReference();

        if (ref == null || !ref.isValid()) {
            context.sendMessage(MESSAGE_PLAYER_NOT_FOUND);
            return;
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null) {
                context.sendMessage(MESSAGE_PLAYER_NOT_FOUND);
                return;
            }

            UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent == null) {
                context.sendMessage(MESSAGE_PLAYER_NOT_FOUND);
                return;
            }

            UUID playerUUID = uuidComponent.getUuid();
            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            String playerName = playerRefComponent != null ? playerRefComponent.getUsername() : "Unknown";

            List<String> transactions = readRecentTransactions(playerUUID, count);

            if (transactions.isEmpty()) {
                context.sendMessage(MESSAGE_NO_TRANSACTIONS);
                return;
            }

            context.sendMessage(Message.raw(String.format(
                    "=== Recent transactions for %s (showing %d) ===",
                    playerName, transactions.size())));

            for (String tx : transactions) {
                context.sendMessage(Message.raw(formatTransaction(tx)));
            }
        });
    }

    /**
     * Read recent transactions for a player from audit log files.
     * <p>
     * Searches today's log and previous days if needed.
     */
    @Nonnull
    private List<String> readRecentTransactions(@Nonnull UUID playerUUID, int count) {
        List<String> transactions = new ArrayList<>();
        String playerUUIDString = playerUUID.toString();

        // Search today's log and previous days
        for (int daysBack = 0; daysBack < 7 && transactions.size() < count; daysBack++) {
            LocalDate date = LocalDate.now().minusDays(daysBack);
            Path logPath = getLogPath(date);

            if (!Files.exists(logPath)) {
                continue;
            }

            try {
                List<String> lines = Files.readAllLines(logPath);
                // Read from end of file for most recent
                for (int i = lines.size() - 1; i >= 0 && transactions.size() < count; i--) {
                    String line = lines.get(i);
                    if (line.contains(playerUUIDString)) {
                        transactions.add(line);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read audit log: " + logPath, e);
            }
        }

        // Transactions are already in reverse chronological order
        return transactions;
    }

    @Nonnull
    private Path getLogPath(@Nonnull LocalDate date) {
        String filename = LOG_FILE_PREFIX + "_" + date.format(DATE_FORMAT) + ".log";
        return Paths.get(LOG_DIR, filename);
    }

    /**
     * Format a transaction log line for display.
     * <p>
     * Log format: timestamp | txId | uuid | type | amount | before | after | reason
     */
    @Nonnull
    private String formatTransaction(@Nonnull String logLine) {
        String[] parts = logLine.split("\\|");
        if (parts.length < 8) {
            return logLine; // Return as-is if unexpected format
        }

        String timestamp = parts[0].trim();
        String type = parts[3].trim();
        String amount = parts[4].trim();
        String before = parts[5].trim();
        String after = parts[6].trim();
        String reason = parts[7].trim();

        // Simplify timestamp for display
        String shortTimestamp = timestamp.length() > 19 ? timestamp.substring(0, 19) : timestamp;

        return String.format("%s | %s | %s | %s → %s | %s",
                shortTimestamp, type, amount, before, after, reason);
    }
}
