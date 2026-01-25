# Passive Trees API Reference

This document provides code examples for working with the Hyforged Passive Trees system.

## Table of Contents

- [Service Access](#service-access)
- [Tree Queries](#tree-queries)
- [Node Allocation](#node-allocation)
- [Refund Operations](#refund-operations)
- [Events](#events)
- [Custom Effect Handlers](#custom-effect-handlers)
- [Point Books](#point-books)

---

## Service Access

### Get the Service

```java
PassiveTreeService service = PassiveTreeService.get();
```

### Initialize the Service (required)

Call during plugin setup after component registration:

```java
PassiveTreeService.get().initialize(
    passiveTreeComponentType,
    playerUnlocksComponentType,
    playerSpellsComponentType,
    progressionComponentType
);
```

### Get the Registry

```java
PassiveTreeRegistry registry = PassiveTreeRegistry.get();
```

---

## Tree Queries

### Get Trees

```java
// Get the general tree
PassiveTree generalTree = service.getGeneralTree();

// Get a class tree
PassiveTree classTree = service.getClassTree("hyforged:warrior");

// Get any tree by ID
PassiveTree tree = service.getTree("hyforged:passive-tree-general");
```

### Query Allocated Nodes

```java
// Get all allocated nodes for a player in a tree
Set<String> allocated = service.getAllocatedNodes(entityRef, treeId);

// Get available points
int points = service.getAvailablePoints(entityRef, treeId);
int generalPoints = service.getAvailableGeneralPoints(entityRef);
int classPoints = service.getAvailableClassPoints(entityRef, "hyforged:warrior");
```

### Query Reachable Nodes

```java
// Get nodes the player can allocate next
Set<String> reachable = service.getReachableNodes(entityRef, treeId);

// Find path to a distant node
List<String> path = service.findPathToNode(entityRef, treeId, "hyforged:keystone-node");
```

---

## Node Allocation

### Allocate Single Node

```java
AllocationResult result = service.allocateNode(entityRef, treeId, nodeId);

if (result.success()) {
    // Node allocated successfully
} else {
    // Check failure reason
    switch (result.reason()) {
        case AllocationResult.REASON_ALREADY_ALLOCATED -> { /* ... */ }
        case AllocationResult.REASON_NOT_CONNECTED -> { /* ... */ }
        case AllocationResult.REASON_INSUFFICIENT_POINTS -> { /* ... */ }
        case AllocationResult.REASON_KEYSTONE_CONFLICT -> { /* ... */ }
        case AllocationResult.REASON_NODE_NOT_FOUND -> { /* ... */ }
        case AllocationResult.REASON_TREE_NOT_FOUND -> { /* ... */ }
    }
}
```

### Allocate Path

Auto-allocate all nodes in the path to a target node:

```java
AllocationResult result = service.allocatePath(entityRef, treeId, targetNodeId);

if (result.success()) {
    // All nodes in path allocated
} else {
    // Path allocation failed (insufficient points, blocked, etc.)
}
```

### Check Before Allocating

```java
boolean canAllocate = service.canAllocate(entityRef, treeId, nodeId);
```

---

## Refund Operations

### Calculate Refund Cost

```java
// Cost to refund a single node
int cost = service.calculateRefundCost(entityRef, nodeId);

// Cost to refund multiple nodes
int totalCost = service.calculateTotalRefundCost(entityRef, List.of(node1, node2, node3));
```

### Refund Single Node

```java
RefundResult result = service.refundNode(entityRef, treeId, nodeId);

if (result.success()) {
    int pointsRefunded = result.pointsReturned();
    int tradebarCost = result.totalCost();
} else {
    // Check failure reason
    switch (result.reason()) {
        case RefundResult.REASON_NODE_NOT_ALLOCATED -> { /* ... */ }
        case RefundResult.REASON_CANNOT_REFUND_STARTING_NODE -> { /* ... */ }
        case RefundResult.REASON_INSUFFICIENT_TRADEBARS -> { /* ... */ }
        case RefundResult.REASON_TREE_NOT_FOUND -> { /* ... */ }
    }
}
```

### Full Respec

```java
// Refund all nodes in a tree (costs currency)
RefundResult result = service.refundAll(entityRef, treeId);

// Free respec (admin/special circumstances)
RefundResult result = service.refundAllFree(entityRef, treeId);
```

### Check for Orphans

```java
// Get nodes that would be orphaned if a node is refunded
Set<String> orphaned = service.getOrphanedNodes(entityRef, treeId, nodeId);
```

---

## Events

### Node Allocated Event

```java
HytaleServer.get().getEventBus()
    .registerGlobal((short) 0, PassiveNodeAllocatedEvent.class, event -> {
        Ref<EntityStore> entityRef = event.entityRef();
        String treeId = event.treeId();
        String nodeId = event.nodeId();
        int remaining = event.remainingPoints();
        
        // React to allocation
    });
```

### Node Refunded Event

```java
HytaleServer.get().getEventBus()
    .registerGlobal((short) 0, PassiveNodeRefundedEvent.class, event -> {
        Ref<EntityStore> entityRef = event.entityRef();
        String treeId = event.treeId();
        List<String> refunded = event.refundedNodes();
        int tradebarCost = event.tradebarCost();
        
        // React to refund
    });
```

### Tree Respec Event

```java
HytaleServer.get().getEventBus()
    .registerGlobal((short) 0, PassiveTreeRespecEvent.class, event -> {
        Ref<EntityStore> entityRef = event.entityRef();
        String treeId = event.treeId();
        int nodesRefunded = event.nodeCount();
        int totalCost = event.tradebarCost();
        
        // React to full respec
    });
```

### Point Book Consumed Event

```java
HytaleServer.get().getEventBus()
    .registerGlobal((short) 0, PointBookConsumedEvent.class, event -> {
        Ref<EntityStore> entityRef = event.entityRef();
        int totalBookPoints = event.newBookPointTotal();
        int maxBookPoints = event.maxBookPoints();
        
        // React to point book use
    });
```

---

## Custom Effect Handlers

### Implement PassiveEffectHandler

```java
public class MyCustomEffectHandler implements PassiveEffectHandler {
    
    @Override
    public void apply(Ref<EntityStore> entityRef, PassiveNode node, PassiveNodeEffect effect) {
        // Extract data from effect
        String customData = effect.getString("CustomField");
        
        // Apply your effect
        // ...
    }
    
    @Override
    public void remove(Ref<EntityStore> entityRef, PassiveNode node, PassiveNodeEffect effect) {
        // Remove your effect
        // ...
    }
    
    @Override
    public String getTooltipText(PassiveNodeEffect effect) {
        return "Custom effect description";
    }
}
```

### Register Your Handler

```java
@Override
public void setup(PluginHandle handle) {
    // Register during plugin setup
    PassiveEffectRegistry.get().register("my-custom-effect", new MyCustomEffectHandler());
}
```

### Use in JSON

```json
{
    "Id": "yourmod:custom-node",
    "Type": "notable",
    "Name": "Custom Node",
    "Description": "Grants a custom effect.",
    "Effects": [
        { 
            "Type": "my-custom-effect",
            "CustomField": "custom-value"
        }
    ]
}
```

---

## Point Books

### Create Point Book Item

Define the item in `Server/<YourMod>/Item/point-book.json`:

```json
{
    "Id": "yourmod:skill-point-book",
    "DisplayName": "Book of Skill Points",
    "Description": "Grants 1 passive skill point when consumed.",
    "MaxStackSize": 99,
    "Interactions": {
        "Secondary": {
            "Type": "Simple",
            "Interactions": [
                { "Type": "hyforged:point-book-consume" }
            ]
        }
    }
}
```

### Programmatic Point Grant

```java
PassiveTreeComponent component = entityRef.get(passiveTreeComponentType);
if (component != null) {
    component.addBookPoint();
}
```

---

## Graph Utilities

### PassiveTreeGraph Static Methods

```java
// Find shortest path from allocated nodes to a target
List<String> path = PassiveTreeGraph.findShortestPath(tree, allocatedNodes, targetNodeId);

// Check if allocations remain connected to start
boolean connected = PassiveTreeGraph.isConnectedToStart(tree, allocatedNodes, startNodeId);

// Get nodes reachable from start through allocated nodes
Set<String> reachable = PassiveTreeGraph.getReachableFromStart(tree, allocatedNodes, startNodeId);

// Get nodes that would be orphaned if a node is removed
Set<String> orphaned = PassiveTreeGraph.getOrphanedNodes(tree, allocatedNodes, startNodeId, nodeToRemove);

// Get unallocated nodes adjacent to allocated nodes
Set<String> available = PassiveTreeGraph.getReachableUnallocatedNodes(tree, allocatedNodes);

// Validation helpers
boolean canAlloc = PassiveTreeGraph.canAllocateNode(tree, allocatedNodes, nodeId);
boolean canDealloc = PassiveTreeGraph.canDeallocateNode(tree, allocatedNodes, startNodeId, nodeId);

// Get allocation order for a path
List<String> order = PassiveTreeGraph.getPathAllocationOrder(tree, allocatedNodes, path);
```

---

## Component Access

### PassiveTreeComponent

```java
PassiveTreeComponent component = entityRef.get(passiveTreeComponentType);

// General tree allocations
Set<String> generalNodes = component.getGeneralAllocatedNodes();
int generalCount = component.getGeneralAllocatedCount();

// Class tree allocations
Set<String> classNodes = component.getClassAllocatedNodes("hyforged:warrior");
int classCount = component.getClassAllocatedCount("hyforged:warrior");

// Book points
int bookPoints = component.getBookPointsUsed();
component.addBookPoint();
```

### PlayerUnlocksComponent

```java
PlayerUnlocksComponent unlocks = entityRef.get(playerUnlocksComponentType);

// Check if a flag is unlocked
boolean hasFlag = unlocks.hasFlag("hyforged:stun-immune");

// Get all flags from a source node
Set<String> nodeFlags = unlocks.getFlagsFromSource("hyforged:keystone-node");
```

### PlayerSpellsComponent

```java
PlayerSpellsComponent spells = entityRef.get(playerSpellsComponentType);

// Check if a spell is granted
boolean hasSpell = spells.hasSpell("hyforged:fireball");

// Get all spells from a source node
Set<String> nodeSpells = spells.getSpellsFromSource("hyforged:spell-unlock-node");
```
