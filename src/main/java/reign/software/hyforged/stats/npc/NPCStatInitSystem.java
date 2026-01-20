package reign.software.hyforged.stats.npc;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Random;
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
    private final Query<EntityStore> query;
    
    private final Random random = new Random();

    /**
     * Default NPC level when none can be determined.
     */
    private static final int DEFAULT_NPC_LEVEL = 1;

    public NPCStatInitSystem() {
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        this.npcComponentType = NPCEntity.getComponentType();
        
        // Query for entities with BOTH NPCEntity and HyforgedStatComponent
        this.query = Query.and(npcComponentType, statComponentType);
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
        HyforgedStatComponent statComponent = commandBuffer.getComponent(ref, statComponentType);
        
        if (npcEntity == null || statComponent == null) {
            return;
        }

        // Determine the template ID based on NPC role
        String templateId = determineTemplateId(npcEntity);
        
        // Determine NPC level
        int level = determineNPCLevel(npcEntity);
        
        LOGGER.fine("Initializing NPC stats from template: " + templateId + " at level " + level);
        
        // Apply template stats
        applyTemplateStats(statComponent, templateId, level);
        
        // Check for elite/boss status and apply modifiers
        applyEliteModifiers(statComponent, npcEntity, templateId);
        
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
     * Currently returns default level. Future: check spawn group,
     * region difficulty, or other sources.
     *
     * @param npcEntity The NPC entity
     * @return The NPC level
     */
    private int determineNPCLevel(@Nonnull NPCEntity npcEntity) {
        // TODO: Check spawn configuration for level
        // TODO: Check region difficulty
        // For now, return default level
        return DEFAULT_NPC_LEVEL;
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
     * Elite status is detected from NPC properties.
     * Currently a placeholder - needs integration with Hytale's elite system.
     *
     * @param component  The stat component
     * @param npcEntity  The NPC entity
     * @param templateId The template ID
     */
    private void applyEliteModifiers(
            @Nonnull HyforgedStatComponent component,
            @Nonnull NPCEntity npcEntity,
            @Nonnull String templateId
    ) {
        // TODO: Detect elite/boss status from NPC entity
        // For now, this is a placeholder
        
        // Check if elite (placeholder condition)
        boolean isElite = false;  // Would check npcEntity for elite flag
        boolean isBoss = false;   // Would check npcEntity for boss flag
        
        if (!isElite && !isBoss) {
            return;
        }
        
        NPCStatTemplateRegistry registry = NPCStatTemplateRegistry.get();
        NPCStatTemplate template = registry.getTemplate(templateId);
        
        if (template == null) {
            return;
        }
        
        // Get appropriate modifier pool
        String poolName = isBoss ? "boss" : "elite";
        java.util.List<String> modifierPool = template.getModifierPool(poolName);
        
        if (modifierPool.isEmpty()) {
            return;
        }
        
        // Select random modifiers
        int modifierCount = isBoss ? 3 : 1;
        for (int i = 0; i < modifierCount && !modifierPool.isEmpty(); i++) {
            String modifierId = modifierPool.get(random.nextInt(modifierPool.size()));
            // TODO: Look up modifier by ID and apply to component
            LOGGER.fine("Would apply elite modifier: " + modifierId);
        }
    }
}
