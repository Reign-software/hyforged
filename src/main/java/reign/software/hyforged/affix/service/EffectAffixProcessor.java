package reign.software.hyforged.affix.service;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionType;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.component.HyforgedActiveEffectsComponent;
import reign.software.hyforged.affix.event.EffectAffixExecutedEvent;
import reign.software.hyforged.affix.event.EffectAffixTriggeredEvent;
import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.AffixTrigger;
import reign.software.hyforged.affix.model.AffixTriggeredEffect;
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.combat.CombatMath;
import reign.software.hyforged.quality.component.HyforgedNPCQualityComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes triggered effect affixes for various trigger types.
 */
public class EffectAffixProcessor {

    private static final Logger LOGGER = Logger.getLogger(EffectAffixProcessor.class.getName());

    private final ComponentType<EntityStore, HyforgedActiveEffectsComponent> activeEffectsType;
    private final AffixDefinitionRegistry affixRegistry;
    private final EffectExecutorService executor;

    public EffectAffixProcessor(
            @Nonnull ComponentType<EntityStore, HyforgedActiveEffectsComponent> activeEffectsType
    ) {
        this(activeEffectsType, AffixDefinitionRegistry.get(), new DefaultEffectExecutorService());
    }

    public EffectAffixProcessor(
            @Nonnull ComponentType<EntityStore, HyforgedActiveEffectsComponent> activeEffectsType,
            @Nonnull AffixDefinitionRegistry affixRegistry,
            @Nonnull EffectExecutorService executor
    ) {
        this.activeEffectsType = activeEffectsType;
        this.affixRegistry = affixRegistry;
        this.executor = executor;
    }

    public void processOnHit(
            @Nonnull Ref<EntityStore> attacker,
            @Nonnull Ref<EntityStore> victim,
            @Nonnull Damage damage,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nullable Vector3d position
    ) {
        processDamageTrigger(attacker, victim, damage, "on_hit", accessor, position);
    }

    public void processOnDamaged(
            @Nonnull Ref<EntityStore> victim,
            @Nullable Ref<EntityStore> attacker,
            @Nonnull Damage damage,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nullable Vector3d position
    ) {
        processDamageTrigger(victim, attacker, damage, "on_damaged", accessor, position);
    }

    public void processOnBlock(
            @Nonnull Ref<EntityStore> defender,
            @Nullable Ref<EntityStore> attacker,
            @Nonnull Damage damage,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nullable Vector3d position
    ) {
        processDamageTrigger(defender, attacker, damage, "on_block", accessor, position);
    }

    public void processOnKill(
            @Nonnull Ref<EntityStore> killer,
            @Nonnull Ref<EntityStore> victim,
            @Nonnull Damage damage,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nullable Vector3d position
    ) {
        HyforgedActiveEffectsComponent activeEffects = accessor.getComponent(killer, activeEffectsType);
        if (activeEffects == null || activeEffects.isEmpty()) {
            return;
        }

        Map<String, HyforgedActiveEffectsComponent.ActiveEffectState> effects = activeEffects.getActiveEffects();
        if (effects.isEmpty()) {
            return;
        }

        long nowMs = getNowMillis(accessor);
        for (Map.Entry<String, HyforgedActiveEffectsComponent.ActiveEffectState> entry : effects.entrySet()) {
            HyforgedActiveEffectsComponent.ActiveEffectState state = entry.getValue();
            AffixDefinition definition = affixRegistry.get(state.getAffixId());
            if (definition == null) {
                continue;
            }
            List<AffixTriggeredEffect> triggeredEffects = definition.triggeredEffects();
            int index = state.getEffectIndex();
            if (index < 0 || index >= triggeredEffects.size()) {
                continue;
            }
            AffixTriggeredEffect triggeredEffect = triggeredEffects.get(index);
            AffixTrigger trigger = triggeredEffect.trigger();
            if (!"on_kill".equalsIgnoreCase(trigger.type())) {
                continue;
            }
            if (!matchesTargetTags(victim, trigger.targetTags(), accessor)) {
                continue;
            }
            attemptTrigger(activeEffects, entry.getKey(), state, definition, triggeredEffect, killer, victim, accessor, position, nowMs);
        }
    }

