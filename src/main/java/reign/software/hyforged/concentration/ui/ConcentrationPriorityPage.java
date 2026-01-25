package reign.software.hyforged.concentration.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.concentration.ConcentratedAbility;
import reign.software.hyforged.concentration.ConcentrationService;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Concentration priority queue UI page.
 */
public class ConcentrationPriorityPage extends InteractiveCustomUIPage<ConcentrationPriorityPage.PageEventData> {

    private static final String PAGE_UI_FILE = "UI/Hyforged/ConcentrationPriorityPage.ui";
    private static final String ROW_UI_FILE = "UI/Hyforged/ConcentrationPriorityRow.ui";

    private static final String ACTION_CLOSE = "close";
    private static final String ACTION_REFRESH = "refresh";
    private static final String ACTION_MOVE_UP = "move_up";
    private static final String ACTION_MOVE_DOWN = "move_down";

    public ConcentrationPriorityPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append(PAGE_UI_FILE);
        buildPriorityView(ref, commandBuilder, eventBuilder);
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData eventData
    ) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            sendUpdate();
            return;
        }

        if (ACTION_CLOSE.equals(eventData.getAction())) {
            playerComponent.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        boolean refresh = ACTION_REFRESH.equals(eventData.getAction());
        boolean updated = false;

        if (ACTION_MOVE_UP.equals(eventData.getAction())) {
            updated = moveAbility(ref, eventData.getAbilityId(), true);
        } else if (ACTION_MOVE_DOWN.equals(eventData.getAction())) {
            updated = moveAbility(ref, eventData.getAbilityId(), false);
        } else if (eventData.hasReorderIndices()) {
            updated = reorderByIndices(ref, eventData.resolveOldIndex(), eventData.resolveNewIndex());
        }

        if (refresh || updated) {
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            buildPriorityView(ref, commandBuilder, eventBuilder);
            sendUpdate(commandBuilder, eventBuilder, false);
            return;
        }

        sendUpdate();
    }

    private void buildPriorityView(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder
    ) {
        ConcentrationService service = ConcentrationService.get();
        int current = service.getCurrentConcentration(ref);
        int max = service.getMaxConcentration(ref);

        commandBuilder.set("#CurrentConcentration.Text", current + "/" + max);

        buildPriorityList(ref, commandBuilder, eventBuilder);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of(PageEventData.KEY_ACTION, ACTION_CLOSE), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshButton", EventData.of(PageEventData.KEY_ACTION, ACTION_REFRESH), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ElementReordered, "#PriorityList", false);
    }

    private void buildPriorityList(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder
    ) {
        commandBuilder.clear("#PriorityList");

        List<ConcentratedAbility> abilities = ConcentrationService.get().getPriorityQueue(ref);
        if (abilities.isEmpty()) {
            commandBuilder.appendInline(
                    "#PriorityList",
                    "Label { Text: No concentrated abilities found.; Style: (Alignment: Center; FontSize: 12; Color: #888888); }"
            );
            return;
        }

        for (int i = 0; i < abilities.size(); i++) {
            ConcentratedAbility ability = abilities.get(i);
            String selector = "#PriorityList[" + i + "]";

            commandBuilder.append("#PriorityList", ROW_UI_FILE);
            commandBuilder.set(selector + " #OrderIndex.Text", Integer.toString(i + 1));
            commandBuilder.set(selector + " #AbilityName.TextSpans", buildAbilityMessage(formatAbilityName(ability.abilityId()), ability.enabled()));
            commandBuilder.set(selector + " #AbilityId.Text", ability.abilityId());
            commandBuilder.set(selector + " #Cost.Text", Integer.toString(ability.cost()));
            commandBuilder.set(selector + " #Status.TextSpans", buildStatusMessage(ability.enabled()));
            commandBuilder.set(selector + " #MoveUp.Visible", i > 0);
            commandBuilder.set(selector + " #MoveDown.Visible", i < abilities.size() - 1);

            EventData moveUp = new EventData().append(PageEventData.KEY_ACTION, ACTION_MOVE_UP).append(PageEventData.KEY_ABILITY_ID, ability.abilityId());
            EventData moveDown = new EventData().append(PageEventData.KEY_ACTION, ACTION_MOVE_DOWN).append(PageEventData.KEY_ABILITY_ID, ability.abilityId());
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector + " #MoveUp", moveUp, false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector + " #MoveDown", moveDown, false);
        }
    }

    private boolean moveAbility(@Nonnull Ref<EntityStore> ref, String abilityId, boolean moveUp) {
        if (abilityId == null || abilityId.isBlank()) {
            return false;
        }
        List<String> order = getAbilityOrder(ref);
        if (order.isEmpty()) {
            return false;
        }

        int index = order.indexOf(abilityId);
        if (index < 0) {
            return false;
        }
        int target = moveUp ? index - 1 : index + 1;
        if (target < 0 || target >= order.size()) {
            return false;
        }
        order.remove(index);
        order.add(target, abilityId);
        ConcentrationService.get().setPriorityOrder(ref, order);
        return true;
    }

    private boolean reorderByIndices(@Nonnull Ref<EntityStore> ref, int oldIndex, int newIndex) {
        List<String> order = getAbilityOrder(ref);
        if (order.isEmpty()) {
            return false;
        }

        int from = normalizeIndex(oldIndex, order.size());
        int to = normalizeIndex(newIndex, order.size());
        if (from < 0 || to < 0 || from == to) {
            return false;
        }
        String abilityId = order.remove(from);
        order.add(to, abilityId);
        ConcentrationService.get().setPriorityOrder(ref, order);
        return true;
    }

    @Nonnull
    private List<String> getAbilityOrder(@Nonnull Ref<EntityStore> ref) {
        List<ConcentratedAbility> abilities = ConcentrationService.get().getPriorityQueue(ref);
        List<String> order = new ArrayList<>(abilities.size());
        for (ConcentratedAbility ability : abilities) {
            order.add(ability.abilityId());
        }
        return order;
    }

    private static int normalizeIndex(int rawIndex, int size) {
        if (rawIndex >= 0 && rawIndex < size) {
            return rawIndex;
        }
        if (rawIndex > 0 && rawIndex <= size) {
            return rawIndex - 1;
        }
        return -1;
    }

    @Nonnull
    private static Message buildAbilityMessage(@Nonnull String name, boolean enabled) {
        Message message = Message.raw(name);
        if (!enabled) {
            message = message.color("#888888");
        }
        return message;
    }

    @Nonnull
    private static Message buildStatusMessage(boolean enabled) {
        return enabled
                ? Message.raw("Enabled").color("#60d394")
                : Message.raw("Disabled").color("#d36c6c");
    }

    @Nonnull
    private static String formatAbilityName(@Nonnull String abilityId) {
        String trimmed = abilityId.trim();
        int separator = trimmed.indexOf(':');
        String base = separator >= 0 ? trimmed.substring(separator + 1) : trimmed;
        String[] parts = base.split("[-_]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.length() == 0 ? base : builder.toString();
    }

    public static class PageEventData {
        static final String KEY_ACTION = "Action";
        static final String KEY_ABILITY_ID = "AbilityId";
        static final String KEY_OLD_INDEX = "OldIndex";
        static final String KEY_NEW_INDEX = "NewIndex";
        static final String KEY_FROM_INDEX = "FromIndex";
        static final String KEY_TO_INDEX = "ToIndex";

        @Nonnull
        static final BuilderCodec<PageEventData> CODEC = BuilderCodec
                .builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (data, value) -> data.action = value, data -> data.action)
                .add()
                .append(new KeyedCodec<>(KEY_ABILITY_ID, Codec.STRING), (data, value) -> data.abilityId = value, data -> data.abilityId)
                .add()
                .append(new KeyedCodec<>(KEY_OLD_INDEX, Codec.STRING), (data, value) -> data.oldIndex = value, data -> data.oldIndex)
                .add()
                .append(new KeyedCodec<>(KEY_NEW_INDEX, Codec.STRING), (data, value) -> data.newIndex = value, data -> data.newIndex)
                .add()
                .append(new KeyedCodec<>(KEY_FROM_INDEX, Codec.STRING), (data, value) -> data.fromIndex = value, data -> data.fromIndex)
                .add()
                .append(new KeyedCodec<>(KEY_TO_INDEX, Codec.STRING), (data, value) -> data.toIndex = value, data -> data.toIndex)
                .add()
                .build();

        private String action;
        private String abilityId;
        private String oldIndex;
        private String newIndex;
        private String fromIndex;
        private String toIndex;

        public PageEventData() {
        }

        public String getAction() {
            return action;
        }

        public String getAbilityId() {
            return abilityId;
        }

        boolean hasReorderIndices() {
            return resolveOldIndex() >= 0 && resolveNewIndex() >= 0;
        }

        int resolveOldIndex() {
            return parseIndex(firstNonNull(oldIndex, fromIndex));
        }

        int resolveNewIndex() {
            return parseIndex(firstNonNull(newIndex, toIndex));
        }

        private static String firstNonNull(String primary, String secondary) {
            return primary != null ? primary : secondary;
        }

        private static int parseIndex(String value) {
            if (value == null || value.isBlank()) {
                return -1;
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
    }
}
