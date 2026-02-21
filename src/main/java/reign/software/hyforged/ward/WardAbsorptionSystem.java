package reign.software.hyforged.ward;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.combat.CombatMeta;
import reign.software.hyforged.stats.StatAccessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Intercepts incoming damage and absorbs it into the entity's Ward pool.
 * <p>
 * Ward acts as a flat hit-point buffer that absorbs damage before life. Only
 * entities with a non-zero Ward max (set by the bridge from {@code hyforged:max-ward-flat})
 * are affected. The absorption happens after all other mitigation systems
 * (armor, evasion, block, suppression) but before the damage is applied to HP.
 * <p>
 * Ward does NOT regenerate passively; it is restored through the
 * {@link WardRestoreOnCastListener} when the player casts a skill.
 * <p>
 * Damage that bypasses resistances (e.g. true damage) also bypasses Ward.
 */
public class WardAbsorptionSystem extends DamageEventSystem {

    @Nonnull
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    // Lazily cached EntityStat index for Ward
    private int wardEntityStatIndex = -1;
    private boolean indexInitialized = false;

    public WardAbsorptionSystem() {
        this.entityStatMapType = EntityStatMap.getComponentType();
        this.query = Query.and(entityStatMapType);
        // Run after all mitigation systems, just before damage is applied to HP
        this.dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, reign.software.hyforged.combat.HyforgedDamageTakenSystem.class),
            new SystemDependency<>(Order.BEFORE, DamageSystems.ApplyDamage.class)
        );
    }

    @Nullable
    @Override
    public com.hypixel.hytale.component.SystemGroup<EntityStore> getGroup() {
        return com.hypixel.hytale.server.core.modules.entity.damage.DamageModule.get().getFilterDamageGroup();
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

        // Bypass-resistance damage skips Ward
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause == null || damageCause.doesBypassResistances()) {
            return;
        }

        EntityStatMap statMap = archetypeChunk.getComponent(index, entityStatMapType);
        if (statMap == null) {
            return;
        }

        if (!indexInitialized) {
            wardEntityStatIndex = EntityStatType.getAssetMap().getIndex("Ward");
            indexInitialized = true;
        }

        if (wardEntityStatIndex < 0) {
            return;
        }

        if (!StatAccessor.hasStatSlot(statMap, wardEntityStatIndex)) {
            return;
        }

        EntityStatValue wardValue = statMap.get(wardEntityStatIndex);
        if (wardValue == null || wardValue.getMax() <= 0) {
            return;
        }

        float currentWard = wardValue.get();
        if (currentWard <= 0) {
            return;
        }

        // Absorb as much damage as the current ward allows
        float incomingDamage = damage.getAmount();
        float absorbed = Math.min(incomingDamage, currentWard);

        statMap.setStatValue(wardEntityStatIndex, currentWard - absorbed);
        damage.setAmount(incomingDamage - absorbed);

        // Record for combat log / other systems
        damage.putMetaObject(CombatMeta.WARD_ABSORBED, absorbed);
    }
}
