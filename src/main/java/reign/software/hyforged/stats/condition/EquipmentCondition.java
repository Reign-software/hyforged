package reign.software.hyforged.stats.condition;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Condition that checks equipped weapon or item types.
 * <p>
 * Examples:
 * - "while wielding a sword"
 * - "while shield equipped"
 * - "while dual wielding"
 */
public class EquipmentCondition implements ModifierCondition {

    @Nonnull
    private final Set<QueryContext.WeaponType> requiredWeaponTypes;
    private final boolean requireShield;
    private final boolean anyMatch;

    /**
     * Create an equipment condition.
     *
     * @param requiredWeaponTypes Set of weapon types to check for
     * @param requireShield Whether a shield must be equipped
     * @param anyMatch If true, any of the weapon types matches;
     *                 if false, all weapon types must be equipped (rare)
     */
    public EquipmentCondition(
            @Nonnull Set<QueryContext.WeaponType> requiredWeaponTypes,
            boolean requireShield,
            boolean anyMatch
    ) {
        this.requiredWeaponTypes = requiredWeaponTypes.isEmpty() 
            ? EnumSet.noneOf(QueryContext.WeaponType.class)
            : EnumSet.copyOf(requiredWeaponTypes);
        this.requireShield = requireShield;
        this.anyMatch = anyMatch;
    }

    /**
     * Create a condition for wielding any of the specified weapon types.
     *
     * @param weaponTypes The weapon types to check for
     * @return A new EquipmentCondition
     */
    public static EquipmentCondition wielding(@Nonnull QueryContext.WeaponType... weaponTypes) {
        return new EquipmentCondition(EnumSet.of(weaponTypes[0], weaponTypes), false, true);
    }

    /**
     * Create a condition for wielding a specific weapon type.
     *
     * @param weaponType The required weapon type
     * @return A new EquipmentCondition
     */
    public static EquipmentCondition wielding(@Nonnull QueryContext.WeaponType weaponType) {
        return new EquipmentCondition(EnumSet.of(weaponType), false, true);
    }

    /**
     * Create a condition for having a shield equipped.
     *
     * @return A new EquipmentCondition
     */
    public static EquipmentCondition withShield() {
        return new EquipmentCondition(EnumSet.noneOf(QueryContext.WeaponType.class), true, true);
    }

    /**
     * Create a condition for wielding a specific weapon type AND having a shield.
     *
     * @param weaponType The required weapon type
     * @return A new EquipmentCondition
     */
    public static EquipmentCondition swordAndBoard(@Nonnull QueryContext.WeaponType weaponType) {
        return new EquipmentCondition(EnumSet.of(weaponType), true, true);
    }

    @Override
    public boolean evaluate(@Nonnull Ref<EntityStore> entityRef, @Nonnull QueryContext context) {
        // Check shield requirement first
        if (requireShield && !context.hasShieldEquipped()) {
            return false;
        }
        
        // If no weapon type requirements, just the shield check mattered
        if (requiredWeaponTypes.isEmpty()) {
            return !requireShield || context.hasShieldEquipped();
        }
        
        // Check weapon types
        if (anyMatch) {
            // Any of the required weapon types must be equipped
            for (QueryContext.WeaponType weaponType : requiredWeaponTypes) {
                if (context.hasWeaponType(weaponType)) {
                    return true;
                }
            }
            return false;
        } else {
            // All required weapon types must be equipped (unusual but supported)
            for (QueryContext.WeaponType weaponType : requiredWeaponTypes) {
                if (!context.hasWeaponType(weaponType)) {
                    return false;
                }
            }
            return true;
        }
    }

    @Nonnull
    public Set<QueryContext.WeaponType> getRequiredWeaponTypes() {
        return EnumSet.copyOf(requiredWeaponTypes);
    }

    public boolean isRequireShield() {
        return requireShield;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("EquipmentCondition[");
        if (!requiredWeaponTypes.isEmpty()) {
            sb.append("wielding ");
            sb.append(anyMatch ? "any of " : "all of ");
            sb.append(requiredWeaponTypes);
        }
        if (requireShield) {
            if (!requiredWeaponTypes.isEmpty()) {
                sb.append(" and ");
            }
            sb.append("with shield");
        }
        sb.append("]");
        return sb.toString();
    }
}
