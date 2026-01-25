package reign.software.hyforged.effect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Definition of a Hyforged effect and its modifier specs.
 */
public class HyforgedEffectDefinition {

    private final String effectId;
    private final List<HyforgedEffectModifierSpec> modifiers;
    private final int concentrationCost;
    private final String concentrationAbilityId;
    private final Integer concentrationPriority;

    public HyforgedEffectDefinition(
            @Nonnull String effectId,
            @Nonnull List<HyforgedEffectModifierSpec> modifiers
    ) {
        this(effectId, modifiers, 0, null, null);
    }

    public HyforgedEffectDefinition(
            @Nonnull String effectId,
            @Nonnull List<HyforgedEffectModifierSpec> modifiers,
            int concentrationCost,
            @Nullable String concentrationAbilityId,
            @Nullable Integer concentrationPriority
    ) {
        this.effectId = effectId;
        this.modifiers = modifiers;
        this.concentrationCost = Math.max(0, concentrationCost);
        this.concentrationAbilityId = concentrationAbilityId;
        this.concentrationPriority = concentrationPriority;
    }

    @Nonnull
    public String getEffectId() {
        return effectId;
    }

    @Nonnull
    public List<HyforgedEffectModifierSpec> getModifiers() {
        return modifiers;
    }

    public int getConcentrationCost() {
        return concentrationCost;
    }

    @Nullable
    public String getConcentrationAbilityId() {
        return concentrationAbilityId;
    }

    @Nullable
    public Integer getConcentrationPriority() {
        return concentrationPriority;
    }
}
