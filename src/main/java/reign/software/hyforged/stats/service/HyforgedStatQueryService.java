package reign.software.hyforged.stats.service;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.breakdown.BreakdownEntry;
import reign.software.hyforged.stats.breakdown.ScalingContribution;
import reign.software.hyforged.stats.breakdown.StatBreakdown;
import reign.software.hyforged.stats.component.ConditionalStatModifier;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.component.StatModifier;
import reign.software.hyforged.stats.condition.QueryContext;
import reign.software.hyforged.stats.engine.ScalingEngine;
import reign.software.hyforged.stats.engine.StackingEngine;
import reign.software.hyforged.stats.scaling.ScalingRule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Query helpers for computed Hyforged stats.
 * <p>
 * This keeps computation logic out of ECS components while providing
 * consistent access to context-aware stat values and breakdowns.
 */
public final class HyforgedStatQueryService {

    private HyforgedStatQueryService() {
    }

    /**
     * Get the effective value for a stat with context-aware modifier evaluation.
     * <p>
     * This method evaluates conditional modifiers based on the provided context,
     * including only those modifiers whose conditions are met.
     * <p>
     * Note: This performs on-demand computation and does not use the cached value.
     * For performance-critical paths, prefer using cached values with periodic
     * context updates.
     */
    public static int getEffectiveValue(
            @Nonnull HyforgedStatComponent component,
            int statIndex,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull QueryContext context
    ) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        StatDefinition statDef = registry.getStat(statIndex);

        if (statDef == null) {
            return 0;
        }

        int baseValue;
        if (statDef.hasScaling()) {
            baseValue = ScalingEngine.computeScaledBase(
                statDef,
                component::getCachedValue,
                registry
            );
        } else {
            baseValue = component.getBaseValue(statIndex);
        }

        List<StatModifier> applicableModifiers = new ArrayList<>();
        for (StatModifier mod : component.getModifiers()) {
            if (isModifierApplicable(mod, statIndex, statDef, registry)) {
                applicableModifiers.add(mod);
            }
        }

        for (ConditionalStatModifier condMod : component.getConditionalModifiers()) {
            if (isModifierApplicable(condMod.modifier(), statIndex, statDef, registry)) {
                if (condMod.isUnconditional() || condMod.condition().evaluate(entityRef, context)) {
                    applicableModifiers.add(condMod.modifier());
                }
            }
        }

        return StackingEngine.compute(baseValue, applicableModifiers, statDef);
    }

    /**
     * Get the effective value for a stat by StatId with context-aware evaluation.
     */
    public static int getEffectiveValue(
            @Nonnull HyforgedStatComponent component,
            @Nonnull StatId statId,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull QueryContext context
    ) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int statIndex = registry.getIndex(statId);
        if (statIndex < 0) {
            return 0;
        }
        return getEffectiveValue(component, statIndex, entityRef, context);
    }

    /**
     * Get a detailed breakdown of a stat's value for UI display.
     */
    @Nullable
    public static StatBreakdown getStatBreakdown(
            @Nonnull HyforgedStatComponent component,
            int statIndex,
            int targetLevel
    ) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        StatDefinition statDef = registry.getStat(statIndex);

        if (statDef == null) {
            return null;
        }

        int rawBase = 0;
        List<ScalingContribution> scalingContribs = new ArrayList<>();

        if (statDef.hasScaling()) {
            for (ScalingRule rule : statDef.scaling()) {
                int sourceIndex = registry.getIndex(rule.source());
                if (sourceIndex >= 0) {
                    int sourceValue = component.getCachedValue(sourceIndex);
                    int contribution = ScalingEngine.computeContribution(rule, sourceValue);

                    StatDefinition sourceDef = registry.getStat(sourceIndex);
                    String sourceDisplayName = sourceDef != null ? sourceDef.displayName() : rule.source().fullId();

                    scalingContribs.add(new ScalingContribution(
                        rule.source(),
                        sourceDisplayName,
                        contribution,
                        rule.type()
                    ));
                }
            }
            rawBase = scalingContribs.stream()
                .mapToInt(ScalingContribution::contribution)
                .sum();
        } else {
            rawBase = component.getBaseValue(statIndex);
        }

        int explicitBase = component.hasBaseValue(statIndex) ? component.getBaseValue(statIndex) : 0;
        int scaledBase = rawBase + (statDef.hasScaling() ? explicitBase : 0);

        List<StatModifier> applicable = new ArrayList<>();
        for (StatModifier mod : component.getModifiers()) {
            if (mod.targetStatIndex() == statIndex) {
                applicable.add(mod);
            } else if (mod.targetTagId() != null) {
                if (statDef.tags().contains(mod.targetTagId()) ||
                    registry.getStatIndicesForTag(mod.targetTagId()).contains(statIndex)) {
                    applicable.add(mod);
                }
            }
        }

        StackingEngine.ComputeResult result =
            StackingEngine.computeWithBreakdown(scaledBase, applicable, statDef);

        StatBreakdown.Builder builder = StatBreakdown.builder(statDef.id())
            .from(statDef)
            .baseValue(statDef.hasScaling() ? 0 : rawBase)
            .scalingContributions(scalingContribs)
            .scaledBase(scaledBase)
            .flatTotal(result.flatTotal)
            .afterFlat(result.afterFlat)
            .increasedTotalBps(result.increasedTotalBps)
            .afterIncreased(result.afterIncreased)
            .afterMore(result.afterMore)
            .afterCap(result.afterCap)
            .finalValue(result.finalValue);

        for (StatModifier mod : result.getAllModifiers()) {
            builder.addEntry(new BreakdownEntry(
                mod.sourceId(),
                mod.sourceType(),
                mod.modifierType(),
                mod.value(),
                mod.sourceId()
            ));
        }

        if (statDef.isRating()) {
            int effectiveness = component.getEffectiveness(statIndex, targetLevel);
            builder.effectivenessBps(effectiveness);
        }

        return builder.build();
    }

    /**
     * Get a detailed breakdown of a stat by StatId.
     */
    @Nullable
    public static StatBreakdown getStatBreakdown(
            @Nonnull HyforgedStatComponent component,
            @Nonnull StatId statId,
            int targetLevel
    ) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int statIndex = registry.getIndex(statId);
        if (statIndex < 0) {
            return null;
        }
        return getStatBreakdown(component, statIndex, targetLevel);
    }

    private static boolean isModifierApplicable(
            @Nonnull StatModifier mod,
            int statIdx,
            @Nonnull StatDefinition statDef,
            @Nonnull StatDefinitionRegistry registry
    ) {
        if (mod.targetStatIndex() == statIdx) {
            return true;
        }

        String tagId = mod.targetTagId();
        if (tagId != null) {
            Set<Integer> affectedStats = registry.getStatIndicesForTag(tagId);
            if (affectedStats.contains(statIdx)) {
                return true;
            }
            if (statDef.tags().contains(tagId)) {
                return true;
            }
        }

        return false;
    }
}
