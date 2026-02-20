package reign.software.hyforged.affix.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.Message;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.service.AffixTooltipProvider;
import reign.software.hyforged.affix.service.HyforgedItemDataService;
import reign.software.hyforged.quality.service.HyforgedQualityService;
import reign.software.hyforged.stats.CategoryDefinition;
import reign.software.hyforged.stats.DisplayFormat;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.engine.ScalingEngine;
import reign.software.hyforged.stats.modifier.HyforgedModifier;
import reign.software.hyforged.passive.ui.PassiveTreePage;
import reign.software.hyforged.util.MessageColors;

import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
 * Categories are fully data-driven from JSON definitions in Server/Hyforged/Stats/Categories/.
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
    
    /** Translation key for empty equipment slot */
    private static final String EMPTY_SLOT_TRANSLATION_KEY = "hyforged.characterStats.slot.empty";

    /** Translation key for equipment panel title */
    private static final String EQUIP_PANEL_TITLE_KEY = "hyforged.characterStats.equipmentPanel.title";

    /** Translation key prefix for slot type labels */
    private static final String SLOT_TRANSLATION_PREFIX = "hyforged.characterStats.slot.";

    /** Armor slot identifiers for translation key suffixes and UI selectors */
    private static final String[] ARMOR_SLOT_IDS = {"head", "chest", "legs", "feet"};

    /** Hand slot identifiers for translation key suffixes and UI selectors */
    private static final String[] HAND_SLOT_IDS = {"mainHand", "offHand"};

    /** UI action key for showing an equipment tooltip */
    private static final String ACTION_SHOW_EQUIP_TOOLTIP = "showEquipTooltip";

    /** UI action key for hiding an equipment tooltip */
    private static final String ACTION_HIDE_EQUIP_TOOLTIP = "hideEquipTooltip";

    /** Fallback color when quality color is unavailable */
    private static final String DEFAULT_QUALITY_COLOR = "#CCCCCC";

    /** Maximum modifier source lines shown in the custom tooltip */
    private static final int MAX_TOOLTIP_LINES = 8;

    /** Maximum characters per modifier source line in the custom tooltip */
    private static final int MAX_TOOLTIP_LINE_CHARS = 140;

    /** Source ID prefix for class-level progression modifiers */
    private static final String CLASS_LEVEL_SOURCE_PREFIX = "class-level:";

    /** Source ID prefix for character-level progression modifiers */
    private static final String CHARACTER_LEVEL_SOURCE_PREFIX = "character-level:";

    /** UI action key for showing a modifier tooltip */
    private static final String ACTION_SHOW_MODIFIER_TOOLTIP = "showModifierTooltip";

    /** UI action key for hiding a modifier tooltip */
    private static final String ACTION_HIDE_MODIFIER_TOOLTIP = "hideModifierTooltip";

    /** Selector of the currently visible modifier tooltip row */
    @Nullable
    private String activeModifierTooltipTarget;

    /** Prebuilt modifier tooltip text by row selector. */
    @Nonnull
    private final Map<String, Message> modifierTooltipByRowSelector = new HashMap<>();

    /** Prebuilt equipment tooltip text by slot identifier (e.g. "armor:0", "hand:1"). */
    @Nonnull
    private final Map<String, Message> equipTooltipBySlotId = new HashMap<>();

    /** Selector of the currently visible equipment tooltip slot */
    @Nullable
    private String activeEquipTooltipTarget;


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
        this.activeModifierTooltipTarget = null;
        this.modifierTooltipByRowSelector.clear();
        this.equipTooltipBySlotId.clear();
        this.activeEquipTooltipTarget = null;
        commandBuilder.append(PAGE_UI_FILE);
        commandBuilder.set("#ModifierTooltip.Visible", false);
        commandBuilder.set("#EquipmentTooltip.Visible", false);
        
        // Set localized title
        commandBuilder.set("#Title.TextSpans", Message.translation(TITLE_TRANSLATION_KEY));
        
        // Set localized column headers
        commandBuilder.set("#ColStat.TextSpans", Message.translation(HEADER_TRANSLATION_PREFIX + "stat"));
        commandBuilder.set("#ColBase.TextSpans", Message.translation(HEADER_TRANSLATION_PREFIX + "base"));
        commandBuilder.set("#ColMod.TextSpans", Message.translation(HEADER_TRANSLATION_PREFIX + "modifier"));
        commandBuilder.set("#ColTotal.TextSpans", Message.translation(HEADER_TRANSLATION_PREFIX + "effective"));

        // Add footer button events first to ensure they are preserved even with many row bindings
        bindFooterButtons(eventBuilder);
        
        // Get the stat component for this entity
        HyforgedStatComponent statComponent = getStatComponent(ref, store);
        EntityStatMap statMap = StatAccessor.getStatMap(store, ref);
        
        // Build stat categories dynamically
        buildStatCategories(commandBuilder, eventBuilder, statComponent, statMap);
        
        // Build equipment panel with item icons and hover tooltips
        buildEquipmentPanel(commandBuilder, eventBuilder, ref, store);
    }

        private void bindFooterButtons(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Action", "close"),
            false
        );

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#RefreshButton",
            EventData.of("Action", "refresh"),
            false
        );

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
            UIEventBuilder eventBuilder,
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
            if (statComponent != null || statMap != null) {
                breakdown = getModifierBreakdown(statComponent, statMap, i, registry);
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
            elementIndex = buildCategoryGroup(commandBuilder, eventBuilder, categoryId, stats, registry, elementIndex);
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
            UIEventBuilder eventBuilder,
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
            String statDescriptionTooltip = buildStatDescriptionTooltip(stat.definition());
            if (!statDescriptionTooltip.isEmpty()) {
                commandBuilder.set(rowSelector + " #StatName.TooltipText", statDescriptionTooltip);
            }
            
            // Set base value
            String baseStr = formatValue(stat.baseValue(), stat.definition().displayFormat());
            commandBuilder.set(rowSelector + " #BaseValue.TextSpans",
                    Message.raw(baseStr));
            
            // Set modifier with color coding (green = positive, red = negative, gray = none)
            if (stat.modifierTotal() != 0) {
                String modStr = formatModifierValue(stat.modifierTotal(), stat.definition().displayFormat());
                String modColor = stat.modifierTotal() > 0 ? "#60d394" : "#d36c6c";
                commandBuilder.set(rowSelector + " #ModValueLabel.TextSpans",
                        Message.raw(modStr).color(modColor));
                Message modifierTooltipMessage = buildModifierSourcesTooltipMessage(stat);
                if (modifierTooltipMessage != null) {
                    modifierTooltipByRowSelector.put(rowSelector, modifierTooltipMessage);
                    bindModifierTooltipEvents(eventBuilder, rowSelector);
                }
            } else {
                commandBuilder.set(rowSelector + " #ModValueLabel.TextSpans",
                        Message.raw("-").color("#666666"));
            }
            
            // Set effective/total value (bold via template style)
            String totalStr = formatValue(stat.computedValue(), stat.definition().displayFormat());
            commandBuilder.set(rowSelector + " #TotalValue.TextSpans",
                    Message.raw(totalStr));
            
            elementIndex++;
        }
        
        return elementIndex;
    }

        private void bindModifierTooltipEvents(@Nonnull UIEventBuilder eventBuilder, @Nonnull String rowSelector) {
        EventData showData = new EventData()
            .append(PageEventData.KEY_ACTION, ACTION_SHOW_MODIFIER_TOOLTIP)
            .append(PageEventData.KEY_TOOLTIP_TARGET, rowSelector);
        EventData hideData = new EventData()
            .append(PageEventData.KEY_ACTION, ACTION_HIDE_MODIFIER_TOOLTIP)
            .append(PageEventData.KEY_TOOLTIP_TARGET, rowSelector);

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.MouseEntered,
            rowSelector + " #ModValue",
            showData,
            false
        );

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.MouseExited,
            rowSelector + " #ModValue",
            hideData,
            false
        );
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
     * Build the equipment panel with item icons and hover tooltips for all 6 slots.
     * <p>
     * Populates each slot with a 1-slot ItemGrid showing the equipped item icon,
     * or a text fallback showing "Empty" for unoccupied slots. Pre-builds tooltip
     * content for equipped items and binds hover events.
     */
    private void buildEquipmentPanel(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store
    ) {
        // Set localized equipment panel title
        commandBuilder.set("#EquipmentPanelTitle.TextSpans", Message.translation(EQUIP_PANEL_TITLE_KEY));
        
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            populateEmptySlots(commandBuilder);
            return;
        }
        
        var inventory = player.getInventory();
        if (inventory == null) {
            populateEmptySlots(commandBuilder);
            return;
        }
        
        // Build armor slots (0-3)
        ItemContainer armorContainer = inventory.getArmor();
        for (int i = 0; i < ARMOR_SLOT_IDS.length; i++) {
            String slotUiId = "#SlotArmor" + i;
            String translationKey = SLOT_TRANSLATION_PREFIX + ARMOR_SLOT_IDS[i];
            ItemStack itemStack = (armorContainer != null && i < armorContainer.getCapacity())
                    ? armorContainer.getItemStack((short) i) : null;
            String slotKey = "armor:" + i;
            populateEquipmentSlot(commandBuilder, eventBuilder, slotUiId, translationKey, itemStack, slotKey);
        }
        
        // Build hand slots (0-1)
        ItemContainer hotbar = inventory.getHotbar();
        for (int i = 0; i < HAND_SLOT_IDS.length; i++) {
            String slotUiId = "#SlotHand" + i;
            String translationKey = SLOT_TRANSLATION_PREFIX + HAND_SLOT_IDS[i];
            ItemStack itemStack = (hotbar != null && i < hotbar.getCapacity())
                    ? hotbar.getItemStack((short) i) : null;
            String slotKey = "hand:" + i;
            populateEquipmentSlot(commandBuilder, eventBuilder, slotUiId, translationKey, itemStack, slotKey);
        }
    }

    /**
     * Populate all equipment slots with empty state (used when player/inventory is null).
     */
    private void populateEmptySlots(@Nonnull UICommandBuilder commandBuilder) {
        for (int i = 0; i < ARMOR_SLOT_IDS.length; i++) {
            String slotUiId = "#SlotArmor" + i;
            commandBuilder.set(slotUiId + " #SlotLabel.TextSpans",
                    Message.translation(SLOT_TRANSLATION_PREFIX + ARMOR_SLOT_IDS[i]));
            commandBuilder.set(slotUiId + " #SlotGrid.Visible", false);
            commandBuilder.set(slotUiId + " #SlotFallback.Visible", true);
            commandBuilder.set(slotUiId + " #SlotFallback.TextSpans",
                    Message.translation(EMPTY_SLOT_TRANSLATION_KEY).color("#666666"));
        }
        for (int i = 0; i < HAND_SLOT_IDS.length; i++) {
            String slotUiId = "#SlotHand" + i;
            commandBuilder.set(slotUiId + " #SlotLabel.TextSpans",
                    Message.translation(SLOT_TRANSLATION_PREFIX + HAND_SLOT_IDS[i]));
            commandBuilder.set(slotUiId + " #SlotGrid.Visible", false);
            commandBuilder.set(slotUiId + " #SlotFallback.Visible", true);
            commandBuilder.set(slotUiId + " #SlotFallback.TextSpans",
                    Message.translation(EMPTY_SLOT_TRANSLATION_KEY).color("#666666"));
        }
    }

    /**
     * Populate a single equipment slot with item icon or empty indicator.
     * Binds hover events for equipped items and pre-builds tooltip content.
     */
    private void populateEquipmentSlot(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull String slotUiId,
            @Nonnull String translationKey,
            @Nullable ItemStack itemStack,
            @Nonnull String slotKey
    ) {
        // Set localized slot label
        commandBuilder.set(slotUiId + " #SlotLabel.TextSpans", Message.translation(translationKey));
        
        if (itemStack == null || itemStack.isEmpty()) {
            // Empty slot: hide ItemGrid, show fallback label with "Empty" text
            commandBuilder.set(slotUiId + " #SlotGrid.Visible", false);
            commandBuilder.set(slotUiId + " #SlotFallback.Visible", true);
            commandBuilder.set(slotUiId + " #SlotFallback.TextSpans",
                    Message.translation(EMPTY_SLOT_TRANSLATION_KEY).color("#666666"));
            // No hover events for empty slots (FR-6)
        } else {
            // Equipped item: show ItemGrid with item icon, hide fallback
            // Strip custom Hyforged metadata — client can't deserialize it as ClientItemMetadata
            ItemStack displayStack = itemStack.withMetadata((BsonDocument) null);
            commandBuilder.set(slotUiId + " #SlotGrid.Slots",
                    new ItemGridSlot[]{ new ItemGridSlot(displayStack) });
            commandBuilder.set(slotUiId + " #SlotGrid.Visible", true);
            commandBuilder.set(slotUiId + " #SlotFallback.Visible", false);
            
            // Pre-build tooltip content and cache it
            Message tooltipMessage = buildEquipmentTooltipMessage(itemStack);
            if (tooltipMessage != null) {
                equipTooltipBySlotId.put(slotKey, tooltipMessage);
            }
            
            // Bind hover events for tooltip display
            EventData showData = new EventData()
                    .append(PageEventData.KEY_ACTION, ACTION_SHOW_EQUIP_TOOLTIP)
                    .append(PageEventData.KEY_TOOLTIP_TARGET, slotKey);
            EventData hideData = new EventData()
                    .append(PageEventData.KEY_ACTION, ACTION_HIDE_EQUIP_TOOLTIP)
                    .append(PageEventData.KEY_TOOLTIP_TARGET, slotKey);
            
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.SlotMouseEntered,
                    slotUiId + " #SlotGrid",
                    showData,
                    false
            );
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.SlotMouseExited,
                    slotUiId + " #SlotGrid",
                    hideData,
                    false
            );
        }
    }

    /**
     * Build a rich tooltip message for an equipped item showing quality-colored name,
     * quality label, and affix lines.
     *
     * @param itemStack The equipped item
     * @return The tooltip message, or null if no content could be generated
     */
    @Nullable
    private Message buildEquipmentTooltipMessage(@Nonnull ItemStack itemStack) {
        // Resolve item display name
        String itemName = itemStack.getItem() != null ? itemStack.getItem().getId() : "Unknown";
        if (itemName.contains(":")) {
            itemName = itemName.substring(itemName.lastIndexOf(':') + 1);
        }
        itemName = titleCase(itemName.replace('-', ' ').replace('_', ' '));
        
        // Resolve quality color
        String qualityId = HyforgedQualityService.getEffectiveQuality(itemStack);
        String qualityColor = resolveQualityHexColor(qualityId);
        
        // Build message: item name colored by quality
        Message tooltip = Message.raw(itemName).color(qualityColor);
        
        // Add quality label line (e.g., "[Rare]") if quality is available
        if (qualityId != null && !qualityId.isBlank()) {
            tooltip.insert(Message.raw("\n[" + qualityId + "]").color(qualityColor));
        }
        
        // Add affix lines from AffixTooltipProvider
        HyforgedItemData itemData = HyforgedItemDataService.read(itemStack);
        AffixTooltipProvider.TooltipContent tooltipContent = AffixTooltipProvider.generateTooltip(itemData);
        if (tooltipContent.hasContent()) {
            for (AffixTooltipProvider.TooltipSection section : tooltipContent.sections()) {
                if (section.lines().isEmpty()) {
                    continue;
                }
                // Section header
                tooltip.insert(Message.raw("\n" + section.sectionName()).color(section.hudColor()));
                // Affix lines
                for (AffixTooltipProvider.TooltipLine line : section.lines()) {
                    String lineColor = line.color() != null ? line.color() : section.hudColor();
                    tooltip.insert(Message.raw("\n  " + line.text()).color(lineColor));
                }
            }
        }
        
        return tooltip;
    }

    /**
     * Resolve the hex color for a quality tier from the quality asset system.
     *
     * @param qualityId The quality identifier (e.g., "Rare", "Common")
     * @return Hex color string (e.g., "#FF5500"), or the default fallback color
     */
    @Nonnull
    private String resolveQualityHexColor(@Nullable String qualityId) {
        if (qualityId == null || qualityId.isBlank()) {
            return DEFAULT_QUALITY_COLOR;
        }
        try {
            ItemQuality quality = ItemQuality.getAssetMap().getAsset(qualityId);
            if (quality != null && quality.getTextColor() != null) {
                return toHexColor(quality.getTextColor());
            }
        } catch (Exception ignored) {
            // Fallback to default if asset lookup fails
        }
        return DEFAULT_QUALITY_COLOR;
    }

    /**
     * Convert a Hytale Color to a hex color string.
     *
     * @param color The color to convert
     * @return Hex color string (e.g., "#FF5500")
     */
    @Nonnull
    private static String toHexColor(@Nonnull Color color) {
        int r = Byte.toUnsignedInt(color.red);
        int g = Byte.toUnsignedInt(color.green);
        int b = Byte.toUnsignedInt(color.blue);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * Show the equipment tooltip for a specific slot.
     */
    private void handleShowEquipTooltip(@Nullable String tooltipTarget) {
        if (tooltipTarget == null || tooltipTarget.isBlank()) {
            handleHideEquipTooltip(null);
            return;
        }

        Message tooltipMessage = equipTooltipBySlotId.get(tooltipTarget);
        if (tooltipMessage == null) {
            handleHideEquipTooltip(tooltipTarget);
            return;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.set("#EquipmentTooltipText.TextSpans", tooltipMessage);
        commandBuilder.set("#EquipmentTooltip.Visible", true);
        activeEquipTooltipTarget = tooltipTarget;
        sendUpdate(commandBuilder, new UIEventBuilder(), false);
    }

    /**
     * Hide the equipment tooltip.
     */
    private void handleHideEquipTooltip(@Nullable String tooltipTarget) {
        if (tooltipTarget != null
                && !tooltipTarget.isBlank()
                && activeEquipTooltipTarget != null
                && !activeEquipTooltipTarget.isBlank()
                && !activeEquipTooltipTarget.equals(tooltipTarget)) {
            return;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.set("#EquipmentTooltip.Visible", false);
        activeEquipTooltipTarget = null;
        sendUpdate(commandBuilder, new UIEventBuilder(), false);
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
    private List<ModifierBreakdown> getModifierBreakdown(
            @Nullable HyforgedStatComponent statComponent,
            @Nullable EntityStatMap statMap,
            int statIndex,
            @Nonnull StatDefinitionRegistry registry
    ) {
        List<ModifierBreakdown> breakdown = new ArrayList<>();

        List<HyforgedModifier> allModifiers = new ArrayList<>();
        if (statComponent != null) {
            allModifiers.addAll(statComponent.getModifiers());
            for (var conditionalModifier : statComponent.getConditionalModifiers()) {
                allModifiers.add(conditionalModifier.modifier());
            }
        }
        if (statMap != null) {
            allModifiers.addAll(StatAccessor.getAllHyforgedModifiers(statMap));
        }

        Set<String> seen = new HashSet<>();
        for (HyforgedModifier modifier : allModifiers) {
            if (modifier.getTargetStatIndex() == statIndex) {
                String dedupeKey = buildModifierDedupeKey(modifier);
                if (seen.add(dedupeKey)) {
                    breakdown.add(new ModifierBreakdown(
                            modifier.getSourceId(),
                            modifier.getSourceType(),
                            modifier.getAmount(),
                            modifier.getStackType()
                    ));
                }
            } else if (modifier.getTargetTagIndex() != HyforgedModifier.NO_TAG) {
                var affected = registry.getStatIndicesForTagIndex(modifier.getTargetTagIndex());
                if (affected.contains(statIndex)) {
                    String dedupeKey = buildModifierDedupeKey(modifier);
                    if (seen.add(dedupeKey)) {
                        breakdown.add(new ModifierBreakdown(
                                modifier.getSourceId(),
                                modifier.getSourceType(),
                                modifier.getAmount(),
                                modifier.getStackType()
                        ));
                    }
                }
            }
        }
        
        return breakdown;
    }

    @Nonnull
    private String buildModifierDedupeKey(@Nonnull HyforgedModifier modifier) {
        return (modifier.getSourceId() == null ? "" : modifier.getSourceId())
                + "|" + modifier.getSourceType()
                + "|" + modifier.getAmount()
                + "|" + modifier.getStackType()
                + "|" + modifier.getTargetStatIndex()
                + "|" + modifier.getTargetTagIndex();
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
     * Build tooltip text for a stat name using the stat description.
     * <p>
     * When the stat has meaningful min/max bounds, the range is appended
     * to the tooltip text (FR-8).
     */
    @Nonnull
    private String buildStatDescriptionTooltip(@Nonnull StatDefinition definition) {
        StringBuilder tooltip = new StringBuilder();
        String description = definition.description();
        if (description != null && !description.isBlank()) {
            tooltip.append(description.trim());
        }
        // Append range info when meaningful (not both bounds unbounded)
        String rangeStr = formatRange(definition);
        if (!"-".equals(rangeStr)) {
            if (tooltip.length() > 0) {
                tooltip.append("\n");
            }
            tooltip.append("Range: ").append(rangeStr);
        }
        return tooltip.toString();
    }

    /**
     * Build a compact color-coded custom tooltip message listing modifier sources.
     */
    @Nullable
    private Message buildModifierSourcesTooltipMessage(@Nonnull StatEntry stat) {
        if (stat.breakdown() == null || stat.breakdown().isEmpty()) {
            return null;
        }

        Message tooltip = Message.raw("");
        int linesAdded = 0;

        for (ModifierBreakdown breakdown : stat.breakdown()) {
            if (linesAdded >= MAX_TOOLTIP_LINES) {
                break;
            }

            String line = formatModifierSourceLine(breakdown, stat.definition().displayFormat());
            if (line.length() > MAX_TOOLTIP_LINE_CHARS) {
                line = line.substring(0, Math.max(0, MAX_TOOLTIP_LINE_CHARS - 3)) + "...";
            }

            if (linesAdded > 0) {
                tooltip.insert(Message.raw("\n"));
            }
            tooltip.insert(Message.raw(line).color(resolveModifierSourceColor(breakdown)));
            linesAdded++;
        }

        if (stat.breakdown().size() > linesAdded) {
            tooltip.insert(Message.raw("\n...").color(MessageColors.GRAY));
        }

        return linesAdded > 0 ? tooltip : null;
    }

    /**
     * Format one modifier source line for tooltip display.
     */
    @Nonnull
    private String formatModifierSourceLine(@Nonnull ModifierBreakdown breakdown, @Nonnull DisplayFormat displayFormat) {
        String amountText = switch (breakdown.modifierType()) {
            case FLAT -> formatModifierValue(breakdown.value(), displayFormat);
            case INCREASED, MORE -> formatSignedPercentBps(breakdown.value());
            case CAP -> (breakdown.value() >= 0 ? "+" : "") + breakdown.value();
        };

        StringBuilder line = new StringBuilder();
        line.append(amountText)
            .append(" [")
            .append(formatModifierTypeLabel(breakdown.modifierType()))
            .append("] ")
            .append(formatModifierSourceCategory(breakdown));

        String sourceLabel = formatSourceLabel(breakdown.sourceId());
        if (!sourceLabel.isEmpty()) {
            line.append(" - ").append(sourceLabel);
        }

        return line.toString();
    }

    @Nonnull
    private String formatModifierSourceCategory(@Nonnull ModifierBreakdown breakdown) {
        String sourceId = breakdown.sourceId();
        if (sourceId != null) {
            if (sourceId.startsWith(CHARACTER_LEVEL_SOURCE_PREFIX)) {
                return "Character Level";
            }
            if (sourceId.startsWith(CLASS_LEVEL_SOURCE_PREFIX)) {
                return "Class Level";
            }
        }

        if (breakdown.sourceType() == HyforgedModifier.SourceType.PASSIVE) {
            return "Passive";
        }
        if (breakdown.sourceType() == HyforgedModifier.SourceType.EQUIPMENT) {
            return "Equipment";
        }

        return formatSourceTypeLabel(breakdown.sourceType());
    }

    @Nonnull
    private String resolveModifierSourceColor(@Nonnull ModifierBreakdown breakdown) {
        String sourceId = breakdown.sourceId();
        if (sourceId != null) {
            if (sourceId.startsWith(CHARACTER_LEVEL_SOURCE_PREFIX)) {
                return MessageColors.AQUA;
            }
            if (sourceId.startsWith(CLASS_LEVEL_SOURCE_PREFIX)) {
                return MessageColors.PURPLE;
            }
        }

        if (breakdown.sourceType() == HyforgedModifier.SourceType.PASSIVE) {
            return MessageColors.SUCCESS;
        }
        if (breakdown.sourceType() == HyforgedModifier.SourceType.EQUIPMENT) {
            return MessageColors.GOLD;
        }
        if (breakdown.sourceType() == HyforgedModifier.SourceType.CLASS) {
            return MessageColors.PURPLE;
        }

        return MessageColors.WHITE;
    }

    /**
     * Format basis points as signed percent text.
     */
    @Nonnull
    private String formatSignedPercentBps(int valueBps) {
        double percent = valueBps / 100.0;
        String formatted = (percent == Math.rint(percent))
                ? String.format(Locale.ROOT, "%.0f%%", percent)
                : String.format(Locale.ROOT, "%.1f%%", percent);
        return (percent >= 0 ? "+" : "") + formatted;
    }

    /**
     * Format source type into a readable label.
     */
    @Nonnull
    private String formatSourceTypeLabel(@Nonnull HyforgedModifier.SourceType sourceType) {
        return titleCase(sourceType.name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    /**
     * Format modifier stack type into a readable label.
     */
    @Nonnull
    private String formatModifierTypeLabel(@Nonnull HyforgedModifier.StackType stackType) {
        return titleCase(stackType.name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    /**
     * Format a source identifier into a readable label.
     */
    @Nonnull
    private String formatSourceLabel(@Nullable String sourceId) {
        if (sourceId == null) {
            return "";
        }

        String normalized = sourceId.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        if (normalized.startsWith(CLASS_LEVEL_SOURCE_PREFIX)) {
            return formatClassLevelSourceLabel(normalized);
        }

        if (normalized.startsWith(CHARACTER_LEVEL_SOURCE_PREFIX)) {
            return formatCharacterLevelSourceLabel(normalized);
        }

        if (normalized.contains(":")) {
            normalized = normalized.substring(normalized.lastIndexOf(':') + 1);
        }

        normalized = normalized.replace('_', ' ').replace('-', ' ');
        if (normalized.isEmpty()) {
            return "";
        }

        return titleCase(normalized);
    }

    @Nonnull
    private String formatClassLevelSourceLabel(@Nonnull String sourceId) {
        String payload = sourceId.substring(CLASS_LEVEL_SOURCE_PREFIX.length());
        int statLocalSeparator = payload.lastIndexOf(':');
        if (statLocalSeparator < 0) {
            return "Class Level";
        }

        int statNamespaceSeparator = payload.lastIndexOf(':', statLocalSeparator - 1);
        if (statNamespaceSeparator < 0) {
            return "Class Level";
        }

        int classLevelSeparator = payload.lastIndexOf(':', statNamespaceSeparator - 1);
        if (classLevelSeparator < 0) {
            return "Class Level";
        }

        String classId = payload.substring(0, classLevelSeparator);
        String level = payload.substring(classLevelSeparator + 1, statNamespaceSeparator);
        String className = classId;
        if (className.contains(":")) {
            className = className.substring(className.lastIndexOf(':') + 1);
        }
        className = titleCase(className.replace('_', ' ').replace('-', ' '));
        if (className.isEmpty()) {
            className = "Class";
        }

        return "Class L" + level + " (" + className + ")";
    }

    @Nonnull
    private String formatCharacterLevelSourceLabel(@Nonnull String sourceId) {
        String payload = sourceId.substring(CHARACTER_LEVEL_SOURCE_PREFIX.length());
        int levelSeparator = payload.indexOf(':');
        String level = levelSeparator >= 0 ? payload.substring(0, levelSeparator) : payload;
        return "Character L" + level;
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
            activeModifierTooltipTarget = null;
            rebuild();
        } else if ("openPassiveTree".equals(action)) {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent != null) {
                PassiveTreePage passivePage = new PassiveTreePage(this.playerRef, null);
                playerComponent.getPageManager().openCustomPage(ref, store, passivePage);
            }
        } else if (ACTION_SHOW_MODIFIER_TOOLTIP.equals(action)) {
            handleShowModifierTooltip(eventData.getTooltipTarget());
        } else if (ACTION_HIDE_MODIFIER_TOOLTIP.equals(action)) {
            handleHideModifierTooltip(eventData.getTooltipTarget());
        } else if (ACTION_SHOW_EQUIP_TOOLTIP.equals(action)) {
            handleShowEquipTooltip(eventData.getTooltipTarget());
        } else if (ACTION_HIDE_EQUIP_TOOLTIP.equals(action)) {
            handleHideEquipTooltip(eventData.getTooltipTarget());
        }
    }

    private void handleShowModifierTooltip(@Nullable String tooltipTarget) {
        if (tooltipTarget == null || tooltipTarget.isBlank()) {
            handleHideModifierTooltip(null);
            return;
        }

        Message tooltipMessage = modifierTooltipByRowSelector.get(tooltipTarget);
        if (tooltipMessage == null) {
            handleHideModifierTooltip(tooltipTarget);
            return;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.set("#ModifierTooltipText.TextSpans", tooltipMessage);
        commandBuilder.set("#ModifierTooltip.Visible", true);
        activeModifierTooltipTarget = tooltipTarget;
        sendUpdate(commandBuilder, new UIEventBuilder(), false);
    }

    private void handleHideModifierTooltip(@Nullable String tooltipTarget) {
        if (tooltipTarget != null
                && !tooltipTarget.isBlank()
                && activeModifierTooltipTarget != null
                && !activeModifierTooltipTarget.isBlank()
                && !activeModifierTooltipTarget.equals(tooltipTarget)) {
            return;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.set("#ModifierTooltip.Visible", false);
        activeModifierTooltipTarget = null;
        sendUpdate(commandBuilder, new UIEventBuilder(), false);
    }
    
    // ========== INNER CLASSES ==========
    
    /**
     * Event data for page interactions.
     */
    public static class PageEventData {
        private static final String KEY_ACTION = "Action";
        private static final String KEY_TOOLTIP_TARGET = "TooltipTarget";

        public static final BuilderCodec<PageEventData> CODEC = BuilderCodec.builder(
                        PageEventData.class, PageEventData::new
                )
            .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (e, s) -> e.action = s, e -> e.action)
            .add()
            .append(new KeyedCodec<>(KEY_TOOLTIP_TARGET, Codec.STRING), (e, s) -> e.tooltipTarget = s, e -> e.tooltipTarget)
                .add()
                .build();
        
        private String action;
        private String tooltipTarget;
        
        public PageEventData() {}
        
        public String getAction() {
            return action;
        }

        @Nullable
        public String getTooltipTarget() {
            return tooltipTarget;
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