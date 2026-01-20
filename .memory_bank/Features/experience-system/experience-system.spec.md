# Feature Spec: Experience System

## Metadata
- Feature ID (slug): experience-system
- Status: Draft
- Owner: JBurl
- Date: 2026-01-20

## Summary
Implement server-authoritative character experience (XP) and level progression to level 100, with a data-driven exponential XP curve, aggregated XP notifications, and deterministic level-up rewards. Character level grants general passive points for the character-wide passive tree and informs combat rating effectiveness, but does not grant ability scores (handled by the Class System).

## Goals
- Provide a clear, server-authoritative character XP and level system capped at 100.
- Support data-driven exponential XP curves that are tunable without code changes.
- Award XP from configurable sources (combat kills, biome discovery, objectives) via ECS-driven hooks.
- Ensure level-ups grant 1 general passive point per level (100 total at cap).
- Provide UI-friendly progress values and rate-limited XP notifications.
- Persist and audit all XP/level changes with admin tools for adjustments.

## Non-Goals
- Class-specific XP and class progression (handled by Class System).
- Ability score grants on character level-up.
- Quest system XP (to be introduced later).
- Client-authoritative XP/level changes.

## User Experience
- Players see character level, current XP, XP-to-next, and progress percent in the character UI.
- XP gains appear as aggregated notifications to avoid spam; level-ups trigger distinct feedback.
- Character level is clearly capped at 100 in UI (tooltip or label).

## Functional Requirements

### FR-1: Character Progression State
- Track per-player character progression state: level, current XP, XP-to-next-level, and progress percent.
- Expose state for UI and other systems (e.g., combat effectiveness).

### FR-2: XP Curve (Data-Driven)
- XP required per level follows an exponential curve, configurable via data assets.
- The curve supports tuning without code changes and enforces a cap at level 100.

### FR-3: XP Awards (ECS-Driven Sources)
- XP sources are extensible via ECS hooks (no core changes required for new sources).
- Initial sources include:
  - Combat kills (scaled by enemy level/difficulty)
  - Biome discovery
  - Objective completion (aligned with objectives in `lib/Server` definitions)
- XP awards are server-authoritative and logged for audit.

### FR-4: Level-Up Flow
- Detect threshold crossings and support multi-level gains from a single award.
- Grant 1 general passive point per level gained.
- Emit level-up feedback (VFX/SFX/notification) and UI updates.

### FR-5: Notifications & Aggregation
- Aggregate XP gain notifications per tick or per event to reduce spam.
- Support lightweight floating text when available (optional presentation layer).

### FR-6: Persistence & Admin Tools
- Persist character XP/level state with existing persistence systems.
- Provide admin tools to set/add/remove XP and set level.
- Log admin adjustments for auditability.

## Non-Functional Requirements
- **Performance:** O(1) per XP award; aggregation prevents UI spam.
- **Security:** Clients cannot directly set XP/level.
- **Observability:** XP changes and level-ups are auditable.

## Dependencies
- Entity Stats (for level-based effectiveness calculations).
- ECS event model for XP source hooks.
- Persistence framework already used for player state.

## Data/Schema Impact
- New data assets for character XP curve configuration.
- Player progression state persisted alongside existing player data.

## API Changes
- New progression events for XP changes and level-ups.
- Admin command surface for XP/level adjustments.

## Security/Privacy
- Server-only authority for XP awards and level changes.
- Permissions required for admin XP/level commands.

## Observability
- Log XP awards with source category and amount.
- Log level-up events with old/new level and passive points granted.

## Risks
- XP award spam without aggregation could flood UI; mitigate with rate limits.
- Balancing curve across level 1–100 requires tuning and telemetry.

## Open Questions
- Exact UI placement and presentation for XP notifications and floating text.

## Acceptance Criteria
- [ ] Character XP/level state is tracked and persisted per player.
- [ ] XP curve is data-driven and enforces a level cap at 100.
- [ ] XP awards are extensible via ECS and include kills, biomes, and objectives.
- [ ] Level-ups grant 1 general passive point per level (100 total at cap).
- [ ] Notifications are rate-limited/aggregated.
- [ ] Admin tools exist for XP/level adjustments with audit logging.

## Impacted Areas (High-Level)
- Player progression state storage
- ECS XP award hooks
- Character UI
- Admin command tooling

## Required Codebase/Architecture Changes (High-Level)
- Add character progression state model with persistence integration.
- Add data-driven XP curve assets and loaders.
- Add ECS-driven XP award pipeline with aggregation and notifications.
- Add admin command surface for XP/level adjustments.

## References
- Requirements entry: .memory_bank/Requirements/rpg-arpg/experience-system.md
- Related systems: Entity Stats, Class System
