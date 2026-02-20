package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedConfig;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;

/**
 * Applies per-second resource regeneration stats to entities.
 * <p>
 * Interval-based (configurable via {@link reign.software.hyforged.HyforgedConfig#getRegenIntervalTicks()},
 * default 20 ticks / 1 second) for performance — NOT per-tick.
 * Applies the {@code healing-effectiveness-bps} multiplier to HP regen only.
 * <p>
 * Supported stats:
 * <ul>
 *   <li>{@code hyforged:health-regen-flat} — HP regen per second</li>
 *   <li>{@code hyforged:mana-regen-flat} — mana regen per second</li>
 *   <li>{@code hyforged:stamina-regen-flat} — stamina regen per second</li>
 *   <li>{@code hyforged:energy-regen-flat} — signature energy regen per second</li>
 *   <li>{@code hyforged:health-regen-percent-bps} — percent of max HP regen per second (healing-effectiveness applies)</li>
 *   <li>{@code hyforged:mana-regen-percent-bps} — percent of max mana regen per second</li>
 * </ul>
 */
public class HyforgedRegenSystem extends DelayedEntitySystem<EntityStore> {

    private static final int BPS_100_PERCENT = 10000;

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapType;

    @Nonnull
    private final Query<EntityStore> query;

    // Cached indices — never call getIndex() per tick
    private int healthRegenFlatIndex = -1;
    private int manaRegenFlatIndex = -1;
    private int staminaRegenFlatIndex = -1;
    private int energyRegenFlatIndex = -1;
    private int healthRegenPercentBpsIndex = -1;
    private int manaRegenPercentBpsIndex = -1;
    private int healingEffectivenessIndex = -1;
    private boolean indicesCached = false;

    public HyforgedRegenSystem() {
        super(HyforgedConfig.get().getRegenIntervalTicks() / 20.0f);
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.entityStatMapType = EntityStatMap.getComponentType();
        this.query = Query.and(statComponentType, entityStatMapType);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        HyforgedStatComponent hyforgedStats = archetypeChunk.getComponent(index, statComponentType);
        EntityStatMap entityStatMap = archetypeChunk.getComponent(index, entityStatMapType);

        if (hyforgedStats == null || entityStatMap == null) {
            return;
        }

        ensureIndicesCached();

        int healingEffectivenessBps = healingEffectivenessIndex >= 0
                ? hyforgedStats.getCachedValue(healingEffectivenessIndex)
                : 0;

        // HP regen — applies healing effectiveness multiplier
        applyRegen(entityStatMap, hyforgedStats, DefaultEntityStatTypes.getHealth(),
                healthRegenFlatIndex, dt, healingEffectivenessBps);

        // HP percent regen — applies healing effectiveness multiplier
        applyPercentRegen(entityStatMap, hyforgedStats, DefaultEntityStatTypes.getHealth(),
                healthRegenPercentBpsIndex, dt, healingEffectivenessBps);

        // Mana regen — no healing effectiveness
        applyRegen(entityStatMap, hyforgedStats, DefaultEntityStatTypes.getMana(),
                manaRegenFlatIndex, dt, 0);

        // Mana percent regen — no healing effectiveness
        applyPercentRegen(entityStatMap, hyforgedStats, DefaultEntityStatTypes.getMana(),
                manaRegenPercentBpsIndex, dt, 0);

        // Stamina regen — no healing effectiveness
        applyRegen(entityStatMap, hyforgedStats, DefaultEntityStatTypes.getStamina(),
                staminaRegenFlatIndex, dt, 0);

        // Signature energy regen — no healing effectiveness
        applyRegen(entityStatMap, hyforgedStats, DefaultEntityStatTypes.getSignatureEnergy(),
                energyRegenFlatIndex, dt, 0);
    }

    private void applyRegen(
            @Nonnull EntityStatMap entityStatMap,
            @Nonnull HyforgedStatComponent hyforgedStats,
            int hytaleStatIndex,
            int regenStatIndex,
            float dt,
            int healingEffectivenessBps
    ) {
        if (regenStatIndex < 0 || hytaleStatIndex < 0) {
            return;
        }

        int regenFlat = hyforgedStats.getCachedValue(regenStatIndex);
        if (regenFlat == 0) {
            return;
        }

        float regenAmount = regenFlat * dt;

        if (healingEffectivenessBps != 0) {
            regenAmount *= (1.0f + healingEffectivenessBps / (float) BPS_100_PERCENT);
        }

        if (regenAmount <= 0.0f) {
            return;
        }

        if (!StatAccessor.hasStatSlot(entityStatMap, hytaleStatIndex)) {
            return;
        }

        entityStatMap.addStatValue(EntityStatMap.Predictable.SELF, hytaleStatIndex, regenAmount);
    }

    /**
     * Applies percent-of-max regen to a stat.
     *
     * @param entityStatMap          the entity’s stat map
     * @param hyforgedStats          the entity’s Hyforged stat component
     * @param hytaleStatIndex        the Hytale stat to modify (Health, Mana…)
     * @param regenPercentBpsIndex   the Hyforged percent-regen stat index (bps of max per second)
     * @param dt                     delta time in seconds (equal to the regen interval)
     * @param healingEffectivenessBps healing effectiveness modifier in bps (applied to HP regen only)
     */
    private void applyPercentRegen(
            @Nonnull EntityStatMap entityStatMap,
            @Nonnull HyforgedStatComponent hyforgedStats,
            int hytaleStatIndex,
            int regenPercentBpsIndex,
            float dt,
            int healingEffectivenessBps
    ) {
        if (regenPercentBpsIndex < 0 || hytaleStatIndex < 0) {
            return;
        }

        int regenBps = hyforgedStats.getCachedValue(regenPercentBpsIndex);
        if (regenBps == 0) {
            return;
        }

        EntityStatValue statValue = entityStatMap.get(hytaleStatIndex);
        if (statValue == null) {
            return;
        }

        float maxAmount = statValue.getMax();
        if (maxAmount <= 0.0f) {
            return;
        }

        float regenAmount = maxAmount * regenBps / (float) BPS_100_PERCENT * dt;

        if (healingEffectivenessBps != 0) {
            regenAmount *= (1.0f + healingEffectivenessBps / (float) BPS_100_PERCENT);
        }

        if (regenAmount <= 0.0f) {
            return;
        }

        entityStatMap.addStatValue(EntityStatMap.Predictable.SELF, hytaleStatIndex, regenAmount);
    }

    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        healthRegenFlatIndex = registry.getIndex(StatId.hyforged("health-regen-flat"));
        manaRegenFlatIndex = registry.getIndex(StatId.hyforged("mana-regen-flat"));
        staminaRegenFlatIndex = registry.getIndex(StatId.hyforged("stamina-regen-flat"));
        energyRegenFlatIndex = registry.getIndex(StatId.hyforged("energy-regen-flat"));
        healthRegenPercentBpsIndex = registry.getIndex(StatId.hyforged("health-regen-percent-bps"));
        manaRegenPercentBpsIndex = registry.getIndex(StatId.hyforged("mana-regen-percent-bps"));
        healingEffectivenessIndex = registry.getIndex(StatId.hyforged("healing-effectiveness-bps"));
        indicesCached = true;
    }
}
