# Requirements: Class System

## Vision
- Provide weapon-determined classes with distinct identities, progression, and integrations into stats, passives, and abilities—while remaining extensible and server-authoritative.

## Goals
- Class identity and selection
  - A player’s active class is determined by the weapon they are using.
  - Class determination rules are explicit and data-driven (weapon categories → class).
  - UI clearly communicates the currently active class and what weapon types map to which classes.
- Extensible classes and abilities
  - Classes define:
    - Core identity (name, description)
    - Associated weapon categories
    - Class progression track (class level)
    - Class-specific stat bonuses and/or unlock conditions
  - System provides an API for other plugins/systems to:
    - Register new classes
    - Modify weapon-to-class mapping
    - Add/remove class progression rewards
    - Query current class and class level
- Separate class progression
  - Each class has its own progression system and class level.
  - Class level-ups grant a small ability score bonus.
  - Class levels are earned through class-relevant gameplay (configurable sources).
  - Switching weapons can change active class without deleting progress.
- Persistence
  - Persist per-player class progression and associated unlocks/allocations.
  - Maintain backward-compatible saved data as classes are added/changed.
- UI
  - Provide a class panel in inventory/character UI:
    - Active class
    - Class level and progress
    - Summary of class bonuses

## Non-Goals
- A single irreversible “choose at character creation” class lock.
- Forcing a class to require only one specific weapon instance; mapping is by category.

## Quality Attributes
- Extensible: new classes can be introduced without breaking saves.
- Clear: switching weapons yields understandable class changes.
- Balanced: class rewards are tunable and small at the ability-score level (as specified).
- Secure: class selection cannot be spoofed client-side.

## Feature Index
- Weapon → class mapping rules
- Class progression
  - XP sources
  - Level-up rewards (ability scores)
- API/integration
  - Register/query classes
  - Hook for other systems
- UI
  - Active class display
  - Class progression display

## Change Log
- 2026-01-19: Initial version drafted.
