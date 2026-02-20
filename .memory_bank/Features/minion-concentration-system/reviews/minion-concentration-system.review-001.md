# Review: Minion Concentration System — 2026-02-20

## Review Metadata
- Reviewer: GitHub Copilot (Automated)
- Scope: All 16 new files + 8 modified files for minion-concentration-system feature
- Spec Version: minion-concentration-system.spec.md (2026-02-19)
- Plan Version: minion-concentration-system.plan.md (Phases 1–7)
- Overall Status: **Needs Changes**

## Summary

The minion concentration system implementation is substantially complete with clean compilation, 26 new passing tests, and correct ECS patterns throughout. The core summoning flow (spawn, despawn, reconnect, death handling, stat bridge, HUD breakpoints) works as designed. However, one spec requirement (**FR-7 reactive cap enforcement**) is unimplemented, and there are several major items around TOCTOU safety, wall-clock duration timers, and missing NPC template load-time validation that need to be addressed before shipping.

**Findings**: 1 Critical, 4 Major, 6 Minor

---

## Findings

### Critical

#### C-1: FR-7 Reactive Cap Enforcement Not Implemented

**File**: `src/main/java/reign/software/hyforged/minion/MinionSummonService.java`
**Spec Requirement**: FR-7 states: "If `max-minions` decreases while minions are active (e.g., equipment change): unsummon the lowest-priority minion(s) until count <= new cap."
**Acceptance Criterion**: "If `max-minions` decreases, lowest-priority minions are unsummoned until count is within cap (FR-7)"

The plan (line 197) describes an `enforceCapReduction(Ref<EntityStore>, int)` method and defers it to "Phase 3 stat bridge wiring." Phase 3's implementation summary (line 234) confirms it was deferred, but it was never implemented in any subsequent phase. A workspace-wide search for `enforceCapReduction` finds zero matches outside the plan file.

Currently, `max-minions` is only validated at spawn time in `summon()` (line 163). If a player's max-minions stat decreases (e.g., unequipping an item), existing minions remain active above the new cap indefinitely.

**Severity**: Critical — a spec requirement is completely missing.
**Fix**: Implement `enforceCapReduction` in `MinionSummonService`. Wire it as either:
  - A stat change listener on `max-minions` (preferred), or
  - A periodic check in `processTick()` that compares active count vs current cap for each summoner.
  Must iterate minions in ascending priority order and enqueue despawn for excess.

---

### Major

#### M-1: TOCTOU Race in Summon Validation

**File**: `src/main/java/reign/software/hyforged/minion/MinionSummonService.java` (lines 163–199 and 595–700)
**Issue**: `summon()` validates concentration availability and max-minions cap, then enqueues a `SpawnRequest`. The actual spawn in `performSpawn()` (processed a tick later) does **not** re-validate that sufficient concentration remains available before calling `reserveConcentration()`. Between enqueue and processing, another summon or concentration drain could consume the needed concentration.

**Severity**: Major — the spawn could reserve concentration the summoner no longer has, leading to negative effective concentration.
**Fix**: In `performSpawn()`, re-check available concentration against `adjustedCost` before calling `ConcentrationService.get().reserveConcentration()`. If insufficient, abort the spawn and log at FINE level.

#### M-2: Duration Timers Use Wall-Clock Time Instead of Game Ticks

**File**: `src/main/java/reign/software/hyforged/minion/MinionSummonService.java` (lines 700–702)
**Issue**: `performSpawn()` stores duration deadlines using `System.currentTimeMillis()` and `checkDurationTimers()` compares against `System.currentTimeMillis()`. The code comment acknowledges this: "approximate, replaced by game ticks if available" — but it was never replaced. Wall-clock time drifts during server lag spikes, server pauses, or time-of-day changes. Spec NFR-1 requires "single-tick spawn and despawn operations" and the plan specifies game-tick-based duration.

**Severity**: Major — durations will be inaccurate during server lag. A 60-second timed minion could last significantly longer or shorter than intended.
**Fix**: Track duration in game ticks. Either:
  - Pass the world tick counter from `processTick()` (available via `Store<EntityStore>` tick counter if accessible), or
  - Use an incrementing tick counter inside `processTick()` itself (increment by 1 each call).
  Convert `baseDurationSeconds` to ticks at 20 TPS: `baseDurationSeconds * 20`.

