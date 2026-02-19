package reign.software.hyforged.combat.scaling;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.quality.component.HyforgedNPCQualityComponent;
import reign.software.hyforged.quality.system.NPCQualityAffixStatSystem;
import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Sets NPC nameplates to display name, quality, affixes, and level.
 * <p>
 * Implemented as a {@link RefChangeSystem} on {@link HyforgedNPCQualityComponent}
 * so that nameplates are set AFTER quality and level components have been committed
 * to the store. This is necessary because {@code CommandBuffer.getComponent()} reads
 * from the committed store — components staged via {@code putComponent()} by earlier
 * RefSystems during entity-add are not visible until the buffer is consumed.
 * <p>
 * By the time HyforgedNPCQualityComponent is committed, MonsterLevelComponent has
 * already been committed (it was queued first by HyforgedMonsterScalingSystem), so
 * both level and quality data are readable.
 * <p>
 * Nameplate format: {@code {Quality} Name Lv.X - AffixOne, AffixTwo}
 * <p>
 * Quality is shown in brackets for non-Common tiers. Common mobs show no quality tag.
 * <p>
 * Examples:
 * <ul>
 *   <li>{@code Skeleton Lv.5} (Common — no tag)</li>
 *   <li>{@code Rare Skeleton Lv.15}</li>
 *   <li>{@code Rare Skeleton Lv.15 - Armored, Regenerating}</li>
 *   <li>{@code Legendary Skeleton Lv.30 - Vortex, Undying, Soul Linked}</li>
 * </ul>
 */
