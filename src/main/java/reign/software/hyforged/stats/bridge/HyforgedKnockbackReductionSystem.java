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
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Replaces Hytale's {@link DamageSystems.ArmorKnockbackReduction} with Hyforged's knockback resistance.
 * <p>
 * This system reads the knockback resistance stat from {@link HyforgedStatComponent} and reduces
 * incoming knockback based on the entity's resistance. Resistance values are in basis points (10000 = 100%).
 * <p>
 * Knockback reduction formula: {@code knockbackMultiplier = 1 - (resistance / 10000)}
 * <p>
 * A resistance of 10000 bps (100%) means complete immunity to knockback.
 *
 * @see <a href="../../.memory_bank/ADRs.md#adr-0006">ADR-0006</a>
 */
public class HyforgedKnockbackReductionSystem extends DamageEventSystem {

    /** Maximum knockback resistance cap in basis points (100% = 10000 bps) */
    private static final int MAX_RESISTANCE_BPS = 10000;

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;

    @Nonnull
    private final ComponentType<EntityStore, DamageDataComponent> damageDataComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    // Cached stat index for knockback resistance
    private int knockbackResistanceIndex = -1;
    private boolean indexInitialized = false;

    public HyforgedKnockbackReductionSystem() {
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        this.damageDataComponentType = DamageDataComponent.getComponentType();
        
        // Query for entities with both HyforgedStatComponent and DamageDataComponent
        this.query = Query.and(statComponentType, damageDataComponentType);
        
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
        // Skip if damage is cancelled
        if (damage.isCancelled()) {
            return;
        }

        // Get the knockback component from the damage event
        KnockbackComponent knockbackComponent = damage.getIfPresentMetaObject(Damage.KNOCKBACK_COMPONENT);
        if (knockbackComponent == null) {
            return;
        }

        // Get the entity's stat component
        HyforgedStatComponent statComponent = archetypeChunk.getComponent(index, statComponentType);
        if (statComponent == null) {
            return;
        }

        // Initialize stat index on first use
        if (!indexInitialized) {
            initializeStatIndex();
        }

        // Get knockback resistance
        if (knockbackResistanceIndex < 0) {
            return;
        }

        int resistanceBps = statComponent.getCachedValue(knockbackResistanceIndex);

        // Clamp resistance to valid range (0 to 100%)
        resistanceBps = Math.max(0, Math.min(MAX_RESISTANCE_BPS, resistanceBps));

        // Apply knockback reduction: multiplier = 1 - (resistance / 10000)
        if (resistanceBps > 0) {
            float multiplier = 1.0f - (resistanceBps / 10000.0f);
            knockbackComponent.addModifier(Math.max(0, multiplier));
        }
    }

    /**
     * Initialize cached stat index for knockback resistance.
     */
    private void initializeStatIndex() {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        knockbackResistanceIndex = registry.getIndex(StatId.hyforged("knockback-resistance-bps"));
        indexInitialized = true;
    }
}
