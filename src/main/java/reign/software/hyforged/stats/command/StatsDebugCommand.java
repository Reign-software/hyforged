package reign.software.hyforged.stats.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.breakdown.StatBreakdown;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.service.HyforgedStatQueryService;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Admin command to display stat breakdown for a player.
 * <p>
 * Usage: {@code /hyforged stats debug <player> [stat]}
 * <p>
 * If no stat is specified, shows a summary of all core stats.
 * If a stat ID is specified, shows detailed breakdown for that stat.
 */
public class StatsDebugCommand extends CommandBase {

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("Player not found or not in a world.");
    private static final Message MESSAGE_NO_STAT_COMPONENT = Message.raw("Player does not have a Hyforged stat component.");

    @Nonnull
    private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg(
            "player", 
            "hyforged.commands.stats.debug.player.desc", 
            ArgTypes.PLAYER_REF
    );

    @Nonnull
    private final OptionalArg<String> statArg = this.withOptionalArg(
            "stat",
            "hyforged.commands.stats.debug.stat.desc",
            ArgTypes.STRING
    );

    public StatsDebugCommand() {
        super("debug", "hyforged.commands.stats.debug.desc");
        this.requirePermission("hyforged.admin.stats.debug");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef targetPlayerRef = this.playerArg.get(context);
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

            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            String playerName = playerRefComponent != null ? playerRefComponent.getUsername() : "Unknown";

            if (this.statArg.provided(context)) {
                // Show detailed breakdown for specific stat
                String statIdStr = this.statArg.get(context);
                showStatBreakdown(context, playerName, statComponent, statIdStr, store, ref);
            } else {
                // Show summary of all core stats
                showStatSummary(context, playerName, statComponent, store, ref);
            }
        });
    }

    /**
     * Show a summary of all core stats for the player.
     */
    private void showStatSummary(
            @Nonnull CommandContext context,
            @Nonnull String playerName,
            @Nonnull HyforgedStatComponent component,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();

        StringBuilder sb = new StringBuilder();
        sb.append("═══════ Stats for ").append(playerName).append(" ═══════\n");

        // Ability Scores - query by Type=ability-score tag
        sb.append("\n▸ Ability Scores\n");
        for (StatId statId : registry.getStatIdsForTagValue("Type", "ability-score")) {
            appendStatLine(sb, registry, component, statId, store, ref);
        }

        // Offensive stats - query by category
        sb.append("\n▸ Offensive\n");
        for (StatDefinition stat : registry.getStatsInCategory("offense")) {
            appendStatLine(sb, registry, component, stat.id(), store, ref);
        }

        // Defensive stats - query by category
        sb.append("\n▸ Defensive\n");
        for (StatDefinition stat : registry.getStatsInCategory("defense")) {
            appendStatLine(sb, registry, component, stat.id(), store, ref);
        }

        // Modifiers summary
        List<HyforgedModifier> modifiers = StatAccessor.getAllHyforgedModifiers(store, ref);
        if (modifiers.isEmpty()) {
            modifiers = component.getModifiers();
        }
        sb.append("\nActive modifiers: ").append(modifiers.size()).append("\n");

        context.sendMessage(Message.raw(sb.toString()));
    }

    /**
     * Append a single stat line to the summary.
     */
    private void appendStatLine(
            @Nonnull StringBuilder sb,
            @Nonnull StatDefinitionRegistry registry,
            @Nonnull HyforgedStatComponent component,
            @Nonnull StatId statId,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        int index = registry.getIndex(statId);
        if (index < 0) {
            return;
        }

        StatDefinition def = registry.getStat(index);
        if (def == null) {
            return;
        }

        // Use StatAccessor for unified stat value access
        int value = StatAccessor.getStatValueInt(store, ref, index);
        int baseValue = component.getBaseValue(index);
        int bonus = value - baseValue;

        String displayName = formatStatName(statId);
        String valueStr;
        if (def.isRating()) {
            // Show rating stats as percentage effectiveness at level 1
            valueStr = String.format("%d (rating)", value);
        } else if (statId.name().contains("bps")) {
            // Show basis points as percentage
            valueStr = String.format("%.1f%%", value / 100.0);
        } else {
            valueStr = String.valueOf(value);
        }

        if (bonus != 0) {
            String bonusColor = bonus > 0 ? "" : "";
            sb.append(String.format("  %-20s %s %s(%+d)\n", displayName, valueStr, bonusColor, bonus));
        } else {
            sb.append(String.format("  %-20s %s\n", displayName, valueStr));
        }
    }

    /**
     * Show detailed breakdown for a specific stat.
     */
    private void showStatBreakdown(
            @Nonnull CommandContext context,
            @Nonnull String playerName,
            @Nonnull HyforgedStatComponent component,
            @Nonnull String statIdStr,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();

        // Parse stat ID
        StatId statId;
        if (statIdStr.contains(":")) {
            statId = StatId.parse(statIdStr);
        } else {
            statId = StatId.hyforged(statIdStr);
        }

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

        // Get full breakdown
        StatBreakdown breakdown = HyforgedStatQueryService.getStatBreakdown(component, index, 1, ref);

        StringBuilder sb = new StringBuilder();
        sb.append("═══════ ").append(formatStatName(statId)).append(" Breakdown ═══════\n");
        sb.append("Player: ").append(playerName).append("\n\n");

        // Definition info
        sb.append("▸ Definition\n");
        sb.append(String.format("  ID: %s\n", statId));
        sb.append(String.format("  Category: %s\n", def.category()));
        sb.append(String.format("  Is Rating: %s\n", def.isRating()));
        sb.append(String.format("  Bounds: [%d, %d]\n", def.minValue(), def.maxValue()));

        // Base value
        sb.append("\n▸ Base Value\n");
        int baseValue = component.getBaseValue(index);
        sb.append(String.format("  Allocated/Default: %d\n", baseValue));

        if (breakdown != null) {
            // Scaling contributions
            if (!breakdown.scalingContributions().isEmpty()) {
                sb.append("\n▸ Scaling Contributions\n");
                for (var contrib : breakdown.scalingContributions()) {
                    sb.append(String.format("  %s: %+d (%s)\n",
                            contrib.sourceDisplayName(), contrib.contribution(), contrib.ruleType()));
                }
            }

            // Modifier breakdown
            if (!breakdown.entries().isEmpty()) {
                sb.append("\n▸ Modifiers\n");
                for (var entry : breakdown.entries()) {
                    String typeColor = switch (entry.modifierType()) {
                        case FLAT -> "";
                        case INCREASED -> "";
                        case MORE -> "";
                        case CAP -> "";
                    };
                    sb.append(String.format("  %s[%s] %s: %+d from '%s'\n",
                            typeColor,
                            entry.modifierType().name(),
                            entry.sourceType().name(),
                            entry.value(),
                            entry.sourceId()));
                }
            }

            // Stacking phases
            sb.append("\n▸ Stacking Calculation\n");
            sb.append(String.format("  After Flat: %d\n", breakdown.afterFlat()));
            sb.append(String.format("  After Increased: %d\n", breakdown.afterIncreased()));
            sb.append(String.format("  After More: %d\n", breakdown.afterMore()));
            sb.append(String.format("  After Cap: %d\n", breakdown.afterCap()));
        }

        // Final value - use StatAccessor for unified access
        int finalValue = StatAccessor.getStatValueInt(store, ref, index);
        sb.append("\n▸ Final Value\n");
        sb.append(String.format("  %d\n", finalValue));

        context.sendMessage(Message.raw(sb.toString()));
    }

    /**
     * Format a stat ID into a display-friendly name.
     */
    @Nonnull
    private String formatStatName(@Nonnull StatId statId) {
        // Convert "max-health-flat" to "Max Health Flat"
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
