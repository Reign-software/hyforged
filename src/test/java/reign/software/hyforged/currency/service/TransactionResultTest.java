package reign.software.hyforged.currency.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TransactionResult}.
 */
@DisplayName("TransactionResult")
class TransactionResultTest {

    @Nested
    @DisplayName("Success factory method")
    class SuccessTests {

        @Test
        @DisplayName("creates successful result with generated ID")
        void successWithGeneratedId() {
            TransactionResult result = TransactionResult.success(100, 150);

            assertTrue(result.success());
            assertNotNull(result.transactionId());
            assertFalse(result.transactionId().isEmpty());
            assertEquals(100, result.balanceBefore());
            assertEquals(150, result.balanceAfter());
            assertNull(result.failureReason());
        }

        @Test
        @DisplayName("creates successful result with custom ID")
        void successWithCustomId() {
            TransactionResult result = TransactionResult.success("custom-tx-123", 50, 100);

            assertTrue(result.success());
            assertEquals("custom-tx-123", result.transactionId());
            assertEquals(50, result.balanceBefore());
            assertEquals(100, result.balanceAfter());
            assertNull(result.failureReason());
        }

        @Test
        @DisplayName("calculates positive amount changed for deposit")
        void amountChangedForDeposit() {
            TransactionResult result = TransactionResult.success(100, 250);

            assertEquals(150, result.amountChanged());
        }

        @Test
        @DisplayName("calculates negative amount changed for deduction")
        void amountChangedForDeduction() {
            TransactionResult result = TransactionResult.success(500, 350);

            assertEquals(-150, result.amountChanged());
        }
    }

    @Nested
    @DisplayName("Failure factory method")
    class FailureTests {

        @Test
        @DisplayName("creates failed result with reason")
        void failureWithReason() {
            TransactionResult result = TransactionResult.failure(
                    TransactionResult.REASON_INSUFFICIENT_BALANCE, 100);

            assertFalse(result.success());
            assertNotNull(result.transactionId());
            assertEquals(100, result.balanceBefore());
            assertEquals(100, result.balanceAfter()); // Unchanged on failure
            assertEquals(TransactionResult.REASON_INSUFFICIENT_BALANCE, result.failureReason());
        }

        @Test
        @DisplayName("has zero amount changed on failure")
        void amountChangedOnFailure() {
            TransactionResult result = TransactionResult.failure(
                    TransactionResult.REASON_INVALID_AMOUNT, 200);

            assertEquals(0, result.amountChanged());
        }
    }

    @Nested
    @DisplayName("Failure reason constants")
    class FailureReasonConstants {

        @Test
        @DisplayName("has INSUFFICIENT_BALANCE constant")
        void insufficientBalance() {
            assertEquals("INSUFFICIENT_BALANCE", TransactionResult.REASON_INSUFFICIENT_BALANCE);
        }

        @Test
        @DisplayName("has INSUFFICIENT_SPACE constant")
        void insufficientSpace() {
            assertEquals("INSUFFICIENT_SPACE", TransactionResult.REASON_INSUFFICIENT_SPACE);
        }

        @Test
        @DisplayName("has NOT_OWNER constant")
        void notOwner() {
            assertEquals("NOT_OWNER", TransactionResult.REASON_NOT_OWNER);
        }

        @Test
        @DisplayName("has VAULT_NOT_FOUND constant")
        void vaultNotFound() {
            assertEquals("VAULT_NOT_FOUND", TransactionResult.REASON_VAULT_NOT_FOUND);
        }

        @Test
        @DisplayName("has VAULT_CAPACITY_EXCEEDED constant")
        void vaultCapacityExceeded() {
            assertEquals("VAULT_CAPACITY_EXCEEDED", TransactionResult.REASON_VAULT_CAPACITY_EXCEEDED);
        }

        @Test
        @DisplayName("has INVALID_AMOUNT constant")
        void invalidAmount() {
            assertEquals("INVALID_AMOUNT", TransactionResult.REASON_INVALID_AMOUNT);
        }

        @Test
        @DisplayName("has NOT_IMPLEMENTED constant")
        void notImplemented() {
            assertEquals("NOT_IMPLEMENTED", TransactionResult.REASON_NOT_IMPLEMENTED);
        }

        @Test
        @DisplayName("has PLAYER_NOT_FOUND constant")
        void playerNotFound() {
            assertEquals("PLAYER_NOT_FOUND", TransactionResult.REASON_PLAYER_NOT_FOUND);
        }

        @Test
        @DisplayName("has INVALID_PLAYER constant")
        void invalidPlayer() {
            assertEquals("INVALID_PLAYER", TransactionResult.REASON_INVALID_PLAYER);
        }

        @Test
        @DisplayName("has INVENTORY_FULL constant")
        void inventoryFull() {
            assertEquals("INVENTORY_FULL", TransactionResult.REASON_INVENTORY_FULL);
        }
    }
}
