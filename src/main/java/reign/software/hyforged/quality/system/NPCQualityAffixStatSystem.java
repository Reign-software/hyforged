package reign.software.hyforged.quality.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.quality.component.HyforgedNPCQualityComponent;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applies NPC affix stat modifiers when NPC quality components are assigned or updated.
 */
public class NPCQualityAffixStatSystem extends RefChangeSystem<EntityStore, HyforgedNPCQualityComponent> {

    private static final Logger LOGGER = Logger.getLogger(NPCQualityAffixStatSystem.class.getName());
    private static final String AFFIX_SOURCE_PREFIX = "hyforged:npc_affix:";

    private final ComponentType<EntityStore, HyforgedNPCQualityComponent> qualityComponentType;
    private final ComponentType<EntityStore, EntityStatMap> statMapType;
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;
    private final Query<EntityStore> query;
    private final Set<Dependency<EntityStore>> dependencies;

    public NPCQualityAffixStatSystem() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.qualityComponentType = plugin.getNpcQualityComponentType();
        this.statMapType = EntityStatMap.getComponentType();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.query = qualityComponentType;
        this.dependencies = Set.of(new SystemDependency<>(Order.AFTER, NPCQualitySystem.class));
    }

    @Nonnull
    @Override
    public ComponentType<EntityStore, HyforgedNPCQualityComponent> componentType() {
        return qualityComponentType;
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull HyforgedNPCQualityComponent component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        clearAffixModifiers(ref, commandBuffer);
        applyAffixModifiers(ref, component, commandBuffer);
    }

    @Override
    public void onComponentSet(
            @Nonnull Ref<EntityStore> ref,
            @Nullable HyforgedNPCQualityComponent previous,
            @Nonnull HyforgedNPCQualityComponent component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        clearAffixModifiers(ref, commandBuffer);
        applyAffixModifiers(ref, component, commandBuffer);
    }

    @Override
    public void onComponentRemoved(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull HyforgedNPCQualityComponent component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        clearAffixModifiers(ref, commandBuffer);
    }

    private void clearAffixModifiers(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        EntityStatMap statMap = commandBuffer.getComponent(ref, statMapType);
        HyforgedStatComponent statComponent = commandBuffer.getComponent(ref, statComponentType);
        if (statMap != null) {
            int removed = StatAccessor.removeAllModifiersByKeyPrefix(statMap, AFFIX_SOURCE_PREFIX);
            if (removed > 0) {
                LOGGER.log(Level.FINER, "Removed {0} NPC affix modifiers", removed);
            }
            return;
        }
        if (statComponent != null) {
            int removed = statComponent.removeModifiersIf(
                    modifier -> modifier.getSourceId().startsWith(AFFIX_SOURCE_PREFIX),
                    modifier -> {
                    }
            );
            if (removed > 0) {
                statComponent.markAllDirty();
                LOGGER.log(Level.FINER, "Removed {0} NPC affix modifiers", removed);
            }
        }
    }

    private void applyAffixModifiers(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull HyforgedNPCQualityComponent component,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (!component.hasAffixes()) {
            return;
        }

        EntityStatMap statMap = commandBuffer.getComponent(ref, statMapType);
        HyforgedStatComponent statComponent = commandBuffer.getComponent(ref, statComponentType);
        if (statMap == null && statComponent == null) {
            return;
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        for (RolledAffix affix : component.getAffixes()) {
            String affixSourcePrefix = AFFIX_SOURCE_PREFIX + affix.affixId();
            for (Map.Entry<String, RolledAffix.RolledStat> entry : affix.rolledStats().entrySet()) {
                String statIdStr = entry.getKey();
                RolledAffix.RolledStat rolledStat = entry.getValue();
                StatId statId = StatId.parse(statIdStr);
                int statIndex = registry.getIndex(statId);
                if (statIndex < 0) {
                    LOGGER.log(Level.WARNING, "Unknown stat for NPC affix {0}: {1}", new Object[]{affix.affixId(), statIdStr});
                    continue;
                }

                String sourceId = affixSourcePrefix + ":" + statIdStr;
                HyforgedModifier modifier = HyforgedModifier.builder()
                        .sourceId(sourceId)
                        .sourceType(HyforgedModifier.SourceType.EFFECT)
                        .stackType(rolledStat.stackType())
                        .amount(rolledStat.value())
                        .targetStat(statIndex)
                        .priority(0)
                        .permanent()
                        .build();

                if (statMap != null) {
                    statMap.putModifier(statIndex, sourceId, modifier);
                } else if (statComponent != null) {
                    statComponent.upsertModifier(modifier);
                }
            }
        }
    }
}