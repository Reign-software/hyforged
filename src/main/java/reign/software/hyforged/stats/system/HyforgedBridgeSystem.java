package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.Set;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

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

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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
     * Modifier key for max ward.
     */
    public static final String MODIFIER_KEY_MAX_WARD = MODIFIER_KEY_PREFIX + "MaxWard";

    /**
     * Modifier key for movement speed (unused — movement speed bridge uses MovementManager, not EntityStatMap).
     */
    public static final String MODIFIER_KEY_MOVEMENT_SPEED = MODIFIER_KEY_PREFIX + "MovementSpeed";

    /**
     * <p>
     * No attack-speed stat exists in {@link DefaultEntityStatTypes} in the current server version.
     * The Hytale stat registry does not expose an "AttackSpeed" entry.
     * When Hytale adds a native attack speed stat, bridge it here using:
     * <pre>
     *   float multiplier = 1.0f + currentBps / 10000.0f;
     *   // apply relative to defaultAttackSpeed baseline (like movement speed bridge)
     * </pre>
     * The index is cached below so it is never -1 at runtime and the stat value
     * is accessible for future wiring without any constant changes.
     */
    
    /**
     * Minimum delta to trigger an update (prevents excessive updates for tiny changes).
     * A delta of 1 means we update for any change of at least 1 point.
     */
    private static final int UPDATE_THRESHOLD = 1;
    
    // Cached component types for optional movement speed bridge
    @Nonnull
    private final ComponentType<EntityStore, MovementManager> movementManagerType;

    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefType;

    // Cached stat indices for resource stats
    private int maxHealthIndex = -1;
    private int maxManaIndex = -1;
    private int maxStaminaIndex = -1;
    private int maxConcentrationIndex = -1;
    private int maxRageIndex = -1;
    private int maxWardIndex = -1;
    private int concentrationEntityStatIndex = -1;
    private int rageEntityStatIndex = -1;
    private int wardEntityStatIndex = -1;
    private int movementSpeedBpsIndex = -1;

    private boolean indicesInitialized = false;

    public HyforgedBridgeSystem() {
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        this.entityStatMapType = EntityStatMap.getComponentType();
        this.movementManagerType = MovementManager.getComponentType();
        this.playerRefType = PlayerRef.getComponentType();

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
        bridgeMaxWard(hyforgedStats, entityStatMap);

        // Bridge movement speed (optional — players only)
        Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
        bridgeMovementSpeed(hyforgedStats, entityRef, store);
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
        maxWardIndex = registry.getIndex(StatId.hyforged("max-ward-flat"));
        concentrationEntityStatIndex = EntityStatType.getAssetMap().getIndex("Concentration");
        rageEntityStatIndex = EntityStatType.getAssetMap().getIndex("Rage");
        wardEntityStatIndex = EntityStatType.getAssetMap().getIndex("Ward");
        movementSpeedBpsIndex = registry.getIndex(StatId.hyforged("movement-speed-bps"));
        indicesInitialized = true;
    }

    /**
     * Bridge max health from Hyforged to Hytale's EntityStatMap.
     * On first bridge after login (lastBridged == 0), maximize the stat
     * so the player spawns at full health.
     */
    private void bridgeMaxHealth(
            @Nonnull HyforgedStatComponent hyforgedStats,
            @Nonnull EntityStatMap entityStatMap
    ) {
        if (maxHealthIndex < 0) {
            return;
        }
        
        int currentValue = hyforgedStats.getCachedValue(maxHealthIndex);
        int lastBridged = hyforgedStats.getLastBridgedMaxHealth();

        // Also re-apply if modifier was wiped (e.g. by an interaction that reinitialises EntityStatMap)
        boolean modifierMissing = entityStatMap.getModifier(DefaultEntityStatTypes.getHealth(), MODIFIER_KEY_MAX_HEALTH) == null;

        int delta = currentValue - lastBridged;
        if (modifierMissing || Math.abs(delta) >= UPDATE_THRESHOLD) {
            // firstBridge is true only on the very first login (lastBridged == 0).
            // When the modifier is merely missing (interaction reset), do NOT maximize
            // so current HP is preserved at whatever Hytale already has.
            boolean firstBridge = lastBridged == 0;
            applyModifier(entityStatMap, DefaultEntityStatTypes.getHealth(), MODIFIER_KEY_MAX_HEALTH, currentValue);
            hyforgedStats.setLastBridgedMaxHealth(currentValue);
            logBridge("Health", currentValue, lastBridged);

            // On first bridge after login, maximize health so the player starts at full HP
            if (firstBridge) {
                entityStatMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
            }
        }
    }

    /**
     * Bridge max mana from Hyforged to Hytale's EntityStatMap.
     * On first bridge after login, maximize the stat.
     */
    private void bridgeMaxMana(
            @Nonnull HyforgedStatComponent hyforgedStats,
            @Nonnull EntityStatMap entityStatMap
    ) {
        if (maxManaIndex < 0) {
            return;
        }
        
        int currentValue = hyforgedStats.getCachedValue(maxManaIndex);
        int lastBridged = hyforgedStats.getLastBridgedMaxMana();

        boolean modifierMissing = entityStatMap.getModifier(DefaultEntityStatTypes.getMana(), MODIFIER_KEY_MAX_MANA) == null;

        int delta = currentValue - lastBridged;
        if (modifierMissing || Math.abs(delta) >= UPDATE_THRESHOLD) {
            boolean firstBridge = lastBridged == 0;
            applyModifier(entityStatMap, DefaultEntityStatTypes.getMana(), MODIFIER_KEY_MAX_MANA, currentValue);
            hyforgedStats.setLastBridgedMaxMana(currentValue);
            logBridge("Mana", currentValue, lastBridged);

            if (firstBridge) {
                entityStatMap.maximizeStatValue(DefaultEntityStatTypes.getMana());
            }
        }
    }

    /**
     * Bridge max stamina from Hyforged to Hytale's EntityStatMap.
     * On first bridge after login, maximize the stat.
     */
    private void bridgeMaxStamina(
            @Nonnull HyforgedStatComponent hyforgedStats,
            @Nonnull EntityStatMap entityStatMap
    ) {
        if (maxStaminaIndex < 0) {
            return;
        }
        
        int currentValue = hyforgedStats.getCachedValue(maxStaminaIndex);
        int lastBridged = hyforgedStats.getLastBridgedMaxStamina();

        boolean modifierMissing = entityStatMap.getModifier(DefaultEntityStatTypes.getStamina(), MODIFIER_KEY_MAX_STAMINA) == null;

        int delta = currentValue - lastBridged;
        if (modifierMissing || Math.abs(delta) >= UPDATE_THRESHOLD) {
            boolean firstBridge = lastBridged == 0;
            applyModifier(entityStatMap, DefaultEntityStatTypes.getStamina(), MODIFIER_KEY_MAX_STAMINA, currentValue);
            hyforgedStats.setLastBridgedMaxStamina(currentValue);
            logBridge("Stamina", currentValue, lastBridged);

            if (firstBridge) {
                entityStatMap.maximizeStatValue(DefaultEntityStatTypes.getStamina());
            }
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

        int currentValue = hyforgedStats.getCachedValue(maxConcentrationIndex);
        int lastBridged = hyforgedStats.getLastBridgedMaxConcentration();

        boolean modifierMissing = entityStatMap.getModifier(concentrationEntityStatIndex, MODIFIER_KEY_MAX_CONCENTRATION) == null;

        int delta = currentValue - lastBridged;
        if (modifierMissing || Math.abs(delta) >= UPDATE_THRESHOLD) {
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

        int currentValue = hyforgedStats.getCachedValue(maxRageIndex);
        int lastBridged = hyforgedStats.getLastBridgedMaxRage();

        boolean modifierMissing = entityStatMap.getModifier(rageEntityStatIndex, MODIFIER_KEY_MAX_RAGE) == null;

        int delta = currentValue - lastBridged;
        if (modifierMissing || Math.abs(delta) >= UPDATE_THRESHOLD) {
            applyModifier(entityStatMap, rageEntityStatIndex, MODIFIER_KEY_MAX_RAGE, currentValue);
            hyforgedStats.setLastBridgedMaxRage(currentValue);
            logBridge("Rage", currentValue, lastBridged);
        }
    }

    /**
     * Bridge max ward from Hyforged to Hytale's EntityStatMap.
     * On first bridge after login, maximizes the stat so the player starts with full Ward.
     */
    private void bridgeMaxWard(
            @Nonnull HyforgedStatComponent hyforgedStats,
            @Nonnull EntityStatMap entityStatMap
    ) {
        if (maxWardIndex < 0 || wardEntityStatIndex < 0) {
            return;
        }

        int currentValue = hyforgedStats.getCachedValue(maxWardIndex);
        int lastBridged = hyforgedStats.getLastBridgedMaxWard();

        boolean modifierMissing = entityStatMap.getModifier(wardEntityStatIndex, MODIFIER_KEY_MAX_WARD) == null;
        int delta = currentValue - lastBridged;
        if (modifierMissing || Math.abs(delta) >= UPDATE_THRESHOLD) {
            boolean firstBridge = lastBridged == 0 && currentValue > 0;
            applyModifier(entityStatMap, wardEntityStatIndex, MODIFIER_KEY_MAX_WARD, currentValue);
            hyforgedStats.setLastBridgedMaxWard(currentValue);
            logBridge("Ward", currentValue, lastBridged);
            if (firstBridge) {
                entityStatMap.maximizeStatValue(wardEntityStatIndex);
            }
        }
    }

    /**
     * Bridge movement speed from Hyforged to Hytale's MovementManager.
     * <p>
     * Movement speed is NOT part of EntityStatMap; it lives in {@link MovementManager}.
     * We apply: {@code settings.baseSpeed = defaultSettings.baseSpeed * (1 + bps / 10000.0)}.
     * By computing relative to {@code defaultSettings.baseSpeed} every time, drift is prevented
     * even if other systems (e.g. mounts) later reset settings.
     * <p>
     * Only applies to player entities that have both {@link MovementManager} and {@link PlayerRef}.
     */
    private void bridgeMovementSpeed(
            @Nonnull HyforgedStatComponent hyforgedStats,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store
    ) {
        if (movementSpeedBpsIndex < 0) {
            return;
        }

        int currentBps = hyforgedStats.getCachedValue(movementSpeedBpsIndex);
        int lastBridged = hyforgedStats.getLastBridgedMovementSpeedBps();

        if (currentBps == lastBridged) {
            return;
        }

        MovementManager movementManager = store.getComponent(entityRef, movementManagerType);
        if (movementManager == null) {
            // NPC or entity without movement management — skip silently
            return;
        }

        MovementSettings defaultSettings = movementManager.getDefaultSettings();
        if (defaultSettings == null) {
            return;
        }

        // Compute new speed relative to the default baseline (prevents drift)
        float multiplier = 1.0f + currentBps / 10000.0f;
        if (multiplier < 0.0f) {
            multiplier = 0.0f;
        }
        movementManager.getSettings().baseSpeed = defaultSettings.baseSpeed * multiplier;

        // Send updated movement settings to the player client
        PlayerRef playerRef = store.getComponent(entityRef, playerRefType);
        if (playerRef != null) {
            movementManager.update(playerRef.getPacketHandler());
        }

        hyforgedStats.setLastBridgedMovementSpeedBps(currentBps);
        logBridge("MovementSpeed (bps)", currentBps, lastBridged);
    }

    private void logBridge(@Nonnull String statName, int newValue, int oldValue) {
        LOGGER.at(Level.FINE).log("Bridged %s: %d -> %d", statName, oldValue, newValue);
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
        if (!StatAccessor.hasStatSlot(entityStatMap, statIndex)) {
            return;
        }

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
