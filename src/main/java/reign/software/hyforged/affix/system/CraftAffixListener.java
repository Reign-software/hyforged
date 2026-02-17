package reign.software.hyforged.affix.system;

import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import reign.software.hyforged.affix.event.AffixesRolledEvent;
import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.registry.AffixPoolRegistry;
import reign.software.hyforged.affix.service.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Event listener that rolls affixes on items entering player inventory without affixes.
 * <p>
 * Listens to {@link LivingEntityInventoryChangeEvent} and checks modified slots for
 * items that match an affix pool but don't yet have affixes. This covers all item
 * acquisition methods: crafting, trading, picking up un-affixed items, etc.
 * <p>
 * This complements {@link LootAffixSystem} which handles dropped loot entities
 * (world item pickups). Together they ensure all equipment items receive affixes
 * regardless of how they were acquired.
 * <p>
 * Guard against double-rolling:
 * <ul>
 *   <li>{@link HyforgedItemDataService#read} checks for existing affixes</li>
 *   <li>Items already processed by LootAffixSystem will have affix metadata</li>
 *   <li>The {@code setItemStackForSlot} call will re-trigger this listener, but
 *       the updated item already has affixes so it is skipped</li>
 * </ul>
 */
public class CraftAffixListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final AffixRollerService rollerService;
    private final AffixPoolRegistry poolRegistry;

    private EventRegistration<String, LivingEntityInventoryChangeEvent> registration;

    /**
     * Create a CraftAffixListener with default services.
     */
    public CraftAffixListener() {
        this(new AffixRollerService(), AffixPoolRegistry.get());
    }

    /**
     * Create a CraftAffixListener with custom services (for testing).
     */
    public CraftAffixListener(
            @Nonnull AffixRollerService rollerService,
            @Nonnull AffixPoolRegistry poolRegistry
    ) {
        this.rollerService = rollerService;
        this.poolRegistry = poolRegistry;
    }

    /**
     * Start listening for inventory change events.
     */
    public void register() {
        registration = HytaleServer.get().getEventBus()
                .registerGlobal((short) 0, LivingEntityInventoryChangeEvent.class, this::onInventoryChange);

        LOGGER.atInfo().log("CraftAffixListener registered for inventory change events");
    }

    /**
     * Stop listening for events.
     */
    public void unregister() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }

    /**
     * Handle an inventory change event.
     * <p>
     * Checks modified slots for items that are eligible for affixes but don't have any.
     * This covers crafted items, traded items, or any item entering inventory without
     * prior affix processing.
     */
    private void onInventoryChange(@Nonnull LivingEntityInventoryChangeEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) {
            return;
        }

        ItemContainer container = event.getItemContainer();
        if (container == null) {
            return;
        }

        // Only process player inventory containers (hotbar, storage, backpack)
        Inventory inventory = entity.getInventory();
        if (inventory == null) {
            return;
        }

        boolean isRelevant = container == inventory.getHotbar()
                || container == inventory.getStorage()
                || container == inventory.getBackpack();
        if (!isRelevant) {
            return;
        }

        Transaction transaction = event.getTransaction();
        if (transaction == null || !transaction.succeeded()) {
            return;
        }

        // Check only modified slots for new items needing affixes
        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            if (!transaction.wasSlotModified(slot)) {
                continue;
            }

            ItemStack itemStack = container.getItemStack(slot);
            if (ItemStack.isEmpty(itemStack)) {
                continue;
            }

            // Skip items that already have affix data (prevents double-rolling)
            HyforgedItemData existingData = HyforgedItemDataService.read(itemStack);
            if (existingData.hasAffixes()) {
                continue;
            }

            // Build roll context from the item
            AffixRollContext context = ItemContextExtractor.buildContext(itemStack);
            if (context == null) {
                continue;
            }

            // Check if any affix pool applies to this item
            Set<String> categories = Set.of(context.itemCategories());
            Set<String> tags = Set.of(context.itemTags());
            if (poolRegistry.resolve(categories, tags) == null) {
                continue;
            }

            // Roll affixes
            AffixRollResult result = rollerService.rollAffixes(context, new Random());

            if (result.hasAffixes()) {
                // Emit AffixesRolledEvent for other systems to react/cancel
                AffixesRolledEvent rolledEvent = emitAffixesRolledEvent(context, result);

                if (rolledEvent != null && rolledEvent.isCancelled()) {
                    LOGGER.at(Level.FINE).log("Affix rolling cancelled for item %s", itemStack.getItemId());
                    continue;
                }

                List<RolledAffix> effectiveAffixes = rolledEvent != null
                        ? rolledEvent.getEffectiveAffixes()
                        : result.affixes();

                // Write affixes to item and update the slot
                HyforgedItemData itemData = HyforgedItemData.create(effectiveAffixes);
                ItemStack updatedStack = HyforgedItemDataService.write(itemStack, itemData);
                container.setItemStackForSlot(slot, updatedStack);

                LOGGER.at(Level.FINE).log("Applied %s affixes to item %s in slot %s", effectiveAffixes.size(), itemStack.getItemId(), slot);
            }
        }
    }

    /**
     * Emit an AffixesRolledEvent for the item.
     *
     * @return The dispatched event, or null if dispatch failed
     */
    @Nullable
    private AffixesRolledEvent emitAffixesRolledEvent(
            @Nonnull AffixRollContext context,
            @Nonnull AffixRollResult result
    ) {
        try {
            IEventDispatcher<AffixesRolledEvent, AffixesRolledEvent> dispatcher =
                    HytaleServer.get().getEventBus().dispatchFor(AffixesRolledEvent.class);

            AffixesRolledEvent event = new AffixesRolledEvent(
                    context,
                    result.poolId(),
                    result.affixes(),
                    0L
            );

            dispatcher.dispatch(event);
            return event;
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to emit AffixesRolledEvent for item %s", context.itemId());
            return null;
        }
    }
}
