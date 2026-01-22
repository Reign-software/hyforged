package reign.software.hyforged.affix.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.affix.event.AffixesRolledEvent;
import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.service.*;
import reign.software.hyforged.affix.registry.AffixPoolRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ECS system that hooks into item entity creation to roll affixes on eligible items.
 * <p>
 * This system watches for new ItemComponent additions and rolls affixes for
 * equipment items that match affix pool criteria.
 * <p>
 * The system is designed to:
 * <ul>
 *   <li>React to newly spawned item entities</li>
 *   <li>Check if the item is eligible for affixes (equipment with valid pool)</li>
 *   <li>Skip items that already have affixes (prevents double-rolling)</li>
 *   <li>Roll and apply affixes using the AffixRollerService</li>
 * </ul>
 * <p>
 * Note: This hooks into the low-level ECS component system. Items created via
 * ItemComponent.generateItemDrop() will trigger this system when the component
 * is added to the entity.
 */
public class LootAffixSystem extends RefChangeSystem<EntityStore, ItemComponent> {
    
    private static final Logger LOGGER = Logger.getLogger(LootAffixSystem.class.getName());
    
    private final AffixRollerService rollerService;
    private final AffixPoolRegistry poolRegistry;
    
    /**
     * Create a LootAffixSystem with default services.
     */
    public LootAffixSystem() {
        this(new AffixRollerService(), AffixPoolRegistry.get());
    }
    
    /**
     * Create a LootAffixSystem with custom services (for testing).
     */
    public LootAffixSystem(
            @Nonnull AffixRollerService rollerService,
            @Nonnull AffixPoolRegistry poolRegistry
    ) {
        this.rollerService = rollerService;
        this.poolRegistry = poolRegistry;
    }
    
    @Nonnull
    @Override
    public ComponentType<EntityStore, ItemComponent> componentType() {
        return ItemComponent.getComponentType();
    }
    
    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        // Return the ComponentType as the query - this filters to entities with ItemComponent
        return ItemComponent.getComponentType();
    }
    
    /**
     * Called when an ItemComponent is added to an entity (new item spawned).
     * <p>
     * This is the hook point for rolling affixes on newly dropped loot.
     */
    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ItemComponent itemComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        ItemStack itemStack = itemComponent.getItemStack();
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }
        
        // Check if already has affixes (skip to prevent double-rolling)
        HyforgedItemData existingData = HyforgedItemDataService.read(itemStack);
        if (existingData.hasAffixes()) {
            LOGGER.log(Level.FINER, "Item {0} already has affixes, skipping", itemStack.getItemId());
            return;
        }
        
        // Create roll context
        AffixRollContext context = createContext(itemStack);
        if (context == null) {
            return;
        }
        
        // Check if any pool matches this item
        Set<String> categories = Set.of(context.itemCategories());
        Set<String> tags = Set.of(context.itemTags());
        if (poolRegistry.resolve(categories, tags) == null) {
            LOGGER.log(Level.FINER, "No affix pool found for item {0}", itemStack.getItemId());
            return;
        }
        
        // Roll affixes
        AffixRollResult result = rollerService.rollAffixes(context, new Random());
        
        if (result.hasAffixes()) {
            // Emit AffixesRolledEvent BEFORE applying - allows cancellation/modification
            AffixesRolledEvent event = emitAffixesRolledEvent(context, result);
            
            // Check if event was cancelled
            if (event != null && event.isCancelled()) {
                LOGGER.log(Level.FINE, "Affix rolling cancelled for item {0}", itemStack.getItemId());
                return;
            }
            
            // Get effective affixes (may have been replaced by event listeners)
            List<RolledAffix> effectiveAffixes = event != null ? event.getEffectiveAffixes() : result.affixes();
            
            // Apply affixes to item metadata
            HyforgedItemData itemData = HyforgedItemData.create(effectiveAffixes);
            ItemStack updatedStack = HyforgedItemDataService.write(itemStack, itemData);
            itemComponent.setItemStack(updatedStack);
            
            LOGGER.log(Level.FINE, "Applied {0} affixes to item {1}", 
                    new Object[]{effectiveAffixes.size(), itemStack.getItemId()});
        }
    }
    
    /**
     * Emit an AffixesRolledEvent to notify other systems of the roll.
     * <p>
     * Returns the event so the caller can check if it was cancelled.
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
                    0L // No seed tracking for now
            );
            
            dispatcher.dispatch(event);
            return event;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to emit AffixesRolledEvent for item " + context.itemId(), e);
            return null;
        }
    }
    
    /**
     * Called when an ItemComponent is modified.
     * <p>
     * We don't roll on modification - only on initial creation.
     */
    @Override
    public void onComponentSet(
            @Nonnull Ref<EntityStore> ref,
            @Nullable ItemComponent oldComponent,
            @Nonnull ItemComponent newComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No action on component modification
    }
    
    /**
     * Called when an ItemComponent is removed from an entity.
     * <p>
     * No cleanup needed for affixes as they're stored in item metadata.
     */
    @Override
    public void onComponentRemoved(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ItemComponent itemComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No cleanup needed
    }
    
    /**
     * Create a roll context from an ItemStack.
     * <p>
     * Extracts item ID, quality, level, categories, and tags from the item's metadata.
     *
     * @param itemStack The item to create context for
     * @return The roll context, or null if item is not eligible
     */
    @Nullable
    private AffixRollContext createContext(@Nonnull ItemStack itemStack) {
        return ItemContextExtractor.buildContext(itemStack);
    }
}