#### M-3: NPC Template Validation at Load Time Not Implemented

**File**: `src/main/java/reign/software/hyforged/minion/MinionDefinitionRegistry.java` (lines 103–127)
**Spec Requirement**: FR-2: "Definitions are validated at server load time (including NPC template availability)"
**Plan Step 1.4**: "Validate NPC template exists via NPCPlugin.get().getIndex()"

`loadFromResources()` parses JSON and validates required fields (`Id`, `NpcTemplate`) but does **not** check whether the NPC template actually exists in the NPC plugin's asset map. Invalid template references are only caught at spawn time in `performSpawn()`.

**Severity**: Major — server operators get no feedback at load time about misconfigured minion definitions. A typo in `NpcTemplate` silently passes initialization and only fails when a player attempts to summon.
**Fix**: After parsing each definition, call `NPCPlugin.get().getIndex(def.getNpcTemplate())` and log a WARNING if it returns < 0. Optionally skip registration of invalid definitions.

#### M-4: MinionDefinition Missing Stat Overrides Field

**File**: `src/main/java/reign/software/hyforged/minion/MinionDefinition.java`
**Spec Requirement**: FR-2: "Each definition specifies: NPC template reference, concentration cost, default priority, base duration (0 = permanent), spawn offset, tags, **and stat overrides**"

`MinionDefinition` has fields for `id`, `npcTemplate`, `concentrationCost`, `defaultPriority`, `baseDuration`, `spawnOffset{X,Y,Z}`, and `tags` — but no `statOverrides` field. The JSON parser does not read a `StatOverrides` section. Neither JSON example file includes stat overrides.

**Severity**: Major — a spec-defined field is entirely absent from the data model.
**Fix**: Add a `Map<String, Integer> statOverrides` field to `MinionDefinition`, parse `StatOverrides` as an object map from JSON, and pass overrides to `HyforgedMinionStatBridgeSystem.propagateStats()` (or an equivalent application point). If stat overrides are intentionally deferred, document this as a deviation in the plan and update the spec accordingly.

---

### Minor

#### m-1: Breakpoint Hash Does Not Include Ability ID

**File**: `src/main/java/reign/software/hyforged/stats/hud/ResourceStatsHudSystem.java` (lines 194–203)
**Issue**: `computeBreakpointHash()` only hashes `bp.cost()` and `bp.enabled()`, not `bp.abilityId()`. If two abilities are reordered but have identical costs and enabled states, the hash won't change and the HUD won't update.

**Severity**: Minor — unlikely in practice since cost+enabled combinations are usually distinct, but it's a correctness gap.
**Fix**: Include `bp.abilityId().hashCode()` in the hash computation.

#### m-2: SpawnRequest.summonerRef Field Is Unused

**File**: `src/main/java/reign/software/hyforged/minion/MinionSummonService.java` (lines 918–926)
**Issue**: `SpawnRequest` stores a `@Nullable Ref<EntityStore> summonerRef` but `performSpawn()` always re-resolves the summoner from UUID via `store.getExternalData().getRefFromUUID(summonerUuid)`. The cached ref is never read.

**Severity**: Minor — dead field adds confusion.
**Fix**: Either remove the `summonerRef` field from `SpawnRequest`, or use it as a fast-path in `performSpawn()` with a fallback to UUID resolution if invalid.

#### m-3: parseMinionTypeId Placement

**File**: `src/main/java/reign/software/hyforged/minion/MinionReconnectHandler.java`
**Issue**: The static utility method `parseMinionTypeId(String abilityId)` lives in `MinionReconnectHandler` but is tested from `MinionSummonServiceTest`. This cross-package test coupling suggests it belongs in `MinionSummonService` (where the ability ID format is defined) or a shared utility class.

**Severity**: Minor — code organization only.
**Fix**: Move `parseMinionTypeId` to `MinionSummonService` as a package-private static method alongside `resolveNextAbilityId`.

