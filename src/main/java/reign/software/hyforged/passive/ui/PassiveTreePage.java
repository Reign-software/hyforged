package reign.software.hyforged.passive.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.graph.PassiveTreeGraph;
import reign.software.hyforged.passive.model.PassiveConnection;
import reign.software.hyforged.passive.model.PassiveNode;
import reign.software.hyforged.passive.model.PassiveNodeEffect;
import reign.software.hyforged.passive.model.PassiveNodeType;
import reign.software.hyforged.passive.model.PassiveTree;
import reign.software.hyforged.passive.registry.PassiveTreeRegistry;
import reign.software.hyforged.passive.service.PassiveTreeService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Passive Tree UI page for viewing and allocating passive nodes.
 * <p>
 * Displays the passive tree with:
 * <ul>
 *   <li>Zoomable/pannable tree canvas</li>
 *   <li>Node tooltips with effects</li>
 *   <li>Allocation/refund interactions</li>
 *   <li>Point tracking panel</li>
 * </ul>
 * <p>
 * Access methods:
 * <ul>
 *   <li>Command: {@code /passive ui} (TODO: implement command)</li>
 *   <li>Interaction: Configurable keybind (TODO: configure)</li>
 * </ul>
 *
 * @see InteractiveCustomUIPage
 */
public class PassiveTreePage extends InteractiveCustomUIPage<PassiveTreePage.PageEventData> {
    
    private static final Logger LOGGER = Logger.getLogger(PassiveTreePage.class.getName());
    
    /** UI file path for the passive tree page layout */
    private static final String PAGE_UI_FILE = "UI/Hyforged/PassiveTreePage.ui";
    
    /** The tree being viewed (null for tree selection mode) */
    @Nullable
    private final String treeId;
    
    /** Pending confirmation action (e.g., "respec", "refund-keystone") */
    @Nullable
    private String pendingAction;
    
    /** Pending node ID for actions that target a specific node */
    @Nullable
    private String pendingNodeId;
    
    /** Current search query for filtering nodes */
    @Nullable
    private String currentSearchQuery;
    
    /** Set of node IDs matching the current search filter */
    @Nonnull
    private Set<String> searchMatchNodes = new HashSet<>();
    
    /** Currently highlighted path to target node */
    @Nonnull
    private Set<String> highlightedPath = new HashSet<>();
    
    /** Node ID being highlighted for path preview */
    @Nullable
    private String pathTargetNodeId;
    
    /** Comparison mode: shows stat differences */
    private boolean comparisonMode;
    
    /** Node ID being compared in comparison mode */
    @Nullable
    private String comparisonNodeId;
    
    /** Current zoom level (1.0 = 100%) */
    private float zoomLevel = 1.0f;
    
    /** Minimum zoom level */
    private static final float MIN_ZOOM = 0.25f;
    
    /** Maximum zoom level */
    private static final float MAX_ZOOM = 2.0f;
    
    /** Zoom step per action */
    private static final float ZOOM_STEP = 0.25f;
    
    /** Pan offset X */
    private int panOffsetX = 0;
    
    /** Pan offset Y */
    private int panOffsetY = 0;
    
    /**
     * Create a new PassiveTreePage for the general tree.
     *
     * @param playerRef The player viewing the tree
     */
    public PassiveTreePage(@Nonnull PlayerRef playerRef) {
        this(playerRef, null);
    }
    
