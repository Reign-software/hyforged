package reign.software.hyforged.hud;

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
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.affix.ui.CharacterStatsPage;
import reign.software.hyforged.concentration.ui.ConcentrationPriorityPage;
import reign.software.hyforged.options.HyforgedOptionsPage;
import reign.software.hyforged.passive.ui.PassiveTreePage;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central hub page for navigating to all Hyforged systems.
 * <p>
 * Provides navigation buttons to:
 * <ul>
 *   <li><b>Character Stats</b> — View equipment affixes and derived stats</li>
 *   <li><b>Passive Tree</b> — Allocate passive skill points</li>
 *   <li><b>Concentration</b> — Manage ability priority queue</li>
 *   <li><b>Options</b> — Toggle Hyforged display and admin settings</li>
 * </ul>
 * <p>
 * Accessible via the Map page anchor button or {@code /hf} command.
 */
public class HyforgedHubPage extends InteractiveCustomUIPage<HyforgedHubPage.PageEventData> {

    private static final Logger LOGGER = Logger.getLogger(HyforgedHubPage.class.getName());
    private static final String PAGE_UI_FILE = "Hyforged/HyforgedHubPage.ui";

    private static final String ACTION_OPEN_STATS = "openStats";
    private static final String ACTION_OPEN_PASSIVE_TREE = "openPassiveTree";
    private static final String ACTION_OPEN_CONCENTRATION = "openConcentration";
    private static final String ACTION_OPEN_OPTIONS = "openOptions";
    private static final String ACTION_CLOSE = "close";

    private final PlayerRef playerRef;

    public HyforgedHubPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerRef = playerRef;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append(PAGE_UI_FILE);

        // Bind navigation buttons
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#StatsButton",
                EventData.of("Action", ACTION_OPEN_STATS), false);
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#PassiveTreeButton",
                EventData.of("Action", ACTION_OPEN_PASSIVE_TREE), false);
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#ConcentrationButton",
                EventData.of("Action", ACTION_OPEN_CONCENTRATION), false);
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#OptionsButton",
                EventData.of("Action", ACTION_OPEN_OPTIONS), false);
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of("Action", ACTION_CLOSE), false);
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData eventData
    ) {
        String action = eventData.getAction();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        switch (action) {
            case ACTION_CLOSE -> player.getPageManager().setPage(ref, store, Page.None);

            case ACTION_OPEN_STATS -> {
                player.getPageManager().openCustomPage(ref, store, new CharacterStatsPage(playerRef));
                LOGGER.log(Level.FINE, "[Hyforged] Hub → Character Stats");
            }
            case ACTION_OPEN_PASSIVE_TREE -> {
                player.getPageManager().openCustomPage(ref, store, new PassiveTreePage(playerRef, null));
                LOGGER.log(Level.FINE, "[Hyforged] Hub → Passive Tree");
            }
            case ACTION_OPEN_CONCENTRATION -> {
                player.getPageManager().openCustomPage(ref, store, new ConcentrationPriorityPage(playerRef));
                LOGGER.log(Level.FINE, "[Hyforged] Hub → Concentration Priority");
            }
            case ACTION_OPEN_OPTIONS -> {
                player.getPageManager().openCustomPage(ref, store, new HyforgedOptionsPage(playerRef));
                LOGGER.log(Level.FINE, "[Hyforged] Hub → Options");
            }
            default -> LOGGER.log(Level.FINE, "[Hyforged] Hub received unknown action: {0}", action);
        }
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
