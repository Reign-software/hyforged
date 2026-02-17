# Combat System

The Hyforged Combat System extends Hytale's damage pipeline with ARPG-style mechanics including hit resolution, auto-blocking, multi-element damage, critical hits, and ailment effects.

## Features

### Hit Resolution
- Accuracy vs Evasion check before damage
- Level difference affects hit chance
- Configurable via `accuracy-rating` and `evasion-chance-bps` stats

### Auto-Block
- Chance-based passive blocking without manual input
- Consumes reduced stamina compared to manual block
- Configurable via `block-chance-bps` and `block-mitigation-bps` stats

### Multi-Element Damage
- Each attack can deal multiple damage types
- Per-element resistance and penetration
- Inheritance system for damage type variants

### Critical Hits
- Crit chance with level-based penalties vs higher-level targets
- Configurable multiplier via `crit-multiplier-bps`
- Visual/audio hooks for crit feedback

### Ailment System
- Threshold-based status effect triggering
- Per-element accumulation windows
- Duration scaling via `effect-duration-bps`
- Data-driven ailment definitions

### Combat Logging
- Per-player combat history
- Encounter grouping with timeout detection
- Detailed per-attack breakdown
- API for UI consumption

### Healing System
- Multiplicative healing formula with three modifiers
- Healer effectiveness stat (outgoing healing bonus)
- Target healing received stat (incoming healing bonus)
- Target recovery rate stat (regeneration bonus)
- HealingSpec/HealingResult API for programmatic healing
- No resistance, penetration, or crit mechanics (clean bypasses)

### Debug Mode
- Runtime toggle for verbose combat calculations
- Per-stage logging (hit, block, damage, crit, heal)
- Balance testing and debugging support
- Thread-safe configuration via CombatConfig

## Combat Pipeline Order

```
1. Gather Damage
   └─ HyforgedHitResolutionSystem (accuracy vs evasion)

2. Filter Damage
   ├─ HyforgedAutoBlockSystem (block chance, stamina)
   └─ HyforgedDamageReductionSystem (resistance, penetration)

3. Apply Damage
   └─ DamageSystems.ApplyDamage (health subtraction)

4. Inspect Damage
   ├─ HyforgedCriticalHitSystem (crit multiplier)
   ├─ HyforgedCombatLogSystem (event recording)
   └─ HyforgedAilmentSystem (threshold accumulation)
```

## Configuration

### Damage Type Extensions

Define resistance and penetration mappings in `Server/Hyforged/Stats/Damage/`:

```json
{
  "Id": "Fire",
  "HyforgedResistanceStat": "hyforged:fire-resistance-bps",
  "HyforgedPenetrationStat": "hyforged:fire-penetration-bps",
  "HyforgedElementTag": "fire"
}
```

### Ailment Definitions

Define ailments in `Server/Hyforged/Combat/Ailments/`:

```json
{
  "Id": "hyforged:fire-ailment",
  "ElementTag": "fire",
  "EntityEffectId": "Burn",
  "BaseThreshold": 100,
  "AccumulationWindowMs": 5000,
  "BaseDurationSeconds": 4.0,
  "DisplayName": "Ignite",
  "Description": "Deals fire damage over time."
}
```

## Stats Reference

### Offensive Stats
| Stat ID | Description |
|---------|-------------|
| `accuracy-rating` | Increases hit chance |
| `crit-chance-bps` | Chance to critically hit |
| `crit-multiplier-bps` | Bonus damage on crit (1500 = 15%) |
| `effect-duration-bps` | Ailment duration scaling |
| `*-penetration-bps` | Reduces target's resistance |

### Defensive Stats
| Stat ID | Description |
|---------|-------------|
| `evasion-chance-bps` | Chance to evade attacks |
| `block-chance-bps` | Auto-block chance |
| `block-mitigation-bps` | Damage reduction on block |
| `*-resistance-bps` | Reduces incoming damage |

### Healing Stats
| Stat ID | Description |
|---------|-------------|
| `healing-effectiveness-bps` | Outgoing healing bonus (10000 = 100%) |
| `healing-received-bps` | Incoming healing bonus |
| `life-recovery-rate-bps` | Regeneration multiplier |

## Debug Mode

Enable combat debug mode for balance testing and debugging:

```java
// Enable debug logging
CombatConfig.setDebugEnabled(true);

// Enable extra-verbose logging
CombatConfig.setVerboseEnabled(true);
```

