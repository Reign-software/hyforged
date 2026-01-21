package reign.software.hyforged.stats.bridge;

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
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.combat.CombatMeta;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.damage.DamageTypeExtensionRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Replaces Hytale's {@link DamageSystems.ArmorDamageReduction} with Hyforged's resistance-based damage reduction.
 * <p>
 * This system reads resistance stats from {@link HyforgedStatComponent} and reduces incoming damage
 * based on the entity's resistance to the damage type. Resistance values are in basis points (10000 = 100%).
 * <p>
 * Damage reduction formula: {@code finalDamage = damage * (1 - resistance / 10000)}
 * <p>
 * Negative resistance values increase damage taken (vulnerability).
 * <p>
 * The system supports damage type inheritance: if no specific resistance exists for a damage type,
 * it will check the parent damage type (e.g., Bleed inherits from Physical).
 * <p>
 * Caps are data-driven from stat definitions (SoftCapBps/HardCapBps in JSON).
 *
 * @see <a href="../../.memory_bank/ADRs.md#adr-0006">ADR-0006</a>
 */
public class HyforgedDamageReductionSystem extends DamageEventSystem {

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    // Cached stat indices for performance (damage type ID -> resistance stat index)
    private final Map<String, Integer> resistanceStatIndices = new HashMap<>();
    
    // Cached stat indices for performance (damage type ID -> penetration stat index)
    private final Map<String, Integer> penetrationStatIndices = new HashMap<>();

    public HyforgedDamageReductionSystem() {
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        
        // Query for entities with HyforgedStatComponent
        this.query = statComponentType;
        
        // Run before ApplyDamage in the filter damage group
        this.dependencies = Set.of(
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
        // Skip if damage is already zero or cancelled
        if (damage.getAmount() <= 0 || damage.isCancelled()) {
            return;
        }
        
        // Skip if already processed by CombatService (avoids re-applying pipeline)
        Boolean pipelineProcessed = damage.getIfPresentMetaObject(CombatMeta.PIPELINE_PROCESSED);
        if (pipelineProcessed != null && pipelineProcessed) {
            return;
        }

        // Get the damage cause via index (getCause() is deprecated)
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause == null) {
            return;
        }

        // Check if this damage type bypasses resistances
        if (damageCause.doesBypassResistances()) {
            return;
        }

        // Get the defender's stat component
        HyforgedStatComponent defenderStats = archetypeChunk.getComponent(index, statComponentType);
        if (defenderStats == null) {
            return;
        }

        // Get the resistance stat info for this damage type
        ResistanceInfo resistanceInfo = getResistanceInfoForDamageType(damageCause, defenderStats);
        if (resistanceInfo == null) {
            // No resistance defined for this damage type
            return;
        }
        
        int resistanceBps = resistanceInfo.value;

        // Get attacker's penetration if damage has an entity source
        int penetrationBps = 0;
        if (damage.getSource() instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef.isValid()) {
                HyforgedStatComponent attackerStats = store.getComponent(attackerRef, statComponentType);
                if (attackerStats != null) {
                    penetrationBps = getPenetrationForDamageType(damageCause, attackerStats);
                }
            }
        }
        
        // Calculate effective resistance after penetration
        // Penetration only applies to positive resistance (can't make vulnerability worse)
        int effectiveResistanceBps;
        if (resistanceBps >= 0) {
            // Positive resistance: penetration reduces it (but not below 0)
            effectiveResistanceBps = Math.max(0, resistanceBps - penetrationBps);
        } else {
            // Negative resistance (vulnerability): penetration doesn't help
            effectiveResistanceBps = resistanceBps;
        }

        // Clamp to data-driven min/max from stat definition
        effectiveResistanceBps = Math.max(resistanceInfo.minValue, 
                Math.min(resistanceInfo.maxCap, effectiveResistanceBps));
        
        // Record values for combat log
        damage.putMetaObject(CombatMeta.RESISTANCE_BPS, resistanceBps);
        damage.putMetaObject(CombatMeta.PENETRATION_BPS, penetrationBps);
        damage.putMetaObject(CombatMeta.EFFECTIVE_RESISTANCE_BPS, effectiveResistanceBps);

