package reign.software.hyforged.stats.system;

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
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.combat.CombatMeta;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Applies on-hit resource recovery stats to the attacker when a hit connects.
 * <p>
 * Runs in the {@code inspectDamageGroup} (after damage has been applied) and accesses
 * the attacker's stats to grant resources back to the attacker.
 * <p>
 * Supported stats:
 * <ul>
 *   <li>{@code hyforged:life-on-hit-flat} — HP added to attacker on hit (modified by healing-effectiveness-bps)</li>
 *   <li>{@code hyforged:mana-on-hit-flat} — mana added to attacker on hit</li>
 * </ul>
 */
public class HyforgedOnHitRecoverySystem extends DamageEventSystem {

    private static final int BPS_100_PERCENT = 10000;

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    // Cached indices — never call getIndex() per event
    private int lifeOnHitFlatIndex = -1;
    private int manaOnHitFlatIndex = -1;
    private int healingEffectivenessIndex = -1;
    private boolean indicesCached = false;

    public HyforgedOnHitRecoverySystem() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.entityStatMapType = EntityStatMap.getComponentType();
        // Defender entities drive the iteration; attacker is accessed via damage.getSource()
        this.query = entityStatMapType;
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
        // Skip cancelled damage
        if (damage.isCancelled()) {
            return;
        }

        // Skip zero/negative damage
        if (damage.getAmount() <= 0) {
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

        // Get attacker's components
        HyforgedStatComponent attackerStats = store.getComponent(attackerRef, statComponentType);
        EntityStatMap attackerStatMap = store.getComponent(attackerRef, entityStatMapType);
        if (attackerStats == null || attackerStatMap == null) {
            return;
        }

        ensureIndicesCached();

        if (lifeOnHitFlatIndex < 0 && manaOnHitFlatIndex < 0) {
            return;
        }

        int healingEffectivenessBps = healingEffectivenessIndex >= 0
                ? attackerStats.getCachedValue(healingEffectivenessIndex)
                : 0;

        // Life-on-hit with healing effectiveness
        if (lifeOnHitFlatIndex >= 0) {
            int lifeGain = attackerStats.getCachedValue(lifeOnHitFlatIndex);
            if (lifeGain > 0) {
                float effectiveGain = applyHealingEffectiveness(lifeGain, healingEffectivenessBps);
                addResource(attackerStatMap, DefaultEntityStatTypes.getHealth(), effectiveGain);
            }
        }

        // Mana-on-hit (healing effectiveness does not apply to mana)
        if (manaOnHitFlatIndex >= 0) {
            int manaGain = attackerStats.getCachedValue(manaOnHitFlatIndex);
            if (manaGain > 0) {
                addResource(attackerStatMap, DefaultEntityStatTypes.getMana(), (float) manaGain);
            }
        }
    }

    private float applyHealingEffectiveness(int amount, int healingEffectivenessBps) {
        if (healingEffectivenessBps == 0) {
            return (float) amount;
        }
        return amount * (1.0f + healingEffectivenessBps / (float) BPS_100_PERCENT);
    }

    private void addResource(@Nonnull EntityStatMap statMap, int statIndex, float amount) {
        if (amount <= 0.0f || statIndex < 0 || !StatAccessor.hasStatSlot(statMap, statIndex)) {
            return;
        }
        statMap.addStatValue(EntityStatMap.Predictable.SELF, statIndex, amount);
    }

    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        lifeOnHitFlatIndex = registry.getIndex(StatId.hyforged("life-on-hit-flat"));
        manaOnHitFlatIndex = registry.getIndex(StatId.hyforged("mana-on-hit-flat"));
        healingEffectivenessIndex = registry.getIndex(StatId.hyforged("healing-effectiveness-bps"));
        indicesCached = true;
    }
}
