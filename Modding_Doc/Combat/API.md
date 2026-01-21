# Combat System API

The Hyforged Combat Service API provides programmatic access to the combat pipeline for skills, abilities, and custom damage sources.

## Overview

The Combat Service runs damage through the full Hyforged pipeline:
1. **Hit Resolution** - Accuracy vs Evasion check
2. **Block Check** - Auto-block chance with stamina consumption
3. **Damage Reduction** - Per-element resistance minus penetration
4. **Critical Hits** - Crit chance and multiplier application
5. **Ailment Accumulation** - Elemental damage threshold tracking
6. **Combat Logging** - Records events for both attacker and defender

## Quick Start

```java
// Get the service
CombatService combat = CombatService.get();

// Simple damage
DamageSpec spec = DamageSpec.of("Physical", 50);
CombatResult result = combat.applyDamage(attackerRef, defenderRef, spec, commandBuffer);

if (result.wasHit()) {
    System.out.println("Dealt " + result.getTotalFinalDamage() + " damage");
    if (result.isCriticalHit()) {
        System.out.println("Critical hit!");
    }
}
```

## DamageSpec

`DamageSpec` defines the damage to be applied. Use the builder for complex configurations.

### Factory Methods

```java
// Simple single-element damage
DamageSpec spec = DamageSpec.of("Fire", 100);
```

### Builder Pattern

```java
DamageSpec spec = DamageSpec.builder()
    .addDamage("Physical", 30)      // Add physical damage
    .addDamage("Fire", 20)           // Add fire damage
    .forceCrit(true)                 // Guarantee crit
    .skipEvasion(true)               // Cannot be evaded
    .skipBlock(true)                 // Cannot be blocked
    .skipResistance(true)            // Ignore resistance (true damage)
    .skipAilments(true)              // Don't trigger ailments
    .sourceDescription("Fireball")   // For combat log
    .build();
```

### DamageSpec Options

| Option | Default | Description |
|--------|---------|-------------|
| `forceCrit(boolean)` | false | Force the attack to critically hit |
| `noCrit(boolean)` | false | Prevent the attack from critting |
| `skipEvasion(boolean)` | false | Attack cannot be evaded |
| `skipBlock(boolean)` | false | Attack cannot be blocked |
| `skipResistance(boolean)` | false | Ignore defender's resistance |
| `skipAilments(boolean)` | false | Don't accumulate ailment thresholds |
| `sourceDescription(String)` | null | Custom description for combat log |

## CombatResult

`CombatResult` contains all details about what happened during damage application.

### Outcome Types

```java
CombatResult.Outcome outcome = result.getOutcome();

switch (outcome) {
    case HIT:            // Damage was applied
    case EVADED:         // Attack was evaded (missed)
    case BLOCKED:        // Attack was fully blocked
    case CANCELLED:      // Attack was cancelled
    case TARGET_DEAD:    // Defender was already dead
    case INVALID_ENTITY: // Invalid entity reference
}
```

### Convenience Methods

```java
result.wasHit();           // true if outcome is HIT
result.wasEvaded();        // true if outcome is EVADED
result.wasBlocked();       // true if block occurred (partial or full)
result.wasAutoBlocked();   // true if auto-block triggered
result.isCriticalHit();    // true if crit
```

### Damage Information

```java
float baseDamage = result.getTotalBaseDamage();    // Before modifiers
float finalDamage = result.getTotalFinalDamage();  // After all modifiers
float reduction = result.getDamageReductionPercent(); // 0-100%

// Per-element breakdown
for (CombatResult.DamageBreakdown breakdown : result.getDamageBreakdown()) {
    String type = breakdown.damageCauseId();
    float base = breakdown.baseDamage();
    float final_ = breakdown.finalDamage();
    int resistance = breakdown.resistanceBps();
    int penetration = breakdown.penetrationBps();
    int effective = breakdown.getEffectiveResistanceBps(); // resistance - penetration
}
```

### Combat Modifiers

```java
int critMultiplier = result.getCritMultiplierBps();  // e.g., 1500 = 15% bonus
int blockMitigation = result.getBlockMitigationBps(); // e.g., 5000 = 50%
List<String> ailments = result.getAilmentsTriggered();
```

## CombatService Methods

### applyDamage

Apply damage from an entity attacker to a defender. Uses `CommandBuffer` for deferred entity modifications.

```java
CombatResult applyDamage(
    Ref<EntityStore> attackerRef,
    Ref<EntityStore> defenderRef,
    DamageSpec spec,
    CommandBuffer<EntityStore> commandBuffer
);
```

### applyDamageImmediate

Apply damage with immediate resolution using `ComponentAccessor`.

```java
CombatResult applyDamageImmediate(
    Ref<EntityStore> attackerRef,
    Ref<EntityStore> defenderRef,
    DamageSpec spec,
    ComponentAccessor<EntityStore> componentAccessor
);
```

### applyEnvironmentalDamage

Apply damage from non-entity sources (lava, fall damage, etc.). Skips hit resolution.

