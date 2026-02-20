# Review: Minion Concentration System — Follow-Up (2026-02-19)

## Review Metadata
- Reviewer: GitHub Copilot (Automated)
- Scope: All files modified to address review #001 findings (C-1, M-1–M-4, m-1–m-6)
- Spec Version: minion-concentration-system.spec.md (2026-02-19)
- Plan Version: minion-concentration-system.plan.md (Phases 1–7, all complete)
- Previous Review: minion-concentration-system.review-001.md (1 Critical, 4 Major, 6 Minor)
- Overall Status: **Pass**

## Summary

All 11 findings from review #001 have been addressed. The fixes are correct, well-implemented, and introduce no new Critical or Major issues. Build passes with zero compilation errors. All 34 minion/concentration unit tests pass (28 minion + 6 breakpoint). One new minor observation is noted but is non-blocking.

---

## Review #001 Finding Verification

### C-1: FR-7 Reactive Cap Enforcement — RESOLVED

**File**: `MinionSummonService.java` (lines 875–945)

`enforceCapReduction(Store<EntityStore>)` is implemented and called every 20 ticks from `processTick()`. The method:
- Iterates `activeSummoners` and reads current `max-minions` stat via `StatAccessor.getStatValueInt()`
- Skips summoners within cap (`currentCount <= maxMinions`)
- Collects minion ability IDs and sorts ascending by priority (lowest-priority despawned first)
- Enqueues despawn requests with `releaseConcentration=true` for excess minions
- Logs at INFO level per spec NFR-5

The 20-tick interval (~1 second) is a reasonable performance/reactivity tradeoff.

**Verdict**: Correctly implements FR-7 reactive enforcement per the spec acceptance criterion.

---

### M-1: TOCTOU Race in Summon Flow — RESOLVED

**File**: `MinionSummonService.java`, `performSpawn()` (lines ~680–700)

Two re-validation checks are now present at execution time, before `reserveConcentration()`:
1. Concentration check: `if (adjustedCost > 0 && availableConcentration < adjustedCost)` — aborts spawn with FINE log
2. Max-minions cap check: `if (maxMinions > 0 && currentCount >= maxMinions)` — aborts spawn with FINE log

Both use freshly-read data from the store at execution time, eliminating the check-then-act race between enqueue and processing.

**Verdict**: TOCTOU race eliminated.

---

### M-2: Wall-Clock Duration Timers — RESOLVED

**File**: `MinionSummonService.java`

- `currentTickCounter` (long, line ~91) incremented as the first operation in `processTick()` (line ~420)
- `computeEffectiveDurationTicks()` converts `baseDurationSeconds * 20L` (ticks per second)
- `durationTimers` stores tick-based deadlines: `currentTickCounter + effectiveDurationTicks`
- `checkDurationTimers()` compares: `if (now >= entry.getValue())` using `currentTickCounter`
- No remaining references to `System.currentTimeMillis()` for duration logic (only used in `SummonerLinkComponent.summonTimestamp` for informational metadata, which is correct)

**Verdict**: Duration timers are fully tick-based per NFR-1.

---

### M-3: NPC Template Load-Time Validation — RESOLVED

**File**: `MinionDefinitionRegistry.java` (lines 245–274), `HyforgedPlugin.java` (line 347)

- `validateTemplates()` iterates all registered definitions and calls `NPCPlugin.get().getIndex(npcTemplate)` for each
- Logs WARNING for any definition whose template returns `index < 0`
- Returns failure count; handles NPCPlugin unavailability gracefully
- Called from `HyforgedPlugin` after `loadFromIndex()` with appropriate sequencing

**Verdict**: FR-2 load-time NPC template validation is implemented.

---

### M-4: Stat Overrides in MinionDefinition — RESOLVED

**Files**: `MinionDefinition.java`, `MinionDefinitionRegistry.java`, `HyforgedMinionStatBridgeSystem.java`, JSON files

