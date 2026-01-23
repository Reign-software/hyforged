package reign.software.hyforged.quality.service;

import com.hypixel.hytale.server.core.asset.type.responsecurve.config.ResponseCurve;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import reign.software.hyforged.quality.model.QualityEligibilityRule;
import reign.software.hyforged.quality.model.QualityModifierConfig;
import reign.software.hyforged.quality.model.QualityRollContext;
import reign.software.hyforged.quality.model.QualityWeightProfile;
import reign.software.hyforged.quality.model.QualityWeightTable;
import reign.software.hyforged.quality.registry.QualityEligibilityRegistry;
import reign.software.hyforged.quality.registry.QualityModifierRegistry;
import reign.software.hyforged.quality.registry.QualityWeightRegistry;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for rolling item quality based on context and configured weights.
 */
public final class QualityRollerService {

    private static final Logger LOGGER = Logger.getLogger(QualityRollerService.class.getName());

    private static final QualityModifierConfig FALLBACK_MODIFIERS = new QualityModifierConfig(
            "fallback",
            "Fallback quality modifiers",
            new QualityModifierConfig.LevelScalingConfig(false, "", Collections.emptyMap()),
            new QualityModifierConfig.ItemRarityConfig(false, "", 0.0, 0, 0),
            new QualityModifierConfig.NpcQualityBonusConfig(false, Collections.emptyMap())
    );

    private static final int MAX_LEVEL_DEFAULT = 100;

    private final QualityEligibilityRegistry eligibilityRegistry;
    private final QualityWeightRegistry weightRegistry;
    private final QualityModifierRegistry modifierRegistry;

    public QualityRollerService() {
        this(QualityEligibilityRegistry.get(), QualityWeightRegistry.get(), QualityModifierRegistry.get());
    }

    public QualityRollerService(
            @Nonnull QualityEligibilityRegistry eligibilityRegistry,
            @Nonnull QualityWeightRegistry weightRegistry,
            @Nonnull QualityModifierRegistry modifierRegistry
    ) {
        this.eligibilityRegistry = Objects.requireNonNull(eligibilityRegistry, "eligibilityRegistry cannot be null");
        this.weightRegistry = Objects.requireNonNull(weightRegistry, "weightRegistry cannot be null");
        this.modifierRegistry = Objects.requireNonNull(modifierRegistry, "modifierRegistry cannot be null");
    }

    @Nullable
    public String rollQuality(@Nonnull QualityRollContext context) {
        return rollQuality(context, new Random());
    }

    @Nullable
    public String rollQuality(@Nonnull QualityRollContext context, long seed) {
        return rollQuality(context, new Random(seed));
    }

