package reign.software.hyforged.stats.debug;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.CoreStats;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.component.StatModifier;
import reign.software.hyforged.stats.breakdown.BreakdownEntry;
import reign.software.hyforged.stats.breakdown.StatBreakdown;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;

/**
 * Admin service for stat system operations.
 * <p>
 * Provides commands and utilities for:
 * - Inspecting entity stat components
 * - Forcing stat recomputation
 * - Querying metrics
 * - Managing debug traces
 * <p>
 * All admin actions are audit logged.
 */
public final class StatAdminService {
    
    private static final HytaleLogger LOGGER = HytaleLogger.get("Hyforged.StatAdmin");
    private static final HytaleLogger AUDIT_LOGGER = HytaleLogger.get("Hyforged.StatAdmin.Audit");
    
    private StatAdminService() {} // Static utility class
    
    /**
     * Get the ComponentType for HyforgedStatComponent.
     */
    private static ComponentType<EntityStore, HyforgedStatComponent> getComponentType() {
        return HyforgedPlugin.getInstance().getHyforgedStatComponentType();
    }
    
    // ========== ENTITY INSPECTION ==========
    
    /**
     * Inspect an entity's HyforgedStatComponent.
     * <p>
     * Returns a formatted string with:
     * - Ability scores
     * - Modifier count and list
     * - Dirty flags status
     * - Selected stat values
     *
     * @param store The entity store
     * @param entityRef The entity reference
     * @param adminId Identifier for the admin performing the action (for audit)
     * @return Formatted inspection result, or null if entity has no stat component
     */
    @Nullable
    public static String inspectEntity(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull String adminId) {
        
        auditLog(adminId, "INSPECT", "Entity index: " + entityRef.getIndex());
        
        HyforgedStatComponent component = store.getComponent(entityRef, getComponentType());
        if (component == null) {
            return null;
        }
        
        return formatInspection(entityRef.getIndex(), component);
    }
    
    /**
     * Format a full inspection of an entity's stat component.
     */
    @Nonnull
    public static String formatInspection(int entityIndex, @Nonnull HyforgedStatComponent component) {
        StringBuilder sb = new StringBuilder();
        sb.append("========== ENTITY STAT INSPECTION ==========\n");
        sb.append(String.format("Entity Index: %d\n", entityIndex));
        sb.append(String.format("Schema Version: %d\n", HyforgedStatComponent.SCHEMA_VERSION));
        
        // Base Values (Ability Scores)
        sb.append("\n--- Base Values ---\n");
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        String[] abilityNames = {"STR", "DEX", "INT", "CON", "WIS", "SPI", "LCK"};
        StatId[] abilityStats = {
            CoreStats.STRENGTH, CoreStats.DEXTERITY, CoreStats.INTELLIGENCE,
            CoreStats.CONSTITUTION, CoreStats.WISDOM, CoreStats.SPIRIT, CoreStats.LUCK
        };
        for (int i = 0; i < abilityStats.length && i < abilityNames.length; i++) {
            int statIndex = registry.getIndex(abilityStats[i]);
            int value = statIndex >= 0 ? component.getBaseValue(statIndex) : 0;
            sb.append(String.format("  %s: %d\n", abilityNames[i], value));
        }
        
        // Modifiers
        sb.append("\n--- Modifiers ---\n");
        List<StatModifier> modifiers = component.getModifiers();
        sb.append(String.format("Count: %d / %d\n", modifiers.size(), HyforgedStatComponent.MAX_MODIFIERS));
        
        if (modifiers.size() <= 20) {
            for (StatModifier mod : modifiers) {
                sb.append(formatModifierLine(mod));
            }
        } else {
            // Summarize if too many
            sb.append("  (too many to list, showing first 10 and last 5)\n");
            for (int i = 0; i < 10; i++) {
                sb.append(formatModifierLine(modifiers.get(i)));
            }
            sb.append("  ...\n");
            for (int i = modifiers.size() - 5; i < modifiers.size(); i++) {
                sb.append(formatModifierLine(modifiers.get(i)));
            }
        }
        
        // Dirty Flags
        sb.append("\n--- Dirty Flags ---\n");
        sb.append(String.format("Has Any Dirty: %s\n", component.hasAnyDirty()));
        int[] dirtyIndices = component.getDirtyStatIndices();
        if (dirtyIndices.length <= 20) {
            sb.append(String.format("Dirty Stats: %d indices\n", dirtyIndices.length));
        } else {
            sb.append(String.format("Dirty Stats: %d indices (all dirty)\n", dirtyIndices.length));
        }
        
        // Bridge State
        sb.append("\n--- Bridge State ---\n");
        sb.append(String.format("Last Bridged MaxHealth: %d\n", component.getLastBridgedMaxHealth()));
        sb.append(String.format("Last Bridged MaxMana: %d\n", component.getLastBridgedMaxMana()));
        sb.append(String.format("Last Bridged MaxStamina: %d\n", component.getLastBridgedMaxStamina()));
        
        // Sample Stats
        sb.append("\n--- Sample Stat Values ---\n");
        appendStatValue(sb, registry, component, "hyforged:max-health");
        appendStatValue(sb, registry, component, "hyforged:max-mana");
        appendStatValue(sb, registry, component, "hyforged:max-stamina");
        appendStatValue(sb, registry, component, "hyforged:armor");
        appendStatValue(sb, registry, component, "hyforged:physical-damage");
        
        sb.append("=============================================\n");
        return sb.toString();
    }
    
