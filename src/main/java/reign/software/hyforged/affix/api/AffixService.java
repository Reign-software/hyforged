package reign.software.hyforged.affix.api;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import reign.software.hyforged.affix.model.*;
import reign.software.hyforged.affix.registry.*;
import reign.software.hyforged.affix.service.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Public API facade for the Hyforged Affix System.
 * <p>
 * This is the primary entry point for other plugins to interact with the affix system.
 * It provides methods for:
 * <ul>
 *   <li>Querying affixes on items</li>
 *   <li>Rolling affixes for items</li>
 *   <li>Creating items with specific affixes</li>
 *   <li>Registering custom affixes and pools at runtime</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * // Get instance
 * AffixService service = AffixService.get();
 * 
 * // Query affixes on an item
 * List&lt;RolledAffix&gt; affixes = service.getAffixes(itemStack);
 * 
 * // Roll affixes on an item
 * ItemStack withAffixes = service.rollAffixes(itemStack);
 * 
 * // Create item with specific affixes
 * ItemStack crafted = service.createWithAffixes("Items.Weapons.Sword", List.of(
 *     AffixSpec.of("sturdy", 2, 35),
 *     AffixSpec.of("of-the-bear", 1)
 * ));
 * 
 * // Register custom affix from another plugin
 * service.registerAffix(myCustomAffix);
 * </pre>
 *
 * @see AffixSpec
 * @see RolledAffix
 */
public final class AffixService {
    
    private static final Logger LOGGER = Logger.getLogger(AffixService.class.getName());
    private static AffixService instance;
    
    private final AffixRollerService rollerService;
    private final AffixDefinitionRegistry affixRegistry;
    private final AffixPoolRegistry poolRegistry;
    private final AffixTypeRegistry typeRegistry;
    private final QualityAffixRuleRegistry qualityRegistry;
    
    private AffixService() {
        this.rollerService = new AffixRollerService();
        this.affixRegistry = AffixDefinitionRegistry.get();
        this.poolRegistry = AffixPoolRegistry.get();
        this.typeRegistry = AffixTypeRegistry.get();
        this.qualityRegistry = QualityAffixRuleRegistry.get();
    }
    
    /**
     * Get the singleton AffixService instance.
     *
     * @return The AffixService instance
     */
    @Nonnull
    public static synchronized AffixService get() {
        if (instance == null) {
            instance = new AffixService();
        }
        return instance;
    }
    
    /**
     * Reset the service (for testing).
     */
    public static synchronized void reset() {
        instance = null;
    }
    
    // =========================================================================
    // Query Methods
    // =========================================================================
    
    /**
     * Get all affixes rolled on an item.
     * <p>
     * Returns an empty list if the item has no affixes.
     *
     * @param itemStack The item to query
     * @return List of rolled affixes on the item (never null)
     */
    @Nonnull
    public List<RolledAffix> getAffixes(@Nonnull ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        
        HyforgedItemData data = HyforgedItemDataService.read(itemStack);
        return data.affixes();
    }
    
    /**
     * Check if an item has any affixes.
     *
     * @param itemStack The item to check
     * @return true if the item has one or more affixes
     */
    public boolean hasAffixes(@Nonnull ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        return HyforgedItemDataService.hasData(itemStack) && 
               !HyforgedItemDataService.read(itemStack).affixes().isEmpty();
    }
    
    /**
     * Get the full Hyforged item data for an item.
     *
     * @param itemStack The item to query
     * @return The Hyforged data, or EMPTY if none
     */
    @Nonnull
    public HyforgedItemData getItemData(@Nonnull ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        return HyforgedItemDataService.read(itemStack);
    }
    
    /**
     * Get the affix definition for a given ID.
     *
     * @param affixId The affix ID to look up
     * @return The affix definition, or null if not found
     */
    @Nullable
    public AffixDefinition getAffixDefinition(@Nonnull String affixId) {
        Objects.requireNonNull(affixId, "affixId cannot be null");
        return affixRegistry.get(affixId);
    }
    
    /**
     * Get the affix type definition for a given type ID.
     *
     * @param typeId The type ID to look up (e.g., "prefix", "suffix")
     * @return The affix type, or null if not found
     */
    @Nullable
    public AffixType getAffixType(@Nonnull String typeId) {
        Objects.requireNonNull(typeId, "typeId cannot be null");
        return typeRegistry.get(typeId);
    }
    
