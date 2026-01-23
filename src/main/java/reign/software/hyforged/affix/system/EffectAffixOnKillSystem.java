package reign.software.hyforged.affix.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector4d;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.service.EffectAffixProcessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Triggers affix effects when an entity kills another entity.
 */
public class EffectAffixOnKillSystem extends DeathSystems.OnDeathSystem {

    @Nonnull
    private final Query<EntityStore> query = Query.any();

    @Nonnull
    private final EffectAffixProcessor processor;

    public EffectAffixOnKillSystem() {
        this.processor = new EffectAffixProcessor(HyforgedPlugin.getInstance().getActiveEffectsComponentType());
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull DeathComponent component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Damage deathInfo = component.getDeathInfo();
        if (deathInfo == null) {
            return;
        }
        if (!(deathInfo.getSource() instanceof Damage.EntitySource entitySource)) {
            return;
        }
        Ref<EntityStore> killer = entitySource.getRef();
        if (killer == null || !killer.isValid()) {
            return;
        }
        Vector3d position = resolveHitPosition(deathInfo);
        processor.processOnKill(killer, ref, deathInfo, commandBuffer, position);
    }

    @Nullable
    private Vector3d resolveHitPosition(@Nonnull Damage damage) {
        Vector4d hit = damage.getIfPresentMetaObject(Damage.HIT_LOCATION);
        if (hit == null) {
            return null;
        }
        return new Vector3d(hit.x, hit.y, hit.z);
    }
}
