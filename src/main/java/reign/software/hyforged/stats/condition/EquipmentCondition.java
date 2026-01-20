package reign.software.hyforged.stats.condition;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Condition that checks equipped weapon or item types.
 * <p>
 * Uses String-based weapon type IDs for moddability. Convention: use namespaced IDs
 * like "hyforged:sword" or "mymod:laser_rifle".
 * <p>
 * Examples:
 * - "while wielding a sword" → EquipmentCondition.wielding(WeaponTypes.SWORD)
 * - "while shield equipped" → EquipmentCondition.withShield()
 * - "while wielding custom weapon" → EquipmentCondition.wielding("mymod:laser_rifle")
 */
public class EquipmentCondition implements ModifierCondition {

    @Nonnull
    private final Set<String> requiredWeaponTypeIds;
    private final boolean requireShield;
    private final boolean anyMatch;

    /**
     * Create an equipment condition.
     *
     * @param requiredWeaponTypeIds Set of weapon type IDs to check for (e.g., "hyforged:sword")
     * @param requireShield Whether a shield must be equipped
     * @param anyMatch If true, any of the weapon types matches;
     *                 if false, all weapon types must be equipped (rare)
     */
    public EquipmentCondition(
            @Nonnull Set<String> requiredWeaponTypeIds,
            boolean requireShield,
            boolean anyMatch
    ) {
        this.requiredWeaponTypeIds = requiredWeaponTypeIds.isEmpty() 
            ? Collections.emptySet()
            : Set.copyOf(requiredWeaponTypeIds);
        this.requireShield = requireShield;
        this.anyMatch = anyMatch;
    }

    /**
     * Create a condition for wielding any of the specified weapon types.
     *
     * @param weaponTypeIds The weapon type IDs to check for
     * @return A new EquipmentCondition
     */
    public static EquipmentCondition wielding(@Nonnull String... weaponTypeIds) {
        Set<String> types = new HashSet<>();
        Collections.addAll(types, weaponTypeIds);
        return new EquipmentCondition(types, false, true);
    }

    /**
     * Create a condition for wielding a specific weapon type.
     *
     * @param weaponTypeId The required weapon type ID (e.g., "hyforged:sword")
     * @return A new EquipmentCondition
     */
    public static EquipmentCondition wielding(@Nonnull String weaponTypeId) {
        return new EquipmentCondition(Set.of(weaponTypeId), false, true);
    }

    /**
     * Create a condition for having a shield equipped.
     *
     * @return A new EquipmentCondition
     */
    public static EquipmentCondition withShield() {
        return new EquipmentCondition(Collections.emptySet(), true, true);
    }

    /**
     * Create a condition for wielding a specific weapon type AND having a shield.
     *
     * @param weaponTypeId The required weapon type ID (e.g., \"hyforged:sword\")
     * @return A new EquipmentCondition
     */
    public static EquipmentCondition swordAndBoard(@Nonnull String weaponTypeId) {
        return new EquipmentCondition(Set.of(weaponTypeId), true, true);
    }

    @Override
    public boolean evaluate(@Nonnull Ref<EntityStore> entityRef, @Nonnull QueryContext context) {
        // Check shield requirement first
        if (requireShield && !context.hasShieldEquipped()) {
            return false;
        }
        
        // If no weapon type requirements, just the shield check mattered
        if (requiredWeaponTypeIds.isEmpty()) {
            return !requireShield || context.hasShieldEquipped();
        }
        
        // Check weapon types
        if (anyMatch) {
            // Any of the required weapon types must be equipped
            for (String weaponTypeId : requiredWeaponTypeIds) {
                if (context.hasWeaponType(weaponTypeId)) {
                    return true;
                }
            }
            return false;
        } else {
            // All required weapon types must be equipped (unusual but supported)
            for (String weaponTypeId : requiredWeaponTypeIds) {
                if (!context.hasWeaponType(weaponTypeId)) {
                    return false;
                }
            }
            return true;
        }
    }

    @Nonnull
    public Set<String> getRequiredWeaponTypeIds() {
        return Set.copyOf(requiredWeaponTypeIds);
    }

    public boolean isRequireShield() {
        return requireShield;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("EquipmentCondition[");
        if (!requiredWeaponTypeIds.isEmpty()) {
            sb.append("wielding ");
            sb.append(anyMatch ? "any of " : "all of ");
            sb.append(requiredWeaponTypeIds);
        }
        if (requireShield) {
            if (!requiredWeaponTypeIds.isEmpty()) {
                sb.append(" and ");
            }
            sb.append("with shield");
        }
        sb.append("]");
        return sb.toString();
    }
}
