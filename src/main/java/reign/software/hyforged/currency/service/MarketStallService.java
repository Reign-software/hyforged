package reign.software.hyforged.currency.service;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.currency.audit.CurrencyAuditLogger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Service for selling items at Market Stalls.
 */
public class MarketStallService {

    private static final Logger LOGGER = Logger.getLogger(MarketStallService.class.getName());

    private static MarketStallService instance;

    public static MarketStallService getInstance() {
        if (instance == null) {
            instance = new MarketStallService();
        }
        return instance;
    }

    private MarketStallService() {
        // Singleton
    }

    /**
     * Calculate the total sell value for a list of items.
     *
     * @param items The items to value
     * @return The total Tradebar value
     */
    public int calculateTotalValue(@Nonnull List<ItemStack> items) {
        CurrencyService currencyService = CurrencyService.get();
        int total = 0;
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty()) {
                int value = currencyService.calculateSellValue(item);
                total += value * item.getQuantity();
            }
        }
        return total;
    }

    /**
     * Sell items and deposit Tradebars into the player's inventory.
     *
     * @param playerRef The player selling items
     * @param items     The items to sell (will be consumed)
     * @return Transaction result
     */
    @Nonnull
    public SellResult sellItems(@Nonnull Ref<EntityStore> playerRef, @Nonnull List<ItemStack> items) {
        if (!playerRef.isValid()) {
            return SellResult.failure("Invalid player reference");
        }

        // Calculate total value
        CurrencyService currencyService = CurrencyService.get();
        List<ItemSellInfo> sellInfo = new ArrayList<>();
        int totalValue = 0;

        for (ItemStack item : items) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            
            int unitValue = currencyService.calculateSellValue(item);
            if (unitValue <= 0) {
                continue; // Skip unsellable items
            }
            
            int quantity = item.getQuantity();
            int itemTotal = unitValue * quantity;
            totalValue += itemTotal;
            
            sellInfo.add(new ItemSellInfo(
                    item.getItemId(),
                    quantity,
                    unitValue,
                    itemTotal
            ));
        }

        if (totalValue <= 0) {
            return SellResult.failure("No sellable items");
        }

        // Make final for use in lambda/later code
        final int finalTotalValue = totalValue;

        // Deposit Tradebars
        TransactionResult depositResult = currencyService.deposit(playerRef, finalTotalValue, "market_stall_sale");
        if (!depositResult.success()) {
            return SellResult.failure("Failed to deposit Tradebars: " + depositResult.failureReason());
        }

        // Log the sale
        UUID playerUUID = getPlayerUUID(playerRef);
        if (playerUUID != null) {
            CurrencyAuditLogger.get().log(
                    playerUUID,
                    depositResult.transactionId(),
                    TransactionType.SELL,
                    finalTotalValue,
                    depositResult.balanceBefore(),
                    depositResult.balanceAfter(),
                    "Sold " + sellInfo.size() + " item types"
            );
        }

        LOGGER.fine("Sold items for " + finalTotalValue + " Tradebars to player");

        return SellResult.success(finalTotalValue, sellInfo);
    }

    @javax.annotation.Nullable
    private UUID getPlayerUUID(@Nonnull Ref<EntityStore> playerRef) {
        if (!playerRef.isValid()) {
            return null;
        }
        UUIDComponent uuidComponent = playerRef.getStore().getComponent(playerRef, UUIDComponent.getComponentType());
        return uuidComponent != null ? uuidComponent.getUuid() : null;
    }

    /**
     * Result of a sell operation.
     */
    public record SellResult(
            boolean success,
            int totalValue,
            @Nonnull List<ItemSellInfo> itemsSold,
            @javax.annotation.Nullable String errorMessage
    ) {
        public static SellResult success(int totalValue, @Nonnull List<ItemSellInfo> itemsSold) {
            return new SellResult(true, totalValue, itemsSold, null);
        }

        public static SellResult failure(@Nonnull String errorMessage) {
            return new SellResult(false, 0, List.of(), errorMessage);
        }
    }

    /**
     * Information about a sold item.
     */
    public record ItemSellInfo(
            @Nonnull String itemId,
            int quantity,
            int unitValue,
            int totalValue
    ) {}
}
