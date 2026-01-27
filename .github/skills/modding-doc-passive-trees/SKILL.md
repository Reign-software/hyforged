---
name: modding-doc-passive-trees
description: Implements passive skill trees for character progression in Hyforged. Use when adding nodes to existing trees, creating new class trees, defining node templates, creating layout files, or working with PassiveTreeService, PassiveTreeRegistry, or node effects. Also use when deriving guidance from Modding_Doc/PassiveTrees. Triggers - passive tree, passive node, skill tree, class tree, general tree, node template, layout file, starting node, keystone, notable, mastery, modding doc.
---

# Hyforged Passive Tree System (Implemented)

This skill reflects the current implementation described in Modding_Doc/PassiveTrees.

## Quick Reference

| Task | Approach |
|------|----------|
| Add nodes to the general tree | Create node template JSON + layout JSON (no tree def needed) |
| Create a new class tree | Tree definition JSON + node templates + layout |
| Reuse a node template | Use `InstanceId` in layout placements |
| Connect to existing nodes | Reference node IDs in layout `Connections` |
| Cross-mod connections | Use namespaced IDs (e.g., `hyforged:notable-fire-mastery`) |
| Mark a starting node | Use `IsStarting: true` on placement or add to `StartingNodes` array |

## General Tree Layout Architecture

The general passive tree uses a **top-down vertical layout** designed for vertical-only scrolling.

### 4 Main Attribute Lanes

| Lane | X Range | Sub-paths (X) | Theme |
|------|---------|---------------|-------|
| Strength | -250 to -150 | -230, -200, -170 | Physical damage, melee, DoT |
| Dexterity | -120 to -20 | -100, -70, -40 | Evasion, accuracy, projectiles |
| Intelligence | 20 to 120 | 40, 70, 100 | Spells, mana, elemental |
| Wisdom | 150 to 250 | 170, 200, 230 | Regen, resistance, concentration |

### Bridge Zones

Bridge zones connect adjacent lanes, allowing hybrid builds:

| Bridge | Position | Nodes Used | Connects |
|--------|----------|------------|----------|
| Constitution | X = -135 | `travel-constitution`, `thick-skin`, `second-wind` | STR ↔ DEX |
| Spirit | X = 0 | `travel-spirit`, `wellspring`, `soul-siphon` | DEX ↔ INT |
| Luck | X = 135 | `travel-luck`, `fortune-favors`, `lucky-strike` | INT ↔ WIS |

### Coordinate System

- **Y = 0**: Starting nodes (top of tree)
- **Y increases downward**: Tree extends from Y=40 to Y=570+
- **X centered around 0**: Lanes spread from X=-250 to X=250
- **COORD_SCALE = 1.5f**: Multiplier for rendering positions

### Layout File Organization

```
layouts/general/
├── starting-nodes.json    # 4 starting nodes at Y=0
├── strength.json          # STR lane (3 vertical paths)
├── dexterity.json         # DEX lane (3 vertical paths)
├── intelligence.json      # INT lane (3 vertical paths)
├── wisdom.json            # WIS lane (3 vertical paths)
├── bridges.json           # CON/SPI/LUCK bridge clusters
└── stat-coverage.json     # Test/debug nodes (disconnected)
```

### UI Constraints

- **Vertical-only scrolling**: Native `TopScrolling` layout mode
- **Fixed viewport**: 800×600px with 120px sidebar
- **Horizontal centering**: Tree content centered in viewport

## Documentation References

- [Passive Tree Overview](../../../Modding_Doc/PassiveTrees/README.md) — JSON schema, concepts, load order
- [Passive Tree API Reference](../../../Modding_Doc/PassiveTrees/API.md) — Allocation, refunds, events, effect handlers

## Core Concepts

### Graph-Based Structure
Passive trees are graphs. Players allocate from starting nodes and must stay connected.

### Effect Stacking
All stat modifiers stack per the Stats System rules.

### Multi-File Additive Structure
The system is **additive** across mods:

