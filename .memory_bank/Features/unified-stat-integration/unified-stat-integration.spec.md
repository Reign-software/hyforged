# Feature Spec: Unified Stat Integration

## Metadata
- Feature ID (slug): unified-stat-integration
- Status: Draft
- Owner: JBurl
- Date: 2026-01-21

## Summary
Extend Hytale's `EntityStatValue` class to create `HyforgedStatValue`, enabling Hyforged's ARPG stat system to integrate directly with all Hytale systems (combat, items, buffs, effects) without maintaining a parallel stat layer and complex bridging code.

## Problem Statement
The current architecture (ADR-0001) uses a "hybrid" approach:
- `HyforgedStatComponent` stores ability scores, modifiers, and computed values
- `EntityStatMap` (Hytale) stores resource stats
- Bridge systems sync between the two layers

This creates significant problems:
1. **Duplication**: Every Hytale system that uses stats (combat, items, buffs, effects) expects `EntityStatValue` — Hyforged must intercept/wrap each one
2. **Complexity**: Bridge systems must translate between Hyforged and Hytale representations
3. **Maintenance burden**: Adding new features (e.g., buffs) requires writing Hyforged-aware wrappers for Hytale APIs
4. **Integration friction**: Cannot use Hyforged stats directly in Hytale item JSON without custom loaders
5. **Code explosion**: "Re-writing the whole game" to make Hyforged stats work everywhere

## Goals
- Create a single stat value type that works with both Hyforged and Hytale systems
- Enable Hyforged modifiers (FLAT/INCREASED/MORE/CAP) to be applied via standard Hytale APIs
- Eliminate the need for separate bridge systems for each Hytale subsystem
- Allow items, buffs, and effects to reference Hyforged stats using standard JSON
- Reduce code complexity and maintenance burden significantly

## Non-Goals
- Replacing all of `HyforgedStatComponent` — some features (base values, conditional modifiers, event coalescing) may remain
- Breaking changes to existing Hyforged stat JSON schemas
- Modifying Hytale's decompiled source code (all changes must be plugin-side)

## Architectural Approach

### Key Insight: EntityStatValue is Extensible
```
EntityStatValue:
  - protected EntityStatValue() — subclass allowed
  - protected float set(float) — overridable  
  - protected void computeModifiers(EntityStatType) — **key extension point**
  - public synchronizeAsset(int, EntityStatType) — calls computeModifiers
```

The `computeModifiers` method is where modifier stacking logic lives. By subclassing and overriding this, `HyforgedStatValue` can implement ARPG stacking (FLAT → INCREASED → MORE → CAP) while remaining compatible with all Hytale systems.

### Challenge: EntityStatMap Creates EntityStatValue Directly
`EntityStatMap.update()` does `new EntityStatValue(index, asset)` — hardcoded.

Options to address:
1. **Replace EntityStatMap values post-creation** — After Hytale creates EntityStatValue instances, replace them with HyforgedStatValue instances
2. **Custom EntityStatMap component** — Register our own component type that uses HyforgedStatValue
3. **Reflection/mixin injection** — Modify EntityStatMap behavior at runtime (fragile)

Recommended: **Option 1** — Swap values via a setup system after EntityStatMap is initialized.

## Functional Requirements

### FR-1: HyforgedStatValue Class
- Extends `EntityStatValue`
- Overrides `computeModifiers()` to apply ARPG stacking semantics
- Supports additional base value bonus from Hyforged systems
- Provides change listeners for reactive systems
- Links to `StatDefinition` from `StatDefinitionRegistry` when available
- Fully compatible as drop-in replacement for EntityStatValue

### FR-2: ARPG Modifier Stacking
Order of operations in `computeModifiers()`:
1. Base value (from EntityStatType + hyforgedBaseBonus)
2. Sum all FLAT modifiers
3. Sum all INCREASED modifiers, apply as (1 + sum/10000) multiplier
4. Apply each MORE modifier sequentially as (1 + amount/10000)
5. Apply CAP modifiers (min/max bounds)
6. Call parent `computeModifiers()` for native Hytale modifier compatibility

