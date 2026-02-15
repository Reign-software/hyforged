package reign.software.hyforged.hud;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages the single {@link HyforgedHud} instance per player.
 * <p>
 * Because Hytale only allows one {@code CustomUIHud} per player, all
 * Hyforged HUD sections live inside one composite HUD. This manager
 * lazily creates and caches that HUD, and cleans up on disconnect.
 * <p>
 * HUD creation is gated behind {@link #markReady(UUID)} which must be
 * called from a {@code PlayerReadyEvent} handler. Sending HUD commands
 * before the client has loaded the plugin's asset pack causes crashes.
 * <p>
 * All HUD systems should call {@link #getOrCreate(UUID, Player, PlayerRef)}
 * instead of creating their own {@code CustomUIHud} instances. The method
 * returns {@code null} if the player's client isn't ready yet.
 */
public final class HyforgedHudManager {

    private static final Logger LOGGER = Logger.getLogger(HyforgedHudManager.class.getName());

    /** Players whose clients have fully loaded (PlayerReadyEvent received) */
    private static final Set<UUID> readyPlayers = ConcurrentHashMap.newKeySet();

    /** Per-player HUD instances */
    private static final Map<UUID, HyforgedHud> playerHuds = new ConcurrentHashMap<>();

    private HyforgedHudManager() {}

    /**
     * Mark a player as ready to receive HUD commands.
     * Call from a {@code PlayerReadyEvent} handler.
     *
     * @param playerUuid The player's UUID
     */
    public static void markReady(@Nonnull UUID playerUuid) {
        readyPlayers.add(playerUuid);
        LOGGER.fine(() -> "Player ready for HUD: " + playerUuid);
    }

    /**
     * Check if a player's client has finished loading.
     *
     * @param playerUuid The player's UUID
     * @return true if the player is ready for HUD commands
     */
    public static boolean isReady(@Nonnull UUID playerUuid) {
        return readyPlayers.contains(playerUuid);
    }

    /**
     * Get or lazily create the composite HUD for a player.
     * <p>
     * Returns {@code null} if the player's client hasn't finished loading yet
     * (i.e. {@link #markReady(UUID)} hasn't been called). This prevents the
     * crash caused by sending UI commands before the .ui file is available.
     * <p>
     * On first call for a ready player this creates the HUD, sets it on the
     * player's {@code HudManager}, and calls {@code show()} which sends
     * the initial .ui file to the client.
     *
     * @param playerUuid The player's UUID
     * @param player     The player entity
     * @param playerRef  The player reference (for packet sending)
     * @return The composite HUD instance, or null if client is not ready
     */
    @Nullable
    public static HyforgedHud getOrCreate(@Nonnull UUID playerUuid,
                                          @Nonnull Player player,
                                          @Nonnull PlayerRef playerRef) {
        if (!readyPlayers.contains(playerUuid)) {
            return null;
        }

        HyforgedHud hud = playerHuds.get(playerUuid);
        if (hud != null) {
            return hud;
        }

        hud = new HyforgedHud(playerRef);
        player.getHudManager().setCustomHud(playerRef, hud);
        playerHuds.put(playerUuid, hud);
        LOGGER.fine(() -> "Created HyforgedHud for player " + playerUuid);
        return hud;
    }

    /**
     * Get the composite HUD for a player, or null if not yet created.
     *
     * @param playerUuid The player's UUID
     * @return The HUD instance, or null
     */
    @Nullable
    public static HyforgedHud get(@Nonnull UUID playerUuid) {
        return playerHuds.get(playerUuid);
    }

    /**
     * Remove the HUD for a player (on disconnect).
     *
     * @param playerUuid The player's UUID
     */
    public static void remove(@Nonnull UUID playerUuid) {
        readyPlayers.remove(playerUuid);
        playerHuds.remove(playerUuid);
        LOGGER.fine(() -> "Removed HyforgedHud for player " + playerUuid);
    }
}
