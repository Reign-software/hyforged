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
import reign.software.hyforged.passive.model.PassiveNode;
import reign.software.hyforged.passive.model.PassiveNodeEffect;
import reign.software.hyforged.passive.model.PassiveNodeType;
import reign.software.hyforged.passive.model.PassiveTree;
import reign.software.hyforged.passive.registry.NodeVisualTemplateRegistry;
import reign.software.hyforged.passive.registry.PassiveTreeRegistry;
import reign.software.hyforged.passive.service.PassiveTreeService;
import reign.software.hyforged.passive.model.NodeVisualTemplate;
import reign.software.hyforged.passive.model.TextLabel;
import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Passive Tree UI page using native Hytale UI.
 * <p>
 * Features:
 * <ul>
 *   <li>PoB-style layout with left sidebar and fullscreen tree canvas</li>
 *   <li>Native scrolling via TopScrolling layout mode</li>
 *   <li>Hover tooltips, allocation/refund on click</li>
 *   <li>Search functionality</li>
 * </ul>
 * <p>
 * Uses PassiveTreePage.ui for the base layout and dynamically renders
 * nodes and connections via UICommandBuilder.appendInline().
 */
public class PassiveTreePage extends InteractiveCustomUIPage<PassiveTreePage.PageEventData> {
    
    private static final Logger LOGGER = Logger.getLogger(PassiveTreePage.class.getName());
    
    /** UI file path for the passive tree page layout */
    private static final String PAGE_UI_FILE = "Hyforged/PassiveTreePage.ui";
    
    /** Default icon for nodes without an explicit icon */
    private static final String DEFAULT_ICON = "Hyforged/Textures/Passive.png";
    
    // Layout constants matching HyUI version
    private static final float COORD_SCALE = 1.5f;  // Scale tree coords to pixels
    private static final int VIEWPORT_WIDTH = 900;  // Fixed tree area width in pixels
    
    /** The tree being viewed (null for general tree) */
    @Nullable
    private final String treeId;
    
    /** Current search query for filtering nodes */
    @Nullable
    private String currentSearchQuery;
    
    /** Set of node IDs matching the current search filter */
    @Nonnull
    private Set<String> searchMatchNodes = new HashSet<>();
    
    /** Tracks rendered node IDs to element IDs for event binding */
    @Nonnull
    private Map<String, String> renderedNodeElementIds = new LinkedHashMap<>();
    
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
        
        // Get passive data
        PassiveTreeComponent component = getPassiveComponent(ref, store);
        String activeTreeId = getActiveTreeId();
        PassiveTree tree = PassiveTreeRegistry.get().getTree(activeTreeId);
        
        if (tree == null) {
            LOGGER.warning("Cannot show passive tree page - tree not found: " + activeTreeId);
            return;
        }
        
        // Gather allocation data
        Set<String> allocatedNodes = getAllocatedNodes(component, tree);
        Set<String> reachableNodes = getReachableNodes(tree, allocatedNodes);
        
        int availablePoints = PassiveTreeService.get().getAvailablePoints(ref, activeTreeId);
        int allocatedCount = allocatedNodes.size();
        int maxPoints = availablePoints + allocatedCount;
        
        // Populate sidebar
        populateSidebar(commandBuilder, availablePoints, allocatedCount, maxPoints);
        
        // Check if starting region selection is needed
        boolean isGeneralTree = tree.isGeneralTree();
        boolean needsStartingRegion = isGeneralTree 
                && component != null 
                && component.getGeneralStartingNode() == null
                && allocatedNodes.isEmpty();
        
        if (needsStartingRegion) {
            showStartingRegionSelection(commandBuilder, eventBuilder, tree);
        }
        
        // Render tree nodes and connections on the canvas
        renderTreeCanvas(commandBuilder, eventBuilder, tree, allocatedNodes, reachableNodes);
        
        // Add sidebar event bindings
        addSidebarEvents(eventBuilder);
        
        // Add node events for rendered nodes
        addNodeEvents(eventBuilder, tree, allocatedNodes, reachableNodes);
        
