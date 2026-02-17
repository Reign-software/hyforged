package reign.software.hyforged.combat.scaling;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * System that assigns monster levels based on distance from world spawn.
 * <p>
 * When an NPC entity is added to the world, this system:
 * <ol>
 *   <li>Calculates the monster's distance from world spawn</li>
 *   <li>Determines the monster level using {@link MonsterScalingService}</li>
 *   <li>Attaches a {@link MonsterLevelComponent} to cache the level</li>
 *   <li>Applies stat modifiers based on the NPC's scaling configuration</li>
 * </ol>
 * <p>
 * Stat scaling is fully data-driven. Each NPC type can define which stats
 * scale with level via {@link MonsterScalingConfigAsset} JSON files.
 * <p>
 * This system uses {@link RefSystem} to react to NPC entity creation.
 */
public class HyforgedMonsterScalingSystem extends RefSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Source ID for level-based stat modifiers */
    private static final String LEVEL_MODIFIER_SOURCE = "hyforged:monster_level";

    @Nonnull
    private final ComponentType<EntityStore, EntityStatMap> statMapType;

    @Nonnull
    private final ComponentType<EntityStore, MonsterLevelComponent> levelComponentType;

    @Nonnull
    private final ComponentType<EntityStore, NPCEntity> npcComponentType;

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    public HyforgedMonsterScalingSystem() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.statMapType = EntityStatMap.getComponentType();
        this.levelComponentType = plugin.getMonsterLevelComponentType();
        this.npcComponentType = NPCEntity.getComponentType();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.transformComponentType = TransformComponent.getComponentType();

        // Query for NPCs that also have a transform (needed for position)
        this.query = Query.and(npcComponentType, transformComponentType);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    /**
     * Called when an entity matching our query is added.
     */
    @Override
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        NPCEntity npcEntity = commandBuffer.getComponent(ref, npcComponentType);
        TransformComponent transform = commandBuffer.getComponent(ref, transformComponentType);

        if (npcEntity == null || transform == null) {
            return;
        }

        Vector3d position = transform.getPosition();

        // Get world for spawn distance calculation
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        // Get the NPC's role name for config lookup
        String roleName = npcEntity.getRoleName();

        // Calculate monster level
        MonsterScalingService scalingService = MonsterScalingService.get();
        int level = scalingService.calculateMonsterLevel(world, position);

        LOGGER.at(Level.FINE).log("Assigning level %s to NPC '%s' at (%s, %s, %s)", level, roleName, position.getX(), position.getY(), position.getZ());

        // Add MonsterLevelComponent
        commandBuffer.putComponent(ref, levelComponentType, new MonsterLevelComponent(level));

        // Apply stat modifiers if entity has EntityStatMap
        EntityStatMap statMap = commandBuffer.getComponent(ref, statMapType);
        HyforgedStatComponent statComponent = commandBuffer.getComponent(ref, statComponentType);
        if (statMap != null || statComponent != null) {
            applyLevelModifiers(statMap, statComponent, level, roleName, scalingService);
        }
    }

    @Override
    public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Cleanup not needed; entity is being removed
    }

    /**
     * Apply level-based stat modifiers to the entity.
     * <p>
     * Stats to scale are read from the NPC's scaling configuration.
     * Each stat entry defines the stat ID, modifier type, and scaling per level.
     *
     * @param statMap The entity's stat map component
     * @param level The monster's level
     * @param roleName The NPC's role name for config lookup
     * @param scalingService The scaling service
     */
    private void applyLevelModifiers(
            @Nullable EntityStatMap statMap,
            @Nullable HyforgedStatComponent statComponent,
            int level,
            @Nonnull String roleName,
            @Nonnull MonsterScalingService scalingService
    ) {
        // Get the scaled stats for this NPC (uses default if no specific config)
        List<ScaledStatEntry> scaledStats = scalingService.getScaledStats(roleName);
        
        if (scaledStats.isEmpty()) {
            LOGGER.at(Level.FINE).log("No scaled stats configured for NPC '%s'", roleName);
            return;
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        WorldScalingConfig worldConfig = scalingService.getActiveConfig();
        int minLevel = worldConfig.minLevel();

        for (ScaledStatEntry entry : scaledStats) {
            StatId statId = entry.toStatId();
            int statIndex = registry.getIndex(statId);
            
            if (statIndex < 0) {
                LOGGER.atWarning().log("Unknown stat '%s' in scaling config for NPC '%s'", entry.getStatId(), roleName);
                continue;
            }

            // Calculate the modifier value based on level
            int modifierValue = entry.calculateModifierValue(level, minLevel);
            
            if (modifierValue == 0) {
                continue; // No scaling at this level
            }

            // Create the stat modifier
            HyforgedModifier modifier = HyforgedModifier.builder()
                    .sourceId(LEVEL_MODIFIER_SOURCE)
                    .sourceType(HyforgedModifier.SourceType.EFFECT)
                    .stackType(entry.getModifierType())
                    .targetStat(statIndex)
                    .amount(modifierValue)
                    .priority(0)
                    .permanent()
                    .build();
            
            // Use unique source key per stat to avoid collisions
            String sourceKey = LEVEL_MODIFIER_SOURCE + ":" + statId.fullId();
            if (statMap != null && StatAccessor.hasStatSlot(statMap, statIndex)) {
                statMap.putModifier(statIndex, sourceKey, modifier);
            } else if (statComponent != null) {
                statComponent.upsertModifier(modifier);
            }
            
            LOGGER.at(Level.FINER).log("Applied %s modifier to stat '%s': %s (level %s)", entry.getModifierType(), entry.getStatId(), modifierValue, level);
        }
        // EntityStatMap auto-recomputes, no need to mark dirty
    }
}
