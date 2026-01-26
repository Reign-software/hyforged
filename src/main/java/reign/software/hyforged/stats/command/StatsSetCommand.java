package reign.software.hyforged.stats.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;

/**
 * Admin command to set a player's stat base value.
 * <p>
 * Usage: {@code /hyforged stats set <player> <stat> <value>}
 * <p>
 * This sets the base (allocated) value for a stat, which will trigger
 * a recalculation of all dependent stats.
 */
public class StatsSetCommand extends CommandBase {

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("Player not found or not in a world.");
    private static final Message MESSAGE_NO_STAT_COMPONENT = Message.raw("Player does not have a Hyforged stat component.");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player",
            "hyforged.commands.stats.set.player.desc",
            ArgTypes.PLAYER_REF
    );

    @Nonnull
    private final RequiredArg<String> statArg = this.withRequiredArg(
            "stat",
            "hyforged.commands.stats.set.stat.desc",
            ArgTypes.STRING
    );

    @Nonnull
    private final RequiredArg<Integer> valueArg = this.withRequiredArg(
            "value",
            "hyforged.commands.stats.set.value.desc",
            ArgTypes.INTEGER
    );

    public StatsSetCommand() {
        super("set", "hyforged.commands.stats.set.desc");
        this.requirePermission("hyforged.admin.stats.set");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef targetPlayerRef = this.playerArg.get(context);
        String statIdStr = this.statArg.get(context);
        int value = this.valueArg.get(context);

        Ref<EntityStore> ref = targetPlayerRef.getReference();

        if (ref == null || !ref.isValid()) {
            context.sendMessage(MESSAGE_PLAYER_NOT_FOUND);
            return;
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null) {
                context.sendMessage(MESSAGE_PLAYER_NOT_FOUND);
                return;
            }

            HyforgedStatComponent statComponent = store.getComponent(
                    ref,
                    HyforgedPlugin.getInstance().getHyforgedStatComponentType()
            );

            if (statComponent == null) {
                context.sendMessage(MESSAGE_NO_STAT_COMPONENT);
                return;
            }

            // Parse stat ID
            StatId statId;
            if (statIdStr.contains(":")) {
                statId = StatId.parse(statIdStr);
            } else {
                statId = StatId.hyforged(statIdStr);
            }

            StatDefinitionRegistry registry = StatDefinitionRegistry.get();
            int index = registry.getIndex(statId);
            if (index < 0) {
                context.sendMessage(Message.raw("Stat not found: " + statIdStr));
                return;
            }

            StatDefinition def = registry.getStat(index);
            if (def == null) {
                context.sendMessage(Message.raw("Stat not found: " + statIdStr));
                return;
            }

            // Clamp value to stat bounds
            int clampedValue = Math.max(def.minValue(), Math.min(def.maxValue(), value));

            // Get old value for feedback
            int oldValue = statComponent.getBaseValue(index);

            // Set the new base value (this will mark the stat dirty)
            statComponent.setBaseValue(index, clampedValue);

            // Get player name for feedback
            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            String playerName = playerRefComponent != null ? playerRefComponent.getUsername() : "Unknown";

            // Format stat name
            String displayName = formatStatName(statId);

            // Send success message
            String message = String.format("Set %s to %d for %s (was: %d)%s",
                    displayName,
                    clampedValue,
                    playerName,
                    oldValue,
                    clampedValue != value ? String.format(" [clamped from %d]", value) : ""
            );
            context.sendMessage(Message.raw(message));
        });
    }

    /**
     * Format a stat ID into a display-friendly name.
     */
    @Nonnull
    private String formatStatName(@Nonnull StatId statId) {
        String id = statId.name();
        String[] parts = id.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
    }
}
