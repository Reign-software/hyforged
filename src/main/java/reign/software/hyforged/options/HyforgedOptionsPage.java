package reign.software.hyforged.options;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.combat.hud.CombatLogHudSystem;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Options page for Hyforged player preferences and admin settings.
 * <p>
 * Displays toggleable options:
 * <ul>
 *   <li><b>Display section</b> (no permission required):
 *     Combat Log HUD, Combat Text, XP Notifications, Damage Numbers</li>
 *   <li><b>Admin section</b> (requires {@code hyforged.admin.options}):
 *     Debug Mode, Quality Debug</li>
 * </ul>
 */
public class HyforgedOptionsPage extends InteractiveCustomUIPage<HyforgedOptionsPage.PageEventData> {

    private static final String PAGE_UI_FILE = "Hyforged/HyforgedOptionsPage.ui";
    private static final String ADMIN_PERMISSION = "hyforged.admin.options";

    private final UUID playerUuid;

    public HyforgedOptionsPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerUuid = playerRef.getUuid();
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append(PAGE_UI_FILE);

        HyforgedPlayerOptions.PlayerOptions opts = HyforgedPlayerOptions.get(playerUuid);

        // Sync combat log HUD state with CombatLogHudSystem
        opts.setCombatLogHud(CombatLogHudSystem.isHudVisible(playerUuid));

        // Set toggle labels for display options
        setToggleState(commandBuilder, "#CombatLogHudRow", opts.isCombatLogHud());
        setToggleState(commandBuilder, "#CombatTextRow", opts.isCombatText());
        setToggleState(commandBuilder, "#XpNotificationsRow", opts.isXpNotifications());
        setToggleState(commandBuilder, "#DamageNumbersRow", opts.isDamageNumbers());

        // Check admin permission
        boolean isAdmin = PermissionsModule.get().hasPermission(playerUuid, ADMIN_PERMISSION);
        if (isAdmin) {
            commandBuilder.set("#AdminOptions.Visible", true);
            setToggleState(commandBuilder, "#DebugModeRow", opts.isDebugMode());
            setToggleState(commandBuilder, "#QualityDebugRow", opts.isQualityDebug());
        }

        // Bind toggle events
        bindToggle(eventBuilder, "#CombatLogHudRow", "toggleCombatLogHud");
        bindToggle(eventBuilder, "#CombatTextRow", "toggleCombatText");
        bindToggle(eventBuilder, "#XpNotificationsRow", "toggleXpNotifications");
        bindToggle(eventBuilder, "#DamageNumbersRow", "toggleDamageNumbers");

        if (isAdmin) {
            bindToggle(eventBuilder, "#DebugModeRow", "toggleDebugMode");
            bindToggle(eventBuilder, "#QualityDebugRow", "toggleQualityDebug");
        }

        // Bind close button
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CloseButton",
                EventData.of("Action", "close"),
                false
        );
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData eventData
    ) {
        String action = eventData.getAction();

        if ("close".equals(action)) {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent != null) {
                playerComponent.getPageManager().setPage(ref, store, Page.None);
            }
            return;
        }

        HyforgedPlayerOptions.PlayerOptions opts = HyforgedPlayerOptions.get(playerUuid);

        switch (action) {
            case "toggleCombatLogHud" -> {
                boolean newState = opts.toggleCombatLogHud();
                CombatLogHudSystem.setHudVisible(playerUuid, newState);
            }
            case "toggleCombatText" -> opts.toggleCombatText();
            case "toggleXpNotifications" -> opts.toggleXpNotifications();
            case "toggleDamageNumbers" -> opts.toggleDamageNumbers();
            case "toggleDebugMode" -> {
                if (PermissionsModule.get().hasPermission(playerUuid, ADMIN_PERMISSION)) {
                    opts.toggleDebugMode();
                }
            }
            case "toggleQualityDebug" -> {
                if (PermissionsModule.get().hasPermission(playerUuid, ADMIN_PERMISSION)) {
                    opts.toggleQualityDebug();
                }
            }
            default -> {
                return; // Unknown action, skip rebuild
            }
        }

        // Rebuild the page to reflect new state
        rebuild();
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private void setToggleState(UICommandBuilder cmd, String rowSelector, boolean enabled) {
        String label = enabled ? "ON" : "OFF";
        String color = enabled ? "#7cfc7c" : "#fc7c7c";
        cmd.set(rowSelector + " #ToggleLabel.Text", label);
        cmd.set(rowSelector + " #ToggleLabel.Style.TextColor", color);
    }

    private void bindToggle(UIEventBuilder events, String rowSelector, String action) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                rowSelector + " #ToggleButton",
                EventData.of("Action", action),
                false
        );
    }

    // ── Event Data ──────────────────────────────────────────────────

    public static class PageEventData {
        public static final BuilderCodec<PageEventData> CODEC = BuilderCodec.builder(
                        PageEventData.class, PageEventData::new
                )
                .append(new KeyedCodec<>("Action", Codec.STRING), (e, s) -> e.action = s, e -> e.action)
                .add()
                .build();

        private String action;

        public PageEventData() {
        }

        public String getAction() {
            return action;
        }
    }
}
