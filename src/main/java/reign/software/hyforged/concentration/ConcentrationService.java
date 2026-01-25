package reign.software.hyforged.concentration;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for registering and managing concentrated abilities per entity.
 */
public final class ConcentrationService {

    private static final Logger LOGGER = Logger.getLogger(ConcentrationService.class.getName());
    private static final StatId MAX_CONCENTRATION_STAT = StatId.hyforged("concentration");

    private static ConcentrationService instance;

    private final ComponentType<EntityStore, ConcentrationPriorityComponent> concentrationPriorityComponentType;
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapType;

    private int maxConcentrationStatIndex = -1;
    private int concentrationEntityStatIndex = -1;
    private boolean indicesInitialized = false;

    private ConcentrationService() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.concentrationPriorityComponentType = plugin.getConcentrationPriorityComponentType();
        this.entityStatMapType = EntityStatMap.getComponentType();
    }

    @Nonnull
    public static synchronized ConcentrationService get() {
        if (instance == null) {
            instance = new ConcentrationService();
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = null;
    }

    /**
     * Register a concentrated ability for an entity.
     */
    public void reserveConcentration(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull String abilityId,
            int cost,
            @Nullable Runnable onDisable,
            @Nullable Runnable onEnable
    ) {
        Objects.requireNonNull(entityRef, "entityRef cannot be null");
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        if (!entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        ConcentrationPriorityComponent component = getOrCreateComponent(store, entityRef);
        int priority = resolveDefaultPriority(component);
        registerAbility(component, abilityId, cost, priority, onDisable, onEnable);

        int maxConcentration = getMaxConcentration(store, entityRef);
        int current = resolveCurrentConcentration(component, store, entityRef, maxConcentration);
        disableUntilSufficient(component, current);
    }

    /**
     * Release a concentrated ability for an entity.
     */
    public void releaseConcentration(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull String abilityId
    ) {
        Objects.requireNonNull(entityRef, "entityRef cannot be null");
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        if (!entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        ConcentrationPriorityComponent component = store.getComponent(entityRef, concentrationPriorityComponentType);
        if (component == null) {
            return;
        }

        component.removeAbility(abilityId);

        int maxConcentration = getMaxConcentration(store, entityRef);
        int current = resolveCurrentConcentration(component, store, entityRef, maxConcentration);
        enableWhilePossible(component, current);
    }

    /**
     * Set the priority for a concentrated ability.
     */
    public void setPriority(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull String abilityId,
            int priority
    ) {
        Objects.requireNonNull(entityRef, "entityRef cannot be null");
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        if (!entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        ConcentrationPriorityComponent component = store.getComponent(entityRef, concentrationPriorityComponentType);
        if (component == null) {
            return;
        }

        component.reorderAbility(abilityId, priority);

        int maxConcentration = getMaxConcentration(store, entityRef);
        int current = resolveCurrentConcentration(component, store, entityRef, maxConcentration);
        reconcileEnabledStates(component, current);
    }

    /**
     * Apply a new priority ordering for the ability list.
     */
    public void setPriorityOrder(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull List<String> orderedAbilityIds
    ) {
        Objects.requireNonNull(entityRef, "entityRef cannot be null");
        Objects.requireNonNull(orderedAbilityIds, "orderedAbilityIds cannot be null");
        if (!entityRef.isValid() || orderedAbilityIds.isEmpty()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        ConcentrationPriorityComponent component = store.getComponent(entityRef, concentrationPriorityComponentType);
        if (component == null) {
            return;
        }

        int maxConcentration = getMaxConcentration(store, entityRef);
        int current = resolveCurrentConcentration(component, store, entityRef, maxConcentration);
        applyPriorityOrder(component, orderedAbilityIds, current);
    }

    /**
     * Get the ordered list of concentrated abilities.
     */
    @Nonnull
    public List<ConcentratedAbility> getPriorityQueue(@Nonnull Ref<EntityStore> entityRef) {
        Objects.requireNonNull(entityRef, "entityRef cannot be null");
        if (!entityRef.isValid()) {
            return List.of();
        }

        ConcentrationPriorityComponent component = entityRef.getStore().getComponent(entityRef, concentrationPriorityComponentType);
        if (component == null) {
            return List.of();
        }
        return component.getAbilities();
    }

    /**
     * Get the entity's current concentration.
     */
    public int getCurrentConcentration(@Nonnull Ref<EntityStore> entityRef) {
        Objects.requireNonNull(entityRef, "entityRef cannot be null");
        if (!entityRef.isValid()) {
            return 0;
        }

        Store<EntityStore> store = entityRef.getStore();
        ConcentrationPriorityComponent component = store.getComponent(entityRef, concentrationPriorityComponentType);
        int maxConcentration = getMaxConcentration(store, entityRef);
        if (component == null) {
            return readCurrentFromStatMap(store, entityRef, maxConcentration);
        }
        return resolveCurrentConcentration(component, store, entityRef, maxConcentration);
    }

    /**
     * Apply concentration loss due to damage.
     */
    public void applyConcentrationLoss(
            @Nonnull Ref<EntityStore> entityRef,
            int lossAmount
    ) {
        Objects.requireNonNull(entityRef, "entityRef cannot be null");
        if (lossAmount <= 0 || !entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        ConcentrationPriorityComponent component = store.getComponent(entityRef, concentrationPriorityComponentType);
        if (component == null) {
            return;
        }

        int maxConcentration = getMaxConcentration(store, entityRef);
        if (maxConcentration <= 0) {
            return;
        }

        int current = resolveCurrentConcentration(component, store, entityRef, maxConcentration);
        int newCurrent = Math.max(0, Math.min(maxConcentration, current - lossAmount));
        if (newCurrent != current) {
            component.setCurrentConcentration(newCurrent);
            syncCurrentToStatMap(store, entityRef, newCurrent);

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format(
                        "Concentration loss applied: entity=%s damageLoss=%d current=%d max=%d",
                        entityRef, lossAmount, newCurrent, maxConcentration));
            }
        }

        disableUntilSufficient(component, newCurrent);
    }

    /**
     * Apply regeneration for an entity.
     */
    public void tickRegeneration(
            @Nonnull Ref<EntityStore> entityRef,
            float regenAmount
    ) {
        Objects.requireNonNull(entityRef, "entityRef cannot be null");
        if (!entityRef.isValid() || regenAmount <= 0f) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        ConcentrationPriorityComponent component = store.getComponent(entityRef, concentrationPriorityComponentType);
        if (component == null) {
            return;
        }

        int maxConcentration = getMaxConcentration(store, entityRef);
        if (maxConcentration <= 0) {
            return;
        }

        int current = resolveCurrentConcentration(component, store, entityRef, maxConcentration);
        float total = regenAmount + component.getRegenRemainder();
        int whole = (int) Math.floor(total);
        float remainder = total - whole;
        component.setRegenRemainder(remainder);

        if (whole <= 0) {
            enableWhilePossible(component, current);
            return;
        }

        int newCurrent = Math.max(0, Math.min(maxConcentration, current + whole));
        if (newCurrent != current) {
            component.setCurrentConcentration(newCurrent);
            syncCurrentToStatMap(store, entityRef, newCurrent);
        }

        enableWhilePossible(component, newCurrent);
    }

    /**
     * Get the entity's max concentration from Hyforged stats.
     */
    public int getMaxConcentration(@Nonnull Ref<EntityStore> entityRef) {
        Objects.requireNonNull(entityRef, "entityRef cannot be null");
        if (!entityRef.isValid()) {
            return 0;
        }
        return getMaxConcentration(entityRef.getStore(), entityRef);
    }

    int resolveCurrentConcentration(
            @Nonnull ConcentrationPriorityComponent component,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            int maxConcentration
    ) {
        int current = component.getCurrentConcentration();
        if (current == 0 && component.getAbilitiesInternal().isEmpty()) {
            current = readCurrentFromStatMap(store, entityRef, maxConcentration);
            component.setCurrentConcentration(current);
            syncCurrentToStatMap(store, entityRef, current);
        }
        return Math.max(0, Math.min(maxConcentration, current));
    }

    static void registerAbility(
            @Nonnull ConcentrationPriorityComponent component,
            @Nonnull String abilityId,
            int cost,
            int priority,
            @Nullable Runnable onDisable,
            @Nullable Runnable onEnable
    ) {
        component.setAbility(abilityId, cost, priority, onDisable, onEnable);
    }

    static void applyLossToComponent(
            @Nonnull ConcentrationPriorityComponent component,
            int maxConcentration,
            int lossAmount
    ) {
        int current = component.getCurrentConcentration();
        int newCurrent = Math.max(0, Math.min(maxConcentration, current - lossAmount));
        component.setCurrentConcentration(newCurrent);
        disableUntilSufficient(component, newCurrent);
    }

    static void applyRegenToComponent(
            @Nonnull ConcentrationPriorityComponent component,
            int maxConcentration,
            float regenAmount
    ) {
        int current = component.getCurrentConcentration();
        float total = regenAmount + component.getRegenRemainder();
        int whole = (int) Math.floor(total);
        float remainder = total - whole;
        component.setRegenRemainder(remainder);
        int newCurrent = Math.max(0, Math.min(maxConcentration, current + whole));
        component.setCurrentConcentration(newCurrent);
        enableWhilePossible(component, newCurrent);
    }

    private ConcentrationPriorityComponent getOrCreateComponent(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef
    ) {
        ConcentrationPriorityComponent component = store.getComponent(entityRef, concentrationPriorityComponentType);
        if (component != null) {
            return component;
        }

        ConcentrationPriorityComponent created = new ConcentrationPriorityComponent();
        int maxConcentration = getMaxConcentration(store, entityRef);
        int current = readCurrentFromStatMap(store, entityRef, maxConcentration);
        if (current <= 0 && maxConcentration > 0) {
            current = maxConcentration;
        }
        created.setCurrentConcentration(current);
        store.addComponent(entityRef, concentrationPriorityComponentType, created);
        syncCurrentToStatMap(store, entityRef, current);
        return created;
    }

    static int resolveDefaultPriority(@Nonnull ConcentrationPriorityComponent component) {
        int max = 0;
        for (ConcentratedAbility ability : component.getAbilitiesInternal()) {
            max = Math.max(max, ability.priority());
        }
        return max + 1;
    }

    private int readCurrentFromStatMap(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            int maxConcentration
    ) {
        ensureIndicesInitialized();
        if (concentrationEntityStatIndex < 0) {
            return Math.max(0, maxConcentration);
        }

        EntityStatMap statMap = store.getComponent(entityRef, entityStatMapType);
        if (statMap == null) {
            return Math.max(0, maxConcentration);
        }

        EntityStatValue value = statMap.get(concentrationEntityStatIndex);
        if (value == null) {
            return Math.max(0, maxConcentration);
        }

        return Math.max(0, Math.round(value.get()));
    }

    private void syncCurrentToStatMap(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            int current
    ) {
        ensureIndicesInitialized();
        if (concentrationEntityStatIndex < 0) {
            return;
        }

        EntityStatMap statMap = store.getComponent(entityRef, entityStatMapType);
        if (statMap == null) {
            return;
        }

        statMap.setStatValue(concentrationEntityStatIndex, current);
    }

    private int getMaxConcentration(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef
    ) {
        ensureIndicesInitialized();
        if (maxConcentrationStatIndex < 0) {
            return 0;
        }
        return Math.max(0, StatAccessor.getStatValueInt(store, entityRef, maxConcentrationStatIndex));
    }

    private void ensureIndicesInitialized() {
        if (indicesInitialized) {
            return;
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        maxConcentrationStatIndex = registry.getIndex(MAX_CONCENTRATION_STAT);
        concentrationEntityStatIndex = EntityStatType.getAssetMap().getIndex("Concentration");
        indicesInitialized = true;
    }

    static void disableUntilSufficient(
            @Nonnull ConcentrationPriorityComponent component,
            int currentConcentration
    ) {
        List<ConcentratedAbility> abilities = component.getAbilitiesInternal();
        if (abilities.isEmpty()) {
            return;
        }

        int totalCost = getTotalEnabledCost(abilities);
        if (totalCost <= currentConcentration) {
            return;
        }

        for (int i = abilities.size() - 1; i >= 0 && totalCost > currentConcentration; i--) {
            ConcentratedAbility ability = abilities.get(i);
            if (!ability.enabled()) {
                continue;
            }
            abilities.set(i, ability.withEnabled(false));
            totalCost -= Math.max(0, ability.cost());
            if (ability.onDisable() != null) {
                ability.onDisable().run();
            }
            LOGGER.log(Level.INFO, "Disabled concentration ability {0}", ability.abilityId());
        }
    }

    static void enableWhilePossible(
            @Nonnull ConcentrationPriorityComponent component,
            int currentConcentration
    ) {
        List<ConcentratedAbility> abilities = component.getAbilitiesInternal();
        if (abilities.isEmpty()) {
            return;
        }

        int totalCost = getTotalEnabledCost(abilities);
        for (int i = 0; i < abilities.size(); i++) {
            ConcentratedAbility ability = abilities.get(i);
            if (ability.enabled()) {
                continue;
            }
            int cost = Math.max(0, ability.cost());
            if (totalCost + cost > currentConcentration) {
                break;
            }
            abilities.set(i, ability.withEnabled(true));
            totalCost += cost;
            if (ability.onEnable() != null) {
                ability.onEnable().run();
            }
            LOGGER.log(Level.INFO, "Re-enabled concentration ability {0}", ability.abilityId());
        }
    }

    static int getTotalEnabledCost(@Nonnull List<ConcentratedAbility> abilities) {
        int total = 0;
        for (ConcentratedAbility ability : abilities) {
            if (ability.enabled()) {
                total += Math.max(0, ability.cost());
            }
        }
        return total;
    }

    static void applyPriorityOrder(
            @Nonnull ConcentrationPriorityComponent component,
            @Nonnull List<String> orderedAbilityIds,
            int currentConcentration
    ) {
        if (orderedAbilityIds.isEmpty()) {
            return;
        }

        List<ConcentratedAbility> existing = component.getAbilitiesInternal();
        if (existing.isEmpty()) {
            return;
        }

        Set<String> uniqueOrder = new LinkedHashSet<>(orderedAbilityIds);
        List<ConcentratedAbility> reordered = new ArrayList<>(existing.size());
        Set<String> remaining = new LinkedHashSet<>();
        for (ConcentratedAbility ability : existing) {
            remaining.add(ability.abilityId());
        }

        for (String abilityId : uniqueOrder) {
            if (!remaining.remove(abilityId)) {
                continue;
            }
            for (ConcentratedAbility ability : existing) {
                if (ability.abilityId().equals(abilityId)) {
                    reordered.add(ability);
                    break;
                }
            }
        }

        if (!remaining.isEmpty()) {
            for (ConcentratedAbility ability : existing) {
                if (remaining.contains(ability.abilityId())) {
                    reordered.add(ability);
                }
            }
        }

        int priority = reordered.size();
        for (int i = 0; i < reordered.size(); i++) {
            reordered.set(i, reordered.get(i).withPriority(priority - i));
        }

        existing.clear();
        existing.addAll(reordered);
        reconcileEnabledStates(component, currentConcentration);
    }

    static void reconcileEnabledStates(
            @Nonnull ConcentrationPriorityComponent component,
            int currentConcentration
    ) {
        List<ConcentratedAbility> abilities = component.getAbilitiesInternal();
        if (abilities.isEmpty()) {
            return;
        }

        int totalCost = 0;
        for (int i = 0; i < abilities.size(); i++) {
            ConcentratedAbility ability = abilities.get(i);
            int cost = Math.max(0, ability.cost());
            boolean shouldEnable = totalCost + cost <= currentConcentration;
            if (shouldEnable) {
                if (!ability.enabled() && ability.onEnable() != null) {
                    ability.onEnable().run();
                }
                totalCost += cost;
            } else if (ability.enabled() && ability.onDisable() != null) {
                ability.onDisable().run();
            }
            abilities.set(i, ability.withEnabled(shouldEnable));
        }
    }
}
