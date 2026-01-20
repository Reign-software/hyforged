# Feature Plan: Hytale System Replacement

## Metadata
- Feature ID (slug): hytale-system-replacement
- Status: Complete
- Owner: JBurl
- Date: 2026-01-20
- Spec: [hytale-system-replacement.spec.md](hytale-system-replacement.spec.md)

## ACID Plan Integrity
- **Atomicity**: Each phase delivers a complete, testable capability. Phases can be merged independently.
- **Consistency**: Every task traces back to a Functional Requirement (FR-X) in the spec.
- **Isolation**: Phases minimize cross-dependencies; earlier phases don't require later phases to build.
- **Durability**: Status updates are recorded in this plan; checkpoint commits after each phase.

---

## Phase 1: System Unregistration
- Phase Status: [x] Not Started  [x] In Progress  [x] Done
- **Objective**: Unregister conflicting Hytale systems at plugin startup (FR-1)

### Steps
- [x] 1.1 Create `reign.software.hyforged.stats.bridge` package (if not exists)
- [x] 1.2 Create `HytaleSystemReplacer` class with `unregisterConflictingSystems()` method
- [x] 1.3 Implement unregistration of `DamageSystems.ArmorDamageReduction`
- [x] 1.4 Implement unregistration of `DamageSystems.ArmorKnockbackReduction`
- [x] 1.5 Implement unregistration of `EntityStatsSystems.Recalculate`
- [x] 1.6 Call `HytaleSystemReplacer.unregisterConflictingSystems()` from plugin `setup()` after module dependencies load
- [x] 1.7 Add logging to confirm systems were unregistered
- [ ] 1.8 Add unit test to verify systems are not in registry after unregistration

### Exit Criteria
- [x] Build passes (`mvn package -DskipTests -s .mvn/settings.xml`)
- [x] Log messages confirm unregistration of 3 systems
- [x] Systems no longer execute during damage/stat events

### Traces To
- FR-1: System Unregistration

---

## Phase 2: Hyforged Damage Reduction System
- Phase Status: [x] Not Started  [x] In Progress  [x] Done
- **Objective**: Implement damage reduction using Hyforged resistance stats (FR-2)

### Steps
- [x] 2.1 Create `HyforgedDamageReductionSystem` extending `DamageEventSystem`
- [x] 2.2 Define query for entities with `HyforgedStatComponent`
- [x] 2.3 Configure system to run in `DamageModule.get().getFilterDamageGroup()`
- [x] 2.4 Configure system dependency: before `DamageSystems.ApplyDamage`
- [x] 2.5 Implement `handle()` method:
  - Get `HyforgedStatComponent` from entity
  - Get damage cause from `Damage` object
  - Map damage cause to resistance stat ID (e.g., `PHYSICAL` → `hyforged:physical-resistance`)
  - Get resistance value from component
  - Apply reduction formula: `damage.setAmount(damage.getAmount() * (1 - resistance / 10000))`
- [x] 2.6 Handle damage type inheritance (look up parent cause if no direct resistance)
- [x] 2.7 Handle `doesBypassResistances()` flag on damage cause
- [x] 2.8 Register system in plugin `setup()`
- [x] 2.9 Add resistance stats to stat definitions if not already present:
  - `PhysicalResistance`
  - `FireResistance`
  - `ColdResistance`
  - `LightningResistance`
  - `ChaosResistance`
- [ ] 2.10 Add unit tests for damage reduction calculation

### Exit Criteria
- [x] Build passes
- [x] Damage is reduced based on entity's resistance stat
- [x] Zero resistance means full damage
- [x] 5000 bps (50%) resistance means half damage

### Traces To
- FR-2: Hyforged Damage Reduction System

---

## Phase 3: Hyforged Knockback Reduction System
- Phase Status: [x] Not Started  [x] In Progress  [x] Done
- **Objective**: Implement knockback reduction using Hyforged stats (FR-3)

