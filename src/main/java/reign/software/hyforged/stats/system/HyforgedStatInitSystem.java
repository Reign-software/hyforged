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
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;

/**
 * ECS System for initializing entities with HyforgedStatComponent.
 * <p>
 * This RefSystem handles entity lifecycle events:
 * - onEntityAdded: Initialize default ability scores
 * - onEntityRemove: Clean up any external state if needed
 * <p>
 * Following ECS principles, this system contains only processing logic.
 * All data is stored in the HyforgedStatComponent.
 * <p>
 * Note: Ability score → derived stat scaling is now handled by the scaling
 * system via ScalingRules defined in stat JSON assets.
 */
public class HyforgedStatInitSystem extends RefSystem<EntityStore> {

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;
    
    @Nonnull
    private final Query<EntityStore> query;

    /**
     * Default base value for ability scores when initializing a new entity.
     */
    private static final int DEFAULT_ABILITY_SCORE = 10;

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

        // Initialize default ability scores if not already set
        initializeAbilityScores(component);
        
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
     * Initialize ability scores to default values if they haven't been set.
     * <p>
     * Sets default base values for core ability scores.
     * Derived stat scaling is handled by ScalingRules in HyforgedStatComputeSystem.
     */
    private void initializeAbilityScores(@Nonnull HyforgedStatComponent component) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        
        // Set default ability score base values (10 is the standard baseline)
        int[] abilityStats = {
            registry.getIndex(CoreStats.STRENGTH),
            registry.getIndex(CoreStats.DEXTERITY),
            registry.getIndex(CoreStats.INTELLIGENCE),
            registry.getIndex(CoreStats.CONSTITUTION),
            registry.getIndex(CoreStats.WISDOM),
            registry.getIndex(CoreStats.SPIRIT),
            registry.getIndex(CoreStats.LUCK)
        };
        
        for (int statIndex : abilityStats) {
            if (statIndex >= 0 && component.getBaseValue(statIndex) == 0) {
                component.setBaseValue(statIndex, DEFAULT_ABILITY_SCORE);
            }
        }
    }
}
