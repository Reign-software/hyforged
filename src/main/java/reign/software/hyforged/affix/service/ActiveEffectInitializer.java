package reign.software.hyforged.affix.service;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.component.HyforgedActiveEffectsComponent;
import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.AffixTriggeredEffect;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.affix.service.HyforgedItemDataService;
import reign.software.hyforged.quality.component.HyforgedNPCQualityComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for rebuilding active triggered effects from affix sources.
 */
public final class ActiveEffectInitializer {

    private static final Logger LOGGER = Logger.getLogger(ActiveEffectInitializer.class.getName());

    public static final String SOURCE_EQUIPMENT = "equipment";
    public static final String SOURCE_NPC_QUALITY = "npc_quality";

    private ActiveEffectInitializer() {
        // Utility class
    }

    public static void refreshFromEquipment(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Inventory inventory,
            @Nonnull Store<EntityStore> store
    ) {
        List<EffectSource> sources = new ArrayList<>();

        ItemContainer armor = inventory.getArmor();
        if (armor != null) {
            for (short slot = 0; slot < armor.getCapacity(); slot++) {
                ItemStack itemStack = armor.getItemStack(slot);
                if (itemStack == null || itemStack.isEmpty()) {
                    continue;
                }
                sources.add(new EffectSource(SOURCE_EQUIPMENT, "armor:" + slot, HyforgedItemDataService.read(itemStack).affixes()));
            }
        }

        ItemStack held = inventory.getItemInHand();
        if (held != null && !held.isEmpty()) {
            sources.add(new EffectSource(SOURCE_EQUIPMENT, "hand", HyforgedItemDataService.read(held).affixes()));
        }

        applyActiveEffects(ref, store, sources);
    }

    public static void refreshFromNpcQuality(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull HyforgedNPCQualityComponent qualityComponent,
            @Nonnull Store<EntityStore> store
    ) {
        List<RolledAffix> affixes = qualityComponent.getAffixes();
        if (affixes == null || affixes.isEmpty()) {
            applyActiveEffects(ref, store, List.of());
            return;
        }

        String sourceId = qualityComponent.getQualityId();
        List<EffectSource> sources = List.of(new EffectSource(SOURCE_NPC_QUALITY, sourceId, affixes));
        applyActiveEffects(ref, store, sources);
    }

    private static void applyActiveEffects(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull List<EffectSource> sources
    ) {
        ComponentType<EntityStore, HyforgedActiveEffectsComponent> componentType =
                HyforgedPlugin.getInstance().getActiveEffectsComponentType();

        HyforgedActiveEffectsComponent previous = store.getComponent(ref, componentType);
        Map<String, HyforgedActiveEffectsComponent.ActiveEffectState> nextStates = buildActiveEffects(sources, previous);

        if (nextStates.isEmpty()) {
            if (previous != null) {
                store.removeComponent(ref, componentType);
            }
            return;
        }

        store.putComponent(ref, componentType, new HyforgedActiveEffectsComponent(nextStates));
    }

    @Nonnull
    private static Map<String, HyforgedActiveEffectsComponent.ActiveEffectState> buildActiveEffects(
            @Nonnull List<EffectSource> sources,
            @Nullable HyforgedActiveEffectsComponent previous
    ) {
        Map<String, HyforgedActiveEffectsComponent.ActiveEffectState> previousStates =
                previous != null ? previous.getActiveEffects() : Map.of();

        Map<String, HyforgedActiveEffectsComponent.ActiveEffectState> nextStates = new LinkedHashMap<>();
        Map<String, Integer> stackCounts = new HashMap<>();

        AffixDefinitionRegistry registry = AffixDefinitionRegistry.get();

        for (EffectSource source : sources) {
            if (source.affixes == null || source.affixes.isEmpty()) {
                continue;
            }
            for (RolledAffix affix : source.affixes) {
                AffixDefinition definition = registry.get(affix.affixId());
                if (definition == null || !definition.hasTriggeredEffects()) {
                    continue;
                }

                List<AffixTriggeredEffect> triggeredEffects = definition.triggeredEffects();
                for (int i = 0; i < triggeredEffects.size(); i++) {
                    AffixTriggeredEffect triggeredEffect = triggeredEffects.get(i);
                    String stackGroup = resolveStackGroup(definition.id(), i, triggeredEffect);
                    int maxStacks = Math.max(1, triggeredEffect.maxStacks());
                    int currentStacks = stackCounts.getOrDefault(stackGroup, 0);
                    if (currentStacks >= maxStacks) {
                        continue;
                    }
                    stackCounts.put(stackGroup, currentStacks + 1);

                    String key = buildKey(source.sourceType, source.sourceId, definition.id(), i);
                    HyforgedActiveEffectsComponent.ActiveEffectState previousState = previousStates.get(key);
                    long lastTriggered = previousState != null ? previousState.getLastTriggeredMs() : 0L;
                    float accumulated = previousState != null ? previousState.getAccumulatedTime() : 0f;

                    HyforgedActiveEffectsComponent.ActiveEffectState state =
                            new HyforgedActiveEffectsComponent.ActiveEffectState(
                                    definition.id(),
                                    i,
                                    source.sourceType,
                                    source.sourceId,
                                    lastTriggered,
                                    1,
                                    accumulated
                            );
                    nextStates.put(key, state);
                }
            }
        }

        if (!nextStates.isEmpty()) {
            for (HyforgedActiveEffectsComponent.ActiveEffectState state : nextStates.values()) {
                AffixDefinition definition = registry.get(state.getAffixId());
                if (definition == null) {
                    continue;
                }
                int effectIndex = state.getEffectIndex();
                if (effectIndex < 0 || effectIndex >= definition.triggeredEffects().size()) {
                    continue;
                }
                AffixTriggeredEffect triggeredEffect = definition.triggeredEffects().get(effectIndex);
                String stackGroup = resolveStackGroup(definition.id(), effectIndex, triggeredEffect);
                Integer stacks = stackCounts.get(stackGroup);
                if (stacks != null) {
                    state.setStacks(stacks);
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINER) && !nextStates.isEmpty()) {
            LOGGER.finer("Active effects rebuilt: " + nextStates.size());
        }

        return nextStates;
    }

    @Nonnull
    private static String buildKey(
            @Nonnull String sourceType,
            @Nonnull String sourceId,
            @Nonnull String affixId,
            int effectIndex
    ) {
        return sourceType + ":" + sourceId + ":" + affixId + ":" + effectIndex;
    }

    @Nonnull
    private static String resolveStackGroup(
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

    private record EffectSource(
            @Nonnull String sourceType,
            @Nonnull String sourceId,
            @Nonnull List<RolledAffix> affixes
    ) {
        private EffectSource {
            Objects.requireNonNull(sourceType, "sourceType cannot be null");
            Objects.requireNonNull(sourceId, "sourceId cannot be null");
            Objects.requireNonNull(affixes, "affixes cannot be null");
        }
    }
}
