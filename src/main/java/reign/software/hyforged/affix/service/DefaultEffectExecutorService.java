package reign.software.hyforged.affix.service;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.FastRandom;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.DespawnComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabBufferUtil;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.IPrefabBuffer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PrefabUtil;
import com.hypixel.hytale.server.core.util.TargetUtil;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.model.AffixEffect;
import reign.software.hyforged.affix.model.AffixTriggeredEffect;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation for executing triggered affix effects.
 */
public class DefaultEffectExecutorService implements EffectExecutorService {

    private static final Logger LOGGER = Logger.getLogger(DefaultEffectExecutorService.class.getName());

    @Override
    public boolean execute(@Nonnull AffixTriggeredEffect triggeredEffect, @Nonnull EffectContext context) {
        AffixEffect effect = triggeredEffect.effect();
        String type = effect.type().toLowerCase(Locale.ROOT);

        return switch (type) {
            case "spawn_projectile" -> spawnProjectiles(effect, context);
            case "spawn_prefab" -> spawnPrefab(effect, context);
            case "apply_effect" -> applyEntityEffect(effect, context);
            case "damage_area" -> damageArea(effect, context);
            case "run_interaction" -> runInteraction(effect, context);
            case "modify_stat" -> modifyStat(effect, context);
            default -> {
                LOGGER.log(Level.FINE, "Unknown effect type: {0}", effect.type());
                yield false;
            }
        };
    }

