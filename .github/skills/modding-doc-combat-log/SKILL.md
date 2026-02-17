---
name: modding-doc-combat-log
description: Hyforged combat log system for recording, formatting, and displaying combat events. Use when working with CombatEvent, CombatLogService, CombatLogFormatter, CombatLogHudSystem, PlayerDeathCombatLogSystem, HyforgedCombatLogSystem, or the /combatlog command. Triggers - combat log, CombatEvent, CombatLogService, CombatEncounter, CombatLogFormatter, CombatLogHudSystem, HyforgedCombatLogSystem, PlayerDeathCombatLogSystem, CombatLogCommand, combat HUD, extra lines, death log, damage log, DPS display.
---

# Modding Doc: Combat Log

Records, formats, and displays per-player combat events in real-time on the HUD, plus a `/hyforged combatlog` command for history review.

## Architecture / Data Flow

```
Damage event fires (ECS pipeline)
    |
    v
HyforgedCombatLogSystem (DamageEventSystem in inspectDamageGroup)
    |- reads final damage state, quality, crit, block, miss
    |- builds CombatEvent via builder
    v
CombatLogService (singleton, thread-safe)
    |- stores events per-player in CombatEncounter objects
    |- auto-groups events within 10s timeout
    v
CombatLogHudSystem (DelayedEntitySystem, 200ms tick)
    |- polls CombatLogService for recent events
    |- formats via CombatLogFormatter.formatEventMessage()
    |- appends extra lines (XP, death messages)
    |- pushes rich-text to HyforgedHud
```

Additional entry points:
- `CombatServiceImpl.recordToCombatLogImmediate()` — programmatic damage (non-ECS path)
- `PlayerDeathCombatLogSystem` — adds death lines via `CombatLogHudSystem.addExtraLine()`
- `XPAwardSystem` / `HyforgedPlugin` — adds XP gain lines via `addExtraLine()`

## Key Files

| File | Package | Purpose |
|------|---------|---------|
| `CombatEvent.java` | `combat.log` | Immutable record — one damage event |
| `CombatEncounter.java` | `combat.log` | Groups events within 10s timeout |
| `CombatLogService.java` | `combat.log` | Singleton storage per-player |
| `HyforgedCombatLogSystem.java` | `combat.log` | DamageEventSystem — captures events from ECS pipeline |
| `CombatLogFormatter.java` | `combat.hud` | Static utility — formats events to rich-text Messages |
| `CombatLogHudSystem.java` | `combat.hud` | DelayedEntitySystem — updates HUD every 200ms |
| `PlayerDeathCombatLogSystem.java` | `combat.hud` | OnDeathSystem — adds death entries |
| `CombatLogCommand.java` | `combat.command` | `/hyforged combatlog` chat command |
| `CombatMeta.java` | `combat` | MetaKey constants read by the log system |

## CombatEvent Record Fields

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `timestamp` | `long` | no | Server time in ms |
| `attackerUuid` | `UUID` | yes | Null for environmental damage |
| `defenderUuid` | `UUID` | no | Always present |
| `attackerName` | `String` | yes | Display name |
| `defenderName` | `String` | no | Display name |
| `damageCauseId` | `String` | no | e.g. "Physical", "Fire"; default "Unknown" |
| `baseDamage` | `float` | no | Pre-reduction |
| `finalDamage` | `float` | no | After all modifications |
| `missed` | `boolean` | no | Attack missed |
| `blocked` | `boolean` | no | Manual block |
| `autoBlocked` | `boolean` | no | Auto-block triggered |
| `criticalHit` | `boolean` | no | Was a crit |
| `critMultiplierBps` | `int` | no | Basis points (e.g. 15000 = 150%) |
| `resistanceAppliedBps` | `int` | no | Resistance in bps |
| `penetrationAppliedBps` | `int` | no | Penetration in bps |
| `attackerQuality` | `String` | yes | NPC quality tier (e.g. "rare", "epic"), null for players |
| `defenderQuality` | `String` | yes | NPC quality tier, null for players |

## CombatLogService API

Singleton via `CombatLogService.get()`. Thread-safe (`ConcurrentHashMap` + synchronized inner class).

| Method | Description |
|--------|-------------|
| `recordEvent(UUID playerUuid, CombatEvent event)` | Add event to current encounter or start new one |
| `getRecentEncounters(UUID playerUuid)` | Returns encounters (newest first), defensive copy |
| `getCurrentEncounter(UUID playerUuid)` | Active encounter, or null if timed out |
| `getLastEncounter(UUID playerUuid)` | First ended encounter |
| `clearLog(UUID playerUuid)` | Remove player data |
| `onPlayerDisconnect(UUID playerUuid)` | Alias for clearLog |
| `clearAll()` | Remove everything |

