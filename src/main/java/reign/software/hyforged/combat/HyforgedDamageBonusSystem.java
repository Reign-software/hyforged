package reign.software.hyforged.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
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
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.damage.DamageTypeExtensionRegistry;

import com.hypixel.hytale.logger.HytaleLogger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Applies outgoing damage bonuses from attacker stats to damage events.
 * <p>
 * This system runs in the {@code gatherDamageGroup} (attacker-side), before crit and resistance,
 * applying percentage-based MORE multipliers for:
 * <ul>
 *   <li>Global damage: {@code hyforged:damage-increased-bps}</li>
 *   <li>Element-specific: fire/cold/lightning/physical/chaos/bleed/poison increased damage</li>
 *   <li>Elemental group: {@code hyforged:elemental-damage-increased-bps} for fire/ice/lightning</li>
 *   <li>Projectile: {@code hyforged:projectile-damage-bps} for {@link Damage.ProjectileSource}</li>
 *   <li>Melee: {@code hyforged:melee-damage-increased-bps} for direct entity attacks</li>
 * </ul>
 * <p>
 * BPS formula: {@code finalDamage = baseDamage * (1 + bps / 10000)}.
 * Each bonus is an independent MORE multiplier: multiplied together, not added.
 * <p>
 * Attack/spell/area/dot/minion detection is not yet supported without additional
 * meta keys; those stats are accumulated and will be applied in a future phase.
 */
