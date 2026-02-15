package reign.software.hyforged.stats;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.stats.modifier.HyforgedModifier;
import reign.software.hyforged.stats.value.HyforgedStatValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Static utility for accessing stats from EntityStatMap.
 * <p>
 * This utility provides a unified interface for reading stat values and
 * managing modifiers through Hytale's EntityStatMap, which now contains
 * HyforgedStatValue instances with ARPG stacking.
 * <p>
 * Use this class instead of HyforgedStatComponent.getCachedValue() for
 * reading stat values.
 * 
 * @see HyforgedStatValue for ARPG stacking implementation
 */
public final class StatAccessor {
    
    /** Lazily initialized to avoid static initialization issues in tests */
    private static volatile ComponentType<EntityStore, EntityStatMap> statMapTypeCache = null;
    
    private StatAccessor() {
        // Utility class
    }
    
    /**
     * Get the EntityStatMap component type (lazy initialization).
     */
    private static ComponentType<EntityStore, EntityStatMap> getStatMapTypeInternal() {
        if (statMapTypeCache == null) {
            synchronized (StatAccessor.class) {
                if (statMapTypeCache == null) {
                    statMapTypeCache = EntityStatMap.getComponentType();
                }
            }
        }
        return statMapTypeCache;
    }
    
    // ========== STAT VALUE READING ==========
    
    /**
     * Get the computed stat value for an entity by stat index.
     * <p>
     * This reads directly from EntityStatMap, which contains HyforgedStatValue
     * instances after entity initialization.
     *
     * @param store The entity store
     * @param entityRef The entity reference
     * @param statIndex The stat index to read
     * @return The computed stat value, or 0 if not available
     */
    public static float getStatValue(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            int statIndex) {
        EntityStatMap statMap = store.getComponent(entityRef, getStatMapTypeInternal());
        if (statMap == null) {
            return 0f;
        }
        EntityStatValue value = statMap.get(statIndex);
        return value != null ? value.get() : 0f;
    }
    
    /**
     * Get the computed stat value for an entity from a chunk by stat index.
     *
     * @param chunk The archetype chunk containing the entity
     * @param index The entity's index within the chunk
     * @param statIndex The stat index to read
     * @return The computed stat value, or 0 if not available
     */
    public static float getStatValue(
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            int index,
            int statIndex) {
        EntityStatMap statMap = chunk.getComponent(index, getStatMapTypeInternal());
        if (statMap == null) {
            return 0f;
        }
        EntityStatValue value = statMap.get(statIndex);
        return value != null ? value.get() : 0f;
    }
    
    /**
     * Get the computed stat value as an integer.
     *
     * @param store The entity store
     * @param entityRef The entity reference
     * @param statIndex The stat index to read
     * @return The computed stat value as int, or 0 if not available
     */
    public static int getStatValueInt(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            int statIndex) {
        return (int) getStatValue(store, entityRef, statIndex);
    }
    
    /**
     * Get the computed stat value as an integer from a chunk.
     *
     * @param chunk The archetype chunk containing the entity
     * @param index The entity's index within the chunk
     * @param statIndex The stat index to read
     * @return The computed stat value as int, or 0 if not available
     */
    public static int getStatValueInt(
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            int index,
            int statIndex) {
        return (int) getStatValue(chunk, index, statIndex);
    }
    
    /**
     * Get the computed stat value directly from an EntityStatMap.
     *
     * @param statMap The entity stat map
     * @param statIndex The stat index to read
     * @return The computed stat value, or 0 if not available
     */
    public static float getStatValue(
            @Nonnull EntityStatMap statMap,
            int statIndex) {
        EntityStatValue value = statMap.get(statIndex);
        return value != null ? value.get() : 0f;
    }
    
    /**
     * Get the computed stat value as an integer directly from an EntityStatMap.
     *
     * @param statMap The entity stat map
     * @param statIndex The stat index to read
     * @return The computed stat value as int, or 0 if not available
     */
    public static int getStatValueInt(
            @Nonnull EntityStatMap statMap,
            int statIndex) {
        return (int) getStatValue(statMap, statIndex);
    }

