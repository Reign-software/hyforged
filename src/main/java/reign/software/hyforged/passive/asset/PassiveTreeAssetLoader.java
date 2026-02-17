package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import reign.software.hyforged.passive.model.*;
import reign.software.hyforged.passive.registry.NodeIconTemplateRegistry;
import reign.software.hyforged.passive.registry.NodeVisualTemplateRegistry;
import reign.software.hyforged.passive.registry.PassiveTreeRegistry;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Handles loading passive tree assets from JSON files.
 * <p>
 * The passive tree system uses a multi-file, additive structure:
 * <ol>
 *   <li>Node Templates (nodes/) - Reusable node definitions without position</li>
 *   <li>Tree Definitions (trees/) - Metadata about trees</li>
 *   <li>Layout Files (layouts/) - Placements and connections (additive)</li>
 * </ol>
 * <p>
 * Load order:
 * 1. PassiveRefundConfigAsset (refund cost configuration)
 * 2. NodeTemplateFileAsset (node templates from all mods)
 * 3. PassiveTreeAsset (tree definitions)
 * 4. TreeLayoutAsset (layouts from all mods - merged additively)
 */
public final class PassiveTreeAssetLoader {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Path for tree definitions */
    public static final String TREE_PATH = "Hyforged/PassiveTrees/trees";

    /** Path for node templates */
    public static final String NODES_PATH = "Hyforged/PassiveTrees/nodes";

    /** Path for layout files */
    public static final String LAYOUTS_PATH = "Hyforged/PassiveTrees/layouts";

    /** Path for visual/icon templates */
    public static final String TEMPLATES_PATH = "Hyforged/PassiveTrees/templates";

    /** Path for passive refund config */
    public static final String CONFIG_PATH = "Hyforged/Config/PassiveRefund";

    private static boolean initialized = false;
    
    /** Track if node templates have been loaded */
    private static volatile boolean nodeTemplatesLoaded = false;
    
    /** Track if trees have been loaded */
    private static volatile boolean treesLoaded = false;

    /** Registered node templates by ID */
    private static final Map<String, NodeTemplateAsset> nodeTemplates = new ConcurrentHashMap<>();

    /** Track placeholder templates created for missing IDs */
    private static final Set<String> placeholderTemplateIds = ConcurrentHashMap.newKeySet();

    /** Pending layouts waiting for trees to be registered */
    private static final List<TreeLayoutAsset> pendingLayouts = Collections.synchronizedList(new ArrayList<>());

    private PassiveTreeAssetLoader() {
        // Utility class
    }

    /**
     * Initialize the asset stores and register event handlers.
     * Should be called during plugin setup.
     */
    public static void initialize(@Nonnull JavaPlugin plugin) {
        if (initialized) {
            LOGGER.atWarning().log("PassiveTreeAssetLoader already initialized");
            return;
        }

        LOGGER.atInfo().log("Initializing Hyforged passive tree asset loading (multi-file structure)...");

        // Register all asset stores
        registerRefundConfigAssetStore();
        registerVisualTemplateAssetStore();
        registerIconTemplateAssetStore();
        registerNodeTemplateAssetStore();
        registerPassiveTreeAssetStore();
        registerLayoutAssetStore();

        // Register event handlers in order (templates load first)
        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                NodeVisualTemplateAsset.class,
                PassiveTreeAssetLoader::onVisualTemplatesLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                NodeIconTemplateAsset.class,
                PassiveTreeAssetLoader::onIconTemplatesLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                PassiveRefundConfigAsset.class,
                PassiveTreeAssetLoader::onRefundConfigAssetsLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                NodeTemplateFileAsset.class,
                PassiveTreeAssetLoader::onNodeTemplatesLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                PassiveTreeAsset.class,
                PassiveTreeAssetLoader::onPassiveTreeAssetsLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                TreeLayoutAsset.class,
                PassiveTreeAssetLoader::onLayoutsLoaded
        );

