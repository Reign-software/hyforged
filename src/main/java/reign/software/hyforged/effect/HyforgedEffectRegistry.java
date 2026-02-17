package reign.software.hyforged.effect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Registry for Hyforged effect definitions keyed by Hytale EntityEffect ID.
 */
public final class HyforgedEffectRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final HyforgedEffectRegistry INSTANCE = new HyforgedEffectRegistry();

    private final Map<String, HyforgedEffectDefinition> definitions = new HashMap<>();

    private HyforgedEffectRegistry() {
    }

    @Nonnull
    public static HyforgedEffectRegistry get() {
        return INSTANCE;
    }

    public void register(@Nonnull String effectId, @Nonnull List<HyforgedEffectModifierSpec> modifiers) {
        register(effectId, modifiers, 0, null, null);
    }

    public void register(
            @Nonnull String effectId,
            @Nonnull List<HyforgedEffectModifierSpec> modifiers,
            int concentrationCost,
            @Nullable String concentrationAbilityId,
            @Nullable Integer concentrationPriority
    ) {
        if (definitions.containsKey(effectId)) {
            LOGGER.atWarning().log("Duplicate Hyforged effect definition for: %s (ignoring duplicate)", effectId);
            return;
        }
        definitions.put(effectId, new HyforgedEffectDefinition(
                effectId,
                modifiers,
                concentrationCost,
                concentrationAbilityId,
                concentrationPriority
        ));
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
