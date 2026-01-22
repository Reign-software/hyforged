package reign.software.hyforged.effect;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Definition of a Hyforged effect and its modifier specs.
 */
public class HyforgedEffectDefinition {

    private final String effectId;
    private final List<HyforgedEffectModifierSpec> modifiers;

    public HyforgedEffectDefinition(
            @Nonnull String effectId,
            @Nonnull List<HyforgedEffectModifierSpec> modifiers
    ) {
        this.effectId = effectId;
        this.modifiers = modifiers;
    }

    @Nonnull
    public String getEffectId() {
        return effectId;
    }

    @Nonnull
    public List<HyforgedEffectModifierSpec> getModifiers() {
        return modifiers;
    }
}
