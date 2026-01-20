# Requirements: Stats System

## Vision
- Provide a unified, extensible character statistics model (ability scores + derived stats) that drives combat and gameplay outcomes, with transparent UI breakdowns and first-class support for modifiers from equipment, buffs, debuffs, passives, and other systems.

## Goals
- Stat taxonomy
  - All stats (including ability scores) are first-class stats in the registry.
  - Ability scores (e.g., Strength, Dexterity) are stats tagged `attribute` in the `Attributes` category.
  - Derived stats (e.g., attack power, crit chance) are stats that scale from other stats.
  - Group stats into categories suitable for UI navigation (e.g., Attributes, Offense, Defense, Utility, Elemental, Resource).
  - Each stat has a canonical identifier, display name, category, and description.
- Stat tags and dependencies
  - Stats can be assigned one or more tags (e.g., `attribute`, `offense`, `elemental`, `resource`).
  - Tags enable modifiers to target groups of stats (e.g., "+5% to all Attributes" applies to all stats tagged `attribute`).
  - Stats can declare scaling dependencies on other stats (see Scaling Model below).
  - Dependency graph:
    - The system builds a directed acyclic graph (DAG) of stat dependencies at registration time.
    - Circular dependencies are detected and rejected at registration.
    - The DAG determines evaluation order: stats with no dependencies first, then stats that depend on them, etc.
  - Tag-based modifiers:
    - Modifiers can target a specific stat ID or a tag.
    - Tag-targeted modifiers apply to all stats with that tag.
    - Tag resolution happens once at modifier application; if new stats with the tag are registered later, existing modifiers do not retroactively apply.
- Scaling model (data-driven)
  - Stats use **either** a fixed base value **or** scaling from other stats—not both.
  - A stat with no scaling uses its `defaultValue` as the base before modifiers.
  - A stat with scaling declares one or more **scaling rules** in JSON:
    - Each rule references a source stat and defines how it contributes.
    - Scaling types:
      - **Linear**: `ratio` (e.g., 2 Strength = 1 Attack Power → `{"source": "hyforged:strength", "ratio": 0.5}`)
      - **Threshold**: `perPoints` + `bonus` (e.g., every 5 Luck = 1% crit → `{"source": "hyforged:luck", "perPoints": 5, "bonusBps": 100}`)
      - **Diminishing returns**: `curve` + `scale` (e.g., rating-to-effectiveness formula for crit chance so you can't reach 100%)
    - Multiple scaling rules on one stat are additive (sum the contributions).
  - Formulas are evaluated after all modifiers on source stats are resolved.
  - Modifiers on the derived stat itself are applied after the scaling computes the base.
  - Example JSON:
    ```json
    {
      "id": "hyforged:attack-power",
      "scaling": [
        { "source": "hyforged:strength", "type": "linear", "ratio": 2.0 }
      ]
    }
    ```
    ```json
    {
      "id": "hyforged:crit-chance-bps",
      "scaling": [
        { "source": "hyforged:luck", "type": "threshold", "perPoints": 5, "bonusBps": 100 },
        { "source": "hyforged:crit-chance-bps", "type": "diminishing", "curve": "rating", "scale": 1.0, "capBps": 7500 }
      ]
    }
    ```
- Units and rating semantics
  - Percent-valued stats use basis points where 10000 = 100%.
  - Rating stats represent raw rating; percent modifiers (increased/more) apply to the rating value, not the converted effectiveness.
  - Resistances are flat percent stats (basis points), not rating conversions.
  - Combat consumes rating stats by converting rating → effectiveness using the rating curves; flat percent stats are consumed directly.
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
    - “More/Less” multipliers
    - Caps/clamps (e.g., max resistance)
  - Define deterministic stacking rules and a consistent evaluation order:
    1. Resolve stats in dependency order (DAG topological sort).
    2. For each stat:
       a. Compute base value: either `defaultValue` (if no scaling) or sum of scaling rule contributions (if scaling defined).
       b. Apply modifiers in type order: flat → percent → more/less → caps.
    3. Scaling contributions use the **final** (post-modifier) values of source stats.
  - Modifiers can have metadata: source, duration (if temporary), and visibility (show/hide in UI).
  - Modifiers can specify a target:
    - `statId`: applies to a single stat.
    - `tag`: applies to all stats with the specified tag.
  - Modifiers can declare scope and conditions (e.g., global, weapon-local, skill-tagged, or state-based), and stat queries can accept a context descriptor to resolve applicability.
  - Modifier application should be idempotent per source: reapplying the same source refreshes/overwrites rather than stacking by default.
  - Prefer leveraging existing Hytale systems (e.g., status effect/buff tracking) for modifier lifecycle and cleanup where available.
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
  - All stats are first-class (including ability scores)
  - Categories and UI grouping
  - Tags and tag-based targeting
  - Scaling model (linear, threshold, diminishing returns)
  - Units and rating semantics
  - Dependency graph (DAG) and evaluation order
- Modifier framework
  - Types (flat/percent/multipliers/caps)
  - Stacking order rules
  - Target types (statId vs tag)
  - Visibility and attribution
  - Scope, conditions, and lifecycle
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
- 2026-01-19: Added unit conventions, rating semantics, scoped modifiers, and modifier lifecycle rules.
- 2026-01-19: Refined scaling model with linear/threshold/diminishing return types; ability scores now first-class stats.
- 2026-01-19: Added stat tags, dependency graph, and derived stat formulas.
- 2026-01-19: Initial version drafted.
