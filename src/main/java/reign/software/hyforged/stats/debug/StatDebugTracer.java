package reign.software.hyforged.stats.debug;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;
import reign.software.hyforged.stats.engine.StackingEngine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Debug tracer for stat computations.
 * <p>
 * Enables detailed trace logging for specific entities to help diagnose
 * stat calculation issues without impacting overall performance.
 * <p>
 * Usage:
 * - Call {@code enableTrace(entityRef)} to start tracing an entity
 * - Call {@code disableTrace(entityRef)} to stop tracing
 * - Trace output goes to the logger at INFO level
 */
public final class StatDebugTracer {
    
    private static final HytaleLogger LOGGER = HytaleLogger.get("Hyforged.StatDebugTracer");
    
    /** Set of entity indices with trace enabled */
    private static final Set<Integer> tracedEntities = ConcurrentHashMap.newKeySet();
    
    /** Global trace flag for all entities (expensive, for development only) */
    private static volatile boolean globalTraceEnabled = false;
    
    private StatDebugTracer() {} // Static utility class
    
    // ========== TRACE CONTROL ==========
    
    /**
     * Enable trace for a specific entity.
     * @param entityRef The entity reference
     */
    public static void enableTrace(@Nonnull Ref<EntityStore> entityRef) {
        int entityIndex = entityRef.getIndex();
        tracedEntities.add(entityIndex);
        LOGGER.at(Level.INFO).log("[TRACE] Enabled stat trace for entity %d", entityIndex);
    }
    
    /**
     * Disable trace for a specific entity.
     * @param entityRef The entity reference
     */
    public static void disableTrace(@Nonnull Ref<EntityStore> entityRef) {
        int entityIndex = entityRef.getIndex();
        tracedEntities.remove(entityIndex);
        LOGGER.at(Level.INFO).log("[TRACE] Disabled stat trace for entity %d", entityIndex);
    }
    
    /**
     * Check if trace is enabled for an entity.
     */
    public static boolean isTraceEnabled(@Nonnull Ref<EntityStore> entityRef) {
        return globalTraceEnabled || tracedEntities.contains(entityRef.getIndex());
    }
    
    /**
     * Check if trace is enabled for an entity by index.
     */
    public static boolean isTraceEnabled(int entityIndex) {
        return globalTraceEnabled || tracedEntities.contains(entityIndex);
    }
    
    /**
     * Enable global trace for all entities.
     * WARNING: This is expensive and should only be used for development.
     */
    public static void enableGlobalTrace() {
        globalTraceEnabled = true;
        LOGGER.at(Level.WARNING).log("[TRACE] GLOBAL TRACE ENABLED - Performance may be impacted");
    }
    
    /**
     * Disable global trace.
     */
    public static void disableGlobalTrace() {
        globalTraceEnabled = false;
        LOGGER.at(Level.INFO).log("[TRACE] Global trace disabled");
    }
    
    /**
     * Clear all per-entity traces.
     */
    public static void clearAllTraces() {
        int count = tracedEntities.size();
        tracedEntities.clear();
        LOGGER.at(Level.INFO).log("[TRACE] Cleared %d entity traces", count);
    }
    
    /**
     * Get count of entities being traced.
     */
    public static int getTracedEntityCount() {
        return tracedEntities.size();
    }
    
    /**
     * Get indices of all traced entities.
     */
    @Nonnull
    public static Set<Integer> getTracedEntityIndices() {
        return new HashSet<>(tracedEntities);
    }
    
    // ========== TRACE OUTPUT ==========
    
