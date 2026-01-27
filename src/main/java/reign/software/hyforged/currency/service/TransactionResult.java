package reign.software.hyforged.currency.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Result of a currency transaction.
 * <p>
 * Provides information about the success or failure of a transaction,
 * including balances before and after, and a unique transaction ID.
 */
public record TransactionResult(
    boolean success,
    @Nonnull String transactionId,
    int balanceBefore,
    int balanceAfter,
    @Nullable String failureReason
) {
    
    // Common failure reasons
    public static final String REASON_INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
    public static final String REASON_INSUFFICIENT_SPACE = "INSUFFICIENT_SPACE";
    public static final String REASON_NOT_OWNER = "NOT_OWNER";
    public static final String REASON_VAULT_NOT_FOUND = "VAULT_NOT_FOUND";
    public static final String REASON_VAULT_CAPACITY_EXCEEDED = "VAULT_CAPACITY_EXCEEDED";
    public static final String REASON_INVALID_AMOUNT = "INVALID_AMOUNT";
    public static final String REASON_NOT_IMPLEMENTED = "NOT_IMPLEMENTED";
    public static final String REASON_PLAYER_NOT_FOUND = "PLAYER_NOT_FOUND";
    public static final String REASON_INVALID_PLAYER = "INVALID_PLAYER";
    public static final String REASON_INVENTORY_FULL = "INVENTORY_FULL";
    
    /**
     * Create a successful transaction result.
     *
     * @param before Balance before the transaction
     * @param after Balance after the transaction
     * @return Successful transaction result
     */
    @Nonnull
    public static TransactionResult success(int before, int after) {
        return new TransactionResult(
            true,
            generateTransactionId(),
            before,
            after,
            null
        );
    }
    
    /**
     * Create a successful transaction result with custom transaction ID.
     *
     * @param txId Custom transaction ID
     * @param before Balance before the transaction
     * @param after Balance after the transaction
     * @return Successful transaction result
     */
    @Nonnull
    public static TransactionResult success(@Nonnull String txId, int before, int after) {
        return new TransactionResult(
            true,
            txId,
            before,
            after,
            null
        );
    }
    
    /**
     * Create a failed transaction result.
     *
     * @param reason The failure reason
     * @param balance The current balance (unchanged)
     * @return Failed transaction result
     */
    @Nonnull
    public static TransactionResult failure(@Nonnull String reason, int balance) {
        return new TransactionResult(
            false,
            generateTransactionId(),
            balance,
            balance,
            reason
        );
    }
    
    /**
     * Generate a unique transaction ID.
     *
     * @return UUID-based transaction ID
     */
    @Nonnull
    private static String generateTransactionId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Get the amount changed in this transaction.
     *
     * @return The difference between after and before balances
     */
    public int amountChanged() {
        return balanceAfter - balanceBefore;
    }
}
