package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.IntSet;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;
import reign.software.hyforged.stats.engine.ScalingEngine;
import reign.software.hyforged.stats.engine.StackingEngine;
import reign.software.hyforged.stats.event.StatBatchChangedEvent;
import reign.software.hyforged.stats.event.StatChange;
import reign.software.hyforged.stats.event.StatChangedEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapType;
    
    @Nonnull
    private final Query<EntityStore> query;
    
    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;
    
    /**
     * Reusable list for collecting modifiers per stat (reduces allocations).
     */
    private final ThreadLocal<List<HyforgedModifier>> tempModifierList = 
            ThreadLocal.withInitial(ArrayList::new);

    public HyforgedStatComputeSystem() {
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        this.entityStatMapType = EntityStatMap.getComponentType();
        this.query = Query.and(statComponentType, entityStatMapType);
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
        EntityStatMap statMap = archetypeChunk.getComponent(index, entityStatMapType);
        if (component == null) {
            return;
        }
        
        // Skip if no stats are dirty
        if (!component.hasAnyDirty()) {
            return;
        }
        
        // Get current game tick for expiration handling
        long currentTick = commandBuffer.getExternalData().getWorld().getTick();
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        
        // Remove expired modifiers first
        removeExpiredModifiers(component, currentTick, registry);
        
        // Get entity reference for events
        Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
        
        // Recompute dirty stats and collect changes
        List<StatChange> changes = recomputeDirtyStatsWithTracking(component, statMap);
        
        // Clear dirty flags after computation
        component.clearAllDirtyFlags();
        
        // Emit events for stat changes
        if (!changes.isEmpty()) {
            emitStatChangeEvents(entityRef, changes);
        }
    }
    
    /**
     * Emit stat change events for the given changes.
     * <p>
     * Emits both individual {@link StatChangedEvent} for each change and
     * a batch {@link StatBatchChangedEvent} containing all changes.
     *
     * @param entityRef The entity reference
     * @param changes List of stat changes
     */
    private void emitStatChangeEvents(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull List<StatChange> changes
    ) {
        // Get event dispatchers
        IEventDispatcher<StatChangedEvent, StatChangedEvent> individualDispatcher =
            HytaleServer.get().getEventBus().dispatchFor(StatChangedEvent.class, entityRef);
        IEventDispatcher<StatBatchChangedEvent, StatBatchChangedEvent> batchDispatcher =
            HytaleServer.get().getEventBus().dispatchFor(StatBatchChangedEvent.class, entityRef);
        
        // Emit individual events for each stat change
        for (StatChange change : changes) {
            individualDispatcher.dispatch(new StatChangedEvent(entityRef, change));
        }
        
        // Emit batch event containing all changes
        batchDispatcher.dispatch(new StatBatchChangedEvent(entityRef, changes));
    }

    /**
     * Recompute all stats that have been marked dirty, in topological order,
     * tracking changes for event emission.
     * <p>
     * This ensures that source stats are computed before stats that scale from them.
     * Dirty flags are expanded to include all transitive dependents.
     *
     * @param component The stat component to recompute
     * @return List of stat changes (old value != new value)
     */
    @Nonnull
    private List<StatChange> recomputeDirtyStatsWithTracking(
            @Nonnull HyforgedStatComponent component,
            @Nullable EntityStatMap statMap) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        List<HyforgedModifier> allModifiers = new ArrayList<>(component.getModifiers());
        if (statMap != null) {
            allModifiers.addAll(StatAccessor.getAllHyforgedModifiers(statMap));
        }
        List<StatChange> changes = new ArrayList<>();
        
        int statCount = registry.getStatCount();
        if (statCount == 0) {
            return changes;
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
            
            // Record old value before recomputation
            int oldValue = component.getCachedValue(statIdx);
            
            // Compute base value from scaling rules or stored base
            int baseValue = computeBaseValue(statIdx, statDef, component, registry);

            // Compute the new value
            int newValue = computeStatValue(statIdx, statDef, baseValue, allModifiers, component, registry);
            
            // Update cache
            component.setCachedValue(statIdx, newValue);
            
            // Track change if value actually changed
            if (oldValue != newValue) {
                changes.add(new StatChange(
                    statDef.id(),
                    statIdx,
                    oldValue,
                    newValue,
                    null // Source ID is not tracked at this level
                ));
            }
        }
        
        return changes;
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
    private int computeBaseValue(
            int statIdx,
            @Nonnull StatDefinition statDef,
            @Nonnull HyforgedStatComponent component,
            @Nonnull StatDefinitionRegistry registry
    ) {
        if (statDef.hasScaling()) {
            return ScalingEngine.computeScaledBase(
                statDef,
                component::getCachedValue,
                registry
            );
        }

        return component.getBaseValue(statIdx);
    }

    private int computeStatValue(
            int statIdx,
            @Nonnull StatDefinition statDef,
            int baseValue,
            @Nonnull List<HyforgedModifier> allModifiers,
            @Nonnull HyforgedStatComponent component,
            @Nonnull StatDefinitionRegistry registry
    ) {
        // Collect applicable modifiers
        List<HyforgedModifier> applicable = tempModifierList.get();
        applicable.clear();
        
        for (HyforgedModifier mod : allModifiers) {
            if (isModifierApplicable(mod, statIdx, statDef, registry)) {
                applicable.add(mod);
            }
        }
        
        // Compute final value using stacking engine with stat lookup for soft cap bonus stats
        return StackingEngine.compute(baseValue, applicable, statDef, statId -> {
            int bonusIdx = registry.getIndex(statId);
            return bonusIdx >= 0 ? component.getCachedValue(bonusIdx) : 0;
        });
    }

    /**
     * Check if a modifier applies to a specific stat.
     * <p>
     * A modifier applies if:
     * - It directly targets the stat (targetStatIndex matches)
     * - OR it targets a tag that includes this stat
     */
    private boolean isModifierApplicable(
            @Nonnull HyforgedModifier mod,
            int statIdx,
            @Nonnull StatDefinition statDef,
            @Nonnull StatDefinitionRegistry registry
    ) {
        // Direct targeting
        if (mod.getTargetStatIndex() == statIdx) {
            return true;
        }
        
        // Tag targeting (using Hytale AssetRegistry integer indices)
        int tagIndex = mod.getTargetTagIndex();
        if (tagIndex != HyforgedModifier.NO_TAG) {
            // Check if this stat is affected by the tag (using integer index for O(1) lookup)
            IntSet affectedStats = registry.getStatIndicesForTagIndex(tagIndex);
            if (affectedStats.contains(statIdx)) {
                return true;
            }
        }
        
        return false;
    }

    private int removeExpiredModifiers(
            @Nonnull HyforgedStatComponent component,
            long currentTick,
            @Nonnull StatDefinitionRegistry registry
    ) {
        return component.removeModifiersIf(
            modifier -> modifier.getExpirationTick() > 0 && modifier.getExpirationTick() <= currentTick,
            modifier -> markAffectedStatsDirty(component, modifier, registry)
        );
    }

    private void markAffectedStatsDirty(
            @Nonnull HyforgedStatComponent component,
            @Nonnull HyforgedModifier modifier,
            @Nonnull StatDefinitionRegistry registry
    ) {
        if (modifier.getTargetStatIndex() >= 0) {
            component.markStatDirty(modifier.getTargetStatIndex());
        }
        int tagIndex = modifier.getTargetTagIndex();
        if (tagIndex != HyforgedModifier.NO_TAG) {
            for (int statIdx : registry.getStatIndicesForTagIndex(tagIndex)) {
                component.markStatDirty(statIdx);
            }
        }
    }
}
