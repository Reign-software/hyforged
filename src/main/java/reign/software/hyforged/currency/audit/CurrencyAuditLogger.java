package reign.software.hyforged.currency.audit;

import reign.software.hyforged.currency.service.TransactionType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Audit logger for currency transactions.
 * <p>
 * Logs all currency operations to a dedicated audit file with daily rotation.
 * Format: ISO timestamp | transaction ID | player UUID | type | amount | before | after | reason
 * <p>
 * Features:
 * - Daily log rotation
 * - Append-only for crash safety (flush on write)
 * - Rate limiting for repeated small transactions
 */
public final class CurrencyAuditLogger {

    private static final Logger LOGGER = Logger.getLogger(CurrencyAuditLogger.class.getName());

    private static final String LOG_DIR = "logs/hyforged";
    private static final String LOG_FILE_PREFIX = "currency_audit";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    
    // Rate limiting: aggregate small transactions within 1 second
    private static final long RATE_LIMIT_WINDOW_MS = 1000;
    
    private static CurrencyAuditLogger instance;
    
    private PrintWriter writer;
    private LocalDate currentLogDate;
    private final Object writeLock = new Object();
    
    // Rate limiting state per player
    private final Map<UUID, AggregatedTransaction> pendingTransactions = new ConcurrentHashMap<>();

    private CurrencyAuditLogger() {
        ensureLogDirectory();
        openLogFile();
    }

    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static CurrencyAuditLogger get() {
        if (instance == null) {
            instance = new CurrencyAuditLogger();
        }
        return instance;
    }

    /**
     * Log a currency transaction.
     *
     * @param playerUUID Player UUID
     * @param transactionId Transaction ID
     * @param type Transaction type
     * @param amount Amount changed
     * @param balanceBefore Balance before transaction
     * @param balanceAfter Balance after transaction
     * @param reason Reason/source for the transaction
     */
    public void log(
            @Nonnull UUID playerUUID,
            @Nonnull String transactionId,
            @Nonnull TransactionType type,
            int amount,
            int balanceBefore,
            int balanceAfter,
            @Nullable String reason
    ) {
        // Check for rate limiting aggregation
        if (shouldAggregate(playerUUID, type, amount)) {
            aggregateTransaction(playerUUID, type, amount, balanceAfter, reason);
            return;
        }
        
        writeLogEntry(playerUUID, transactionId, type, amount, balanceBefore, balanceAfter, reason);
    }

    /**
     * Flush any pending aggregated transactions for a player.
     *
     * @param playerUUID Player UUID
     */
    public void flushPending(@Nonnull UUID playerUUID) {
        AggregatedTransaction pending = pendingTransactions.remove(playerUUID);
        if (pending != null) {
            writeLogEntry(
                playerUUID,
                UUID.randomUUID().toString(),
                pending.type,
                pending.totalAmount,
                pending.balanceBefore,
                pending.balanceAfter,
                pending.reason + " (aggregated: " + pending.count + " transactions)"
            );
        }
    }

    /**
     * Flush all pending aggregated transactions.
     */
    public void flushAllPending() {
        for (UUID playerUUID : pendingTransactions.keySet()) {
            flushPending(playerUUID);
        }
    }

    /**
     * Close the audit logger.
     */
    public void close() {
        flushAllPending();
        synchronized (writeLock) {
            if (writer != null) {
                writer.close();
                writer = null;
            }
        }
    }

    private void writeLogEntry(
            @Nonnull UUID playerUUID,
            @Nonnull String transactionId,
            @Nonnull TransactionType type,
            int amount,
            int balanceBefore,
            int balanceAfter,
            @Nullable String reason
    ) {
        synchronized (writeLock) {
            checkLogRotation();
            
            if (writer == null) {
                LOGGER.warning("Audit log writer not available");
                return;
            }
            
            String timestamp = ZonedDateTime.now().format(TIMESTAMP_FORMAT);
            String reasonStr = reason != null ? reason : "";
            
            // Format: timestamp | txId | playerUUID | type | amount | before | after | reason
            String entry = String.format("%s|%s|%s|%s|%d|%d|%d|%s",
                timestamp,
                transactionId,
                playerUUID,
                type.name(),
                amount,
                balanceBefore,
                balanceAfter,
                reasonStr
            );
            
            writer.println(entry);
            writer.flush(); // Flush immediately for crash safety
        }
    }

    private boolean shouldAggregate(@Nonnull UUID playerUUID, @Nonnull TransactionType type, int amount) {
        // Only aggregate small EARN transactions
        if (type != TransactionType.EARN || Math.abs(amount) > 100) {
            return false;
        }
        
        AggregatedTransaction pending = pendingTransactions.get(playerUUID);
        if (pending == null) {
            return false;
        }
        
        // Aggregate if within time window and same type
        return pending.type == type && 
               (System.currentTimeMillis() - pending.startTime) < RATE_LIMIT_WINDOW_MS;
    }

    private void aggregateTransaction(
            @Nonnull UUID playerUUID,
            @Nonnull TransactionType type,
            int amount,
            int balanceAfter,
            @Nullable String reason
    ) {
        pendingTransactions.compute(playerUUID, (uuid, existing) -> {
            if (existing == null) {
                return new AggregatedTransaction(type, amount, balanceAfter - amount, balanceAfter, reason);
            }
            existing.totalAmount += amount;
            existing.balanceAfter = balanceAfter;
            existing.count++;
            return existing;
        });
    }

    private void ensureLogDirectory() {
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
                LOGGER.info("Created currency audit log directory: " + LOG_DIR);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create audit log directory", e);
        }
    }

    private void openLogFile() {
        synchronized (writeLock) {
            try {
                currentLogDate = LocalDate.now();
                String filename = LOG_FILE_PREFIX + "_" + currentLogDate.format(DATE_FORMAT) + ".log";
                Path logPath = Paths.get(LOG_DIR, filename);
                
                writer = new PrintWriter(new BufferedWriter(new FileWriter(logPath.toFile(), true)));
                LOGGER.info("Opened currency audit log: " + logPath);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to open audit log file", e);
            }
        }
    }

    private void checkLogRotation() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentLogDate)) {
            // Close current file and open new one
            if (writer != null) {
                writer.close();
            }
            openLogFile();
        }
    }

    /**
     * Helper class for aggregating small transactions.
     */
    private static class AggregatedTransaction {
        final TransactionType type;
        int totalAmount;
        int balanceBefore;
        int balanceAfter;
        String reason;
        int count;
        final long startTime;

        AggregatedTransaction(TransactionType type, int amount, int before, int after, String reason) {
            this.type = type;
            this.totalAmount = amount;
            this.balanceBefore = before;
            this.balanceAfter = after;
            this.reason = reason;
            this.count = 1;
            this.startTime = System.currentTimeMillis();
        }
    }
}