    /**
     * Get all registered affix IDs.
     *
     * @return Unmodifiable set of affix IDs
     */
    @Nonnull
    public Set<String> getAllAffixIds() {
        return affixRegistry.getAll().stream()
                .map(AffixDefinition::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    
    /**
     * Get all registered affix type IDs.
     *
     * @return Unmodifiable set of type IDs
     */
    @Nonnull
    public Set<String> getAllTypeIds() {
        return typeRegistry.getAll().stream()
                .map(AffixType::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    
    /**
     * Get all registered affix pool IDs.
     *
     * @return Unmodifiable set of pool IDs
     */
    @Nonnull
    public Set<String> getAllPoolIds() {
        return poolRegistry.getAll().stream()
                .map(AffixPool::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    
    // =========================================================================
    // Rolling Methods
    // =========================================================================
    
    /**
     * Roll affixes for an item using a random seed.
     * <p>
     * The item must be eligible for affixes (equipment type, valid quality).
     * Returns the same item unchanged if not eligible.
     *
     * @param itemStack The item to roll affixes for
     * @return A new ItemStack with affixes rolled (or unchanged if not eligible)
     */
    @Nonnull
    public ItemStack rollAffixes(@Nonnull ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        return rollAffixes(itemStack, new Random());
    }
    
    /**
     * Roll affixes for an item with a specific random instance.
     *
     * @param itemStack The item to roll affixes for
     * @param random    The random instance to use
     * @return A new ItemStack with affixes rolled (or unchanged if not eligible)
     */
    @Nonnull
    public ItemStack rollAffixes(@Nonnull ItemStack itemStack, @Nonnull Random random) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        Objects.requireNonNull(random, "random cannot be null");
        
        // Build context from item
        AffixRollContext context = buildContextFromItem(itemStack);
        if (context == null) {
            LOGGER.log(Level.FINE, "Item not eligible for affixes: {0}", itemStack.getItemId());
            return itemStack;
        }
        
        // Roll affixes
        AffixRollResult result = rollerService.rollAffixes(context, random);
        if (!result.hasAffixes()) {
            return itemStack;
        }
        
        // Apply to item
        HyforgedItemData newData = result.toItemData();
        
        return HyforgedItemDataService.write(itemStack, newData);
    }
    
    /**
     * Roll affixes for an item with a specific seed (deterministic).
     *
     * @param itemStack The item to roll affixes for
     * @param seed      The random seed for deterministic rolling
     * @return A new ItemStack with affixes rolled (or unchanged if not eligible)
     */
    @Nonnull
    public ItemStack rollAffixes(@Nonnull ItemStack itemStack, long seed) {
        return rollAffixes(itemStack, new Random(seed));
    }
    
    // =========================================================================
    // Creation Methods
    // =========================================================================
    
    /**
     * Create an item with specific affixes.
     * <p>
     * This creates a new ItemStack with the specified affixes applied.
     * Affixes are validated and rolled according to their specs.
     *
     * @param itemId      The item ID to create
     * @param affixSpecs  The affixes to apply
     * @return A new ItemStack with the affixes applied
     * @throws IllegalArgumentException if any affix ID is not found
     */
    @Nonnull
    public ItemStack createWithAffixes(@Nonnull String itemId, @Nonnull List<AffixSpec> affixSpecs) {
        return createWithAffixes(itemId, 1, affixSpecs);
    }
    
    /**
     * Create an item with specific affixes.
     *
     * @param itemId      The item ID to create
     * @param quantity    The stack quantity
     * @param affixSpecs  The affixes to apply
     * @return A new ItemStack with the affixes applied
     * @throws IllegalArgumentException if any affix ID is not found
     */
    @Nonnull
    public ItemStack createWithAffixes(
            @Nonnull String itemId, 
            int quantity,
            @Nonnull List<AffixSpec> affixSpecs
    ) {
        return createWithAffixes(itemId, quantity, affixSpecs, new Random());
    }
    
    /**
     * Create an item with specific affixes using a deterministic seed.
     *
     * @param itemId      The item ID to create
     * @param quantity    The stack quantity
     * @param affixSpecs  The affixes to apply
     * @param seed        The random seed for rolling unspecified tiers/values
     * @return A new ItemStack with the affixes applied
     * @throws IllegalArgumentException if any affix ID is not found
     */
    @Nonnull
    public ItemStack createWithAffixes(
            @Nonnull String itemId, 
            int quantity,
            @Nonnull List<AffixSpec> affixSpecs,
            long seed
    ) {
        return createWithAffixes(itemId, quantity, affixSpecs, new Random(seed));
    }
    
    /**
     * Create an item with specific affixes.
     *
     * @param itemId      The item ID to create
     * @param quantity    The stack quantity
     * @param affixSpecs  The affixes to apply
     * @param random      The random instance for rolling
     * @return A new ItemStack with the affixes applied
     * @throws IllegalArgumentException if any affix ID is not found
     */
    @Nonnull
    public ItemStack createWithAffixes(
            @Nonnull String itemId, 
            int quantity,
            @Nonnull List<AffixSpec> affixSpecs,
            @Nonnull Random random
    ) {
        Objects.requireNonNull(itemId, "itemId cannot be null");
        Objects.requireNonNull(affixSpecs, "affixSpecs cannot be null");
        Objects.requireNonNull(random, "random cannot be null");
        
        // Create base item
        ItemStack itemStack = new ItemStack(itemId, quantity);
        
        if (affixSpecs.isEmpty()) {
            return itemStack;
        }
        
        // Convert specs to rolled affixes
        List<RolledAffix> rolledAffixes = new ArrayList<>();
        for (AffixSpec spec : affixSpecs) {
            RolledAffix affix = rollFromSpec(spec, random);
            if (affix != null) {
                rolledAffixes.add(affix);
            }
        }
        
        // Apply affixes to item
        HyforgedItemData data = HyforgedItemData.EMPTY.withAffixes(rolledAffixes);
        return HyforgedItemDataService.write(itemStack, data);
    }
    
    /**
     * Roll a single affix from a spec.
     */
    @Nullable
    private RolledAffix rollFromSpec(@Nonnull AffixSpec spec, @Nonnull Random random) {
        AffixDefinition definition = affixRegistry.get(spec.affixId());
        if (definition == null) {
            LOGGER.log(Level.WARNING, "Unknown affix ID in spec: {0}", spec.affixId());
            throw new IllegalArgumentException("Unknown affix ID: " + spec.affixId());
        }
        
        List<AffixTierDefinition> tiers = definition.tiers();
        if (tiers.isEmpty()) {
            LOGGER.log(Level.WARNING, "Affix has no tiers: {0}", spec.affixId());
            return null;
        }
        
        // Determine tier
        int tier;
        if (spec.hasTier()) {
            tier = spec.requireTier();
        } else {
            // Roll random tier (weighted toward higher tier numbers = more common)
            tier = rollRandomTier(tiers, random);
        }
        
        // Find tier definition
        AffixTierDefinition tierDef = findTier(tiers, tier);
        if (tierDef == null) {
            LOGGER.log(Level.WARNING, "Tier {0} not found for affix: {1}", new Object[]{tier, spec.affixId()});
            // Fall back to first tier
            tierDef = tiers.get(0);
            tier = tierDef.tier();
        }
        
        // Roll values for all stats in this tier
        Map<String, RolledAffix.RolledStat> rolledStats = rollAllStats(tierDef, random);
        
        return RolledAffix.from(definition, tier, rolledStats);
    }
    
    /**
     * Roll values for all stats in a tier.
     *
     * @param tier   The tier definition containing stats
     * @param random Random source for rolling values
     * @return Map of stat ID to rolled stat with value and stack type
     */
    private Map<String, RolledAffix.RolledStat> rollAllStats(
            @Nonnull AffixTierDefinition tier,
            @Nonnull Random random
    ) {
        Map<String, RolledAffix.RolledStat> result = new HashMap<>();
        for (Map.Entry<String, AffixTierStat> entry : tier.stats().entrySet()) {
            String statId = entry.getKey();
            AffixTierStat tierStat = entry.getValue();
            int rolledValue = tierStat.rollValue(random.nextDouble());
            result.put(statId, new RolledAffix.RolledStat(rolledValue, tierStat.stackType()));
        }
        return result;
    }
    
    /**
     * Roll a random tier from available tiers.
     */
    private int rollRandomTier(@Nonnull List<AffixTierDefinition> tiers, @Nonnull Random random) {
        // Weight higher tier numbers more (linear weighting)
        int totalWeight = 0;
        for (int i = 0; i < tiers.size(); i++) {
            totalWeight += (i + 1); // Tier 1 = weight 1, Tier 5 = weight 5
        }
        
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < tiers.size(); i++) {
            cumulative += (i + 1);
            if (roll < cumulative) {
                return tiers.get(i).tier();
            }
        }
        
        return tiers.get(tiers.size() - 1).tier();
    }
    
    /**
     * Find a tier definition by tier number.
     */
    @Nullable
    private AffixTierDefinition findTier(@Nonnull List<AffixTierDefinition> tiers, int tier) {
        for (AffixTierDefinition tierDef : tiers) {
            if (tierDef.tier() == tier) {
                return tierDef;
            }
        }
        return null;
    }
    
    // =========================================================================
    // Registration Methods (for plugins)
    // =========================================================================
    
    /**
     * Register a custom affix definition.
     * <p>
     * Allows other plugins to add new affixes at runtime.
     * The affix will be available for rolling on eligible items.
     *
     * @param affix The affix definition to register
     * @throws IllegalStateException if the registry is frozen
     */
    public void registerAffix(@Nonnull AffixDefinition affix) {
        Objects.requireNonNull(affix, "affix cannot be null");
        affixRegistry.register(affix);
        LOGGER.log(Level.INFO, "Registered affix via API: {0}", affix.id());
    }
    
    /**
     * Register a custom affix pool.
     * <p>
     * Allows other plugins to add new affix pools at runtime.
     *
     * @param pool The affix pool to register
     * @throws IllegalStateException if the registry is frozen
     */
    public void registerPool(@Nonnull AffixPool pool) {
        Objects.requireNonNull(pool, "pool cannot be null");
        poolRegistry.register(pool);
        LOGGER.log(Level.INFO, "Registered affix pool via API: {0}", pool.id());
    }
    
    /**
     * Register a custom affix type.
     * <p>
     * Allows other plugins to add new affix types at runtime.
     *
     * @param type The affix type to register
     * @throws IllegalStateException if the registry is frozen
     */
    public void registerType(@Nonnull AffixType type) {
        Objects.requireNonNull(type, "type cannot be null");
        typeRegistry.register(type);
        LOGGER.log(Level.INFO, "Registered affix type via API: {0}", type.id());
    }
    
    /**
     * Register a custom quality affix rule.
     * <p>
     * Allows other plugins to add or override quality capacity rules.
     *
     * @param rule The quality rule to register
     * @throws IllegalStateException if the registry is frozen
     */
    public void registerQualityRule(@Nonnull QualityAffixRule rule) {
        Objects.requireNonNull(rule, "rule cannot be null");
        qualityRegistry.register(rule);
        LOGGER.log(Level.INFO, "Registered quality affix rule via API: {0}", rule.quality());
    }
    
    // =========================================================================
    // Item Modification Methods
    // =========================================================================
    
    /**
     * Add an affix to an existing item.
     *
     * @param itemStack The item to modify
     * @param spec      The affix to add
     * @return A new ItemStack with the affix added
     */
    @Nonnull
    public ItemStack addAffix(@Nonnull ItemStack itemStack, @Nonnull AffixSpec spec) {
        return addAffix(itemStack, spec, new Random());
    }
    
    /**
     * Add an affix to an existing item.
     *
     * @param itemStack The item to modify
     * @param spec      The affix to add
     * @param random    Random instance for rolling
     * @return A new ItemStack with the affix added
     */
    @Nonnull
    public ItemStack addAffix(@Nonnull ItemStack itemStack, @Nonnull AffixSpec spec, @Nonnull Random random) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        Objects.requireNonNull(spec, "spec cannot be null");
        Objects.requireNonNull(random, "random cannot be null");
        
        RolledAffix affix = rollFromSpec(spec, random);
        if (affix == null) {
            return itemStack;
        }
        
        HyforgedItemData data = HyforgedItemDataService.read(itemStack);
        List<RolledAffix> affixes = new ArrayList<>(data.affixes());
        affixes.add(affix);
        
        return HyforgedItemDataService.write(itemStack, data.withAffixes(affixes));
    }
    
    /**
     * Remove an affix from an item by affix ID.
     *
     * @param itemStack The item to modify
     * @param affixId   The affix ID to remove
     * @return A new ItemStack with the affix removed
     */
    @Nonnull
    public ItemStack removeAffix(@Nonnull ItemStack itemStack, @Nonnull String affixId) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        Objects.requireNonNull(affixId, "affixId cannot be null");
        
        HyforgedItemData data = HyforgedItemDataService.read(itemStack);
        List<RolledAffix> remaining = data.affixes().stream()
                .filter(a -> !a.affixId().equals(affixId))
                .toList();
        
        return HyforgedItemDataService.write(itemStack, data.withAffixes(remaining));
    }
    
    /**
     * Clear all affixes from an item.
     *
     * @param itemStack The item to clear
     * @return A new ItemStack with no affixes
     */
    @Nonnull
    public ItemStack clearAffixes(@Nonnull ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        return HyforgedItemDataService.clear(itemStack);
    }
    
    // =========================================================================
    // Context Building
    // =========================================================================
    
    /**
     * Build an AffixRollContext from an ItemStack.
     * <p>
     * Returns null if the item is not eligible for affixes.
     */
    @Nullable
    private AffixRollContext buildContextFromItem(@Nonnull ItemStack itemStack) {
        return ItemContextExtractor.buildContext(itemStack);
    }
}