    /**
     * Ensure a stat slot exists in EntityStatMap before mutating it.
     * <p>
     * Some entities can temporarily carry stale stat arrays (e.g. before map refresh).
     * This helper refreshes the map once and re-checks the slot.
     *
     * @param statMap The entity stat map
     * @param statIndex The stat index
     * @return true if the slot exists and can be safely mutated
     */
    public static boolean hasStatSlot(
            @Nonnull EntityStatMap statMap,
            int statIndex
    ) {
        if (statIndex < 0) {
            return false;
        }
        if (statMap.get(statIndex) != null) {
            return true;
        }
        statMap.update();
        return statMap.get(statIndex) != null;
    }
    
    // ========== HYFORGED STAT VALUE ACCESS ==========
    
    /**
     * Get the HyforgedStatValue for a stat index, if available.
     * <p>
     * This can be used to access ARPG-specific features like change listeners.
     *
     * @param store The entity store
     * @param entityRef The entity reference
     * @param statIndex The stat index
     * @return The HyforgedStatValue, or null if not available or not a HyforgedStatValue
     */
    @Nullable
    public static HyforgedStatValue getHyforgedStatValue(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            int statIndex) {
        EntityStatMap statMap = store.getComponent(entityRef, getStatMapTypeInternal());
        if (statMap == null) {
            return null;
        }
        EntityStatValue value = statMap.get(statIndex);
        return value instanceof HyforgedStatValue hsv ? hsv : null;
    }
    
    // ========== MODIFIER MANAGEMENT ==========
    
    /**
     * Add or replace a modifier on an entity's stat.
     * <p>
     * This delegates to EntityStatMap.putModifier(), ensuring proper
     * integration with Hytale's stat system.
     *
     * @param store The entity store
     * @param entityRef The entity reference
     * @param statIndex The stat index to modify
     * @param key The unique modifier key (e.g., "hyforged:equipment:sword_1")
     * @param modifier The modifier to add
     * @return The previous modifier with this key, or null
     */
    @Nullable
    public static HyforgedModifier putModifier(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            int statIndex,
            @Nonnull String key,
            @Nonnull HyforgedModifier modifier) {
        EntityStatMap statMap = store.getComponent(entityRef, getStatMapTypeInternal());
        if (statMap == null) {
            return null;
        }
        if (!hasStatSlot(statMap, statIndex)) {
            return null;
        }
        var previous = statMap.putModifier(statIndex, key, modifier);
        return previous instanceof HyforgedModifier hm ? hm : null;
    }
    
    /**
     * Remove a modifier from an entity's stat.
     *
     * @param store The entity store
     * @param entityRef The entity reference
     * @param statIndex The stat index
     * @param key The modifier key to remove
     * @return The removed modifier, or null if not found
     */
    @Nullable
    public static HyforgedModifier removeModifier(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            int statIndex,
            @Nonnull String key) {
        EntityStatMap statMap = store.getComponent(entityRef, getStatMapTypeInternal());
        if (statMap == null) {
            return null;
        }
        if (!hasStatSlot(statMap, statIndex)) {
            return null;
        }
        var removed = statMap.removeModifier(statIndex, key);
        return removed instanceof HyforgedModifier hm ? hm : null;
    }
    
    /**
     * Get a modifier from an entity's stat.
     *
     * @param store The entity store
     * @param entityRef The entity reference
     * @param statIndex The stat index
     * @param key The modifier key
     * @return The modifier, or null if not found
     */
    @Nullable
    public static HyforgedModifier getModifier(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            int statIndex,
            @Nonnull String key) {
        EntityStatMap statMap = store.getComponent(entityRef, getStatMapTypeInternal());
        if (statMap == null) {
            return null;
        }
        var modifier = statMap.getModifier(statIndex, key);
        return modifier instanceof HyforgedModifier hm ? hm : null;
    }
    
