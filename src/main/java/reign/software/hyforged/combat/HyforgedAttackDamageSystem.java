package reign.software.hyforged.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Injects the attacker's computed {@code attack-power} stat as the base physical
 * weapon damage, replacing the vanilla weapon {@code BaseDamage: 0} placeholder.
 * <p>
 * Hyforged weapon JSON files intentionally set {@code BaseDamage: {Physical: 0}}
 * so that weapon damage is fully driven by character stats rather than by item tier.
 * This system sets the damage amount to the attacker's {@code hyforged:attack-power}
 * stat before percentage-based modifiers ({@link HyforgedDamageBonusSystem}) run.
 * <p>
 * Attack-power formula (JSON-driven): {@code attack-power = strength * 2}.
 * <p>
 * Applies to:
 * <ul>
 *   <li>Physical damage events from entity sources</li>
 *   <li>Player attackers only (NPCs retain their own BaseDamage values)</li>
 *   <li>Non-bypass, non-pipeline-processed damage events</li>
 * </ul>
 */
public class HyforgedAttackDamageSystem extends DamageEventSystem {

    private static final StatId ATTACK_POWER = StatId.hyforged("attack-power");

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    // Cached indices, initialised lazily on first call
    private int attackPowerIndex = -1;
    private int physicalDamageCauseIndex = -1;
    private boolean indicesCached = false;

    public HyforgedAttackDamageSystem() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.playerComponentType = Player.getComponentType();

        // Match any entity with an EntityStatMap (defender side — we read attacker from source)
        this.query = StatAccessor.getStatMapType();

        // Run in gather group; no ordering constraints needed
        this.dependencies = Set.of();
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getGatherDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damage
    ) {
        // Skip cancelled events
        if (damage.isCancelled()) {
            return;
        }

        // Skip CombatService-created damage (abilities, DoTs — they set their own amounts)
        Boolean pipelineProcessed = damage.getIfPresentMetaObject(CombatMeta.PIPELINE_PROCESSED);
        if (pipelineProcessed != null && pipelineProcessed) {
            return;
        }

        // Only entity-sourced attacks
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (!attackerRef.isValid()) {
            return;
        }

        // Only applies to player attackers — NPC weapons retain their own BaseDamage
        Player player = store.getComponent(attackerRef, playerComponentType);
        if (player == null) {
            return;
        }

        // Skip bypass-resistance damage (environmental, fall damage, etc.)
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause == null || damageCause.doesBypassResistances()) {
            return;
        }

        // Only inject for Physical damage (melee weapon attacks)
        ensureIndicesCached();
        if (physicalDamageCauseIndex < 0 || damage.getDamageCauseIndex() != physicalDamageCauseIndex) {
            return;
        }

        // Read attacker's computed attack-power from HyforgedStatComponent
        HyforgedStatComponent statComponent = store.getComponent(attackerRef, statComponentType);
        if (statComponent == null) {
            return;
        }

        if (attackPowerIndex < 0) {
            return;
        }

        int attackPower = statComponent.getCachedValue(attackPowerIndex);
        if (attackPower <= 0) {
            return;
        }

        damage.setAmount(attackPower);
    }

    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }
        attackPowerIndex = StatDefinitionRegistry.get().getIndex(ATTACK_POWER);
        physicalDamageCauseIndex = DamageCause.getAssetMap().getIndex("Physical");
        indicesCached = true;
    }
}
