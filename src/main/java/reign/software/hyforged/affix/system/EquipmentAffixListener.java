package reign.software.hyforged.affix.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.event.AffixModifiersAppliedEvent;
import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.service.ActiveEffectInitializer;
import reign.software.hyforged.affix.service.HyforgedItemDataService;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

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
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    /** Prefix for equipment-sourced modifier IDs */
    public static final String EQUIPMENT_SOURCE_PREFIX = "equipment:";
    
    private EventRegistration<String, LivingEntityInventoryChangeEvent> globalRegistration;
    
    /**
     * Start listening for inventory change events.
     */
    public void register() {
        globalRegistration = HytaleServer.get().getEventBus()
                .registerGlobal((short) 0, LivingEntityInventoryChangeEvent.class, this::onInventoryChange);
        
        LOGGER.atInfo().log("EquipmentAffixListener registered for inventory changes");
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
        
        // Get the entity's EntityStatMap
        Ref<EntityStore> entityRef = entity.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }
        
        EntityStatMap entityStatMap = getEntityStatMap(entityRef);
        HyforgedStatComponent statComponent = getHyforgedStatComponent(entityRef);
        if (entityStatMap == null && statComponent == null) {
            return;
        }
        
        // Process the equipment change
        if (isArmorChange) {
            processArmorChange(entity, entityStatMap, statComponent, inventory);
        } else if (isHotbarChange) {
            processHotbarChange(entity, entityStatMap, statComponent, inventory);
        }

        ActiveEffectInitializer.refreshFromEquipment(entityRef, inventory, entityRef.getStore());
    }
    
    /**
     * Get the EntityStatMap for an entity.
     */
    @Nullable
    private EntityStatMap getEntityStatMap(@Nonnull Ref<EntityStore> ref) {
        if (!ref.isValid()) {
            return null;
        }
        return StatAccessor.getStatMap(ref.getStore(), ref);
    }

    @Nullable
    private HyforgedStatComponent getHyforgedStatComponent(@Nonnull Ref<EntityStore> ref) {
        if (!ref.isValid()) {
            return null;
        }
        return ref.getStore().getComponent(ref, HyforgedPlugin.getInstance().getHyforgedStatComponentType());
    }
    
    /**
     * Process armor slot changes.
     */
    private void processArmorChange(
            @Nonnull LivingEntity entity,
            @Nullable EntityStatMap entityStatMap,
            @Nullable HyforgedStatComponent statComponent,
            @Nonnull Inventory inventory
    ) {
        ItemContainer armorContainer = inventory.getArmor();
        
        // Re-sync all armor slot modifiers
        // First, remove all existing armor affix modifiers
        removeArmorAffixModifiers(entityStatMap, statComponent);
        
        // Then apply modifiers from current armor
        List<HyforgedModifier> appliedModifiers = new ArrayList<>();
        
        for (short slot = 0; slot < armorContainer.getCapacity(); slot++) {
            ItemStack itemStack = armorContainer.getItemStack(slot);
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            
            List<HyforgedModifier> slotModifiers = applyAffixModifiersFromItem(
                    entityStatMap, statComponent, itemStack, "armor:" + slot);
            appliedModifiers.addAll(slotModifiers);
        }
        
        // Always mark dirty after equipment change so cached values are recomputed,
        // even if there were no prior modifiers to remove (first equip scenario)
        if (statComponent != null && !appliedModifiers.isEmpty()) {
            statComponent.markAllDirty();
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
            @Nullable EntityStatMap entityStatMap,
            @Nullable HyforgedStatComponent statComponent,
            @Nonnull Inventory inventory
    ) {
        // Remove existing hand affix modifiers
        removeHandAffixModifiers(entityStatMap, statComponent);
        
        // Apply modifiers from held item
        ItemStack heldItem = inventory.getItemInHand();
        if (heldItem == null || heldItem.isEmpty()) {
            return;
        }
        
        List<HyforgedModifier> appliedModifiers = applyAffixModifiersFromItem(
            entityStatMap, statComponent, heldItem, "hand");
        
        // Always mark dirty after equipment change so cached values are recomputed,
        // even if there were no prior modifiers to remove (first equip scenario)
        if (statComponent != null && !appliedModifiers.isEmpty()) {
            statComponent.markAllDirty();
        }
        
        if (!appliedModifiers.isEmpty()) {
            emitModifiersAppliedEvent(entity, "hand", appliedModifiers);
        }
    }
    
    /**
     * Remove all armor-sourced affix modifiers.
     */
    private void removeArmorAffixModifiers(
            @Nullable EntityStatMap statMap,
            @Nullable HyforgedStatComponent statComponent
    ) {
        int removed = 0;
        if (statMap != null) {
            removed += StatAccessor.removeAllModifiersByKeyPrefix(statMap, EQUIPMENT_SOURCE_PREFIX + "armor:");
        }
        if (statComponent != null) {
            removed += statComponent.removeModifiersIf(
                    modifier -> modifier.getSourceId().startsWith(EQUIPMENT_SOURCE_PREFIX + "armor:"),
                    modifier -> {
                    }
            );
        }
        if (removed > 0) {
            if (statComponent != null) {
                statComponent.markAllDirty();
            }
            LOGGER.at(Level.FINER).log("Removed %s armor affix modifiers", removed);
        }
    }
    
    /**
     * Remove all hand-sourced affix modifiers.
     */
    private void removeHandAffixModifiers(
            @Nullable EntityStatMap statMap,
            @Nullable HyforgedStatComponent statComponent
    ) {
        int removed = 0;
        if (statMap != null) {
            removed += StatAccessor.removeAllModifiersByKeyPrefix(statMap, EQUIPMENT_SOURCE_PREFIX + "hand:");
        }
        if (statComponent != null) {
            removed += statComponent.removeModifiersIf(
                    modifier -> modifier.getSourceId().startsWith(EQUIPMENT_SOURCE_PREFIX + "hand:"),
                    modifier -> {
                    }
            );
        }
        if (removed > 0) {
            if (statComponent != null) {
                statComponent.markAllDirty();
            }
            LOGGER.at(Level.FINER).log("Removed %s hand affix modifiers", removed);
        }
    }
    
    /**
     * Apply affix modifiers from an item to the entity stat map.
     *
     * @param statMap The entity stat map to apply to
     * @param itemStack The item containing affixes
     * @param slotId Slot identifier (e.g., "armor:0", "hand")
     * @return List of applied modifiers
     */
    @Nonnull
    private List<HyforgedModifier> applyAffixModifiersFromItem(
            @Nullable EntityStatMap statMap,
            @Nullable HyforgedStatComponent statComponent,
            @Nonnull ItemStack itemStack,
            @Nonnull String slotId
    ) {
        if (statMap == null && statComponent == null) {
            return List.of();
        }

        HyforgedItemData itemData = HyforgedItemDataService.read(itemStack);
        if (!itemData.hasAffixes()) {
            return List.of();
        }
        
        List<HyforgedModifier> applied = new ArrayList<>();
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        
        for (RolledAffix affix : itemData.affixes()) {
            String affixSourcePrefix = EQUIPMENT_SOURCE_PREFIX + slotId + ":" + affix.affixId();
            
            // Apply each stat in the affix
            for (Map.Entry<String, RolledAffix.RolledStat> entry : affix.rolledStats().entrySet()) {
                String statIdStr = entry.getKey();
                RolledAffix.RolledStat rolledStat = entry.getValue();
                
                // Resolve stat index
                StatId statId = StatId.parse(statIdStr);
                int statIndex = registry.getIndex(statId);
                if (statIndex < 0) {
                    LOGGER.atWarning().log("Unknown stat for affix %s: %s", affix.affixId(), statIdStr);
                    continue;
                }
                
                String sourceId = affixSourcePrefix + ":" + statIdStr;
                
                // Create the HyforgedModifier
                HyforgedModifier modifier = HyforgedModifier.builder()
                        .sourceId(sourceId)
                        .sourceType(HyforgedModifier.SourceType.EQUIPMENT)
                        .stackType(rolledStat.stackType())
                        .amount(rolledStat.value())
                        .targetStat(statIndex)
                        .permanent()
                        .build();
                
                if (statMap != null && StatAccessor.hasStatSlot(statMap, statIndex)) {
                    statMap.putModifier(statIndex, sourceId, modifier);
                    applied.add(modifier);
                    // Mark dirty so HyforgedStatComputeSystem recomputes cached values
                    if (statComponent != null) {
                        statComponent.markStatDirty(statIndex);
                    }
                } else if (statComponent != null) {
                    statComponent.upsertModifier(modifier);
                    applied.add(modifier);
                }

                LOGGER.at(Level.FINE).log("Applied affix modifier: %s = %s %s", statIdStr, rolledStat.value(), rolledStat.stackType());
            }
        }
        
        return applied;
    }
    
    /**
     * Emit an event after modifiers are applied.
     */
    private void emitModifiersAppliedEvent(
            @Nonnull LivingEntity entity,
            @Nonnull String slotType,
            @Nonnull List<HyforgedModifier> modifiers
    ) {
        try {
            IEventDispatcher<AffixModifiersAppliedEvent, AffixModifiersAppliedEvent> dispatcher =
                    HytaleServer.get().getEventBus().dispatchFor(AffixModifiersAppliedEvent.class);
            
            dispatcher.dispatch(new AffixModifiersAppliedEvent(entity, slotType, modifiers));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to emit AffixModifiersAppliedEvent");
        }
    }
}
