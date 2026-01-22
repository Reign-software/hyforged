package reign.software.hyforged.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
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
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.bridge.ProgressionStatBridge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Critical hit system that applies crit multiplier to damage.
 * <p>
 * This system runs in the {@code inspectDamageGroup} after damage reduction,
 * rolling crit chance on the attacker and applying the crit multiplier if successful.
 * <p>
 * Critical hits:
 * <ul>
 *   <li>Are rolled based on attacker's {@code crit-chance-bps} stat (capped per stat definition)</li>
 *   <li>Apply a penalty against higher-level targets</li>
 *   <li>Multiply final damage by {@code (1 + crit-multiplier-bps / 10000)}</li>
 *   <li>Set a meta key for combat log and visual feedback</li>
 *   <li>Share crit roll outcome across all damage types in a multi-element attack</li>
 * </ul>
 *
 * @see CombatMath#calculateCritChance(int, int, int)
 */
public class HyforgedCriticalHitSystem extends DamageEventSystem {

    /** Meta key to mark that this damage was a critical hit */
    public static final MetaKey<Boolean> CRITICAL_HIT = Damage.META_REGISTRY.registerMetaObject(data -> Boolean.FALSE);
    
    /** Meta key to store the crit multiplier that was applied (for combat log) */
    public static final MetaKey<Integer> CRITICAL_MULTIPLIER = Damage.META_REGISTRY.registerMetaObject(data -> 0);
    
    /** 
     * Meta key to mark that crit was already rolled for this attack.
     * <p>
     * This ensures multi-element attacks share a single crit roll outcome.
     * Value: TRUE = already rolled, apply cached result; not present = need to roll.
     */
    public static final MetaKey<Boolean> CRIT_ROLLED = Damage.META_REGISTRY.registerMetaObject(data -> Boolean.FALSE);
    
    /** Default crit multiplier bonus in bps (50% = 5000 bps additional damage) */
    private static final int DEFAULT_CRIT_MULTIPLIER_BPS = 5000;
    
    /** Stat ID for crit chance */
    private static final StatId CRIT_CHANCE = StatId.hyforged("crit-chance-bps");
    
    /** Stat ID for crit multiplier */
    private static final StatId CRIT_MULTIPLIER = StatId.hyforged("crit-multiplier-bps");

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;
    
    // Cached stat indices
    private int critChanceIndex = -1;
    private int critMultiplierIndex = -1;
    private boolean indicesCached = false;

    public HyforgedCriticalHitSystem() {
        // Query for entities with EntityStatMap (for stat access)
        this.query = StatAccessor.getStatMapType();
        
        // Run in inspect group after damage has been calculated/reduced
        // but before entity UI events for crit display
        this.dependencies = Set.of(
            new SystemDependency<>(Order.BEFORE, DamageSystems.EntityUIEvents.class)
        );
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
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
        // Skip if already cancelled or zero damage
        if (damage.isCancelled() || damage.getAmount() <= 0) {
            return;
        }
        
        // Only entity-sourced damage can crit
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) {
            return;
        }
        
        // Skip if already processed by CombatService (avoids re-applying pipeline)
        Boolean pipelineProcessed = damage.getIfPresentMetaObject(CombatMeta.PIPELINE_PROCESSED);
        if (pipelineProcessed != null && pipelineProcessed) {
            return;
        }
        
        // Get the attacker reference
        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (!attackerRef.isValid()) {
            return;
        }
        
        // Check if this damage type can crit (environmental damage cannot)
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause != null && damageCause.doesBypassResistances()) {
            return;
        }
        
        // Check if crit was already rolled for this attack (multi-element cohesion)
        Boolean alreadyRolled = damage.getIfPresentMetaObject(CRIT_ROLLED);
        if (alreadyRolled != null && alreadyRolled) {
            // Apply cached crit result if it was a crit
            Boolean wasCrit = damage.getIfPresentMetaObject(CRITICAL_HIT);
            if (wasCrit != null && wasCrit) {
                Integer cachedMultiplier = damage.getIfPresentMetaObject(CRITICAL_MULTIPLIER);
                if (cachedMultiplier != null && cachedMultiplier > 0) {
                    float originalDamage = damage.getAmount();
                    float totalMultiplier = 1.0f + (cachedMultiplier / (float) CombatMath.BPS_100);
                    damage.setAmount(originalDamage * totalMultiplier);
                }
            }
            return;
        }
        
        // Mark that we've rolled crit for this attack
        damage.putMetaObject(CRIT_ROLLED, Boolean.TRUE);
        
        // Cache stat indices
        ensureIndicesCached();
        
        // Get crit chance
        int critChanceBps = 0;
        if (critChanceIndex >= 0) {
            critChanceBps = StatAccessor.getStatValueInt(store, attackerRef, critChanceIndex);
        }
        
        // Skip if no crit chance
        if (critChanceBps <= 0) {
            return;
        }
        
        // Get level difference for crit penalty
        int attackerLevel = ProgressionStatBridge.getCharacterLevel(attackerRef, store);
        int defenderLevel = ProgressionStatBridge.getCharacterLevel(archetypeChunk.getReferenceTo(index), store);
        
        // Calculate effective crit chance with level penalty
        int effectiveCritChance = CombatMath.calculateCritChance(critChanceBps, attackerLevel, defenderLevel);
        
        // Roll crit using seeded RNG
        if (!CombatMath.rollChance(effectiveCritChance)) {
            return;
        }
        
        // Crit successful!
        
        // Get crit multiplier (this is bonus damage, not total multiplier)
        int critMultiplierBps = DEFAULT_CRIT_MULTIPLIER_BPS;
        if (critMultiplierIndex >= 0) {
            int statValue = StatAccessor.getStatValueInt(store, attackerRef, critMultiplierIndex);
            if (statValue > 0) {
                critMultiplierBps = statValue;
            }
        }
        
        // Apply crit multiplier: damage = damage * (1 + critMultiplier/10000)
        // e.g., 1500 bps = 15% bonus = 1.15x multiplier
        float originalDamage = damage.getAmount();
        float totalMultiplier = 1.0f + (critMultiplierBps / (float) CombatMath.BPS_100);
        float critDamage = originalDamage * totalMultiplier;
        damage.setAmount(critDamage);
        
        // Mark as critical hit for combat log / visual effects
        damage.putMetaObject(CRITICAL_HIT, Boolean.TRUE);
        damage.putMetaObject(CRITICAL_MULTIPLIER, critMultiplierBps);
    }
    
    /**
     * Cache stat indices for performance.
     */
    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }
        
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        critChanceIndex = registry.getIndex(CRIT_CHANCE);
        critMultiplierIndex = registry.getIndex(CRIT_MULTIPLIER);
        indicesCached = true;
    }
}