        // Apply damage modification: finalDamage = damage * (1 - resistance / 10000)
        // Positive resistance = damage reduction
        // Negative resistance = damage increase (vulnerability)
        if (effectiveResistanceBps != 0) {
            float multiplier = 1.0f - (effectiveResistanceBps / 10000.0f);
            float modifiedDamage = damage.getAmount() * multiplier;
            damage.setAmount(Math.max(0, modifiedDamage));
        }
    }
    
    /**
     * Container for resistance stat value and its caps.
     */
    private record ResistanceInfo(int value, int minValue, int maxCap) {}

    /**
     * Get the resistance value and caps for a damage type using the data-driven registry.
     * <p>
     * The mapping is defined in Server/Hyforged/Damage/*.json files, where each
     * damage type extension specifies which resistance stat applies via
     * "HyforgedResistanceStat". The registry handles inheritance automatically.
     * <p>
     * Note: Soft cap bonus stats (e.g., max-fire-resistance-bps) are evaluated here
     * to provide the correct cap for damage reduction.
     * <p>
     * Returns null if no resistance stat is defined for this damage type.
     */
    @Nullable
    private ResistanceInfo getResistanceInfoForDamageType(
            @Nonnull DamageCause damageCause,
            @Nonnull HyforgedStatComponent statComponent
    ) {
        // Use the data-driven registry to find the resistance stat
        DamageTypeExtensionRegistry registry = DamageTypeExtensionRegistry.get();
        StatId resistanceStat = registry.getResistanceStatForDamage(damageCause);
        
        if (resistanceStat == null) {
            // No resistance defined for this damage type
            return null;
        }
        
        // Get the cached stat index (or cache it now)
        String damageId = damageCause.getId();
        Integer statIndex = resistanceStatIndices.get(damageId);
        
        if (statIndex == null) {
            // Cache the index for this damage type
            statIndex = StatDefinitionRegistry.get().getIndex(resistanceStat);
            resistanceStatIndices.put(damageId, statIndex);
        }
        
        if (statIndex < 0) {
            // Stat not found in registry
            return null;
        }
        
        // Get the stat definition to read caps
        StatDefinition statDef = StatDefinitionRegistry.get().getStat(statIndex);
        if (statDef == null) {
            return null;
        }
        
        int value = statComponent.getCachedValue(statIndex);
        int minValue = statDef.minValue();
        
        // Calculate effective soft cap including bonus stat
        int maxCap;
        if (statDef.hasSoftCap()) {
            int baseSoftCap = statDef.softCapBps();
            int bonusCapAmount = 0;
            
            // Check for soft cap bonus stat (e.g., max-fire-resistance-bps)
            if (statDef.softCapBonusStat() != null) {
                StatId bonusStatId = statDef.softCapBonusStat();
                int bonusStatIndex = StatDefinitionRegistry.get().getIndex(bonusStatId);
                if (bonusStatIndex >= 0) {
                    bonusCapAmount = statComponent.getCachedValue(bonusStatIndex);
                }
            }
            
            // Effective soft cap = base soft cap + bonus
            int effectiveSoftCap = baseSoftCap + bonusCapAmount;
            
            // Clamp to hard cap if defined
            if (statDef.hasHardCap()) {
                maxCap = Math.min(effectiveSoftCap, statDef.hardCapBps());
            } else {
                maxCap = effectiveSoftCap;
            }
        } else if (statDef.hasHardCap()) {
            maxCap = statDef.hardCapBps();
        } else {
            maxCap = statDef.maxValue();
        }
        
        return new ResistanceInfo(value, minValue, maxCap);
    }

    /**
     * Get the penetration value for a damage type using the data-driven registry.
     * <p>
     * Penetration reduces the defender's effective resistance. The attacker's
     * penetration stat is looked up based on the damage type.
     */
    private int getPenetrationForDamageType(
            @Nonnull DamageCause damageCause,
            @Nonnull HyforgedStatComponent attackerStats
    ) {
        // Use the data-driven registry to find the penetration stat
        DamageTypeExtensionRegistry registry = DamageTypeExtensionRegistry.get();
        StatId penetrationStat = registry.getPenetrationStatForDamage(damageCause);
        
        if (penetrationStat == null) {
            // No penetration defined for this damage type
            return 0;
        }
        
        // Get the cached stat index (or cache it now)
        String damageId = damageCause.getId();
        Integer statIndex = penetrationStatIndices.get(damageId);
        
        if (statIndex == null) {
            // Cache the index for this damage type
            statIndex = StatDefinitionRegistry.get().getIndex(penetrationStat);
            penetrationStatIndices.put(damageId, statIndex);
        }
        
        if (statIndex < 0) {
            // Stat not found in registry
            return 0;
        }
        
        return attackerStats.getCachedValue(statIndex);
    }
}
