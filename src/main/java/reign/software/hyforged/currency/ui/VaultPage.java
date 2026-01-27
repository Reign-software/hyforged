package reign.software.hyforged.currency.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.currency.component.TradebarVaultComponent;
import reign.software.hyforged.currency.hud.CurrencyHudSystem;
import reign.software.hyforged.currency.service.CurrencyService;
import reign.software.hyforged.currency.service.TransactionResult;
import reign.software.hyforged.currency.service.VaultService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Vault UI page for depositing, withdrawing, and upgrading Tradebar storage.
 */
public class VaultPage extends InteractiveCustomUIPage<VaultPage.PageEventData> {

    private static final String PAGE_UI_FILE = "Hyforged/VaultPage.ui";

    private static final String ACTION_CLOSE = "close";
    private static final String ACTION_DEPOSIT = "deposit";
    private static final String ACTION_DEPOSIT_ALL = "deposit_all";
    private static final String ACTION_WITHDRAW = "withdraw";
    private static final String ACTION_WITHDRAW_ALL = "withdraw_all";
    private static final String ACTION_UPGRADE = "upgrade";

    @Nonnull
    private final Ref<ChunkStore> blockRef;
    
    @Nonnull
    private final TradebarVaultComponent vaultComponent;
    
    @Nullable
    private UUID playerUUID;

    public VaultPage(@Nonnull PlayerRef playerRef, @Nonnull Ref<ChunkStore> blockRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.blockRef = blockRef;
        this.vaultComponent = blockRef.getStore().getComponent(blockRef, HyforgedPlugin.getInstance().getTradebarVaultComponentType());
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append(PAGE_UI_FILE);
        
        // Cache player UUID
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent != null) {
            playerUUID = uuidComponent.getUuid();
            
            // Register vault for HUD tracking if owned by this player
            if (vaultComponent != null && vaultComponent.isOwner(playerUUID)) {
                CurrencyHudSystem.registerVaultAccess(playerUUID, blockRef);
            }
        }
        
        buildVaultView(ref, commandBuilder, eventBuilder);
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

        if (vaultComponent == null || playerUUID == null) {
            sendUpdate();
            return;
        }

        String statusMessage = null;
        boolean isError = false;

        VaultService vaultService = VaultService.getInstance();
        CurrencyService currencyService = CurrencyService.get();

