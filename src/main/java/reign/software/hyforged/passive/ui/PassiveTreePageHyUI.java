package reign.software.hyforged.passive.ui;

import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.model.PassiveNode;
import reign.software.hyforged.passive.model.PassiveNodeEffect;
import reign.software.hyforged.passive.model.PassiveNodeType;
import reign.software.hyforged.passive.model.PassiveTree;
import reign.software.hyforged.passive.registry.PassiveTreeRegistry;
import reign.software.hyforged.passive.registry.StatIconRegistry;
import reign.software.hyforged.passive.service.PassiveTreeService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Passive Tree UI page using HyUI framework.
 * <p>
 * Features:
 * <ul>
 *   <li>PoB-style layout with left sidebar and fullscreen tree canvas</li>
 *   <li>HYUIML-based markup with TemplateProcessor for dynamic content</li>
 *   <li>Lambda-based event handling via PageBuilder</li>
 *   <li>Zoomable/pannable tree with connection lines</li>
 *   <li>Hover tooltips, allocation/refund on click</li>
 * </ul>
 */
public class PassiveTreePageHyUI {
    
    private static final Logger LOGGER = Logger.getLogger(PassiveTreePageHyUI.class.getName());
    
    // Layout constants
    private static final float COORD_SCALE = 1.5f;  // Scale tree coords to pixels
    private static final int VIEWPORT_WIDTH = 900;  // Fixed tree area width in pixels
    private static final int SIDEBAR_WIDTH = 140;   // Sidebar width in pixels
    private static final int TOTAL_WIDTH = VIEWPORT_WIDTH + SIDEBAR_WIDTH;  // Total page width
    private static final int TOTAL_HEIGHT = 600;    // Fixed page height in pixels
    
    // State
    private final PlayerRef playerRef;
    private final Ref<EntityStore> entityRef;
    private final Store<EntityStore> store;
    
    @Nullable
    private final String treeId;
    
    @Nullable
    private String currentSearchQuery;
    @Nonnull
    private Set<String> searchMatchNodes = new HashSet<>();
    
    @Nullable
    private HyUIPage currentPage;
    
    /**
     * Open the passive tree page for a player.
     */
    public static void open(
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable String treeId
    ) {
        new PassiveTreePageHyUI(playerRef, ref, store, treeId).show();
    }
    
    private PassiveTreePageHyUI(
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable String treeId
    ) {
        this.playerRef = playerRef;
        this.entityRef = ref;
        this.store = store;
        this.treeId = treeId;
    }
    
    /**
     * Show the passive tree page.
     */
    public void show() {
        // Get passive data
        PassiveTreeComponent component = getPassiveComponent();
        String activeTreeId = getActiveTreeId();
        PassiveTree tree = PassiveTreeRegistry.get().getTree(activeTreeId);
        
        if (tree == null) {
            LOGGER.warning("Cannot show passive tree page - tree not found: " + activeTreeId);
            return;
        }
        
        // Debug: log starting nodes
        LOGGER.info("Tree " + activeTreeId + " has " + tree.getNodeCount() + " nodes, starting nodes: " + tree.getStartingNodeIds());
        
        // Gather allocation data
        Set<String> allocatedNodes = getAllocatedNodes(component, tree);
        Set<String> reachableNodes = getReachableNodes(tree, allocatedNodes);
        
        // Debug: log reachable nodes
        LOGGER.info("Allocated: " + allocatedNodes.size() + ", Reachable: " + reachableNodes.size());
        int availablePoints = PassiveTreeService.get().getAvailablePoints(entityRef, activeTreeId);
        int allocatedCount = allocatedNodes.size();
        int maxPoints = availablePoints + allocatedCount;
        
        // Build the page using HYUIML template
        PageHtmlResult pageResult = buildPageHtml(tree, allocatedNodes, reachableNodes, availablePoints, allocatedCount, maxPoints);
        
        // Create page with events
        PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
            .fromHtml(pageResult.html())
            .withLifetime(CustomPageLifetime.CanDismiss);
        
        // Add sidebar button events
        addSidebarEvents(builder);
        
        // Add node events only for nodes that were actually rendered
        addNodeEvents(builder, tree, allocatedNodes, reachableNodes, pageResult.renderedNodeIds());
        
        // Open the page
        this.currentPage = builder.open(store);
        
        LOGGER.fine(() -> String.format(
            "PassiveTreePageHyUI opened: tree=%s, available=%d, allocated=%d",
            activeTreeId, availablePoints, allocatedCount
        ));
    }
    
