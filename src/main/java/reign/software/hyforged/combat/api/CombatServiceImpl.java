package reign.software.hyforged.combat.api;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.combat.CombatMath;
import reign.software.hyforged.combat.CombatMeta;
import reign.software.hyforged.combat.HyforgedAutoBlockSystem;
import reign.software.hyforged.combat.HyforgedCriticalHitSystem;
import reign.software.hyforged.combat.ailment.AilmentDefinition;
import reign.software.hyforged.combat.ailment.AilmentRegistry;
import reign.software.hyforged.combat.log.CombatEvent;
import reign.software.hyforged.combat.log.CombatLogService;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.bridge.ProgressionStatBridge;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.damage.DamageTypeExtensionRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the {@link CombatService} API.
 * <p>
 * Provides programmatic damage application with full combat pipeline resolution.
 * This class is thread-safe and uses a singleton pattern.
 */
public final class CombatServiceImpl implements CombatService {

    private static final Logger LOGGER = Logger.getLogger(CombatServiceImpl.class.getName());
    
    private static final CombatServiceImpl INSTANCE = new CombatServiceImpl();

    // Cached stat IDs
    private static final StatId ACCURACY_RATING = StatId.hyforged("accuracy-rating");
    private static final StatId EVASION_CHANCE = StatId.hyforged("evasion-chance-bps");
    private static final StatId BLOCK_CHANCE = StatId.hyforged("block-chance-bps");
    private static final StatId BLOCK_MITIGATION = StatId.hyforged("block-mitigation-bps");
    private static final StatId CRIT_CHANCE = StatId.hyforged("crit-chance-bps");
    private static final StatId CRIT_MULTIPLIER = StatId.hyforged("crit-multiplier-bps");
    private static final StatId EFFECT_DURATION = StatId.hyforged("effect-duration-bps");

    // Cached stat indices (lazily initialized)
    private int accuracyIndex = -1;
    private int evasionIndex = -1;
    private int blockChanceIndex = -1;
    private int blockMitigationIndex = -1;
    private int critChanceIndex = -1;
    private int critMultiplierIndex = -1;
    private int effectDurationIndex = -1;
    private boolean indicesCached = false;

    // Resistance/penetration cache per damage type
    private final Map<String, Integer> resistanceStatIndices = new HashMap<>();
    private final Map<String, Integer> penetrationStatIndices = new HashMap<>();

    private CombatServiceImpl() {
        // Singleton
    }

    /**
     * Get the singleton instance.
     *
     * @return The CombatServiceImpl instance
     */
    @Nonnull
    public static CombatServiceImpl getInstance() {
        return INSTANCE;
    }

    @Nonnull
    @Override
    public CombatResult applyDamage(
            @Nonnull Ref<EntityStore> attackerRef,
            @Nonnull Ref<EntityStore> defenderRef,
            @Nonnull DamageSpec spec,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Calculate damage and then apply via command buffer
        CombatResult result = calculateDamageInternal(attackerRef, defenderRef, spec, commandBuffer, true);
        
        // Record to combat log
        recordToCombatLog(attackerRef, defenderRef, spec, result, commandBuffer);
        
        return result;
    }

    @Nonnull
    @Override
    public CombatResult applyEnvironmentalDamage(
            @Nonnull Ref<EntityStore> defenderRef,
            @Nonnull DamageSpec spec,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nullable String sourceDescription
    ) {
        // Environmental damage skips evasion and block by default
        DamageSpec modifiedSpec = DamageSpec.builder()
                .skipEvasion(true)
                .skipBlock(true)
                .noCrit(true) // No crit for environmental
                .sourceDescription(sourceDescription)
                .build();
        
        // Copy damage entries
        for (DamageSpec.DamageEntry entry : spec.getDamageEntries()) {
            modifiedSpec = DamageSpec.builder()
                    .addDamage(entry.damageCauseId(), entry.amount())
                    .skipEvasion(true)
                    .skipBlock(true)
                    .noCrit(true)
                    .sourceDescription(sourceDescription)
                    .build();
        }

        return applyEnvironmentalDamageInternal(defenderRef, spec, commandBuffer, sourceDescription);
    }

