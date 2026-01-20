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
import reign.software.hyforged.stats.CoreStats;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.breakdown.StatBreakdown;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.component.StatModifier;

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

    private static final Message MESSAGE_PLAYER_NOT_FOUND = Message.raw("§cPlayer not found or not in a world.");
    private static final Message MESSAGE_NO_STAT_COMPONENT = Message.raw("§cPlayer does not have a Hyforged stat component.");

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
                showStatBreakdown(context, playerName, statComponent, statIdStr);
            } else {
                // Show summary of all core stats
                showStatSummary(context, playerName, statComponent);
            }
        });
    }

    /**
     * Show a summary of all core stats for the player.
     */
    private void showStatSummary(
            @Nonnull CommandContext context,
            @Nonnull String playerName,
            @Nonnull HyforgedStatComponent component
    ) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();

        StringBuilder sb = new StringBuilder();
        sb.append("§6═══════ Stats for ").append(playerName).append(" ═══════§r\n");

        // Ability Scores
        sb.append("\n§e▸ Ability Scores§r\n");
        appendStatLine(sb, registry, component, CoreStats.STRENGTH);
        appendStatLine(sb, registry, component, CoreStats.DEXTERITY);
        appendStatLine(sb, registry, component, CoreStats.INTELLIGENCE);
        appendStatLine(sb, registry, component, CoreStats.CONSTITUTION);
        appendStatLine(sb, registry, component, CoreStats.WISDOM);
        appendStatLine(sb, registry, component, CoreStats.SPIRIT);
        appendStatLine(sb, registry, component, CoreStats.LUCK);

        // Derived Stats - Offensive
        sb.append("\n§e▸ Offensive§r\n");
        appendStatLine(sb, registry, component, CoreStats.ATTACK_POWER);
        appendStatLine(sb, registry, component, CoreStats.SPELL_POWER);
        appendStatLine(sb, registry, component, CoreStats.CRIT_CHANCE_BPS);
        appendStatLine(sb, registry, component, CoreStats.CRIT_MULTIPLIER_BPS);

        // Derived Stats - Defensive
        sb.append("\n§e▸ Defensive§r\n");
        appendStatLine(sb, registry, component, CoreStats.ARMOR_RATING);
        appendStatLine(sb, registry, component, CoreStats.EVASION_RATING);
        appendStatLine(sb, registry, component, CoreStats.MAX_HEALTH_FLAT);

        // Modifiers summary
        List<StatModifier> modifiers = component.getModifiers();
        sb.append("\n§7Active modifiers: ").append(modifiers.size()).append("§r\n");

        context.sendMessage(Message.raw(sb.toString()));
    }

    /**
     * Append a single stat line to the summary.
     */
    private void appendStatLine(
            @Nonnull StringBuilder sb,
            @Nonnull StatDefinitionRegistry registry,
            @Nonnull HyforgedStatComponent component,
            @Nonnull StatId statId
    ) {
        int index = registry.getIndex(statId);
        if (index < 0) {
            return;
        }

        StatDefinition def = registry.getStat(index);
        if (def == null) {
            return;
        }

        int value = component.getCachedValue(index);
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
            String bonusColor = bonus > 0 ? "§a" : "§c";
            sb.append(String.format("  §f%-20s §7%s %s(%+d)§r\n", displayName, valueStr, bonusColor, bonus));
        } else {
            sb.append(String.format("  §f%-20s §7%s§r\n", displayName, valueStr));
        }
    }

    /**
     * Show detailed breakdown for a specific stat.
     */
    private void showStatBreakdown(
            @Nonnull CommandContext context,
            @Nonnull String playerName,
            @Nonnull HyforgedStatComponent component,
            @Nonnull String statIdStr
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
            context.sendMessage(Message.raw("§cStat not found: " + statIdStr));
            return;
        }

        StatDefinition def = registry.getStat(index);
        if (def == null) {
            context.sendMessage(Message.raw("§cStat not found: " + statIdStr));
            return;
        }

        // Get full breakdown
        StatBreakdown breakdown = component.getStatBreakdown(index, 1);

        StringBuilder sb = new StringBuilder();
        sb.append("§6═══════ ").append(formatStatName(statId)).append(" Breakdown ═══════§r\n");
        sb.append("§7Player: ").append(playerName).append("§r\n\n");

        // Definition info
        sb.append("§e▸ Definition§r\n");
        sb.append(String.format("  §7ID: §f%s§r\n", statId));
        sb.append(String.format("  §7Category: §f%s§r\n", def.category()));
        sb.append(String.format("  §7Is Rating: §f%s§r\n", def.isRating()));
        sb.append(String.format("  §7Bounds: §f[%d, %d]§r\n", def.minValue(), def.maxValue()));

        // Base value
        sb.append("\n§e▸ Base Value§r\n");
        int baseValue = component.getBaseValue(index);
        sb.append(String.format("  §7Allocated/Default: §f%d§r\n", baseValue));

        if (breakdown != null) {
            // Scaling contributions
            if (!breakdown.scalingContributions().isEmpty()) {
                sb.append("\n§e▸ Scaling Contributions§r\n");
                for (var contrib : breakdown.scalingContributions()) {
                    sb.append(String.format("  §7%s: §f%+d §7(%s)§r\n",
                            contrib.sourceDisplayName(), contrib.contribution(), contrib.ruleType()));
                }
            }

            // Modifier breakdown
            if (!breakdown.entries().isEmpty()) {
                sb.append("\n§e▸ Modifiers§r\n");
                for (var entry : breakdown.entries()) {
                    String typeColor = switch (entry.modifierType()) {
                        case FLAT -> "§b";
                        case INCREASED -> "§a";
                        case MORE -> "§d";
                        case CAP -> "§c";
                    };
                    sb.append(String.format("  %s[%s]§r §7%s§r: §f%+d§r from '%s'\n",
                            typeColor,
                            entry.modifierType().name(),
                            entry.sourceType().name(),
                            entry.value(),
                            entry.sourceId()));
                }
            }

            // Stacking phases
            sb.append("\n§e▸ Stacking Calculation§r\n");
            sb.append(String.format("  §7After Flat: §f%d§r\n", breakdown.afterFlat()));
            sb.append(String.format("  §7After Increased: §f%d§r\n", breakdown.afterIncreased()));
            sb.append(String.format("  §7After More: §f%d§r\n", breakdown.afterMore()));
            sb.append(String.format("  §7After Cap: §f%d§r\n", breakdown.afterCap()));
        }

        // Final value
        int finalValue = component.getCachedValue(index);
        sb.append("\n§e▸ Final Value§r\n");
        sb.append(String.format("  §a%d§r\n", finalValue));

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
