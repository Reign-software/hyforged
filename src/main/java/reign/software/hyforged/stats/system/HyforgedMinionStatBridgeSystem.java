package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.minion.MinionDefinition;
import reign.software.hyforged.minion.MinionDefinitionRegistry;
import reign.software.hyforged.minion.component.SummonerLinkComponent;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * RefChangeSystem that bridges summoner minion stats to newly spawned minion entities.
 * <p>
 * When a {@link SummonerLinkComponent} is added to a minion entity (during spawn),
 * this system reads the summoner's minion-related stats and applies them as INCREASED
 * modifiers on the minion's {@link EntityStatMap}. This propagates stats such as:
 * <ul>
 *   <li>{@code hyforged:minion-damage-bps} &rarr; minion attack power</li>
 *   <li>{@code hyforged:minion-life-bps} &rarr; minion max health</li>
 *   <li>{@code hyforged:minion-speed-bps} &rarr; minion movement speed</li>
 *   <li>{@code hyforged:minion-accuracy-bps} &rarr; minion accuracy rating</li>
 *   <li>{@code hyforged:minion-attack-speed-bps} &rarr; minion attack speed</li>
 *   <li>{@code hyforged:minion-crit-chance-bps} &rarr; minion crit chance</li>
 * </ul>
 * <p>
 * Duration BPS and max-minions are <b>not</b> applied to minion entities; they are
 * handled by {@link reign.software.hyforged.minion.MinionSummonService} during
 * spawn validation and timer computation.
 */
