package reign.software.hyforged.concentration;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.TestAssetStore;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.logger.backend.HytaleLogManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import reign.software.hyforged.affix.asset.AffixDefinitionAsset;
import reign.software.hyforged.affix.asset.AffixPoolAsset;
import reign.software.hyforged.affix.api.AffixService;
import reign.software.hyforged.affix.api.AffixSpec;
import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.AffixPool;
import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.affix.registry.AffixPoolRegistry;
import reign.software.hyforged.affix.service.HyforgedItemDataService;
import reign.software.hyforged.effect.HyforgedEffectAsset;
import reign.software.hyforged.effect.HyforgedEffectDefinition;
import reign.software.hyforged.effect.HyforgedEffectRegistry;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.asset.StatDefinitionAsset;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(
    named = "java.util.logging.manager",
    matches = "com.hypixel.hytale.logger.backend.HytaleLogManager"
)
class ConcentrationAssetLoadingTest {

    private static final String PACK_KEY = "Hytale:Hytale";

    private static final Set<String> CONCENTRATION_STAT_IDS = Set.of(
            "hyforged:concentration-regen-rate-bps",
            "hyforged:concentration-loss-reduction-bps",
            "hyforged:concentration-loss-threshold-bps"
    );

    private static final Set<String> CONCENTRATION_EFFECT_IDS = Set.of(
            "hyforged:focused-mind",
            "hyforged:iron-will",
            "hyforged:mental-fortress",
            "hyforged:monks-serenity",
            "hyforged:mind-fog",
            "hyforged:psychic-scream",
            "hyforged:shattered-focus",
            "hyforged:brain-rot"
    );

    private static final Set<String> CONCENTRATION_AFFIX_IDS = Set.of(
            "hyforged:of-clarity",
            "hyforged:of-resolve",
            "hyforged:steadfast",
            "hyforged:mental-bastion",
            "hyforged:unshakeable-focus"
    );

    private static final Set<String> CONCENTRATION_POOL_IDS = Set.of("WeaponMelee", "Armor");

    @BeforeAll
    static void ensureHytaleLogManager() {
        String managerName = LogManager.getLogManager().getClass().getName();
        Assumptions.assumeTrue(
                managerName.equals(HytaleLogManager.class.getName()),
                "Requires HytaleLogManager for asset loading tests"
        );
    }

    @BeforeEach
    void resetRegistries() {
        StatDefinitionRegistry.reset();
        HyforgedEffectRegistry.get().clear();
        AffixDefinitionRegistry.reset();
        AffixPoolRegistry.reset();
    }

    @Test
    void concentrationStatsLoadIntoRegistry() {
        AssetStore<String, StatDefinitionAsset, IndexedLookupTableAssetMap<String, StatDefinitionAsset>> store =
                ensureStatDefinitionStore();

        loadAssets(store,
                "Server/Hyforged/Stats/ConcentrationRegenRate.json",
                "Server/Hyforged/Stats/ConcentrationLossReduction.json",
                "Server/Hyforged/Stats/ConcentrationLossThreshold.json"
        );

        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        for (StatDefinitionAsset asset : store.getAssetMap().getAssetMap().values()) {
            if (CONCENTRATION_STAT_IDS.contains(asset.getId()) && !registry.hasStat(asset.getId())) {
                registry.registerStat(asset.toStatDefinition());
            }
        }
        registry.freeze();

        StatDefinition regenRate = registry.getStat(StatId.hyforged("concentration-regen-rate-bps"));
        assertNotNull(regenRate, "Concentration regen rate stat should be registered");
        assertEquals("resource", regenRate.category());
        assertEquals(0, regenRate.defaultValue());
        assertEquals(0, regenRate.minValue());
        assertEquals(100000, regenRate.maxValue());
        assertTrue(regenRate.tags().contains("Domain=resource"));
        assertTrue(regenRate.tags().contains("Mechanic=aura"));
        assertTrue(regenRate.tags().contains("Mechanic=minion"));
        assertTrue(regenRate.tags().contains("Type=rate"));
        assertTrue(regenRate.tags().contains("Modifier=percent"));

        StatDefinition lossReduction = registry.getStat(StatId.hyforged("concentration-loss-reduction-bps"));
        assertNotNull(lossReduction, "Concentration loss reduction stat should be registered");
        assertEquals("defense", lossReduction.category());
        assertEquals(0, lossReduction.defaultValue());
        assertEquals(0, lossReduction.minValue());
        assertEquals(10000, lossReduction.maxValue());
        assertEquals(7500, lossReduction.softCapBps());
        assertTrue(lossReduction.tags().contains("Domain=defense"));
        assertTrue(lossReduction.tags().contains("Type=mitigation"));

        StatDefinition lossThreshold = registry.getStat(StatId.hyforged("concentration-loss-threshold-bps"));
        assertNotNull(lossThreshold, "Concentration loss threshold stat should be registered");
        assertEquals("resource", lossThreshold.category());
        assertEquals(7500, lossThreshold.defaultValue());
        assertEquals(0, lossThreshold.minValue());
        assertEquals(10000, lossThreshold.maxValue());
        assertTrue(lossThreshold.tags().contains("Domain=resource"));
        assertTrue(lossThreshold.tags().contains("Type=threshold"));
    }

