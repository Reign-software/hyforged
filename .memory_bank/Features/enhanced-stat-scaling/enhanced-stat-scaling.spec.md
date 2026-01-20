# Feature Spec: Enhanced Stat Scaling

## Metadata
- Feature ID (slug): enhanced-stat-scaling
- Status: Draft
- Owner: JBurl
- Date: 2026-01-19
- Extends: hyforged-stats-system

## Summary
Extend the existing Hyforged Stats System with data-driven stat scaling, dependency graph evaluation, and unified ability score handling. This enables derived stats (e.g., Attack Power) to scale from other stats (e.g., Strength) using configurable rules defined in JSON, with support for linear, threshold, and diminishing returns scaling types.

## Goals
- Enable stats to declare scaling rules that compute their base value from other stats.
- Unify ability scores as first-class stats (tagged `attribute`) rather than a separate data structure.
- Build a dependency graph (DAG) at stat registration to determine evaluation order.
- Support three scaling types: linear, threshold, and diminishing returns.
- Keep scaling fully data-driven (JSON) for modder extensibility.
- Ensure scaling uses post-modifier values of source stats (not raw bases).

## Non-Goals
- Full expression language for formulas (keep scaling types simple and composable).
- Breaking changes to the existing modifier stacking engine.
- Client-side stat computation.

## User Experience
- Players see the same stat categories and breakdowns as before.
- When inspecting a derived stat (e.g., Attack Power), the breakdown shows:
  - Scaling contribution from source stats (e.g., "+40 from Strength")
  - Then modifiers (flat, %, more/less, caps) as before.
- Modders can define new stats that scale from existing stats without Java code.

## Functional Requirements

### 1. Unified Ability Scores
- Remove the separate `int[7] abilityScores` array from `HyforgedStatComponent`.
- Ability scores become regular stats:
  - Tagged with `attribute`.
  - Category: `Attributes`.
  - Base value stored per-entity (allocated points or template value).
- Entity stat container stores base values keyed by stat index for stats that use `defaultValue` (no scaling).

### 2. Scaling Model
A stat uses **either** `defaultValue` **or** scaling rules—not both.

#### Scaling Types

| Type | JSON Fields | Formula | Use Case |
|------|-------------|---------|----------|
| **linear** | `source`, `ratio` | `contribution = sourceValue * ratio` | 1 STR = 2 Attack Power → `ratio: 2.0` |
| **threshold** | `source`, `perPoints`, `bonusBps` | `contribution = floor(sourceValue / perPoints) * bonusBps` | 5 LCK = 100 bps (1%) crit → `perPoints: 5, bonusBps: 100` |
| **diminishing** | `source`, `curve`, `scale`, `capBps` | Uses `RatingConverter` formula with cap | Crit rating → crit chance with 75% cap |

- Multiple scaling rules on one stat are additive (sum contributions).
- Scaling contributions are computed from the **final** (post-modifier) values of source stats.
- Modifiers on the derived stat apply **after** scaling computes the base.

#### JSON Schema
```json
{
  "id": "hyforged:attack-power",
  "displayName": "Attack Power",
  "category": "offense",
  "tags": ["offense"],
  "scaling": [
    { "source": "hyforged:strength", "type": "linear", "ratio": 2.0 }
  ]
}
```

```json
{
  "id": "hyforged:crit-chance-bps",
  "displayName": "Critical Strike Chance",
  "category": "critical",
  "tags": ["critical"],
  "displayFormat": "BPS_PERCENT",
  "scaling": [
    { "source": "hyforged:luck", "type": "threshold", "perPoints": 5, "bonusBps": 100 }
  ],
  "maxValue": 7500
}
```

### 3. Dependency Graph (DAG)
- At stat registration time, build a directed acyclic graph from scaling rules.
- Each scaling rule creates an edge: `source → target`.
- Detect circular dependencies at registration and reject with an error.
- Store topological order for efficient evaluation.

