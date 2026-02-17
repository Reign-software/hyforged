package reign.software.hyforged.quality.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.systems.RoleBuilderSystem;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.service.AffixRollContext;
import reign.software.hyforged.affix.service.AffixRollResult;
import reign.software.hyforged.affix.service.AffixRollerService;
import reign.software.hyforged.affix.service.ActiveEffectInitializer;
import reign.software.hyforged.combat.scaling.MonsterLevelComponent;
import reign.software.hyforged.quality.component.HyforgedNPCQualityComponent;
import reign.software.hyforged.quality.event.NPCQualityAssignedEvent;
import reign.software.hyforged.quality.model.NPCQualityRule;
import reign.software.hyforged.quality.model.QualityWeightTable;
import reign.software.hyforged.quality.registry.NPCQualityRegistry;
import reign.software.hyforged.quality.service.NPCQualityService;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;
import reign.software.hyforged.stats.npc.NPCStatInitSystem;
import reign.software.hyforged.stats.npc.NPCStatTemplate;
import reign.software.hyforged.stats.npc.NPCStatTemplateRegistry;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

/**
 * Assigns NPC quality tiers on spawn and applies stat scaling.
 */
public class NPCQualitySystem extends RefSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String QUALITY_MODIFIER_SOURCE = "hyforged:npc_quality";

    private final ComponentType<EntityStore, NPCEntity> npcComponentType;
    private final ComponentType<EntityStore, HyforgedNPCQualityComponent> qualityComponentType;
    private final ComponentType<EntityStore, EntityStatMap> statMapType;
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;
    private final Query<EntityStore> query;
    private final Set<Dependency<EntityStore>> dependencies;
    private final Random random = new Random();
    private final AffixRollerService affixRollerService = new AffixRollerService();

    public NPCQualitySystem() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.npcComponentType = NPCEntity.getComponentType();
        this.qualityComponentType = plugin.getNpcQualityComponentType();
        this.statMapType = EntityStatMap.getComponentType();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.query = npcComponentType;
        this.dependencies = Set.of(
                new SystemDependency<>(Order.AFTER, RoleBuilderSystem.class),
                new SystemDependency<>(Order.AFTER, NPCStatInitSystem.class)
        );
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        NPCEntity npcEntity = commandBuffer.getComponent(ref, npcComponentType);
        if (npcEntity == null) {
            return;
        }

        HyforgedNPCQualityComponent existing = commandBuffer.getComponent(ref, qualityComponentType);
        if (existing != null && existing.getQualityId() != null && !existing.getQualityId().isBlank()) {
            return;
        }

        NPCQualityRule rule = NPCQualityRegistry.get().resolveRuleForRole(npcEntity.getRoleName());
        if (rule == null) {
            LOGGER.at(Level.FINE).log("No NPC quality rule available; skipping quality assignment");
            return;
        }

        QualityWeightTable table = NPCQualityRegistry.get().getTable(rule.id());
        if (table == null) {
            LOGGER.atWarning().log("Missing NPC quality weight table for rule %s", rule.id());
            return;
        }

        String qualityId = table.roll(random);
        if (qualityId == null || qualityId.isBlank()) {
            return;
        }

        List<RolledAffix> affixes = rollNpcAffixes(ref, npcEntity, store, qualityId);
        HyforgedNPCQualityComponent component = new HyforgedNPCQualityComponent(qualityId, affixes);
        commandBuffer.putComponent(ref, qualityComponentType, component);

        commandBuffer.run(entityStore -> ActiveEffectInitializer.refreshFromNpcQuality(ref, component, entityStore));

        applyStatScaling(ref, commandBuffer, npcEntity, rule, qualityId);

        emitQualityAssignedEvent(ref, qualityId, rule.id(), component.getAffixes());
    }

        @Override
        public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
        ) {
        // No cleanup required
        }

    private void applyStatScaling(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull NPCEntity npcEntity,
            @Nonnull NPCQualityRule rule,
            @Nonnull String qualityId
    ) {
        double multiplier = NPCQualityService.resolveStatMultiplier(rule, qualityId);
        if (multiplier == 1.0 || multiplier == 0.0) {
            return;
        }

        int bonusBps = toBasisPoints(multiplier);
        if (bonusBps == 0) {
            return;
        }

        NPCStatTemplate template = resolveTemplate(npcEntity);
        if (template == null) {
            return;
        }

        EntityStatMap statMap = commandBuffer.getComponent(ref, statMapType);
        HyforgedStatComponent statComponent = commandBuffer.getComponent(ref, statComponentType);
        if (statMap == null && statComponent == null) {
            return;
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        for (StatId statId : template.stats().keySet()) {
            int statIndex = registry.getIndex(statId);
            if (statIndex < 0) {
                LOGGER.at(Level.FINE).log("Unknown stat '%s' in NPC template '%s'", statId.fullId(), template.id());
                continue;
            }

            HyforgedModifier modifier = HyforgedModifier.builder()
                    .sourceId(QUALITY_MODIFIER_SOURCE)
                    .sourceType(HyforgedModifier.SourceType.EFFECT)
                    .stackType(HyforgedModifier.StackType.MORE)
                    .targetStat(statIndex)
                    .amount(bonusBps)
                    .priority(0)
                    .permanent()
                    .build();

            if (statMap != null && StatAccessor.hasStatSlot(statMap, statIndex)) {
                String sourceKey = QUALITY_MODIFIER_SOURCE + ":" + statId.fullId();
                statMap.putModifier(statIndex, sourceKey, modifier);
            } else if (statComponent != null) {
                statComponent.upsertModifier(modifier);
            }
        }
    }

    @Nonnull
    private List<RolledAffix> rollNpcAffixes(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull NPCEntity npcEntity,
            @Nonnull Store<EntityStore> store,
            @Nonnull String qualityId
    ) {
        int itemLevel = resolveNpcLevel(ref, store);
        String roleName = npcEntity.getRoleName();
        String itemId = roleName != null && !roleName.isBlank()
                ? "npc:" + roleName.toLowerCase().replace(" ", "_")
                : "npc:unknown";

        String[] categories = new String[]{"Entities.NPC"};
        String[] tags = buildNpcTags(npcEntity);

        AffixRollContext context = AffixRollContext.of(itemId, qualityId, itemLevel, categories, tags);
        AffixRollResult result = affixRollerService.rollAffixes(context, random);
        return result.affixes();
    }

    private int resolveNpcLevel(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        MonsterLevelComponent monsterLevel = store.getComponent(ref, HyforgedPlugin.getInstance().getMonsterLevelComponentType());
        if (monsterLevel != null && monsterLevel.getLevel() > 0) {
            return monsterLevel.getLevel();
        }
        return 1;
    }

    @Nonnull
    private String[] buildNpcTags(@Nonnull NPCEntity npcEntity) {
        List<String> tags = new ArrayList<>();
        tags.add("Type:NPC");
        String roleName = npcEntity.getRoleName();
        if (roleName != null && !roleName.isBlank()) {
            tags.add("Role:" + roleName);
        }
        return tags.toArray(String[]::new);
    }

    @Nullable
    private NPCStatTemplate resolveTemplate(@Nonnull NPCEntity npcEntity) {
        String templateId = determineTemplateId(npcEntity);
        return NPCStatTemplateRegistry.get().getTemplateOrBase(templateId);
    }

    @Nonnull
    private String determineTemplateId(@Nonnull NPCEntity npcEntity) {
        String roleName = npcEntity.getRoleName();
        if (roleName != null && !roleName.isBlank()) {
            String roleTemplateId = "hyforged:" + roleName.toLowerCase().replace(" ", "_");
            NPCStatTemplateRegistry registry = NPCStatTemplateRegistry.get();
            if (registry.hasTemplate(roleTemplateId)) {
                return roleTemplateId;
            }
        }
        return NPCStatTemplateRegistry.HOSTILE_TEMPLATE_ID;
    }

    private int toBasisPoints(double multiplier) {
        if (Double.isNaN(multiplier) || Double.isInfinite(multiplier)) {
            return 0;
        }
        return (int) Math.round((multiplier - 1.0) * HyforgedModifier.BPS_100_PERCENT);
    }

    private void emitQualityAssignedEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull String qualityId,
            @Nonnull String ruleId,
            @Nonnull List<RolledAffix> affixes
    ) {
        try {
            IEventDispatcher<NPCQualityAssignedEvent, NPCQualityAssignedEvent> dispatcher =
                    HytaleServer.get().getEventBus().dispatchFor(NPCQualityAssignedEvent.class);
            dispatcher.dispatch(new NPCQualityAssignedEvent(ref, qualityId, ruleId, affixes));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to emit NPCQualityAssignedEvent");
        }
    }
}