### FR-3: HyforgedStatValue Factory/Installer
- System or utility that replaces `EntityStatValue` instances in `EntityStatMap` with `HyforgedStatValue`
- Runs after entity initialization
- Preserves existing modifier state during swap
- Handles hot-swap scenarios (e.g., reload)

### FR-4: Integration with HyforgedModifier
- `HyforgedModifier` (already registered with `Modifier.CODEC`) works seamlessly
- When added to EntityStatMap via `putModifier()`, HyforgedStatValue recognizes and processes it correctly
- Standard Hytale `StaticModifier` continues to work (handled by parent class)

### FR-5: Backward Compatibility
- Existing `HyforgedStatComponent` usage should continue working during migration
- Bridge systems can be gradually deprecated as direct integration takes over
- No breaking changes to stat JSON schemas

## Non-Functional Requirements

### Performance
- `computeModifiers()` called on modifier add/remove — must be O(n) in modifier count
- No per-tick overhead beyond existing Hytale stat recomputation

### Memory
- HyforgedStatValue adds minimal overhead per stat (1 int, 1 reference, 1 float)

### Thread Safety
- Follow Hytale's existing threading model (main thread for component access)

## Dependencies
- `HyforgedModifier` — already implemented and registered
- `StatDefinitionRegistry` — for linking to Hyforged stat metadata
- Hytale's `EntityStatMap`, `EntityStatValue`, `Modifier` — base classes

## Data/Schema Impact
- No changes to JSON schemas
- `HyforgedModifier` JSON format unchanged
- Stat definition JSON unchanged

## API Changes
- New: `HyforgedStatValue extends EntityStatValue`
- New: `HyforgedStatValueInstaller` (system or utility)
- Deprecated (eventually): Bridge systems like `HyforgedDamageReductionSystem`, `HyforgedKnockbackReductionSystem`
- Deprecated (eventually): Portions of `HyforgedStatComponent` that duplicate EntityStatValue functionality

## Risks

### R-1: EntityStatMap Reinstantiation
- **Risk**: Hytale may recreate EntityStatMap or EntityStatValue instances in certain scenarios
- **Mitigation**: Installer system runs as RefChangeSystem to catch re-creation

### R-2: Serialization/Persistence
- **Risk**: EntityStatValue.CODEC may not deserialize HyforgedStatValue correctly
- **Mitigation**: HyforgedStatValue adds no persistent fields; post-load swap and recompute

### R-3: Network Sync
- **Risk**: Client may expect vanilla EntityStatValue format
- **Mitigation**: `toPacket()` unchanged (parent behavior); server-authoritative

### R-4: Modifier Order Sensitivity
- **Risk**: Hytale systems may add modifiers in unexpected order
- **Mitigation**: ARPG stacking order is defined by StackType, not insertion order

## Design Decisions

### D-1: Persistence Strategy — No Extra Fields (Option C)
- **Decision**: `HyforgedStatValue` adds NO persistent fields
- **Rationale**: 
  - EntityStatValue.CODEC uses `EntityStatValue::new` as factory — always deserializes to parent type
  - Post-load, installer system swaps `EntityStatValue` → `HyforgedStatValue`
  - `computeModifiers()` recalculates ARPG stacking from existing modifiers
  - Simplest approach; no codec modifications needed
- **Implication**: All HyforgedStatValue extensions (listeners, links) are transient and recomputed

### D-2: Modifier Storage — Use EntityStatValue.modifiers
- **Decision**: Use `EntityStatValue.modifiers` (Map<String, Modifier>) exclusively
- **Rationale**:
  - `HyforgedModifier extends Modifier` — already compatible
  - Modifiers added via `EntityStatMap.putModifier()` automatically stored
  - `computeModifiers()` override detects and processes `HyforgedModifier` instances
  - Enables seamless integration with items, buffs, effects
- **Implication**: Remove `StatModifier` class; migrate all usages to `HyforgedModifier`

