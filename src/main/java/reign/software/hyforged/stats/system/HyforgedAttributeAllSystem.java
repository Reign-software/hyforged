package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.logger.HytaleLogger;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * ECS System that fans out {@code hyforged:attribute-all} to all individual attribute stats.
 * <p>
 * Attribute stats are discovered at init time via the {@code Domain=attributes} tag — no
 * hardcoded stat IDs in logic. The source stat ({@code attribute-all}) is excluded from the
 * target list to prevent self-referential modifier loops.
 * <p>
 * <b>Anti-loop guarantee:</b> Modifiers are injected under the key {@value #MODIFIER_KEY}
 * with {@code SourceType.SYSTEM}. Individual attribute stats (Strength, Dexterity, etc.)
 * do not have any scaling source pointing back to {@code attribute-all}, so recomputation
 * terminates.
 * <p>
 * <b>Perpetual-dirty guard:</b> Before calling {@link HyforgedStatComponent#upsertModifier},
 * the system checks whether the existing modifier already has the same amount. This prevents
 * marking stats dirty every tick when the {@code attribute-all} value has not changed.
 * <p>
 * Pattern follows {@link ClassLevelModifierSystem}: use
 * {@link HyforgedStatComponent#upsertModifier} so the dirty-flag subsystem propagates
 * the change correctly.
 */
public class HyforgedAttributeAllSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Modifier source ID for all attribute-all injected modifiers.
     * This distinguished key guards against conflicts with other sources and anti-loops.
     */
    public static final String MODIFIER_KEY = "hyforged:attribute-all";

    private static final StatId ATTRIBUTE_ALL_STAT = StatId.hyforged("attribute-all");

    /**
     * Tag shared by all individual attribute stats (Strength, Dexterity, Intelligence, etc.).
     * Derived from {@code "Domain": ["attributes"]} in each stat's JSON definition, which
     * expands to the composite tag {@code "Domain=attributes"} via StatDefinitionAsset.
     */
    private static final String DOMAIN_ATTRIBUTES_TAG = "Domain=attributes";

    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapType;
    private final Query<EntityStore> query;
    private final Set<Dependency<EntityStore>> dependencies;

    // Lazily cached after registry is frozen on first tick
    private int attributeAllIndex = -1;
    private int[] attributeStatIndices = null;
    private boolean indicesCached = false;

    public HyforgedAttributeAllSystem() {
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        this.entityStatMapType = EntityStatMap.getComponentType();
        this.query = Query.and(statComponentType, entityStatMapType);
        this.dependencies = Set.of(
                new SystemDependency<>(Order.AFTER, HyforgedStatComputeSystem.class),
                new SystemDependency<>(Order.BEFORE, HyforgedBridgeSystem.class)
        );
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
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        HyforgedStatComponent statComponent = chunk.getComponent(index, statComponentType);
        if (statComponent == null) {
            return;
        }

        ensureIndicesCached();

        if (attributeAllIndex < 0 || attributeStatIndices == null || attributeStatIndices.length == 0) {
            return;
        }

        int attributeAllValue = StatAccessor.getStatValueInt(chunk, index, attributeAllIndex);

        if (attributeAllValue == 0) {
            // Remove any previously-injected attribute-all modifiers
            int removed = statComponent.removeModifiersIf(
                    m -> MODIFIER_KEY.equals(m.getSourceId())
                            && m.getSourceType() == HyforgedModifier.SourceType.BASE,
                    m -> {
                    }
            );
            if (removed > 0) {
                statComponent.markAllDirty();
            }
        } else {
            // Get the modifier list once to check existing values without repeated allocations
            List<HyforgedModifier> existingModifiers = statComponent.getModifiers();

            boolean changed = false;
            for (int attrIndex : attributeStatIndices) {
                // Perpetual-dirty guard: skip upsert if modifier already has the same amount
                if (existingModifierMatchesValue(existingModifiers, attrIndex, attributeAllValue)) {
                    continue;
                }

                HyforgedModifier modifier = HyforgedModifier.builder()
                        .sourceId(MODIFIER_KEY)
                        .sourceType(HyforgedModifier.SourceType.BASE)
                        .flat(attributeAllValue)
                        .targetStat(attrIndex)
                        .permanent()
                        .build();

                statComponent.upsertModifier(modifier);
                changed = true;
            }

            if (changed) {
                statComponent.markAllDirty();
            }
        }
    }

    /**
     * Return {@code true} if the modifier list already contains a modifier for the given
     * target stat with the expected amount. Avoids perpetual dirty-marking when the
     * {@code attribute-all} value has not changed since the last tick.
     */
    private static boolean existingModifierMatchesValue(
            @Nonnull List<HyforgedModifier> modifiers,
            int targetStatIndex,
            int expectedAmount
    ) {
        for (HyforgedModifier m : modifiers) {
            if (MODIFIER_KEY.equals(m.getSourceId())
                    && m.getSourceType() == HyforgedModifier.SourceType.BASE
                    && m.getTargetStatIndex() == targetStatIndex) {
                return m.getAmount() == expectedAmount;
            }
        }
        return false; // No existing modifier — needs to be added
    }

    /**
     * Lazily discover attribute stats via tag query once the registry is frozen.
     * <p>
     * Attribute stats are all stats with the {@code Domain=attributes} tag,
     * excluding {@code attribute-all} itself (which shares that tag in its
     * {@code AppliesTo} group).
     */
    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();

        attributeAllIndex = registry.getIndex(ATTRIBUTE_ALL_STAT);
        if (attributeAllIndex < 0) {
            LOGGER.atWarning().log(
                    "HyforgedAttributeAllSystem: stat '%s' not found in registry; system disabled",
                    ATTRIBUTE_ALL_STAT.fullId());
            indicesCached = true;
            return;
        }

        // Discover individual attribute stats via tag query — no hardcoded ID list
        Set<Integer> taggedIndices = registry.getStatIndicesForTag(DOMAIN_ATTRIBUTES_TAG);
        if (taggedIndices.isEmpty()) {
            LOGGER.atWarning().log(
                    "HyforgedAttributeAllSystem: no stats found for tag '%s'; fan-out disabled",
                    DOMAIN_ATTRIBUTES_TAG);
            attributeStatIndices = new int[0];
            indicesCached = true;
            return;
        }

        // Exclude attribute-all itself from the target list (self-referential modifier guard)
        List<Integer> attrList = new ArrayList<>(taggedIndices.size());
        for (int idx : taggedIndices) {
            if (idx != attributeAllIndex) {
                attrList.add(idx);
            }
        }

        attributeStatIndices = new int[attrList.size()];
        for (int i = 0; i < attrList.size(); i++) {
            attributeStatIndices[i] = attrList.get(i);
        }

        LOGGER.at(Level.FINE).log(
                "HyforgedAttributeAllSystem: discovered %d attribute stats via tag '%s'",
                attributeStatIndices.length, DOMAIN_ATTRIBUTES_TAG);
        indicesCached = true;
    }
}