    private boolean spawnProjectiles(@Nonnull AffixEffect effect, @Nonnull EffectContext context) {
        if (effect.projectileId().isBlank()) {
            return false;
        }

        ComponentAccessor<EntityStore> accessor = context.accessor();
        Transform baseTransform = resolveSourceTransform(context.sourceRef(), accessor);
        if (baseTransform == null) {
            return false;
        }

        Vector3d origin = baseTransform.getPosition();
        Vector3f baseRotation = baseTransform.getRotation();
        String pattern = effect.pattern().isBlank() ? "forward" : effect.pattern().toLowerCase(Locale.ROOT);
        int count = effect.count();
        float velocityOverride = effect.velocity();
        float rotationSpeed = effect.rotationSpeed();
        float durationSeconds = effect.durationSeconds();

        Ref<EntityStore> sourceRef = context.sourceRef();
        UUIDComponent uuidComponent = accessor.getComponent(sourceRef, UUIDComponent.getComponentType());
        UUID sourceUuid = uuidComponent != null ? uuidComponent.getUuid() : null;
        if (sourceUuid == null) {
            return false;
        }

        TimeResource timeResource = accessor.getResource(TimeResource.getResourceType());
        if (timeResource == null) {
            return false;
        }

        int spawned = 0;
        switch (pattern) {
            case "spread" -> {
                float spreadAngle = effect.spreadAngle() > 0 ? effect.spreadAngle() : 15f;
                float start = -spreadAngle * 0.5f;
                float step = count > 1 ? spreadAngle / (count - 1) : 0f;
                for (int i = 0; i < count; i++) {
                    float yaw = baseRotation.getYaw() + (float) Math.toRadians(start + step * i);
                    Vector3f rotation = new Vector3f(baseRotation.getPitch(), yaw, baseRotation.getRoll());
                    Vector3d velocity = resolveVelocityOverride(velocityOverride, rotation);
                    if (spawnProjectileInstance(timeResource, effect.projectileId(), origin, rotation, sourceUuid, accessor, velocity, durationSeconds)) {
                        spawned++;
                    }
                }
            }
            case "nova" -> {
                for (int i = 0; i < count; i++) {
                    float yaw = (float) (Math.PI * 2 * i / count);
                    Vector3f rotation = new Vector3f(0f, yaw, 0f);
                    Vector3d velocity = resolveVelocityOverride(velocityOverride, rotation);
                    if (spawnProjectileInstance(timeResource, effect.projectileId(), origin, rotation, sourceUuid, accessor, velocity, durationSeconds)) {
                        spawned++;
                    }
                }
            }
            case "orbit" -> {
                float radius = effect.orbitRadius() > 0 ? effect.orbitRadius() : 2f;
                for (int i = 0; i < count; i++) {
                    double angle = Math.PI * 2 * i / count;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;
                    Vector3d position = new Vector3d(origin.getX() + offsetX, origin.getY(), origin.getZ() + offsetZ);
                    float yaw = (float) angle;
                    Vector3f rotation = new Vector3f(0f, yaw, 0f);
                    Vector3d velocity = resolveOrbitVelocity(angle, rotationSpeed, radius);
                    if (velocity == null) {
                        velocity = resolveVelocityOverride(velocityOverride, rotation);
                    }
                    if (spawnProjectileInstance(timeResource, effect.projectileId(), position, rotation, sourceUuid, accessor, velocity, durationSeconds)) {
                        spawned++;
                    }
                }
            }
            case "targeted" -> {
                Vector3d targetPos = resolvePosition(context, true);
                if (targetPos == null) {
                    return false;
                }
                Vector3d direction = new Vector3d(targetPos);
                direction.subtract(origin);
                Vector3f rotation = rotationFromDirection(direction);
                Vector3d velocity = resolveVelocityOverride(velocityOverride, rotation);
                if (spawnProjectileInstance(timeResource, effect.projectileId(), origin, rotation, sourceUuid, accessor, velocity, durationSeconds)) {
                    spawned++;
                }
            }
            case "ground" -> {
                Vector3d groundPos = resolvePosition(context, true);
                if (groundPos == null) {
                    return false;
                }
                Vector3f rotation = new Vector3f(0f, baseRotation.getYaw(), 0f);
                for (int i = 0; i < count; i++) {
                    Vector3d velocity = resolveVelocityOverride(velocityOverride, rotation);
                    if (spawnProjectileInstance(timeResource, effect.projectileId(), groundPos, rotation, sourceUuid, accessor, velocity, durationSeconds)) {
                        spawned++;
                    }
                }
            }
            case "random_area" -> {
                Vector3d center = resolvePosition(context, true);
                if (center == null) {
                    center = origin;
                }
                float areaRadius = effect.radius();
                if (areaRadius <= 0f) {
                    return false;
                }
                FastRandom random = new FastRandom();
                for (int i = 0; i < count; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double r = Math.sqrt(random.nextDouble()) * areaRadius;
                    double dx = Math.cos(angle) * r;
                    double dz = Math.sin(angle) * r;
                    Vector3d pos = new Vector3d(center.getX() + dx, center.getY(), center.getZ() + dz);
                    Vector3d velocity = resolveVelocityOverride(velocityOverride, baseRotation);
                    if (spawnProjectileInstance(timeResource, effect.projectileId(), pos, baseRotation, sourceUuid, accessor, velocity, durationSeconds)) {
                        spawned++;
                    }
                }
            }
            case "forward" -> {
                for (int i = 0; i < count; i++) {
                    Vector3d velocity = resolveVelocityOverride(velocityOverride, baseRotation);
                    if (spawnProjectileInstance(timeResource, effect.projectileId(), origin, baseRotation, sourceUuid, accessor, velocity, durationSeconds)) {
                        spawned++;
                    }
                }
            }
            default -> {
                for (int i = 0; i < count; i++) {
                    Vector3d velocity = resolveVelocityOverride(velocityOverride, baseRotation);
                    if (spawnProjectileInstance(timeResource, effect.projectileId(), origin, baseRotation, sourceUuid, accessor, velocity, durationSeconds)) {
                        spawned++;
                    }
                }
            }
        }

        return spawned > 0;
    }