        switch (eventData.getAction()) {
            case ACTION_DEPOSIT -> {
                int amount = eventData.parseAmount();
                if (amount > 0) {
                    // Check if player has enough in inventory
                    int balance = currencyService.getBalance(ref);
                    if (balance < amount) {
                        statusMessage = "Insufficient Tradebars in inventory";
                        isError = true;
                    } else {
                        // Deduct from inventory first
                        TransactionResult deductResult = currencyService.deduct(ref, amount, "vault_deposit");
                        if (deductResult.success()) {
                            // Then deposit to vault
                            TransactionResult depositResult = vaultService.deposit(vaultComponent, amount, playerUUID);
                            if (depositResult.success()) {
                                statusMessage = "Deposited " + amount + " Tradebars";
                            } else {
                                // Refund if vault deposit failed
                                currencyService.deposit(ref, amount, "vault_deposit_refund");
                                statusMessage = "Vault is full";
                                isError = true;
                            }
                        } else {
                            statusMessage = "Failed to deduct from inventory";
                            isError = true;
                        }
                    }
                }
            }
            case ACTION_DEPOSIT_ALL -> {
                int balance = currencyService.getBalance(ref);
                if (balance > 0) {
                    TransactionResult deductResult = currencyService.deduct(ref, balance, "vault_deposit_all");
                    if (deductResult.success()) {
                        TransactionResult depositResult = vaultService.deposit(vaultComponent, balance, playerUUID);
                        int deposited = depositResult.balanceAfter() - depositResult.balanceBefore();
                        if (deposited < balance) {
                            // Refund overflow
                            int refund = balance - deposited;
                            currencyService.deposit(ref, refund, "vault_deposit_all_refund");
                        }
                        statusMessage = "Deposited " + deposited + " Tradebars";
                    }
                } else {
                    statusMessage = "No Tradebars to deposit";
                    isError = true;
                }
            }
            case ACTION_WITHDRAW -> {
                int amount = eventData.parseAmount();
                if (amount > 0) {
                    TransactionResult withdrawResult = vaultService.withdraw(vaultComponent, amount, playerUUID);
                    if (withdrawResult.success()) {
                        int withdrawn = withdrawResult.balanceBefore() - withdrawResult.balanceAfter();
                        TransactionResult depositResult = currencyService.deposit(ref, withdrawn, "vault_withdraw");
                        if (depositResult.success()) {
                            statusMessage = "Withdrew " + withdrawn + " Tradebars";
                        } else {
                            // Refund if inventory deposit failed
                            vaultComponent.setStoredAmount(vaultComponent.getStoredAmount() + withdrawn);
                            statusMessage = "Inventory is full";
                            isError = true;
                        }
                    } else {
                        statusMessage = "Vault is empty";
                        isError = true;
                    }
                }
            }
            case ACTION_WITHDRAW_ALL -> {
                int vaultBalance = vaultComponent.getStoredAmount();
                if (vaultBalance > 0) {
                    TransactionResult withdrawResult = vaultService.withdraw(vaultComponent, vaultBalance, playerUUID);
                    if (withdrawResult.success()) {
                        int withdrawn = withdrawResult.balanceBefore() - withdrawResult.balanceAfter();
                        TransactionResult depositResult = currencyService.deposit(ref, withdrawn, "vault_withdraw_all");
                        if (depositResult.success()) {
                            statusMessage = "Withdrew " + withdrawn + " Tradebars";
                        } else {
                            // Refund if inventory deposit failed
                            vaultComponent.setStoredAmount(vaultComponent.getStoredAmount() + withdrawn);
                            statusMessage = "Inventory is full";
                            isError = true;
                        }
                    }
                } else {
                    statusMessage = "Vault is empty";
                    isError = true;
                }
            }
            case ACTION_UPGRADE -> {
                // First check if player has an upgrade item in inventory
                int targetTier = vaultComponent.getTier() + 1;
                String upgradeItemId = vaultService.getUpgradeItemId(targetTier);
                boolean usedItem = false;
                
                if (upgradeItemId != null) {
                    // Get player's inventory
                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player != null) {
                        Inventory inventory = player.getInventory();
                        if (inventory != null) {
                            CombinedItemContainer container = inventory.getCombinedHotbarFirst();
                            if (container != null) {
                                // Check if player has the upgrade item
                                int itemCount = container.countItemStacks(
                                    stack -> upgradeItemId.equals(stack.getItemId())
                                );
                                if (itemCount > 0) {
                                    // Consume one upgrade item
                                    container.removeItemStack(new ItemStack(upgradeItemId, 1));
                                    // Upgrade without Tradebar cost
                                    TransactionResult upgradeResult = vaultService.upgradeWithItem(vaultComponent, playerUUID);
                                    if (upgradeResult.success()) {
                                        statusMessage = "Upgraded to Tier " + vaultComponent.getTier() + " (used upgrade item)";
                                        usedItem = true;
                                    } else {
                                        // Refund item if upgrade failed
                                        container.addItemStack(new ItemStack(upgradeItemId, 1));
                                        statusMessage = upgradeResult.failureReason() != null 
                                                ? upgradeResult.failureReason() 
                                                : "Upgrade failed";
                                        isError = true;
                                    }
                                }
                            }
                        }
                    }
                }
                
                // If no upgrade item was used, try Tradebar-based upgrade
                if (!usedItem && !isError) {
                    TransactionResult upgradeResult = vaultService.upgrade(vaultComponent, playerUUID);
                    if (upgradeResult.success()) {
                        statusMessage = "Upgraded to Tier " + vaultComponent.getTier();
                    } else {
                        statusMessage = upgradeResult.failureReason() != null 
                                ? upgradeResult.failureReason() 
                                : "Upgrade failed";
                        isError = true;
                    }
                }
            }
        }

        // Rebuild and send updated UI
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        buildVaultView(ref, commandBuilder, eventBuilder);
        
        if (statusMessage != null) {
            String color = isError ? "#d36c6c" : "#6cd36c";
            commandBuilder.set("#StatusMessage.Text", statusMessage);
            commandBuilder.set("#StatusMessage.Style", "(Color: " + color + ";)");
        }
        
        sendUpdate(commandBuilder, eventBuilder, false);
    }

    private void buildVaultView(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder
    ) {
        VaultService vaultService = VaultService.getInstance();
        CurrencyService currencyService = CurrencyService.get();
        
        int inventoryBalance = currencyService.getBalance(ref);
        commandBuilder.set("#InventoryBalance.Text", formatNumber(inventoryBalance));

        if (vaultComponent == null) {
            commandBuilder.set("#VaultBalance.Text", "0");
            commandBuilder.set("#Capacity.Text", "0 / 0");
            commandBuilder.set("#TierDisplay.Text", "Tier 0");
            commandBuilder.set("#UpgradeButton.Enabled", false);
            commandBuilder.set("#UpgradeCost.Text", "");
            return;
        }

        int vaultBalance = vaultComponent.getStoredAmount();
        int tier = vaultComponent.getTier();
        int capacity = vaultService.getCapacity(tier);
        int maxTier = vaultService.getMaxTier();

        commandBuilder.set("#VaultBalance.Text", formatNumber(vaultBalance));
        commandBuilder.set("#Capacity.Text", formatNumber(vaultBalance) + " / " + formatNumber(capacity));
        commandBuilder.set("#TierDisplay.Text", "Tier " + tier);

        // Upgrade button
        boolean canUpgrade = tier < maxTier;
        commandBuilder.set("#UpgradeButton.Visible", canUpgrade);
        if (canUpgrade) {
            int upgradeCost = vaultService.getUpgradeCost(tier);
            boolean hasEnough = vaultBalance >= upgradeCost;
            commandBuilder.set("#UpgradeButton.Enabled", hasEnough);
            commandBuilder.set("#UpgradeCost.Text", "Cost: " + formatNumber(upgradeCost) + " Tradebars");
            String costColor = hasEnough ? "#6cd36c" : "#d36c6c";
            commandBuilder.set("#UpgradeCost.Style", "(Color: " + costColor + ";)");
        } else {
            commandBuilder.set("#UpgradeCost.Text", "Max Tier Reached");
        }

        // Event bindings
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of(PageEventData.KEY_ACTION, ACTION_CLOSE), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#DepositButton", EventData.of(PageEventData.KEY_ACTION, ACTION_DEPOSIT), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#DepositAllButton", EventData.of(PageEventData.KEY_ACTION, ACTION_DEPOSIT_ALL), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WithdrawButton", EventData.of(PageEventData.KEY_ACTION, ACTION_WITHDRAW), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WithdrawAllButton", EventData.of(PageEventData.KEY_ACTION, ACTION_WITHDRAW_ALL), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#UpgradeButton", EventData.of(PageEventData.KEY_ACTION, ACTION_UPGRADE), false);
    }

    @Nonnull
    private static String formatNumber(int value) {
        return String.format("%,d", value);
    }

    /**
     * Event data codec for vault page events.
     */
    public static class PageEventData {
        static final String KEY_ACTION = "Action";
        static final String KEY_AMOUNT = "Amount";

        @Nonnull
        static final BuilderCodec<PageEventData> CODEC = BuilderCodec
                .builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (data, value) -> data.action = value, data -> data.action)
                .add()
                .append(new KeyedCodec<>(KEY_AMOUNT, Codec.STRING), (data, value) -> data.amount = value, data -> data.amount)
                .add()
                .build();

        private String action;
        private String amount;

        public PageEventData() {
        }

        public String getAction() {
            return action;
        }

        public int parseAmount() {
            if (amount == null || amount.isBlank()) {
                return 0;
            }
            try {
                return Math.max(0, Integer.parseInt(amount.trim().replace(",", "")));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }
}
