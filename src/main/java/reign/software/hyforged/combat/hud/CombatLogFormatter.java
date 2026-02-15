package reign.software.hyforged.combat.hud;

import com.hypixel.hytale.server.core.Message;
import reign.software.hyforged.combat.log.CombatEvent;

import javax.annotation.Nonnull;

/**
 * Formats {@link CombatEvent} instances into rich-text {@link Message} objects
 * for the combat log HUD, with colored player names, creature names, and damage.
 */
public final class CombatLogFormatter {

    // Colors for entity names
    private static final String COLOR_PLAYER = "#55FF55";    // Green for player names
    private static final String COLOR_CREATURE = "#FF6666";  // Red for creature/NPC names
    
    // Colors for damage values
    private static final String COLOR_DAMAGE_PHYSICAL = "#FFFFFF";
    private static final String COLOR_DAMAGE_FIRE = "#FF4400";
    private static final String COLOR_DAMAGE_ICE = "#66CCFF";
    private static final String COLOR_DAMAGE_LIGHTNING = "#FFEE00";
    private static final String COLOR_DAMAGE_POISON = "#44DD44";
    private static final String COLOR_DAMAGE_ARCANE = "#CC66FF";
    
    // Colors for special events
    private static final String COLOR_CRIT = "#FF4444";      // Red for crits
    private static final String COLOR_BLOCK = "#FFB347";     // Gold for blocks
    private static final String COLOR_MISS = "#888888";      // Gray for misses
    private static final String COLOR_ARROW = "#AAAAAA";     // Gray for arrow separator
    private static final String COLOR_DAMAGE_TYPE = "#BBBBBB"; // Light gray for damage type label

    private CombatLogFormatter() {}

    /**
     * Format a combat event as a rich-text {@link Message} with colored segments.
     */
    @Nonnull
    public static Message formatEventMessage(@Nonnull CombatEvent event) {
        if (event.missed()) {
            return formatMiss(event);
        }
        if (event.blocked() || event.autoBlocked()) {
            return formatBlock(event);
        }
        return formatDamage(event);
    }

    /**
     * Format a combat event as a plain string (legacy fallback).
     */
    @Nonnull
    public static String formatEvent(@Nonnull CombatEvent event) {
        return formatEventPlain(event);
    }

    @Nonnull
    private static Message formatMiss(@Nonnull CombatEvent event) {
        Message root = Message.raw("");
        root.insert(coloredName(event.attackerName(), event.attackerUuid() != null));
        root.insert(Message.raw("'s attack missed ").color(COLOR_MISS));
        root.insert(coloredName(event.defenderName(), false));
        return root;
    }

    @Nonnull
    private static Message formatBlock(@Nonnull CombatEvent event) {
        Message root = Message.raw("");
        if (event.autoBlocked()) {
            root.insert(Message.raw("\u26E8 ").color(COLOR_BLOCK));
        }
        root.insert(coloredName(event.defenderName(), false));
        root.insert(Message.raw(" blocked ").color(COLOR_BLOCK));
        root.insert(coloredName(event.attackerName(), event.attackerUuid() != null));
        if (event.finalDamage() > 0) {
            String dmgColor = getDamageColor(event.damageCauseId());
            root.insert(Message.raw(" (" + Math.round(event.finalDamage()) + ")").color(dmgColor));
        }
        return root;
    }

    @Nonnull
    private static Message formatDamage(@Nonnull CombatEvent event) {
        Message root = Message.raw("");
        String dmgColor = getDamageColor(event.damageCauseId());

        // Crit prefix
        if (event.criticalHit()) {
            root.insert(Message.raw("\u2726 ").color(COLOR_CRIT));
        }

        // Attacker name (green = player, red = creature)
        boolean attackerIsPlayer = isPlayerName(event);
        root.insert(coloredName(event.attackerName(), attackerIsPlayer));

        // Arrow separator
        root.insert(Message.raw(" \u2192 ").color(COLOR_ARROW));

        // Defender name (opposite color)
        root.insert(coloredName(event.defenderName(), !attackerIsPlayer));

        // Damage value
        root.insert(Message.raw(": ").color(COLOR_ARROW));
        if (event.criticalHit()) {
            root.insert(Message.raw(String.valueOf(Math.round(event.finalDamage()))).color(COLOR_CRIT));
        } else {
            root.insert(Message.raw(String.valueOf(Math.round(event.finalDamage()))).color(dmgColor));
        }

        // Damage type
        root.insert(Message.raw(" " + formatDamageType(event.damageCauseId())).color(COLOR_DAMAGE_TYPE));

        return root;
    }

    /**
     * Returns a colored Message for an entity name.
     * Players are green, creatures/NPCs are red.
     */
    @Nonnull
    private static Message coloredName(@Nonnull String name, boolean isPlayer) {
        String display = truncateName(name);
        return Message.raw(display).color(isPlayer ? COLOR_PLAYER : COLOR_CREATURE);
    }

    /**
     * Heuristic: if the attacker has a UUID recorded, check if the defender also
     * has one. For this formatter, we pass in whether the entity is a player.
     */
    private static boolean isPlayerName(@Nonnull CombatEvent event) {
        // The attacker UUID being present means it's an entity source.
        // We distinguish players from NPCs based on naming patterns - 
        // but really the CombatLogHudSystem has the context. For the formatter,
        // we use a simple heuristic: assume attacker is the player if they have a UUID.
        return event.attackerUuid() != null;
    }

    @Nonnull
    private static String getDamageColor(@Nonnull String damageCauseId) {
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

    @Nonnull
    private static String formatDamageType(@Nonnull String damageCauseId) {
        String name = damageCauseId;
        int colonIdx = name.lastIndexOf(':');
        if (colonIdx >= 0 && colonIdx < name.length() - 1) {
            name = name.substring(colonIdx + 1);
        }
        if (!name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return "(" + name + ")";
    }

    @Nonnull
    private static String truncateName(@Nonnull String name) {
        if (name == null) return "???";
        if (name.length() <= 16) return name;
        return name.substring(0, 15) + "\u2026";
    }

    /**
     * Plain-text fallback for legacy use.
     */
    @Nonnull
    private static String formatEventPlain(@Nonnull CombatEvent event) {
        StringBuilder sb = new StringBuilder();

        if (event.missed()) {
            sb.append(truncateName(event.attackerName()));
            sb.append("'s attack missed ");
            sb.append(truncateName(event.defenderName()));
            return sb.toString();
        }

        if (event.blocked() || event.autoBlocked()) {
            if (event.autoBlocked()) {
                sb.append("\u26E8 ");
            }
            sb.append(truncateName(event.defenderName()));
            sb.append(" blocked ");
            sb.append(truncateName(event.attackerName()));
            if (event.finalDamage() > 0) {
                sb.append(" (").append(Math.round(event.finalDamage())).append(")");
            }
            return sb.toString();
        }

        if (event.criticalHit()) {
            sb.append("\u2726 ");
        }
        sb.append(truncateName(event.attackerName()));
        sb.append(" \u2192 ");
        sb.append(truncateName(event.defenderName()));
        sb.append(": ");
        sb.append(Math.round(event.finalDamage()));
        sb.append(" ").append(formatDamageType(event.damageCauseId()));

        return sb.toString();
    }
}
