package reign.software.hyforged.currency.service;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.service.HyforgedItemDataService;
import reign.software.hyforged.currency.audit.CurrencyAuditLogger;
import reign.software.hyforged.currency.config.SellValueConfig;
import reign.software.hyforged.quality.service.HyforgedQualityService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing Tradebar currency operations.
 * <p>
 * Provides methods for:
 * - Balance queries (inventory and vault)
 * - Deposit and deduct operations
 * - Sell value calculations
 * - Vault operations
 * <p>
 * All operations are server-authoritative and fully audited.
 */
public final class CurrencyService {

    private static final Logger LOGGER = Logger.getLogger(CurrencyService.class.getName());

    /** The Tradebar item ID */
    public static final String TRADEBAR_ITEM_ID = "hyforged:tradebar";
    
    /** Maximum stack size for Tradebars */
    public static final int MAX_STACK_SIZE = 10000;

    private static CurrencyService instance;

    private CurrencyService() {
        // Initialize
        LOGGER.info("CurrencyService initialized");
    }

    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static CurrencyService get() {
        if (instance == null) {
            instance = new CurrencyService();
        }
        return instance;
    }

    // ========== BALANCE OPERATIONS ==========

    /**
     * Get the total Tradebar balance in a player's inventory.
     *
     * @param player The player entity reference
     * @return Total Tradebars in inventory
     */
    public int getBalance(@Nonnull Ref<EntityStore> player) {
        if (!player.isValid()) {
            LOGGER.fine("getBalance: Invalid player reference");
            return 0;
        }
        
        Player playerComponent = player.getStore().getComponent(player, Player.getComponentType());
        if (playerComponent == null) {
            LOGGER.fine("getBalance: No Player component found");
            return 0;
        }
        
        Inventory inventory = playerComponent.getInventory();
        if (inventory == null) {
            LOGGER.fine("getBalance: No inventory found");
            return 0;
        }
        
        CombinedItemContainer container = inventory.getCombinedHotbarFirst();
        if (container == null) {
            LOGGER.fine("getBalance: No combined container found");
            return 0;
        }
        
        // Count all Tradebar items in inventory
        int total = container.countItemStacks(stack -> isTradebar(stack));
        
        LOGGER.fine("getBalance: Player has " + total + " Tradebars");
        return total;
    }

    /**
     * Get the Tradebar balance in a specific vault.
     *
     * @param player The player entity reference (for ownership check)
     * @param vaultPos The vault block position
     * @return Tradebars in vault, or -1 if not owner or vault not found
     */
    public int getVaultBalance(@Nonnull Ref<EntityStore> player, @Nonnull BlockPosition vaultPos) {
        // TODO: Implement vault component access
        // This requires looking up TradebarVaultComponent at the block position
        // and validating ownership
        LOGGER.fine("getVaultBalance called - returning -1 (not implemented)");
        return -1;
    }

    // ========== INVENTORY OPERATIONS ==========