- `MinionDefinition` has `Map<String, Integer> statOverrides` field (immutable via `Map.copyOf()`)
- Backward-compatible constructor (without statOverrides) delegates with empty map
- `parseDefinition()` reads `StatOverrides` as a JSON object map
- `HyforgedMinionStatBridgeSystem.applyStatOverrides()` reads definition from registry and applies as INCREASED modifiers with distinct source key `"hyforged:minion-bridge:override:{statId}"`
- Called from `propagateStats()` after standard bridge modifiers
- `SkeletonWarrior.json` includes example: `"hyforged:minion-damage-bps": 500`
- `KweebecSapling.json` includes empty `"StatOverrides": {}`

**Verdict**: FR-2 stat overrides fully implemented in data model, parser, JSON, and bridge system.

---

### m-1: Breakpoint Hash Includes Ability ID — RESOLVED

**File**: `ResourceStatsHudSystem.java` (lines 191–203)

`computeBreakpointHash()` now includes `hash = 31 * hash + bp.abilityId().hashCode()` in addition to cost and enabled state. Reordered abilities with identical costs will now produce different hashes, triggering HUD updates.

**Verdict**: Fixed.

---

### m-2: summonerRef Fast-Path — RESOLVED

**File**: `MinionSummonService.java`, `performSpawn()` (lines ~635–639)

```
Ref<EntityStore> summonerRef = request.summonerRef();
if (summonerRef == null || !summonerRef.isValid()) {
    summonerRef = store.getExternalData().getRefFromUUID(summonerUuid);
}
```

The cached ref is tried first with a validity check, then falls back to UUID resolution. The `SpawnRequest` record's `summonerRef` field is no longer dead.

**Verdict**: Fixed.

---

### m-3: parseMinionTypeId Placement — RESOLVED

**Files**: `MinionSummonService.java`, `MinionReconnectHandler.java`

- Core `parseMinionTypeId(String)` logic is now a package-private static method in `MinionSummonService` (lines 853–872)
- `MinionReconnectHandler` retains a `@Deprecated` delegate that calls `MinionSummonService.parseMinionTypeId()` directly
- Both classes are in the same package (`reign.software.hyforged.minion`), so package-private access works correctly
- `MinionSummonServiceTest` tests the method in the correct location

**Verdict**: Fixed. The deprecated delegate is a reasonable transition pattern.

---

### m-4: Tracker List Comment — RESOLVED

**File**: `MinionTrackerComponent.java` (lines 24–28)

Design comment present:
```
// Design note (m-4): activeMinions uses List<Ref<EntityStore>> per ability ID
// rather than a single Ref. This supports future multi-minion-per-ability scenarios
// (e.g., an ability that summons 2+ copies). For current single-minion abilities,
// the list always contains 0 or 1 entries.
```

**Verdict**: Fixed.

---

### m-5: Hardcoded Paths — RESOLVED

**Files**: `MinionIndex.json`, `MinionDefinitionRegistry.java`, `HyforgedPlugin.java`

- `Server/Hyforged/Minions/MinionIndex.json` created with a `Definitions` array listing resource paths
- `loadFromIndex(String indexPath)` method reads the index JSON and delegates to `loadFromResources()`
- `HyforgedPlugin` calls `loadFromIndex("Server/Hyforged/Minions/MinionIndex.json")` at line 343
- Adding new minion types requires editing only `MinionIndex.json` — no code changes needed

**Verdict**: Fixed. Resource discovery is now data-driven via the index manifest.

---

### m-6: store.removeEntity Comment — RESOLVED

**File**: `MinionSummonService.java`, `performDespawn()` (lines 773–776)

```java
// Remove the minion entity using store.removeEntity() directly.
// This follows the established Hytale pattern for ticking systems where
// entity removal is performed on the world tick thread (processTick).
// CommandBuffer is not used because we are already on the tick thread.
```

**Verdict**: Fixed. Pattern documented.

---

## New Findings

### Minor

#### m-NEW-1: MinionReconnectHandler Retains Deprecated parseMinionTypeId Delegate

**File**: `MinionReconnectHandler.java` (lines 226–229)