    public void processOnCast(
            @Nonnull Ref<EntityStore> caster,
            @Nullable Ref<EntityStore> target,
            @Nonnull InteractionType interactionType,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nullable Vector3d position
    ) {
        HyforgedActiveEffectsComponent activeEffects = accessor.getComponent(caster, activeEffectsType);
        if (activeEffects == null || activeEffects.isEmpty()) {
            return;
        }

        long nowMs = getNowMillis(accessor);
        for (Map.Entry<String, HyforgedActiveEffectsComponent.ActiveEffectState> entry : activeEffects.getActiveEffects().entrySet()) {
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
            if (!"on_cast".equalsIgnoreCase(trigger.type())) {
                continue;
            }
            if (!matchesInteractionType(trigger.interactionTypes(), interactionType)) {
                continue;
            }
            attemptTrigger(activeEffects, entry.getKey(), state, definition, triggeredEffect, caster, target, accessor, position, nowMs);
        }
    }

    public boolean attemptIntervalTrigger(
            @Nonnull HyforgedActiveEffectsComponent activeEffects,
            @Nonnull String effectKey,
            @Nonnull HyforgedActiveEffectsComponent.ActiveEffectState state,
            @Nonnull AffixDefinition definition,
            @Nonnull AffixTriggeredEffect triggeredEffect,
            @Nonnull Ref<EntityStore> source,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nullable Vector3d position
    ) {
        long nowMs = getNowMillis(accessor);
        return attemptTrigger(activeEffects, effectKey, state, definition, triggeredEffect, source, null, accessor, position, nowMs);
    }

    private void processDamageTrigger(
            @Nonnull Ref<EntityStore> source,
            @Nullable Ref<EntityStore> target,
            @Nonnull Damage damage,
            @Nonnull String triggerType,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nullable Vector3d position
    ) {
        HyforgedActiveEffectsComponent activeEffects = accessor.getComponent(source, activeEffectsType);
        if (activeEffects == null || activeEffects.isEmpty()) {
            return;
        }

        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        String damageCauseId = damageCause != null ? damageCause.getId() : null;
        float damageAmount = damage.getAmount();
        long nowMs = getNowMillis(accessor);

        for (Map.Entry<String, HyforgedActiveEffectsComponent.ActiveEffectState> entry : activeEffects.getActiveEffects().entrySet()) {
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
            if (!triggerType.equalsIgnoreCase(trigger.type())) {
                continue;
            }
            if (trigger.hasDamageCauseFilter() && !matchesDamageCause(trigger.damageCauses(), damageCauseId)) {
                continue;
            }
            if (trigger.minDamage() > 0 && damageAmount < trigger.minDamage()) {
                continue;
            }

            attemptTrigger(activeEffects, entry.getKey(), state, definition, triggeredEffect, source, target, accessor, position, nowMs);
        }
    }

    private boolean attemptTrigger(
            @Nonnull HyforgedActiveEffectsComponent activeEffects,
            @Nonnull String effectKey,
            @Nonnull HyforgedActiveEffectsComponent.ActiveEffectState state,
            @Nonnull AffixDefinition definition,
            @Nonnull AffixTriggeredEffect triggeredEffect,
            @Nonnull Ref<EntityStore> source,
            @Nullable Ref<EntityStore> target,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nullable Vector3d position,
            long nowMs
    ) {
        AffixTrigger trigger = triggeredEffect.trigger();
        int chance = trigger.chance();
        if (chance <= 0) {
            return false;
        }
        if (chance < CombatMath.BPS_100 && !CombatMath.rollChance(chance)) {
            return false;
        }

        if (!isCooldownReady(activeEffects, state, definition, triggeredEffect, nowMs)) {
            return false;
        }

        if (!dispatchTriggeredEvent(source, definition.id(), trigger.type(), target)) {
            return false;
        }

        EffectContext context = new EffectContext(effectKey, source, target, accessor, position);
        boolean executed = executor.execute(triggeredEffect, context);
        if (!executed) {
            return false;
        }

        applyCooldown(activeEffects, state, definition, triggeredEffect, nowMs);
        dispatchExecutedEvent(source, definition.id(), triggeredEffect.effect().type(), target);
        return true;
    }

