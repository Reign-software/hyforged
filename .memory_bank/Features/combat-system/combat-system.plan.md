# Feature Plan: Combat System

## Metadata
- Feature ID (slug): combat-system
- Status: Planned
- Owner: JBurl
- Date: 2026-01-20

## ACID Plan Integrity
- **Atomicity**: Each phase is independently completable with a buildable deliverable
- **Consistency**: All tasks trace to combat-system.spec.md requirements and acceptance criteria
- **Isolation**: Phases minimize cross-dependencies; Phase 1–3 can proceed with stubs for later phases
- **Durability**: Status updates recorded in this plan; code changes tracked via git

---

## Phase 1: Stat Cap System and Combat Stats Foundation
- Phase Status: [x] Not Started  [ ] In Progress  [ ] Done

### Description
Extend the existing stats system to support soft/hard cap metadata and ensure all combat-relevant stats are defined with proper cap configuration.

### Steps
- [ ] 1.1 Update `StatDefinitionAsset` schema to include `SoftCap` and `HardCap` fields (optional integers, basis points)
- [ ] 1.2 Update `StatDefinition` record to store cap metadata
- [ ] 1.3 Update `StatDefinitionRegistry` to expose cap lookup API
- [ ] 1.4 Implement cap enforcement in `HyforgedStatComponent` during value computation
  - Soft cap applies by default
  - Stats like `max-crit-chance-bps` can increase soft cap toward hard cap
- [ ] 1.5 Define or update combat stat definitions with cap metadata:
  - `crit-chance-bps`: soft 5000 (50%), hard 9500 (95%)
  - `block-chance-bps`: soft 7500 (75%), hard 9000 (90%)
  - `evasion-rating` → derived `evasion-chance-bps`: soft 7500, hard 9000
  - All resistance stats: soft 7500, hard 9000
- [ ] 1.6 Add `max-crit-chance-bps`, `max-block-chance-bps`, `max-evasion-chance-bps` stats that raise soft caps
- [ ] 1.7 Create unit tests for cap enforcement logic

### Exit Criteria
- [ ] Build passes (`mvn package -DskipTests -s .mvn/settings.xml`)
- [ ] All cap-related unit tests pass
- [ ] Stats JSON updated with cap metadata

### Dependencies
- Existing stats system (hyforged-stats-system feature — complete)

---

## Phase 2: Hit Resolution System (Accuracy vs Evasion)
- Phase Status: [x] Not Started  [ ] In Progress  [ ] Done

### Description
Implement the accuracy vs evasion check that runs before damage is applied. This is a new system that intercepts attacks before the damage pipeline.

### Steps
- [ ] 2.1 Research Hytale attack initiation events
  - Check `InteractionType` and `DamageEntityInteraction` for pre-damage hooks
  - Document findings in ADR if new integration pattern needed
- [ ] 2.2 Create `HyforgedHitResolutionSystem` extending `DamageEventSystem`
  - Query for entities with `HyforgedStatComponent`
  - Run in `gatherDamageGroup` before `filterDamageGroup`
  - Roll accuracy vs evasion check
- [ ] 2.3 Implement hit chance formula:
  - Base hit chance = f(attacker accuracy, defender evasion)
  - Level difference penalty: if monster level > player level, reduce evasion effectiveness
  - Use seeded RNG or Hytale's random with documented source
- [ ] 2.4 Cancel damage event if attack misses
- [ ] 2.5 Add miss indicator to `Damage` meta (for combat log / UI)
- [ ] 2.6 Define accuracy-related stats if not already present:
  - `accuracy-rating` (attacker stat — exists)
  - `evasion-rating` (defender stat — exists)
  - `evasion-chance-bps` (derived from rating; level-dependent)
- [ ] 2.7 Create `CombatMath` utility class for shared combat formulas
- [ ] 2.8 Register system in `HyforgedPlugin.setup()`
- [ ] 2.9 Create unit tests for hit/miss resolution
- [ ] 2.10 Create integration test with mock attacker/defender entities

