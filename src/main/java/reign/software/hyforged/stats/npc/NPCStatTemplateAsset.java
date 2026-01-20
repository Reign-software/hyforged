package reign.software.hyforged.stats.npc;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * JSON asset definition for NPC stat templates.
 * <p>
 * NPC stat templates define base stats and level scaling for NPC types.
 * Templates can inherit from parent templates for reusability.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Id": "hyforged:hostile",
 *   "Parent": "hyforged:base",
 *   "MaxHealth": 100,
 *   "MaxHealthPerLevel": 20,
 *   "Strength": 10,
 *   "StrengthPerLevel": 2,
 *   "EliteModifiers": ["hyforged:tough", "hyforged:swift"]
 * }
 * </pre>
 */
public class NPCStatTemplateAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, NPCStatTemplateAsset>> {

    /**
     * Codec for loading NPCStatTemplateAsset from JSON.
     */
    public static final AssetBuilderCodec<String, NPCStatTemplateAsset> CODEC = AssetBuilderCodec
            .builder(
                    NPCStatTemplateAsset.class,
                    NPCStatTemplateAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .appendInherited(
                    new KeyedCodec<>("Parent", Codec.STRING),
                    (asset, value) -> asset.parentId = value,
                    asset -> asset.parentId,
                    (asset, parent) -> { if (asset.parentId == null) asset.parentId = parent.parentId; }
            )
            .add()
            // Core ability scores
            .appendInherited(
                    new KeyedCodec<>("Strength", Codec.INTEGER),
                    (asset, value) -> asset.strength = value != null ? value : 0,
                    asset -> asset.strength,
                    (asset, parent) -> { if (asset.strength == 0) asset.strength = parent.strength; }
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("StrengthPerLevel", Codec.INTEGER),
                    (asset, value) -> asset.strengthPerLevel = value != null ? value : 0,
                    asset -> asset.strengthPerLevel,
                    (asset, parent) -> { if (asset.strengthPerLevel == 0) asset.strengthPerLevel = parent.strengthPerLevel; }
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Dexterity", Codec.INTEGER),
                    (asset, value) -> asset.dexterity = value != null ? value : 0,
                    asset -> asset.dexterity,
                    (asset, parent) -> { if (asset.dexterity == 0) asset.dexterity = parent.dexterity; }
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("DexterityPerLevel", Codec.INTEGER),
                    (asset, value) -> asset.dexterityPerLevel = value != null ? value : 0,
                    asset -> asset.dexterityPerLevel,
                    (asset, parent) -> { if (asset.dexterityPerLevel == 0) asset.dexterityPerLevel = parent.dexterityPerLevel; }
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Intelligence", Codec.INTEGER),
                    (asset, value) -> asset.intelligence = value != null ? value : 0,
                    asset -> asset.intelligence,
                    (asset, parent) -> { if (asset.intelligence == 0) asset.intelligence = parent.intelligence; }
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("IntelligencePerLevel", Codec.INTEGER),
                    (asset, value) -> asset.intelligencePerLevel = value != null ? value : 0,
                    asset -> asset.intelligencePerLevel,
                    (asset, parent) -> { if (asset.intelligencePerLevel == 0) asset.intelligencePerLevel = parent.intelligencePerLevel; }
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Constitution", Codec.INTEGER),
                    (asset, value) -> asset.constitution = value != null ? value : 0,
                    asset -> asset.constitution,
                    (asset, parent) -> { if (asset.constitution == 0) asset.constitution = parent.constitution; }
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("ConstitutionPerLevel", Codec.INTEGER),
                    (asset, value) -> asset.constitutionPerLevel = value != null ? value : 0,
                    asset -> asset.constitutionPerLevel,
                    (asset, parent) -> { if (asset.constitutionPerLevel == 0) asset.constitutionPerLevel = parent.constitutionPerLevel; }
            )
            .add()
            // Derived stats - health
            .appendInherited(
                    new KeyedCodec<>("MaxHealth", Codec.INTEGER),
                    (asset, value) -> asset.maxHealth = value != null ? value : 0,
                    asset -> asset.maxHealth,
                    (asset, parent) -> { if (asset.maxHealth == 0) asset.maxHealth = parent.maxHealth; }
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("MaxHealthPerLevel", Codec.INTEGER),
                    (asset, value) -> asset.maxHealthPerLevel = value != null ? value : 0,
                    asset -> asset.maxHealthPerLevel,
                    (asset, parent) -> { if (asset.maxHealthPerLevel == 0) asset.maxHealthPerLevel = parent.maxHealthPerLevel; }
            )
            .add()
            // Physical stats
            .appendInherited(
                    new KeyedCodec<>("PhysicalPower", Codec.INTEGER),
                    (asset, value) -> asset.physicalPower = value != null ? value : 0,
                    asset -> asset.physicalPower,
                    (asset, parent) -> { if (asset.physicalPower == 0) asset.physicalPower = parent.physicalPower; }
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("PhysicalPowerPerLevel", Codec.INTEGER),
                    (asset, value) -> asset.physicalPowerPerLevel = value != null ? value : 0,
                    asset -> asset.physicalPowerPerLevel,
                    (asset, parent) -> { if (asset.physicalPowerPerLevel == 0) asset.physicalPowerPerLevel = parent.physicalPowerPerLevel; }
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("ArmorRating", Codec.INTEGER),
                    (asset, value) -> asset.armorRating = value != null ? value : 0,
                    asset -> asset.armorRating,
                    (asset, parent) -> { if (asset.armorRating == 0) asset.armorRating = parent.armorRating; }
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("ArmorRatingPerLevel", Codec.INTEGER),
                    (asset, value) -> asset.armorRatingPerLevel = value != null ? value : 0,
                    asset -> asset.armorRatingPerLevel,
                    (asset, parent) -> { if (asset.armorRatingPerLevel == 0) asset.armorRatingPerLevel = parent.armorRatingPerLevel; }
            )
            .add()
            .build();

    // Asset metadata
    private String id;
    private AssetExtraInfo.Data data;
    
    // Inheritance
    private String parentId;
    
    // Ability scores
    private int strength = 0;
    private int strengthPerLevel = 0;
    private int dexterity = 0;
    private int dexterityPerLevel = 0;
    private int intelligence = 0;
    private int intelligencePerLevel = 0;
    private int constitution = 0;
    private int constitutionPerLevel = 0;
    
    // Derived stats
    private int maxHealth = 0;
    private int maxHealthPerLevel = 0;
    private int physicalPower = 0;
    private int physicalPowerPerLevel = 0;
    private int armorRating = 0;
    private int armorRatingPerLevel = 0;
    
    // Modifier pools (hardcoded field names for now)
    private List<String> eliteModifiers = Collections.emptyList();
    private List<String> bossModifiers = Collections.emptyList();

    public NPCStatTemplateAsset() {
        // Required for codec
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nullable
    public String getParentId() {
        return parentId;
    }
    
    /**
     * Convert to an NPCStatTemplate domain object (unresolved inheritance).
     * <p>
     * Note: This creates a template with raw values from this asset.
     * Inheritance resolution should be done by NPCStatTemplateRegistry.
     */
    @Nonnull
    public NPCStatTemplate toTemplate() {
        NPCStatTemplate.Builder builder = NPCStatTemplate.builder(id)
                .parent(parentId);
        
        // Add ability scores
        if (strength != 0 || strengthPerLevel != 0) {
            builder.stat(StatId.hyforged("strength"), strength, strengthPerLevel);
        }
        if (dexterity != 0 || dexterityPerLevel != 0) {
            builder.stat(StatId.hyforged("dexterity"), dexterity, dexterityPerLevel);
        }
        if (intelligence != 0 || intelligencePerLevel != 0) {
            builder.stat(StatId.hyforged("intelligence"), intelligence, intelligencePerLevel);
        }
        if (constitution != 0 || constitutionPerLevel != 0) {
            builder.stat(StatId.hyforged("constitution"), constitution, constitutionPerLevel);
        }
        
        // Add derived stats
        if (maxHealth != 0 || maxHealthPerLevel != 0) {
            builder.stat(StatId.hyforged("max-health"), maxHealth, maxHealthPerLevel);
        }
        if (physicalPower != 0 || physicalPowerPerLevel != 0) {
            builder.stat(StatId.hyforged("physical-power"), physicalPower, physicalPowerPerLevel);
        }
        if (armorRating != 0 || armorRatingPerLevel != 0) {
            builder.stat(StatId.hyforged("armor-rating"), armorRating, armorRatingPerLevel);
        }
        
        // Add modifier pools
        if (!eliteModifiers.isEmpty()) {
            builder.modifierPool("elite", eliteModifiers);
        }
        if (!bossModifiers.isEmpty()) {
            builder.modifierPool("boss", bossModifiers);
        }
        
        return builder.build();
    }
}
