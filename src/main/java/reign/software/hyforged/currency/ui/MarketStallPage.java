package reign.software.hyforged.currency.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.currency.service.CurrencyService;
import reign.software.hyforged.currency.service.MarketStallService;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Market Stall UI page for selling items for Tradebars.
 */
public class MarketStallPage extends InteractiveCustomUIPage<MarketStallPage.PageEventData> {

    private static final String PAGE_UI_FILE = "Hyforged/MarketStallPage.ui";

    private static final String ACTION_CLOSE = "close";
    private static final String ACTION_SELL_ALL = "sell_all";
    private static final String ACTION_REFRESH = "refresh";

    public MarketStallPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append(PAGE_UI_FILE);
        buildMarketView(ref, commandBuilder, eventBuilder, store);
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData eventData
    ) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            sendUpdate();
            return;
        }

        if (ACTION_CLOSE.equals(eventData.getAction())) {
            playerComponent.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        String statusMessage = null;
        boolean isError = false;

        if (ACTION_SELL_ALL.equals(eventData.getAction())) {
            // Get all sellable items from inventory
            List<ItemStack> sellableItems = getSellableItems(ref, store);
            
            if (sellableItems.isEmpty()) {
                statusMessage = "No sellable items in inventory";
                isError = true;
            } else {
                // Sell all items
                MarketStallService.SellResult result = MarketStallService.getInstance().sellItems(ref, sellableItems);
                
                if (result.success()) {
                    // Remove sold items from inventory
                    removeSoldItems(ref, store, sellableItems);
                    statusMessage = "Sold items for " + formatNumber(result.totalValue()) + " Tradebars!";
                } else {
                    statusMessage = result.errorMessage();
                    isError = true;
                }
            }
        }

        // Rebuild and send updated UI
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        buildMarketView(ref, commandBuilder, eventBuilder, store);
        
        if (statusMessage != null) {
            String color = isError ? "#d36c6c" : "#6cd36c";
            commandBuilder.set("#StatusMessage.Text", statusMessage);
            commandBuilder.set("#StatusMessage.Style", "(Color: " + color + ";)");
        }
        
        sendUpdate(commandBuilder, eventBuilder, false);
    }

    private void buildMarketView(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        CurrencyService currencyService = CurrencyService.get();
        
        // Show current balance
        int balance = currencyService.getBalance(ref);
        commandBuilder.set("#CurrentBalance.Text", formatNumber(balance));

        // Calculate total value of sellable items
        List<ItemStack> sellableItems = getSellableItems(ref, store);
        int totalValue = MarketStallService.getInstance().calculateTotalValue(sellableItems);
        int itemCount = sellableItems.size();

        commandBuilder.set("#TotalValue.Text", formatNumber(totalValue) + " Tradebars");
        commandBuilder.set("#ItemCount.Text", itemCount + " sellable items");

        // Enable/disable sell button based on whether there are items
        boolean canSell = totalValue > 0;
        commandBuilder.set("#SellAllButton.Enabled", canSell);

        // Event bindings
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of(PageEventData.KEY_ACTION, ACTION_CLOSE), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SellAllButton", EventData.of(PageEventData.KEY_ACTION, ACTION_SELL_ALL), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshButton", EventData.of(PageEventData.KEY_ACTION, ACTION_REFRESH), false);
    }

    @Nonnull
    private List<ItemStack> getSellableItems(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        List<ItemStack> sellableItems = new ArrayList<>();
        
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return sellableItems;
        }
        
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return sellableItems;
        }

        CombinedItemContainer container = inventory.getCombinedHotbarFirst();
        if (container == null) {
            return sellableItems;
        }

        CurrencyService currencyService = CurrencyService.get();
        
        // Collect all sellable items by iterating slots
        for (short slot = 0; slot < container.getCapacity(); slot++) {
            ItemStack item = container.getItemStack(slot);
            if (item != null && !item.isEmpty()) {
                int value = currencyService.calculateSellValue(item);
                if (value > 0) {
                    // Skip Tradebars - you can't sell currency
                    if (!CurrencyService.TRADEBAR_ITEM_ID.equals(item.getItemId())) {
                        sellableItems.add(item);
                    }
                }
            }
        }
        
        return sellableItems;
    }

    private void removeSoldItems(
            @Nonnull Ref<EntityStore> ref, 
            @Nonnull Store<EntityStore> store,
            @Nonnull List<ItemStack> soldItems
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        CombinedItemContainer container = inventory.getCombinedHotbarFirst();
        if (container == null) {
            return;
        }

        // Remove each sold item from inventory by removing from their original slots
        for (ItemStack soldItem : soldItems) {
            // Remove by item ID and quantity
            container.removeItemStack(new ItemStack(soldItem.getItemId(), soldItem.getQuantity()));
        }
    }

    @Nonnull
    private static String formatNumber(int value) {
        return String.format("%,d", value);
    }

    /**
     * Event data codec for market stall page events.
     */
    public static class PageEventData {
        static final String KEY_ACTION = "Action";

        @Nonnull
        static final BuilderCodec<PageEventData> CODEC = BuilderCodec
                .builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (data, value) -> data.action = value, data -> data.action)
                .add()
                .build();

        private String action;

        public PageEventData() {
        }

        public String getAction() {
            return action;
        }
    }
}