The `@Deprecated` delegate `parseMinionTypeId()` is retained in `MinionReconnectHandler`. While `processReconnect()` calls this local delegate (which delegates to `MinionSummonService`), it would be cleaner to call `MinionSummonService.parseMinionTypeId()` directly and remove the deprecated wrapper entirely. Since both classes are in the same package, the package-private method is directly accessible.

**Severity**: Minor — no functional impact, the delegation works correctly. The `@Deprecated` annotation prevents external callers from using the old location.

**Suggested Fix**: In a future cleanup pass, replace `parseMinionTypeId(abilityId)` calls in `MinionReconnectHandler.processReconnect()` with `MinionSummonService.parseMinionTypeId(abilityId)` and remove the deprecated wrapper.

---

## Spec Compliance Summary

| Requirement | Status | Notes |
|-------------|--------|-------|
| FR-1: SummonerLinkComponent | **Pass** | Implemented and registered |
| FR-2: Definition Data Format | **Pass** | JSON with stat overrides, load-time NPC validation (M-3, M-4 resolved) |
| FR-3: Spawning Service | **Pass** | Full spawn flow with TOCTOU guard (M-1 resolved) |
| FR-4: Unsummoning (Disable) | **Pass** | onDisable enqueues despawn |
| FR-5: Re-Summoning (Enable) | **Pass** | onEnable enqueues respawn |
| FR-6: Stat Bridging | **Pass** | 6 combat stats + stat overrides from definition |
| FR-7: Max Minions Cap | **Pass** | Spawn-time + reactive enforcement (C-1 resolved) |
| FR-8: Voluntary Release | **Pass** | Full concentration release |
| FR-9: HUD Breakpoints | **Pass** | Segmented bar with abilityId hash (m-1 resolved) |
| FR-10: HUD Regen Rate | **Pass** | Wisdom-based calculation displayed |
| FR-11: Death Handling | **Pass** | Full release, notification, cleanup |
| FR-12: Session Persistence | **Pass** | Disconnect despawns, reconnect re-summons |
| NFR-1: Performance | **Pass** | Tick-based durations (M-2 resolved), reactive cap every 20 ticks |
| NFR-2: Data-Driven | **Pass** | Index manifest, stat overrides from JSON (m-5, M-4 resolved) |
| NFR-3: Localization | **Pass** | All 9 keys use Message.translation() with named params |
| NFR-4: ECS Compliance | **Pass** | Components pure data, systems contain logic, store.removeEntity documented (m-6) |
| NFR-5: Observability | **Pass** | FINE/INFO/WARNING logging at appropriate levels |

## Build & Test Verification

- **Build**: BUILD SUCCESS — zero compilation errors, zero warnings (POM parent warnings are expected/benign)
- **Tests**: 34 pass (MinionSummonServiceTest, MinionDefinitionRegistryTest, SummonerLinkComponentTest: 28; ConcentrationBreakpointTest: 6)
- **Static Analysis**: `get_errors` returns no errors across all source files

## ECS Checklist

- [x] Components are pure data (no logic)
- [x] Components implement `Component<EntityStore>` with default + copy constructors and `clone()`
- [x] Systems contain all logic
- [x] `Ref<EntityStore>` used for entity references
- [x] `Store<EntityStore>` used for component access
- [x] Queries used to filter entities
- [x] Component and system registration in plugin setup
- [x] store.removeEntity() usage on tick thread documented

## Data-Driven Checklist

- [x] No hard-coded minion type IDs, costs, or behaviors in Java code
- [x] JSON files under `Server/Hyforged/Minions/`
- [x] Stat overrides defined in JSON
- [x] Resource discovery via MinionIndex.json manifest
- [x] No enums for JSON-sourced data

## Localization Checklist

- [x] All user-facing text uses `Message.translation()` with named params
- [x] 9 translation keys in `Server/Languages/en-US/minion.lang`
- [x] No Unicode characters
- [x] `fallback.lang` not modified

## Notes

- All 11 findings from review #001 have been verified as resolved
- The only new finding (m-NEW-1) is a non-blocking cleanup item
- End-to-end in-game testing (Plan step 7.4) still requires manual verification
- Test count increased from 26 to 34 (additional tests likely added alongside fixes)