    @Nonnull
    private CombatResult applyEnvironmentalDamageInternal(
            @Nonnull Ref<EntityStore> defenderRef,
            @Nonnull DamageSpec spec,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nullable String sourceDescription
    ) {
        ensureIndicesCached();

        CombatResult.Builder resultBuilder = CombatResult.builder()
                .totalBaseDamage(spec.getTotalBaseDamage())
                .timestamp(System.currentTimeMillis());

        // Dispatch environmental damage events
        float totalFinal = 0;
        List<CombatResult.DamageBreakdown> breakdowns = new ArrayList<>();

        for (DamageSpec.DamageEntry entry : spec.getDamageEntries()) {
            DamageCause cause = getDamageCause(entry.damageCauseId());
            if (cause == null) {
                LOGGER.warning("Unknown damage cause: " + entry.damageCauseId());
                continue;
            }

            // Create and dispatch damage event
            Damage damage = new Damage(
                    new Damage.EnvironmentSource(sourceDescription != null ? sourceDescription : entry.damageCauseId()),
                    cause,
                    entry.amount()
            );

            DamageSystems.executeDamage(defenderRef, commandBuffer, damage);
            totalFinal += entry.amount(); // Environmental damage not reduced here

            breakdowns.add(new CombatResult.DamageBreakdown(
                    entry.damageCauseId(),
                    entry.amount(),
                    entry.amount(),
                    0, 0
            ));
        }

        for (CombatResult.DamageBreakdown b : breakdowns) {
            resultBuilder.addDamageBreakdown(b);
        }

        return resultBuilder
                .outcome(CombatResult.Outcome.HIT)
                .totalFinalDamage(totalFinal)
                .build();
    }

    @Nonnull
    @Override
    public CombatResult applyDamageImmediate(
            @Nonnull Ref<EntityStore> attackerRef,
            @Nonnull Ref<EntityStore> defenderRef,
            @Nonnull DamageSpec spec,
            @Nonnull ComponentAccessor<EntityStore> componentAccessor
    ) {
        CombatResult result = calculateDamageInternal(attackerRef, defenderRef, spec, componentAccessor, true);
        recordToCombatLogImmediate(attackerRef, defenderRef, spec, result, componentAccessor);
        return result;
    }

    @Nonnull
    @Override
    public CombatResult calculateDamage(
            @Nonnull Ref<EntityStore> attackerRef,
            @Nonnull Ref<EntityStore> defenderRef,
            @Nonnull DamageSpec spec,
            @Nonnull ComponentAccessor<EntityStore> componentAccessor
    ) {
        return calculateDamageInternal(attackerRef, defenderRef, spec, componentAccessor, false);
    }

