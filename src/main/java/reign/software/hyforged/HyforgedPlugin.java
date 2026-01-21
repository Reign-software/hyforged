package reign.software.hyforged;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.affix.asset.AffixAssetLoader;
import reign.software.hyforged.affix.system.EquipmentAffixListener;
import reign.software.hyforged.affix.system.LootAffixSystem;
import reign.software.hyforged.affix.ui.CharacterStatsPage;
import reign.software.hyforged.progression.asset.XPCurveAssetLoader;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.progression.persistence.ProgressionCodec;
import reign.software.hyforged.progression.system.ActiveClassResolutionSystem;
import reign.software.hyforged.progression.xp.DiscoveryXPSystem;
import reign.software.hyforged.progression.xp.XPAwardOnKillSystem;
import reign.software.hyforged.progression.xp.XPAwardSystem;
import reign.software.hyforged.progression.xp.XPConfigAssetLoader;
import reign.software.hyforged.progression.xp.XPNotificationAggregator;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.asset.ClassAssetLoader;
import reign.software.hyforged.stats.asset.StatAssetLoader;
import reign.software.hyforged.stats.bridge.HyforgedDamageReductionSystem;
import reign.software.hyforged.stats.bridge.HyforgedKnockbackReductionSystem;
import reign.software.hyforged.stats.bridge.HytaleSystemReplacer;
import reign.software.hyforged.stats.command.HyforgedCommand;
import reign.software.hyforged.stats.component.EffectBridgeComponent;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.damage.DamageTypeAssetLoader;
import reign.software.hyforged.stats.modifier.HyforgedModifier;
import reign.software.hyforged.stats.npc.NPCStatInitSystem;
import reign.software.hyforged.stats.npc.NPCStatTemplateLoader;
import reign.software.hyforged.stats.persistence.HyforgedStatCodec;
import reign.software.hyforged.stats.resource.RageDecayConfigAssetLoader;
import reign.software.hyforged.stats.resource.RageDecaySystem;
import reign.software.hyforged.stats.hud.ResourceStatsHudSystem;
import reign.software.hyforged.stats.system.ClassLevelModifierSystem;
import reign.software.hyforged.stats.system.HyforgedBridgeSystem;
import reign.software.hyforged.stats.system.HyforgedEffectBridgeSystem;
import reign.software.hyforged.stats.system.HyforgedStatComputeSystem;
import reign.software.hyforged.stats.system.HyforgedStatInitSystem;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Main plugin class for Hyforged.
 * <p>
 * Hyforged extends Hytale with an ARPG-style stats system including:
 * - Ability scores (primary stats)
 * - Derived stats (secondary stats)
 * - ARPG modifier stacking (Flat → Increased → More → Caps)
 * - Rating-to-effectiveness conversions
 * - Bridge to Hytale's native stat system
 * - Character and class progression (XP and levels)
 * </p>
 */
public class HyforgedPlugin extends JavaPlugin {

    private static HyforgedPlugin instance;
    
    // ECS Component Types
    private ComponentType<EntityStore, HyforgedStatComponent> hyforgedStatComponentType;
    private ComponentType<EntityStore, ProgressionComponent> progressionComponentType;
    private ComponentType<EntityStore, EffectBridgeComponent> effectBridgeComponentType;
    
    // ECS Resource Types
    private ResourceType<EntityStore, XPNotificationAggregator.AggregationResource> xpNotificationResourceType;

    public HyforgedPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("Initializing Hyforged Stats System...");
        
        // Register Hyforged modifier type with Hytale's codec system
        registerModifierTypes();
        
        // Register objective completion types
        registerObjectiveCompletionTypes();
        
        // Initialize stat and tag definitions
        initializeStatDefinitions();
        
        // Register ECS components
        registerComponents();
        
        // Register ECS systems
        registerSystems();
        
        // Unregister conflicting Hytale systems (ADR-0006)
        // This must be done after Hytale's modules have registered their systems
        HytaleSystemReplacer.unregisterConflictingSystems(this);
        
        // Register commands
        registerCommands();
        
        // Register custom UI pages (for interaction-based access)
        registerCustomUIPages();
        