### Exit Criteria
- [ ] Build passes
- [ ] Hit resolution tests pass
- [ ] Attacks can miss based on accuracy vs evasion

### Dependencies
- Phase 1 (cap system for evasion chance)
- Existing `HyforgedStatComponent`

---

## Phase 3: Auto-Block System
- Phase Status: [x] Not Started  [ ] In Progress  [ ] Done

### Description
Implement chance-based auto-block that consumes less stamina than manual blocking and provides partial mitigation.

### Steps
- [ ] 3.1 Research Hytale's existing block mechanics
  - Locate stamina consumption on manual block
  - Identify how `BlockingComponent` or similar tracks block state
- [ ] 3.2 Create `HyforgedAutoBlockSystem` extending `DamageEventSystem`
  - Run in `filterDamageGroup` after hit resolution, before damage reduction
  - Query for entities with `HyforgedStatComponent` and stamina
- [ ] 3.3 Implement auto-block logic:
  - Check if defender has `block-chance-bps` > 0
  - Check if defender has stamina > 0
  - Check if defender is NOT manually blocking (mutually exclusive)
  - Roll block chance
- [ ] 3.4 On successful auto-block:
  - Apply `block-mitigation-bps` (default 5000 = 50%) damage reduction
  - Consume stamina at reduced rate (configurable, e.g., 10% of manual block cost)
- [ ] 3.5 Define or update stats:
  - `block-chance-bps` (exists)
  - `block-mitigation-bps` (new, default 5000)
  - `auto-block-stamina-cost-bps` (new, default 1000 = 10%)
- [ ] 3.6 Add block indicator to `Damage` meta (for combat log)
- [ ] 3.7 Register system in `HyforgedPlugin.setup()` with correct dependency ordering
- [ ] 3.8 Create unit tests for auto-block logic
- [ ] 3.9 Create integration test with stamina consumption verification

### Exit Criteria
- [ ] Build passes
- [ ] Auto-block tests pass
- [ ] Stamina is consumed on auto-block

### Dependencies
- Phase 2 (hit resolution runs first)
- Hytale stamina system (EntityStatMap)

---

## Phase 4: Multi-Element Damage and Penetration
- Phase Status: [x] Not Started  [ ] In Progress  [ ] Done

### Description
Extend the existing `HyforgedDamageReductionSystem` to support multi-element attacks with per-type resistance and penetration calculations.

### Steps
- [ ] 4.1 Design multi-element damage representation
  - Option A: Multiple `Damage` events per attack
  - Option B: Custom meta on `Damage` with damage type breakdown
  - Document decision in ADR
- [ ] 4.2 Update `HyforgedDamageReductionSystem`:
  - Parse multi-element damage breakdown from meta or sources
  - Apply resistance per damage type independently
- [ ] 4.3 Implement penetration:
  - `EffectiveResistance = max(0, Resistance - Penetration)`
  - Each penetration stat applies only to its element
- [ ] 4.4 Update `DamageTypeExtension` schema to include penetration stat reference
  - Already has `HyforgedPenetrationStat` field (verify usage)
- [ ] 4.5 Update `DamageTypeExtensionRegistry` to expose penetration stat lookup
- [ ] 4.6 Ensure all damage type extensions have penetration stats defined:
  - `physical-penetration-bps` (exists as ArmorPenetration)
  - `fire-penetration-bps` (exists)
  - `cold-penetration-bps` (exists)
  - etc.
- [ ] 4.7 Create unit tests for multi-element damage calculation
- [ ] 4.8 Create unit tests for penetration interactions

### Exit Criteria
- [ ] Build passes
- [ ] Multi-element damage tests pass
- [ ] Penetration correctly reduces effective resistance

### Dependencies
- Existing `HyforgedDamageReductionSystem`
- `DamageTypeExtensionRegistry`

---

## Phase 5: Critical Hit System
- Phase Status: [x] Not Started  [ ] In Progress  [ ] Done