public class HyforgedDamageBonusSystem extends DamageEventSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // ---- Global (applies to all entity-sourced damage) ----
    private static final StatId GLOBAL_DAMAGE = StatId.hyforged("damage-increased-bps");

    // ---- Mechanic-based (not element-driven; checked via source type) ----
    private static final StatId PROJECTILE_DAMAGE = StatId.hyforged("projectile-damage-bps");
    private static final StatId MELEE_DAMAGE      = StatId.hyforged("melee-damage-increased-bps");

    // ---- Special rolls ----
    private static final StatId DOUBLE_DAMAGE_CHANCE  = StatId.hyforged("chance-to-deal-double-damage-bps");
    private static final StatId CULLING_STRIKE        = StatId.hyforged("culling-strike-threshold-bps");

    /**
     * TODO: Wire stun-duration-bps into a dedicated stun system once a stun application
     * system exists. The index is cached here so it is not -1 at runtime and the stat
     * can be referenced without a compile-time constant in a future system.
     *
     * @see <a href="hyforged:stun-duration-bps">StunDuration stat definition</a>
     */
    private static final StatId STUN_DURATION = StatId.hyforged("stun-duration-bps");

    /**
     * Intimidate effect stat — cached for future wiring.
     * A debug log fires at FINE level when the attacker has a nonzero value so the stat
     * can be observed before the mechanic is fully designed.
     * TODO: Intimidate mechanic pending design — wire into a dedicated system once finalized.
     */
    private static final StatId INTIMIDATE_EFFECT = StatId.hyforged("intimidate-effect-bps");

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    // Cached static stat indices (set on first globalIndicesCached check)
    private int globalDamageIndex       = -1;
    private int projectileDamageIndex   = -1;
    private int meleeDamageIndex        = -1;
    private int doubleDamageChanceIndex = -1;
    private int cullingStrikeIndex      = -1;
    @SuppressWarnings("unused")
    private int stunDurationIndex       = -1; // TODO: used by future stun system
    private int intimidateEffectIndex   = -1;
    private boolean globalIndicesCached = false;

    /**
     * Per-damage-cause index arrays for element-driven bonus stats.
     * Populated lazily on first encounter of each damage cause.
     * Each array entry is a stat index from {@link StatDefinitionRegistry};
     * all are applied as independent MORE multipliers.
     */
    private final Map<String, int[]> elementBonusIndices = new HashMap<>();

    public HyforgedDamageBonusSystem() {
        this.query = StatAccessor.getStatMapType();
        // Run in gather group before ApplyDamage, same as HyforgedHitResolutionSystem
        this.dependencies = Set.of(
            new SystemDependency<>(Order.BEFORE, DamageSystems.ApplyDamage.class)
        );
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getGatherDamageGroup();
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

        // Damage bonuses only apply to entity-sourced attacks
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (!attackerRef.isValid()) {
            return;
        }

        // Bypass-resistance damage (e.g. environmental) skips bonuses
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause == null || damageCause.doesBypassResistances()) {
            return;
        }

        ensureGlobalIndicesCached();

        float multiplier = 1.0f;

        // 1. Global damage bonus (applies to ALL damage)
        int globalBps = globalDamageIndex >= 0
                ? StatAccessor.getStatValueInt(store, attackerRef, globalDamageIndex) : 0;
        if (globalBps != 0) {
            multiplier *= (1.0f + globalBps / (float) CombatMath.BPS_100);
        }

        // 2. Element-driven bonus stats — data-driven via DamageTypeExtensionRegistry
        // Each registered JSON level in the inheritance chain contributes one stat.
        // Example: fire damage applies both fire-damage-increased-bps AND elemental-damage-increased-bps.
        int[] elemIndices = getOrCacheElementBonusIndices(damageCause);
        for (int idx : elemIndices) {
            if (idx < 0) continue;
            int bps = StatAccessor.getStatValueInt(store, attackerRef, idx);
            if (bps != 0) multiplier *= (1.0f + bps / (float) CombatMath.BPS_100);
        }

        // 3. Mechanic-based bonuses (by damage source type, not element)
        if (damage.getSource() instanceof Damage.ProjectileSource) {
            // Projectile attack
            int bps = projectileDamageIndex >= 0
                    ? StatAccessor.getStatValueInt(store, attackerRef, projectileDamageIndex) : 0;
            if (bps != 0) multiplier *= (1.0f + bps / (float) CombatMath.BPS_100);
        } else {
            // Non-projectile entity attack; treat as melee
            int bps = meleeDamageIndex >= 0
                    ? StatAccessor.getStatValueInt(store, attackerRef, meleeDamageIndex) : 0;
            if (bps != 0) multiplier *= (1.0f + bps / (float) CombatMath.BPS_100);
        }

        // Apply combined multiplier if non-trivial
        if (multiplier != 1.0f) {
            damage.setAmount(damage.getAmount() * multiplier);
        }

        // 4. Double-damage roll
        // If the attacker has chance-to-deal-double-damage-bps and the roll succeeds,
        // double the final damage and record it in combat meta.
        if (doubleDamageChanceIndex >= 0) {
            int chanceBps = StatAccessor.getStatValueInt(store, attackerRef, doubleDamageChanceIndex);
            if (chanceBps > 0 && CombatMath.rollChance(chanceBps)) {
                damage.setAmount(damage.getAmount() * 2.0f);
                damage.putMetaObject(CombatMeta.DOUBLE_DAMAGE, Boolean.TRUE);
            }
        }

        // 5. Culling strike
        // If the attacker has culling-strike-threshold-bps, check if the defender's
        // current HP is at or below the threshold; if so, set damage to exactly the
        // defender's remaining HP (guaranteed kill).
        if (cullingStrikeIndex >= 0) {
            int thresholdBps = StatAccessor.getStatValueInt(store, attackerRef, cullingStrikeIndex);
            if (thresholdBps > 0) {
                EntityStatMap defenderStatMap = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
                if (defenderStatMap != null) {
                    EntityStatValue hpStat = defenderStatMap.get(DefaultEntityStatTypes.getHealth());
                    if (hpStat != null && hpStat.getMax() > 0) {
                        float currentHp = hpStat.get();
                        float maxHp     = hpStat.getMax();
                        int hpPctBps    = (int) (currentHp / maxHp * CombatMath.BPS_100);
                        if (hpPctBps <= thresholdBps) {
                            // Set damage equal to current HP to guarantee a kill
                            damage.setAmount(currentHp);
                        }
                    }
                }
            }
        }

        // 6. Intimidate — stat index cached; mechanic pending design.
        // Log at FINE when the attacker has a nonzero value so it can be observed in server
        // logs before the full mechanic is wired into a dedicated system.
        if (intimidateEffectIndex >= 0) {
            int intimidateBps = StatAccessor.getStatValueInt(store, attackerRef, intimidateEffectIndex);
            if (intimidateBps != 0) {
                LOGGER.at(Level.FINE).log(
                        "[Intimidate] entity %s has intimidate-effect-bps=%d — mechanic pending design",
                        attackerRef, intimidateBps);
            }
        }
    }

    /**
     * Cache the three global/mechanic stat indices on first use.
     */
    private void ensureGlobalIndicesCached() {
        if (globalIndicesCached) {
            return;
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        globalDamageIndex       = registry.getIndex(GLOBAL_DAMAGE);
        projectileDamageIndex   = registry.getIndex(PROJECTILE_DAMAGE);
        meleeDamageIndex        = registry.getIndex(MELEE_DAMAGE);
        doubleDamageChanceIndex = registry.getIndex(DOUBLE_DAMAGE_CHANCE);
        cullingStrikeIndex      = registry.getIndex(CULLING_STRIKE);
        stunDurationIndex       = registry.getIndex(STUN_DURATION);
        intimidateEffectIndex   = registry.getIndex(INTIMIDATE_EFFECT);
        globalIndicesCached     = true;
    }

    /**
     * Return a cached array of stat indices for element-driven bonus stats associated with
     * {@code damageCause}. The array is built once per unique damage cause by querying
     * {@link DamageTypeExtensionRegistry#getDamageBonusStatsForDamage} which walks the full
     * inheritance chain, so adding a new damage type or new bonus stat only requires a JSON change.
     */
    @Nonnull
    private int[] getOrCacheElementBonusIndices(@Nonnull DamageCause damageCause) {
        String id = damageCause.getId();
        int[] cached = elementBonusIndices.get(id);
        if (cached != null) {
            return cached;
        }
        List<StatId> stats = DamageTypeExtensionRegistry.get().getDamageBonusStatsForDamage(damageCause);
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int[] indices = new int[stats.size()];
        for (int i = 0; i < stats.size(); i++) {
            indices[i] = registry.getIndex(stats.get(i));
        }
        elementBonusIndices.put(id, indices);
        return indices;
    }
}
