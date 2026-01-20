# Requirements: Combat System

## Vision
- Provide an extensible, stat-driven combat layer that supports ARPG mechanics (crit, dodge, block, elemental resistances) while remaining performant and server-authoritative.

## Goals
- Integration with stats
  - Damage, healing, mitigation, and status effects are computed from effective stats (ability scores + derived stats + modifiers).
  - All combat math consumes the Stats System as the source of truth.
  - Combat mechanics are abstract enough to be extended/modified via stats and config rather than hard-coded.
- Core mechanics support
  - Critical hits (chance + multiplier).
  - Dodge/evade (chance-based mitigation).
  - Block (chance-based mitigation and/or flat reduction).
  - Elemental resistances (per element) and caps.
  - Damage types and tags (physical/elemental/other) for scaling and reduction.
- Monster scaling by world progression
  - Monsters scale based on region difficulty derived from distance from spawn.
  - The scaling function is configurable and supports:
    - A smooth curve (no abrupt difficulty spikes)
    - Minimum/maximum level bounds
    - Region-based overrides (e.g., dungeons, events)
  - Monster level influences:
    - Base stats
    - XP reward
    - Loot rarity/affix generation weighting
- Server authority and anti-cheat
  - The server is authoritative for hit resolution, damage outcomes, and drops.
  - Client-reported values (UI, predicted hits) never override server combat results.
- Performance
  - Combat resolution is bounded and efficient per event.
  - Avoids O(n) scans across unrelated modifiers or entities on each hit.
  - Supports high concurrency (many mobs + players) without tick degradation.
- Observability and debugging
  - Provide optional detailed combat logs for administrators (rate-limited) to debug balance and issues.
  - Support reproducible combat calculation traces for a single event (inputs → outputs).

## Non-Goals
- Full reimplementation of the base game combat engine; this layers a stat-driven model on top of server events where possible.
- Perfect client-side prediction; correctness is prioritized over prediction.

## Quality Attributes
- Deterministic: same inputs yield same outputs.
- Extensible: new mechanics can be modeled as stats/modifiers.
- Performant: minimal allocations and bounded per-hit work.
- Configurable: region scaling and mechanic toggles can be tuned.

## Feature Index
- Stat-driven combat pipeline
  - Inputs: attacker/defender effective stats + tags
  - Outputs: damage/healing + applied effects
- Mechanics
  - Crit
  - Dodge
  - Block
  - Resistances and caps
  - Damage types/tags
- Monster scaling
  - Distance-to-level curve
  - Overrides for special regions
  - Hooks into XP/loot
- Debugging
  - Admin trace/log options

## Change Log
- 2026-01-19: Initial version drafted.