### 4. Evaluation Order
When recomputing stats:
1. Collect dirty stats and expand to include all transitive dependents.
2. Sort affected stats in topological order (sources before dependents).
3. For each stat in order:
   a. Compute base value:
      - If no scaling: use stored base value (for attributes) or `defaultValue`.
      - If scaling: sum contributions from source stats (using their cached final values).
   b. Apply modifiers in type order (flat → % → more/less → caps).
   c. Cache the final value.
4. Emit change events for stats whose values changed.

### 5. Base Value Storage
- For stats with no scaling (e.g., ability scores), the entity stores a base value.
- This replaces the current `int[7] abilityScores` array with a more general mechanism.
- Options:
  - **A**: Store base values in a sparse map keyed by stat index.
  - **B**: Store base values in the same `cachedValues` array but distinguish via a separate "base" pass.
- Recommended: Option A (sparse map) for clarity and to support entities with different stat sets.

### 6. API Additions
Extend `StatDefinition` and `StatDefinitionRegistry`:

```java
// StatDefinition additions
public record StatDefinition(
    // ... existing fields ...
    List<ScalingRule> scaling  // NEW: empty list means use defaultValue
) { }

// ScalingRule record
public sealed interface ScalingRule {
    StatId source();
    
    record Linear(StatId source, double ratio) implements ScalingRule {}
    record Threshold(StatId source, int perPoints, int bonusBps) implements ScalingRule {}
    record Diminishing(StatId source, String curve, double scale, int capBps) implements ScalingRule {}
}

// StatDefinitionRegistry additions
public interface StatDefinitionRegistry {
    // ... existing methods ...
    
    /** Get stats that depend on this stat (reverse lookup). */
    Set<Integer> getDependentStats(int statIndex);
    
    /** Get topological order for evaluation. */
    int[] getEvaluationOrder();
    
    /** Check if stat has scaling (vs using defaultValue). */
    boolean hasScaling(int statIndex);
}
```

Extend `HyforgedStatComponent`:

```java
public class HyforgedStatComponent {
    // REMOVE: int[] abilityScores
    
    // ADD: Base values for stats without scaling (e.g., attributes)
    private final Int2IntMap baseValues = new Int2IntOpenHashMap();
    
    /** Get base value for a stat (returns defaultValue if not set). */
    public int getBaseValue(int statIndex);
    
    /** Set base value for a stat (e.g., allocating attribute points). */
    public void setBaseValue(int statIndex, int value);
}
```

### 7. Scaling Engine
New class to compute scaling contributions:

```java
public final class ScalingEngine {
    
    /**
     * Compute base value for a stat from scaling rules.
     * @param statDef The stat definition
     * @param sourceValueProvider Function to get final value of a source stat
     * @return Computed base value (sum of contributions)
     */
    public static int computeScaledBase(
        StatDefinition statDef,
        IntUnaryOperator sourceValueProvider
    );
    
    /**
     * Compute contribution from a single scaling rule.
     */
    public static int computeContribution(ScalingRule rule, int sourceValue);
}
```

### 8. Asset Schema Changes
Update `StatDefinitionAsset` to parse scaling rules:

```json
{
  "$schema": "...",
  "type": "object",
  "properties": {
    "id": { "type": "string" },
    "displayName": { "type": "string" },
    "category": { "type": "string" },
    "tags": { "type": "array", "items": { "type": "string" } },
    "defaultValue": { "type": "integer" },
    "minValue": { "type": "integer" },
    "maxValue": { "type": "integer" },
    "displayFormat": { "type": "string" },
    "isRating": { "type": "boolean" },
    "scaling": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "source": { "type": "string" },
          "type": { "enum": ["linear", "threshold", "diminishing"] },
          "ratio": { "type": "number" },
          "perPoints": { "type": "integer" },
          "bonusBps": { "type": "integer" },
          "curve": { "type": "string" },
          "scale": { "type": "number" },
          "capBps": { "type": "integer" }
        },
        "required": ["source", "type"]
      }
    }
  }
}
```

## Non-Functional Requirements
- **Determinism**: Topological order is stable across restarts.
- **Performance**: DAG construction is O(V + E) at registration; evaluation uses cached order.
- **Backward Compatibility**: Existing stats without scaling continue to work unchanged.