### Description
Implement critical hit mechanics where crit is rolled once per attack and applies a multiplier to total post-mitigation damage.

### Steps
- [ ] 5.1 Create `HyforgedCriticalHitSystem` extending `DamageEventSystem`
  - Run in `inspectDamageGroup` after damage reduction
  - Query for entities where `Damage.Source` is `EntitySource`
- [ ] 5.2 Implement crit logic:
  - Roll crit chance from attacker's `crit-chance-bps` (capped per Phase 1)
  - Apply level difference penalty vs higher-level monsters
  - On crit: multiply final damage by `crit-multiplier-bps / 10000`
- [ ] 5.3 Define or verify stats:
  - `crit-chance-bps` (exists)
  - `crit-multiplier-bps` (exists, verify default e.g., 15000 = 150%)
- [ ] 5.4 Add crit indicator to `Damage` meta (for combat log / visual feedback)
- [ ] 5.5 Investigate Hytale's existing crit visual/audio hooks
  - May hook into `DamageSystems.EntityUIEvents` for crit text
- [ ] 5.6 Register system in `HyforgedPlugin.setup()` with correct ordering
- [ ] 5.7 Create unit tests for crit chance and multiplier
- [ ] 5.8 Create tests for level-based crit penalty

### Exit Criteria
- [ ] Build passes
- [ ] Crit tests pass
- [ ] Crit multiplier correctly amplifies damage

### Dependencies
- Phase 1 (crit chance caps)
- Phase 4 (runs after damage reduction)

---

## Phase 6: Monster Scaling System
- Phase Status: [x] Not Started  [ ] In Progress  [ ] Done

### Description
Implement distance-based monster scaling where monster stats scale based on distance from world spawn.

### Steps
- [ ] 6.1 Research Hytale world spawn retrieval
  - Identify API for world spawn coordinates or document assumption (0,0)
- [ ] 6.2 Create `Server/Hyforged/Combat/WorldScaling/` directory structure
- [ ] 6.3 Define `WorldScalingConfig` asset schema:
  - `ScalingCurve`: formula type (linear, logarithmic, stepped)
  - `BlocksPerLevel`: distance in blocks per monster level
  - `MinLevel`, `MaxLevel` bounds
  - Optional region overrides (future)
- [ ] 6.4 Create `WorldScalingConfigAsset` and loader
- [ ] 6.5 Create `MonsterScalingService` singleton:
  - `getMonsterLevel(double distanceFromSpawn, WorldScalingConfig)` → int
  - `getScaledStats(baseStats, monsterLevel)` → scaled stats
- [ ] 6.6 Create `MonsterLevelComponent` for cached monster level
- [ ] 6.7 Create `HyforgedMonsterScalingSystem`:
  - On NPC spawn, calculate distance from world spawn
  - Assign monster level to `MonsterLevelComponent`
  - Apply stat scaling to `HyforgedStatComponent`
- [ ] 6.8 Define scaled stats:
  - Max health scaling
  - Damage scaling
  - Resistance scaling
- [ ] 6.9 Integrate level difference into combat formulas (Phases 2, 3, 5)
- [ ] 6.10 Create default `WorldScaling.json` configuration
- [ ] 6.11 Create unit tests for scaling formulas
- [ ] 6.12 Create integration test spawning NPCs at varying distances

### Exit Criteria
- [ ] Build passes
- [ ] Scaling tests pass
- [ ] Monsters at greater distances have higher level and stats

### Dependencies
- Hytale NPC spawning systems
- `HyforgedStatComponent`

### Open Questions to Resolve
- Exact world spawn coordinates API (document finding)
- Scaling formula details (propose defaults, mark for balance iteration)

---

## Phase 7: Combat Log System
- Phase Status: [x] Not Started  [ ] In Progress  [ ] Done

### Description
Implement per-player combat logging that stores recent encounters with per-attack breakdowns.

### Steps
- [ ] 7.1 Design combat log data structures:
  - `CombatEncounter`: list of `CombatEvent`, start/end time, participants
  - `CombatEvent`: attacker, defender, damage breakdown, crit/block/miss flags
