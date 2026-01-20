package reign.software.hyforged.affix.system;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.event.AffixModifiersAppliedEvent;
import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.service.HyforgedItemDataService;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.component.ModifierSource;
import reign.software.hyforged.stats.component.ModifierType;
import reign.software.hyforged.stats.component.StatModifier;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event listener that applies affix stat modifiers when equipment changes.
 * <p>
 * This listener subscribes to {@link LivingEntityInventoryChangeEvent} and:
 * <ul>
 *   <li>Detects armor slot changes</li>
 *   <li>Reads affixes from unequipped items and removes their modifiers</li>
 *   <li>Reads affixes from equipped items and applies their modifiers</li>
 *   <li>Triggers stat recalculation via the dirty-flag model</li>
 * </ul>
 * <p>
 * Equipment affix modifiers use source IDs in format: {@code "equipment:{slot}:{affixId}"}
 * where slot is the armor slot index (0-4) or "hand" for held items.
 */
public class EquipmentAffixListener {
    
    private static final Logger LOGGER = Logger.getLogger(EquipmentAffixListener.class.getName());
    
    /** Prefix for equipment-sourced modifier IDs */
    public static final String EQUIPMENT_SOURCE_PREFIX = "equipment:";
    
    private EventRegistration<String, LivingEntityInventoryChangeEvent> globalRegistration;
    
    /**
     * Start listening for inventory change events.
     */
    public void register() {
        globalRegistration = HytaleServer.get().getEventBus()
                .registerGlobal((short) 0, LivingEntityInventoryChangeEvent.class, this::onInventoryChange);
        
        LOGGER.log(Level.INFO, "EquipmentAffixListener registered for inventory changes");
    }
    
    /**
     * Stop listening for events.
     */
    public void unregister() {
        if (globalRegistration != null) {
            globalRegistration.unregister();
            globalRegistration = null;
        }
    }
    
    /**
     * Handle inventory change events.
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
        
        // Check if this is an armor container change
        Inventory inventory = entity.getInventory();
        if (inventory == null) {
            return;
        }
        
        boolean isArmorChange = container == inventory.getArmor();
        boolean isHotbarChange = container == inventory.getHotbar();
        
        if (!isArmorChange && !isHotbarChange) {
            // Not an equipment change we care about
            return;
        }
        
        // Get the entity's HyforgedStatComponent
        Ref<EntityStore> entityRef = entity.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }
        
        HyforgedStatComponent statComponent = getStatComponent(entity);
        if (statComponent == null) {
            // Entity doesn't have our stat component
            return;
        }
        
        // Process the equipment change
        if (isArmorChange) {
            processArmorChange(entity, statComponent, inventory);
        } else if (isHotbarChange) {
            processHotbarChange(entity, statComponent, inventory);
        }
    }
    
    /**
     * Get the HyforgedStatComponent for an entity.
     */
    @Nullable
    private HyforgedStatComponent getStatComponent(@Nonnull LivingEntity entity) {
        Ref<EntityStore> ref = entity.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        ComponentType<EntityStore, HyforgedStatComponent> componentType = 
                HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        return ref.getStore().getComponent(ref, componentType);
    }
    
    /**
     * Process armor slot changes.
     */
    private void processArmorChange(
            @Nonnull LivingEntity entity,
            @Nonnull HyforgedStatComponent statComponent,
            @Nonnull Inventory inventory
    ) {
        ItemContainer armorContainer = inventory.getArmor();
        
        // Re-sync all armor slot modifiers
        // First, remove all existing armor affix modifiers
        removeArmorAffixModifiers(statComponent);
        
        // Then apply modifiers from current armor
        List<StatModifier> appliedModifiers = new ArrayList<>();
        
        for (short slot = 0; slot < armorContainer.getCapacity(); slot++) {
            ItemStack itemStack = armorContainer.getItemStack(slot);
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            
            List<StatModifier> slotModifiers = applyAffixModifiersFromItem(
                    statComponent, itemStack, "armor:" + slot);
            appliedModifiers.addAll(slotModifiers);
        }
        
        if (!appliedModifiers.isEmpty()) {
            emitModifiersAppliedEvent(entity, "armor", appliedModifiers);
        }
    }
    