### D-3: computeModifiers() Entry Points — Confirmed
- **Finding**: `computeModifiers()` is called automatically by:
  - `EntityStatValue.putModifier(key, modifier)` — when modifier added
  - `EntityStatValue.removeModifier(key)` — when modifier removed
  - `EntityStatValue.synchronizeAsset(index, asset)` — on init, asset reload, or `EntityStatMap.update()`
- **Implication**: Override `computeModifiers()` is the correct extension point

### D-4: Custom EntityStatMap — Not Needed
- **Decision**: Do not register custom EntityStatMap component type
- **Rationale**: Option C persistence strategy handles codec concerns without custom component
- **Implication**: Use post-load swap approach exclusively

### D-5: Modifier Unification — HyforgedModifier Only
- **Decision**: Unify on `HyforgedModifier`, deprecate `StatModifier`
- **Migration**:
  - `StatModifier` record → `HyforgedModifier` class
  - `ModifierType` enum → `HyforgedModifier.StackType`
  - `ModifierSource` enum → `HyforgedModifier.SourceType`
  - All 8+ modifier creation sites updated
- **Implication**: Removes dual-modifier complexity; single source of truth

### D-6: HyforgedStatComponent Future — Lightweight Companion
- **Decision**: `HyforgedStatComponent` becomes supplementary, not primary

**Keep:**
| Responsibility | Reason |
|----------------|--------|
| Base value storage (ability scores) | EntityStatValue has no "base value" concept separate from asset defaults |
| Conditional modifiers | EntityStatValue has no conditional modifier support |
| Change buffer / event coalescing | EntityStatValue has no event system |
| HUD state tracking | Still needed for UI delta updates |

**Remove:**
| Responsibility | Reason |
|----------------|--------|
| StatModifier list | Replaced by EntityStatValue.modifiers with HyforgedModifier |
| Cached computed values | EntityStatValue tracks its own value |
| Bridge state tracking | Bridge systems deprecated |

- **Suggested rename**: `HyforgedStatExtensionComponent` to clarify supplementary role

## Acceptance Criteria
- [ ] HyforgedStatValue compiles and extends EntityStatValue
- [ ] HyforgedStatValue adds NO persistent fields (transient only)
- [ ] ARPG stacking (FLAT/INCREASED/MORE/CAP) works correctly
- [ ] HyforgedModifier added via `putModifier()` is processed by HyforgedStatValue
- [ ] Existing StaticModifier continues to work
- [ ] Items using HyforgedModifier in JSON affect stats correctly
- [ ] Buffs using HyforgedModifier work without custom wrapper code
- [ ] StatModifier class deprecated and removed
- [ ] HyforgedStatComponent simplified to companion role
- [ ] Performance: no measurable regression in stat computation
- [ ] At least one bridge system can be removed
- [ ] All 29+ test files updated and passing

---

## Comprehensive Impact Analysis

**Total Impact: 75 files (46 source + 29 test)**

### Impact Summary by Dependency Type

| Type | Source Files | Test Files | Total |
|------|--------------|------------|-------|
| StatDefinition | 3 | 2 | 5 |
| StatModifier | 8 | 4 | 12 |
| HyforgedStatComponent | 17 | 1 | 18 |
| StatDefinitionRegistry | 16 | 4 | 20 |
| HyforgedModifier | 4 | 11 | 15 |
| Bridge Systems | 4 | 0 | 4 |

### 1. Stats System (Core Impact) — 25 source files, 10 test files

#### 1.1 Component Package (CRITICAL)
| File | Impact |
|------|--------|
| `HyforgedStatComponent.java` | **CRITICAL** - Core refactoring target. May delegate to EntityStatMap |
| `StatModifier.java` | **HIGH** - May be replaced by HyforgedModifier storage |
| `ConditionalStatModifier.java` | **HIGH** - Needs migration strategy |
| `ModifierType.java` | **Medium** - Maps to HyforgedModifier.StackType |
| `ModifierSource.java` | **Low** - May move to HyforgedModifier |
| `ActiveEffectState.java` | **Medium** - Bridge may be simplified |

