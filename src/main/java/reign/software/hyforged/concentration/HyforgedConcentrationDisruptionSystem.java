package reign.software.hyforged.concentration;

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
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.combat.HyforgedAutoBlockSystem;
import reign.software.hyforged.combat.HyforgedHitResolutionSystem;
import reign.software.hyforged.combat.ailment.HyforgedAilmentSystem;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Applies concentration loss when damage is taken below the threshold.
 */
public class HyforgedConcentrationDisruptionSystem extends DamageEventSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final StatId LOSS_THRESHOLD_STAT = StatId.hyforged("concentration-loss-threshold-bps");
    private static final StatId LOSS_REDUCTION_STAT = StatId.hyforged("concentration-loss-reduction-bps");
    private static final StatId MAX_CONCENTRATION_STAT = StatId.hyforged("concentration");

    @Nonnull
    private final ComponentType<EntityStore, ConcentrationPriorityComponent> concentrationPriorityComponentType;

    @Nonnull
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    private int lossThresholdIndex = -1;
    private int lossReductionIndex = -1;
    private int maxConcentrationIndex = -1;
    private int healthStatIndex = -1;
    private boolean indicesInitialized = false;

    public HyforgedConcentrationDisruptionSystem(
            @Nonnull ComponentType<EntityStore, ConcentrationPriorityComponent> concentrationPriorityComponentType
    ) {
        this.concentrationPriorityComponentType = concentrationPriorityComponentType;
        this.entityStatMapType = EntityStatMap.getComponentType();
        this.query = Query.and(concentrationPriorityComponentType, entityStatMapType);
        this.dependencies = Set.of(
                new SystemDependency<>(Order.AFTER, HyforgedAilmentSystem.class),
                new SystemDependency<>(Order.AFTER, DamageSystems.EntityUIEvents.class)
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
        if (!shouldApplyLoss(damage)) {
            return;
        }

        ConcentrationPriorityComponent component = archetypeChunk.getComponent(index, concentrationPriorityComponentType);
        EntityStatMap statMap = archetypeChunk.getComponent(index, entityStatMapType);
        if (component == null || statMap == null) {
            return;
        }

        initializeIndices();
        if (lossThresholdIndex < 0 || lossReductionIndex < 0 || maxConcentrationIndex < 0 || healthStatIndex < 0) {
            return;
        }

        EntityStatValue healthValue = statMap.get(healthStatIndex);
        if (healthValue == null || healthValue.getMax() <= 0f) {
            return;
        }

        float currentHp = healthValue.get();
        float maxHp = healthValue.getMax();
        int thresholdBps = StatAccessor.getStatValueInt(statMap, lossThresholdIndex);
        if (!isBelowThreshold(currentHp, maxHp, thresholdBps)) {
            return;
        }

        int maxConcentration = StatAccessor.getStatValueInt(statMap, maxConcentrationIndex);
        if (maxConcentration <= 0) {
            return;
        }

        int baseLoss = calculateBaseLoss(damage.getAmount(), maxHp, maxConcentration);
        if (baseLoss <= 0) {
            return;
        }

        int lossReductionBps = StatAccessor.getStatValueInt(statMap, lossReductionIndex);
        int effectiveLoss = applyLossReduction(baseLoss, lossReductionBps);
        if (effectiveLoss <= 0) {
            return;
        }

        Ref<EntityStore> defenderRef = archetypeChunk.getReferenceTo(index);
        ConcentrationService.get().applyConcentrationLoss(defenderRef, effectiveLoss);

        if (LOGGER.at(Level.FINE).isEnabled()) {
            LOGGER.at(Level.FINE).log(
                    "Concentration disrupted: entity=%s damage=%s loss=%s threshold=%s",
                    defenderRef, damage.getAmount(), effectiveLoss, thresholdBps);
        }
    }

    static boolean shouldApplyLoss(@Nonnull Damage damage) {
        Boolean missed = damage.getIfPresentMetaObject(HyforgedHitResolutionSystem.MISS);
        Boolean blocked = damage.getIfPresentMetaObject(HyforgedAutoBlockSystem.AUTO_BLOCKED);
        return shouldApplyLoss(damage.isCancelled(), damage.getAmount(), missed, blocked);
    }

    static boolean shouldApplyLoss(
            boolean cancelled,
            float damageAmount,
            @Nullable Boolean missed,
            @Nullable Boolean blocked
    ) {
        if (cancelled || damageAmount <= 0f) {
            return false;
        }
        if (Boolean.TRUE.equals(missed)) {
            return false;
        }
        return !Boolean.TRUE.equals(blocked);
    }

    static boolean isBelowThreshold(float currentHp, float maxHp, int thresholdBps) {
        if (maxHp <= 0f) {
            return false;
        }
        float hpBps = (currentHp / maxHp) * 10000f;
        return hpBps < thresholdBps;
    }

    static int calculateBaseLoss(float damageAmount, float maxHp, int maxConcentration) {
        if (damageAmount <= 0f || maxHp <= 0f || maxConcentration <= 0) {
            return 0;
        }
        float loss = (damageAmount / maxHp) * maxConcentration;
        return Math.max(0, Math.round(loss));
    }

    static int applyLossReduction(int baseLoss, int lossReductionBps) {
        if (baseLoss <= 0) {
            return 0;
        }
        int reduction = Math.min(lossReductionBps, 10000);
        float multiplier = 1.0f - (reduction / 10000.0f);
        return Math.max(0, Math.round(baseLoss * multiplier));
    }

    private void initializeIndices() {
        if (indicesInitialized) {
            return;
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        lossThresholdIndex = registry.getIndex(LOSS_THRESHOLD_STAT);
        lossReductionIndex = registry.getIndex(LOSS_REDUCTION_STAT);
        maxConcentrationIndex = registry.getIndex(MAX_CONCENTRATION_STAT);
        healthStatIndex = EntityStatType.getAssetMap().getIndex("Health");
        indicesInitialized = true;
    }
}
