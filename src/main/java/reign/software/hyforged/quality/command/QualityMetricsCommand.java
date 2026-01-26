package reign.software.hyforged.quality.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import reign.software.hyforged.quality.service.QualityMetrics;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Debug command to display quality roll metrics.
 * <p>
 * Usage: {@code /hyforged quality metrics}
 */
public class QualityMetricsCommand extends CommandBase {

    public QualityMetricsCommand() {
        super("metrics", "hyforged.commands.quality.metrics.desc");
        this.addAliases("qualitymetrics");
        this.requirePermission("hyforged.admin.quality.metrics");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        QualityMetrics metrics = QualityMetrics.get();

        long attempts = metrics.getRollAttempts();
        long successes = metrics.getRollSuccesses();
        long failures = Math.max(0, attempts - successes);
        double successRate = attempts > 0 ? (successes * 100.0 / attempts) : 0.0;

        context.sendMessage(Message.raw("=== Quality Roll Metrics ==="));
        context.sendMessage(Message.raw("Roll Attempts: " + attempts));
        context.sendMessage(Message.raw("Roll Successes: " + successes));
        context.sendMessage(Message.raw("Roll Failures: " + failures));
        context.sendMessage(Message.raw("Success Rate: " + String.format("%.1f%%", successRate)));

        Map<String, Long> byQuality = metrics.getRollsByQuality();
        if (!byQuality.isEmpty()) {
            context.sendMessage(Message.raw("By Quality:"));
            byQuality.forEach((k, v) -> context.sendMessage(Message.raw("  " + k + ": " + v)));
        }

        Map<String, Long> bySource = metrics.getRollsBySourceType();
        if (!bySource.isEmpty()) {
            context.sendMessage(Message.raw("By Source Type:"));
            bySource.forEach((k, v) -> context.sendMessage(Message.raw("  " + k + ": " + v)));
        }
    }
}
