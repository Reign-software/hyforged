---
name: modding-doc-passive-trees
description: Implements passive skill trees for character progression in Hyforged. Use when adding nodes to existing trees, creating new class trees, defining node templates, creating layout files, or working with PassiveTreeService, PassiveTreeRegistry, or node effects. Also use when deriving guidance from Modding_Doc/PassiveTrees. Triggers - passive tree, passive node, skill tree, class tree, general tree, node template, layout file, starting node, keystone, notable, mastery, modding doc.
---

# Hyforged Passive Tree System

This skill provides step-by-step guidance for implementing passive tree features in Hyforged.

## Quick Reference

| Task | Approach |
|------|----------|
| Add nodes to general tree | Create node template JSON + layout JSON |
| Create new class tree | Tree definition JSON + node templates + layout |
| Reuse nodes in multiple places | Use `InstanceId` in layout placements |
| Connect to existing nodes | Reference node IDs in layout `Connections` |
| Cross-mod connections | Use namespaced IDs (e.g., `hyforged:notable-fire-mastery`) |

## Documentation References

- [Passive Tree Overview](../../../Modding_Doc/PassiveTrees/README.md) — Concepts, JSON schemas, multi-mod support
- [Passive Tree API Reference](../../../Modding_Doc/PassiveTrees/API.md) — Programmatic API for allocation, refunds, events

## Key Concepts

### Multi-File Additive Structure

The passive tree system uses three types of files:

1. **Node Templates** (`nodes/`) — Define what nodes do without position
2. **Tree Definitions** (`trees/`) — Metadata only (type, class association)
3. **Layout Files** (`layouts/`) — Where nodes are placed + connections

### Cross-Mod Connections

Any mod can connect to any node from any other mod using namespaced IDs:

```json
{
    "TreeId": "hyforged:passive-tree-general",
    "Connections": [
        { "From": "hyforged:notable-fire-mastery", "To": "yourmod:fire-node-1" },
        { "From": "moda:fire-notable", "To": "yourmod:ice-fire-bridge" }
    ]
}
```

### Node Reuse with InstanceId

Place the same node template multiple times using `InstanceId`:

```json
{
    "Placements": [
        { "NodeId": "yourmod:strength-5", "Position": { "X": 0, "Y": 80 }, "InstanceId": "yourmod:str-a" },
        { "NodeId": "yourmod:strength-5", "Position": { "X": 40, "Y": 80 }, "InstanceId": "yourmod:str-b" }
    ]
}
```

## File Structure

```
Server/<YourMod>/PassiveTrees/
├── trees/
│   ├── general.json              # General tree definition (only one per mod)
│   └── classes/
│       └── my-class.json         # Class tree definitions
├── nodes/
│   ├── general/                  # Node templates for general tree
│   │   ├── core.json             # Core nodes (starting regions, keystones)
│   │   ├── strength.json         # Strength region nodes
│   │   ├── dexterity.json        # Dexterity region nodes
│   │   └── intelligence.json     # Intelligence region nodes
│   └── classes/
│       └── my-class/
│           └── core.json
└── layouts/
    ├── general/                  # Placements & connections (additive)
    │   ├── core.json             # Core layout (starting nodes, connections)
    │   ├── strength.json         # Strength region layout
    │   └── my-additions.json     # Your mod's additions
    └── classes/
        └── my-class/
            └── core-layout.json
```

## JSON Schemas

### Tree Definition Schema

```json
{
    "Id": "yourmod:passive-tree-general",
    "TreeType": "general",
    "Version": 1
}
```

For class trees:
```json
{
    "Id": "yourmod:passive-tree-warrior",
    "TreeType": "class",
    "ClassId": "yourmod:warrior",
    "Version": 1
}
```

### Node Template Schema

