package reign.software.hyforged.combat.hud;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.combat.scaling.MonsterLevelComponent;
import reign.software.hyforged.quality.component.HyforgedNPCQualityComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adds a death entry to the player's combat log when they die.
 * <p>
 * If killed by a mob, shows mob name, level, quality, and lethal damage amount.
 * If killed by environment or unknown source, shows a generic death message.
 * <p>
 * Example combat log lines:
 * <ul>
 *   <li>{@code YOU DIED — [Rare] Skeleton Lv.15 (28 damage)}</li>
 *   <li>{@code YOU DIED — Wolf Black Lv.3 (19 damage)}</li>
 *   <li>{@code YOU DIED — Environment}</li>
 * </ul>
 */
public class PlayerDeathCombatLogSystem extends DeathSystems.OnDeathSystem {

    private static final Logger LOGGER = Logger.getLogger(PlayerDeathCombatLogSystem.class.getName());

    public PlayerDeathCombatLogSystem() {
        super();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(DeathComponent.getComponentType(), Player.getComponentType());
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull DeathComponent deathComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Get the player's UUID for combat log
        UUIDComponent uuidComp = store.getComponent(playerRef, UUIDComponent.getComponentType());
        if (uuidComp == null) {
            return;
        }
        UUID playerUuid = uuidComp.getUuid();

        // Build the death message
        Damage deathInfo = deathComponent.getDeathInfo();
        Message line = buildDeathLine(deathInfo, store);

        CombatLogHudSystem.addExtraLine(playerUuid, line);

        LOGGER.log(Level.FINE, "Added death entry to combat log for player {0}", playerUuid);
    }

    /**
     * Build a formatted death message for the combat log.
     */
    @Nonnull
    private Message buildDeathLine(@Nullable Damage deathInfo, @Nonnull Store<EntityStore> store) {
        Message line = Message.raw("");

        // "YOU DIED" in red
        line.insert(Message.raw("YOU DIED").color("#FF4444"));

        if (deathInfo == null) {
            return line;
        }

        float damage = deathInfo.getAmount();
        Damage.Source source = deathInfo.getSource();

        if (source instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> killerRef = entitySource.getRef();
            if (killerRef != null && killerRef.isValid()) {
                line.insert(Message.raw(" — ").color("#888888"));
                appendMobInfo(line, killerRef, store);
                if (damage > 0) {
                    line.insert(Message.raw(" (").color("#888888"));
                    line.insert(Message.raw(String.valueOf((int) damage) + " damage").color("#FF8888"));
                    line.insert(Message.raw(")").color("#888888"));
                }
                return line;
            }
        }

        // Non-entity death (environment, fall, etc.)
        line.insert(Message.raw(" — ").color("#888888"));
        line.insert(Message.raw("Environment").color("#AAAAAA"));
        if (damage > 0) {
            line.insert(Message.raw(" (").color("#888888"));
            line.insert(Message.raw(String.valueOf((int) damage) + " damage").color("#FF8888"));
            line.insert(Message.raw(")").color("#888888"));
        }

        return line;
    }

    /**
     * Append mob info (quality, name, level) to the combat log death line.
     */
    private void appendMobInfo(
            @Nonnull Message line,
            @Nonnull Ref<EntityStore> killerRef,
            @Nonnull Store<EntityStore> store
    ) {
        // Quality
        String quality = resolveQuality(killerRef, store);
        if (quality != null && !quality.isBlank() && !"Common".equalsIgnoreCase(quality)) {
            String qualityColor = resolveQualityColor(quality);
            line.insert(Message.raw("[" + capitalizeFirst(quality) + "] ").color(qualityColor));
        }

        // Name
        String name = resolveKillerName(killerRef, store);
        line.insert(Message.raw(name).color("#FF6666"));

        // Level
        int level = resolveLevel(killerRef, store);
        if (level > 0) {
            line.insert(Message.raw(" Lv." + level).color("#CCCCCC"));
        }
    }

    @Nonnull
    private String resolveKillerName(@Nonnull Ref<EntityStore> killerRef, @Nonnull Store<EntityStore> store) {
        // Try display name first
        DisplayNameComponent displayNameComp = store.getComponent(killerRef, DisplayNameComponent.getComponentType());
        if (displayNameComp != null) {
            Message displayName = displayNameComp.getDisplayName();
            if (displayName != null) {
                String rawText = displayName.getRawText();
                if (rawText != null && !rawText.isEmpty()) {
                    return rawText;
                }
            }
        }

        // Try NPC role name
        NPCEntity npcEntity = store.getComponent(killerRef, NPCEntity.getComponentType());
        if (npcEntity != null) {
            String roleName = npcEntity.getRoleName();
            if (roleName != null && !roleName.isEmpty()) {
                return roleName.replace('_', ' ');
            }
        }

        // Try player name (PvP)
        Player killerPlayer = store.getComponent(killerRef, Player.getComponentType());
        if (killerPlayer != null) {
            return killerPlayer.getDisplayName();
        }

        return "Unknown";
    }

    @Nullable
    private String resolveQuality(@Nonnull Ref<EntityStore> killerRef, @Nonnull Store<EntityStore> store) {
        HyforgedNPCQualityComponent quality = store.getComponent(killerRef,
                HyforgedPlugin.getInstance().getNpcQualityComponentType());
        if (quality != null && !quality.getQualityId().isBlank()) {
            return quality.getQualityId();
        }
        return null;
    }

    private int resolveLevel(@Nonnull Ref<EntityStore> killerRef, @Nonnull Store<EntityStore> store) {
        MonsterLevelComponent levelComp = store.getComponent(killerRef,
                HyforgedPlugin.getInstance().getMonsterLevelComponentType());
        if (levelComp != null && levelComp.getLevel() > 0) {
            return levelComp.getLevel();
        }
        return 0;
    }

    /**
     * Resolve the hex color for a quality tier from Hytale's ItemQuality asset registry.
     */
    @Nonnull
    private static String resolveQualityColor(@Nonnull String qualityId) {
        try {
            ItemQuality quality = ItemQuality.getAssetMap().getAsset(qualityId);
            if (quality != null && quality.getTextColor() != null) {
                Color c = quality.getTextColor();
                return String.format("#%02X%02X%02X",
                        Byte.toUnsignedInt(c.red),
                        Byte.toUnsignedInt(c.green),
                        Byte.toUnsignedInt(c.blue));
            }
        } catch (Exception ignored) {
            // Asset registry may not be ready
        }
        return "#CCCCCC";
    }

    @Nonnull
    private static String capitalizeFirst(@Nonnull String text) {
        if (text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