#### 1.2 System Package (HIGH IMPACT)
| File | Impact |
|------|--------|
| `HyforgedStatComputeSystem.java` | **CRITICAL** - May be replaced by HyforgedStatValue.computeModifiers() |
| `HyforgedStatInitSystem.java` | **HIGH** - Needs to install HyforgedStatValue |
| `HyforgedBridgeSystem.java` | **HIGH** - Candidate for deprecation |
| `HyforgedEffectBridgeSystem.java` | **HIGH** - May be simplified |
| `HyforgedConditionalModifierSystem.java` | **Medium** - Changes to use HyforgedModifier |

#### 1.3 Bridge Package (DEPRECATION CANDIDATES)
| File | Impact |
|------|--------|
| `HyforgedDamageReductionSystem.java` | **HIGH** - Primary deprecation target |
| `HyforgedKnockbackReductionSystem.java` | **HIGH** - Deprecation candidate |
| `ProgressionStatBridge.java` | **Medium** - Partial deprecation |
| `HytaleSystemReplacer.java` | **LOW** - Still needed |

#### 1.4 Engine Package
| File | Impact |
|------|--------|
| `StackingEngine.java` | **HIGH** - Logic moves to HyforgedStatValue |
| `ScalingEngine.java` | **Low** - May be reused |
| `RatingConverter.java` | **Low** - Unchanged |

#### 1.5 Other Stats Files
| File | Impact |
|------|--------|
| `HyforgedStatCodec.java` | **HIGH** - Must adapt to HyforgedStatValue |
| `HyforgedStatQueryService.java` | **HIGH** - May need codec changes |
| `StatDataMigrator.java` | **Medium** - Migration logic |
| `NPCStatInitSystem.java` | **Medium** - NPC stat initialization |
| `ResourceStatsHudSystem.java` | **Low** - Reads computed values |
| `StatDebugCommand.java` | **Low** - Debug only |
| `CharacterStatsCommand.java` | **Low** - Admin commands |
| `EffectModifierProcessor.java` | **Medium** - Effect processing |
| `HyforgedModifier.java` | **CRITICAL** - Core of new integration |

### 2. Combat System — 8 source files, 6 test files

| File | Impact |
|------|--------|
| `HyforgedHitResolutionSystem.java` | **HIGH** - Direct stat reads |
| `CriticalHitSystem.java` | **HIGH** - Crit chance calculation |
| `BlockSystem.java` | **HIGH** - Block chance |
| `CombatServiceImpl.java` | **HIGH** - Combat API |
| `HyforgedHealingSystem.java` | **Medium** - Healing calculation |
| `AilmentSystem.java` | **Medium** - Ailment processing |
| `MonsterScalingService.java` | **HIGH** - Monster stat scaling |
| `MonsterScalingConfig.java` | **Low** - Data record |

**Combat Migration**: All 8 files read from HyforgedStatComponent. Must migrate to reading from EntityStatMap with HyforgedStatValue.

### 3. Affix System — 9 source files, 12 test files

| File | Impact |
|------|--------|
| `EquipmentAffixListener.java` | **HIGH** - Creates stat modifiers |
| `AffixTooltipService.java` | **Medium** - Tooltip generation |
| `AffixRollerService.java` | **Low** - Affix rolling |
| `AffixService.java` | **Low** - Registry |
| `AffixDefinition.java` | **Medium** - Affix definition |
| `RolledAffix.java` | **HIGH** - Creates HyforgedModifier |
| `AffixAssetLoader.java` | **Medium** - JSON loading |
| `CharacterStatsPage.java` | **HIGH** - UI display |
| `AffixEquipEvent.java` | **Medium** - Event with modifiers |

**Affix Migration**: `EquipmentAffixListener` and `RolledAffix` create modifiers. `CharacterStatsPage` reads all stat data for display.

### 4. Progression System — 3 source files, 1 test file

| File | Impact |
|------|--------|
| `ProgressionComponent.java` | **Low** - Reads class definitions |
| `XPAwardCompletion.java` | **Low** - XP awards |
| `ProgressionDebugCommand.java` | **Low** - Debug only |

**Low direct impact** - Primarily uses ClassDefinition, not modifiers.

### 5. Plugin Registration — 1 file

