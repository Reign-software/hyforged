# Requirements: Stats System

## Vision
- Provide a unified, extensible character statistics model (ability scores + derived stats) that drives combat and gameplay outcomes, with transparent UI breakdowns and first-class support for modifiers from equipment, buffs, debuffs, passives, and other systems.

## Goals
- Stat taxonomy
  - Define a clear separation between:
    - Ability scores (primary stats): e.g., Strength, Intelligence, Constitution, Dexterity, Wisdom, etc.
    - Derived stats (secondary stats): e.g., attack power, spell power, crit chance, dodge chance, block chance, elemental resistances.
  - Group stats into categories suitable for UI navigation (e.g., Offense, Defense, Utility, Elemental, Resource).
  - Each stat has a canonical identifier, display name, category, and description.
- Extensibility and API
  - The system supports adding new stats at runtime/config time without breaking existing characters.
  - The system exposes a clear API for other plugins/systems to:
    - Register new stats
    - Add/remove modifiers
    - Query effective stat values
    - Subscribe to stat change events (e.g., for UI updates)
  - Modifiers are first-class objects that can be created by equipment, buffs/debuffs, passives, class progression, and enchantments.
- Modifier model
  - Support common modifier types:
    - Flat addition/subtraction
    - Percent increase/decrease
    - “More/Less” multipliers (optional but ARPG-friendly)
    - Caps/clamps (e.g., max resistance)
  - Define deterministic stacking rules and a consistent evaluation order.
  - Modifiers can have metadata: source, duration (if temporary), and visibility (show/hide in UI).
- Player-facing UI
  - Provide a user-friendly interface to view stats.
  - Provide breakdowns per stat showing contributing sources (equipment, buffs, passives, class, level/ability scores).
  - Ensure terminology and grouping are consistent across all UI surfaces.
- Performance and caching
  - Stat computation is performant with many active modifiers.
  - Support caching and invalidation: recompute only when dependencies change.
  - Define bounded worst-case behavior (e.g., maximum modifier count per entity; graceful degradation if exceeded).
- Persistence
  - Persist only the minimal necessary player-owned state (e.g., allocated ability scores, passive selections, class levels) and recompute derived stats on load.
  - Temporary modifiers (e.g., buffs) do not persist across restarts unless explicitly designed to.

## Non-Goals
- A fully general-purpose scripting language for stat math.
- Per-tick continuous recomputation for all entities regardless of changes.
- Hard-coding specific ARPG formulas; formulas should be configurable and iteratable.

## Quality Attributes
- Extensible: new stats and new modifier sources are easy to introduce.
- Explainable: UI breakdowns match server calculations.
- Deterministic: stacking order is documented and stable.
- High-performance: supports large modifier counts and frequent equipment swaps.

## Feature Index
- Stat definitions
  - Ability scores (primary)
  - Derived stats (secondary)
  - Categories and UI grouping
- Modifier framework
  - Types (flat/percent/multipliers/caps)
  - Stacking order rules
  - Visibility and attribution
- API surface (conceptual)
  - Register stat
  - Apply/remove modifier
  - Query stat
  - Events/notifications
- UI/UX
  - Character sheet
  - Tooltip breakdowns
  - Inventory integration
- Performance
  - Caching/invalidation model
  - Upper bounds and profiling expectations

## Change Log
- 2026-01-19: Initial version drafted.