**Constants:** `MAX_ENCOUNTERS_PER_PLAYER = 5`

## CombatEncounter API

| Method | Description |
|--------|-------------|
| `addEvent(CombatEvent)` | Adds event; false if ended or full |
| `isTimedOut(long currentTime)` | True if gap > 10s since last event |
| `end()` / `isEnded()` | Mark/check ended state |
| `getEvents()` | Unmodifiable list |
| `getDuration()` | `lastEventTime - startTime` in ms |
| `getTotalDamageToDefender(UUID)` | Sum finalDamage for specific defender |
| `getTotalDamageByAttacker(UUID)` | Sum finalDamage for specific attacker |
| `getCritCount()` / `getMissCount()` / `getBlockCount()` | Stat counters |

**Constants:** `ENCOUNTER_TIMEOUT_MS = 10_000`, `MAX_EVENTS_PER_ENCOUNTER = 1000`

## CombatLogFormatter

Static utility. All user-facing text uses ASCII only (no Unicode).

### Color Constants

| Constant | Hex | Usage |
|----------|-----|-------|
| `COLOR_PLAYER` | `#55FF55` | Green for player names |
| `COLOR_CREATURE` | `#FF6666` | Red for NPC names |
| `COLOR_CRIT` | `#FF4444` | Critical hit text |
| `COLOR_BLOCK` | `#FFB347` | Block text (gold) |
| `COLOR_MISS` | `#888888` | Miss text (gray) |
| `COLOR_ARROW` | `#AAAAAA` | Separator (gray) |
| `COLOR_DAMAGE_TYPE` | `#BBBBBB` | Damage type label |
| `COLOR_DAMAGE_PHYSICAL` | `#FFFFFF` | Physical |
| `COLOR_DAMAGE_FIRE` | `#FF4400` | Fire/burn |
| `COLOR_DAMAGE_ICE` | `#66CCFF` | Ice/frost/cold |
| `COLOR_DAMAGE_LIGHTNING` | `#FFEE00` | Lightning/electric/shock |
| `COLOR_DAMAGE_POISON` | `#44DD44` | Poison/toxic/venom |
| `COLOR_DAMAGE_ARCANE` | `#CC66FF` | Arcane/magic/chaos |

### Message Formats

- **Normal damage:** `CRIT {AttackerName} => {DefenderName}: {dmg} (DamageType)`
- **Block:** `BLOCK {DefenderName} blocked {AttackerName} (dmg)`
- **Miss:** `{AttackerName}'s attack missed {DefenderName}`
- **Death:** `YOU DIED -- [Quality] KillerName Lv.X (N damage)`

NPC names with quality show as `[Epic] Eye Void` with tier-colored bracket tag. Quality colors resolved from `ItemQuality.getAssetMap().getAsset(qualityId).getTextColor()`.

### Key Methods

| Method | Description |
|--------|-------------|
| `formatEventMessage(CombatEvent)` | Rich-text Message for HUD |
| `formatEvent(CombatEvent)` | Plain-text string fallback |
| `coloredNameWithQuality(name, isPlayer, quality)` | Name with optional quality tag |
| `displayName(name)` | Replaces underscores with spaces |
| `getDamageColor(damageCauseId)` | Substring matching for element colors |

## CombatLogHudSystem

`DelayedEntitySystem<EntityStore>`, ticks every 200ms. Query: `Query.and(Player, PlayerRef, UUIDComponent)`.

### Constants

- `UPDATE_INTERVAL_SEC = 0.2f`
- `MAX_DISPLAY_EVENTS = 12`
- `MAX_EXTRA_LINES = 8`

### Static API

| Method | Description |
|--------|-------------|
| `addExtraLine(UUID, Message)` | Add non-combat line (XP, death) |
| `isHudVisible(UUID)` | Check visibility (default true) |
| `setHudVisible(UUID, boolean)` | Set visibility |
| `toggleHudVisibility(UUID)` | Toggle, returns new state |
| `clearPlayerState(UUID)` | Cleanup on disconnect |

### tick() Flow

1. Skip if HUD invisible
2. Gather recent events across encounters (sorted by timestamp)
3. Dirty check (skip if unchanged)
4. Calculate DPS from current encounter
5. Format events via `CombatLogFormatter.formatEventMessage()`
6. Append extra lines
7. Trim to `MAX_DISPLAY_EVENTS`
8. Push to HUD: `hud.updateCombatLog(lines, dps, hits, crits)`

## HyforgedCombatLogSystem

`DamageEventSystem` in `inspectDamageGroup`. Runs after `HyforgedCriticalHitSystem`, before `DamageSystems.EntityUIEvents`.

### Constructor

```java
new HyforgedCombatLogSystem(npcQualityComponentType)
```

Takes `ComponentType<EntityStore, HyforgedNPCQualityComponent>` for quality resolution.

