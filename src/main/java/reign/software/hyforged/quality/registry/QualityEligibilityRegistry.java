package reign.software.hyforged.quality.registry;

import reign.software.hyforged.quality.model.QualityEligibilityRule;
import reign.software.hyforged.quality.model.QualityRollContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry for quality eligibility rules.
 */
public final class QualityEligibilityRegistry {

    private static final Logger LOGGER = Logger.getLogger(QualityEligibilityRegistry.class.getName());
    private static QualityEligibilityRegistry instance;

    private final Map<String, QualityEligibilityRule> rulesById = new ConcurrentHashMap<>();
    private List<QualityEligibilityRule> sortedRules = new ArrayList<>();
    private boolean sortDirty = true;
    private boolean frozen = false;

    private QualityEligibilityRegistry() {}

    @Nonnull
    public static synchronized QualityEligibilityRegistry get() {
        if (instance == null) {
            instance = new QualityEligibilityRegistry();
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = new QualityEligibilityRegistry();
    }

    public synchronized void register(@Nonnull QualityEligibilityRule rule) {
        Objects.requireNonNull(rule, "rule cannot be null");
        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot register new rules");
        }
        if (rulesById.containsKey(rule.id())) {
            LOGGER.log(Level.WARNING, "Quality eligibility rule ''{0}'' overridden by later definition", rule.id());
        }
        rulesById.put(rule.id(), rule);
        sortDirty = true;
    }

    @Nullable
    public QualityEligibilityRule get(@Nonnull String id) {
        return rulesById.get(id);
    }

    @Nullable
    public QualityEligibilityRule resolve(@Nonnull QualityRollContext context) {
        ensureSorted();
        for (QualityEligibilityRule rule : sortedRules) {
            if (rule.matches(context)) {
                return rule;
            }
        }
        return null;
    }

    public synchronized void freeze() {
        ensureSorted();
        frozen = true;
    }

    private void ensureSorted() {
        if (sortDirty) {
            sortedRules = rulesById.values().stream()
                    .sorted(Comparator
                            .comparingInt(QualityEligibilityRule::priority).reversed()
                            .thenComparing(QualityEligibilityRule::id))
                    .toList();
            sortDirty = false;
        }
    }
}
