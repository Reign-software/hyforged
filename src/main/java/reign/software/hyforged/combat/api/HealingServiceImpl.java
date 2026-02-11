package reign.software.hyforged.combat.api;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.combat.log.CombatEvent;
import reign.software.hyforged.combat.log.CombatLogService;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the {@link HealingService} API.
 * <p>
 * Provides programmatic healing application with stat modifier support.
 * This class is thread-safe and uses a singleton pattern.
 * 
 * <h2>Healing Formula</h2>
 * <pre>
 * finalHealing = baseHealing
 *     * (1 + healerEffectiveness / 10000)
 *     * (1 + targetHealingReceived / 10000)
 *     * (1 + targetLifeRecoveryRate / 10000)
 * </pre>
 */
public final class HealingServiceImpl implements HealingService {

    private static final Logger LOGGER = Logger.getLogger(HealingServiceImpl.class.getName());
    
    private static final HealingServiceImpl INSTANCE = new HealingServiceImpl();

    // Cached stat IDs
    private static final StatId HEALING_EFFECTIVENESS = StatId.hyforged("healing-effectiveness-bps");
    private static final StatId HEALING_RECEIVED = StatId.hyforged("healing-received-bps");
    private static final StatId LIFE_RECOVERY_RATE = StatId.hyforged("life-recovery-rate-bps");

    // Cached stat indices (lazily initialized)
    private int healingEffectivenessIndex = -1;
    private int healingReceivedIndex = -1;
    private int lifeRecoveryRateIndex = -1;
    private volatile boolean indicesCached = false;

    private HealingServiceImpl() {
        // Singleton
    }

    /**
     * Get the singleton instance.
     *
     * @return The HealingServiceImpl instance
     */
    @Nonnull
    public static HealingServiceImpl getInstance() {
        return INSTANCE;
    }

    @Nonnull
    @Override
    public HealingResult applyHealing(
            @Nonnull Ref<EntityStore> healerRef,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull HealingSpec spec,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        return applyHealingInternal(healerRef, targetRef, spec, commandBuffer, true);
    }

    @Nonnull
    @Override
    public HealingResult applyHealing(
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull HealingSpec spec,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        return applyHealingInternal(null, targetRef, spec, commandBuffer, true);
    }

    @Nonnull
    @Override
    public HealingResult applyHealingImmediate(
            @Nullable Ref<EntityStore> healerRef,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull HealingSpec spec,
            @Nonnull ComponentAccessor<EntityStore> componentAccessor
    ) {
        return applyHealingInternal(healerRef, targetRef, spec, componentAccessor, true);
    }

    @Nonnull
    @Override
    public HealingResult calculateHealing(
            @Nullable Ref<EntityStore> healerRef,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull HealingSpec spec,
            @Nonnull ComponentAccessor<EntityStore> componentAccessor
    ) {
        return applyHealingInternal(healerRef, targetRef, spec, componentAccessor, false);
    }