    @Test
    void concentrationEffectsLoadIntoRegistry() {
        ensureEntityEffectStore();
        AssetStore<String, HyforgedEffectAsset, IndexedLookupTableAssetMap<String, HyforgedEffectAsset>> store =
                ensureHyforgedEffectStore();

        loadAssets(store,
                "Server/Hyforged/Effects/Buffs/FocusedMind.json",
                "Server/Hyforged/Effects/Buffs/IronWill.json",
                "Server/Hyforged/Effects/Buffs/MentalFortress.json",
                "Server/Hyforged/Effects/Buffs/MonksSerenity.json",
                "Server/Hyforged/Effects/Debuffs/MindFog.json",
                "Server/Hyforged/Effects/Debuffs/PsychicScream.json",
                "Server/Hyforged/Effects/Debuffs/ShatteredFocus.json",
                "Server/Hyforged/Effects/Debuffs/BrainRot.json"
        );

                HyforgedEffectRegistry registry = HyforgedEffectRegistry.get();
        registry.clear();
                Map<String, HyforgedEffectAsset> assetsById = new HashMap<>();
        for (HyforgedEffectAsset asset : store.getAssetMap().getAssetMap().values()) {
            if (!CONCENTRATION_EFFECT_IDS.contains(asset.getId())) {
                continue;
            }
            String effectId = asset.getEntityEffectId();
            assertNotNull(effectId, "Effect ID should not be null for " + asset.getId());
                        assetsById.put(asset.getId(), asset);
            registry.register(effectId, asset.getHyforgedModifiers());
        }

                assertEquals(
                                CONCENTRATION_EFFECT_IDS.size(),
                                assetsById.size(),
                                "All concentration effect assets should be loaded"
                );
                assertEquals(
                                CONCENTRATION_EFFECT_IDS.size(),
                                registry.size(),
                                "All concentration effects should be registered"
                );
                for (String assetId : CONCENTRATION_EFFECT_IDS) {
                        HyforgedEffectAsset asset = assetsById.get(assetId);
                        assertNotNull(asset, "Missing effect asset: " + assetId);
                        String effectId = asset.getEntityEffectId();
                        assertNotNull(registry.get(effectId), "Missing effect definition: " + effectId);
        }

                HyforgedEffectAsset focusedMindAsset = assetsById.get("hyforged:focused-mind");
                assertNotNull(focusedMindAsset, "Focused Mind asset should be loaded");
                HyforgedEffectDefinition focusedMind = registry.get(focusedMindAsset.getEntityEffectId());
        assertNotNull(focusedMind, "Focused Mind effect should be registered");
        assertTrue(
                focusedMind.getModifiers().stream().anyMatch(modifier ->
                        modifier.getStatId().equals("hyforged:concentration-regen-rate-bps")
                                && modifier.getStackType() == HyforgedModifier.StackType.INCREASED
                                && modifier.getAmount() == 2500
                ),
                "Focused Mind should grant +2500 INCREASED concentration regen rate"
        );

        HyforgedEffectAsset brainRotAsset = assetsById.get("hyforged:brain-rot");
        assertNotNull(brainRotAsset, "Brain Rot asset should be loaded");
        HyforgedEffectDefinition brainRot = registry.get(brainRotAsset.getEntityEffectId());
        assertNotNull(brainRot, "Brain Rot effect should be registered");
        assertTrue(
                brainRot.getModifiers().stream().anyMatch(modifier ->
                        modifier.getStatId().equals("hyforged:concentration-regen-rate-bps")
                                && modifier.getStackType() == HyforgedModifier.StackType.INCREASED
                                && modifier.getAmount() == -2000
                ),
                "Brain Rot should reduce concentration regen rate"
        );
        assertTrue(
                brainRot.getModifiers().stream().anyMatch(modifier ->
                        modifier.getStatId().equals("hyforged:concentration-loss-reduction-bps")
                                && modifier.getStackType() == HyforgedModifier.StackType.FLAT
                                && modifier.getAmount() == -2500
                ),
                "Brain Rot should apply concentration loss reduction penalty"
        );
    }

