package reign.software.hyforged.effect;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;

/**
 * Data-driven specification for a Hyforged modifier applied by an effect.
 */
public class HyforgedEffectModifierSpec {

    public static final BuilderCodec<HyforgedEffectModifierSpec> CODEC = BuilderCodec.builder(
            HyforgedEffectModifierSpec.class,
            HyforgedEffectModifierSpec::new
    )
            .append(
                    new KeyedCodec<>("StatId", Codec.STRING),
                    (spec, value) -> spec.statId = value,
                    spec -> spec.statId
            )
            .addValidator(Validators.nonNull())
            .add()
            .append(
                    new KeyedCodec<>("StackType", new EnumCodec<>(HyforgedModifier.StackType.class)),
                    (spec, value) -> spec.stackType = value,
                    spec -> spec.stackType
            )
            .addValidator(Validators.nonNull())
            .add()
            .append(
                    new KeyedCodec<>("Amount", Codec.INTEGER),
                    (spec, value) -> spec.amount = value,
                    spec -> spec.amount
            )
            .add()
            .append(
                    new KeyedCodec<>("Target", new EnumCodec<>(Modifier.ModifierTarget.class)),
                    (spec, value) -> spec.target = value,
                    spec -> spec.target
            )
            .add()
            .append(
                    new KeyedCodec<>("Priority", Codec.INTEGER),
                    (spec, value) -> spec.priority = value,
                    spec -> spec.priority
            )
            .add()
            .build();

    private String statId;
    private HyforgedModifier.StackType stackType = HyforgedModifier.StackType.FLAT;
    private int amount = 0;
    private Modifier.ModifierTarget target = Modifier.ModifierTarget.MAX;
    private int priority = 0;

    public HyforgedEffectModifierSpec() {
    }

    @Nonnull
    public String getStatId() {
        return statId;
    }

    @Nonnull
    public HyforgedModifier.StackType getStackType() {
        return stackType;
    }

    public int getAmount() {
        return amount;
    }

    @Nonnull
    public Modifier.ModifierTarget getTarget() {
        return target;
    }

    public int getPriority() {
        return priority;
    }
}