    /**
     * Core healing logic.
     *
     * @param healerRef Optional healer reference
     * @param targetRef Target to heal
     * @param spec Healing specification
     * @param bufferOrAccessor CommandBuffer or ComponentAccessor
     * @param applyHealing Whether to actually apply healing (false = preview only)
     * @return Healing result
     */
    @Nonnull
    private HealingResult applyHealingInternal(
            @Nullable Ref<EntityStore> healerRef,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull HealingSpec spec,
            @Nonnull Object bufferOrAccessor,
            boolean applyHealing
    ) {
        ensureIndicesCached();

        // Get component accessor or command buffer
        ComponentAccessor<EntityStore> accessor = null;
        CommandBuffer<EntityStore> commandBuffer = null;
        
        if (bufferOrAccessor instanceof ComponentAccessor) {
            @SuppressWarnings("unchecked")
            ComponentAccessor<EntityStore> castAccessor = (ComponentAccessor<EntityStore>) bufferOrAccessor;
            accessor = castAccessor;
        } else if (bufferOrAccessor instanceof CommandBuffer) {
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> castBuffer = (CommandBuffer<EntityStore>) bufferOrAccessor;
            commandBuffer = castBuffer;
            // Use command buffer as accessor
            accessor = commandBuffer;
        }

        if (accessor == null) {
            LOGGER.warning("No valid accessor provided for healing");
            return HealingResult.invalidTarget();
        }

        // Validate target reference
        if (!targetRef.isValid()) {
            return HealingResult.invalidTarget();
        }

        // Check if target is dead
        if (accessor.getArchetype(targetRef).contains(DeathComponent.getComponentType())) {
            return HealingResult.targetDead();
        }

        // Get EntityStatMap for target (Hytale's stat component)
        EntityStatMap targetStatMap = accessor.getComponent(targetRef, EntityStatMap.getComponentType());
        if (targetStatMap == null) {
            return HealingResult.invalidTarget();
        }

        // Get current and max health
        int healthIndex = DefaultEntityStatTypes.getHealth();
        com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue healthStat = targetStatMap.get(healthIndex);
        if (healthStat == null) {
            return HealingResult.invalidTarget();
        }

        float currentHealth = healthStat.get();
        float maxHealth = healthStat.getMax();
        float missingHealth = maxHealth - currentHealth;

        // Check if already at full health
        if (missingHealth <= 0) {
            return HealingResult.alreadyFull(spec.getAmount());
        }

        // Get healer's healing effectiveness
        int healerEffectivenessBps = 0;
        if (healerRef != null && healerRef.isValid()) {
            if (healingEffectivenessIndex >= 0) {
                healerEffectivenessBps = StatAccessor.getStatValueInt(
                    healerRef.getStore(),
                    healerRef,
                    healingEffectivenessIndex
                );
            }
        }

        // Get target's healing received and recovery rate
        int healingReceivedBps = 0;
        int recoveryRateBps = 0;
        if (!spec.isSkipHealingReceived() && healingReceivedIndex >= 0) {
            healingReceivedBps = StatAccessor.getStatValueInt(
                targetRef.getStore(),
                targetRef,
                healingReceivedIndex
            );
        }
        if (!spec.isSkipRecoveryRate() && lifeRecoveryRateIndex >= 0) {
            recoveryRateBps = StatAccessor.getStatValueInt(
                targetRef.getStore(),
                targetRef,
                lifeRecoveryRateIndex
            );
        }

        // Calculate final healing
        float baseAmount = spec.getAmount();
        float finalAmount = calculateFinalHealing(baseAmount, healerEffectivenessBps, healingReceivedBps, recoveryRateBps);

        // Cap at missing health
        float actualHealing = Math.min(finalAmount, missingHealth);
        float overheal = spec.isOverhealAllowed() ? Math.max(0, finalAmount - actualHealing) : 0;

        // Build result
        HealingResult.Builder resultBuilder = HealingResult.builder()
                .outcome(applyHealing ? HealingResult.Outcome.HEALED : HealingResult.Outcome.PREVIEW)
                .baseAmount(baseAmount)
                .finalAmount(finalAmount)
                .actualHealing(actualHealing)
                .overheal(overheal)
                .healerEffectivenessBps(healerEffectivenessBps)
                .targetHealingReceivedBps(healingReceivedBps)
                .targetRecoveryRateBps(recoveryRateBps)
                .source(spec.getSource());

        // Apply healing if not just a preview
        if (applyHealing && actualHealing > 0) {
            targetStatMap.addStatValue(EntityStatMap.Predictable.SELF, healthIndex, actualHealing);
            
            // Log to combat log if requested
            if (spec.isLogToCombatLog()) {
                recordHealingToCombatLog(healerRef, targetRef, spec, resultBuilder.build(), accessor);
            }
        }

        return resultBuilder.build();
    }