#### m-4: MinionTrackerComponent Uses List Per Ability ID

**File**: `src/main/java/reign/software/hyforged/minion/component/MinionTrackerComponent.java`
**Issue**: The tracker stores `Map<String, List<Ref<EntityStore>>>` keyed by ability ID, but the convention is one minion per ability ID (`minion:<typeId>:<index>` ensures uniqueness). The List adds allocation overhead and API complexity.

**Severity**: Minor — works correctly but is over-designed for current requirements.
**Fix**: Consider simplifying to `Map<String, Ref<EntityStore>>`. If multi-ref-per-ability is needed in the future, it can be reintroduced.

#### m-5: Hardcoded Minion Definition Resource Paths

**File**: `src/main/java/reign/software/hyforged/HyforgedPlugin.java` (around line 343)
**Issue**: `MinionDefinitionRegistry.loadFromResources()` is called with an explicit list of two paths (`SkeletonWarrior.json`, `KweebecSapling.json`). Adding new minion types requires a code change.

**Severity**: Minor — NFR-2 requires fully data-driven design. While the JSON contents are data-driven, discovery is not.
**Fix**: Implement resource directory scanning for `Server/Hyforged/Minions/*.json` or use a manifest/index file. This aligns with how other registries (e.g., stat definitions) handle resource discovery.

#### m-6: MinionDeathSystem Uses store.removeEntity Directly

**File**: `src/main/java/reign/software/hyforged/minion/system/MinionDeathSystem.java`
**Issue**: NFR-4 states "Entity mutations go through CommandBuffer." The MinionDeathSystem is a `RefChangeSystem` that receives a `CommandBuffer` parameter. However, the death system delegates cleanup to `MinionSummonService` which calls `store.removeEntity()` directly in `performDespawn()`. The `onComponentAdded` callback itself doesn't mutate entities directly (it calls service methods that enqueue), so this is technically compliant, but the `performDespawn` path in the ticking system does direct store mutation.

**Severity**: Minor — `store.removeEntity()` in a ticking system is acceptable per Hytale patterns, but CommandBuffer is preferred for consistency.
**Fix**: Evaluate whether `commandBuffer.removeEntity()` should be used instead. If `store.removeEntity()` is the established pattern in the codebase for ticking systems, document this deviation.

---

## Spec Compliance Summary

| Requirement | Status | Notes |
|-------------|--------|-------|
| FR-1: SummonerLinkComponent | **Pass** | Registered, attached with correct fields |
| FR-2: Definition Data Format | **Partial** | JSON loads correctly; missing stat overrides field (M-4) and load-time NPC validation (M-3) |
| FR-3: Spawning Service | **Pass** | Full spawn flow with cap/concentration validation, NPC spawn, callbacks |
| FR-4: Unsummoning (Disable) | **Pass** | onDisable callback enqueues despawn, processed same/next tick |
| FR-5: Re-Summoning (Enable) | **Pass** | onEnable callback enqueues respawn at summoner position |
| FR-6: Stat Bridging | **Pass** | 6 of 6 combat stats bridged; duration and max-minions correctly excluded |
| FR-7: Max Minions Cap | **Partial** | Spawn-time check works; reactive cap enforcement missing (C-1) |
| FR-8: Voluntary Release | **Pass** | `unsummon()` releases concentration fully |
| FR-9: HUD Breakpoints | **Pass** | Segmented bar with priority ordering, disabled styling |
| FR-10: HUD Regen Rate | **Pass** | Wisdom-based calculation displayed, hidden when <= 0.01 |
| FR-11: Death Handling | **Pass** | Full release, summoner notification, tracker cleanup |
| FR-12: Session Persistence | **Pass** | Disconnect despawns, reconnect scans and re-summons |
| NFR-1: Performance | **Partial** | Duration timers use wall-clock (M-2); otherwise efficient |
| NFR-2: Data-Driven | **Partial** | JSON definitions correct; hardcoded resource paths (m-5), missing stat overrides (M-4) |
| NFR-3: Localization | **Pass** | All 9 keys use `Message.translation()` with named parameters |
| NFR-4: ECS Compliance | **Pass** | Components are pure data, systems contain logic, CommandBuffer usage acceptable |
| NFR-5: Observability | **Pass** | FINE/INFO/WARNING logging at appropriate levels |

