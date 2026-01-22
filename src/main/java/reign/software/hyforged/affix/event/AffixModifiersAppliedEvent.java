package reign.software.hyforged.affix.event;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * Event fired after affix stat modifiers are applied or removed from an entity.
 * <p>
 * This event is emitted by {@link reign.software.hyforged.affix.system.EquipmentAffixListener}
 * when equipment changes trigger modifier updates. Other systems can subscribe
 * to react to equipment-based stat changes.
 * <p>
 * Usage:
 * <pre>
 * eventBus.registerGlobal((short) 0, AffixModifiersAppliedEvent.class, event -> {
 *     LivingEntity entity = event.getEntity();
 *     String slotType = event.getSlotType(); // "armor" or "hand"
 *     List&lt;HyforgedModifier&gt; modifiers = event.getModifiers();
 *     // React to stat changes...
 * });
 * </pre>
 * <p>
 * Implements {@link IEvent} with {@code Void} key type for global (non-keyed) dispatch.
 */
public class AffixModifiersAppliedEvent implements IEvent<Void> {
    
    private final LivingEntity entity;
    private final String slotType;
    private final List<HyforgedModifier> modifiers;
    
    /**
     * Create a new AffixModifiersAppliedEvent.
     *
     * @param entity    The entity whose modifiers changed
     * @param slotType  The equipment slot type ("armor" or "hand")
     * @param modifiers The modifiers that were applied
     */
    public AffixModifiersAppliedEvent(
            @Nonnull LivingEntity entity,
            @Nonnull String slotType,
            @Nonnull List<HyforgedModifier> modifiers
    ) {
        this.entity = Objects.requireNonNull(entity, "entity cannot be null");
        this.slotType = Objects.requireNonNull(slotType, "slotType cannot be null");
        this.modifiers = List.copyOf(Objects.requireNonNull(modifiers, "modifiers cannot be null"));
    }
    
    /**
     * Get the entity whose modifiers changed.
     */
    @Nonnull
    public LivingEntity getEntity() {
        return entity;
    }
    
    /**
     * Get the equipment slot type that triggered the change.
     *
     * @return "armor" for armor slots, "hand" for held item
     */
    @Nonnull
    public String getSlotType() {
        return slotType;
    }
    
    /**
     * Get the modifiers that were applied.
     *
     * @return Immutable list of applied modifiers
     */
    @Nonnull
    public List<HyforgedModifier> getModifiers() {
        return modifiers;
    }
    
    /**
     * Check if this event has any modifiers.
     */
    public boolean hasModifiers() {
        return !modifiers.isEmpty();
    }
    
    /**
     * Get the count of applied modifiers.
     */
    public int getModifierCount() {
        return modifiers.size();
    }
    
    @Override
    public String toString() {
        return "AffixModifiersAppliedEvent{" +
                "entity=" + entity +
                ", slotType='" + slotType + '\'' +
                ", modifierCount=" + modifiers.size() +
                '}';
    }
}