    /**
     * Trace a stat computation for an entity.
     * <p>
     * This produces detailed output showing:
     * - Base value
     * - All modifiers grouped by type
     * - Intermediate values after each stacking phase
     * - Final computed value
     *
     * @param entityIndex The entity index
     * @param statIndex The stat index being computed
     * @param component The stat component
     */
    public static void traceComputation(int entityIndex, int statIndex, @Nonnull HyforgedStatComponent component) {
        if (!isTraceEnabled(entityIndex)) {
            return;
        }
        
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        StatDefinition statDef = registry.getStat(statIndex);
        if (statDef == null) {
            LOGGER.at(Level.INFO).log("[TRACE] Entity %d: Unknown stat index %d", entityIndex, statIndex);
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== STAT TRACE ==========\n");
        sb.append(String.format("Entity: %d | Stat: %s (index: %d)\n", entityIndex, statDef.id(), statIndex));
        sb.append(String.format("Category: %s | Rating: %s\n", statDef.category(), statDef.isRating()));
        
        // Get applicable modifiers
        List<HyforgedModifier> applicable = getApplicableModifiers(component, statIndex, statDef, registry);
        
        sb.append(String.format("\n--- Base Value ---\n"));
        sb.append(String.format("Default: %d\n", statDef.defaultValue()));
        
        // Use the detailed compute method
        StackingEngine.ComputeResult result = StackingEngine.computeWithBreakdown(
            statDef.defaultValue(), applicable, statDef
        );
        
        sb.append(String.format("\n--- FLAT Modifiers (%d) ---\n", result.flatModifiers.size()));
        for (HyforgedModifier mod : result.flatModifiers) {
            sb.append(formatModifier(mod));
        }
        sb.append(String.format("Flat Total: %+d → After Flat: %d\n", result.flatTotal, result.afterFlat));
        
        sb.append(String.format("\n--- INCREASED Modifiers (%d) ---\n", result.increasedModifiers.size()));
        for (HyforgedModifier mod : result.increasedModifiers) {
            sb.append(formatModifier(mod));
        }
        sb.append(String.format("Increased Total: %+d bps (%+.1f%%) → After Increased: %d\n", 
            result.increasedTotalBps, result.increasedTotalBps / 100.0, result.afterIncreased));
        
        sb.append(String.format("\n--- MORE Modifiers (%d) ---\n", result.moreModifiers.size()));
        for (HyforgedModifier mod : result.moreModifiers) {
            sb.append(formatModifier(mod));
        }
        sb.append(String.format("After More: %d\n", result.afterMore));
        
        sb.append(String.format("\n--- CAP Modifiers (%d) ---\n", result.capModifiers.size()));
        for (HyforgedModifier mod : result.capModifiers) {
            sb.append(formatModifier(mod));
        }
        sb.append(String.format("After Cap: %d\n", result.afterCap));
        
        sb.append(String.format("\n--- Final ---\n"));
        sb.append(String.format("Final Value: %d\n", result.finalValue));
        sb.append(String.format("Stat Bounds: [%d, %d]\n", statDef.minValue(), statDef.maxValue()));
        
        sb.append("=================================\n");
        
        LOGGER.at(Level.INFO).log(sb.toString());
    }
    
    /**
     * Trace a bridge update to Hytale stats.
     */
    public static void traceBridgeUpdate(int entityIndex, String statName, int oldValue, int newValue, int delta) {
        if (!isTraceEnabled(entityIndex)) {
            return;
        }
        
        LOGGER.at(Level.INFO).log("[TRACE] Entity %d: Bridge update %s: %d → %d (delta: %+d)", 
            entityIndex, statName, oldValue, newValue, delta);
    }
    
    /**
     * Trace a modifier addition.
     */
    public static void traceModifierAdded(int entityIndex, @Nonnull HyforgedModifier modifier) {
        if (!isTraceEnabled(entityIndex)) {
            return;
        }
        
        LOGGER.at(Level.INFO).log("[TRACE] Entity %d: Modifier added - %s", entityIndex, formatModifierCompact(modifier));
    }
    
    /**
     * Trace a modifier removal.
     */
    public static void traceModifierRemoved(int entityIndex, @Nonnull String sourceId, int count) {
        if (!isTraceEnabled(entityIndex)) {
            return;
        }
        
        LOGGER.at(Level.INFO).log("[TRACE] Entity %d: Removed %d modifiers from source '%s'", entityIndex, count, sourceId);
    }
    
    /**
     * Trace dirty flag changes.
     */
    public static void traceDirtyFlagSet(int entityIndex, int statIndex, @Nullable String reason) {
        if (!isTraceEnabled(entityIndex)) {
            return;
        }
        
        String statName = "ALL";
        if (statIndex >= 0) {
            StatDefinition def = StatDefinitionRegistry.get().getStat(statIndex);
            statName = def != null ? def.id().toString() : "index:" + statIndex;
        }
        
        String reasonStr = reason != null ? " (" + reason + ")" : "";
        LOGGER.at(Level.INFO).log("[TRACE] Entity %d: Dirty flag set for %s%s", entityIndex, statName, reasonStr);
    }
    
    // ========== HELPER METHODS ==========
    
    private static List<HyforgedModifier> getApplicableModifiers(
            @Nonnull HyforgedStatComponent component,
            int statIndex,
            @Nonnull StatDefinition statDef,
            @Nonnull StatDefinitionRegistry registry) {
        
        List<HyforgedModifier> applicable = new ArrayList<>();
        for (HyforgedModifier mod : component.getModifiers()) {
            if (mod.getTargetStatIndex() == statIndex) {
                applicable.add(mod);
            } else if (mod.getTargetTagIndex() != HyforgedModifier.NO_TAG) {
                if (registry.getStatIndicesForTagIndex(mod.getTargetTagIndex()).contains(statIndex)) {
                    applicable.add(mod);
                }
            }
        }
        return applicable;
    }
    
    private static String formatModifier(@Nonnull HyforgedModifier mod) {
        return String.format("  [%s] %s from '%s': %+d (priority: %d)\n",
            mod.getStackType().name(),
            mod.getSourceType().name(),
            mod.getSourceId(),
            mod.getAmount(),
            mod.getPriority()
        );
    }
    
    private static String formatModifierCompact(@Nonnull HyforgedModifier mod) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        String targetName;
        if (mod.getTargetTagIndex() != HyforgedModifier.NO_TAG) {
            targetName = "tagIndex:" + mod.getTargetTagIndex();
        } else if (mod.getTargetStatIndex() >= 0) {
            StatDefinition def = registry.getStat(mod.getTargetStatIndex());
            targetName = def != null ? def.id().toString() : "index:" + mod.getTargetStatIndex();
        } else {
            targetName = "unknown";
        }
        
        return String.format("%s %s %+d to %s from '%s'",
            mod.getStackType().name(),
            mod.getSourceType().name(),
            mod.getAmount(),
            targetName,
            mod.getSourceId()
        );
    }
}
