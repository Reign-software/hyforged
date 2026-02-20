# Review: Stat Integration Audit — 2026-02-18 (Follow-up)

## Review Metadata
- Reviewer: GitHub Copilot (reviewer mode)
- Scope: Targeted re-review of 6 Major + 1 Minor findings from `review-001`
- Spec Version: `stat-integration-audit.spec.md` (2026-02-18)
- Plan Version: `stat-integration-audit.plan.md` (2026-02-18)
- Overall Status: **Pass**

---

## Summary

All seven findings from `review-001` (M-1 through M-6 plus one Minor) are fully resolved. The
remediation is clean, follows ECS patterns, and introduces no regressions in the files reviewed.
One new Minor finding is noted in `HyforgedKnockbackSystem` (direct store mutation instead of
`CommandBuffer`), which is non-blocking.

---

## Findings

### Critical

None.

---

### Major

None.

---

### Minor

#### N-M1 — `HyforgedKnockbackSystem`: `store.ensureAndGetComponent` Used Instead of `CommandBuffer`

**File:** [HyforgedKnockbackSystem.java](src/main/java/reign/software/hyforged/combat/HyforgedKnockbackSystem.java#L207-L210)

**Issue:** When applying knockback the system calls:
```java
KnockbackComponent knockbackComponent = store.ensureAndGetComponent(
        defenderRef, KnockbackComponent.getComponentType());
```
`ensureAndGetComponent` creates a component on the store if one is not present — a direct store
mutation. Per the project ECS guidelines: "Use `CommandBuffer` for entity/component changes instead
of mutating the store directly (thread safety + ordering)." The `commandBuffer` argument is
available in `handle()` but is not used for this operation.

**Severity Rationale:** Non-blocking because the system runs on the main game thread inside
`DamageModule.inspectDamageGroup` (no concurrent writers) and `KnockbackComponent` is Hytale's own
API designed for this use case. Functional correctness is unaffected.

**Suggested Fix:** If `CommandBuffer` exposes a path for `ensureAndGetComponent` semantics, prefer
that. Otherwise, document why direct store access is intentional here (i.e., Hytale's knockback API
is designed for in-situ mutation) with a brief inline comment so future reviewers don't flag it.

---

## Resolved Findings (from review-001)

### M-1 — `HyforgedOnHitRecoverySystem`: Inverted `PIPELINE_PROCESSED` Guard — **RESOLVED**

**Verification:** [HyforgedOnHitRecoverySystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedOnHitRecoverySystem.java#L107-L110)

Guard is now:
```java
Boolean pipelineProcessed = damage.getIfPresentMetaObject(CombatMeta.PIPELINE_PROCESSED);
if (pipelineProcessed != null && pipelineProcessed) {
    return;
}
```
This matches the standard pattern used by all other Hyforged pipeline systems. Native Hytale melee
damage (no flag set) now correctly flows through on-hit recovery; `CombatService` events (flag =
`true`) are correctly skipped. `life-on-hit-flat` and `mana-on-hit-flat` are live stats.

---

### M-2 — `HyforgedRegenSystem`: Missing Percent-Based Regen Stats — **RESOLVED**

**Verification:** [HyforgedRegenSystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedRegenSystem.java)

Both `health-regen-percent-bps` and `mana-regen-percent-bps` are fully implemented:

- Indices `healthRegenPercentBpsIndex` and `manaRegenPercentBpsIndex` are declared and cached in
  `ensureIndicesCached()`.
- A dedicated `applyPercentRegen()` method handles the percent path. Formula is
  `maxAmount * regenBps / BPS_100_PERCENT * dt`, which correctly scales by max value and delta time.
- `healing-effectiveness-bps` is applied to HP percent regen only (non-zero `healingEffectivenessBps`
  passed for health, `0` passed for mana). This matches FR-5 semantics.
- Javadoc on the class lists all six supported stats including both new ones.

---

### M-3 — `HyforgedRegenSystem`: Hardcoded Regen Interval — **RESOLVED**

**Verification:** [HyforgedRegenSystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedRegenSystem.java#L64)
and [HyforgedConfig.java](src/main/java/reign/software/hyforged/HyforgedConfig.java#L34-L36)

Constructor is:
```java
super(HyforgedConfig.get().getRegenIntervalTicks() / 20.0f);
```
No literal `1.0f` remains. `HyforgedConfig` exposes `private int regenIntervalTicks = 20` with
both `getRegenIntervalTicks()` and `setRegenIntervalTicks()`, satisfying the requirement that the
value be configurable outside source code. Division by `20.0f` correctly converts ticks to seconds
for `DelayedEntitySystem`. Note: the interval is baked at construction time (inherent `DelayedEntitySystem`
limitation) — a restart or server reload changes the effective interval; runtime hot-change is still
not possible, but this was accepted as the minimum fix scope.

---

### M-4 — `HyforgedBridgeSystem`: Attack Speed Bridge (FR-4) Absent — **RESOLVED**

**Verification:** [HyforgedBridgeSystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedBridgeSystem.java#L91-L104)
and [`initializeStatIndices()`](src/main/java/reign/software/hyforged/stats/system/HyforgedBridgeSystem.java#L207)

`attackSpeedBpsIndex` is:
1. Declared as `@SuppressWarnings("unused") private int attackSpeedBpsIndex = -1;`
2. Cached in `initializeStatIndices()` via `registry.getIndex(StatId.hyforged("attack-speed-bps"))`
3. Accompanied by a detailed Javadoc TODO block explaining the blocker (`DefaultEntityStatTypes` has
   no attack speed entry in the current server version) and providing the implementation pattern for
   when Hytale adds native support.

The plan step 4.2 / FR-4 blocker is properly documented in-source with no silent omission.

---

### M-5 — `HyforgedDamageBonusSystem`: Stun Duration Unwired — **RESOLVED**

**Verification:** [HyforgedDamageBonusSystem.java line 70](src/main/java/reign/software/hyforged/combat/HyforgedDamageBonusSystem.java#L70)
and [line 77](src/main/java/reign/software/hyforged/combat/HyforgedDamageBonusSystem.java#L77)
and [line 100](src/main/java/reign/software/hyforged/combat/HyforgedDamageBonusSystem.java#L100)
and [line 272](src/main/java/reign/software/hyforged/combat/HyforgedDamageBonusSystem.java#L272)

- `STUN_DURATION = StatId.hyforged("stun-duration-bps")` constant declared.
- `stunDurationIndex = -1` field exists (tagged "used by future stun system" comment).
- `stunDurationIndex` populated in `ensureIndicesCached()`.
- TODO comment at the field-declaration site explicitly states: "Wire stun-duration-bps into a
  dedicated stun system once a stun application [system is implemented]."

The index is ready; the deferred wiring is accurately documented.

---

### M-6 — Knockback Not Implemented (FR-12) — **RESOLVED**

**Verification:** [HyforgedKnockbackSystem.java](src/main/java/reign/software/hyforged/combat/HyforgedKnockbackSystem.java)
and [HyforgedPlugin.java line 859](src/main/java/reign/software/hyforged/HyforgedPlugin.java#L859)

`HyforgedKnockbackSystem` is a complete, well-structured `DamageEventSystem`:

- Runs in `DamageModule.inspectDamageGroup`, ordered `BEFORE DamageSystems.EntityUIEvents`.
- `PIPELINE_PROCESSED` guard follows the standard pattern (skip CombatService events).
- `knockback-chance-bps` rolled via `CombatRandom.rollChance(knockbackChanceBps)`.
- `knockback-distance-bps` scales attacker side; `knockback-resistance-bps` reduces defender side.
- Formula: `base * (1 + distanceBps/10000) * (1 - resistanceBps/10000)` applied correctly.
- `baseKnockbackVelocity` and `baseKnockbackDurationSeconds` sourced from `HyforgedConfig` — no
  hardcoded game values.
- Direction computed from attacker→defender horizontal vector with safe degenerate case (exact
  overlap → +X).
- `KnockbackComponent` API used for velocity dispatch with `ChangeVelocityType.Add`.
- All three stat indices cached lazily; `StatId` constants declared at class level.
- Registered at `HyforgedPlugin.java:859` with an `FINE` log line.

---

### Minor (review-001) — `HyforgedMinionStatBridgeSystem` Missing `minion-duration-bps` — **RESOLVED**

**Verification:** [HyforgedMinionStatBridgeSystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedMinionStatBridgeSystem.java)

`MINION_DURATION_BPS = StatId.hyforged("minion-duration-bps")` declared as a static constant;
`minionDurationIndex = -1` field present; cached in `ensureIndicesCached()` and included in the
`FINE`-level log line listing all eight indices. The Javadoc TODO also explicitly lists
`hyforged:minion-duration-bps` in the future wiring checklist.

---

## Notes

- Build: Stated as passing with 365 source files and 0 warnings. Spot-check confirms pattern
  consistency with existing codebase — no obvious compiler red flags in reviewed files.
- `HyforgedConfig.regenIntervalTicks` (M-3): runtime hot-change of regen interval is not supported
  (interval baked into `DelayedEntitySystem` at construction). This is a known limitation accepted
  as out of scope. If runtime config reloads become a requirement, `HyforgedRegenSystem` will need
  to be recreated and re-registered.
- `HyforgedKnockbackSystem` self-damage guard (skip if `attackerRef.equals(defenderRef)`) is a
  clean addition not explicitly required by spec but obviously correct.