    /**
     * Create a new PassiveTreePage for a specific tree.
     *
     * @param playerRef The player viewing the tree
     * @param treeId The tree ID to display, or null for general tree
     */
    public PassiveTreePage(@Nonnull PlayerRef playerRef, @Nullable String treeId) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.treeId = treeId;
    }
    
    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        // Append the main UI layout
        commandBuilder.append(PAGE_UI_FILE);
        
        // Get component
        PassiveTreeComponent passiveComponent = getPassiveTreeComponent(ref, store);
        
        // Determine which tree to show
        final String activeTreeId = getActiveTreeId();
        
        // Get allocation info for the side panel
        final int availablePoints = PassiveTreeService.get().getAvailablePoints(ref, activeTreeId);
        PassiveTree tree = PassiveTreeRegistry.get().getTree(activeTreeId);
        
        final int allocatedCount;
        final int maxPoints;
        final int bookPoints;
        final boolean isGeneralTree;
        Set<String> allocatedNodes;
        
        if (passiveComponent != null && tree != null) {
            isGeneralTree = tree.isGeneralTree();
            allocatedNodes = isGeneralTree ? 
                    passiveComponent.getGeneralAllocatedNodes() :
                    passiveComponent.getClassAllocatedNodes(tree.getClassId());
            allocatedCount = allocatedNodes.size();
            
            // Max points calculation
            maxPoints = PassiveTreeService.get().getAvailablePoints(ref, activeTreeId) + allocatedCount;
            
            // Book points (general tree only)
            bookPoints = isGeneralTree ? passiveComponent.getBookPointsUsed() : 0;
        } else {
            allocatedCount = 0;
            maxPoints = 0;
            bookPoints = 0;
            isGeneralTree = true;
            allocatedNodes = Set.of();
        }
        
        // Populate side panel with point info
        populateSidePanel(commandBuilder, tree, availablePoints, allocatedCount, maxPoints, bookPoints, isGeneralTree);
        
        // Check if starting region selection is needed (General Tree, first time)
        boolean needsStartingRegion = isGeneralTree && tree != null 
                && passiveComponent != null 
                && passiveComponent.getGeneralStartingNode() == null
                && allocatedNodes.isEmpty();
        
        if (needsStartingRegion) {
            // Show starting region selection overlay
            showStartingRegionSelection(commandBuilder, eventBuilder, tree);
        }
        
        // Render tree nodes on the canvas
        if (tree != null) {
            renderTreeCanvas(commandBuilder, eventBuilder, tree, allocatedNodes, passiveComponent);
        }
        
        // Add close button event
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CloseButton",
                EventData.of("Action", "close"),
                false
        );
        
        // Add tree selector events
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#GeneralTreeTab",
                EventData.of("Action", "selectGeneral"),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ClassTreeTab",
                EventData.of("Action", "selectClass"),
                false
        );
        
        // Add quick action events
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#FullRespec",
                EventData.of("Action", "respec"),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#UndoLast",
                EventData.of("Action", "undoLast"),
                false
        );
        
        // Add search event
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#SearchInput",
                EventData.of("@Query", "#SearchInput.Value"),
                false
        );
        
        // Add zoom control events
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ZoomIn",
                EventData.of("Action", "zoomIn"),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ZoomOut",
                EventData.of("Action", "zoomOut"),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ZoomReset",
                EventData.of("Action", "zoomReset"),
                false
        );
        
        // Add comparison mode toggle event
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ComparisonToggle",
                EventData.of("Action", "toggleComparison"),
                false
        );
        
        // Add clear search button event
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ClearSearch",
                EventData.of("Action", "clearSearch"),
                false
        );
        
        // Add confirmation dialog events
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ConfirmCancel",
                EventData.of("Action", "confirmCancel"),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ConfirmAccept",
                EventData.of("Action", "confirmAccept"),
                false
        );
        
        // Log initialization for debugging
        LOGGER.fine(() -> String.format(
                "PassiveTreePage built: tree=%s, available=%d, allocated=%d",
                activeTreeId, availablePoints, allocatedCount));
    }
    
    /**
     * Populate the side panel with point information.
     */
    private void populateSidePanel(
            @Nonnull UICommandBuilder commandBuilder,
            @Nullable PassiveTree tree,
            int availablePoints,
            int allocatedCount,
            int maxPoints,
            int bookPoints,
            boolean isGeneralTree
    ) {
        // Set tree name
        String treeName = tree != null ? tree.getId() : "Passive Tree";
        commandBuilder.set("#TreeName.Text", treeName);
        
        // Set tree type label
        String treeType = isGeneralTree ? "General Passive Tree" : "Class: " + (tree != null ? tree.getClassId() : "Unknown");
        commandBuilder.set("#TreeType.Text", treeType);
        
        // Set point values
        commandBuilder.set("#AvailableValue.Text", String.valueOf(availablePoints));
        commandBuilder.set("#AllocatedValue.Text", String.valueOf(allocatedCount));
        commandBuilder.set("#MaxValue.Text", String.valueOf(maxPoints));
        
        // Show/hide book points row (general tree only)
        commandBuilder.set("#BookPointsRow.Visible", isGeneralTree);
        if (isGeneralTree) {
            commandBuilder.set("#BookPointsValue.Text", String.valueOf(bookPoints));
        }
        
        // Set tab highlighting based on current tree
        commandBuilder.set("#GeneralTreeTab.Selected", isGeneralTree);
        commandBuilder.set("#ClassTreeTab.Selected", !isGeneralTree);
    }
    
    /**
     * Show the starting region selection overlay for first-time General Tree access.
     */
    private void showStartingRegionSelection(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull PassiveTree tree
    ) {
        commandBuilder.set("#RegionSelectionOverlay.Visible", true);
        
        // Clear and populate region options
        commandBuilder.clear("#RegionOptions");
        
        Set<String> startingNodeIds = tree.getStartingNodeIds();
        for (String startingNodeId : startingNodeIds) {
            PassiveNode node = tree.getNode(startingNodeId);
            if (node == null) continue;
            
            // Get region name from the node's region field or use node name
            String regionName = node.region() != null ? node.region() : node.name();
            String regionColor = getRegionColor(node.region());
            
            // Create a region option button
            String selector = "Region_" + sanitizeSelector(startingNodeId);
            String regionUI = String.format(
                "Button #%s { Text: \"%s\"; Style: (Width: 120; Height: 80; Background: %s; " +
                "BorderRadius: 8; Margin: 5); DataEvent: { Action: \"selectRegion\", NodeId: \"%s\" }; }",
                selector, regionName, regionColor, startingNodeId
            );
            commandBuilder.appendInline("#RegionOptions", regionUI);
            
            // Add event binding for region selection
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#" + selector,
                    EventData.of("Action", "selectRegion").append("NodeId", startingNodeId),
                    false
            );
        }
    }
    
    /**
     * Get color for a region (for visual theming).
     */
    private String getRegionColor(@Nullable String region) {
        if (region == null) return "rgba(80, 80, 90, 0.9)";
        
        String lower = region.toLowerCase();
        if (lower.contains("strength") || lower.contains("str") || lower.contains("warrior")) {
            return "rgba(180, 50, 50, 0.9)"; // Red
        } else if (lower.contains("dexterity") || lower.contains("dex") || lower.contains("ranger")) {
            return "rgba(50, 150, 50, 0.9)"; // Green
        } else if (lower.contains("intelligence") || lower.contains("int") || lower.contains("mage")) {
            return "rgba(50, 80, 180, 0.9)"; // Blue
        }
        return "rgba(80, 80, 90, 0.9)"; // Default grey
    }
    
    /**
     * Render the tree nodes and connections on the canvas.
     */
    private void renderTreeCanvas(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nullable PassiveTreeComponent passiveComponent
    ) {
        // Clear existing canvas content
        commandBuilder.clear("#TreeCanvas");
        
        // Calculate which nodes are reachable (for highlighting)
        Set<String> reachableNodes = PassiveTreeGraph.getReachableUnallocatedNodes(tree, allocatedNodes);
        Set<String> startingNodes = tree.getStartingNodeIds();
        
        // First render connection lines (so nodes appear on top)
        renderConnections(commandBuilder, tree, allocatedNodes);
        
        // Render each node
        for (PassiveNode node : tree.getNodes().values()) {
            renderNode(commandBuilder, eventBuilder, node, tree, allocatedNodes, reachableNodes, startingNodes);
        }
    }
    
    /**
     * Render connection lines between nodes.
     */
    private void renderConnections(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes
    ) {
        for (PassiveConnection conn : tree.getConnections()) {
            PassiveNode fromNode = tree.getNode(conn.from());
            PassiveNode toNode = tree.getNode(conn.to());
            
            if (fromNode == null || toNode == null) continue;
            
            // Determine connection state
            boolean fromAllocated = allocatedNodes.contains(conn.from());
            boolean toAllocated = allocatedNodes.contains(conn.to());
            boolean isAllocated = fromAllocated && toAllocated;
            
            // Calculate positions
            int x1 = fromNode.position().x();
            int y1 = fromNode.position().y();
            int x2 = toNode.position().x();
            int y2 = toNode.position().y();
            
            // Connection styling based on state
            String color = isAllocated ? "#4CAF50" : 
                           (fromAllocated || toAllocated) ? "#607D8B" : "#37474F";
            String opacity = isAllocated ? "1.0" : "0.5";
            int lineThickness = isAllocated ? 3 : 2;
            
            // Render connection line via inline SVG-style path
            String connectionId = "conn_" + conn.from().hashCode() + "_" + conn.to().hashCode();
            String lineUI = String.format(
                "Panel #%s { Style: (Position: Absolute; Left: %d; Top: %d; Width: %d; Height: %d; " +
                "Background: %s; Opacity: %s; Transform: rotate(%ddeg)); }",
                connectionId,
                Math.min(x1, x2),
                Math.min(y1, y2),
                (int) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)),
                lineThickness,
                color,
                opacity,
                (int) Math.toDegrees(Math.atan2(y2 - y1, x2 - x1))
            );
            commandBuilder.appendInline("#TreeCanvas", lineUI);
        }
    }
    
    /**
     * Render a single node.
     */
    private void renderNode(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull PassiveNode node,
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull Set<String> reachableNodes,
            @Nonnull Set<String> startingNodes
    ) {
        String nodeId = node.id();
        boolean isAllocated = allocatedNodes.contains(nodeId);
        boolean isReachable = reachableNodes.contains(nodeId);
        boolean isStarting = startingNodes.contains(nodeId);
        boolean isInPath = highlightedPath.contains(nodeId);
        boolean isPathTarget = nodeId.equals(pathTargetNodeId);
        
        // Check if node matches search filter (or no filter active)
        boolean matchesSearch = currentSearchQuery == null || 
                                currentSearchQuery.isEmpty() || 
                                searchMatchNodes.contains(nodeId);
        
        // Apply search dimming - non-matching nodes are rendered with low opacity
        String searchOpacity = matchesSearch ? "1.0" : "0.3";
        
        // Node sizing based on type
        int size = getNodeSize(node.type());
        int halfSize = size / 2;
        
        // Calculate position (center the node)
        int left = node.position().x() - halfSize;
        int top = node.position().y() - halfSize;
        
        // Determine node colors based on state
        String bgColor;
        String borderColor;
        String glowColor = "transparent";
        
        if (isPathTarget) {
            // Path target gets special highlight
            bgColor = "rgba(255, 215, 0, 0.9)"; // Gold
            borderColor = "#FFD700";
            glowColor = "rgba(255, 215, 0, 0.8)";
        } else if (isInPath && !isAllocated) {
            // Path nodes get green highlight
            bgColor = "rgba(76, 175, 80, 0.7)";
            borderColor = "#4CAF50";
            glowColor = "rgba(76, 175, 80, 0.5)";
        } else if (isAllocated) {
            bgColor = getNodeColor(node.type());
            borderColor = "#FFFFFF";
            glowColor = getNodeColor(node.type());
        } else if (isReachable || isStarting) {
            bgColor = "rgba(60, 60, 70, 0.9)";
            borderColor = getNodeColor(node.type());
            glowColor = "rgba(255, 255, 255, 0.3)";
        } else {
            bgColor = "rgba(40, 40, 50, 0.7)";
            borderColor = "rgba(100, 100, 110, 0.5)";
        }
        
        // Build node UI element
        String shape = getNodeShape(node.type());
        String selector = sanitizeSelector(nodeId);
        
        String nodeUI = String.format(
            "Panel #%s { Style: (Position: Absolute; Left: %d; Top: %d; Width: %d; Height: %d; " +
            "Background: %s; Border: 2px solid %s; BorderRadius: %s; BoxShadow: 0 0 10px %s; " +
            "Cursor: pointer; Opacity: %s); DataEvent: { Action: \"%s\", NodeId: \"%s\" }; " +
            "Image #Icon { Source: \"%s\"; Style: (Alignment: Fill; Margin: 4); } }",
            selector,
            left,
            top,
            size,
            size,
            bgColor,
            borderColor,
            shape,
            glowColor,
            searchOpacity,
            isAllocated ? "refund" : "allocate",
            nodeId,
            node.icon() != null ? node.icon() : getDefaultIcon(node.type())
        );
        commandBuilder.appendInline("#TreeCanvas", nodeUI);
        
        // Add click event for allocation/refund
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#" + selector,
                EventData.of("Action", isAllocated ? "refund" : "allocate").append("NodeId", nodeId),
                false
        );
        
        // Add hover event for tooltip
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.MouseEntered,
                "#" + selector,
                EventData.of("Action", "showTooltip").append("NodeId", nodeId),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.MouseExited,
                "#" + selector,
                EventData.of("Action", "hideTooltip"),
                false
        );
    }
    
    /**
     * Get node size based on type.
     */
    private int getNodeSize(@Nonnull String type) {
        return switch (type.toLowerCase()) {
            case PassiveNodeType.MINOR -> 24;
            case PassiveNodeType.NOTABLE -> 32;
            case PassiveNodeType.KEYSTONE -> 48;
            case PassiveNodeType.MASTERY -> 36;
            case PassiveNodeType.UNLOCK -> 30;
            default -> 24;
        };
    }
    
    /**
     * Get node border radius based on type (for shape).
     */
    private String getNodeShape(@Nonnull String type) {
        return switch (type.toLowerCase()) {
            case PassiveNodeType.MINOR -> "50%"; // Circle
            case PassiveNodeType.NOTABLE -> "4px"; // Rounded square
            case PassiveNodeType.KEYSTONE -> "0"; // Sharp octagon-like
            case PassiveNodeType.MASTERY -> "50%"; // Circle with star overlay
            case PassiveNodeType.UNLOCK -> "50%"; // Lock shape
            default -> "50%";
        };
    }
    
    /**
     * Get node color based on type.
     */
    private String getNodeColor(@Nonnull String type) {
        return switch (type.toLowerCase()) {
            case PassiveNodeType.MINOR -> "#607D8B"; // Blue-grey
            case PassiveNodeType.NOTABLE -> "#FFD700"; // Gold
            case PassiveNodeType.KEYSTONE -> "#9C27B0"; // Purple
            case PassiveNodeType.MASTERY -> "#FF9800"; // Orange
            case PassiveNodeType.UNLOCK -> "#4CAF50"; // Green
            default -> "#607D8B";
        };
    }
    
    /**
     * Get default icon for node type.
     */
    private String getDefaultIcon(@Nonnull String type) {
        return switch (type.toLowerCase()) {
            case PassiveNodeType.MINOR -> "UI/Icons/PassiveNodes/minor.png";
            case PassiveNodeType.NOTABLE -> "UI/Icons/PassiveNodes/notable.png";
            case PassiveNodeType.KEYSTONE -> "UI/Icons/PassiveNodes/keystone.png";
            case PassiveNodeType.MASTERY -> "UI/Icons/PassiveNodes/mastery.png";
            case PassiveNodeType.UNLOCK -> "UI/Icons/PassiveNodes/unlock.png";
            default -> "UI/Icons/PassiveNodes/minor.png";
        };
    }
    
    /**
     * Sanitize node ID for use as CSS selector.
     */
    private String sanitizeSelector(@Nonnull String nodeId) {
        return "node_" + nodeId.replaceAll("[^a-zA-Z0-9]", "_");
    }
    
    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData eventData
    ) {
        String action = eventData.getAction();
        
        // Handle search query (no action, just query value)
        if (action == null && eventData.getQuery() != null) {
            handleSearch(ref, store, eventData.getQuery());
            return;
        }
        
        if (action == null) {
            return;
        }
        
        switch (action) {
            case "close" -> closePage(ref, store);
            case "allocate" -> handleAllocate(ref, store, eventData);
            case "refund" -> handleRefund(ref, store, eventData);
            case "respec" -> handleRespec(ref, store);
            case "undoLast" -> handleUndoLast(ref, store);
            case "selectGeneral" -> handleSelectTree(ref, store, null); // null = general tree
            case "selectClass" -> handleSelectClassTree(ref, store); // Opens class tree selection
            case "selectTreeById" -> handleSelectTreeById(ref, store, eventData);
            case "selectRegion" -> handleSelectRegion(ref, store, eventData);
            case "zoomIn" -> handleZoomIn();
            case "zoomOut" -> handleZoomOut();
            case "zoomReset" -> handleZoomReset();
            case "toggleComparison" -> handleToggleComparison(ref, store);
            case "clearSearch" -> handleClearSearch(ref, store);
            case "highlightPath" -> handleHighlightPath(ref, store, eventData);
            case "clearPathHighlight" -> handleClearPathHighlight();
            case "confirmCancel" -> handleConfirmCancel();
            case "confirmAccept" -> handleConfirmAccept(ref, store);
            case "showTooltip" -> handleShowTooltip(ref, store, eventData);
            case "hideTooltip" -> handleHideTooltip();
            default -> LOGGER.fine("Unknown action: " + action);
        }
    }
    
    private void handleSearch(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String query) {
        this.currentSearchQuery = query.trim().toLowerCase();
        this.searchMatchNodes.clear();
        
        if (currentSearchQuery.isEmpty()) {
            // Clear search - show all nodes normally
            LOGGER.fine("Search cleared");
        } else {
            // Find matching nodes
            String activeTreeId = getActiveTreeId();
            PassiveTree tree = PassiveTreeRegistry.get().getTree(activeTreeId);
            if (tree != null) {
                this.searchMatchNodes = tree.getNodes().values().stream()
                        .filter(node -> matchesSearch(node, currentSearchQuery))
                        .map(PassiveNode::id)
                        .collect(Collectors.toSet());
                LOGGER.fine("Search query '" + query + "' matched " + searchMatchNodes.size() + " nodes");
            }
        }
        
        // Rebuild to apply visual filter
        rebuild();
    }
    
    /**
     * Check if a node matches the search query.
     */
    private boolean matchesSearch(@Nonnull PassiveNode node, @Nonnull String query) {
        // Match against name
        if (node.name().toLowerCase().contains(query)) {
            return true;
        }
        
        // Match against description
        if (node.description() != null && node.description().toLowerCase().contains(query)) {
            return true;
        }
        
        // Match against effects (stat names, spell names, etc.)
        for (PassiveNodeEffect effect : node.effects()) {
            String effectType = effect.type();
            if (effectType.toLowerCase().contains(query)) {
                return true;
            }
            
            // Check effect data values
            for (Object value : effect.data().values()) {
                if (value != null && value.toString().toLowerCase().contains(query)) {
                    return true;
                }
            }
        }
        
        // Match against type
        if (node.type().toLowerCase().contains(query)) {
            return true;
        }
        
        // Match against region
        if (node.region() != null && node.region().toLowerCase().contains(query)) {
            return true;
        }
        
        return false;
    }
    
    private void handleClearSearch(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        this.currentSearchQuery = null;
        this.searchMatchNodes.clear();
        rebuild();
    }
    
    private void handleUndoLast(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // Note: Full undo functionality requires allocation history tracking in PassiveTreeComponent
        // For now, we provide feedback that the feature is pending component enhancement
        LOGGER.fine("Undo last requested - requires allocation history tracking (future enhancement)");
        
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#NotificationText.Text", "Undo requires allocation history (coming soon)");
        builder.set("#NotificationPanel.Visible", true);
        sendUpdate(builder, new UIEventBuilder(), false);
    }
    
    private void handleSelectTree(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nullable String treeType) {
        // Switch to general tree by recreating the page
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent != null) {
            PassiveTreePage newPage = new PassiveTreePage(this.playerRef, null); // null = general tree
            playerComponent.getPageManager().openCustomPage(ref, store, newPage);
            LOGGER.fine("Switched to general tree");
        }
    }
    
    private void handleSelectClassTree(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // Get available class trees for this player
        PassiveTreeComponent passiveComponent = getPassiveTreeComponent(ref, store);
        
        if (passiveComponent == null) {
            LOGGER.warning("Cannot show class tree selection - no PassiveTreeComponent");
            return;
        }
        
        // Show class tree selection overlay
        UICommandBuilder builder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        builder.set("#ClassTreeSelectionOverlay.Visible", true);
        builder.clear("#ClassTreeOptions");
        
        // Find all class trees the player has access to
        var allTrees = PassiveTreeRegistry.get().getAllTrees();
        int classTreeCount = 0;
        
        for (PassiveTree tree : allTrees) {
            if (!tree.isGeneralTree() && tree.getClassId() != null) {
                String classId = tree.getClassId();
                String treeId = tree.getId();
                String selector = "ClassTree_" + sanitizeSelector(treeId);
                String buttonUI = String.format(
                    "Button #%s { Text: \"%s\"; Style: (Width: 150; Height: 40; Margin: 5); }",
                    selector, classId
                );
                builder.appendInline("#ClassTreeOptions", buttonUI);
                
                // Add event binding for this class tree button
                eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#" + selector,
                        EventData.of("Action", "selectTreeById").append("NodeId", treeId),
                        false
                );
                classTreeCount++;
            }
        }
        
        if (classTreeCount == 0) {
            builder.appendInline("#ClassTreeOptions", 
                "Label { Text: \"No class trees available\"; Style: (FontSize: 14; Color: #999); }");
        }
        
        sendUpdate(builder, eventBuilder, false);
    }
    
    private void handleSelectTreeById(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData eventData) {
        String targetTreeId = eventData.getNodeId(); // Reusing nodeId field for tree ID
        if (targetTreeId == null || targetTreeId.isEmpty()) {
            return;
        }
        
        // Verify tree exists
        PassiveTree tree = PassiveTreeRegistry.get().getTree(targetTreeId);
        if (tree == null) {
            LOGGER.warning("Cannot switch to tree - not found: " + targetTreeId);
            return;
        }
        
        // Recreate page with new tree
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent != null) {
            PassiveTreePage newPage = new PassiveTreePage(this.playerRef, targetTreeId);
            playerComponent.getPageManager().openCustomPage(ref, store, newPage);
            LOGGER.fine("Switched to tree: " + targetTreeId);
        }
    }
    
    private void handleSelectRegion(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData eventData
    ) {
        String nodeId = eventData.getNodeId();
        if (nodeId == null || nodeId.isEmpty()) {
            return;
        }
        
        // Allocate the starting node - this sets the starting region
        String activeTreeId = getActiveTreeId();
        var result = PassiveTreeService.get().allocateNode(ref, activeTreeId, nodeId);
        
        if (result.success()) {
            LOGGER.fine("Selected starting region: " + nodeId);
            // Hide the region selection overlay and rebuild the page
            rebuild();
        } else {
            LOGGER.warning("Failed to select starting region " + nodeId + ": " + result.reason());
        }
    }
    
    private void handleZoomIn() {
        this.zoomLevel = Math.min(MAX_ZOOM, this.zoomLevel + ZOOM_STEP);
        applyZoomAndPan();
        LOGGER.fine("Zoom in: " + zoomLevel);
    }
    
    private void handleZoomOut() {
        this.zoomLevel = Math.max(MIN_ZOOM, this.zoomLevel - ZOOM_STEP);
        applyZoomAndPan();
        LOGGER.fine("Zoom out: " + zoomLevel);
    }
    
    private void handleZoomReset() {
        this.zoomLevel = 1.0f;
        this.panOffsetX = 0;
        this.panOffsetY = 0;
        applyZoomAndPan();
        LOGGER.fine("Zoom reset");
    }
    
    private void applyZoomAndPan() {
        UICommandBuilder builder = new UICommandBuilder();
        
        // Apply transform to tree canvas for zoom and pan
        String transform = String.format(
            "scale(%.2f) translate(%dpx, %dpx)", 
            zoomLevel, panOffsetX, panOffsetY
        );
        builder.set("#TreeCanvas.Style.Transform", transform);
        
        // Update zoom indicator
        builder.set("#ZoomLevel.Text", String.format("%.0f%%", zoomLevel * 100));
        
        sendUpdate(builder, new UIEventBuilder(), false);
    }
    
    private void handleToggleComparison(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        this.comparisonMode = !this.comparisonMode;
        
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#ComparisonToggle.Selected", this.comparisonMode);
        builder.set("#ComparisonPanel.Visible", this.comparisonMode);
        
        if (this.comparisonMode) {
            // Show comparison mode instructions
            builder.set("#ComparisonInstructions.Text", "Hover over a node to compare stat changes");
        }
        
        sendUpdate(builder, new UIEventBuilder(), false);
        LOGGER.fine("Comparison mode: " + (comparisonMode ? "enabled" : "disabled"));
    }
    
    private void handleHighlightPath(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData eventData) {
        String nodeId = eventData.getNodeId();
        if (nodeId == null || nodeId.isEmpty()) {
            return;
        }
        
        this.pathTargetNodeId = nodeId;
        
        // Calculate path to node
        String activeTreeId = getActiveTreeId();
        List<String> path = PassiveTreeService.get().findPathToNode(ref, activeTreeId, nodeId);
        
        this.highlightedPath.clear();
        if (path != null) {
            this.highlightedPath.addAll(path);
        }
        
        // Rebuild to show highlighted path
        rebuild();
    }
    
    private void handleClearPathHighlight() {
        this.pathTargetNodeId = null;
        this.highlightedPath.clear();
        rebuild();
    }
    
    private void handleConfirmCancel() {
        // Clear pending state
        this.pendingAction = null;
        this.pendingNodeId = null;
        
        // Hide confirmation dialog
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#ConfirmationOverlay.Visible", false);
        sendUpdate(builder, new UIEventBuilder(), false);
    }
    
    private void handleConfirmAccept(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // Hide the confirmation overlay first
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#ConfirmationOverlay.Visible", false);
        
        // Execute the pending action
        if (this.pendingAction != null) {
            switch (this.pendingAction) {
                case "respec" -> {
                    String treeIdToRespec = this.pendingNodeId != null ? this.pendingNodeId : getActiveTreeId();
                    var result = PassiveTreeService.get().refundAll(ref, treeIdToRespec);
                    if (result.success()) {
                        LOGGER.fine("Respec completed: " + result.pointsReturned() + " points returned");
                    } else {
                        LOGGER.warning("Respec failed: " + result.reason());
                    }
                }
                case "allocate-keystone" -> {
                    if (this.pendingNodeId != null) {
                        var result = PassiveTreeService.get().allocateNode(ref, getActiveTreeId(), this.pendingNodeId);
                        if (!result.success()) {
                            LOGGER.warning("Keystone allocation failed: " + result.reason());
                        }
                    }
                }
                default -> LOGGER.fine("Unknown pending action: " + this.pendingAction);
            }
            
            // Clear pending state
            this.pendingAction = null;
            this.pendingNodeId = null;
        }
        
        // Rebuild the page to reflect changes
        rebuild();
    }
    
    private void handleShowTooltip(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData eventData
    ) {
        String nodeId = eventData.getNodeId();
        if (nodeId == null || nodeId.isEmpty()) {
            return;
        }
        
        String activeTreeId = getActiveTreeId();
        PassiveTree tree = PassiveTreeRegistry.get().getTree(activeTreeId);
        if (tree == null) {
            return;
        }
        
        PassiveNode node = tree.getNode(nodeId);
        if (node == null) {
            return;
        }
        
        // Get allocation state
        PassiveTreeComponent passiveComponent = getPassiveTreeComponent(ref, store);
        Set<String> allocatedNodes = passiveComponent != null ? 
                (tree.isGeneralTree() ? passiveComponent.getGeneralAllocatedNodes() : 
                 passiveComponent.getClassAllocatedNodes(tree.getClassId())) : Set.of();
        boolean isAllocated = allocatedNodes.contains(nodeId);
        
        UICommandBuilder builder = new UICommandBuilder();
        
        // Show and populate tooltip
        builder.set("#TooltipOverlay.Visible", true);
        builder.set("#TooltipName.Text", node.name());
        builder.set("#TooltipType.Text", formatNodeType(node.type()));
        builder.set("#TooltipDescription.Text", node.description());
        
        // Populate effects list
        builder.clear("#EffectsList");
        for (PassiveNodeEffect effect : node.effects()) {
            String effectText = formatEffectText(effect);
            builder.appendInline("#EffectsList", 
                String.format("Label { Text: \"%s\"; Style: (FontSize: 11; Color: #CCCCCC); }", effectText));
        }
        
        // Show path cost or refund cost
        List<String> path = null;
        if (isAllocated) {
            // Show refund cost
            int refundCost = PassiveTreeService.get().calculateRefundCost(ref, nodeId);
            builder.set("#PathCostPanel.Visible", false);
            builder.set("#RefundCostPanel.Visible", true);
            builder.set("#RefundCostValue.Text", String.valueOf(refundCost));
        } else {
            // Show path cost and calculate path for highlighting
            path = PassiveTreeService.get().findPathToNode(ref, activeTreeId, nodeId);
            int pathCost = path != null ? path.size() : 0;
            builder.set("#RefundCostPanel.Visible", false);
            builder.set("#PathCostPanel.Visible", true);
            builder.set("#PathCostValue.Text", pathCost + " points");
        }
        
        // Update path highlighting for hover preview
        if (!isAllocated && path != null && !path.isEmpty()) {
            this.pathTargetNodeId = nodeId;
            this.highlightedPath.clear();
            this.highlightedPath.addAll(path);
        }
        
        // Comparison mode - show stat impact if enabled
        if (comparisonMode && !isAllocated) {
            builder.set("#ComparisonStatsPanel.Visible", true);
            builder.clear("#ComparisonStatsList");
            
            // List the stat effects this node would add
            for (PassiveNodeEffect effect : node.effects()) {
                if ("stat-modifier".equals(effect.type())) {
                    String stat = String.valueOf(effect.data().getOrDefault("Stat", effect.data().getOrDefault("stat", "")));
                    String value = String.valueOf(effect.data().getOrDefault("Value", effect.data().getOrDefault("value", "0")));
                    String modifier = String.valueOf(effect.data().getOrDefault("Modifier", effect.data().getOrDefault("modifier", "flat")));
                    
                    String prefix = value.startsWith("-") ? "" : "+";
                    String suffix = "percent".equalsIgnoreCase(modifier) ? "%" : "";
                    String color = value.startsWith("-") ? "#D32F2F" : "#4CAF50";
                    
                    String statLabel = String.format(
                        "Panel { Style: (Direction: Row; Gap: 5); " +
                        "Label { Text: \"%s\"; Style: (FontSize: 11; Color: %s); } " +
                        "Label { Text: \"%s\"; Style: (FontSize: 11; Color: #AAAAAA); } }",
                        prefix + value + suffix, color, formatStatName(stat)
                    );
                    builder.appendInline("#ComparisonStatsList", statLabel);
                }
            }
        } else {
            builder.set("#ComparisonStatsPanel.Visible", false);
        }
        
        // Set icon if available
        if (node.icon() != null) {
            builder.set("#TooltipIcon.Source", node.icon());
        } else {
            builder.set("#TooltipIcon.Source", getDefaultIcon(node.type()));
        }
        
        sendUpdate(builder, new UIEventBuilder(), false);
    }
    
    private void handleHideTooltip() {
        // Clear path highlighting when hover ends
        if (pathTargetNodeId != null) {
            this.pathTargetNodeId = null;
            this.highlightedPath.clear();
        }
        
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#TooltipOverlay.Visible", false);
        sendUpdate(builder, new UIEventBuilder(), false);
    }
    
    private String formatNodeType(@Nonnull String type) {
        return switch (type.toLowerCase()) {
            case PassiveNodeType.MINOR -> "Minor Passive";
            case PassiveNodeType.NOTABLE -> "Notable Passive";
            case PassiveNodeType.KEYSTONE -> "Keystone";
            case PassiveNodeType.MASTERY -> "Mastery";
            case PassiveNodeType.UNLOCK -> "Unlock";
            default -> "Passive";
        };
    }
    
    private String formatEffectText(@Nonnull PassiveNodeEffect effect) {
        String type = effect.type();
        var data = effect.data();
        
        return switch (type) {
            case "stat-modifier" -> {
                String stat = String.valueOf(data.getOrDefault("Stat", data.getOrDefault("stat", "unknown")));
                String value = String.valueOf(data.getOrDefault("Value", data.getOrDefault("value", "0")));
                String modifier = String.valueOf(data.getOrDefault("Modifier", data.getOrDefault("modifier", "flat")));
                String prefix = value.startsWith("-") ? "" : "+";
                String suffix = "percent".equalsIgnoreCase(modifier) ? "%" : "";
                yield prefix + value + suffix + " " + formatStatName(stat);
            }
            case "spell-grant" -> "Grants: " + String.valueOf(data.getOrDefault("SpellId", data.getOrDefault("spell", "Unknown Spell")));
            case "unlock-flag" -> "Unlocks: " + String.valueOf(data.getOrDefault("Description", data.getOrDefault("FlagId", data.getOrDefault("flag", "Unknown"))));
            case "mastery-choice" -> "Choice: Select one mastery option";
            default -> type + ": " + data.toString();
        };
    }
    
    private String formatStatName(@Nonnull String stat) {
        // Convert stat IDs like "physical_damage" to "Physical Damage"
        return stat.replace("_", " ")
                   .replace("-", " ")
                   .replaceAll("([a-z])([A-Z])", "$1 $2")
                   .replace("  ", " ")
                   .trim();
    }
    
    private void closePage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent != null) {
            playerComponent.getPageManager().setPage(ref, store, Page.None);
        }
    }
    
    private void handleAllocate(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData eventData
    ) {
        String nodeId = eventData.getNodeId();
        if (nodeId == null || nodeId.isEmpty()) {
            return;
        }
        
        String activeTreeId = getActiveTreeId();
        
        // Attempt allocation
        var result = PassiveTreeService.get().allocateNode(ref, activeTreeId, nodeId);
        
        if (result.success()) {
            // Refresh page to show updated state
            rebuild();
        } else {
            LOGGER.fine("Allocation failed for node " + nodeId + ": " + result.reason());
        }
    }
    
    private void handleRefund(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData eventData
    ) {
        String nodeId = eventData.getNodeId();
        if (nodeId == null || nodeId.isEmpty()) {
            return;
        }
        
        String activeTreeId = getActiveTreeId();
        
        // Attempt refund
        var result = PassiveTreeService.get().refundNode(ref, activeTreeId, nodeId);
        
        if (result.success()) {
            rebuild();
        } else {
            LOGGER.fine("Refund failed for node " + nodeId + ": " + result.reason());
        }
    }
    
    private void handleRespec(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store
    ) {
        String activeTreeId = getActiveTreeId();
        PassiveTree tree = PassiveTreeRegistry.get().getTree(activeTreeId);
        PassiveTreeComponent passiveComponent = getPassiveTreeComponent(ref, store);
        
        if (tree == null || passiveComponent == null) {
            return;
        }
        
        // Calculate respec cost
        Set<String> allocatedNodes = tree.isGeneralTree() ? 
                passiveComponent.getGeneralAllocatedNodes() :
                passiveComponent.getClassAllocatedNodes(tree.getClassId());
        
        int nodeCount = allocatedNodes.size();
        if (nodeCount == 0) {
            LOGGER.fine("No nodes to refund");
            return;
        }
        
        int totalCost = PassiveTreeService.get().calculateTotalRefundCost(ref, allocatedNodes);
        
        // Store pending action
        this.pendingAction = "respec";
        this.pendingNodeId = activeTreeId;
        
        // Show confirmation dialog
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#ConfirmationOverlay.Visible", true);
        builder.set("#ConfirmTitle.Text", "Full Respec");
        builder.set("#ConfirmMessage.Text", "Are you sure you want to refund all " + nodeCount + " allocated nodes?");
        builder.set("#CostSummary.Visible", true);
        builder.set("#NodeCountValue.Text", String.valueOf(nodeCount));
        builder.set("#TotalCostValue.Text", String.valueOf(totalCost));
        
        // TODO: Get actual tradebar balance from currency system
        int balance = 10000; // Placeholder
        builder.set("#BalanceValue.Text", String.valueOf(balance));
        builder.set("#BalanceValue.Color", balance >= totalCost ? "#4CAF50" : "#D32F2F");
        
        // Hide keystone preview (not relevant for respec)
        builder.set("#KeystonePreview.Visible", false);
        
        sendUpdate(builder, new UIEventBuilder(), false);
    }
    
    /**
     * Get the active tree ID being displayed.
     */
    @Nonnull
    private String getActiveTreeId() {
        if (this.treeId != null) {
            return this.treeId;
        }
        PassiveTree generalTree = PassiveTreeRegistry.get().getGeneralTree();
        return generalTree != null ? generalTree.getId() : "general";
    }
    
    /**
     * Get the PassiveTreeComponent for an entity.
     */
    @Nullable
    private PassiveTreeComponent getPassiveTreeComponent(Ref<EntityStore> ref, Store<EntityStore> store) {
        var componentType = HyforgedPlugin.getInstance().getPassiveTreeComponentType();
        if (componentType == null) {
            return null;
        }
        return store.getComponent(ref, componentType);
    }
    
    // ========== PAGE EVENT DATA ==========
    
    /**
     * Event data sent from the client UI.
     */
    public static class PageEventData {
        public static final BuilderCodec<PageEventData> CODEC = BuilderCodec.builder(
                        PageEventData.class, PageEventData::new
                )
                .append(new KeyedCodec<>("Action", Codec.STRING, true), (e, s) -> e.action = s, e -> e.action)
                .add()
                .append(new KeyedCodec<>("NodeId", Codec.STRING, true), (e, s) -> e.nodeId = s, e -> e.nodeId)
                .add()
                .append(new KeyedCodec<>("@Query", Codec.STRING, true), (e, s) -> e.query = s, e -> e.query)
                .add()
                .build();
        
        private String action;
        private String nodeId;
        private String query;
        
        public PageEventData() {}
        
        public String getAction() {
            return action;
        }
        
        public String getNodeId() {
            return nodeId;
        }
        
        public String getQuery() {
            return query;
        }
    }
}
