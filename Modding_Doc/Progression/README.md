# Progression System

The Hyforged Progression System provides ARPG-style character and class progression with XP-based leveling.

## Overview

The system tracks two types of progression:
- **Character Progression**: Overall player level (1-100) earned through any XP source
- **Class Progression**: Weapon-based specialization levels (1-20 per class)

## XP Curves

XP requirements are defined via JSON assets that specify exponential growth curves.

### XP Curve JSON Schema

```json
{
  "id": "hyforged:character_curve",
  "type": "CHARACTER",
  "baseXp": 100,
  "exponent": 1.15,
  "maxLevel": 100
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique curve identifier (namespace:name format) |
| `type` | enum | `CHARACTER` or `CLASS` |
| `baseXp` | int | XP required for level 2 |
| `exponent` | double | Exponential growth factor |
| `maxLevel` | int | Maximum level for this curve |

### XP Formula

XP required for level N (incremental cost):
```
xpForLevel(n) = baseXp * exponent^(n-2)   // for n >= 2
```

Total cumulative XP to reach level N:
```
totalXpForLevel(n) = sum(xpForLevel(2..n))
```

**Important**: The system stores **total accumulated XP** and uses cumulative thresholds for level-up detection. Progress toward the next level is calculated as:
```
xpProgress = totalXp - totalXpForLevel(currentLevel)
```

### Asset Location

Place XP curve assets in:
```
Server/Hyforged/Progression/
```

## Class Definitions

Classes define weapon-based specializations with ability score bonuses.

### Class Definition JSON Schema

```json
{
  "id": "hyforged:warrior",
  "displayName": "Warrior",
  "description": "Masters of melee combat",
  "abilityScores": {
    "hyforged:strength": 10,
    "hyforged:constitution": 8,
    "hyforged:dexterity": 6,
    "hyforged:intelligence": 4,
    "hyforged:wisdom": 4
  },
  "weaponTagFamilies": [
    "Family=Sword",
    "Family=Axe",
    "Family=Hammer"
  ],
  "levelRewards": {
    "5": {
      "hyforged:strength": 1
    },
    "10": {
      "hyforged:strength": 1,
      "hyforged:constitution": 1
    }
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique class identifier |
| `displayName` | string | UI display name |
| `description` | string | Tooltip description |
| `abilityScores` | map | Base ability scores for this class |
| `weaponTagFamilies` | array | Weapon tags that activate this class (uses Hytale expanded tag format, e.g., `Family=Sword`) |
| `levelRewards` | map | Ability bonuses granted at specific levels |

### Level Rewards

When a player reaches a class level that has a `levelRewards` entry, the specified ability bonuses are automatically applied as stat modifiers. These are cumulative across all levels gained.

Character level rewards use the same `levelRewards` structure, sourced from `hyforged:default` in `Server/Hyforged/Stats/Classes/Default.json`. This keeps character-level bonus tuning data-driven and separate from per-class rewards.

Example: A Warrior reaching level 10 would receive:
- Level 5 reward: +1 Strength
- Level 10 reward: +1 Strength, +1 Constitution
- Total bonuses: +2 Strength, +1 Constitution

### Weapon Tag Resolution

When a player equips a weapon, the system:
1. Reads weapon tags from the item
2. Matches tags against each class's `weaponTagFamilies`
3. Sets the matching class as active
4. All XP earned goes to both character and active class

### Asset Location

Place class definitions in:
```
Server/Hyforged/Stats/Classes/
```

## Admin Commands

All progression admin commands require the `hyforged.admin.progression.*` permission.

### Command Reference

| Command | Description |
|---------|-------------|
| `/hyforged progression info <player>` | Show player's progression state |
| `/hyforged progression debug <player>` | Show detailed debug info |
| `/hyforged progression xp add <player> <amount>` | Add character XP |
| `/hyforged progression xp set <player> <amount>` | Set character XP |
| `/hyforged progression classxp add <player> <classId> <amount>` | Add class XP |
| `/hyforged progression level set <player> <level>` | Set character level |
| `/hyforged progression classlevel set <player> <classId> <level>` | Set class level |
| `/hyforged progression reset <player>` | Reset all progression |

### Permission Nodes

| Permission | Description |
|------------|-------------|
| `hyforged.admin.progression.info` | View progression info |
| `hyforged.admin.progression.debug` | View debug info |
| `hyforged.admin.progression.xp` | Modify XP values |
| `hyforged.admin.progression.level` | Modify levels |
| `hyforged.admin.progression.reset` | Reset progression |

## Events

The progression system emits events for integration with other systems.

### Character Level Up

Emitted when a player gains character level(s). Grants 1 general passive point per level gained, and can apply ability bonuses if `hyforged:default` has matching `levelRewards` entries.

```java
CharacterLevelUpEvent {
    Ref<EntityStore> entityRef,
    int oldLevel,
    int newLevel,
    List<Integer> levelsGained,
    int passivePointsGranted  // Always equals levelsGained.size()
}
```

### Class Level Up

Emitted when a player gains class level(s). Grants 1 class passive point per level gained, plus any ability bonuses from `levelRewards`.

```java
ClassLevelUpEvent {
    Ref<EntityStore> entityRef,
    String classId,
    int oldLevel,
    int newLevel,
    List<Integer> levelsGained,
    Map<String, Integer> abilityBonuses,  // Cumulative from levelRewards
    int classPassivePointsGranted         // Always equals levelsGained.size()
}
```

### XP Gain Notification

```java
XPGainNotificationEvent {
    Ref<EntityStore> entityRef,
    long totalCharacterXp,
    long totalClassXp,
    String activeClassId,
    Map<String, Long> sourceBreakdown
}
```

## Integration

### Getting Character Level

Use `ProgressionStatBridge` for combat effectiveness calculations:

```java
int attackerLevel = ProgressionStatBridge.getCharacterLevel(attackerRef, store);
int reduction = RatingConverter.armorToReduction(armorRating, attackerLevel);
```

### Stat System Bridge

Class and character level bonuses are automatically applied as stat modifiers via `ClassLevelModifierSystem`. The modifiers use:
- Source type: `ModifierSource.CLASS`
- Source ID format: `class-level:{classId}:{level}:{abilityId}`
- Source ID format (character): `character-level:{level}:{abilityId}`

## Configuration

XP behavior is configured via `Server/Hyforged/Progression/XPConfig.json`:

```json
{
  "Id": "hyforged:xp_config",
  "CombatBaseXp": 10,
  "DiscoveryBiomeXp": 100,
  "DiscoveryLandmarkXp": 50,
  "ObjectiveMinorXp": 25,
  "ObjectiveStandardXp": 50,
  "ObjectiveMajorXp": 100,
  "ObjectiveLegendaryXp": 250,
  "ClassXpRatio": 1.0,
  "MaxCharacterLevel": 100,
  "MaxClassLevel": 20
}
```

| Field | Type | Description |
|-------|------|-------------|
| `CombatBaseXp` | long | Base XP for combat kills |
| `DiscoveryBiomeXp` | long | XP for discovering a major zone |
| `DiscoveryLandmarkXp` | long | XP for discovering a minor zone |
| `ObjectiveMinorXp` | long | XP for minor objective completion |
| `ObjectiveStandardXp` | long | XP for standard objective completion |
| `ObjectiveMajorXp` | long | XP for major objective completion |
| `ObjectiveLegendaryXp` | long | XP for legendary objective completion |
| `ClassXpRatio` | double | Ratio of class XP to character XP (1.0 = same amount) |
| `MaxCharacterLevel` | int | Maximum character level |
| `MaxClassLevel` | int | Maximum class level |

## XP Sources

Hyforged awards XP from multiple sources:

| Source | Trigger | Configuration |
|--------|---------|---------------|
| Combat | Entity kill | `CombatBaseXp` scaled by enemy stats |
| Discovery | Zone discovery | `DiscoveryBiomeXp` / `DiscoveryLandmarkXp` based on zone type |
| Objective | Quest completion | `ObjectiveMinorXp` / `ObjectiveStandardXp` / `ObjectiveMajorXp` / `ObjectiveLegendaryXp` |

### Adding XP to Objectives

Objectives can grant XP on completion using the `hyforged:xp_award` completion type:

```json
{
  "Completions": [
    {
      "Type": "hyforged:xp_award",
      "Tier": "major"
    }
  ]
}
```

Or with an explicit amount:

```json
{
  "Completions": [
    {
      "Type": "hyforged:xp_award",
      "XpAmount": 500
    }
  ]
}
```
