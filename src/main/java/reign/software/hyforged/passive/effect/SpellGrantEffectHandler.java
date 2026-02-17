package reign.software.hyforged.passive.effect;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.passive.component.PlayerSpellsComponent;
import reign.software.hyforged.passive.model.PassiveNode;
import reign.software.hyforged.passive.model.PassiveNodeEffect;

import javax.annotation.Nonnull;
import java.util.logging.Level;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Effect handler for spell-grant type passive node effects.
 * <p>
 * Grants or revokes spells on the entity's {@link PlayerSpellsComponent}
 * when nodes are allocated or deallocated.
 * <p>
 * JSON effect format:
 * <pre>
 * {
 *   "Type": "spell-grant",
 *   "SpellId": "hyforged:fireball"
 * }
 * </pre>
 * <p>
 * Spells track their source nodes, so a spell is only fully removed when
 * no nodes granting it remain allocated.
 */
public final class SpellGrantEffectHandler implements PassiveEffectHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Effect type identifier.
     */
    public static final String EFFECT_TYPE = "spell-grant";

    private final ComponentType<EntityStore, PlayerSpellsComponent> spellsComponentType;

    /**
     * Create a new handler.
     *
     * @param spellsComponentType The component type for PlayerSpellsComponent
     */
    public SpellGrantEffectHandler(
            @Nonnull ComponentType<EntityStore, PlayerSpellsComponent> spellsComponentType
    ) {
        this.spellsComponentType = spellsComponentType;
    }

    @Override
    public void apply(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect) {
        PlayerSpellsComponent spellsComponent = entityRef.getStore().getComponent(entityRef, spellsComponentType);
        if (spellsComponent == null) {
            LOGGER.atWarning().log("Entity has no PlayerSpellsComponent, cannot grant spell from node: %s", node.id());
            return;
        }

        String spellId = effect.getString("SpellId");
        if (spellId == null) {
            LOGGER.atWarning().log("spell-grant effect missing 'SpellId' field on node: %s", node.id());
            return;
        }

        spellsComponent.grantSpell(spellId, node.id());
        LOGGER.at(Level.FINE).log("Granted spell: %s from node %s", spellId, node.id());
    }

    @Override
    public void remove(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect) {
        PlayerSpellsComponent spellsComponent = entityRef.getStore().getComponent(entityRef, spellsComponentType);
        if (spellsComponent == null) {
            return;
        }

        String spellId = effect.getString("SpellId");
        if (spellId == null) {
            return;
        }

        spellsComponent.revokeSpell(spellId, node.id());
        LOGGER.at(Level.FINE).log("Revoked spell grant: %s from node %s", spellId, node.id());
    }

    @Override
    @Nonnull
    public String getTooltipText(@Nonnull PassiveNodeEffect effect) {
        String spellId = effect.getString("SpellId");
        if (spellId == null) {
            return "Grants: Unknown Spell";
        }
        
        // TODO: Look up spell display name from spell registry when available
        String displayName = spellId;
        if (spellId.contains(":")) {
            displayName = spellId.substring(spellId.indexOf(':') + 1);
        }
        
        // Capitalize and format
        displayName = formatDisplayName(displayName);
        
        return "Grants: " + displayName;
    }
    
    private String formatDisplayName(String id) {
        // Convert kebab-case or snake_case to Title Case
        String[] parts = id.replace('_', '-').split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }
}