```json
{
    "Nodes": [
        {
            "Id": "yourmod:node-id",
            "Type": "minor|notable|keystone|mastery|unlock",
            "Name": "Display Name",
            "Description": "Effect description",
            "Icon": "yourmod:icons/passive/icon-name",
            "Effects": [...],
            "KeystoneFamily": "yourmod:family-id"
        }
    ]
}
```

### Layout Schema

```json
{
    "TreeId": "hyforged:passive-tree-general",
    "Placements": [
        {
            "NodeId": "yourmod:node-template-id",
            "Position": { "X": 100, "Y": 200 },
            "Region": "strength",
            "InstanceId": "yourmod:unique-instance-id"
        }
    ],
    "Connections": [
        { "From": "yourmod:node-a", "To": "yourmod:node-b" }
    ],
    "StartingNodes": ["yourmod:start-node"]
}
```

## Step-by-Step: Add Nodes to the General Tree

### Step 1: Create Node Templates

Create `Server/YourMod/PassiveTrees/nodes/general/my-nodes.json`:

```json
{
    "Nodes": [
        {
            "Id": "yourmod:strength-5",
            "Type": "minor",
            "Name": "Strength",
            "Description": "+5 to Strength",
            "Icon": "yourmod:icons/passive/strength",
            "Effects": [
                { "Type": "stat-modifier", "Stat": "hyforged:strength", "Value": 5 }
            ]
        },
        {
            "Id": "yourmod:notable-power",
            "Type": "notable",
            "Name": "Raw Power",
            "Description": "+15 Strength\n5% increased Physical Damage",
            "Icon": "yourmod:icons/passive/raw-power",
            "Effects": [
                { "Type": "stat-modifier", "Stat": "hyforged:strength", "Value": 15 },
                { "Type": "stat-modifier", "Stat": "hyforged:physical-damage-increase", "Value": 500 }
            ]
        }
    ]
}
```

### Step 2: Create Layout File

Create `Server/YourMod/PassiveTrees/layouts/general/my-layout.json`:

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

## Step-by-Step: Create the General Tree (Base Mod Only)

### Step 1: Create Tree Definition

Create `Server/Hyforged/PassiveTrees/trees/general.json`:

```json
{
    "Id": "hyforged:passive-tree-general",
    "TreeType": "general",
    "Version": 1
}
```

### Step 2: Create Core Node Templates

Create `Server/Hyforged/PassiveTrees/nodes/general/core.json` with starting nodes and keystones.

### Step 3: Create Region-Specific Node Templates

Create separate files for each attribute region:
- `strength.json` - STR-based nodes
- `dexterity.json` - DEX-based nodes  
- `intelligence.json` - INT-based nodes
- `hybrid-str-dex.json` - STR/DEX bridge nodes
- `hybrid-str-int.json` - STR/INT bridge nodes
- `hybrid-dex-int.json` - DEX/INT bridge nodes

### Step 4: Create Layout Files

Create `Server/Hyforged/PassiveTrees/layouts/general/core.json` with:
- All node placements with positions
- All connections between nodes
- Starting node declarations

## Step-by-Step: Create a New Class Tree

### Step 1: Create Tree Definition

Create `Server/YourMod/PassiveTrees/trees/classes/myclass.json`:

```json
{
    "Id": "yourmod:passive-tree-myclass",
    "TreeType": "class",
    "ClassId": "yourmod:myclass",
    "Version": 1
}
```

### Step 2: Create Node Templates

Create `Server/YourMod/PassiveTrees/nodes/classes/myclass/core.json`:

```json
{
    "Nodes": [
        {
            "Id": "yourmod:myclass-start",
            "Type": "minor",
            "Name": "Class Origin",
            "Description": "Starting point for your class.",
            "Effects": []
        },
        {
            "Id": "yourmod:myclass-power",
            "Type": "notable",
            "Name": "Class Mastery",
            "Description": "+20% Class Damage",
            "Effects": [
                { "Type": "stat-modifier", "Stat": "hyforged:damage-increase", "Value": 2000 }
            ]
        }
    ]
}
```

