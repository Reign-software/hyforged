package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ECS System for bridging Hyforged stat values to Hytale's EntityStatMap.
 * <p>
 * This system syncs resource caps (MaxHealth, MaxMana, MaxStamina) computed
 * by Hyforged into Hytale's native stat system so the game engine can use them.
 * <p>
 * Following ECS principles, this system contains only processing logic.
 * It reads from HyforgedStatComponent and writes to EntityStatMap.
 */
public class HyforgedBridgeSystem extends EntityTickingSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(HyforgedBridgeSystem.class.getName());

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapType;
    
    @Nonnull
    private final Query<EntityStore> query;
    
    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;
    
    /**
     * Modifier key prefix for Hyforged modifiers in EntityStatMap.
     */
    private static final String MODIFIER_KEY_PREFIX = "Hyforged_";
    
    /**
     * Modifier key for max health.
     */
    public static final String MODIFIER_KEY_MAX_HEALTH = MODIFIER_KEY_PREFIX + "MaxHealth";
    
    /**
     * Modifier key for max mana.
     */
    public static final String MODIFIER_KEY_MAX_MANA = MODIFIER_KEY_PREFIX + "MaxMana";
    
    /**
     * Modifier key for max stamina.
     */
    public static final String MODIFIER_KEY_MAX_STAMINA = MODIFIER_KEY_PREFIX + "MaxStamina";

    /**
     * Modifier key for max concentration.
     */
    public static final String MODIFIER_KEY_MAX_CONCENTRATION = MODIFIER_KEY_PREFIX + "MaxConcentration";

    /**
     * Modifier key for max rage.
     */
    public static final String MODIFIER_KEY_MAX_RAGE = MODIFIER_KEY_PREFIX + "MaxRage";
    
    /**
     * Minimum delta to trigger an update (prevents excessive updates for tiny changes).
     * A delta of 1 means we update for any change of at least 1 point.
     */
    private static final int UPDATE_THRESHOLD = 1;
    
    // Cached stat indices for resource stats
    private int maxHealthIndex = -1;
    private int maxManaIndex = -1;
    private int maxStaminaIndex = -1;
    private int maxConcentrationIndex = -1;
    private int maxRageIndex = -1;
    private int concentrationEntityStatIndex = -1;
    private int rageEntityStatIndex = -1;
    private boolean indicesInitialized = false;

    public HyforgedBridgeSystem() {
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        this.entityStatMapType = EntityStatMap.getComponentType();
        
        // Query for entities with both components
        this.query = Query.and(statComponentType, entityStatMapType);
        
        // Run after stat computation is complete
        this.dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, HyforgedStatComputeSystem.class)
        );
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        HyforgedStatComponent hyforgedStats = archetypeChunk.getComponent(index, statComponentType);
        EntityStatMap entityStatMap = archetypeChunk.getComponent(index, entityStatMapType);
        
        if (hyforgedStats == null || entityStatMap == null) {
            return;
        }
        
        // Initialize cached stat indices on first run
        if (!indicesInitialized
                || maxConcentrationIndex < 0
                || maxRageIndex < 0
                || concentrationEntityStatIndex < 0
                || rageEntityStatIndex < 0) {
            initializeStatIndices();
        }
        
        // Bridge each resource stat
        bridgeMaxHealth(hyforgedStats, entityStatMap);
        bridgeMaxMana(hyforgedStats, entityStatMap);
        bridgeMaxStamina(hyforgedStats, entityStatMap);
        bridgeMaxConcentration(hyforgedStats, entityStatMap);
        bridgeMaxRage(hyforgedStats, entityStatMap);
    }

    /**
     * Initialize cached stat indices from the registry.
     */
    private void initializeStatIndices() {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        maxHealthIndex = registry.getIndex(StatId.hyforged("max-health-flat"));
        maxManaIndex = registry.getIndex(StatId.hyforged("max-mana-flat"));
        maxStaminaIndex = registry.getIndex(StatId.hyforged("max-stamina-flat"));
        maxConcentrationIndex = registry.getIndex(StatId.hyforged("concentration"));
        maxRageIndex = registry.getIndex(StatId.hyforged("rage-max"));
        concentrationEntityStatIndex = EntityStatType.getAssetMap().getIndex("Concentration");
        rageEntityStatIndex = EntityStatType.getAssetMap().getIndex("Rage");
        indicesInitialized = true;
    }

    /**
     * Bridge max health from Hyforged to Hytale's EntityStatMap.
     */
    private void bridgeMaxHealth(
            @Nonnull HyforgedStatComponent hyforgedStats,
            @Nonnull EntityStatMap entityStatMap
    ) {
        if (maxHealthIndex < 0) {
            return;
        }
        
        int currentValue = StatAccessor.getStatValueInt(entityStatMap, maxHealthIndex);
        int lastBridged = hyforgedStats.getLastBridgedMaxHealth();
        
        int delta = currentValue - lastBridged;
        if (Math.abs(delta) >= UPDATE_THRESHOLD) {
            applyModifier(entityStatMap, DefaultEntityStatTypes.getHealth(), MODIFIER_KEY_MAX_HEALTH, currentValue);
            hyforgedStats.setLastBridgedMaxHealth(currentValue);
            logBridge("Health", currentValue, lastBridged);
        }
    }

    /**
     * Bridge max mana from Hyforged to Hytale's EntityStatMap.
     */
    private void bridgeMaxMana(
            @Nonnull HyforgedStatComponent hyforgedStats,
            @Nonnull EntityStatMap entityStatMap
    ) {
        if (maxManaIndex < 0) {
            return;
        }
        
        int currentValue = StatAccessor.getStatValueInt(entityStatMap, maxManaIndex);
        int lastBridged = hyforgedStats.getLastBridgedMaxMana();
        
        int delta = currentValue - lastBridged;
        if (Math.abs(delta) >= UPDATE_THRESHOLD) {
            applyModifier(entityStatMap, DefaultEntityStatTypes.getMana(), MODIFIER_KEY_MAX_MANA, currentValue);
            hyforgedStats.setLastBridgedMaxMana(currentValue);
            logBridge("Mana", currentValue, lastBridged);
        }
    }

    /**
     * Bridge max stamina from Hyforged to Hytale's EntityStatMap.
     */
    private void bridgeMaxStamina(
            @Nonnull HyforgedStatComponent hyforgedStats,
            @Nonnull EntityStatMap entityStatMap
    ) {
        if (maxStaminaIndex < 0) {
            return;
        }
        
        int currentValue = StatAccessor.getStatValueInt(entityStatMap, maxStaminaIndex);
        int lastBridged = hyforgedStats.getLastBridgedMaxStamina();
        
        int delta = currentValue - lastBridged;
        if (Math.abs(delta) >= UPDATE_THRESHOLD) {
            applyModifier(entityStatMap, DefaultEntityStatTypes.getStamina(), MODIFIER_KEY_MAX_STAMINA, currentValue);
            hyforgedStats.setLastBridgedMaxStamina(currentValue);
            logBridge("Stamina", currentValue, lastBridged);
        }
    }

    /**
     * Bridge max concentration from Hyforged to Hytale's EntityStatMap.
     */
    private void bridgeMaxConcentration(
            @Nonnull HyforgedStatComponent hyforgedStats,
            @Nonnull EntityStatMap entityStatMap
    ) {
        if (maxConcentrationIndex < 0 || concentrationEntityStatIndex < 0) {
            return;
        }

        int currentValue = StatAccessor.getStatValueInt(entityStatMap, maxConcentrationIndex);
        int lastBridged = hyforgedStats.getLastBridgedMaxConcentration();

        int delta = currentValue - lastBridged;
        if (Math.abs(delta) >= UPDATE_THRESHOLD) {
            applyModifier(entityStatMap, concentrationEntityStatIndex, MODIFIER_KEY_MAX_CONCENTRATION, currentValue);
            hyforgedStats.setLastBridgedMaxConcentration(currentValue);
            logBridge("Concentration", currentValue, lastBridged);
        }
    }

    /**
     * Bridge max rage from Hyforged to Hytale's EntityStatMap.
     */
    private void bridgeMaxRage(
            @Nonnull HyforgedStatComponent hyforgedStats,
            @Nonnull EntityStatMap entityStatMap
    ) {
        if (maxRageIndex < 0 || rageEntityStatIndex < 0) {
            return;
        }

        int currentValue = StatAccessor.getStatValueInt(entityStatMap, maxRageIndex);
        int lastBridged = hyforgedStats.getLastBridgedMaxRage();

        int delta = currentValue - lastBridged;
        if (Math.abs(delta) >= UPDATE_THRESHOLD) {
            applyModifier(entityStatMap, rageEntityStatIndex, MODIFIER_KEY_MAX_RAGE, currentValue);
            hyforgedStats.setLastBridgedMaxRage(currentValue);
            logBridge("Rage", currentValue, lastBridged);
        }
    }

    private void logBridge(@Nonnull String statName, int newValue, int oldValue) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Bridged %s: %d -> %d", statName, oldValue, newValue));
        }
    }

    /**
     * Apply or update a modifier on EntityStatMap.
     * <p>
     * Uses HyforgedModifier with FLAT stack type to set the computed value.
     *
     * @param entityStatMap The entity's stat map
     * @param statIndex The Hytale stat index (from DefaultEntityStatTypes)
     * @param modifierKey The unique key for this modifier
     * @param value The absolute value to set (we compute the delta from base)
     */
    private void applyModifier(
            @Nonnull EntityStatMap entityStatMap,
            int statIndex,
            @Nonnull String modifierKey,
            int value
    ) {
        // Create a HyforgedModifier that adds to the MAX value
        // Using FLAT type since we're bridging the fully computed value
        HyforgedModifier modifier = HyforgedModifier.builder()
            .target(Modifier.ModifierTarget.MAX)
            .flat(value)
            .sourceType(HyforgedModifier.SourceType.BASE)
            .sourceId("hyforged_bridge")
            .build();
        
        entityStatMap.putModifier(statIndex, modifierKey, modifier);
    }
}
