# Feature Plan: Enhanced Stat Scaling

## Metadata
- Feature ID (slug): enhanced-stat-scaling
- Status: Planned
- Owner: JBurl
- Date: 2026-01-19

## ACID Plan Integrity
- Atomicity: Each phase produces a working build; no phase leaves the system in a broken state.
- Consistency: All tasks trace to spec requirements and acceptance criteria.
- Isolation: Phases can be merged independently; each phase has its own tests.
- Durability: Phase status updates recorded in this plan; changes committed per phase.

---

## Phase 1: Scaling Data Model
- Phase Status: [ ] Not Started  [ ] In Progress  [ ] Done

### Summary
Add the scaling rule data structures and extend `StatDefinition` to hold scaling information.

### Steps
- [ ] Create `ScalingRule` sealed interface with `Linear`, `Threshold`, `Diminishing` record implementations in `reign.software.hyforged.stats.scaling` package.
- [ ] Add `scaling` field to `StatDefinition` record (list of `ScalingRule`, defaults to empty).
- [ ] Update `StatDefinition.Builder` to accept scaling rules.
- [ ] Add `hasScaling()` method to `StatDefinition`.

### Files to Create/Modify
| File | Action |
|------|--------|
| `src/main/java/reign/software/hyforged/stats/scaling/ScalingRule.java` | Create |
| `src/main/java/reign/software/hyforged/stats/scaling/LinearScaling.java` | Create |
| `src/main/java/reign/software/hyforged/stats/scaling/ThresholdScaling.java` | Create |
| `src/main/java/reign/software/hyforged/stats/scaling/DiminishingScaling.java` | Create |
| `src/main/java/reign/software/hyforged/stats/StatDefinition.java` | Modify |

### Exit Criteria
- [ ] Build passes.
- [ ] Unit tests pass for ScalingRule records (construction, validation).

---

## Phase 2: Dependency Graph in Registry
- Phase Status: [ ] Not Started  [ ] In Progress  [ ] Done

### Summary
Build the dependency DAG at stat registration time, detect cycles, and compute topological order.

### Steps
- [ ] Add `dependents` map (`int → Set<Integer>`) to `StatDefinitionRegistry` for reverse lookup.
- [ ] Add `evaluationOrder` array (topological sort) to registry.
- [ ] Modify `registerStat()` to:
  - Extract source stat indices from scaling rules.
  - Add edges to dependency graph.
- [ ] Implement `buildEvaluationOrder()` using Kahn's algorithm (BFS topological sort).
- [ ] Call `buildEvaluationOrder()` in `freeze()` and validate no cycles.
- [ ] Add public methods: `getDependentStats(int)`, `getEvaluationOrder()`, `hasScaling(int)`.

### Files to Create/Modify
| File | Action |
|------|--------|
| `src/main/java/reign/software/hyforged/stats/StatDefinitionRegistry.java` | Modify |

### Exit Criteria
- [ ] Build passes.
- [ ] Unit test: register stats with dependencies → correct topological order.
- [ ] Unit test: circular dependency → exception at freeze.

---

## Phase 3: Scaling Engine
- Phase Status: [ ] Not Started  [ ] In Progress  [ ] Done

### Summary
Implement the computation logic for scaling contributions.

### Steps
- [ ] Create `ScalingEngine` utility class in `reign.software.hyforged.stats.engine`.
- [ ] Implement `computeScaledBase(StatDefinition, IntUnaryOperator)` → sum of contributions.
- [ ] Implement `computeContribution(ScalingRule, int sourceValue)`:
  - `Linear`: `(int)(sourceValue * ratio)`
  - `Threshold`: `(sourceValue / perPoints) * bonusBps`
  - `Diminishing`: use `RatingConverter` with cap.
- [ ] Handle empty scaling list (return 0 or defer to defaultValue in caller).
- [ ] Add unit tests for each scaling type.

### Files to Create/Modify
| File | Action |
|------|--------|
| `src/main/java/reign/software/hyforged/stats/engine/ScalingEngine.java` | Create |

### Exit Criteria
- [ ] Build passes.
- [ ] Unit tests pass for all three scaling types.
- [ ] Edge cases: zero source value, negative source value (if allowed), large values.

