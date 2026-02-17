package reign.software.hyforged.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageEvent;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageEventType;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBinding;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Injects a Hyforged Menu button into the Map page via an
 * {@code UpdateAnchorUI} packet targeting the built-in
 * {@code #ServerContent} anchor element in {@code MapPage.ui}.
 * <p>
 * Clicking the button opens the {@link HyforgedHubPage}, which
 * provides navigation to all Hyforged systems.
 * <p>
 * Events are intercepted via a {@link PacketFilter} on
 * {@link CustomPageEvent} packets since {@code AnchorActionModule}
 * is not exposed in the plugin API.
 */
public final class HyforgedReticleUI {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Anchor ID for the Map page's built-in server content anchor. */
    public static final String ANCHOR_ID = "MapServerContent";

    /** UI file path for the menu button (relative to Custom/). */
    private static final String UI_FILE = "Hyforged/HyforgedQuickActions.ui";

    /** Action name for opening the hub page. */
    private static final String ACTION_OPEN_HUB = "hyforgedOpenHub";

    /** All action names we handle, for fast lookup. */
    private static final Set<String> HANDLED_ACTIONS = Set.of(ACTION_OPEN_HUB);

    // ── Reflection handles (cached at class load) ───────────────────
    //
    // UpdateAnchorUI and ToClientPacket are internal server classes
    // not exposed in the plugin API JAR. We must use reflection.

    private static final Constructor<?> UPDATE_ANCHOR_CTOR;
    private static final Method WRITE_NO_CACHE;

    static {
        try {
            Class<?> updateAnchorClass = Class.forName(
                    "com.hypixel.hytale.protocol.packets.interface_.UpdateAnchorUI");
            UPDATE_ANCHOR_CTOR = updateAnchorClass.getConstructor(
                    String.class, boolean.class,
                    CustomUICommand[].class, CustomUIEventBinding[].class);

            Class<?> toClientPacket = Class.forName(
                    "com.hypixel.hytale.protocol.ToClientPacket");
            WRITE_NO_CACHE = PacketHandler.class.getMethod("writeNoCache", toClientPacket);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(
                    "HyforgedReticleUI: failed to resolve UpdateAnchorUI or writeNoCache — " + e);
        }
    }

    // ── Inbound filter (saved for deregistration) ───────────────────

    private static PacketFilter inboundFilter;

    private HyforgedReticleUI() {
    }

    // ── Lifecycle ───────────────────────────────────────────────────

    /**
     * Install the inbound packet filter that handles reticle button events.
     * Call once during plugin startup.
     */
    public static void install() {
        inboundFilter = HyforgedReticleUI::filterInbound;
        PacketAdapters.registerInbound(inboundFilter);
        LOGGER.atInfo().log("[Hyforged] Installed Map anchor inbound event filter");
    }

    /**
     * Remove the inbound packet filter. Call during plugin shutdown.
     */
    public static void uninstall() {
        if (inboundFilter != null) {
            PacketAdapters.deregisterInbound(inboundFilter);
            inboundFilter = null;
        }
    }

    // ── Sending UI to client ────────────────────────────────────────

    /**
     * Send the Hyforged menu button to the Map page's server content anchor.
     * Should be called after the player is ready (post-{@code PlayerReadyEvent}).
     *
     * @param playerRef The player to inject the button for
     */
    public static void send(@Nonnull PlayerRef playerRef) {
        LOGGER.at(Level.FINE).log("[Hyforged] Building Map anchor button for %s", playerRef.getUsername());

        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.append(UI_FILE);

        UIEventBuilder eventBuilder = new UIEventBuilder();
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#HyforgedMenuButton",
                EventData.of("action", ACTION_OPEN_HUB), false);

        sendAnchorPacket(playerRef.getPacketHandler(),
                ANCHOR_ID, true, commandBuilder.getCommands(), eventBuilder.getEvents());
    }

    /**
     * Clear the Hyforged button from the Map page anchor.
     *
     * @param playerRef The player to clear the button for
     */
    public static void clear(@Nonnull PlayerRef playerRef) {
        sendAnchorPacket(playerRef.getPacketHandler(), ANCHOR_ID, true, null, null);
    }

    // ── Inbound event handling ──────────────────────────────────────

    /**
     * Inbound packet filter callback.
     * Returns {@code true} to consume the packet (our event), {@code false} to pass through.
     */
    private static boolean filterInbound(@Nonnull PacketHandler packetHandler, @Nonnull Packet packet) {
        if (!(packet instanceof CustomPageEvent event)) {
            return false;
        }
        if (event.type != CustomPageEventType.Data || event.data == null) {
            return false;
        }

        String action = parseAction(event.data);
        if (action == null || !HANDLED_ACTIONS.contains(action)) {
            return false; // Not our event — let it pass through
        }

        LOGGER.at(Level.FINE).log("[Hyforged] Received reticle button event: %s", action);

        // Resolve player from the packet handler
        var auth = packetHandler.getAuth();
        if (auth == null) {
            return true; // Consume but can't handle — no auth context
        }

        PlayerRef playerRef = Universe.get().getPlayer(auth.getUuid());
        if (playerRef == null) {
            return true;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return true;
        }

        // Dispatch to the world thread for safe entity access
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        final String finalAction = action;
        world.execute(() -> {
            if (ref.isValid()) {
                handleAction(finalAction, playerRef, ref, store);
            }
        });

        return true; // Consume — we handled it
    }

    /**
     * Handle an anchor button action on the world thread.
     */
    private static void handleAction(@Nonnull String action,
                                     @Nonnull PlayerRef playerRef,
                                     @Nonnull Ref<EntityStore> ref,
                                     @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if (ACTION_OPEN_HUB.equals(action)) {
            player.getPageManager().openCustomPage(ref, store, new HyforgedHubPage(playerRef));
            LOGGER.at(Level.FINE).log("[Hyforged] Opened Hyforged Hub from Map anchor");
        } else {
            LOGGER.atWarning().log("[Hyforged] Unknown anchor action: %s", action);
        }
    }

    // ── Internal helpers ────────────────────────────────────────────

    /**
     * Parse the "action" field from a JSON event data string.
     */
    private static String parseAction(@Nonnull String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.has("action") ? obj.get("action").getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Construct and send an {@code UpdateAnchorUI} packet via reflection.
     */
    private static void sendAnchorPacket(@Nonnull PacketHandler handler,
                                         @Nonnull String anchorId,
                                         boolean clear,
                                         CustomUICommand[] commands,
                                         CustomUIEventBinding[] eventBindings) {
        try {
            Object packet = UPDATE_ANCHOR_CTOR.newInstance(anchorId, clear, commands, eventBindings);
            LOGGER.at(Level.FINE).log("[Hyforged] Sending UpdateAnchorUI: anchor=%s, clear=%s, commands=%s, events=%s",
                    anchorId, clear,
                    commands != null ? commands.length : 0,
                    eventBindings != null ? eventBindings.length : 0);
            WRITE_NO_CACHE.invoke(handler, packet);
            LOGGER.at(Level.FINE).log("[Hyforged] UpdateAnchorUI sent successfully");
        } catch (Exception e) {
            LOGGER.atWarning().log("[Hyforged] Failed to send UpdateAnchorUI: %s - %s",
                    e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                LOGGER.atWarning().log("[Hyforged]   Caused by: %s - %s",
                        e.getCause().getClass().getName(), e.getCause().getMessage());
            }
        }
    }
}
