package reign.software.hyforged.combat.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.combat.log.CombatEncounter;
import reign.software.hyforged.combat.log.CombatEvent;
import reign.software.hyforged.combat.log.CombatLogService;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Command to display the player's combat log.
 * <p>
 * Usage: {@code /hyforged combatlog}
 * <p>
 * Shows the last 5 combat encounters with per-attack breakdowns.
 */
public class CombatLogCommand extends AbstractPlayerCommand {

    private static final Message MESSAGE_NO_UUID = Message.raw("Could not find player UUID.");
    private static final Message MESSAGE_NO_LOG = Message.raw("No combat history found.");
    private static final Message MESSAGE_HEADER = Message.raw("=== Combat Log ===");
    
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public CombatLogCommand() {
        super("combatlog", "hyforged.commands.combatlog.desc");
        this.addAliases("clog", "combat");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        // Get player UUID
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            context.sendMessage(MESSAGE_NO_UUID);
            return;
        }
        
        UUID playerUuid = uuidComponent.getUuid();
        
        // Get recent encounters
        List<CombatEncounter> encounters = CombatLogService.get().getRecentEncounters(playerUuid);
        if (encounters.isEmpty()) {
            context.sendMessage(MESSAGE_NO_LOG);
            return;
        }
        
        // Display header
        context.sendMessage(MESSAGE_HEADER);
        
        // Display each encounter (most recent first)
        int encounterNum = 1;
        for (int i = encounters.size() - 1; i >= 0; i--) {
            CombatEncounter encounter = encounters.get(i);
            displayEncounter(context, encounter, encounterNum++);
        }
    }
    
    private void displayEncounter(@Nonnull CommandContext context, @Nonnull CombatEncounter encounter, int num) {
        List<CombatEvent> events = encounter.getEvents();
        if (events.isEmpty()) {
            return;
        }
        
        // Encounter header
        String startTime = TIME_FORMAT.format(new Date(encounter.getStartTime()));
        long durationMs = encounter.getDuration();
        float durationSec = durationMs / 1000.0f;
        
        // Calculate total damage from all events
        float totalDamage = 0;
        for (CombatEvent event : events) {
            totalDamage += event.finalDamage();
        }
        
        context.sendMessage(Message.raw(String.format(
            "--- Encounter #%d [%s] %.1fs, %.0f damage ---",
            num, startTime, durationSec, totalDamage
        )));
        
        // Show summary stats
        int hits = 0;
        int crits = 0;
        int blocks = 0;
        int misses = 0;
        
        for (CombatEvent event : events) {
            if (event.missed()) {
                misses++;
            } else {
                hits++;
                if (event.criticalHit()) crits++;
                if (event.blocked() || event.autoBlocked()) blocks++;
            }
        }
        
        context.sendMessage(Message.raw(String.format(
            "  Hits: %d | Crits: %d | Blocks: %d | Misses: %d",
            hits, crits, blocks, misses
        )));
        
        // Show last 5 events of this encounter
        int eventsToShow = Math.min(5, events.size());
        int startIdx = events.size() - eventsToShow;
        
        if (eventsToShow > 0) {
            context.sendMessage(Message.raw("  Recent attacks:"));
        }
        
        for (int i = startIdx; i < events.size(); i++) {
            CombatEvent event = events.get(i);
            displayEvent(context, event);
        }
        
        if (events.size() > 5) {
            context.sendMessage(Message.raw(String.format(
                "  ... and %d more attacks",
                events.size() - 5
            )));
        }
    }
    
    private void displayEvent(@Nonnull CommandContext context, @Nonnull CombatEvent event) {
        StringBuilder sb = new StringBuilder("    ");
        
        // Time
        String time = TIME_FORMAT.format(new Date(event.timestamp()));
        sb.append("[").append(time).append("] ");
        
        // Source
        String attackerName = event.attackerName() != null ? event.attackerName() : "Unknown";
        sb.append("").append(attackerName);
        sb.append(" → ");
        
        // Target
        String defenderName = event.defenderName() != null ? event.defenderName() : "Unknown";
        sb.append("").append(defenderName);
        sb.append(": ");
        
        // Damage info
        if (event.missed()) {
            sb.append("MISS");
        } else {
            // Damage amount with modifiers
            if (event.criticalHit()) {
                sb.append("✦ ");
            } else if (event.blocked() || event.autoBlocked()) {
                sb.append("⛨ ");
            }
            
            sb.append("").append((int) event.finalDamage());
            
            // Show base damage if different
            if (Math.abs(event.baseDamage() - event.finalDamage()) > 0.5f) {
                sb.append(" (").append((int) event.baseDamage()).append(")");
            }
            
            // Damage type
            String damageType = event.damageCauseId();
            if (damageType != null) {
                // Extract just the last part of the ID for brevity
                int colonIdx = damageType.lastIndexOf(':');
                if (colonIdx >= 0) {
                    damageType = damageType.substring(colonIdx + 1);
                }
                sb.append(" [").append(damageType).append("]");
            }
        }
        
        context.sendMessage(Message.raw(sb.toString()));
    }
}
