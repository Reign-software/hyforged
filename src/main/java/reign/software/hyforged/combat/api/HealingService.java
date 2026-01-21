package reign.software.hyforged.combat.api;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Service API for programmatic healing application.
 * <p>
 * The HealingService provides a unified interface for applying healing through
 * the Hyforged stat system, including:
 * <ul>
 *   <li>Healing effectiveness (healer's outgoing healing modifier)</li>
 *   <li>Healing received (target's incoming healing modifier)</li>
 *   <li>Life recovery rate (general recovery scaling)</li>
 *   <li>Combat log integration (optional)</li>
 * </ul>
 * <p>
 * This service should be used for skills, abilities, consumables, and custom
 * healing sources instead of directly modifying health stats.
 *
 * <h2>Healing Formula</h2>
 * <pre>
 * finalHealing = baseHealing
 *     * (1 + healerEffectiveness / 10000)
 *     * (1 + targetHealingReceived / 10000)
 *     * (1 + targetLifeRecoveryRate / 10000)
 * </pre>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * HealingService healing = HealingService.get();
 *
 * // Simple healing (no healer, e.g., regeneration effect)
 * HealingSpec spec = HealingSpec.of(50);
 * HealingResult result = healing.applyHealing(targetRef, spec, commandBuffer);
 *
 * // Healing from another entity (e.g., heal spell)
 * HealingSpec healSpec = HealingSpec.builder()
 *     .amount(100)
 *     .source("Holy Light")
 *     .logToCombatLog(true)
 *     .build();
 * HealingResult result = healing.applyHealing(healerRef, targetRef, healSpec, commandBuffer);
 * }</pre>
 *
 * @see HealingSpec
 * @see HealingResult
 */
public interface HealingService {

    /**
     * Apply healing from an entity healer to a target.
     * <p>
     * This method applies the full healing pipeline:
     * <ol>
     *   <li>Validate healer and target references</li>
     *   <li>Get healer's healing-effectiveness-bps stat</li>
     *   <li>Get target's healing-received-bps stat</li>
     *   <li>Get target's life-recovery-rate-bps stat</li>
     *   <li>Calculate final healing amount</li>
     *   <li>Apply healing to target's health</li>
     *   <li>Optionally record to combat log</li>
     * </ol>
     *
     * @param healerRef Entity reference for the healer (for healing effectiveness)
     * @param targetRef Entity reference for the healing target
     * @param spec The healing specification
     * @param commandBuffer Command buffer for entity modifications
     * @return Result containing healing details
     */
    @Nonnull
    HealingResult applyHealing(
            @Nonnull Ref<EntityStore> healerRef,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull HealingSpec spec,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    );

    /**
     * Apply healing without a healer (e.g., regeneration, environmental healing).
     * <p>
     * This method skips healer effectiveness and applies:
     * <ol>
     *   <li>Get target's healing-received-bps stat</li>
     *   <li>Get target's life-recovery-rate-bps stat</li>
     *   <li>Calculate final healing amount</li>
     *   <li>Apply healing to target's health</li>
     * </ol>
     *
     * @param targetRef Entity reference for the healing target
     * @param spec The healing specification
     * @param commandBuffer Command buffer for entity modifications
     * @return Result containing healing details
     */
    @Nonnull
    HealingResult applyHealing(
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull HealingSpec spec,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    );

    /**
     * Apply healing with component accessor for immediate resolution.
     * <p>
     * Use this variant when you have a ComponentAccessor and need immediate
     * healing application without waiting for command buffer flush.
     *
     * @param healerRef Entity reference for the healer (nullable for no healer)
     * @param targetRef Entity reference for the healing target
     * @param spec The healing specification
     * @param componentAccessor Component accessor for immediate operations
     * @return Result containing healing details
     */
    @Nonnull
    HealingResult applyHealingImmediate(
            @Nullable Ref<EntityStore> healerRef,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull HealingSpec spec,
            @Nonnull ComponentAccessor<EntityStore> componentAccessor
    );

    /**
     * Calculate potential healing without applying it.
     * <p>
     * Useful for previewing healing in UI or tooltip calculations.
     *
     * @param healerRef Entity reference for the healer (nullable for no healer)
     * @param targetRef Entity reference for the healing target
     * @param spec The healing specification
     * @param componentAccessor Component accessor for stat lookup
     * @return Result with calculated values (healing not actually applied)
     */
    @Nonnull
    HealingResult calculateHealing(
            @Nullable Ref<EntityStore> healerRef,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull HealingSpec spec,
            @Nonnull ComponentAccessor<EntityStore> componentAccessor
    );

    /**
     * Get the singleton instance of the HealingService.
     *
     * @return The HealingService singleton
     */
    @Nonnull
    static HealingService get() {
        return HealingServiceImpl.getInstance();
    }
}
