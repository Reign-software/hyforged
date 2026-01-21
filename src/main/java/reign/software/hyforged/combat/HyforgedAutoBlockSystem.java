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
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
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
 * Auto-block system that provides chance-based blocking for entities with block chance.
 * <p>
 * This system runs in the {@code filterDamageGroup} after hit resolution but before 
 * damage reduction, checking if the defender can auto-block the incoming attack.
 * <p>
 * Auto-block differs from manual blocking:
 * <ul>
 *   <li>Triggers automatically based on block chance stat</li>
 *   <li>Provides partial damage mitigation (default 50%)</li>
 *   <li>Consumes much less stamina (default 10% of manual block cost)</li>
 *   <li>Does not trigger if entity is already manually blocking</li>
 * </ul>
 *
 * @see CombatMath#calculateBlockChance(int)
 */
public class HyforgedAutoBlockSystem extends DamageEventSystem {

    /** Meta key to mark that auto-block was triggered */
    public static final MetaKey<Boolean> AUTO_BLOCKED = Damage.META_REGISTRY.registerMetaObject(data -> Boolean.FALSE);
    
    /** Default stamina cost per block (in stamina units) */
    private static final float DEFAULT_BLOCK_STAMINA_COST = 10.0f;
    
    /** Stat ID for block chance */
    private static final StatId BLOCK_CHANCE = StatId.hyforged("block-chance-bps");
    
    /** Stat ID for block mitigation */
    private static final StatId BLOCK_MITIGATION = StatId.hyforged("block-mitigation-bps");
    
    /** Stat ID for auto-block stamina cost modifier */
    private static final StatId AUTO_BLOCK_STAMINA_COST = StatId.hyforged("auto-block-stamina-cost-bps");
    
    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;
    
    // Cached stat indices
    private int blockChanceIndex = -1;
    private int blockMitigationIndex = -1;
    private int autoBlockStaminaCostIndex = -1;
    private boolean indicesCached = false;

    public HyforgedAutoBlockSystem() {
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        
        // Query for entities with both HyforgedStatComponent and EntityStatMap (for stamina)
        this.query = Query.and(statComponentType, EntityStatMap.getComponentType());
        
        // Run in filter group after hit resolution, before damage reduction
        this.dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, HyforgedHitResolutionSystem.class),
            new SystemDependency<>(Order.BEFORE, DamageSystems.ApplyDamage.class)
        );
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
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
        // Skip if already cancelled (e.g., missed)
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
        
        // Skip if already manually blocked (Hytale's native blocking)
        Boolean isBlocked = damage.getIfPresentMetaObject(Damage.BLOCKED);
        if (isBlocked != null && isBlocked) {
            return;
        }
        
        // Get damage cause to check if it can be blocked
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause == null) {
            return;
        }
        
        // Environmental damage cannot be blocked
        if (damageCause.doesBypassResistances()) {
            return;
        }
        
        // Cache stat indices
        ensureIndicesCached();
        
        // Get defender's stat component
        HyforgedStatComponent defenderStats = archetypeChunk.getComponent(index, statComponentType);
        if (defenderStats == null) {
            return;
        }
        
        // Get block chance
        int blockChanceBps = 0;
        if (blockChanceIndex >= 0) {
            blockChanceBps = defenderStats.getCachedValue(blockChanceIndex);
        }
        
        // Skip if no block chance
        if (blockChanceBps <= 0) {
            return;
        }
        
        // Get EntityStatMap for stamina check
        EntityStatMap entityStatMap = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return;
        }
        
        // Check if defender has enough stamina
        int staminaStatIndex = DefaultEntityStatTypes.getStamina();
        var staminaStat = entityStatMap.get(staminaStatIndex);
        if (staminaStat == null) {
            return;
        }
        
        float currentStamina = staminaStat.get();
        if (currentStamina <= 0) {
            return;
        }
        
        // Get levels for level difference penalty
        Ref<EntityStore> defenderRef = archetypeChunk.getReferenceTo(index);
        int defenderLevel = ProgressionStatBridge.getCharacterLevel(defenderRef, store);
        int attackerLevel = 1; // Default if no entity source
        
        if (damage.getSource() instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef.isValid()) {
                attackerLevel = ProgressionStatBridge.getCharacterLevel(attackerRef, store);
            }
        }
        
        // Calculate block chance with level penalty
        int effectiveBlockChance = CombatMath.calculateBlockChance(blockChanceBps, defenderLevel, attackerLevel);
        
        // Roll block chance
        if (!CombatMath.rollChance(effectiveBlockChance)) {
            return;
        }
        
        // Block successful!
        
        // Get block mitigation (default 50% = 5000 bps)
        int blockMitigationBps = 5000; // default
        if (blockMitigationIndex >= 0) {
            int statValue = defenderStats.getCachedValue(blockMitigationIndex);
            if (statValue > 0) {
                blockMitigationBps = statValue;
            }
        }
        
        // Apply damage reduction
        float originalDamage = damage.getAmount();
        float mitigatedDamage = CombatMath.applyReduction(originalDamage, blockMitigationBps);
        damage.setAmount(mitigatedDamage);
        
        // Record block mitigation for combat log
        damage.putMetaObject(CombatMeta.BLOCK_MITIGATION_BPS, blockMitigationBps);
        
        // Get auto-block stamina cost modifier (default 10% = 1000 bps)
        int staminaCostBps = 1000; // default 10%
        if (autoBlockStaminaCostIndex >= 0) {
            int statValue = defenderStats.getCachedValue(autoBlockStaminaCostIndex);
            if (statValue > 0) {
                staminaCostBps = statValue;
            }
        }
        
        // Calculate and consume stamina
        float staminaCost = DEFAULT_BLOCK_STAMINA_COST * staminaCostBps / CombatMath.BPS_100;
        entityStatMap.subtractStatValue(staminaStatIndex, staminaCost);
        
        // Mark as auto-blocked
        damage.putMetaObject(AUTO_BLOCKED, Boolean.TRUE);
        
        // Also set BLOCKED for Hytale's native systems to recognize
        damage.putMetaObject(Damage.BLOCKED, Boolean.TRUE);
        
        // Set a reduced stamina drain multiplier (auto-block already consumed stamina)
        // This prevents DamageStamina from double-draining
        damage.putMetaObject(Damage.STAMINA_DRAIN_MULTIPLIER, 0.0f);
    }
    
    /**
     * Cache stat indices for performance.
     */
    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }
        
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        blockChanceIndex = registry.getIndex(BLOCK_CHANCE);
        blockMitigationIndex = registry.getIndex(BLOCK_MITIGATION);
        autoBlockStaminaCostIndex = registry.getIndex(AUTO_BLOCK_STAMINA_COST);
        indicesCached = true;
    }
}
