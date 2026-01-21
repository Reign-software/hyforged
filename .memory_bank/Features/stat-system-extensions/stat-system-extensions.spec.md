# Feature Spec: Stat System Extensions

## Metadata
- Feature ID (slug): stat-system-extensions
- Status: Draft
- Owner: JBurl
- Date: 2026-01-20

## Summary
Extensions to the Hyforged Stats System enabling:
1. **Soft/Hard Caps**: Dynamic stat caps where other stats can raise the soft cap up to a hard cap
2. **EntityEffect Bridge**: Integration between Hytale's native effect system and Hyforged's tag-based stat modifiers
3. **Duration Scaling**: Effect durations scale based on `EffectDuration` stat

These extensions are prerequisites for the Combat System feature.

## Goals
- Enable combat stats (crit, block, evasion, resistances) to have soft caps that can be raised by other stats
- Allow EntityEffects to benefit from Hyforged's tag-based modifier system
- Provide duration scaling for effects via the Hyforged stats framework

## Non-Goals
- Replace Hytale's EntityEffect system entirely
- Add new visual or audio effects (leverage Hytale's existing effect visuals)
- Modify effect stacking/overlap behavior (use Hytale's native behaviors)

## User Experience
- Players with "+Max Crit Chance" stats can exceed the default crit cap
- Status effects that grant "+10% elemental damage" apply to all elemental damage types via tags
- Effects last longer when players have "+Effect Duration" bonuses

## Functional Requirements

### FR-1: Soft/Hard Cap System

Stats may define:
- **Soft Cap**: Default maximum value (e.g., 50% crit chance)
- **Hard Cap**: Absolute maximum that cannot be exceeded (e.g., 95% crit chance)
- **Soft Cap Bonus Stat**: Another stat that raises the soft cap

**Computation Order:**
1. Normal stacking (FLAT → INCREASED → MORE → CAP modifiers)
2. Calculate effective soft cap = softCap + bonusStatValue
3. Clamp effective soft cap to hardCap
4. Clamp computed value to effective soft cap

**Example:**
```
Base crit chance: 30%
+25% from gear: 55%
Soft cap: 50%
Hard cap: 95%
MaxCritChance stat: +15%

Without MaxCritChance: crit = 50% (clamped to soft cap)
With MaxCritChance: effective soft cap = 50% + 15% = 65%, crit = 55%
```

### FR-2: EntityEffect Bridge

Hytale effects that apply stat modifiers should integrate with Hyforged:

**Effect Application:**
1. When `EffectControllerComponent` gains an effect with `StatModifiers`
2. Bridge creates corresponding `StatModifier` entries in `HyforgedStatComponent`
3. Source ID format: `"effect:{effectId}"`
4. Modifiers removed when effect ends

**Tag Support:**
- Effect modifiers targeting tags apply to all stats with that tag
- Example: Effect with "+10% elemental" applies to fire, cold, lightning damage

**Hybrid Approach:**
- Hytale's EntityEffect handles: visuals, sounds, DoT damage, client sync
- Hyforged handles: stat modifier stacking, tag resolution, breakdown attribution

### FR-3: Duration Scaling

Effects applied through combat system can scale duration:

**Formula:**
```
scaledDuration = baseDuration × (1 + effectDurationBps / 10000)
```

**Constraints:**
- Minimum duration: 0.1 seconds
- Only applies to effects applied via `HyforgedEffectService`
- Existing Hytale effect applications unchanged

## Non-Functional Requirements

- **Performance**: Effect bridge runs only when effects change (not every tick)
- **Determinism**: Soft/hard cap calculation is deterministic
- **Backward Compatibility**: Stats without cap metadata use existing behavior

## Dependencies

- Hyforged Stats System (complete)
- Hytale `EffectControllerComponent`
- Hytale `EntityEffect` assets

## Data/Schema Impact

### StatDefinition Extensions

New optional fields in stat definition JSON:
```json
{
  "Id": "hyforged:crit-chance-bps",
  "SoftCapBps": 5000,
  "HardCapBps": 9500,
  "SoftCapBonusStat": "hyforged:max-crit-chance-bps"
}
```

### New Stats

| Stat ID | Purpose | Default | Soft Cap | Hard Cap |
|---------|---------|---------|----------|----------|
| `max-crit-chance-bps` | Raises crit soft cap | 0 | - | 4500 |
| `max-block-chance-bps` | Raises block soft cap | 0 | - | 2500 |
| `max-evasion-chance-bps` | Raises evasion soft cap | 0 | - | 2500 |
| `max-fire-resistance-bps` | Raises fire res soft cap | 0 | - | 1500 |
| `max-cold-resistance-bps` | Raises cold res soft cap | 0 | - | 1500 |
| `max-lightning-resistance-bps` | Raises lightning res soft cap | 0 | - | 1500 |
| `effect-duration-bps` | Scales effect durations | 0 | - | - |

### Combat Stats with Caps

| Stat ID | Soft Cap | Hard Cap | Bonus Stat |
|---------|----------|----------|------------|
| `crit-chance-bps` | 5000 | 9500 | `max-crit-chance-bps` |
| `block-chance-bps` | 7500 | 9000 | `max-block-chance-bps` |
| `evasion-chance-bps` | 5000 | 7500 | `max-evasion-chance-bps` |
| `fire-resistance-bps` | 7500 | 9000 | `max-fire-resistance-bps` |
| `cold-resistance-bps` | 7500 | 9000 | `max-cold-resistance-bps` |
| `lightning-resistance-bps` | 7500 | 9000 | `max-lightning-resistance-bps` |

## API Changes

### StatDefinition

```java
public record StatDefinition(
    // ... existing fields ...
    int softCapBps,           // NEW: soft cap in basis points (-1 = no cap)
    int hardCapBps,           // NEW: hard cap in basis points (-1 = no cap)
    @Nullable StatId softCapBonusStat  // NEW: stat that raises soft cap
)
```

### StackingEngine

```java
// Updated signature
public static int compute(
    int baseValue, 
    List<StatModifier> modifiers, 
    StatDefinition statDef,
    IntFunction<Integer> statValueProvider  // NEW: for reading bonus stat
);
```

### HyforgedEffectService

```java
public class HyforgedEffectService {
    /**
     * Apply an effect with duration scaling.
     */
    public static void applyEffect(
        Ref<EntityStore> entityRef,
        String effectId,
        float baseDuration,
        ComponentAccessor<EntityStore> accessor
    );
}
```

## Security/Privacy
- No additional security concerns
- Effect modifiers are server-authoritative

## Observability
- Stat breakdown UI shows cap information
- Debug logging for effect bridge add/remove operations

## Risks
| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Effect state tracking bugs | Medium | Medium | Comprehensive unit tests |
| Performance regression | Low | Medium | Early-exit optimization, profiling |
| Hytale API changes | Low | High | Loose coupling, interface abstraction |

## Open Questions
1. Should negative effect duration bonuses be allowed? (Current answer: Yes, with minimum floor)
2. Should the effect bridge be opt-in per effect type? (Current answer: No, applies to all)

## Acceptance Criteria
- [ ] Stats with soft caps respect the cap unless bonus stat raises it
- [ ] Stats cannot exceed hard cap regardless of bonuses
- [ ] EntityEffect stat modifiers appear in Hyforged stat breakdowns
- [ ] Tag-based effect modifiers apply to all matching stats
- [ ] `EffectDuration` stat scales effect durations correctly
- [ ] All unit tests pass
- [ ] Build passes

## Impacted Areas
- `reign.software.hyforged.stats` — StatDefinition, StackingEngine extensions
- `reign.software.hyforged.stats.system` — New HyforgedEffectBridgeSystem
- `reign.software.hyforged.stats.service` — New HyforgedEffectService
- `src/main/resources/Server/Hyforged/Stats/` — Combat stat definitions with caps

## Required Codebase/Architecture Changes

### Modified Files
- `StatDefinition.java` — Add cap fields
- `StatDefinition.Builder` — Add cap builder methods
- `StatDefinitionAsset.java` — Add codec for cap fields
- `StackingEngine.java` — Add cap logic to compute methods
- `ModifierSource.java` — Add EFFECT source type

### New Files
- `HyforgedEffectBridgeSystem.java` — ECS system for effect → stat bridging
- `EffectBridgeComponent.java` — Pure data component for tracking effect state
- `HyforgedEffectService.java` — Utility for applying effects with duration scaling
- `src/main/resources/Server/Hyforged/Stats/CombatCaps.json` — Combat stat cap definitions

### New JSON Assets
- Effect duration stat definition
- Max cap bonus stat definitions
- Updated combat stat definitions with cap metadata

## References
- Combat System Spec: [combat-system.spec.md](../combat-system/combat-system.spec.md) (Appendix A)
- Stats System Spec: [hyforged-stats-system.spec.md](../hyforged-stats-system/hyforged-stats-system.spec.md)
- Hytale Reference: `EffectControllerComponent`, `EntityEffect`, `ActiveEntityEffect`