    private boolean isCooldownReady(
            @Nonnull HyforgedActiveEffectsComponent activeEffects,
            @Nonnull HyforgedActiveEffectsComponent.ActiveEffectState state,
            @Nonnull AffixDefinition definition,
            @Nonnull AffixTriggeredEffect triggeredEffect,
            long nowMs
    ) {
        float cooldownSeconds = triggeredEffect.cooldownSeconds();
        if (cooldownSeconds <= 0f) {
            return true;
        }
        long cooldownMs = Math.round(cooldownSeconds * 1000f);

        if (!triggeredEffect.isSharedCooldown()) {
            return nowMs - state.getLastTriggeredMs() >= cooldownMs;
        }

        String groupKey = resolveCooldownGroup(definition.id(), state.getEffectIndex(), triggeredEffect);
        long lastTriggered = 0L;
        for (HyforgedActiveEffectsComponent.ActiveEffectState other : activeEffects.getActiveEffects().values()) {
            AffixDefinition otherDef = affixRegistry.get(other.getAffixId());
            if (otherDef == null) {
                continue;
            }
            int index = other.getEffectIndex();
            if (index < 0 || index >= otherDef.triggeredEffects().size()) {
                continue;
            }
            AffixTriggeredEffect otherEffect = otherDef.triggeredEffects().get(index);
            if (!groupKey.equals(resolveCooldownGroup(otherDef.id(), index, otherEffect))) {
                continue;
            }
            lastTriggered = Math.max(lastTriggered, other.getLastTriggeredMs());
        }
        return nowMs - lastTriggered >= cooldownMs;
    }

    private void applyCooldown(
            @Nonnull HyforgedActiveEffectsComponent activeEffects,
            @Nonnull HyforgedActiveEffectsComponent.ActiveEffectState state,
            @Nonnull AffixDefinition definition,
            @Nonnull AffixTriggeredEffect triggeredEffect,
            long nowMs
    ) {
        if (!triggeredEffect.isSharedCooldown()) {
            state.setLastTriggeredMs(nowMs);
            return;
        }
        String groupKey = resolveCooldownGroup(definition.id(), state.getEffectIndex(), triggeredEffect);
        for (HyforgedActiveEffectsComponent.ActiveEffectState other : activeEffects.getActiveEffects().values()) {
            AffixDefinition otherDef = affixRegistry.get(other.getAffixId());
            if (otherDef == null) {
                continue;
            }
            int index = other.getEffectIndex();
            if (index < 0 || index >= otherDef.triggeredEffects().size()) {
                continue;
            }
            AffixTriggeredEffect otherEffect = otherDef.triggeredEffects().get(index);
            if (groupKey.equals(resolveCooldownGroup(otherDef.id(), index, otherEffect))) {
                other.setLastTriggeredMs(nowMs);
            }
        }
    }

    private boolean dispatchTriggeredEvent(
            @Nonnull Ref<EntityStore> source,
            @Nonnull String affixId,
            @Nonnull String triggerType,
            @Nullable Ref<EntityStore> target
    ) {
        try {
            EffectAffixTriggeredEvent event = new EffectAffixTriggeredEvent(source, affixId, triggerType, target);
            HytaleServer.get().getEventBus().dispatchFor(EffectAffixTriggeredEvent.class).dispatch(event);
            return !event.isCancelled();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to dispatch EffectAffixTriggeredEvent", e);
            return true;
        }
    }

    private void dispatchExecutedEvent(
            @Nonnull Ref<EntityStore> source,
            @Nonnull String affixId,
            @Nonnull String effectType,
            @Nullable Ref<EntityStore> target
    ) {
        try {
            EffectAffixExecutedEvent event = new EffectAffixExecutedEvent(source, affixId, effectType, target);
            HytaleServer.get().getEventBus().dispatchFor(EffectAffixExecutedEvent.class).dispatch(event);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to dispatch EffectAffixExecutedEvent", e);
        }
    }