1. **Node Templates** (`nodes/`) — Define what nodes do (no positions)
2. **Tree Definitions** (`trees/`) — Only required for **new trees**
3. **Layout Files** (`layouts/`) — Placement + connections (merged across mods)

### Load Order & Merging
1. All `nodes/` files load and register node templates
2. All `layouts/` files merge additively (placements, connections, starting nodes)
3. Connections can reference any node ID from any mod

### Cross-Mod Connections

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
│   └── classes/
│       └── my-class.json         # Only needed for new class trees
├── nodes/
│   ├── general/                  # General tree node templates
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

## JSON Schemas

### Tree Definition Schema (new trees only)

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
            "Type": "minor|notable|keystone|unlock|mastery",
            "Name": "Display Name",
            "Description": "Effect description",
            "Effects": [],
            "Icon": "yourmod:icons/passive/icon-name",
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
            "InstanceId": "yourmod:unique-instance-id",
            "IsStarting": true
        }
    ],
    "Connections": [
        { "From": "yourmod:node-a", "To": "yourmod:node-b" }
    ],
    "StartingNodes": ["yourmod:start-node"],
    "TextLabels": [
        {
            "Text": "STRENGTH",
            "Position": { "X": -200, "Y": 20 },
            "FontSize": 16,
            "Color": "#FFCC00",
            "Anchor": "center",
            "Region": "strength",
            "FontWeight": "bold",
            "Opacity": 1.0,
            "Rotation": 0.0
        }
    ]
}
```

**Placement fields:**
- `NodeId` — Reference to a node template (required)
- `Position` — `{ "X": number, "Y": number }` (required)
- `Region` — Visual grouping (`strength`, `dexterity`, `intelligence`, `wisdom`, `bridge`)
- `InstanceId` — Unique ID when placing same template multiple times
- `IsStarting` — Mark this placement as a starting node (alternative to `StartingNodes` array)

**TextLabel fields:**
- `Text` — The text content to display (required)
- `Position` — `{ "X": number, "Y": number }` in tree coordinates (required)
- `FontSize` — Font size in pixels (default: 14)
- `Color` — Hex color string (default: "#FFFFFF")
- `Anchor` — Text alignment: `left`, `center`, or `right` (default: "left")
- `Region` — Visual grouping for filtering/highlighting
- `FontWeight` — `normal` or `bold` (default: "normal")
- `Opacity` — 0.0 to 1.0 (default: 1.0)
- `Rotation` — Rotation in degrees (default: 0.0)

**Region values for general tree:**
- `strength`, `dexterity`, `intelligence`, `wisdom` — Main lanes
- `bridge` — Cross-lane connection zones

**Position guidelines:**
- Y=0 is reserved for starting nodes
- Y=40+ for first tier of regular nodes
- Keep X within lane ranges (see Layout Architecture above)
- Bridge nodes use X positions between lanes

## Node Types

| Type | Purpose |
|------|---------|
| `minor` | Small stat bonuses |
| `notable` | Significant bonuses, often multiple effects |
| `keystone` | Build-defining tradeoffs (family-limited) |
| `unlock` | Grants access to abilities/mechanics |
| `mastery` | Choice-based node (mutually exclusive options) |

## Node Effects

### stat-modifier
Values use basis points for percentages (10000 = 100%).

```json
{ "Type": "stat-modifier", "Stat": "hyforged:strength", "Value": 10 }
```

### spell-grant

```json
{ "Type": "spell-grant", "SpellId": "hyforged:fireball" }
```

### unlock-flag

```json
{ "Type": "unlock-flag", "FlagId": "hyforged:stun-immune" }
```

### mastery-choice

```json
{
    "Type": "mastery-choice",
    "Choices": [
        { "Type": "stat-modifier", "Stat": "hyforged:damage-increase", "Value": 2000 },
        { "Type": "stat-modifier", "Stat": "hyforged:defense-increase", "Value": 2000 }
    ]
}
```

## Connections

Connections are **bidirectional** and define adjacency for allocation.

```json
{
    "Connections": [
        { "From": "yourmod:start", "To": "yourmod:node1" },
        { "From": "yourmod:node1", "To": "yourmod:node2" }
    ]
}
```

## Point Economy

### General Tree
$$\text{Available} = (\text{Character Level} - 1) + \text{Book Points Used} - \text{Allocated Nodes}$$

### Class Tree
$$\text{Available} = \text{Class Level} - \text{Allocated Nodes}$$

## Refund System

### Refund Rules
- Single refunds only for leaf nodes.
- Orphaned nodes refund together if a refund breaks connectivity.

### Refund Config
Configure in `Server/<YourMod>/PassiveTrees/refund-config.json`:

```json
{
    "BaseCost": 10,
    "CostPerLevel": 5,
    "MaxBookPoints": 30
}
```

## Programmatic API (Current)

### Service & Registry

```java
PassiveTreeService service = PassiveTreeService.get();
PassiveTreeRegistry registry = PassiveTreeRegistry.get();
```

### Queries

```java
PassiveTree generalTree = service.getGeneralTree();
PassiveTree classTree = service.getClassTree("hyforged:warrior");
PassiveTree tree = service.getTree("hyforged:passive-tree-general");