public class HyforgedMinionStatBridgeSystem extends RefChangeSystem<EntityStore, SummonerLinkComponent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Modifier source key prefix for all minion bridge modifiers. */
    private static final String BRIDGE_SOURCE = "hyforged:minion-bridge";

    // --- Summoner stat IDs (source: read from summoner) ---
    private static final StatId MINION_DAMAGE_BPS       = StatId.hyforged("minion-damage-bps");
    private static final StatId MINION_LIFE_BPS         = StatId.hyforged("minion-life-bps");
    private static final StatId MINION_SPEED_BPS        = StatId.hyforged("minion-speed-bps");
    private static final StatId MINION_ACCURACY_BPS     = StatId.hyforged("minion-accuracy-bps");
    private static final StatId MINION_ATTACK_SPEED_BPS = StatId.hyforged("minion-attack-speed-bps");
    private static final StatId MINION_CRIT_CHANCE_BPS  = StatId.hyforged("minion-crit-chance-bps");
    private static final StatId MAX_MINIONS             = StatId.hyforged("max-minions");
    private static final StatId MINION_DURATION_BPS     = StatId.hyforged("minion-duration-bps");

    // --- Minion target stat IDs (target: applied to minion) ---
    private static final StatId TARGET_ATTACK_POWER   = StatId.hyforged("attack-power");
    private static final StatId TARGET_MOVEMENT_SPEED = StatId.hyforged("movement-speed-bps");
    private static final StatId TARGET_ACCURACY       = StatId.hyforged("accuracy-rating");
    private static final StatId TARGET_CRIT_CHANCE    = StatId.hyforged("crit-chance-bps");

    private final ComponentType<EntityStore, SummonerLinkComponent> summonerLinkType;
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;
    private final ComponentType<EntityStore, EntityStatMap> statMapType;

    // Cached summoner stat indices (source)
    private int minionDamageIndex      = -1;
    private int minionLifeIndex        = -1;
    private int minionSpeedIndex       = -1;
    private int minionAccuracyIndex    = -1;
    private int minionAttackSpeedIndex = -1;
    private int minionCritChanceIndex  = -1;
    private int maxMinionsIndex        = -1;
    private int minionDurationIndex    = -1;

    // Cached minion target stat indices
    private int targetAttackPowerIndex   = -1;
    private int targetHealthIndex        = -1;
    private int targetMovementSpeedIndex = -1;
    private int targetAccuracyIndex      = -1;
    private int targetAttackSpeedIndex   = -1;
    private int targetCritChanceIndex    = -1;

    private boolean indicesCached = false;

    public HyforgedMinionStatBridgeSystem(
            @Nonnull ComponentType<EntityStore, SummonerLinkComponent> summonerLinkType
    ) {
        this.summonerLinkType = summonerLinkType;
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        this.statMapType = EntityStatMap.getComponentType();
    }

    @Nonnull
    @Override
    public ComponentType<EntityStore, SummonerLinkComponent> componentType() {
        return summonerLinkType;
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return summonerLinkType;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of();
    }

    // ========== RefChangeSystem callbacks ==========

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> minionRef,
            @Nonnull SummonerLinkComponent link,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        propagateStats(minionRef, link, store);
    }

    @Override
    public void onComponentSet(
            @Nonnull Ref<EntityStore> minionRef,
            @Nullable SummonerLinkComponent previous,
            @Nonnull SummonerLinkComponent current,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Re-propagate stats if the link is updated (e.g., summoner changed)
        propagateStats(minionRef, current, store);
    }

    @Override
    public void onComponentRemoved(
            @Nonnull Ref<EntityStore> minionRef,
            @Nonnull SummonerLinkComponent link,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Clean up bridge modifiers when the link is removed
        removeBridgeModifiers(minionRef, store);
    }

    // ========== Stat propagation ==========

    /**
     * Read the summoner's minion stats and apply them as modifiers to the minion entity.
     *
     * @param minionRef   The minion entity reference
     * @param link        The summoner link component on the minion
     * @param store       The entity store
     */
    private void propagateStats(
            @Nonnull Ref<EntityStore> minionRef,
            @Nonnull SummonerLinkComponent link,
            @Nonnull Store<EntityStore> store
    ) {
        ensureIndicesCached();

        if (!minionRef.isValid()) {
            LOGGER.atWarning().log("MinionStatBridge: minion ref is invalid, skipping propagation");
            return;
        }

        UUID summonerUuid = link.getSummonerUuid();
        if (summonerUuid == null) {
            LOGGER.atWarning().log("MinionStatBridge: SummonerLinkComponent has null summoner UUID");
            return;
        }

        // Resolve summoner entity
        Ref<EntityStore> summonerRef = store.getExternalData().getRefFromUUID(summonerUuid);
        if (summonerRef == null || !summonerRef.isValid()) {
            LOGGER.atWarning().log(
                    "MinionStatBridge: summoner ref invalid for UUID %s, skipping stat propagation",
                    summonerUuid);
            return;
        }

        // Read summoner's stat component for BPS values
        HyforgedStatComponent summonerStats = store.getComponent(summonerRef, statComponentType);
        if (summonerStats == null) {
            LOGGER.at(Level.FINE).log(
                    "MinionStatBridge: summoner %s has no HyforgedStatComponent, skipping",
                    summonerUuid);
            return;
        }

        // Get the minion's EntityStatMap for applying modifiers
        EntityStatMap minionStatMap = store.getComponent(minionRef, statMapType);
        if (minionStatMap == null) {
            LOGGER.atWarning().log("MinionStatBridge: minion has no EntityStatMap, cannot apply modifiers");
            return;
        }

        // Also get the minion's HyforgedStatComponent as fallback
        HyforgedStatComponent minionStats = store.getComponent(minionRef, statComponentType);

        int applied = 0;

        // Bridge each summoner minion stat to the corresponding minion target stat
        applied += applyBridgeModifier(minionStatMap, minionStats, summonerStats,
                minionDamageIndex, targetAttackPowerIndex, "damage");
        applied += applyBridgeModifier(minionStatMap, minionStats, summonerStats,
                minionLifeIndex, targetHealthIndex, "life");
        applied += applyBridgeModifier(minionStatMap, minionStats, summonerStats,
                minionSpeedIndex, targetMovementSpeedIndex, "speed");
        applied += applyBridgeModifier(minionStatMap, minionStats, summonerStats,
                minionAccuracyIndex, targetAccuracyIndex, "accuracy");
        applied += applyBridgeModifier(minionStatMap, minionStats, summonerStats,
                minionAttackSpeedIndex, targetAttackSpeedIndex, "attack-speed");
        applied += applyBridgeModifier(minionStatMap, minionStats, summonerStats,
                minionCritChanceIndex, targetCritChanceIndex, "crit-chance");

        // M-4: Apply stat overrides from the MinionDefinition (data-driven per-type overrides)
        applied += applyStatOverrides(minionStatMap, minionStats, link.getMinionTypeId());

        if (minionStats != null && applied > 0) {
            minionStats.markAllDirty();
        }

        LOGGER.at(Level.FINE).log(
                "MinionStatBridge: applied %d bridge modifiers from summoner %s to minion (type=%s)",
                applied, summonerUuid, link.getMinionTypeId());
    }

    /**
     * Apply a single bridge modifier from a summoner stat to a minion target stat.
     *
     * @param minionStatMap  The minion's EntityStatMap
     * @param minionStats    The minion's HyforgedStatComponent (nullable fallback)
     * @param summonerStats  The summoner's HyforgedStatComponent
     * @param sourceIndex    The summoner stat index to read BPS from
     * @param targetIndex    The minion stat index to apply the modifier to
     * @param label          Human-readable label for logging
     * @return 1 if a modifier was applied, 0 otherwise
     */
    private int applyBridgeModifier(
            @Nonnull EntityStatMap minionStatMap,
            @Nullable HyforgedStatComponent minionStats,
            @Nonnull HyforgedStatComponent summonerStats,
            int sourceIndex,
            int targetIndex,
            @Nonnull String label
    ) {
        if (sourceIndex < 0 || targetIndex < 0) {
            return 0;
        }

        int bpsValue = summonerStats.getCachedValue(sourceIndex);
        if (bpsValue == 0) {
            return 0;
        }

        HyforgedModifier modifier = HyforgedModifier.builder()
                .sourceId(BRIDGE_SOURCE)
                .sourceType(HyforgedModifier.SourceType.EFFECT)
                .stackType(HyforgedModifier.StackType.INCREASED)
                .targetStat(targetIndex)
                .amount(bpsValue)
                .priority(0)
                .permanent()
                .build();

        String modifierKey = BRIDGE_SOURCE + ":" + label;

        if (StatAccessor.hasStatSlot(minionStatMap, targetIndex)) {
            minionStatMap.putModifier(targetIndex, modifierKey, modifier);
            return 1;
        } else if (minionStats != null) {
            return minionStats.upsertModifier(modifier) ? 1 : 0;
        }
        return 0;
    }

    /**
     * Apply stat overrides from the MinionDefinition to the minion entity.
     * <p>
     * Stat overrides are per-type values defined in the minion JSON (e.g.,
     * {@code "StatOverrides": { "hyforged:minion-damage-bps": 500 }}). They are applied
     * as INCREASED modifiers with a distinct source key for easy removal.
     *
     * @param minionStatMap The minion's EntityStatMap
     * @param minionStats   The minion's HyforgedStatComponent (nullable fallback)
     * @param minionTypeId  The minion type ID to look up the definition
     * @return the number of override modifiers applied
     */
    private int applyStatOverrides(
            @Nonnull EntityStatMap minionStatMap,
            @Nullable HyforgedStatComponent minionStats,
            @Nullable String minionTypeId
    ) {
        if (minionTypeId == null) {
            return 0;
        }

        MinionDefinition definition = MinionDefinitionRegistry.get().get(minionTypeId);
        if (definition == null || definition.getStatOverrides().isEmpty()) {
            return 0;
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int applied = 0;

        for (Map.Entry<String, Integer> entry : definition.getStatOverrides().entrySet()) {
            String statIdStr = entry.getKey();
            int overrideValue = entry.getValue();
            if (overrideValue == 0) {
                continue;
            }

            int targetIndex = registry.getIndex(StatId.parse(statIdStr));
            if (targetIndex < 0) {
                LOGGER.at(Level.FINE).log(
                        "MinionStatBridge: stat override '%s' not found in registry, skipping", statIdStr);
                continue;
            }

            String modifierKey = BRIDGE_SOURCE + ":override:" + statIdStr;
            HyforgedModifier modifier = HyforgedModifier.builder()
                    .sourceId(BRIDGE_SOURCE)
                    .sourceType(HyforgedModifier.SourceType.EFFECT)
                    .stackType(HyforgedModifier.StackType.INCREASED)
                    .targetStat(targetIndex)
                    .amount(overrideValue)
                    .priority(0)
                    .permanent()
                    .build();

            if (StatAccessor.hasStatSlot(minionStatMap, targetIndex)) {
                minionStatMap.putModifier(targetIndex, modifierKey, modifier);
                applied++;
            } else if (minionStats != null) {
                applied += minionStats.upsertModifier(modifier) ? 1 : 0;
            }
        }

        if (applied > 0) {
            LOGGER.at(Level.FINE).log(
                    "MinionStatBridge: applied %d stat override(s) from definition '%s'",
                    applied, minionTypeId);
        }

        return applied;
    }

    /**
     * Remove all bridge modifiers from a minion when its summoner link is removed.
     */
    private void removeBridgeModifiers(
            @Nonnull Ref<EntityStore> minionRef,
            @Nonnull Store<EntityStore> store
    ) {
        if (!minionRef.isValid()) {
            return;
        }

        EntityStatMap minionStatMap = store.getComponent(minionRef, statMapType);
        HyforgedStatComponent minionStats = store.getComponent(minionRef, statComponentType);
        int removed = 0;

        if (minionStatMap != null) {
            removed += removeIfPresent(minionStatMap, targetAttackPowerIndex, "damage");
            removed += removeIfPresent(minionStatMap, targetHealthIndex, "life");
            removed += removeIfPresent(minionStatMap, targetMovementSpeedIndex, "speed");
            removed += removeIfPresent(minionStatMap, targetAccuracyIndex, "accuracy");
            removed += removeIfPresent(minionStatMap, targetAttackSpeedIndex, "attack-speed");
            removed += removeIfPresent(minionStatMap, targetCritChanceIndex, "crit-chance");
        }

        if (minionStats != null) {
            int componentRemoved = minionStats.removeModifiersIf(
                    mod -> BRIDGE_SOURCE.equals(mod.getSourceId()),
                    mod -> { }
            );
            removed += componentRemoved;
            if (componentRemoved > 0) {
                minionStats.markAllDirty();
            }
        }

        if (removed > 0) {
            LOGGER.at(Level.FINE).log("MinionStatBridge: removed %d bridge modifiers from minion", removed);
        }
    }

    private int removeIfPresent(@Nonnull EntityStatMap statMap, int targetIndex, @Nonnull String label) {
        if (targetIndex < 0) {
            return 0;
        }
        String modifierKey = BRIDGE_SOURCE + ":" + label;
        return statMap.removeModifier(targetIndex, modifierKey) != null ? 1 : 0;
    }

    // ========== Index caching ==========

    /**
     * Cache all minion-related stat indices from the registry.
     * <p>
     * Resolves both summoner source indices (minion-* BPS stats) and
     * minion target indices (the actual stats to modify on the minion).
     */
    public void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();

        // Summoner source indices
        minionDamageIndex      = registry.getIndex(MINION_DAMAGE_BPS);
        minionLifeIndex        = registry.getIndex(MINION_LIFE_BPS);
        minionSpeedIndex       = registry.getIndex(MINION_SPEED_BPS);
        minionAccuracyIndex    = registry.getIndex(MINION_ACCURACY_BPS);
        minionAttackSpeedIndex = registry.getIndex(MINION_ATTACK_SPEED_BPS);
        minionCritChanceIndex  = registry.getIndex(MINION_CRIT_CHANCE_BPS);
        maxMinionsIndex        = registry.getIndex(MAX_MINIONS);
        minionDurationIndex    = registry.getIndex(MINION_DURATION_BPS);

        // Minion target indices
        targetAttackPowerIndex   = registry.getIndex(TARGET_ATTACK_POWER);
        targetHealthIndex        = DefaultEntityStatTypes.getHealth();
        targetMovementSpeedIndex = registry.getIndex(TARGET_MOVEMENT_SPEED);
        targetAccuracyIndex      = registry.getIndex(TARGET_ACCURACY);
        targetCritChanceIndex    = registry.getIndex(TARGET_CRIT_CHANCE);

        indicesCached = true;

        LOGGER.at(Level.FINE).log(
                "HyforgedMinionStatBridgeSystem: cached indices — "
                        + "source[damage=%d, life=%d, speed=%d, accuracy=%d, "
                        + "attackSpeed=%d, critChance=%d, maxMinions=%d, duration=%d] "
                        + "target[attackPower=%d, health=%d, moveSpeed=%d, accuracy=%d, "
                        + "critChance=%d]",
                minionDamageIndex, minionLifeIndex, minionSpeedIndex,
                minionAccuracyIndex, minionAttackSpeedIndex, minionCritChanceIndex,
                maxMinionsIndex, minionDurationIndex,
                targetAttackPowerIndex, targetHealthIndex, targetMovementSpeedIndex,
                targetAccuracyIndex, targetCritChanceIndex);
    }
}
