package reign.software.hyforged.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.bridge.ProgressionStatBridge;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Hit resolution system that performs accuracy vs evasion checks.
 * <p>
 * This system runs in the {@code gatherDamageGroup} before damage filtering,
 * determining whether an attack hits based on:
 * <ul>
 *   <li>Attacker's accuracy rating</li>
 *   <li>Defender's evasion chance</li>
 *   <li>Level difference between attacker and defender</li>
 * </ul>
 * <p>
 * If an attack misses, the damage event is cancelled and a MISS meta flag is set.
 *
 * @see CombatMath#calculateHitChance(int, int, int, int)
 */
public class HyforgedHitResolutionSystem extends DamageEventSystem {

    /** Meta key to mark a damage event as a miss */
    public static final MetaKey<Boolean> MISS = Damage.META_REGISTRY.registerMetaObject(data -> Boolean.FALSE);
    
    /** Stat ID for accuracy rating (attacker) */
    private static final StatId ACCURACY_RATING = StatId.hyforged("accuracy-rating");
    
    /** Stat ID for evasion chance (defender) */
    private static final StatId EVASION_CHANCE = StatId.hyforged("evasion-chance-bps");
    
    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;
    
    // Cached stat indices
    private int accuracyIndex = -1;
    private int evasionIndex = -1;
    private boolean indicesCached = false;

    public HyforgedHitResolutionSystem() {
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        
        // Query for entities with HyforgedStatComponent (defender must have stats)
        this.query = statComponentType;
        
        // Run in gather group before any damage filtering
        // This is the earliest point we can intercept damage
        this.dependencies = Set.of(
            new SystemDependency<>(Order.BEFORE, DamageSystems.ApplyDamage.class)
        );
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        // Run in gather damage group - earliest damage processing phase
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
        // Skip if already cancelled
        if (damage.isCancelled()) {
            return;
        }
        
        // Skip if damage is zero
        if (damage.getAmount() <= 0) {
            return;
        }
        
        // Skip if already processed by CombatService (avoids re-applying pipeline)
        Boolean pipelineProcessed = damage.getIfPresentMetaObject(CombatMeta.PIPELINE_PROCESSED);
        if (pipelineProcessed != null && pipelineProcessed) {
            return;
        }
        
        // Only apply to entity-sourced damage (attacks from entities)
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }
        
        // Get damage cause to check if it can be evaded
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause == null) {
            return;
        }
        
        // Environmental damage, fall damage, etc. cannot be evaded
        // Only "attack" type damage should be evadable
        // For now, we skip damage that bypasses resistances (typically environmental)
        if (damageCause.doesBypassResistances()) {
            return;
        }
        
        // Record base damage for combat log (before any modifications)
        damage.putMetaObject(CombatMeta.BASE_DAMAGE, damage.getAmount());
        
        // Cache stat indices
        ensureIndicesCached();
        
        // Get defender's stat component
        HyforgedStatComponent defenderStats = archetypeChunk.getComponent(index, statComponentType);
        if (defenderStats == null) {
            return;
        }
        
        // Get attacker's stat component
        Ref<EntityStore> attackerRef = entitySource.getRef();
        HyforgedStatComponent attackerStats = store.getComponent(attackerRef, statComponentType);
        
        // Get levels for level difference calculation
        int attackerLevel = ProgressionStatBridge.getCharacterLevel(attackerRef, store);
        Ref<EntityStore> defenderRef = archetypeChunk.getReferenceTo(index);
        int defenderLevel = ProgressionStatBridge.getCharacterLevel(defenderRef, store);
        
        // Get accuracy from attacker (0 if no stats)
        int attackerAccuracy = 0;
        if (attackerStats != null && accuracyIndex >= 0) {
            attackerAccuracy = attackerStats.getCachedValue(accuracyIndex);
        }
        
        // Get evasion chance from defender
        int defenderEvasion = 0;
        if (evasionIndex >= 0) {
            defenderEvasion = defenderStats.getCachedValue(evasionIndex);
        }
        
        // Skip hit resolution if defender has no evasion
        if (defenderEvasion <= 0) {
            return;
        }
        
        // Calculate hit chance
        int hitChance = CombatMath.calculateHitChance(
            attackerAccuracy, 
            defenderEvasion, 
            attackerLevel, 
            defenderLevel
        );
        
        // Roll to hit
        if (!CombatMath.rollChance(hitChance)) {
            // Attack missed!
            damage.putMetaObject(MISS, Boolean.TRUE);
            damage.setCancelled(true);
        }
    }
    
    /**
     * Cache stat indices for performance.
     */
    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }
        
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        accuracyIndex = registry.getIndex(ACCURACY_RATING);
        evasionIndex = registry.getIndex(EVASION_CHANCE);
        indicesCached = true;
    }
}