| File | Impact |
|------|--------|
| `HyforgedPlugin.java` | **HIGH** - Must add HyforgedStatValue installer registration |

### 6. Buff/Effect System

**Finding**: No standalone buff system exists. Buffs are handled through Hytale's EntityEffect system via `HyforgedEffectBridgeSystem`.

---

## Key Architectural Observations

### Dual Modifier Problem
Two parallel modifier types currently exist:
- `StatModifier` (internal Hyforged component storage) — records in HyforgedStatComponent
- `HyforgedModifier` (Hytale-compatible, extends Modifier) — registered with Modifier.CODEC

**Refactoring should unify on HyforgedModifier** stored in EntityStatMap.modifiers.

### Bridge Systems Are Workarounds
The 4 bridge systems exist specifically to sync Hyforged stats with Hytale systems. These are the primary reason for the "re-writing the whole game" complaint and are prime deprecation candidates.

### ProgressionStatBridge Widely Used
4 combat files depend on this for level lookups. Needs graceful migration path.

### Heavy Test Coverage
29+ test files will need updates, but good coverage enables safer refactoring.

---

## Impacted Areas (Detailed)

### Direct Dependencies (Must Change) — 17 files
| System | Files |
|--------|-------|
| Stats Core | HyforgedStatComponent, StatModifier, HyforgedStatComputeSystem, HyforgedStatInitSystem, StackingEngine |
| Stats Bridge | HyforgedBridgeSystem, HyforgedEffectBridgeSystem, HyforgedDamageReductionSystem, HyforgedKnockbackReductionSystem |
| Stats Persistence | HyforgedStatCodec, HyforgedStatQueryService |
| Plugin | HyforgedPlugin |
| Combat | HyforgedHitResolutionSystem, CombatServiceImpl |
| Affix | EquipmentAffixListener, CharacterStatsPage |

### Indirect Dependencies (May Need Review) — 15 files
Files that read stats but don't create modifiers. May work with minimal changes if HyforgedStatComponent continues to expose computed values or delegates to HyforgedStatValue.

### Test Code (Must Update) — 29+ test files
All tests using HyforgedStatComponent, StatModifier, or integration tests.

---

## Required Codebase/Architecture Changes (Detailed)

### Phase 1: Core
1. Create `HyforgedStatValue extends EntityStatValue`
2. Implement `computeModifiers()` with ARPG stacking
3. Create installer system to swap EntityStatValue → HyforgedStatValue

### Phase 2: Component Adaptation
1. Update `HyforgedStatComponent` to delegate to `EntityStatMap` for stat storage
2. Keep `HyforgedStatComponent` for:
   - Base value storage (ability scores)
   - Conditional modifier storage
   - Event coalescing
   - Dirty flag tracking

### Phase 3: Modifier Unification
1. Migrate from `StatModifier` to `HyforgedModifier` storage
2. Update all modifier creation sites
3. Remove `StatModifier` class

### Phase 4: Bridge Deprecation
1. Deprecate `HyforgedDamageReductionSystem`
2. Deprecate `HyforgedKnockbackReductionSystem`
3. Simplify `HyforgedBridgeSystem`
4. Update `ProgressionStatBridge` usage

### Phase 5: Consumer Migration
1. Update combat systems to read from EntityStatMap
2. Update affix system to add modifiers via EntityStatMap.putModifier()
3. Update UI/HUD to read from appropriate source

### Phase 6: Test Updates
1. Update 29+ test files
2. Add new integration tests for HyforgedStatValue
3. Verify all existing functionality preserved

---

## Code Removal Analysis

**Summary: ~2,500-3,000 lines removable (20-24% of stats package)**

### Current Stats Package Size
| Category | Lines |
|----------|-------|
| Source code | 12,651 |
| Test code | 2,360 |
| **Total** | **15,011** |

### Files to Remove Entirely (~1,500 lines)

