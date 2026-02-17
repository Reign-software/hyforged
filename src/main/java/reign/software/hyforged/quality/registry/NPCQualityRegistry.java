package reign.software.hyforged.quality.registry;

import reign.software.hyforged.quality.model.NPCQualityRule;
import reign.software.hyforged.quality.model.QualityWeightTable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for NPC quality rules.
 */
public final class NPCQualityRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static NPCQualityRegistry instance;

    private final Map<String, NPCQualityRule> rulesById = new ConcurrentHashMap<>();
    private final Map<String, QualityWeightTable> tablesById = new ConcurrentHashMap<>();
    private boolean frozen = false;

    private NPCQualityRegistry() {}

    @Nonnull
    public static synchronized NPCQualityRegistry get() {
        if (instance == null) {
            instance = new NPCQualityRegistry();
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = new NPCQualityRegistry();
    }

    public synchronized void register(@Nonnull NPCQualityRule rule) {
        Objects.requireNonNull(rule, "rule cannot be null");
        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot register new NPC quality rules");
        }
        if (rulesById.containsKey(rule.id())) {
            LOGGER.atWarning().log("NPC quality rule '%s' overridden by later definition", rule.id());
        }
        rulesById.put(rule.id(), rule);
        tablesById.put(rule.id(), QualityWeightTable.fromWeights(rule.weights()));
    }

    @Nullable
    public NPCQualityRule get(@Nonnull String id) {
        return rulesById.get(id);
    }

    @Nullable
    public QualityWeightTable getTable(@Nonnull String id) {
        return tablesById.get(id);
    }

    @Nullable
    public NPCQualityRule getDefaultRule() {
        NPCQualityRule rule = rulesById.get("default-spawn");
        if (rule != null) {
            return rule;
        }
        return rulesById.values().stream().findFirst().orElse(null);
    }

    @Nullable
    public NPCQualityRule resolveRuleForRole(@Nullable String roleName) {
        if (roleName != null && !roleName.isBlank()) {
            for (NPCQualityRule rule : rulesById.values()) {
                if (rule.appliesToRole(roleName)) {
                    return rule;
                }
            }
        }
        return getDefaultRule();
    }

    public synchronized void freeze() {
        frozen = true;
    }
}
