package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.CoreStats;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.asset.ClassDefinition;
import reign.software.hyforged.stats.asset.ClassDefinitionRegistry;
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
    private final Query<EntityStore> query;

    /**
     * Default base value for ability scores when class doesn't specify.
     */
    private static final int DEFAULT_ABILITY_SCORE = 1;

    public HyforgedStatInitSystem() {
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
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
        initializeAbilityScores(component, ref);
        
        // Mark all stats dirty for initial computation
        // Scaling-based derived stats will be computed by HyforgedStatComputeSystem
        component.markAllDirty();
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
     * Currently returns the default class for all entities.
     * Future: Look up class from player data, equipment, or other sources.
     *
     * @param entityRef Reference to the entity
     * @return The class ID (e.g., "hyforged:default")
     */
    @Nonnull
    private String getPlayerClass(@Nonnull Ref<EntityStore> entityRef) {
        // TODO: In future, retrieve class from player data component
        // For now, return the default class
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
            @Nonnull Ref<EntityStore> entityRef
    ) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        ClassDefinitionRegistry classRegistry = ClassDefinitionRegistry.get();
        
        // Get the entity's class
        String classId = getPlayerClass(entityRef);
        ClassDefinition classDef = classRegistry.getOrDefault(classId);
        
        LOGGER.fine("Initializing ability scores for entity with class: " + classDef.id());
        
        // Get ability scores from class definition
        Map<StatId, Integer> abilityScores = classDef.abilityScores();
        
        // Set ability score base values from class
        int[] abilityStats = {
            registry.getIndex(CoreStats.STRENGTH),
            registry.getIndex(CoreStats.DEXTERITY),
            registry.getIndex(CoreStats.INTELLIGENCE),
            registry.getIndex(CoreStats.CONSTITUTION),
            registry.getIndex(CoreStats.WISDOM),
            registry.getIndex(CoreStats.SPIRIT),
            registry.getIndex(CoreStats.LUCK)
        };
        
        StatId[] abilityStatIds = {
            StatId.hyforged("strength"),
            StatId.hyforged("dexterity"),
            StatId.hyforged("intelligence"),
            StatId.hyforged("constitution"),
            StatId.hyforged("wisdom"),
            StatId.hyforged("spirit"),
            StatId.hyforged("luck")
        };
        
        for (int i = 0; i < abilityStats.length; i++) {
            int statIndex = abilityStats[i];
            StatId statId = abilityStatIds[i];
            
            if (statIndex >= 0 && component.getBaseValue(statIndex) == 0) {
                // Use class-defined value, or default if not specified
                int value = abilityScores.getOrDefault(statId, DEFAULT_ABILITY_SCORE);
                component.setBaseValue(statIndex, value);
            }
        }
    }
}
