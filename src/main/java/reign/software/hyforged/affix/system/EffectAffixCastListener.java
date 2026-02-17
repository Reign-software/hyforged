package reign.software.hyforged.affix.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.service.EffectAffixProcessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Listens for player interactions to trigger on-cast affix effects.
 */
@SuppressWarnings("deprecation")
public class EffectAffixCastListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private EventRegistration<String, PlayerInteractEvent> globalRegistration;

    @Nonnull
    private final EffectAffixProcessor processor;

    public EffectAffixCastListener() {
        this.processor = new EffectAffixProcessor(HyforgedPlugin.getInstance().getActiveEffectsComponentType());
    }

    public void register() {
        globalRegistration = HytaleServer.get().getEventBus()
                .registerGlobal((short) 0, PlayerInteractEvent.class, this::onInteract);
        LOGGER.atInfo().log("EffectAffixCastListener registered for player interactions");
    }

    public void unregister() {
        if (globalRegistration != null) {
            globalRegistration.unregister();
            globalRegistration = null;
        }
    }

    private void onInteract(@Nonnull PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Ref<EntityStore> caster = event.getPlayerRef();
        if (caster == null || !caster.isValid()) {
            return;
        }

        Ref<EntityStore> target = event.getTargetRef();
        Ref<EntityStore> targetRef = target != null && target.isValid() ? target : null;
        Vector3d position = toVector3d(event.getTargetBlock());

        caster.getStore().forEachChunk((chunk, commandBuffer) -> {
            // Use the first chunk solely to obtain a command buffer and avoid duplicate triggers.
            processor.processOnCast(caster, targetRef, event.getActionType(), commandBuffer, position);
            return true;
        });
    }

    @Nullable
    private Vector3d toVector3d(@Nullable Vector3i block) {
        if (block == null) {
            return null;
        }
        return new Vector3d(block.getX(), block.getY(), block.getZ());
    }
}
