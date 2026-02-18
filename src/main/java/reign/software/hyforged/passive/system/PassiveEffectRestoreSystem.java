package reign.software.hyforged.passive.system;

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
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.service.PassiveTreeService;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.system.HyforgedStatInitSystem;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.logging.Level;

/**
 * ECS System that restores passive tree effects when an entity is loaded.
 * <p>
 * Passive tree modifiers are NOT persisted with HyforgedStatComponent — only
 * the allocation data is persisted in PassiveTreeComponent. When a player logs
 * in or the entity is added to the store, this system iterates all allocated
 * nodes and re-applies their effects (stat modifiers, spell grants, etc.)
 * via the registered {@link reign.software.hyforged.passive.effect.PassiveEffectHandler}s.
 * <p>
 * Runs after {@link HyforgedStatInitSystem} to ensure base ability scores are
 * set before modifiers are layered on top.
 */
public class PassiveEffectRestoreSystem extends RefSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final ComponentType<EntityStore, PassiveTreeComponent> passiveTreeComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    public PassiveEffectRestoreSystem() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.passiveTreeComponentType = plugin.getPassiveTreeComponentType();
        this.query = Query.and(statComponentType, passiveTreeComponentType);
        // Run after stat init so ability scores are already set
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
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        PassiveTreeComponent passiveComponent = commandBuffer.getComponent(ref, passiveTreeComponentType);
        if (passiveComponent == null) {
            return;
        }

        // Only restore if there are allocations to restore
        int generalCount = passiveComponent.getGeneralAllocatedCount();
        int classCount = passiveComponent.getClassIdsWithAllocations().stream()
                .mapToInt(passiveComponent::getClassAllocatedCount)
                .sum();

        if (generalCount == 0 && classCount == 0) {
            return;
        }

        int restored = PassiveTreeService.get().restoreAllEffects(ref, passiveComponent);

        LOGGER.at(Level.FINE).log(
                "Restored %d passive effects for entity (general=%d, class=%d)",
                restored, generalCount, classCount
        );
    }

    @Override
    public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No cleanup needed - modifiers are discarded with the entity
    }
}