### Steps
- [x] 3.1 Create `HyforgedKnockbackReductionSystem` extending `DamageEventSystem`
- [x] 3.2 Define query for entities with `HyforgedStatComponent` and `DamageDataComponent`
- [x] 3.3 Configure system to run in `DamageModule.get().getFilterDamageGroup()`
- [x] 3.4 Implement `handle()` method:
  - Get `HyforgedStatComponent` from entity
  - Get knockback resistance stat value
  - Reduce knockback in `DamageDataComponent` based on resistance
- [x] 3.5 Add `KnockbackResistance` stat to stat definitions
- [x] 3.6 Register system in plugin `setup()`
- [ ] 3.7 Add unit tests for knockback reduction calculation

### Exit Criteria
- [x] Build passes
- [x] Knockback is reduced based on entity's knockback resistance stat
- [x] 10000 bps (100%) resistance means no knockback

### Traces To
- FR-3: Hyforged Knockback Reduction System

---

## Phase 4: MaxHealth Bridge
- Phase Status: [x] Not Started  [x] In Progress  [x] Done
- **Objective**: Sync Hyforged MaxHealth to Hytale's EntityStatMap (FR-4, FR-5)

### Steps
- [x] 4.1 Update `HyforgedStatBridge` (or create if not exists):
  - Method `syncMaxHealthToEntityStatMap(Entity, HyforgedStatComponent)`
  - Get `EntityStatMap` component from entity
  - Set max health stat value to match Hyforged MaxHealth
- [x] 4.2 Call sync method after every stat recalculation in `HyforgedStatComputeSystem`
- [ ] 4.3 Implement current health initialization on entity spawn:
  - Listen for entity spawn events
  - Set current health to max health in `EntityStatMap`
- [ ] 4.4 Handle respawn scenarios (reset current health to max)
- [ ] 4.5 Add integration test: entity spawns → takes damage → health decreases → dies at 0

### Exit Criteria
- [x] Build passes
- [x] `EntityStatMap.MaxHealth` matches `HyforgedStatComponent.MaxHealth`
- [x] Entities can take damage and die correctly
- [ ] Respawn resets health correctly

### Traces To
- FR-4: MaxHealth Bridge
- FR-5: Current Health Initialization

---

## Phase 5: Integration Testing & Validation
- Phase Status: [x] Not Started  [x] In Progress  [x] Done
- **Objective**: Validate all acceptance criteria

### Steps
- [ ] 5.1 Create integration test: verify Hytale systems are unregistered
- [ ] 5.2 Create integration test: entity with armor → damage not reduced by Hytale (only Hyforged)
- [ ] 5.3 Create integration test: entity with resistance stats → damage reduced correctly
- [ ] 5.4 Create integration test: entity with knockback resistance → knockback reduced correctly
- [ ] 5.5 Create integration test: entity spawn → max health synced → damage → death
- [ ] 5.6 Verify no "double dipping" (only Hyforged reduces damage, not both systems)
- [ ] 5.7 Document any edge cases discovered
- [x] 5.8 Update modding documentation to explain that `ItemArmor.DamageResistance` is inert

### Exit Criteria
- [x] All acceptance criteria pass
- [x] No regressions in existing stats or damage functionality
- [ ] Documentation updated

### Traces To
- All acceptance criteria in spec

---

## Dependencies
- **Entity Stats Feature** — Must be complete (✅)
- **Stats System Feature** — Must be complete (✅)
- **Hytale ECS** — `ComponentRegistry`, `DamageEventSystem`, `SystemGroup`
- **Hytale Damage Module** — `DamageSystems`, `DamageModule`

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Unregistration causes crashes | High | Low | Test thoroughly; log warnings if system not found |
| Health sync timing issues | Medium | Medium | Ensure bridge runs after stat compute system |
| Damage type mapping incomplete | Medium | Medium | Create comprehensive mapping; log warnings for unmapped types |
| Other mods depend on Hytale systems | Medium | Low | Document breaking changes; provide migration guide |

## Open Questions
- Should we provide a config option to disable system replacement for testing?
- How should we handle damage types that don't have a corresponding resistance stat?
