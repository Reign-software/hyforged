package reign.software.hyforged.combat.scaling;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Service for calculating monster levels and retrieving scaling configurations.
 * <p>
 * This service:
 * <ul>
 *   <li>Caches world spawn points for performance</li>
 *   <li>Calculates monster levels using the active world scaling config</li>
 *   <li>Maps NPC role names to their per-monster scaling configurations</li>
 *   <li>Supports region-based overrides for dungeons, events, and special areas</li>
 * </ul>
 * <p>
 * Thread-safe singleton pattern.
 */
public final class MonsterScalingService {

    private static final Logger LOGGER = Logger.getLogger(MonsterScalingService.class.getName());

    private static volatile MonsterScalingService INSTANCE;

    /** Cache of world spawn positions (world ID → spawn position) */
    private final ConcurrentMap<String, Vector3d> worldSpawnCache = new ConcurrentHashMap<>();

    /** Map of NPC role name → scaling config */
    private final ConcurrentMap<String, MonsterScalingConfigAsset> roleScalingMap = new ConcurrentHashMap<>();
    
    /** Map of region scaling overrides (override ID → override) */
    private final ConcurrentMap<String, RegionScalingOverride> regionOverrides = new ConcurrentHashMap<>();
    
    /** Alternative scaling configs by ID (for region CONFIG overrides) */
    private final ConcurrentMap<String, WorldScalingConfig> scalingConfigs = new ConcurrentHashMap<>();

    /** Default spawn position if none configured */
    private static final Vector3d DEFAULT_SPAWN = new Vector3d(0, 64, 0);

    /** Active world scaling configuration (for level calculation) */
    private volatile WorldScalingConfig activeConfig;

    /** Default scaling stats when no NPC-specific config exists */
    private volatile List<ScaledStatEntry> defaultScaledStats;

