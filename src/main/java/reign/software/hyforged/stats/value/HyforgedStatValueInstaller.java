package reign.software.hyforged.stats.value;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ECS System that installs HyforgedStatValue instances into EntityStatMap.
 * <p>
 * This RefSystem runs after entity initialization and swaps Hytale's native
 * EntityStatValue instances with HyforgedStatValue instances that implement
 * ARPG-style modifier stacking.
 * <p>
 * The swap preserves all existing modifiers and triggers recomputation using
 * the ARPG stacking order.
 * <p>
 * This system:
 * <ul>
 *   <li>Listens for entities with both HyforgedStatComponent and EntityStatMap</li>
 *   <li>Swaps EntityStatValue → HyforgedStatValue in the EntityStatMap.values array</li>
 *   <li>Preserves all existing modifiers during the swap</li>
 *   <li>Links base bonuses from HyforgedStatComponent to HyforgedStatValue</li>
 * </ul>
 * 
 * @see HyforgedStatValue for the ARPG stat value implementation
 */
public class HyforgedStatValueInstaller extends RefSystem<EntityStore> {
    
    private static final Logger LOGGER = Logger.getLogger(HyforgedStatValueInstaller.class.getName());
    
    /**
     * Reflection field for accessing EntityStatMap.values array.
     * Cached for performance.
     */
    private static Field valuesField = null;
    
    static {
        try {
            valuesField = EntityStatMap.class.getDeclaredField("values");
            valuesField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            LOGGER.log(Level.SEVERE, "Failed to find EntityStatMap.values field", e);
        }
    }
    
    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapType;
    
    @Nonnull
    private final Query<EntityStore> query;
    
    public HyforgedStatValueInstaller() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.entityStatMapType = EntityStatMap.getComponentType();
        