```java
CombatResult applyEnvironmentalDamage(
    Ref<EntityStore> defenderRef,
    DamageSpec spec,
    CommandBuffer<EntityStore> commandBuffer,
    String sourceDescription
);
```

### calculateDamage

Calculate potential damage without applying it. Useful for AI decision-making or damage preview UI.

```java
CombatResult calculateDamage(
    Ref<EntityStore> attackerRef,
    Ref<EntityStore> defenderRef,
    DamageSpec spec,
    ComponentAccessor<EntityStore> componentAccessor
);
```

## Combat Formula Reference

### Hit Chance
```
hitChance = 10000 - evasionBps + accuracyBps - levelPenalty
levelPenalty = max(0, (defenderLevel - attackerLevel)) * 500
```

### Damage Reduction
```
effectiveResistance = max(0, resistanceBps - penetrationBps)
finalDamage = baseDamage * (1 - effectiveResistance / 10000)
```

### Critical Hit
```
critMultiplier = 1 + critMultiplierBps / 10000
finalDamage = preCritDamage * critMultiplier
```

### Block Mitigation
```
finalDamage = preBlockDamage * (1 - blockMitigationBps / 10000)
```

### Healing
```
finalHealing = baseHealing * (effectiveness / 10000) * (received / 10000) * (recoveryRate / 10000)
```
Where each modifier defaults to 10000 (100%), making them multiplicative bonuses.

## HealingService

The Healing Service provides programmatic healing through a simplified pipeline that bypasses combat mechanics (no resistance, penetration, or crits).

### HealingSpec

`HealingSpec` defines healing to be applied.

```java
// Simple healing
HealingSpec spec = HealingSpec.of(50);

// With custom source
HealingSpec spec = HealingSpec.builder()
    .baseHealing(50)
    .source("Healing Potion")
    .build();
```

### HealingResult

`HealingResult` contains the outcome of healing application.

```java
// Apply healing and get result
HealingResult result = HealingService.get().applyHealing(healerRef, targetRef, spec, commandBuffer);

float baseHealing = result.baseHealing();
float finalHealing = result.finalHealing();
int effectivenessBps = result.healerEffectivenessBps();
int receivedBps = result.targetHealingReceivedBps();
int recoveryBps = result.targetRecoveryRateBps();
float totalMultiplier = result.getTotalMultiplier(); // Combined multiplier
```

### HealingService Methods

```java
// Apply healing with command buffer
HealingResult applyHealing(
    Ref<EntityStore> healerRef,
    Ref<EntityStore> targetRef,
    HealingSpec spec,
    CommandBuffer<EntityStore> commandBuffer
);

// Calculate healing without applying
HealingResult calculateHealing(
    Ref<EntityStore> healerRef,
    Ref<EntityStore> targetRef,
    HealingSpec spec,
    ComponentAccessor<EntityStore> componentAccessor
);

// Pure formula calculation
static float calculateFinalHealing(
    float baseHealing,
    int effectivenessBps,
    int receivedBps,
    int recoveryRateBps
);
```

### Example: Healing Spell

```java
public class HealSpell {
    
    public HealingResult cast(
        Ref<EntityStore> caster,
        Ref<EntityStore> target,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        HealingSpec spec = HealingSpec.builder()
            .baseHealing(100)
            .source("Heal")
            .build();
        
        return HealingService.get().applyHealing(caster, target, spec, commandBuffer);
    }
}
```

## CombatConfig (Debug Mode)

Toggle debug logging for balance testing:

```java
// Enable debug mode
CombatConfig.setDebugEnabled(true);

// Check if enabled
if (CombatConfig.isDebugEnabled()) {
    // ... expensive debug operations
}

// Log helpers
CombatConfig.debug("Message");
CombatConfig.debug("Value: %d", 42);

// Specialized logging
CombatConfig.logHitCalc(accuracy, evasion, hitChance, roll, didHit);
CombatConfig.logBlockCalc(blockChance, stamina, cost, blocked);
CombatConfig.logDamageCalc(element, baseDmg, resistance, penetration, finalDmg);
CombatConfig.logCritCalc(critChance, roll, isCrit, multiplier);
CombatConfig.logHealCalc(baseHeal, effectiveness, received, recovery, finalHeal);
```

## Example: Custom Skill

```java
public class FireballSkill {
    
    public CombatResult cast(
        Ref<EntityStore> caster,
        Ref<EntityStore> target,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        // Create damage spec with fire damage, forced accuracy
        DamageSpec spec = DamageSpec.builder()
            .addDamage("Fire", 75)
            .skipEvasion(true)  // Spells can't be evaded
            .sourceDescription("Fireball")
            .build();
        
        // Apply through combat service
        CombatResult result = CombatService.get().applyDamage(
            caster, target, spec, commandBuffer
        );
        
        // React to result
        if (result.wasHit() && !result.getAilmentsTriggered().isEmpty()) {
            // Target was ignited!
            playIgniteEffect(target);
        }
        
        return result;
    }
}
```

## See Also

- [Stats API](../Stats/API.md) - Stat definitions and modifiers
- [Affixes API](../Affixes/API.md) - Equipment affix system
- Combat Log Service - Event recording and retrieval
