# Requirements: Experience System

## Vision
- Provide a clear, satisfying, and server-authoritative progression loop where players earn experience (XP), gain levels up to 100, and receive character-level progression for the general passive tree. Ability-score growth is owned by class progression.

## Goals
- Player-visible progression
  - Player XP, level, progress percentage, and XP-to-next-level are visible in the user interface.
  - Level cap (max level 100) is clearly communicated via a tooltip wherever level is displayed.
  - XP and level are viewable within the inventory UI (e.g., character panel / stats pane).
- XP sources and feedback
  - Combat XP is awarded for defeating enemies.
  - XP is awarded for completing quests and other configured in-game activities.
  - XP gains are communicated with a lightweight notification (amount + source category) that can be rate-limited/aggregated.
  - Level-ups trigger visual and/or auditory feedback to clearly indicate progression.
- Progression curve
  - XP required per level follows a configurable curve that supports long-term progression pacing to level 100.
  - The curve supports “early fast, late slow” shaping while avoiding extreme grind spikes.
  - XP rewards scale appropriately with activity difficulty (enemy level, quest tier, region difficulty).
- Rewards and coupling
- Character level grants general passive points (1 per level) and drives overall character progression.
- Ability scores are not granted by character level; they are granted by class progression.
- Level-up rewards are deterministic and auditable (no hidden random power spikes at level-up).
- Persistence and integrity
  - XP and level are persisted per-player and restored on login.
  - The server is authoritative for XP awards; clients cannot directly set XP/level.
  - XP changes (awards, penalties if any, admin adjustments) are logged in an auditable way.
- Admin and configuration
  - XP curve and reward tuning are data-driven (configurable without code changes).
  - Admin commands/tools exist to set/add/remove XP and set levels for testing and support.

## Non-Goals
- A “skill XP per activity” system (e.g., separate mining/crafting skill lines) unless explicitly introduced later.
- Class-specific XP and class level rewards (handled by the Class System).
- Power rewards at level-up other than general passive points (e.g., direct +crit% on level-up) unless explicitly introduced later.
- Offline XP accrual while the player is logged out.

## Quality Attributes
- Clarity: XP presentation avoids confusing spam; notifications are aggregated where necessary.
- Balance-friendly: curve and reward multipliers are configurable and can be iterated quickly.
- Security: XP awarding is server-only and resilient to packet/UI spoofing.
- Performance: XP computations are O(1) per award; UI updates are rate-limited.

## Feature Index
- XP state model
  - Level: integer in [1, 100]
  - Current XP: non-negative integer/long
  - XP-to-next-level: computed from curve
  - Progress percent: derived display value
  - General passive points: +1 per level
- XP awarding
  - Enemy defeat XP (scaled by enemy level/difficulty)
  - Objective completion XP (scaled by objective tier)
  - Biome discovery XP
  - “Other activity” hooks (config-driven categories)
  - Notification/aggregation rules
- Level-up flow
  - Detect crossing thresholds (supports multi-level from one award)
  - Trigger feedback (VFX/SFX)
  - Grant general passive points (no ability-score gains)
- Persistence & audit
  - Save/load behavior
  - Admin adjustment logging

## Change Log
- 2026-01-20: Consolidated into Progression Systems spec.
- 2026-01-20: Aligned rewards and XP sources with class progression split.
- 2026-01-19: Initial version drafted.