        getLogger().at(Level.INFO).log("Hyforged Stats System initialized with %d stats and %d tags",
            StatDefinitionRegistry.get().getStatCount(),
            StatDefinitionRegistry.get().getAllTags().size());
    }
    
    /**
     * Register Hyforged modifier types with Hytale's modifier codec.
     * This allows items to use our ARPG-style modifiers in their JSON.
     */
    private void registerModifierTypes() {
        // Register "Hyforged" modifier type for ARPG stacking
        Modifier.CODEC.register("Hyforged", HyforgedModifier.class, HyforgedModifier.CODEC);
        
        getLogger().at(Level.FINE).log("Registered HyforgedModifier with Hytale's Modifier.CODEC");
    }
    
    /**
     * Register custom objective completion types with Hytale's objective system.
     * This allows objectives to grant XP on completion.
     */
    private void registerObjectiveCompletionTypes() {
        com.hypixel.hytale.builtin.adventure.objectives.ObjectivePlugin objectivePlugin =
                com.hypixel.hytale.builtin.adventure.objectives.ObjectivePlugin.get();
        
        objectivePlugin.registerCompletion(
                "hyforged:xp_award",
                reign.software.hyforged.progression.xp.objective.XPAwardCompletionAsset.class,
                reign.software.hyforged.progression.xp.objective.XPAwardCompletionAsset.CODEC,
                reign.software.hyforged.progression.xp.objective.XPAwardCompletion::new
        );
        
        getLogger().at(Level.FINE).log("Registered XPAwardCompletion with ObjectivePlugin");
    }
    
    /**
     * Initialize stat and tag definitions from data-driven assets.
     * <p>
     * Stats and tags are defined in JSON files in Server/Hyforged/Stats/ and Server/Hyforged/Tags/.
     * This approach follows Hytale's data-driven pattern rather than hardcoding definitions in Java.
     */
    private void initializeStatDefinitions() {
        // Initialize asset loader for stat and tag definitions
        // Assets are loaded from Server/Hyforged/Stats/ and Server/Hyforged/Tags/
        // The StatAssetLoader handles registration with StatDefinitionRegistry
        StatAssetLoader.initialize(this);
        
        // Initialize asset loader for class definitions
        // Classes define ability score distributions for players
        ClassAssetLoader.initialize(this);
        
        // Initialize asset loader for NPC stat templates
        // Templates define stat scaling for NPCs
        NPCStatTemplateLoader.initialize(this);
        
        // Initialize asset loader for damage type extensions
        // Extensions define which resistance stats apply to each damage type (ECS pattern)
        DamageTypeAssetLoader.initialize(this);
        
        // Initialize asset loader for XP curves (progression system)
        // Curves define experience required for character and class level progression
        XPCurveAssetLoader.initialize(this);
        
        // Initialize asset loader for XP configuration (progression system)
        // Config defines XP amounts for various activities
        XPConfigAssetLoader.initialize(this);

        // Initialize asset loader for rage decay configuration
        RageDecayConfigAssetLoader.initialize(this);
        
        // Initialize asset loader for affix definitions (affix system)
        // Includes affix types, quality rules, affix definitions, and affix pools
        AffixAssetLoader.initialize(this);
        
        getLogger().at(Level.FINE).log("Stat and tag asset loading initialized, awaiting asset load...");
    }
    
    /**
     * Register ECS components with Hytale's entity store.
     */
    private void registerComponents() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();
        
        // Register HyforgedStatComponent with persistence codec
        hyforgedStatComponentType = entityStoreRegistry.registerComponent(
            HyforgedStatComponent.class,
            HyforgedStatCodec.COMPONENT_ID,
            HyforgedStatCodec.CODEC
        );
        
        getLogger().at(Level.FINE).log("Registered HyforgedStatComponent with persistence codec");
        
        // Register ProgressionComponent with persistence codec
        progressionComponentType = entityStoreRegistry.registerComponent(
            ProgressionComponent.class,
            ProgressionCodec.COMPONENT_ID,
            ProgressionCodec.CODEC
        );
        
        getLogger().at(Level.FINE).log("Registered ProgressionComponent with persistence codec");
        
        // Register EffectBridgeComponent (no persistence - runtime tracking only)
        effectBridgeComponentType = entityStoreRegistry.registerComponent(
            EffectBridgeComponent.class,
            EffectBridgeComponent::new
        );
        
        getLogger().at(Level.FINE).log("Registered EffectBridgeComponent");
        
        // Register XP notification aggregation resource
        xpNotificationResourceType = entityStoreRegistry.registerResource(
            XPNotificationAggregator.AggregationResource.class,
            XPNotificationAggregator.AggregationResource::new
        );
        
        getLogger().at(Level.FINE).log("Registered XPNotificationAggregator resource");
    }
    
    /**
     * Register ECS systems with Hytale's entity store.
     */
    private void registerSystems() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();
        
        // Register HyforgedStatInitSystem (handles player entity lifecycle)
        entityStoreRegistry.registerSystem(new HyforgedStatInitSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedStatInitSystem");
        
        // Register NPCStatInitSystem (handles NPC entity stat initialization)
        entityStoreRegistry.registerSystem(new NPCStatInitSystem());
        getLogger().at(Level.FINE).log("Registered NPCStatInitSystem");
        
        // Register HyforgedEffectBridgeSystem (bridges Hytale effects to Hyforged stats)
        entityStoreRegistry.registerSystem(new HyforgedEffectBridgeSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedEffectBridgeSystem");
        
        // Register HyforgedStatComputeSystem (recomputes dirty stats)
        entityStoreRegistry.registerSystem(new HyforgedStatComputeSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedStatComputeSystem");
        
        // Register HyforgedBridgeSystem (bridges to Hytale's EntityStatMap)
        entityStoreRegistry.registerSystem(new HyforgedBridgeSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedBridgeSystem");

        // Register RageDecaySystem (out-of-combat rage decay)
        entityStoreRegistry.registerSystem(new RageDecaySystem());
        getLogger().at(Level.FINE).log("Registered RageDecaySystem");

        // Register ResourceStatsHudSystem (custom HUD for resource bars)
        entityStoreRegistry.registerSystem(new ResourceStatsHudSystem());
        getLogger().at(Level.FINE).log("Registered ResourceStatsHudSystem");
        
        // Register Hyforged damage reduction system (replaces Hytale's ArmorDamageReduction)
        entityStoreRegistry.registerSystem(new HyforgedDamageReductionSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedDamageReductionSystem");
        
        // Register Hyforged knockback reduction system (replaces Hytale's ArmorKnockbackReduction)
        entityStoreRegistry.registerSystem(new HyforgedKnockbackReductionSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedKnockbackReductionSystem");
        
        // Register ActiveClassResolutionSystem (resolves active class from weapon tags)
        entityStoreRegistry.registerSystem(new ActiveClassResolutionSystem());
        getLogger().at(Level.FINE).log("Registered ActiveClassResolutionSystem");
        
        // Register XPAwardSystem (processes XP award events)
        entityStoreRegistry.registerSystem(new XPAwardSystem());
        getLogger().at(Level.FINE).log("Registered XPAwardSystem");
        
        // Register XPAwardOnKillSystem (awards XP on entity kills)
        entityStoreRegistry.registerSystem(new XPAwardOnKillSystem());
        getLogger().at(Level.FINE).log("Registered XPAwardOnKillSystem");
        
        // Register DiscoveryXPSystem (awards XP on zone discovery)
        entityStoreRegistry.registerSystem(new DiscoveryXPSystem());
        getLogger().at(Level.FINE).log("Registered DiscoveryXPSystem");
        
        // Register XPNotificationAggregator (aggregates XP notifications per tick)
        entityStoreRegistry.registerSystem(new XPNotificationAggregator(xpNotificationResourceType));
        getLogger().at(Level.FINE).log("Registered XPNotificationAggregator");
        
        // Initialize ClassLevelModifierSystem (event-driven, applies class level bonuses)
        new ClassLevelModifierSystem();
        getLogger().at(Level.FINE).log("Initialized ClassLevelModifierSystem");
        
        // Register LootAffixSystem (rolls affixes on item drops)
        entityStoreRegistry.registerSystem(new LootAffixSystem());
        getLogger().at(Level.FINE).log("Registered LootAffixSystem");
        
        // Register EquipmentAffixListener (applies affix modifiers on equipment change)
        EquipmentAffixListener equipmentAffixListener = new EquipmentAffixListener();
        equipmentAffixListener.register();
        getLogger().at(Level.FINE).log("Registered EquipmentAffixListener");
    }
    
    /**
     * Register admin commands for the Hyforged plugin.
     */
    private void registerCommands() {
        this.getCommandRegistry().registerCommand(new HyforgedCommand());
        getLogger().at(Level.FINE).log("Registered Hyforged commands");
    }
    
    /**
     * Register custom UI pages for interaction-based access.
     * <p>
     * This allows pages to be opened via RootInteraction JSON definitions,
     * enabling keybind/menu access without using commands.
     */
    private void registerCustomUIPages() {
        // Register CharacterStatsPage for interaction-based opening
        // This can be referenced in RootInteraction JSON as:
        // { "Type": "OpenCustomUI", "Page": { "Type": "CharacterStatsPage" } }
        OpenCustomUIInteraction.registerSimple(
            this,
            CharacterStatsPage.class,
            "CharacterStatsPage",
            CharacterStatsPage::new
        );
        
        getLogger().at(Level.FINE).log("Registered CharacterStatsPage custom UI interaction");
    }

    /**
     * Get the plugin instance.
     */
    @Nonnull
    public static HyforgedPlugin getInstance() {
        if (instance == null) {
            throw new IllegalStateException("HyforgedPlugin not initialized");
        }
        return instance;
    }
    
    /**
     * Get the HyforgedStatComponent type for ECS operations.
     */
    @Nonnull
    public ComponentType<EntityStore, HyforgedStatComponent> getHyforgedStatComponentType() {
        if (hyforgedStatComponentType == null) {
            throw new IllegalStateException("HyforgedPlugin not initialized");
        }
        return hyforgedStatComponentType;
    }
    
    /**
     * Get the ProgressionComponent type for ECS operations.
     */
    @Nonnull
    public ComponentType<EntityStore, ProgressionComponent> getProgressionComponentType() {
        if (progressionComponentType == null) {
            throw new IllegalStateException("HyforgedPlugin not initialized");
        }
        return progressionComponentType;
    }
    
    /**
     * Get the EffectBridgeComponent type for ECS operations.
     */
    @Nonnull
    public ComponentType<EntityStore, EffectBridgeComponent> getEffectBridgeComponentType() {
        if (effectBridgeComponentType == null) {
            throw new IllegalStateException("HyforgedPlugin not initialized");
        }
        return effectBridgeComponentType;
    }
}