    @Test
    void concentrationAffixesLoadIntoRegistries() {
        AssetStore<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>> affixStore =
                ensureAffixDefinitionStore();

        loadAssets(affixStore,
                "Server/Hyforged/Affixes/Prefix/OfClarity.json",
                "Server/Hyforged/Affixes/Prefix/OfResolve.json",
                "Server/Hyforged/Affixes/Prefix/Steadfast.json",
                "Server/Hyforged/Affixes/Forged/MentalBastion.json",
                "Server/Hyforged/Affixes/Forged/UnshakeableFocus.json"
        );

        AffixDefinitionRegistry affixRegistry = AffixDefinitionRegistry.get();
        for (AffixDefinitionAsset asset : affixStore.getAssetMap().getAssetMap().values()) {
            if (CONCENTRATION_AFFIX_IDS.contains(asset.getId())) {
                affixRegistry.register(asset.toAffixDefinition());
            }
        }

        AffixDefinition clarity = affixRegistry.get("hyforged:of-clarity");
        assertNotNull(clarity, "of Clarity should be registered");
        assertEquals("prefix", clarity.type());
        assertEquals(5, clarity.getTierCount());
        assertTrue(clarity.getStatIds().contains("hyforged:concentration-regen-rate-bps"));

        AffixDefinition resolve = affixRegistry.get("hyforged:of-resolve");
        assertNotNull(resolve, "of Resolve should be registered");
        assertEquals("prefix", resolve.type());
        assertTrue(resolve.getStatIds().contains("hyforged:concentration-loss-reduction-bps"));

        AffixDefinition steadfast = affixRegistry.get("hyforged:steadfast");
        assertNotNull(steadfast, "Steadfast should be registered");
        assertEquals("prefix", steadfast.type());
        assertTrue(steadfast.getStatIds().contains("hyforged:concentration-loss-threshold-bps"));

        AffixDefinition mentalBastion = affixRegistry.get("hyforged:mental-bastion");
        assertNotNull(mentalBastion, "Mental Bastion should be registered");
        assertEquals("forged", mentalBastion.type());
        assertEquals(3, mentalBastion.getTierCount());
        assertTrue(mentalBastion.getStatIds().contains("hyforged:concentration-loss-reduction-bps"));
        assertTrue(mentalBastion.getStatIds().contains("hyforged:concentration-regen-rate-bps"));

        AffixDefinition unshakeableFocus = affixRegistry.get("hyforged:unshakeable-focus");
        assertNotNull(unshakeableFocus, "Unshakeable Focus should be registered");
        assertEquals("forged", unshakeableFocus.type());
        assertEquals(3, unshakeableFocus.getTierCount());
        assertTrue(unshakeableFocus.getStatIds().contains("hyforged:concentration-loss-threshold-bps"));
        assertTrue(unshakeableFocus.getStatIds().contains("hyforged:concentration-regen-rate-bps"));

        AssetStore<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>> poolStore =
                ensureAffixPoolStore();
        loadAssets(poolStore,
                "Server/Hyforged/AffixPools/WeaponMelee.json",
                "Server/Hyforged/AffixPools/Armor.json"
        );

        AffixPoolRegistry poolRegistry = AffixPoolRegistry.get();
        for (AffixPoolAsset asset : poolStore.getAssetMap().getAssetMap().values()) {
            if (CONCENTRATION_POOL_IDS.contains(asset.getId())) {
                poolRegistry.register(asset.toAffixPool());
            }
        }

        AffixPool weaponPool = poolRegistry.get("WeaponMelee");
        assertNotNull(weaponPool, "WeaponMelee pool should be registered");
        assertTrue(weaponPool.prefixes().contains("hyforged:of-clarity"));
        assertTrue(weaponPool.prefixes().contains("hyforged:of-resolve"));
        assertTrue(weaponPool.prefixes().contains("hyforged:steadfast"));
        assertTrue(weaponPool.forged().contains("hyforged:mental-bastion"));
        assertTrue(weaponPool.forged().contains("hyforged:unshakeable-focus"));

        AffixPool armorPool = poolRegistry.get("Armor");
        assertNotNull(armorPool, "Armor pool should be registered");
        assertTrue(armorPool.prefixes().contains("hyforged:of-clarity"));
        assertTrue(armorPool.prefixes().contains("hyforged:of-resolve"));
        assertTrue(armorPool.prefixes().contains("hyforged:steadfast"));
        assertTrue(armorPool.forged().contains("hyforged:mental-bastion"));
        assertTrue(armorPool.forged().contains("hyforged:unshakeable-focus"));
    }