public class NPCNameplateSystem extends RefChangeSystem<EntityStore, HyforgedNPCQualityComponent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
     private static final String NAMEPLATE_AFFIX_SEPARATOR = " - ";

    @Nonnull
    private final ComponentType<EntityStore, HyforgedNPCQualityComponent> qualityComponentType;

    @Nonnull
    private final ComponentType<EntityStore, NPCEntity> npcComponentType;

    @Nonnull
    private final ComponentType<EntityStore, MonsterLevelComponent> levelComponentType;

    @Nonnull
    private final ComponentType<EntityStore, Nameplate> nameplateComponentType;

    @Nonnull
    private final ComponentType<EntityStore, DisplayNameComponent> displayNameComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    public NPCNameplateSystem() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.qualityComponentType = plugin.getNpcQualityComponentType();
        this.npcComponentType = NPCEntity.getComponentType();
        this.levelComponentType = plugin.getMonsterLevelComponentType();
        this.nameplateComponentType = Nameplate.getComponentType();
        this.displayNameComponentType = DisplayNameComponent.getComponentType();

        this.query = qualityComponentType;

        this.dependencies = Set.of(
                new SystemDependency<>(Order.AFTER, NPCQualityAffixStatSystem.class)
        );
    }

    @Nonnull
    @Override
    public ComponentType<EntityStore, HyforgedNPCQualityComponent> componentType() {
        return qualityComponentType;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull HyforgedNPCQualityComponent component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        updateNameplate(ref, component, commandBuffer);
    }

    @Override
    public void onComponentSet(
            @Nonnull Ref<EntityStore> ref,
            @Nullable HyforgedNPCQualityComponent previous,
            @Nonnull HyforgedNPCQualityComponent component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        updateNameplate(ref, component, commandBuffer);
    }

    @Override
    public void onComponentRemoved(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull HyforgedNPCQualityComponent component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Quality removed — reset nameplate to base name + level only
        NPCEntity npcEntity = commandBuffer.getComponent(ref, npcComponentType);
        if (npcEntity == null) {
            return;
        }

        String baseName = resolveBaseName(npcEntity, ref, commandBuffer);
        MonsterLevelComponent levelComp = commandBuffer.getComponent(ref, levelComponentType);
        int level = (levelComp != null) ? levelComp.getLevel() : 0;

        String nameplateText = buildNameplateText(null, baseName, List.of(), level);
        commandBuffer.putComponent(ref, nameplateComponentType, new Nameplate(nameplateText));
    }

    /**
     * Build and set the full nameplate for an NPC with quality data.
     */
    private void updateNameplate(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull HyforgedNPCQualityComponent qualityComp,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        NPCEntity npcEntity = commandBuffer.getComponent(ref, npcComponentType);
        if (npcEntity == null) {
            return;
        }

        String baseName = resolveBaseName(npcEntity, ref, commandBuffer);

        // Level is already committed to the store (queued before quality)
        MonsterLevelComponent levelComp = commandBuffer.getComponent(ref, levelComponentType);
        int level = (levelComp != null) ? levelComp.getLevel() : 0;

        String qualityId = (qualityComp.getQualityId() != null && !qualityComp.getQualityId().isBlank())
                ? qualityComp.getQualityId()
                : null;
        List<RolledAffix> affixes = qualityComp.getAffixes();

        String nameplateText = buildNameplateText(qualityId, baseName, affixes, level);
        commandBuffer.putComponent(ref, nameplateComponentType, new Nameplate(nameplateText));

        LOGGER.at(Level.FINE).log("Set nameplate for NPC '%s': %s",
                npcEntity.getRoleName(), nameplateText);
    }

    /**
     * Resolve the base display name for an NPC.
     * <p>
     * Resolution priority:
     * <ol>
     *   <li>DisplayNameComponent raw text</li>
     *   <li>NPC role name (with underscore-to-space formatting)</li>
     * </ol>
     */
    @Nonnull
    private String resolveBaseName(
            @Nonnull NPCEntity npcEntity,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Try DisplayNameComponent first (may have a proper localized name)
        DisplayNameComponent displayNameComp = commandBuffer.getComponent(ref, displayNameComponentType);
        if (displayNameComp != null) {
            Message displayName = displayNameComp.getDisplayName();
            if (displayName != null) {
                String rawText = displayName.getRawText();
                if (rawText != null && !rawText.isEmpty()) {
                    return rawText;
                }
            }
        }

        // Fall back to role name, formatted
        String roleName = npcEntity.getRoleName();
        if (roleName != null && !roleName.isEmpty()) {
            return formatRoleName(roleName);
        }

        return "Unknown";
    }

    /**
    * Build the full nameplate text string.
    * <p>
    * Format: {@code {Quality} Name Lv.X - AffixOne, AffixTwo, ...}
     *
     * @param qualityId the NPC's quality tier (e.g., "Common", "Rare", "Epic"), or null for none
     * @param baseName  the base NPC name
     * @param affixes   rolled affixes on this NPC
     * @param level     the monster's level (0 = don't show)
     * @return the formatted nameplate string
     */
    @Nonnull
    static String buildNameplateText(
            @Nullable String qualityId,
            @Nonnull String baseName,
            @Nonnull List<RolledAffix> affixes,
            int level
    ) {
        StringBuilder primaryLine = new StringBuilder();

        // Quality tag
        if (qualityId != null && !qualityId.isBlank()) {
            primaryLine.append(capitalizeFirst(qualityId)).append(" ");
        }

        // Base name
        primaryLine.append(baseName);

        // Level
        if (level > 0) {
            primaryLine.append(" Lv.").append(level);
        }

        String affixLine = resolveAffixDisplayNames(affixes);
        if (affixLine.isEmpty()) {
            return primaryLine.toString();
        }

        return primaryLine.append(NAMEPLATE_AFFIX_SEPARATOR).append(affixLine).toString();
    }

    /**
     * Resolve display names for all rolled affixes.
     *
     * @param affixes the rolled affixes
     * @return comma-separated display names, or empty string
     */
    @Nonnull
    private static String resolveAffixDisplayNames(@Nonnull List<RolledAffix> affixes) {
        if (affixes.isEmpty()) {
            return "";
        }

        AffixDefinitionRegistry registry = AffixDefinitionRegistry.get();
        StringBuilder sb = new StringBuilder();

        for (RolledAffix affix : affixes) {
            AffixDefinition definition = registry.get(affix.affixId());
            if (definition == null) {
                continue;
            }

            String displayName = definition.displayName();
            if (displayName == null || displayName.isBlank()) {
                continue;
            }

            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(displayName);
        }

        return sb.toString();
    }

    /**
     * Format a role name by replacing underscores with spaces.
     * Example: "Skeleton_Praetorian" → "Skeleton Praetorian"
     */
    @Nonnull
    private static String formatRoleName(@Nonnull String roleName) {
        return roleName.replace('_', ' ');
    }

    /**
     * Capitalize the first letter of a string.
     */
    @Nonnull
    private static String capitalizeFirst(@Nonnull String text) {
        if (text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

}
