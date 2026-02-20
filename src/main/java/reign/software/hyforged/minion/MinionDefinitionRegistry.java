package reign.software.hyforged.minion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.npc.NPCPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

/**
 * Singleton registry for {@link MinionDefinition} instances keyed by namespaced ID.
 * <p>
 * Follows the {@code HyforgedEffectRegistry} singleton pattern.
 * Definitions are loaded from {@code Server/Hyforged/Minions/*.json} during plugin setup.
 */
public final class MinionDefinitionRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final MinionDefinitionRegistry INSTANCE = new MinionDefinitionRegistry();

    private final Map<String, MinionDefinition> definitions = new HashMap<>();

    private MinionDefinitionRegistry() {
    }

    @Nonnull
    public static MinionDefinitionRegistry get() {
        return INSTANCE;
    }

    /**
     * Register a minion definition.
     *
     * @param definition the definition to register
     * @throws NullPointerException if definition is null
     */
    public void register(@Nonnull MinionDefinition definition) {
        Objects.requireNonNull(definition, "definition cannot be null");
        String id = definition.getId();
        if (definitions.containsKey(id)) {
            LOGGER.atWarning().log("Duplicate minion definition for: %s (ignoring duplicate)", id);
            return;
        }
        definitions.put(id, definition);
        LOGGER.at(Level.FINE).log("Registered minion definition: %s", id);
    }

    /**
     * Get a minion definition by ID.
     *
     * @param id the namespaced minion ID (e.g. "hyforged:skeleton-warrior")
     * @return the definition, or null if not found
     */
    @Nullable
    public MinionDefinition get(@Nonnull String id) {
        Objects.requireNonNull(id, "id cannot be null");
        return definitions.get(id);
    }

    /**
     * Get all registered definitions as an unmodifiable map.
     */
    @Nonnull
    public Map<String, MinionDefinition> getAll() {
        return Collections.unmodifiableMap(definitions);
    }

    /**
     * Clear all registered definitions (for reload or testing).
     */
    public void clear() {
        definitions.clear();
    }

    /**
     * Get the number of registered definitions.
     */
    public int size() {
        return definitions.size();
    }

    /**
     * Load minion definitions from a JSON index file.
     * <p>
     * The index file should contain a JSON object with a {@code "Definitions"} array
     * listing the resource paths to individual minion definition JSON files.
     * <p>
     * Example index:
     * <pre>
     * {
     *   "Definitions": [
     *     "Server/Hyforged/Minions/SkeletonWarrior.json",
     *     "Server/Hyforged/Minions/KweebecSapling.json"
     *   ]
     * }
     * </pre>
     *
     * @param indexPath classpath resource path to the index JSON file
     */
    public void loadFromIndex(@Nonnull String indexPath) {
        Objects.requireNonNull(indexPath, "indexPath cannot be null");

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(indexPath)) {
            if (is == null) {
                LOGGER.atWarning().log("Minion index resource not found: %s", indexPath);
                return;
            }

            JsonObject indexObj = JsonParser.parseReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8)
            ).getAsJsonObject();

            if (!indexObj.has("Definitions") || !indexObj.get("Definitions").isJsonArray()) {
                LOGGER.atWarning().log("Minion index '%s' missing 'Definitions' array", indexPath);
                return;
            }

            JsonArray paths = indexObj.getAsJsonArray("Definitions");
            List<String> resourcePaths = new ArrayList<>(paths.size());
            for (JsonElement element : paths) {
                resourcePaths.add(element.getAsString());
            }

            loadFromResources(resourcePaths);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to load minion index: %s", indexPath);
        }
    }

    /**
     * Load minion definitions from a list of JSON resource paths.
     * <p>
     * Each JSON file should contain a single minion definition object with fields:
     * {@code Id}, {@code NpcTemplate}, {@code ConcentrationCost}, {@code DefaultPriority},
     * {@code BaseDuration}, {@code SpawnOffset} (object with X, Y, Z), and {@code Tags} (array).
     *
     * @param resourcePaths list of classpath resource paths to load
     */
    public void loadFromResources(@Nonnull List<String> resourcePaths) {
        Objects.requireNonNull(resourcePaths, "resourcePaths cannot be null");
        int loaded = 0;
        int errors = 0;

        for (String path : resourcePaths) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    LOGGER.atWarning().log("Minion definition resource not found: %s", path);
                    errors++;
                    continue;
                }

                JsonObject obj = JsonParser.parseReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8)
                ).getAsJsonObject();

                MinionDefinition def = parseDefinition(obj, path);
                register(def);
                loaded++;
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to load minion definition: %s", path);
                errors++;
            }
        }

        LOGGER.atInfo().log("Loaded %s minion definitions%s",
                loaded, (errors > 0 ? " (" + errors + " errors)" : ""));
    }

    /**
     * Parse a single minion definition from a JSON object.
     */
    @Nonnull
    private static MinionDefinition parseDefinition(@Nonnull JsonObject obj, @Nonnull String sourcePath) {
        String id = getRequiredString(obj, "Id", sourcePath);
        String npcTemplate = getRequiredString(obj, "NpcTemplate", sourcePath);
        int concentrationCost = obj.has("ConcentrationCost") ? obj.get("ConcentrationCost").getAsInt() : 0;
        int defaultPriority = obj.has("DefaultPriority") ? obj.get("DefaultPriority").getAsInt() : 0;
        int baseDuration = obj.has("BaseDuration") ? obj.get("BaseDuration").getAsInt() : 0;

        float spawnOffsetX = 0f;
        float spawnOffsetY = 0f;
        float spawnOffsetZ = 0f;
        if (obj.has("SpawnOffset")) {
            JsonObject offset = obj.getAsJsonObject("SpawnOffset");
            spawnOffsetX = offset.has("X") ? offset.get("X").getAsFloat() : 0f;
            spawnOffsetY = offset.has("Y") ? offset.get("Y").getAsFloat() : 0f;
            spawnOffsetZ = offset.has("Z") ? offset.get("Z").getAsFloat() : 0f;
        }

        List<String> tags = new ArrayList<>();
        if (obj.has("Tags")) {
            JsonArray tagArray = obj.getAsJsonArray("Tags");
            for (JsonElement element : tagArray) {
                tags.add(element.getAsString());
            }
        }

        Map<String, Integer> statOverrides = new HashMap<>();
        if (obj.has("StatOverrides") && obj.get("StatOverrides").isJsonObject()) {
            JsonObject overrides = obj.getAsJsonObject("StatOverrides");
            for (Map.Entry<String, JsonElement> entry : overrides.entrySet()) {
                statOverrides.put(entry.getKey(), entry.getValue().getAsInt());
            }
        }

        return new MinionDefinition(
                id, npcTemplate, concentrationCost, defaultPriority, baseDuration,
                spawnOffsetX, spawnOffsetY, spawnOffsetZ, tags, statOverrides
        );
    }

    @Nonnull
    private static String getRequiredString(@Nonnull JsonObject obj, @Nonnull String field, @Nonnull String sourcePath) {
        if (!obj.has(field) || obj.get(field).isJsonNull()) {
            throw new IllegalStateException("Missing required field '" + field + "' in " + sourcePath);
        }
        return obj.get(field).getAsString();
    }

    /**
     * Validate all registered definitions' NPC templates against {@link NPCPlugin}.
     * <p>
     * Call this after NPCPlugin is initialized (e.g. in plugin setup after NPC registration).
     * Logs a WARNING for any definition whose NPC template is not found, but does not
     * unregister the definition — templates may load later via other plugins.
     *
     * @return the number of templates that failed validation
     */
    public int validateTemplates() {
        int failures = 0;
        NPCPlugin npcPlugin;
        try {
            npcPlugin = NPCPlugin.get();
        } catch (Exception e) {
            LOGGER.atWarning().log("Cannot validate NPC templates: NPCPlugin not available yet");
            return -1;
        }

        for (MinionDefinition definition : definitions.values()) {
            try {
                int index = npcPlugin.getIndex(definition.getNpcTemplate());
                if (index < 0) {
                    LOGGER.atWarning().log(
                            "Minion definition '%s': NPC template '%s' not found in NPCPlugin (index=%d). "
                                    + "The template may load later from another plugin.",
                            definition.getId(), definition.getNpcTemplate(), index);
                    failures++;
                }
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log(
                        "Minion definition '%s': Failed to validate NPC template '%s'",
                        definition.getId(), definition.getNpcTemplate());
                failures++;
            }
        }

        if (failures == 0) {
            LOGGER.atInfo().log("All %d minion NPC templates validated successfully", definitions.size());
        } else {
            LOGGER.atWarning().log("%d of %d minion NPC template(s) failed validation",
                    failures, definitions.size());
        }
        return failures;
    }
}
