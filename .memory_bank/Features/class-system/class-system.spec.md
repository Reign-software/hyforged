# Feature Spec: Class System

## Metadata
- Feature ID (slug): class-system
- Status: Draft
- Owner: JBurl
- Date: 2026-01-20

## Summary
Implement weapon-tag-driven class progression with class-specific XP, level cap 20, and deterministic rewards. Class XP is awarded alongside character XP but only for the active class determined by main-hand weapon tags. Class levels grant ability score bonuses and 1 class passive point per level for class-specific passive trees.

## Goals
- Define data-driven classes mapped to weapon tag families.
- Award class XP from shared sources (combat kills, biome discovery, objectives) filtered by active class weapon tags.
- Enforce class level cap of 20 with exponential curve (lower max XP requirement than character XP).
- Grant ability score bonuses and 1 class passive point per class level.
- Persist class progression per player and expose state for UI and other systems.

## Non-Goals
- Character XP and character level progression (handled by Experience System).
- Hard-coded class definitions or weapon mappings.
- Quest system class XP (to be introduced later).

## User Experience
- Players see their active class, class level, and class XP progress in the character UI.
- Switching weapons updates the active class without losing prior class progress.
- Level-ups trigger feedback and update class passive points and ability score bonuses.

## Functional Requirements

### FR-1: Class Definitions (Data-Driven)
- Class definitions are data-driven and include:
  - Class identity (name, description)
  - Eligible weapon tag families for class XP
  - Class progression curve (level 1–20)
  - Ability score rewards per class level
- Classes can be added or modified without code changes.

### FR-2: Active Class Resolution
- Active class is determined by main-hand weapon tags matched against class definitions.
- Only one class earns XP at a time based on active class resolution.
- If no main-hand weapon tags match any class, XP is applied only to character progression and no class XP is awarded.

### FR-3: Class XP Awards
- Class XP is awarded from the same source events as character XP.
- Awards are filtered by active class weapon tags.
- XP awards support multi-level gains in a single event.

### FR-4: Class Level Rewards
- Each class level grants:
  - Ability score bonuses (data-driven per class)
  - 1 class passive point (20 total at cap)
- Level-up rewards are deterministic and auditable.

### FR-5: Persistence & Admin Tools
- Persist per-player class XP/level state using existing persistence systems.
- Provide admin tools to set/add/remove class XP and set class level.
- Log admin adjustments for auditability.

### FR-6: UI & Notifications
- Class panel displays active class, class level, XP-to-next, and progress percent.
- Level-up notifications are aggregated/rate-limited to avoid spam.

## Non-Functional Requirements
- **Performance:** O(1) per class XP award; no per-tick scanning of classes.
- **Security:** Server-authoritative class XP/level changes.
- **Extensibility:** New classes and weapon-tag mappings can be introduced via data.

## Dependencies
- Experience System (shared XP source pipeline and notifications).
- Entity Stats (ability score application).
- Weapon item definitions and tag families from base data (`lib/Server`).

## Data/Schema Impact
- New data assets for class definitions and class XP curve configuration.
- Persisted per-player class progression state (by class ID).

## API Changes
- New class progression events for class XP changes and class level-ups.
- Admin command surface for class XP/level adjustments.

## Security/Privacy
- Server-only authority for class XP/level changes.
- Permissions required for admin class commands.

## Observability
- Log class XP awards with source category and active class.
- Log class level-up events with old/new level and rewards applied.

## Risks
- Weapon tag ambiguities could lead to unclear class selection; requires clear data definitions.
- Rapid weapon swapping could create confusing class XP attribution without clear UI feedback.

## Open Questions
- None.

## Acceptance Criteria
- [ ] Active class is resolved from main-hand weapon tags.
- [ ] Class XP uses shared XP sources and is filtered by active class.
- [ ] Class level cap is 20 with a configurable exponential curve.
- [ ] Class levels grant ability score bonuses and 1 class passive point per level.
- [ ] Class progression is persisted and admin tools exist for adjustments.
- [ ] UI displays class progression with aggregated notifications.

## Impacted Areas (High-Level)
- Class data assets
- Player progression state storage
- UI (class panel and notifications)
- Admin command tooling

## Required Codebase/Architecture Changes (High-Level)
- Add class progression state model with persistence integration.
- Add data-driven class definition assets and class XP curves.
- Implement active class resolution from weapon tags.
- Integrate class XP awards into shared XP pipeline.
- Add admin command surface for class XP/level adjustments.

## References
- Requirements entry: .memory_bank/Requirements/rpg-arpg/class-system.md
- Related systems: Experience System, Entity Stats
