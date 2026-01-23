package reign.software.hyforged.affix.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.vector.Vector3d;
import reign.software.hyforged.affix.model.AffixEffect;
import reign.software.hyforged.affix.model.AffixTrigger;
import reign.software.hyforged.affix.model.AffixTriggeredEffect;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Asset codec for triggered effect affix definitions.
 */
public class AffixTriggeredEffectAsset {

    private static final ArrayCodec<String> STRING_ARRAY_CODEC = new ArrayCodec<>(Codec.STRING, String[]::new);

    public static final BuilderCodec<TriggerAsset> TRIGGER_CODEC = BuilderCodec.builder(
                    TriggerAsset.class,
                    TriggerAsset::new
            )
            .append(new KeyedCodec<>("Type", Codec.STRING), (asset, value) -> asset.type = value, asset -> asset.type)
            .add()
            .append(new KeyedCodec<>("Chance", Codec.INTEGER), (asset, value) -> asset.chance = value != null ? value : AffixTrigger.DEFAULT_CHANCE_BPS, asset -> asset.chance)
            .add()
            .append(new KeyedCodec<>("DamageCauses", STRING_ARRAY_CODEC), (asset, value) -> asset.damageCauses = value, asset -> asset.damageCauses)
            .add()
            .append(new KeyedCodec<>("TargetTags", STRING_ARRAY_CODEC), (asset, value) -> asset.targetTags = value, asset -> asset.targetTags)
            .add()
            .append(new KeyedCodec<>("MinDamage", Codec.INTEGER), (asset, value) -> asset.minDamage = value != null ? value : 0, asset -> asset.minDamage)
            .add()
            .append(new KeyedCodec<>("IntervalSeconds", Codec.FLOAT), (asset, value) -> asset.intervalSeconds = value != null ? value : 0f, asset -> asset.intervalSeconds)
            .add()
            .append(new KeyedCodec<>("RequireCombat", Codec.BOOLEAN), (asset, value) -> asset.requireCombat = value != null && value, asset -> asset.requireCombat)
            .add()
            .append(new KeyedCodec<>("InteractionTypes", STRING_ARRAY_CODEC), (asset, value) -> asset.interactionTypes = value, asset -> asset.interactionTypes)
            .add()
            .build();

    public static final BuilderCodec<EffectAsset> EFFECT_CODEC = BuilderCodec.builder(
                    EffectAsset.class,
                    EffectAsset::new
            )
            .append(new KeyedCodec<>("Type", Codec.STRING), (asset, value) -> asset.type = value, asset -> asset.type)
            .add()
            .append(new KeyedCodec<>("ProjectileId", Codec.STRING), (asset, value) -> asset.projectileId = value, asset -> asset.projectileId)
            .add()
            .append(new KeyedCodec<>("Count", Codec.INTEGER), (asset, value) -> asset.count = value != null ? value : 1, asset -> asset.count)
            .add()
            .append(new KeyedCodec<>("Pattern", Codec.STRING), (asset, value) -> asset.pattern = value, asset -> asset.pattern)
            .add()
            .append(new KeyedCodec<>("Velocity", Codec.FLOAT), (asset, value) -> asset.velocity = value != null ? value : 0f, asset -> asset.velocity)
            .add()
            .append(new KeyedCodec<>("SpreadAngle", Codec.FLOAT), (asset, value) -> asset.spreadAngle = value != null ? value : 0f, asset -> asset.spreadAngle)
            .add()
            .append(new KeyedCodec<>("OrbitRadius", Codec.FLOAT), (asset, value) -> asset.orbitRadius = value != null ? value : 0f, asset -> asset.orbitRadius)
            .add()
            .append(new KeyedCodec<>("RotationSpeed", Codec.FLOAT), (asset, value) -> asset.rotationSpeed = value != null ? value : 0f, asset -> asset.rotationSpeed)
            .add()
            .append(new KeyedCodec<>("Duration", Codec.FLOAT), (asset, value) -> asset.durationSeconds = value != null ? value : 0f, asset -> asset.durationSeconds)
            .add()
            .append(new KeyedCodec<>("PrefabPath", Codec.STRING), (asset, value) -> asset.prefabPath = value, asset -> asset.prefabPath)
            .add()
            .append(new KeyedCodec<>("Offset", Vector3d.CODEC), (asset, value) -> asset.offset = value, asset -> asset.offset)
            .add()
            .append(new KeyedCodec<>("EffectId", Codec.STRING), (asset, value) -> asset.effectId = value, asset -> asset.effectId)
            .add()
            .append(new KeyedCodec<>("Target", Codec.STRING), (asset, value) -> asset.target = value, asset -> asset.target)
            .add()
            .append(new KeyedCodec<>("Radius", Codec.FLOAT), (asset, value) -> asset.radius = value != null ? value : 0f, asset -> asset.radius)
            .add()
            .append(new KeyedCodec<>("Damage", Codec.INTEGER), (asset, value) -> asset.damage = value != null ? value : 0, asset -> asset.damage)
            .add()
            .append(new KeyedCodec<>("DamageCause", Codec.STRING), (asset, value) -> asset.damageCause = value, asset -> asset.damageCause)
            .add()
            .append(new KeyedCodec<>("ExcludeSelf", Codec.BOOLEAN), (asset, value) -> asset.excludeSelf = value != null && value, asset -> asset.excludeSelf)
            .add()
            .append(new KeyedCodec<>("InteractionId", Codec.STRING), (asset, value) -> asset.interactionId = value, asset -> asset.interactionId)
            .add()
            .append(new KeyedCodec<>("InteractionType", Codec.STRING), (asset, value) -> asset.interactionType = value, asset -> asset.interactionType)
            .add()
            .append(new KeyedCodec<>("StatId", Codec.STRING), (asset, value) -> asset.statId = value, asset -> asset.statId)
            .add()
            .append(new KeyedCodec<>("Amount", Codec.INTEGER), (asset, value) -> asset.amount = value != null ? value : 0, asset -> asset.amount)
            .add()
            .append(new KeyedCodec<>("StackType", Codec.STRING), (asset, value) -> asset.stackType = value, asset -> asset.stackType)
            .add()
            .append(new KeyedCodec<>("StatDuration", Codec.FLOAT), (asset, value) -> asset.statDurationSeconds = value != null ? value : 0f, asset -> asset.statDurationSeconds)
            .add()
            .build();