Set<String> allocated = service.getAllocatedNodes(entityRef, treeId);
int points = service.getAvailablePoints(entityRef, treeId);
int generalPoints = service.getAvailableGeneralPoints(entityRef);
int classPoints = service.getAvailableClassPoints(entityRef, "hyforged:warrior");

Set<String> reachable = service.getReachableNodes(entityRef, treeId);
List<String> path = service.findPathToNode(entityRef, treeId, nodeId);
boolean canAllocate = service.canAllocate(entityRef, treeId, nodeId);
```

### Allocation

```java
AllocationResult result = service.allocateNode(entityRef, treeId, nodeId);
AllocationResult pathResult = service.allocatePath(entityRef, treeId, targetNodeId);
```

Allocation failure reasons:
`ALREADY_ALLOCATED`, `NOT_ADJACENT`, `INSUFFICIENT_POINTS`, `REQUIREMENTS_NOT_MET`, `KEYSTONE_CONFLICT`.

### Refunds

```java
int cost = service.calculateRefundCost(entityRef, nodeId);
int totalCost = service.calculateTotalRefundCost(entityRef, List.of(node1, node2));

RefundResult single = service.refundNode(entityRef, treeId, nodeId);
RefundResult all = service.refundAll(entityRef, treeId);
RefundResult free = service.refundAllFree(entityRef, treeId);

Set<String> orphaned = service.getOrphanedNodes(entityRef, treeId, nodeId);
```

Refund failure reasons:
`NOT_ALLOCATED`, `WOULD_ORPHAN`, `INSUFFICIENT_CURRENCY`.

### Events

```java
EventRegistry.register(PassiveNodeAllocatedEvent.class, EventPriority.NORMAL, event -> { /* ... */ });
EventRegistry.register(PassiveNodeRefundedEvent.class, EventPriority.NORMAL, event -> { /* ... */ });
EventRegistry.register(PassiveTreeRespecEvent.class, EventPriority.NORMAL, event -> { /* ... */ });
EventRegistry.register(PointBookConsumedEvent.class, EventPriority.NORMAL, event -> { /* ... */ });
```

### Custom Effect Handlers

```java
public class MyCustomEffectHandler implements PassiveEffectHandler {
    @Override
    public void apply(Ref<EntityStore> entityRef, PassiveNode node, PassiveNodeEffect effect) { }
    @Override
    public void remove(Ref<EntityStore> entityRef, PassiveNode node, PassiveNodeEffect effect) { }
    @Override
    public String getTooltipText(PassiveNodeEffect effect) { return ""; }
}