    private boolean matchesDamageCause(@Nonnull List<String> filters, @Nullable String causeId) {
        if (filters.isEmpty()) {
            return true;
        }
        if (causeId == null) {
            return false;
        }
        for (String filter : filters) {
            if (filter != null && filter.equalsIgnoreCase(causeId)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesInteractionType(@Nonnull List<String> filters, @Nonnull InteractionType interactionType) {
        if (filters.isEmpty()) {
            return true;
        }
        String name = interactionType.name().toLowerCase(Locale.ROOT);
        for (String filter : filters) {
            if (filter != null && filter.trim().toLowerCase(Locale.ROOT).equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesTargetTags(
            @Nonnull Ref<EntityStore> target,
            @Nonnull List<String> tags,
            @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        if (tags.isEmpty()) {
            return true;
        }
        if (!target.isValid()) {
            return false;
        }
        Set<String> candidates = new HashSet<>();
        collectTargetTags(target, accessor, candidates);
        if (candidates.isEmpty()) {
            return false;
        }

        for (String tag : tags) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            for (String candidate : candidates) {
                if (candidate != null && !candidate.isBlank() && candidate.equalsIgnoreCase(tag)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void collectTargetTags(
            @Nonnull Ref<EntityStore> target,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Set<String> output
    ) {
        String entityType = resolveEntityTypeId(target, accessor);
        if (entityType != null && !entityType.isBlank()) {
            addTagVariants(output, "Type", entityType);
        }

        NPCEntity npcEntity = accessor.getComponent(target, NPCEntity.getComponentType());
        String roleName = npcEntity != null ? npcEntity.getRoleName() : null;
        if (roleName != null && !roleName.isBlank()) {
            addTagVariants(output, "Role", roleName);
        }

        HyforgedNPCQualityComponent quality = accessor.getComponent(target, HyforgedPlugin.getInstance().getNpcQualityComponentType());
        String qualityId = quality != null ? quality.getQualityId() : null;
        if (qualityId != null && !qualityId.isBlank()) {
            addTagVariants(output, "Quality", qualityId);
        }

        collectModelAssetTags(target, accessor, output);
    }

    private void collectModelAssetTags(
            @Nonnull Ref<EntityStore> target,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Set<String> output
    ) {
        ModelComponent modelComponent = accessor.getComponent(target, ModelComponent.getComponentType());
        if (modelComponent == null) {
            return;
        }

        Model model = modelComponent.getModel();
        if (model == null) {
            return;
        }

        String modelAssetId = model.getModelAssetId();
        if (modelAssetId == null || modelAssetId.isBlank()) {
            return;
        }

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelAssetId);
        if (modelAsset == null) {
            return;
        }

        AssetExtraInfo.Data data = ModelAsset.getAssetStore().getCodec().getData(modelAsset);
        if (data == null) {
            return;
        }

        Map<String, String[]> rawTags = data.getRawTags();
        if (rawTags == null || rawTags.isEmpty()) {
            return;
        }

        expandRawTags(rawTags, output);
    }

    private void addTagVariants(@Nonnull Set<String> output, @Nonnull String category, @Nonnull String value) {
        if (value.isBlank()) {
            return;
        }
        output.add(value);
        if (!category.isBlank()) {
            output.add(category);
            output.add(category + "=" + value);
            output.add(category + ":" + value);
        }
    }

    private void expandRawTags(@Nonnull Map<String, String[]> rawTags, @Nonnull Set<String> output) {
        for (Map.Entry<String, String[]> entry : rawTags.entrySet()) {
            String category = entry.getKey();
            if (category == null || category.isBlank()) {
                continue;
            }

            String[] values = entry.getValue();
            if (values == null || values.length == 0) {
                output.add(category);
                continue;
            }

            output.add(category);
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                output.add(value);
                output.add(category + "=" + value);
                output.add(category + ":" + value);
            }
        }
    }

    private long getNowMillis(@Nonnull ComponentAccessor<EntityStore> accessor) {
        TimeResource time = accessor.getResource(TimeResource.getResourceType());
        if (time == null) {
            return System.currentTimeMillis();
        }
        return time.getNow().toEpochMilli();
    }

    @Nonnull
    private String resolveCooldownGroup(
            @Nonnull String affixId,
            int effectIndex,
            @Nonnull AffixTriggeredEffect triggeredEffect
    ) {
        if (triggeredEffect.isSharedCooldown()) {
            String group = triggeredEffect.sharedCooldownGroup();
            if (group != null && !group.isBlank()) {
                return "shared:" + group;
            }
        }
        return "affix:" + affixId + ":" + effectIndex;
    }

    @Nullable
    private String resolveEntityTypeId(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        Archetype<EntityStore> archetype = accessor.getArchetype(ref);
        if (archetype == null) {
            return null;
        }
        for (int i = archetype.getMinIndex(); i < archetype.length(); i++) {
            ComponentType<EntityStore, ?> componentType = archetype.get(i);
            if (componentType == null) {
                continue;
            }
            Class<?> typeClass = componentType.getTypeClass();
            if (Entity.class.isAssignableFrom(typeClass)) {
                return EntityModule.get().getIdentifier(typeClass.asSubclass(Entity.class));
            }
        }
        return null;
    }
}