| File | Lines | Reason |
|------|-------|--------|
| StackingEngine.java | 441 | Logic moves to `HyforgedStatValue.computeModifiers()` |
| HyforgedStatComputeSystem.java | 340 | Replaced by `EntityStatValue.computeModifiers()` auto-trigger |
| HyforgedBridgeSystem.java | 291 | No longer needed — Hytale sees HyforgedStatValue directly |
| StatModifier.java | 218 | Replaced by HyforgedModifier |
| ModifierType.java | ~50 | Replaced by HyforgedModifier.StackType |
| **Subtotal** | **~1,340** | |

### Files to Deprecate (Remove Later) (~740 lines)

| File | Lines | Reason |
|------|-------|--------|
| HyforgedDamageReductionSystem.java | 287 | Hytale damage system uses EntityStatValue directly |
| ProgressionStatBridge.java | 243 | Progression reads from EntityStatMap directly |
| HyforgedKnockbackReductionSystem.java | 125 | Same pattern — Hytale uses EntityStatValue |
| HytaleSystemReplacer.java | 85 | No longer replacing systems |
| **Subtotal** | **~740** | |

### Files to Simplify Significantly (~400-500 lines removed)

| File | Current Lines | Est. Removal | Keep |
|------|---------------|--------------|------|
| HyforgedStatComponent.java | 689 | ~400 | Base values, conditional mods, events |
| HyforgedEffectBridgeSystem.java | 310 | ~100 | Effects add modifiers directly to EntityStatValue |
| **Subtotal** | 999 | **~500** | |

### Files Unchanged (Core Keepers)

| File | Lines | Reason |
|------|-------|--------|
| StatDefinitionRegistry.java | 658 | Core registry — unchanged |
| HyforgedModifier.java | 368 | **Unified modifier type** — enhanced |
| StatDefinition.java | 285 | Core data model — unchanged |
| RatingConverter.java | 243 | Rating math — unchanged |
| StatBreakdown.java | 243 | Debug/UI — unchanged |
| ScalingEngine.java | 201 | Level scaling logic — unchanged |
| (debug, npc, assets, etc.) | ~3,500 | Mostly unchanged |

### New Code Added

| File | Est. Lines | Purpose |
|------|-----------|---------|
| HyforgedStatValue.java | ~200-250 | Extends EntityStatValue, implements computeModifiers() |
| HyforgedStatValueInstaller.java | ~100-150 | Swaps EntityStatValue → HyforgedStatValue post-load |
| **Subtotal** | **~350-400** | |

### Net Change Summary

```
Removed entirely:      ~1,500 lines
Deprecated (future):   ~740 lines  
Simplified:            ~500 lines
Added:                 ~350-400 lines
─────────────────────────────────────
Net Removal:           ~1,800-2,300 lines
```

**Before:** 12,651 lines (source) + 2,360 lines (test) = **15,011 total**  
**After:**  ~10,500 lines (source) + ~1,800 lines (test) = **~12,300 total**

### Qualitative Improvements

Beyond raw line counts:
1. **Eliminates bridge pattern** — No more "copy value from A to B" systems
2. **Single source of truth** — `EntityStatValue` IS the value, not a copy
3. **Automatic integration** — Hytale's combat, buffs, items use our stats without bridges
4. **Simpler debugging** — One modifier list, one computation path
5. **Reduced tick overhead** — No dirty flag checking, no system coordination
6. **Extensibility** — New Hytale features (items, effects) work automatically

---

## Cross-System Simplification Impact

This change ripples across Combat, Affix, and Progression systems.

### Combat System (7,574 lines, 39 files)

**Current Pattern:**
```java
// Get Hyforged component
HyforgedStatComponent defenderStats = store.getComponent(ref, statComponentType);
// Read cached value by index
int evasion = defenderStats.getCachedValue(evasionIndex);
```

**After Migration:**
```java
// Get Hytale's native component  
EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
// Read directly from EntityStatValue
int evasion = (int) statMap.get(evasionIndex).get();
```

| Impact Area | Files | Change Type | Simplification |
|-------------|-------|-------------|----------------|
| Stat reads (`getCachedValue`) | 3 files | Migrate to `EntityStatMap.get()` | ~50 lines changed |
| `HyforgedStatComponent` dependency | 5 files | Remove import/access | Minor cleanup |
| `StatModifier` usage | 1 file (HyforgedMonsterScalingSystem) | Replace with `HyforgedModifier` | ~30 lines simplified |
| **Net Change** | | | **~80 lines simplified** |

