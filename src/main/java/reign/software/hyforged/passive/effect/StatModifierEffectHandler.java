package reign.software.hyforged.passive.effect;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.passive.model.PassiveNode;
import reign.software.hyforged.passive.model.PassiveNodeEffect;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Effect handler for stat-modifier type passive node effects.
 * <p>
 * Adds or removes {@link HyforgedModifier} instances on the entity's
 * {@link HyforgedStatComponent} when nodes are allocated or deallocated.
 * <p>
 * JSON effect format:
 * <pre>
 * {
 *   "Type": "stat-modifier",
 *   "Stat": "hyforged:strength",
 *   "Value": 10,
 *   "StackType": "FLAT"  // Optional, defaults to FLAT
 * }
 * </pre>
 * <p>
 * Value interpretation:
 * - FLAT: Raw value added to base
 * - INCREASED/MORE: Basis points (100 = 1%)
 */
public final class StatModifierEffectHandler implements PassiveEffectHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Effect type identifier.
     */
    public static final String EFFECT_TYPE = "stat-modifier";

    /** Prefix for expanded stat tags that indicate this stat should apply to another target tag. */
    private static final String APPLIES_TO_TAG_PREFIX = "AppliesTo=";

    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    /**
     * Create a new handler.
     *
     * @param statComponentType The component type for HyforgedStatComponent
     */
    public StatModifierEffectHandler(
            @Nonnull ComponentType<EntityStore, HyforgedStatComponent> statComponentType
    ) {
        this.statComponentType = statComponentType;
    }

    @Override
    public void apply(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect) {
        HyforgedStatComponent statComponent = entityRef.getStore().getComponent(entityRef, statComponentType);
        if (statComponent == null) {
            LOGGER.atWarning().log("Entity has no HyforgedStatComponent, cannot apply stat modifier from node: %s", node.id());
            return;
        }

        String statId = effect.getString("Stat");
        if (statId == null) {
            LOGGER.atWarning().log("stat-modifier effect missing 'Stat' field on node: %s", node.id());
            return;
        }

        int value = effect.getInt("Value", 0);
        String stackTypeStr = effect.getString("StackType");
        HyforgedModifier.StackType stackType = parseStackType(stackTypeStr);

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();

        // Resolve stat index
        int statIndex = registry.getIndex(statId);
        if (statIndex < 0) {
            LOGGER.atWarning().log("Unknown stat ID: %s in passive node: %s", statId, node.id());
            return;
        }

        StatDefinition definition = registry.getStat(statId);

        // Create modifier with source = node ID for tracking
        HyforgedModifier.Builder modifierBuilder = HyforgedModifier.builder()
                .stackType(stackType)
                .amount(value)
                .sourceType(HyforgedModifier.SourceType.PASSIVE)
                .sourceId(node.id());

        String appliesToTag = resolveAppliesToTag(definition);
        if (appliesToTag != null && !appliesToTag.isBlank()) {
            int tagIndex = registry.getTagIndex(appliesToTag);
            if (tagIndex != Integer.MIN_VALUE) {
                modifierBuilder.targetTag(tagIndex);
            } else {
                LOGGER.atWarning().log(
                        "Unknown AppliesTo tag '%s' on stat %s; falling back to direct stat target",
                        appliesToTag,
                        statId
                );
                modifierBuilder.targetStat(statIndex);
            }
        } else {
            modifierBuilder.targetStat(statIndex);
        }

        HyforgedModifier modifier = modifierBuilder.build();

        if (statComponent.addModifier(modifier)) {
            LOGGER.at(Level.FINE).log("Applied stat modifier: %s = %s (%s) from node %s", statId, value, stackType, node.id());
        } else {
            LOGGER.atWarning().log("Failed to add modifier for node: %s - possibly at max capacity", node.id());
        }
    }

    @Override
    public void remove(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect) {
        HyforgedStatComponent statComponent = entityRef.getStore().getComponent(entityRef, statComponentType);
        if (statComponent == null) {
            return;
        }

        // Remove all modifiers with this node as source
        if (statComponent.removeModifiersBySource(node.id())) {
            LOGGER.at(Level.FINE).log("Removed stat modifiers from node: %s", node.id());
        }
    }

    @Override
    @Nonnull
    public String getTooltipText(@Nonnull PassiveNodeEffect effect) {
        String statId = effect.getString("Stat");
        int value = effect.getInt("Value", 0);
        String stackTypeStr = effect.getString("StackType");
        HyforgedModifier.StackType stackType = parseStackType(stackTypeStr);

        // Format value based on stack type
        String formattedValue;
        String prefix = value >= 0 ? "+" : "";
        
        switch (stackType) {
            case INCREASED -> {
                double percent = value / 100.0;
                formattedValue = prefix + String.format("%.1f%% increased", percent);
            }
            case MORE -> {
                double percent = value / 100.0;
                formattedValue = prefix + String.format("%.1f%% more", percent);
            }
            default -> {
                formattedValue = prefix + value;
            }
        }

        // Try to get display name from registry
        String displayName = statId;
        if (statId != null) {
            var definition = StatDefinitionRegistry.get().getStat(statId);
            if (definition != null) {
                displayName = definition.displayName();
            }
        }

        return formattedValue + " " + displayName;
    }

    private HyforgedModifier.StackType parseStackType(String stackTypeStr) {
        if (stackTypeStr == null || stackTypeStr.isEmpty()) {
            return HyforgedModifier.StackType.FLAT;
        }
        try {
            return HyforgedModifier.StackType.valueOf(stackTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.atWarning().log("Unknown StackType: %s, defaulting to FLAT", stackTypeStr);
            return HyforgedModifier.StackType.FLAT;
        }
    }

    @Nullable
    private String resolveAppliesToTag(@Nullable StatDefinition definition) {
        if (definition == null || definition.tags().isEmpty()) {
            return null;
        }

        for (String tag : definition.tags()) {
            if (tag != null && tag.startsWith(APPLIES_TO_TAG_PREFIX) && tag.length() > APPLIES_TO_TAG_PREFIX.length()) {
                return tag.substring(APPLIES_TO_TAG_PREFIX.length());
            }
        }

        return null;
    }
}