## ECS Checklist

- [x] Components are pure data (no logic)
- [x] Components implement `Component<EntityStore>`
- [x] Components have default constructors and `clone()` methods
- [x] Components have no persistence codec (transient — correct per spec)
- [x] Systems contain all logic
- [x] `CommandBuffer` available where needed (RefChangeSystem)
- [x] `Ref<EntityStore>` used for entity references
- [x] `Store<EntityStore>` used for component access
- [x] Queries used to filter entities
- [x] Components registered in `setup()` phase
- [x] Systems registered properly with dependencies

## Data-Driven Checklist

- [x] No hard-coded minion type IDs, costs, or behaviors in Java code
- [x] JSON files placed under `src/main/resources/Server/Hyforged/Minions/`
- [x] JSON structure follows existing patterns
- [x] No enums used for data from JSON
- [ ] Stat overrides defined in JSON (missing — M-4)
- [ ] Resource discovery is automatic (hardcoded paths — m-5)

## Localization Checklist

- [x] All user-facing text uses `Message.translation()`
- [x] 9 translation keys in `Server/Languages/en-US/minion.lang`
- [x] No Unicode characters in user-facing text
- [x] `fallback.lang` not modified

## Plan Compliance

- [x] Phase 1: Data Layer — Complete
- [x] Phase 2: Core Service — Complete (minus enforceCapReduction)
- [x] Phase 3: Stat Bridge — Complete
- [x] Phase 4: Death System — Complete
- [x] Phase 5: Reconnect — Complete
- [x] Phase 6: HUD — Complete
- [x] Phase 7: Integration & Testing — Partial (in-game testing pending)
- [ ] `enforceCapReduction` — Deferred in Phase 2, never completed in subsequent phases

## Notes

- Build: BUILD SUCCESS, 0 compile errors in hyforged source
- Tests: 1277/1277 pass (26 new)
- The implementation is well-structured with clear separation of concerns between the service, systems, and components
- Thread-safety via ConcurrentLinkedQueue is appropriate for the callback-to-tick-thread pattern
- The stale ref scanner (every 200 ticks) is a good defensive measure

---

## Remediation Required

### C-1 — Reactive Cap Enforcement Missing
- **File**: `MinionSummonService.java`
- **Issue**: FR-7 reactive cap enforcement (`enforceCapReduction`) is completely unimplemented
- **Fix**: Add a method that checks active minion count vs current `max-minions` stat and enqueues despawns for lowest-priority minions exceeding the cap. Wire via stat change listener or periodic check in `processTick()`.

### M-1 — TOCTOU Race in Summon Flow
- **File**: `MinionSummonService.java`, `performSpawn()` method
- **Issue**: No re-validation of concentration availability between enqueue and actual spawn
- **Fix**: Add concentration re-check before `reserveConcentration()` in `performSpawn()`. Abort spawn if insufficient.

### M-2 — Wall-Clock Duration Timers
- **File**: `MinionSummonService.java`, lines 700–702 and `checkDurationTimers()`
- **Issue**: Uses `System.currentTimeMillis()` instead of game ticks for duration tracking
- **Fix**: Switch to a tick counter. Increment in `processTick()`, convert base duration seconds to ticks (seconds * 20).

### M-3 — Missing NPC Template Load-Time Validation
- **File**: `MinionDefinitionRegistry.java`, `loadFromResources()` method
- **Issue**: NPC template names not validated against `NPCPlugin` at load time per FR-2
- **Fix**: After parsing, call `NPCPlugin.get().getIndex(npcTemplate)` and log WARNING if invalid. Optionally skip registration.

### M-4 — Missing Stat Overrides in MinionDefinition
- **File**: `MinionDefinition.java`
- **Issue**: Spec FR-2 requires a stat overrides field; it's absent from the data model and JSON parser
- **Fix**: Add `Map<String, Integer> statOverrides` field, parse from JSON, and apply during stat bridge propagation. Or update spec to explicitly defer this.
