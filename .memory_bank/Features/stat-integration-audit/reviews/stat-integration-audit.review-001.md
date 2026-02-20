# Review: Stat Integration Audit — 2026-02-18

## Review Metadata
- Reviewer: GitHub Copilot (reviewer mode)
- Scope: All 6 phases of stat-integration-audit implementation
- Spec Version: `stat-integration-audit.spec.md` (2026-02-18)
- Plan Version: `stat-integration-audit.plan.md` (2026-02-18)
- Overall Status: **Needs Changes**

---

## Summary

The implementation delivers the bulk of its intended scope: Phase 1 (critical bug fix), Phase 2 (damage bonus/taken systems), Phase 3 (regen/on-hit/movement speed), Phase 5 (XP, attribute-all), and Phase 6 (stubs) are implemented with solid ECS patterns, correct data-driven registry usage, and clean code. All new systems are registered in `HyforgedPlugin`. The inheritance-chain walking in `DamageTypeExtensionRegistry` is well-designed, the anti-loop guard in `HyforgedAttributeAllSystem` is correct, and the movement-speed baseline anti-drift mechanism (`defaultSettings.baseSpeed` multiplication) is sound.

However, **seven blocking issues** were found:

1. `HyforgedOnHitRecoverySystem` has an **inverted** `PIPELINE_PROCESSED` guard — the only inspect-group system that requires the flag to be true, causing it to never fire during native Hytale combat events (the primary scenario).
2. `HyforgedRegenSystem` omits `health-regen-percent-bps` and `mana-regen-percent-bps`, two of five FR-5 stats required by the spec and plan step 3.3.
3. The regen interval is hardcoded as `1.0f` in violation of the spec NFR ("all thresholds, frequencies must come from JSON, not hardcoded constants") and plan step 3.3 ("Interval must be read from plugin config/JSON").
4. FR-4 (attack speed bridge) is entirely absent with no blocker documentation in the plan.
5. FR-11 (stun duration) stat index is cached but never wired to the existing stun system; plan step 4.4 explicitly required this wiring.
6. FR-12 (knockback chance/distance) is entirely unimplemented with no deferral note.
7. `HyforgedMinionStatBridgeSystem` stub is missing `minion-duration-bps` from its index cache (one of seven FR-21 required indices).

---

## Findings

### Critical

None.

---

### Major

#### M-1 — `HyforgedOnHitRecoverySystem`: Inverted `PIPELINE_PROCESSED` Guard

**File:** [HyforgedOnHitRecoverySystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedOnHitRecoverySystem.java#L113-L116)

**Issue:** The guard logic is:
```java
Boolean pipelineProcessed = damage.getIfPresentMetaObject(CombatMeta.PIPELINE_PROCESSED);
if (pipelineProcessed == null || !pipelineProcessed) {
    return; // Only process damage from Hyforged's pipeline
}
```
Every other Hyforged pipeline system (bonus, reduction, leech, spellblock, dodge, crit, auto-block) uses the **opposite** pattern:
```java
if (pipelineProcessed != null && pipelineProcessed) { return; } // skip CombatService events
```
`CombatMeta.PIPELINE_PROCESSED` is set to `true` by `CombatServiceImpl` line 387 on programmatically generated events. Native Hytale melee attacks never set this flag. The current guard means:
- **Native combat** (no flag set): on-hit recovery **never fires** (returns because flag is null).
- **CombatService events** (flag = true): on-hit recovery fires, but the damage calculation systems were all skipped for this same event.

Result: `life-on-hit-flat` and `mana-on-hit-flat` are dead stats in normal gameplay.

**Fix:** Change to the standard guard:
```java
Boolean pipelineProcessed = damage.getIfPresentMetaObject(CombatMeta.PIPELINE_PROCESSED);
if (pipelineProcessed != null && pipelineProcessed) {
    return; // Skip CombatService-handled events
}
```

---

#### M-2 — `HyforgedRegenSystem`: Missing `health-regen-percent-bps` and `mana-regen-percent-bps`

**File:** [HyforgedRegenSystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedRegenSystem.java)

**Issue:** FR-5 (spec) and plan step 3.3 both require five regen stats. Only four are implemented (flat variants for HP, mana, stamina, and an unspecified `energy-regen-flat`). The following two are entirely absent:
- `hyforged:health-regen-percent-bps` → `(maxHealth * bps / 10000.0)` HP per tick
- `hyforged:mana-regen-percent-bps` → `(maxMana * bps / 10000.0)` mana per tick

Plan step 3.3 explicitly states both:
> `hyforged:health-regen-percent-bps` → % of max HP per tick  
> `hyforged:mana-regen-percent-bps` → % of max mana per tick

No deferral note exists for these variants. They are defined stat IDs that will accumulate modifiers with zero gameplay effect.

**Fix:** Add percent-based regen paths in `tick()` alongside the flat paths. Requires reading `hpStat.getMax()` from the `EntityStatMap` before applying:
```java
int percentBps = hyforgedStats.getCachedValue(healthRegenPercentIndex);
if (percentBps > 0) {
    EntityStatValue hpStat = entityStatMap.get(DefaultEntityStatTypes.getHealth());
    if (hpStat != null && hpStat.getMax() > 0) {
        float regenAmount = hpStat.getMax() * percentBps / (float) BPS_100_PERCENT;
        // apply healing-effectiveness multiplier then addStatValue...
    }
}
```
Cache `healthRegenPercentIndex` and `manaRegenPercentIndex` in `ensureIndicesCached()`.

---

#### M-3 — `HyforgedRegenSystem`: Hardcoded Regen Interval

**File:** [HyforgedRegenSystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedRegenSystem.java#L37)

**Issue:** `private static final float REGEN_INTERVAL_SECONDS = 1.0f;` violates:
- Spec NFR: "All thresholds, frequencies, and multiplier stacking rules must come from JSON, not hard-coded constants."
- Plan step 3.3: "Interval must be read from plugin config/JSON — not hardcoded."

**Fix:** Move the interval to a plugin config entry or a JSON data file. At minimum, read it from `HyforgedConfig`:
```java
// HyforgedConfig
private float regenIntervalSeconds = 1.0f; // settable via config reload

// HyforgedRegenSystem
public HyforgedRegenSystem() {
    super(HyforgedConfig.get().getRegenIntervalSeconds());
    ...
}
```
Note: `DelayedEntitySystem` may only read the interval at construction time, so if true runtime config is needed, a reload mechanism is also required.

---

#### M-4 — FR-4: Attack Speed Bridge Not Implemented, No Blocker Documented

**File:** None (missing system)

**Issue:** `hyforged:attack-speed-bps` has no consuming system. Plan step 4.1 says: "If no suitable Hytale stat exists, document as a blocker in this plan and skip." Neither a `HyforgedAttackSpeedSystem` was created nor a blocker note was added to the plan. The stat accumulates modifiers with zero effect.

**Fix:** One of:
- Implement `HyforgedAttackSpeedSystem` by researching `DefaultEntityStatTypes` in `lib/hytale-server/` for an attack interval stat type.
- Or add a blocker note to the plan: "Plan step 4.1 deferred — `DefaultEntityStatTypes` does not expose an attack interval stat accessible via `EntityStatMap`. Stat cached as index only; mechanic blocked until Hytale API expands."

---

#### M-5 — FR-11: Stun Duration Not Wired, Only Cached

**File:** [HyforgedDamageBonusSystem.java](src/main/java/reign/software/hyforged/combat/HyforgedDamageBonusSystem.java#L70-L77)

**Issue:** `stun-duration-bps` index is cached in `HyforgedDamageBonusSystem` with a TODO comment but never applied. Plan step 4.4 explicitly states:
> "Locate the existing stun application code (search for `stun-threshold-bps` and `stun-avoidance-bps` consumers). When stun is applied, multiply the effective stun duration by `(1 + stun-duration-bps / 10000.0)` from the attacker's stats."

The existing stun system does consume `stun-threshold-bps` and `stun-avoidance-bps`. Wiring `stun-duration-bps` into the stun application path is a required deliverable for Phase 4.

**Fix:** Find the stun effect application call site (search for `stun-threshold-bps` in Java source), read attacker's `stun-duration-bps`, and multiply the applied stun duration by `(1 + bps / 10000.0)`.

---

#### M-6 — FR-12: Knockback Chance and Distance Not Implemented, No Deferral Note

**File:** None (missing implementation)

**Issue:** `knockback-chance-bps` and `knockback-distance-bps` appear nowhere in the Java source after this implementation. Plan step 4.5 required either:
- Extending the existing knockback code path, or
- Creating `HyforgedKnockbackSystem`.

No deferral note was added to the plan. The DD confirms `knockback-resistance-bps` is already read by a system, implying a knockback framework exists to extend.

**Fix:** One of:
- Implement knockback chance roll and distance scaling in the existing knockback pathway.
- Add a deferral note to the plan with justification.

---

### Minor

#### m-1 — `HyforgedMinionStatBridgeSystem` Missing `minion-duration-bps` Index

**File:** [HyforgedMinionStatBridgeSystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedMinionStatBridgeSystem.java#L62-L67)

**Issue:** Plan step 5.5 and FR-21 list seven minion stats to cache. The stub caches six (`damage`, `life`, `speed`, `accuracy`, `attack-speed`, `crit-chance`) plus `max-minions`, but omits `minion-duration-bps`. Plan exit criteria: "all 7 minion stat indices cached."

**Fix:** Add to `HyforgedMinionStatBridgeSystem`:
```java
private static final StatId MINION_DURATION_BPS = StatId.hyforged("minion-duration-bps");
private int minionDurationIndex = -1;
// ... cache in ensureIndicesCached()
minionDurationIndex = registry.getIndex(MINION_DURATION_BPS);
```

---

#### m-2 — `HyforgedMinionStatBridgeSystem.ensureIndicesCached()` Never Called at Runtime

**File:** [HyforgedMinionStatBridgeSystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedMinionStatBridgeSystem.java#L89-L91)

**Issue:** `tick()` is the only caller of `ensureIndicesCached()`, but `tick()` is unreachable because `getQuery()` returns `Query.and(X, Query.not(X))` (always empty). The stat indices are never actually cached at runtime despite the summary claiming otherwise. The benefit of "pre-caching indices so they are ready when the summoning system is added" is not realized.

**Fix:** Call `ensureIndicesCached()` from `HyforgedPlugin.start()` or register it to be triggered after the stat registry is frozen, so indices are populated at server start:
```java
// In HyforgedPlugin.start()
HyforgedMinionStatBridgeSystem.getInstance().ensureIndicesCached();
```
Requires converting the system to a singleton or storing the reference in the plugin.

---

#### m-3 — `HyforgedConfig` Not Backed by Hytale Config API

**File:** [HyforgedConfig.java](src/main/java/reign/software/hyforged/HyforgedConfig.java)

**Issue:** The plan step 6.5 mentions adding flags to "`HyforgedConfig` (or the existing plugin config class)." The implementation uses a plain singleton. The flags reset to `false` on every restart (intentionally for stubbed systems), but a server operator has no way to enable them persistently without code changes. The `hytale-plugin-config` skill documents a `Config` / `withConfig()` pattern for persistent, file-backed configuration.

**Impact:** Low — defaults of `false` are correct per spec; this is a convenience/operations concern, not a correctness issue. Monitor for complaint when operators want to test spell block.

**Fix (optional):** Migrate `HyforgedConfig` to the Hytale `Config` + `BuilderCodec` pattern so flags survive restarts. Or document the limitation in `.memory_bank/ADRs.md`.

---

#### m-4 — `energy-regen-flat` Not in Spec or Plan

**File:** [HyforgedRegenSystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedRegenSystem.java#L28)

**Issue:** `hyforged:energy-regen-flat` is implemented and documented but is not present in FR-5 or plan step 3.3. Scope creep, although benign.

**Recommendation:** Either add `energy-regen-flat` to FR-5 via spec amendment, or remove it from the regen system and add a comment that it was intentionally added as an undocumented extension.

---

#### m-5 — FR-2 Deferred Items Not Tracked in Plan Exit Criteria

**File:** [stat-integration-audit.plan.md](.memory_bank/Features/stat-integration-audit/stat-integration-audit.plan.md) (step 2.2 implementation note)

**Issue:** Plan step 2.2 documents the following as deferred with no follow-up tracking:
- Weapon-stance bonuses (`one-handed-damage-bps`, `two-handed-damage-bps`, `dual-wield-damage-bps`, `shield-damage-bps`, `unarmed-damage-bps`)
- Flat damage additions (`added-physical-damage-flat`, `added-fire-damage-flat`, `added-cold-damage-flat`, `added-lightning-damage-flat`, `added-chaos-damage-flat`)
- `ranged-damage-increased-bps`, `mine-damage-bps`, `attack-damage-increased-bps`, `spell-damage-increased-bps`

These are explicitly in FR-2 of the spec and represent ~14 unimplemented stat IDs that accumulate modifiers with zero effect. The deferral is plan-authorized for this phase, but no follow-up issue or plan phase tracks their completion.

**Fix:** Create a follow-up task or plan phase to implement weapon detection (via inventory slot query) and add the deferred bonuses. Update `stat-integration-audit.plan.md` with a Phase 7 stub or a "Remaining Work" section.

---

## Notes

### Verified Correct

- **Phase 1 (physical-power)**: No references to `StatId.hyforged("physical-power")` remain in the Java source. Fix confirmed.
- **Chaos/Bleed `Inherits` removal**: `collectDamageBonusStats()` uses `ext.inherits()` when an extension record exists, correctly stopping chain propagation for Bleed/Chaos. Resistance resolution is unaffected because both JSONs define `HyforgedResistanceStat` directly (`bleed-resistance-bps`, `chaos-resistance-bps`). No regression in `HyforgedDamageReductionSystem`.
- **`DamageTypeExtensionRegistry` chain walking**: `collectDamageBonusStats` and `collectDamageTakenStats` accumulate stats at **every** level of the parent chain (not first-match), correctly resulting in Fire damage applying both `fire-damage-increased-bps` and `elemental-damage-increased-bps`. Ailment stats use first-match resolution (`resolveAilmentStat`), which is correct (a damage type has one ailment type).
- **`HyforgedAttributeAllSystem` anti-loop guard**: The `MODIFIER_KEY` (`"hyforged:attribute-all"`) applied as `SourceType.BASE` on individual attribute stats is distinct from any modifier that reads back into `attribute-all`. The target attribute stats (strength, dexterity, etc.) have no reverse scaling path to `attribute-all`, so recomputation terminates. The perpetual-dirty guard (checking `existingModifierMatchesValue` before `upsertModifier`) correctly prevents dirty-marking on every tick.
- **Movement speed anti-drift**: `HyforgedBridgeSystem.bridgeMovementSpeed()` computes `settings.baseSpeed = defaultSettings.baseSpeed * multiplier` (relative to the default baseline), not an absolute override. Drift is prevented.
- **`healing-effectiveness-bps` consistency**: Applied in regen (HP only, per plan), on-hit recovery (HP only), on-kill (`OnKillResourceRecoverySystem`), leech (`HyforgedLeechSystem`), and the `HealingServiceImpl` (programmatic healing). Consistent across all four recovery pathways.
- **Config flags default false / Phase 6 zero-overhead**: Both `spellBlockEnabled` and `dodgeEnabled` default to `false`. `HyforgedSpellBlockSystem` and `HyforgedDodgeSystem` both short-circuit at the first line of `handle()`. No performance impact.
- **`HyforgedLootMultiplierSystem` zero-overhead**: `resolveLootSourcePlayer()` returns null, logging is gated on `quantityBps == 0 && rarityBps == 0`, and the main logic returns early for all item spawns. True zero-overhead aside from index caching.
- **All new systems registered**: `HyforgedDamageBonusSystem`, `HyforgedDamageTakenSystem`, `HyforgedRegenSystem`, `HyforgedOnHitRecoverySystem`, `HyforgedLeechSystem`, `HyforgedAttributeAllSystem`, `HyforgedMinionStatBridgeSystem`, `HyforgedLootMultiplierSystem`, `HyforgedSpellBlockSystem`, `HyforgedDodgeSystem` — all present in `HyforgedPlugin`.
- **`HyforgedSkillLevelSystem`**: Correctly implemented as a static helper (not a system). Not registered (correct per plan step 6.3). Lazy discovery with double-checked locking is thread-safe.
- **`CombatMeta.DOUBLE_DAMAGE` and `CombatMeta.DODGE_ROLLED` keys**: Both added to `CombatMeta.java` as required by Phase 4 and 6.
- **Culling strike target HP check**: `archetypeChunk.getComponent(index, EntityStatMap.getComponentType())` correctly reads the **defender's** HP (the entity being iterated in `DamageEventSystem`), not the attacker's. Logic is correct.
- **Not Verifiable — requires manual testing**: FR-2 fire damage bonus at +50% produces measurably ~50% more fire damage; FR-5 regen visible recovery; FR-7 leech recovery proportional to damage dealt; FR-10 double-damage and culling strike trigger correctly; FR-14 ailment per-element scaling.

---

## Remediation Required

### M-1 — Inverted PIPELINE_PROCESSED Guard in `HyforgedOnHitRecoverySystem`
- **File:** [HyforgedOnHitRecoverySystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedOnHitRecoverySystem.java#L113-L116)
- **Issue:** Guard `if (pipelineProcessed == null || !pipelineProcessed) return;` is the **opposite** of all other Hyforged pipeline systems. On-hit recovery never fires during native Hytale combat.
- **Fix:** Replace with `if (pipelineProcessed != null && pipelineProcessed) return;` to skip CombatService-handled events and process native events.

### M-2 — Missing Percent Regen Stats in `HyforgedRegenSystem`
- **File:** [HyforgedRegenSystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedRegenSystem.java)
- **Issue:** `health-regen-percent-bps` and `mana-regen-percent-bps` required by FR-5 and plan step 3.3 are not implemented.
- **Fix:** Add two new index fields (`healthRegenPercentIndex`, `manaRegenPercentIndex`), cache them in `ensureIndicesCached()`, and apply them in `tick()` by reading `EntityStatValue.getMax()` from the `EntityStatMap`.

### M-3 — Hardcoded Regen Interval
- **File:** [HyforgedRegenSystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedRegenSystem.java#L37)
- **Issue:** `REGEN_INTERVAL_SECONDS = 1.0f` is hardcoded; violates spec NFR and plan step 3.3.
- **Fix:** Move to `HyforgedConfig` or a JSON data file; read at construction time.

### M-4 — FR-4 Attack Speed: No Implementation and No Blocker Documentation
- **File:** Plan document
- **Issue:** No `HyforgedAttackSpeedSystem` exists; no blocker note in plan.
- **Fix:** Research `DefaultEntityStatTypes` in `lib/hytale-server/` and either implement or add an explicit plan blocker note explaining why.

### M-5 — FR-11 Stun Duration Not Wired
- **File:** [HyforgedDamageBonusSystem.java](src/main/java/reign/software/hyforged/combat/HyforgedDamageBonusSystem.java#L70-L77) + stun application site
- **Issue:** Stat index cached but never applied; plan step 4.4 required wiring.
- **Fix:** Locate `stun-threshold-bps` / `stun-avoidance-bps` consumer code; add attacker `stun-duration-bps` multiplication at stun application.

### M-6 — FR-12 Knockback Chance/Distance Not Implemented
- **File:** None (missing)
- **Issue:** Not implemented, not deferred.
- **Fix:** Implement channel logic in existing knockback pathway or create `HyforgedKnockbackSystem`; or add deferral note with justification to the plan.

### m-1 — `HyforgedMinionStatBridgeSystem` Missing `minion-duration-bps` Index
- **File:** [HyforgedMinionStatBridgeSystem.java](src/main/java/reign/software/hyforged/stats/system/HyforgedMinionStatBridgeSystem.java)
- **Issue:** Stub missing one of seven required FR-21 minion stat indices.
- **Fix:** Add `MINION_DURATION_BPS` static constant and cache its index in `ensureIndicesCached()`.
