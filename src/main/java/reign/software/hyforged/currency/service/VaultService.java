package reign.software.hyforged.currency.service;

import reign.software.hyforged.currency.component.TradebarVaultComponent;
import reign.software.hyforged.currency.config.VaultUpgradesConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Service for managing Tradebar vaults and their storage operations.
 */
public class VaultService {

    private static VaultService instance;

    public static VaultService getInstance() {
        if (instance == null) {
            instance = new VaultService();
        }
        return instance;
    }

    private VaultService() {
        // Singleton
    }

    /**
     * Get the capacity of a vault based on its tier.
     *
     * @param tier The vault tier (1-5)
     * @return The maximum Tradebars the vault can hold
     */
    public int getCapacity(int tier) {
        return VaultUpgradesConfig.get().getCapacity(tier);
    }

    /**
     * Get the cost to upgrade a vault to the next tier.
     *
     * @param currentTier The current tier of the vault
     * @return The cost in Tradebars to upgrade, or 0 if max tier
     */
    public int getUpgradeCost(int currentTier) {
        return VaultUpgradesConfig.get().getUpgradeCost(currentTier + 1);
    }

    /**
     * Get the maximum tier a vault can be upgraded to.
     *
     * @return The maximum tier
     */
    public int getMaxTier() {
        return VaultUpgradesConfig.get().getMaxTier();
    }

    /**
     * Deposit Tradebars into a vault.
     *
     * @param vault  The vault component
     * @param amount The amount to deposit
     * @param playerUUID The player performing the deposit
     * @return Transaction result indicating success or failure
     */
    @Nonnull
    public TransactionResult deposit(@Nonnull TradebarVaultComponent vault, int amount, @Nonnull UUID playerUUID) {
        if (!vault.isOwner(playerUUID)) {
            return TransactionResult.failure(TransactionResult.REASON_NOT_OWNER, vault.getStoredAmount());
        }

        if (amount <= 0) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_AMOUNT, vault.getStoredAmount());
        }

        int capacity = getCapacity(vault.getTier());
        int currentAmount = vault.getStoredAmount();
        int space = capacity - currentAmount;

        if (space <= 0) {
            return TransactionResult.failure(TransactionResult.REASON_VAULT_CAPACITY_EXCEEDED, currentAmount);
        }

        int toDeposit = Math.min(amount, space);
        int newAmount = currentAmount + toDeposit;
        vault.setStoredAmount(newAmount);

        return TransactionResult.success(currentAmount, newAmount);
    }

    /**
     * Withdraw Tradebars from a vault.
     *
     * @param vault  The vault component
     * @param amount The amount to withdraw
     * @param playerUUID The player performing the withdrawal
     * @return Transaction result indicating success or failure
     */
    @Nonnull
    public TransactionResult withdraw(@Nonnull TradebarVaultComponent vault, int amount, @Nonnull UUID playerUUID) {
        if (!vault.isOwner(playerUUID)) {
            return TransactionResult.failure(TransactionResult.REASON_NOT_OWNER, vault.getStoredAmount());
        }

        if (amount <= 0) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_AMOUNT, vault.getStoredAmount());
        }

        int currentAmount = vault.getStoredAmount();
        if (currentAmount <= 0) {
            return TransactionResult.failure(TransactionResult.REASON_INSUFFICIENT_BALANCE, 0);
        }

        int toWithdraw = Math.min(amount, currentAmount);
        int newAmount = currentAmount - toWithdraw;
        vault.setStoredAmount(newAmount);

        return TransactionResult.success(currentAmount, newAmount);
    }

    /**
     * Upgrade a vault to the next tier using Tradebars from vault storage.
     *
     * @param vault  The vault component
     * @param playerUUID The player performing the upgrade
     * @return Transaction result indicating success or failure
     */
    @Nonnull
    public TransactionResult upgrade(@Nonnull TradebarVaultComponent vault, @Nonnull UUID playerUUID) {
        if (!vault.isOwner(playerUUID)) {
            return TransactionResult.failure(TransactionResult.REASON_NOT_OWNER, vault.getStoredAmount());
        }

        int currentTier = vault.getTier();
        if (currentTier >= getMaxTier()) {
            return TransactionResult.failure("MAX_TIER_REACHED", vault.getStoredAmount());
        }

        int upgradeCost = getUpgradeCost(currentTier);
        if (vault.getStoredAmount() < upgradeCost) {
            return TransactionResult.failure(TransactionResult.REASON_INSUFFICIENT_BALANCE, vault.getStoredAmount());
        }

        // Deduct cost from vault storage and upgrade
        int newAmount = vault.getStoredAmount() - upgradeCost;
        vault.setStoredAmount(newAmount);
        vault.setTier(currentTier + 1);

        return TransactionResult.success(vault.getStoredAmount() + upgradeCost, newAmount);
    }

    /**
     * Upgrade a vault to the next tier without cost (used when upgrade item is consumed).
     * Does not check for payment - caller is responsible for consuming the upgrade item first.
     *
     * @param vault      The vault component
     * @param playerUUID The player performing the upgrade
     * @return Transaction result indicating success or failure
     */
    @Nonnull
    public TransactionResult upgradeWithItem(@Nonnull TradebarVaultComponent vault, @Nonnull UUID playerUUID) {
        if (!vault.isOwner(playerUUID)) {
            return TransactionResult.failure(TransactionResult.REASON_NOT_OWNER, vault.getStoredAmount());
        }

        int currentTier = vault.getTier();
        if (currentTier >= getMaxTier()) {
            return TransactionResult.failure("MAX_TIER_REACHED", vault.getStoredAmount());
        }

        // Upgrade without cost (item already consumed)
        vault.setTier(currentTier + 1);
        
        return TransactionResult.success(vault.getStoredAmount(), vault.getStoredAmount());
    }

    /**
     * Get the upgrade item ID for upgrading to a specific tier.
     *
     * @param targetTier The target tier (e.g., 2, 3, or 4)
     * @return The item ID for the upgrade item, or null if tier is invalid
     */
    @Nullable
    public String getUpgradeItemId(int targetTier) {
        if (targetTier < 2 || targetTier > getMaxTier()) {
            return null;
        }
        return "hyforged:Vault_Upgrade_Tier" + targetTier;
    }
}
