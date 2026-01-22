package reign.software.hyforged.effect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Registry for Hyforged effect definitions keyed by Hytale EntityEffect ID.
 */
public final class HyforgedEffectRegistry {

    private static final Logger LOGGER = Logger.getLogger(HyforgedEffectRegistry.class.getName());
    private static final HyforgedEffectRegistry INSTANCE = new HyforgedEffectRegistry();

    private final Map<String, HyforgedEffectDefinition> definitions = new HashMap<>();

    private HyforgedEffectRegistry() {
    }

    @Nonnull
    public static HyforgedEffectRegistry get() {
        return INSTANCE;
    }

    public void register(@Nonnull String effectId, @Nonnull List<HyforgedEffectModifierSpec> modifiers) {
        if (definitions.containsKey(effectId)) {
            LOGGER.warning("Duplicate Hyforged effect definition for: " + effectId + " (ignoring duplicate)");
            return;
        }
        definitions.put(effectId, new HyforgedEffectDefinition(effectId, modifiers));
    }

    @Nullable
    public HyforgedEffectDefinition get(@Nonnull String effectId) {
        return definitions.get(effectId);
    }

    @Nonnull
    public Map<String, HyforgedEffectDefinition> getAll() {
        return Collections.unmodifiableMap(definitions);
    }

    public void clear() {
        definitions.clear();
    }

    public int size() {
        return definitions.size();
    }
}
