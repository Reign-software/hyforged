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
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
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
    private static final String PAGE_UI_FILE = "Hyforged/PassiveTreePage.ui";
    
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
    
    /** Maps UI button index to node ID for click handling */
    @Nonnull
    private final String[] displayedNodeIds = new String[50]; // Increased for visual tree
    
    /** Current zoom level (1.0 = 100%) */
    private float zoomLevel = 1.0f;
    
    /** Minimum zoom level */
    private static final float MIN_ZOOM = 0.25f;
    
    /** Maximum zoom level */
    private static final float MAX_ZOOM = 2.0f;
    
    /** Zoom step per action */
    private static final float ZOOM_STEP = 0.25f;
    
    /** Pan offset X (in tree coordinate units) */
    private int panOffsetX = 200;
    
    /** Pan offset Y (in tree coordinate units) */
    private int panOffsetY = 400;
    
    /** Pan step per button click */
    private static final int PAN_STEP = 100;
    
    /** Sidebar width in pixels */
    private static final int SIDEBAR_WIDTH = 220;
    
    /** Viewport width in pixels (screen width minus sidebar) */
    private static final int VIEWPORT_WIDTH = 1280 - SIDEBAR_WIDTH;
    
    /** Viewport height in pixels (screen height minus bottom bar) */
    private static final int VIEWPORT_HEIGHT = 720 - 32;
    
    /** Scale factor from tree coords to screen pixels */
    private static final float COORD_SCALE = 1.5f;
    
    /** Node counter for dynamic node IDs */
    private int dynamicNodeCounter = 0;
    
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
        
        // Add pan navigation events
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#PanUp",
                EventData.of("Action", "panUp"),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#PanDown",
                EventData.of("Action", "panDown"),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#PanLeft",
                EventData.of("Action", "panLeft"),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#PanRight",
                EventData.of("Action", "panRight"),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CenterOnStart",
                EventData.of("Action", "centerOnStart"),
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
        // Set points display
        String pointsText = String.format("%d / %d", allocatedCount, maxPoints);
        commandBuilder.set("#PointsDisplay.Text", pointsText);
        
        // Set ascendancy points (if applicable)
        String ascText = isGeneralTree ? 
                String.format("From books: %d", bookPoints) :
                String.format("Ascendancy: %d/8", allocatedCount);
        commandBuilder.set("#AscendancyPointsLabel.Text", ascText);
        
        // Note: Tab selection styling would require dynamic style changes
        // For now, just rely on the tree name/type labels to show which is active
    }
    
    /** Region button template path */
    private static final String REGION_BUTTON_TEMPLATE = "Hyforged/RegionButton.ui";
    
    /** Counter for unique region entry IDs */
    private int regionEntryCounter = 0;
    
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
        
        // Reset counter
        regionEntryCounter = 0;
        
        Set<String> startingNodeIds = tree.getStartingNodeIds();
        for (String startingNodeId : startingNodeIds) {
            PassiveNode node = tree.getNode(startingNodeId);
            if (node == null) continue;
            
            // Get region name from the node's region field or use node name
            String regionName = node.region() != null ? node.region() : node.name();
            
            // Use template approach - create wrapper group first
            String entryId = "RegionEntry" + (regionEntryCounter++);
            String inlineGroup = String.format("Group #%s { Padding: (Right: 10); }", entryId);
            commandBuilder.appendInline("#RegionOptions", inlineGroup);
            
            // Append the template inside the group
            commandBuilder.append("#" + entryId, REGION_BUTTON_TEMPLATE);
            
            // Configure the template elements
            String selector = "#" + entryId + " #RegionButton";
            commandBuilder.set(selector + " #RegionName.Text", regionName);
            commandBuilder.set(selector + " #RegionDesc.Text", "Click to select");
            
            // Add event binding for region selection
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector,
                    EventData.of("Action", "selectRegion").append("NodeId", startingNodeId),
                    false
            );
        }
    }
    
    /**
     * Render the tree nodes and connections on the canvas.
     * Creates a visual tree with positioned nodes based on their coordinates.
     */
    private void renderTreeCanvas(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nullable PassiveTreeComponent passiveComponent
    ) {
        // Get starting nodes for the tree
        Set<String> startingNodes = tree.getStartingNodeIds();
        
        // Calculate reachable nodes (nodes adjacent to allocated ones)
        Set<String> reachableNodes = new HashSet<>();
        for (String allocatedNodeId : allocatedNodes) {
            reachableNodes.addAll(tree.getAdjacentNodes(allocatedNodeId));
        }
        reachableNodes.addAll(startingNodes);
        reachableNodes.removeAll(allocatedNodes);
        
        // Clear displayed node IDs tracking and dynamic canvas
        java.util.Arrays.fill(displayedNodeIds, null);
        commandBuilder.clear("#TreeCanvas");
        commandBuilder.clear("#ConnectionsCanvas");
        dynamicNodeCounter = 0;
        
        // Calculate viewport bounds (in tree coordinates)
        int viewportTreeWidth = (int) (VIEWPORT_WIDTH / COORD_SCALE / zoomLevel);
        int viewportTreeHeight = (int) (VIEWPORT_HEIGHT / COORD_SCALE / zoomLevel);
        int viewMinX = panOffsetX - viewportTreeWidth / 2;
        int viewMaxX = panOffsetX + viewportTreeWidth / 2;
        int viewMinY = panOffsetY - viewportTreeHeight / 2;
        int viewMaxY = panOffsetY + viewportTreeHeight / 2;
        
        // Update viewport info
        commandBuilder.set("#ViewportInfo.Text", String.format("View: %d,%d Zoom: %.0f%%", panOffsetX, panOffsetY, zoomLevel * 100));
        
        // Render connection lines between nodes first (so they appear behind nodes)
        renderConnectionLines(commandBuilder, tree, allocatedNodes, startingNodes, reachableNodes,
                viewMinX, viewMaxX, viewMinY, viewMaxY);
        
        int totalNodes = tree.getNodes().size();
        int renderedCount = 0;
        int maxVisibleNodes = 50; // Limit for performance
        
        // Render nodes that are within the viewport
        for (PassiveNode node : tree.getNodes().values()) {
            if (renderedCount >= maxVisibleNodes) break;
            
            // Apply search filter if active
            if (!searchMatchNodes.isEmpty() && !searchMatchNodes.contains(node.id())) {
                continue;
            }
            
            int nodeX = node.position().x();
            int nodeY = node.position().y();
            
            // Check if node is in viewport (with some padding for node size)
            int nodePadding = 30;
            if (nodeX < viewMinX - nodePadding || nodeX > viewMaxX + nodePadding ||
                nodeY < viewMinY - nodePadding || nodeY > viewMaxY + nodePadding) {
                continue; // Node is outside viewport
            }
            
            // Calculate screen position relative to viewport
            int screenX = (int) ((nodeX - viewMinX) * COORD_SCALE * zoomLevel);
            int screenY = (int) ((nodeY - viewMinY) * COORD_SCALE * zoomLevel);
            
            // Clamp to viewport bounds
            screenX = Math.max(0, Math.min(VIEWPORT_WIDTH - 30, screenX));
            screenY = Math.max(0, Math.min(VIEWPORT_HEIGHT - 30, screenY));
            
            String nodeId = node.id();
            boolean isAllocated = allocatedNodes.contains(nodeId);
            boolean isReachable = reachableNodes.contains(nodeId);
            boolean isStarting = startingNodes.contains(nodeId);
            
            // Store node ID for click handler lookup
            if (renderedCount < displayedNodeIds.length) {
                displayedNodeIds[renderedCount] = nodeId;
            }
            
            // Determine node size based on type
            int nodeSize = getNodeSize(node.type());
            
            // Determine background image based on node type and state
            String bgImage = getNodeBackgroundImage(node.type(), isAllocated, isReachable || isStarting);
            
            // Create a positioned node button using inline UI with image background
            String nodeElementId = "VisNode" + dynamicNodeCounter++;
            
            // Inline UI for a positioned node button with image background
            // Note: Cannot use variable references like $Common.@ButtonSounds in appendInline
            String nodeUI = String.format(
                "Button #%s { Anchor: (Left: %d, Top: %d, Width: %d, Height: %d); " +
                "Background: PatchStyle(TexturePath: \"%s\"); }",
                nodeElementId, screenX, screenY, nodeSize, nodeSize,
                bgImage
            );
            
            commandBuilder.appendInline("#TreeCanvas", nodeUI);
            
            // Add event binding for clicking this node - directly allocate or refund
            String clickAction = isAllocated ? "refund" : "allocate";
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#" + nodeElementId,
                    EventData.of("Action", clickAction).append("NodeId", nodeId),
                    false
            );
            
            // Add hover events for tooltip
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.MouseEntered,
                    "#" + nodeElementId,
                    EventData.of("Action", "showTooltip").append("NodeId", nodeId),
                    false
            );
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.MouseExited,
                    "#" + nodeElementId,
                    EventData.of("Action", "hideTooltip"),
                    false
            );
            
            renderedCount++;
        }
        
        // Update viewport info with node count
        String countText = String.format("%d,%d | %d nodes | %.0f%%", panOffsetX, panOffsetY, renderedCount, zoomLevel * 100);
        if (!searchMatchNodes.isEmpty()) {
            countText += String.format(" | %d match", searchMatchNodes.size());
        }
        commandBuilder.set("#ViewportInfo.Text", countText);
        
        LOGGER.info("Rendered " + renderedCount + " nodes in viewport from tree with " + totalNodes + " total nodes");
    }
    
    /**
     * Get the background image path for a node based on its type and state.
     */
    private String getNodeBackgroundImage(@Nonnull String type, boolean allocated, boolean canAllocate) {
        String prefix = switch (type.toLowerCase()) {
            case PassiveNodeType.KEYSTONE -> "Hyforged/Textures/KeystoneFrame";
            case PassiveNodeType.NOTABLE -> "Hyforged/Textures/NotableFrame";
            case PassiveNodeType.MASTERY -> "Hyforged/Textures/PassiveSkillScreenAscendancyFrameLarge";
            case PassiveNodeType.UNLOCK -> "Hyforged/Textures/PassiveSkillScreenAscendancyFrameLarge";
            case PassiveNodeType.MINOR -> "Hyforged/Textures/PassiveSkillScreenAscendancyFrameSmall";
            default -> "Hyforged/Textures/PassiveSkillScreenAscendancyFrameSmall";
        };
        
        String suffix = allocated ? "Allocated" : (canAllocate ? "CanAllocate" : "Normal");
        // Minor nodes use "Normal" instead of "Unallocated"
        if (!allocated && !canAllocate && (type.equalsIgnoreCase(PassiveNodeType.KEYSTONE) || 
                type.equalsIgnoreCase(PassiveNodeType.NOTABLE))) {
            suffix = "Unallocated";
        }
        
        return prefix + suffix + ".png";
    }
    
    /**
     * Render connection lines between nodes.
     * Lines are rendered as narrow Group elements with a background color.
     */
    private void renderConnectionLines(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull Set<String> startingNodes,
            @Nonnull Set<String> reachableNodes,
            int viewMinX, int viewMaxX, int viewMinY, int viewMaxY
    ) {
        int lineCounter = 0;
        int maxLines = 100; // Limit for performance
        
        // Track rendered connections to avoid duplicates
        Set<String> renderedConnections = new HashSet<>();
        
        for (PassiveNode node : tree.getNodes().values()) {
            if (lineCounter >= maxLines) break;
            
            int nodeX = node.position().x();
            int nodeY = node.position().y();
            
            // Skip if node is far outside viewport
            int padding = 100;
            if (nodeX < viewMinX - padding || nodeX > viewMaxX + padding ||
                nodeY < viewMinY - padding || nodeY > viewMaxY + padding) {
                continue;
            }
            
            // Get adjacent nodes for this node from the tree (not from the node itself)
            Set<String> adjacentNodes = tree.getAdjacentNodes(node.id());
            if (adjacentNodes.isEmpty()) continue;
            
            for (String targetId : adjacentNodes) {
                // Create unique connection key to avoid duplicates
                String connKey = node.id().compareTo(targetId) < 0 ? 
                        node.id() + "-" + targetId : targetId + "-" + node.id();
                if (renderedConnections.contains(connKey)) continue;
                renderedConnections.add(connKey);
                
                PassiveNode targetNode = tree.getNode(targetId);
                if (targetNode == null) continue;
                
                int targetX = targetNode.position().x();
                int targetY = targetNode.position().y();
                
                // Calculate screen positions
                int screenX1 = (int) ((nodeX - viewMinX) * COORD_SCALE * zoomLevel);
                int screenY1 = (int) ((nodeY - viewMinY) * COORD_SCALE * zoomLevel);
                int screenX2 = (int) ((targetX - viewMinX) * COORD_SCALE * zoomLevel);
                int screenY2 = (int) ((targetY - viewMinY) * COORD_SCALE * zoomLevel);
                
                // Determine line color based on allocation state
                boolean nodeAllocated = allocatedNodes.contains(node.id());
                boolean targetAllocated = allocatedNodes.contains(targetId);
                boolean bothAllocated = nodeAllocated && targetAllocated;
                boolean eitherReachable = reachableNodes.contains(node.id()) || 
                        reachableNodes.contains(targetId) ||
                        startingNodes.contains(node.id()) || 
                        startingNodes.contains(targetId);
                
                String lineColor;
                if (bothAllocated) {
                    lineColor = "#4CAF50"; // Green - allocated path
                } else if (nodeAllocated || targetAllocated || eitherReachable) {
                    lineColor = "#806030"; // Gold/amber - available path
                } else {
                    lineColor = "#404050"; // Dark gray - locked path
                }
                
                // Render line as multiple segments (horizontal then vertical for L-shape)
                // Simple approach: create a thin rectangle
                String lineId = "Line" + (lineCounter++);
                
                // Offset to center of nodes
                int nodeSize1 = getNodeSize(node.type()) / 2;
                int nodeSize2 = getNodeSize(targetNode.type()) / 2;
                int centerX1 = screenX1 + nodeSize1;
                int centerY1 = screenY1 + nodeSize1;
                int centerX2 = screenX2 + nodeSize2;
                int centerY2 = screenY2 + nodeSize2;
                
                // Render as two segments (L-shape): horizontal then vertical
                int thickness = (int) Math.max(2, 3 * zoomLevel);
                
                // Horizontal segment
                if (Math.abs(centerX2 - centerX1) > 5) {
                    int hMinX = Math.min(centerX1, centerX2);
                    int hWidth = Math.abs(centerX2 - centerX1);
                    String hLineUI = String.format(
                        "Group #%sH { Anchor: (Left: %d, Top: %d, Width: %d, Height: %d); Background: PatchStyle(Color: %s); }",
                        lineId, hMinX, centerY1 - thickness/2, hWidth, thickness, lineColor
                    );
                    commandBuilder.appendInline("#ConnectionsCanvas", hLineUI);
                }
                
                // Vertical segment
                if (Math.abs(centerY2 - centerY1) > 5) {
                    int vMinY = Math.min(centerY1, centerY2);
                    int vHeight = Math.abs(centerY2 - centerY1);
                    String vLineUI = String.format(
                        "Group #%sV { Anchor: (Left: %d, Top: %d, Width: %d, Height: %d); Background: PatchStyle(Color: %s); }",
                        lineId, centerX2 - thickness/2, vMinY, thickness, vHeight, lineColor
                    );
                    commandBuilder.appendInline("#ConnectionsCanvas", vLineUI);
                }
            }
        }
        
        LOGGER.fine("Rendered " + lineCounter + " connection lines");
    }

    @Nonnull
    private Value<?> getNodeFrameRef(@Nonnull String type, boolean allocated, boolean canAllocate) {
        String stateSuffix = allocated ? "Allocated" : (canAllocate ? "CanAllocate" : "Unallocated");
        String prefix = switch (type.toLowerCase()) {
            case PassiveNodeType.KEYSTONE -> "FrameKeystone";
            case PassiveNodeType.NOTABLE -> "FrameNotable";
            case PassiveNodeType.MASTERY -> "FrameMastery";
            case PassiveNodeType.UNLOCK -> "FrameUnlock";
            case PassiveNodeType.MINOR -> "FrameMinor";
            default -> "FrameMinor";
        };
        return Value.ref(PAGE_UI_FILE, prefix + stateSuffix);
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
            case "panUp" -> handlePan(0, -PAN_STEP);
            case "panDown" -> handlePan(0, PAN_STEP);
            case "panLeft" -> handlePan(-PAN_STEP, 0);
            case "panRight" -> handlePan(PAN_STEP, 0);
            case "centerOnStart" -> handleCenterOnStart(ref, store);
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
                "Label { Text: \"No class trees available\"; Style: (FontSize: 14); }");
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
        this.panOffsetX = 200;
        this.panOffsetY = 400;
        rebuild();
        LOGGER.fine("Zoom reset");
    }
    
    /**
     * Handle pan navigation.
     */
    private void handlePan(int deltaX, int deltaY) {
        this.panOffsetX += deltaX;
        this.panOffsetY += deltaY;
        
        // Clamp to reasonable bounds (tree coords are roughly 0-500 X, 0-2000 Y based on layout files)
        this.panOffsetX = Math.max(0, Math.min(500, this.panOffsetX));
        this.panOffsetY = Math.max(0, Math.min(2000, this.panOffsetY));
        
        rebuild();
        LOGGER.fine("Pan to: " + panOffsetX + ", " + panOffsetY);
    }
    
    /**
     * Center the viewport on a starting node.
     */
    private void handleCenterOnStart(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        String activeTreeId = getActiveTreeId();
        PassiveTree tree = PassiveTreeRegistry.get().getTree(activeTreeId);
        if (tree == null) {
            return;
        }
        
        // Find a starting node and center on it
        Set<String> startingIds = tree.getStartingNodeIds();
        if (!startingIds.isEmpty()) {
            String firstStartId = startingIds.iterator().next();
            PassiveNode startNode = tree.getNode(firstStartId);
            if (startNode != null) {
                // Center viewport on this node
                this.panOffsetX = startNode.position().x();
                this.panOffsetY = startNode.position().y();
                rebuild();
                LOGGER.fine("Centered on start node: " + firstStartId + " at " + panOffsetX + ", " + panOffsetY);
            }
        }
    }
    
    private void applyZoomAndPan() {
        UICommandBuilder builder = new UICommandBuilder();
        
        // Update viewport info and zoom level indicator
        builder.set("#ZoomLevel.Text", String.format("%.0f%%", zoomLevel * 100));
        builder.set("#ViewportInfo.Text", String.format("View: %d,%d", panOffsetX, panOffsetY));
        
        sendUpdate(builder, new UIEventBuilder(), false);
    }
    
    private void handleToggleComparison(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        this.comparisonMode = !this.comparisonMode;
        
        UICommandBuilder builder = new UICommandBuilder();
        // Note: Button doesn't have a .Selected property, use visibility for comparison panel only
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
        
        // Calculate reachable nodes
        Set<String> reachableNodes = new HashSet<>();
        for (String allocatedNodeId : allocatedNodes) {
            reachableNodes.addAll(tree.getAdjacentNodes(allocatedNodeId));
        }
        reachableNodes.addAll(tree.getStartingNodeIds());
        reachableNodes.removeAll(allocatedNodes);
        boolean canAllocate = reachableNodes.contains(nodeId) || tree.getStartingNodeIds().contains(nodeId);
        
        UICommandBuilder builder = new UICommandBuilder();
        
        // Show and populate tooltip
        builder.set("#TooltipOverlay.Visible", true);
        builder.set("#TooltipName.Text", node.name());
        builder.set("#TooltipType.Text", formatNodeType(node.type()));
        
        // Show node status
        String status;
        if (isAllocated) {
            status = "ALLOCATED";
        } else if (canAllocate) {
            status = "AVAILABLE";
        } else {
            status = "LOCKED";
        }
        builder.set("#TooltipStatus.Text", status);
        
        builder.set("#TooltipDescription.Text", node.description() != null ? node.description() : "");
        
        // Populate effects list - build as text instead of appendInline
        StringBuilder effectsText = new StringBuilder();
        for (PassiveNodeEffect effect : node.effects()) {
            String effectText = formatEffectText(effect);
            if (effectsText.length() > 0) {
                effectsText.append("\n");
            }
            effectsText.append("• ").append(effectText);
        }
        builder.set("#TooltipEffects.Text", effectsText.toString());
        
        // Set action hint based on node state
        String actionHint;
        if (isAllocated) {
            actionHint = "Click to refund this node";
        } else if (canAllocate) {
            actionHint = "Click to allocate this node";
        } else {
            actionHint = "Allocate connected nodes first";
        }
        builder.set("#TooltipActionHint.Text", actionHint);
        
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
            
            // Build comparison stats as text
            StringBuilder comparisonText = new StringBuilder();
            for (PassiveNodeEffect effect : node.effects()) {
                if ("stat-modifier".equals(effect.type())) {
                    String stat = String.valueOf(effect.data().getOrDefault("Stat", effect.data().getOrDefault("stat", "")));
                    String value = String.valueOf(effect.data().getOrDefault("Value", effect.data().getOrDefault("value", "0")));
                    String modifier = String.valueOf(effect.data().getOrDefault("Modifier", effect.data().getOrDefault("modifier", "flat")));
                    
                    String prefix = value.startsWith("-") ? "" : "+";
                    String suffix = "percent".equalsIgnoreCase(modifier) ? "%" : "";
                    
                    if (comparisonText.length() > 0) {
                        comparisonText.append("\n");
                    }
                    comparisonText.append(prefix).append(value).append(suffix).append(" ").append(formatStatName(stat));
                }
            }
            builder.set("#ComparisonStatsText.Text", comparisonText.toString());
        } else {
            builder.set("#ComparisonStatsPanel.Visible", false);
        }
        
        // Set icon background if available (using PatchStyle format)
        if (node.icon() != null) {
            builder.set("#TooltipIcon.Background", "PatchStyle(TexturePath: \"" + node.icon() + "\")");
        } else {
            builder.set("#TooltipIcon.Background", "PatchStyle(TexturePath: \"" + getDefaultIcon(node.type()) + "\")");
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
                .append(new KeyedCodec<>("Action", Codec.STRING), (e, s) -> e.action = s, e -> e.action)
                .add()
                .append(new KeyedCodec<>("NodeId", Codec.STRING), (e, s) -> e.nodeId = s, e -> e.nodeId)
                .add()
                .append(new KeyedCodec<>("@Query", Codec.STRING), (e, s) -> e.query = s, e -> e.query)
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
