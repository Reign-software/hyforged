package reign.software.hyforged.concentration;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;

/**
 * Regenerates concentration based on Wisdom and regen rate stats.
 */
public class HyforgedConcentrationRegenerationSystem extends DelayedEntitySystem<EntityStore> {

    private static final StatId WISDOM_STAT = StatId.hyforged("wisdom");
    private static final StatId REGEN_RATE_STAT = StatId.hyforged("concentration-regen-rate-bps");

    @Nonnull
    private final ComponentType<EntityStore, ConcentrationPriorityComponent> concentrationPriorityComponentType;

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    private int wisdomStatIndex = -1;
    private int regenRateStatIndex = -1;
    private boolean indicesInitialized = false;

    public HyforgedConcentrationRegenerationSystem() {
        super(ConcentrationRegenConfig.get().getUpdateIntervalSeconds());
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.concentrationPriorityComponentType = plugin.getConcentrationPriorityComponentType();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.query = Query.and(concentrationPriorityComponentType, statComponentType);
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
        ConcentrationPriorityComponent component = archetypeChunk.getComponent(index, concentrationPriorityComponentType);
        HyforgedStatComponent statComponent = archetypeChunk.getComponent(index, statComponentType);
        if (component == null || statComponent == null) {
            return;
        }

        initializeIndices();
        if (wisdomStatIndex < 0 || regenRateStatIndex < 0) {
            return;
        }

        Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
        int wisdom = StatAccessor.getStatValueInt(store, entityRef, wisdomStatIndex);
        int regenRateBps = StatAccessor.getStatValueInt(store, entityRef, regenRateStatIndex);

        ConcentrationRegenConfig config = ConcentrationRegenConfig.get();
        float regenPerSecond = calculateRegenPerSecond(wisdom, config.getWisdomScalingFactor(), regenRateBps);
        if (regenPerSecond <= 0f) {
            return;
        }

        float regenPerTick = regenPerSecond * dt;
        if (regenPerTick <= 0f) {
            return;
        }

        ConcentrationService.get().tickRegeneration(entityRef, regenPerTick);
    }

    public static float calculateRegenPerSecond(int wisdom, float scalingFactor, int regenRateBps) {
        if (wisdom <= 0 || scalingFactor <= 0f) {
            return 0f;
        }
        float multiplier = 1.0f + (regenRateBps / 10000.0f);
        if (multiplier < 0f) {
            multiplier = 0f;
        }
        return wisdom * scalingFactor * multiplier;
    }

    private void initializeIndices() {
        if (indicesInitialized) {
            return;
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        wisdomStatIndex = registry.getIndex(WISDOM_STAT);
        regenRateStatIndex = registry.getIndex(REGEN_RATE_STAT);
        indicesInitialized = true;
    }
}