    /**
     * Result from building page HTML.
     */
    private record PageHtmlResult(String html, Set<String> renderedNodeIds) {}
    
    /**
     * Build the complete page HTML using HYUIML.
     * Returns both the HTML and the set of rendered node IDs for event binding.
     */
    private PageHtmlResult buildPageHtml(
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull Set<String> reachableNodes,
            int availablePoints,
            int allocatedCount,
            int maxPoints
    ) {
        boolean isGeneral = tree.isGeneralTree();
        
        TemplateProcessor template = new TemplateProcessor()
            .setVariable("treeName", tree.getId())
            .setVariable("isGeneralTree", isGeneral)
            .setVariable("availablePoints", availablePoints)
            .setVariable("allocatedCount", allocatedCount)
            .setVariable("maxPoints", maxPoints);
        
        // Register reusable components
        template.registerComponent("statRow", """
            <div style="layout-mode: Left; anchor-height: 16; padding-left: 4;">
                <p style="font-size: 10; color: #aaaaaa; anchor-width: 100;">{{$label}}</p>
                <p style="font-size: 10; color: #ffffff;">{{$value}}</p>
            </div>
        """);
        
        template.registerComponent("separator", """
            <div style="anchor-height: 1; background-color: #404050;"></div>
        """);
        
        // Build nodes HTML first to get tree bounds
        NodesHtmlResult nodesResult = buildNodesHtml(tree, allocatedNodes, reachableNodes);
        
        // Calculate tree bounds for connections (same logic as buildNodesHtml)
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        for (PassiveNode node : tree.getNodes().values()) {
            minX = Math.min(minX, node.position().x());
            maxX = Math.max(maxX, node.position().x());
            minY = Math.min(minY, node.position().y());
        }
        int treePadding = 50;
        minY -= treePadding;
        
        // Calculate horizontal centering offset
        int treeWidthPx = (int) ((maxX - minX) * COORD_SCALE);
        int xOffset = (VIEWPORT_WIDTH - treeWidthPx) / 2;
        if (xOffset < 0) xOffset = 0;
        
        String connectionsHtml = buildConnectionsHtml(tree, allocatedNodes, reachableNodes, minX, minY, xOffset);
        
        template.setVariable("nodesHtml", nodesResult.html());
        template.setVariable("connectionsHtml", connectionsHtml);
        template.setVariable("canvasWidth", nodesResult.canvasWidth());
        template.setVariable("canvasHeight", nodesResult.canvasHeight());
        
        String html = template.process(PAGE_TEMPLATE)
            .replace("{{{nodesHtml}}}", nodesResult.html())
            .replace("{{{connectionsHtml}}}", connectionsHtml);
        
        return new PageHtmlResult(html, nodesResult.renderedNodeIds());
    }
    
    /**
     * Result from building nodes HTML, containing both the HTML and the set of rendered node IDs.
     */
    private record NodesHtmlResult(String html, Set<String> renderedNodeIds, int canvasWidth, int canvasHeight) {}
    
