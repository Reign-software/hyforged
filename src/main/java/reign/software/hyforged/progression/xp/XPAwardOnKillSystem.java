package reign.software.hyforged.progression.xp;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.combat.scaling.MonsterLevelComponent;
import reign.software.hyforged.combat.scaling.MonsterScalingService;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.quality.component.HyforgedNPCQualityComponent;
import reign.software.hyforged.quality.model.NPCQualityRule;
import reign.software.hyforged.quality.registry.NPCQualityRegistry;
import reign.software.hyforged.stats.bridge.ProgressionStatBridge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.logging.Logger;

/**
 * System that awards XP to players when they kill entities.
 * <p>
 * Extends Hytale's OnDeathSystem to react when DeathComponent is added to an entity.
 * Checks if the killer is a player with a ProgressionComponent and dispatches
 * an XPAwardEvent based on the killed entity's configured XP value.
 * <p>
 * XP values are currently static but will be data-driven via JSON config in future phases.
 */
public class XPAwardOnKillSystem extends DeathSystems.OnDeathSystem {
    
    private static final Logger LOGGER = Logger.getLogger(XPAwardOnKillSystem.class.getName());
    
    public XPAwardOnKillSystem() {
        super();
    }
    
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        // We want to process all entities with DeathComponent (killed entities)
        // The base OnDeathSystem handles the DeathComponent filter already
        return DeathComponent.getComponentType();
    }
    
    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> victimRef,
            @Nonnull DeathComponent deathComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Get the damage info from the death to find the killer
        Damage deathInfo = deathComponent.getDeathInfo();
        if (deathInfo == null) {
            return;
        }
        
        Damage.Source source = deathInfo.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            // Not killed by an entity (e.g., environment, command)
            return;
        }
        
        Ref<EntityStore> killerRef = entitySource.getRef();
        if (killerRef == null || !killerRef.isValid()) {
            return;
        }
        
        // Check if the killer is a player
        Player killerPlayer = store.getComponent(killerRef, Player.getComponentType());
        if (killerPlayer == null) {
            return;
        }
        
        // Check if the killer has a ProgressionComponent
        ProgressionComponent killerProgression = store.getComponent(killerRef, 
                HyforgedPlugin.getInstance().getProgressionComponentType());
        if (killerProgression == null) {
            return;
        }
        
        // Calculate XP to award
        long xpAmount = calculateXpForKill(victimRef, store);
        if (xpAmount <= 0) {
            return;
        }

        // Resolve mob info for combat log display (must happen now while victim is still valid)
        String mobDisplayName = resolveVictimDisplayName(victimRef, store);
        int mobLevel = resolveEnemyLevel(victimRef, store);
        String mobQuality = resolveEnemyQuality(victimRef, store);

        // Dispatch XP award event to the killer with mob info
        XPAwardEvent xpEvent = XPAwardEvent.combat(xpAmount, victimRef, mobDisplayName, mobLevel, mobQuality);
        commandBuffer.invoke(killerRef, xpEvent);
        
        LOGGER.fine(String.format("Awarding %d combat XP to player for killing %s [%s] Lv.%d",
                xpAmount, mobDisplayName != null ? mobDisplayName : "entity",
                mobQuality != null ? mobQuality : "Common", mobLevel));
    }
    
    /**
     * Calculate XP to award for killing an entity.
     * <p>
        * Uses XPConfig for base values and scaling, sourcing level and difficulty
        * from monster scaling, quality rules, or model tags when available.
     * 
     * @param victimRef the killed entity
     * @param store the entity store
     * @return XP amount to award
     */
    private long calculateXpForKill(Ref<EntityStore> victimRef, Store<EntityStore> store) {
        // Get base XP from config with level/difficulty scaling
        int enemyLevel = resolveEnemyLevel(victimRef, store);
        String difficulty = resolveEnemyDifficulty(victimRef, store);

        return XPConfig.get().calculateCombatXp(enemyLevel, difficulty);
    }

    private int resolveEnemyLevel(@Nonnull Ref<EntityStore> victimRef, @Nonnull Store<EntityStore> store) {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        MonsterLevelComponent monsterLevel = store.getComponent(victimRef, plugin.getMonsterLevelComponentType());
        if (monsterLevel != null && monsterLevel.getLevel() > 0) {
            return monsterLevel.getLevel();
        }

        TransformComponent transform = store.getComponent(victimRef, TransformComponent.getComponentType());
        World world = store.getExternalData().getWorld();
        if (transform != null && world != null) {
            return MonsterScalingService.get().calculateMonsterLevel(world, transform.getPosition());
        }

        return ProgressionStatBridge.getCharacterLevel(victimRef, store);
    }

    /**
     * Resolve the quality tier of the killed entity from its NPC quality component.
     *
     * @param victimRef the killed entity
     * @param store the entity store
     * @return quality ID (e.g., "Common", "Rare"), or null if not available
     */
    @Nullable
    private String resolveEnemyQuality(@Nonnull Ref<EntityStore> victimRef, @Nonnull Store<EntityStore> store) {
        HyforgedNPCQualityComponent quality = store.getComponent(victimRef,
                HyforgedPlugin.getInstance().getNpcQualityComponentType());
        if (quality != null && !quality.getQualityId().isBlank()) {
            return quality.getQualityId();
        }
        return null;
    }

    @Nullable
    private String resolveEnemyDifficulty(@Nonnull Ref<EntityStore> victimRef, @Nonnull Store<EntityStore> store) {
        NPCEntity npcEntity = store.getComponent(victimRef, NPCEntity.getComponentType());
        if (npcEntity != null) {
            NPCQualityRule rule = NPCQualityRegistry.get().resolveRuleForRole(npcEntity.getRoleName());
            if (rule != null && !rule.id().isBlank()) {
                return rule.id();
            }
        }

        HyforgedNPCQualityComponent quality = store.getComponent(victimRef, HyforgedPlugin.getInstance().getNpcQualityComponentType());
        if (quality != null && !quality.getQualityId().isBlank()) {
            return quality.getQualityId();
        }

        return resolveDifficultyFromModelTags(victimRef, store);
    }

    @Nullable
    private String resolveDifficultyFromModelTags(@Nonnull Ref<EntityStore> victimRef, @Nonnull Store<EntityStore> store) {
        ModelComponent modelComponent = store.getComponent(victimRef, ModelComponent.getComponentType());
        if (modelComponent == null) {
            return null;
        }

        Model model = modelComponent.getModel();
        if (model == null) {
            return null;
        }

        String modelAssetId = model.getModelAssetId();
        if (modelAssetId == null || modelAssetId.isBlank()) {
            return null;
        }

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelAssetId);
        if (modelAsset == null) {
            return null;
        }

        AssetExtraInfo.Data data = ModelAsset.getAssetStore().getCodec().getData(modelAsset);
        if (data == null) {
            return null;
        }

        Map<String, String[]> rawTags = data.getRawTags();
        if (rawTags == null || rawTags.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, String[]> entry : rawTags.entrySet()) {
            String key = entry.getKey();
            if (key == null || !"Difficulty".equalsIgnoreCase(key)) {
                continue;
            }

            String[] values = entry.getValue();
            if (values == null || values.length == 0) {
                return null;
            }

            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }

        return null;
    }

    /**
     * Resolve a display name for the killed entity for combat log.
     * <p>
     * Resolution priority:
     * <ol>
     *   <li>Nameplate text (includes quality, affixes, level if NPCNameplateSystem ran)</li>
     *   <li>DisplayNameComponent raw text</li>
     *   <li>NPC role name (formatted)</li>
     * </ol>
     *
     * @param victimRef the killed entity
     * @param store the entity store
     * @return display name, or null if unresolvable
     */
    @Nullable
    private String resolveVictimDisplayName(@Nonnull Ref<EntityStore> victimRef, @Nonnull Store<EntityStore> store) {
        // Prefer DisplayNameComponent (clean base name without level/quality decorations)
        DisplayNameComponent displayNameComp = store.getComponent(victimRef, DisplayNameComponent.getComponentType());
        if (displayNameComp != null) {
            com.hypixel.hytale.server.core.Message displayName = displayNameComp.getDisplayName();
            if (displayName != null) {
                String rawText = displayName.getRawText();
                if (rawText != null && !rawText.isEmpty()) {
                    return rawText;
                }
            }
        }

        // Try NPC role name
        NPCEntity npcEntity = store.getComponent(victimRef, NPCEntity.getComponentType());
        if (npcEntity != null) {
            String roleName = npcEntity.getRoleName();
            if (roleName != null && !roleName.isEmpty()) {
                return roleName.replace('_', ' ');
            }
        }

        // Fallback to nameplate, but strip level suffix and quality prefix
        // (nameplate format: "Quality {Prefix} Name {Suffix} Lv.X")
        Nameplate nameplate = store.getComponent(victimRef, Nameplate.getComponentType());
        if (nameplate != null) {
            String text = nameplate.getText();
            if (text != null && !text.isEmpty()) {
                return stripNameplateDecorations(text);
            }
        }

        return null;
    }

    /**
     * Strip level suffix and quality prefix from a nameplate string.
     * <p>
     * Nameplate format: {@code Quality {Prefix} Name {Suffix} Lv.X}
     * Returns just: {@code {Prefix} Name {Suffix}}
     */
    @Nonnull
    private static String stripNameplateDecorations(@Nonnull String nameplate) {
        // Strip trailing " Lv.\d+"
        String stripped = nameplate.replaceAll("\\s+Lv\\.\\d+$", "");
        // Strip leading quality tag (e.g. "Rare ", "Epic ", or bracket format "[Rare] ")
        stripped = stripped.replaceAll("^\\[\\w+]\\s*", "");
        return stripped.isEmpty() ? nameplate : stripped;
    }
}
