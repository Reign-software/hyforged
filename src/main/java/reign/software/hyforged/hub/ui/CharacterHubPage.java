package reign.software.hyforged.hub.ui;

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
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.ui.CharacterStatsPage;
import reign.software.hyforged.concentration.ui.ConcentrationPriorityPage;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.service.PassiveTreeService;
import reign.software.hyforged.passive.ui.PassiveTreePageHyUI;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Character Hub page - central navigation for all character-related screens.
 * <p>
 * Provides quick access to:
 * <ul>
 *   <li>Character Stats - view attributes and equipment</li>
 *   <li>Passive Tree - allocate passive skill points</li>
 *   <li>Concentration Priority - manage ability focus order</li>
 * </ul>
 * <p>
 * Access via command: {@code /hyforged hub} or {@code /hyforged character hub}
 */
public class CharacterHubPage extends InteractiveCustomUIPage<CharacterHubPage.PageEventData> {

    private static final Logger LOGGER = Logger.getLogger(CharacterHubPage.class.getName());

    /** UI file path for the character hub page layout */
    private static final String PAGE_UI_FILE = "Hyforged/CharacterHubPage.ui";

    /**
     * Create a new CharacterHubPage for a player.
     *
     * @param playerRef The player to display the hub for
     */
    public CharacterHubPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        // Append the main UI layout
        commandBuilder.append(PAGE_UI_FILE);

        // Get player info
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            commandBuilder.set("#PlayerName.Text", playerRef.getUsername());
        }

        // Populate summary info
        populateSummary(commandBuilder, ref, store);

        // Add navigation button events
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#StatsButton",
                EventData.of("Action", "openStats"),
                false
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#PassiveTreeButton",
                EventData.of("Action", "openPassiveTree"),
                false
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ConcentrationButton",
                EventData.of("Action", "openConcentration"),
                false
        );

        // Add close button event
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CloseButton",
                EventData.of("Action", "close"),
                false
        );
    }

    /**
     * Populate the quick summary section with player info.
     */
    private void populateSummary(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store
    ) {
        // Get progression info
        var progressionComponentType = HyforgedPlugin.getInstance().getProgressionComponentType();
        if (progressionComponentType != null) {
            ProgressionComponent progression = store.getComponent(ref, progressionComponentType);
            if (progression != null) {
                commandBuilder.set("#LevelInfo.Text", "Level: " + progression.getCharacterLevel());
                String className = progression.getActiveClassId() != null ? 
                        progression.getActiveClassId() : "None";
                commandBuilder.set("#ClassInfo.Text", "Class: " + className);
            }
        }

        // Get health info using stat indices from registry
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int healthIndex = registry.getIndex("hyforged:max-health-flat");
        if (healthIndex >= 0) {
            int maxHealth = StatAccessor.getStatValueInt(store, ref, healthIndex);
            commandBuilder.set("#HealthInfo.Text", "Health: " + maxHealth);
        }

        // Get passive points info
        var passiveComponentType = HyforgedPlugin.getInstance().getPassiveTreeComponentType();
        if (passiveComponentType != null) {
            PassiveTreeComponent passiveComponent = store.getComponent(ref, passiveComponentType);
            if (passiveComponent != null) {
                int availablePoints = PassiveTreeService.get().getAvailablePoints(ref, null);
                commandBuilder.set("#PassivePoints.Text", "Passive Points: " + availablePoints);
            }
        }
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData eventData
    ) {
        String action = eventData.getAction();
        if (action == null) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        switch (action) {
            case "close" -> {
                player.getPageManager().setPage(ref, store, Page.None);
            }
            case "openStats" -> {
                CharacterStatsPage statsPage = new CharacterStatsPage(this.playerRef);
                player.getPageManager().openCustomPage(ref, store, statsPage);
                LOGGER.fine("Opened Character Stats from hub");
            }
            case "openPassiveTree" -> {
                // Use HyUI-based page
                PassiveTreePageHyUI.open(this.playerRef, ref, store, null);
                LOGGER.fine("Opened Passive Tree from hub");
            }
            case "openConcentration" -> {
                ConcentrationPriorityPage concentrationPage = new ConcentrationPriorityPage(this.playerRef);
                player.getPageManager().openCustomPage(ref, store, concentrationPage);
                LOGGER.fine("Opened Concentration Priority from hub");
            }
            default -> LOGGER.fine("Unknown hub action: " + action);
        }
    }

    // ========== PAGE EVENT DATA ==========

    /**
     * Event data for page interactions.
     */
    public static class PageEventData {
        public static final BuilderCodec<PageEventData> CODEC = BuilderCodec.builder(
                        PageEventData.class, PageEventData::new
                )
                .append(new KeyedCodec<>("Action", Codec.STRING), (e, s) -> e.action = s, e -> e.action)
                .add()
                .build();

        private String action;

        public PageEventData() {}

        public String getAction() {
            return action;
        }
    }
}
