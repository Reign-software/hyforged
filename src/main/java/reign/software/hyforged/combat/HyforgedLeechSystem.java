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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Instant life and mana leech system.
 * <p>
 * Runs in the {@code inspectDamageGroup} after damage has been applied. When the attacker
 * has life-leech or mana-leech stats, a fraction of the damage dealt is immediately
 * recovered as health or mana.
 * <p>
 * Formula:
 * <pre>
 *   rawLeech      = damageDealt * leechBps / 10000
 *   perEventCap   = attackerMaxHP * leechRateBps / 10000   (if leech-rate-bps > 0)
 *   hardCap       = attackerMaxHP * maxLeechRateBps / 10000 (if max-life-leech-rate-bps > 0)
 *   effectiveLeech = min(rawLeech, perEventCap, hardCap)
 *   recovered     = effectiveLeech * (1 + healingEffectivenessBps / 10000)
 * </pre>
 *
 * <p>Mana leech uses the same rate/cap stats (based on max HP) but recovers mana instead.
 */
public class HyforgedLeechSystem extends DamageEventSystem {

    private static final StatId LIFE_LEECH_BPS        = StatId.hyforged("life-leech-bps");
    private static final StatId MANA_LEECH_BPS        = StatId.hyforged("mana-leech-bps");
    private static final StatId LEECH_RATE_BPS        = StatId.hyforged("leech-rate-bps");
    private static final StatId MAX_LIFE_LEECH_RATE   = StatId.hyforged("max-life-leech-rate-bps");
    private static final StatId HEALING_EFFECTIVENESS = StatId.hyforged("healing-effectiveness-bps");

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    // Cached stat indices (set on first use)
    private int lifeLeechIndex        = -1;
    private int manaLeechIndex        = -1;
    private int leechRateIndex        = -1;
    private int maxLifeLeechRateIndex = -1;
    private int healingEffIndex       = -1;
    private boolean indicesCached     = false;

    public HyforgedLeechSystem() {
        this.query = StatAccessor.getStatMapType();
        // Run in inspect group after damage has been applied
        this.dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, DamageSystems.ApplyDamage.class)
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
        // Skip cancelled or zero-damage events
        if (damage.isCancelled() || damage.getAmount() <= 0) {
            return;
        }

        // Skip if already processed by CombatService
        Boolean pipelineProcessed = damage.getIfPresentMetaObject(CombatMeta.PIPELINE_PROCESSED);
        if (pipelineProcessed != null && pipelineProcessed) {
            return;
        }

        // Leech only applies to entity-sourced attacks
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (!attackerRef.isValid()) {
            return;
        }

        // Bypass-resistance damage (environmental etc.) does not leech
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause == null || damageCause.doesBypassResistances()) {
            return;
        }

        ensureIndicesCached();

        // Check if attacker has any leech stats
        int lifeLeechBps = lifeLeechIndex >= 0
                ? StatAccessor.getStatValueInt(store, attackerRef, lifeLeechIndex) : 0;
        int manaLeechBps = manaLeechIndex >= 0
                ? StatAccessor.getStatValueInt(store, attackerRef, manaLeechIndex) : 0;

        if (lifeLeechBps <= 0 && manaLeechBps <= 0) {
            return;
        }

        // Get attacker's EntityStatMap for HP/Mana and cap calculations
        EntityStatMap attackerStatMap = store.getComponent(attackerRef, EntityStatMap.getComponentType());
        if (attackerStatMap == null) {
            return;
        }

        float damageDealt = damage.getAmount();

        // Compute per-event leech cap from leech-rate-bps and max-life-leech-rate-bps
        float leechCap = Float.MAX_VALUE;
        int leechRateBps = leechRateIndex >= 0
                ? StatAccessor.getStatValueInt(store, attackerRef, leechRateIndex) : 0;
        if (leechRateBps > 0) {
            EntityStatValue hpStat = attackerStatMap.get(DefaultEntityStatTypes.getHealth());
            if (hpStat != null) {
                float maxHp = hpStat.getMax();
                leechCap = maxHp * leechRateBps / (float) CombatMath.BPS_100;
            }
        }

        int maxLeechRateBps = maxLifeLeechRateIndex >= 0
                ? StatAccessor.getStatValueInt(store, attackerRef, maxLifeLeechRateIndex) : 0;
        if (maxLeechRateBps > 0) {
            EntityStatValue hpStat = attackerStatMap.get(DefaultEntityStatTypes.getHealth());
            if (hpStat != null) {
                float maxHp = hpStat.getMax();
                float hardCap = maxHp * maxLeechRateBps / (float) CombatMath.BPS_100;
                leechCap = Math.min(leechCap, hardCap);
            }
        }

        // Read healing effectiveness multiplier
        float healingMult = 1.0f;
        if (healingEffIndex >= 0) {
            int healingBps = StatAccessor.getStatValueInt(store, attackerRef, healingEffIndex);
            if (healingBps != 0) {
                healingMult = 1.0f + healingBps / (float) CombatMath.BPS_100;
            }
        }

        // Apply life leech
        if (lifeLeechBps > 0 && StatAccessor.hasStatSlot(attackerStatMap, DefaultEntityStatTypes.getHealth())) {
            float rawLeech  = damageDealt * lifeLeechBps / (float) CombatMath.BPS_100;
            float capped    = leechCap < Float.MAX_VALUE ? Math.min(rawLeech, leechCap) : rawLeech;
            float recovered = Math.max(0, capped * healingMult);
            if (recovered > 0) {
                attackerStatMap.addStatValue(EntityStatMap.Predictable.SELF,
                        DefaultEntityStatTypes.getHealth(), recovered);
            }
        }

        // Apply mana leech
        if (manaLeechBps > 0 && StatAccessor.hasStatSlot(attackerStatMap, DefaultEntityStatTypes.getMana())) {
            float rawLeech  = damageDealt * manaLeechBps / (float) CombatMath.BPS_100;
            float capped    = leechCap < Float.MAX_VALUE ? Math.min(rawLeech, leechCap) : rawLeech;
            float recovered = Math.max(0, capped);
            if (recovered > 0) {
                attackerStatMap.addStatValue(EntityStatMap.Predictable.SELF,
                        DefaultEntityStatTypes.getMana(), recovered);
            }
        }
    }

    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        lifeLeechIndex        = registry.getIndex(LIFE_LEECH_BPS);
        manaLeechIndex        = registry.getIndex(MANA_LEECH_BPS);
        leechRateIndex        = registry.getIndex(LEECH_RATE_BPS);
        maxLifeLeechRateIndex = registry.getIndex(MAX_LIFE_LEECH_RATE);
        healingEffIndex       = registry.getIndex(HEALING_EFFECTIVENESS);
        indicesCached         = true;
    }
}
