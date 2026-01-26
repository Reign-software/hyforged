package reign.software.hyforged.affix.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import reign.software.hyforged.affix.service.AffixMetrics;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Debug command to display affix system metrics.
 * <p>
 * Usage: {@code /hyforged affixmetrics}
 * <p>
 * Shows roll attempt counts, success rates, tier distribution, etc.
 */
public class AffixMetricsCommand extends CommandBase {
    
    public AffixMetricsCommand() {
        super("affixmetrics", "hyforged.commands.affixmetrics.desc");
        this.addAliases("metrics", "affixstats");
    }
    
    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        AffixMetrics metrics = AffixMetrics.get();
        
        context.sendMessage(Message.raw("=== Affix System Metrics ==="));
        context.sendMessage(Message.raw("Roll Attempts: " + metrics.getRollAttempts()));
        context.sendMessage(Message.raw("Roll Successes: " + metrics.getRollSuccesses()));
        context.sendMessage(Message.raw("Roll Failures: " + metrics.getRollFailures()));
        context.sendMessage(Message.raw("Success Rate: " + String.format("%.1f%%", metrics.getSuccessRate())));
        context.sendMessage(Message.raw("Total Affixes Rolled: " + metrics.getTotalAffixesRolled()));
        context.sendMessage(Message.raw("Avg Affixes/Roll: " + String.format("%.2f", metrics.getAverageAffixesPerRoll())));
        
        Map<String, Long> byQuality = metrics.getRollsByQuality();
        if (!byQuality.isEmpty()) {
            context.sendMessage(Message.raw("By Quality:"));
            byQuality.forEach((k, v) -> 
                    context.sendMessage(Message.raw("  " + k + ": " + v)));
        }
        
        Map<Integer, Long> byTier = metrics.getRollsByTier();
        if (!byTier.isEmpty()) {
            context.sendMessage(Message.raw("By Tier:"));
            byTier.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> context.sendMessage(Message.raw("  T" + e.getKey() + ": " + e.getValue())));
        }
        
        Map<String, Long> byType = metrics.getRollsByType();
        if (!byType.isEmpty()) {
            context.sendMessage(Message.raw("By Type:"));
            byType.forEach((k, v) -> 
                    context.sendMessage(Message.raw("  " + k + ": " + v)));
        }
    }
}