**Key Win:** Combat systems become **native Hytale consumers** — they read from `EntityStatMap` like any standard Hytale system.

### Affix System (7,739 lines, 41 files)

**Current Pattern (EquipmentAffixListener):**
```java
// Get Hyforged component
HyforgedStatComponent statComponent = getStatComponent(entity);
// Create internal StatModifier
StatModifier modifier = new StatModifier(
    sourceId, ModifierSource.EQUIPMENT, modifierType, statIndex, ...);
// Add to Hyforged component
statComponent.addModifier(modifier);
```

**After Migration:**
```java
// Get Hytale's native component
EntityStatMap statMap = entity.getStatMap();
// Create HyforgedModifier directly
HyforgedModifier modifier = new HyforgedModifier(statId, stackType, value);
// Add via standard Hytale API
statMap.putModifier(statIndex, sourceId, modifier);
```

| Impact Area | Files | Change Type | Simplification |
|-------------|-------|-------------|----------------|
| `EquipmentAffixListener` | 1 file (309 lines) | Major rewrite | **~100 lines removed** |
| `CharacterStatsPage` UI | 1 file (433 lines) | Migrate stat reads + modifier breakdown | ~80 lines simplified |
| `StatModifier` usage | 3 files | Replace with `HyforgedModifier` | ~60 lines |
| Type conversion (`convertModifierType`) | 1 file | Remove entirely | ~15 lines removed |
| **Net Change** | | | **~250-300 lines removed** |

**Key Wins:**
- **Remove `convertModifierType()`** — no more mapping between `HyforgedModifier.StackType` and internal `ModifierType`
- **Remove `StatModifier` dependency** — affixes create `HyforgedModifier` directly
- **Standard API for modifier add/remove** — use `EntityStatMap.putModifier()`/`removeModifier()`

### Progression System (4,380 lines, 38 files)

**Current State:** Already minimal stat integration. `ProgressionStatBridge` provides level lookups.

| Impact Area | Files | Change Type | Simplification |
|-------------|-------|-------------|----------------|
| `ProgressionStatBridge` | 1 file (248 lines) | Keep as-is | No change |
| Class level modifiers | Via `ClassLevelModifierSystem` | Uses stat system | Indirect benefit |
| **Net Change** | | | **~0 lines** |

**Key Win:** No direct changes needed. Progression benefits from simpler stat system for future integrations.

### Cross-System Summary

| System | Files | Current Lines | Est. Removal | Primary Benefit |
|--------|-------|---------------|--------------|-----------------|
| Stats | 74 | 12,651 | ~2,000 | Core simplification |
| Combat | 39 | 7,574 | ~80 | Native Hytale integration |
| Affix | 41 | 7,739 | ~250-300 | Remove type conversion, standard API |
| Progression | 38 | 4,380 | ~0 | Indirect benefit |
| **Total** | **192** | **32,344** | **~2,330-2,380** | |

### Architectural Benefits

1. **Single Modifier Type**: Remove `StatModifier` ↔ `HyforgedModifier` conversion everywhere
2. **Standard Hytale APIs**: Items, buffs, effects use `EntityStatMap.putModifier()` — no special Hyforged path
3. **No Component Double-Access**: Combat reads from ONE component (`EntityStatMap`), not two
4. **UI Simplification**: `CharacterStatsPage` reads modifiers from `EntityStatValue.getModifiers()` directly
5. **Future-Proof**: New Hytale features (buffs, items, skills) automatically work with Hyforged stats

---

## References
- ADR-0001: Hybrid Hyforged + Hytale Stats (Superseded by ADR-0010)
- ADR-0002: Extend Hytale Modifier System
- ADR-0010: Unified Stat Integration via EntityStatValue Extension
- Feature Spec: hyforged-stats-system
- Hytale source: `EntityStatValue.java`, `EntityStatMap.java`, `Modifier.java`
