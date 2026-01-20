package reign.software.hyforged.stats.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON asset definition for character classes.
 * <p>
 * Classes define the base ability score distribution for players,
 * weapon tag families for class XP activation, and per-level ability rewards.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Id": "hyforged:warrior",
 *   "DisplayName": "Warrior",
 *   "Description": "A powerful melee combatant...",
 *   "Strength": 5,
 *   "Constitution": 3,
 *   "Dexterity": 2,
 *   "WeaponTagFamilies": ["weapon:sword", "weapon:axe", "weapon:mace"],
 *   "LevelRewards": [
 *     { "Level": 5, "Strength": 1 },
 *     { "Level": 10, "Strength": 1, "Constitution": 1 },
 *     { "Level": 15, "Strength": 2 },
 *     { "Level": 20, "Strength": 2, "Constitution": 1 }
 *   ]
 * }
 * </pre>
 * <p>
 * Ability scores not specified default to 1.
 */
public class ClassDefinitionAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, ClassDefinitionAsset>> {

    /**
     * Codec for loading ClassDefinitionAsset from JSON.
     */
    public static final AssetBuilderCodec<String, ClassDefinitionAsset> CODEC = AssetBuilderCodec
            .builder(
                    ClassDefinitionAsset.class,
                    ClassDefinitionAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .appendInherited(
                    new KeyedCodec<>("DisplayName", Codec.STRING),
                    (asset, value) -> asset.displayName = value != null ? value : "Unknown Class",
                    asset -> asset.displayName,
                    (asset, parent) -> asset.displayName = parent.displayName
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Description", Codec.STRING),
                    (asset, value) -> asset.description = value != null ? value : "",
                    asset -> asset.description,
                    (asset, parent) -> asset.description = parent.description
            )
            .add()
            // Core ability scores as individual fields
            .appendInherited(
                    new KeyedCodec<>("Strength", Codec.INTEGER),
                    (asset, value) -> asset.strength = value != null ? value : 1,
                    asset -> asset.strength,
                    (asset, parent) -> asset.strength = parent.strength
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Dexterity", Codec.INTEGER),
                    (asset, value) -> asset.dexterity = value != null ? value : 1,
                    asset -> asset.dexterity,
                    (asset, parent) -> asset.dexterity = parent.dexterity
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Intelligence", Codec.INTEGER),
                    (asset, value) -> asset.intelligence = value != null ? value : 1,
                    asset -> asset.intelligence,
                    (asset, parent) -> asset.intelligence = parent.intelligence
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Constitution", Codec.INTEGER),
                    (asset, value) -> asset.constitution = value != null ? value : 1,
                    asset -> asset.constitution,
                    (asset, parent) -> asset.constitution = parent.constitution
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Wisdom", Codec.INTEGER),
                    (asset, value) -> asset.wisdom = value != null ? value : 1,
                    asset -> asset.wisdom,
                    (asset, parent) -> asset.wisdom = parent.wisdom
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Spirit", Codec.INTEGER),
                    (asset, value) -> asset.spirit = value != null ? value : 1,
                    asset -> asset.spirit,
                    (asset, parent) -> asset.spirit = parent.spirit
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Luck", Codec.INTEGER),
                    (asset, value) -> asset.luck = value != null ? value : 1,
                    asset -> asset.luck,
                    (asset, parent) -> asset.luck = parent.luck
            )
            .add()
            // Weapon tag families - tags that activate this class for XP
            .appendInherited(
                    new KeyedCodec<>("WeaponTagFamilies", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (asset, value) -> asset.weaponTagFamilies = value != null ? new ArrayList<>(Arrays.asList(value)) : new ArrayList<>(),
                    asset -> asset.weaponTagFamilies.toArray(new String[0]),
                    (asset, parent) -> asset.weaponTagFamilies = new ArrayList<>(parent.weaponTagFamilies)
            )
            .add()
            // Level rewards - per-level ability bonuses
            .appendInherited(
                    new KeyedCodec<>("LevelRewards", LevelRewardEntry.ARRAY_CODEC),
                    (asset, value) -> asset.levelRewards = value != null ? new ArrayList<>(Arrays.asList(value)) : new ArrayList<>(),
                    asset -> asset.levelRewards.toArray(new LevelRewardEntry[0]),
                    (asset, parent) -> asset.levelRewards = new ArrayList<>(parent.levelRewards)
            )
            .add()
            .build();

    // Asset metadata
    private String id;
    private AssetExtraInfo.Data data;
    
    // Class properties
    private String displayName = "Unknown Class";
    private String description = "";
    
    // Ability scores
    private int strength = 1;
    private int dexterity = 1;
    private int intelligence = 1;
    private int constitution = 1;
    private int wisdom = 1;
    private int spirit = 1;
    private int luck = 1;
    
    // New progression fields
    private List<String> weaponTagFamilies = new ArrayList<>();
    private List<LevelRewardEntry> levelRewards = new ArrayList<>();

    public ClassDefinitionAsset() {
        // Required for codec
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }
    
    public int getStrength() { return strength; }
    public int getDexterity() { return dexterity; }
    public int getIntelligence() { return intelligence; }
    public int getConstitution() { return constitution; }
    public int getWisdom() { return wisdom; }
    public int getSpirit() { return spirit; }
    public int getLuck() { return luck; }
    
    @Nonnull
    public List<String> getWeaponTagFamilies() {
        return Collections.unmodifiableList(weaponTagFamilies);
    }
    
    @Nonnull
    public List<LevelRewardEntry> getLevelRewards() {
        return Collections.unmodifiableList(levelRewards);
    }

    /**
     * Convert to a ClassDefinition domain object.
     */
    @Nonnull
    public ClassDefinition toClassDefinition() {
        Map<StatId, Integer> abilityScores = new HashMap<>();
        
        // Map the fixed ability scores to StatIds
        abilityScores.put(StatId.hyforged("strength"), strength);
        abilityScores.put(StatId.hyforged("dexterity"), dexterity);
        abilityScores.put(StatId.hyforged("intelligence"), intelligence);
        abilityScores.put(StatId.hyforged("constitution"), constitution);
        abilityScores.put(StatId.hyforged("wisdom"), wisdom);
        abilityScores.put(StatId.hyforged("spirit"), spirit);
        abilityScores.put(StatId.hyforged("luck"), luck);
        
        // Convert weapon tag families to set
        Set<String> weaponTags = new HashSet<>(weaponTagFamilies);
        
        // Convert level rewards to map
        Map<Integer, Map<StatId, Integer>> rewardsMap = new HashMap<>();
        for (LevelRewardEntry entry : levelRewards) {
            rewardsMap.put(entry.getLevel(), entry.toAbilityBonuses());
        }
        
        return new ClassDefinition(
            id,
            displayName,
            description,
            abilityScores,
            weaponTags,
            rewardsMap
        );
    }
    
    /**
     * Entry representing ability score bonuses at a specific level.
     */
    public static class LevelRewardEntry {
        public static final BuilderCodec<LevelRewardEntry> CODEC = BuilderCodec
                .builder(LevelRewardEntry.class, LevelRewardEntry::new)
                .append(new KeyedCodec<>("Level", Codec.INTEGER), (e, v) -> e.level = v != null ? v : 1, e -> e.level).add()
                .append(new KeyedCodec<>("Strength", Codec.INTEGER), (e, v) -> e.strength = v != null ? v : 0, e -> e.strength).add()
                .append(new KeyedCodec<>("Dexterity", Codec.INTEGER), (e, v) -> e.dexterity = v != null ? v : 0, e -> e.dexterity).add()
                .append(new KeyedCodec<>("Intelligence", Codec.INTEGER), (e, v) -> e.intelligence = v != null ? v : 0, e -> e.intelligence).add()
                .append(new KeyedCodec<>("Constitution", Codec.INTEGER), (e, v) -> e.constitution = v != null ? v : 0, e -> e.constitution).add()
                .append(new KeyedCodec<>("Wisdom", Codec.INTEGER), (e, v) -> e.wisdom = v != null ? v : 0, e -> e.wisdom).add()
                .append(new KeyedCodec<>("Spirit", Codec.INTEGER), (e, v) -> e.spirit = v != null ? v : 0, e -> e.spirit).add()
                .append(new KeyedCodec<>("Luck", Codec.INTEGER), (e, v) -> e.luck = v != null ? v : 0, e -> e.luck).add()
                .build();
        
        public static final Codec<LevelRewardEntry[]> ARRAY_CODEC = 
                new ArrayCodec<>(CODEC, LevelRewardEntry[]::new);
        
        private int level = 1;
        private int strength = 0;
        private int dexterity = 0;
        private int intelligence = 0;
        private int constitution = 0;
        private int wisdom = 0;
        private int spirit = 0;
        private int luck = 0;
        
        public LevelRewardEntry() {
            // Required for codec
        }
        
        public int getLevel() { return level; }
        public int getStrength() { return strength; }
        public int getDexterity() { return dexterity; }
        public int getIntelligence() { return intelligence; }
        public int getConstitution() { return constitution; }
        public int getWisdom() { return wisdom; }
        public int getSpirit() { return spirit; }
        public int getLuck() { return luck; }
        
        /**
         * Convert to a map of StatId to bonus values.
         * Only includes non-zero bonuses.
         */
        @Nonnull
        public Map<StatId, Integer> toAbilityBonuses() {
            Map<StatId, Integer> bonuses = new HashMap<>();
            if (strength != 0) bonuses.put(StatId.hyforged("strength"), strength);
            if (dexterity != 0) bonuses.put(StatId.hyforged("dexterity"), dexterity);
            if (intelligence != 0) bonuses.put(StatId.hyforged("intelligence"), intelligence);
            if (constitution != 0) bonuses.put(StatId.hyforged("constitution"), constitution);
            if (wisdom != 0) bonuses.put(StatId.hyforged("wisdom"), wisdom);
            if (spirit != 0) bonuses.put(StatId.hyforged("spirit"), spirit);
            if (luck != 0) bonuses.put(StatId.hyforged("luck"), luck);
            return bonuses;
        }
    }
}

