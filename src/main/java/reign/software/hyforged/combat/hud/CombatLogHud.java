package reign.software.hyforged.combat.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import reign.software.hyforged.combat.log.CombatEvent;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * WoW-style combat log HUD that displays scrolling combat events.
 * <p>
 * Features:
 * <ul>
 *   <li>Shows last 12 combat events in a scrolling list</li>
 *   <li>Color-coded entries by damage type and result</li>
 *   <li>Real-time DPS, hits, and crit counters</li>
 *   <li>Positioned in bottom-right corner</li>
 * </ul>
 */
public class CombatLogHud extends CustomUIHud {

    /** Path to the UI definition file */
    public static final String UI_PATH = "Hyforged/CombatLogHud.ui";
    
    /** Maximum number of log entries to display */
    public static final int MAX_ENTRIES = 12;

    // Color codes for different event types (Minecraft-style formatting)
    private static final String COLOR_DAMAGE_PHYSICAL = "§f";  // White
    private static final String COLOR_DAMAGE_FIRE = "§c";       // Red
    private static final String COLOR_DAMAGE_ICE = "§b";        // Aqua
    private static final String COLOR_DAMAGE_LIGHTNING = "§e";  // Yellow
    private static final String COLOR_DAMAGE_POISON = "§2";     // Dark Green
    private static final String COLOR_DAMAGE_ARCANE = "§d";     // Light Purple
    private static final String COLOR_CRIT = "§c";              // Red (bold in text)
    private static final String COLOR_BLOCK = "§6";             // Gold
    private static final String COLOR_MISS = "§7";              // Gray
    private static final String COLOR_HEAL = "§a";              // Green
    private static final String COLOR_DEFAULT = "§f";           // White

    public CombatLogHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append(UI_PATH);
    }

    /**
     * Update the combat log with current combat data.
     *
     * @param events      List of recent events (newest first, max 12)
     * @param dps         Current DPS value (or -1 if not calculated)
     * @param totalHits   Total hits in current encounter
     * @param totalCrits  Total crits in current encounter
     */
    public void updateLog(
            @Nonnull List<CombatEvent> events,
            float dps,
            int totalHits,
            int totalCrits
    ) {
        UICommandBuilder builder = new UICommandBuilder();

        // Update log entries (reverse order - newest at bottom like WoW)
        for (int i = 0; i < MAX_ENTRIES; i++) {
            String selector = "#LogEntry" + i + ".Text";
            if (i < events.size()) {
                CombatEvent event = events.get(events.size() - 1 - i);
                String formattedEntry = formatEvent(event);
                builder.set(selector, formattedEntry);
            } else {
                builder.set(selector, "");
            }
        }

        // Update footer stats
        String dpsText = dps >= 0 ? String.format("DPS: %.1f", dps) : "DPS: ----";
        builder.set("#DpsLabel.Text", dpsText);
        builder.set("#HitsLabel.Text", "Hits: " + totalHits);
        builder.set("#CritsLabel.Text", "Crits: " + totalCrits);

        update(false, builder);
    }

    /**
     * Clear all log entries.
     */
    public void clearLog() {
        UICommandBuilder builder = new UICommandBuilder();
        for (int i = 0; i < MAX_ENTRIES; i++) {
            builder.set("#LogEntry" + i + ".Text", "");
        }
        builder.set("#DpsLabel.Text", "DPS: ----");
        builder.set("#HitsLabel.Text", "Hits: 0");
        builder.set("#CritsLabel.Text", "Crits: 0");
        update(false, builder);
    }

    /**
     * Format a combat event for display.
     */
    @Nonnull
    private String formatEvent(@Nonnull CombatEvent event) {
        StringBuilder sb = new StringBuilder();

        // Handle miss
        if (event.missed()) {
            sb.append(COLOR_MISS);
            sb.append(truncateName(event.attackerName()));
            sb.append("'s attack missed ");
            sb.append(truncateName(event.defenderName()));
            return sb.toString();
        }

        // Handle block
        if (event.blocked() || event.autoBlocked()) {
            sb.append(COLOR_BLOCK);
            if (event.autoBlocked()) {
                sb.append("⛨ "); // Shield icon for auto-block
            }
            sb.append(truncateName(event.defenderName()));
            sb.append(" blocked ");
            sb.append(truncateName(event.attackerName()));
            if (event.finalDamage() > 0) {
                sb.append(" (");
                sb.append(Math.round(event.finalDamage()));
                sb.append(")");
            }
            return sb.toString();
        }

        // Normal hit or crit
        String damageColor = getDamageColor(event.damageCauseId());
        
        if (event.criticalHit()) {
            sb.append(COLOR_CRIT);
            sb.append("✦ "); // Sparkle for crit
        } else {
            sb.append(damageColor);
        }

        sb.append(truncateName(event.attackerName()));
        sb.append(" → ");
        sb.append(truncateName(event.defenderName()));
        sb.append(": ");
        
        // Damage amount
        if (event.criticalHit()) {
            sb.append("§l"); // Bold for crits
        }
        sb.append(Math.round(event.finalDamage()));
        sb.append("§r"); // Reset formatting
        
        // Damage type
        sb.append(" ");
        sb.append(damageColor);
        sb.append(formatDamageType(event.damageCauseId()));

        return sb.toString();
    }

    /**
     * Get color code for damage type.
     */
    @Nonnull
    private String getDamageColor(@Nonnull String damageCauseId) {
        String lowerCause = damageCauseId.toLowerCase();
        if (lowerCause.contains("fire") || lowerCause.contains("burn")) {
            return COLOR_DAMAGE_FIRE;
        } else if (lowerCause.contains("ice") || lowerCause.contains("frost") || lowerCause.contains("cold")) {
            return COLOR_DAMAGE_ICE;
        } else if (lowerCause.contains("lightning") || lowerCause.contains("electric") || lowerCause.contains("shock")) {
            return COLOR_DAMAGE_LIGHTNING;
        } else if (lowerCause.contains("poison") || lowerCause.contains("toxic") || lowerCause.contains("venom")) {
            return COLOR_DAMAGE_POISON;
        } else if (lowerCause.contains("arcane") || lowerCause.contains("magic") || lowerCause.contains("chaos")) {
            return COLOR_DAMAGE_ARCANE;
        }
        return COLOR_DAMAGE_PHYSICAL;
    }

    /**
     * Format damage type for display (capitalize, remove namespaces).
     */
    @Nonnull
    private String formatDamageType(@Nonnull String damageCauseId) {
        // Remove namespace (e.g., "hyforged:fire" -> "fire")
        String name = damageCauseId;
        int colonIdx = name.lastIndexOf(':');
        if (colonIdx >= 0 && colonIdx < name.length() - 1) {
            name = name.substring(colonIdx + 1);
        }
        // Capitalize first letter
        if (!name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return "(" + name + ")";
    }

    /**
     * Truncate name for display (max 10 chars).
     */
    @Nonnull
    private String truncateName(@Nonnull String name) {
        if (name == null) {
            return "???";
        }
        if (name.length() <= 10) {
            return name;
        }
        return name.substring(0, 9) + "…";
    }
}
