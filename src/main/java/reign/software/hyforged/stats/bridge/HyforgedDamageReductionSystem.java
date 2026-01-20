package reign.software.hyforged.stats.bridge;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
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
 * The system supports damage type inheritance: if no specific resistance exists for a damage type,
 * it will check the parent damage type (e.g., Bleed inherits from Physical).
 *
 * @see <a href="../../.memory_bank/ADRs.md#adr-0006">ADR-0006</a>
 */
public class HyforgedDamageReductionSystem extends DamageEventSystem {

    /** Maximum resistance cap in basis points (75% = 7500 bps) */
    private static final int MAX_RESISTANCE_BPS = 7500;
    
    /** Minimum resistance (can be negative for increased damage taken) */
    private static final int MIN_RESISTANCE_BPS = -5000;

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    // Cached stat indices for performance (damage type ID -> resistance stat index)
    private final Map<String, Integer> resistanceStatIndices = new HashMap<>();

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

        // Get the damage cause via index (getCause() is deprecated)
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause == null) {
            return;
        }

        // Check if this damage type bypasses resistances
        if (damageCause.doesBypassResistances()) {
            return;
        }

        // Get the entity's stat component
        HyforgedStatComponent statComponent = archetypeChunk.getComponent(index, statComponentType);
        if (statComponent == null) {
            return;
        }

        // Get the resistance for this damage type from the data-driven registry
        int resistanceBps = getResistanceForDamageType(damageCause, statComponent);

        // Clamp resistance to valid range
        resistanceBps = Math.max(MIN_RESISTANCE_BPS, Math.min(MAX_RESISTANCE_BPS, resistanceBps));

        // Apply damage reduction: finalDamage = damage * (1 - resistance / 10000)
        if (resistanceBps != 0) {
            float multiplier = 1.0f - (resistanceBps / 10000.0f);
            float reducedDamage = damage.getAmount() * multiplier;
            damage.setAmount(Math.max(0, reducedDamage));
        }
    }

    /**
     * Get the resistance value for a damage type using the data-driven registry.
     * <p>
     * The mapping is defined in Server/Hyforged/Damage/*.json files, where each
     * damage type extension specifies which resistance stat applies via
     * "HyforgedResistanceStat". The registry handles inheritance automatically.
     */
    private int getResistanceForDamageType(
            @Nonnull DamageCause damageCause,
            @Nonnull HyforgedStatComponent statComponent
    ) {
        // Use the data-driven registry to find the resistance stat
        DamageTypeExtensionRegistry registry = DamageTypeExtensionRegistry.get();
        StatId resistanceStat = registry.getResistanceStatForDamage(damageCause);
        
        if (resistanceStat == null) {
            // No resistance defined for this damage type
            return 0;
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
            return 0;
        }
        
        return statComponent.getCachedValue(statIndex);
    }
}