    private boolean spawnProjectileInstance(
            @Nonnull TimeResource timeResource,
            @Nonnull String projectileId,
            @Nonnull Vector3d position,
            @Nonnull Vector3f rotation,
            @Nonnull UUID sourceUuid,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nullable Vector3d velocityOverride,
            float durationSeconds
    ) {
        Holder<EntityStore> holder = ProjectileComponent.assembleDefaultProjectile(timeResource, projectileId, position, rotation);
        ProjectileComponent projectileComponent = holder.getComponent(ProjectileComponent.getComponentType());
        if (projectileComponent == null) {
            return false;
        }
        holder.ensureComponent(Intangible.getComponentType());
        if (projectileComponent.getProjectile() == null) {
            projectileComponent.initialize();
            if (projectileComponent.getProjectile() == null) {
                return false;
            }
        }

        projectileComponent.shoot(holder, sourceUuid, position.getX(), position.getY(), position.getZ(), rotation.getYaw(), rotation.getPitch());
        if (velocityOverride != null) {
            projectileComponent.getSimplePhysicsProvider().setVelocity(velocityOverride);
            Velocity velocityComponent = holder.getComponent(Velocity.getComponentType());
            if (velocityComponent != null) {
                velocityComponent.set(velocityOverride);
            }
        }
        if (durationSeconds > 0f) {
            DespawnComponent despawn = holder.getComponent(DespawnComponent.getComponentType());
            if (despawn != null) {
                despawn.setDespawnTo(timeResource.getNow(), durationSeconds);
            } else {
                holder.putComponent(DespawnComponent.getComponentType(), DespawnComponent.despawnInSeconds(timeResource, durationSeconds));
            }
        }
        return addEntity(holder, accessor);
    }

    @Nullable
    private Vector3d resolveVelocityOverride(float velocity, @Nonnull Vector3f rotation) {
        if (velocity <= 0f) {
            return null;
        }
        Vector3d direction = new Vector3d();
        PhysicsMath.vectorFromAngles(rotation.getYaw(), rotation.getPitch(), direction);
        direction.setLength(velocity);
        return direction;
    }

    @Nullable
    private Vector3d resolveOrbitVelocity(double angle, float rotationSpeed, float radius) {
        if (rotationSpeed <= 0f || radius <= 0f) {
            return null;
        }
        double angularSpeed = Math.toRadians(rotationSpeed);
        double speed = angularSpeed * radius;
        double vx = -Math.sin(angle) * speed;
        double vz = Math.cos(angle) * speed;
        return new Vector3d(vx, 0.0, vz);
    }

    private boolean spawnPrefab(@Nonnull AffixEffect effect, @Nonnull EffectContext context) {
        if (effect.prefabPath().isBlank()) {
            return false;
        }
        ComponentAccessor<EntityStore> accessor = context.accessor();
        World world = accessor.getExternalData().getWorld();
        if (world == null) {
            return false;
        }

        Vector3d position = resolvePosition(context, "target".equalsIgnoreCase(effect.target()));
        if (position == null) {
            return false;
        }

        IPrefabBuffer prefab = PrefabBufferUtil.getCached(PrefabStore.get().getAssetPrefabsPath().resolve(effect.prefabPath()));
        if (prefab == null) {
            LOGGER.log(Level.FINE, "Prefab not found: {0}", effect.prefabPath());
            return false;
        }

        Vector3d offset = effect.offset();
        int count = Math.max(1, effect.count());
        float spawnRadius = effect.spawnRadius();
        FastRandom random = new FastRandom();
        boolean spawned = false;

        for (int i = 0; i < count; i++) {
            double dx = 0.0;
            double dz = 0.0;
            if (spawnRadius > 0f) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(random.nextDouble()) * spawnRadius;
                dx = Math.cos(angle) * r;
                dz = Math.sin(angle) * r;
            }
            Vector3d adjusted = new Vector3d(
                    position.getX() + offset.getX() + dx,
                    position.getY() + offset.getY(),
                    position.getZ() + offset.getZ() + dz
            );
            PrefabUtil.paste(prefab, world, adjusted.toVector3i(), Rotation.None, false, random, accessor);
            spawned = true;
        }