- [ ] 7.2 Create `CombatLogComponent` for per-player combat history
  - Ring buffer of last 5 encounters
  - In-memory only (no persistence)
- [ ] 7.3 Create `CombatLogService`:
  - `recordEvent(playerRef, CombatEvent)`
  - `getRecentEncounters(playerRef)` → List<CombatEncounter>
  - Encounter boundary detection (time-based or combat state)
- [ ] 7.4 Create `HyforgedCombatLogSystem`:
  - Run in `inspectDamageGroup` after all damage modifications
  - Collect all meta from `Damage` event (crit, block, miss, damage types)
  - Record to `CombatLogComponent` for both attacker and defender (if players)
- [ ] 7.5 Register component and system in `HyforgedPlugin.setup()`
- [ ] 7.6 Define `CombatLogService` API for UI consumption
- [ ] 7.7 Create unit tests for combat log recording
- [ ] 7.8 Create tests for encounter boundary detection

### Exit Criteria
- [ ] Build passes
- [ ] Combat log tests pass
- [ ] API can retrieve recent combat events per player

### Dependencies
- Phases 2–5 (combat events produce meta to log)

---

## Phase 8: Status Effects and Ailments Integration
- Phase Status: [x] Not Started  [ ] In Progress  [ ] Done

### Description
Integrate with Hytale's `EntityEffect` system for ailments (freeze, ignite, poison, etc.) with threshold-based triggering and stat-driven duration.

### Steps
- [ ] 8.1 Research Hytale `EntityEffect` extension points
  - Can custom fields be added to effect JSON?
  - Document approach in ADR (extend vs parallel structure)
- [ ] 8.2 Define ailment threshold mechanics:
  - Damage of a type accumulates within a time window
  - When threshold exceeded, apply corresponding ailment
- [ ] 8.3 Create `AilmentAccumulatorComponent`:
  - Per-entity tracking of elemental damage per window
  - Reset on window expiry or ailment application
- [ ] 8.4 Create `HyforgedAilmentSystem`:
  - Run in `inspectDamageGroup` after damage
  - Accumulate damage by type
  - Check thresholds, apply `EntityEffect` on trigger
- [ ] 8.5 Integrate `effect-duration-bps` stat for ailment duration scaling
- [ ] 8.6 Create Hyforged ailment EntityEffect JSON assets if needed:
  - `Server/Entity/Effects/Status/Freeze.json` (check if exists)
  - `Server/Entity/Effects/Status/Ignite.json`
  - etc.
- [ ] 8.7 Define ailment stats:
  - `ailment-threshold-bps` (damage required to trigger)
  - `freeze-threshold-bps`, `ignite-threshold-bps`, etc. (per element)
  - `effect-duration-bps` (exists)
- [ ] 8.8 Create unit tests for threshold accumulation
- [ ] 8.9 Create integration test for ailment application

### Exit Criteria
- [ ] Build passes
- [ ] Ailment tests pass
- [ ] Cold damage can trigger freeze effect

### Dependencies
- Hytale `EntityEffect` system
- Phases 4 (multi-element damage provides type breakdown)

### Open Questions to Resolve
- Can `EntityEffect` JSON support custom tag fields for Hyforged targeting?
- Baseline threshold values (mark for balance iteration)

---

## Phase 9: Combat Service API
- Phase Status: [x] Not Started  [ ] In Progress  [ ] Done

### Description
Create a unified `CombatService` API for programmatic damage application with full stat resolution, bypassing raw `Damage` event creation.

### Steps
- [ ] 9.1 Design `CombatService` interface:
  - `applyDamage(attackerRef, defenderRef, DamageSpec)` → `CombatResult`
  - `DamageSpec`: damage types/amounts, forced crit, flags
  - `CombatResult`: final damage, hit/miss, crit, block, applied effects
- [ ] 9.2 Implement `CombatServiceImpl`:
  - Resolve attacker and defender stats
  - Run hit resolution, block, damage reduction, crit
  - Create and dispatch `Damage` event
  - Return result