### Step 3: Create Layout

Create `Server/YourMod/PassiveTrees/layouts/classes/myclass/core.json`:

```json
{
    "TreeId": "yourmod:passive-tree-myclass",
    "Placements": [
        { "NodeId": "yourmod:myclass-start", "Position": { "X": 0, "Y": 0 } },
        { "NodeId": "yourmod:myclass-power", "Position": { "X": 0, "Y": -30 } }
    ],
    "Connections": [
        { "From": "yourmod:myclass-start", "To": "yourmod:myclass-power" }
    ],
    "StartingNodes": ["yourmod:myclass-start"]
}
```

## Node Types

| Type | Purpose | Visual |
|------|---------|--------|
| `minor` | Small stat bonuses | Small circle |
| `notable` | Significant bonuses, multiple effects | Large circle |
| `keystone` | Build-defining with tradeoffs | Diamond |
| `unlock` | Grants abilities/mechanics | Special icon |
| `mastery` | End-of-path choice between options | Star |

## Tree Layout Design

### Coordinate System

- Origin (0, 0) is the center of the tree
- X increases to the right, Y increases downward
- Recommended node spacing: 30-50 units
- Starting nodes typically placed on outer edge (radius ~400-600)

### Triangle Layout Pattern (PoE-style)

The general tree uses a triangular arrangement with three primary attributes at the vertices:

```
                    Intelligence
                    (0, -500)
                        ▲
                       /|\
                      / | \
                     /  |  \
                    /   |   \
                   /    |    \
                  /     |     \
                 /      |      \
                /       |       \
               /        |        \
              ▼─────────┼─────────▼
    Dexterity           │         Strength
    (-433, 250)         │         (433, 250)
                        │
                     Center
                      (0, 0)
```

### Region Colors

- **Strength** (bottom-right): Red tones - physical damage, life, armor
- **Dexterity** (bottom-left): Green tones - attack speed, evasion, crit
- **Intelligence** (top): Blue tones - mana, spell damage, energy shield

### Hybrid Regions

Bridge areas between primary attributes:
- **STR/DEX**: Melee damage, accuracy, bleed
- **STR/INT**: Spell power, life regen, elemental
- **DEX/INT**: Cast speed, crit spells, dodge

## Node Effects

### stat-modifier

Apply a stat modifier (basis points for percentages, 10000 = 100%):

```json
{ "Type": "stat-modifier", "Stat": "hyforged:strength", "Value": 10 }
{ "Type": "stat-modifier", "Stat": "hyforged:physical-damage-increase", "Value": 1000 }
```

### spell-grant

Grant access to a spell/ability:

```json
{ "Type": "spell-grant", "SpellId": "hyforged:fireball" }
```

### unlock-flag

Set a gameplay flag for other systems:

```json
{ "Type": "unlock-flag", "FlagId": "hyforged:stun-immune" }
```

### mastery-choice

Present mutually exclusive options (for mastery nodes):

```json
{
    "Type": "mastery-choice",
    "Choices": [
        { "Type": "stat-modifier", "Stat": "hyforged:damage-increase", "Value": 2000 },
        { "Type": "stat-modifier", "Stat": "hyforged:defense-increase", "Value": 2000 }
    ]
}
```

## Keystone Families

Only one keystone per family can be allocated. Use `KeystoneFamily` to group mutually exclusive keystones:

```json
{
    "Id": "yourmod:keystone-offense",
    "Type": "keystone",
    "Name": "Offensive Stance",
    "KeystoneFamily": "yourmod:stance-keystones",
    "Effects": [
        { "Type": "stat-modifier", "Stat": "hyforged:damage-increase", "Value": 3000 },
        { "Type": "stat-modifier", "Stat": "hyforged:defense-rating", "Value": -2000 }
    ]
}
```

## Programmatic API

### Get Service Instance

```java
PassiveTreeService service = PassiveTreeService.get();
```

