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
import reign.software.hyforged.stats.DisplayFormat;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;
import reign.software.hyforged.passive.ui.PassiveTreePage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
     * Uses the fixed stat slots defined in CharacterStatsPage.ui:
     * - #CoreStats: #Stat0-4
     * - #OffensiveStats: #Stat5-9
     * - #DefensiveStats: #Stat10-14
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
        Map<String, List<StatEntry>> categorizedStats = new LinkedHashMap<>();
        categorizedStats.put("Core", new ArrayList<>());
        categorizedStats.put("Offensive", new ArrayList<>());
        categorizedStats.put("Defensive", new ArrayList<>());
        
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
            
            // Add to appropriate category
            if (categorizedStats.containsKey(category)) {
                categorizedStats.get(category).add(entry);
            } else {
                // Default to Core for unknown categories
                categorizedStats.get("Core").add(entry);
            }
        }
        
        // Build UI for Core stats (slots 0-4)
        int statIndex = 0;
        List<StatEntry> coreStats = categorizedStats.get("Core");
        for (int i = 0; i < 5 && i < coreStats.size(); i++) {
            StatEntry stat = coreStats.get(i);
            String text = formatStatLine(stat);
            commandBuilder.set("#Stat" + statIndex + ".Text", text);
            statIndex++;
        }
        // Clear remaining core stat slots
        for (int i = coreStats.size(); i < 5; i++) {
            commandBuilder.set("#Stat" + statIndex + ".Text", "");
            statIndex++;
        }
        
        // Build UI for Offensive stats (slots 5-9)
        List<StatEntry> offensiveStats = categorizedStats.get("Offensive");
        for (int i = 0; i < 5 && i < offensiveStats.size(); i++) {
            StatEntry stat = offensiveStats.get(i);
            String text = formatStatLine(stat);
            commandBuilder.set("#Stat" + statIndex + ".Text", text);
            statIndex++;
        }
        // Clear remaining offensive stat slots
        for (int i = offensiveStats.size(); i < 5; i++) {
            commandBuilder.set("#Stat" + statIndex + ".Text", "");
            statIndex++;
        }
        
        // Build UI for Defensive stats (slots 10-14)
        List<StatEntry> defensiveStats = categorizedStats.get("Defensive");
        for (int i = 0; i < 5 && i < defensiveStats.size(); i++) {
            StatEntry stat = defensiveStats.get(i);
            String text = formatStatLine(stat);
            commandBuilder.set("#Stat" + statIndex + ".Text", text);
            statIndex++;
        }
        // Clear remaining defensive stat slots
        for (int i = defensiveStats.size(); i < 5; i++) {
            commandBuilder.set("#Stat" + statIndex + ".Text", "");
            statIndex++;
        }
    }
    
    /**
     * Format a stat entry as a display line.
     */
    private String formatStatLine(StatEntry stat) {
        String name = stat.definition.displayName();
        String value = formatValue(stat.computedValue, stat.definition.displayFormat());
        String modifier = "";
        if (stat.modifierTotal != 0) {
            modifier = " (" + formatModifierValue(stat.modifierTotal, stat.definition.displayFormat()) + ")";
        }
        return name + ": " + value + modifier;
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
        
        // Build armor slot summaries (use fixed selectors #Armor0-3)
        ItemContainer armorContainer = inventory.getArmor();
        if (armorContainer != null) {
            int slotCount = Math.min((int) armorContainer.getCapacity(), 4);
            for (int i = 0; i < slotCount; i++) {
                ItemStack itemStack = armorContainer.getItemStack((short) i);
                String slotText = buildSlotText("Armor", i, itemStack);
                commandBuilder.set("#Armor" + i + ".Text", slotText);
            }
            // Clear remaining slots
            for (int i = slotCount; i < 4; i++) {
                commandBuilder.set("#Armor" + i + ".Text", "");
            }
        }
        
        // Build hand slot summaries (use fixed selectors #Hand0-1)
        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar != null) {
            // Only show first 2 hotbar slots as "hands"
            for (int i = 0; i < 2; i++) {
                ItemStack itemStack = i < hotbar.getCapacity() ? hotbar.getItemStack((short) i) : null;
                String slotText = buildSlotText("Hand", i, itemStack);
                commandBuilder.set("#Hand" + i + ".Text", slotText);
            }
        }
    }
    
    /**
     * Build text for an equipment slot.
     */
    private String buildSlotText(String slotType, int index, ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return slotType + " " + (index + 1) + ": Empty";
        }
        String itemName = itemStack.getItem() != null ? itemStack.getItem().getId() : "Unknown";
        // Simplify the ID for display
        if (itemName.contains(":")) {
            itemName = itemName.substring(itemName.lastIndexOf(':') + 1);
        }
        return slotType + " " + (index + 1) + ": " + itemName;
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
     * Determine category for a stat based on its definition.
     * Returns "Core", "Offensive", or "Defensive" to match UI groups.
     */
    private String getCategoryForStat(StatDefinition statDef) {
        return statDef.category().toLowerCase();
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
            // Open the passive tree page using native UI
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