## Dependencies
- Existing: `hyforged-stats-system` (modifier engine, stacking, registry)
- Existing: `StatAssetLoader` (JSON asset loading)
- Existing: `RatingConverter` (for diminishing returns curves)

## Data/Schema Impact
- **StatDefinition**: Add `scaling` field (list of rules).
- **HyforgedStatComponent**: Replace `abilityScores` array with `baseValues` map.
- **JSON assets**: Add optional `scaling` array to stat definitions.

## API Changes
- `StatDefinition`: Add `scaling()` method.
- `StatDefinitionRegistry`: Add `getDependentStats()`, `getEvaluationOrder()`, `hasScaling()`.
- `HyforgedStatComponent`: Add `getBaseValue()`, `setBaseValue()`; deprecate ability score accessors.

## Security/Privacy
- No new attack surface; scaling is computed server-side from validated definitions.

## Observability
- Debug trace includes scaling contributions in stat breakdown.
- Log warning if stat definition has both `defaultValue != 0` and non-empty `scaling`.

## Risks
- **Complexity**: DAG evaluation adds complexity to the compute system.
- **Migration**: Existing ability score data must migrate to new base value storage.
- **Modder confusion**: Clear documentation needed on scaling vs modifiers.

## Open Questions
- ~~Should ability scores be first-class stats?~~ → **Yes** (decided)
- ~~Data-driven or code-driven formulas?~~ → **Data-driven** (decided)
- ~~Exclusive base model (defaultValue OR scaling)?~~ → **Yes, exclusive** (decided)

## Acceptance Criteria
- [ ] Ability scores are registered as stats with tag `attribute` and category `Attributes`.
- [ ] `HyforgedStatComponent` no longer uses separate `abilityScores` array.
- [ ] Stats can define `scaling` rules in JSON.
- [ ] Linear scaling: Attack Power correctly computes from Strength.
- [ ] Threshold scaling: Crit chance correctly computes from Luck (5 LCK = 1%).
- [ ] Diminishing returns scaling: Uses `RatingConverter` and respects cap.
- [ ] Circular dependencies are detected and rejected at registration.
- [ ] Topological evaluation order is correct (sources before dependents).
- [ ] Scaling uses post-modifier values of source stats.
- [ ] Stat breakdown shows scaling contribution as a separate line.
- [ ] Existing stats without scaling continue to work.
- [ ] Migration path for existing ability score data.

## Impacted Areas (High-Level)
- `StatDefinition` and `StatDefinitionRegistry`
- `HyforgedStatComponent`
- `HyforgedStatComputeSystem`
- `StatAssetLoader` / `StatDefinitionAsset`
- JSON stat definition files
- UI breakdown display

## Required Codebase/Architecture Changes (High-Level)

| Component | Change |
|-----------|--------|
| `StatDefinition` | Add `scaling` field (list of `ScalingRule`) |
| `ScalingRule` | New sealed interface with `Linear`, `Threshold`, `Diminishing` records |
| `ScalingEngine` | New utility class for computing scaling contributions |
| `StatDefinitionRegistry` | Build DAG, detect cycles, store topological order, add dependency lookups |
| `HyforgedStatComponent` | Replace `abilityScores` with `baseValues` map; add accessors |
| `HyforgedStatComputeSystem` | Evaluate in topological order; compute scaled base before modifiers |
| `StatDefinitionAsset` | Parse `scaling` array from JSON |
| `StatAssetLoader` | Validate scaling rules; resolve source stat IDs |
| `StatBreakdown` | Add scaling contribution line item |
| JSON assets | Update ability score definitions with `attribute` tag; add `scaling` to derived stats |

## References
- Requirements: [stats-system.md](../../Requirements/rpg-arpg/stats-system.md)
- Requirements: [entity-stats.md](../../Requirements/rpg-arpg/entity-stats.md)
- Base feature: [hyforged-stats-system.spec.md](../hyforged-stats-system/hyforged-stats-system.spec.md)
- ADR: ADR-0001 (Hybrid Hyforged + Hytale stats)
