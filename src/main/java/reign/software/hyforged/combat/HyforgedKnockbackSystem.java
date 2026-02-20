package reign.software.hyforged.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedConfig;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Applies knockback to defenders when the attacker rolls {@code hyforged:knockback-chance-bps}.
 * <p>
 * Runs in the {@code inspectDamageGroup} (after damage has been applied) and checks
 * the attacker's knockback chance stat. If triggered, the defender is knocked away from
 * the attacker using Hytale's {@link KnockbackComponent}.
 * <p>
 * Supported stats (attacker):
 * <ul>
 *   <li>{@code hyforged:knockback-chance-bps} — chance to knockback on hit</li>
 *   <li>{@code hyforged:knockback-distance-bps} — scales base knockback velocity</li>
 * </ul>
 * Supported stats (defender):
 * <ul>
 *   <li>{@code hyforged:knockback-resistance-bps} — reduces effective knockback velocity</li>
 * </ul>
 * Base knockback velocity and duration are configurable via
 * {@link reign.software.hyforged.HyforgedConfig#getBaseKnockbackVelocity()} and
 * {@link reign.software.hyforged.HyforgedConfig#getBaseKnockbackDurationSeconds()}.
 */
public class HyforgedKnockbackSystem extends DamageEventSystem {

    private static final int BPS_100_PERCENT = 10000;

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    // Stat IDs — resolved lazily on first event
    private static final StatId KNOCKBACK_CHANCE_BPS    = StatId.hyforged("knockback-chance-bps");
    private static final StatId KNOCKBACK_DISTANCE_BPS  = StatId.hyforged("knockback-distance-bps");
    private static final StatId KNOCKBACK_RESISTANCE_BPS = StatId.hyforged("knockback-resistance-bps");

    // Cached indices
    private int knockbackChanceIndex    = -1;
    private int knockbackDistanceIndex  = -1;
    private int knockbackResistanceIndex = -1;
    private boolean indicesCached = false;

    public HyforgedKnockbackSystem() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.query = EntityStatMap.getComponentType();
        this.dependencies = Set.of(
                new SystemDependency<>(Order.BEFORE, DamageSystems.EntityUIEvents.class)
        );
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
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
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damage
    ) {
        // Skip cancelled or zero damage
        if (damage.isCancelled() || damage.getAmount() <= 0) {
            return;
        }

        // Skip if already processed by CombatService (avoids re-applying pipeline)
        Boolean pipelineProcessed = damage.getIfPresentMetaObject(CombatMeta.PIPELINE_PROCESSED);
        if (pipelineProcessed != null && pipelineProcessed) {
            return;
        }

        // Only entity-sourced damage has an attacker
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return;
        }

        // Skip self-damage
        Ref<EntityStore> defenderRef = archetypeChunk.getReferenceTo(index);
        if (attackerRef == defenderRef) {
            return;
        }

        ensureIndicesCached();

        if (knockbackChanceIndex < 0) {
            return;
        }

        // Check attacker's knockback chance
        HyforgedStatComponent attackerStats = store.getComponent(attackerRef, statComponentType);
        if (attackerStats == null) {
            return;
        }

        int knockbackChanceBps = attackerStats.getCachedValue(knockbackChanceIndex);
        if (knockbackChanceBps <= 0) {
            return;
        }

        // Roll knockback chance
        if (!CombatRandom.rollChance(knockbackChanceBps)) {
            return;
        }

        // Compute effective knockback velocity from attacker distance bonus
        float baseVelocity = HyforgedConfig.get().getBaseKnockbackVelocity();
        int knockbackDistanceBps = knockbackDistanceIndex >= 0
                ? attackerStats.getCachedValue(knockbackDistanceIndex)
                : 0;

        // Factor in defender knockback resistance
        HyforgedStatComponent defenderStats = archetypeChunk.getComponent(index, statComponentType);
        int knockbackResistanceBps = (knockbackResistanceIndex >= 0 && defenderStats != null)
                ? defenderStats.getCachedValue(knockbackResistanceIndex)
                : 0;

        // effectiveVelocity = base * (1 + distanceBps/10000) * (1 - resistanceBps/10000)
        float effectiveVelocity = baseVelocity
                * (1.0f + knockbackDistanceBps / (float) BPS_100_PERCENT)
                * (1.0f - knockbackResistanceBps / (float) BPS_100_PERCENT);

        if (effectiveVelocity <= 0.0f) {
            return;
        }

        // Get entity positions for horizontal direction calculation
        TransformComponent attackerTransform = store.getComponent(attackerRef, TransformComponent.getComponentType());
        TransformComponent defenderTransform = store.getComponent(defenderRef, TransformComponent.getComponentType());
        if (attackerTransform == null || defenderTransform == null) {
            return;
        }

        Vector3d attackerPos = attackerTransform.getPosition();
        Vector3d defenderPos = defenderTransform.getPosition();

        double dx = defenderPos.x - attackerPos.x;
        double dz = defenderPos.z - attackerPos.z;
        double len = Math.sqrt(dx * dx + dz * dz);

        // If entities overlap exactly, knock in +X as a safe default
        if (len < 0.001) {
            dx = 1.0;
            dz = 0.0;
            len = 1.0;
        }

        // Build knockback velocity: horizontal + slight upward component
        Vector3d knockbackVelocity = new Vector3d(
                (dx / len) * effectiveVelocity,
                0.1 * effectiveVelocity,
                (dz / len) * effectiveVelocity
        );

        // Apply via KnockbackComponent — uses Hytale's built-in velocity dispatch
        KnockbackComponent knockbackComponent = store.ensureAndGetComponent(
                defenderRef, KnockbackComponent.getComponentType());
        knockbackComponent.setVelocity(knockbackVelocity);
        knockbackComponent.setVelocityType(ChangeVelocityType.Add);
        knockbackComponent.setDuration(HyforgedConfig.get().getBaseKnockbackDurationSeconds());
    }

    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        knockbackChanceIndex    = registry.getIndex(KNOCKBACK_CHANCE_BPS);
        knockbackDistanceIndex  = registry.getIndex(KNOCKBACK_DISTANCE_BPS);
        knockbackResistanceIndex = registry.getIndex(KNOCKBACK_RESISTANCE_BPS);
        indicesCached = true;
    }
}