    public static final BuilderCodec<AffixTriggeredEffectAsset> CODEC = BuilderCodec.builder(
                    AffixTriggeredEffectAsset.class,
                    AffixTriggeredEffectAsset::new
            )
            .append(new KeyedCodec<>("Trigger", TRIGGER_CODEC), (asset, value) -> asset.trigger = value, asset -> asset.trigger)
            .add()
            .append(new KeyedCodec<>("Effect", EFFECT_CODEC), (asset, value) -> asset.effect = value, asset -> asset.effect)
            .add()
            .append(new KeyedCodec<>("StackBehavior", Codec.STRING), (asset, value) -> asset.stackBehavior = value != null ? value : AffixTriggeredEffect.STACK_BEHAVIOR_INDEPENDENT, asset -> asset.stackBehavior)
            .add()
            .append(new KeyedCodec<>("MaxStacks", Codec.INTEGER), (asset, value) -> asset.maxStacks = value != null ? value : 1, asset -> asset.maxStacks)
            .add()
            .append(new KeyedCodec<>("CooldownSeconds", Codec.FLOAT), (asset, value) -> asset.cooldownSeconds = value != null ? value : 0f, asset -> asset.cooldownSeconds)
            .add()
            .append(new KeyedCodec<>("SharedCooldownGroup", Codec.STRING), (asset, value) -> asset.sharedCooldownGroup = value != null ? value : "", asset -> asset.sharedCooldownGroup)
            .add()
            .build();

    public static final ArrayCodec<AffixTriggeredEffectAsset> ARRAY_CODEC = new ArrayCodec<>(CODEC, AffixTriggeredEffectAsset[]::new);

    private TriggerAsset trigger = new TriggerAsset();
    private EffectAsset effect = new EffectAsset();
    private String stackBehavior = AffixTriggeredEffect.STACK_BEHAVIOR_INDEPENDENT;
    private int maxStacks = 1;
    private float cooldownSeconds = 0f;
    private String sharedCooldownGroup = "";

    public AffixTriggeredEffectAsset() {
    }

    @Nonnull
    public AffixTriggeredEffect toTriggeredEffect() {
        return new AffixTriggeredEffect(
                trigger.toTriggerDefinition(),
                effect.toEffectDefinition(),
                stackBehavior,
                maxStacks,
                cooldownSeconds,
                sharedCooldownGroup
        );
    }

    public static class TriggerAsset {
        private String type = "";
        private int chance = AffixTrigger.DEFAULT_CHANCE_BPS;
        private String[] damageCauses = new String[0];
        private String[] targetTags = new String[0];
        private int minDamage = 0;
        private float intervalSeconds = 0f;
        private boolean requireCombat = false;
        private String[] interactionTypes = new String[0];

        @Nonnull
        public AffixTrigger toTriggerDefinition() {
            List<String> damageList = damageCauses != null ? Arrays.asList(damageCauses) : Collections.emptyList();
            List<String> targetList = targetTags != null ? Arrays.asList(targetTags) : Collections.emptyList();
            List<String> interactionList = interactionTypes != null ? Arrays.asList(interactionTypes) : Collections.emptyList();
            return new AffixTrigger(
                    type != null ? type : "",
                    chance,
                    damageList,
                    targetList,
                    minDamage,
                    intervalSeconds,
                    requireCombat,
                    interactionList
            );
        }
    }

    public static class EffectAsset {
        private String type = "";
        private String projectileId = "";
        private int count = 1;
        private String pattern = "";
        private float velocity = 0f;
        private float spreadAngle = 0f;
        private float orbitRadius = 0f;
        private float rotationSpeed = 0f;
        private float durationSeconds = 0f;
        private String prefabPath = "";
        private Vector3d offset = Vector3d.ZERO;
        private String effectId = "";
        private String target = "";
        private float radius = 0f;
        private int damage = 0;
        private String damageCause = "";
        private boolean excludeSelf = false;
        private String interactionId = "";
        private String interactionType = "";
        private String statId = "";
        private int amount = 0;
        private String stackType = "";
        private float statDurationSeconds = 0f;

        @Nonnull
        public AffixEffect toEffectDefinition() {
            return new AffixEffect(
                    type != null ? type : "",
                    projectileId != null ? projectileId : "",
                    count,
                    pattern != null ? pattern : "",
                    velocity,
                    spreadAngle,
                    orbitRadius,
                    rotationSpeed,
                    durationSeconds,
                    prefabPath != null ? prefabPath : "",
                    offset != null ? offset : Vector3d.ZERO,
                    effectId != null ? effectId : "",
                    target != null ? target : "",
                    radius,
                    damage,
                    damageCause != null ? damageCause : "",
                    excludeSelf,
                    interactionId != null ? interactionId : "",
                    interactionType != null ? interactionType : "",
                    statId != null ? statId : "",
                    amount,
                    stackType != null ? stackType : "",
                    statDurationSeconds
            );
        }
    }
}
