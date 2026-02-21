package reign.software.hyforged.ward;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;

/**
 * Listens for player interactions (spells/skills) and restores Ward on each cast.
 * <p>
 * Ward recovery on cast is controlled by two stats:
 * <ul>
 *   <li>{@code hyforged:ward-on-cast-flat} — flat amount restored per cast</li>
 *   <li>{@code hyforged:ward-on-cast-bps} — percentage of max Ward restored per cast (in basis points)</li>
 * </ul>
 * The two values are summed each cast and added to the current Ward up to the maximum.
 * <p>
 * Entities with zero max Ward (no Ward affixes) are skipped with no overhead.
 */
@SuppressWarnings("deprecation")
public class WardRestoreOnCastListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private EventRegistration<String, PlayerInteractEvent> globalRegistration;

    private int wardOnCastFlatIndex = -1;
    private int wardOnCastBpsIndex = -1;
    private int wardEntityStatIndex = -1;
    private boolean indicesInitialized = false;

    public void register() {
        globalRegistration = HytaleServer.get().getEventBus()
                .registerGlobal((short) 0, PlayerInteractEvent.class, this::onInteract);
        LOGGER.atInfo().log("WardRestoreOnCastListener registered for player interactions");
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

        caster.getStore().forEachChunk((chunk, commandBuffer) -> {
            applyWardRestore(caster);
            return true;
        });
    }

    private void applyWardRestore(@Nonnull Ref<EntityStore> casterRef) {
        ensureIndicesInitialized();

        if (wardEntityStatIndex < 0) {
            return;
        }

        EntityStatMap statMap = casterRef.getStore().getComponent(casterRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }

        if (!StatAccessor.hasStatSlot(statMap, wardEntityStatIndex)) {
            return;
        }

        EntityStatValue wardValue = statMap.get(wardEntityStatIndex);
        if (wardValue == null || wardValue.getMax() <= 0) {
            return;
        }

        float maxWard = wardValue.getMax();
        float currentWard = wardValue.get();
        if (currentWard >= maxWard) {
            return;
        }

        int flatRestore = 0;
        int percentBps = 0;
        if (wardOnCastFlatIndex >= 0) {
            flatRestore = StatAccessor.getStatValueInt(statMap, wardOnCastFlatIndex);
        }
        if (wardOnCastBpsIndex >= 0) {
            percentBps = StatAccessor.getStatValueInt(statMap, wardOnCastBpsIndex);
        }

        if (flatRestore <= 0 && percentBps <= 0) {
            return;
        }

        float percentRestore = maxWard * percentBps / 10000.0f;
        float totalRestore = flatRestore + percentRestore;

        if (totalRestore <= 0) {
            return;
        }

        float newWard = Math.min(currentWard + totalRestore, maxWard);
        statMap.setStatValue(wardEntityStatIndex, newWard);

        LOGGER.at(java.util.logging.Level.FINE)
                .log("Ward restore on cast: %.1f -> %.1f / %.1f (flat=%d, pct=%d bps)",
                        currentWard, newWard, maxWard, flatRestore, percentBps);
    }

    private void ensureIndicesInitialized() {
        if (indicesInitialized) {
            return;
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        wardOnCastFlatIndex = registry.getIndex(StatId.hyforged("ward-on-cast-flat"));
        wardOnCastBpsIndex = registry.getIndex(StatId.hyforged("ward-on-cast-bps"));
        wardEntityStatIndex = EntityStatType.getAssetMap().getIndex("Ward");
        indicesInitialized = true;
    }
}
