package reign.software.hyforged.stats.bridge;

import com.hypixel.hytale.component.ComponentRegistry;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Replaces conflicting Hytale stat and damage systems with Hyforged equivalents.
 * <p>
 * Hytale has built-in systems that modify entity stats based on armor, weapons, and effects.
 * These systems conflict with Hyforged's ARPG modifier system. This class unregisters the
 * conflicting Hytale systems so Hyforged can handle stats exclusively.
 * <p>
 * Systems replaced:
 * <ul>
 *   <li>{@link DamageSystems.ArmorDamageReduction} — replaced by {@link HyforgedDamageReductionSystem}</li>
 *   <li>{@link DamageSystems.ArmorKnockbackReduction} — replaced by {@link HyforgedKnockbackReductionSystem}</li>
 *   <li>{@link EntityStatsSystems.Recalculate} — Hyforged handles all stat modifiers</li>
 * </ul>
 * 
 * @see <a href="../../.memory_bank/ADRs.md#adr-0006">ADR-0006</a>
 */
@SuppressWarnings("deprecation") // DamageSystems inner classes are deprecated but we need to reference them
public final class HytaleSystemReplacer {

    private HytaleSystemReplacer() {} // Static utility class

    /**
     * Unregister conflicting Hytale systems.
     * <p>
     * This should be called during plugin setup, after Hytale's modules have registered
     * their systems but before the game starts processing entities.
     *
     * @param plugin The Hyforged plugin instance for logging
     */
    public static void unregisterConflictingSystems(@Nonnull HyforgedPlugin plugin) {
        // Access the global EntityStore registry directly
        ComponentRegistry<EntityStore> registry = EntityStore.REGISTRY;
        
        int successCount = 0;
        
        // Unregister Hytale's armor damage reduction system
        // Hyforged will handle resistance calculation via HyforgedDamageReductionSystem
        try {
            registry.unregisterSystem(DamageSystems.ArmorDamageReduction.class);
            plugin.getLogger().at(Level.INFO).log("Unregistered Hytale ArmorDamageReduction system");
            successCount++;
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log(
                "Failed to unregister ArmorDamageReduction: %s", e.getMessage()
            );
        }
        
        // Unregister Hytale's armor knockback reduction system
        // Hyforged will handle knockback resistance via HyforgedKnockbackReductionSystem
        try {
            registry.unregisterSystem(DamageSystems.ArmorKnockbackReduction.class);
            plugin.getLogger().at(Level.INFO).log("Unregistered Hytale ArmorKnockbackReduction system");
            successCount++;
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log(
                "Failed to unregister ArmorKnockbackReduction: %s", e.getMessage()
            );
        }
        
        // Unregister Hytale's entity stats recalculate system
        // This system applies armor/weapon stat modifiers via StatModifiersManager
        // Hyforged handles all stat modifiers through HyforgedStatComponent
        try {
            registry.unregisterSystem(EntityStatsSystems.Recalculate.class);
            plugin.getLogger().at(Level.INFO).log("Unregistered Hytale EntityStatsSystems.Recalculate system");
            successCount++;
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log(
                "Failed to unregister EntityStatsSystems.Recalculate: %s", e.getMessage()
            );
        }
        
        // Log summary
        plugin.getLogger().at(Level.INFO).log(
            "Hyforged system replacement complete: %d/3 Hytale systems unregistered", 
            successCount
        );
    }
}
