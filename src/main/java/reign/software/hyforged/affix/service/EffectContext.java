package reign.software.hyforged.affix.service;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Execution context for triggered affix effects.
 */
public record EffectContext(
    @Nonnull String effectKey,
    @Nonnull Ref<EntityStore> sourceRef,
    @Nullable Ref<EntityStore> targetRef,
    @Nonnull ComponentAccessor<EntityStore> accessor,
    @Nullable Vector3d position
) {

    @Nullable
    public Store<EntityStore> getStore() {
        if (accessor instanceof Store<?> store) {
            @SuppressWarnings("unchecked")
            Store<EntityStore> typed = (Store<EntityStore>) store;
            return typed;
        }
        if (accessor instanceof CommandBuffer<?> commandBuffer) {
            @SuppressWarnings("unchecked")
            Store<EntityStore> typed = (Store<EntityStore>) commandBuffer.getStore();
            return typed;
        }
        return null;
    }
}