    /**
     * Build HTML for all tree nodes.
     * Returns HTML, rendered node IDs, and canvas dimensions.
     */
    private NodesHtmlResult buildNodesHtml(
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull Set<String> reachableNodes
    ) {
        StringBuilder sb = new StringBuilder();
        Set<String> startingNodes = tree.getStartingNodeIds();
        Set<String> renderedNodeIds = new LinkedHashSet<>(); // Preserve insertion order
        
        // Sort nodes by ID for consistent rendering order
        List<PassiveNode> sortedNodes = tree.getNodes().values().stream()
            .sorted((a, b) -> a.id().compareTo(b.id()))
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
        
        // Add padding around tree
        int treePadding = 50;
        minY -= treePadding;
        maxY += treePadding;
        
        // Calculate tree dimensions in pixels
        int treeWidthPx = (int) ((maxX - minX) * COORD_SCALE);
        int canvasHeight = (int) ((maxY - minY) * COORD_SCALE);
        
        // Center tree horizontally within viewport
        int xOffset = (VIEWPORT_WIDTH - treeWidthPx) / 2;
        if (xOffset < 0) xOffset = 0; // Don't go negative if tree wider than viewport
        
        // Canvas width is viewport width (fixed), height is tree height (scrollable)
        int canvasWidth = VIEWPORT_WIDTH;
        
        // Nodes are positioned at fixed coordinates within the canvas
        // Scrolling is handled by moving the entire canvas container
        
        for (PassiveNode node : sortedNodes) {
            // Apply search filter
            if (!searchMatchNodes.isEmpty() && !searchMatchNodes.contains(node.id())) {
                continue;
            }
            
            int nodeX = node.position().x();
            int nodeY = node.position().y();
            
            // Calculate position within canvas (centered horizontally)
            int screenX = (int) ((nodeX - minX) * COORD_SCALE) + xOffset;
            int screenY = (int) ((nodeY - minY) * COORD_SCALE);
            
            boolean isAllocated = allocatedNodes.contains(node.id());
            boolean isStarting = startingNodes.contains(node.id());
            // Starting nodes are always reachable, even if not in the reachable set
            boolean isReachable = isStarting || reachableNodes.contains(node.id());
            
            int nodeSize = isStarting ? 36 : getNodeSize(node.type()); // Starting nodes are larger
            String nodeImage = isStarting 
                ? getStartingNodeImage(isAllocated, isReachable)
                : getNodeImage(node.type(), isAllocated, isReachable);
            String nodeElementId = sanitizeId(node.id());
            
            // Build rich tooltip text
            String tooltipText = buildTooltipText(node, isAllocated, isReachable, isStarting);
            
            // Center the node on its position
            int posX = screenX - nodeSize / 2;
            int posY = screenY - nodeSize / 2;
            
            // Add attribute label above starting nodes
            if (isStarting) {
                String attributeLabel = getStartingNodeLabel(node.id());
                int labelWidth = 120;
                int labelX = screenX - labelWidth / 2;
                int labelY = posY - 20; // Above the node
                sb.append(String.format(
                    "<div style=\"anchor-left: %d; anchor-top: %d; anchor-width: %d; anchor-height: 16; font-size: 11; text-align: center; color: #FFD700;\">%s</div>",
                    labelX, labelY, labelWidth, attributeLabel
                ));
            }
            
            // Add stat icon background based on node region (rendered behind the frame)
            String statIcon = getStatIconForNode(node, isStarting);
            if (statIcon != null) {
                int iconSize = (int) (nodeSize * 0.80); // Icon fills most of the frame
                int iconX = posX + (nodeSize - iconSize) / 2;
                int iconY = posY + (nodeSize - iconSize) / 2;
                sb.append(String.format(
                    "<div style=\"anchor-left: %d; anchor-top: %d; anchor-width: %d; anchor-height: %d; background-image: url('%s'); background-repeat: no-repeat; background-position: center; background-size: contain;\"></div>",
                    iconX, iconY, iconSize, iconSize, statIcon
                ));
            }
            
            // Clickable node container - use a styled div with the node image
            // The button events work on divs too when using addEventListener
            sb.append(String.format(
                "<div id=\"%s\" style=\"anchor-left: %d; anchor-top: %d; anchor-width: %d; anchor-height: %d; background-image: url('%s'); background-repeat: no-repeat; background-position: center; background-size: contain;\" data-hyui-tooltiptext=\"%s\"></div>",
                nodeElementId, posX, posY, nodeSize, nodeSize,
                nodeImage,
                escapeHtml(tooltipText)
            ));
            
            // Track this node as rendered
            renderedNodeIds.add(node.id());
        }
        
        return new NodesHtmlResult(sb.toString(), renderedNodeIds, canvasWidth, canvasHeight);
    }
    
