package reign.software.hyforged.progression.hud;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.affix.ui.CharacterStatsPage;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.hud.HyforgedHud;
import reign.software.hyforged.hud.HyforgedHudManager;
import reign.software.hyforged.options.HyforgedPlayerOptions;
import reign.software.hyforged.progression.CharacterProgression;
import reign.software.hyforged.progression.ClassProgression;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.stats.asset.ClassDefinition;
import reign.software.hyforged.stats.asset.ClassDefinitionRegistry;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Updates the progression section of the composite Hyforged HUD.
 * Displays character level, active class name, class level, and a character XP bar.
 * <p>
 * Polls at 0.5s intervals - progression doesn't change rapidly.
 */
public class ProgressionHudSystem extends DelayedEntitySystem<EntityStore> {

    private static final float UPDATE_INTERVAL_SEC = 0.5f;

    /** Cache of last-sent values per player to avoid redundant updates */
    private static final Map<UUID, ProgressionHudCache> playerCache = new ConcurrentHashMap<>();

    @Nonnull
    private final ComponentType<EntityStore, ProgressionComponent> progressionComponentType;

    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType;

    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType;

    @Nonnull
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    public ProgressionHudSystem() {
        super(UPDATE_INTERVAL_SEC);
        this.progressionComponentType = HyforgedPlugin.getInstance().getProgressionComponentType();
        this.playerComponentType = Player.getComponentType();
        this.playerRefComponentType = PlayerRef.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
        this.query = Query.and(progressionComponentType, playerComponentType, playerRefComponentType, uuidComponentType);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        ProgressionComponent progression = archetypeChunk.getComponent(index, progressionComponentType);
        Player player = archetypeChunk.getComponent(index, playerComponentType);
        PlayerRef playerRef = archetypeChunk.getComponent(index, playerRefComponentType);
        UUIDComponent uuidComponent = archetypeChunk.getComponent(index, uuidComponentType);

        if (progression == null || player == null || playerRef == null || uuidComponent == null) {
            return;
        }

        UUID playerUuid = uuidComponent.getUuid();

        HyforgedHud hud = HyforgedHudManager.getOrCreate(playerUuid, player, playerRef);
        if (hud == null) {
            return; // Client not ready yet
        }

        // Suppress progression bar while Character Stats page is open.
        if (isCharacterStatsPageOpen(player)) {
            hud.hideProgression();
            playerCache.remove(playerUuid);
            return;
        }

        // Respect the player's option toggle
        boolean enabled = HyforgedPlayerOptions.get(playerUuid).isProgressionHud();
        if (!enabled) {
            ProgressionHudCache cache = playerCache.get(playerUuid);
            if (cache != null) {
                hud.hideProgression();
                playerCache.remove(playerUuid);
            }
            return;
        }

        // Read character progression
        CharacterProgression charProg = progression.getCharacterProgression();
        int charLevel = charProg.level();
        long xpProgress = charProg.currentXp();
        long xpToNext = charProg.xpToNext();

        // Read active class info
        String activeClassId = progression.getActiveClassId();
        String className = "";
        int classLevel = 0;
        long classXpProgress = 0;
        long classXpToNext = 0;

        if (activeClassId != null && !activeClassId.isEmpty()) {
            ClassDefinition classDef = ClassDefinitionRegistry.get().get(activeClassId);
            if (classDef != null) {
                className = classDef.displayName();
            } else {
                // Fallback: extract name from ID (e.g. "hyforged:warrior" -> "Warrior")
                int colonIdx = activeClassId.indexOf(':');
                String raw = colonIdx >= 0 ? activeClassId.substring(colonIdx + 1) : activeClassId;
                className = raw.substring(0, 1).toUpperCase() + raw.substring(1);
            }

            ClassProgression classProg = progression.getClassProgressionSnapshot(activeClassId);
            classLevel = classProg.level();
            classXpProgress = classProg.currentXp();
            classXpToNext = classProg.xpToNext();
        }

        // Check if values changed since last update
        ProgressionHudCache cache = playerCache.get(playerUuid);
        boolean needsUpdate = cache == null
                || cache.charLevel != charLevel
                || cache.xpProgress != xpProgress
                || cache.xpToNext != xpToNext
                || cache.classLevel != classLevel
                || cache.classXpProgress != classXpProgress
                || cache.classXpToNext != classXpToNext
                || !cache.className.equals(className);

        if (!needsUpdate) {
            return;
        }

        hud.updateProgression(charLevel, className, classLevel, xpProgress, xpToNext, classXpProgress, classXpToNext);
        playerCache.put(playerUuid, new ProgressionHudCache(charLevel, className, classLevel, xpProgress, xpToNext, classXpProgress, classXpToNext));
    }

    private static boolean isCharacterStatsPageOpen(@Nonnull Player player) {
        CustomUIPage customPage = player.getPageManager().getCustomPage();
        return customPage instanceof CharacterStatsPage;
    }

    /**
     * Clear cached HUD state for a player (call on disconnect).
     *
     * @param playerUuid The player's UUID
     */
    public static void clearCache(@Nonnull UUID playerUuid) {
        playerCache.remove(playerUuid);
    }

    /**
     * Cached progression HUD values to avoid redundant network updates.
     */
    private record ProgressionHudCache(
            int charLevel,
            String className,
            int classLevel,
            long xpProgress,
            long xpToNext,
            long classXpProgress,
            long classXpToNext
    ) {}
}
