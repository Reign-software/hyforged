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
import reign.software.hyforged.affix.ui.CharacterStatsPage;
import reign.software.hyforged.concentration.ui.ConcentrationPriorityPage;
import reign.software.hyforged.passive.ui.PassiveTreePage;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Injects quick-access character buttons into the Reticle HUD's {@code #ServerEvent}
 * anchor point using the {@code UpdateAnchorUI} system (accessed via reflection
 * because the class is not yet exposed in the plugin API JAR).
 * <p>
 * Inbound events are intercepted via a {@link PacketFilter} on
 * {@link CustomPageEvent} packets — no dependency on {@code AnchorActionModule}.
 * <p>
 * Buttons:
 * <ul>
 *   <li><b>S</b> — Character Stats page</li>
 *   <li><b>P</b> — Passive Tree page</li>
 *   <li><b>C</b> — Concentration Priority page</li>
 * </ul>
 * <p>
 * Replaces the old CharacterHubPage with always-visible HUD buttons.
 */
public final class HyforgedReticleUI {

    private static final Logger LOGGER = Logger.getLogger(HyforgedReticleUI.class.getName());

    /** Anchor ID matching the Reticle.ui {@code #ServerEvent} element. */
    public static final String ANCHOR_ID = "ReticleServerEvent";

    /** UI file path (relative to Custom/ in our asset pack). */
    private static final String UI_FILE = "Hyforged/HyforgedReticleButtons.ui";

    // Action names — sent as the "action" field in event bindings
    private static final String ACTION_OPEN_STATS = "hyforgedOpenStats";
    private static final String ACTION_OPEN_PASSIVE_TREE = "hyforgedOpenPassiveTree";
    private static final String ACTION_OPEN_CONCENTRATION = "hyforgedOpenConcentration";

    /** All action names we handle, for fast lookup. */
    private static final Set<String> HANDLED_ACTIONS = Set.of(
            ACTION_OPEN_STATS, ACTION_OPEN_PASSIVE_TREE, ACTION_OPEN_CONCENTRATION
    );

    // ── Reflection handles (cached at class load) ───────────────────

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
        LOGGER.info("Installed HyforgedReticleUI inbound event filter");
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
     * Send the button strip to a player's Reticle HUD.
     * Should be called after the player is ready (post-{@code PlayerReadyEvent}).
     *
     * @param playerRef The player to inject the buttons for
     */
    public static void send(@Nonnull PlayerRef playerRef) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.append(UI_FILE);

        UIEventBuilder eventBuilder = new UIEventBuilder();
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#StatsButton",
                EventData.of("action", ACTION_OPEN_STATS), false);
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#PassiveTreeButton",
                EventData.of("action", ACTION_OPEN_PASSIVE_TREE), false);
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#ConcentrationButton",
                EventData.of("action", ACTION_OPEN_CONCENTRATION), false);

        sendAnchorPacket(playerRef.getPacketHandler(),
                ANCHOR_ID, true, commandBuilder.getCommands(), eventBuilder.getEvents());

        LOGGER.fine("Sent reticle buttons to " + playerRef.getUsername());
    }

    /**
     * Clear the button strip from a player's Reticle HUD.
     *
     * @param playerRef The player to clear buttons for
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
     * Handle a reticle button action on the world thread.
     */
    private static void handleAction(@Nonnull String action,
                                     @Nonnull PlayerRef playerRef,
                                     @Nonnull Ref<EntityStore> ref,
                                     @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        switch (action) {
            case ACTION_OPEN_STATS -> {
                player.getPageManager().openCustomPage(ref, store, new CharacterStatsPage(playerRef));
                LOGGER.fine("Opened Character Stats from reticle button");
            }
            case ACTION_OPEN_PASSIVE_TREE -> {
                player.getPageManager().openCustomPage(ref, store, new PassiveTreePage(playerRef, null));
                LOGGER.fine("Opened Passive Tree from reticle button");
            }
            case ACTION_OPEN_CONCENTRATION -> {
                player.getPageManager().openCustomPage(ref, store, new ConcentrationPriorityPage(playerRef));
                LOGGER.fine("Opened Concentration Priority from reticle button");
            }
            default -> LOGGER.warning("Unknown reticle action: " + action);
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
            WRITE_NO_CACHE.invoke(handler, packet);
        } catch (ReflectiveOperationException e) {
            LOGGER.warning("Failed to send UpdateAnchorUI: " + e.getMessage());
        }
    }
}
