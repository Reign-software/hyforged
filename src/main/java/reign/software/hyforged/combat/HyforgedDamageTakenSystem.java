package reign.software.hyforged.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.bridge.HyforgedDamageReductionSystem;
import reign.software.hyforged.stats.damage.DamageTypeExtensionRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies incoming damage multipliers from defender stats to damage events.
 * <p>
 * This system runs in the {@code filterDamageGroup} (defender-side), after
 * {@link HyforgedDamageReductionSystem} has applied resistance-based reduction.
 * A positive stat value increases damage taken; negative reduces it.
 * <p>
 * Applied as independent MORE multipliers:
 * <ul>
 *   <li>Global: {@code hyforged:damage-taken-bps} — applies to all damage</li>
 *   <li>Elemental group: {@code hyforged:elemental-damage-taken-bps} — fire/ice/lightning</li>
 *   <li>Physical: {@code hyforged:physical-damage-taken-bps}</li>
 *   <li>Chaos: {@code hyforged:chaos-damage-taken-bps}</li>
 * </ul>
 * <p>
 * BPS formula: {@code finalDamage = currentDamage * (1 + bps / 10000)}.
 * Example: {@code damage-taken-bps = 1000} → 10% more damage taken.
 * Example: {@code physical-damage-taken-bps = -2000} → 20% less physical damage taken.
 */
public class HyforgedDamageTakenSystem extends DamageEventSystem {

    // ---- Global (applies to all incoming non-bypass damage) ----
    private static final StatId DAMAGE_TAKEN = StatId.hyforged("damage-taken-bps");

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    // Cached stat indices
    private int damageTakenIndex        = -1;
    private boolean globalIndexCached   = false;

    /**
     * Per-damage-cause index arrays for element-driven taken stats.
     * Populated lazily on first encounter of each damage cause.
     */
    private final Map<String, int[]> elementTakenIndices = new HashMap<>();

    public HyforgedDamageTakenSystem() {
        this.query = StatAccessor.getStatMapType();
        // Run in filter group, after resistance reduction
        this.dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, HyforgedDamageReductionSystem.class)
        );
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
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
        // Skip cancelled or zero-damage events
        if (damage.isCancelled() || damage.getAmount() <= 0) {
            return;
        }

        // Skip if already processed by CombatService
        Boolean pipelineProcessed = damage.getIfPresentMetaObject(CombatMeta.PIPELINE_PROCESSED);
        if (pipelineProcessed != null && pipelineProcessed) {
            return;
        }

        // Bypass-resistance damage skips taken modifiers as well
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause == null || damageCause.doesBypassResistances()) {
            return;
        }

        ensureGlobalIndexCached();

        float multiplier = 1.0f;

        // 1. Global damage-taken modifier (applies to ALL incoming damage)
        int globalBps = damageTakenIndex >= 0
                ? StatAccessor.getStatValueInt(archetypeChunk, index, damageTakenIndex) : 0;
        if (globalBps != 0) {
            multiplier *= (1.0f + globalBps / (float) CombatMath.BPS_100);
        }

        // 2. Element-driven taken stats — data-driven via DamageTypeExtensionRegistry
        // Each registered JSON level in the inheritance chain contributes one stat.
        // Example: fire damage applies elemental-damage-taken-bps (from Elemental.json).
        int[] elemIndices = getOrCacheElementTakenIndices(damageCause);
        for (int idx : elemIndices) {
            if (idx < 0) continue;
            int bps = StatAccessor.getStatValueInt(archetypeChunk, index, idx);
            if (bps != 0) multiplier *= (1.0f + bps / (float) CombatMath.BPS_100);
        }

        // Apply combined multiplier if non-trivial
        if (multiplier != 1.0f) {
            float modified = damage.getAmount() * multiplier;
            damage.setAmount(Math.max(0, modified));
        }
    }

    /**
     * Cache the global damage-taken index on first use.
     */
    private void ensureGlobalIndexCached() {
        if (globalIndexCached) {
            return;
        }
        damageTakenIndex  = StatDefinitionRegistry.get().getIndex(DAMAGE_TAKEN);
        globalIndexCached = true;
    }

    /**
     * Return a cached array of stat indices for element-driven taken stats associated with
     * {@code damageCause}. Built once per unique damage cause via
     * {@link DamageTypeExtensionRegistry#getDamageTakenStatsForDamage}, so new damage types
     * only require a JSON change.
     */
    @Nonnull
    private int[] getOrCacheElementTakenIndices(@Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        int[] cached = elementTakenIndices.get(id);
        if (cached != null) {
            return cached;
        }
        List<StatId> stats = DamageTypeExtensionRegistry.get().getDamageTakenStatsForDamage(damageCause);
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int[] indices = new int[stats.size()];
        for (int i = 0; i < stats.size(); i++) {
            indices[i] = registry.getIndex(stats.get(i));
        }
        elementTakenIndices.put(id, indices);
        return indices;
    }
}
