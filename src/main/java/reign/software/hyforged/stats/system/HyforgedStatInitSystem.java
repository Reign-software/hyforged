package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.asset.ClassDefinition;
import reign.software.hyforged.stats.asset.ClassDefinitionRegistry;
import reign.software.hyforged.stats.component.EffectBridgeComponent;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.logging.Logger;

/**
 * ECS System for initializing entities with HyforgedStatComponent.
 * <p>
 * This RefSystem handles entity lifecycle events:
 * - onEntityAdded: Initialize default ability scores based on class
 * - onEntityRemove: Clean up any external state if needed
 * <p>
 * Following ECS principles, this system contains only processing logic.
 * All data is stored in the HyforgedStatComponent.
 * <p>
 * Note: Ability score → derived stat scaling is now handled by the scaling
 * system via ScalingRules defined in stat JSON assets.
 */
public class HyforgedStatInitSystem extends RefSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(HyforgedStatInitSystem.class.getName());

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, ProgressionComponent> progressionComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, EffectBridgeComponent> effectBridgeComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, EffectControllerComponent> effectControllerComponentType;
    
    @Nonnull
    private final Query<EntityStore> query;

    /**
     * Category ID for ability score stats (data-driven from assets).
     */
    private static final String ABILITY_SCORE_CATEGORY = "ability-score";

    public HyforgedStatInitSystem() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.progressionComponentType = plugin.getProgressionComponentType();
        this.effectBridgeComponentType = plugin.getEffectBridgeComponentType();
        this.effectControllerComponentType = EffectControllerComponent.getComponentType();
        this.query = statComponentType;
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
        HyforgedStatComponent component = commandBuffer.getComponent(ref, statComponentType);
        if (component == null) {
            return;
        }

        // Initialize ability scores based on player class
        initializeAbilityScores(component, ref, commandBuffer);
        
        // Mark all stats dirty for initial computation
        // Scaling-based derived stats will be computed by HyforgedStatComputeSystem
        component.markAllDirty();
        
        // Add EffectBridgeComponent if entity has EffectControllerComponent
        // This enables the HyforgedEffectBridgeSystem to track effects
        EffectControllerComponent effectController = commandBuffer.getComponent(ref, effectControllerComponentType);
        if (effectController != null) {
            EffectBridgeComponent effectBridge = commandBuffer.getComponent(ref, effectBridgeComponentType);
            if (effectBridge == null) {
                commandBuffer.addComponent(ref, effectBridgeComponentType, new EffectBridgeComponent());
            }
        }
    }

    @Override
    public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No cleanup needed - component data is disposed with entity
        // If we had external references (e.g., party stat sharing), clean them here
    }

    /**
     * Get the class ID for an entity.
     * <p>
     * Reads from ProgressionComponent if available, otherwise returns default class.
     *
     * @param entityRef Reference to the entity
     * @param commandBuffer Command buffer for component access
     * @return The class ID (e.g., "hyforged:default")
     */
    @Nonnull
    private String getPlayerClass(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        ProgressionComponent progression = commandBuffer.getComponent(entityRef, progressionComponentType);
        if (progression != null) {
            String activeClassId = progression.getActiveClassId();
            if (activeClassId != null && !activeClassId.isEmpty()) {
                return activeClassId;
            }
        }
        return ClassDefinitionRegistry.DEFAULT_CLASS_ID;
    }

    /**
     * Initialize ability scores based on class definition.
     * <p>
     * Sets base values for ability scores from the class definition.
     * Stats not specified in the class default to 1.
     */
    private void initializeAbilityScores(
            @Nonnull HyforgedStatComponent component,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        ClassDefinitionRegistry classRegistry = ClassDefinitionRegistry.get();
        
        // Get the entity's class
        String classId = getPlayerClass(entityRef, commandBuffer);
        ClassDefinition classDef = classRegistry.getOrDefault(classId);
        
        LOGGER.fine("Initializing ability scores for entity with class: " + classDef.id());
        
        // Get ability scores from class definition
        Map<StatId, Integer> abilityScores = classDef.abilityScores();

        // Set ability score base values from class - query by category
        for (reign.software.hyforged.stats.StatDefinition statDef : registry.getStatsInCategory(ABILITY_SCORE_CATEGORY)) {
            StatId statId = statDef.id();
            int statIndex = registry.getIndex(statId);

            if (statIndex >= 0 && component.getBaseValue(statIndex) == 0) {
                int value = abilityScores.getOrDefault(statId, statDef.defaultValue());
                component.setBaseValue(statIndex, value);
            }
        }
    }
}
