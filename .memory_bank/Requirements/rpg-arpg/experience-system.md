# Requirements: Experience System

## Vision
- Provide a clear, satisfying, and server-authoritative progression loop where players earn experience (XP), gain levels up to 100, and receive ability-score growth as the primary power reward.

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
  - Only ability scores are directly affected by player level.
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
- Power rewards at level-up other than ability-score changes (e.g., direct +crit% on level-up) unless those are expressed as ability-score effects.
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
- XP awarding
  - Enemy defeat XP (scaled by enemy level/difficulty)
  - Quest completion XP (scaled by quest tier)
  - “Other activity” hooks (config-driven categories)
  - Notification/aggregation rules
- Level-up flow
  - Detect crossing thresholds (supports multi-level from one award)
  - Trigger feedback (VFX/SFX)
  - Apply ability-score gains
- Persistence & audit
  - Save/load behavior
  - Admin adjustment logging

## Change Log
- 2026-01-19: Initial version drafted.