- [ ] 9.3 Create `HyforgedDamageSpec` for multi-element damage specification
- [ ] 9.4 Expose `CombatService` singleton via `HyforgedPlugin`
- [ ] 9.5 Document API in `Modding_Doc/Combat/API.md`
- [ ] 9.6 Create unit tests for `CombatService`
- [ ] 9.7 Create integration tests verifying full pipeline

### Exit Criteria
- [ ] Build passes
- [ ] `CombatService` API tests pass
- [ ] API documentation complete

### Dependencies
- Phases 2–5 (all combat mechanics)
- Phase 7 (combat log integration)

---

## Phase 10: Combat UI Integration
- Phase Status: [x] Not Started  [ ] In Progress  [ ] Done

### Description
Create player-accessible combat log UI and integrate visual feedback for crit/block/miss.

### Steps
- [ ] 10.1 Research Hytale UI systems:
  - `UIComponentList`, `EntityUIEvents`
  - How damage numbers are displayed
- [ ] 10.2 Design combat log UI layout:
  - Accessible via command or keybind
  - Shows last 5 encounters with expandable details
- [ ] 10.3 Implement combat log UI component
  - Query `CombatLogService` for data
  - Render encounter list with per-attack breakdown
- [ ] 10.4 Integrate crit visual indicator:
  - Hook into `DamageSystems.EntityUIEvents` or create parallel system
  - Show distinct crit text (color, size, animation)
- [ ] 10.5 Integrate miss visual indicator:
  - Show "Miss" text on evaded attacks
- [ ] 10.6 Integrate block visual indicator:
  - Show "Blocked" text with reduced damage
- [ ] 10.7 Create command to toggle combat log: `/combatlog`
- [ ] 10.8 Test UI in-game
- [ ] 10.9 Update `Modding_Doc/Combat/README.md` with UI documentation

### Exit Criteria
- [ ] Build passes
- [ ] Combat log UI functional in-game
- [ ] Crit/miss/block visuals display correctly

### Dependencies
- Phase 7 (CombatLogService)
- Hytale UI systems

---

## Phase 11: Healing Integration
- Phase Status: [x] Not Started  [ ] In Progress  [ ] Done

### Description
Integrate healing into the combat pipeline, ensuring resistances don't affect healing and healing-related stats are applied.

### Steps
- [ ] 11.1 Research Hytale healing mechanics:
  - Identify healing event system if separate from damage
  - Document approach
- [ ] 11.2 Create `HyforgedHealingSystem` if needed:
  - Apply healing modifiers from stats
  - `life-recovery-rate-bps`, `healing-received-bps`
- [ ] 11.3 Ensure healing bypasses resistance/penetration pipeline
- [ ] 11.4 Define or verify healing stats:
  - `life-recovery-rate-bps` (exists)
  - `healing-received-bps` (new if needed)
  - `healing-effectiveness-bps` (new, outgoing healing)
- [ ] 11.5 Integrate healing events with combat log (optional)
- [ ] 11.6 Create unit tests for healing modifiers

### Exit Criteria
- [ ] Build passes
- [ ] Healing tests pass
- [ ] Healing stats correctly modify healing amounts

### Dependencies
- Hytale healing systems
- `HyforgedStatComponent`

---

## Phase 12: Testing, Validation, and Documentation
- Phase Status: [x] Not Started  [ ] In Progress  [ ] Done

### Description
Final integration testing, balance validation hooks, and documentation completion.

### Steps
- [ ] 12.1 Create end-to-end integration tests:
  - Full combat scenario (attack → hit check → block → damage → crit → log)
  - Multi-element attack scenario
  - Monster scaling scenario
- [ ] 12.2 Add determinism tests:
  - Verify same inputs produce same outputs with seeded RNG
- [ ] 12.3 Add performance benchmarks:
  - Measure per-hit combat resolution time
  - Ensure O(1) per damage type
