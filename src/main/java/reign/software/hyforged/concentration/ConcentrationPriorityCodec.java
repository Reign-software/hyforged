package reign.software.hyforged.concentration;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

/**
 * Codec for serializing and deserializing ConcentrationPriorityComponent.
 */
public final class ConcentrationPriorityCodec {

    private ConcentrationPriorityCodec() {
    }

    public static final String COMPONENT_ID = "Hyforged_ConcentrationPriority";

    private static final ArrayCodec<String> STRING_ARRAY_CODEC = new ArrayCodec<>(Codec.STRING, String[]::new);

    public static final BuilderCodec<ConcentrationPriorityComponent> CODEC = BuilderCodec
            .builder(ConcentrationPriorityComponent.class, ConcentrationPriorityComponent::new)
            .versioned()
            .codecVersion(ConcentrationPriorityComponent.SCHEMA_VERSION)
            .append(
                    new KeyedCodec<>("AbilityIds", STRING_ARRAY_CODEC),
                    (component, value) -> component.setTempAbilityIds(value),
                    ConcentrationPriorityComponent::getAbilityIdsForSave
            )
            .add()
            .append(
                    new KeyedCodec<>("AbilityCosts", Codec.INT_ARRAY),
                    (component, value) -> component.setTempAbilityCosts(value),
                    ConcentrationPriorityComponent::getAbilityCostsForSave
            )
            .add()
            .append(
                    new KeyedCodec<>("AbilityPriorities", Codec.INT_ARRAY),
                    (component, value) -> component.setTempAbilityPriorities(value),
                    ConcentrationPriorityComponent::getAbilityPrioritiesForSave
            )
            .add()
            .append(
                    new KeyedCodec<>("AbilityEnabled", Codec.INT_ARRAY),
                    (component, value) -> component.setTempAbilityEnabled(value),
                    ConcentrationPriorityComponent::getAbilityEnabledForSave
            )
            .add()
            .append(
                    new KeyedCodec<>("CurrentConcentration", Codec.INTEGER),
                    (component, value) -> {
                        if (value != null) {
                            component.setCurrentConcentration(value);
                        }
                    },
                    ConcentrationPriorityComponent::getCurrentConcentration
            )
            .add()
            .afterDecode((component, extraInfo) -> {
                component.applyLoadedAbilities();
                component.clearTempLoadData();
                component.resetRegenRemainder();
            })
            .build();
}
