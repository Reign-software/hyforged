# Feature Plan: Resource Stats UI (Concentration & Rage)

## Metadata
- Feature ID (slug): resource-stats-ui
- Status: In Progress
- Owner: JBurl
- Date: 2026-01-20

## ACID Plan Integrity
- Atomicity: Each phase delivers a buildable, coherent slice (data assets, server systems, validation) that can stand alone.
- Consistency: Every step maps to the spec requirements for stats, UI, bridge sync, and rage decay.
- Isolation: Phases focus on distinct layers (data, runtime logic, validation) with minimal overlap.
- Durability: Plan and status updates are recorded in the memory bank.

## Phase 1: Data Definitions & Assets
- Phase Status: [ ] Not Started  [ ] In Progress  [x] Done

### Steps
- [x] Update Hyforged stat definition for Concentration (category=resource, add Type=resource tag).
- [x] Verify MaxRage stat definition category and tags (resource + Type=resource).
- [x] Add EntityStatType assets for Concentration and Rage under Server/Hyforged/Entity/Stats.
- [x] Add Entity UI component assets for Concentration and Rage bars under Server/Hyforged/Entity/UI.
- [x] Document asset locations and reference patterns (Mana/Healthbar) for future consistency.

### Exit Criteria
- [ ] Build passes
- [ ] Assets and stat definitions present and validated against reference JSON schemas

## Phase 2: Runtime Systems & Bridging
- Phase Status: [ ] Not Started  [ ] In Progress  [x] Done

### Steps
- [x] Extend HyforgedBridgeSystem with Concentration and Rage sync to EntityStatMap (update only on stat changes).
- [x] Add HUD integration logic to show/hide custom resource bars when max > 0 (player-only).
- [x] Implement Rage decay system with out-of-combat delay and decay rate (tick-based, server-authoritative).
- [x] Add debug-level logging for bridge sync and rage decay ticks.

### Exit Criteria
- [ ] Build passes
- [ ] Runtime systems operate without per-frame updates or redundant syncs

## Phase 3: Validation & UX Checks
- Phase Status: [ ] Not Started  [x] In Progress  [ ] Done

### Steps
- [ ] Verify Concentration bar displays available/max and reflects reservation changes.
- [ ] Verify Rage bar displays current/max and decays after 4s out-of-combat delay.
- [ ] Confirm bars appear/disappear when max value crosses 0.
- [ ] Ensure no NPC resource display (player-only).
- [x] Update Modding_Doc as needed with user-facing notes (if applicable).

### Exit Criteria
- [ ] Build passes
- [ ] Test validation completed (manual or automated as available)

## Dependencies
- Hyforged Stats System
- Hytale EntityStats module
- Hytale EntityUI module
- HyforgedBridgeSystem

## Risks & Mitigations
- Custom HUD complexity — Mitigate by reusing established EntityUI patterns and keeping assets minimal.
- Network overhead from stat sync — Mitigate by diff-based updates on stat changes only.
- Rage decay feel/tuning — Mitigate by config-driven decay rate and debug logging for tuning.

## Testing Strategy
- Manual in-game validation for HUD visibility, values, and rage decay timing.
- Targeted debug logs to confirm bridge updates and decay ticks.

## Rollback Plan
- Revert newly added assets and disable bridge/decay systems; resource bars will no longer render.

## Deployment / Release Notes
- Adds new Concentration and Rage resource bars with server-authoritative sync and rage decay.

## Implementation Summary (post-development)
- Added data-driven resource stat updates for Concentration and Rage, plus EntityStatType and Entity UI assets for custom bars.
- Extended bridging to sync Concentration/Rage max values into EntityStatMap with debug logging.
- Implemented custom HUD rendering and rage decay system (out-of-combat) backed by a JSON config asset.

## Test Results (post-validation)
- Not run (manual validation pending).

## Lessons Learned (post-release)
- TBD