    // ========== ENTITY STAT MAP ACCESS ==========
    
    /**
     * Get the EntityStatMap for an entity.
     *
     * @param store The entity store
     * @param entityRef The entity reference
     * @return The EntityStatMap, or null if entity doesn't have one
     */
    @Nullable
    public static EntityStatMap getStatMap(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef) {
        return store.getComponent(entityRef, getStatMapTypeInternal());
    }
    
    /**
     * Get the EntityStatMap component type.
     * <p>
     * Useful for ECS queries.
     */
    @Nonnull
    public static ComponentType<EntityStore, EntityStatMap> getStatMapType() {
        return getStatMapTypeInternal();
    }

    // ========== MODIFIER QUERIES ==========

    /**
     * Collect all Hyforged modifiers from an EntityStatMap.
     *
     * @param statMap The entity stat map
     * @return List of Hyforged modifiers (empty if none)
     */
    @Nonnull
    public static List<HyforgedModifier> getAllHyforgedModifiers(@Nullable EntityStatMap statMap) {
        if (statMap == null) {
            return List.of();
        }

        List<HyforgedModifier> result = new ArrayList<>();
        for (int i = 0; i < statMap.size(); i++) {
            EntityStatValue value = statMap.get(i);
            if (value == null) {
                continue;
            }

            Map<String, Modifier> modifiers = value.getModifiers();
            if (modifiers == null || modifiers.isEmpty()) {
                continue;
            }

            for (Modifier modifier : modifiers.values()) {
                if (modifier instanceof HyforgedModifier hm) {
                    result.add(hm);
                }
            }
        }
        return result;
    }

    /**
     * Collect all Hyforged modifiers for an entity.
     *
     * @param store The entity store
     * @param entityRef The entity reference
     * @return List of Hyforged modifiers (empty if none)
     */
    @Nonnull
    public static List<HyforgedModifier> getAllHyforgedModifiers(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef) {
        return getAllHyforgedModifiers(getStatMap(store, entityRef));
    }
    
    // ========== BULK MODIFIER OPERATIONS ==========
    
    /**
     * Remove all modifiers from a stat whose keys match a prefix.
     * <p>
     * This is useful for removing all modifiers from a source, such as
     * all equipment modifiers when equipment changes.
     *
     * @param statMap The entity stat map
     * @param statIndex The stat index to remove modifiers from
     * @param keyPrefix The prefix to match (e.g., "equipment:armor:")
     * @return The number of modifiers removed
     */
    public static int removeModifiersByKeyPrefix(
            @Nonnull EntityStatMap statMap,
            int statIndex,
            @Nonnull String keyPrefix) {
        EntityStatValue value = statMap.get(statIndex);
        if (value == null || value.getModifiers() == null) {
            return 0;
        }
        
        // Collect keys to remove (can't modify while iterating)
        List<String> keysToRemove = new ArrayList<>();
        for (String key : value.getModifiers().keySet()) {
            if (key.startsWith(keyPrefix)) {
                keysToRemove.add(key);
            }
        }
        
        // Remove collected keys
        for (String key : keysToRemove) {
            statMap.removeModifier(statIndex, key);
        }
        
        return keysToRemove.size();
    }
    
    /**
     * Remove all modifiers from ALL stats whose keys match a prefix.
     * <p>
     * Iterates over all stats and removes matching modifiers.
     *
     * @param statMap The entity stat map
     * @param keyPrefix The prefix to match (e.g., "equipment:armor:")
     * @return The number of modifiers removed
     */
    public static int removeAllModifiersByKeyPrefix(
            @Nonnull EntityStatMap statMap,
            @Nonnull String keyPrefix) {
        int totalRemoved = 0;
        int statCount = StatDefinitionRegistry.get().getStatCount();
        
        for (int statIndex = 0; statIndex < statCount; statIndex++) {
            totalRemoved += removeModifiersByKeyPrefix(statMap, statIndex, keyPrefix);
        }
        
        return totalRemoved;
    }
}