### Tree Access

```java
// Get specific trees
PassiveTree generalTree = service.getGeneralTree();
PassiveTree classTree = service.getClassTree("hyforged:warrior");
PassiveTree tree = service.getTree("hyforged:passive-tree-general");
```

### Allocate a Node

```java
AllocationResult result = service.allocateNode(entityRef, treeId, nodeId);
if (result.success()) {
    // Node allocated successfully
} else {
    String reason = result.reason(); // e.g., "not reachable", "no points"
}
```

### Allocate a Path

```java
// Allocate all nodes on the path to target
AllocationResult result = service.allocatePath(entityRef, treeId, targetNodeId);
```

### Refund a Node

```java
RefundResult result = service.refundNode(entityRef, treeId, nodeId);
if (result.success()) {
    int pointsReturned = result.pointsReturned();
    // Orphaned nodes are automatically refunded
}
```

### Refund All Nodes

```java
RefundResult result = service.refundAll(entityRef, treeId);
```

### Query Methods

```java
// Get allocated nodes
Set<String> allocated = service.getAllocatedNodes(entityRef, treeId);

// Get available points
int available = service.getAvailablePoints(entityRef, treeId);
int generalAvailable = service.getAvailableGeneralPoints(entityRef);
int classAvailable = service.getAvailableClassPoints(entityRef, "hyforged:warrior");

// Get reachable unallocated nodes
Set<String> reachable = service.getReachableNodes(entityRef, treeId);

// Find shortest path to a node
List<String> path = service.findPathToNode(entityRef, treeId, nodeId);

// Check if allocation is possible
boolean canAllocate = service.canAllocate(entityRef, treeId, nodeId);
```

### Point Book Methods

```java
// Check book points used
int bookPointsUsed = service.getBookPointsUsed(entityRef);

// Get max book points (default: 20)
int maxBookPoints = service.getMaxBookPoints();

// Consume a Point Book item
boolean success = service.consumePointBook(entityRef);
```

### Refund Cost Calculation

```java
// Cost for single node
int cost = service.calculateRefundCost(entityRef, nodeId);

// Total cost for multiple nodes
int totalCost = service.calculateTotalRefundCost(entityRef, nodeIds);
```

### Listen to Allocation Events

```java
plugin.getEventRegistry().register(
    PassiveNodeAllocatedEvent.class,
    event -> {
        String nodeId = event.getNodeId();
        PassiveNode node = event.getNode();
        Ref<EntityStore> entity = event.getEntityRef();
        // Handle allocation
    }
);

plugin.getEventRegistry().register(
    PassiveNodeRefundedEvent.class,
    event -> {
        String nodeId = event.getNodeId();
        // Handle refund
    }
);
```

### Registry API (for extending trees)

```java
PassiveTreeRegistry registry = PassiveTreeRegistry.get();

// Add a node to an existing tree
registry.addNode(treeId, node);

// Add a connection between nodes
registry.addConnection(treeId, new PassiveConnection(fromId, toId));

// Get all registered trees
Collection<PassiveTree> allTrees = registry.getAllTrees();
```

## Multi-Mod Best Practices

1. **Always use namespaced IDs** — `yourmod:node-name`
2. **Connect to existing nodes via layout Connections** — Don't modify other mods' files
3. **Choose unique positions** — Avoid overlapping with base tree nodes
4. **Use InstanceId for reused templates** — Prevents ID conflicts
5. **Prefer additive over replacement** — Each mod adds its own files

## Load Order

1. Node templates from all mods are loaded
2. Tree definitions are registered
3. Layout files are merged additively
4. Connections can reference any node from any mod

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Node not appearing | Check TreeId in layout matches tree definition Id |
| Connection not working | Verify both node IDs exist (check for typos) |
| Duplicate node error | Use InstanceId when placing same template multiple times |
| Layout not loading | Ensure JSON is valid and in correct folder path |
