# Feature Spec: Resource Stats UI (Concentration & Rage)

## Metadata
- Feature ID (slug): resource-stats-ui
- Status: Implemented
- Owner: JBurl
- Date: 2026-01-20

## Summary
Convert Concentration and Rage stats to proper resource stats with HUD display support. Both resources should appear as bars on the player UI when their max value is greater than 0, following the same pattern as Mana/Stamina. Concentration is a reservation-based resource (like Spirit in PoE2), while Rage is a builder/spender that decays out of combat (like Rage in PoE2).

## Goals
- Register Concentration and Rage as Hytale EntityStatTypes for stat synchronization
- Create custom HUD components for each resource (Hytale's HudComponent enum cannot be extended)
- Display resource bars for players when max value > 0
- Bridge Hyforged stat values to Hytale's EntityStatMap for replication
- Implement Rage decay when out of combat
- Support reservation tracking for Concentration (reserved vs total)

## Non-Goals
- Implementing the full aura/herald/minion reservation logic (separate feature)
- Implementing rage generation triggers (handled by combat/skill systems)
- NPC resource display (player-only for now)

## User Experience
- Players with Concentration > 0 max see a Concentration bar showing available/max (e.g., "15/30" where 15 is unreserved)
- Players with Rage > 0 max see a Rage bar showing current/max (e.g., "75/100")
- Rage visibly decays over time when out of combat
- Resource bars appear/disappear dynamically as max values change (e.g., via equipment or class)
- Bars integrate with existing HUD alongside Health, Mana, Stamina

## Functional Requirements

### Concentration Resource
- Category: `resource`
- Behavior: Reservation system
  - "Current" value represents available (unreserved) concentration
  - "Max" value represents total concentration capacity
  - Reserved = Max - Current (calculated by reservation systems)
  - UI displays available/max format
- Scaling: Linear with Wisdom (ratio 2.0) — unchanged from current definition
- No regeneration
- Tags: Domain=resource, Mechanic=aura,minion, Type=resource

### Rage Resource
- Category: `resource`
- Behavior: Builder/spender with decay
  - "Current" value represents accumulated rage
  - "Max" value represents rage cap
  - Decays over time when out of combat (4 second delay, 5-10 per second decay rate)
- Scaling: None (flat max value from equipment/passives only)
- No regeneration (built via combat actions, decays otherwise)
- Tags: Domain=resource, Type=resource

### Hytale Integration
- Register `Concentration` as EntityStatType asset in `Server/Hyforged/Entity/Stats/`
- Register `Rage` as EntityStatType asset in `Server/Hyforged/Entity/Stats/` (for HUD/decay systems, not bridged to Hytale native)
- Create Entity UI components in `Server/Hyforged/Entity/UI/` for bar display
- Extend `HyforgedBridgeSystem` to sync MaxConcentration to EntityStatMap
- Rage is Hyforged-internal only (Hytale doesn't use Rage natively)

### Conditional Display
- Resource bars shown only when entity's max value > 0
- Use `HudManager` to dynamically show/hide custom HUD components
- Or leverage EntityUI visibility conditions if supported

## Non-Functional Requirements
- Bridge sync must be efficient (only update on stat changes, not every tick)
- Rage decay should use appropriate tick intervals (not per-frame)
- No client-side computation; server-authoritative

## Dependencies
- Hyforged Stats System (complete)
- Hytale EntityStats module
- Hytale EntityUI module
- HyforgedBridgeSystem

## Data/Schema Impact

### Stat Definition Changes
- `Concentration.json`: Change category from "aura" to "resource", add Type=resource tag
- `MaxRage.json`: Verify category is "resource", ensure proper tagging

### New Assets Required
- `Server/Hyforged/Entity/Stats/Concentration.json` — Hytale EntityStatType definition
- `Server/Hyforged/Entity/Stats/Rage.json` — Hytale EntityStatType definition
- `Server/Hyforged/Entity/UI/ConcentrationBar.json` — Entity UI component
- `Server/Hyforged/Entity/UI/RageBar.json` — Entity UI component

## API Changes
- Extend `HyforgedBridgeSystem` with `bridgeConcentration()` and `bridgeRage()` methods
- Add rage decay system (new `RageDecaySystem` or extend existing tick system)
- Add reservation API for Concentration (reserve/release methods) — may be separate feature

## Security/Privacy
- No special considerations; stats are player-visible by design

## Observability
- Log bridge sync events at DEBUG level
- Track rage decay ticks for debugging combat state issues

## Risks
- Custom HUD implementation complexity (must extend CustomUIHud, manage lifecycle)
- HUD update frequency may impact network traffic if not throttled
- Rage decay timing may feel unresponsive if tick rate is too low

## Open Questions
- [x] ~~Exact rage decay rate and delay~~ — **Confirmed: 4s delay, 5-10/sec decay**
- [x] ~~Should Concentration bar show "available" or "reserved/max"?~~ — **Confirmed: available/max**
- [x] ~~Are there HudComponent enum slots available for custom resources, or do we need custom UI?~~ — **Confirmed: Custom HUD required (HudComponent enum cannot be extended)**
- [x] ~~Should rage generation be part of this spec or a separate combat integration spec?~~ — **Confirmed: Separate combat spec**

## Acceptance Criteria
- [x] Concentration stat definition updated to category "resource"
- [x] Concentration registered as Hytale EntityStatType
- [x] Rage registered as Hytale EntityStatType (for HUD/decay, not bridged)
- [x] Custom HUD components created for Concentration and Rage bars
- [x] Bridge system syncs Concentration to EntityStatMap
- [x] Resource bars display for players when max > 0
- [x] Rage decays over time when out of combat (4s delay, 7/sec)
- [x] Bars hide when max value becomes 0
- [x] Separate `hyforged:rage` stat for current value, `hyforged:rage-max` for max bonus

## Impacted Areas (High-Level)
- Stats system (stat definitions)
- Bridge system (HyforgedBridgeSystem)
- Asset registration (EntityStatType, EntityUI)
- Combat system (rage decay trigger)

## Required Codebase/Architecture Changes (High-Level)
- Update `Concentration.json` Hyforged stat definition (category + tags)
- Create Hytale EntityStatType assets for Concentration and Rage
- Create custom HUD classes extending `CustomUIHud` for resource bars
- Implement HUD manager integration to show/hide based on max value
- Extend `HyforgedBridgeSystem` to bridge Concentration and Rage
- Implement `RageDecaySystem` for out-of-combat decay
- Add conditional HUD visibility logic (show/hide based on max value)

## References
- Requirements: .memory_bank/Requirements/rpg-arpg/README.md (Stats System)
- ADR-0001: Hybrid Hyforged + Hytale Stats
- ADR-0006: Replace Hytale Stat/Damage Systems for Exclusive Hyforged Control
- Hytale EntityStatType: `lib/Server/Entity/Stats/Mana.json` (reference pattern)
- Hytale EntityUI: `lib/Server/Entity/UI/Healthbar.json` (reference pattern)
- PoE2 Spirit mechanic (reservation model)
- PoE2 Rage mechanic (decay model)