    /**
     * Core calculation logic.
     */
    @Nonnull
    private CombatResult calculateDamageInternal(
            @Nonnull Ref<EntityStore> attackerRef,
            @Nonnull Ref<EntityStore> defenderRef,
            @Nonnull DamageSpec spec,
            @Nonnull Object bufferOrAccessor,
            boolean applyDamage
    ) {
        ensureIndicesCached();

        CombatResult.Builder resultBuilder = CombatResult.builder()
                .totalBaseDamage(spec.getTotalBaseDamage())
                .timestamp(System.currentTimeMillis());

        // Try to get component accessor
        ComponentAccessor<EntityStore> accessor = null;
        CommandBuffer<EntityStore> commandBuffer = null;
        
        if (bufferOrAccessor instanceof ComponentAccessor<?> ca) {
            @SuppressWarnings("unchecked") // Safe: we only pass ComponentAccessor<EntityStore> to this method
            ComponentAccessor<EntityStore> typedAccessor = (ComponentAccessor<EntityStore>) ca;
            accessor = typedAccessor;
        } else if (bufferOrAccessor instanceof CommandBuffer<?> cb) {
            @SuppressWarnings("unchecked") // Safe: we only pass CommandBuffer<EntityStore> to this method
            CommandBuffer<EntityStore> typedBuffer = (CommandBuffer<EntityStore>) cb;
            commandBuffer = typedBuffer;
            // For command buffer, we can't directly access components, so we'll use simpler path
        }

        // Get UUIDs for result
        UUID attackerUuid = getEntityUuid(attackerRef, accessor);
        UUID defenderUuid = getEntityUuid(defenderRef, accessor);
        resultBuilder.attackerUuid(attackerUuid).defenderUuid(defenderUuid);

        // Check if defender has death component (already dead)
        if (accessor != null && accessor.getArchetype(defenderRef).contains(DeathComponent.getComponentType())) {
            return CombatResult.targetDead(defenderUuid);
        }

        // Get stat maps
        EntityStatMap attackerStatMap = StatAccessor.getStatMap(attackerRef.getStore(), attackerRef);
        EntityStatMap defenderStatMap = StatAccessor.getStatMap(defenderRef.getStore(), defenderRef);

        // Get levels for formulas
        int attackerLevel = accessor != null ? 
                ProgressionStatBridge.getCharacterLevel(attackerRef, accessor) : 1;
        int defenderLevel = accessor != null ? 
                ProgressionStatBridge.getCharacterLevel(defenderRef, accessor) : 1;

        // Step 1: Hit Resolution (evasion check)
        if (!spec.isSkipEvasion() && attackerStatMap != null && defenderStatMap != null) {
            int accuracy = StatAccessor.getStatValueInt(attackerRef.getStore(), attackerRef, accuracyIndex);
            int evasion = StatAccessor.getStatValueInt(defenderRef.getStore(), defenderRef, evasionIndex);
            int hitChance = CombatMath.calculateHitChance(accuracy, evasion, attackerLevel, defenderLevel);

            if (!CombatMath.rollChance(hitChance)) {
                return CombatResult.evaded(attackerUuid, defenderUuid, spec.getTotalBaseDamage());
            }
        }

        // Step 2: Block check
        boolean blocked = false;
        boolean autoBlocked = false;
        int blockMitigation = 0;

        if (!spec.isSkipBlock() && defenderStatMap != null) {
            int blockChance = StatAccessor.getStatValueInt(defenderRef.getStore(), defenderRef, blockChanceIndex);
            if (CombatMath.rollChance(blockChance)) {
                blocked = true;
                autoBlocked = true;
                blockMitigation = StatAccessor.getStatValueInt(defenderRef.getStore(), defenderRef, blockMitigationIndex);
                if (blockMitigation <= 0) {
                    blockMitigation = 5000; // Default 50%
                }
            }
        }

        resultBuilder.blocked(blocked).autoBlocked(autoBlocked).blockMitigationBps(blockMitigation);

        // Step 3: Critical hit check
        boolean criticalHit = false;
        int critMultiplier = 0;

        if (spec.isForceCrit()) {
            criticalHit = true;
            critMultiplier = attackerStatMap != null
                    ? StatAccessor.getStatValueInt(attackerRef.getStore(), attackerRef, critMultiplierIndex) : 1500;
        } else if (!spec.isNoCrit() && attackerStatMap != null) {
            int critChance = StatAccessor.getStatValueInt(attackerRef.getStore(), attackerRef, critChanceIndex);
            int effectiveCritChance = CombatMath.calculateCritChance(critChance, attackerLevel, defenderLevel);
            
            if (CombatMath.rollChance(effectiveCritChance)) {
                criticalHit = true;
                critMultiplier = StatAccessor.getStatValueInt(attackerRef.getStore(), attackerRef, critMultiplierIndex);
                if (critMultiplier <= 0) {
                    critMultiplier = 1500; // Default 15% bonus
                }
            }
        }

        resultBuilder.criticalHit(criticalHit).critMultiplierBps(critMultiplier);

        // Step 4: Calculate damage per element
        List<CombatResult.DamageBreakdown> breakdowns = new ArrayList<>();
        float totalFinalDamage = 0;
        List<String> ailmentsTriggered = new ArrayList<>();

        for (DamageSpec.DamageEntry entry : spec.getDamageEntries()) {
            float baseDamage = entry.amount();
            float damage = baseDamage;

            // Get resistance and penetration
            int resistance = 0;
            int penetration = 0;

            if (!spec.isSkipResistance() && defenderStatMap != null) {
                resistance = getResistanceForDamageType(defenderRef, entry.damageCauseId());
            }
            if (attackerStatMap != null) {
                penetration = getPenetrationForDamageType(attackerRef, entry.damageCauseId());
            }

            // Apply resistance after penetration
            int effectiveResistance = CombatMath.calculateEffectiveResistance(resistance, penetration);
            if (effectiveResistance > 0) {
                damage = CombatMath.applyReduction(damage, effectiveResistance);
            }

            // Apply block mitigation
            if (blocked && blockMitigation > 0) {
                damage = CombatMath.applyReduction(damage, blockMitigation);
            }

            // Apply crit multiplier
            if (criticalHit && critMultiplier > 0) {
                // Crit multiplier is bonus damage (1500 bps = 15% bonus = 1.15x)
                damage = damage * (CombatMath.BPS_100 + critMultiplier) / CombatMath.BPS_100;
            }

            damage = Math.max(0, damage);
            totalFinalDamage += damage;

            breakdowns.add(new CombatResult.DamageBreakdown(
                    entry.damageCauseId(),
                    baseDamage,
                    damage,
                    resistance,
                    penetration
            ));

            // Check for ailment trigger
            if (!spec.isSkipAilments()) {
                DamageCause damageType = getDamageCause(entry.damageCauseId());
                String elementTag = damageType != null ? 
                        DamageTypeExtensionRegistry.get().getElementTagForDamage(damageType) : null;
                if (elementTag != null) {
                    AilmentDefinition ailment = AilmentRegistry.get().getByElement(elementTag);
                    if (ailment != null) {
                        // Note: actual ailment accumulation happens through the system
                        // Here we just track if threshold would be exceeded
                        if (damage >= ailment.baseThreshold()) {
                            ailmentsTriggered.add(ailment.id());
                        }
                    }
                }
            }

            // Dispatch damage event if applying
            if (applyDamage && damage > 0) {
                DamageCause cause = getDamageCause(entry.damageCauseId());
                if (cause != null) {
                    Damage damageEvent = new Damage(
                            new Damage.EntitySource(attackerRef),
                            cause,
                            damage
                    );

                    // Mark as already processed by CombatService to prevent ECS systems
                    // from re-applying evasion, block, crit, and resistance
                    damageEvent.putMetaObject(CombatMeta.PIPELINE_PROCESSED, true);
                    
                    // Record base damage for combat log
                    damageEvent.putMetaObject(CombatMeta.BASE_DAMAGE, baseDamage);
                    damageEvent.putMetaObject(CombatMeta.RESISTANCE_BPS, resistance);
                    damageEvent.putMetaObject(CombatMeta.PENETRATION_BPS, penetration);
                    damageEvent.putMetaObject(CombatMeta.EFFECTIVE_RESISTANCE_BPS, effectiveResistance);

                    // Set meta flags
                    if (criticalHit) {
                        damageEvent.putMetaObject(HyforgedCriticalHitSystem.CRITICAL_HIT, true);
                        damageEvent.putMetaObject(HyforgedCriticalHitSystem.CRITICAL_MULTIPLIER, critMultiplier);
                        damageEvent.putMetaObject(HyforgedCriticalHitSystem.CRIT_ROLLED, true);
                    }
                    if (blocked) {
                        damageEvent.putMetaObject(Damage.BLOCKED, true);
                        damageEvent.putMetaObject(HyforgedAutoBlockSystem.AUTO_BLOCKED, autoBlocked);
                        damageEvent.putMetaObject(CombatMeta.BLOCK_MITIGATION_BPS, blockMitigation);
                    }

                    if (commandBuffer != null) {
                        DamageSystems.executeDamage(defenderRef, commandBuffer, damageEvent);
                    } else if (accessor != null) {
                        accessor.invoke(defenderRef, damageEvent);
                    }
                }
            }
        }

        // Add all breakdowns and ailments
        for (CombatResult.DamageBreakdown b : breakdowns) {
            resultBuilder.addDamageBreakdown(b);
        }
        for (String a : ailmentsTriggered) {
            resultBuilder.addAilmentTriggered(a);
        }

        // Determine outcome
        CombatResult.Outcome outcome = CombatResult.Outcome.HIT;
        if (blocked && totalFinalDamage <= 0) {
            outcome = CombatResult.Outcome.BLOCKED;
        }

        return resultBuilder
                .outcome(outcome)
                .totalFinalDamage(totalFinalDamage)
                .build();
    }

