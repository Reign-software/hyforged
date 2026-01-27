package reign.software.hyforged.combat.api;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Service API for programmatic combat damage application.
 * <p>
 * The CombatService provides a unified interface for applying damage through
 * the full Hyforged combat pipeline, including:
 * <ul>
 *   <li>Hit resolution (accuracy vs evasion)</li>
 *   <li>Auto-block checks</li>
 *   <li>Damage reduction (resistance - penetration)</li>
 *   <li>Critical hit calculation</li>
 *   <li>Ailment threshold accumulation</li>
 *   <li>Combat log recording</li>
 * </ul>
 * <p>
 * This service is designed for skills, abilities, and custom damage sources
 * that need programmatic control over damage application rather than relying
 * on interaction-based combat.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * CombatService combat = CombatService.get();
 *
 * // Simple damage
 * DamageSpec spec = DamageSpec.of("Fire", 50);
 * CombatResult result = combat.applyDamage(attackerRef, defenderRef, spec, commandBuffer);
 *
 * if (result.wasHit()) {
 *     // Damage was applied
 *     System.out.println("Dealt " + result.getTotalFinalDamage() + " damage");
 * } else if (result.wasEvaded()) {
 *     // Attack missed
 *     System.out.println("Attack evaded!");
 * }
 *
 * // Multi-element attack with forced crit
 * DamageSpec multiSpec = DamageSpec.builder()
 *     .addDamage("Physical", 30)
 *     .addDamage("Lightning", 20)
 *     .forceCrit(true)
 *     .build();
 * CombatResult multiResult = combat.applyDamage(attackerRef, defenderRef, multiSpec, commandBuffer);
 * }</pre>
 *
 * @see DamageSpec
 * @see CombatResult
 */
public interface CombatService {

    /**
     * Apply damage from an entity attacker to a defender.
     * <p>
     * This method runs the full combat pipeline:
     * <ol>
     *   <li>Validate attacker and defender references</li>
     *   <li>Run hit resolution (unless {@code skipEvasion} is set)</li>
     *   <li>Run block check (unless {@code skipBlock} is set)</li>
     *   <li>Apply resistance and penetration per damage type</li>
     *   <li>Apply critical hit multiplier</li>
     *   <li>Dispatch damage events to Hytale's damage system</li>
     *   <li>Accumulate ailment thresholds</li>
     *   <li>Record to combat log</li>
     * </ol>
     *
     * @param attackerRef Entity reference for the attacker (required for stat lookup)
     * @param defenderRef Entity reference for the defender
     * @param spec The damage specification
     * @param commandBuffer Command buffer for entity modifications
     * @return Result containing all combat details
     */
    @Nonnull
    CombatResult applyDamage(
            @Nonnull Ref<EntityStore> attackerRef,
            @Nonnull Ref<EntityStore> defenderRef,
            @Nonnull DamageSpec spec,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    );

    /**
     * Apply damage from a non-entity source (environmental, command, etc.).
     * <p>
     * This method skips hit resolution (no attacker stats) and applies damage directly.
     * Resistance is still applied based on the defender's stats.
     *
     * @param defenderRef Entity reference for the defender
     * @param spec The damage specification
     * @param commandBuffer Command buffer for entity modifications
     * @param sourceDescription Description for combat log (e.g., "Lava", "Fall damage")
     * @return Result containing all combat details
     */
    @Nonnull
    CombatResult applyEnvironmentalDamage(
            @Nonnull Ref<EntityStore> defenderRef,
            @Nonnull DamageSpec spec,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nullable String sourceDescription
    );

    /**
     * Apply damage with component accessor for immediate resolution.
     * <p>
     * Use this variant when you have a ComponentAccessor and need immediate
     * damage application without waiting for command buffer flush.
     *
     * @param attackerRef Entity reference for the attacker
     * @param defenderRef Entity reference for the defender
     * @param spec The damage specification
     * @param componentAccessor Component accessor for immediate operations
     * @return Result containing all combat details
     */
    @Nonnull
    CombatResult applyDamageImmediate(
            @Nonnull Ref<EntityStore> attackerRef,
            @Nonnull Ref<EntityStore> defenderRef,
            @Nonnull DamageSpec spec,
            @Nonnull ComponentAccessor<EntityStore> componentAccessor
    );

    /**
     * Calculate potential damage without applying it.
     * <p>
     * Useful for previewing damage in UI or for AI decision-making.
     * Runs the full calculation pipeline but does not dispatch damage events.
     *
     * @param attackerRef Entity reference for the attacker
     * @param defenderRef Entity reference for the defender
     * @param spec The damage specification
     * @param componentAccessor Component accessor for stat lookup
     * @return Result with calculated values (damage not actually applied)
     */
    @Nonnull
    CombatResult calculateDamage(
            @Nonnull Ref<EntityStore> attackerRef,
            @Nonnull Ref<EntityStore> defenderRef,
            @Nonnull DamageSpec spec,
            @Nonnull ComponentAccessor<EntityStore> componentAccessor
    );

    /**
     * Get the singleton instance of the CombatService.
     *
     * @return The CombatService instance
     */
    @Nonnull
    static CombatService get() {
        return CombatServiceImpl.getInstance();
    }
}
