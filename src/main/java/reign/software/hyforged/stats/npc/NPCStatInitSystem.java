package reign.software.hyforged.stats.npc;

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
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.combat.scaling.HyforgedMonsterScalingSystem;
import reign.software.hyforged.combat.scaling.MonsterLevelComponent;
import reign.software.hyforged.combat.scaling.MonsterScalingService;
import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.AffixTierDefinition;
import reign.software.hyforged.affix.model.AffixTierStat;
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.quality.model.NPCQualityRule;
import reign.software.hyforged.quality.registry.NPCQualityRegistry;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

/**
 * ECS System for initializing NPC entities with stats from templates.
 * <p>
 * This RefSystem handles NPC entity lifecycle events:
 * - onEntityAdded: Apply template stats based on NPC role
 * - Apply elite/boss modifiers from template pools
 * <p>
 * Uses the NPC role name to look up the appropriate stat template.
 * Falls back to base template if no matching template is found.
 */
public class NPCStatInitSystem extends RefSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(NPCStatInitSystem.class.getName());

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, NPCEntity> npcComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, MonsterLevelComponent> monsterLevelComponentType;

    @Nonnull
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;
    
    private final Random random = new Random();

    public NPCStatInitSystem() {
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        this.npcComponentType = NPCEntity.getComponentType();
        this.monsterLevelComponentType = HyforgedPlugin.getInstance().getMonsterLevelComponentType();
        this.transformComponentType = TransformComponent.getComponentType();
        
        // Query for entities with BOTH NPCEntity and HyforgedStatComponent
        this.query = Query.and(npcComponentType, statComponentType);
        this.dependencies = Set.of(
                new SystemDependency<>(Order.AFTER, HyforgedMonsterScalingSystem.class)
        );
    }

    @Nonnull
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
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        NPCEntity npcEntity = commandBuffer.getComponent(ref, npcComponentType);
        HyforgedStatComponent statComponent = commandBuffer.getComponent(ref, statComponentType);
        
        if (npcEntity == null || statComponent == null) {
            return;
        }

        // Determine the template ID based on NPC role
        String templateId = determineTemplateId(npcEntity);
        
        // Determine NPC level
        int level = determineNPCLevel(ref, store, commandBuffer);
        
        LOGGER.fine("Initializing NPC stats from template: " + templateId + " at level " + level);
        
        // Apply template stats
        applyTemplateStats(statComponent, templateId, level);
        
        // Check for elite/boss status and apply modifiers
        applyEliteModifiers(statComponent, npcEntity, templateId, level);
        
        // Mark all stats dirty for computation
        statComponent.markAllDirty();
    }

    @Override
    public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No cleanup needed
    }

    /**
     * Determine the stat template ID for an NPC.
     * <p>
     * Uses the NPC role name to construct a template ID.
     * Falls back to hostile or base template if not found.
     *
     * @param npcEntity The NPC entity
     * @return The template ID to use
     */
    @Nonnull
    private String determineTemplateId(@Nonnull NPCEntity npcEntity) {
        String roleName = npcEntity.getRoleName();
        
        if (roleName != null && !roleName.isEmpty()) {
            // Try role-specific template first
            String roleTemplateId = "hyforged:" + roleName.toLowerCase().replace(" ", "_");
            
            NPCStatTemplateRegistry registry = NPCStatTemplateRegistry.get();
            if (registry.hasTemplate(roleTemplateId)) {
                return roleTemplateId;
            }
        }
        
        // Default to hostile template for most NPCs
        return NPCStatTemplateRegistry.HOSTILE_TEMPLATE_ID;
    }

    /**
     * Determine the level for an NPC.
     * <p>
     * Prefers the cached level from {@link MonsterLevelComponent} and falls back
     * to distance-based scaling when possible. If no world/position data exists,
     * uses the active scaling config's minimum level.
     *
     * @return The NPC level
     */
    private int determineNPCLevel(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        MonsterLevelComponent levelComponent = commandBuffer.getComponent(ref, monsterLevelComponentType);
        if (levelComponent != null && levelComponent.getLevel() > 0) {
            return levelComponent.getLevel();
        }

        MonsterScalingService scalingService = MonsterScalingService.get();
        int level = scalingService.getActiveConfig().minLevel();

        TransformComponent transform = commandBuffer.getComponent(ref, transformComponentType);
        World world = store.getExternalData().getWorld();
        if (transform != null && world != null) {
            level = scalingService.calculateMonsterLevel(world, transform.getPosition());
        }

        if (levelComponent == null) {
            commandBuffer.putComponent(ref, monsterLevelComponentType, new MonsterLevelComponent(level));
        } else {
            levelComponent.setLevel(level);
        }

        return level;
    }

    /**
     * Apply stats from a template to the component.
     *
     * @param component  The stat component
     * @param templateId The template ID
     * @param level      The NPC level
     */
    private void applyTemplateStats(
            @Nonnull HyforgedStatComponent component,
            @Nonnull String templateId,
            int level
    ) {
        NPCStatTemplateRegistry registry = NPCStatTemplateRegistry.get();
        StatDefinitionRegistry statRegistry = StatDefinitionRegistry.get();
        
        // Get resolved stats for this template and level
        Map<StatId, Integer> resolvedStats = registry.resolveStats(templateId, level);
        
        // Apply each stat as a base value
        for (Map.Entry<StatId, Integer> entry : resolvedStats.entrySet()) {
            StatId statId = entry.getKey();
            int value = entry.getValue();
            
            int statIndex = statRegistry.getIndex(statId.fullId());
            if (statIndex >= 0) {
                component.setBaseValue(statIndex, value);
            }
        }
    }

    /**
     * Apply elite/boss modifiers from the template's modifier pools.
     * <p>
        * Modifier pool selection is driven by NPC quality rules.
     *
     * @param component  The stat component
     * @param npcEntity  The NPC entity
     * @param templateId The template ID
     */
    private void applyEliteModifiers(
            @Nonnull HyforgedStatComponent component,
            @Nonnull NPCEntity npcEntity,
            @Nonnull String templateId,
            int level
    ) {
        NPCQualityRule rule = NPCQualityRegistry.get().resolveRuleForRole(npcEntity.getRoleName());
        if (rule == null) {
            return;
        }

        String poolName = rule.modifierPool();
        int modifierCount = rule.modifierCount();
        if (poolName.isBlank() || modifierCount <= 0) {
            return;
        }
        
        NPCStatTemplateRegistry registry = NPCStatTemplateRegistry.get();
        NPCStatTemplate template = registry.getTemplate(templateId);
        
        if (template == null) {
            return;
        }
        
        java.util.List<String> modifierPool = template.getModifierPool(poolName);
        
        if (modifierPool.isEmpty()) {
            return;
        }
        
        List<String> remaining = new ArrayList<>(modifierPool);
        for (int i = 0; i < modifierCount && !remaining.isEmpty(); i++) {
            String modifierId = remaining.remove(random.nextInt(remaining.size()));
            applyModifierById(component, modifierId, level, poolName);
        }
    }

    private void applyModifierById(
            @Nonnull HyforgedStatComponent component,
            @Nonnull String modifierId,
            int level,
            @Nonnull String poolName
    ) {
        AffixDefinition definition = resolveAffixDefinition(modifierId);
        if (definition == null) {
            LOGGER.fine("Unknown modifier id in NPC template pool '" + poolName + "': " + modifierId);
            return;
        }

        AffixTierDefinition tier = selectTierForLevel(definition, level);
        if (tier == null) {
            LOGGER.fine("No eligible tiers for modifier '" + definition.id() + "' at level " + level);
            return;
        }

        StatDefinitionRegistry statRegistry = StatDefinitionRegistry.get();
        for (Map.Entry<String, AffixTierStat> entry : tier.stats().entrySet()) {
            AffixTierStat tierStat = entry.getValue();
            StatId statId = tierStat.statId();
            int statIndex = statRegistry.getIndex(statId);
            if (statIndex < 0) {
                LOGGER.fine("Unknown stat for modifier '" + definition.id() + "': " + statId.fullId());
                continue;
            }

            int rolledValue = tierStat.rollValue(random.nextDouble());
            String sourceId = "hyforged:npc_template_modifier:" + definition.id() + ":" + statId.fullId();
            HyforgedModifier modifier = HyforgedModifier.builder()
                    .sourceId(sourceId)
                    .sourceType(HyforgedModifier.SourceType.EFFECT)
                    .stackType(tierStat.stackType())
                    .amount(rolledValue)
                    .targetStat(statIndex)
                    .priority(0)
                    .permanent()
                    .build();

            component.upsertModifier(modifier);
        }
    }

    @Nullable
    private AffixDefinition resolveAffixDefinition(@Nonnull String modifierId) {
        AffixDefinitionRegistry registry = AffixDefinitionRegistry.get();
        AffixDefinition definition = registry.get(modifierId);
        if (definition != null) {
            return definition;
        }

        if (!modifierId.contains(":")) {
            definition = registry.get("hyforged:" + modifierId);
        }

        return definition;
    }

    @Nullable
    private AffixTierDefinition selectTierForLevel(
            @Nonnull AffixDefinition definition,
            int level
    ) {
        List<AffixTierDefinition> eligibleTiers = definition.getAvailableTiers(level);
        if (eligibleTiers.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (AffixTierDefinition tier : eligibleTiers) {
            totalWeight += Math.max(1, tier.weight());
        }

        if (totalWeight <= 0) {
            return eligibleTiers.get(0);
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (AffixTierDefinition tier : eligibleTiers) {
            cumulative += Math.max(1, tier.weight());
            if (roll < cumulative) {
                return tier;
            }
        }

        return eligibleTiers.get(eligibleTiers.size() - 1);
    }
}
