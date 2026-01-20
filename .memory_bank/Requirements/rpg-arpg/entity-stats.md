# Requirements: Entity Stats (Applying Stats to Entities)

## Vision
- Define how the Stats System attaches to entities (players and NPCs), ensuring all stat sources aggregate correctly, recalculate efficiently, and integrate cleanly with the ECS architecture and other systems.

## Goals
- Entity stat container
  - Every combat-capable entity (player or NPC) has a stat container (component) that holds:
    - Base stat values (for stats without scaling, e.g., player-allocated attribute points)
    - Active modifiers from all sources
    - Cached effective stat values for all stats
  - Ability scores (Strength, Dexterity, etc.) are regular stats tagged `attribute`; players allocate points that set their base value.
  - The stat container is the single source of truth for an entity's current stats.
  - The container supports querying any stat's effective value and its breakdown by source.
- Stat source aggregation
  - The system aggregates modifiers from multiple sources:
    1. Base values (entity template or player-allocated points for attributes)
    2. Level-derived bonuses (from character level)
    3. Class bonuses (from class level and class identity)
    4. Equipment (from worn items and their affixes)
    5. Passive tree allocations
    6. Buffs and debuffs (temporary modifiers)
    7. Environmental/situational modifiers (region effects, weather, etc.)
  - Each modifier is tagged with its source type for UI breakdown and debugging.
  - The evaluation order follows the Stats System's modifier stacking rules.
  - Scaling (derived stats) is computed using the final values of source stats, not raw bases.
  - Modifiers are stored with stable source keys so reapplication refreshes/overwrites rather than stacking by default.
  - The container supports context-aware queries (weapon/skill tags, state flags) so only applicable modifiers are applied per action.
- Player entity lifecycle
  - On player join/load:
    - Load persisted allocations (ability scores, class levels, passive selections, equipment).
    - Rebuild the stat container by aggregating all sources.
    - Subscribe to change events (equipment swap, buff applied, passive allocated, level up).
  - On relevant events (equipment change, buff expire, level up, passive allocation):
    - Invalidate affected cached stats.
    - Recompute effective values.
    - Emit stat-change events for UI and dependent systems.
  - On player disconnect/save:
    - Persist only source data (allocations, equipment references), not computed stats.
- NPC entity lifecycle
  - NPC templates define:
    - Base stats (or formulas referencing NPC level)
    - Optional modifier overrides (e.g., elite modifiers, boss auras)
  - On NPC spawn:
    - Determine NPC level (from region difficulty, scaling rules, or template override).
    - Instantiate stat container with base stats scaled to level.
    - Apply any template-defined modifiers.
  - NPCs may receive temporary modifiers (debuffs from players, environmental effects).
  - On NPC despawn:
    - No persistence required; stat containers are discarded.
- Stat recalculation triggers
  - Define explicit triggers that cause stat recalculation:
    - Equipment equipped/unequipped
    - Buff/debuff applied/expired
    - Passive node allocated/deallocated
    - Character level up
    - Class level up or class change (weapon swap)
    - Environmental modifier zone enter/exit
  - Batch recalculations when multiple changes occur in the same tick (e.g., equipping a full gear set).
- Stat dependency resolution
  - Stats can scale from other stats (e.g., Attack Power scales from Strength). See [Stats System](stats-system.md) for the scaling model and dependency graph.
  - Recalculation uses the Stats System's dependency DAG to determine evaluation order:
    1. Collect all dirty (invalidated) stats.
    2. Expand to include any stats that depend on dirty stats (transitive closure via scaling rules).
    3. Topologically sort the affected stats by dependency order.
    4. For each stat in order:
       a. Compute base value:
          - If stat has no scaling: use `defaultValue` from definition.
          - If stat has scaling rules: sum contributions from already-computed source stats.
       b. Collect all modifiers targeting this stat (by ID or by tag).
       c. Apply modifiers in type order (flat → percent → more/less → caps).
       d. Cache the effective value.
    5. Emit change events for all stats whose effective value changed.
  - Scaling contributions use the **final** (post-modifier) values of source stats, not raw bases.
  - Tag-based modifiers (e.g., "+5% to all Attributes") are resolved by the Stats System; the Entity Stats container applies them to each matching stat during recalculation.
  - Circular dependencies are prevented at stat registration time (Stats System responsibility); recalculation assumes a valid DAG.
- API and integration
  - Provide an API for other plugins/systems to:
    - Retrieve an entity's stat container
    - Query effective stat values
    - Add/remove modifiers with source attribution
    - Subscribe to stat-change events on a specific entity
    - Access breakdown by source for UI or debugging
    - Upsert/refresh modifiers by source key to prevent duplicate stacking
  - Combat System and other consumers use this API rather than accessing raw data.
- Performance
  - Stat containers use dirty-flag caching; recompute only changed stats.
  - Bulk operations (e.g., loading a player with 20 equipment pieces) batch into a single recalculation pass.
  - Define upper bounds on modifier count per entity; graceful handling if exceeded.
- ECS integration
  - Stat container is implemented as an ECS component attached to entities.
  - Stat recalculation is handled by a dedicated system that processes dirty stat containers.
  - Events (stat changes) integrate with Hytale's event/messaging patterns.
  - Prefer leveraging native Hytale buff/status systems for modifier lifetime tracking and cleanup where possible.

## Non-Goals
- Storing computed stats in persistence; only source data is saved.
- Per-stat custom logic; all stats follow the unified modifier model.
- Client-side stat containers; the server is authoritative.

## Quality Attributes
- Single source of truth: all systems query the same stat container.
- Extensible: new stat sources can be added without modifying existing aggregation logic.
- Performant: dirty-flag caching minimizes unnecessary recomputation.
- Observable: stat changes emit events for UI and logging.
- ECS-native: integrates cleanly with Hytale's entity-component-system architecture.

## Feature Index
- Stat container component
  - Structure and data model
  - Effective value cache
  - Source breakdown
- Source aggregation
  - Ordered source list
  - Modifier application rules
  - Modifier identity and refresh
- Player lifecycle
  - Load/join flow
  - Change event handling
  - Save/disconnect flow
- NPC lifecycle
  - Template-based instantiation
  - Level-based scaling
  - Temporary modifiers
- Recalculation
  - Trigger definitions
  - Batching and dirty-flag model
  - Dependency resolution (DAG traversal)
  - Tag-based modifier expansion
- API surface
  - Query effective stats
  - Add/remove modifiers
  - Subscribe to events
  - Breakdown access
- ECS integration
  - Component design
  - System responsibilities

## Change Log
- 2026-01-19: Added modifier identity/refresh rules, context-aware queries, and lifecycle integration notes.
- 2026-01-19: Updated to reflect ability scores as regular stats; clarified scaling vs modifier distinction.
- 2026-01-19: Expanded stat dependency resolution and tag-based modifier handling.
- 2026-01-19: Initial version drafted.