    /**
     * Build a compact tooltip text for a node.
     */
    private String buildTooltipText(@Nonnull PassiveNode node, boolean isAllocated, boolean canAllocate, boolean isStarting) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.name());
        
        // Add description if present (truncate long descriptions)
        if (node.description() != null && !node.description().isEmpty()) {
            String desc = node.description();
            if (desc.length() > 60) {
                desc = desc.substring(0, 57) + "...";
            }
            sb.append("\n").append(desc);
        }
        
        // Status indicator - single line
        if (isAllocated) {
            sb.append("\n[Allocated]");
        } else if (canAllocate || isStarting) {
            sb.append("\n[Click to allocate]");
        } else {
            sb.append("\n[Locked]");
        }
        
        return sb.toString();
    }
    
    /**
     * Build HTML for connection lines between nodes.
     * Connections are positioned at fixed coordinates within the canvas.
     */
    private String buildConnectionsHtml(
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull Set<String> reachableNodes,
            int minX, int minY, int xOffset
    ) {
        StringBuilder sb = new StringBuilder();
        Set<String> startingNodes = tree.getStartingNodeIds();
        Set<String> renderedConnections = new HashSet<>();
        
        // Sort nodes by ID for consistent rendering order
        List<PassiveNode> sortedNodes = tree.getNodes().values().stream()
            .sorted((a, b) -> a.id().compareTo(b.id()))
            .toList();
        
        for (PassiveNode node : sortedNodes) {
            int nodeX = node.position().x();
            int nodeY = node.position().y();
            
            // Sort adjacent nodes for consistent connection order
            List<String> sortedAdjacentNodes = tree.getAdjacentNodes(node.id()).stream()
                .sorted()
                .toList();
            
            for (String targetId : sortedAdjacentNodes) {
                String connKey = node.id().compareTo(targetId) < 0 ?
                    node.id() + "-" + targetId : targetId + "-" + node.id();
                if (renderedConnections.contains(connKey)) continue;
                renderedConnections.add(connKey);
                
                PassiveNode targetNode = tree.getNode(targetId);
                if (targetNode == null) continue;
                
                int targetX = targetNode.position().x();
                int targetY = targetNode.position().y();
                
                // Apply horizontal centering offset
                int screenX1 = (int) ((nodeX - minX) * COORD_SCALE) + xOffset;
                int screenY1 = (int) ((nodeY - minY) * COORD_SCALE);
                int screenX2 = (int) ((targetX - minX) * COORD_SCALE) + xOffset;
                int screenY2 = (int) ((targetY - minY) * COORD_SCALE);
                
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
                
                int thickness = 3;
                
                // Draw L-shaped connections with consistent elbow direction
                // Horizontal first (from source X to dest X at source Y), then vertical down
                // This makes branches clearly independent from parent
                
                boolean needsLShape = Math.abs(screenX2 - screenX1) > 2 && Math.abs(screenY2 - screenY1) > 2;
                
                if (needsLShape) {
                    // Determine source and target based on Y position (higher node is source/parent)
                    int srcX, srcY, dstX, dstY;
                    if (screenY1 <= screenY2) {
                        // node is higher (parent), draw from node to target
                        srcX = screenX1; srcY = screenY1; dstX = screenX2; dstY = screenY2;
                    } else {
                        // target is higher (parent), draw from target to node
                        srcX = screenX2; srcY = screenY2; dstX = screenX1; dstY = screenY1;
                    }
                    
                    // Draw: horizontal from srcX to dstX at srcY, then vertical down to dstY
                    // This puts the elbow at (dstX, srcY) - branches go out then down
                    
                    // Horizontal segment: from srcX to dstX at srcY
                    int hMinX = Math.min(srcX, dstX);
                    int hWidth = Math.abs(dstX - srcX);
                    sb.append(String.format(
                        "<div style=\"anchor-left: %d; anchor-top: %d; anchor-width: %d; anchor-height: %d; background-color: %s;\"></div>",
                        hMinX, srcY - thickness/2, hWidth + thickness, thickness, lineColor
                    ));
                    
                    // Vertical segment: from srcY to dstY at dstX
                    int vMinY = Math.min(srcY, dstY);
                    int vHeight = Math.abs(dstY - srcY);
                    sb.append(String.format(
                        "<div style=\"anchor-left: %d; anchor-top: %d; anchor-width: %d; anchor-height: %d; background-color: %s;\"></div>",
                        dstX - thickness/2, vMinY, thickness, vHeight, lineColor
                    ));
                } else if (Math.abs(screenX2 - screenX1) > 2) {
                    // Purely horizontal
                    int hMinX = Math.min(screenX1, screenX2);
                    int hWidth = Math.abs(screenX2 - screenX1);
                    sb.append(String.format(
                        "<div style=\"anchor-left: %d; anchor-top: %d; anchor-width: %d; anchor-height: %d; background-color: %s;\"></div>",
                        hMinX, screenY1 - thickness/2, hWidth, thickness, lineColor
                    ));
                } else if (Math.abs(screenY2 - screenY1) > 2) {
                    // Purely vertical
                    int vMinY = Math.min(screenY1, screenY2);
                    int vHeight = Math.abs(screenY2 - screenY1);
                    sb.append(String.format(
                        "<div style=\"anchor-left: %d; anchor-top: %d; anchor-width: %d; anchor-height: %d; background-color: %s;\"></div>",
                        screenX1 - thickness/2, vMinY, thickness, vHeight, lineColor
                    ));
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Add sidebar button event listeners.
     */
    private void addSidebarEvents(@Nonnull PageBuilder builder) {
        // Tree type tabs
        builder.addEventListener("GeneralTreeBtn", CustomUIEventBindingType.Activating, (data, ctx) -> {
            switchTree(null);
        });
        
        builder.addEventListener("ClassTreeBtn", CustomUIEventBindingType.Activating, (data, ctx) -> {
            // TODO: Show class tree selection
            LOGGER.fine("Class tree selection requested");
        });
        
        // Search
        builder.addEventListener("SearchInput", CustomUIEventBindingType.ValueChanged, (data, ctx) -> {
            ctx.getValue("SearchInput", String.class).ifPresent(this::handleSearch);
        });
        
        builder.addEventListener("SearchBtn", CustomUIEventBindingType.Activating, (data, ctx) -> {
            ctx.getValue("SearchInput", String.class).ifPresent(this::handleSearch);
        });
        
        // Action buttons
        builder.addEventListener("ResetTreeBtn", CustomUIEventBindingType.Activating, (data, ctx) -> {
            handleRespec();
        });
        
        builder.addEventListener("CloseBtn", CustomUIEventBindingType.Activating, (data, ctx) -> {
            ctx.getPage().ifPresent(HyUIPage::close);
        });
    }
    
    /**
     * Add node click and hover events.
     * Only adds events for nodes that were actually rendered in the HTML.
     */
    private void addNodeEvents(
            @Nonnull PageBuilder builder,
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull Set<String> reachableNodes,
            @Nonnull Set<String> renderedNodeIds
    ) {
        Set<String> startingNodes = tree.getStartingNodeIds();
        int eventCount = 0;
        
        for (String nodeId : renderedNodeIds) {
            PassiveNode node = tree.getNode(nodeId);
            if (node == null) continue;
            
            String nodeElementId = sanitizeId(nodeId);
            boolean isAllocated = allocatedNodes.contains(nodeId);
            boolean isReachable = reachableNodes.contains(nodeId) || startingNodes.contains(nodeId);
            
            // Click to allocate or refund (use MouseButtonReleased which works on all elements)
            final String finalNodeId = nodeId;
            final boolean finalIsAllocated = isAllocated;
            final boolean finalIsReachable = isReachable;
            builder.addEventListener(nodeElementId, CustomUIEventBindingType.MouseButtonReleased, (data, ctx) -> {
                LOGGER.info("Node clicked: " + finalNodeId + " (allocated=" + finalIsAllocated + ", reachable=" + finalIsReachable + ")");
                if (finalIsAllocated) {
                    handleRefund(finalNodeId);
                } else if (finalIsReachable) {
                    handleAllocate(finalNodeId);
                } else {
                    LOGGER.info("Node " + finalNodeId + " cannot be allocated - not reachable");
                }
            });
            
            // Hover for tooltip
            builder.addEventListener(nodeElementId, CustomUIEventBindingType.MouseEntered, (data, ctx) -> {
                showTooltip(node, isAllocated, isReachable);
            });
            
            builder.addEventListener(nodeElementId, CustomUIEventBindingType.MouseExited, (data, ctx) -> {
                hideTooltip();
            });
            
            eventCount++;
        }
        
        LOGGER.info("Registered click events for " + eventCount + " nodes (button overlay IDs)");
    }
    
    // ========== ACTION HANDLERS ==========
    
    private void handleAllocate(@Nonnull String nodeId) {
        String activeTreeId = getActiveTreeId();
        LOGGER.info("Attempting to allocate node: " + nodeId + " in tree: " + activeTreeId);
        var result = PassiveTreeService.get().allocateNode(entityRef, activeTreeId, nodeId);
        if (result.success()) {
            LOGGER.info("Allocation successful for: " + nodeId);
            rebuild();
        } else {
            LOGGER.warning("Allocation failed for " + nodeId + ": " + result.reason());
        }
    }
    
    private void handleRefund(@Nonnull String nodeId) {
        String activeTreeId = getActiveTreeId();
        LOGGER.info("Attempting to refund node: " + nodeId + " in tree: " + activeTreeId);
        var result = PassiveTreeService.get().refundNode(entityRef, activeTreeId, nodeId);
        if (result.success()) {
            LOGGER.info("Refund successful for: " + nodeId);
            rebuild();
        } else {
            LOGGER.warning("Refund failed for " + nodeId + ": " + result.reason());
        }
    }
    
    private void handleRespec() {
        String activeTreeId = getActiveTreeId();
        var result = PassiveTreeService.get().refundAll(entityRef, activeTreeId);
        if (result.success()) {
            LOGGER.fine("Respec completed: " + result.pointsReturned() + " points returned");
            rebuild();
        } else {
            LOGGER.warning("Respec failed: " + result.reason());
        }
    }
    
    private void handleSearch(@Nonnull String query) {
        currentSearchQuery = query.trim().toLowerCase();
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
    
    private void switchTree(@Nullable String newTreeId) {
        if (currentPage != null) {
            currentPage.close();
        }
        PassiveTreePageHyUI.open(playerRef, entityRef, store, newTreeId);
    }
    
    private void rebuild() {
        if (currentPage != null) {
            currentPage.close();
        }
        show();
    }
    
    /**
     * Show tooltip for a node.
     * Note: Currently using data-hyui-tooltiptext on nodes for basic tooltips.
     * For detailed tooltips, would need to track hovered node state and rebuild.
     */
    private void showTooltip(@Nonnull PassiveNode node, boolean isAllocated, boolean canAllocate) {
        // Tooltips are handled via data-hyui-tooltiptext attribute on the node buttons
        // For more complex tooltip updates, we would need to store the hovered node
        // and include its details in the template during rebuild.
        LOGGER.fine("Showing tooltip for node: " + node.id());
    }
    
    private void hideTooltip() {
        LOGGER.fine("Hiding tooltip");
    }
    
    // ========== HELPERS ==========
    
    @Nonnull
    private String getActiveTreeId() {
        if (treeId != null) return treeId;
        PassiveTree generalTree = PassiveTreeRegistry.get().getGeneralTree();
        return generalTree != null ? generalTree.getId() : "general";
    }
    
    @Nullable
    private PassiveTreeComponent getPassiveComponent() {
        var type = HyforgedPlugin.getInstance().getPassiveTreeComponentType();
        return type != null ? store.getComponent(entityRef, type) : null;
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
        
        // If nothing allocated yet, only starting nodes are reachable
        if (allocatedNodes.isEmpty()) {
            reachable.addAll(startingNodeIds);
            return reachable;
        }
        
        // Nodes adjacent to allocated nodes are reachable
        for (String allocatedId : allocatedNodes) {
            reachable.addAll(tree.getAdjacentNodes(allocatedId));
        }
        
        // Starting nodes are always reachable (for refund path)
        reachable.addAll(startingNodeIds);
        
        // Remove already allocated nodes from reachable set
        reachable.removeAll(allocatedNodes);
        return reachable;
    }
    
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
    
    private String getNodeImage(@Nonnull String type, boolean allocated, boolean canAllocate) {
        // Keystone/Notable use "Unallocated" suffix, AscendancyFrame use "Normal" suffix
        String keystoneSuffix = allocated ? "Allocated" : (canAllocate ? "CanAllocate" : "Unallocated");
        String frameSuffix = allocated ? "Allocated" : (canAllocate ? "CanAllocate" : "Normal");
        
        return switch (type.toLowerCase()) {
            case PassiveNodeType.KEYSTONE -> "Hyforged/Textures/KeystoneFrame" + keystoneSuffix + ".png";
            case PassiveNodeType.NOTABLE -> "Hyforged/Textures/NotableFrame" + keystoneSuffix + ".png";
            case PassiveNodeType.MASTERY, PassiveNodeType.UNLOCK -> 
                "Hyforged/Textures/PassiveSkillScreenAscendancyFrameLarge" + frameSuffix + ".png";
            default -> "Hyforged/Textures/PassiveSkillScreenAscendancyFrameSmall" + frameSuffix + ".png";
        };
    }
    
    private String getStartingNodeImage(boolean allocated, boolean canAllocate) {
        // Starting nodes use a distinctive frame
        String suffix = allocated ? "Allocated" : (canAllocate ? "CanAllocate" : "Normal");
        return "Hyforged/Textures/PassiveSkillScreenAscendancyFrameLarge" + suffix + ".png";
    }
    
    private String getStartingNodeLabel(@Nonnull String nodeId) {
        // Extract attribute from node ID (e.g., "hyforged:start-strength" -> "STRENGTH")
        String id = nodeId.toLowerCase();
        if (id.contains("strength")) return "STRENGTH";
        if (id.contains("dexterity")) return "DEXTERITY";
        if (id.contains("intelligence")) return "INTELLIGENCE";
        if (id.contains("wisdom")) return "WISDOM";
        // Fallback: extract from the ID itself
        int colonIndex = nodeId.indexOf(':');
        if (colonIndex >= 0 && colonIndex + 1 < nodeId.length()) {
            String slug = nodeId.substring(colonIndex + 1);
            if (slug.startsWith("start-")) {
                slug = slug.substring(6);
            }
            return slug.toUpperCase().replace("-", " ");
        }
        return "START";
    }
    
    /**
     * Get the stat icon texture for a node using the data-driven StatIconRegistry.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>Explicit node icon (from node JSON)</li>
     *   <li>Starting node attribute icon</li>
     *   <li>Node type icon (keystone, notable)</li>
     *   <li>Stat pattern matching from effects</li>
     *   <li>Default icon</li>
     * </ol>
     */
    @Nullable
    private String getStatIconForNode(@Nonnull PassiveNode node, boolean isStarting) {
        return StatIconRegistry.get().getIconForNode(node, isStarting);
    }
    
    private String sanitizeId(@Nonnull String id) {
        return "node_" + id.replaceAll("[^a-zA-Z0-9]", "_");
    }
    
    private String escapeHtml(@Nonnull String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("\n", "&#10;")
                   .replace("\r", "");
    }
    
    // ========== PAGE TEMPLATE ==========
    
    private static final String PAGE_TEMPLATE = """
        <style>
            .page-container {
                anchor-width: 1040;
                anchor-height: 600;
                anchor-left: 50%%;
                anchor-top: 50%%;
                margin-left: -520;
                margin-top: -300;
                background-color: #1a1a1e;
            }
            
            .sidebar {
                anchor-left: 0;
                anchor-top: 0;
                anchor-bottom: 0;
                anchor-width: 140;
                layout-mode: Top;
                background-color: #121215;
                padding: 6;
            }
            
            .sidebar-section {
                layout-mode: Top;
                padding-top: 4;
                padding-bottom: 4;
            }
            
            .tree-tabs {
                layout-mode: Top;
                padding-bottom: 4;
            }
            
            .tab-btn {
                anchor-height: 20;
                font-size: 9;
            }
            
            .small-btn {
                anchor-height: 16;
                anchor-width: 24;
                font-size: 5;
            }
            
            .action-btn {
                anchor-height: 24;
                font-size: 10;
            }
            
            .separator {
                anchor-height: 1;
                background-color: #404050;
            }
            
            .points-label {
                font-size: 9;
                color: #888888;
            }
            
            .points-value {
                font-size: 12;
                font-weight: bold;
                color: #ffffff;
            }
            
            .stats-label {
                font-size: 9;
                color: #888888;
            }
            
            .tree-area {
                anchor-left: 140;
                anchor-top: 0;
                anchor-width: 900;
                anchor-bottom: 0;
                background-color: #0a0a0f;
                layout-mode: TopScrolling;
            }
        </style>
        
        <div class="page-container">
            <!-- LEFT SIDEBAR -->
            <div class="sidebar">
                <!-- Tree Type Tabs -->
                <div class="tree-tabs">
                    <button id="GeneralTreeBtn" class="tab-btn">General</button>
                    <button id="ClassTreeBtn" class="tab-btn">Class</button>
                </div>
                
                <div class="separator"></div>
                
                <!-- Points Display -->
                <div class="sidebar-section">
                    <p class="points-label">Passive Points</p>
                    <p id="PointsDisplay" class="points-value">{{$allocatedCount}} / {{$maxPoints}}</p>
                    <p class="points-label">Available: {{$availablePoints}}</p>
                </div>
                
                <div class="separator"></div>
                
                <!-- Search -->
                <div class="sidebar-section" style="layout-mode: Top;">
                    <input type="text" id="SearchInput" value="" placeholder="Search..." 
                           style="anchor-height: 20; font-size: 9;" />
                    <button id="SearchBtn" style="anchor-height: 20; font-size: 9;">Search</button>
                </div>
                
                <div class="separator"></div>
                
                <!-- Stats Summary -->
                <div class="sidebar-section" style="flex-weight: 1;">
                    <p class="stats-label">Allocated Stats</p>
                    <div id="StatsList" style="layout-mode: Top;">
                        <p style="font-size: 6; color: #666666;">No nodes allocated</p>
                    </div>
                </div>
                
                <div class="separator"></div>
                
                <!-- Action Buttons -->
                <div class="sidebar-section">
                    <button id="ResetTreeBtn" class="action-btn">Reset</button>
                    <button id="CloseBtn" class="action-btn">Close</button>
                </div>
            </div>
            
            <!-- MAIN TREE AREA (with native scrolling via TopScrolling layout mode) -->
            <div class="tree-area">
                <div id="TreeCanvas" style="anchor-width: {{$canvasWidth}}; anchor-height: {{$canvasHeight}};">
                    {{{connectionsHtml}}}
                    {{{nodesHtml}}}
                </div>
            </div>
        </div>
        """;
}
