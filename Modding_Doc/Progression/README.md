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
    "weapon:sword",
    "weapon:axe",
    "weapon:hammer"
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
| `weaponTagFamilies` | array | Weapon tags that activate this class |
| `levelRewards` | map | Ability bonuses granted at specific levels |

### Level Rewards

When a player reaches a class level that has a `levelRewards` entry, the specified ability bonuses are automatically applied as stat modifiers. These are cumulative across all levels gained.

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

Emitted when a player gains character level(s). Grants 1 general passive point per level gained.

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

Class level bonuses are automatically applied as stat modifiers via `ClassLevelModifierSystem`. The modifiers use:
- Source type: `ModifierSource.CLASS`
- Source ID format: `class-level:{classId}:{level}:{abilityId}`

## Configuration

XP behavior is configured via `Server/Hyforged/Progression/XPConfig.json`:

```json
{
  "Id": "hyforged:xp_config",
  "combatBaseXp": 10,
  "discoveryBiomeXp": 100,
  "discoveryLandmarkXp": 50,
  "objectiveMinorXp": 25,
  "objectiveStandardXp": 50,
  "objectiveMajorXp": 100,
  "objectiveLegendaryXp": 250,
  "classXpRatio": 1.0,
  "maxCharacterLevel": 100,
  "maxClassLevel": 20
}
```

| Field | Type | Description |
|-------|------|-------------|
| `combatBaseXp` | long | Base XP for combat kills |
| `discoveryBiomeXp` | long | XP for discovering a major zone |
| `discoveryLandmarkXp` | long | XP for discovering a minor zone |
| `objectiveMinorXp` | long | XP for minor objective completion |
| `objectiveStandardXp` | long | XP for standard objective completion |
| `objectiveMajorXp` | long | XP for major objective completion |
| `objectiveLegendaryXp` | long | XP for legendary objective completion |
| `classXpRatio` | double | Ratio of class XP to character XP (1.0 = same amount) |
| `maxCharacterLevel` | int | Maximum character level |
| `maxClassLevel` | int | Maximum class level |

## XP Sources

Hyforged awards XP from multiple sources:

| Source | Trigger | Configuration |
|--------|---------|---------------|
| Combat | Entity kill | `combatBaseXp` scaled by enemy stats |
| Discovery | Zone discovery | `discoveryBiomeXp` / `discoveryLandmarkXp` based on zone type |
| Objective | Quest completion | `objectiveMinorXp` / `objectiveStandardXp` / `objectiveMajorXp` / `objectiveLegendaryXp` |

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
