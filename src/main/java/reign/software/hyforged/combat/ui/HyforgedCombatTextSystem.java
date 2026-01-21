package reign.software.hyforged.combat.ui;

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
import com.hypixel.hytale.protocol.CombatTextUpdate;
import com.hypixel.hytale.protocol.ComponentUpdate;
import com.hypixel.hytale.protocol.ComponentUpdateType;
import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.entityui.EntityUIModule;
import com.hypixel.hytale.server.core.modules.entityui.UIComponentList;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.combat.HyforgedAutoBlockSystem;
import reign.software.hyforged.combat.HyforgedCriticalHitSystem;
import reign.software.hyforged.combat.HyforgedHitResolutionSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * ECS system that creates enhanced combat text with crit/miss/block indicators.
 * <p>
 * This system runs in {@code inspectDamageGroup} after the crit system and before
 * Hytale's built-in {@code EntityUIEvents} system. It provides custom combat text
 * that includes:
 * <ul>
 *   <li>Critical hit indicators (§c✦ prefix)</li>
 *   <li>Block indicators (§6⛨ prefix)</li>
 *   <li>Miss text (§7Miss)</li>
 * </ul>
 * <p>
 * Combat text is sent to the attacker's client via the entity viewer system.
 */
public class HyforgedCombatTextSystem extends DamageEventSystem {

    /** Meta key to indicate combat text has been handled by Hyforged */
    public static final MetaKey<Boolean> COMBAT_TEXT_HANDLED = 
            Damage.META_REGISTRY.registerMetaObject(data -> Boolean.FALSE);
    
    // Color codes for combat text
    private static final String COLOR_CRIT = "§c";      // Red for crits
    private static final String COLOR_BLOCK = "§6";     // Gold for blocks
    private static final String COLOR_MISS = "§7";      // Gray for misses
    private static final String COLOR_NORMAL = "§f";    // White for normal hits
    
    // Symbol prefixes
    private static final String SYMBOL_CRIT = "✦ ";     // Sparkle for crit
    private static final String SYMBOL_BLOCK = "⛨ ";   // Shield for block
    
    @Nonnull
    private final ComponentType<EntityStore, EntityTrackerSystems.Visible> visibleComponentType = 
            EntityModule.get().getVisibleComponentType();
    @Nonnull
    private final ComponentType<EntityStore, UIComponentList> uiComponentListComponentType = 
            EntityUIModule.get().getUIComponentListType();
    @Nonnull
    private final Query<EntityStore> query = Query.and(this.visibleComponentType, this.uiComponentListComponentType);
    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    public HyforgedCombatTextSystem() {
        // Run before Hytale's EntityUIEvents so we can provide our own text
        this.dependencies = Set.of(
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
        return this.query;
    }
    
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return this.dependencies;
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damage
    ) {
        // Check for miss first - if missed, show miss text
        Boolean missed = damage.getIfPresentMetaObject(HyforgedHitResolutionSystem.MISS);
        if (Boolean.TRUE.equals(missed)) {
            sendCombatText(archetypeChunk, index, store, damage, COLOR_MISS + "Miss");
            return;
        }
        
        // Get damage amount
        float damageAmount = damage.getAmount();
        if (damageAmount <= 0.0F) {
            return;
        }
        
        // Build the combat text
        StringBuilder textBuilder = new StringBuilder();
        
        // Check for crit
        Boolean isCrit = damage.getIfPresentMetaObject(HyforgedCriticalHitSystem.CRITICAL_HIT);
        boolean wasCrit = Boolean.TRUE.equals(isCrit);
        
        // Check for block
        Boolean isBlocked = damage.getIfPresentMetaObject(HyforgedAutoBlockSystem.AUTO_BLOCKED);
        Boolean isNativeBlocked = damage.getIfPresentMetaObject(Damage.BLOCKED);
        boolean wasBlocked = Boolean.TRUE.equals(isBlocked) || Boolean.TRUE.equals(isNativeBlocked);
        
        // Add color and symbol prefix
        if (wasCrit) {
            textBuilder.append(COLOR_CRIT).append(SYMBOL_CRIT);
        } else if (wasBlocked) {
            textBuilder.append(COLOR_BLOCK).append(SYMBOL_BLOCK);
        } else {
            textBuilder.append(COLOR_NORMAL);
        }
        
        // Add damage number
        textBuilder.append((int) Math.floor(damageAmount));
        
        // Add suffix for blocked attacks
        if (wasBlocked && !wasCrit) {
            textBuilder.append(" (Blocked)");
        }
        
        sendCombatText(archetypeChunk, index, store, damage, textBuilder.toString());
    }
    
    /**
     * Sends combat text to the attacker's client.
     */
    private void sendCombatText(
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            int index,
            @Nonnull Store<EntityStore> store,
            @Nonnull Damage damage,
            @Nonnull String text
    ) {
        // Only send if attacker is a player
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) {
            return;
        }
        
        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (!attackerRef.isValid()) {
            return;
        }
        
        PlayerRef playerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        
        EntityTrackerSystems.EntityViewer entityViewer = store.getComponent(
                attackerRef, EntityTrackerSystems.EntityViewer.getComponentType()
        );
        if (entityViewer == null) {
            return;
        }
        
        // Get defender ref for the combat text target
        Ref<EntityStore> defenderRef = archetypeChunk.getReferenceTo(index);
        
        // Get hit angle for text positioning
        Float hitAngleDeg = damage.getIfPresentMetaObject(Damage.HIT_ANGLE);
        
        // Create and queue the combat text update
        ComponentUpdate update = new ComponentUpdate();
        update.type = ComponentUpdateType.CombatText;
        
        CombatTextUpdate combatTextUpdate = new CombatTextUpdate();
        combatTextUpdate.hitAngleDeg = hitAngleDeg == null ? 0.0F : hitAngleDeg;
        combatTextUpdate.text = text;
        update.combatTextUpdate = combatTextUpdate;
        
        entityViewer.queueUpdate(defenderRef, update);
        
        // Mark that we handled the combat text so EntityUIEvents doesn't duplicate it
        damage.putMetaObject(COMBAT_TEXT_HANDLED, true);
    }
}
