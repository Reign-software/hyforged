# Feature Spec: Progression Systems (Experience + Class)

## Metadata
- Feature ID (slug): progression-systems
- Status: Draft
- Owner: JBurl
- Date: 2026-01-20

## Summary
Define and implement server-authoritative character and class progression as a single integrated feature. Character XP drives overall character level (cap 100) and grants general passive points, while class XP drives class level (cap 20) and grants ability score bonuses plus class passive points. Class XP is awarded alongside character XP but only when main-hand weapon tags match a class definition. Character level informs combat rating effectiveness calculations.

## Goals
- Provide a unified, data-driven progression pipeline for character XP and class XP.
- Ensure character level is capped at 100 with an exponential XP curve.
- Ensure class levels are capped at 20 with an exponential XP curve (lower max XP requirement).
- Award XP from extensible ECS-driven sources (kills, biome discovery, objectives).
- Grant deterministic level-up rewards: character passive points and class ability score bonuses + class passive points.
- Persist all progression state and provide admin tools for adjustment.
- Provide UI-friendly progress data and rate-limited notifications.

## Non-Goals
- Quest system XP (added later).
- Client-authoritative XP/level changes.
- Hard-coded class definitions or weapon mappings.

## User Experience
- Character UI shows character level, XP-to-next, progress percent, and general passive points.
- Character level cap (100) is clearly communicated in UI.
- Class UI shows active class, class level, XP-to-next, and progress percent.
- Switching weapons updates active class without losing prior class progress.
- XP gains are aggregated/rate-limited; level-ups trigger distinct feedback.
- If no class matches main-hand weapon tags, only character XP is awarded.

## Functional Requirements

### FR-1: Character Progression State
- Track per-player character level, current XP, XP-to-next, and progress percent.
- Level cap is 100.
- Character level grants 1 general passive point per level.
- Character level is exposed for combat effectiveness calculations and UI.

### FR-2: Class Definitions (Data-Driven)
- Class definitions are data-driven and include:
  - Class identity (name, description)
  - Eligible weapon tag families for class XP
  - Class progression curve (level 1–20)
  - Ability score rewards per class level
- Classes can be added or modified without code changes.

### FR-3: Class Progression State
- Track per-player class XP/level per class ID.
- Class level cap is 20.
- Class level grants:
  - Ability score bonuses (data-driven per class)
  - 1 class passive point per class level

### FR-4: XP Curves (Data-Driven)
- Character and class XP curves are exponential and data-driven.
- Class curve has lower maximum XP requirements than the character curve.

### FR-5: XP Awards (ECS-Driven Sources)
- XP sources are extensible via ECS hooks.
- Initial sources include:
  - Combat kills (scaled by enemy level/difficulty)
  - Biome discovery
  - Objective completion (aligned with objectives in `lib/Server` definitions)
- XP awards are server-authoritative and logged for audit.

### FR-6: Active Class Resolution
- Active class is resolved from main-hand weapon tags matched against class definitions.
- Class definitions declare which weapon tag families contribute to class XP.
- If no class matches, only character XP is awarded and no class XP is granted.

### FR-7: Level-Up Flow
- Detect threshold crossings and support multi-level gains from a single award.
- Character level-up grants general passive points only.
- Class level-up grants ability score bonuses and class passive points.

### FR-8: Notifications & Aggregation
- Aggregate XP gain notifications per tick or per event to reduce spam.
- Support lightweight floating text when available (optional presentation layer).

### FR-9: Persistence & Admin Tools
- Persist character and class progression using existing persistence systems.
- Provide admin tools to set/add/remove XP and set levels for both systems.
- Log admin adjustments for auditability.

## Non-Functional Requirements
- **Performance:** O(1) per award; aggregation prevents UI spam.
- **Security:** Server-authoritative progression state.
- **Extensibility:** New XP sources and classes can be added via data.

## Dependencies
- Entity Stats (ability score application and effectiveness calculations).
- ECS event model for XP source hooks.
- Weapon tag families from base data (`lib/Server`).
- Persistence framework already used for player state.

## Data/Schema Impact
- New data assets for character XP curve configuration.
- New data assets for class definitions and class XP curves.
- Persisted per-player character and class progression state.

## API Changes
- New progression events for XP changes and level-ups.
- Admin command surface for character/class XP and level adjustments.

## Security/Privacy
- Server-only authority for XP/level changes.
- Permissions required for admin progression commands.

## Observability
- Log XP awards with source category and amount.
- Log character/class level-ups with old/new levels and rewards applied.

## Risks
- Weapon tag ambiguities could cause unclear class selection; require clear data definitions.
- Balancing exponential curves across caps requires tuning and telemetry.
- Rapid weapon swapping could create confusing class XP attribution without clear UI feedback.

## Open Questions
- None.

## Acceptance Criteria
- [ ] Character progression tracked and persisted; cap 100 enforced.
- [ ] Class progression tracked and persisted; cap 20 enforced.
- [ ] Exponential, data-driven XP curves for both systems.
- [ ] Shared XP source pipeline with class XP filtered by weapon tags.
- [ ] Character level grants 1 general passive point per level only.
- [ ] Class level grants ability score bonuses and 1 class passive point per level.
- [ ] Aggregated notifications and level-up feedback implemented.
- [ ] Admin tools for XP/level adjustments with audit logging.

## Impacted Areas (High-Level)
- Player progression state storage
- ECS XP award hooks and aggregation
- Class definition assets and weapon tag mapping
- UI (character and class panels)
- Admin command tooling

## Required Codebase/Architecture Changes (High-Level)
- Add unified progression state model with persistence integration.
- Add data-driven XP curve assets (character and class).
- Implement active class resolution and class XP filtering.
- Add admin command surface for progression adjustments.

## References
- Requirements entries:
  - .memory_bank/Requirements/rpg-arpg/experience-system.md
  - .memory_bank/Requirements/rpg-arpg/class-system.md
- Related systems: Entity Stats
