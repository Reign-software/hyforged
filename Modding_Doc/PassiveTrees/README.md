# Hyforged Passive Trees System — Modding Guide

This guide explains how to create and customize passive skill trees in Hyforged. The system supports Path-of-Exile-scale passive trees with 1000+ nodes per tree.

## Documentation

- [API Reference](API.md) — Code examples for allocation, refunds, events, and effect handlers
- This README — JSON schema and data-driven configuration

## Quick Start

1. Create a passive tree JSON in your mod's `Server/<YourMod>/PassiveTrees/` folder
2. For class trees, place them in `Server/<YourMod>/PassiveTrees/classes/`
3. Use namespaced IDs to avoid conflicts with other mods

---

## Table of Contents

- [Core Concepts](#core-concepts)
- [Tree Types](#tree-types)
- [Defining Trees (JSON)](#defining-trees-json)
- [Node Types](#node-types)
- [Node Effects](#node-effects)
- [Connections](#connections)
- [Point Economy](#point-economy)
- [Refund System](#refund-system)
- [Examples](#examples)

---

## Core Concepts

### Graph-Based Structure
Passive trees are graphs where nodes are connected by edges. Players allocate points from starting nodes outward along connections.

### Path Connectivity
All allocated nodes must remain connected to a starting node. If a node would be orphaned by a refund, all orphaned nodes are refunded together.

### Effect Stacking
Multiple nodes can grant the same stat modifier. All modifiers stack according to the [Stats System](../Stats/README.md) stacking rules.

---

## Tree Types

### General Tree
- One shared tree for all characters
- Points earned from character level and Point Books
- Contains 1000+ nodes covering all playstyles
- Multiple starting regions (e.g., Strength, Dexterity, Intelligence)

### Class Trees
- One tree per class (e.g., Warrior, Ranger, Mage)
- Points earned from class level only
- Contains 50-150 nodes focused on class theme
- Single starting node per tree

---

## Defining Trees (JSON)

The passive tree system uses a **multi-file, additive** structure:

1. **Node Templates** — Reusable node definitions (effects, type, name) without position
2. **Tree Definitions** — Metadata about trees (type, class association)
3. **Layout Files** — Position nodes and define connections (additive across mods)

Any mod can add nodes and connections to any tree. Connections reference nodes by namespaced ID, so ModA can connect to Hyforged nodes, and ModB can connect to both.

---

## Folder Structure

```
Server/<YourMod>/PassiveTrees/
├── trees/
│   └── classes/
│       └── my-class.json         # Only needed for new class trees
├── nodes/
│   ├── general/                  # Node templates for general tree
│   │   ├── strength.json
│   │   └── defense.json
│   └── classes/
│       └── warrior/
│           └── core.json
└── layouts/
    ├── general/                  # Placements & connections (additive)
    │   └── yourmod-nodes.json
    └── classes/
        └── warrior/
            └── yourmod-additions.json
```

### Load Order & Merging
1. All `nodes/` files across all mods are loaded (node templates registered)
2. All `layouts/` files are merged additively (placements, connections)
3. Connections can reference any node from any mod by namespaced ID

---

## Tree Definition

Tree definitions are only needed when creating a new tree. Hyforged defines the general tree. Mods create new class trees.

`Server/Hyforged/PassiveTrees/trees/general.json`:
```json
{
    "Id": "hyforged:passive-tree-general",
    "TreeType": "general",
    "Version": 1
}
```

`Server/YourMod/PassiveTrees/trees/classes/my-class.json`:
```json
{
    "Id": "yourmod:passive-tree-myclass",
    "TreeType": "class",
    "ClassId": "yourmod:myclass",
    "Version": 1
}
```

### Tree Definition Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `Id` | string | ✓ | Namespaced tree identifier |
| `TreeType` | string | ✓ | `"general"` or `"class"` |
| `Version` | int | ✓ | Schema version for migration support |
| `ClassId` | string | class only | Links to class definition |

---

## Node Templates

Node templates define **what** a node does, without **where** it's placed. This allows reuse across the tree.

`Server/YourMod/PassiveTrees/nodes/general/strength.json`:
```json
{
    "Nodes": [
        {
            "Id": "yourmod:strength-5",
            "Type": "minor",
            "Name": "Strength",
            "Description": "+5 to Strength",
            "Effects": [
                { "Type": "stat-modifier", "Stat": "hyforged:strength", "Value": 5 }
            ]
        },
        {
            "Id": "yourmod:strength-10",
            "Type": "minor",
            "Name": "Strength",
            "Description": "+10 to Strength",
            "Effects": [
                { "Type": "stat-modifier", "Stat": "hyforged:strength", "Value": 10 }
            ]
        },
        {
            "Id": "yourmod:notable-brutal-force",
            "Type": "notable",
            "Name": "Brutal Force",
            "Description": "+20 Strength\n10% increased Physical Damage",
            "Effects": [
                { "Type": "stat-modifier", "Stat": "hyforged:strength", "Value": 20 },
                { "Type": "stat-modifier", "Stat": "hyforged:physical-damage-increase", "Value": 1000 }
            ]
        }
    ]
}
```

### Node Template Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `Id` | string | ✓ | Namespaced node identifier |
| `Type` | string | ✓ | `minor`, `notable`, `keystone`, `unlock`, or `mastery` |
| `Name` | string | ✓ | Display name |
| `Description` | string | ✓ | Tooltip description |
| `Effects` | Effect[] | | Array of effects granted |
| `Icon` | string | | Asset reference for node icon |
| `KeystoneFamily` | string | | For keystones — only one per family allowed |

---

## Layout Files (Additive)

Layout files define **where** nodes are placed and how they connect. Each mod provides its own layout files — all are merged together.

`Server/YourMod/PassiveTrees/layouts/general/yourmod-strength.json`:
```json
{
    "TreeId": "hyforged:passive-tree-general",
    "Placements": [
        {
            "NodeId": "yourmod:strength-5",
            "Position": { "X": 0, "Y": 80 },
            "Region": "strength"
        },
        {
            "NodeId": "yourmod:strength-5",
            "Position": { "X": 20, "Y": 80 },
            "Region": "strength",
            "InstanceId": "yourmod:strength-5-b"
        },
        {
            "NodeId": "yourmod:notable-brutal-force",
            "Position": { "X": 10, "Y": 60 },
            "Region": "strength"
        }
    ],
    "Connections": [
        { "From": "hyforged:start-strength", "To": "yourmod:strength-5" },
        { "From": "hyforged:start-strength", "To": "yourmod:strength-5-b" },
        { "From": "yourmod:strength-5", "To": "yourmod:notable-brutal-force" },
        { "From": "yourmod:strength-5-b", "To": "yourmod:notable-brutal-force" }
    ]
}
```

### Layout Fields

| Field | Type | Description |
|-------|------|-------------|
| `TreeId` | string | Which tree this layout contributes to |
| `Placements` | Placement[] | Node placements with positions |
| `Connections` | Connection[] | Connections between nodes (cross-mod allowed) |
| `StartingNodes` | string[] | Node IDs that can be allocated first (additive) |

### Placement Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `NodeId` | string | ✓ | Reference to a node template |
| `Position` | object | ✓ | `{ "X": number, "Y": number }` |
| `Region` | string | | Visual grouping region |
| `InstanceId` | string | | Unique ID when placing same template multiple times |

### Node Reuse with InstanceId

Place the same node template multiple times using `InstanceId`:

```json
{
    "Placements": [
        { "NodeId": "yourmod:strength-5", "Position": { "X": 0, "Y": 80 }, "InstanceId": "yourmod:str-a" },
        { "NodeId": "yourmod:strength-5", "Position": { "X": 40, "Y": 80 }, "InstanceId": "yourmod:str-b" },
        { "NodeId": "yourmod:strength-5", "Position": { "X": 80, "Y": 80 }, "InstanceId": "yourmod:str-c" }
    ],
    "Connections": [
        { "From": "hyforged:some-node", "To": "yourmod:str-a" },
        { "From": "hyforged:some-node", "To": "yourmod:str-b" },
        { "From": "hyforged:some-node", "To": "yourmod:str-c" }
    ]
}
```

---

## Multi-Mod Example

**Hyforged** provides the base general tree:
```
Server/Hyforged/PassiveTrees/
├── trees/general.json
├── nodes/general/core.json
└── layouts/general/hyforged-core.json
```

**ModA** adds fire-themed nodes and connects to Hyforged's tree:
```
Server/ModA/PassiveTrees/
├── nodes/general/fire.json
└── layouts/general/moda-fire.json
```

**ModB** adds ice-themed nodes and connects to BOTH Hyforged and ModA:
```
Server/ModB/PassiveTrees/
├── nodes/general/ice.json
└── layouts/general/modb-ice.json
```

### ModA Connecting to Hyforged

ModA's layout file connects their nodes to Hyforged's existing nodes:

```json
{
    "TreeId": "hyforged:passive-tree-general",
    "Placements": [
        { "NodeId": "moda:fire-node-1", "Position": { "X": 100, "Y": 50 } },
        { "NodeId": "moda:fire-notable", "Position": { "X": 100, "Y": 30 } }
    ],
    "Connections": [
        { "From": "hyforged:notable-fire-mastery", "To": "moda:fire-node-1" },
        { "From": "moda:fire-node-1", "To": "moda:fire-notable" }
    ]
}
```

### ModB Connecting to ModA

ModB can connect to both Hyforged AND ModA nodes:

```json
{
    "TreeId": "hyforged:passive-tree-general",
    "Placements": [
        { "NodeId": "modb:ice-fire-bridge", "Position": { "X": 120, "Y": 40 } }
    ],
    "Connections": [
        { "From": "moda:fire-notable", "To": "modb:ice-fire-bridge" },
        { "From": "hyforged:notable-cold", "To": "modb:ice-fire-bridge" }
    ]
}
```

### Result at Load Time
1. All node templates registered (Hyforged, ModA, ModB)
2. All placements merged into the general tree
3. All connections merged — cross-mod connections just work
4. Players see one unified tree with content from all three mods

---

## Node Types

### Minor
Small stat bonuses. The majority of tree nodes are minor.

```json
{
    "Id": "yourmod:node-vigor",
    "Type": "minor",
    "Name": "Vigor",
    "Description": "+5 to Maximum Health",
    "Effects": [
        { "Type": "stat-modifier", "Stat": "hyforged:flat-max-health", "Value": 5 }
    ]
}
```

### Notable
Significant bonuses. Visually distinct in UI, often with multiple effects.

```json
{
    "Id": "yourmod:node-brutal-force",
    "Type": "notable",
    "Name": "Brutal Force",
    "Description": "10% increased Physical Damage.\n+20 to Maximum Health.",
    "Effects": [
        { "Type": "stat-modifier", "Stat": "hyforged:physical-damage-increase", "Value": 1000 },
        { "Type": "stat-modifier", "Stat": "hyforged:flat-max-health", "Value": 20 }
    ]
}
```

### Keystone
Powerful build-defining effects with significant tradeoffs. Only one keystone per "family" can be allocated.

```json
{
    "Id": "yourmod:keystone-unwavering-stance",
    "Type": "keystone",
    "Name": "Unwavering Stance",
    "Description": "Cannot be stunned.\nCannot Evade enemy attacks.",
    "Effects": [
        { "Type": "unlock-flag", "FlagId": "hyforged:stun-immune" },
        { "Type": "stat-modifier", "Stat": "hyforged:evasion-rating", "Value": -10000 }
    ],
    "KeystoneFamily": "yourmod:stance-keystones"
}
```

### Unlock
Grants access to new abilities or mechanics.

```json
{
    "Id": "yourmod:node-warcry-unlock",
    "Type": "unlock",
    "Name": "Warcry Mastery",
    "Description": "Unlocks the Rallying Cry skill.",
    "Effects": [
        { "Type": "spell-grant", "SpellId": "hyforged:rallying-cry" }
    ]
}
```

### Mastery
End-of-path nodes that offer a choice between mutually exclusive bonuses.

```json
{
    "Id": "yourmod:mastery-berserker",
    "Type": "mastery",
    "Name": "Berserker Mastery",
    "Description": "Choose a berserker specialization.",
    "Effects": [
        {
            "Type": "mastery-choice",
            "Choices": [
                { "Type": "stat-modifier", "Stat": "hyforged:attack-damage-increase", "Value": 2000 },
                { "Type": "stat-modifier", "Stat": "hyforged:life-leech", "Value": 500 }
            ]
        }
    ]
}
```

### Node Template Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `Id` | string | ✓ | Namespaced node identifier |
| `Type` | string | ✓ | `minor`, `notable`, `keystone`, `unlock`, or `mastery` |
| `Name` | string | ✓ | Display name |
| `Description` | string | ✓ | Tooltip description |
| `Effects` | Effect[] | | Array of effects granted by this node |
| `Icon` | string | | Asset reference for node icon |
| `KeystoneFamily` | string | | For keystones — only one per family allowed |

**Note**: `Position` and `Region` are defined in layout files, not node templates.

---

## Node Effects

### stat-modifier
Applies a stat modifier to the player.

```json
{
    "Type": "stat-modifier",
    "Stat": "hyforged:strength",
    "Value": 10
}
```

Values use basis points for percentages (10000 = 100%).

### spell-grant
Grants access to a spell/ability.

```json
{
    "Type": "spell-grant",
    "SpellId": "hyforged:fireball"
}
```

### unlock-flag
Sets a gameplay flag that other systems can check.

```json
{
    "Type": "unlock-flag",
    "FlagId": "hyforged:stun-immune"
}
```

### mastery-choice
Presents mutually exclusive options. Player selects one on allocation.

```json
{
    "Type": "mastery-choice",
    "Choices": [
        { "Type": "stat-modifier", "Stat": "hyforged:damage-increase", "Value": 2000 },
        { "Type": "stat-modifier", "Stat": "hyforged:defense-increase", "Value": 2000 }
    ]
}
```

---

## Connections

Connections define which nodes can be reached from which. They are bidirectional.

```json
{
    "Connections": [
        { "From": "yourmod:start", "To": "yourmod:node1" },
        { "From": "yourmod:node1", "To": "yourmod:node2" },
        { "From": "yourmod:node1", "To": "yourmod:node3" }
    ]
}
```

### Connection Rules
- Players can only allocate nodes connected to already-allocated nodes
- Starting nodes can always be allocated
- Connections are treated as bidirectional for traversal

---

## Point Economy

### General Tree Points
```
Available Points = (Character Level - 1) + Book Points Used - Allocated Nodes
```

- Characters start at level 1 with 0 points
- Each level grants 1 additional point
- Point Books grant bonus points when consumed

### Class Tree Points
```
Available Points = Class Level - Allocated Nodes
```

- Each class level grants 1 point for that class tree
- Class levels are earned separately from character level

---

## Refund System

### Single Node Refund
Players can refund individual nodes that are "leaf" nodes (no other allocated nodes depend on them).

**Cost**: Refund costs a currency amount based on character level.

### Full Respec
Players can refund all nodes in a tree at once.

**Cost**: Sum of all individual refund costs.

### Orphan Handling
If refunding a node would disconnect other nodes from the start, all orphaned nodes are refunded together.

### Refund Configuration

Configure refund costs in `Server/<YourMod>/PassiveTrees/refund-config.json`:

```json
{
    "BaseCost": 10,
    "CostPerLevel": 5,
    "MaxBookPoints": 30
}
```

| Field | Type | Description |
|-------|------|-------------|
| `BaseCost` | int | Base currency cost per refund |
| `CostPerLevel` | int | Additional cost per character level |
| `MaxBookPoints` | int | Maximum bonus points from Point Books |

---

## Examples

### Adding Nodes to the General Tree

**Step 1**: Create node templates in `Server/YourMod/PassiveTrees/nodes/general/mymod.json`:
```json
{
    "Nodes": [
        {
            "Id": "yourmod:strength-5",
            "Type": "minor",
            "Name": "Strength",
            "Description": "+5 to Strength",
            "Effects": [
                { "Type": "stat-modifier", "Stat": "hyforged:strength", "Value": 5 }
            ]
        },
        {
            "Id": "yourmod:notable-power",
            "Type": "notable",
            "Name": "Raw Power",
            "Description": "+15 Strength\n5% increased Physical Damage",
            "Effects": [
                { "Type": "stat-modifier", "Stat": "hyforged:strength", "Value": 15 },
                { "Type": "stat-modifier", "Stat": "hyforged:physical-damage-increase", "Value": 500 }
            ]
        }
    ]
}
```

**Step 2**: Create a layout file in `Server/YourMod/PassiveTrees/layouts/general/mymod-strength.json`:
```json
{
    "TreeId": "hyforged:passive-tree-general",
    "Placements": [
        { "NodeId": "yourmod:strength-5", "Position": { "X": 50, "Y": 100 }, "Region": "strength" },
        { "NodeId": "yourmod:strength-5", "Position": { "X": 70, "Y": 100 }, "Region": "strength", "InstanceId": "yourmod:str-b" },
        { "NodeId": "yourmod:notable-power", "Position": { "X": 60, "Y": 80 }, "Region": "strength" }
    ],
    "Connections": [
        { "From": "hyforged:start-strength", "To": "yourmod:strength-5" },
        { "From": "hyforged:start-strength", "To": "yourmod:str-b" },
        { "From": "yourmod:strength-5", "To": "yourmod:notable-power" },
        { "From": "yourmod:str-b", "To": "yourmod:notable-power" }
    ]
}
```

### Creating a New Class Tree

**Step 1**: Create tree definition in `Server/YourMod/PassiveTrees/trees/classes/myclass.json`:
```json
{
    "Id": "yourmod:passive-tree-myclass",
    "TreeType": "class",
    "ClassId": "yourmod:myclass",
    "Version": 1
}
```

**Step 2**: Create node templates in `Server/YourMod/PassiveTrees/nodes/classes/myclass/core.json`:
```json
{
    "Nodes": [
        {
            "Id": "yourmod:myclass-start",
            "Type": "minor",
            "Name": "Class Origin",
            "Description": "Starting point.",
            "Effects": []
        },
        {
            "Id": "yourmod:myclass-node1",
            "Type": "minor",
            "Name": "First Step",
            "Description": "+5 Strength",
            "Effects": [
                { "Type": "stat-modifier", "Stat": "hyforged:strength", "Value": 5 }
            ]
        }
    ]
}
```

**Step 3**: Create layout in `Server/YourMod/PassiveTrees/layouts/classes/myclass/core.json`:
```json
{
    "TreeId": "yourmod:passive-tree-myclass",
    "Placements": [
        { "NodeId": "yourmod:myclass-start", "Position": { "X": 0, "Y": 0 } },
        { "NodeId": "yourmod:myclass-node1", "Position": { "X": 0, "Y": -20 } }
    ],
    "Connections": [
        { "From": "yourmod:myclass-start", "To": "yourmod:myclass-node1" }
    ],
    "StartingNodes": ["yourmod:myclass-start"]
}
```

### Complete Sample Trees

See the Hyforged sample trees for complete examples:

- `Server/Hyforged/PassiveTrees/` — General tree with multiple regions
- `Server/Hyforged/PassiveTrees/classes/` — Class trees with mastery nodes

---

## Version Migration

When you modify a tree structure (add/remove/move nodes), increment the `Version` field. The system will automatically handle migration:

1. Invalid allocations (nodes removed or moved) are refunded
2. Players receive their points back
3. Effects from invalid nodes are removed

This ensures save compatibility when updating tree definitions.
