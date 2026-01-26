package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import reign.software.hyforged.passive.model.*;
import reign.software.hyforged.passive.registry.PassiveTreeRegistry;
import reign.software.hyforged.passive.registry.StatIconRegistry;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger LOGGER = Logger.getLogger(PassiveTreeAssetLoader.class.getName());

    /** Path for tree definitions */
    public static final String TREE_PATH = "Hyforged/PassiveTrees/trees";

    /** Path for node templates */
    public static final String NODES_PATH = "Hyforged/PassiveTrees/nodes";

    /** Path for layout files */
    public static final String LAYOUTS_PATH = "Hyforged/PassiveTrees/layouts";

    /** Path for passive refund config */
    public static final String CONFIG_PATH = "Hyforged/Config";

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
            LOGGER.warning("PassiveTreeAssetLoader already initialized");
            return;
        }

        LOGGER.info("Initializing Hyforged passive tree asset loading (multi-file structure)...");

        // Register all asset stores
        registerRefundConfigAssetStore();
        registerStatIconConfigAssetStore();
        registerNodeTemplateAssetStore();
        registerPassiveTreeAssetStore();
        registerLayoutAssetStore();

        // Register event handlers in order
        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                PassiveRefundConfigAsset.class,
                PassiveTreeAssetLoader::onRefundConfigAssetsLoaded
        );

        plugin.getEventRegistry().register(
                LoadedAssetsEvent.class,
                StatIconConfigAsset.class,
                PassiveTreeAssetLoader::onStatIconConfigLoaded
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
        LOGGER.info("Hyforged passive tree asset loading initialized");
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
        LOGGER.fine("Registered NodeTemplateFileAsset store at path: " + NODES_PATH);
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
        LOGGER.fine("Registered PassiveTreeAsset store at path: " + TREE_PATH);
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
        LOGGER.fine("Registered TreeLayoutAsset store at path: " + LAYOUTS_PATH);
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
        LOGGER.fine("Registered PassiveRefundConfigAsset store at path: " + CONFIG_PATH);
    }

    private static void registerStatIconConfigAssetStore() {
        AssetStore<String, StatIconConfigAsset, IndexedLookupTableAssetMap<String, StatIconConfigAsset>> store =
                ((HytaleAssetStore.Builder<String, StatIconConfigAsset, IndexedLookupTableAssetMap<String, StatIconConfigAsset>>)
                        ((HytaleAssetStore.Builder<String, StatIconConfigAsset, IndexedLookupTableAssetMap<String, StatIconConfigAsset>>)
                                ((HytaleAssetStore.Builder<String, StatIconConfigAsset, IndexedLookupTableAssetMap<String, StatIconConfigAsset>>)
                                        ((HytaleAssetStore.Builder<String, StatIconConfigAsset, IndexedLookupTableAssetMap<String, StatIconConfigAsset>>)
                                                HytaleAssetStore.builder(
                                                        StatIconConfigAsset.class,
                                                        new IndexedLookupTableAssetMap<>(StatIconConfigAsset[]::new)
                                                )
                                                        .setPath(CONFIG_PATH))
                                                .setReplaceOnRemove(key -> new StatIconConfigAsset()))
                                        .setCodec(StatIconConfigAsset.CODEC))
                                .setKeyFunction(asset -> "stat-icons"))  // Single config file
                        .build();

        AssetRegistry.register(store);
        LOGGER.fine("Registered StatIconConfigAsset store at path: " + CONFIG_PATH);
    }

    // ========== Event Handlers ==========

    /**
     * Handle stat icon config loaded event.
     */
    private static void onStatIconConfigLoaded(
            LoadedAssetsEvent<String, StatIconConfigAsset, IndexedLookupTableAssetMap<String, StatIconConfigAsset>> event
    ) {
        LOGGER.info("Loading stat icon configuration...");

        StatIconConfigAsset selected = null;
        for (StatIconConfigAsset asset : event.getLoadedAssets().values()) {
            selected = asset;  // Take the last one (or the only one)
        }

        if (selected != null) {
            StatIconRegistry.get().load(selected);
            LOGGER.info("Loaded stat icon configuration");
        } else {
            LOGGER.info("No stat icon config found, using defaults");
            StatIconRegistry.get().loadDefaults();
        }
    }

    /**
     * Handle refund config assets loaded event.
     */
    private static void onRefundConfigAssetsLoaded(
            LoadedAssetsEvent<String, PassiveRefundConfigAsset, IndexedLookupTableAssetMap<String, PassiveRefundConfigAsset>> event
    ) {
        LOGGER.info("Loading passive refund configuration...");

        PassiveTreeRegistry registry = PassiveTreeRegistry.get();
        PassiveRefundConfigAsset selected = null;

        for (PassiveRefundConfigAsset asset : event.getLoadedAssets().values()) {
            try {
                if (selected == null || "hyforged:passive-refund-config".equals(asset.getId())) {
                    selected = asset;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to evaluate refund config: " + asset.getId(), e);
            }
        }

        if (selected != null) {
            try {
                registry.setRefundConfig(selected);
                LOGGER.fine("Loaded refund config: " + selected.getId() +
                        " (BaseCost=" + selected.getBaseCost() +
                        ", LevelMult=" + selected.getLevelMultiplier() + ")");
                LOGGER.info("Loaded refund config: " + selected.getId());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to load refund config: " + selected.getId(), e);
            }
        } else {
            LOGGER.warning("No refund config assets found in Hyforged/Config");
        }
    }

    /**
     * Handle node template files loaded event.
     */
    private static void onNodeTemplatesLoaded(
            LoadedAssetsEvent<String, NodeTemplateFileAsset, IndexedLookupTableAssetMap<String, NodeTemplateFileAsset>> event
    ) {
        LOGGER.info("Loading passive node templates...");

        int loaded = 0;
        int skipped = 0;

        for (NodeTemplateFileAsset file : event.getLoadedAssets().values()) {
            for (NodeTemplateAsset template : file.getNodes()) {
                String nodeId = template.getId();

                if (nodeTemplates.containsKey(nodeId)) {
                    LOGGER.warning("Duplicate node template ID '" + nodeId + "' - skipping");
                    skipped++;
                    continue;
                }

                nodeTemplates.put(nodeId, template);
                loaded++;
                LOGGER.fine("Registered node template: " + nodeId);
            }
        }

        LOGGER.info("Loaded " + loaded + " node templates" +
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
        LOGGER.info("Loading passive tree definitions...");

        PassiveTreeRegistry registry = PassiveTreeRegistry.get();
        int loaded = 0;
        int skipped = 0;

        for (PassiveTreeAsset asset : event.getLoadedAssets().values()) {
            String id = asset.getId();

            // Check for conflicts
            if (registry.hasTree(id)) {
                LOGGER.warning("Duplicate passive tree ID '" + id + "' - skipping");
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

                LOGGER.fine("Registered passive tree: " + id + " (type=" + asset.getTreeType() + ")");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to load passive tree: " + id, e);
                skipped++;
            }
        }

        LOGGER.info("Loaded " + loaded + " passive trees" +
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
            LOGGER.fine("Waiting for node templates to load before processing layouts");
            return;
        }
        if (!treesLoaded) {
            LOGGER.fine("Waiting for trees to load before processing layouts");
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
        LOGGER.info("Loading passive tree layouts...");

        PassiveTreeRegistry registry = PassiveTreeRegistry.get();
        List<TreeLayoutAsset> validLayouts = new ArrayList<>();
        int deferred = 0;

        // First pass: apply placements and starting nodes for all layouts
        for (TreeLayoutAsset layout : event.getLoadedAssets().values()) {
            String treeId = layout.getTreeId();

            // Check if tree exists
            if (!registry.hasTree(treeId)) {
                LOGGER.fine("Deferring layout for tree '" + treeId + "' (tree not yet loaded)");
                pendingLayouts.add(layout);
                deferred++;
                continue;
            }

            try {
                applyLayoutPlacements(layout);
                validLayouts.add(layout);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to apply layout placements: " + layout.getId(), e);
            }
        }

        // Second pass: apply connections (now all nodes should exist)
        for (TreeLayoutAsset layout : validLayouts) {
            try {
                applyLayoutConnections(layout);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to apply layout connections: " + layout.getId(), e);
            }
        }

        LOGGER.info("Applied " + validLayouts.size() + " layouts" +
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

        LOGGER.info("Processing " + pendingLayouts.size() + " deferred layouts...");

        PassiveTreeRegistry registry = PassiveTreeRegistry.get();
        List<TreeLayoutAsset> stillPending = new ArrayList<>();
        List<TreeLayoutAsset> validLayouts = new ArrayList<>();
        int processed = 0;

        // First pass: validate layouts and apply placements/starting nodes
        for (TreeLayoutAsset layout : pendingLayouts) {
            String treeId = layout.getTreeId();

            if (!registry.hasTree(treeId)) {
                LOGGER.warning("Layout references unknown tree: " + treeId);
                stillPending.add(layout);
                continue;
            }

            try {
                applyLayoutPlacements(layout);
                validLayouts.add(layout);
                processed++;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to apply deferred layout placements: " + layout.getId(), e);
            }
        }

        // Second pass: apply connections (now all nodes should exist)
        for (TreeLayoutAsset layout : validLayouts) {
            try {
                applyLayoutConnections(layout);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to apply deferred layout connections: " + layout.getId(), e);
            }
        }

        pendingLayouts.clear();
        pendingLayouts.addAll(stillPending);

        LOGGER.info("Processed " + processed + " deferred layouts" +
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
                LOGGER.fine("Node already exists in tree: " + effectiveId);
                continue;
            }

            // Build the node from template + placement
            PassiveNode node = buildNodeFromTemplate(template, placement);

            try {
                registry.addNode(treeId, node);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to add node " + effectiveId + " to tree " + treeId, e);
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
                        .version(tree.getVersion())
                        .build();

                // Update registry
                registry.replaceTree(tree, updatedTree);
                tree = updatedTree;
                LOGGER.info("Added starting node '" + startingNodeId + "' to tree '" + treeId + "'. Total starting nodes: " + tree.getStartingNodeIds().size());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to add starting node " + startingNodeId, e);
            }
        }

        LOGGER.fine("Applied placements from layout " + layout.getId() + " to tree " + treeId);
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
                LOGGER.log(Level.WARNING, "Failed to add connection " + fromId + " -> " + toId, e);
            }
        }

        LOGGER.fine("Applied connections from layout " + layout.getId() + " to tree " + treeId);
    }

    /**
     * Apply a layout to its target tree (legacy method, uses two-pass internally).
     */
    private static void applyLayout(TreeLayoutAsset layout) {
        applyLayoutPlacements(layout);
        applyLayoutConnections(layout);
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
                template.getIcon(),
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
