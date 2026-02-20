package reign.software.hyforged.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedConfig;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.damage.DamageTypeExtensionRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Spell block and suppression system.
 * <p>
 * <b>This system is fully inactive until the {@code spellBlockEnabled} config flag is set to
 * {@code true} via {@link HyforgedConfig#setSpellBlockEnabled(boolean)}.</b>
 * <p>
 * When enabled, this system runs in the {@code filterDamageGroup} after
 * {@link HyforgedAutoBlockSystem} and applies two independent defensive mechanics
 * against spell-type damage:
 * <ol>
 *   <li><b>Spell Block</b> — rolls {@code hyforged:block-spell-chance-bps} (defender).
 *       If triggered, reduces damage by {@code hyforged:block-mitigation-bps} (default 50 %)
 *       and marks the event as blocked. Spell block and suppression are mutually exclusive —
 *       block takes priority.</li>
 *   <li><b>Spell Suppression</b> — rolls {@code hyforged:suppression-chance-bps} (defender).
 *       If triggered, reduces damage by {@code hyforged:suppression-effect-bps} (default 50 %).</li>
 * </ol>
 * <p>
 * Spell damage is detected via the damage cause's element tag (defined per damage type in
 * {@code Server/Hyforged/Stats/Damage/} JSON files). A damage cause with
 * {@code elementTag = "spell"} is treated as spell damage.
 * <p>
 * TODO: No damage causes currently define {@code elementTag = "spell"}. Both the config
 *       guard and the element-tag guard must pass before any mechanic fires. The system will
 *       remain fully dormant until (a) a spell damage type is added to
 *       {@code Server/Hyforged/Stats/Damage/} and (b) the config flag is enabled.
 */
public class HyforgedSpellBlockSystem extends DamageEventSystem {

    /** Element tag value that marks spell-type damage causes. */
    private static final String SPELL_ELEMENT_TAG = "spell";

    /** Chance to block spell damage (defender). */
    private static final StatId BLOCK_SPELL_CHANCE = StatId.hyforged("block-spell-chance-bps");

    /** Fraction of spell damage blocked when block triggers. Shared with auto-block. */
    private static final StatId BLOCK_MITIGATION   = StatId.hyforged("block-mitigation-bps");

    /** Chance to suppress (partially reduce) spell damage (defender). */
    private static final StatId SUPPRESSION_CHANCE = StatId.hyforged("suppression-chance-bps");

    /** Fraction of spell damage reduced when suppression triggers (defender). */
    private static final StatId SUPPRESSION_EFFECT = StatId.hyforged("suppression-effect-bps");

    /** Default block/suppression mitigation when the stat is not defined: 50 % = 5000 bps. */
    private static final int DEFAULT_MITIGATION_BPS = 5000;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    // Cached stat indices
    private int blockSpellChanceIndex  = -1;
    private int blockMitigationIndex   = -1;
    private int suppressionChanceIndex = -1;
    private int suppressionEffectIndex = -1;
    private boolean indicesCached      = false;

    public HyforgedSpellBlockSystem() {
        this.query = StatAccessor.getStatMapType();
        this.dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, HyforgedAutoBlockSystem.class),
            new SystemDependency<>(Order.BEFORE, DamageSystems.ApplyDamage.class)
        );
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damage
    ) {
        // Feature flag guard — no gameplay impact until explicitly enabled
        if (!HyforgedConfig.get().isSpellBlockEnabled()) {
            return;
        }

        // Skip cancelled or zero-damage events
        if (damage.isCancelled() || damage.getAmount() <= 0) {
            return;
        }

        // Skip if already processed by CombatService
        Boolean pipelineProcessed = damage.getIfPresentMetaObject(CombatMeta.PIPELINE_PROCESSED);
        if (pipelineProcessed != null && pipelineProcessed) {
            return;
        }

        // Resolve damage cause
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause == null) {
            return;
        }

        // TODO: No damage causes currently define elementTag="spell".
        //       This system will remain dormant until a spell damage type is added to
        //       Server/Hyforged/Stats/Damage/ with "ElementTag": "spell".
        String elementTag = DamageTypeExtensionRegistry.get().getElementTagForDamage(damageCause);
        if (!SPELL_ELEMENT_TAG.equals(elementTag)) {
            return;
        }

        ensureIndicesCached();

        // --- Spell Block ---
        int spellBlockChanceBps = blockSpellChanceIndex >= 0
                ? StatAccessor.getStatValueInt(archetypeChunk, index, blockSpellChanceIndex) : 0;
        if (spellBlockChanceBps > 0 && CombatMath.rollChance(spellBlockChanceBps)) {
            int mitigationBps = blockMitigationIndex >= 0
                    ? StatAccessor.getStatValueInt(archetypeChunk, index, blockMitigationIndex) : DEFAULT_MITIGATION_BPS;
            if (mitigationBps <= 0) {
                mitigationBps = DEFAULT_MITIGATION_BPS;
            }
            float mitigated = CombatMath.applyReduction(damage.getAmount(), mitigationBps);
            damage.setAmount(mitigated);
            damage.putMetaObject(CombatMeta.BLOCK_MITIGATION_BPS, mitigationBps);
            damage.putMetaObject(Damage.BLOCKED, Boolean.TRUE);
            // Block and suppression are mutually exclusive — stop here
            return;
        }

        // --- Spell Suppression ---
        int suppressionChanceBps = suppressionChanceIndex >= 0
                ? StatAccessor.getStatValueInt(archetypeChunk, index, suppressionChanceIndex) : 0;
        if (suppressionChanceBps > 0 && CombatMath.rollChance(suppressionChanceBps)) {
            int suppressionEffectBps = suppressionEffectIndex >= 0
                    ? StatAccessor.getStatValueInt(archetypeChunk, index, suppressionEffectIndex) : DEFAULT_MITIGATION_BPS;
            if (suppressionEffectBps <= 0) {
                suppressionEffectBps = DEFAULT_MITIGATION_BPS;
            }
            float suppressed = CombatMath.applyReduction(damage.getAmount(), suppressionEffectBps);
            damage.setAmount(suppressed);
        }
    }

    /**
     * Cache stat indices on first use.
     */
    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        blockSpellChanceIndex  = registry.getIndex(BLOCK_SPELL_CHANCE);
        blockMitigationIndex   = registry.getIndex(BLOCK_MITIGATION);
        suppressionChanceIndex = registry.getIndex(SUPPRESSION_CHANCE);
        suppressionEffectIndex = registry.getIndex(SUPPRESSION_EFFECT);
        indicesCached          = true;
    }
}
