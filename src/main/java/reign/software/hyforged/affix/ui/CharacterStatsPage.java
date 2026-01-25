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
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.affix.service.HyforgedItemDataService;
import reign.software.hyforged.stats.DisplayFormat;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;
import reign.software.hyforged.passive.ui.PassiveTreePage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Character Stats Screen UI page.
 * <p>
 * Displays:
 * <ul>
 *   <li>All character stats organized by category</li>
 *   <li>Base value, modifiers breakdown, and effective value per stat</li>
 *   <li>Equipment slot display with affix contribution summaries</li>
 * </ul>
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
    
    /** Category for primary ability scores */
    public static final String CATEGORY_ABILITY_SCORES = "Ability Scores";
    
    /** Category for combat stats */
    public static final String CATEGORY_COMBAT = "Combat";
    
    /** Category for defensive stats */
    public static final String CATEGORY_DEFENSE = "Defense";
    
    /** Category for resource stats */
    public static final String CATEGORY_RESOURCES = "Resources";
    
    /** Category for miscellaneous stats */
    public static final String CATEGORY_MISC = "Misc";
    
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
        // Append the main UI layout
        commandBuilder.append(PAGE_UI_FILE);
        
        // Get the stat component for this entity
        HyforgedStatComponent statComponent = getStatComponent(ref, store);
        
        // Build stat categories
        buildStatCategories(commandBuilder, statComponent, store, ref);
        
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
     * Build the stat categories section of the UI.
     */
    private void buildStatCategories(
            UICommandBuilder commandBuilder,
            HyforgedStatComponent statComponent,
            Store<EntityStore> store,
            Ref<EntityStore> ref
    ) {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        EntityStatMap statMap = StatAccessor.getStatMap(store, ref);
        
        // Group stats by category
        Map<String, List<StatEntry>> categorizedStats = new HashMap<>();
        
        for (int i = 0; i < registry.getStatCount(); i++) {
            StatDefinition statDef = registry.getStat(i);
            if (statDef == null) {
                continue;
            }
            
            // Determine category from stat ID prefix
            String category = getCategoryForStat(statDef);
            
            // Get values - use StatAccessor for unified access pattern
            int baseValue = statComponent != null ? statComponent.getBaseValue(i) : statDef.defaultValue();
            int computedValue = StatAccessor.getStatValueInt(store, ref, i);
            int modifierTotal = computedValue - baseValue;
            
            // Get modifier breakdown
            List<ModifierBreakdown> breakdown = new ArrayList<>();
            if (statMap != null) {
                breakdown = getModifierBreakdown(statMap, i);
            }
            
            StatEntry entry = new StatEntry(statDef, i, baseValue, computedValue, modifierTotal, breakdown);
            
            categorizedStats.computeIfAbsent(category, k -> new ArrayList<>()).add(entry);
        }
        
        // Build UI for each category
        int categoryIndex = 0;
        for (Map.Entry<String, List<StatEntry>> categoryEntry : categorizedStats.entrySet()) {
            String category = categoryEntry.getKey();
            List<StatEntry> stats = categoryEntry.getValue();
            
            String categorySelector = "#StatCategories[" + categoryIndex + "]";
            
            // Set category header
            commandBuilder.set(categorySelector + " #CategoryName.Text", category);
            
            // Build stat rows
            int statRowIndex = 0;
            for (StatEntry stat : stats) {
                String rowSelector = categorySelector + " #StatRows[" + statRowIndex + "]";
                
                commandBuilder.set(rowSelector + " #StatName.Text", stat.definition.displayName());
                commandBuilder.set(rowSelector + " #BaseValue.Text", formatValue(stat.baseValue, stat.definition.displayFormat()));
                commandBuilder.set(rowSelector + " #ModifierValue.Text", formatModifierValue(stat.modifierTotal, stat.definition.displayFormat()));
                commandBuilder.set(rowSelector + " #EffectiveValue.Text", formatValue(stat.computedValue, stat.definition.displayFormat()));
                
                // Set breakdown tooltip (simplified for now)
                if (!stat.breakdown.isEmpty()) {
                    String tooltip = buildBreakdownTooltip(stat);
                    commandBuilder.set(rowSelector + " #BreakdownTooltip.Text", tooltip);
                }
                
                statRowIndex++;
            }
            
            categoryIndex++;
        }
    }
    
    /**
     * Build the equipment affix summary section.
     */
    private void buildEquipmentSummary(UICommandBuilder commandBuilder, Ref<EntityStore> ref, Store<EntityStore> store) {
        // Get player component for inventory access (Player extends LivingEntity)
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        var inventory = player.getInventory();
        if (inventory == null) {
            return;
        }
        
        // Build armor slot summaries
        ItemContainer armorContainer = inventory.getArmor();
        if (armorContainer != null) {
            buildEquipmentSlotSummary(commandBuilder, armorContainer, "Armor", "#ArmorSlots");
        }
        
        // Build hotbar summary (first slot is typically main hand)
        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar != null) {
            buildEquipmentSlotSummary(commandBuilder, hotbar, "Hand", "#HandSlots");
        }
    }
    
    /**
     * Build summary for a set of equipment slots.
     */
    private void buildEquipmentSlotSummary(
            UICommandBuilder commandBuilder,
            ItemContainer container,
            String slotType,
            String selector
    ) {
        short slotCount = container.getCapacity();
        
        for (short i = 0; i < slotCount; i++) {
            ItemStack itemStack = container.getItemStack(i);
            String slotSelector = selector + "[" + i + "]";
            
            if (itemStack == null || itemStack.isEmpty()) {
                commandBuilder.set(slotSelector + " #SlotLabel.Text", slotType + " " + (i + 1) + ": Empty");
                commandBuilder.set(slotSelector + " #AffixSummary.Text", "");
                continue;
            }
            
            // Get item name
            String itemName = itemStack.getItem() != null ? itemStack.getItem().getId() : "Unknown";
            commandBuilder.set(slotSelector + " #SlotLabel.Text", slotType + " " + (i + 1) + ": " + itemName);
            
            // Get affixes
            HyforgedItemData itemData = HyforgedItemDataService.read(itemStack);
            if (itemData == null || itemData.affixes().isEmpty()) {
                commandBuilder.set(slotSelector + " #AffixSummary.Text", "No affixes");
                continue;
            }
            
            // Build affix summary
            StringBuilder summary = new StringBuilder();
            for (RolledAffix affix : itemData.affixes()) {
                if (summary.length() > 0) {
                    summary.append(", ");
                }
                
                var affixDef = AffixDefinitionRegistry.get().get(affix.affixId());
                String affixName = affixDef != null ? affixDef.displayName() : affix.affixId();
                summary.append("[T").append(affix.tier()).append("] ").append(affixName);
            }
            
            commandBuilder.set(slotSelector + " #AffixSummary.Text", summary.toString());
        }
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
     * Determine category for a stat based on its ID.
     */
    private String getCategoryForStat(StatDefinition statDef) {
        String id = statDef.id().toString();
        
        if (id.contains("strength") || id.contains("dexterity") || id.contains("intelligence") ||
            id.contains("vitality") || id.contains("wisdom") || id.contains("charisma")) {
            return CATEGORY_ABILITY_SCORES;
        }
        
        if (id.contains("attack") || id.contains("damage") || id.contains("critical") ||
            id.contains("accuracy") || id.contains("penetration")) {
            return CATEGORY_COMBAT;
        }
        
        if (id.contains("armor") || id.contains("defense") || id.contains("resistance") ||
            id.contains("block") || id.contains("evasion")) {
            return CATEGORY_DEFENSE;
        }
        
        if (id.contains("health") || id.contains("mana") || id.contains("stamina") ||
            id.contains("regen") || id.contains("max")) {
            return CATEGORY_RESOURCES;
        }
        
        return CATEGORY_MISC;
    }
    
    /**
     * Format a stat value according to its display format.
     */
    private String formatValue(int value, DisplayFormat format) {
        return switch (format) {
            case INTEGER -> String.valueOf(value);
            case PERCENT_BPS -> String.format("%.1f%%", value / 100.0);
            case RATING -> value + " rating";
            case FLAT_BONUS -> (value >= 0 ? "+" : "") + value;
            case MULTIPLIER -> String.format("%.2fx", value / 10000.0);
        };
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
     * Build a tooltip string for modifier breakdown.
     */
    private String buildBreakdownTooltip(StatEntry stat) {
        if (stat.breakdown.isEmpty()) {
            return "No modifiers";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Modifiers:\n");
        
        for (ModifierBreakdown mod : stat.breakdown) {
            sb.append("  ").append(mod.sourceId).append(": ");
            sb.append(mod.value >= 0 ? "+" : "").append(mod.value);
            sb.append(" (").append(mod.sourceType.name().toLowerCase()).append(")\n");
        }
        
        return sb.toString();
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
            // Rebuild the page with updated data
            rebuild();
        } else if ("openPassiveTree".equals(action)) {
            // Open the passive tree page
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent != null) {
                PassiveTreePage passiveTreePage = new PassiveTreePage(this.playerRef);
                playerComponent.getPageManager().openCustomPage(ref, store, passiveTreePage);
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