        LOGGER.fine(() -> String.format(
            "PassiveTreePage built: tree=%s, available=%d, allocated=%d, nodes=%d",
            activeTreeId, availablePoints, allocatedCount, renderedNodeElementIds.size()
        ));
    }
    
    /**
     * Populate the sidebar with point information.
     */
    private void populateSidebar(
            @Nonnull UICommandBuilder commandBuilder,
            int availablePoints,
            int allocatedCount,
            int maxPoints
    ) {
        String pointsText = String.format("%d / %d", allocatedCount, maxPoints);
        commandBuilder.set("#PointsDisplay.Text", pointsText);
        commandBuilder.set("#AscendancyPointsLabel.Text", "Available: " + availablePoints);
    }
    
    /**
     * Show the starting region selection overlay.
     */
    private void showStartingRegionSelection(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull PassiveTree tree
    ) {
        commandBuilder.set("#RegionSelectionOverlay.Visible", true);
        commandBuilder.clear("#RegionOptions");
        
        int regionIndex = 0;
        for (String startingNodeId : tree.getStartingNodeIds()) {
            PassiveNode node = tree.getNode(startingNodeId);
            if (node == null) continue;
            
            String regionName = node.region() != null ? node.region() : node.name();
            String buttonId = "RegionBtn" + regionIndex;
            
            // Create region button inline
            String buttonUI = String.format(
                "Button #%s { Anchor: (Width: 100, Height: 60); Background: PatchStyle(Color: #2a2a3a); " +
                "LayoutMode: Top; Padding: (Full: 6); " +
                "Label { Text: \"%s\"; Style: (FontSize: 12, HorizontalAlignment: Center, RenderBold: true); } " +
                "Label { Text: \"Click to select\"; Style: (FontSize: 9, HorizontalAlignment: Center, TextColor: #888888); } }",
                buttonId, regionName
            );
            commandBuilder.appendInline("#RegionOptions", buttonUI);
            
            // Add event for this region button
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#" + buttonId,
                EventData.of("Action", "selectRegion").append("NodeId", startingNodeId),
                false
            );
            
            regionIndex++;
        }
    }
    
    /**
     * Render tree nodes and connections on the canvas.
     */
    private void renderTreeCanvas(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull Set<String> reachableNodes
    ) {
        // Clear previous renders - clear the child containers, not TreeArea
        renderedNodeElementIds.clear();
        commandBuilder.clear("#ConnectionsCanvas");
        commandBuilder.clear("#NodesCanvas");
        
        Set<String> startingNodes = tree.getStartingNodeIds();
        
        // Sort nodes for consistent rendering
        List<PassiveNode> sortedNodes = tree.getNodes().values().stream()
            .sorted(Comparator.comparing(PassiveNode::id))
            .toList();
        
        // Find tree bounds
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (PassiveNode node : sortedNodes) {
            minX = Math.min(minX, node.position().x());
            maxX = Math.max(maxX, node.position().x());
            minY = Math.min(minY, node.position().y());
            maxY = Math.max(maxY, node.position().y());
        }
        
        // Add padding
        int treePadding = 50;
        minY -= treePadding;
        maxY += treePadding;
        
        // Calculate dimensions
        int treeWidthPx = (int) ((maxX - minX) * COORD_SCALE);
        
        // Center tree horizontally
        int xOffset = (VIEWPORT_WIDTH - treeWidthPx) / 2;
        if (xOffset < 0) xOffset = 0;
        
        // Render connections first (behind nodes)
        renderConnections(commandBuilder, tree, allocatedNodes, reachableNodes, startingNodes, 
                         minX, minY, xOffset);
        
        // Render text labels (behind nodes but above connections)
        renderTextLabels(commandBuilder, tree, minX, minY, xOffset);
        
        // Render nodes
        renderNodes(commandBuilder, tree, sortedNodes, allocatedNodes, reachableNodes, startingNodes,
                   minX, minY, xOffset);
    }
    
    /**
     * Render text labels to the canvas.
     */
    private void renderTextLabels(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull PassiveTree tree,
            int minX, int minY, int xOffset
    ) {
        List<TextLabel> labels = tree.getTextLabels();
        if (labels.isEmpty()) {
            return;
        }
        
        int labelIndex = 0;
        for (TextLabel label : labels) {
            int screenX = (int) ((label.x() - minX) * COORD_SCALE) + xOffset;
            int screenY = (int) ((label.y() - minY) * COORD_SCALE);
            
            // Format font weight
            boolean isBold = "bold".equalsIgnoreCase(label.fontWeight());
            
            // Determine horizontal alignment based on anchor
            String alignment = switch (label.anchor().toLowerCase()) {
                case "left" -> "Left";
                case "right" -> "Right";
                default -> "Center";
            };
            
            // Estimate width for positioning - give generous width for centered text
            int labelWidth = label.text().length() * (label.fontSize() / 2 + 4) + 40;
            int posX = screenX - labelWidth / 2;
            
            // Build the label UI using Label with Style
            String labelUI = String.format(
                "Label #TL%d { Anchor: (Left: %d, Top: %d, Width: %d); Text: \"%s\"; Style: (FontSize: %d, TextColor: %s, HorizontalAlignment: %s, RenderBold: %s); }",
                labelIndex,
                posX,
                screenY - label.fontSize() / 2,
                labelWidth,
                label.text().replace("\"", "\\\""),
                label.fontSize(),
                label.color(),
                alignment,
                isBold ? "true" : "false"
            );
            commandBuilder.appendInline("#NodesCanvas", labelUI);
            labelIndex++;
        }
    }
    
    /**
     * Render nodes to the canvas.
     */
    private void renderNodes(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull PassiveTree tree,
            @Nonnull List<PassiveNode> sortedNodes,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull Set<String> reachableNodes,
            @Nonnull Set<String> startingNodes,
            int minX, int minY, int xOffset
    ) {
        int nodeIndex = 0;
        for (PassiveNode node : sortedNodes) {
            if (!searchMatchNodes.isEmpty() && !searchMatchNodes.contains(node.id())) {
                continue;
            }
            
            int nodeX = node.position().x();
            int nodeY = node.position().y();
            
            int screenX = (int) ((nodeX - minX) * COORD_SCALE) + xOffset;
            int screenY = (int) ((nodeY - minY) * COORD_SCALE);
            
            boolean isAllocated = allocatedNodes.contains(node.id());
            boolean isStarting = startingNodes.contains(node.id());
            boolean isReachable = isStarting || reachableNodes.contains(node.id());
            
            // Resolve visual template (frame and size) from node data or defaults
            NodeVisualTemplate frameTemplate = NodeVisualTemplateRegistry.get()
                .resolve(node.frameTemplate(), node.type(), isStarting);
            int nodeSize = frameTemplate.size();
            int posX = screenX - nodeSize / 2;
            int posY = screenY - nodeSize / 2;
            
            // Get texture for node frame based on allocation state
            String nodeImage = frameTemplate.getTexture(isAllocated, isReachable);
            
            String elementId = "N" + nodeIndex;
            
            // Add label above node (from explicit node data only - region labels use TextLabels)
            String labelText = node.label();
            if (labelText != null && !labelText.isEmpty()) {
                int labelWidth = 120;
                int labelX = screenX - labelWidth / 2;
                int labelY = posY - 20;
                String labelUI = String.format(
                    "Label #L%d { Anchor: (Left: %d, Top: %d, Width: %d, Height: 16); " +
                    "Text: \"%s\"; Style: (FontSize: 11, HorizontalAlignment: Center, TextColor: #FFD700); }",
                    nodeIndex, labelX, labelY, labelWidth, labelText
                );
                commandBuilder.appendInline("#NodesCanvas", labelUI);
            }
            
            // Resolve icon - use node's explicit icon or default
            String iconPath = node.icon();
            if (iconPath == null || iconPath.isEmpty()) {
                iconPath = DEFAULT_ICON;
            }
            int iconSize = (int) (nodeSize * 0.80);
            
            if (iconPath != null) {
                int iconX = posX + (nodeSize - iconSize) / 2;
                int iconY = posY + (nodeSize - iconSize) / 2;
                String iconUI = String.format(
                    "Group #I%d { Anchor: (Left: %d, Top: %d, Width: %d, Height: %d); Background: PatchStyle(TexturePath: \"%s\"); }",
                    nodeIndex, iconX, iconY, iconSize, iconSize, iconPath
                );
                commandBuilder.appendInline("#NodesCanvas", iconUI);
            }
            
            // Render node frame button on top
            String nodeUI = String.format(
                "Button #%s { Anchor: (Left: %d, Top: %d, Width: %d, Height: %d); Background: PatchStyle(TexturePath: \"%s\"); }",
                elementId, posX, posY, nodeSize, nodeSize, nodeImage
            );
            commandBuilder.appendInline("#NodesCanvas", nodeUI);
            
            renderedNodeElementIds.put(node.id(), elementId);
            nodeIndex++;
        }
    }
    
    /**
     * Render connection lines between nodes.
     */
    private void renderConnections(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull Set<String> reachableNodes,
            @Nonnull Set<String> startingNodes,
            int minX, int minY, int xOffset
    ) {
        Set<String> renderedConnections = new HashSet<>();
        int lineCounter = 0;
        int thickness = 3;
        
        for (PassiveNode node : tree.getNodes().values()) {
            int nodeX = node.position().x();
            int nodeY = node.position().y();
            
            for (String targetId : tree.getAdjacentNodes(node.id())) {
                // Avoid duplicates
                String connKey = node.id().compareTo(targetId) < 0 
                    ? node.id() + "-" + targetId 
                    : targetId + "-" + node.id();
                if (renderedConnections.contains(connKey)) continue;
                renderedConnections.add(connKey);
                
                PassiveNode targetNode = tree.getNode(targetId);
                if (targetNode == null) continue;
                
                int targetX = targetNode.position().x();
                int targetY = targetNode.position().y();
                
                int screenX1 = (int) ((nodeX - minX) * COORD_SCALE) + xOffset;
                int screenY1 = (int) ((nodeY - minY) * COORD_SCALE);
                int screenX2 = (int) ((targetX - minX) * COORD_SCALE) + xOffset;
                int screenY2 = (int) ((targetY - minY) * COORD_SCALE);
                
                // Determine line color
                boolean nodeAllocated = allocatedNodes.contains(node.id());
                boolean targetAllocated = allocatedNodes.contains(targetId);
                boolean bothAllocated = nodeAllocated && targetAllocated;
                boolean eitherReachable = reachableNodes.contains(node.id()) ||
                    reachableNodes.contains(targetId) ||
                    startingNodes.contains(node.id()) ||
                    startingNodes.contains(targetId);
                
                String lineColor;
                if (bothAllocated) {
                    lineColor = "#4CAF50";
                } else if (nodeAllocated || targetAllocated || eitherReachable) {
                    lineColor = "#806030";
                } else {
                    lineColor = "#404050";
                }
                
                // Draw L-shaped connections
                boolean needsLShape = Math.abs(screenX2 - screenX1) > 2 && Math.abs(screenY2 - screenY1) > 2;
                
                if (needsLShape) {
                    int srcX, srcY, dstX, dstY;
                    if (screenY1 <= screenY2) {
                        srcX = screenX1; srcY = screenY1; dstX = screenX2; dstY = screenY2;
                    } else {
                        srcX = screenX2; srcY = screenY2; dstX = screenX1; dstY = screenY1;
                    }
                    
                    // Horizontal segment
                    int hMinX = Math.min(srcX, dstX);
                    int hWidth = Math.abs(dstX - srcX);
                    String hLineUI = String.format(
                        "Group #LineH%d { Anchor: (Left: %d, Top: %d, Width: %d, Height: %d); Background: PatchStyle(Color: %s); }",
                        lineCounter, hMinX, srcY - thickness/2, hWidth + thickness, thickness, lineColor
                    );
                    commandBuilder.appendInline("#ConnectionsCanvas", hLineUI);
                    
                    // Vertical segment
                    int vMinY = Math.min(srcY, dstY);
                    int vHeight = Math.abs(dstY - srcY);
                    String vLineUI = String.format(
                        "Group #LineV%d { Anchor: (Left: %d, Top: %d, Width: %d, Height: %d); Background: PatchStyle(Color: %s); }",
                        lineCounter, dstX - thickness/2, vMinY, thickness, vHeight, lineColor
                    );
                    commandBuilder.appendInline("#ConnectionsCanvas", vLineUI);
                    lineCounter++;
                } else if (Math.abs(screenX2 - screenX1) > 2) {
                    // Purely horizontal
                    int hMinX = Math.min(screenX1, screenX2);
                    int hWidth = Math.abs(screenX2 - screenX1);
                    String lineUI = String.format(
                        "Group #Line%d { Anchor: (Left: %d, Top: %d, Width: %d, Height: %d); Background: PatchStyle(Color: %s); }",
                        lineCounter++, hMinX, screenY1 - thickness/2, hWidth, thickness, lineColor
                    );
                    commandBuilder.appendInline("#ConnectionsCanvas", lineUI);
                } else if (Math.abs(screenY2 - screenY1) > 2) {
                    // Purely vertical
                    int vMinY = Math.min(screenY1, screenY2);
                    int vHeight = Math.abs(screenY2 - screenY1);
                    String lineUI = String.format(
                        "Group #Line%d { Anchor: (Left: %d, Top: %d, Width: %d, Height: %d); Background: PatchStyle(Color: %s); }",
                        lineCounter++, screenX1 - thickness/2, vMinY, thickness, vHeight, lineColor
                    );
                    commandBuilder.appendInline("#ConnectionsCanvas", lineUI);
                }
            }
        }
    }
    
    /**
     * Add sidebar event bindings.
     */
    private void addSidebarEvents(@Nonnull UIEventBuilder eventBuilder) {
        // Tree tabs
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#GeneralTreeTab",
            EventData.of("Action", "switchGeneral"),
            false
        );
        
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ClassTreeTab",
            EventData.of("Action", "switchClass"),
            false
        );
        
        // Search
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#SearchInput",
            EventData.of("Action", "search").append("@Query", "#SearchInput.Value"),
            false
        );
        
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SearchBtn",
            EventData.of("Action", "search").append("@Query", "#SearchInput.Value"),
            false
        );
        
        // Actions
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ResetTreeBtn",
            EventData.of("Action", "respec"),
            false
        );
        
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseBtn",
            EventData.of("Action", "close"),
            false
        );
        
        // Confirmation dialog
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
    }
    
    /**
     * Add node event bindings for rendered nodes.
     */
    private void addNodeEvents(
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull Set<String> reachableNodes
    ) {
        for (Map.Entry<String, String> entry : renderedNodeElementIds.entrySet()) {
            String nodeId = entry.getKey();
            String nodeElementId = entry.getValue();
            boolean isAllocated = allocatedNodes.contains(nodeId);
            
            // Click to allocate or refund
            String action = isAllocated ? "refund" : "allocate";
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#" + nodeElementId,
                EventData.of("Action", action).append("NodeId", nodeId),
                false
            );
            
            // Hover for tooltip
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
        }
    }
    
    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData eventData
    ) {
        String action = eventData.getAction();
        if (action == null) return;
        
        switch (action) {
            case "close" -> closePage(ref, store);
            case "allocate" -> handleAllocate(ref, store, eventData.getNodeId());
            case "refund" -> handleRefund(ref, store, eventData.getNodeId());
            case "respec" -> handleRespec(ref, store);
            case "selectRegion" -> handleSelectRegion(ref, store, eventData.getNodeId());
            case "search" -> handleSearch(eventData.getQuery());
            case "switchGeneral" -> handleSwitchTree(ref, store, null);
            case "switchClass" -> handleSwitchClass(ref, store);
            case "showTooltip" -> handleShowTooltip(ref, store, eventData.getNodeId());
            case "hideTooltip" -> handleHideTooltip();
            case "confirmCancel" -> handleConfirmCancel();
            case "confirmAccept" -> handleConfirmAccept(ref, store);
            default -> LOGGER.fine("Unknown action: " + action);
        }
    }
    
    // ========== ACTION HANDLERS ==========
    
    private void closePage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }
    
    private void handleAllocate(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nullable String nodeId) {
        if (nodeId == null) return;
        
        String activeTreeId = getActiveTreeId();
        var result = PassiveTreeService.get().allocateNode(ref, activeTreeId, nodeId);
        if (result.success()) {
            LOGGER.info("Allocation successful for: " + nodeId);
            rebuild();
        } else {
            LOGGER.warning("Allocation failed for " + nodeId + ": " + result.reason());
        }
    }
    
    private void handleRefund(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nullable String nodeId) {
        if (nodeId == null) return;
        
        String activeTreeId = getActiveTreeId();
        var result = PassiveTreeService.get().refundNode(ref, activeTreeId, nodeId);
        if (result.success()) {
            LOGGER.info("Refund successful for: " + nodeId);
            rebuild();
        } else {
            LOGGER.warning("Refund failed for " + nodeId + ": " + result.reason());
        }
    }
    
    private void handleRespec(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // Show confirmation dialog
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#ConfirmationOverlay.Visible", true);
        builder.set("#ConfirmTitle.Text", Message.translation("hyforged.passive.respec.confirmTitle"));
        builder.set("#ConfirmMessage.Text", Message.translation("hyforged.passive.respec.confirmMessage"));
        sendUpdate(builder, new UIEventBuilder(), false);
    }
    
    private void handleConfirmCancel() {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#ConfirmationOverlay.Visible", false);
        sendUpdate(builder, new UIEventBuilder(), false);
    }
    
    private void handleConfirmAccept(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        String activeTreeId = getActiveTreeId();
        var result = PassiveTreeService.get().refundAll(ref, activeTreeId);
        if (result.success()) {
            LOGGER.fine("Respec completed: " + result.pointsReturned() + " points returned");
        } else {
            LOGGER.warning("Respec failed: " + result.reason());
        }
        rebuild();
    }
    
    private void handleSelectRegion(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nullable String nodeId) {
        if (nodeId == null) return;
        
        String activeTreeId = getActiveTreeId();
        var result = PassiveTreeService.get().allocateNode(ref, activeTreeId, nodeId);
        if (result.success()) {
            LOGGER.fine("Selected starting region: " + nodeId);
            rebuild();
        } else {
            LOGGER.warning("Failed to select starting region: " + result.reason());
        }
    }
    
    private void handleSearch(@Nullable String query) {
        currentSearchQuery = query != null ? query.trim().toLowerCase() : "";
        searchMatchNodes.clear();
        
        if (!currentSearchQuery.isEmpty()) {
            PassiveTree tree = PassiveTreeRegistry.get().getTree(getActiveTreeId());
            if (tree != null) {
                searchMatchNodes = tree.getNodes().values().stream()
                    .filter(node -> matchesSearch(node, currentSearchQuery))
                    .map(PassiveNode::id)
                    .collect(Collectors.toSet());
            }
        }
        
        rebuild();
    }
    
    private boolean matchesSearch(@Nonnull PassiveNode node, @Nonnull String query) {
        if (node.name().toLowerCase().contains(query)) return true;
        if (node.description() != null && node.description().toLowerCase().contains(query)) return true;
        if (node.type().toLowerCase().contains(query)) return true;
        if (node.region() != null && node.region().toLowerCase().contains(query)) return true;
        
        for (PassiveNodeEffect effect : node.effects()) {
            if (effect.type().toLowerCase().contains(query)) return true;
            for (Object value : effect.data().values()) {
                if (value != null && value.toString().toLowerCase().contains(query)) return true;
            }
        }
        
        return false;
    }
    
    private void handleSwitchTree(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nullable String newTreeId) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            PassiveTreePage newPage = new PassiveTreePage(this.playerRef, newTreeId);
            player.getPageManager().openCustomPage(ref, store, newPage);
        }
    }
    
    private void handleSwitchClass(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // TODO: Show class tree selection overlay
        LOGGER.fine("Class tree selection requested");
    }
    
    private void handleShowTooltip(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nullable String nodeId) {
        if (nodeId == null) return;
        
        PassiveTree tree = PassiveTreeRegistry.get().getTree(getActiveTreeId());
        if (tree == null) return;
        
        PassiveNode node = tree.getNode(nodeId);
        if (node == null) return;
        
        PassiveTreeComponent component = getPassiveComponent(ref, store);
        Set<String> allocatedNodes = getAllocatedNodes(component, tree);
        Set<String> reachableNodes = getReachableNodes(tree, allocatedNodes);
        boolean isAllocated = allocatedNodes.contains(nodeId);
        boolean isStarting = tree.getStartingNodeIds().contains(nodeId);
        boolean canAllocate = isStarting || reachableNodes.contains(nodeId);
        
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#TooltipOverlay.Visible", true);
        builder.set("#TooltipName.Text", node.name());
        builder.set("#TooltipType.Text", formatNodeType(node.type()));
        
        String status = isAllocated ? "ALLOCATED" : (canAllocate ? "AVAILABLE" : "LOCKED");
        builder.set("#TooltipStatus.Text", status);
        builder.set("#TooltipDescription.Text", node.description() != null ? node.description() : "");
        
        // Build effects text
        StringBuilder effectsText = new StringBuilder();
        for (PassiveNodeEffect effect : node.effects()) {
            if (effectsText.length() > 0) effectsText.append("\n");
            effectsText.append("• ").append(formatEffectText(effect));
        }
        builder.set("#TooltipEffects.Text", effectsText.toString());
        
        String actionHint = isAllocated ? "Click to refund" : (canAllocate ? "Click to allocate" : "Locked");
        builder.set("#TooltipActionHint.Text", actionHint);
        
        sendUpdate(builder, new UIEventBuilder(), false);
    }
    
    private void handleHideTooltip() {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#TooltipOverlay.Visible", false);
        sendUpdate(builder, new UIEventBuilder(), false);
    }
    
    // ========== HELPERS ==========
    
    @Nonnull
    private String getActiveTreeId() {
        if (treeId != null) return treeId;
        PassiveTree generalTree = PassiveTreeRegistry.get().getGeneralTree();
        return generalTree != null ? generalTree.getId() : "general";
    }
    
    @Nullable
    private PassiveTreeComponent getPassiveComponent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        var type = HyforgedPlugin.getInstance().getPassiveTreeComponentType();
        return type != null ? store.getComponent(ref, type) : null;
    }
    
    @Nonnull
    private Set<String> getAllocatedNodes(@Nullable PassiveTreeComponent component, @Nonnull PassiveTree tree) {
        if (component == null) return Set.of();
        return tree.isGeneralTree() ?
            component.getGeneralAllocatedNodes() :
            component.getClassAllocatedNodes(tree.getClassId());
    }
    
    @Nonnull
    private Set<String> getReachableNodes(@Nonnull PassiveTree tree, @Nonnull Set<String> allocatedNodes) {
        Set<String> reachable = new HashSet<>();
        Set<String> startingNodeIds = tree.getStartingNodeIds();
        
        if (allocatedNodes.isEmpty()) {
            reachable.addAll(startingNodeIds);
            return reachable;
        }
        
        for (String allocatedId : allocatedNodes) {
            reachable.addAll(tree.getAdjacentNodes(allocatedId));
        }
        
        reachable.addAll(startingNodeIds);
        reachable.removeAll(allocatedNodes);
        return reachable;
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
            case "spell-grant" -> "Grants: " + data.getOrDefault("SpellId", data.getOrDefault("spell", "Unknown"));
            case "unlock-flag" -> "Unlocks: " + data.getOrDefault("Description", data.getOrDefault("FlagId", "Unknown"));
            default -> type + ": " + data;
        };
    }
    
    private String formatStatName(@Nonnull String stat) {
        return stat.replace("_", " ")
                   .replace("-", " ")
                   .replaceAll("([a-z])([A-Z])", "$1 $2")
                   .trim();
    }
    
    // ========== PAGE EVENT DATA ==========
    
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
        
        @Nullable
        public String getAction() { return action; }
        
        @Nullable
        public String getNodeId() { return nodeId; }
        
        @Nullable
        public String getQuery() { return query; }
    }
}