When enabled, logs detailed calculation breakdowns:
```
[COMBAT DEBUG] HIT CHECK: acc=80 eva=20 -> hitChance=85% roll=4000 -> HIT
[COMBAT DEBUG] BLOCK CHECK: chance=30% stamina=80.0 cost=15.0 -> NOT BLOCKED
[COMBAT DEBUG] DAMAGE [physical]: base=100.00 res=25% pen=10% -> final=85.00
[COMBAT DEBUG] CRIT CHECK: chance=15% roll=800 -> CRITICAL x1.50
[COMBAT DEBUG] HEAL: base=50.00 eff=110% recv=105% recov=102% -> final=58.91
```

## Combat UI

### Combat Text

The `HyforgedCombatTextSystem` displays enhanced combat feedback:

| Indicator | Display | Meaning |
|-----------|---------|---------|
| Critical Hit | §c✦ 150 | Red text with sparkle |
| Blocked | §6⛨ 50 (Blocked) | Gold text with shield |
| Miss | §7Miss | Gray "Miss" text |
| Normal Hit | §f100 | White damage number |

Combat text automatically replaces Hytale's default damage numbers when hits, crits, blocks, or misses occur.

### Combat Log Command

View your combat history with `/hyforged combatlog` (aliases: `clog`, `combat`).

Shows:
- Last 5 encounters with timestamps and duration
- Hit/crit/block/miss statistics per encounter
- Recent attack details with damage types
- Base vs final damage comparison

Example output:
```
=== Combat Log ===
--- Encounter #1 [14:32:15] 8.5s, 450 damage ---
  Hits: 12 | Crits: 3 | Blocks: 2 | Misses: 1
  Recent attacks:
    [14:32:22] Player → Zombie: ✦ 45 (30) [Physical]
    [14:32:21] Player → Zombie: 28 [Physical]
    [14:32:20] Player → Zombie: ⛨ 15 (Blocked) [Physical]
```

### Combat Log HUD (WoW-style)

A real-time graphical combat log positioned in the bottom-right corner, similar to World of Warcraft.

**Toggle:** `/hyforged combatloghud` (aliases: `cloghud`, `combathud`)

**Features:**
- Displays last 12 combat events in a scrolling list
- Color-coded entries by damage type and result:
  - **Red (§c✦)** - Critical hits
  - **Gold (§6⛨)** - Blocked attacks  
  - **Gray (§7)** - Missed attacks
  - **White** - Physical damage
  - **Red** - Fire damage
  - **Aqua** - Ice/Frost damage
  - **Yellow** - Lightning damage
  - **Green** - Poison damage
  - **Purple** - Arcane/Magic damage
- Real-time footer stats:
  - Current DPS (damage per second)
  - Total hits in encounter
  - Total crits in encounter
- Semi-transparent background for visibility

**Layout:**
```
┌─────────────────────────────────────┐
│ Combat Log                      [x] │
├─────────────────────────────────────┤
│ Player → Zombie: 45 (Physical)      │
│ ✦ Player → Zombie: 78 (Fire)        │
│ Zombie's attack missed Player       │
│ ⛨ Player blocked Skeleton (12)      │
│ Player → Skeleton: 32 (Ice)         │
│ ...                                 │
├─────────────────────────────────────┤
│ DPS: 45.2   Hits: 12   Crits: 3     │
└─────────────────────────────────────┘
```

**Technical Notes:**
- Uses MultipleHUD library to coexist with other HUDs
- Updates every 200ms with dirty checking for performance
- Per-player visibility state (survives across encounters)
- Clears state on player disconnect

## API Usage

See [API.md](API.md) for programmatic combat API documentation.

```java
// Quick example
CombatService combat = CombatService.get();
DamageSpec spec = DamageSpec.of("Physical", 50);
CombatResult result = combat.applyDamage(attacker, defender, spec, commandBuffer);
```

## Files

### Core Classes
- `CombatService` - Main API interface
- `DamageSpec` - Damage specification builder
- `CombatResult` - Combat result with breakdowns
- `CombatMath` - Shared combat formulas

### Systems
- `HyforgedHitResolutionSystem` - Accuracy vs evasion
- `HyforgedAutoBlockSystem` - Passive block chance
- `HyforgedDamageReductionSystem` - Resistance calculation
- `HyforgedCriticalHitSystem` - Crit processing
- `HyforgedCombatTextSystem` - Enhanced damage text display
- `HyforgedCombatLogSystem` - Event recording
- `HyforgedAilmentSystem` - Status effect triggering
- `CombatLogHudSystem` - Real-time combat log HUD management

### Commands
- `/hyforged combatlog` - Display combat history (chat)
- `/hyforged combatloghud` - Toggle graphical combat log HUD

### UI Components
- `CombatLogHud` - WoW-style graphical combat log
- `CombatLogHud.ui` - HUD layout definition

### Services
- `CombatLogService` - Per-player combat history
- `AilmentRegistry` - Ailment definition lookup
