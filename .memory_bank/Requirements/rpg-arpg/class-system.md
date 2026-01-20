# Requirements: Class System

## Vision
- Provide weapon-determined classes with distinct identities, progression, and integrations into stats, passives, and abilities—while remaining extensible and server-authoritative. Class progression is driven by class-specific XP filtered by equipped weapon tags.

## Goals
- Class identity and selection
- A player’s active class is determined by the weapon they are using (main-hand weapon tags).
- Class determination rules are explicit and data-driven (weapon tag families → class).
- UI clearly communicates the currently active class and what weapon tags map to which classes.
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
- Each class has its own progression system and class level (cap 20).
- Class level-ups grant ability score bonuses and 1 class passive point per level.
- Class levels are earned through class-relevant gameplay (configurable sources) filtered by weapon tags.
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
- Weapon tag → class mapping rules
- Class progression
  - XP sources (shared with character XP, filtered by weapon tags)
  - Level cap (20)
  - Level-up rewards (ability scores, class passive points)
- API/integration
  - Register/query classes
  - Hook for other systems
- UI
  - Active class display
  - Class progression display

## Change Log
- 2026-01-20: Consolidated into Progression Systems spec.
- 2026-01-20: Added class XP, weapon tag filtering, cap 20, and passive point rewards.
- 2026-01-19: Initial version drafted.