    @Nullable
    public String rollQuality(@Nonnull QualityRollContext context, @Nonnull Random random) {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(random, "random cannot be null");

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Rolling quality for item {0} (sourceType={1}, sourceLevel={2}, npcQuality={3})",
                new Object[]{
                    context.itemId(),
                    normalizeSourceType(context.sourceType()),
                    context.sourceLevel(),
                    context.npcQuality()
                }
            );
        }

        QualityEligibilityRule rule = eligibilityRegistry.resolve(context);
        if (rule == null) {
            return null;
        }

        QualityWeightProfile profile = weightRegistry.get(rule.weightProfileId());
        if (profile == null) {
            LOGGER.log(Level.FINE, "Missing quality weight profile: {0}", rule.weightProfileId());
            return null;
        }

        Map<String, Integer> weights = buildBaseWeights(profile);
        if (weights.isEmpty()) {
            return null;
        }

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER, "Base quality weights for profile {0}: {1}", new Object[]{profile.id(), weights});
        }

        QualityModifierConfig modifiers = resolveModifiers(rule);
        applyModifiers(weights, modifiers, context);

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER, "Quality weights after modifiers: {0}", weights);
        }

        QualityMetrics.get().recordRollAttempt(normalizeSourceType(context.sourceType()));

        QualityWeightTable table = QualityWeightTable.fromWeights(weights);
        String rolled = table.roll(random);
        if (rolled != null) {
            QualityMetrics.get().recordRollSuccess(rolled);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Rolled quality {0} for item {1}", new Object[]{rolled, context.itemId()});
            }
        }
        return rolled;
    }

    @Nonnull
    public List<String> getEligibleQualities(@Nonnull QualityRollContext context) {
        Objects.requireNonNull(context, "context cannot be null");

        QualityEligibilityRule rule = eligibilityRegistry.resolve(context);
        if (rule == null) {
            return List.of();
        }

        QualityWeightProfile profile = weightRegistry.get(rule.weightProfileId());
        if (profile == null) {
            return List.of();
        }

        Map<String, Integer> weights = buildBaseWeights(profile);
        if (weights.isEmpty()) {
            return List.of();
        }

        List<String> qualities = new ArrayList<>(weights.keySet());
        Map<String, Integer> order = weightRegistry.getQualityOrder();
        qualities.sort((a, b) -> Integer.compare(order.getOrDefault(a, Integer.MAX_VALUE), order.getOrDefault(b, Integer.MAX_VALUE)));
        return List.copyOf(qualities);
    }

    @Nonnull
    private Map<String, Integer> buildBaseWeights(@Nonnull QualityWeightProfile profile) {
        Map<String, Integer> result = new HashMap<>();
        Set<String> eligibleQualities = weightRegistry.getEquipmentEligibleQualities();

        for (Map.Entry<String, Integer> entry : profile.weights().entrySet()) {
            String qualityId = entry.getKey();
            Integer weight = entry.getValue();
            if (qualityId == null || qualityId.isBlank() || weight == null || weight <= 0) {
                continue;
            }
            if (!profile.isQualityAllowed(qualityId)) {
                continue;
            }
            if (!eligibleQualities.contains(qualityId)) {
                continue;
            }
            result.put(qualityId, weight);
        }

        return result;
    }

    @Nonnull
    private QualityModifierConfig resolveModifiers(@Nonnull QualityEligibilityRule rule) {
        QualityModifierConfig config = modifierRegistry.getDefault(FALLBACK_MODIFIERS);
        return config.applyOverrides(rule.modifierOverrides());
    }

    private void applyModifiers(
            @Nonnull Map<String, Integer> weights,
            @Nonnull QualityModifierConfig modifiers,
            @Nonnull QualityRollContext context
    ) {
        applyLevelScaling(weights, modifiers.levelScaling(), context);
        applyItemRarity(weights, modifiers.itemRarity(), context);
        applyNpcQualityBonus(weights, modifiers.npcQualityBonus(), context);
    }

    private void applyLevelScaling(
            @Nonnull Map<String, Integer> weights,
            @Nonnull QualityModifierConfig.LevelScalingConfig config,
            @Nonnull QualityRollContext context
    ) {
        if (!config.enabled()) {
            return;
        }

        int level = context.sourceLevel();
        if (level <= 0) {
            return;
        }

        double curveMultiplier = resolveCurveMultiplier(config.curveId(), level);
        if (curveMultiplier <= 0.0) {
            return;
        }

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(
                    Level.FINER,
                    "Applying level scaling (level={0}, curve={1}, multiplier={2})",
                    new Object[]{level, config.curveId(), curveMultiplier}
            );
        }

        for (Map.Entry<String, Double> entry : config.qualityBonusPerLevel().entrySet()) {
            String quality = entry.getKey();
            Double perLevelBonus = entry.getValue();
            if (quality == null || perLevelBonus == null || perLevelBonus <= 0.0) {
                continue;
            }
            if (!weights.containsKey(quality)) {
                continue;
            }
            double rawBonus = perLevelBonus * level * curveMultiplier;
            int bonus = (int) Math.round(rawBonus);
            if (bonus <= 0) {
                continue;
            }
            int current = weights.getOrDefault(quality, 0);
            weights.put(quality, Math.max(0, current + bonus));
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Level scaling bonus applied: {0} +{1}", new Object[]{quality, bonus});
            }
        }
    }

    private void applyItemRarity(
            @Nonnull Map<String, Integer> weights,
            @Nonnull QualityModifierConfig.ItemRarityConfig config,
            @Nonnull QualityRollContext context
    ) {
        if (!config.enabled()) {
            return;
        }

        int rarityValue = resolveItemRarityValue(config, context.playerRef());
        if (rarityValue <= 0) {
            return;
        }

        int resolvedBonus = (int) Math.round(rarityValue * config.scalingFactor());
        if (config.maxBonus() > 0) {
            resolvedBonus = Math.min(resolvedBonus, config.maxBonus());
        }

        if (resolvedBonus <= 0) {
            return;
        }

        // Apply the total bonus budget to higher-tier qualities only.
        // Higher-tier qualities (lower order number) receive proportionally larger shares.
        Map<String, Integer> order = weightRegistry.getQualityOrder();
        List<String> qualitiesByOrder = new ArrayList<>(weights.keySet());
        qualitiesByOrder.sort((a, b) -> Integer.compare(
                order.getOrDefault(a, Integer.MAX_VALUE),
                order.getOrDefault(b, Integer.MAX_VALUE)
        ));

        // Exclude the lowest tier (highest order) from rarity bonus
        if (qualitiesByOrder.size() <= 1) {
            return;
        }
        qualitiesByOrder.remove(qualitiesByOrder.size() - 1);

        // Distribute bonus weighted by inverse order (higher tiers get more)
        int totalInverseOrder = 0;
        int maxOrder = qualitiesByOrder.size();
        for (int i = 0; i < qualitiesByOrder.size(); i++) {
            totalInverseOrder += (maxOrder - i);
        }

        int remainingBonus = resolvedBonus;
        for (int i = 0; i < qualitiesByOrder.size() && remainingBonus > 0; i++) {
            String quality = qualitiesByOrder.get(i);
            int share = (int) Math.round((double) resolvedBonus * (maxOrder - i) / totalInverseOrder);
            share = Math.min(share, remainingBonus);
            if (share > 0) {
                int current = weights.getOrDefault(quality, 0);
                weights.put(quality, current + share);
                remainingBonus -= share;
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Item rarity bonus applied: {0} +{1}", new Object[]{quality, share});
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER, "Applied item rarity bonus total={0} (statId={1})", new Object[]{resolvedBonus, config.statId()});
        }
    }

    private void applyNpcQualityBonus(
            @Nonnull Map<String, Integer> weights,
            @Nonnull QualityModifierConfig.NpcQualityBonusConfig config,
            @Nonnull QualityRollContext context
    ) {
        if (!config.enabled()) {
            return;
        }

        String npcQuality = context.npcQuality();
        if (npcQuality == null || npcQuality.isBlank()) {
            return;
        }

        Map<String, Integer> order = weightRegistry.getQualityOrder();
        Integer npcOrder = order.get(npcQuality);
        if (npcOrder == null) {
            return;
        }

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER, "Applying NPC quality bonus for tier {0}", npcQuality);
        }

        for (Map.Entry<String, Integer> entry : config.bonusPerTier().entrySet()) {
            String quality = entry.getKey();
            Integer bonus = entry.getValue();
            if (quality == null || bonus == null || bonus <= 0) {
                continue;
            }
            if (!weights.containsKey(quality)) {
                continue;
            }
            Integer qualityOrder = order.get(quality);
            if (qualityOrder == null || qualityOrder < npcOrder) {
                continue;
            }
            int current = weights.getOrDefault(quality, 0);
            weights.put(quality, Math.max(0, current + bonus));
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "NPC quality bonus applied: {0} +{1}", new Object[]{quality, bonus});
            }
        }
    }

    private int resolveItemRarityValue(
            @Nonnull QualityModifierConfig.ItemRarityConfig config,
            @Nullable Ref<EntityStore> playerRef
    ) {
        String statId = config.statId();
        if (statId == null || statId.isBlank() || playerRef == null || !playerRef.isValid()) {
            return config.fallbackValue();
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int statIndex;
        try {
            statIndex = registry.getIndex(StatId.parse(statId));
        } catch (IllegalArgumentException e) {
            return config.fallbackValue();
        }

        if (statIndex < 0) {
            return config.fallbackValue();
        }

        return StatAccessor.getStatValueInt(playerRef.getStore(), playerRef, statIndex);
    }

    private double resolveCurveMultiplier(@Nonnull String curveId, int level) {
        if (curveId == null || curveId.isBlank()) {
            return 1.0;
        }

        int index = ResponseCurve.getAssetMap().getIndex(curveId);
        ResponseCurve curve = ResponseCurve.getAssetMap().getAsset(index);
        if (curve == null) {
            LOGGER.log(Level.FINE, "Missing response curve for quality scaling: {0}", curveId);
            return 1.0;
        }

        double normalized = normalizeLevel(level, MAX_LEVEL_DEFAULT);
        double value;
        try {
            value = curve.computeY(normalized);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to compute response curve for quality scaling", e);
            return 1.0;
        }

        return clamp01(value);
    }

    private double normalizeLevel(int level, int maxLevel) {
        if (level <= 1) {
            return 0.0;
        }
        if (maxLevel <= 1) {
            return 1.0;
        }
        double clamped = Math.min(level, maxLevel);
        return (clamped - 1.0) / (maxLevel - 1.0);
    }

    private double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    @Nonnull
    private String normalizeSourceType(@Nullable String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            return "unknown";
        }
        return sourceType;
    }
}