    /**
     * Get a DamageCause by ID.
     */
    @Nullable
    private DamageCause getDamageCause(@Nonnull String damageCauseId) {
        return DamageCause.getAssetMap().getAsset(damageCauseId);
    }

    /**
     * Get entity UUID.
     */
    @Nullable
    private UUID getEntityUuid(@Nonnull Ref<EntityStore> ref, @Nullable ComponentAccessor<EntityStore> accessor) {
        if (accessor == null) return null;
        UUIDComponent uuidComponent = accessor.getComponent(ref, UUIDComponent.getComponentType());
        return uuidComponent != null ? uuidComponent.getUuid() : null;
    }

    /**
     * Get resistance stat value for a damage type.
     */
    private int getResistanceForDamageType(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull String damageCauseId) {
        Integer cachedIndex = resistanceStatIndices.get(damageCauseId);
        if (cachedIndex != null) {
            return StatAccessor.getStatValueInt(entityRef.getStore(), entityRef, cachedIndex);
        }

        DamageCause damageCause = getDamageCause(damageCauseId);
        if (damageCause == null) return 0;

        StatId resistanceStat = DamageTypeExtensionRegistry.get().getResistanceStatForDamage(damageCause);
        if (resistanceStat == null) return 0;

        int index = StatDefinitionRegistry.get().getIndex(resistanceStat.fullId());
        if (index < 0) return 0;

        resistanceStatIndices.put(damageCauseId, index);
        return StatAccessor.getStatValueInt(entityRef.getStore(), entityRef, index);
    }