PassiveEffectRegistry.get().register("my-custom-effect", new MyCustomEffectHandler());
```

### Point Books

```json
{
    "Id": "yourmod:skill-point-book",
    "DisplayName": "Book of Skill Points",
    "Interactions": [
        { "Type": "hyforged:point-book", "PointsGranted": 1 }
    ]
}
```

```java
PassiveTreeComponent component = entityRef.get(passiveTreeComponentType);
if (component != null) {
    component.addBookPoints(1);
}
```

### Graph Utilities

```java
List<String> path = PassiveTreeGraph.findShortestPath(tree, allocatedNodes, targetNodeId);
boolean connected = PassiveTreeGraph.isConnectedToStart(tree, allocatedNodes, startNodeId);
Set<String> reachable = PassiveTreeGraph.getReachableFromStart(tree, allocatedNodes, startNodeId);
Set<String> orphaned = PassiveTreeGraph.getOrphanedNodes(tree, allocatedNodes, startNodeId, nodeToRemove);
Set<String> available = PassiveTreeGraph.getReachableUnallocatedNodes(tree, allocatedNodes);
boolean canAlloc = PassiveTreeGraph.canAllocateNode(tree, allocatedNodes, nodeId);
boolean canDealloc = PassiveTreeGraph.canDeallocateNode(tree, allocatedNodes, startNodeId, nodeId);
List<String> order = PassiveTreeGraph.getPathAllocationOrder(tree, allocatedNodes, path);
```

## Version Migration
Increment `Version` when changing tree structure. The system refunds invalid allocations and returns points automatically.

## Multi-Mod Best Practices

1. Always use namespaced IDs (`yourmod:node-name`).
2. Connect via your own layout files; do not edit other mods’ files.
3. Use `InstanceId` for template reuse to avoid duplicate ID errors.
4. Keep layouts additive and minimal.
5. **For general tree additions**: Place nodes in appropriate lane X ranges.
6. **For bridge nodes**: Use X positions between lanes (-135, 0, or 135).
7. **Connect to existing nodes**: Reference `hyforged:` prefixed IDs from main layouts.

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Node not appearing | Check `TreeId` matches the tree definition ID and JSON is valid |
| Connection not working | Verify both node IDs exist (typos, namespacing) |
| Duplicate node error | Use `InstanceId` when placing the same template multiple times |
| Layout not loading | Ensure JSON is valid and in correct folder path |
| Node outside viewport | Check X is within -250 to 250 range |
| Node not reachable | Ensure connection path exists from starting node |

## Visual Templates (Data-Driven)

Visual templates define the appearance of node frames and icons. They are fully data-driven from JSON.

### Frame Templates

Located at `Server/<Mod>/PassiveTrees/templates/frame-templates.json`:

```json
{
    "FrameTemplates": [
        {
            "Id": "hyforged:frame-minor",
            "Size": 24,
            "AllocatedTexture": "Hyforged/Textures/PassiveTree/FrameMinorAllocated.png",
            "AvailableTexture": "Hyforged/Textures/PassiveTree/FrameMinorAvailable.png",
            "LockedTexture": "Hyforged/Textures/PassiveTree/FrameMinorLocked.png"
        }
    ],
    "TypeDefaults": {
        "minor": "hyforged:frame-minor",
        "notable": "hyforged:frame-notable",
        "keystone": "hyforged:frame-keystone",
        "mastery": "hyforged:frame-mastery",
        "starting": "hyforged:frame-starting"
    }
}
```

### Node Icons

Icons are specified directly in node templates using the `Icon` field with a texture path:

```json
{
    "Nodes": [
        {
            "Id": "hyforged:travel-strength",
            "Type": "minor",
            "Effects": [{ "Stat": "hyforged:strength", "Value": 1 }],
            "Icon": "Hyforged/Textures/Strength.png"
        }
    ]
}
```

If no `Icon` is specified, the default icon (`Hyforged/Textures/Passive.png`) is used.

### Default Frame Sizes by Type

| Type | Size | Purpose |
|------|------|---------|
| minor | 24px | Small stat bonuses |
| notable | 32px | Significant bonuses |
| keystone | 48px | Build-defining |
| mastery | 36px | Choice nodes |
| starting | 36px | Entry points |

### Key Classes

| Class | Purpose |
|-------|---------|
| `NodeVisualTemplate` | Record defining frame textures and size |
| `NodeVisualTemplateRegistry` | Singleton registry for frame templates |
| `NodeVisualTemplateAsset` | Asset loader for frame-templates.json |