        return spawned;
    }

    private boolean applyEntityEffect(@Nonnull AffixEffect effect, @Nonnull EffectContext context) {
        String effectId = effect.effectId();
        if (effectId.isBlank()) {
            return false;
        }

        ComponentAccessor<EntityStore> accessor = context.accessor();
        EntityEffect entityEffect = EntityEffect.getAssetMap().getAsset(effectId);
        if (entityEffect == null) {
            LOGGER.log(Level.FINE, "EntityEffect not found: {0}", effectId);
            return false;
        }

        float duration = effect.durationSeconds();
        float radius = effect.radius();
        boolean applied = false;

        if (radius > 0f) {
            Vector3d position = resolvePosition(context, "target".equalsIgnoreCase(effect.target()));
            if (position == null) {
                return false;
            }
            List<Ref<EntityStore>> targets = TargetUtil.getAllEntitiesInSphere(position, radius, accessor);
            for (Ref<EntityStore> targetRef : targets) {
                if (targetRef == null || !targetRef.isValid()) {
                    continue;
                }
                if (effect.excludeSelf() && Objects.equals(targetRef, context.sourceRef())) {
                    continue;
                }
                applied |= applyEffectToTarget(entityEffect, duration, targetRef, accessor);
                applied |= applyStatModifiers(effect, context, targetRef, duration);
            }
            return applied;
        }

        Ref<EntityStore> targetRef = resolveTargetRef(effect, context);
        if (targetRef == null || !targetRef.isValid()) {
            return false;
        }

        applied = applyEffectToTarget(entityEffect, duration, targetRef, accessor);
        applied |= applyStatModifiers(effect, context, targetRef, duration);
        return applied;
    }

    private boolean applyEffectToTarget(
            @Nonnull EntityEffect entityEffect,
            float durationSeconds,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        EffectControllerComponent effectController = accessor.getComponent(targetRef, EffectControllerComponent.getComponentType());
        if (effectController == null) {
            return false;
        }
        if (durationSeconds > 0f) {
            return effectController.addEffect(targetRef, entityEffect, durationSeconds, OverlapBehavior.OVERWRITE, accessor);
        }
        return effectController.addEffect(targetRef, entityEffect, accessor);
    }

    private boolean applyStatModifiers(
            @Nonnull AffixEffect effect,
            @Nonnull EffectContext context,
            @Nonnull Ref<EntityStore> targetRef,
            float durationSeconds
    ) {
        if (effect.statModifiers().isEmpty()) {
            return false;
        }
        float duration = durationSeconds > 0f ? durationSeconds
                : (effect.statDurationSeconds() > 0f ? effect.statDurationSeconds() : effect.durationSeconds());

        boolean applied = false;
        for (var entry : effect.statModifiers().entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            int amount = entry.getValue() != null ? entry.getValue() : 0;
            if (amount == 0) {
                continue;
            }
            applied |= applyStatModifier(entry.getKey(), amount, effect.stackType(), duration, context, targetRef);
        }
        return applied;
    }

    private boolean damageArea(@Nonnull AffixEffect effect, @Nonnull EffectContext context) {
        float radius = effect.radius();
        int damageAmount = resolveDamageAmount(effect);
        if (radius <= 0f || damageAmount <= 0) {
            return false;
        }

        ComponentAccessor<EntityStore> accessor = context.accessor();
        Vector3d position = resolvePosition(context, false);
        if (position == null) {
            return false;
        }

        int damageCauseIndex = resolveDamageCauseIndex(effect.damageCause());
        if (damageCauseIndex == Integer.MIN_VALUE) {
            return false;
        }

        EntityEffect applyEffect = null;
        String applyEffectId = effect.applyEffectId();
        if (!applyEffectId.isBlank()) {
            applyEffect = EntityEffect.getAssetMap().getAsset(applyEffectId);
            if (applyEffect == null) {
                LOGGER.log(Level.FINE, "EntityEffect not found: {0}", applyEffectId);
            }
        }
        float applyDuration = effect.applyEffectDurationSeconds() > 0f
                ? effect.applyEffectDurationSeconds()
                : effect.durationSeconds();

        List<Ref<EntityStore>> targets = TargetUtil.getAllEntitiesInSphere(position, radius, accessor);
        boolean executed = false;
        for (Ref<EntityStore> targetRef : targets) {
            if (targetRef == null || !targetRef.isValid()) {
                continue;
            }
            if (effect.excludeSelf() && Objects.equals(targetRef, context.sourceRef())) {
                continue;
            }
            Damage damage = new Damage(new Damage.EntitySource(context.sourceRef()), damageCauseIndex, damageAmount);
            DamageSystems.executeDamage(targetRef, accessor, damage);
            if (applyEffect != null) {
                applyEffectToTarget(applyEffect, applyDuration, targetRef, accessor);
            }
            executed = true;
        }

        return executed;
    }

    private int resolveDamageAmount(@Nonnull AffixEffect effect) {
        int baseDamage = effect.damage();
        if (baseDamage <= 0) {
            return 0;
        }
        float scaling = effect.damageScaling();
        if (scaling > 0f) {
            return Math.round(baseDamage * scaling);
        }
        return baseDamage;
    }

    private boolean runInteraction(@Nonnull AffixEffect effect, @Nonnull EffectContext context) {
        if (effect.interactionId().isBlank()) {
            return false;
        }
        if (!(context.accessor() instanceof CommandBuffer<?> commandBuffer)) {
            LOGGER.fine("Interaction execution requires CommandBuffer context");
            return false;
        }

        @SuppressWarnings("unchecked")
        CommandBuffer<EntityStore> entityCommandBuffer = (CommandBuffer<EntityStore>) commandBuffer;
        Ref<EntityStore> targetRef = resolveTargetRef(effect, context);
        if (targetRef == null || !targetRef.isValid()) {
            return false;
        }

        InteractionManager manager = entityCommandBuffer.getComponent(targetRef, InteractionModule.get().getInteractionManagerComponent());
        if (manager == null) {
            return false;
        }

        InteractionType interactionType = parseInteractionType(effect.interactionType());
        RootInteraction root = RootInteraction.getAssetMap().getAsset(effect.interactionId());
        if (root == null) {
            LOGGER.log(Level.FINE, "RootInteraction not found: {0}", effect.interactionId());
            return false;
        }

        InteractionContext interactionContext = InteractionContext.forInteraction(manager, targetRef, interactionType, entityCommandBuffer);
        return manager.tryStartChain(targetRef, entityCommandBuffer, interactionType, interactionContext, root);
    }

    private boolean modifyStat(@Nonnull AffixEffect effect, @Nonnull EffectContext context) {
        Ref<EntityStore> targetRef = resolveTargetRef(effect, context);
        if (targetRef == null || !targetRef.isValid()) {
            return false;
        }
        float duration = effect.statDurationSeconds() > 0f ? effect.statDurationSeconds() : effect.durationSeconds();

        boolean applied = false;
        if (!effect.statModifiers().isEmpty()) {
            for (var entry : effect.statModifiers().entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                int amount = entry.getValue() != null ? entry.getValue() : 0;
                if (amount == 0) {
                    continue;
                }
                applied |= applyStatModifier(entry.getKey(), amount, effect.stackType(), duration, context, targetRef);
            }
        }

        if (!effect.statId().isBlank() && effect.amount() != 0) {
            applied |= applyStatModifier(effect.statId(), effect.amount(), effect.stackType(), duration, context, targetRef);
        }

        return applied;
    }

    private boolean applyStatModifier(
            @Nonnull String statIdValue,
            int amount,
            @Nullable String stackType,
            float durationSeconds,
            @Nonnull EffectContext context,
            @Nonnull Ref<EntityStore> targetRef
    ) {
        Store<EntityStore> store = context.getStore();
        if (store == null) {
            return false;
        }

        StatId statId = StatId.parse(statIdValue);
        int statIndex = StatDefinitionRegistry.get().getIndex(statId);
        if (statIndex < 0) {
            LOGGER.log(Level.FINE, "Unknown stat for modify_stat: {0}", statIdValue);
            return false;
        }

        long expirationTick = 0L;
        if (durationSeconds > 0f) {
            World world = store.getExternalData().getWorld();
            if (world != null) {
                long currentTick = world.getTick();
                long ticks = Math.round(durationSeconds * world.getTps());
                expirationTick = currentTick + ticks;
            }
        }

        String sourceKey = "affix-effect:" + context.effectKey() + ":" + statIdValue;
        HyforgedModifier modifier = HyforgedModifier.builder()
                .sourceType(HyforgedModifier.SourceType.EFFECT)
                .sourceId(sourceKey)
                .stackType(parseStackType(stackType))
                .amount(amount)
                .targetStat(statIndex)
                .priority(0)
                .expiresAt(expirationTick)
                .build();

        EntityStatMap statMap = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (statMap != null && StatAccessor.hasStatSlot(statMap, statIndex)) {
            statMap.putModifier(statIndex, sourceKey, modifier);
            return true;
        }

        HyforgedStatComponent statComponent = store.getComponent(targetRef, HyforgedPlugin.getInstance().getHyforgedStatComponentType());
        if (statComponent != null) {
            statComponent.upsertModifier(modifier);
            return true;
        }

        return false;
    }

    @Nullable
    private Transform resolveSourceTransform(@Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> accessor) {
        TransformComponent transformComponent = accessor.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent == null) {
            return null;
        }

        Vector3d position = transformComponent.getPosition();
        Vector3f rotation = transformComponent.getRotation();

        HeadRotation headRotation = accessor.getComponent(ref, HeadRotation.getComponentType());
        if (headRotation != null) {
            Vector3f headRot = headRotation.getRotation();
            float eyeHeight = 0f;
            ModelComponent modelComponent = accessor.getComponent(ref, ModelComponent.getComponentType());
            if (modelComponent != null) {
                eyeHeight = modelComponent.getModel().getEyeHeight(ref, accessor);
            }
            return new Transform(
                    position.getX(),
                    position.getY() + eyeHeight,
                    position.getZ(),
                    headRot.getPitch(),
                    headRot.getYaw(),
                    headRot.getRoll()
            );
        }

        return new Transform(position, rotation);
    }

    @Nullable
    private Vector3d resolvePosition(@Nonnull EffectContext context, boolean preferTarget) {
        if (context.position() != null) {
            return context.position();
        }

        Ref<EntityStore> ref = preferTarget ? context.targetRef() : context.sourceRef();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        TransformComponent transformComponent = context.accessor().getComponent(ref, TransformComponent.getComponentType());
        return transformComponent != null ? transformComponent.getPosition() : null;
    }

    @Nullable
    private Ref<EntityStore> resolveTargetRef(@Nonnull AffixEffect effect, @Nonnull EffectContext context) {
        String target = effect.target();
        if (target == null || target.isBlank()) {
            return context.sourceRef();
        }
        String normalized = target.toLowerCase(Locale.ROOT);
        if (normalized.equals("self") || normalized.equals("source")) {
            return context.sourceRef();
        }
        if (normalized.equals("target") || normalized.equals("hit_target")) {
            return context.targetRef() != null ? context.targetRef() : context.sourceRef();
        }
        return context.sourceRef();
    }

    private boolean addEntity(@Nonnull Holder<EntityStore> holder, @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (accessor instanceof CommandBuffer<?> commandBuffer) {
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> entityCommandBuffer = (CommandBuffer<EntityStore>) commandBuffer;
            entityCommandBuffer.addEntity(holder, AddReason.SPAWN);
            return true;
        }
        if (accessor instanceof Store<?> store) {
            @SuppressWarnings("unchecked")
            Store<EntityStore> entityStore = (Store<EntityStore>) store;
            entityStore.addEntity(holder, AddReason.SPAWN);
            return true;
        }
        return false;
    }

    private int resolveDamageCauseIndex(@Nullable String damageCauseId) {
        if (damageCauseId == null || damageCauseId.isBlank()) {
            return DamageCause.getAssetMap().getIndex("Physical");
        }
        int index = DamageCause.getAssetMap().getIndex(damageCauseId);
        if (index == Integer.MIN_VALUE) {
            LOGGER.log(Level.FINE, "Unknown damage cause: {0}", damageCauseId);
        }
        return index;
    }

    @Nonnull
    private Vector3f rotationFromDirection(@Nonnull Vector3d direction) {
        double length = Math.sqrt(direction.getX() * direction.getX() + direction.getY() * direction.getY() + direction.getZ() * direction.getZ());
        if (length == 0) {
            return new Vector3f(0f, 0f, 0f);
        }
        double normX = direction.getX() / length;
        double normY = direction.getY() / length;
        double normZ = direction.getZ() / length;

        float pitch = (float) Math.asin(normY);
        float yaw = (float) Math.atan2(-normX, -normZ);
        return new Vector3f(pitch, yaw, 0f);
    }

    @Nonnull
    private InteractionType parseInteractionType(@Nullable String type) {
        if (type == null || type.isBlank()) {
            return InteractionType.Primary;
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        try {
            return InteractionType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return InteractionType.Primary;
        }
    }

    @Nonnull
    private HyforgedModifier.StackType parseStackType(@Nullable String stackType) {
        if (stackType == null || stackType.isBlank()) {
            return HyforgedModifier.StackType.FLAT;
        }
        try {
            return HyforgedModifier.StackType.valueOf(stackType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return HyforgedModifier.StackType.FLAT;
        }
    }
}