    /**
     * Get penetration stat value for a damage type.
     */
    private int getPenetrationForDamageType(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull String damageCauseId) {
        Integer cachedIndex = penetrationStatIndices.get(damageCauseId);
        if (cachedIndex != null) {
            return StatAccessor.getStatValueInt(entityRef.getStore(), entityRef, cachedIndex);
        }

        DamageCause damageCause = getDamageCause(damageCauseId);
        if (damageCause == null) return 0;

        StatId penetrationStat = DamageTypeExtensionRegistry.get().getPenetrationStatForDamage(damageCause);
        if (penetrationStat == null) return 0;

        int index = StatDefinitionRegistry.get().getIndex(penetrationStat.fullId());
        if (index < 0) return 0;

        penetrationStatIndices.put(damageCauseId, index);
        return StatAccessor.getStatValueInt(entityRef.getStore(), entityRef, index);
    }

    /**
     * Record combat result to combat log.
     */
    private void recordToCombatLog(
            @Nonnull Ref<EntityStore> attackerRef,
            @Nonnull Ref<EntityStore> defenderRef,
            @Nonnull DamageSpec spec,
            @Nonnull CombatResult result,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Combat log recording happens automatically through HyforgedCombatLogSystem
        // when damage events are dispatched. This is intentionally left as a no-op
        // for command buffer path.
    }

    /**
     * Record combat result to combat log (immediate).
     */
    private void recordToCombatLogImmediate(
            @Nonnull Ref<EntityStore> attackerRef,
            @Nonnull Ref<EntityStore> defenderRef,
            @Nonnull DamageSpec spec,
            @Nonnull CombatResult result,
            @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        UUID attackerUuid = result.getAttackerUuid();
        UUID defenderUuid = result.getDefenderUuid();

        if (attackerUuid == null && defenderUuid == null) return;

        // Create combat event for log
        for (CombatResult.DamageBreakdown breakdown : result.getDamageBreakdown()) {
            CombatEvent.Builder eventBuilder = CombatEvent.builder()
                    .timestamp(result.getTimestamp())
                    .attackerUuid(attackerUuid)
                    .defenderUuid(defenderUuid)
                    .attackerName(attackerUuid != null ? attackerUuid.toString() : "Unknown")
                    .defenderName(defenderUuid != null ? defenderUuid.toString() : "Unknown")
                    .damageCauseId(breakdown.damageCauseId())
                    .baseDamage(breakdown.baseDamage())
                    .finalDamage(breakdown.finalDamage())
                    .missed(result.wasEvaded())
                    .blocked(result.wasBlocked())
                    .autoBlocked(result.wasAutoBlocked())
                    .criticalHit(result.isCriticalHit())
                    .critMultiplierBps(result.getCritMultiplierBps())
                    .resistanceAppliedBps(breakdown.resistanceBps())
                    .penetrationAppliedBps(breakdown.penetrationBps());

            CombatEvent event = eventBuilder.build();

            // Record for both participants if they're players
            if (attackerUuid != null) {
                CombatLogService.get().recordEvent(attackerUuid, event);
            }
            if (defenderUuid != null) {
                CombatLogService.get().recordEvent(defenderUuid, event);
            }
        }
    }

    /**
     * Ensure stat indices are cached.
     */
    private void ensureIndicesCached() {
        if (indicesCached) return;

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();

        accuracyIndex = getStatIndex(registry, ACCURACY_RATING.toString());
        evasionIndex = getStatIndex(registry, EVASION_CHANCE.toString());
        blockChanceIndex = getStatIndex(registry, BLOCK_CHANCE.toString());
        blockMitigationIndex = getStatIndex(registry, BLOCK_MITIGATION.toString());
        critChanceIndex = getStatIndex(registry, CRIT_CHANCE.toString());
        critMultiplierIndex = getStatIndex(registry, CRIT_MULTIPLIER.toString());
        effectDurationIndex = getStatIndex(registry, EFFECT_DURATION.toString());

        indicesCached = true;
    }

    private int getStatIndex(@Nonnull StatDefinitionRegistry registry, @Nonnull String statId) {
        int index = registry.getIndex(statId);
        if (index < 0) {
            LOGGER.log(Level.WARNING, "Combat stat not found: " + statId);
        }
        return index;
    }

    /**
     * Clear cached indices (useful for testing/reload).
     */
    public void clearCache() {
        indicesCached = false;
        resistanceStatIndices.clear();
        penetrationStatIndices.clear();
    }
}
