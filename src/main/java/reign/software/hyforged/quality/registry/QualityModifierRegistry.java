package reign.software.hyforged.quality.registry;

import reign.software.hyforged.quality.model.QualityModifierConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry for quality modifier configs.
 */
public final class QualityModifierRegistry {

    private static final Logger LOGGER = Logger.getLogger(QualityModifierRegistry.class.getName());
    private static QualityModifierRegistry instance;

    private final Map<String, QualityModifierConfig> configsById = new ConcurrentHashMap<>();
    private boolean frozen = false;

    private QualityModifierRegistry() {}

    @Nonnull
    public static synchronized QualityModifierRegistry get() {
        if (instance == null) {
            instance = new QualityModifierRegistry();
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = new QualityModifierRegistry();
    }

    public synchronized void register(@Nonnull QualityModifierConfig config) {
        Objects.requireNonNull(config, "config cannot be null");
        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot register new configs");
        }
        if (configsById.containsKey(config.id())) {
            LOGGER.log(Level.WARNING, "Quality modifier config ''{0}'' overridden by later definition", config.id());
        }
        configsById.put(config.id(), config);
    }

    @Nullable
    public QualityModifierConfig get(@Nonnull String id) {
        return configsById.get(id);
    }

    @Nonnull
    public QualityModifierConfig getDefault(@Nonnull QualityModifierConfig fallback) {
        QualityModifierConfig config = configsById.get("default");
        return config != null ? config : fallback;
    }

    public synchronized void freeze() {
        frozen = true;
    }
}