### handle() reads these meta keys

| MetaKey | Source System |
|---------|---------------|
| `CombatMeta.BASE_DAMAGE` | First pipeline system |
| `CombatMeta.RESISTANCE_BPS` | HyforgedDamageReductionSystem |
| `CombatMeta.PENETRATION_BPS` | HyforgedDamageReductionSystem |
| `HyforgedHitResolutionSystem.MISS` | Hit resolution |
| `Damage.BLOCKED` | Block system |
| `HyforgedAutoBlockSystem.AUTO_BLOCKED` | Auto-block |
| `HyforgedCriticalHitSystem.CRITICAL_HIT` | Crit system |
| `HyforgedCriticalHitSystem.CRITICAL_MULTIPLIER` | Crit system |

### Entity Name Resolution Order

1. `Player.getDisplayName()` (player name)
2. `DisplayNameComponent.getDisplayName().getRawText()` (clean base name)
3. `NPCEntity.getRoleName()` (NPC role)
4. `Nameplate.getText()` with `stripNameplateDecorations()` (fallback)
5. UUID substring (8 chars)
6. `"Unknown"`

## PlayerDeathCombatLogSystem

`DeathSystems.OnDeathSystem` (RefChangeSystem). Query: `Query.and(DeathComponent, Player)`.

Death line formats:
- Entity killer: `YOU DIED -- [Quality] KillerName Lv.X (N damage)` (quality suppressed for Common)
- Environment: `YOU DIED -- Environment (N damage)`

Adds line via `CombatLogHudSystem.addExtraLine()`.

## Plugin Registration

```java
// In HyforgedPlugin.start():
entityStoreRegistry.registerSystem(new HyforgedCombatLogSystem(npcQualityComponentType));
entityStoreRegistry.registerSystem(new CombatLogHudSystem());
entityStoreRegistry.registerSystem(new PlayerDeathCombatLogSystem());

// Cleanup on disconnect:
CombatLogService.get().onPlayerDisconnect(uuid);
CombatLogHudSystem.clearPlayerState(uuid);
```

## HUD Integration

HyforgedHud UI element IDs:
- `#CombatLog.Visible` — show/hide the combat log panel
- `#LogEntry0` through `#LogEntry11` — `.TextSpans` for rich text, `.Text` for clear
- `#DpsLabel.Text`, `#HitsLabel.Text`, `#CritsLabel.Text` — summary stats

## Common Patterns

### Recording a custom combat event

```java
CombatEvent event = CombatEvent.builder()
    .timestamp(System.currentTimeMillis())
    .attackerUuid(attackerUuid)
    .defenderUuid(defenderUuid)
    .attackerName("Skeleton")
    .defenderName("Pandaros")
    .damageCauseId("Physical")
    .baseDamage(100f)
    .finalDamage(75f)
    .criticalHit(true)
    .critMultiplierBps(15000)
    .attackerQuality("rare")
    .build();
CombatLogService.get().recordEvent(playerUuid, event);
```

### Adding extra lines to the HUD

```java
Message xpLine = Message.raw("+500 XP").color("#55AAFF");
CombatLogHudSystem.addExtraLine(playerUuid, xpLine);
```

### Toggling HUD visibility

```java
boolean newState = CombatLogHudSystem.toggleHudVisibility(playerUuid);
```

### Querying combat history

```java
List<CombatEncounter> encounters = CombatLogService.get().getRecentEncounters(playerUuid);
CombatEncounter current = CombatLogService.get().getCurrentEncounter(playerUuid);
float dps = current != null ? current.getTotalDamageByAttacker(playerUuid) / (current.getDuration() / 1000f) : 0;
```

### Cleanup on disconnect

```java
CombatLogService.get().onPlayerDisconnect(uuid);
CombatLogHudSystem.clearPlayerState(uuid);
```

## Important Rules

- **No Unicode in user-facing text.** Use ASCII alternatives (e.g., `CRIT` not `✦`, `=>` not `→`, `--` not `∞`).
- Quality colors are resolved dynamically from `ItemQuality.getAssetMap()` — do not hard-code tier colors.
- Common quality is suppressed in display (not shown on nameplates, death lines, or combat log).
- `displayName()` replaces underscores with spaces — all entity names go through this.
- Use `CombatEvent.builder()` — never construct the record directly.
- Record events for both attacker and defender when both are players.
- Extra lines are capped at `MAX_EXTRA_LINES = 8` per player.
- Encounters auto-group within 10s; max 5 encounters retained per player.

## Test Coverage

- `CombatEventTest` — Builder, defaults, miss/crit events, equality
- `CombatLogServiceTest` — Singleton, encounter grouping, timeout splits, limits, cleanup
- `CombatEncounterTest` — Event tracking, timeout logic, statistics, defensive copies
