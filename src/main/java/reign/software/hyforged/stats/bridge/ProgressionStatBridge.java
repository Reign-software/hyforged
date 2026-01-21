package reign.software.hyforged.stats.bridge;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.stats.engine.RatingConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Bridge class connecting the progression system to the stats system.
 * <p>
 * Provides utility methods to access progression data for combat calculations
 * and stat effectiveness computations.
 * <p>
 * Usage in combat systems:
 * <pre>
 * int defenderLevel = ProgressionStatBridge.getCharacterLevel(defenderRef, store);
 * int damageReduction = RatingConverter.armorToReduction(armorRating, attackerLevel);
 * </pre>
 */
public final class ProgressionStatBridge {
    
    private ProgressionStatBridge() {} // Static utility class
    
    /**
     * Default character level when progression component is not available.
     */
    public static final int DEFAULT_CHARACTER_LEVEL = 1;
    
    /**
     * Get the character level for an entity.
     * <p>
     * Returns the character level from the ProgressionComponent if present,
     * otherwise returns {@link #DEFAULT_CHARACTER_LEVEL}.
     *
     * @param entityRef Reference to the entity
     * @param store The entity store
     * @return The entity's character level (1-100), or default if not found
     */
    public static int getCharacterLevel(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        if (plugin == null) {
            return DEFAULT_CHARACTER_LEVEL;
        }
        
        ProgressionComponent progression = store.getComponent(entityRef, plugin.getProgressionComponentType());
        if (progression == null) {
            return DEFAULT_CHARACTER_LEVEL;
        }
        
        return progression.getCharacterLevel();
    }
    
    /**
     * Get the character level for an entity using a command buffer.
     * <p>
     * Prefer this version when called from within system handlers that receive
     * a CommandBuffer for thread-safe component access.
     *
     * @param entityRef Reference to the entity
     * @param commandBuffer The command buffer for component access
     * @return The entity's character level (1-100), or default if not found
     */
    public static int getCharacterLevel(
            @Nonnull Ref<EntityStore> entityRef, 
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        if (plugin == null) {
            return DEFAULT_CHARACTER_LEVEL;
        }
        
        ProgressionComponent progression = commandBuffer.getComponent(entityRef, plugin.getProgressionComponentType());
        if (progression == null) {
            return DEFAULT_CHARACTER_LEVEL;
        }
        
        return progression.getCharacterLevel();
    }
    
    /**
     * Get the character level for an entity using a component accessor.
     * <p>
     * Use this version when you have a ComponentAccessor from an ECS system.
     *
     * @param entityRef Reference to the entity
     * @param accessor The component accessor for component access
     * @return The entity's character level (1-100), or default if not found
     */
    public static int getCharacterLevel(
            @Nonnull Ref<EntityStore> entityRef, 
            @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        if (plugin == null) {
            return DEFAULT_CHARACTER_LEVEL;
        }
        
        ProgressionComponent progression = accessor.getComponent(entityRef, plugin.getProgressionComponentType());
        if (progression == null) {
            return DEFAULT_CHARACTER_LEVEL;
        }
        
        return progression.getCharacterLevel();
    }
    
    /**
     * Get the active class ID for an entity.
     * <p>
     * Returns the active class from the ProgressionComponent if present,
     * otherwise returns null.
     *
     * @param entityRef Reference to the entity
     * @param store The entity store
     * @return The entity's active class ID, or null if not set
     */
    @Nullable
    public static String getActiveClassId(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        if (plugin == null) {
            return null;
        }
        
        ProgressionComponent progression = store.getComponent(entityRef, plugin.getProgressionComponentType());
        if (progression == null) {
            return null;
        }
        
        return progression.getActiveClassId();
    }
    
    /**
     * Get the class level for an entity's active class.
     * <p>
     * Returns 0 if the entity has no active class or no class progression.
     *
     * @param entityRef Reference to the entity
     * @param store The entity store
     * @return The entity's class level (0-50), or 0 if not found
     */
    public static int getActiveClassLevel(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        if (plugin == null) {
            return 0;
        }
        
        ProgressionComponent progression = store.getComponent(entityRef, plugin.getProgressionComponentType());
        if (progression == null) {
            return 0;
        }
        
        String activeClassId = progression.getActiveClassId();
        if (activeClassId == null) {
            return 0;
        }
        
        var classProgression = progression.getClassProgression(activeClassId);
        return classProgression != null ? classProgression.level : 0;
    }
    
    /**
     * Calculate armor damage reduction for a defender against an attacker.
     * <p>
     * Convenience method that looks up the attacker's level and uses it
     * for the diminishing returns calculation.
     *
     * @param armorRating The defender's armor rating
     * @param attackerRef Reference to the attacking entity
     * @param store The entity store
     * @return Physical damage reduction in basis points (10000 = 100%)
     */
    public static int calculateArmorReduction(
            int armorRating,
            @Nonnull Ref<EntityStore> attackerRef,
            @Nonnull Store<EntityStore> store
    ) {
        int attackerLevel = getCharacterLevel(attackerRef, store);
        return RatingConverter.armorToReduction(armorRating, attackerLevel);
    }
    
    /**
     * Calculate evasion chance for a defender against an attacker.
     * <p>
     * Convenience method that looks up the attacker's level and uses it
     * for the diminishing returns calculation.
     *
     * @param evasionRating The defender's evasion rating
     * @param attackerRef Reference to the attacking entity
     * @param store The entity store
     * @return Evasion chance in basis points (10000 = 100%)
     */
    public static int calculateEvasionChance(
            int evasionRating,
            @Nonnull Ref<EntityStore> attackerRef,
            @Nonnull Store<EntityStore> store
    ) {
        int attackerLevel = getCharacterLevel(attackerRef, store);
        return RatingConverter.evasionToChance(evasionRating, attackerLevel);
    }
    
    /**
     * Calculate resistance damage reduction for a defender against an attacker.
     * <p>
     * Convenience method that looks up the attacker's level and uses it
     * for the diminishing returns calculation.
     *
     * @param resistRating The defender's resistance rating
     * @param attackerRef Reference to the attacking entity
     * @param store The entity store
     * @return Elemental damage reduction in basis points (10000 = 100%)
     */
    public static int calculateResistanceReduction(
            int resistRating,
            @Nonnull Ref<EntityStore> attackerRef,
            @Nonnull Store<EntityStore> store
    ) {
        int attackerLevel = getCharacterLevel(attackerRef, store);
        return RatingConverter.resistanceToReduction(resistRating, attackerLevel);
    }
    
    /**
     * Calculate hit chance for an attacker against a defender.
     * <p>
     * Convenience method that looks up the defender's level and uses it
     * for the diminishing returns calculation.
     *
     * @param accuracyRating The attacker's accuracy rating
     * @param defenderRef Reference to the defending entity
     * @param store The entity store
     * @return Hit chance in basis points (10000 = 100%)
     */
    public static int calculateHitChance(
            int accuracyRating,
            @Nonnull Ref<EntityStore> defenderRef,
            @Nonnull Store<EntityStore> store
    ) {
        int defenderLevel = getCharacterLevel(defenderRef, store);
        return RatingConverter.accuracyToHitChance(accuracyRating, defenderLevel);
    }
}
