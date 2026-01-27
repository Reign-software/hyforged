package reign.software.hyforged;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import reign.software.hyforged.passive.interaction.PointBookInteraction;
import reign.software.hyforged.passive.ui.PassiveTreePage;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.affix.asset.AffixAssetLoader;
import reign.software.hyforged.affix.component.HyforgedActiveEffectsComponent;
import reign.software.hyforged.affix.resource.AffixTierColorConfigAssetLoader;
import reign.software.hyforged.affix.system.EffectAffixCastListener;
import reign.software.hyforged.affix.system.EffectAffixDamageTriggerSystem;
import reign.software.hyforged.affix.system.EffectAffixIntervalSystem;
import reign.software.hyforged.affix.system.EffectAffixOnKillSystem;
import reign.software.hyforged.affix.system.EquipmentAffixListener;
import reign.software.hyforged.affix.system.LootAffixSystem;
import reign.software.hyforged.affix.ui.CharacterStatsPage;
import reign.software.hyforged.hub.ui.CharacterHubPage;
import reign.software.hyforged.combat.HyforgedAutoBlockSystem;
import reign.software.hyforged.combat.HyforgedCriticalHitSystem;
import reign.software.hyforged.combat.ailment.AilmentAccumulatorComponent;
import reign.software.hyforged.combat.ailment.AilmentLoader;
import reign.software.hyforged.combat.ailment.HyforgedAilmentSystem;
import reign.software.hyforged.combat.log.HyforgedCombatLogSystem;
import reign.software.hyforged.combat.hud.CombatLogHudSystem;
import reign.software.hyforged.combat.HyforgedHitResolutionSystem;
import reign.software.hyforged.combat.scaling.HyforgedMonsterScalingSystem;
import reign.software.hyforged.combat.scaling.MonsterLevelComponent;
import reign.software.hyforged.combat.scaling.ScalingAssetLoader;
import reign.software.hyforged.combat.ui.HyforgedCombatTextSystem;
import reign.software.hyforged.progression.asset.XPCurveAssetLoader;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.progression.persistence.ProgressionCodec;
import reign.software.hyforged.progression.system.ActiveClassResolutionSystem;
import reign.software.hyforged.progression.xp.DiscoveryXPSystem;
import reign.software.hyforged.progression.xp.XPAwardOnKillSystem;
import reign.software.hyforged.progression.xp.XPAwardSystem;
import reign.software.hyforged.progression.xp.XPConfigAssetLoader;
import reign.software.hyforged.progression.xp.XPNotificationAggregator;
import reign.software.hyforged.quality.asset.QualityAssetLoader;
import reign.software.hyforged.quality.component.HyforgedNPCQualityComponent;
import reign.software.hyforged.quality.system.LootQualitySystem;
import reign.software.hyforged.quality.system.NPCQualityAffixStatSystem;
import reign.software.hyforged.quality.system.NPCQualitySystem;
import reign.software.hyforged.concentration.ConcentrationPriorityCodec;
import reign.software.hyforged.concentration.ConcentrationPriorityComponent;
import reign.software.hyforged.concentration.ConcentrationService;
import reign.software.hyforged.concentration.HyforgedConcentrationDisruptionSystem;
import reign.software.hyforged.concentration.HyforgedConcentrationRegenerationSystem;
import reign.software.hyforged.concentration.ui.ConcentrationPriorityPage;
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
import reign.software.hyforged.stats.value.HyforgedStatValueInstaller;
import reign.software.hyforged.effect.HyforgedEffectAssetLoader;
import reign.software.hyforged.passive.asset.PassiveTreeAssetLoader;
import reign.software.hyforged.passive.system.ClassTreeStartingNodeSystem;
import reign.software.hyforged.passive.system.PassiveTreeMigrationSystem;
import reign.software.hyforged.hub.system.WelcomeMessageSystem;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.component.PlayerSpellsComponent;
import reign.software.hyforged.passive.component.PlayerUnlocksComponent;
import reign.software.hyforged.passive.effect.MasteryChoiceEffectHandler;
import reign.software.hyforged.passive.effect.PassiveEffectRegistry;
import reign.software.hyforged.passive.effect.SpellGrantEffectHandler;
import reign.software.hyforged.passive.effect.StatModifierEffectHandler;
import reign.software.hyforged.passive.effect.UnlockFlagEffectHandler;
import reign.software.hyforged.passive.service.PassiveTreeService;
import reign.software.hyforged.currency.config.CurrencyConfigAssetLoader;
import reign.software.hyforged.currency.service.CurrencyService;
import reign.software.hyforged.currency.component.TradebarVaultComponent;
import reign.software.hyforged.currency.hud.CurrencyHudSystem;
import reign.software.hyforged.currency.system.VaultBreakProtectionSystem;
import reign.software.hyforged.currency.ui.MarketStallPage;
import reign.software.hyforged.currency.ui.VaultPage;

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
    private ComponentType<EntityStore, AilmentAccumulatorComponent> ailmentAccumulatorComponentType;
    private ComponentType<EntityStore, MonsterLevelComponent> monsterLevelComponentType;
    private ComponentType<EntityStore, HyforgedNPCQualityComponent> npcQualityComponentType;
    private ComponentType<EntityStore, HyforgedActiveEffectsComponent> activeEffectsComponentType;
    private ComponentType<EntityStore, ConcentrationPriorityComponent> concentrationPriorityComponentType;
    private ComponentType<EntityStore, PassiveTreeComponent> passiveTreeComponentType;
    private ComponentType<EntityStore, PlayerUnlocksComponent> playerUnlocksComponentType;
    private ComponentType<EntityStore, PlayerSpellsComponent> playerSpellsComponentType;
    
    // ChunkStore Component Types (block data)
    private ComponentType<ChunkStore, TradebarVaultComponent> tradebarVaultComponentType;
    
    // ECS Resource Types
    private ResourceType<EntityStore, XPNotificationAggregator.AggregationResource> xpNotificationResourceType;

    public HyforgedPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("Initializing Hyforged Stats System...");

        // Preload core registry/model classes to avoid late classloading failures
        preloadRuntimeTypes();
        
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
        
        // Note: Actual stat count is logged by StatAssetLoader when assets are loaded asynchronously
        getLogger().at(Level.INFO).log("Hyforged setup complete - asset loading will continue asynchronously");
    }

    /**
     * Preload core classes used by asset loaders and registries.
     * <p>
     * This forces class resolution early so runtime classloading issues are surfaced
     * immediately (and avoids late NoClassDefFoundError during asset events).
     */
    private void preloadRuntimeTypes() {
        ClassLoader classLoader = getClass().getClassLoader();
        String[] classNames = new String[] {
                "reign.software.hyforged.stats.npc.NPCStatTemplateRegistry",
                "reign.software.hyforged.stats.damage.DamageTypeExtensionRegistry",
                "reign.software.hyforged.quality.model.QualityModifierOverrides",
                "reign.software.hyforged.quality.model.QualityWeightProfile",
                "reign.software.hyforged.quality.registry.NPCQualityRegistry",
                "reign.software.hyforged.stats.asset.ClassDefinitionRegistry",
                "reign.software.hyforged.stats.CategoryDefinition$Builder",
                "reign.software.hyforged.stats.StatDefinition$Builder",
                "reign.software.hyforged.stats.resource.RageDecayConfig",
                "reign.software.hyforged.progression.xp.XPConfig",
                "reign.software.hyforged.progression.asset.XPCurveRegistry"
        };

        boolean failed = false;
        for (String className : classNames) {
            try {
                Class.forName(className, true, classLoader);
            } catch (ClassNotFoundException e) {
                failed = true;
                getLogger().at(Level.SEVERE).log("Missing runtime class: " + className, e);
            }
        }

        if (failed) {
            throw new IllegalStateException("Hyforged runtime class preload failed. See logs for missing classes.");
        }
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

        // Initialize asset loader for Hyforged effects (single-file buff/debuff definitions)
        HyforgedEffectAssetLoader.initialize(this);
        
        // Initialize asset loader for XP curves (progression system)
        // Curves define experience required for character and class level progression
        XPCurveAssetLoader.initialize(this);
        
        // Initialize asset loader for XP configuration (progression system)
        // Config defines XP amounts for various activities
        XPConfigAssetLoader.initialize(this);

        // Initialize asset loader for rage decay configuration
        RageDecayConfigAssetLoader.initialize(this);

        // Initialize asset loader for affix tier colors (tooltip formatting)
        AffixTierColorConfigAssetLoader.initialize(this);
        
        // Initialize asset loader for affix definitions (affix system)
        // Includes affix types, quality rules, affix definitions, and affix pools
        AffixAssetLoader.initialize(this);

        // Initialize asset loader for quality rolling configurations
        QualityAssetLoader.initialize(this);
        
        // Initialize asset loader for ailment definitions (combat system)
        // Defines threshold-based status effects triggered by elemental damage
        AilmentLoader.initialize(this);
        
        // Initialize asset loader for scaling configurations (combat system)
        // Includes world scaling (level calculation) and monster scaling (per-NPC stat scaling)
        ScalingAssetLoader.initialize(this);
        
        // Initialize asset loader for passive tree definitions (passive skill tree system)
        // Includes general tree, class trees, and refund configuration
        PassiveTreeAssetLoader.initialize(this);
        
        // Initialize asset loader for currency configuration (currency system)
        // Includes sell value config and vault upgrade tiers
        CurrencyConfigAssetLoader.initialize(this);
        
        // Initialize CurrencyService singleton
        CurrencyService.get();
        
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
        
        // Register AilmentAccumulatorComponent (no persistence - runtime tracking only)
        ailmentAccumulatorComponentType = entityStoreRegistry.registerComponent(
            AilmentAccumulatorComponent.class,
            AilmentAccumulatorComponent::new
        );
        
        getLogger().at(Level.FINE).log("Registered AilmentAccumulatorComponent");
        
        // Register MonsterLevelComponent (no persistence - runtime tracking only)
        monsterLevelComponentType = entityStoreRegistry.registerComponent(
            MonsterLevelComponent.class,
            MonsterLevelComponent::new
        );
        
        getLogger().at(Level.FINE).log("Registered MonsterLevelComponent");

        // Register HyforgedNPCQualityComponent (no persistence - runtime tracking only)
        npcQualityComponentType = entityStoreRegistry.registerComponent(
            HyforgedNPCQualityComponent.class,
            HyforgedNPCQualityComponent::new
        );

        getLogger().at(Level.FINE).log("Registered HyforgedNPCQualityComponent");

        // Register HyforgedActiveEffectsComponent (no persistence - runtime tracking only)
        activeEffectsComponentType = entityStoreRegistry.registerComponent(
            HyforgedActiveEffectsComponent.class,
            HyforgedActiveEffectsComponent::new
        );

        getLogger().at(Level.FINE).log("Registered HyforgedActiveEffectsComponent");

        // Register ConcentrationPriorityComponent with persistence codec
        concentrationPriorityComponentType = entityStoreRegistry.registerComponent(
            ConcentrationPriorityComponent.class,
            ConcentrationPriorityCodec.COMPONENT_ID,
            ConcentrationPriorityCodec.CODEC
        );

        getLogger().at(Level.FINE).log("Registered ConcentrationPriorityComponent with persistence codec");

        // Register PassiveTreeComponent (no persistence codec yet - will add in Phase 5)
        passiveTreeComponentType = entityStoreRegistry.registerComponent(
            PassiveTreeComponent.class,
            PassiveTreeComponent::new
        );

        getLogger().at(Level.FINE).log("Registered PassiveTreeComponent");

        // Register PlayerUnlocksComponent (no persistence codec yet)
        playerUnlocksComponentType = entityStoreRegistry.registerComponent(
            PlayerUnlocksComponent.class,
            PlayerUnlocksComponent::new
        );

        getLogger().at(Level.FINE).log("Registered PlayerUnlocksComponent");

        // Register PlayerSpellsComponent (no persistence codec yet)
        playerSpellsComponentType = entityStoreRegistry.registerComponent(
            PlayerSpellsComponent.class,
            PlayerSpellsComponent::new
        );

        getLogger().at(Level.FINE).log("Registered PlayerSpellsComponent");

        // Initialize PassiveTreeService with component types
        PassiveTreeService.get().initialize(
            passiveTreeComponentType,
            playerUnlocksComponentType,
            playerSpellsComponentType,
            progressionComponentType
        );

        getLogger().at(Level.FINE).log("Initialized PassiveTreeService");

        // Register passive effect handlers
        registerPassiveEffectHandlers();
        
        // Register XP notification aggregation resource
        xpNotificationResourceType = entityStoreRegistry.registerResource(
            XPNotificationAggregator.AggregationResource.class,
            XPNotificationAggregator.AggregationResource::new
        );
        
        getLogger().at(Level.FINE).log("Registered XPNotificationAggregator resource");
        
        // Register ChunkStore components (block data)
        registerChunkStoreComponents();
    }
    
    /**
     * Register ChunkStore components for block-based data.
     */
    private void registerChunkStoreComponents() {
        ComponentRegistryProxy<ChunkStore> chunkStoreRegistry = this.getChunkStoreRegistry();
        
        // Register TradebarVaultComponent for vault block storage
        tradebarVaultComponentType = chunkStoreRegistry.registerComponent(
            TradebarVaultComponent.class,
            "TradebarVault",
            TradebarVaultComponent.CODEC
        );
        
        getLogger().at(Level.FINE).log("Registered TradebarVaultComponent with ChunkStore");
    }

    /**
     * Register passive tree effect handlers.
     */
    private void registerPassiveEffectHandlers() {
        PassiveEffectRegistry registry = PassiveEffectRegistry.get();

        // Register stat modifier handler
        registry.register(
            StatModifierEffectHandler.EFFECT_TYPE,
            new StatModifierEffectHandler(hyforgedStatComponentType)
        );

        // Register spell grant handler
        registry.register(
            SpellGrantEffectHandler.EFFECT_TYPE,
            new SpellGrantEffectHandler(playerSpellsComponentType)
        );

        // Register unlock flag handler
        registry.register(
            UnlockFlagEffectHandler.EFFECT_TYPE,
            new UnlockFlagEffectHandler(playerUnlocksComponentType)
        );

        // Register mastery choice handler
        registry.register(
            MasteryChoiceEffectHandler.EFFECT_TYPE,
            new MasteryChoiceEffectHandler(passiveTreeComponentType)
        );

        getLogger().at(Level.FINE).log("Registered " + registry.getHandlerCount() + " passive effect handlers");
    }
    
    /**
     * Register ECS systems with Hytale's entity store.
     */
    private void registerSystems() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();

        // Initialize concentration service singleton
        ConcentrationService.get();
        getLogger().at(Level.FINE).log("Initialized ConcentrationService");
        
        // Register HyforgedStatInitSystem (handles player entity lifecycle)
        entityStoreRegistry.registerSystem(new HyforgedStatInitSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedStatInitSystem");
        
        // Register HyforgedStatValueInstaller (swaps EntityStatValue → HyforgedStatValue for ARPG stacking)
        entityStoreRegistry.registerSystem(new HyforgedStatValueInstaller());
        getLogger().at(Level.FINE).log("Registered HyforgedStatValueInstaller");
        
        // Register NPCStatInitSystem (handles NPC entity stat initialization)
        entityStoreRegistry.registerSystem(new NPCStatInitSystem());
        getLogger().at(Level.FINE).log("Registered NPCStatInitSystem");

        // Register NPCQualitySystem (assigns NPC quality tiers and scaling)
        entityStoreRegistry.registerSystem(new NPCQualitySystem());
        getLogger().at(Level.FINE).log("Registered NPCQualitySystem");

        // Register NPCQualityAffixStatSystem (applies NPC affix stat modifiers)
        entityStoreRegistry.registerSystem(new NPCQualityAffixStatSystem());
        getLogger().at(Level.FINE).log("Registered NPCQualityAffixStatSystem");
        
        // Register HyforgedStatComputeSystem (recomputes dirty stats)
        // NOTE: Must be registered before HyforgedEffectBridgeSystem (which depends on it)
        entityStoreRegistry.registerSystem(new HyforgedStatComputeSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedStatComputeSystem");
        
        // Register HyforgedEffectBridgeSystem (bridges Hytale effects to Hyforged stats)
        entityStoreRegistry.registerSystem(new HyforgedEffectBridgeSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedEffectBridgeSystem");
        
        // Register HyforgedBridgeSystem (bridges to Hytale's EntityStatMap)
        entityStoreRegistry.registerSystem(new HyforgedBridgeSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedBridgeSystem");

        // Register RageDecaySystem (out-of-combat rage decay)
        entityStoreRegistry.registerSystem(new RageDecaySystem());
        getLogger().at(Level.FINE).log("Registered RageDecaySystem");

        // Register concentration regeneration system (always active)
        entityStoreRegistry.registerSystem(new HyforgedConcentrationRegenerationSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedConcentrationRegenerationSystem");

        // Register ResourceStatsHudSystem (custom HUD for resource bars)
        entityStoreRegistry.registerSystem(new ResourceStatsHudSystem());
        getLogger().at(Level.FINE).log("Registered ResourceStatsHudSystem");

        // Register CurrencyHudSystem (custom HUD for Tradebar balance)
        entityStoreRegistry.registerSystem(new CurrencyHudSystem());
        getLogger().at(Level.FINE).log("Registered CurrencyHudSystem");

        // Register VaultBreakProtectionSystem (prevents non-owners from breaking vaults)
        entityStoreRegistry.registerSystem(new VaultBreakProtectionSystem());
        getLogger().at(Level.FINE).log("Registered VaultBreakProtectionSystem");
        
        // Register Hyforged damage reduction system (replaces Hytale's ArmorDamageReduction)
        entityStoreRegistry.registerSystem(new HyforgedDamageReductionSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedDamageReductionSystem");
        
        // Register Hyforged knockback reduction system (replaces Hytale's ArmorKnockbackReduction)
        entityStoreRegistry.registerSystem(new HyforgedKnockbackReductionSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedKnockbackReductionSystem");
        
        // Register combat systems
        registerCombatSystems(entityStoreRegistry);

        // Register interval-based effect affix system
        entityStoreRegistry.registerSystem(new EffectAffixIntervalSystem());
        getLogger().at(Level.FINE).log("Registered EffectAffixIntervalSystem");
        
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

        // Initialize ClassTreeStartingNodeSystem (auto-allocates class tree starting nodes)
        new ClassTreeStartingNodeSystem();
        getLogger().at(Level.FINE).log("Initialized ClassTreeStartingNodeSystem");

        // Initialize PassiveTreeMigrationSystem (runs migrations on player connect)
        new PassiveTreeMigrationSystem();
        getLogger().at(Level.FINE).log("Initialized PassiveTreeMigrationSystem");
        
        // Initialize WelcomeMessageSystem (sends welcome message with commands on player connect)
        new WelcomeMessageSystem();
        getLogger().at(Level.FINE).log("Initialized WelcomeMessageSystem");
        
        // Register LootAffixSystem (rolls affixes on item drops)
        // NOTE: Must be registered before LootQualitySystem (which depends on it)
        entityStoreRegistry.registerSystem(new LootAffixSystem());
        getLogger().at(Level.FINE).log("Registered LootAffixSystem");

        // Register LootQualitySystem (assigns quality tiers to loot)
        entityStoreRegistry.registerSystem(new LootQualitySystem());
        getLogger().at(Level.FINE).log("Registered LootQualitySystem");
        
        // Register EquipmentAffixListener (applies affix modifiers on equipment change)
        EquipmentAffixListener equipmentAffixListener = new EquipmentAffixListener();
        equipmentAffixListener.register();
        getLogger().at(Level.FINE).log("Registered EquipmentAffixListener");

        // Register EffectAffixCastListener (triggers on-cast effects)
        EffectAffixCastListener effectAffixCastListener = new EffectAffixCastListener();
        effectAffixCastListener.register();
        getLogger().at(Level.FINE).log("Registered EffectAffixCastListener");
    }
    
    /**
     * Register combat-related ECS systems.
     * <p>
     * These systems handle the combat pipeline:
     * - Hit resolution (accuracy vs evasion)
     * - Auto-block
     * - Critical hits
     */
    private void registerCombatSystems(ComponentRegistryProxy<EntityStore> entityStoreRegistry) {
        // Register hit resolution system (runs in gather group before damage filtering)
        entityStoreRegistry.registerSystem(new HyforgedHitResolutionSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedHitResolutionSystem");
        
        // Register auto-block system (runs in filter group after hit resolution)
        entityStoreRegistry.registerSystem(new HyforgedAutoBlockSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedAutoBlockSystem");
        
        // Register critical hit system (runs in inspect group after damage reduction)
        entityStoreRegistry.registerSystem(new HyforgedCriticalHitSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedCriticalHitSystem");
        
        // Register combat text system (runs in inspect group before EntityUIEvents)
        entityStoreRegistry.registerSystem(new HyforgedCombatTextSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedCombatTextSystem");
        
        // Register combat log system (runs in inspect group to record final damage)
        entityStoreRegistry.registerSystem(new HyforgedCombatLogSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedCombatLogSystem");

        // Register effect affix triggers on damage events
        entityStoreRegistry.registerSystem(new EffectAffixDamageTriggerSystem());
        getLogger().at(Level.FINE).log("Registered EffectAffixDamageTriggerSystem");

        // Register effect affix on-kill triggers
        entityStoreRegistry.registerSystem(new EffectAffixOnKillSystem());
        getLogger().at(Level.FINE).log("Registered EffectAffixOnKillSystem");
        
        // Register combat log HUD system (manages WoW-style combat log UI)
        entityStoreRegistry.registerSystem(new CombatLogHudSystem());
        getLogger().at(Level.FINE).log("Registered CombatLogHudSystem");
        
        // Register ailment system (runs in inspect group after damage is applied)
        // Ailment definitions are loaded via AilmentLoader.initialize() in initializeStatDefinitions()
        entityStoreRegistry.registerSystem(new HyforgedAilmentSystem(ailmentAccumulatorComponentType));
        getLogger().at(Level.FINE).log("Registered HyforgedAilmentSystem");

        // Register concentration disruption system (runs last in inspect group)
        entityStoreRegistry.registerSystem(new HyforgedConcentrationDisruptionSystem(concentrationPriorityComponentType));
        getLogger().at(Level.FINE).log("Registered HyforgedConcentrationDisruptionSystem");
        
        // Register monster scaling system (assigns levels to NPCs based on spawn distance)
        entityStoreRegistry.registerSystem(new HyforgedMonsterScalingSystem());
        getLogger().at(Level.FINE).log("Registered HyforgedMonsterScalingSystem");
    }
    
    /**
     * Register admin commands for the Hyforged plugin.
     */
    private void registerCommands() {
        this.getCommandRegistry().registerCommand(new HyforgedCommand());
        this.getCommandRegistry().registerCommand(new reign.software.hyforged.passive.command.PassiveCommand());
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

        OpenCustomUIInteraction.registerSimple(
            this,
            ConcentrationPriorityPage.class,
            "ConcentrationPriorityPage",
            ConcentrationPriorityPage::new
        );

        getLogger().at(Level.FINE).log("Registered ConcentrationPriorityPage custom UI interaction");

        // Register PassiveTreePage for passive tree viewing/allocation
        OpenCustomUIInteraction.registerSimple(
            this,
            PassiveTreePage.class,
            "PassiveTreePage",
            PassiveTreePage::new
        );

        getLogger().at(Level.FINE).log("Registered PassiveTreePage custom UI interaction");

        // Register CharacterHubPage for central navigation
        OpenCustomUIInteraction.registerSimple(
            this,
            CharacterHubPage.class,
            "CharacterHubPage",
            CharacterHubPage::new
        );

        getLogger().at(Level.FINE).log("Registered CharacterHubPage custom UI interaction");

        // Register Point Book interaction for consuming point books
        this.getCodecRegistry(Interaction.CODEC).register(
            PointBookInteraction.TYPE_ID,
            PointBookInteraction.class,
            PointBookInteraction.CODEC
        );

        getLogger().at(Level.FINE).log("Registered PointBookInteraction");

        // Register Tradebar Vault page for block-based vault access
        // The vault component is created on first interaction if it doesn't exist
        OpenCustomUIInteraction.registerBlockEntityCustomPage(
            this,
            VaultPage.class,
            "TradebarVault",
            VaultPage::new,
            () -> {
                Holder<ChunkStore> holder = ChunkStore.REGISTRY.newHolder();
                holder.ensureComponent(tradebarVaultComponentType);
                return holder;
            }
        );

        getLogger().at(Level.FINE).log("Registered TradebarVault custom UI interaction");

        // Register Market Stall page for selling items
        // Market stalls don't require persistent state - just player ref needed
        OpenCustomUIInteraction.registerBlockEntityCustomPage(
            this,
            MarketStallPage.class,
            "MarketStall",
            (playerRef, blockRef) -> new MarketStallPage(playerRef)
        );

        getLogger().at(Level.FINE).log("Registered MarketStall custom UI interaction");
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
    
    /**
     * Get the AilmentAccumulatorComponent type for ECS operations.
     */
    @Nonnull
    public ComponentType<EntityStore, AilmentAccumulatorComponent> getAilmentAccumulatorComponentType() {
        if (ailmentAccumulatorComponentType == null) {
            throw new IllegalStateException("HyforgedPlugin not initialized");
        }
        return ailmentAccumulatorComponentType;
    }
    
    /**
     * Get the MonsterLevelComponent type for ECS operations.
     */
    @Nonnull
    public ComponentType<EntityStore, MonsterLevelComponent> getMonsterLevelComponentType() {
        if (monsterLevelComponentType == null) {
            throw new IllegalStateException("HyforgedPlugin not initialized");
        }
        return monsterLevelComponentType;
    }

    /**
     * Get the HyforgedNPCQualityComponent type for ECS operations.
     */
    @Nonnull
    public ComponentType<EntityStore, HyforgedNPCQualityComponent> getNpcQualityComponentType() {
        if (npcQualityComponentType == null) {
            throw new IllegalStateException("HyforgedPlugin not initialized");
        }
        return npcQualityComponentType;
    }

    /**
     * Get the HyforgedActiveEffectsComponent type for ECS operations.
     */
    @Nonnull
    public ComponentType<EntityStore, HyforgedActiveEffectsComponent> getActiveEffectsComponentType() {
        if (activeEffectsComponentType == null) {
            throw new IllegalStateException("HyforgedPlugin not initialized");
        }
        return activeEffectsComponentType;
    }

    /**
     * Get the ConcentrationPriorityComponent type for ECS operations.
     */
    @Nonnull
    public ComponentType<EntityStore, ConcentrationPriorityComponent> getConcentrationPriorityComponentType() {
        if (concentrationPriorityComponentType == null) {
            throw new IllegalStateException("HyforgedPlugin not initialized");
        }
        return concentrationPriorityComponentType;
    }

    /**
     * Get the PassiveTreeComponent type for ECS operations.
     */
    @Nonnull
    public ComponentType<EntityStore, PassiveTreeComponent> getPassiveTreeComponentType() {
        if (passiveTreeComponentType == null) {
            throw new IllegalStateException("HyforgedPlugin not initialized");
        }
        return passiveTreeComponentType;
    }

    /**
     * Get the PlayerUnlocksComponent type for ECS operations.
     */
    @Nonnull
    public ComponentType<EntityStore, PlayerUnlocksComponent> getPlayerUnlocksComponentType() {
        if (playerUnlocksComponentType == null) {
            throw new IllegalStateException("HyforgedPlugin not initialized");
        }
        return playerUnlocksComponentType;
    }

    /**
     * Get the PlayerSpellsComponent type for ECS operations.
     */
    @Nonnull
    public ComponentType<EntityStore, PlayerSpellsComponent> getPlayerSpellsComponentType() {
        if (playerSpellsComponentType == null) {
            throw new IllegalStateException("HyforgedPlugin not initialized");
        }
        return playerSpellsComponentType;
    }
    
    /**
     * Get the TradebarVaultComponent type for ChunkStore operations.
     */
    @Nonnull
    public ComponentType<ChunkStore, TradebarVaultComponent> getTradebarVaultComponentType() {
        if (tradebarVaultComponentType == null) {
            throw new IllegalStateException("HyforgedPlugin not initialized");
        }
        return tradebarVaultComponentType;
    }
}
