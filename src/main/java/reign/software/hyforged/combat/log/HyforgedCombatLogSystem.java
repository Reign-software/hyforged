package reign.software.hyforged.combat.log;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
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
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import reign.software.hyforged.combat.CombatMeta;
import reign.software.hyforged.combat.HyforgedAutoBlockSystem;
import reign.software.hyforged.combat.HyforgedCriticalHitSystem;
import reign.software.hyforged.combat.HyforgedHitResolutionSystem;
import reign.software.hyforged.quality.component.HyforgedNPCQualityComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Combat log recording system.
 * <p>
 * Runs in the {@code inspectDamageGroup} after all damage modifications to
 * collect the final state of each damage event and record it to the combat log.
 * <p>
 * Records events for both player attackers and defenders, allowing players
 * to review their combat history.
 */
public class HyforgedCombatLogSystem extends DamageEventSystem {

    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, Nameplate> nameplateComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, DisplayNameComponent> displayNameComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, NPCEntity> npcEntityComponentType;

    @Nonnull
    private final ComponentType<EntityStore, HyforgedNPCQualityComponent> qualityComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    public HyforgedCombatLogSystem(
            @Nonnull ComponentType<EntityStore, HyforgedNPCQualityComponent> qualityComponentType
    ) {
        this.playerComponentType = Player.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
        this.nameplateComponentType = Nameplate.getComponentType();
        this.displayNameComponentType = DisplayNameComponent.getComponentType();
        this.npcEntityComponentType = NPCEntity.getComponentType();
        this.qualityComponentType = qualityComponentType;
        
        // Query for entities with UUIDComponent (all entities we can log)
        this.query = uuidComponentType;
        
        // Run at the end of inspect group, after crit system
        this.dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, HyforgedCriticalHitSystem.class),
            new SystemDependency<>(Order.BEFORE, DamageSystems.EntityUIEvents.class)
        );
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
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
        // Get defender info
        UUIDComponent defenderUuid = archetypeChunk.getComponent(index, uuidComponentType);
        if (defenderUuid == null) {
            return;
        }
        
        // Build the combat event
        CombatEvent.Builder eventBuilder = CombatEvent.builder()
                .timestamp(System.currentTimeMillis())
                .defenderUuid(defenderUuid.getUuid())
                .defenderName(getEntityName(archetypeChunk, index, store));
        
        // Defender quality (NPCs only)
        HyforgedNPCQualityComponent defenderQualityComp = archetypeChunk.getComponent(index, qualityComponentType);
        if (defenderQualityComp != null && !defenderQualityComp.getQualityId().isEmpty()) {
            eventBuilder.defenderQuality(defenderQualityComp.getQualityId());
        }
        
        // Get damage info
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (damageCause != null) {
            eventBuilder.damageCauseId(damageCause.getId());
        }
        
        // Get damage amounts
        // Read base damage from meta (set by hit resolution), fallback to final if not set
        Float baseDmg = damage.getIfPresentMetaObject(CombatMeta.BASE_DAMAGE);
        eventBuilder.baseDamage(baseDmg != null && baseDmg > 0 ? baseDmg : damage.getAmount());
        eventBuilder.finalDamage(damage.getAmount());
        
        // Read resistance and penetration from meta
        Integer resistance = damage.getIfPresentMetaObject(CombatMeta.RESISTANCE_BPS);
        Integer penetration = damage.getIfPresentMetaObject(CombatMeta.PENETRATION_BPS);
        eventBuilder.resistanceAppliedBps(resistance != null ? resistance : 0);
        eventBuilder.penetrationAppliedBps(penetration != null ? penetration : 0);
        
        // Check for miss
        Boolean missed = damage.getIfPresentMetaObject(HyforgedHitResolutionSystem.MISS);
        eventBuilder.missed(missed != null && missed);
        
        // Check for block
        Boolean blocked = damage.getIfPresentMetaObject(Damage.BLOCKED);
        eventBuilder.blocked(blocked != null && blocked);
        
        // Check for auto-block
        Boolean autoBlocked = damage.getIfPresentMetaObject(HyforgedAutoBlockSystem.AUTO_BLOCKED);
        eventBuilder.autoBlocked(autoBlocked != null && autoBlocked);
        
        // Check for crit
        Boolean crit = damage.getIfPresentMetaObject(HyforgedCriticalHitSystem.CRITICAL_HIT);
        eventBuilder.criticalHit(crit != null && crit);
        
        Integer critMultiplier = damage.getIfPresentMetaObject(HyforgedCriticalHitSystem.CRITICAL_MULTIPLIER);
        eventBuilder.critMultiplierBps(critMultiplier != null ? critMultiplier : 0);
        
        // Get attacker info if entity source
        if (damage.getSource() instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef.isValid()) {
                UUIDComponent attackerUuidComp = store.getComponent(attackerRef, uuidComponentType);
                if (attackerUuidComp != null) {
                    eventBuilder.attackerUuid(attackerUuidComp.getUuid());
                    eventBuilder.attackerName(getEntityNameFromRef(attackerRef, store));
                }
                // Attacker quality (NPCs only)
                HyforgedNPCQualityComponent attackerQualityComp = store.getComponent(attackerRef, qualityComponentType);
                if (attackerQualityComp != null && !attackerQualityComp.getQualityId().isEmpty()) {
                    eventBuilder.attackerQuality(attackerQualityComp.getQualityId());
                }
            }
        }
        
        CombatEvent event = eventBuilder.build();
        
        // Record to defender's log if they're a player
        Player defenderPlayer = archetypeChunk.getComponent(index, playerComponentType);
        if (defenderPlayer != null) {
            CombatLogService.get().recordEvent(defenderUuid.getUuid(), event);
        }
        
        // Record to attacker's log if they're a player
        if (event.attackerUuid() != null && damage.getSource() instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef.isValid()) {
                Player attackerPlayer = store.getComponent(attackerRef, playerComponentType);
                if (attackerPlayer != null) {
                    CombatLogService.get().recordEvent(event.attackerUuid(), event);
                }
            }
        }
    }
    
    /**
     * Get a display name for an entity.
     * <p>
     * Resolution order: Player name → Nameplate → DisplayNameComponent (raw) → NPCEntity role → UUID.
     */
    @Nonnull
    private String getEntityName(
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            int index,
            @Nonnull Store<EntityStore> store
    ) {
        // Try player name
        Player player = archetypeChunk.getComponent(index, playerComponentType);
        if (player != null) {
            String name = player.getDisplayName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        
        // Prefer DisplayNameComponent (clean base name without level/quality decorations)
        DisplayNameComponent displayNameComp = archetypeChunk.getComponent(index, displayNameComponentType);
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
        NPCEntity npcEntity = archetypeChunk.getComponent(index, npcEntityComponentType);
        if (npcEntity != null) {
            String roleName = npcEntity.getRoleName();
            if (roleName != null && !roleName.isEmpty()) {
                return roleName;
            }
        }
        
        // Fallback to Nameplate, but strip level suffix and quality stars
        Nameplate nameplate = archetypeChunk.getComponent(index, nameplateComponentType);
        if (nameplate != null) {
            String text = nameplate.getText();
            if (text != null && !text.isEmpty()) {
                return stripNameplateDecorations(text);
            }
        }
        
        // Fallback to UUID
        UUIDComponent uuid = archetypeChunk.getComponent(index, uuidComponentType);
        if (uuid != null) {
            return uuid.getUuid().toString().substring(0, 8);
        }
        
        return "Unknown";
    }
    
    /**
     * Get a display name for an entity from a reference.
     * <p>
     * Resolution order: Player name → Nameplate → DisplayNameComponent (raw) → NPCEntity role → UUID.
     */
    @Nonnull
    private String getEntityNameFromRef(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store
    ) {
        // Try player name
        Player player = store.getComponent(ref, playerComponentType);
        if (player != null) {
            String name = player.getDisplayName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        
        // Prefer DisplayNameComponent (clean base name without level/quality decorations)
        DisplayNameComponent displayNameComp = store.getComponent(ref, displayNameComponentType);
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
        NPCEntity npcEntity = store.getComponent(ref, npcEntityComponentType);
        if (npcEntity != null) {
            String roleName = npcEntity.getRoleName();
            if (roleName != null && !roleName.isEmpty()) {
                return roleName;
            }
        }
        
        // Fallback to Nameplate, but strip level suffix and quality stars
        Nameplate nameplate = store.getComponent(ref, nameplateComponentType);
        if (nameplate != null) {
            String text = nameplate.getText();
            if (text != null && !text.isEmpty()) {
                return stripNameplateDecorations(text);
            }
        }
        
        // Fallback to UUID
        UUIDComponent uuid = store.getComponent(ref, uuidComponentType);
        if (uuid != null) {
            return uuid.getUuid().toString().substring(0, 8);
        }
        
        return "Unknown";
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