        // Query for entities that have both HyforgedStatComponent and EntityStatMap
        this.query = Query.and(statComponentType, entityStatMapType);
    }
    
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }
    
    @Override
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        EntityStatMap statMap = commandBuffer.getComponent(ref, entityStatMapType);
        HyforgedStatComponent statComponent = commandBuffer.getComponent(ref, statComponentType);
        
        if (statMap == null || statComponent == null) {
            return;
        }
        
        installHyforgedStatValues(statMap, statComponent);
    }
    
    @Override
    public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No cleanup needed - HyforgedStatValue has no external references
    }
    
    /**
     * Install HyforgedStatValue instances into an EntityStatMap.
     * <p>
     * This method replaces each EntityStatValue in the map with a HyforgedStatValue
     * that preserves all modifiers and supports ARPG stacking.
     * 
     * @param statMap The EntityStatMap to modify
     * @param statComponent The HyforgedStatComponent for base value access
     */
    public void installHyforgedStatValues(
            @Nonnull EntityStatMap statMap,
            @Nonnull HyforgedStatComponent statComponent
    ) {
        if (valuesField == null) {
            LOGGER.warning("Cannot install HyforgedStatValues: reflection field not available");
            return;
        }
        
        try {
            EntityStatValue[] values = (EntityStatValue[]) valuesField.get(statMap);
            if (values == null || values.length == 0) {
                return;
            }
            
            int swapped = 0;
            for (int i = 0; i < values.length; i++) {
                EntityStatValue original = values[i];
                if (original == null) {
                    continue;
                }
                
                // Skip if already a HyforgedStatValue
                if (original instanceof HyforgedStatValue) {
                    continue;
                }
                
                // Create HyforgedStatValue from the original
                HyforgedStatValue hyforgedValue = new HyforgedStatValue(original);
                
                // Link base bonus from HyforgedStatComponent (relative to asset initial value)
                int baseValue = statComponent.getBaseValue(i);
                int baseBonus = baseValue;
                EntityStatType asset = EntityStatType.getAssetMap().getAsset(i);
                if (asset != null) {
                    baseBonus = baseValue - Math.round(asset.getInitialValue());
                }
                hyforgedValue.setHyforgedBaseBonus(baseBonus);
                
                // Replace in the array
                values[i] = hyforgedValue;
                swapped++;
            }
            
            if (swapped > 0) {
                final int swappedCount = swapped;
                LOGGER.fine(() -> "Installed " + swappedCount + " HyforgedStatValues");
            }
            
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "Failed to access EntityStatMap.values", e);
        }
    }
    
    /**
     * Reinstall HyforgedStatValues for an entity.
     * <p>
     * This can be called when EntityStatMap is recreated or needs refresh.
     * 
     * @param statMap The EntityStatMap to modify
     * @param statComponent The HyforgedStatComponent for base value access
     */
    public static void reinstall(
            @Nonnull EntityStatMap statMap,
            @Nonnull HyforgedStatComponent statComponent
    ) {
        if (valuesField == null) {
            return;
        }
        
        try {
            EntityStatValue[] values = (EntityStatValue[]) valuesField.get(statMap);
            if (values == null) {
                return;
            }
            
            for (int i = 0; i < values.length; i++) {
                EntityStatValue original = values[i];
                if (original == null || original instanceof HyforgedStatValue) {
                    continue;
                }
                
                HyforgedStatValue hyforgedValue = new HyforgedStatValue(original);
                int baseValue = statComponent.getBaseValue(i);
                int baseBonus = baseValue;
                EntityStatType asset = EntityStatType.getAssetMap().getAsset(i);
                if (asset != null) {
                    baseBonus = baseValue - Math.round(asset.getInitialValue());
                }
                hyforgedValue.setHyforgedBaseBonus(baseBonus);
                
                values[i] = hyforgedValue;
            }
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "Failed to reinstall HyforgedStatValues", e);
        }
    }
    
    /**
     * Get a HyforgedStatValue from an EntityStatMap.
     * <p>
     * Returns null if the stat at the given index is not a HyforgedStatValue.
     * 
     * @param statMap The EntityStatMap
     * @param index The stat index
     * @return The HyforgedStatValue, or null if not found or not a HyforgedStatValue
     */
    @Nonnull
    public static HyforgedStatValue getOrInstall(
            @Nonnull EntityStatMap statMap,
            int index,
            @Nonnull HyforgedStatComponent statComponent
    ) {
        EntityStatValue value = statMap.get(index);
        
        if (value instanceof HyforgedStatValue hsv) {
            return hsv;
        }
        
        // Need to install - this is a fallback path
        if (value == null) {
            // Trigger update to create the EntityStatValue
            statMap.update();
            value = statMap.get(index);
        }
        
        if (value == null) {
            throw new IllegalStateException("Failed to get EntityStatValue for index " + index);
        }
        
        // Swap single value
        HyforgedStatValue hyforgedValue = new HyforgedStatValue(value);
        int baseValue = statComponent.getBaseValue(index);
        int baseBonus = baseValue;
        EntityStatType asset = EntityStatType.getAssetMap().getAsset(index);
        if (asset != null) {
            baseBonus = baseValue - Math.round(asset.getInitialValue());
        }
        hyforgedValue.setHyforgedBaseBonus(baseBonus);
        
        // Replace via reflection
        if (valuesField != null) {
            try {
                EntityStatValue[] values = (EntityStatValue[]) valuesField.get(statMap);
                if (values != null && index < values.length) {
                    values[index] = hyforgedValue;
                }
            } catch (IllegalAccessException e) {
                LOGGER.log(Level.WARNING, "Failed to swap single HyforgedStatValue", e);
            }
        }
        
        return hyforgedValue;
    }
    
    /**
     * Check if all EntityStatValues in the map have been swapped to HyforgedStatValue.
     * 
     * @param statMap The EntityStatMap to check
     * @return true if all values are HyforgedStatValue instances
     */
    public static boolean isFullyInstalled(@Nonnull EntityStatMap statMap) {
        if (valuesField == null) {
            return false;
        }
        
        try {
            EntityStatValue[] values = (EntityStatValue[]) valuesField.get(statMap);
            if (values == null || values.length == 0) {
                return true;
            }
            
            for (EntityStatValue value : values) {
                if (value != null && !(value instanceof HyforgedStatValue)) {
                    return false;
                }
            }
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }
}
