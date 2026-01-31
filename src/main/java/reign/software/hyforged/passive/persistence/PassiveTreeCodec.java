package reign.software.hyforged.passive.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import reign.software.hyforged.passive.component.PassiveTreeComponent;

import java.util.*;

/**
 * Codec for serializing and deserializing PassiveTreeComponent.
 * <p>
 * Uses Hytale's BuilderCodec pattern for persistence.
 * <p>
 * Persistence Strategy:
 * - General tree starting node and allocated nodes
 * - Point Book points used
 * - Class tree allocations (flattened to parallel arrays)
 * - Mastery choices (node ID to option ID mappings)
 * - Tree versions for migration tracking
 * <p>
 * Pending mastery choices are NOT persisted (transient UI state).
 */
public final class PassiveTreeCodec {

    private PassiveTreeCodec() {
        // Utility class
    }

    /**
     * The component ID used for registration.
     */
    public static final String COMPONENT_ID = "Hyforged_PassiveTree";

    /**
     * The codec for PassiveTreeComponent.
     * <p>
     * Schema (v1):
     * <pre>
     * {
     *   "Version": int,                    // Schema version for migration
     *   "GeneralStartingNode": string,     // Starting node ID (empty string if none)
     *   "GeneralAllocatedNodes": string[], // Node IDs in general tree
     *   "BookPointsUsed": int,             // Point Book points used
     *   "ClassIds": string[],              // Class IDs with allocations
     *   "ClassNodeCounts": int[],          // Number of nodes per class (parallel)
     *   "ClassNodesFlat": string[],        // All class nodes flattened
     *   "MasteryNodeIds": string[],        // Mastery node IDs with choices
     *   "MasteryOptionIds": string[],      // Parallel array of chosen option IDs
     *   "TreeIds": string[],               // Tree IDs with version tracking
     *   "TreeVersions": int[]              // Parallel array of versions
     * }
     * </pre>
     */
    public static final BuilderCodec<PassiveTreeComponent> CODEC = BuilderCodec
            .builder(PassiveTreeComponent.class, PassiveTreeComponent::new)
            .versioned()
            .codecVersion(PassiveTreeComponent.SCHEMA_VERSION)
            // General starting node (empty string = null)
            .append(
                    new KeyedCodec<>("GeneralStartingNode", Codec.STRING),
                    (component, value) -> {
                        if (value != null && !value.isEmpty()) {
                            component.setGeneralStartingNode(value);
                        }
                    },
                    component -> {
                        String node = component.getGeneralStartingNode();
                        return node != null ? node : "";
                    }
            )
            .add()
            // General allocated nodes
            .append(
                    new KeyedCodec<>("GeneralAllocatedNodes", Codec.STRING_ARRAY),
                    (component, nodes) -> {
                        if (nodes != null) {
                            for (String node : nodes) {
                                component.allocateGeneralNode(node);
                            }
                        }
                    },
                    component -> component.getGeneralAllocatedNodes().toArray(new String[0])
            )
            .add()
            // Book points used
            .append(
                    new KeyedCodec<>("BookPointsUsed", Codec.INTEGER),
                    (component, value) -> {
                        if (value != null) {
                            component.setBookPointsUsed(value);
                        }
                    },
                    PassiveTreeComponent::getBookPointsUsed
            )
            .add()
            // Class allocations - stored as parallel arrays with flattened nodes
            .append(
                    new KeyedCodec<>("ClassIds", Codec.STRING_ARRAY),
                    (component, classIds) -> {
                        if (classIds != null) {
                            component.setTempLoadClassIds(classIds);
                        }
                    },
                    component -> component.getClassIdsWithAllocations().toArray(new String[0])
            )
            .add()
            .append(
                    new KeyedCodec<>("ClassNodeCounts", Codec.INT_ARRAY),
                    (component, counts) -> {
                        if (counts != null) {
                            component.setTempLoadClassNodeCounts(counts);
                        }
                    },
                    component -> {
                        Set<String> classIds = component.getClassIdsWithAllocations();
                        int[] counts = new int[classIds.size()];
                        int i = 0;
                        for (String classId : classIds) {
                            counts[i++] = component.getClassAllocatedNodes(classId).size();
                        }
                        return counts;
                    }
            )
            .add()
            .append(
                    new KeyedCodec<>("ClassNodesFlat", Codec.STRING_ARRAY),
                    (component, flatNodes) -> {
                        String[] classIds = component.getTempLoadClassIds();
                        int[] counts = component.getTempLoadClassNodeCounts();
                        if (classIds != null && counts != null && flatNodes != null) {
                            int offset = 0;
                            for (int i = 0; i < classIds.length && i < counts.length; i++) {
                                String classId = classIds[i];
                                int count = counts[i];
                                for (int j = 0; j < count && offset < flatNodes.length; j++) {
                                    component.allocateClassNode(classId, flatNodes[offset++]);
                                }
                            }
                        }
                        component.clearTempLoadClassIds();
                        component.clearTempLoadClassNodeCounts();
                    },
                    component -> {
                        List<String> allNodes = new ArrayList<>();
                        for (String classId : component.getClassIdsWithAllocations()) {
                            allNodes.addAll(component.getClassAllocatedNodes(classId));
                        }
                        return allNodes.toArray(new String[0]);
                    }
            )
            .add()
            // Mastery choices - stored as parallel arrays
            .append(
                    new KeyedCodec<>("MasteryNodeIds", Codec.STRING_ARRAY),
                    (component, nodeIds) -> {
                        if (nodeIds != null) {
                            component.setTempLoadMasteryNodeIds(nodeIds);
                        }
                    },
                    component -> component.getAllMasteryChoices().keySet().toArray(new String[0])
            )
            .add()
            .append(
                    new KeyedCodec<>("MasteryOptionIds", Codec.STRING_ARRAY),
                    (component, optionIds) -> {
                        String[] nodeIds = component.getTempLoadMasteryNodeIds();
                        if (nodeIds != null && optionIds != null) {
                            for (int i = 0; i < nodeIds.length && i < optionIds.length; i++) {
                                component.setMasteryChoice(nodeIds[i], optionIds[i]);
                            }
                        }
                        component.clearTempLoadMasteryNodeIds();
                    },
                    component -> component.getAllMasteryChoices().values().toArray(new String[0])
            )
            .add()
            // Tree versions - stored as parallel arrays
            .append(
                    new KeyedCodec<>("TreeIds", Codec.STRING_ARRAY),
                    (component, treeIds) -> {
                        if (treeIds != null) {
                            component.setTempLoadTreeIds(treeIds);
                        }
                    },
                    component -> component.getAllTreeVersions().keySet().toArray(new String[0])
            )
            .add()
            .append(
                    new KeyedCodec<>("TreeVersions", Codec.INT_ARRAY),
                    (component, versions) -> {
                        String[] treeIds = component.getTempLoadTreeIds();
                        if (treeIds != null && versions != null) {
                            for (int i = 0; i < treeIds.length && i < versions.length; i++) {
                                component.setTreeVersion(treeIds[i], versions[i]);
                            }
                        }
                        component.clearTempLoadTreeIds();
                    },
                    component -> {
                        Map<String, Integer> versions = component.getAllTreeVersions();
                        int[] result = new int[versions.size()];
                        int i = 0;
                        for (Integer v : versions.values()) {
                            result[i++] = v != null ? v : 0;
                        }
                        return result;
                    }
            )
            .add()
            .afterDecode((component, extraInfo) -> {
                // Mark clean after load
                component.clearDirty();
            })
            .build();
}