    /**
     * Deduct Tradebars from a player's inventory.
     *
     * @param player The player entity reference
     * @param amount The amount to deduct
     * @param reason The reason for deduction (for audit)
     * @return Transaction result
     */
    @Nonnull
    public TransactionResult deduct(@Nonnull Ref<EntityStore> player, int amount, @Nonnull String reason) {
        if (amount <= 0) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_AMOUNT, getBalance(player));
        }

        if (!player.isValid()) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_PLAYER, 0);
        }
        
        Player playerComponent = player.getStore().getComponent(player, Player.getComponentType());
        if (playerComponent == null) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_PLAYER, 0);
        }
        
        Inventory inventory = playerComponent.getInventory();
        if (inventory == null) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_PLAYER, 0);
        }
        
        CombinedItemContainer container = inventory.getCombinedHotbarFirst();
        if (container == null) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_PLAYER, 0);
        }

        int currentBalance = getBalance(player);
        if (currentBalance < amount) {
            LOGGER.fine("Deduct failed: insufficient balance (" + currentBalance + " < " + amount + ")");
            return TransactionResult.failure(TransactionResult.REASON_INSUFFICIENT_BALANCE, currentBalance);
        }

        // Remove Tradebars from inventory
        // Using removeItemStack with a new ItemStack to specify amount to remove
        ItemStack toRemove = new ItemStack(TRADEBAR_ITEM_ID, amount);
        container.removeItemStack(toRemove);
        
        int balanceAfter = currentBalance - amount;
        
        // Generate transaction ID and log
        String transactionId = UUID.randomUUID().toString();
        UUID playerUUID = getPlayerUUID(player);
        if (playerUUID != null) {
            logTransaction(playerUUID, transactionId, TransactionType.SPEND, amount, currentBalance, balanceAfter, reason);
        }
        
        LOGGER.log(Level.FINE, "Deduct: {0} Tradebars deducted for reason: {1}", new Object[]{amount, reason});
        
        return TransactionResult.success(transactionId, currentBalance, balanceAfter);
    }

    /**
     * Deposit Tradebars into a player's inventory.
     *
     * @param player The player entity reference
     * @param amount The amount to deposit
     * @param reason The reason for deposit (for audit)
     * @return Transaction result
     */
    @Nonnull
    public TransactionResult deposit(@Nonnull Ref<EntityStore> player, int amount, @Nonnull String reason) {
        if (amount <= 0) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_AMOUNT, getBalance(player));
        }
        
        if (!player.isValid()) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_PLAYER, 0);
        }
        
        Player playerComponent = player.getStore().getComponent(player, Player.getComponentType());
        if (playerComponent == null) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_PLAYER, 0);
        }
        
        Inventory inventory = playerComponent.getInventory();
        if (inventory == null) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_PLAYER, 0);
        }
        
        CombinedItemContainer container = inventory.getCombinedHotbarFirst();
        if (container == null) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_PLAYER, 0);
        }
        
        int balanceBefore = getBalance(player);
        
        // Create Tradebar ItemStack and add to inventory
        ItemStack tradebars = new ItemStack(TRADEBAR_ITEM_ID, amount);
        ItemStackTransaction transaction = container.addItemStack(tradebars);
        
        ItemStack remainder = transaction.getRemainder();
        int addedAmount = amount;
        if (remainder != null && !remainder.isEmpty()) {
            addedAmount = amount - remainder.getQuantity();
        }
        
        if (addedAmount <= 0) {
            return TransactionResult.failure(TransactionResult.REASON_INVENTORY_FULL, balanceBefore);
        }
        
        int balanceAfter = balanceBefore + addedAmount;
        
        // Generate transaction ID and log
        String transactionId = UUID.randomUUID().toString();
        UUID playerUUID = getPlayerUUID(player);
        if (playerUUID != null) {
            logTransaction(playerUUID, transactionId, TransactionType.EARN, addedAmount, balanceBefore, balanceAfter, reason);
        }
        
        LOGGER.log(Level.FINE, "Deposit: {0} Tradebars deposited for reason: {1}", new Object[]{addedAmount, reason});
        
        // If we couldn't add all, return partial success info in the transaction ID
        if (addedAmount < amount) {
            return TransactionResult.success(transactionId + ":partial:" + addedAmount, balanceBefore, balanceAfter);
        }
        
        return TransactionResult.success(transactionId, balanceBefore, balanceAfter);
    }

    // ========== VAULT OPERATIONS ==========

    /**
     * Deposit Tradebars from inventory into a vault.
     *
     * @param player The player entity reference
     * @param vaultPos The vault block position
     * @param amount The amount to deposit
     * @return Transaction result
     */
    @Nonnull
    public TransactionResult vaultDeposit(@Nonnull Ref<EntityStore> player, @Nonnull BlockPosition vaultPos, int amount) {
        if (amount <= 0) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_AMOUNT, getBalance(player));
        }

        // TODO: Implement vault deposit
        // This requires:
        // 1. Validate vault exists at position
        // 2. Validate player is owner
        // 3. Check vault capacity
        // 4. Deduct from inventory
        // 5. Add to vault component
        // 6. Log the transaction

        LOGGER.fine("VaultDeposit called for " + amount + " Tradebars");
        
        int currentBalance = getBalance(player);
        return TransactionResult.failure(TransactionResult.REASON_NOT_IMPLEMENTED, currentBalance);
    }

    /**
     * Withdraw Tradebars from a vault into inventory.
     *
     * @param player The player entity reference
     * @param vaultPos The vault block position
     * @param amount The amount to withdraw
     * @return Transaction result
     */
    @Nonnull
    public TransactionResult vaultWithdraw(@Nonnull Ref<EntityStore> player, @Nonnull BlockPosition vaultPos, int amount) {
        if (amount <= 0) {
            return TransactionResult.failure(TransactionResult.REASON_INVALID_AMOUNT, getBalance(player));
        }

        // TODO: Implement vault withdraw
        // This requires:
        // 1. Validate vault exists at position
        // 2. Validate player is owner
        // 3. Check vault balance
        // 4. Check inventory space
        // 5. Remove from vault component
        // 6. Add to inventory
        // 7. Log the transaction

        LOGGER.fine("VaultWithdraw called for " + amount + " Tradebars");
        
        int currentBalance = getBalance(player);
        return TransactionResult.failure(TransactionResult.REASON_NOT_IMPLEMENTED, currentBalance);
    }

    // ========== SELL VALUE CALCULATION ==========

    /**
     * Calculate the Tradebar sell value for an item.
     *
     * @param item The item stack to evaluate
     * @return The sell value in Tradebars, or 0 if not sellable
     */
    public int calculateSellValue(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) {
            return 0;
        }

        // Get item rarity (Quality)
        String rarity = HyforgedQualityService.getEffectiveQuality(item);
        
        // Count affixes and calculate average tier using HyforgedItemData
        HyforgedItemData itemData = HyforgedItemDataService.read(item);
        int affixCount = itemData.affixCount();
        int avgAffixTier = 1;
        
        if (affixCount > 0) {
            int totalTier = 0;
            for (RolledAffix affix : itemData.affixes()) {
                totalTier += affix.tier();
            }
            avgAffixTier = totalTier / affixCount;
        }
        
        // No override value checking for now (-1 means use formula)
        int overrideValue = -1;

        int sellValue = SellValueConfig.get().calculateSellValue(rarity, affixCount, avgAffixTier, overrideValue);
        
        LOGGER.log(Level.FINE, "calculateSellValue: {0} with rarity {1}, {2} affixes, avg tier {3} = {4} Tradebars",
                new Object[]{item.getItemId(), rarity, affixCount, avgAffixTier, sellValue});
        
        return sellValue;
    }

    /**
     * Calculate the sell value for an item with known properties.
     *
     * @param rarity The item's rarity
     * @param affixCount The number of affixes
     * @param averageAffixTier The average affix tier
     * @param overrideValue Per-item override value (-1 to use formula)
     * @return The sell value in Tradebars
     */
    public int calculateSellValue(
            @Nonnull String rarity,
            int affixCount,
            int averageAffixTier,
            int overrideValue
    ) {
        return SellValueConfig.get().calculateSellValue(rarity, affixCount, averageAffixTier, overrideValue);
    }

    // ========== UTILITY ==========

    /**
     * Check if an item is the Tradebar currency item.
     *
     * @param item The item to check
     * @return True if this is a Tradebar item
     */
    public boolean isTradebar(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        String itemId = item.getItemId();
        return TRADEBAR_ITEM_ID.equals(itemId);
    }

    /**
     * Get the player UUID from an entity reference.
     * Returns null if not a player or not resolvable.
     */
    @Nullable
    private UUID getPlayerUUID(@Nonnull Ref<EntityStore> player) {
        if (!player.isValid()) {
            return null;
        }
        UUIDComponent uuidComponent = player.getStore().getComponent(player, UUIDComponent.getComponentType());
        return uuidComponent != null ? uuidComponent.getUuid() : null;
    }

    /**
     * Log a transaction to the audit log.
     */
    private void logTransaction(
            @Nonnull UUID playerUUID,
            @Nonnull String transactionId,
            @Nonnull TransactionType type,
            int amount,
            int before,
            int after,
            @Nullable String reason
    ) {
        CurrencyAuditLogger.get().log(playerUUID, transactionId, type, amount, before, after, reason);
    }
}