        @Test
        void concentrationAffixesApplyToItems() {
                AssetStore<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>> affixStore =
                                ensureAffixDefinitionStore();

                loadAssets(affixStore,
                                "Server/Hyforged/Affixes/Prefix/OfClarity.json",
                                "Server/Hyforged/Affixes/Prefix/OfResolve.json",
                                "Server/Hyforged/Affixes/Prefix/Steadfast.json",
                                "Server/Hyforged/Affixes/Forged/MentalBastion.json",
                                "Server/Hyforged/Affixes/Forged/UnshakeableFocus.json"
                );

                AffixDefinitionRegistry affixRegistry = AffixDefinitionRegistry.get();
                for (AffixDefinitionAsset asset : affixStore.getAssetMap().getAssetMap().values()) {
                        if (CONCENTRATION_AFFIX_IDS.contains(asset.getId())) {
                                affixRegistry.register(asset.toAffixDefinition());
                        }
                }

                AffixService.reset();
                AffixService affixService = AffixService.get();

                ensureItemStore();

                ItemStack itemStack = affixService.createWithAffixes(
                                "Weapon_Longsword_Iron",
                                1,
                                List.of(
                                                AffixSpec.of("hyforged:of-clarity", 1),
                                                AffixSpec.of("hyforged:mental-bastion", 1)
                                ),
                                new Random(12345)
                );

                HyforgedItemData itemData = HyforgedItemDataService.read(itemStack);
                assertTrue(itemData.hasAffixes(), "Expected affixes to be applied to item");
                assertEquals(2, itemData.affixCount(), "Expected two affixes to be applied");

                RolledAffix clarity = findRolledAffix(itemData.affixes(), "hyforged:of-clarity");
                assertNotNull(clarity, "Expected of Clarity affix on item");
                RolledAffix.RolledStat clarityStat = clarity.rolledStats().get("hyforged:concentration-regen-rate-bps");
                assertNotNull(clarityStat, "of Clarity should roll concentration regen rate");
                assertEquals(HyforgedModifier.StackType.INCREASED, clarityStat.stackType());

                RolledAffix bastion = findRolledAffix(itemData.affixes(), "hyforged:mental-bastion");
                assertNotNull(bastion, "Expected Mental Bastion affix on item");
                RolledAffix.RolledStat lossReduction = bastion.rolledStats().get("hyforged:concentration-loss-reduction-bps");
                assertNotNull(lossReduction, "Mental Bastion should roll loss reduction");
                assertEquals(HyforgedModifier.StackType.MORE, lossReduction.stackType());
                RolledAffix.RolledStat regenRate = bastion.rolledStats().get("hyforged:concentration-regen-rate-bps");
                assertNotNull(regenRate, "Mental Bastion should roll regen rate");
                assertEquals(HyforgedModifier.StackType.INCREASED, regenRate.stackType());
        }

        private static RolledAffix findRolledAffix(@Nonnull List<RolledAffix> affixes, @Nonnull String affixId) {
                for (RolledAffix affix : affixes) {
                        if (affix.affixId().equals(affixId)) {
                                return affix;
                        }
                }
                return null;
        }

    private static void loadAssets(AssetStore<String, ?, ?> store, String... resources) {
        List<Path> paths = Arrays.stream(resources)
                .map(ConcentrationAssetLoadingTest::resourcePath)
                .collect(Collectors.toList());
        store.loadAssetsFromPaths(PACK_KEY, paths);
    }

    private static Path resourcePath(String resource) {
        Path path = Paths.get("src/main/resources").resolve(resource);
        assertTrue(path.toFile().exists(), "Missing resource: " + resource);
        return path.toAbsolutePath().normalize();
    }

    private static AssetStore<String, StatDefinitionAsset, IndexedLookupTableAssetMap<String, StatDefinitionAsset>>
    ensureStatDefinitionStore() {
        AssetStore<String, StatDefinitionAsset, IndexedLookupTableAssetMap<String, StatDefinitionAsset>> store =
                (AssetStore<String, StatDefinitionAsset, IndexedLookupTableAssetMap<String, StatDefinitionAsset>>) AssetRegistry
                        .getAssetStore(StatDefinitionAsset.class);
        if (store == null) {
            store = TestAssetStore.builder(
                            String.class,
                            StatDefinitionAsset.class,
                            new IndexedLookupTableAssetMap<>(StatDefinitionAsset[]::new)
                    )
                    .setCodec(StatDefinitionAsset.CODEC)
                    .setKeyFunction(StatDefinitionAsset::getId)
                    .setReplaceOnRemove(key -> null)
                    .build();
            AssetRegistry.register(store);
        }
        return store;
    }

