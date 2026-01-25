package reign.software.hyforged.affix.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector4d;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.service.EffectAffixProcessor;
import reign.software.hyforged.combat.HyforgedAutoBlockSystem;
import reign.software.hyforged.combat.HyforgedHitResolutionSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Set;

/**
 * Damage pipeline system that triggers affix effects on hit, damaged, and block events.
 */
public class EffectAffixDamageTriggerSystem extends DamageEventSystem {

    @Nonnull
    private final Query<EntityStore> query = Query.any();

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    @Nonnull
    private final EffectAffixProcessor processor;

    public EffectAffixDamageTriggerSystem() {
        this.processor = new EffectAffixProcessor(HyforgedPlugin.getInstance().getActiveEffectsComponentType());
        this.dependencies = Set.of(
                new SystemDependency<>(Order.AFTER, DamageSystems.ApplyDamage.class),
            new SystemDependency<>(Order.BEFORE, DamageSystems.RecordLastCombat.class),
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
        if (damage.isCancelled() || damage.getAmount() <= 0f) {
            return;
        }

        Boolean missed = damage.getIfPresentMetaObject(HyforgedHitResolutionSystem.MISS);
        if (Boolean.TRUE.equals(missed)) {
            return;
        }

        Ref<EntityStore> victim = archetypeChunk.getReferenceTo(index);
        Ref<EntityStore> attacker = null;
        if (damage.getSource() instanceof Damage.EntitySource entitySource) {
            attacker = entitySource.getRef();
        }

        Vector3d position = resolveHitPosition(damage);
        Instant now = resolveNow(store);

        if (attacker != null && attacker.isValid()) {
            processor.processOnCombatStart(attacker, victim, commandBuffer, position, now);
        }
        if (victim.isValid()) {
            processor.processOnCombatStart(victim, attacker, commandBuffer, position, now);
        }

        if (attacker != null && attacker.isValid()) {
            processor.processOnHit(attacker, victim, damage, commandBuffer, position);
        }

        processor.processOnDamaged(victim, attacker, damage, commandBuffer, position);

        if (isBlocked(damage)) {
            processor.processOnBlock(victim, attacker, damage, commandBuffer, position);
        }
    }

    private boolean isBlocked(@Nonnull Damage damage) {
        Boolean blocked = damage.getIfPresentMetaObject(Damage.BLOCKED);
        if (Boolean.TRUE.equals(blocked)) {
            return true;
        }
        Boolean autoBlocked = damage.getIfPresentMetaObject(HyforgedAutoBlockSystem.AUTO_BLOCKED);
        return Boolean.TRUE.equals(autoBlocked);
    }

    @Nullable
    private Vector3d resolveHitPosition(@Nonnull Damage damage) {
        Vector4d hit = damage.getIfPresentMetaObject(Damage.HIT_LOCATION);
        if (hit == null) {
            return null;
        }
        return new Vector3d(hit.x, hit.y, hit.z);
    }

    @Nonnull
    private Instant resolveNow(@Nonnull Store<EntityStore> store) {
        TimeResource time = store.getResource(TimeResource.getResourceType());
        return time != null ? time.getNow() : Instant.now();
    }
}
