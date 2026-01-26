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
    private static final int SIDEBAR_WIDTH = 220;
    private static final float COORD_SCALE = 2.0f;
    
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
        
        // Gather allocation data
        Set<String> allocatedNodes = getAllocatedNodes(component, tree);
        Set<String> reachableNodes = getReachableNodes(tree, allocatedNodes);
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
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        for (PassiveNode node : tree.getNodes().values()) {
            minX = Math.min(minX, node.position().x());
            minY = Math.min(minY, node.position().y());
        }
        int treePadding = 100;
        minX -= treePadding;
        minY -= treePadding;
        
        String connectionsHtml = buildConnectionsHtml(tree, allocatedNodes, reachableNodes, minX, minY);
        
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
        Set<String> renderedNodeIds = new HashSet<>();
        
        // Find tree bounds
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (PassiveNode node : tree.getNodes().values()) {
            minX = Math.min(minX, node.position().x());
            maxX = Math.max(maxX, node.position().x());
            minY = Math.min(minY, node.position().y());
            maxY = Math.max(maxY, node.position().y());
        }
        
        // Add padding around tree
        int treePadding = 100;
        minX -= treePadding;
        minY -= treePadding;
        maxX += treePadding;
        maxY += treePadding;
        
        // Calculate canvas size (scale tree coords to pixels)
        int canvasWidth = (int) ((maxX - minX) * COORD_SCALE);
        int canvasHeight = (int) ((maxY - minY) * COORD_SCALE);
        
        for (PassiveNode node : tree.getNodes().values()) {
            // Apply search filter
            if (!searchMatchNodes.isEmpty() && !searchMatchNodes.contains(node.id())) {
                continue;
            }
            
            int nodeX = node.position().x();
            int nodeY = node.position().y();
            
            // Calculate screen position (offset from minX/minY, scaled)
            int screenX = (int) ((nodeX - minX) * COORD_SCALE);
            int screenY = (int) ((nodeY - minY) * COORD_SCALE);
            
            boolean isAllocated = allocatedNodes.contains(node.id());
            boolean isReachable = reachableNodes.contains(node.id()) || startingNodes.contains(node.id());
            
            int nodeSize = getNodeSize(node.type());
            String nodeImage = getNodeImage(node.type(), isAllocated, isReachable);
            String nodeElementId = sanitizeId(node.id());
            
            // Build rich tooltip text
            String tooltipText = buildTooltipText(node, isAllocated, isReachable);
            
            // Center the node on its position
            int posX = screenX - nodeSize / 2;
            int posY = screenY - nodeSize / 2;
            
            // Use div with background-image for clickable node with texture
            sb.append(String.format(
                "<div id=\"%s\" style=\"anchor-left: %d; anchor-top: %d; anchor-width: %d; anchor-height: %d; background-image: url('%s');\" data-hyui-tooltiptext=\"%s\"></div>",
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
     * Build a detailed tooltip text for a node.
     */
    private String buildTooltipText(@Nonnull PassiveNode node, boolean isAllocated, boolean canAllocate) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.name()).append(" (").append(formatNodeType(node.type())).append(")");
        
        if (node.description() != null && !node.description().isEmpty()) {
            sb.append("\n").append(node.description());
        }
        
        if (!node.effects().isEmpty()) {
            sb.append("\n---");
            for (PassiveNodeEffect effect : node.effects()) {
                sb.append("\n").append(formatEffect(effect));
            }
        }
        
        sb.append("\n---\n");
        if (isAllocated) {
            sb.append("[ALLOCATED] Click to refund");
        } else if (canAllocate) {
            sb.append("[AVAILABLE] Click to allocate");
        } else {
            sb.append("[LOCKED] Allocate connected nodes first");
        }
        
        return sb.toString();
    }
    
    /**
     * Build HTML for connection lines between nodes.
     */
    private String buildConnectionsHtml(
            @Nonnull PassiveTree tree,
            @Nonnull Set<String> allocatedNodes,
            @Nonnull Set<String> reachableNodes,
            int minX, int minY
    ) {
        StringBuilder sb = new StringBuilder();
        Set<String> startingNodes = tree.getStartingNodeIds();
        Set<String> renderedConnections = new HashSet<>();
        
        for (PassiveNode node : tree.getNodes().values()) {
            int nodeX = node.position().x();
            int nodeY = node.position().y();
            
            Set<String> adjacentNodes = tree.getAdjacentNodes(node.id());
            
            for (String targetId : adjacentNodes) {
                String connKey = node.id().compareTo(targetId) < 0 ?
                    node.id() + "-" + targetId : targetId + "-" + node.id();
                if (renderedConnections.contains(connKey)) continue;
                renderedConnections.add(connKey);
                
                PassiveNode targetNode = tree.getNode(targetId);
                if (targetNode == null) continue;
                
                int targetX = targetNode.position().x();
                int targetY = targetNode.position().y();
                
                int screenX1 = (int) ((nodeX - minX) * COORD_SCALE);
                int screenY1 = (int) ((nodeY - minY) * COORD_SCALE);
                int screenX2 = (int) ((targetX - minX) * COORD_SCALE);
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
                
                // Draw straight line using a rotated div or just horizontal/vertical segments
                // For simplicity, draw L-shaped connections
                if (Math.abs(screenX2 - screenX1) > 2) {
                    int hMinX = Math.min(screenX1, screenX2);
                    int hWidth = Math.abs(screenX2 - screenX1);
                    sb.append(String.format(
                        "<div style=\"anchor-left: %d; anchor-top: %d; anchor-width: %d; anchor-height: %d; background-color: %s;\"></div>",
                        hMinX, screenY1 - thickness/2, hWidth, thickness, lineColor
                    ));
                }
                
                if (Math.abs(screenY2 - screenY1) > 2) {
                    int vMinY = Math.min(screenY1, screenY2);
                    int vHeight = Math.abs(screenY2 - screenY1);
                    sb.append(String.format(
                        "<div style=\"anchor-left: %d; anchor-top: %d; anchor-width: %d; anchor-height: %d; background-color: %s;\"></div>",
                        screenX2 - thickness/2, vMinY, thickness, vHeight, lineColor
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
        
        for (String nodeId : renderedNodeIds) {
            PassiveNode node = tree.getNode(nodeId);
            if (node == null) continue;
            
            String nodeElementId = sanitizeId(nodeId);
            boolean isAllocated = allocatedNodes.contains(nodeId);
            boolean isReachable = reachableNodes.contains(nodeId) || startingNodes.contains(nodeId);
            
            // Click to allocate or refund
            final String finalNodeId = nodeId;
            builder.addEventListener(nodeElementId, CustomUIEventBindingType.Activating, (data, ctx) -> {
                if (isAllocated) {
                    handleRefund(finalNodeId);
                } else if (isReachable) {
                    handleAllocate(finalNodeId);
                }
            });
            
            // Hover for tooltip
            builder.addEventListener(nodeElementId, CustomUIEventBindingType.MouseEntered, (data, ctx) -> {
                showTooltip(node, isAllocated, isReachable);
            });
            
            builder.addEventListener(nodeElementId, CustomUIEventBindingType.MouseExited, (data, ctx) -> {
                hideTooltip();
            });
        }
    }
    
    // ========== ACTION HANDLERS ==========
    
    private void handleAllocate(@Nonnull String nodeId) {
        String activeTreeId = getActiveTreeId();
        var result = PassiveTreeService.get().allocateNode(entityRef, activeTreeId, nodeId);
        if (result.success()) {
            rebuild();
        } else {
            LOGGER.fine("Allocation failed: " + result.reason());
        }
    }
    
    private void handleRefund(@Nonnull String nodeId) {
        String activeTreeId = getActiveTreeId();
        var result = PassiveTreeService.get().refundNode(entityRef, activeTreeId, nodeId);
        if (result.success()) {
            rebuild();
        } else {
            LOGGER.fine("Refund failed: " + result.reason());
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
        for (String allocatedId : allocatedNodes) {
            reachable.addAll(tree.getAdjacentNodes(allocatedId));
        }
        reachable.addAll(tree.getStartingNodeIds());
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
    
    private String formatEffect(@Nonnull PassiveNodeEffect effect) {
        var data = effect.data();
        return switch (effect.type()) {
            case "stat-modifier" -> {
                String stat = String.valueOf(data.getOrDefault("Stat", data.getOrDefault("stat", "?")));
                String value = String.valueOf(data.getOrDefault("Value", data.getOrDefault("value", "0")));
                String mod = String.valueOf(data.getOrDefault("Modifier", data.getOrDefault("modifier", "flat")));
                String prefix = value.startsWith("-") ? "" : "+";
                String suffix = "percent".equalsIgnoreCase(mod) ? "%" : "";
                yield prefix + value + suffix + " " + stat.replace("_", " ");
            }
            case "spell-grant" -> "Grants: " + data.getOrDefault("SpellId", data.getOrDefault("spell", "?"));
            case "unlock-flag" -> "Unlocks: " + data.getOrDefault("Description", data.getOrDefault("FlagId", "?"));
            default -> effect.type() + ": " + data;
        };
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
            .sidebar {
                anchor-left: 0;
                anchor-top: 0;
                anchor-bottom: 0;
                anchor-width: 220;
                layout-mode: Top;
                background-color: #121215;
                padding: 8;
            }
            
            .sidebar-section {
                layout-mode: Top;
                padding-top: 8;
                padding-bottom: 8;
            }
            
            .tree-tabs {
                layout-mode: Left;
                anchor-height: 28;
                padding-bottom: 6;
            }
            
            .separator {
                anchor-height: 1;
                background-color: #404050;
            }
            
            .points-label {
                font-size: 12;
                color: #888888;
            }
            
            .points-value {
                font-size: 14;
                font-weight: bold;
                color: #ffffff;
            }
            
            .stats-label {
                font-size: 11;
                color: #888888;
            }
            
            .tree-area {
                anchor-left: 220;
                anchor-top: 0;
                anchor-right: 0;
                anchor-bottom: 0;
                background-color: #0a0a0f;
            }
            
            .tree-canvas {
                anchor-width: {{$canvasWidth}};
                anchor-height: {{$canvasHeight}};
            }
        </style>
        
        <div class="page-overlay" style="background-color: #1a1a1e;">
            <!-- LEFT SIDEBAR -->
            <div class="sidebar">
                <!-- Tree Type Tabs -->
                <div class="tree-tabs">
                    <button id="GeneralTreeBtn">General</button>
                    <button id="ClassTreeBtn">Class</button>
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
                <div class="sidebar-section" style="layout-mode: Left; anchor-height: 28;">
                    <input type="text" id="SearchInput" value="" placeholder="Search nodes..." 
                           style="anchor-width: 160;" />
                    <button id="SearchBtn">Go</button>
                </div>
                
                <div class="separator"></div>
                
                <!-- Stats Summary -->
                <div class="sidebar-section" style="flex-weight: 1;">
                    <p class="stats-label">Allocated Stats</p>
                    <div id="StatsList" style="layout-mode: Top;">
                        <p style="font-size: 10; color: #666666;">No nodes allocated</p>
                    </div>
                </div>
                
                <div class="separator"></div>
                
                <!-- Action Buttons -->
                <div class="sidebar-section">
                    <button id="ResetTreeBtn">Reset Tree</button>
                    <button id="CloseBtn">Close</button>
                </div>
            </div>
            
            <!-- MAIN TREE AREA -->
            <div class="tree-area">
                <div id="TreeCanvas" class="tree-canvas">
                    {{{connectionsHtml}}}
                    {{{nodesHtml}}}
                </div>
            </div>
        </div>
        """;
}