    private static AssetStore<String, EntityEffect, IndexedLookupTableAssetMap<String, EntityEffect>> ensureEntityEffectStore() {
        AssetStore<String, EntityEffect, IndexedLookupTableAssetMap<String, EntityEffect>> store =
                (AssetStore<String, EntityEffect, IndexedLookupTableAssetMap<String, EntityEffect>>) AssetRegistry
                        .getAssetStore(EntityEffect.class);
        if (store == null) {
            store = TestAssetStore.builder(
                            String.class,
                            EntityEffect.class,
                            new IndexedLookupTableAssetMap<>(EntityEffect[]::new)
                    )
                    .setCodec(EntityEffect.CODEC)
                    .setKeyFunction(EntityEffect::getId)
                    .setReplaceOnRemove(EntityEffect::new)
                    .build();
            AssetRegistry.register(store);
        }
        return store;
    }

        private static AssetStore<String, Item, DefaultAssetMap<String, Item>> ensureItemStore() {
                AssetStore<String, Item, DefaultAssetMap<String, Item>> store =
                                (AssetStore<String, Item, DefaultAssetMap<String, Item>>) AssetRegistry.getAssetStore(Item.class);
                if (store == null) {
                        store = TestAssetStore.builder(
                                                        String.class,
                                                        Item.class,
                                                        new DefaultAssetMap<>()
                                        )
                                        .setCodec(Item.CODEC)
                                        .setKeyFunction(Item::getId)
                                        .build();
                        AssetRegistry.register(store);
                }
                return store;
        }

    private static AssetStore<String, HyforgedEffectAsset, IndexedLookupTableAssetMap<String, HyforgedEffectAsset>>
    ensureHyforgedEffectStore() {
        AssetStore<String, HyforgedEffectAsset, IndexedLookupTableAssetMap<String, HyforgedEffectAsset>> store =
                (AssetStore<String, HyforgedEffectAsset, IndexedLookupTableAssetMap<String, HyforgedEffectAsset>>) AssetRegistry
                        .getAssetStore(HyforgedEffectAsset.class);
        if (store == null) {
            store = TestAssetStore.builder(
                            String.class,
                            HyforgedEffectAsset.class,
                            new IndexedLookupTableAssetMap<>(HyforgedEffectAsset[]::new)
                    )
                    .setCodec(HyforgedEffectAsset.CODEC)
                    .setKeyFunction(HyforgedEffectAsset::getId)
                    .setReplaceOnRemove(key -> null)
                    .build();
            AssetRegistry.register(store);
        }
        return store;
    }

    private static AssetStore<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>>
    ensureAffixDefinitionStore() {
        AssetStore<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>> store =
                (AssetStore<String, AffixDefinitionAsset, IndexedLookupTableAssetMap<String, AffixDefinitionAsset>>) AssetRegistry
                        .getAssetStore(AffixDefinitionAsset.class);
        if (store == null) {
            store = TestAssetStore.builder(
                            String.class,
                            AffixDefinitionAsset.class,
                            new IndexedLookupTableAssetMap<>(AffixDefinitionAsset[]::new)
                    )
                    .setCodec(AffixDefinitionAsset.CODEC)
                    .setKeyFunction(AffixDefinitionAsset::getId)
                    .setReplaceOnRemove(key -> null)
                    .build();
            AssetRegistry.register(store);
        }
        return store;
    }

    private static AssetStore<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>> ensureAffixPoolStore() {
        AssetStore<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>> store =
                (AssetStore<String, AffixPoolAsset, IndexedLookupTableAssetMap<String, AffixPoolAsset>>) AssetRegistry
                        .getAssetStore(AffixPoolAsset.class);
        if (store == null) {
            store = TestAssetStore.builder(
                            String.class,
                            AffixPoolAsset.class,
                            new IndexedLookupTableAssetMap<>(AffixPoolAsset[]::new)
                    )
                    .setCodec(AffixPoolAsset.CODEC)
                    .setKeyFunction(AffixPoolAsset::getId)
                    .setReplaceOnRemove(key -> null)
                    .build();
            AssetRegistry.register(store);
        }
        return store;
    }
}
