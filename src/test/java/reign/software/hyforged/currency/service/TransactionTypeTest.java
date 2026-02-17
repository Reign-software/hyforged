package reign.software.hyforged.currency.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TransactionType}.
 */
@DisplayName("TransactionType")
class TransactionTypeTest {

    @Test
    @DisplayName("has all expected transaction types")
    void hasAllExpectedTypes() {
        TransactionType[] types = TransactionType.values();

        assertEquals(8, types.length);
        assertNotNull(TransactionType.valueOf("EARN"));
        assertNotNull(TransactionType.valueOf("SPEND"));
        assertNotNull(TransactionType.valueOf("VAULT_DEPOSIT"));
        assertNotNull(TransactionType.valueOf("VAULT_WITHDRAW"));
        assertNotNull(TransactionType.valueOf("SELL"));
        assertNotNull(TransactionType.valueOf("ADMIN_GRANT"));
        assertNotNull(TransactionType.valueOf("PASSIVE_REFUND"));
        assertNotNull(TransactionType.valueOf("VAULT_UPGRADE"));
    }

    @Test
    @DisplayName("EARN represents loot and rewards")
    void earnType() {
        assertEquals("EARN", TransactionType.EARN.name());
    }

    @Test
    @DisplayName("SPEND represents purchases")
    void spendType() {
        assertEquals("SPEND", TransactionType.SPEND.name());
    }

    @Test
    @DisplayName("VAULT_DEPOSIT represents vault deposits")
    void vaultDepositType() {
        assertEquals("VAULT_DEPOSIT", TransactionType.VAULT_DEPOSIT.name());
    }

    @Test
    @DisplayName("VAULT_WITHDRAW represents vault withdrawals")
    void vaultWithdrawType() {
        assertEquals("VAULT_WITHDRAW", TransactionType.VAULT_WITHDRAW.name());
    }

    @Test
    @DisplayName("SELL represents item sales")
    void sellType() {
        assertEquals("SELL", TransactionType.SELL.name());
    }

    @Test
    @DisplayName("ADMIN_GRANT represents admin grants")
    void adminGrantType() {
        assertEquals("ADMIN_GRANT", TransactionType.ADMIN_GRANT.name());
    }

    @Test
    @DisplayName("PASSIVE_REFUND represents passive refund costs")
    void passiveRefundType() {
        assertEquals("PASSIVE_REFUND", TransactionType.PASSIVE_REFUND.name());
    }
}
