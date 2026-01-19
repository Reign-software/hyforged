package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.component.StatModifier;
import reign.software.hyforged.stats.engine.ScalingEngine;
import reign.software.hyforged.stats.engine.StackingEngine;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

/**
 * ECS System for computing stat values when dirty flags are set.
 * <p>
 * This EntityTickingSystem processes entities with HyforgedStatComponent
 * and recomputes stats that have been marked dirty.
 * <p>
 * Following ECS principles, this system contains only processing logic.
 * All data is stored in the HyforgedStatComponent.
 */
public class HyforgedStatComputeSystem extends EntityTickingSystem<EntityStore> {

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;
    
    @Nonnull
    private final Query<EntityStore> query;
    
    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;
    
    /**
     * Reusable list for collecting modifiers per stat (reduces allocations).
     */
    private final ThreadLocal<List<StatModifier>> tempModifierList = 
            ThreadLocal.withInitial(ArrayList::new);

    public HyforgedStatComputeSystem() {
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        this.query = statComponentType;
        // Run after init system
        this.dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, HyforgedStatInitSystem.class)
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
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        HyforgedStatComponent component = archetypeChunk.getComponent(index, statComponentType);
        if (component == null) {
            return;
        }
        
        // Skip if no stats are dirty
        if (!component.hasAnyDirty()) {
            return;
        }
        
        // Get current game tick for expiration handling
        long currentTick = commandBuffer.getExternalData().getWorld().getTick();
        
        // Remove expired modifiers first
        component.removeExpiredModifiers(currentTick);
        
        // Recompute dirty stats
        recomputeDirtyStats(component);
        
        // Clear dirty flags after computation
        component.clearAllDirtyFlags();
    }

    /**
     * Recompute all stats that have been marked dirty, in topological order.
     * <p>
     * This ensures that source stats are computed before stats that scale from them.
     * Dirty flags are expanded to include all transitive dependents.
     */
    private void recomputeDirtyStats(@Nonnull HyforgedStatComponent component) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        List<StatModifier> allModifiers = component.getModifiers();
        
        int statCount = registry.getStatCount();
        if (statCount == 0) {
            return;
        }
        
        // Build expanded dirty set (includes transitive dependents)
        BitSet expandedDirty = expandDirtyStats(component, registry, statCount);
        
        // Get evaluation order (topological sort: sources before dependents)
        int[] evalOrder = registry.getEvaluationOrder();
        
        // Process stats in topological order
        for (int statIdx : evalOrder) {
            if (!expandedDirty.get(statIdx)) {
                continue; // Skip non-dirty stats
            }
            
            StatDefinition statDef = registry.getStat(statIdx);
            if (statDef == null) {
                continue; // Shouldn't happen, but be defensive
            }
            
            // Compute the new value
            int newValue = computeStatValue(statIdx, statDef, allModifiers, component, registry);
            
            // Update cache
            component.setCachedValue(statIdx, newValue);
        }
    }
    
    /**
     * Expand dirty flags to include all transitive dependents.
     * <p>
     * If stat A is dirty and stat B depends on A (scales from A),
     * then B must also be recomputed.
     */
    private BitSet expandDirtyStats(
            @Nonnull HyforgedStatComponent component,
            @Nonnull StatDefinitionRegistry registry,
            int statCount
    ) {
        BitSet expanded = new BitSet(statCount);
        
        // Start with directly dirty stats
        for (int statIdx = 0; statIdx < statCount; statIdx++) {
            if (component.isStatDirty(statIdx)) {
                expanded.set(statIdx);
            }
        }
        
        // Expand to include dependents (BFS from dirty stats)
        // Use evaluation order to ensure we process in correct order
        int[] evalOrder = registry.getEvaluationOrder();
        for (int statIdx : evalOrder) {
            if (expanded.get(statIdx)) {
                // This stat is dirty, so all its dependents must also be dirty
                for (int dependent : registry.getDependentStats(statIdx)) {
                    expanded.set(dependent);
                }
            }
        }
        
        return expanded;
    }

    /**
     * Compute the value for a single stat.
     * <p>
     * This handles both scaling and non-scaling stats:
     * - For stats with scaling: compute base from source stats using ScalingEngine
     * - For stats without scaling: use component's base value or stat's defaultValue
     * <p>
     * Then collects all applicable modifiers (direct and tag-based)
     * and applies them using the StackingEngine.
     */
    private int computeStatValue(
            int statIdx,
            @Nonnull StatDefinition statDef,
            @Nonnull List<StatModifier> allModifiers,
            @Nonnull HyforgedStatComponent component,
            @Nonnull StatDefinitionRegistry registry
    ) {
        // Compute base value
        int baseValue;
        
        if (statDef.hasScaling()) {
            // Stat derives its base from other stats via scaling rules
            // Source value provider reads from cached values (already computed in topo order)
            baseValue = ScalingEngine.computeScaledBase(
                statDef,
                component::getCachedValue,
                registry
            );
        } else {
            // Stat uses stored base value or defaultValue
            baseValue = component.getBaseValue(statIdx);
        }
        
        // Collect applicable modifiers
        List<StatModifier> applicable = tempModifierList.get();
        applicable.clear();
        
        for (StatModifier mod : allModifiers) {
            if (isModifierApplicable(mod, statIdx, statDef, registry)) {
                applicable.add(mod);
            }
        }
        
        // Compute final value using stacking engine
        return StackingEngine.compute(baseValue, applicable, statDef);
    }

    /**
     * Check if a modifier applies to a specific stat.
     * <p>
     * A modifier applies if:
     * - It directly targets the stat (targetStatIndex matches)
     * - OR it targets a tag that includes this stat
     */
    private boolean isModifierApplicable(
            @Nonnull StatModifier mod,
            int statIdx,
            @Nonnull StatDefinition statDef,
            @Nonnull StatDefinitionRegistry registry
    ) {
        // Direct targeting
        if (mod.targetStatIndex() == statIdx) {
            return true;
        }
        
        // Tag targeting
        String tagId = mod.targetTagId();
        if (tagId != null) {
            // Check if this stat is affected by the tag
            Set<Integer> affectedStats = registry.getStatIndicesForTag(tagId);
            if (affectedStats.contains(statIdx)) {
                return true;
            }
            
            // Also check if the stat has this tag in its definition
            if (statDef.tags().contains(tagId)) {
                return true;
            }
        }
        
        return false;
    }
}