    /**
     * Process hotbar/held item changes.
     * <p>
     * Note: This only applies affixes from the active hotbar slot (held item).
     */
    private void processHotbarChange(
            @Nonnull LivingEntity entity,
            @Nonnull HyforgedStatComponent statComponent,
            @Nonnull Inventory inventory
    ) {
        // Remove existing hand affix modifiers
        removeHandAffixModifiers(statComponent);
        
        // Apply modifiers from held item
        ItemStack heldItem = inventory.getItemInHand();
        if (heldItem == null || heldItem.isEmpty()) {
            return;
        }
        
        List<StatModifier> appliedModifiers = applyAffixModifiersFromItem(
                statComponent, heldItem, "hand");
        
        if (!appliedModifiers.isEmpty()) {
            emitModifiersAppliedEvent(entity, "hand", appliedModifiers);
        }
    }
    
    /**
     * Remove all armor-sourced affix modifiers.
     */
    private void removeArmorAffixModifiers(@Nonnull HyforgedStatComponent statComponent) {
        statComponent.removeModifiersIf(
                m -> m.sourceId().startsWith(EQUIPMENT_SOURCE_PREFIX + "armor:"),
                m -> LOGGER.log(Level.FINER, "Removed armor affix modifier: {0}", m.sourceId())
        );
    }
    
    /**
     * Remove all hand-sourced affix modifiers.
     */
    private void removeHandAffixModifiers(@Nonnull HyforgedStatComponent statComponent) {
        statComponent.removeModifiersIf(
                m -> m.sourceId().startsWith(EQUIPMENT_SOURCE_PREFIX + "hand:"),
                m -> LOGGER.log(Level.FINER, "Removed hand affix modifier: {0}", m.sourceId())
        );
    }
    
    /**
     * Apply affix modifiers from an item to the stat component.
     *
     * @param statComponent The stat component to apply to
     * @param itemStack The item containing affixes
     * @param slotId Slot identifier (e.g., "armor:0", "hand")
     * @return List of applied modifiers
     */
    @Nonnull
    private List<StatModifier> applyAffixModifiersFromItem(
            @Nonnull HyforgedStatComponent statComponent,
            @Nonnull ItemStack itemStack,
            @Nonnull String slotId
    ) {
        HyforgedItemData itemData = HyforgedItemDataService.read(itemStack);
        if (!itemData.hasAffixes()) {
            return List.of();
        }
        
        List<StatModifier> applied = new ArrayList<>();
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        
        for (RolledAffix affix : itemData.affixes()) {
            String sourceId = EQUIPMENT_SOURCE_PREFIX + slotId + ":" + affix.affixId();
            
            // Resolve stat index
            StatId statId = affix.statId();
            int statIndex = registry.getIndex(statId);
            if (statIndex < 0) {
                LOGGER.log(Level.WARNING, "Unknown stat for affix {0}: {1}", 
                        new Object[]{affix.affixId(), statId});
                continue;
            }
            
            // Convert affix modifier type to StatModifier.ModifierType
            ModifierType modifierType = convertModifierType(affix.modifierType());
            
            // Create the stat modifier
            StatModifier modifier = new StatModifier(
                    sourceId,
                    ModifierSource.EQUIPMENT,
                    modifierType,
                    statIndex,
                    StatModifier.NO_TAG,
                    affix.value(),
                    0, // Permanent (equipment duration)
                    0  // Default priority
            );
            
            boolean added = statComponent.addModifier(modifier);
            if (added) {
                applied.add(modifier);
                LOGGER.log(Level.FINE, "Applied affix modifier: {0} = {1} {2}", 
                        new Object[]{statId, affix.value(), modifierType});
            }
        }
        
        return applied;
    }
    
    /**
     * Convert HyforgedModifier.StackType to StatModifier.ModifierType.
     */
    private ModifierType convertModifierType(@Nonnull HyforgedModifier.StackType stackType) {
        return switch (stackType) {
            case FLAT -> ModifierType.FLAT;
            case INCREASED -> ModifierType.INCREASED;
            case MORE -> ModifierType.MORE;
            case CAP -> ModifierType.CAP;
        };
    }
    
    /**
     * Emit an event after modifiers are applied.
     */
    private void emitModifiersAppliedEvent(
            @Nonnull LivingEntity entity,
            @Nonnull String slotType,
            @Nonnull List<StatModifier> modifiers
    ) {
        try {
            IEventDispatcher<AffixModifiersAppliedEvent, AffixModifiersAppliedEvent> dispatcher =
                    HytaleServer.get().getEventBus().dispatchFor(AffixModifiersAppliedEvent.class);
            
            dispatcher.dispatch(new AffixModifiersAppliedEvent(entity, slotType, modifiers));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to emit AffixModifiersAppliedEvent", e);
        }
    }
}
