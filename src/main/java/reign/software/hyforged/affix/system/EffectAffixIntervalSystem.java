package reign.software.hyforged.affix.system;

import com.hypixel.hytale.common.util.TimeUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.component.HyforgedActiveEffectsComponent;
import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.AffixTrigger;
import reign.software.hyforged.affix.model.AffixTriggeredEffect;
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.affix.service.EffectAffixProcessor;
import reign.software.hyforged.stats.resource.RageDecayConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Ticks interval-based triggered effects for active effect affixes.
 */
public class EffectAffixIntervalSystem extends DelayedEntitySystem<EntityStore> {

    private static final float UPDATE_INTERVAL_SEC = 0.1f;

    @Nonnull
    private final ComponentType<EntityStore, HyforgedActiveEffectsComponent> activeEffectsType;

    @Nonnull
    private final ComponentType<EntityStore, DamageDataComponent> damageDataComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final AffixDefinitionRegistry affixRegistry;

    @Nonnull
    private final EffectAffixProcessor processor;

    public EffectAffixIntervalSystem() {
        super(UPDATE_INTERVAL_SEC);
        this.activeEffectsType = HyforgedPlugin.getInstance().getActiveEffectsComponentType();
        this.damageDataComponentType = DamageDataComponent.getComponentType();
        this.query = Query.and(activeEffectsType);
        this.affixRegistry = AffixDefinitionRegistry.get();
        this.processor = new EffectAffixProcessor(activeEffectsType);
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
        HyforgedActiveEffectsComponent activeEffects = archetypeChunk.getComponent(index, activeEffectsType);
        if (activeEffects == null || activeEffects.isEmpty()) {
            return;
        }

        Map<String, HyforgedActiveEffectsComponent.ActiveEffectState> states = activeEffects.getActiveEffects();
        if (states.isEmpty()) {
            return;
        }

        Ref<EntityStore> source = archetypeChunk.getReferenceTo(index);
        DamageDataComponent damageData = archetypeChunk.getComponent(index, damageDataComponentType);
        Instant now = resolveNow(store);

        for (Map.Entry<String, HyforgedActiveEffectsComponent.ActiveEffectState> entry : states.entrySet()) {
            HyforgedActiveEffectsComponent.ActiveEffectState state = entry.getValue();
            AffixDefinition definition = affixRegistry.get(state.getAffixId());
            if (definition == null) {
                continue;
            }
            int effectIndex = state.getEffectIndex();
            if (effectIndex < 0 || effectIndex >= definition.triggeredEffects().size()) {
                continue;
            }

            AffixTriggeredEffect triggeredEffect = definition.triggeredEffects().get(effectIndex);
            AffixTrigger trigger = triggeredEffect.trigger();
            if (!"interval".equalsIgnoreCase(trigger.type())) {
                continue;
            }

            float intervalSeconds = trigger.intervalSeconds();
            if (intervalSeconds <= 0f) {
                continue;
            }

            if (trigger.requireCombat() && !isInCombat(damageData, now)) {
                continue;
            }

            float accumulated = state.getAccumulatedTime() + dt;
            while (accumulated >= intervalSeconds) {
                processor.attemptIntervalTrigger(activeEffects, entry.getKey(), state, definition, triggeredEffect, source, commandBuffer, null);
                accumulated -= intervalSeconds;
            }
            state.setAccumulatedTime(accumulated);
        }
    }

    @Nonnull
    private Instant resolveNow(@Nonnull Store<EntityStore> store) {
        TimeResource time = store.getResource(TimeResource.getResourceType());
        return time != null ? time.getNow() : Instant.now();
    }

    private boolean isInCombat(@Nullable DamageDataComponent damageData, @Nonnull Instant now) {
        if (damageData == null) {
            return false;
        }
        RageDecayConfig config = RageDecayConfig.get();
        float delaySeconds = config.getOutOfCombatDelaySeconds();
        if (delaySeconds <= 0f) {
            return true;
        }
        Duration delay = Duration.ofMillis(Math.round(delaySeconds * 1000f));
        return TimeUtil.compareDifference(damageData.getLastCombatAction(), now, delay) < 0;
    }
}
