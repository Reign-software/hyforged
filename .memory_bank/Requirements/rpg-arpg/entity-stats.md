# Requirements: Entity Stats (Applying Stats to Entities)

## Vision
- Define how the Stats System attaches to entities (players and NPCs), ensuring all stat sources aggregate correctly, recalculate efficiently, and integrate cleanly with the ECS architecture and other systems.

## Goals
- Entity stat container
  - Every combat-capable entity (player or NPC) has a stat container (component) that holds:
    - Base stats (intrinsic to the entity or template)
    - Active modifiers from all sources
    - Cached effective stat values
  - The stat container is the single source of truth for an entity's current stats.
  - The container supports querying any stat's effective value and its breakdown by source.
- Stat source aggregation
  - The system aggregates stats from multiple sources in a defined order:
    1. Base stats (entity template or player-allocated ability scores)
    2. Level-derived bonuses (from character level)
    3. Class bonuses (from class level and class identity)
    4. Equipment (from worn items and their affixes)
    5. Passive tree allocations
    6. Buffs and debuffs (temporary modifiers)
    7. Environmental/situational modifiers (region effects, weather, etc.)
  - Each source is tagged for UI breakdown and debugging.
  - The evaluation order follows the Stats System's modifier stacking rules.
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
- API and integration
  - Provide an API for other plugins/systems to:
    - Retrieve an entity's stat container
    - Query effective stat values
    - Add/remove modifiers with source attribution
    - Subscribe to stat-change events on a specific entity
    - Access breakdown by source for UI or debugging
  - Combat System and other consumers use this API rather than accessing raw data.
- Performance
  - Stat containers use dirty-flag caching; recompute only changed stats.
  - Bulk operations (e.g., loading a player with 20 equipment pieces) batch into a single recalculation pass.
  - Define upper bounds on modifier count per entity; graceful handling if exceeded.
- ECS integration
  - Stat container is implemented as an ECS component attached to entities.
  - Stat recalculation is handled by a dedicated system that processes dirty stat containers.
  - Events (stat changes) integrate with Hytale's event/messaging patterns.

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
- API surface
  - Query effective stats
  - Add/remove modifiers
  - Subscribe to events
  - Breakdown access
- ECS integration
  - Component design
  - System responsibilities

## Change Log
- 2026-01-19: Initial version drafted.