- [ ] 12.4 Create debug mode toggle:
  - Verbose combat logging for balance testing
  - Configurable via `GameplayConfigs`
- [ ] 12.5 Complete `Modding_Doc/Combat/README.md`:
  - Combat pipeline overview
  - Stat interactions
  - Configuration options
- [ ] 12.6 Complete `Modding_Doc/Combat/API.md`:
  - `CombatService` API reference
  - `CombatLogService` API reference
- [ ] 12.7 Update `.memory_bank/Features/combat-system/` with implementation summary
- [ ] 12.8 Verify all acceptance criteria from spec are met
- [ ] 12.9 Tag release candidate

### Exit Criteria
- [ ] Build passes
- [ ] All tests pass (unit + integration)
- [ ] Documentation complete
- [ ] All spec acceptance criteria met

### Dependencies
- All previous phases

---

## Dependencies Summary
| Phase | Depends On |
|-------|------------|
| 1 | Stats System (complete) |
| 2 | Phase 1 |
| 3 | Phase 2 |
| 4 | Existing damage reduction |
| 5 | Phase 1, Phase 4 |
| 6 | NPCs, Stats |
| 7 | Phases 2–5 |
| 8 | Phase 4, EntityEffect |
| 9 | Phases 2–5, 7 |
| 10 | Phase 7, UI systems |
| 11 | Stats, healing systems |
| 12 | All phases |

---

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Hytale pre-damage hook not available | High | Medium | Research Phase 2.1 early; may need to intercept at `gatherDamageGroup` or create custom interaction |
| `EntityEffect` not extensible with tags | Medium | Medium | Use parallel data structure if needed; document in ADR |
| Performance regression with complex combat | Medium | Low | Benchmark Phase 12.3; cache stat indices; minimize allocations |
| Level difference formula unbalanced | Medium | High | Mark formulas as configurable; iterate post-playtest |
| Multi-element damage representation unclear | Medium | Medium | Decide in Phase 4.1; may use `Damage` meta |
| World spawn API unknown | Low | Medium | Research Phase 6.1; fall back to (0,0) assumption |

---

## Testing Strategy

### Unit Tests
- `CombatMathTest`: Hit chance formulas, crit calculations, cap enforcement
- `HitResolutionTest`: Accuracy vs evasion logic
- `AutoBlockTest`: Block chance, stamina consumption, mitigation
- `DamageReductionTest`: Multi-element damage, penetration
- `CriticalHitTest`: Crit chance, multiplier, level penalty
- `MonsterScalingTest`: Level calculation, stat scaling
- `CombatLogTest`: Event recording, encounter boundaries
- `AilmentTest`: Threshold accumulation, trigger conditions

### Integration Tests
- Full combat pipeline with mock entities
- Monster spawning at distances with scaling verification
- Combat log retrieval and display

### Manual Testing
- In-game combat scenarios
- UI verification for combat log and indicators
- Performance profiling with many entities

---

## Rollback Plan
- Each phase is independently deployable; partial rollback possible
- Combat systems registered separately; can unregister individual systems
- Feature flags (if implemented) can disable specific mechanics
- Revert to previous stat definitions if cap system causes issues

---

## Deployment / Release Notes
- Requires Hyforged Stats System (prerequisite)
- New JSON assets in `Server/Hyforged/Combat/`
- New stat definitions for combat mechanics
- Combat log UI accessible via `/combatlog`
- Monster scaling active by default

---

## Open Questions (to resolve during implementation)
1. **World spawn coordinates**: Is world spawn always (0,0) or configurable per world?
2. **Level difference penalty formula**: Propose linear reduction (e.g., 5% per level), validate in playtest
3. **Ailment threshold values**: Start with 10000 bps base, iterate
4. **EntityEffect tag support**: Research in Phase 8.1
5. **Multi-element damage format**: Decide in Phase 4.1

---

## Implementation Summary (post-development)
*To be completed after implementation*

## Test Results (post-validation)
*To be completed after validation*

## Lessons Learned (post-release)
*To be completed after release*
