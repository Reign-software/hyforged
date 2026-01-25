package reign.software.hyforged.passive.effect;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.passive.model.PassiveNode;
import reign.software.hyforged.passive.model.PassiveNodeEffect;

import javax.annotation.Nonnull;

/**
 * Handler interface for passive node effects.
 * <p>
 * Each effect type (e.g., stat-modifier, spell-grant) has a handler that
 * knows how to apply and remove the effect from entities.
 * <p>
 * Handlers are registered with {@link PassiveEffectRegistry} and invoked
 * by {@link reign.software.hyforged.passive.service.PassiveTreeService}
 * during allocation and deallocation.
 */
public interface PassiveEffectHandler {
    
    /**
     * Apply the effect to an entity.
     *
     * @param entityRef The entity reference
     * @param node The passive node being allocated
     * @param effect The effect to apply
     */
    void apply(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect);
    
    /**
     * Remove the effect from an entity.
     *
     * @param entityRef The entity reference
     * @param node The passive node being deallocated
     * @param effect The effect to remove
     */
    void remove(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect);
    
    /**
     * Generate tooltip text for this effect.
     *
     * @param effect The effect
     * @return Formatted tooltip text
     */
    @Nonnull
    String getTooltipText(@Nonnull PassiveNodeEffect effect);
}
