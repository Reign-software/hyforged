package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;

/**
 * Applies resource recovery stats to the killer when an entity dies.
 *
 * Supported stats:
 * - hyforged:life-on-kill-flat
 * - hyforged:mana-on-kill-flat
 * - hyforged:mana-on-kill-bps (percent of max mana)
 */
public class OnKillResourceRecoverySystem extends DeathSystems.OnDeathSystem {

    private static final int BPS_100_PERCENT = 10000;

    private static final StatId LIFE_ON_KILL_FLAT = StatId.hyforged("life-on-kill-flat");
    private static final StatId MANA_ON_KILL_FLAT = StatId.hyforged("mana-on-kill-flat");
    private static final StatId MANA_ON_KILL_BPS = StatId.hyforged("mana-on-kill-bps");
    private static final StatId HEALING_EFFECTIVENESS = StatId.hyforged("healing-effectiveness-bps");

    private int lifeOnKillFlatIndex = -1;
    private int manaOnKillFlatIndex = -1;
    private int manaOnKillBpsIndex = -1;
    private int healingEffectivenessIndex = -1;
    private boolean indicesCached = false;

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return DeathComponent.getComponentType();
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> victimRef,
            @Nonnull DeathComponent deathComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Damage deathInfo = deathComponent.getDeathInfo();
        if (deathInfo == null || !(deathInfo.getSource() instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> killerRef = entitySource.getRef();
        if (killerRef == null || !killerRef.isValid() || killerRef == victimRef) {
            return;
        }

        ensureIndicesCached();
        if (lifeOnKillFlatIndex < 0 && manaOnKillFlatIndex < 0 && manaOnKillBpsIndex < 0) {
            return;
        }

        EntityStatMap killerStatMap = store.getComponent(killerRef, EntityStatMap.getComponentType());
        if (killerStatMap == null) {
            return;
        }

        int lifeGain = readStatValue(store, killerRef, lifeOnKillFlatIndex);
        int manaGain = readStatValue(store, killerRef, manaOnKillFlatIndex);

        // Apply healing effectiveness multiplier to life-on-kill
        if (lifeGain > 0 && healingEffectivenessIndex >= 0) {
            int healingBps = readStatValue(store, killerRef, healingEffectivenessIndex);
            if (healingBps != 0) {
                lifeGain = Math.round(lifeGain * (1.0f + healingBps / (float) BPS_100_PERCENT));
            }
        }

        int manaOnKillBps = readStatValue(store, killerRef, manaOnKillBpsIndex);
        if (manaOnKillBps != 0) {
            EntityStatValue manaStat = killerStatMap.get(DefaultEntityStatTypes.getMana());
            if (manaStat != null) {
                manaGain += Math.round(manaStat.getMax() * manaOnKillBps / BPS_100_PERCENT);
            }
        }

        addResourceGain(killerStatMap, DefaultEntityStatTypes.getHealth(), lifeGain);
        addResourceGain(killerStatMap, DefaultEntityStatTypes.getMana(), manaGain);
    }

    private int readStatValue(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            int statIndex
    ) {
        if (statIndex < 0) {
            return 0;
        }
        return StatAccessor.getStatValueInt(store, entityRef, statIndex);
    }

    private void addResourceGain(@Nonnull EntityStatMap statMap, int statIndex, int amount) {
        if (amount == 0 || statIndex < 0 || !StatAccessor.hasStatSlot(statMap, statIndex)) {
            return;
        }
        statMap.addStatValue(EntityStatMap.Predictable.SELF, statIndex, amount);
    }

    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        lifeOnKillFlatIndex = registry.getIndex(LIFE_ON_KILL_FLAT);
        manaOnKillFlatIndex = registry.getIndex(MANA_ON_KILL_FLAT);
        manaOnKillBpsIndex = registry.getIndex(MANA_ON_KILL_BPS);
        healingEffectivenessIndex = registry.getIndex(HEALING_EFFECTIVENESS);
        indicesCached = true;
    }
}