        initialized = true;
        LOGGER.atInfo().log("Hyforged passive tree asset loading initialized");
    }

    // ========== Asset Store Registration ==========

    private static void registerNodeTemplateAssetStore() {
        AssetStore<String, NodeTemplateFileAsset, IndexedLookupTableAssetMap<String, NodeTemplateFileAsset>> store =
                ((HytaleAssetStore.Builder<String, NodeTemplateFileAsset, IndexedLookupTableAssetMap<String, NodeTemplateFileAsset>>)
                        ((HytaleAssetStore.Builder<String, NodeTemplateFileAsset, IndexedLookupTableAssetMap<String, NodeTemplateFileAsset>>)
                                ((HytaleAssetStore.Builder<String, NodeTemplateFileAsset, IndexedLookupTableAssetMap<String, NodeTemplateFileAsset>>)
                                        ((HytaleAssetStore.Builder<String, NodeTemplateFileAsset, IndexedLookupTableAssetMap<String, NodeTemplateFileAsset>>)
                                                HytaleAssetStore.builder(
                                                        NodeTemplateFileAsset.class,
                                                        new IndexedLookupTableAssetMap<>(NodeTemplateFileAsset[]::new)
                                                )
                                                        .setPath(NODES_PATH))
                                                .setReplaceOnRemove(key -> new NodeTemplateFileAsset()))
                                        .setCodec(NodeTemplateFileAsset.CODEC))
                                .setKeyFunction(NodeTemplateFileAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered NodeTemplateFileAsset store at path: %s", NODES_PATH);
    }

    private static void registerPassiveTreeAssetStore() {
        AssetStore<String, PassiveTreeAsset, IndexedLookupTableAssetMap<String, PassiveTreeAsset>> store =
                ((HytaleAssetStore.Builder<String, PassiveTreeAsset, IndexedLookupTableAssetMap<String, PassiveTreeAsset>>)
                        ((HytaleAssetStore.Builder<String, PassiveTreeAsset, IndexedLookupTableAssetMap<String, PassiveTreeAsset>>)
                                ((HytaleAssetStore.Builder<String, PassiveTreeAsset, IndexedLookupTableAssetMap<String, PassiveTreeAsset>>)
                                        ((HytaleAssetStore.Builder<String, PassiveTreeAsset, IndexedLookupTableAssetMap<String, PassiveTreeAsset>>)
                                                HytaleAssetStore.builder(
                                                        PassiveTreeAsset.class,
                                                        new IndexedLookupTableAssetMap<>(PassiveTreeAsset[]::new)
                                                )
                                                        .setPath(TREE_PATH))
                                                .setReplaceOnRemove(key -> new PassiveTreeAsset()))
                                        .setCodec(PassiveTreeAsset.CODEC))
                                .setKeyFunction(PassiveTreeAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered PassiveTreeAsset store at path: %s", TREE_PATH);
    }

    private static void registerLayoutAssetStore() {
        AssetStore<String, TreeLayoutAsset, IndexedLookupTableAssetMap<String, TreeLayoutAsset>> store =
                ((HytaleAssetStore.Builder<String, TreeLayoutAsset, IndexedLookupTableAssetMap<String, TreeLayoutAsset>>)
                        ((HytaleAssetStore.Builder<String, TreeLayoutAsset, IndexedLookupTableAssetMap<String, TreeLayoutAsset>>)
                                ((HytaleAssetStore.Builder<String, TreeLayoutAsset, IndexedLookupTableAssetMap<String, TreeLayoutAsset>>)
                                        ((HytaleAssetStore.Builder<String, TreeLayoutAsset, IndexedLookupTableAssetMap<String, TreeLayoutAsset>>)
                                                HytaleAssetStore.builder(
                                                        TreeLayoutAsset.class,
                                                        new IndexedLookupTableAssetMap<>(TreeLayoutAsset[]::new)
                                                )
                                                        .setPath(LAYOUTS_PATH))
                                                .setReplaceOnRemove(key -> new TreeLayoutAsset()))
                                        .setCodec(TreeLayoutAsset.CODEC))
                                .setKeyFunction(TreeLayoutAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered TreeLayoutAsset store at path: %s", LAYOUTS_PATH);
    }

    private static void registerRefundConfigAssetStore() {
        AssetStore<String, PassiveRefundConfigAsset, IndexedLookupTableAssetMap<String, PassiveRefundConfigAsset>> store =
                ((HytaleAssetStore.Builder<String, PassiveRefundConfigAsset, IndexedLookupTableAssetMap<String, PassiveRefundConfigAsset>>)
                        ((HytaleAssetStore.Builder<String, PassiveRefundConfigAsset, IndexedLookupTableAssetMap<String, PassiveRefundConfigAsset>>)
                                ((HytaleAssetStore.Builder<String, PassiveRefundConfigAsset, IndexedLookupTableAssetMap<String, PassiveRefundConfigAsset>>)
                                        ((HytaleAssetStore.Builder<String, PassiveRefundConfigAsset, IndexedLookupTableAssetMap<String, PassiveRefundConfigAsset>>)
                                                HytaleAssetStore.builder(
                                                        PassiveRefundConfigAsset.class,
                                                        new IndexedLookupTableAssetMap<>(PassiveRefundConfigAsset[]::new)
                                                )
                                                        .setPath(CONFIG_PATH))
                                                .setReplaceOnRemove(key -> new PassiveRefundConfigAsset()))
                                        .setCodec(PassiveRefundConfigAsset.CODEC))
                                .setKeyFunction(PassiveRefundConfigAsset::getId))
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered PassiveRefundConfigAsset store at path: %s", CONFIG_PATH);
    }

    private static void registerVisualTemplateAssetStore() {
        AssetStore<String, NodeVisualTemplateAsset, IndexedLookupTableAssetMap<String, NodeVisualTemplateAsset>> store =
                ((HytaleAssetStore.Builder<String, NodeVisualTemplateAsset, IndexedLookupTableAssetMap<String, NodeVisualTemplateAsset>>)
                        ((HytaleAssetStore.Builder<String, NodeVisualTemplateAsset, IndexedLookupTableAssetMap<String, NodeVisualTemplateAsset>>)
                                ((HytaleAssetStore.Builder<String, NodeVisualTemplateAsset, IndexedLookupTableAssetMap<String, NodeVisualTemplateAsset>>)
                                        ((HytaleAssetStore.Builder<String, NodeVisualTemplateAsset, IndexedLookupTableAssetMap<String, NodeVisualTemplateAsset>>)
                                                HytaleAssetStore.builder(
                                                        NodeVisualTemplateAsset.class,
                                                        new IndexedLookupTableAssetMap<>(NodeVisualTemplateAsset[]::new)
                                                )
                                                        .setPath(TEMPLATES_PATH))
                                                .setReplaceOnRemove(key -> new NodeVisualTemplateAsset()))
                                        .setCodec(NodeVisualTemplateAsset.CODEC))
                                .setKeyFunction(asset -> "frame-templates"))  // Single config file
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered NodeVisualTemplateAsset store at path: %s", TEMPLATES_PATH);
    }

    private static void registerIconTemplateAssetStore() {
        AssetStore<String, NodeIconTemplateAsset, IndexedLookupTableAssetMap<String, NodeIconTemplateAsset>> store =
                ((HytaleAssetStore.Builder<String, NodeIconTemplateAsset, IndexedLookupTableAssetMap<String, NodeIconTemplateAsset>>)
                        ((HytaleAssetStore.Builder<String, NodeIconTemplateAsset, IndexedLookupTableAssetMap<String, NodeIconTemplateAsset>>)
                                ((HytaleAssetStore.Builder<String, NodeIconTemplateAsset, IndexedLookupTableAssetMap<String, NodeIconTemplateAsset>>)
                                        ((HytaleAssetStore.Builder<String, NodeIconTemplateAsset, IndexedLookupTableAssetMap<String, NodeIconTemplateAsset>>)
                                                HytaleAssetStore.builder(
                                                        NodeIconTemplateAsset.class,
                                                        new IndexedLookupTableAssetMap<>(NodeIconTemplateAsset[]::new)
                                                )
                                                        .setPath(TEMPLATES_PATH))
                                                .setReplaceOnRemove(key -> new NodeIconTemplateAsset()))
                                        .setCodec(NodeIconTemplateAsset.CODEC))
                                .setKeyFunction(asset -> "icon-templates"))  // Single config file
                        .build();

        AssetRegistry.register(store);
        LOGGER.at(Level.FINE).log("Registered NodeIconTemplateAsset store at path: %s", TEMPLATES_PATH);
    }

    // ========== Event Handlers ==========

    /**
     * Handle visual template assets loaded event.
     */
    private static void onVisualTemplatesLoaded(
            LoadedAssetsEvent<String, NodeVisualTemplateAsset, IndexedLookupTableAssetMap<String, NodeVisualTemplateAsset>> event
    ) {
        LOGGER.atInfo().log("Loading node visual templates...");
        NodeVisualTemplateRegistry registry = NodeVisualTemplateRegistry.get();
        int count = 0;

        for (NodeVisualTemplateAsset asset : event.getLoadedAssets().values()) {
            // Register frame templates
            for (NodeVisualTemplateAsset.FrameTemplateEntry entry : asset.getFrameTemplates()) {
                NodeVisualTemplate template = NodeVisualTemplateAsset.toTemplate(entry);
                if (template != null) {
                    registry.register(template);
                    count++;
                }
            }
            // Register type defaults
            for (Map.Entry<String, String> typeDefault : asset.getTypeDefaults().entrySet()) {
                registry.setTypeDefault(typeDefault.getKey(), typeDefault.getValue());
            }
        }

        LOGGER.atInfo().log("Loaded %s node visual templates", count);
    }

    /**
     * Handle icon template assets loaded event.
     */
    private static void onIconTemplatesLoaded(
            LoadedAssetsEvent<String, NodeIconTemplateAsset, IndexedLookupTableAssetMap<String, NodeIconTemplateAsset>> event
    ) {
        LOGGER.atInfo().log("Loading node icon templates...");
        NodeIconTemplateRegistry registry = NodeIconTemplateRegistry.get();
        int count = 0;

        for (NodeIconTemplateAsset asset : event.getLoadedAssets().values()) {
            for (NodeIconTemplateAsset.IconTemplateEntry entry : asset.getIconTemplates()) {
                NodeIconTemplate template = NodeIconTemplateAsset.toTemplate(entry);
                if (template != null) {
                    registry.register(template);
                    count++;
                }
            }
        }

        LOGGER.atInfo().log("Loaded %s node icon templates", count);
    }

    /**
     * Handle refund config assets loaded event.
     */
    private static void onRefundConfigAssetsLoaded(
            LoadedAssetsEvent<String, PassiveRefundConfigAsset, IndexedLookupTableAssetMap<String, PassiveRefundConfigAsset>> event
    ) {
        LOGGER.atInfo().log("Loading passive refund configuration...");

        PassiveTreeRegistry registry = PassiveTreeRegistry.get();
        PassiveRefundConfigAsset selected = null;

        for (PassiveRefundConfigAsset asset : event.getLoadedAssets().values()) {
            try {
                if (selected == null || "hyforged:passive-refund-config".equals(asset.getId())) {
                    selected = asset;
                }
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to evaluate refund config: %s", asset.getId());
            }
        }

        if (selected != null) {
            try {
                registry.setRefundConfig(selected);
                LOGGER.at(Level.FINE).log("Loaded refund config: %s (BaseCost=%s, LevelMult=%s)",
                        selected.getId(), selected.getBaseCost(), selected.getLevelMultiplier());
                LOGGER.atInfo().log("Loaded refund config: %s", selected.getId());
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to load refund config: %s", selected.getId());
            }
        } else {
            LOGGER.atWarning().log("No refund config assets found in Hyforged/Config");
        }
    }

    /**
     * Handle node template files loaded event.
     */
    private static void onNodeTemplatesLoaded(
            LoadedAssetsEvent<String, NodeTemplateFileAsset, IndexedLookupTableAssetMap<String, NodeTemplateFileAsset>> event
    ) {
        LOGGER.atInfo().log("Loading passive node templates...");

        int loaded = 0;
        int skipped = 0;

        for (NodeTemplateFileAsset file : event.getLoadedAssets().values()) {
            for (NodeTemplateAsset template : file.getNodes()) {
                String nodeId = template.getId();

                if (nodeTemplates.containsKey(nodeId)) {
                    LOGGER.atWarning().log("Duplicate node template ID '%s' - skipping", nodeId);
                    skipped++;
                    continue;
                }

                nodeTemplates.put(nodeId, template);
                loaded++;
                LOGGER.at(Level.FINE).log("Registered node template: %s", nodeId);
            }
        }

        LOGGER.atInfo().log("Loaded %s node templates%s", loaded,
                (skipped > 0 ? " (" + skipped + " skipped due to duplicates)" : ""));
        
        nodeTemplatesLoaded = true;
        tryProcessPendingLayouts();
    }

    /**
     * Handle passive tree definition assets loaded event.
     */
    private static void onPassiveTreeAssetsLoaded(
            LoadedAssetsEvent<String, PassiveTreeAsset, IndexedLookupTableAssetMap<String, PassiveTreeAsset>> event
    ) {
        LOGGER.atInfo().log("Loading passive tree definitions...");

        PassiveTreeRegistry registry = PassiveTreeRegistry.get();
        int loaded = 0;
        int skipped = 0;

        for (PassiveTreeAsset asset : event.getLoadedAssets().values()) {
            String id = asset.getId();

            // Check for conflicts
            if (registry.hasTree(id)) {
                LOGGER.atWarning().log("Duplicate passive tree ID '%s' - skipping", id);
                skipped++;
                continue;
            }

            try {
                // Create empty tree (nodes/connections added by layouts)
                PassiveTree tree = PassiveTree.builder(id)
                        .treeType(asset.getTreeType())
                        .classId(asset.getClassId())
                        .version(asset.getVersion())
                        .build();

                registry.register(tree);
                loaded++;

                LOGGER.at(Level.FINE).log("Registered passive tree: %s (type=%s)", id, asset.getTreeType());
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to load passive tree: %s", id);
                skipped++;
            }
        }

        LOGGER.atInfo().log("Loaded %s passive trees%s", loaded,
                (skipped > 0 ? " (" + skipped + " skipped due to errors/conflicts)" : ""));

        // Mark trees as loaded and try to process pending layouts
        treesLoaded = true;
        tryProcessPendingLayouts();
    }
    
    /**
     * Attempt to process pending layouts if all prerequisites are loaded.
     * <p>
     * Layouts require both node templates AND tree definitions to be loaded first.
     * This method is called after both events and only processes when ready.
     */
    private static synchronized void tryProcessPendingLayouts() {
        if (!nodeTemplatesLoaded) {
            LOGGER.at(Level.FINE).log("Waiting for node templates to load before processing layouts");
            return;
        }
        if (!treesLoaded) {
            LOGGER.at(Level.FINE).log("Waiting for trees to load before processing layouts");
            return;
        }
        
        // Both prerequisites loaded - process any pending layouts
        processPendingLayouts();
    }

    /**
     * Handle layout files loaded event.
     * Uses a two-pass approach: first apply all placements and starting nodes,
     * then apply all connections. This ensures nodes from one layout exist
     * before connections in another layout reference them.
     */
    private static void onLayoutsLoaded(
            LoadedAssetsEvent<String, TreeLayoutAsset, IndexedLookupTableAssetMap<String, TreeLayoutAsset>> event
    ) {
        LOGGER.atInfo().log("Loading passive tree layouts...");

        PassiveTreeRegistry registry = PassiveTreeRegistry.get();
        List<TreeLayoutAsset> validLayouts = new ArrayList<>();
        int deferred = 0;

        // First pass: apply placements and starting nodes for all layouts
        for (TreeLayoutAsset layout : event.getLoadedAssets().values()) {
            String treeId = layout.getTreeId();

            // Check if tree exists
            if (!registry.hasTree(treeId)) {
                LOGGER.at(Level.FINE).log("Deferring layout for tree '%s' (tree not yet loaded)", treeId);
                pendingLayouts.add(layout);
                deferred++;
                continue;
            }

            try {
                applyLayoutPlacements(layout);
                validLayouts.add(layout);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to apply layout placements: %s", layout.getId());
            }
        }

        // Second pass: apply connections (now all nodes should exist)
        for (TreeLayoutAsset layout : validLayouts) {
            try {
                applyLayoutConnections(layout);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to apply layout connections: %s", layout.getId());
            }
        }

        LOGGER.atInfo().log("Applied %s layouts%s", validLayouts.size(),
                (deferred > 0 ? " (" + deferred + " deferred)" : ""));
    }

    /**
     * Process any layouts that were deferred waiting for trees.
     * Uses a two-pass approach: first process all placements and starting nodes,
     * then process all connections. This ensures nodes exist before connections reference them.
     */
    private static void processPendingLayouts() {
        if (pendingLayouts.isEmpty()) {
            return;
        }

        LOGGER.atInfo().log("Processing %s deferred layouts...", pendingLayouts.size());

        PassiveTreeRegistry registry = PassiveTreeRegistry.get();
        List<TreeLayoutAsset> stillPending = new ArrayList<>();
        List<TreeLayoutAsset> validLayouts = new ArrayList<>();
        int processed = 0;

        // First pass: validate layouts and apply placements/starting nodes
        for (TreeLayoutAsset layout : pendingLayouts) {
            String treeId = layout.getTreeId();

            if (!registry.hasTree(treeId)) {
                LOGGER.atWarning().log("Layout references unknown tree: %s", treeId);
                stillPending.add(layout);
                continue;
            }

            try {
                applyLayoutPlacements(layout);
                validLayouts.add(layout);
                processed++;
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to apply deferred layout placements: %s", layout.getId());
            }
        }

        // Second pass: apply connections (now all nodes should exist)
        for (TreeLayoutAsset layout : validLayouts) {
            try {
                applyLayoutConnections(layout);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to apply deferred layout connections: %s", layout.getId());
            }
        }

        pendingLayouts.clear();
        pendingLayouts.addAll(stillPending);

        LOGGER.atInfo().log("Processed %s deferred layouts%s", processed,
                (!stillPending.isEmpty() ? " (" + stillPending.size() + " failed)" : ""));
    }

    /**
     * Apply placements and starting nodes from a layout to its target tree.
     */
    private static void applyLayoutPlacements(TreeLayoutAsset layout) {
        PassiveTreeRegistry registry = PassiveTreeRegistry.get();
        String treeId = layout.getTreeId();
        PassiveTree tree = registry.getTree(treeId);

        if (tree == null) {
            throw new IllegalStateException("Tree not found: " + treeId);
        }

        // Process placements (add nodes)
        for (NodePlacementAsset placement : layout.getPlacements()) {
            String templateId = placement.getNodeId();
            NodeTemplateAsset template = nodeTemplates.get(templateId);

            if (template == null) {
                NodeTemplateAsset placeholder = NodeTemplateAsset.createPlaceholder(templateId);
                NodeTemplateAsset existing = nodeTemplates.putIfAbsent(templateId, placeholder);
                template = existing != null ? existing : placeholder;

                if (existing == null) {
                    placeholderTemplateIds.add(templateId);
                }
            }

            String effectiveId = placement.getEffectiveId();

            // Check if node already exists (refetch tree to get latest state)
            tree = registry.getTree(treeId);
            if (tree.getNode(effectiveId) != null) {
                LOGGER.at(Level.FINE).log("Node already exists in tree: %s", effectiveId);
                continue;
            }

            // Build the node from template + placement
            PassiveNode node = buildNodeFromTemplate(template, placement);

            try {
                registry.addNode(treeId, node);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to add node %s to tree %s", effectiveId, treeId);
            }
        }

        // Refetch tree to get all added nodes before processing starting nodes
        tree = registry.getTree(treeId);

        // Collect starting nodes from both the array and placement IsStarting flags
        Set<String> startingNodeIds = new HashSet<>(layout.getStartingNodes());
        for (NodePlacementAsset placement : layout.getPlacements()) {
            if (placement.isStarting()) {
                startingNodeIds.add(placement.getEffectiveId());
            }
        }

        // Process starting nodes
        for (String startingNodeId : startingNodeIds) {
            if (tree.getStartingNodeIds().contains(startingNodeId)) {
                continue;
            }

            try {
                // Add to starting nodes (requires tree rebuild)
                PassiveTree updatedTree = PassiveTree.builder(treeId)
                        .treeType(tree.getTreeType())
                        .classId(tree.getClassId())
                        .addStartingNodes(tree.getStartingNodeIds())
                        .addStartingNode(startingNodeId)
                        .addNodes(tree.getNodes().values())
                        .addConnections(tree.getConnections())
                        .addTextLabels(tree.getTextLabels())
                        .version(tree.getVersion())
                        .build();

                // Update registry
                registry.replaceTree(tree, updatedTree);
                tree = updatedTree;
                LOGGER.atInfo().log("Added starting node '%s' to tree '%s'. Total starting nodes: %s", startingNodeId, treeId, tree.getStartingNodeIds().size());
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to add starting node %s", startingNodeId);
            }
        }

        // Process text labels
        List<TextLabelAsset> labelAssets = layout.getTextLabels();
        if (!labelAssets.isEmpty()) {
            List<TextLabel> existingLabels = new ArrayList<>(tree.getTextLabels());
            
            for (TextLabelAsset labelAsset : labelAssets) {
                if (labelAsset.getPosition() == null) {
                    LOGGER.atWarning().log("Text label missing position: %s", labelAsset.getText());
                    continue;
                }
                
                TextLabel label = new TextLabel(
                    labelAsset.getText(),
                    labelAsset.getPosition().getX(),
                    labelAsset.getPosition().getY(),
                    labelAsset.getFontSize(),
                    labelAsset.getColor(),
                    labelAsset.getAnchor(),
                    labelAsset.getRegion(),
                    labelAsset.getFontWeight(),
                    labelAsset.getOpacity(),
                    labelAsset.getRotation()
                );
                existingLabels.add(label);
            }
            
            // Rebuild tree with updated labels
            PassiveTree updatedTree = PassiveTree.builder(treeId)
                    .treeType(tree.getTreeType())
                    .classId(tree.getClassId())
                    .addStartingNodes(tree.getStartingNodeIds())
                    .addNodes(tree.getNodes().values())
                    .addConnections(tree.getConnections())
                    .addTextLabels(existingLabels)
                    .version(tree.getVersion())
                    .build();
            
            registry.replaceTree(tree, updatedTree);
            tree = updatedTree;
            LOGGER.at(Level.FINE).log("Added %s text labels to tree %s", labelAssets.size(), treeId);
        }

        LOGGER.at(Level.FINE).log("Applied placements from layout %s to tree %s", layout.getId(), treeId);
    }

    /**
     * Apply connections from a layout to its target tree.
     */
    private static void applyLayoutConnections(TreeLayoutAsset layout) {
        PassiveTreeRegistry registry = PassiveTreeRegistry.get();
        String treeId = layout.getTreeId();

        // Process connections
        for (PassiveConnectionAsset conn : layout.getConnections()) {
            String fromId = conn.getFrom();
            String toId = conn.getTo();

            try {
                registry.addConnection(treeId, fromId, toId);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to add connection %s -> %s", fromId, toId);
            }
        }

        LOGGER.at(Level.FINE).log("Applied connections from layout %s to tree %s", layout.getId(), treeId);
    }
    
    /**
     * Build a PassiveNode from a template and placement.
     */
    private static PassiveNode buildNodeFromTemplate(NodeTemplateAsset template, NodePlacementAsset placement) {
        // Position from placement
        PassiveNodePosition position;
        if (placement.getPosition() != null) {
            position = new PassiveNodePosition(
                    placement.getPosition().getX(),
                    placement.getPosition().getY()
            );
        } else {
            position = PassiveNodePosition.ORIGIN;
        }

        // Effects from template
        List<PassiveNodeEffect> effects = new ArrayList<>();
        for (PassiveNodeEffectAsset effectAsset : template.getEffects()) {
            effects.add(convertEffect(effectAsset));
        }

        return new PassiveNode(
                placement.getEffectiveId(),
                template.getType(),
                template.getName(),
                template.getDescription(),
                template.getFrameTemplate(),
                template.getIcon(),
                template.getLabel(),
                position,
                placement.getRegion(),
                effects,
                PassiveNodeRequirements.NONE, // Requirements removed
                template.getKeystoneFamily()
        );
    }

    /**
     * Convert an effect asset to a PassiveNodeEffect model.
     */
    private static PassiveNodeEffect convertEffect(PassiveNodeEffectAsset asset) {
        Map<String, Object> data = new HashMap<>();

        if (asset.getStat() != null) {
            data.put("Stat", asset.getStat());
        }
        if (asset.getValue() != null) {
            data.put("Value", asset.getValue());
        }
        if (asset.getSpellId() != null) {
            data.put("SpellId", asset.getSpellId());
        }
        if (asset.getFlagId() != null) {
            data.put("FlagId", asset.getFlagId());
        }
        if (asset.getChoices() != null && !asset.getChoices().isEmpty()) {
            List<PassiveNodeEffect> choices = new ArrayList<>();
            for (PassiveNodeEffectAsset choice : asset.getChoices()) {
                choices.add(convertEffect(choice));
            }
            data.put("Choices", choices);
        }

        return new PassiveNodeEffect(asset.getType(), data);
    }

    /**
     * Check if the asset loader has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Reset initialization state (for testing).
     */
    public static void resetForTesting() {
        initialized = false;
        nodeTemplates.clear();
        pendingLayouts.clear();
    }

    /**
     * Get all registered node templates (for testing).
     */
    public static Map<String, NodeTemplateAsset> getNodeTemplates() {
        return Collections.unmodifiableMap(nodeTemplates);
    }
}