---

## Phase 4: Unified Base Values in Component
- Phase Status: [ ] Not Started  [ ] In Progress  [ ] Done

### Summary
Replace the separate `abilityScores` array with a general `baseValues` map.

### Steps
- [ ] Add `Int2IntMap baseValues` field to `HyforgedStatComponent`.
- [ ] Add `getBaseValue(int statIndex)` method (returns value or stat's defaultValue).
- [ ] Add `setBaseValue(int statIndex, int value)` method (marks stat dirty).
- [ ] Deprecate `getAbilityScore(int)`, `setAbilityScore(int, int)`, `getAbilityScores()`, `setAbilityScores(int[])`.
- [ ] Update deprecated methods to delegate to new base value methods using stat indices for STR/DEX/INT/CON/WIS/SPI/LCK.
- [ ] Update `clone()` to copy `baseValues`.

### Files to Create/Modify
| File | Action |
|------|--------|
| `src/main/java/reign/software/hyforged/stats/component/HyforgedStatComponent.java` | Modify |

### Exit Criteria
- [ ] Build passes.
- [ ] Existing ability score tests still pass (via deprecated methods).
- [ ] New tests for `getBaseValue`/`setBaseValue`.

---

## Phase 5: Compute System with Topological Order
- Phase Status: [ ] Not Started  [ ] In Progress  [ ] Done

### Summary
Update `HyforgedStatComputeSystem` to evaluate stats in dependency order and use scaling.

### Steps
- [ ] Modify `recomputeDirtyStats()`:
  - Get evaluation order from registry.
  - For each stat in order, check if dirty (or dependent on dirty).
  - Compute in order.
- [ ] Implement dirty expansion (transitive closure of dependents).
- [ ] Modify `computeStatValue()`:
  - If stat has scaling: call `ScalingEngine.computeScaledBase()` with source value provider.
  - If stat has no scaling: use `component.getBaseValue(statIdx)` or `statDef.defaultValue()`.
  - Then apply modifiers via `StackingEngine`.
- [ ] Ensure source value provider reads from cached values (already computed in order).

### Files to Create/Modify
| File | Action |
|------|--------|
| `src/main/java/reign/software/hyforged/stats/system/HyforgedStatComputeSystem.java` | Modify |

### Exit Criteria
- [ ] Build passes.
- [ ] Integration test: Strength modifier → Attack Power updates correctly.
- [ ] Integration test: Multi-level dependency chain evaluates in correct order.

---

## Phase 6: JSON Asset Parsing
- Phase Status: [ ] Not Started  [ ] In Progress  [ ] Done

### Summary
Extend the asset loader to parse scaling rules from JSON.

### Steps
- [ ] Update `StatDefinitionAsset` to include `scaling` field (list of scaling rule objects).
- [ ] Create `ScalingRuleAsset` record for deserialization.
- [ ] Update `StatAssetLoader` to:
  - Parse scaling rules from JSON.
  - Resolve source stat IDs to indices (may require deferred resolution).
  - Construct `ScalingRule` instances.
  - Pass to `StatDefinition.Builder`.
- [ ] Handle missing source stats gracefully (log error, skip rule).
- [ ] Update JSON schema documentation.

### Files to Create/Modify
| File | Action |
|------|--------|
| `src/main/java/reign/software/hyforged/stats/asset/StatDefinitionAsset.java` | Modify |
| `src/main/java/reign/software/hyforged/stats/asset/ScalingRuleAsset.java` | Create |
| `src/main/java/reign/software/hyforged/stats/asset/StatAssetLoader.java` | Modify |

### Exit Criteria
- [ ] Build passes.
- [ ] Test: load stat with linear scaling from JSON → scaling rule parsed correctly.
- [ ] Test: load stat with threshold scaling from JSON → scaling rule parsed correctly.
- [ ] Test: invalid source stat ID → error logged, stat still loads.

---

## Phase 7: Update Core Stat JSON Assets
- Phase Status: [ ] Not Started  [ ] In Progress  [ ] Done

### Summary
Update the core stat JSON definitions to use the new scaling model.

### Steps
- [ ] Update ability score stats (Strength, Dexterity, etc.) to:
  - Add `attribute` tag.
  - Set category to `Attributes`.
  - Remove any scaling (they are base stats).
- [ ] Update derived stats with scaling rules:
  - `attack-power`: linear scaling from `strength`.
  - `spell-power`: linear scaling from `intelligence`.
  - `crit-chance-bps`: threshold scaling from `luck`.
  - (Other derived stats as appropriate.)
- [ ] Validate all stats load correctly on server start.

### Files to Create/Modify
| File | Action |
|------|--------|
| `src/main/resources/Server/Hyforged/Stats/*.json` | Modify |

### Exit Criteria
- [ ] Build passes.
- [ ] Server starts without errors.
- [ ] Spot-check: Attack Power computed correctly from Strength.

---

## Phase 8: Breakdown UI Updates
- Phase Status: [ ] Not Started  [ ] In Progress  [ ] Done

### Summary
Update stat breakdown to show scaling contributions as a separate line.

### Steps
- [ ] Add `scalingContributions` field to `StatBreakdown`.
- [ ] Create `ScalingContribution` record: `sourceStatId`, `contribution`, `ruleType`.
- [ ] Update `HyforgedStatComponent.getStatBreakdown()` to compute and include scaling contributions.
- [ ] Update UI rendering to display scaling lines (e.g., "+40 from Strength").

### Files to Create/Modify
| File | Action |
|------|--------|
| `src/main/java/reign/software/hyforged/stats/breakdown/StatBreakdown.java` | Modify |
| `src/main/java/reign/software/hyforged/stats/breakdown/ScalingContribution.java` | Create |
| `src/main/java/reign/software/hyforged/stats/component/HyforgedStatComponent.java` | Modify (breakdown method) |

### Exit Criteria
- [ ] Build passes.
- [ ] Breakdown correctly shows scaling contribution.
- [ ] Breakdown correctly shows modifiers after scaling.

---

## Phase 9: Migration and Cleanup
- Phase Status: [ ] Not Started  [ ] In Progress  [ ] Done

### Summary
Migrate existing data and remove deprecated code paths.

### Steps
- [ ] Update `StatDataMigrator` to migrate old `abilityScores` format to `baseValues`.
- [ ] Increment schema version in `HyforgedStatComponent`.
- [ ] Add migration test.
- [ ] Remove deprecated ability score methods (or keep deprecated for one release cycle).
- [ ] Update documentation.

### Files to Create/Modify
| File | Action |
|------|--------|
| `src/main/java/reign/software/hyforged/stats/persistence/StatDataMigrator.java` | Modify |
| `src/main/java/reign/software/hyforged/stats/component/HyforgedStatComponent.java` | Modify |

### Exit Criteria
- [ ] Build passes.
- [ ] Migration test: old format → new format with correct values.
- [ ] No compiler warnings from deprecated method usage in core code.

---

## Dependencies
- Existing `hyforged-stats-system` implementation.
- `RatingConverter` for diminishing returns curves.
- Hytale asset loading system for JSON.

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Circular dependency not detected | Kahn's algorithm detects cycles; add explicit test. |
| Performance regression from DAG evaluation | Topological order is pre-computed; evaluation is O(dirty stats). |
| JSON schema breaking existing mods | `scaling` is optional; existing stats without it continue to work. |
| Migration corrupts player data | Schema version bump + explicit migration + backup recommendation. |

## Testing Strategy
- **Unit tests**: ScalingRule construction, ScalingEngine computation, DAG building.
- **Integration tests**: Full stat computation with dependencies, modifier stacking.
- **Asset tests**: JSON loading with various scaling configurations.
- **Migration tests**: Old schema → new schema data migration.

## Rollback Plan
- Revert to previous schema version in `HyforgedStatComponent`.
- Keep `abilityScores` array as fallback during deprecation period.
- Feature flag (optional): `hyforged.stats.enableScaling=false` to disable scaling computation.

## Deployment / Release Notes
- **New Feature**: Stats can now scale from other stats (linear, threshold, diminishing).
- **Change**: Ability scores are now regular stats tagged `attribute`.
- **Migration**: Existing ability score data automatically migrated; no player action required.
- **Modding**: Add `scaling` array to stat JSON to define derived stat formulas.