    private static void appendStatValue(StringBuilder sb, StatDefinitionRegistry registry, 
            HyforgedStatComponent component, String statIdStr) {
        StatId statId = StatId.parse(statIdStr);
        if (statId != null) {
            int index = registry.getIndex(statId);
            if (index >= 0) {
                int value = component.getCachedValue(index);
                StatDefinition def = registry.getStat(index);
                String name = def != null ? def.displayName() : statIdStr;
                sb.append(String.format("  %s: %d\n", name, value));
            }
        }
    }
    
    private static String formatModifierLine(StatModifier mod) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        String targetName;
        if (mod.targetTagId() != null) {
            targetName = "tag:" + mod.targetTagId();
        } else if (mod.targetStatIndex() >= 0) {
            StatDefinition def = registry.getStat(mod.targetStatIndex());
            targetName = def != null ? def.id().toString() : "idx:" + mod.targetStatIndex();
        } else {
            targetName = "?";
        }
        
        return String.format("  [%s] %s %+d to %s from '%s'\n",
            mod.modifierType().name(),
            mod.sourceType().name(),
            mod.value(),
            targetName,
            mod.sourceId()
        );
    }
    
    /**
     * Format a StatBreakdown as a human-readable string.
     */
    @Nonnull
    private static String formatBreakdown(@Nonnull StatBreakdown breakdown) {
        StringBuilder sb = new StringBuilder();
        sb.append("========== STAT BREAKDOWN ==========\n");
        sb.append(String.format("Stat: %s (%s)\n", breakdown.displayName(), breakdown.statId()));
        
        sb.append("\n--- Computation ---\n");
        sb.append(String.format("Base Value: %d\n", breakdown.baseValue()));
        sb.append(String.format("+ Flat Modifiers: %+d → %d\n", breakdown.flatTotal(), breakdown.afterFlat()));
        sb.append(String.format("× Increased (%+.1f%%) → %d\n", 
            breakdown.increasedTotalBps() / 10.0, breakdown.afterIncreased()));
        sb.append(String.format("× More → %d\n", breakdown.afterMore()));
        sb.append(String.format("→ After Caps: %d\n", breakdown.afterCap()));
        sb.append(String.format("= Final Value: %d\n", breakdown.finalValue()));
        
        if (breakdown.isRating()) {
            String effectiveness = breakdown.getFormattedEffectiveness();
            sb.append(String.format("\nRating Effectiveness: %s\n", 
                effectiveness != null ? effectiveness : "N/A"));
        }
        
        sb.append("\n--- Modifiers ---\n");
        if (breakdown.entries().isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (BreakdownEntry entry : breakdown.entries()) {
                sb.append(String.format("  [%s] %s %+d from '%s'\n",
                    entry.modifierType().name(),
                    entry.sourceType().name(),
                    entry.value(),
                    entry.displayName()
                ));
            }
        }
        
        sb.append("=====================================\n");
        return sb.toString();
    }
    
    // ========== STAT BREAKDOWN ==========
    
    /**
     * Get a detailed breakdown of a specific stat for an entity.
     *
     * @param store The entity store
     * @param entityRef The entity reference
     * @param statId The stat ID to inspect
     * @param targetLevel Target level for rating effectiveness (default 1)
     * @param adminId Identifier for the admin performing the action
     * @return Formatted breakdown, or null if not found
     */
    @Nullable
    public static String getStatBreakdown(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull StatId statId,
            int targetLevel,
            @Nonnull String adminId) {
        
        auditLog(adminId, "BREAKDOWN", String.format("Entity: %d, Stat: %s", entityRef.getIndex(), statId));
        
        HyforgedStatComponent component = store.getComponent(entityRef, getComponentType());
        if (component == null) {
            return null;
        }
        
        StatBreakdown breakdown = component.getStatBreakdown(statId, targetLevel);
        if (breakdown == null) {
            return "Stat not found: " + statId;
        }
        
        return formatBreakdown(breakdown);
    }
    
    // ========== FORCE RECOMPUTE ==========
    
    /**
     * Force recomputation of all stats for an entity.
     *
     * @param store The entity store
     * @param entityRef The entity reference
     * @param adminId Identifier for the admin performing the action
     * @return true if entity has stat component and dirty flags were set
     */
    public static boolean forceRecompute(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull String adminId) {
        
        auditLog(adminId, "FORCE_RECOMPUTE", "Entity index: " + entityRef.getIndex());
        
        HyforgedStatComponent component = store.getComponent(entityRef, getComponentType());
        if (component == null) {
            LOGGER.at(Level.WARNING).log("Force recompute failed: entity %d has no HyforgedStatComponent", 
                entityRef.getIndex());
            return false;
        }
        
        component.markAllDirty();
        LOGGER.at(Level.INFO).log("Force recompute: marked all stats dirty for entity %d", entityRef.getIndex());
        return true;
    }
    
    /**
     * Force recomputation of a specific stat for an entity.
     *
     * @param store The entity store
     * @param entityRef The entity reference
     * @param statId The stat to mark dirty
     * @param adminId Identifier for the admin performing the action
     * @return true if successful
     */
    public static boolean forceRecomputeStat(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull StatId statId,
            @Nonnull String adminId) {
        
        auditLog(adminId, "FORCE_RECOMPUTE_STAT", 
            String.format("Entity: %d, Stat: %s", entityRef.getIndex(), statId));
        
        HyforgedStatComponent component = store.getComponent(entityRef, getComponentType());
        if (component == null) {
            return false;
        }
        
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int statIndex = registry.getIndex(statId);
        if (statIndex < 0) {
            LOGGER.at(Level.WARNING).log("Force recompute failed: unknown stat %s", statId);
            return false;
        }
        
        component.markStatDirty(statIndex);
        LOGGER.at(Level.INFO).log("Force recompute: marked stat %s dirty for entity %d", statId, entityRef.getIndex());
        return true;
    }
    
    // ========== TRACE CONTROL ==========
    
    /**
     * Enable debug trace for an entity.
     */
    public static void enableEntityTrace(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull String adminId) {
        
        auditLog(adminId, "ENABLE_TRACE", "Entity index: " + entityRef.getIndex());
        StatDebugTracer.enableTrace(entityRef);
    }
    
    /**
     * Disable debug trace for an entity.
     */
    public static void disableEntityTrace(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull String adminId) {
        
        auditLog(adminId, "DISABLE_TRACE", "Entity index: " + entityRef.getIndex());
        StatDebugTracer.disableTrace(entityRef);
    }
    
    /**
     * Enable global trace for all entities.
     */
    public static void enableGlobalTrace(@Nonnull String adminId) {
        auditLog(adminId, "ENABLE_GLOBAL_TRACE", "");
        StatDebugTracer.enableGlobalTrace();
    }
    
    /**
     * Disable global trace.
     */
    public static void disableGlobalTrace(@Nonnull String adminId) {
        auditLog(adminId, "DISABLE_GLOBAL_TRACE", "");
        StatDebugTracer.disableGlobalTrace();
    }
    
    // ========== METRICS ==========
    
    /**
     * Get formatted metrics string.
     */
    @Nonnull
    public static String getMetrics(@Nonnull String adminId) {
        auditLog(adminId, "GET_METRICS", "");
        return StatMetrics.formatMetrics();
    }
    
    /**
     * Reset all metrics.
     */
    public static void resetMetrics(@Nonnull String adminId) {
        auditLog(adminId, "RESET_METRICS", "");
        StatMetrics.reset();
        LOGGER.at(Level.INFO).log("Metrics reset by admin: %s", adminId);
    }
    
    // ========== AUDIT LOGGING ==========
    
    private static void auditLog(@Nonnull String adminId, @Nonnull String action, @Nonnull String details) {
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        AUDIT_LOGGER.at(Level.INFO).log("[AUDIT] %s | Admin: %s | Action: %s | %s", 
            timestamp, adminId, action, details);
    }
}
