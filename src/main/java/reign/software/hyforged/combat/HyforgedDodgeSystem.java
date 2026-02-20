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
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedConfig;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Dodge system.
 * <p>
 * <b>This system is fully inactive until the {@code dodgeEnabled} config flag is set to
 * {@code true} via {@link HyforgedConfig#setDodgeEnabled(boolean)}.</b>
 * <p>
 * When enabled, this system runs in the {@code filterDamageGroup} after evasion has been
 * resolved by {@link HyforgedHitResolutionSystem}. It rolls {@code hyforged:dodge-chance-bps}
 * for the defender; if triggered, the incoming damage event is completely cancelled — a full
 * dodge.
 * <p>
 * A dodge is distinct from a miss (evasion-driven). Misses are set by
 * {@link HyforgedHitResolutionSystem#MISS}; dodges are marked by
 * {@link CombatMeta#DODGE_ROLLED} so the combat log can display them differently.
 * <p>
 * Environmental damage (anything that bypasses resistances) cannot be dodged.
 */
public class HyforgedDodgeSystem extends DamageEventSystem {

    /** Dodge chance stat ID (defender). */
    private static final StatId DODGE_CHANCE = StatId.hyforged("dodge-chance-bps");

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    // Cached stat index
    private int dodgeChanceIndex = -1;
    private boolean indicesCached = false;

    public HyforgedDodgeSystem() {
        this.query = StatAccessor.getStatMapType();
        // Run in filter group after hit resolution (same ordering as HyforgedAutoBlockSystem)
        this.dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, HyforgedHitResolutionSystem.class),
            new SystemDependency<>(Order.BEFORE, DamageSystems.ApplyDamage.class)
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
        // Feature flag guard — no gameplay impact until explicitly enabled
        if (!HyforgedConfig.get().isDodgeEnabled()) {
            return;
        }

        // Skip cancelled or zero-damage events
        if (damage.isCancelled() || damage.getAmount() <= 0) {
            return;
        }

        // Skip if already processed by CombatService
        Boolean pipelineProcessed = damage.getIfPresentMetaObject(CombatMeta.PIPELINE_PROCESSED);
        if (pipelineProcessed != null && pipelineProcessed) {
            return;
        }

        // Dodge only applies to entity-sourced attacks
        if (!(damage.getSource() instanceof Damage.EntitySource)) {
            return;
        }

        // Environmental damage (fall damage, etc.) cannot be dodged
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause == null || damageCause.doesBypassResistances()) {
            return;
        }

        ensureIndicesCached();

        int dodgeChanceBps = dodgeChanceIndex >= 0
                ? StatAccessor.getStatValueInt(archetypeChunk, index, dodgeChanceIndex) : 0;
        if (dodgeChanceBps <= 0) {
            return;
        }

        if (CombatMath.rollChance(dodgeChanceBps)) {
            // Full dodge — cancel the damage and mark so combat log can display "Dodge"
            damage.putMetaObject(CombatMeta.DODGE_ROLLED, Boolean.TRUE);
            damage.setCancelled(true);
        }
    }

    /**
     * Cache stat index on first use.
     */
    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }
        dodgeChanceIndex = StatDefinitionRegistry.get().getIndex(DODGE_CHANCE);
        indicesCached    = true;
    }
}