    private MonsterScalingService() {
        // Initialize with default config
        this.activeConfig = WorldScalingConfig.createDefault("hyforged:default-scaling");
        // Initialize with sensible defaults
        this.defaultScaledStats = List.of(
            ScaledStatEntry.increased("hyforged:max-health", 10),
            ScaledStatEntry.increased("hyforged:physical-damage-bps", 5),
            ScaledStatEntry.flat("hyforged:armor-bps", 50)
        );
    }

    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static MonsterScalingService get() {
        if (INSTANCE == null) {
            synchronized (MonsterScalingService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MonsterScalingService();
                }
            }
        }
        return INSTANCE;
    }

    // ========== World Scaling Configuration ==========

    /**
     * Set the active world scaling configuration (for level calculation).
     * 
     * @param config The configuration to use
     */
    public void setActiveConfig(@Nonnull WorldScalingConfig config) {
        this.activeConfig = config;
    }

    /**
     * Get the active world scaling configuration.
     */
    @Nonnull
    public WorldScalingConfig getActiveConfig() {
        return activeConfig;
    }
    
    /**
     * Register an alternative scaling config by ID.
     * <p>
     * Used for region overrides with CONFIG type.
     * 
     * @param config The scaling config to register
     */
    public void registerScalingConfig(@Nonnull WorldScalingConfig config) {
        scalingConfigs.put(config.id(), config);
    }
    
    /**
     * Get a scaling config by ID.
     * 
     * @param id The config ID
     * @return The config, or null if not registered
     */
    @Nullable
    public WorldScalingConfig getScalingConfigById(@Nonnull String id) {
        return scalingConfigs.get(id);
    }

    // ========== Region Overrides ==========

    /**
     * Register a region scaling override.
     * <p>
     * Region overrides allow dungeons, events, and special areas to use
     * different scaling rules than the default distance-based calculation.
     * 
     * @param override The override to register
     */
    public void registerRegionOverride(@Nonnull RegionScalingOverride override) {
        regionOverrides.put(override.id(), override);
        LOGGER.fine("Registered region override: " + override.id() + " for pattern: " + override.regionPattern());
    }
    
    /**
     * Remove a region scaling override.
     * 
     * @param overrideId The override ID to remove
     */
    public void removeRegionOverride(@Nonnull String overrideId) {
        regionOverrides.remove(overrideId);
    }
    
    /**
     * Get the matching region override for a region tag.
     * <p>
     * Returns the highest-priority matching override, or null if none match.
     * 
     * @param regionTag The region tag (e.g., "dungeon/crypt", "event/world_boss")
     * @return The matching override with highest priority, or null
     */
    @Nullable
    public RegionScalingOverride getRegionOverride(@Nonnull String regionTag) {
        return regionOverrides.values().stream()
                .filter(o -> o.matches(regionTag))
                .max((a, b) -> Integer.compare(a.priority(), b.priority()))
                .orElse(null);
    }
    
    /**
     * Clear all region overrides.
     */
    public void clearRegionOverrides() {
        regionOverrides.clear();
    }

    // ========== NPC Scaling Registration ==========

    /**
     * Register a monster scaling configuration asset.
     * <p>
     * Maps each role name in the asset's "AppliesTo" list to this config.
     * 
     * @param asset The scaling config asset to register
     */
    public void registerScalingConfig(@Nonnull MonsterScalingConfigAsset asset) {
        for (String roleName : asset.getAppliesTo()) {
            MonsterScalingConfigAsset existing = roleScalingMap.put(roleName, asset);
            if (existing != null) {
                LOGGER.warning("Overwriting scaling config for NPC role '" + roleName 
                    + "': was " + existing.getId() + ", now " + asset.getId());
            }
            LOGGER.fine("Registered scaling config for NPC role: " + roleName);
        }
    }

    /**
     * Set the default scaled stats used when an NPC has no specific config.
     * 
     * @param stats The default stats to apply
     */
    public void setDefaultScaledStats(@Nonnull List<ScaledStatEntry> stats) {
        this.defaultScaledStats = List.copyOf(stats);
    }

    /**
     * Get the scaling configuration for an NPC role.
     * 
     * @param roleName The NPC role name
     * @return The scaling config, or null if none registered
     */
    @Nullable
    public MonsterScalingConfigAsset getScalingConfig(@Nonnull String roleName) {
        return roleScalingMap.get(roleName);
    }

    /**
     * Get the scaled stats for an NPC role.
     * <p>
     * Returns the NPC-specific stats if registered, otherwise the default stats.
     * 
     * @param roleName The NPC role name
     * @return List of scaled stat entries (never null)
     */
    @Nonnull
    public List<ScaledStatEntry> getScaledStats(@Nonnull String roleName) {
        MonsterScalingConfigAsset config = roleScalingMap.get(roleName);
        if (config != null) {
            return config.getScaledStats();
        }
        return defaultScaledStats;
    }

    /**
     * Check if an NPC role has a specific scaling configuration.
     */
    public boolean hasScalingConfig(@Nonnull String roleName) {
        return roleScalingMap.containsKey(roleName);
    }

    /**
     * Clear all registered scaling configurations.
     */
    public void clearScalingConfigs() {
        roleScalingMap.clear();
    }

    // ========== World Spawn Cache ==========

    /**
     * Get or calculate the spawn point for a world.
     * 
     * @param world The world
     * @return The world's spawn position, or default (0, 64, 0) if not configured
     */
    @Nonnull
    public Vector3d getWorldSpawn(@Nonnull World world) {
        String worldName = world.getName();
        return worldSpawnCache.computeIfAbsent(worldName, id -> calculateWorldSpawn(world));
    }

    /**
     * Calculate the spawn point for a world from its configuration.
     */
    @Nonnull
    @SuppressWarnings("deprecation")
    private Vector3d calculateWorldSpawn(@Nonnull World world) {
        WorldConfig config = world.getWorldConfig();
        if (config == null) {
            return DEFAULT_SPAWN;
        }

        ISpawnProvider spawnProvider = config.getSpawnProvider();
        if (spawnProvider == null) {
            return DEFAULT_SPAWN;
        }

        // getSpawnPoints() is deprecated but still usable for getting all spawn points
        Transform[] spawnPoints = spawnProvider.getSpawnPoints();
        if (spawnPoints == null || spawnPoints.length == 0) {
            return DEFAULT_SPAWN;
        }

        // Use the first spawn point as the "world spawn"
        return spawnPoints[0].getPosition();
    }

    /**
     * Clear the spawn cache (e.g., when worlds are reloaded).
     */
    public void clearSpawnCache() {
        worldSpawnCache.clear();
    }

    // ========== Level Calculation ==========

    /**
     * Calculate the distance from world spawn to a position.
     * <p>
     * Uses horizontal (XZ) distance only, ignoring Y coordinate.
     * 
     * @param world The world
     * @param position The entity's position
     * @return Distance in blocks from spawn (XZ plane only)
     */
    public double getDistanceFromSpawn(@Nonnull World world, @Nonnull Vector3d position) {
        Vector3d spawn = getWorldSpawn(world);
        double dx = position.getX() - spawn.getX();
        double dz = position.getZ() - spawn.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Calculate monster level based on position in world.
     * 
     * @param world The world
     * @param position The entity's position
     * @return The calculated monster level
     */
    public int calculateMonsterLevel(@Nonnull World world, @Nonnull Vector3d position) {
        double distance = getDistanceFromSpawn(world, position);
        return activeConfig.calculateLevel(distance);
    }
    
    /**
     * Calculate monster level based on position in world with region override support.
     * <p>
     * If the entity is in a region with a scaling override, applies that override.
     * Otherwise falls back to distance-based calculation.
     * 
     * @param world The world
     * @param position The entity's position
     * @param regionTag The region tag for the entity's location (e.g., "dungeon/crypt"), or null
     * @return The calculated monster level
     */
    public int calculateMonsterLevel(@Nonnull World world, @Nonnull Vector3d position, 
                                      @Nullable String regionTag) {
        // First calculate the base level from distance
        double distance = getDistanceFromSpawn(world, position);
        int baseLevel = activeConfig.calculateLevel(distance);
        
        // Check for region override
        if (regionTag != null && !regionTag.isEmpty()) {
            RegionScalingOverride override = getRegionOverride(regionTag);
            if (override != null) {
                // Handle CONFIG type specially
                if (override.type() == RegionScalingOverride.OverrideType.CONFIG) {
                    WorldScalingConfig altConfig = scalingConfigs.get(override.scalingConfigId());
                    if (altConfig != null) {
                        baseLevel = altConfig.calculateLevel(distance);
                    }
                }
                // Apply the override
                return override.apply(baseLevel, activeConfig.minLevel(), activeConfig.maxLevel());
            }
        }
        
        return baseLevel;
    }

    /**
     * Calculate monster level based on position in world using a specific config.
     * 
     * @param world The world
     * @param position The entity's position
     * @param config The scaling config to use
     * @return The calculated monster level
     */
    public int calculateMonsterLevel(@Nonnull World world, @Nonnull Vector3d position, 
                                      @Nonnull WorldScalingConfig config) {
        double distance = getDistanceFromSpawn(world, position);
        return config.calculateLevel(distance);
    }

    /**
     * Reset the singleton (for testing).
     */
    public static void resetForTesting() {
        INSTANCE = null;
    }
}
