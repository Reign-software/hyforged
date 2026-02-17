package reign.software.hyforged.affix.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.Message;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.CategoryDefinition;
import reign.software.hyforged.stats.DisplayFormat;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.engine.ScalingEngine;
import reign.software.hyforged.stats.modifier.HyforgedModifier;
import reign.software.hyforged.passive.ui.PassiveTreePage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Character Stats Screen UI page.
 * <p>
 * Displays:
 * <ul>
 *   <li>All character stats organized by data-driven categories</li>
 *   <li>Base value, modifiers breakdown, and effective value per stat</li>
 *   <li>Equipment slot display with affix contribution summaries</li>
 * </ul>
 * <p>
 * Categories are fully data-driven from JSON definitions in Server/Hyforged/Categories/.
 * Stats with the "ability-score" tag type are grouped as "Primary Attributes" and displayed
 * first. All other categories are sorted by their {@link CategoryDefinition#sortOrder()},
 * then alphabetically by display name. Category names are localized via translation keys.
 * <p>
 * Access methods:
 * <ul>
 *   <li>Command: {@code /hyforged character} or {@code /hyforged char}</li>
 *   <li>Interaction: Ability1 key when unarmed (configured via {@code Hyforged:CharacterStats} RootInteraction)</li>
 *   <li>Programmatic: {@code OpenCustomUIInteraction} with Page Id "CharacterStatsPage"</li>
 * </ul>
 *
 * @see InteractiveCustomUIPage
 */
public class CharacterStatsPage extends InteractiveCustomUIPage<CharacterStatsPage.PageEventData> {
    
    /** UI file path for the character stats page layout */
    private static final String PAGE_UI_FILE = "Hyforged/CharacterStatsPage.ui";
    
    /** UI template file for category title labels */
    private static final String TITLE_TEMPLATE_UI = "Hyforged/CharacterStatTitle.ui";
    
    /** UI template file for stat row labels */
    private static final String ROW_TEMPLATE_UI = "Hyforged/CharacterStatRow.ui";
    
    /** Tag type value that identifies primary attributes */
    private static final String ABILITY_SCORE_TAG = "ability-score";
    
    /** Translation key prefix for category display names */
    private static final String CATEGORY_TRANSLATION_PREFIX = "hyforged.characterStats.category.";
    
    /** Translation key prefix for column headers */
    private static final String HEADER_TRANSLATION_PREFIX = "hyforged.characterStats.header.";
    
    /** Translation key for the page title */
    private static final String TITLE_TRANSLATION_KEY = "hyforged.characterStats.title";
    
    /** Translation key for equipment section title */
    private static final String EQUIPMENT_TRANSLATION_KEY = "hyforged.characterStats.equipment";
    
    /** Translation key for empty equipment slot */
    private static final String EMPTY_SLOT_TRANSLATION_KEY = "hyforged.characterStats.slot.empty";
    
    /**
     * Create a new CharacterStatsPage for a player.
     *
     * @param playerRef The player to display stats for
     */
    public CharacterStatsPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
    }
    
    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append(PAGE_UI_FILE);
        
        // Set localized title
        commandBuilder.set("#Title.TextSpans", Message.translation(TITLE_TRANSLATION_KEY));
        
        // Set localized column headers
        commandBuilder.set("#ColStat.TextSpans", Message.translation(HEADER_TRANSLATION_PREFIX + "stat"));
        commandBuilder.set("#ColBase.TextSpans", Message.translation(HEADER_TRANSLATION_PREFIX + "base"));
        commandBuilder.set("#ColMod.TextSpans", Message.translation(HEADER_TRANSLATION_PREFIX + "modifier"));
        commandBuilder.set("#ColTotal.TextSpans", Message.translation(HEADER_TRANSLATION_PREFIX + "effective"));
        
        // Get the stat component for this entity
        HyforgedStatComponent statComponent = getStatComponent(ref, store);
        EntityStatMap statMap = StatAccessor.getStatMap(store, ref);
        
        // Build stat categories dynamically
        buildStatCategories(commandBuilder, statComponent, statMap);
        
        // Build equipment affix summary
        buildEquipmentSummary(commandBuilder, ref, store);
        
        // Add close button event
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CloseButton",
                EventData.of("Action", "close"),
                false
        );
        
        // Add refresh button event
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#RefreshButton",
                EventData.of("Action", "refresh"),
                false
        );
        
        // Add passive tree button event
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#PassiveTreeButton",
                EventData.of("Action", "openPassiveTree"),
                false
        );
    }
    
    /**
     * Get the HyforgedStatComponent for an entity.
     */
    private HyforgedStatComponent getStatComponent(Ref<EntityStore> ref, Store<EntityStore> store) {
        var componentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        if (componentType == null) {
            return null;
        }
        return store.getComponent(ref, componentType);
    }
    
    /**
     * Build all stat category groups dynamically from the registry.
     * <p>
     * Stats with the "ability-score" tag type are treated as primary attributes and
     * displayed first. Remaining categories are ordered by {@link CategoryDefinition#sortOrder()},
     * then alphabetically by display name when sort orders are equal.
     * <p>
     * Effective values are read from the Hyforged stat component's cached values
     * (populated by HyforgedStatComputeSystem), NOT from the Hytale EntityStatMap.
     * Base values for scaling stats are computed via ScalingEngine.
     */
    private void buildStatCategories(
            UICommandBuilder commandBuilder,
            @Nullable HyforgedStatComponent statComponent,
            @Nullable EntityStatMap statMap
    ) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        
        // Group stats by their category (case-insensitive key)
        Map<String, List<StatEntry>> categorizedStats = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        
        for (int i = 0; i < registry.getStatCount(); i++) {
            StatDefinition statDef = registry.getStat(i);
            if (statDef == null) {
                continue;
            }
            
            String categoryKey = statDef.category();
            
            // Compute proper base value: ScalingEngine for scaling stats, stored/default for others
            int baseValue = computeDisplayBase(i, statDef, statComponent, registry);
            
            // Read effective value from Hyforged cached values (NOT EntityStatMap)
            int effectiveValue = statComponent != null ? statComponent.getCachedValue(i) : statDef.defaultValue();
            
            // Modifier is the difference between effective and base
            int modifierTotal = effectiveValue - baseValue;
            
            List<ModifierBreakdown> breakdown = new ArrayList<>();
            if (statMap != null) {
                breakdown = getModifierBreakdown(statMap, i);
            }
            
            StatEntry entry = new StatEntry(statDef, i, baseValue, effectiveValue, modifierTotal, breakdown);
            categorizedStats.computeIfAbsent(categoryKey, k -> new ArrayList<>()).add(entry);
        }
        
        // Build ordered list of categories: ability-score first, then by sortOrder, then alphabetically
        List<Map.Entry<String, List<StatEntry>>> orderedCategories = new ArrayList<>(categorizedStats.entrySet());
        orderedCategories.sort(Comparator
                // Primary attributes (ability-score) always first
                .<Map.Entry<String, List<StatEntry>>, Boolean>comparing(
                        e -> !e.getKey().equalsIgnoreCase(ABILITY_SCORE_TAG))
                // Then by category sortOrder from CategoryDefinition
                .thenComparingInt(e -> {
                    CategoryDefinition catDef = registry.getCategory(e.getKey());
                    return catDef != null ? catDef.sortOrder() : Integer.MAX_VALUE;
                })
                // Then alphabetically by display name
                .thenComparing(e -> {
                    CategoryDefinition catDef = registry.getCategory(e.getKey());
                    return catDef != null ? catDef.displayName() : e.getKey();
                }, String.CASE_INSENSITIVE_ORDER)
        );
        
        // Clear the dynamic container before populating
        commandBuilder.clear("#StatCategories");
        
        // Track element index for indexed selector access
        int elementIndex = 0;
        
        // Build each category group dynamically
        for (Map.Entry<String, List<StatEntry>> categoryEntry : orderedCategories) {
            String categoryId = categoryEntry.getKey();
            List<StatEntry> stats = categoryEntry.getValue();
            
            if (stats.isEmpty()) {
                continue;
            }
            
            // Sort stats within category alphabetically by display name
            stats.sort(Comparator.comparing(
                    e -> e.definition().displayName(), String.CASE_INSENSITIVE_ORDER));
            
            // Build the category group from template files
            elementIndex = buildCategoryGroup(commandBuilder, categoryId, stats, registry, elementIndex);
        }
    }
    
    /**
     * Build a single category group with title and stat rows.
     * Uses file-based append with indexed selectors and descendant child selectors
     * (proven pattern from ConcentrationPriorityPage).
     * <p>
     * Each row is a Group with child Labels for each column:
     * {@code #StatCategories[n] #StatName}, {@code #StatCategories[n] #BaseValue}, etc.
     *
     * @param elementIndex the current child index within #StatCategories (from prior categories)
     * @return the new element index after appending this category's elements
     */
    private int buildCategoryGroup(
            UICommandBuilder commandBuilder,
            String categoryId,
            List<StatEntry> stats,
            StatDefinitionRegistry registry,
            int elementIndex
    ) {
        // Append category title from template
        commandBuilder.append("#StatCategories", TITLE_TEMPLATE_UI);
        String titleSelector = "#StatCategories[" + elementIndex + "]";
        String translationKey = CATEGORY_TRANSLATION_PREFIX + categoryId;
        commandBuilder.set(titleSelector + " #CategoryName.TextSpans",
                Message.translation(translationKey));
        elementIndex++;
        
        // Append stat rows from template
        for (int i = 0; i < stats.size(); i++) {
            commandBuilder.append("#StatCategories", ROW_TEMPLATE_UI);
            String rowSelector = "#StatCategories[" + elementIndex + "]";
            StatEntry stat = stats.get(i);
            
            // Set stat name
            commandBuilder.set(rowSelector + " #StatName.TextSpans",
                    Message.raw(stat.definition().displayName()));
            
            // Set base value
            String baseStr = formatValue(stat.baseValue(), stat.definition().displayFormat());
            commandBuilder.set(rowSelector + " #BaseValue.TextSpans",
                    Message.raw(baseStr));
            
            // Set modifier with color coding (green = positive, red = negative, gray = none)
            if (stat.modifierTotal() != 0) {
                String modStr = formatModifierValue(stat.modifierTotal(), stat.definition().displayFormat());
                String modColor = stat.modifierTotal() > 0 ? "#60d394" : "#d36c6c";
                commandBuilder.set(rowSelector + " #ModValue.TextSpans",
                        Message.raw(modStr).color(modColor));
            } else {
                commandBuilder.set(rowSelector + " #ModValue.TextSpans",
                        Message.raw("-").color("#666666"));
            }
            
            // Set effective/total value (bold via template style)
            String totalStr = formatValue(stat.computedValue(), stat.definition().displayFormat());
            commandBuilder.set(rowSelector + " #TotalValue.TextSpans",
                    Message.raw(totalStr));
            
            // Set range (min / max)
            String rangeStr = formatRange(stat.definition());
            commandBuilder.set(rowSelector + " #RangeValue.TextSpans",
                    Message.raw(rangeStr));
            
            elementIndex++;
        }
        
        return elementIndex;
    }
    
    /**
     * Compute the display base value for a stat.
     * <p>
     * For scaling stats (e.g. Attack Power scales from Strength), the "base" is the
     * ScalingEngine's output — the value before modifiers are applied. For non-scaling
     * stats, the base is the stored value or the stat's default.
     *
     * @param statIndex The stat index
     * @param statDef The stat definition
     * @param component The stat component (nullable)
     * @param registry The stat definition registry
     * @return The base value for display purposes
     */
    private int computeDisplayBase(
            int statIndex,
            @Nonnull StatDefinition statDef,
            @Nullable HyforgedStatComponent component,
            @Nonnull StatDefinitionRegistry registry
    ) {
        if (component == null) {
            return statDef.defaultValue();
        }
        
        if (statDef.hasScaling()) {
            // For scaling stats, recompute the scaled base from source stat cached values
            return ScalingEngine.computeScaledBase(
                    statDef,
                    component::getCachedValue,
                    registry
            );
        }
        
        // For non-scaling stats, use the stored base or default
        return component.getBaseValue(statIndex);
    }
    
    /**
     * Build the equipment affix summary section.
     */
    private void buildEquipmentSummary(UICommandBuilder commandBuilder, Ref<EntityStore> ref, Store<EntityStore> store) {
        // Set localized equipment title
        commandBuilder.set("#EquipmentTitle.TextSpans", Message.translation(EQUIPMENT_TRANSLATION_KEY));
        
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        var inventory = player.getInventory();
        if (inventory == null) {
            return;
        }
        
        // Build armor slot summaries (use fixed selectors #Armor0-3)
        ItemContainer armorContainer = inventory.getArmor();
        if (armorContainer != null) {
            int slotCount = Math.min((int) armorContainer.getCapacity(), 4);
            for (int i = 0; i < slotCount; i++) {
                ItemStack itemStack = armorContainer.getItemStack((short) i);
                Message slotMessage = buildSlotMessage(i, itemStack);
                commandBuilder.set("#Armor" + i + ".TextSpans", slotMessage);
            }
            for (int i = slotCount; i < 4; i++) {
                commandBuilder.set("#Armor" + i + ".Text", "");
            }
        }
        
        // Build hand slot summaries (use fixed selectors #Hand0-1)
        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar != null) {
            for (int i = 0; i < 2; i++) {
                ItemStack itemStack = i < hotbar.getCapacity() ? hotbar.getItemStack((short) i) : null;
                Message slotMessage = buildSlotMessage(i, itemStack);
                commandBuilder.set("#Hand" + i + ".TextSpans", slotMessage);
            }
        }
    }
    
    /**
     * Build a localized message for an equipment slot.
     */
    private Message buildSlotMessage(int index, ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return Message.translation(EMPTY_SLOT_TRANSLATION_KEY)
                    .param("slot", String.valueOf(index + 1));
        }
        String itemName = itemStack.getItem() != null ? itemStack.getItem().getId() : "Unknown";
        // Simplify the ID for display
        if (itemName.contains(":")) {
            itemName = itemName.substring(itemName.lastIndexOf(':') + 1);
        }
        // Title-case the item name
        itemName = titleCase(itemName.replace('-', ' ').replace('_', ' '));
        return Message.raw((index + 1) + ": " + itemName);
    }
    
    /**
     * Title-case a string (first letter of each word capitalized).
     */
    private String titleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String[] words = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
    
    /**
     * Get modifier breakdown for a stat.
     */
    private List<ModifierBreakdown> getModifierBreakdown(EntityStatMap statMap, int statIndex) {
        List<ModifierBreakdown> breakdown = new ArrayList<>();

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        for (HyforgedModifier modifier : StatAccessor.getAllHyforgedModifiers(statMap)) {
            if (modifier.getTargetStatIndex() == statIndex) {
                breakdown.add(new ModifierBreakdown(
                        modifier.getSourceId(),
                        modifier.getSourceType(),
                        modifier.getAmount(),
                        modifier.getStackType()
                ));
            } else if (modifier.getTargetTagIndex() != HyforgedModifier.NO_TAG) {
                var affected = registry.getStatIndicesForTagIndex(modifier.getTargetTagIndex());
                if (affected.contains(statIndex)) {
                    breakdown.add(new ModifierBreakdown(
                            modifier.getSourceId(),
                            modifier.getSourceType(),
                            modifier.getAmount(),
                            modifier.getStackType()
                    ));
                }
            }
        }
        
        return breakdown;
    }
    
    /**
     * Format a stat value according to its display format.
     * Strips unnecessary trailing zeros (e.g., "0.0%" → "0", "15.0%" → "15%").
     */
    private String formatValue(int value, DisplayFormat format) {
        String raw = switch (format) {
            case INTEGER -> String.valueOf(value);
            case PERCENT_BPS -> {
                double pct = value / 100.0;
                yield (pct == (long) pct) ? (long) pct + "%" : String.format("%.1f%%", pct).replaceAll("0+%$", "%");
            }
            case PERCENT -> value + "%";
            case RATING -> value + " rating";
            case FLAT_BONUS -> (value >= 0 ? "+" : "") + value;
            case MULTIPLIER -> {
                double mult = value / 10000.0;
                String s = String.format("%.2fx", mult);
                // Strip trailing zeros but keep at least one decimal
                s = s.replaceAll("0+x$", "x");
                if (s.contains(".") && s.endsWith("x") && s.charAt(s.indexOf('x') - 1) == '.') {
                    s = s.replace(".x", ".0x");
                }
                yield s;
            }
        };
        // If value is exactly 0, just show "0" regardless of format suffix
        if (value == 0) {
            return "0";
        }
        return raw;
    }
    
    /**
     * Format a modifier value with sign.
     */
    private String formatModifierValue(int value, DisplayFormat format) {
        if (value == 0) {
            return "-";
        }
        String formatted = formatValue(Math.abs(value), format);
        return (value >= 0 ? "+" : "-") + formatted;
    }
    
    /**
     * Format the min/max range for a stat.
     * Shows formatted values separated by " / ". Omits unbounded extremes.
     */
    private String formatRange(@Nonnull StatDefinition def) {
        DisplayFormat fmt = def.displayFormat();
        int min = def.minValue();
        int max = def.maxValue();
        
        boolean minUnbounded = (min == Integer.MIN_VALUE || min == -2147483648);
        boolean maxUnbounded = (max == Integer.MAX_VALUE || max == 2147483647);
        
        if (minUnbounded && maxUnbounded) {
            return "-";
        }
        
        String minStr = minUnbounded ? "--" : formatValue(min, fmt);
        String maxStr = maxUnbounded ? "--" : formatValue(max, fmt);
        return minStr + " / " + maxStr;
    }
    
    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData eventData
    ) {
        String action = eventData.getAction();
        
        if ("close".equals(action)) {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent != null) {
                playerComponent.getPageManager().setPage(ref, store, Page.None);
            }
        } else if ("refresh".equals(action)) {
            rebuild();
        } else if ("openPassiveTree".equals(action)) {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent != null) {
                PassiveTreePage passivePage = new PassiveTreePage(this.playerRef, null);
                playerComponent.getPageManager().openCustomPage(ref, store, passivePage);
            }
        }
    }
    
    // ========== INNER CLASSES ==========
    
    /**
     * Event data for page interactions.
     */
    public static class PageEventData {
        public static final BuilderCodec<PageEventData> CODEC = BuilderCodec.builder(
                        PageEventData.class, PageEventData::new
                )
                .append(new KeyedCodec<>("Action", Codec.STRING), (e, s) -> e.action = s, e -> e.action)
                .add()
                .build();
        
        private String action;
        
        public PageEventData() {}
        
        public String getAction() {
            return action;
        }
    }
    
    /**
     * Entry for a single stat in the display.
     */
    public record StatEntry(
            StatDefinition definition,
            int statIndex,
            int baseValue,
            int computedValue,
            int modifierTotal,
            List<ModifierBreakdown> breakdown
    ) {}
    
    /**
     * Breakdown of a single modifier contribution.
     */
    public record ModifierBreakdown(
            String sourceId,
            HyforgedModifier.SourceType sourceType,
            int value,
            HyforgedModifier.StackType modifierType
    ) {}
}