    /**
     * Calculate final healing amount with all modifiers.
     *
     * <pre>
     * finalHealing = baseHealing
     *     * (1 + healerEffectiveness / 10000)
     *     * (1 + targetHealingReceived / 10000)
     *     * (1 + targetLifeRecoveryRate / 10000)
     * </pre>
     *
     * @param baseAmount Base healing amount
     * @param effectivenessBps Healer's healing-effectiveness-bps (can be negative)
     * @param receivedBps Target's healing-received-bps (can be negative)
     * @param recoveryRateBps Target's life-recovery-rate-bps (can be negative)
     * @return Final healing amount (minimum 0)
     */
    public static float calculateFinalHealing(float baseAmount, int effectivenessBps, int receivedBps, int recoveryRateBps) {
        if (baseAmount <= 0) {
            return 0;
        }

        float multiplier = 1.0f;
        
        // Apply healer effectiveness
        multiplier *= (1.0f + effectivenessBps / 10000.0f);
        
        // Apply target healing received
        multiplier *= (1.0f + receivedBps / 10000.0f);
        
        // Apply target recovery rate
        multiplier *= (1.0f + recoveryRateBps / 10000.0f);

        // Final healing can't be negative
        return Math.max(0, baseAmount * multiplier);
    }

    /**
     * Record healing event to combat log.
     */
    private void recordHealingToCombatLog(
            @Nullable Ref<EntityStore> healerRef,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull HealingSpec spec,
            @Nonnull HealingResult result,
            @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        // Get UUIDs
        UUID healerUuid = healerRef != null ? getEntityUuid(healerRef, accessor) : null;
        UUID targetUuid = getEntityUuid(targetRef, accessor);
        
        if (targetUuid == null) {
            return;
        }

        String healerName = spec.getSource() != null ? spec.getSource() : "Unknown";
        String targetName = "Target";

        // Create healing event for combat log
        // Note: We're using CombatEvent with a special "Healing" damage cause
        // A more complete implementation might have a separate HealingEvent class
        CombatEvent healingEvent = new CombatEvent.Builder()
                .timestamp(System.currentTimeMillis())
                .attackerUuid(healerUuid) // healer
                .attackerName(healerName)
                .defenderUuid(targetUuid) // target
                .defenderName(targetName)
                .damageCauseId("Healing")
                .baseDamage(-result.getBaseAmount()) // Negative = healing
                .finalDamage(-result.getActualHealing())
                .missed(false)
                .blocked(false)
                .autoBlocked(false)
                .criticalHit(false)
                .critMultiplierBps(0)
                .build();

        // Record for target
        CombatLogService.get().recordEvent(targetUuid, healingEvent);

        // Record for healer if it's a player
        if (healerUuid != null) {
            CombatLogService.get().recordEvent(healerUuid, healingEvent);
        }
    }

    /**
     * Get entity UUID from reference.
     */
    @Nullable
    private UUID getEntityUuid(@Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (!ref.isValid()) {
            return null;
        }
        UUIDComponent uuidComp = accessor.getComponent(ref, UUIDComponent.getComponentType());
        return uuidComp != null ? uuidComp.getUuid() : null;
    }

    /**
     * Initialize cached stat indices.
     */
    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        
        healingEffectivenessIndex = registry.getIndex(HEALING_EFFECTIVENESS);
        if (healingEffectivenessIndex < 0) {
            LOGGER.log(Level.FINE, "Healing effectiveness stat not found: " + HEALING_EFFECTIVENESS);
        }

        healingReceivedIndex = registry.getIndex(HEALING_RECEIVED);
        if (healingReceivedIndex < 0) {
            LOGGER.log(Level.FINE, "Healing received stat not found: " + HEALING_RECEIVED);
        }

        lifeRecoveryRateIndex = registry.getIndex(LIFE_RECOVERY_RATE);
        if (lifeRecoveryRateIndex < 0) {
            LOGGER.log(Level.FINE, "Life recovery rate stat not found: " + LIFE_RECOVERY_RATE);
        }

        indicesCached = true;
    }
}
