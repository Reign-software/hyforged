package reign.software.hyforged.quality.service;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.service.ActiveEffectInitializer;
import reign.software.hyforged.quality.component.HyforgedNPCQualityComponent;
import reign.software.hyforged.quality.model.NPCQualityRule;
import reign.software.hyforged.quality.registry.NPCQualityRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Public API for querying and updating NPC quality.
 */
public final class NPCQualityService {

    private NPCQualityService() {}

    @Nullable
    public static HyforgedNPCQualityComponent getComponent(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef
    ) {
        ComponentType<EntityStore, HyforgedNPCQualityComponent> type =
                HyforgedPlugin.getInstance().getNpcQualityComponentType();
        return store.getComponent(entityRef, type);
    }

    @Nullable
    public static String getQualityId(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef
    ) {
        HyforgedNPCQualityComponent component = getComponent(store, entityRef);
        if (component == null) {
            return null;
        }
        String qualityId = component.getQualityId();
        return qualityId != null && !qualityId.isBlank() ? qualityId : null;
    }

    public static void setQuality(
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull String qualityId,
            @Nonnull List<RolledAffix> affixes
    ) {
        ComponentType<EntityStore, HyforgedNPCQualityComponent> type =
                HyforgedPlugin.getInstance().getNpcQualityComponentType();
        HyforgedNPCQualityComponent component = commandBuffer.getComponent(entityRef, type);
        if (component == null) {
            HyforgedNPCQualityComponent newComponent = new HyforgedNPCQualityComponent(qualityId, affixes);
            commandBuffer.addComponent(entityRef, type, newComponent);
            commandBuffer.run(store -> ActiveEffectInitializer.refreshFromNpcQuality(entityRef, newComponent, store));
            return;
        }
        component.setQualityId(qualityId);
        component.setAffixes(affixes);
        commandBuffer.run(store -> ActiveEffectInitializer.refreshFromNpcQuality(entityRef, component, store));
    }

    @Nullable
    public static NPCQualityRule getDefaultRule() {
        return NPCQualityRegistry.get().getDefaultRule();
    }

    public static double resolveStatMultiplier(
            @Nonnull NPCQualityRule rule,
            @Nonnull String qualityId
    ) {
        Double multiplier = rule.statMultipliers().get(qualityId);
        return multiplier != null ? multiplier : 1.0;
    }

    public static int resolveLootQualityBonus(
            @Nonnull NPCQualityRule rule,
            @Nonnull String qualityId
    ) {
        Integer bonus = rule.lootQualityBonus().get(qualityId);
        return bonus != null ? bonus : 0;
    }
}
